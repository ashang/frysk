// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

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
