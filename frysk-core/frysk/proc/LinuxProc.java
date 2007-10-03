// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

package frysk.proc;

import frysk.sys.Ptrace;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import inua.eio.ArrayByteBuffer;
import inua.eio.ByteBuffer;
import frysk.sys.proc.Stat;
import frysk.sys.proc.ScanDir;

/**
 * Linux implementation of Proc.
 */

public class LinuxProc
    extends Proc
{
    /**
     * If it hasn't already been read, read the stat structure.
     */
    private Stat getStat ()
    {
	if (stat == null) {
	    stat = new Stat ();
	    stat.refresh (id.id);
	}
	return stat;
    }
    Stat stat;

    public String getCommand ()
    {
	return getStat ().comm;
    }

    private Auxv[] auxv;
    /**
     * Create a new detached process.  RUNNING makes no sense here.
     * Since PARENT could be NULL, also explicitly pass in the host.
     */
    LinuxProc (Host host, Proc parent, ProcId pid, Stat stat)
    {
	super (host, parent, pid);
	this.stat = stat;
    }
    /**
     * Create a new, attached, and possibly running, process.
     */
    LinuxProc (Proc parent, ProcId procId, boolean running)
    {
	super (parent, procId, running);
    }
    void sendRefresh ()
    {
	// Compare this against the existing taskPool.  ADDED
	// accumulates any tasks added to the taskPool.  REMOVED,
	// starting with all known tasks has any existing tasks
	// removed, so that by the end it contains a set of
	// removed tasks.
	class TaskDir
	    extends ScanDir
	{
	    Map added = new HashMap ();
	    HashMap removed = (HashMap) ((HashMap)taskPool).clone ();
	    TaskId searchId = new TaskId ();
	    public void process (int pid)
	    {
		searchId.id = pid;
		if (removed.containsKey (searchId)) {
		    removed.remove (searchId);
		}
		else {
		    // Add the process (it currently isn't attached).
		    Task newTask = new LinuxTask (LinuxProc.this,
						  new TaskId (pid));
		    added.put (newTask.id, newTask);
		}
	    }
	}
	TaskDir tasks = new TaskDir ();
	tasks.refresh (id.id);
	// Tell each task that no longer exists that it has been
	// removed.
	for (Iterator i = tasks.removed.values().iterator(); i.hasNext();) {
	    Task task = (Task) i.next ();
	    // XXX: Should there be a TaskEvent.schedule(), instead of
	    // Manager .eventLoop .appendEvent for injecting the event
	    // into the event loop?
	    task.performRemoval ();
	    remove (task);
	}
    }
    void sendAttach (final boolean runningArg)
    {
	Ptrace.attach (id.id);
	ScanDir tasks = new ScanDir ()
	    {
		boolean running = runningArg;
		public void process (int tid)
		{
		    if (tid != id.id)
			Ptrace.attach (tid);
		    TaskId newTid = new TaskId (tid);
		    sendNewAttachedTask (newTid, running);
		}
	    };
	tasks.refresh (id.id);
    }
    void sendNewAttachedChild (ProcId childId, boolean running)
    {
	// A forked child starts out attached.
	new LinuxProc (this, childId, running);
    }

    void sendNewAttachedTask (TaskId id, boolean running)
    {
	// XXX: Should be abstracted.
	new I386Linux.Task (this, id, running);
    }
    public Auxv[] getAuxv ()
    {
	if (auxv == null) {
	    // XXX: Pull a random pid off the list, use that to
	    // construct the AUXV path; shouldn't be needed, instead
	    // use getPid() and a missing getIsa().
	    ByteBuffer b;
	    LinuxTask pid = (LinuxTask) taskPool.values().iterator().next ();
	    String auxvName = "/proc/" + getPid () + "/auxv";
	    // Due to Linux kernel tackyness, need to slurp in the
	    // AUXV and then create an ByteBuffer from that.
	    try {
		java.io.FileInputStream auxvFile
		    = new java.io.FileInputStream (auxvName);
		byte[] buf = new byte[1024];
		int len = auxvFile.read (buf);
		b = new ArrayByteBuffer (buf, 0, len);
	    }
	    catch (Exception e) {
		throw new RuntimeException (e);
	    }
	    // Adjust the AuxV buffer parameters based on the
	    // current ISA.
	    b.order (pid.getIsa ().byteOrder);
	    b.wordSize (pid.getIsa ().wordSize);
	    // Finally parse the AuxV
	    auxv = Auxv.parse (b);
	}
	return auxv;
    }
}
