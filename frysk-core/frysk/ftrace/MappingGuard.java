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
import java.util.Map;
import java.util.List;
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
    /*package-private*/ static boolean enableSyscallObserver = true;
    /*package-private*/ static boolean enableDebugstateObserver = true;
    protected static final Logger logger = Logger.getLogger(FtraceLogger.LOGGER_ID);

    // HashMap<Task, MappingGuardB>
    private static final Map guardsForTask = new HashMap();

    private static abstract class MappingGuardB
	implements TaskObserver
    {
	private final Map observers = new HashMap(); // HashMap<MappingObserver, Integer>
	protected final Task task;
	private Map maps = java.util.Collections.EMPTY_MAP;
	private boolean lowlevelObserversAdded = false;
	private boolean lowlevelObserversFailed = false;
	private Throwable lowlevelObserverThrowable = null;

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

	    // XXX: again, probably too early for the last observer
	    // which was requestDeleted.
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


	    if (lowlevelObserversAdded)
		observer.addedTo(task);
	    else if (lowlevelObserversFailed)
		observer.addFailed(task, lowlevelObserverThrowable);
	}

	protected MappingGuardB(Task task) {
	    this.task = task;
	}

    	public synchronized void addedTo (final Object observable)
	{
	    if (!lowlevelObserversAdded) {
		eachObserver(new ObserverIterator() {
			public Action action(MappingObserver observer) {
			    observer.addedTo(observable);
			    return Action.CONTINUE;
			}
		    });
		lowlevelObserversAdded = true;
	    }
	}

	public synchronized void addFailed(final Object observable, final Throwable w) {
	    logger.log(Level.FINE, "lowlevel addFailed!");
	    if (!lowlevelObserversFailed) {
		eachObserver(new ObserverIterator() {
			public Action action(MappingObserver observer) {
			    observer.addFailed(observable, w);
			    return Action.CONTINUE;
			}
		    });
		lowlevelObserversFailed = true;
		lowlevelObserverThrowable = w;
	    }
	}

	public void deletedFrom(Object o) {
	    /// XXX: How to delete map/unmap observer?
	    //observer.deletedFrom(o);
	    synchronized (MappingGuard.class) {
		guardsForTask.remove(task);
	    }
	}

    	private interface ObserverIterator {
	    Action action(MappingObserver observer);
	}

	protected void eachObserver(ObserverIterator oit) {
	    for (Iterator it = observers.entrySet().iterator(); it.hasNext();) {
		Map.Entry entry = (Map.Entry)it.next();
		int v = ((Integer)entry.getValue()).intValue();
		MappingObserver ob = (MappingObserver)entry.getKey();
		for (int j = 0; j < v; ++j)
		    if (oit.action(ob) == Action.BLOCK)
			task.blockers.add(ob);
	    }
	}


    	private void updateMappedPart(final Task task,
				      final MemoryMapping mapping,
				      final MemoryMapping.Part part)
	{
	    eachObserver(new ObserverIterator() {
		    public Action action(MappingObserver observer) {
			return observer.updateMappedPart(task, mapping, part);
		    }
		});
	}

	private void updateUnmappedPart(final Task task,
					final MemoryMapping mapping,
					final MemoryMapping.Part part)
	{
	    eachObserver(new ObserverIterator() {
		    public Action action(MappingObserver observer) {
			return observer.updateUnmappedPart(task, mapping, part);
		    }
		});
	}

	private void updateMappedFile(final Task task,
				      final MemoryMapping mapping)
	{
	    eachObserver(new ObserverIterator() {
		    public Action action(MappingObserver observer) {
			return observer.updateMappedFile(task, mapping);
		    }
		});

	    for (Iterator it = mapping.parts.iterator(); it.hasNext(); ) {
		MemoryMapping.Part part = (MemoryMapping.Part)it.next();
		updateMappedPart(task, mapping, part);
	    }
    	}

	private void updateUnmappedFile(final Task task,
					final MemoryMapping mapping)
	{
	    for (Iterator it = mapping.parts.iterator(); it.hasNext(); ) {
		MemoryMapping.Part part = (MemoryMapping.Part)it.next();
		updateUnmappedPart(task, mapping, part);
	    }

	    eachObserver(new ObserverIterator() {
		    public Action action(MappingObserver observer) {
			return observer.updateUnmappedFile(task, mapping);
		    }
		});
    	}

	protected void updateMapping(final Task task, boolean terminating) {
	    Map oldMappings = this.maps;
	    final Map newMappings;
	    if (terminating)
		newMappings = java.util.Collections.EMPTY_MAP;
	    else
		newMappings = MemoryMapping.buildForPid(task.getTid());

	    // Resolve full mmaps, and partial mmaps and unmaps.
	    for (Iterator it = newMappings.entrySet().iterator(); it.hasNext(); ) {
		Map.Entry entry = (Map.Entry)it.next();
		Object oKey = entry.getKey(); // actually a File
		MemoryMapping newMapping = (MemoryMapping)entry.getValue();
		if (!oldMappings.containsKey(oKey)) {
		    updateMappedFile(task, newMapping);
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
			    updateMappedPart(task, newMapping, part);
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
			    updateUnmappedPart(task, oldMapping, part);
			    ++i;
			}
		    }
		}
	    }

	    // Resolve full unmaps.
	    for (Iterator it = oldMappings.entrySet().iterator(); it.hasNext(); ) {
		Map.Entry entry = (Map.Entry)it.next();
		MemoryMapping removedMapping = (MemoryMapping)entry.getValue();
		updateUnmappedFile(task, removedMapping);
	    }

	    this.maps = newMappings;
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

	    if (syscall != null) {
		// Unfortunately, I know of no reasonable (as in platform
		// independent) way to find whether a syscall is mmap,
		// munmap, or anything else.  Hence this hack, which is
		// probably still much better than rescanning the map on
		// each syscall.
		String name = syscall.getName();
		if (name.indexOf("mmap") != -1 || name.indexOf("munmap") != -1)
		    updateMapping(task, false);
	    }
	    return Action.CONTINUE;
	}

	public void remove() {
	    task.requestDeleteSyscallObserver(this);
	}
    }

    private static class DebugStateMappingGuard
	extends MappingGuardB
	implements TaskObserver.Code,
		   TaskObserver.Terminating
    {
	private long address;
	public DebugStateMappingGuard(Task task, long address) {
	    super(task);
	    this.address = address;
	    task.requestAddCodeObserver(this, address);
	    task.requestAddTerminatingObserver(this);
	}

	public Action updateHit (Task task, long address)
	{
	    logger.log(Level.FINE, "Mapping guard hit.");
	    updateMapping(task, false);
	    return Action.CONTINUE;
	}

	public Action updateTerminating (Task task, boolean signal, int value)
	{
	    logger.log(Level.FINE, "The task is terminating.");
	    updateMapping(task, true);
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

	File interppath = objf.resolveInterp();
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
		if (enableDebugstateObserver)
		    guard = setupDebugStateObserver(task);
		if (guard == null && enableSyscallObserver)
		    guard = new SyscallMappingGuard(task);

		if (guard != null)
		    guardsForTask.put(task, guard);
		else
		    observer.addFailed(task, new UnsupportedOperationException("Couldn't initialize mapping guard."));
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
