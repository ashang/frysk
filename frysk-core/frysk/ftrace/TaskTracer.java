// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import frysk.dwfl.ObjectFile;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.rsl.Log;
import frysk.rsl.LogFactory;
import frysk.rt.BreakpointManager;
import frysk.rt.SourceBreakpoint;
import frysk.rt.SourceBreakpointObserver;
import frysk.rt.SymbolBreakpoint;
import frysk.stack.StackFactory;
import frysk.symtab.DwflSymbol;

import lib.dwfl.DwflModule;

/**
 * Package private implementation of Ftrace.Driver interface.
 */

class TaskTracer
    implements Ftrace.Driver
{
    static private final Log fine = LogFactory.fine(TaskTracer.class);
    static private final Log warning = LogFactory.warning(TaskTracer.class);

    // Map<Long(address), FunctionReturnObserver>
    private Map functionReturnObservers = new HashMap();
    final Arch arch;

    // Our umbilical cord.
    private final Ftrace ftrace;

    public TaskTracer(Ftrace ftrace, Task task) {
	this.arch = ArchFactory.instance.getArch(task);
	this.ftrace = ftrace;
    }

    private class FunctionReturnObserver
	implements TaskObserver.Code
    {
	private final LinkedList symbolList = new LinkedList();

	public FunctionReturnObserver(Task task, long address) {
	    task.requestAddCodeObserver(this, address);
	}

	public void add(DwflSymbol symbol) {
	    symbolList.addLast(symbol);
	}

	public Action updateHit (Task task, long address)
	{
	    fine.log("Return breakpoint at 0x" + Long.toHexString(address));

	    DwflSymbol symbol = (DwflSymbol)symbolList.removeLast();
	    Action action = Action.CONTINUE;

	    // Retract lowlevel breakpoint when the last return has
	    // been hit.
	    if (symbolList.isEmpty()) {
		fine.log("Removing leave breakpoint.");
		functionReturnObservers.remove(new Long(address));
		task.requestDeleteCodeObserver(this, address);

		// Take time to retract
		task.requestUnblock(this);
		action = Action.BLOCK;
	    }

	    fine.log("Fetching retval.");
	    Object ret = arch.getReturnValue(task);
	    ftrace.reporter.eventLeave(task, symbol,
				"leave", symbol.getName(), ret);
	    fine.log("Breakpoint handled.");

	    return action;
	}

	public void addedTo (final Object observable) {}
	public void deletedFrom (Object observable) {}
	public void addFailed (final Object observable, final Throwable w) {}
    }

    private class FunctionEnterObserver
	implements SourceBreakpointObserver
    {
	private final DwflSymbol sym;
	public FunctionEnterObserver(DwflSymbol sym) {
	    this.sym = sym;
	}

	private long getReturnAddress(Task task) {
	    try {
		return StackFactory.createFrame(task).getOuter().getAddress();
	    } catch (java.lang.NullPointerException npe) {
		return 0;
	    }
	}

    	public void updateHit(SourceBreakpoint breakpoint, Task task, long address) {
	    if (sym.getAddress() != address)
		warning.log("Breakpoint requested for", sym.getAddress(),
			    "hits at", address, "for symbol", sym);

	    long retAddress = getReturnAddress(task);
	    SymbolBreakpoint symbolBreakpoint = (SymbolBreakpoint)breakpoint;
	    String symbolName = symbolBreakpoint.getName();
	    String eventName = "#" + getLibraryName(sym) + "#" + symbolName;
	    DwflSymbol symbol = symbolBreakpoint.getSymbol();

	    if (retAddress == 0)
		ftrace.reporter.eventSingle(task, "call " + eventName, arch.getCallArguments(task));
	    else {
		ftrace.reporter.eventEntry(task, symbol, "call", eventName, arch.getCallArguments(task));

		Long retAddressL = new Long(retAddress);
		FunctionReturnObserver retObserver
		    = (FunctionReturnObserver)functionReturnObservers.get(retAddressL);
		if (retObserver == null) {
		    retObserver = new FunctionReturnObserver(task, retAddress);
		    functionReturnObservers.put(retAddressL, retObserver);
		}
		retObserver.add(symbol);
	    }

	    // If this symbol is in the stack tracing set, get a
	    // stack trace before continuing on.
	    if (ftrace.stackTraceSetProvider.shouldStackTraceOnSymbol(sym))
		ftrace.reporter.generateStackTrace(task);

	    // And on we go...
	    Ftrace.steppingEngine.continueExecution(task);
	}

	public void addedTo (Object observable) {}
	public void addFailed (Object observable, Throwable w) {}
	public void deletedFrom (Object observable) {}
    }

    private String getLibraryName(DwflSymbol sym) {
	DwflModule module = sym.getModule();
	if (module != null) {
	    String path = sym.getModule().getName();
	    ObjectFile of = ObjectFile.buildFromFile(path);
	    if (of != null)
		return of.getSoname();
	    else
		return path;
	} else
	    return "???";
    }

    private final Set alreadyTracing = new HashSet();
    public void traceSymbol(Task task, DwflSymbol sym)
    {
	if (alreadyTracing.contains(sym))
	    return;

	if (sym.isFunctionSymbol() && sym.getAddress() != 0) {
	    alreadyTracing.add(sym);
	    BreakpointManager bpManager = Ftrace.steppingEngine.getBreakpointManager();
	    final SymbolBreakpoint bp = bpManager.addSymbolBreakpoint(sym);
	    bp.addObserver(new FunctionEnterObserver(sym));
	    bpManager.enableBreakpoint(bp, task);
	}
    }
}
