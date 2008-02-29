// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

import inua.util.PrintWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.*;
import java.util.regex.Pattern;

import frysk.ftrace.Ftrace;
import frysk.ftrace.ObjectFile;
import frysk.ftrace.Symbol;
import frysk.ftrace.TracePoint;
import frysk.ftrace.TracePointOrigin;
import frysk.isa.signals.Signal;
import frysk.isa.signals.SignalTable;
import frysk.isa.syscalls.Syscall;
import frysk.isa.syscalls.SyscallTable;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.util.CommandlineParser;
import frysk.util.Glob;
import frysk.util.Util;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import lib.dwfl.ElfSymbolVersion;

abstract class Rule
{
    final public boolean addition;
    final public boolean stackTrace;

    protected Rule(boolean addition, boolean stackTrace) {
	this.addition = addition;
	this.stackTrace = stackTrace;
    }

    public String toString() {
	return ""
	    + (this.addition ? "" : "-")
	    + (this.stackTrace ? "#" : "");
    }

    public void apply(Logger logger, Collection candidates,
		    Set workingSet, Set stackTraceSet) {
	if (this.addition)
	    // For '+' rules iterate over candidates,
	    // and add what matches to workingSet, and
	    // maybe to stackTraceSet.
	    for (Iterator jt = candidates.iterator(); jt.hasNext(); ) {
		Object candidate = jt.next();
		if (this.matches(candidate))
		{
		    if (workingSet.add(candidate))
			logger.log(Level.CONFIG, this + ": add " + candidate + "'.");
		    if (this.stackTrace
			&& stackTraceSet.add(candidate))
			logger.log(Level.CONFIG, this + ": stack trace on " + candidate + ".");
		}
	    }
	else {
	    // For '-' or '-#' rules iterate over
	    // workingSet or stackTraceSet, and remove
	    // what matches.
	    Set iterateOver = this.stackTrace ? stackTraceSet : workingSet;
	    for (Iterator jt = iterateOver.iterator(); jt.hasNext(); ) {
		Object candidate = jt.next();
		if (this.matches(candidate)) {
		    jt.remove();
		    if (!this.stackTrace)
			stackTraceSet.remove(candidate);
		    logger.log(Level.CONFIG, this + ": remove " + candidate + ".");
		}
	    }
	}
    }

    abstract public boolean matches(Object traceable);
}

class SymbolRule
    extends Rule
{
    /** See namePattern */
    final public Pattern sonamePattern, versionPattern;

    /**
     * Object that performs a pattern matching of a symbol name. null
     * for "anything" matcher.
     */
    final public Pattern namePattern;

    public SymbolRule(boolean addition, boolean stackTrace,
		      String nameRe, String sonameRe, String versionRe) {
	super (addition, stackTrace);
	this.sonamePattern = Glob.compile((sonameRe != null) ? sonameRe : "*");
	this.versionPattern = Glob.compile((versionRe != null) ? versionRe : "*");
	this.namePattern = Glob.compile((nameRe != null) ? nameRe : "*");
    }

    public String toString() {
	return super.toString()
	    + this.namePattern.pattern()
	    + "@" + this.sonamePattern.pattern()
	    + "@@" + this.versionPattern.pattern();
    }


    private boolean checkVersionMatches(final TracePoint tp)
    {
	ElfSymbolVersion[] vers = (tp.origin == TracePointOrigin.PLT)
	    ? (ElfSymbolVersion[])tp.symbol.verneeds
	    : (ElfSymbolVersion[])tp.symbol.verdefs;

	// When there is no version assigned to symbol, we pretend it has
	// a version of ''.  Otherwise we require one of the versions to
	// match the version pattern.
	if (vers.length == 0) {
	    if (this.versionPattern.matcher("").matches())
		return true;
	}
	else
	    for (int i = 0; i < vers.length; ++i)
		if (this.versionPattern.matcher(vers[i].name).matches())
		    return true;

	return false;
    }

    private boolean checkNameMatches(final TracePoint tp)
    {
	Symbol symbol = tp.symbol;

	if (this.namePattern.matcher(symbol.name).matches())
	    return true;

	if (symbol.aliases != null)
	    for (int i = 0; i < symbol.aliases.size(); ++i) {
		String alias = (String)symbol.aliases.get(i);
		if (this.namePattern.matcher(alias).matches())
		    return true;
	    }

	return false;
    }

    public boolean matches(Object traceable) {
	TracePoint tp = (TracePoint)traceable;
	return checkNameMatches(tp)
	    && checkVersionMatches(tp);
    }
}

class MyFtraceController
    implements Ftrace.Controller,
	       Ftrace.StackTracedSymbolsProvider,
	       Ftrace.TracedSyscallProvider,
	       Ftrace.TracedSignalProvider
{
    protected static final Logger logger = Logger.getLogger("frysk");

    // ArrayList<SymbolRule>
    private final List pltRules = new ArrayList();
    private final List dynRules = new ArrayList();
    private final List symRules = new ArrayList();
    private final List sysRules = new ArrayList();
    private final List sigRules = new ArrayList();

    // Which symbols should yield a stack trace.
    private HashSet symbolsStackTraceSet = new HashSet();
    private boolean stackTraceEverything = false;

    public void stackTraceEverything() {
	stackTraceEverything = true;
    }

    public boolean shouldStackTraceOnSymbol(Symbol symbol) {
	return stackTraceEverything
	    || symbolsStackTraceSet.contains(symbol);
    }

    public MyFtraceController() { }

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

    public void gotSysRules(List rules) {
	logger.log(Level.FINER, "Got " + rules.size() + " syscall rules.");
	this.sysRules.addAll(rules);
    }

    public void gotSigRules(List rules) {
	logger.log(Level.FINER, "Got " + rules.size() + " signal rules.");
	this.sigRules.addAll(rules);
    }

    private Map computeWorkingSet(Task task, String what,
				 List rules, ArrayList candidates)
    {
	HashSet workingSet = new HashSet();
	HashSet stackTraceSet = new HashSet();

	for (Iterator it = rules.iterator(); it.hasNext(); ) {
	    final Rule rule = (Rule)it.next();
	    logger.log(Level.FINEST, "Considering syscall rule " + rule + ".");
	    rule.apply(logger, candidates, workingSet, stackTraceSet);
	}

	// Apply the two sets.
	Map ret = new HashMap();
	for (Iterator it = workingSet.iterator(); it.hasNext(); ) {
	    Object syscall = it.next();
	    ret.put(syscall, Boolean.valueOf(stackTraceEverything
					     || stackTraceSet.contains(syscall)));
	}
	return ret;
    }

    // Syscall working and stack trace sets can be pre-computed for
    // each task.  This is in contrast to tracing rules, that are
    // computed incrementally when DSOs are mapped.
    public Map computeSyscallWorkingSet(Task task) {
	SyscallTable syscallTable = task.getSyscallTable();
	long n = syscallTable.getNumSyscalls();
	ArrayList candidates = new ArrayList();
	for (long i = 0; i < n; ++i)
	    candidates.add(syscallTable.getSyscall(i));

	return computeWorkingSet(task, "syscall", sysRules, candidates);
    }

    // Compute signal working and stack trace sets.
    public Map computeSignalWorkingSet(Task task) {
	frysk.sys.Signal[] hostSignals
	    = frysk.sys.Signal.getHostSignalSet().toArray();
	SignalTable signalTable = task.getSignalTable();
	ArrayList candidates = new ArrayList();
	for (int i = 0; i < hostSignals.length; i++)
	    candidates.add(signalTable.get(hostSignals[i].intValue()));

	return computeWorkingSet(task, "signal", sigRules, candidates);
    }

    private boolean isInterpOf(ObjectFile objf, String exe)
    {
	java.io.File exefn = new java.io.File(exe);
	ObjectFile exef = ObjectFile.buildFromFile(exefn);
	java.io.File interpfn = exef.resolveInterp();
	java.io.File objffn = objf.getFilename();
	return objffn.equals(interpfn);
    }

    public void applyTracingRules(final Task task, final ObjectFile objf, final Ftrace.Driver driver,
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

	// Do a lazy init.  With symbol tables this can be very beneficial, because certain symbol 
	boolean candidatesInited = false;

	// Loop through all the rules, and use them to build
	// workingSet from candidates.  Candidates are initialized
	// lazily inside the loop.
	for (Iterator it = rules.iterator(); it.hasNext(); ) {
	    final SymbolRule rule = (SymbolRule)it.next();
	    logger.log(Level.FINEST, "Considering symbol rule " + rule + ".");

	    // MAIN is meta-soname meaning "main executable".
	    if ((rule.sonamePattern.pattern().equals("MAIN")
		 && task.getProc().getExe().equals(objf.getFilename().getPath()))
		|| (rule.sonamePattern.pattern().equals("INTERP")
		    && isInterpOf(objf, task.getProc().getExe()))
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

		rule.apply(logger, candidates, workingSet, stackTraceSet);
	    }
	}

	// Finally, apply constructed working set.
	logger.log(Level.FINER, "Applying working set for origin " + origin + ".");
	for (Iterator it = workingSet.iterator(); it.hasNext(); )
	    driver.tracePoint(task, (TracePoint)it.next());

	for (Iterator it = stackTraceSet.iterator(); it.hasNext(); )
	    symbolsStackTraceSet.add(((TracePoint)it.next()).symbol);
    }

    public void fileMapped(final Task task, final ObjectFile objf, final Ftrace.Driver driver) {
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

class ftrace
{
    //Where to send the output.
    PrintWriter writer;

    protected static final Logger logger = Logger.getLogger("frysk");

    // True if a PID was requested.
    boolean requestedPid;
    // Command and arguments to exec.
    ArrayList commandAndArguments;

    // For configuration of overall working set.  We need to load and
    // apply rules separately, to get all log messages, that's the
    // reason we need these temporary array lists.
    final List pltRules = new ArrayList();
    final List dynRules = new ArrayList();
    final List symRules = new ArrayList();
    final List sysRules = new ArrayList();
    final List sigRules = new ArrayList();
    final MyFtraceController controller = new MyFtraceController();
    boolean allowInterpTracing = false;

    Ftrace tracer = new Ftrace();

    private List parseSymbolRules(String arg) {
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
	    SymbolRule rule = new SymbolRule(addition, stackTrace, symbolRe, sonameRe, versionRe);
	    rules.add(rule);
	}
	return rules;
    }

    private static interface TraceableExaminer {
	int traceableNumber(Object traceable);
	String traceableName(Object traceable);
    }

    private List parseSigSysRules(String arg, final TraceableExaminer examiner,
				  String optionalPrefix)
    {
	String[] strs = arg.split(",", -1);
	Pattern sysnumPat = Pattern.compile("[0-9]+");
	List rules = new ArrayList();
	for (int i = 0; i < strs.length; ++i) {
	    // "14": traceable number 14
	    // "foo*": traceable whose name matches glob
	    // "": wildcard matching all traceables
	    String str = strs[i];
	    final Rule rule;
	    final boolean addition;
	    final boolean stackTrace;

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

	    if (sysnumPat.matcher(str).matches()) {
		logger.log(Level.FINE, i + ": " + str + ": by number rule");
		final int number = (new Integer(str)).intValue();
		rule = new Rule(addition, stackTrace) {
			public boolean matches(final Object traceable) {
			    return number == examiner.traceableNumber(traceable);
			}
		    };
	    }
	    else if (!str.equals("")) {
		logger.log(Level.FINE, i + ": " + str + ": by name rule");
		str = str.toLowerCase();
		if (optionalPrefix != null && !str.startsWith(optionalPrefix))
		    str = optionalPrefix + str;
		final Pattern pattern = Glob.compile(str, Pattern.CASE_INSENSITIVE);
		rule = new Rule(addition, stackTrace) {
			public boolean matches(final Object traceable) {
			    String name = examiner.traceableName(traceable);
			    return pattern.matcher(name).matches();
			}
		    };
	    }
	    else {
		logger.log(Level.FINE, i + ": " + str + ": \"everything\" rule");
		rule = new Rule(addition, stackTrace) {
			public boolean matches(Object traceable) {
			    return true;
			}
		    };
	    }

	    rules.add(rule);
	}
	return rules;
    }

    private void addOptions(CommandlineParser parser)
    {
        parser.add(new Option('o', "output file name", "FILE") {
            public void parsed(String filename) throws OptionException
            {
                // FIXME: strace supports '|' and '!' here for piping.
                try {
                    writer = new PrintWriter(new FileOutputStream(filename));
                } catch (FileNotFoundException fnfe) {
                    OptionException oe = new OptionException(fnfe.getMessage());
                    oe.initCause(fnfe);
                    throw oe;
                }
            }
        });

        parser.add(new Option('c', "trace children as well") {
            public void parsed(String arg0) throws OptionException
            {
                tracer.setTraceChildren();
            }
        });

	parser.add(new Option('p', "pid to trace", "PID") {
            public void parsed(String arg) throws OptionException {
                try {
		    Proc proc = Util.getProcFromPid(Integer.parseInt(arg));
		    tracer.addProc(proc);
		    requestedPid = true;
                } catch (NumberFormatException e) {
                    OptionException oe = new OptionException("couldn't parse pid: " + arg);
                    oe.initCause(e);
                    throw oe;
                }
            }
        });

        parser.add(new Option('m', "print out when library is mapped or unmapped") {
          public void parsed(String arg) throws OptionException
          {
	      tracer.setTraceMmaps();
          }
        });

        parser.add(new Option('i', "don't trace dynamic linker symbols") {
          public void parsed(String arg) throws OptionException
          {
	      allowInterpTracing = true;
          }
        });

        parser.add(new Option("sig", "trace signals", "SIG[,SIG]...") {
		public void parsed(String arg) throws OptionException
		{
		    sigRules.add(arg);
		}
        });

        parser.add(new Option("sys", "trace system calls", "CALL[,CALL]...") {
		public void parsed(String arg) throws OptionException
		{
		    sysRules.add(arg);
		}
        });

	parser.add(new Option("plt", "trace library calls done via PLT", "RULE[,RULE]...") {
		public void parsed(String arg) {
		    pltRules.add(arg);
		}
	});

	parser.add(new Option("dyn", "trace entry points from DYNAMIC symtab", "RULE[,RULE]...") {
		public void parsed(String arg) {
		    dynRules.add(arg);
		}
	});

	parser.add(new Option("sym", "trace entry points from symbol table", "RULE[,RULE]...") {
		public void parsed(String arg) {
		    symRules.add(arg);
		}
	});

	parser.add(new Option("stack", "stack trace on every traced entity") {
		public void parsed(String arg) {
		    controller.stackTraceEverything();
		}
	});
    }

    public void run(String[] args)
    {
        CommandlineParser parser = new CommandlineParser("ftrace") {
            protected void validate() throws OptionException {
                if (! requestedPid && commandAndArguments == null)
                    throw new OptionException("no command or PID specified");
            }

		//@Override
		public void parseCommand(Proc command) {
		    String[] line = command.getCmdLine();
		    commandAndArguments = new ArrayList();
		    for (int i = 0; i < line.length; i++)
			commandAndArguments.add(line[i]);
		}

            //@Override
            public void parsePids(Proc[] procs) {
		for (int i = 0; i < procs.length; ++i)
		    tracer.addProc(procs[i]);
		requestedPid = true;
            }
        };
        addOptions(parser);
        parser.setHeader("usage: ftrace [OPTIONS] COMMAND ARGS...");

        parser.parse(args);
        if (writer == null)
            writer = new PrintWriter(System.out);
        tracer.setWriter(writer);

	if (!pltRules.isEmpty() || !dynRules.isEmpty() || !symRules.isEmpty()) {
	    // If tracing dynamic linker disabled, generate implicit
	    // -@INTERP rule at the end of the chain.
	    if (!allowInterpTracing) {
		pltRules.add("-@INTERP");
		dynRules.add("-@INTERP");
		symRules.add("-@INTERP");
	    }

	    for (Iterator it = pltRules.iterator(); it.hasNext(); )
		controller.gotPltRules(parseSymbolRules((String)it.next()));
	    for (Iterator it = dynRules.iterator(); it.hasNext(); )
		controller.gotDynRules(parseSymbolRules((String)it.next()));
	    for (Iterator it = symRules.iterator(); it.hasNext(); )
		controller.gotSymRules(parseSymbolRules((String)it.next()));

	    tracer.setTraceFunctions(controller, controller);
	}

	if (!sysRules.isEmpty()) {
	    TraceableExaminer syscallExaminer = new TraceableExaminer() {
		    public int traceableNumber(Object traceable) {
			return ((Syscall)traceable).getNumber();
		    }
		    public String traceableName(Object traceable) {
			return ((Syscall)traceable).getName();
		    }
		};
	    for (Iterator it = sysRules.iterator(); it.hasNext(); )
		controller.gotSysRules(parseSigSysRules((String)it.next(),
							syscallExaminer, null));
	    tracer.setTraceSyscalls(controller);
	}

	if (!sigRules.isEmpty()) {
	    TraceableExaminer signalExaminer = new TraceableExaminer() {
		    public int traceableNumber(Object traceable) {
			return ((Signal)traceable).intValue();
		    }
		    public String traceableName(Object traceable) {
			return ((Signal)traceable).getName();
		    }
		};
	    for (Iterator it = sigRules.iterator(); it.hasNext(); )
		controller.gotSigRules(parseSigSysRules((String)it.next(),
							signalExaminer, "sig"));
	    tracer.setTraceSignals(controller);
	}

        if (commandAndArguments != null) {
            String[] cmd = (String[]) commandAndArguments.toArray(new String[0]);
            tracer.trace(cmd);
        }
        else
            tracer.trace();
    }

    public ftrace()
    {
    }

    public static void main(String[] args)
    {
        ftrace m = new ftrace();
        m.run(args);
    }
}
