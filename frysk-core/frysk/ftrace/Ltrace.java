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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.*;

public class Ltrace
    implements TaskObserver.Attached,
	       TaskObserver.Code,
	       TaskObserver.Terminated,
	       TaskObserver.Terminating,
	       MappingObserver
{
    private static final Logger logger = Logger.getLogger(FtraceLogger.LOGGER_ID);

    public static interface Driver {
	void tracePoint(Task task, TracePoint tp);
    }

    // HashMap<Task, Ltrace>
    private static final Map ltraceForTask = new HashMap();

    public static void requestAddFunctionObserver(Task task,
						  FunctionObserver observer,
						  LtraceController controller)
    {
	Ltrace ltrace = (Ltrace)ltraceForTask.get(task);
	if (ltrace == null) {
	    ltrace = new Ltrace(task, controller);
	    ltraceForTask.put(task, ltrace);
	}

	ltrace.addObserver(observer);
    }

    public static void requestDeleteFunctionObserver(Task task, FunctionObserver observer) {
	Ltrace ltrace = (Ltrace)ltraceForTask.get(task);
	if (ltrace == null)
	    observer.addFailed(task, new RuntimeException("This observer doesn't observe given task."));
	else
	    ltrace.removeObserver(observer);
    }

    // ------------------------ instance part ------------------------

    private final Task task;
    private final Arch arch;
    private final LtraceController controller;

    /** Remembers which files are currently mapped in which task. */
    private Map maps = java.util.Collections.EMPTY_MAP;

    /** Remembers which tracepoint is associated with which breakpoint..
	Map&lt;address, TracePoint&gt; */
    private final HashMap breakpoints = new HashMap();

    /** Remembers return from which tracepoint is associated with
	which breakpoint.
	Map&lt;address, List&lt;TracePoint&gt;&gt; */
    private final HashMap retBreakpoints = new HashMap();

    /** Remembers working set preferences for each task.
	Map&lt;Task, Map&lt;File, TracePointWorkingSet&gt;&gt; */
    private final HashMap driversForTask = new HashMap();

    /** Function observers.
	Map&lt;FunctionObserver, Integer&gt; */
    private final HashMap observers = new HashMap();

    private boolean lowlevelObserversAdded = false;
    private boolean lowlevelObserversFailed = false;
    private Throwable lowlevelObserverThrowable = null;

    // ----------------------
    // --- setup/teardown ---
    // ----------------------
    Ltrace(Task task, LtraceController controller) {
	this.task = task;
	this.arch = ArchFactory.instance.getArch(task);
	this.controller = controller;

	task.requestAddAttachedObserver(this);
	task.requestAddTerminatedObserver(this);
	task.requestAddTerminatingObserver(this);
	MappingGuard.requestAddMappingObserver(task, this);
	// There are no code observers right now.  We add them as files
	// get mapped to the process.
    }

    public void remove() {
	// XXX: do the right thing.
    }

    // ---------------------------
    // --- observer management ---
    // ---------------------------

    private interface ObserverIterator {
	Action action(FunctionObserver observer);
    }

    private void eachObserver(ObserverIterator oit) {
	for (Iterator it = observers.entrySet().iterator(); it.hasNext();) {
	    HashMap.Entry entry = (HashMap.Entry)it.next();
	    int v = ((Integer)entry.getValue()).intValue();
	    FunctionObserver ob = (FunctionObserver)entry.getKey();
	    for (int j = 0; j < v; ++j)
		if (oit.action(ob) == Action.BLOCK)
		    task.blockers.add(ob);
	}
    }

    /**
     * Add new observer.
     */
    private synchronized void addObserver(FunctionObserver observer)
    {
	Integer i = (Integer)observers.get(observer);
	if (i == null)
	    i = new Integer(1);
	else
	    i = new Integer(i.intValue() + 1);
	observers.put(observer, i);

	if (lowlevelObserversAdded)
	    observer.addedTo(task);
	else if (lowlevelObserversFailed)
	    observer.addFailed(task, lowlevelObserverThrowable);
    }

    /**
     * Remove given observer.
     */
    private synchronized boolean removeObserver(FunctionObserver observer) {
	Integer i = (Integer)observers.get(observer);
	if (i == null)
	    throw new AssertionError("removed observer not found.");
	int v = i.intValue();
	v--;
	if (v == 0)
	    observers.remove(observer);
	else
	    observers.put(observer, new Integer(v));

	// XXX: Probably too early for the last observer which was
	// requestDeleted.
	observer.deletedFrom(task);
	return v != 0;
    }


    // ------------------------------------------
    // --- code observer, breakpoint handling ---
    // ------------------------------------------

    /*
    public Action libcallEnter(Task task, Symbol symbol, long address)
    {
	// XXX: this makes no sense for PLT tracing.
	Frame frame = StackFactory.createFrame(task);
	if (frame != null)
	    {
		try { frame = frame.getOuter(); }
		catch (java.lang.NullPointerException ex) {}
	    }
	String symbolName = symbol.name;
	String libraryName = symbol.getParent().getSoname();

	String callerLibrary = "(toplevel)";
	if (frame != null)
	    {
		try {
		    callerLibrary = frame.getLibraryName();
		    if (!callerLibrary.equals("Unknown"))
			callerLibrary = ObjectFile.buildFromFile(callerLibrary).getSoname();
		}
		catch (java.lang.Exception ex) {
		    callerLibrary = "(Exception)";
		}
	    }
    }
    */

    public Action updateHit (final Task task, long address)
    {
	logger.log(Level.FINE, "Breakpoint at 0x" + Long.toHexString(address));
	Long laddress = new Long(address);

	TracePoint enter = null;
	TracePoint leave = null;

	// See if we enter somewhere.
	TracePoint tp = (TracePoint)breakpoints.get(laddress);
	if (tp != null)
	    {
		if (address != tp.symbol.getParent().getEntryPoint()) {
		    // Install breakpoint to return address.
		    long retAddr = arch.getReturnAddress(task, tp.symbol);
		    logger.log(Level.FINER,
			       "It's enter tracepoint, return address 0x"
			       + Long.toHexString(retAddr) + ".");
		    Long retAddrL = new Long(retAddr);
		    List tpList = (List)retBreakpoints.get(retAddrL);
		    if (tpList == null)
			{
			    task.requestAddCodeObserver(this, retAddr);
			    tpList = new LinkedList();
			    retBreakpoints.put(retAddrL, tpList);
			}
		    tpList.add(tp);
		}
		else
		    logger.log(Level.FINEST,
			       "It's _start, no return breakpoint established...");

		enter = tp;
	    }

	// See if we returned from somewhere.
	List tpList = (List)retBreakpoints.get(laddress);
	if (tpList != null) {
	    logger.log(Level.FINER, "It's leave tracepoint.");
	    leave = (TracePoint)tpList.remove(tpList.size() - 1);
	    if (tpList.isEmpty())
		{
		    logger.log(Level.FINEST, "Removing leave breakpoint.");
		    retBreakpoints.remove(new Long(address));
		    task.requestDeleteCodeObserver(this, address);
		}
	}

	if (enter != null || leave != null) {
	    if (enter != null) {
		logger.log(Level.FINEST, "Building arglist.");
		final Object[] args = arch.getCallArguments(task, enter.symbol);
		final Symbol symbol = enter.symbol;
		eachObserver(new ObserverIterator() {
			public Action action(FunctionObserver o) {
			    return o.funcallEnter(task, symbol, args);
			}
		    });
	    }

	    if (leave != null) {
		logger.log(Level.FINEST, "Fetching retval.");
		final Object ret = arch.getReturnValue(task, leave.symbol);
		final Symbol symbol = leave.symbol;
		eachObserver(new ObserverIterator() {
			public Action action(FunctionObserver o) {
			    return o.funcallLeave(task, symbol, ret);
			}
		    });
	    }
	}
	else
	    System.err.println("[" + task.getTaskId().intValue() + "] "
			       + "UNKNOWN BREAKPOINT 0x" + Long.toHexString(address));

	logger.log(Level.FINE, "Breakpoint handled.");
	task.requestUnblock(this);
	return Action.BLOCK;
    }



    // -------------------------------------------------
    // --- attached/terminated/terminating observers ---
    // -------------------------------------------------

    public Action updateAttached(Task task)
    {
	// Per-task initialization.
	long pc = task.getIsa().pc(task);
	logger.log(Level.FINE,
		   "new task attached at 0x" + Long.toHexString(pc)
		   + ", pid=" + task.getTaskId().intValue());

	this.checkMapUnmapUpdates(task, false);
	MappingGuard.requestAddMappingObserver(task, this);
	return Action.BLOCK;
    }

    public Action updateTerminating(Task task, boolean signal, int value)
    {
	this.checkMapUnmapUpdates(task, true);
	task.requestUnblock(this);
	return Action.BLOCK;
    }

    public Action updateTerminated(Task task, boolean signal, int value)
    {
	// XXX: `delete this;'???
	return Action.CONTINUE;
    }



    // ----------------------------
    // --- mmap/munmap handling ---
    // ----------------------------

    private class TracePointWorkingSet
	implements Ltrace.Driver
    {
	private Set tracePoints = new HashSet();

	public void tracePoint(Task task, TracePoint tp)
	{
	    logger.log(Level.CONFIG, "Request for tracing `{0}'", tp.symbol.name);
	    tracePoints.add(tp);
	}

	public void populateBreakpoints(Task task, MemoryMapping.Part part)
	{
	    for (Iterator it = tracePoints.iterator(); it.hasNext(); ) {
		TracePoint tp = (TracePoint)it.next();
		if (tp.offset >= part.offset
		    && tp.offset < part.offset + part.addressHigh - part.addressLow) {
		    logger.log(Level.FINER,
			       "Will trace `" + tp.symbol.name + "', "
			       + "address=0x" + Long.toHexString(tp.address) + "; "
			       + "offset=0x" + Long.toHexString(tp.offset) + "; "
			       + "part at=0x" + Long.toHexString(part.addressLow)
			       + ".." + Long.toHexString(part.addressHigh) + "; "
			       + "part off=0x" + Long.toHexString(part.offset) + ";");

		    long actualAddress = tp.offset - part.offset + part.addressLow;
		    Long laddr = new Long(actualAddress);
		    logger.log(Level.CONFIG,
			       "Will trace `" + tp.symbol.name
			       + "' at 0x" + Long.toHexString(actualAddress));

		    // FIXME: probably handle aliases at a lower
		    // lever.  Each tracepoint should point to a list
		    // of symbols that alias it, and should be present
		    // only once in an ObjectFile.
		    synchronized (Ltrace.this) {
			if (breakpoints.containsKey(laddr)) {
			    // We got an alias.  Put the symbol with the
			    // shorter name into the map.
			    TracePoint original = (TracePoint)breakpoints.get(laddr);
			    if (tp.symbol.name.length() < original.symbol.name.length())
				breakpoints.put(laddr, tp);
			}
			else {
			    task.requestAddCodeObserver(Ltrace.this, laddr.longValue());
			    breakpoints.put(laddr, tp);
			}
		    }
		}
	    }
	}

	public void evacuateBreakpoints(Task task, MemoryMapping.Part part)
	{
	    for (Iterator it = tracePoints.iterator(); it.hasNext(); ) {
		TracePoint tp = (TracePoint)it.next();
		if (tp.offset >= part.offset
		    && tp.offset < part.offset + part.addressHigh - part.addressLow) {

		    long actualAddress = tp.offset - part.offset + part.addressLow;
		    Long laddr = new Long(actualAddress);
		    logger.log(Level.CONFIG,
			       "Stopping tracing of `" + tp.symbol.name
			       + "' at 0x" + Long.toHexString(actualAddress));

		    // FIXME: Handle aliases.
		    synchronized (Ltrace.this) {
			TracePoint original = (TracePoint)breakpoints.remove(laddr);
			if (original == null)
			    throw new AssertionError("Couldn't find breakpoint to remove!");
			task.requestDeleteCodeObserver(Ltrace.this, laddr.longValue());
		    }
		}
	    }
	}
    }

    /** Implementation of MappingObserver interface... */
    public Action updateMapping(Task task) {
	checkMapUnmapUpdates(task, false);
	task.requestUnblock(this);
	return Action.BLOCK;
    }

    private void checkMapUnmapUpdates(Task task, boolean terminating)
    {
	Map oldMappings = this.maps;
	final Map newMappings;
	if (terminating)
	    newMappings = java.util.Collections.EMPTY_MAP;
	else
	    newMappings = MemoryMapping.buildForPid(task.getTid());

	// Resolve full mmaps, and partial mmaps and unmaps.
	for (Iterator it = newMappings.entrySet().iterator(); it.hasNext(); ) {
	    Map.Entry entry = (Map.Entry)it.next();
	    Object oKey = entry.getKey(); // actually File
	    MemoryMapping newMapping = (MemoryMapping)entry.getValue();
	    if (!oldMappings.containsKey(oKey)) {
		this.updateMappedFile(task, newMapping);
	    }
	    else {
		// Remove the key from old set, so that at the end, only
		// unmapped values are left.
		MemoryMapping oldMapping = (MemoryMapping)oldMappings.remove(oKey);
		int oldSize = oldMapping.parts.size();
		int newSize = newMapping.parts.size();
		if (oldSize < newSize) {
		    // newMapping.parts[oldSize:] is VERY likely the new stuff
		    for (int i = oldMapping.parts.size(); i < newMapping.parts.size(); ++i) {
			MemoryMapping.Part part = (MemoryMapping.Part)newMapping.parts.get(i);
			this.updateMappedPart(task, newMapping, part);
		    }
		}
		else if (oldSize > newSize) {
		    // Find first non-matching Part.
		    int i = 0;
		    int j = 0;
		    while (i < oldSize && j < newSize
			   && oldMapping.parts.get(i).equals(newMapping.parts.get(j))) {
			++i;
			++j;
		    }

		    // If `j' is at the end, the remaining old parts
		    // are unmapped.  Otherwise only the portion
		    // until the first matching part is unmapped.
		    while (i < oldSize
			   && (j >= newSize
			       || !oldMapping.parts.get(i).equals(newMapping.parts.get(j)))) {
			MemoryMapping.Part part = (MemoryMapping.Part)oldMapping.parts.get(i);
			this.updateUnmappedPart(task, oldMapping, part);
			++i;
		    }
		}
	    }
	}

	// Resolve full unmaps.
	for (Iterator it = oldMappings.entrySet().iterator(); it.hasNext(); ) {
	    Map.Entry entry = (Map.Entry)it.next();
	    MemoryMapping removedMapping = (MemoryMapping)entry.getValue();
	    this.updateUnmappedFile(task, removedMapping);
	}

	this.maps = newMappings;
    }

    private void updateMappedPart(Task task, MemoryMapping mapping,
				  MemoryMapping.Part part,
				  TracePointWorkingSet driver)
    {
	driver.populateBreakpoints(task, part);
    }

    private void updateUnmappedPart(Task task, MemoryMapping mapping,
				    MemoryMapping.Part part,
				    TracePointWorkingSet driver)
    {
	driver.evacuateBreakpoints(task, part);
    }

    private void updateMappedPart(Task task, MemoryMapping mapping, MemoryMapping.Part part)
    {
	// At this point we already know client preferences
	// regarding which tracepoints should be in working set.
	// Just apply this knowledge on new mapped part: all working
	// set elements that fall into this part should be observed.

	// This has to be non-null, we already called
	// updateMappedFile when the file was mapped:
	Map drivers = (Map)driversForTask.get(task);
	TracePointWorkingSet driver = (TracePointWorkingSet)drivers.get(mapping.path);
	updateMappedPart(task, mapping, part, driver);
    }

    private void updateUnmappedPart(Task task, MemoryMapping mapping, MemoryMapping.Part part)
    {
	// This has to be non-null.
	Map drivers = (Map)driversForTask.get(task);
	TracePointWorkingSet driver = (TracePointWorkingSet)drivers.get(mapping.path);
	updateUnmappedPart(task, mapping, part, driver);
    }

    private void updateMappedFile(Task task, MemoryMapping mapping)
    {
	// New file has been mapped.  Notify all observers (TODO: this
	// should go away once ltrace becomes true observer), and let
	// client define working set of this file.  Then implement the
	// working set via updateMappedPart of each part in mapping.
	for (Iterator it = observers.keySet().iterator(); it.hasNext(); ) {
	    FunctionObserver o = (FunctionObserver)it.next();
	    o.fileMapped(task, mapping.path);
	}

	ObjectFile objf = ObjectFile.buildFromFile(mapping.path);
	if (objf == null)
	    return;

	TracePointWorkingSet driver = new TracePointWorkingSet();
	Map drivers = (Map)driversForTask.get(task);
	if (drivers == null) {
	    drivers = new HashMap();
	    driversForTask.put(task, drivers);
	}
	drivers.put(mapping.path, driver);
	controller.fileMapped(task, objf, driver);

	for (Iterator it = mapping.parts.iterator(); it.hasNext(); ) {
	    MemoryMapping.Part part = (MemoryMapping.Part)it.next();
	    if (part.permExecute)
		updateMappedPart(task, mapping, part, driver);
	}
    }

    private void updateUnmappedFile (Task task, MemoryMapping mapping)
    {
	for (Iterator it = observers.keySet().iterator(); it.hasNext(); ) {
	    FunctionObserver o = (FunctionObserver)it.next();
	    o.fileUnmapped(task, mapping.path);
	}

	// This has to be non-null.
	Map drivers = (Map)driversForTask.get(task);
	// This can be null for non-elf files.  We don't care about
	// these.
	TracePointWorkingSet driver = (TracePointWorkingSet)drivers.get(mapping.path);
	if (driver == null)
	    return;

	for (Iterator it = mapping.parts.iterator(); it.hasNext(); ) {
	    MemoryMapping.Part part = (MemoryMapping.Part)it.next();
	    if (part.permExecute)
		updateUnmappedPart(task, mapping, part, driver);
	}
    }



    // ----------------------------------------
    // --- Higher level observer interfaces ---
    // ----------------------------------------

    // XXX: the synchronization will be ugly overkill with all the
    // code observers being added and removed all the time.  Have to
    // invent something a bit better.
    public synchronized void addedTo (final Object observable)
    {
	if (!lowlevelObserversAdded) {
	    eachObserver(new ObserverIterator() {
		    public Action action(FunctionObserver observer) {
			observer.addedTo(observable);
			return Action.CONTINUE;
		    }
		});
	    lowlevelObserversAdded = true;
	}
	((Task)observable).requestUnblock(this); // XXX: what is this?
    }

    public void deletedFrom (Object observable)
    {
	// XXX: write this
    }

    public synchronized void addFailed (final Object observable, final Throwable w)
    {
	logger.log(Level.FINE, "lowlevel addFailed!");
	if (!lowlevelObserversFailed) {
	    eachObserver(new ObserverIterator() {
		    public Action action(FunctionObserver observer) {
			observer.addFailed(observable, w);
			return Action.CONTINUE;
		    }
		});
	    lowlevelObserversFailed = true;
	    lowlevelObserverThrowable = w;
	}
    }
}
