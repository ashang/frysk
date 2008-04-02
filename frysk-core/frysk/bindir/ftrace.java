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

import frysk.debuginfo.PrintStackOptions;
import frysk.util.StackPrintUtil;
import inua.util.PrintWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import frysk.ftrace.Ftrace;
import frysk.isa.signals.Signal;
import frysk.isa.syscalls.Syscall;
import frysk.proc.Proc;
import frysk.util.CommandlineParser;
import frysk.util.Glob;
import frysk.util.Util;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionGroup;
import gnu.classpath.tools.getopt.OptionException;
import frysk.ftrace.Rule;
import frysk.ftrace.SymbolRule;
import frysk.ftrace.FtraceController;
import frysk.rsl.Log;

class ftrace {
    private static final Log fine = Log.fine(ftrace.class);

    //Where to send the output.
    private PrintWriter writer;

    // True if a PID was requested.
    private boolean requestedPid;
    // Command and arguments to exec.
    private Proc commandAndArguments;

    // For configuration of overall working set.  We need to load and
    // apply rules separately, to get all log messages, that's the
    // reason we need these temporary array lists.
    private final List pltRules = new ArrayList();
    private final List symRules = new ArrayList();
    private final List sysRules = new ArrayList();
    private final List sigRules = new ArrayList();
    private final FtraceController controller = new FtraceController();
    private boolean allowInterpTracing = false;

    private final PrintStackOptions stackPrintOptions
	= new PrintStackOptions();
    private final Ftrace tracer = new Ftrace(stackPrintOptions);

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

	    fine.log(i + ": " + str + ": symbol=" + symbolRe + ", soname=" + sonameRe + ", version=" + versionRe);
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

	    final String ruleKern = new String(str);
	    abstract class SigSysRule extends Rule {
		public SigSysRule(boolean addition, boolean stackTrace) {
		    super(addition, stackTrace);
		}
		public String toString() {
		    return super.toString() + ruleKern;
		}
	    }

	    if (sysnumPat.matcher(str).matches()) {
		fine.log(i + ": " + str + ": by number rule");
		final int number = (new Integer(str)).intValue();
		rule = new SigSysRule(addition, stackTrace) {
			public boolean matches(final Object traceable) {
			    return number == examiner.traceableNumber(traceable);
			}
		    };
	    }
	    else if (!str.equals("")) {
		fine.log(i + ": " + str + ": by name rule");
		str = str.toLowerCase();
		if (optionalPrefix != null && !str.startsWith(optionalPrefix))
		    str = optionalPrefix + str;
		final Pattern pattern = Glob.compile(str, Pattern.CASE_INSENSITIVE);
		rule = new SigSysRule(addition, stackTrace) {
			public boolean matches(final Object traceable) {
			    String name = examiner.traceableName(traceable);
			    return pattern.matcher(name).matches();
			}
		    };
	    }
	    else {
		fine.log(i + ": " + str + ": \"everything\" rule");
		rule = new SigSysRule(addition, stackTrace) {
			public boolean matches(Object traceable) {
			    return true;
			}
		    };
	    }

	    rules.add(rule);
	}
	return rules;
    }

    private OptionGroup[] options() {
	OptionGroup group = new OptionGroup("ftrace options");
        group.add(new Option('o', "output file name", "FILE") {
		public void parsed(String filename) throws OptionException {
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
        group.add(new Option("follow", 'f', "follow children as well") {
		public void parsed(String arg0) throws OptionException {
		    tracer.setTraceChildren();
		}
	    });
	group.add(new Option('p', "pid to trace", "PID") {
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
        group.add(new Option('m', "print out when library is mapped or unmapped") {
		public void parsed(String arg) throws OptionException {
		    tracer.setTraceMmaps();
		}
	    });
        group.add(new Option("pc", "show program counter at traced events") {
		public void parsed(String arg) throws OptionException {
		    tracer.setShowPC(true);
		}
	    });
        group.add(new Option("dl", "allow tracing of dynamic linker symbols") {
		public void parsed(String arg) throws OptionException {
		    allowInterpTracing = true;
		}
	    });
        group.add(new Option("sig", "trace signals", "SIG[,SIG]...") {
		public void parsed(String arg) throws OptionException {
		    sigRules.add(arg);
		}
	    });
        group.add(new Option("sys", "trace system calls", "CALL[,CALL]...") {
		public void parsed(String arg) throws OptionException {
		    sysRules.add(arg);
		}
	    });
	group.add(new Option("plt", "trace library calls done via PLT", "RULE[,RULE]...") {
		public void parsed(String arg) {
		    pltRules.add(arg);
		}
	    });
	group.add(new Option("sym", "trace function entry points", "RULE[,RULE]...") {
		public void parsed(String arg) {
		    symRules.add(arg);
		}
	    });
	group.add(new Option("stack", "stack trace on every traced entity") {
		public void parsed(String arg) {
		    controller.stackTraceEverything();
		}
	    });
	return new OptionGroup[] {
	    group,
	    StackPrintUtil.options(stackPrintOptions)
	};
    }

    public void run(String[] args) {
        CommandlineParser parser = new CommandlineParser("ftrace", options()) {
		protected void validate() throws OptionException {
		    if (! requestedPid && commandAndArguments == null)
			throw new OptionException("no command or PID specified");
		}

		//@Override
		public void parseCommand(Proc command) {
		    commandAndArguments = command;
		}

            //@Override
            public void parsePids(Proc[] procs) {
		for (int i = 0; i < procs.length; ++i)
		    tracer.addProc(procs[i]);
		requestedPid = true;
            }
        };
        parser.setHeader("usage: ftrace [OPTIONS] COMMAND ARGS...");

        parser.parse(args);
        if (writer == null)
            writer = new PrintWriter(System.out);
        tracer.setWriter(writer);

	if (!pltRules.isEmpty() || !symRules.isEmpty()) {
	    // If tracing dynamic linker disabled, generate implicit
	    // -@INTERP rule at the end of the chain.
	    if (!allowInterpTracing) {
		pltRules.add("-@INTERP");
		symRules.add("-@INTERP");
	    }

	    for (Iterator it = pltRules.iterator(); it.hasNext(); )
		controller.gotPltRules(parseSymbolRules((String)it.next()));
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
            tracer.trace(commandAndArguments);
        } else {
            tracer.trace();
	}
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
