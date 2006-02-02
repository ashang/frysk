// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Observable; // XXX: Temporary.
import java.util.logging.Level;
import java.util.logging.Logger;
import frysk.Config;

/**
 * A host machine.
 *
 * A HOST has processes which contain threads.  A HOST also has a
 * process that is running this code - frysk is self aware.
 */

public abstract class Host
{
    private static Logger logger = Logger.getLogger (Config.FRYSK_LOG_ID);
    /**
     * The host corresponds to a specific system.
     */
    Host ()
    {
	state = HostState.initial (this);
    }

    // Maintain a collection of all known Tasks.

    // There's no new task observer here.  It's the responsibility of
    // the PROC, and not the MANAGER, to notify OBSERVERs of new
    // THREAD events.  That way its possible for the client to observe
    // things on a per-PROC basis.

    Map taskPool = new HashMap ();
    void add (Task task)
    {
	logger.log (Level.FINE, "{0} add task\n", task);	
	taskPool.put (task.id, task);
    }
    void remove (Task task)
    {
	logger.log (Level.FINE, "{0} remove task\n", task);	
	taskPool.remove (task.id);
    }
    void removeTasks (Collection c)
    {
	logger.log (Level.FINE, "{0} removeTasks tasks\n", c);	
	taskPool.values().removeAll (c);
    }
    Task get (TaskId id)
    {
	logger.log (Level.FINE, "{0} get task\n", id);	
	return (Task) taskPool.get (id);
    }

	
    // Maintain a Collection of all known (live) PROCes.

    protected Map procPool = new HashMap ();
    void add (Proc proc)
    {
	logger.log (Level.FINE, "{0} add proc\n", proc);	
	observableProcAddedXXX.notify (proc);
	procPool.put (proc.id, proc);
    }
    void remove (Proc proc)
    {
	logger.log (Level.FINE, "{0} remove proc\n", proc); 
	procPool.remove (proc.id);
	observableProcRemovedXXX.notify (proc);
    }
    public Iterator getProcIterator ()
    {
	return procPool.values ().iterator ();
    }
    public Proc getProc (ProcId id)
    {
	logger.log (Level.FINE, "{0} getProc\n", id); 
	return (Proc) procPool.get (id);
    }

    // Refresh the list of processes.
    abstract void sendRefresh (boolean refreshAll);
    /**
     * Tell the host to create a running child process.
     */
    abstract void sendCreateAttachedProc (String stdin, String stdout,
					  String stderr, String[] args);

    /**
     * The current state of this host.
     */
    private HostState state;
    /**
     * Return the state represented as a simple string.
     */
    public String getStateString ()
    {
	return state.toString ();
    }

    /**
     * Request that the Host scan the system's process tables
     * refreshing the internal structure to match.  Optionally refresh
     * each processes task list.
     */
    public void requestRefreshXXX (final boolean refreshAllArg)
    {
	logger.log (Level.FINEST, "requestRefresh\n", ""); 
	Manager.eventLoop.add (new HostEvent ("RequestRefresh")
	    {
		boolean refreshAll = refreshAllArg;
		public void execute ()
		{
		    logger.log (Level.FINEST, "requestRefresh execute\n", ""); 
		    state = state.processRequestRefresh (Host.this,
							 refreshAll);
		}
	    });
    }

    /**
     * Request that a new attached and running process (with stdin,
     * stdout, and stderr are shared with this process) be created.
     */
    public final void requestCreateAttachedProcXXX (String[] args)
    {
	logger.log (Level.FINE, "requestCreateAttachedProcXXX\n", ""); 
	requestCreateAttachedProc (null, null, null, args);
    }
    /**
     * Request that a new attached and running process be created.
     */
    public final void requestCreateAttachedProc (final String stdinArg,
						 final String stdoutArg,
						 final String stderrArg,
						 final String[] argsArg)
    {
	logger.log (Level.FINE, "requestCreateAttachedProc\n", ""); 
	Manager.eventLoop.add (new HostEvent ("requestCreateAttachedProc")
	    {
		String stdin = stdinArg;
		String stdout = stdoutArg;
		String stderr = stderrArg;
		String[] args = argsArg;
		public void execute ()
		{
		    logger.log (Level.FINE, "requestCreateAttachedProc execute\n", ""); 
		    state = state.processRequestCreateAttachedProc
			(Host.this, stdin, stdout, stderr, args);
		}
	    });
    }

    /**
     * XXX: Temporary until .observable's are converted to
     * .requestAddObserver.
     */
    public class ObservableXXX
	extends Observable
    {
	void notify (Object o)
	{
	    setChanged ();
	    notifyObservers (o);
	}
    }

    /**
     * A process has been added.  Possible reasons include a process
     * referesh, and a fork.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ObservableXXX observableProcAddedXXX = new ObservableXXX ();

    /*
     * An existing process has been removed.  Possible reasons include
     * the process is no longer listed in the system process table
     * (and presumably has exited).
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ObservableXXX observableProcRemovedXXX = new ObservableXXX ();

    /**
     * Notify of the addition of a task attached to this process.
     *
     * This event indicates the presence of the task, not that it is
     * attached or detached.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ObservableXXX observableTaskAddedXXX = new ObservableXXX ();
    /**
     * Notify of the removal of a task attached to this process.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ObservableXXX observableTaskRemovedXXX = new ObservableXXX ();

    /**
     * Return the process corresponding to this running frysk instance
     * found on this host.
     */
    public Proc getSelf ()
    {
	if (self == null)
	    self = sendrecSelf ();
	return self;
    }
    /**
     * Pointer to <em>frysk</em> running on this Host.
     */
    private Proc self;
    /**
     * Extract a pointer to <em>frysk</em> running on this Host.
     */
    protected abstract Proc sendrecSelf ();

    /**
     * Print this.
     */
    public String toString ()
    {
	return ("{" + super.toString ()
		+ ",state=" + state
		+ "}");
    }
    
    /**
     * * Returns the name of the host 
     */
    public String getName()
    {
                 try {
                         return java.net.InetAddress.getLocalHost().getHostName();
                 } catch (UnknownHostException e) {
                         return "Unknown Host";
                 }
    }
}
