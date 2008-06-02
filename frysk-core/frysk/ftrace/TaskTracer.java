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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import frysk.dwfl.ObjectFile;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.rsl.Log;
import frysk.rsl.LogFactory;
import frysk.rt.BreakpointManager;
import frysk.rt.PLTBreakpoint;
import frysk.rt.SourceBreakpoint;
import frysk.rt.SourceBreakpointObserver;
import frysk.stack.StackFactory;
import frysk.symtab.DwflSymbol;
import frysk.symtab.PLTEntry;
import frysk.util.ArchFormatter;

import lib.dwfl.DwflModule;

/**
 * Package private implementation of Ftrace.Driver interface.
 */

class TaskTracer
    implements Ftrace.Driver
{
    static private final Log fine = LogFactory.fine(TaskTracer.class);
    static private final Log finest = LogFactory.finest(TaskTracer.class);
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

    private class TracePoint
    {
	private final DwflSymbol symbol;
	private final boolean isPlt;
	private boolean chained = false;
	private boolean frozen = false;

	public TracePoint(DwflSymbol symbol) {
	    this.symbol = symbol;
	    this.isPlt = false;
	}

	public TracePoint(PLTEntry entry) {
	    this.symbol = entry.getSymbol();
	    this.isPlt = true;
	}

	public DwflSymbol getSymbol() {
	    return this.symbol;
	}

	public String getName() {
	    return this.symbol.getName();
	}

	public void freeze() {
	    this.frozen = true;
	}

	public boolean isFrozen() {
	    return this.frozen;
	}

	public void setChained() {
	    this.chained = true;
	}

	public boolean isChained() {
	    return this.chained;
	}

	public boolean isPlt() {
	    return this.isPlt;
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

	public String toString() {
	    String symbolName = getName();
	    String eventDescription = "#" + getLibraryName(this.symbol)
		+ "#" + (isPlt ? "plt:" : "") + symbolName;
	    return eventDescription;
	}
    }

    private class FunctionReturnObserver
	implements TaskObserver.Code
    {
	private final LinkedList symbolList = new LinkedList();
	final long address;

	public FunctionReturnObserver(Task task, long address) {
	    fine.log("New FunctionReturnObserver", address);
	    task.requestAddCodeObserver(this, address);
	    this.address = address;
	}

	public void add(TracePoint tracePoint) {
	    fine.log("Adding to FunctionReturnObserver", tracePoint.getSymbol(), "at", this.address);
	    if (!symbolList.isEmpty()) {
		TracePoint previous = (TracePoint)symbolList.getLast();
		if (!previous.isFrozen()
		    && previous.isPlt() && !tracePoint.isPlt()
		    && previous.getSymbol() == tracePoint.getSymbol())
		    tracePoint.setChained();

		previous.freeze();
	    }
	    symbolList.addLast(tracePoint);
	}

	private Action handleReturn(Task task, TracePoint tracePoint,
				    long address)
	{
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
	    ftrace.reporter.eventLeave(task, tracePoint, "leave",
				       "" + tracePoint, ret);
	    fine.log("Breakpoint handled.");
	    return action;
	}

	public Action updateHit(Task task, long address)
	{
	    fine.log("Return breakpoint at 0x" + Long.toHexString(address));

	    TracePoint tracePoint = (TracePoint)symbolList.removeLast();
	    Action action = handleReturn(task, tracePoint, address);

	    if (tracePoint.isChained()) {
		tracePoint = (TracePoint)symbolList.getLast();
		action = handleReturn(task, tracePoint, address);
	    }

	    return action;
	}

	public void addedTo (final Object observable) {}
	public void deletedFrom (Object observable) {}
	public void addFailed (final Object observable, final Throwable w) {}
    }

    private class FunctionEnterObserver
	implements SourceBreakpointObserver
    {
	private DwflSymbol sym = null;
	private final boolean isPlt;
	private SourceBreakpoint observing = null;
	private boolean invalid = false;

	public FunctionEnterObserver() {
	    this.isPlt = false;
	}

	public FunctionEnterObserver(PLTEntry entry) {
	    addSymbol(entry.getSymbol());
	    this.isPlt = true;
	}

	// Frysk doesn't get rid of disabled breakpoints, and turns
	// them back on when _dl_debug_state observer triggers.  In
	// invalidated state, the observer turns the breakpoint back
	// off everytime it's hit.
	public void invalidate() {
	    invalid = true;
	}

	public void setObserving(SourceBreakpoint breakpoint) {
	    this.observing = breakpoint;
	}

	public SourceBreakpoint getObserving() {
	    return this.observing;
	}

	public void addSymbol(DwflSymbol symbol) {
	    if (sym != null
		&& sym.getAddress() != symbol.getAddress())
		warning.log("Two non-aliasing symbols in one observer:",
			    sym, "and", symbol);

	    if (sym == null
		|| sym.getName().length() > symbol.getName().length())
		sym = symbol;
	}

	private long getReturnAddress(Task task) {
	    try {
		return StackFactory.createFrame(task).getOuter().getAddress();
	    } catch (java.lang.NullPointerException npe) {
		return 0;
	    }
	}

    	public void updateHit(SourceBreakpoint breakpoint, Task task, long address) {

	    if (invalid) {
		Ftrace.steppingEngine.continueExecution(task);
		return;
	    }

	    if (!isPlt
		&& (address < sym.getAddress()
		    // Some symbols are reported with size 0, and for
		    // these we want to allow addr == getAddr + getSize
		    || address > sym.getAddress() + sym.getSize()))
		warning.log("Breakpoint requested for", sym.getAddress(),
			    "hits at", address, "for symbol", sym);

	    long retAddress = getReturnAddress(task);
	    TracePoint tracePoint;
	    Object token;

	    if (isPlt) {
		PLTEntry entry = ((PLTBreakpoint)breakpoint).getEntry();
		token = entry;
		tracePoint = new TracePoint(entry);
	    } else {
		token = sym;
		tracePoint = new TracePoint(sym);
	    }

	    String eventName = "" + tracePoint;
	    if (retAddress == 0)
		ftrace.reporter.eventSingle(task, "call " + eventName,
					    arch.getCallArguments(task));
	    else {
		ftrace.reporter.eventEntry(task, tracePoint, "call",
					   eventName, arch.getCallArguments(task));

		Long retAddressL = new Long(retAddress);
		FunctionReturnObserver retObserver
		    = (FunctionReturnObserver)functionReturnObservers.get(retAddressL);
		if (retObserver == null) {
		    retObserver = new FunctionReturnObserver(task, retAddress);
		    functionReturnObservers.put(retAddressL, retObserver);
		}
		retObserver.add(tracePoint);
	    }

	    // If this symbol is in the stack tracing set, get a
	    // stack trace before continuing on.
	    if (ftrace.stackTraceSetProvider.shouldStackTraceOnTracePoint(token))
		ftrace.reporter.generateStackTrace(task);

	    // And on we go...
	    Ftrace.steppingEngine.continueExecution(task);
	}

	public void addedTo (Object observable) {}
	public void addFailed (Object observable, Throwable w) {}
	public void deletedFrom (Object observable) {}
    }

    private class AddressObserver implements TaskObserver.Code {

	private final ObjectFile objf;
	private final Long token;

	public AddressObserver(Long token, ObjectFile objf) {
	    this.token = token;
	    this.objf = objf;
	}

	public Action updateHit(Task task, long address) {
	    fine.log("Address observer hit at 0x" + Long.toHexString(address));
	    String addrFmt = ArchFormatter.toHexString(task, token.longValue());
	    String event = "checkpoint #" + objf.getSoname() + '#' + addrFmt;

	    ftrace.reporter.eventSingle(task, event);

	    if (ftrace.stackTraceSetProvider.shouldStackTraceOnTracePoint(token))
		ftrace.reporter.generateStackTrace(task);

	    return Action.CONTINUE;
	}

	public void addedTo (final Object observable) {}
	public void deletedFrom (Object observable) {}
	public void addFailed (final Object observable, final Throwable w) {}
    }

    // Map<Task, Map<address, FunctionEnterObserver>>
    private final Map symbolObserversForTask = new HashMap();

    private Long getAddress(DwflSymbol sym, PLTEntry entry) {
	long addr = entry != null ? entry.getAddress() : sym.getAddress();
	Long addrL = new Long(addr);
	return addrL;
    }

    private synchronized FunctionEnterObserver getObserver(Task task,
							   DwflSymbol sym,
							   PLTEntry entry) {

	Map symbolObservers = (Map)symbolObserversForTask.get(task);
	if (symbolObservers == null) {
	    symbolObservers = new HashMap();
	    symbolObserversForTask.put(task, symbolObservers);
	}

	Long addrL = getAddress(sym, entry);
	FunctionEnterObserver ob
	    = (FunctionEnterObserver)symbolObservers.get(addrL);
	if (ob == null) {
	    finest.log("New function observer at", sym.getAddress());

	    if (entry != null)
		ob = new FunctionEnterObserver(entry);
	    else
		ob = new FunctionEnterObserver();

	    symbolObservers.put(addrL, ob);

	    BreakpointManager bpManager
		= Ftrace.steppingEngine.getBreakpointManager();
	    final SourceBreakpoint bp;
	    if (entry != null)
		bp = bpManager.addPLTBreakpoint(entry);
	    else
		bp = bpManager.addSymbolBreakpoint(sym);
	    bp.addObserver(ob);
	    ob.setObserving(bp);
	    bpManager.enableBreakpoint(bp, task);
	}
	return ob;
    }

    public void traceSymbol(Task task, DwflSymbol sym) {

	long addr = sym.getAddress();
	if (!sym.isFunctionSymbol() || !sym.isDefined() || addr == 0) {
	    finest.log("Ignoring request for tracing undefined "
		       + "or non-functional symbol", sym);
	    return;
	}

	FunctionEnterObserver ob = getObserver(task, sym, null);
	ob.addSymbol(sym);
	fine.log("Request for tracing symbol", sym, "at", sym.getAddress());
    }

    public void tracePLTEntry(Task task, PLTEntry entry) {

	FunctionEnterObserver ob = getObserver(task, entry.getSymbol(), entry);
	ob.addSymbol(entry.getSymbol());
	fine.log("Request for tracing PLT", entry.getSymbol(),
		 "at", entry.getSymbol().getAddress());
    }

    public void untrace(Task task, List traceables) {

	fine.log("Request to retract multiple traceables:", traceables.size());
	for (Iterator it = traceables.iterator(); it.hasNext(); ) {
	    Object traceable = it.next();

	    final DwflSymbol symbol;
	    final PLTEntry entry;
	    if (traceable instanceof PLTEntry) {
		entry = (PLTEntry)traceable;
		symbol = entry.getSymbol();
	    } else {
		entry = null;
		symbol = (DwflSymbol)traceable;
	    }

	    Map symbolObservers = (Map)symbolObserversForTask.get(task);
	    if (symbolObservers == null)
		// Cool, we are not even tracing it.
		return;

	    Long addrL = getAddress(symbol, entry);
	    Object orig;
	    if ((orig = symbolObservers.remove(addrL)) != null) {
		fine.log("Retracting", symbol, "at", addrL.longValue());
		FunctionEnterObserver ob = (FunctionEnterObserver)orig;
		SourceBreakpoint bp = ob.getObserving();
		BreakpointManager bpManager
		    = Ftrace.steppingEngine.getBreakpointManager();
		bpManager.disableBreakpoint(bp, task);
		ob.invalidate();
	    }
	}
    }

    public void traceAddress(Task task,
			     Long addrToken, long bias, ObjectFile objf)
    {
	long addr = addrToken.longValue();
	fine.log("Request for tracing address", addr,
		 "at", addr+bias);
	task.requestAddCodeObserver(new AddressObserver(addrToken, objf),
				    addr + bias);
    }
}
