// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
//
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.bindir;

import java.util.*;
import java.util.logging.*;
import java.util.regex.*;
import java.io.File;
import inua.util.PrintWriter;

import gnu.classpath.tools.getopt.*;

import frysk.proc.*;
import frysk.sys.Sig;
import frysk.util.CommandlineParser;
import frysk.stack.*;
import lib.dwfl.ElfSymbolVersion;

import frysk.ftrace.*;

public class fltrace
{
  protected static final Logger logger = Logger.getLogger("frysk");

  // The command to execute
  static String[] command;

  // The process id to trace
  static int pid;

  // Command and arguments to exec.
  ArrayList commandAndArguments;

  // True if a PID was requested.
  boolean requestedPid;

  //Where to send output.
  PrintWriter writer;

    class WorkingSetRule {
	final public boolean addition;
	final public boolean stackTrace;

	/**
	 * Object that performs a pattern matching of symbol
	 * name. null for "anything" matcher.
	 */
	final public Pattern namePattern;

	/** See namePattern */
	final public Pattern sonamePattern, versionPattern;

	public WorkingSetRule(boolean addition, boolean stackTrace, String nameRe, String sonameRe, String versionRe) {
	    this.addition = addition;
	    this.stackTrace = stackTrace;
	    this.namePattern = Pattern.compile((nameRe != null) ? nameRe : ".*");
	    this.sonamePattern = Pattern.compile((sonameRe != null) ? sonameRe : ".*");
	    this.versionPattern = Pattern.compile((versionRe != null) ? versionRe : ".*");
	}

	public String toString() {
	    return ""
		+ (this.addition ? "" : "-")
		+ (this.stackTrace ? "#" : "")
		+ this.namePattern.pattern()
		+ "@" + this.sonamePattern.pattern()
		+ "@@" + this.versionPattern.pattern();
	}
    }

    interface LtraceControllerObserver
    {
	void shouldStackTraceOn(Set symbols);
    }

    class MyController
	implements LtraceController
    {
	// ArrayList<WorkingSetRule>
	private final List pltRules = new ArrayList();
	private final List dynRules = new ArrayList();
	private final List symRules = new ArrayList();
	private final LtraceControllerObserver observer;

	public MyController(LtraceControllerObserver observer)
	{
	    this.observer = observer;
	}

	public void gotPltRules(List rules) {
	    logger.log(Level.FINER, "Got " + rules.size() + " PLT rules.");
	    this.pltRules.addAll(rules);
	}

	public void gotDynRules(List rules) {
	    logger.log(Level.FINER, "Got " + rules.size() + " DYNAMIC rules.");
	    this.dynRules.addAll(rules);
	}

	public void gotSymRules(List rules) {
	    logger.log(Level.FINER, "Got " + rules.size() + " SYMTAB rules.");
	    this.symRules.addAll(rules);
	}

	private boolean checkVersionMatches(final TracePoint tp, final WorkingSetRule rule)
	{
	    ElfSymbolVersion[] vers = (tp.origin == TracePointOrigin.PLT)
		? (ElfSymbolVersion[])tp.symbol.verneeds
		: (ElfSymbolVersion[])tp.symbol.verdefs;

	    // When there is no version assigned to symbol, we pretend it has
	    // a version of ''.  Otherwise we require one of the versions to
	    // match the version pattern.
	    if (vers.length == 0) {
		if (rule.versionPattern.matcher("").matches()) {
		    logger.log(Level.FINE, rule + ": `" + tp.symbol.name
			       + "' version match, no version.");
		    return true;
		}
	    }
	    else
		for (int i = 0; i < vers.length; ++i)
		    if (rule.versionPattern.matcher(vers[i].name).matches()) {
			logger.log(Level.FINE, rule + ": `" + tp.symbol.name
				   + "' version match `" + vers[i].name+ "'.");
			return true;
		    }

	    return false;
	}

	public void applyTracingRules(final Task task, final ObjectFile objf, final Ltrace.Driver driver,
				      final List rules, final TracePointOrigin origin)
	    throws lib.dwfl.ElfException
	{
	    logger.log(Level.FINER, "Building working set for origin " + origin + ".");

	    // Skip the set if it's empty...
	    if (rules.isEmpty())
		return;

	    // Set<TracePoint>, all tracepoints in objfile.
	    final Set candidates = new HashSet();
	    // Set<TracePoint>, incrementally built working set.
	    final Set workingSet = new HashSet();
	    // Set<TracePoint>, incrementally built set of tracepoints
	    // that should stacktrace.
	    final Set stackTraceSet = new HashSet();
	    boolean candidatesInited = false;

	    // Loop through all the rules, and use them to build
	    // workingSet from candidates.  Candidates are initialized
	    // lazily inside the loop.
	    for (Iterator it = rules.iterator(); it.hasNext(); ) {
		final WorkingSetRule rule = (WorkingSetRule)it.next();
		logger.log(Level.FINEST, "Considering rule " + rule + ".");

		// MAIN is meta-soname meaning "main executable".
		if ((rule.sonamePattern.pattern().equals("MAIN")
		     && task.getProc().getExe().equals(objf.getFilename().getPath()))
		    || rule.sonamePattern.matcher(objf.getSoname()).matches())
		{
			if (!candidatesInited) {
			    candidatesInited = true;
			    objf.eachTracePoint(new ObjectFile.TracePointIterator() {
				    public void tracePoint(TracePoint tp) {
					if (candidates.add(tp))
					    logger.log(Level.FINE, "candidate `" + tp.symbol.name + "'.");
				    }
				}, origin);
			}

			if (rule.addition)
			    // For '+' rules iterate over candidates,
			    // and add what matches to workingSet, and
			    // maybe to stackTraceSet.
			    for (Iterator jt = candidates.iterator(); jt.hasNext(); ) {
				TracePoint tp = (TracePoint)jt.next();
				if (rule.namePattern.matcher(tp.symbol.name).matches()
				    && checkVersionMatches(tp, rule))
				{
				    if (workingSet.add(tp))
				        logger.log(Level.CONFIG, rule + ": add `" + tp.symbol.name + "'.");
				    if (rule.stackTrace
					&& stackTraceSet.add(tp))
				        logger.log(Level.CONFIG, rule + ": stack trace on `" + tp.symbol.name + "'.");
				}
			    }
			else {
			    // For '-' or '-#' rules iterate over
    			    // workingSet or stackTraceSet, and remove
    			    // what matches.
			    Set iterateOver = rule.stackTrace ? stackTraceSet : workingSet;
			    for (Iterator jt = iterateOver.iterator(); jt.hasNext(); ) {
				TracePoint tp = (TracePoint)jt.next();
				if (rule.namePattern.matcher(tp.symbol.name).matches()
				    && checkVersionMatches(tp, rule)) {
					jt.remove();
					if (!rule.stackTrace)
					    stackTraceSet.remove(tp);
					logger.log(Level.CONFIG, rule + ": remove `" + tp.symbol.name + "'.");
				}
			    }
			}
		}
	    }

	    // Finally, apply constructed working set.
	    logger.log(Level.FINER, "Applying working set for origin " + origin + ".");
	    for (Iterator it = workingSet.iterator(); it.hasNext(); )
		driver.tracePoint(task, (TracePoint)it.next());

	    // And warn our console front end that it should stack
	    // trace if it sees one of these...
	    HashSet stackTraceSymbols = new HashSet();
	    for (Iterator it = stackTraceSet.iterator(); it.hasNext(); )
		stackTraceSymbols.add(((TracePoint)it.next()).symbol);
	    observer.shouldStackTraceOn(stackTraceSymbols);
	}

	public void fileMapped(final Task task, final ObjectFile objf, final Ltrace.Driver driver) {
	    try {
		applyTracingRules(task, objf, driver, pltRules, TracePointOrigin.PLT);
		applyTracingRules(task, objf, driver, dynRules, TracePointOrigin.DYNAMIC);
		applyTracingRules(task, objf, driver, symRules, TracePointOrigin.SYMTAB);
	    }
	    catch (lib.dwfl.ElfException ee) {
		ee.printStackTrace();
	    }
	}
    }

    class MyLtraceObserver
	implements LtraceObserver,
		   LtraceControllerObserver
    {
      private Map levelMap = new HashMap();

      // Which system calls should yield a stack trace.
      private HashSet syscallStackTraceSet = null;

      // Which symbols should yield a stack trace.
      private HashSet symbolsStackTraceSet = new HashSet();

      public synchronized void shouldStackTraceOn(Set symbols)
      {
	  symbolsStackTraceSet.addAll(symbols);
      }

      private Object lastItem = null;
      private Task lastTask = null;

      private int getLevel(Task task)
      {
	int level = 0;
	Integer l = (Integer)levelMap.get(task);
	if (l != null)
	  level = l.intValue();
	return level;
      }

      private void setLevel(Task task, int level)
      {
	levelMap.put(task, new Integer(level));
      }

      private boolean lineOpened()
      {
	return lastItem != null;
      }

      private boolean myLineOpened(Task task, Object item)
      {
	return lastItem == item && lastTask == task;
      }

      private void updateOpenLine(Task task, Object item)
      {
	lastItem = item;
	lastTask = task;
      }

      private String repeat(char c, int count)
      {
	// by Stephen Friedrich
	char[] fill = new char[count];
	Arrays.fill(fill, c);
	return new String(fill);
      }

      private void eventEntry(Task task, Object item, String eventType,
			      String eventName, Object[] args)
      {
	int level = this.getLevel(task);
	String spaces = repeat(' ', level);
	this.setLevel(task, ++level);

        if (lineOpened())
	  System.err.println('\\');

    	System.err.print("[" + task.getTaskId().intValue() + "] "
			 + spaces + eventType + " ");
	System.err.print(eventName + "(");
	for (int i = 0; i < args.length; ++i) {
	    System.err.print(i > 0 ? ", " : "");
	    // Temporary hack to get proper formatting before
	    // something more sane lands.
	    if (args[i] instanceof Long)
		System.err.print("0x" + Long.toHexString(((Long)args[i]).longValue()));
	    else if (args[i] instanceof Integer)
		System.err.print("0x" + Integer.toHexString(((Integer)args[i]).intValue()));
	    else
		System.err.print(args[i]);
	}
    	System.err.print(")");

        updateOpenLine(task, item);
      }

      private void eventLeave(Task task, Object item, String eventType,
			      String eventName, Object retVal)
      {
	int level = this.getLevel(task);
	this.setLevel(task, --level);

        if (!myLineOpened(task, item))
	  {
            if (lineOpened())
	      System.err.println();
	    String spaces = repeat(' ', level);
	    System.err.print("[" + task.getTaskId().intValue() + "] "
			     + spaces + eventType + " " + eventName);
	  }

	System.err.println(" = " + retVal);

        updateOpenLine(null, null);
      }

      private void eventSingle(Task task, String eventName)
      {
	int pid = task.getTid();
	int level = this.getLevel(task);

        if (lineOpened())
	  System.err.println("\\");
	System.err.println("[" + pid + "] " + repeat(' ', level) + eventName);

        updateOpenLine(null, null);
      }

      private void generateStackTrace(Task task)
      {
	  eventSingle(task, "dumping stack trace:");

	  Frame frame = StackFactory.createFrame(task);
	  StackFactory.printStack(writer, frame);
	  writer.flush();
	  updateOpenLine(null, null);
      }

      public void setSyscallStackTracing(HashSet syscallSet)
      {
	  syscallStackTraceSet = syscallSet;
      }

      public synchronized void funcallEnter(Task task, Symbol symbol, Object[] args)
      {
	  String symbolName = symbol.name;
	  String callerLibrary = symbol.getParent().getSoname();
	  String eventName = callerLibrary + "->" + /*libraryName + ":" +*/ symbolName;
	  eventEntry(task, symbol, "call", eventName, args);

	  // If this systsysem call is in the stack tracing HashSet,
	  // get a stack trace before continuing on.
	  if (symbolsStackTraceSet != null
	      && symbolsStackTraceSet.contains(symbol))
	      generateStackTrace(task);
      }

      public synchronized void funcallLeave(Task task, Symbol symbol, Object retVal)
      {
	  eventLeave(task, symbol, "leave", symbol.name, retVal);
      }

      public synchronized void syscallEnter(Task task, Syscall syscall, Object[] args)
      {
	  eventEntry(task, syscall, "syscall", syscall.getName(), args);

	  // If this systsysem call is in the stack tracing HashSet,
	  // get a stack trace before continuing on.
	  if (syscallStackTraceSet != null
	      && syscallStackTraceSet.contains(syscall.getName()))
	      generateStackTrace(task);
      }

      public synchronized void syscallLeave(Task task, Syscall syscall, Object retVal)
      {
	  eventLeave(task, syscall, "syscall leave", syscall.getName(), retVal);
      }

      public synchronized void fileMapped(Task task, File file)
      {
	eventSingle(task, "map " + file);
      }

      public synchronized void fileUnmapped(Task task, File file)
      {
	eventSingle(task, "unmap " + file);
      }

      public synchronized void taskAttached(Task task)
      {
	eventSingle(task, "attached");
      }

      public synchronized void taskTerminated(Task task, boolean signal, int value)
      {
	if (lineOpened())
	  System.err.println('\\');
	int pid = task.getTid();
	System.err.print("[" + pid + "] ");
	if (signal)
	  System.err.println("killed by " + Sig.toPrintString(value));
	else
	  System.err.println("+++ exited (status " + value + ") +++");
      }
    }

    final MyLtraceObserver ltraceObserver = new MyLtraceObserver();
    final MyController controller = new MyController(ltraceObserver);
    final Ltrace tracer = new Ltrace(controller);

    public static void main(String[] args)
    {
	(new fltrace()).run(args);
    }

    public List parseRules(String arg) {
	String[] strs = arg.split(",", -1);
	List rules = new ArrayList();
	for (int i = 0; i < strs.length; ++i) {
	    // 111 single fully qualified symbol:           'symbol@soname@@version'
	    // 101 symbol of given version in all dsos:     'symbol@@version'
	    // 100 symbol of given name from any dso:       'symbol'
	    // 011 all symbols of given version of the dso: '@soname@@version'
	    // 010 all symbols of given soname:             '@soname'
	    // 001 all symbols of given version:            '@@version'
	    // 000 all symbols of all versions in all dsos: ''

	    String str = strs[i];
	    final String symbolRe, sonameRe, versionRe;
	    final boolean addition;
	    final boolean stackTrace;
	    int pos;

	    if ((pos = str.indexOf("@@")) != -1) {
		versionRe = str.substring(pos + 2);
		str = str.substring(0, pos);
	    }
	    else
		versionRe = null;

	    if ((pos = str.indexOf('@')) != -1) {
		sonameRe = str.substring(pos + 1);
		str = str.substring(0, pos);
	    }
	    else
		sonameRe = null;

	    if (str.length() > 0 && str.charAt(0) == '-') {
		addition = false;
		str = str.substring(1);
	    }
	    else
		addition = true;

	    if (str.length() > 0 && str.charAt(0) == '#') {
		stackTrace = true;
		str = str.substring(1);
	    }
	    else
		stackTrace = false;

	    if (!str.equals(""))
		symbolRe = str;
	    else
		symbolRe = null;

	    logger.log(Level.FINE, i + ": " + str + ": symbol=" + symbolRe + ", soname=" + sonameRe + ", version=" + versionRe);
	    WorkingSetRule rule = new WorkingSetRule(addition, stackTrace, symbolRe, sonameRe, versionRe);
	    rules.add(rule);
	}
	return rules;
    }

  public void run(String[] args)
  {
    CommandlineParser parser = new CommandlineParser("fltrace") {
	protected void validate() throws OptionException {
	  if (! requestedPid && commandAndArguments == null)
	    throw new OptionException("no command or PID specified");
	}

	public void parseCommand (String[] command)
	{
	 commandAndArguments = new ArrayList();

	 for (int i = 0; i < command.length; i++)
	   commandAndArguments.add(command[i]);
	}

	public void parsePids (ProcId[] pids)
	{
	  for (int i = 0; i < pids.length; ++i)
	    tracer.addTracePid(pids[i]);
	  requestedPid = true;
	}
    };

    parser.add(new Option('c', "trace children as well") {
	public void parsed(String arg0) throws OptionException
	{
	    tracer.setTraceChildren();
	}
    });

    parser.add(new Option('p', "pid to trace", "PID") {
	public void parsed(String arg) throws OptionException
	{
	    try {
		int pid = Integer.parseInt(arg);
		tracer.addTracePid(new ProcId(pid));
		requestedPid = true;
	    }
	    catch (NumberFormatException e) {
		OptionException oe = new OptionException("couldn't parse pid: " + arg);
		oe.initCause(e);
		throw oe;
	    }
	}
    });

    parser.add(new Option('S', "also trace signals") {
	    public void parsed(String arg) {
		tracer.setTraceSignals();
	    }
    });

    parser.add(new Option('s', "trace system calls", "CALL[,CALL]...") {
	    public void parsed(String arg) {
		tracer.setTraceSyscalls();
		StringTokenizer st = new StringTokenizer(arg, ",");
		HashSet set = new HashSet(2);
		while (st.hasMoreTokens())
		    {
			String name = st.nextToken();
			// FIXME: there's no good way to error out if the
			// syscall is unknown.
			set.add(name);
		    }
		ltraceObserver.setSyscallStackTracing(set);
	    }
    });

    final List pltRules = new ArrayList();
    parser.add(new Option("plt", "trace library calls done via PLT", "RULE[,RULE]...") {
	    public void parsed(String arg) {
		pltRules.add(arg);
	    }
    });

    final List dynRules = new ArrayList();
    parser.add(new Option("dyn", "trace entry points from DYNAMIC symtab", "RULE[,RULE]...") {
	    public void parsed(String arg) {
		dynRules.add(arg);
	    }
    });

    final List symRules = new ArrayList();
    parser.add(new Option("sym", "trace entry points from symbol table", "RULE[,RULE]...") {
	    public void parsed(String arg) {
		symRules.add(arg);
	    }
    });

    parser.setHeader("usage: fltrace [OPTIONS] COMMAND ARGS...");

    command = parser.parse(args);

    // We need to load/apply rules this roundabout way to get all log
    // messages.
    for (Iterator it = pltRules.iterator(); it.hasNext(); )
	controller.gotPltRules(parseRules((String)it.next()));
    for (Iterator it = dynRules.iterator(); it.hasNext(); )
	controller.gotDynRules(parseRules((String)it.next()));
    for (Iterator it = symRules.iterator(); it.hasNext(); )
	controller.gotSymRules(parseRules((String)it.next()));

    if (writer == null)
	writer = new PrintWriter(System.out);

    tracer.addObserver(ltraceObserver);
    if (commandAndArguments != null)
    {
	String[] cmd = (String[]) commandAndArguments.toArray(new String[0]);
	tracer.trace(cmd);
    }
    else
	tracer.trace();
  }
}
