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

/**
 * A host machine.
 *
 * A HOST has processes which contain threads.
 */

package frysk.proc;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

public abstract class Host
{
    // Maintain a collection of all known Tasks.

    // There's no new task observer here.  It's the responsibility of
    // the PROC, and not the MANAGER, to notify OBSERVERs of new
    // THREAD events.  That way its possible for the client to observe
    // things on a per-PROC basis.

    Map taskPool = new HashMap ();
    void add (Task task)
    {
	taskPool.put (task.id, task);
    }
    void remove (Task task)
    {
	taskPool.remove (task.id);
    }
    void removeTasks (Collection c)
    {
	taskPool.values().removeAll (c);
    }
    Task get (TaskId id)
    {
	return (Task) taskPool.get (id);
    }

	
    // Maintain a Collection of all known (live) PROCes.

    protected Map procPool = new HashMap ();
    void add (Proc proc)
    {
	Manager.procDiscovered.notify (proc);
	procAdded.notify (proc);
	procPool.put (proc.id, proc);
    }
    void remove (Proc proc)
    {
	procPool.remove (proc.id);
	Manager.procRemoved.notify (proc);
	procRemoved.notify (proc);
    }
    public Iterator getProcIterator ()
    {
	return procPool.values ().iterator ();
    }
    public Proc getProc (ProcId id)
    {
	return (Proc) procPool.get (id);
    }

    // Refresh the list of processes.
    abstract void sendRefresh (boolean refreshAll);
    abstract void sendCreateProc (String stdin, String stdout,
				  String stderr, String[] args);
    abstract void sendAttachProc (ProcId id);

    protected HostState state = HostState.running;

    /**
     * Request that the Host scan the system's process tables
     * refreshing the internal structure to match.  Optionally refresh
     * each processes task list.
     */
    public void requestRefresh (boolean refreshAll)
    {
	Manager.eventLoop.appendEvent
	    (new HostEvent.RequestRefresh (this, refreshAll));
    }
    /**
     * Request that the Host scan the system's process tables
     * refreshing the internal structures to match.
     */
    public final void requestRefresh ()
    {
	requestRefresh (false);
    }

    /**
     * Request that an attached, but running process be created on the
     * host.
     */
    public void requestCreateProc (String stdin, String stdout,
				   String stderr, String[] args)
    {
	Manager.eventLoop.appendEvent
	    (new HostEvent.RequestCreateProc (this, stdin, stdout, stderr,
					      args));
    }
    public final void requestCreateProc (String[] args)
    {
	requestCreateProc (null, null, null, args);
    }

    /**
     * Request that the process be attached.
     */
    public void requestAttachProc (ProcId id)
    {
	Manager.eventLoop.appendEvent
	    (new HostEvent.RequestAttachProc (this, id));
    }


    /**
     * A process has been added.  Possible reasons include a process
     * referesh, and a fork.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public Manager.ProcObservable procAdded = new Manager.ProcObservable ();
    /*
     * An existing process has been removed.  Possible reasons include
     * that the process is no longer listed in the system process
     * tables (and presumably exited).
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public Manager.ProcObservable procRemoved = new Manager.ProcObservable ();
}
