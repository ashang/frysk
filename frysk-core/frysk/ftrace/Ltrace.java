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

package frysk.ftrace;

import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.*;

public class Ltrace
{
    private static final Logger logger = Logger.getLogger(FtraceLogger.LOGGER_ID);

    // HashMap<Task, Ltrace>
    private static final Map ltraceForTask = new HashMap();

    /**
     * Request that given observer receives enter/leave events of
     * given set of TracePoints.
     */
    public synchronized static
	void requestAddFunctionObserver(Task task,
					FunctionObserver observer,
					Set tracePoints)
    {
	Ltrace ltrace = (Ltrace)ltraceForTask.get(task);
	if (ltrace == null) {
	    ltrace = new Ltrace(task);
	    ltraceForTask.put(task, ltrace);
	}

	ltrace.addObserver(observer, tracePoints);
    }

    public synchronized static
	void requestDeleteFunctionObserver(Task task,
					   FunctionObserver observer,
					   Set tracePoints)
    {
	Ltrace ltrace = (Ltrace)ltraceForTask.get(task);
	if (ltrace == null)
	    observer.addFailed(task, new RuntimeException("This observer does not observe given task."));
	else
	    ltrace.removeObserver(observer, tracePoints);
    }

    // ------------------------ instance part ------------------------

    private final Task task;
    private final Arch arch;

    private interface ObserverIterator {
	Action action(FunctionObserver observer);
    }

    private void eachObserver(Collection observers, ObserverIterator oit) {
	for (Iterator it = observers.iterator(); it.hasNext();) {
	    FunctionObserver fo = (FunctionObserver)it.next();
	    if (oit.action(fo) == Action.BLOCK)
		this.task.blockers.add(fo);
	}
    }

    // Map<Long(address), FunctionReturnObserver>
    private Map functionReturnObservers = new HashMap();

    private class FunctionReturnObserver
	implements TaskObserver.Code
    {
	class Node {
	    final TracePoint tracePoint;
	    final Collection observers;

	    public Node(TracePoint tp, Collection observers) {
		this.tracePoint = tp;
		this.observers = observers;
	    }
	}
	private final LinkedList nodeList = new LinkedList();

	public void add(TracePoint tp, Collection observers) {
	    nodeList.addLast(new Node(tp, observers));
	}

	public Action updateHit (final Task task, long address)
	{
	    logger.log(Level.FINE, "Return breakpoint at 0x" + Long.toHexString(address));

	    final Node leave = (Node)nodeList.removeLast();
	    Action action = Action.CONTINUE;

	    // Retract lowlevel breakpoint when the last return has
	    // been hit.
	    if (nodeList.isEmpty()) {
		logger.log(Level.FINEST, "Removing leave breakpoint.");
		functionReturnObservers.remove(new Long(address));
		task.requestDeleteCodeObserver(this, address);

		// Take time to retract
		task.requestUnblock(this);
		action = Action.BLOCK;
	    }

	    logger.log(Level.FINEST, "Fetching retval.");
	    final Symbol symbol = leave.tracePoint.symbol;
	    final Object ret = arch.getReturnValue(task, symbol);
	    eachObserver(leave.observers, new ObserverIterator() {
		    public Action action(FunctionObserver o) {
			return o.funcallLeave(task, symbol, ret);
		    }
		});

	    logger.log(Level.FINE, "Breakpoint handled.");

	    return action;
	}

	public void addedTo (final Object observable) {}
	public void deletedFrom (Object observable) {}
	public void addFailed (final Object observable, final Throwable w) {}
    }

    /**
     * Objects of this class are created one per tracepoint (and task)
     * to keep track of low-level breakpoint and who to notify when
     * the breakpoint hits.
     */
    private class TracePointObserver
	implements TaskObserver.Code
    {
	final TracePoint tracePoint;
	final ArrayList observers = new ArrayList();
	final Long address;

	// Lowlever breakpoint management.
	boolean breakpointAdded = false;
	boolean breakpointFailed = false;
        Throwable lowlevelObserverThrowable = null;
	/** The observer that should get deletedFrom only after the
	 * breakpoint is removed. */
	FunctionObserver last = null;

	public TracePointObserver(TracePoint tp) {
	    this.tracePoint = tp;
	    this.address = new Long(tp.address);
	}

	public void add(FunctionObserver observer) {
	    observers.add(observer);
	}

	/**
	 * Return != -1 if the same observer is still present in the
	 * `observers' after removal, implying that the observation
	 * was requested multiple times.
	 */
	public int remove(FunctionObserver observer) {
	    // ArrayList is not exactly the optimal container for fast
	    // removal.  But it's fast with the iteration, which is a
	    // MUCH more important operation.
	    last = observer;
	    if (!observers.remove(observer))
		throw new AssertionError("FunctionObserver not found in tracePointObserver.");
	    return observers.indexOf(observer);
	}

	public Action updateHit (final Task task, long address)
	{
	    logger.log(Level.FINE, "Enter breakpoint at 0x" + Long.toHexString(address));

	    if (address != tracePoint.symbol.getParent().getEntryPoint()) {
		// Install breakpoint to return address.
		long retAddr = arch.getReturnAddress(task, tracePoint.symbol);
		logger.log(Level.FINER,
			   "It's enter tracepoint, return address 0x"
			   + Long.toHexString(retAddr) + ".");
		Long retAddrL = new Long(retAddr);
		FunctionReturnObserver retObserver
		    = (FunctionReturnObserver)functionReturnObservers.get(retAddrL);
		if (retObserver == null) {
		    retObserver = new FunctionReturnObserver();
		    task.requestAddCodeObserver(retObserver, retAddr);
		}
		retObserver.add(tracePoint, observers);
	    }
	    else
		logger.log(Level.FINEST,
			   "It's _start, no return breakpoint established...");

	    logger.log(Level.FINEST, "Building arglist.");
	    final Object[] args = arch.getCallArguments(task, tracePoint.symbol);
	    eachObserver(observers, new ObserverIterator() {
		    public Action action(FunctionObserver o) {
			return o.funcallEnter(task, tracePoint.symbol, args);
		    }
		});

	    // Frysk needs time to set up return breakpoint.
	    task.requestUnblock(this);
	    return Action.BLOCK;
	}

	public synchronized void addedTo (final Object observable) {
            if (!breakpointAdded) {
		for (Iterator it = observers.iterator(); it.hasNext(); ) {
		    FunctionObserver fo = (FunctionObserver)it.next();
		    fo.addedTo(observable);
		}
		breakpointAdded = true;
            }
	}

        public synchronized void addFailed(final Object observable, final Throwable w) {
            logger.log(Level.FINE, "lowlevel addFailed!");
            if (!breakpointFailed) {
		for (Iterator it = observers.iterator(); it.hasNext(); ) {
		    FunctionObserver fo = (FunctionObserver)it.next();
		    fo.addFailed(observable, w);
		}

		if (observers.isEmpty()) {
		    // Then it must be the failing requestDelete!
		    if (last == null)
			throw new AssertionError("No last observer set in addFailed!");
		    else
			last.addFailed(observable, w);
		}

		lowlevelObserverThrowable = w;
		breakpointFailed = true;
            }
        }

	public void deletedFrom (Object observable) {
	    if (!observers.isEmpty())
		throw new AssertionError("Observers still present!");
	    if (last == null)
		throw new AssertionError("No last observer set!");
	    last.deletedFrom(observable);
	}
    }

    // WorkingSet ::= Set<TracePointObserver>
    private final HashSet workingSet = new HashSet();

    // Map<FunctionObserver, Set<TracePointObserver>>
    //private final HashMap workingSetForOne = new HashMap();

    // Map<Long(address), TracePointObserver>
    private final HashMap tpObserverForAddress = new HashMap();

    // Map<TracePoint, TracePointObserver>
    private final HashMap tpObserverForTracePoint = new HashMap();

    // ----------------------
    // --- setup/teardown ---
    // ----------------------
    Ltrace(Task task) {
	this.task = task;
	this.arch = ArchFactory.instance.getArch(task);
    }

    public void remove() {
	// XXX: do the right thing.
    }

    // ---------------------------
    // --- observer management ---
    // ---------------------------

    /**
     * Have given observer observe given set of tracepoints.
     */
    private synchronized void addObserver(FunctionObserver observer, Set tracePoints)
    {
	for (Iterator it = tracePoints.iterator(); it.hasNext(); ) {
	    TracePoint tracePoint = (TracePoint)it.next();
	    TracePointObserver tpo = (TracePointObserver)tpObserverForTracePoint.get(tracePoint);
	    if (tpo == null) {
		tpo = new TracePointObserver(tracePoint);
		tpObserverForTracePoint.put(tracePoint, tpo);

		// When the first observer requests this tracepoint, we
		// have to setup a breakpoint.
		if (tpObserverForAddress.put(tpo.address, tpo) != null)
		    throw new AssertionError("Address already occupied with working set element!");

    		task.requestAddCodeObserver(tpo, tracePoint.address);
    	    }

	    tpo.add(observer);

	    workingSet.add(tpo);
	    //workingSetForOne.put(observer, tpo);

	    if (tpo.breakpointAdded)
		observer.addedTo(this.task);
	    else if (tpo.breakpointFailed)
		observer.addFailed(this.task, tpo.lowlevelObserverThrowable);
    	}
    }

    /**
     * Request that given observer stops observing tracepoints in given set.
     */
    private synchronized void removeObserver(FunctionObserver observer, Set tracePoints)
    {
	for (Iterator it = tracePoints.iterator(); it.hasNext(); ) {
	    TracePoint tracePoint = (TracePoint)it.next();
	    TracePointObserver tpo = (TracePointObserver)tpObserverForTracePoint.get(tracePoint);
	    if (tpo == null)
		throw new AssertionError("FunctionObserver doesn't observe the trace point " + tracePoint.symbol.name);

	    // If this observer doesn't observe given tracepoint
	    // anymore, remove it from bookkeeping structures.
	    if (tpo.remove(observer) == -1) {
		//if (workingSetForOne.remove(observer) != tpo) // Not .equals!
		//    throw new AssertionError("Couldn't find tracePointObserver in workingSetForOne.");

		// Retract from more bookkeeping structures if that
		// was the last observer of this tracepoint.
		if (tpo.observers.isEmpty()) {
		    if (!workingSet.remove(tpo))
			throw new AssertionError("Couldn't find tracePointObserver in workingSet.");

		    if (tpObserverForAddress.remove(tpo.address) == null)
			throw new AssertionError("Couldn't find tracePointObserver in tpObserverForAddress.");

		    if (tpObserverForTracePoint.remove(tracePoint) != tpo) // Not .equals!
			throw new AssertionError("Couldn't find tracePointObserver in tpObserverForTracePoint.");

		    // And retract also from the lowlevel breakpoint.
		    this.task.requestDeleteCodeObserver(tpo, tracePoint.address);

		    // Skip the deletedFrom call for the last
		    // observer, it will be called from
		    // TracePointObsever's deletedFrom.
		    continue;
		}
	    }

	    observer.deletedFrom(this.task);
	}
    }
}
