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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.*;

import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

class MappingGuard
    implements TaskObserver.Code,
	       TaskObserver.Syscall
{
    protected static final Logger logger = Logger.getLogger(FtraceLogger.LOGGER_ID);
    private HashMap syscallCache = new HashMap();
    private MappingController ltraceProper;

    public MappingGuard(MappingController ltraceProper) {
	this.ltraceProper = ltraceProper;
    }

    public Action updateHit (Task task, long address)
    {
	ltraceProper.checkMapUnmapUpdates(task);
	task.requestUnblock(this);
	return Action.BLOCK;
    }

    public Action updateSyscallEnter (Task task)
    {
	frysk.proc.Syscall syscall = task.getSyscallEventInfo().getSyscall(task);
	syscallCache.put(task, syscall);
	return Action.CONTINUE;
    }

    public Action updateSyscallExit (Task task)
    {
	frysk.proc.Syscall syscall = (frysk.proc.Syscall)syscallCache.remove(task);

	// Unfortunately, I know of no reasonable (as in
	// platform independent) way to find whether a syscall
	// is mmap, munmap, or anything else.  Hence this hack,
	// which is probably still much better than rescanning
	// the map on each syscall.
	String name = syscall.getName();
	if (name.indexOf("mmap") != -1 || name.indexOf("munmap") != -1) {
	    ltraceProper.checkMapUnmapUpdates(task);
	    task.requestUnblock(this);
	    return Action.BLOCK;
	}

	return Action.CONTINUE;
    }

    public void addFailed(Object o, Throwable t) {}
    public void deletedFrom(Object o) {}
    public void addedTo(Object o) {}

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
    private boolean setupDebugStateObserver(Task task)
    {
	logger.log(Level.FINE, "Entering....");

	java.io.File f = new java.io.File(task.getProc().getExe());
	ObjectFile objf = ObjectFile.buildFromFile(f);
	String interp = objf.getInterp();
	if (interp == null) {
	    // We're boned.
	    logger.log(Level.WARNING, "`{1}' has no interpreter.", f);
	    return false;
	}

	java.io.File interppath = new java.io.File(interp);
	try {
	    interppath = interppath.getCanonicalFile();
	}
	catch (java.io.IOException e) {
	    logger.log(Level.WARNING,
		       "Couldn't get canonical path of ELF interpreter `{1}'.",
		       interppath);
	    return false;
	}

	ObjectFile interpf = ObjectFile.buildFromFile(interppath);
	TracePoint tp = null;
	try {
	    tp = interpf.lookupTracePoint("_dl_debug_state",
					  TracePointOrigin.DYNAMIC);
	    if (tp == null) {
		logger.log(Level.FINE,
			   "Symbol _dl_debug_state not found in `{1}'.",
			   interppath);
		return false;
	    }
	}
	catch (lib.dwfl.ElfException e) {
	    e.printStackTrace();
	    logger.log(Level.WARNING,
		       "Problem reading DYNAMIC entry points from `{1}'",
		       interppath);
	    return false;
	}

	// Load initial set of mapped files.
	Set currentMappings = MemoryMapping.buildForPid(task.getTid());
	long relocation = -1;
	for (Iterator it = currentMappings.iterator(); it.hasNext(); ) {
	    MemoryMapping mm = (MemoryMapping)it.next();
	    if (mm.path.equals(interppath)) {
		relocation = mm.addressLow - interpf.getBaseAddress();
		break;
	    }
	}
	if (relocation == -1) {
	    logger.log(Level.FINE, "Couldn't obtain relocation of interpreter.");
	    return false;
	}

	// There we go!
	long fin = tp.address + relocation;
	task.requestAddCodeObserver(this, fin);
	return true;
    }

    public void attachTo(Task task)
    {
	if (!setupDebugStateObserver(task))
	    task.requestAddSyscallObserver(this);
    }
}
