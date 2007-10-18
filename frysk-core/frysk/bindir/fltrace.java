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

	/**
	 * Object that performs a pattern matching of symbol
	 * name. null for "anything" matcher.
	 */
	final public Pattern namePattern;

	/** See namePattern */
	final public Pattern sonamePattern, versionPattern;

	public WorkingSetRule(boolean addition, String nameRe, String sonameRe, String versionRe) {
	    this.addition = addition;
	    this.namePattern = Pattern.compile((nameRe != null) ? nameRe : ".*");
	    this.sonamePattern = Pattern.compile((sonameRe != null) ? sonameRe : ".*");
	    this.versionPattern = Pattern.compile((versionRe != null) ? versionRe : ".*");
	}

	public String toString() {
	    return ""
		+ (this.addition ? '+' : '-')
		+ this.namePattern.pattern()
		+ "@" + this.sonamePattern.pattern()
		+ "@@" + this.versionPattern.pattern();
	}
    }

    class MyController
	implements LtraceController
    {
	// ArrayList<WorkingSetRule>
	private final List pltRules = new ArrayList()/*, dynRules, symRules*/;

	public void gotPltRules(List pltRules) {
	    this.pltRules.addAll(pltRules);
	}

	public void fileMapped(final Task task, final ObjectFile objf, final Ltrace.Driver driver) {
	    try {
		if (pltRules.size() > 0) {
		    final Set candidates = new HashSet(); // Set<TracePoint>
		    final Set workingSet = new HashSet(); // Set<TracePoint>
		    boolean candidatesInited = false;

		    for (Iterator it = pltRules.iterator(); it.hasNext(); ) {
			final WorkingSetRule rule = (WorkingSetRule)it.next();
			if ((rule.sonamePattern.pattern().equals("MAIN")
			     && task.getProc().getExe().equals(objf.getFilename().getPath()))
			    || rule.sonamePattern.matcher(objf.getSoname()).matches())
			{
			    if (!candidatesInited) {
				candidatesInited = true;
				objf.eachTracePoint(new ObjectFile.TracePointIterator() {
					public void tracePoint(TracePoint tp) {
					    if (candidates.add(tp))
						logger.log(Level.FINE, "candidate `" + tp.symbol.name + "'.", this);
					}
				    }, TracePointOrigin.PLT);
			    }

			    Set iterateOver = rule.addition ? candidates : workingSet;
			    for (Iterator jt = iterateOver.iterator(); jt.hasNext(); ) {
				TracePoint tp = (TracePoint)jt.next();
				if (rule.namePattern.matcher(tp.symbol.name).matches()) {
				    // This is PLT rule, so we will use verneeds.
				    // If any of the matches...
				    boolean versionMatch = false;
				    for (int i = 0; i < tp.symbol.verneeds.length; ++i)
					if (rule.versionPattern.matcher(tp.symbol.verneeds[i].name).matches()) {
					    versionMatch = true;
					    break;
					}

				    if (versionMatch) {
					if (rule.addition) {
					    if (workingSet.add(tp))
						logger.log(Level.CONFIG, rule + ": add `" + tp.symbol.name + "'.", this);
					}
					else {
					    jt.remove();
					    logger.log(Level.CONFIG, rule + ": remove `" + tp.symbol.name + "'.", this);
					}
				    }
				}
			    }
			}
		    }

		    for (Iterator it = workingSet.iterator(); it.hasNext(); )
			driver.tracePoint(task, (TracePoint)it.next());
		}
	    }
	    catch (lib.dwfl.ElfException ee) {
		ee.printStackTrace();
	    }
	}
    }

    final MyController controller = new MyController();
    final Ltrace tracer = new Ltrace(controller);

  LtraceObserver ltraceObserver = new LtraceObserver() {
      private Map levelMap = new HashMap();

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
	for (int i = 0; i < args.length; ++i)
	  System.err.print((i > 0 ? ", " : "") + args[i]);
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

      public synchronized void funcallEnter(Task task, Symbol symbol, Object[] args)
      {
	String symbolName = symbol.name;
	String callerLibrary = symbol.getParent().getSoname();
	String eventName = callerLibrary + "->" + /*libraryName + ":" +*/ symbolName;
	eventEntry(task, symbol, "call", eventName, args);
      }

      public synchronized void funcallLeave(Task task, Symbol symbol, Object retVal)
      {
	eventLeave(task, symbol, "leave", symbol.name, retVal);
      }

      public synchronized void syscallEnter(Task task, Syscall syscall, Object[] args)
      {
	eventEntry(task, syscall, "syscall", syscall.getName(), args);
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
    };

  public static void main(String[] args)
  {
    (new fltrace()).run(args);
  }

    public List parseRules(String arg) {
	String[] strs = arg.split(":", -1);
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

	    if (str.length() > 0) {
		if (str.charAt(0) == '+') {
		    addition = true;
		    str = str.substring(1);
		}
		else if (str.charAt(0) == '-') {
		    addition = false;
		    str = str.substring(1);
		}
		else if (i == 0)
		    addition = true;
		else
		    throw new RuntimeException("Syntax error in rule, first letter has to be + or -.");
	    }
	    else if (i == 0)
		addition = true;
	    else
		throw new RuntimeException("Syntax error in rule, missing + or -.");

	    if (!str.equals(""))
		symbolRe = str;
	    else
		symbolRe = null;

	    logger.log(Level.FINE, i + ": " + str + ": symbol=" + symbolRe + ", soname=" + sonameRe + ", version=" + versionRe);
	    WorkingSetRule rule = new WorkingSetRule(addition, symbolRe, sonameRe, versionRe);
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

    parser.add(new Option("plt", "trace library calls done via PLT", "RULE[,RULE]...") {
	    public void parsed(String arg) {
		controller.gotPltRules(parseRules(arg));
	    }
    });

    parser.setHeader("usage: fltrace [OPTIONS] COMMAND ARGS...");

    command = parser.parse(args);

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
