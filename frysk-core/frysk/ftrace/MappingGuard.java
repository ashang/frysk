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

import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.*;
import java.io.File;

import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

/**
 * Use this pseudo-class to request that an map/unmap observer be
 * attached to given Task.
 */
class MappingGuard
{
    protected static final Logger logger = Logger.getLogger(FtraceLogger.LOGGER_ID);

    // HashMap<Task, MappingGuardB>
    private static final Map guardsForTask = new HashMap();

    private static abstract class MappingGuardB
	implements TaskObserver
    {
	private final Map observers = new HashMap(); // HashMap<MappingObserver, Integer>
	protected final Task task;

	abstract public void remove();

	public synchronized boolean removeObserver(MappingObserver observer) {
	    Integer i = (Integer)observers.get(observer);
	    if (i == null)
		throw new AssertionError("removed observer not found.");
	    int v = i.intValue();
	    v--;
	    if (v == 0)
		observers.remove(observer);
	    else
		observers.put(observer, new Integer(v));
	    observer.deletedFrom(task);
	    return v != 0;
	}

	public synchronized void addObserver(MappingObserver observer) {
	    Integer i = (Integer)observers.get(observer);
	    if (i == null)
		i = new Integer(1);
	    else
		i = new Integer(i.intValue() + 1);
	    observers.put(observer, i);
	    observer.addedTo(task);
	}

	protected MappingGuardB(Task task) {
	    this.task = task;
	}

	public void addFailed(Object o, Throwable t) {
	    //observer.addFailed(o, t);
	}
	public void deletedFrom(Object o) {
	    /// XXX: How to delete map/unmap observer?
	    //observer.deletedFrom(o);
	    synchronized (MappingGuard.class) {
		guardsForTask.remove(task);
	    }
	}
	public void addedTo(Object o) {
	    //observer.addedTo(o);
	}

	protected void notifyObservers(Task task) {
	    for (Iterator it = observers.keySet().iterator(); it.hasNext();) {
		MappingObserver ob = (MappingObserver)it.next();
		boolean block = false;
		Integer i = (Integer)observers.get(ob);
		int v = i.intValue();
		for (int j = 0; j < v; ++j)
		    if (ob.updateMapping(task) == Action.BLOCK)
			block = true;
		if (block)
		    task.blockers.add(ob);
	    }
	}
    }

    private static class SyscallMappingGuard
	extends MappingGuardB
	implements TaskObserver.Syscall
    {
	private frysk.proc.Syscall syscallCache = null;

	public SyscallMappingGuard(Task task) {
	    super(task);
	    task.requestAddSyscallObserver(this);
	}

	public Action updateSyscallEnter (Task task)
	{
	    frysk.proc.Syscall syscall = task.getSyscallEventInfo().getSyscall(task);
	    syscallCache = syscall;
	    return Action.CONTINUE;
	}

	public Action updateSyscallExit (Task task)
	{
	    frysk.proc.Syscall syscall = syscallCache;
	    syscallCache = null;

	    // Unfortunately, I know of no reasonable (as in platform
	    // independent) way to find whether a syscall is mmap,
	    // munmap, or anything else.  Hence this hack, which is
	    // probably still much better than rescanning the map on
	    // each syscall.
	    String name = syscall.getName();
	    if (name.indexOf("mmap") != -1 || name.indexOf("munmap") != -1)
		notifyObservers(task);
	    return Action.CONTINUE;
	}

	public void remove() {
	    task.requestDeleteSyscallObserver(this);
	}
    }

    private static class DebugStateMappingGuard
	extends MappingGuardB
	implements TaskObserver.Code
    {
	private long address;
	public DebugStateMappingGuard(Task task, long address) {
	    super(task);
	    this.address = address;
	    task.requestAddCodeObserver(this, address);
	}

	public Action updateHit (Task task, long address)
	{
	    logger.log(Level.FINE, "Mapping guard hit.");
	    notifyObservers(task);
	    return Action.CONTINUE;
	}

	public void remove() {
	    task.requestDeleteCodeObserver(this, address);
	}
    }

    /**
     * Try to setup guard based on _dl_debug_state.
     *
     * Set up _dl_debug_state observer to spot each mapping.  The
     * proper way to do this is to look up the DT_DEBUG entry in
     * task's DYNAMIC segment, and look into the structure it points
     * to.  But we would have to wait for dynamic linker to fill
     * this info, and meanwhile we would miss all the
     * mapping/unmapping.
     *
     * @return true on success, false on failure.
     */
    private static MappingGuardB setupDebugStateObserver(Task task)
    {
	logger.log(Level.FINE, "Entering....");

	File f = new File(task.getProc().getExe());
	ObjectFile objf = ObjectFile.buildFromFile(f);
	String interp = objf.getInterp();
	if (interp == null) {
	    // We're boned.
	    logger.log(Level.WARNING, "`{1}' has no interpreter.", f);
	    return null;
	}

	File interppath = new File(interp);
	try {
	    interppath = interppath.getCanonicalFile();
	}
	catch (java.io.IOException e) {
	    logger.log(Level.WARNING,
		       "Couldn't get canonical path of ELF interpreter `{0}'.",
		       interppath);
	    return null;
	}

	ObjectFile interpf = ObjectFile.buildFromFile(interppath);
	TracePoint tp = null;
	try {
	    tp = interpf.lookupTracePoint("_dl_debug_state",
					  TracePointOrigin.DYNAMIC);
	    if (tp == null) {
		logger.log(Level.FINE,
			   "Symbol _dl_debug_state not found in `{0}'.",
			   interppath);
		return null;
	    }

	    // Make sure we know the offset of the symbol data.
	    // Necessary for lookup between mappings.
	    if (tp.symbol.offset == 0) {
		logger.log(Level.FINE,
			   "Symbol _dl_debug_state has offset 0.",
			   interppath);
		return null;
	    }
	}
	catch (lib.dwfl.ElfException e) {
	    e.printStackTrace();
	    logger.log(Level.WARNING,
		       "Problem reading DYNAMIC entry points from `{0}'",
		       interppath);
	    return null;
	}

	// Load initial set of mapped files.
	Map currentMappings = MemoryMapping.buildForPid(task.getTid());
	MemoryMapping mm = (MemoryMapping)currentMappings.get(interppath);
	if (mm == null) {
	    logger.log(Level.FINE, "Couldn't obtain mappings of interpreter.");
	    return null;
	}

	List parts = mm.lookupParts(tp.symbol.offset);
	if (parts.size() != 1) {
	    logger.log(Level.FINE, "Ambiguous mapping of interpreter, or the mapping couldn't be determined.");
	    return null;
	}
	MemoryMapping.Part p = (MemoryMapping.Part)parts.get(0);
	long relocation = p.addressLow - interpf.getBaseAddress();

	// There we go!
	long fin = tp.address + relocation;
	logger.log(Level.FINE,
		   "Success: tp.address=0x" + Long.toHexString(tp.address)
		   + ", relocation=0x" + Long.toHexString(relocation)
		   + ", fin=0x" + Long.toHexString(fin));
	return new DebugStateMappingGuard(task, fin);
    }

    public static void requestAddMappingObserver(Task task, MappingObserver observer) {
	MappingGuardB guard;
	synchronized (MappingGuard.class) {
	    guard = (MappingGuardB)guardsForTask.get(task);
	    if (guard == null) {
		guard = setupDebugStateObserver(task);
		if (guard == null)
		    guard = new SyscallMappingGuard(task);
		guardsForTask.put(task, guard);
	    }
	}
	guard.addObserver(observer);
    }

    public static void requestDeleteMappingObserver(Task task, MappingObserver observer) {
	MappingGuardB guard;
	synchronized (MappingGuard.class) {
	    guard = (MappingGuardB)guardsForTask.get(task);
	}
	if (guard == null)
	    throw new AssertionError("No guard for given task");
	if (!guard.removeObserver(observer))
	    guard.remove();
    }
}
