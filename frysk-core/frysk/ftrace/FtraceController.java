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

import frysk.dwfl.ObjectFile;
import frysk.isa.signals.SignalTable;
import frysk.isa.syscalls.SyscallTable;
import frysk.proc.Task;
import frysk.rsl.Log;
import frysk.rsl.LogFactory;
import frysk.symtab.DwflSymbol;
import frysk.symtab.PLTEntry;

public class FtraceController
    implements Ftrace.Controller,
	       Ftrace.StackTracedSymbolsProvider,
	       Ftrace.TracedSyscallProvider,
	       Ftrace.TracedSignalProvider
{
    private static final Log warning = LogFactory.warning(FtraceController.class);
    private static final Log fine = LogFactory.fine(FtraceController.class);

    // ArrayList<SymbolRule>
    private final List pltRules = new ArrayList();
    private final List symRules = new ArrayList();
    private final List sysRules = new ArrayList();
    private final List sigRules = new ArrayList();

    // Which symbols should yield a stack trace.
    private HashSet tracePointStackTraceSet = new HashSet();
    private boolean stackTraceEverything = false;

    public void stackTraceEverything() {
	stackTraceEverything = true;
    }

    public boolean shouldStackTraceOnTracePoint(Object tracePoint) {
	return stackTraceEverything
	    || tracePointStackTraceSet.contains(tracePoint);
    }

    public FtraceController() { }

    public void gotPltRules(List rules) {
	fine.log("Got " + rules.size() + " PLT rules.");
	this.pltRules.addAll(rules);
    }

    public void gotSymRules(List rules) {
	fine.log("Got " + rules.size() + " symbol rules.");
	this.symRules.addAll(rules);
    }

    public void gotSysRules(List rules) {
	fine.log("Got " + rules.size() + " syscall rules.");
	this.sysRules.addAll(rules);
    }

    public void gotSigRules(List rules) {
	fine.log("Got " + rules.size() + " signal rules.");
	this.sigRules.addAll(rules);
    }

    private Map computeWorkingSet(Task task, String what,
				  List rules, ArrayList candidates)
    {
	HashSet workingSet = new HashSet();
	HashSet stackTraceSet = new HashSet();

	for (Iterator it = rules.iterator(); it.hasNext(); ) {
	    final Rule rule = (Rule)it.next();
	    fine.log("Considering " + what + " rule `" + rule + "'.");
	    if (!rule.apply(candidates, workingSet, stackTraceSet))
		warning.log("Rule", rule, "didn't match any", what);
	}

	// Apply the two sets.
	Map ret = new HashMap();
	for (Iterator it = workingSet.iterator(); it.hasNext(); ) {
	    Object sysOrSig = it.next();
	    ret.put(sysOrSig,
		    Boolean.valueOf(stackTraceEverything
				    || stackTraceSet.contains(sysOrSig)));
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
	ObjectFile exef = ObjectFile.buildFromFile(exe);
	java.io.File interpfn = exef.resolveInterp();
	java.io.File objffn = objf.getFilename();
	return objffn.equals(interpfn);
    }

    interface RuleHandler {
	void applyTracePoint(Object tracePoint);
    }

    private void applyTracingRules(final Task task, final ObjectFile objf,
				   final List tracePoints, final List rules,
				   RuleHandler handler)
	throws lib.dwfl.ElfException
    {
	String path = objf.getFilename().getPath();
	fine.log("Building working set for task", task, "and path", path);

	// Skip the set if it's empty...
	if (rules.isEmpty())
	    return;

	// Set<DwflSymbol>, incrementally built working set.
	final Set workingSet = new HashSet();
	// Set<DwflSymbol>, incrementally built set of tracepoints
	// that should stacktrace.
	final Set stackTraceSet = new HashSet();

	// Loop through all the rules, and use them to build
	// workingSet from candidates.  Candidates are initialized
	// lazily inside the loop.
	for (Iterator it = rules.iterator(); it.hasNext(); ) {
	    final SymbolRule rule = (SymbolRule)it.next();
	    fine.log("Considering symbol rule " + rule + ".");

	    // MAIN is meta-soname meaning "main executable".
	    if ((rule.sonamePattern.pattern().equals("MAIN")
		 && task.getProc().getExeFile().getSysRootedPath().equals(path))
		|| (rule.sonamePattern.pattern().equals("INTERP")
		    && isInterpOf(objf, task.getProc().getExeFile().getSysRootedPath()))
		|| rule.sonamePattern.matcher(objf.getSoname()).matches())
	    {
		rule.apply(tracePoints, workingSet, stackTraceSet);
	    }
	}

	// Finally, apply constructed working set.
	fine.log("Applying working set for ", path);
	tracePointStackTraceSet.addAll(stackTraceSet);
	for (Iterator it = workingSet.iterator(); it.hasNext(); )
	    handler.applyTracePoint(it.next());
    }

    public void fileMapped(final Task task, ObjectFile objf,
			   List symbols, List pltEntries,
			   final Ftrace.Driver driver) {
	try {
	    applyTracingRules
		(task, objf, pltEntries, pltRules,
		 new RuleHandler() {
		     public void applyTracePoint(Object tracePoint) {
			 PLTEntry entry = (PLTEntry)tracePoint;
			 driver.tracePLTEntry(task, entry);
		     }
		 });

	    applyTracingRules
		(task, objf, symbols, symRules,
		 new RuleHandler() {
		     public void applyTracePoint(Object tracePoint) {
			 DwflSymbol symbol = (DwflSymbol)tracePoint;
			 driver.traceSymbol(task, symbol);
		     }
		 });
	}
	catch (lib.dwfl.ElfException ee) {
	    ee.printStackTrace();
	}
    }
}
