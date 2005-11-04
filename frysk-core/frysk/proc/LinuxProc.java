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

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import frysk.sys.proc.Stat;
import frysk.sys.proc.IdBuilder;
import frysk.sys.proc.AuxvBuilder;

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
     * Create a new, definitely attached, definitely running fork of
     * Task.
     */
    LinuxProc (Task task, ProcId forkId)
    {
	super (task, forkId);
    }
    void sendRefresh ()
    {
	// Compare this against the existing taskPool.  ADDED
	// accumulates any tasks added to the taskPool.  REMOVED,
	// starting with all known tasks has any existing tasks
	// removed, so that by the end it contains a set of
	// removed tasks.
	class TidBuilder
	    extends IdBuilder
	{
	    Map added = new HashMap ();
	    HashMap removed = (HashMap) ((HashMap)taskPool).clone ();
	    TaskId searchId = new TaskId ();
	    public void buildId (int tid)
	    {
		searchId.id = tid;
		if (removed.containsKey (searchId)) {
		    removed.remove (searchId);
		}
		else {
		    // Add the process (it currently isn't attached).
		    Task newTask = new LinuxTask (LinuxProc.this,
						  new TaskId (tid),
						  false);
		    added.put (newTask.id, newTask);
		}
	    }
	}
	TidBuilder tasks = new TidBuilder ();
	tasks.construct (id.id);
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

    void sendNewAttachedTask (TaskId id)
    {
	// XXX: Should be abstracted.
	new LinuxTask (this, id, true);
    }
    Auxv[] sendrecAuxv ()
    {
	class BuildAuxv
	    extends AuxvBuilder
	{
	    Auxv[] vec;
	    public void buildBuffer (byte[] auxv)
	    {
	    }
	    public void buildDimensions (int wordSize, boolean bigEndian,
					 int length)
	    {
		vec = new Auxv[length];
	    }
	    public void buildAuxiliary (int index, int type, long val)
	    {
		vec[index] = new Auxv (type, val);
	    }
	}
	BuildAuxv auxv = new BuildAuxv ();
	auxv.construct (getPid ());
	return auxv.vec;
    }
}
