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

package frysk.ftrace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import frysk.isa.signals.SignalTable;
import frysk.isa.syscalls.SyscallTable;
import frysk.proc.Task;

public class FtraceController
    implements Ftrace.Controller,
	       Ftrace.StackTracedSymbolsProvider,
	       Ftrace.TracedSyscallProvider,
	       Ftrace.TracedSignalProvider
{
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

    public FtraceController() { }

    public void gotPltRules(List rules) {
	FtraceLogger.fine.log("Got " + rules.size() + " PLT rules.");
	this.pltRules.addAll(rules);
    }

    public void gotDynRules(List rules) {
	FtraceLogger.fine.log("Got " + rules.size() + " DYNAMIC rules.");
	this.dynRules.addAll(rules);
    }

    public void gotSymRules(List rules) {
	FtraceLogger.fine.log("Got " + rules.size() + " SYMTAB rules.");
	this.symRules.addAll(rules);
    }

    public void gotSysRules(List rules) {
	FtraceLogger.fine.log("Got " + rules.size() + " syscall rules.");
	this.sysRules.addAll(rules);
    }

    public void gotSigRules(List rules) {
	FtraceLogger.fine.log("Got " + rules.size() + " signal rules.");
	this.sigRules.addAll(rules);
    }

    private Map computeWorkingSet(Task task, String what,
				  List rules, ArrayList candidates)
    {
	HashSet workingSet = new HashSet();
	HashSet stackTraceSet = new HashSet();

	for (Iterator it = rules.iterator(); it.hasNext(); ) {
	    final Rule rule = (Rule)it.next();
	    FtraceLogger.fine.log("Considering " + what + " rule `" + rule + "'.");
	    if (!rule.apply(candidates, workingSet, stackTraceSet))
		FtraceLogger.warning.log("Rule `" + rule + "' didn't match any " + what + ".");
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
	FtraceLogger.fine.log("Building working set for origin " + origin + ".");

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
	    FtraceLogger.fine.log("Considering symbol rule " + rule + ".");

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
				    FtraceLogger.fine.log("candidate `" + tp.symbol.name + "'.");
			    }
			}, origin);
		}

		rule.apply(candidates, workingSet, stackTraceSet);
	    }
	}

	// Finally, apply constructed working set.
	FtraceLogger.fine.log("Applying working set for origin " + origin + ".");
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
