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

import frysk.event.EventLoop;
import frysk.event.SignalEvent;
import frysk.sys.Ptrace;
import frysk.sys.Wait;
import frysk.sys.Sig;
import frysk.sys.Pid;
import frysk.sys.proc.Stat;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import frysk.sys.proc.ScanDir;

/**
 * A Linux Host.
 */

public class LinuxHost
    extends Host
{
    EventLoop eventLoop;
    /**
     * Construct an instance of the LinuxHost that uses the specified
     * eventLoop.
     */
    LinuxHost (EventLoop eventLoop)
    {
	this.eventLoop = eventLoop;
	eventLoop.add (new PollWaitOnSigChld ());
    }

    /**
     * Either add or update a process, however, before doing that
     * determine the parent and ensure that it has been updated.
     */
    private class ProcChanges
    {
	/**
	 * ADDED accumulates all the tasks added as things are
	 * updated.  */
	List added = new LinkedList ();
	/**
	 * REMOVED starts with the full list of processes and then
	 * works backwards removing any that are processed, by the end
	 * it contains processes that no longer exist.
	 */ 
	HashMap removed = (HashMap) ((HashMap)procPool).clone ();
	/**
	 * Update PROCID, either adding it 
	 */
	Proc update (int pid)
	{
	    ProcId procId = new ProcId (pid);
	    Proc proc = (Proc) procPool.get (procId);
	    if (proc == null) {
		// New, unknown process. Try to find both the process
		// and its parent.  In the case of a daemon process, a
		// second attempt may be needed.
		Stat stat = new Stat ();
		Proc parent = null;
		int attempt = 0;
		while (true) {
		    // Should take no more than two attempts - one for
		    // a normal process, and one for a daemon.
		    if (attempt++ >= 2)
			return null;
		    // Scan in the process's stat file.  Of course, if
		    // the stat file disappeared indicating that the
		    // process exited, return NULL.
		    if (!stat.refresh (procId.id))
			return null;
		    // Find the parent, every process, except process
		    // 1, has a parent.
		    if (pid <= 1)
			break;
		    parent = update (stat.ppid);
		    if (parent != null)
			break;
		}
		// .. and then add this process.
		proc = new LinuxProc (LinuxHost.this, parent, procId, stat);
		added.add (proc);
	    }
	    else if (removed.get (procId) != null) {
		// Process 1 never gets a [new] parent.
		if (pid > 1) {
		    Stat stat = ((LinuxProc)proc).stat;
		    // An existing process that hasn't yet been
		    // updated.  Still need check that its parent
		    // didn't change (assuming there is one).
		    if (!stat.refresh (pid))
			// Oops, just disappeared.
			return null;
		    Proc oldParent = proc.getParent ();
		    if (oldParent.getPid () != stat.ppid) {
			// Transfer ownership
			Proc newParent = update (stat.ppid);
			oldParent.remove (proc);
			proc.parent = newParent;
			newParent.add (proc);
		    }
		}
		removed.remove (procId);
	    }
	    return proc;
	}
    }

    void sendRefresh (boolean refreshAll)
    {
	final ProcChanges procChanges = new ProcChanges ();
	ScanDir scanDir = new ScanDir ()
	    {
		ProcChanges changes = procChanges;
		public void process (int pid)
		{
		    changes.update (pid);
		}
	    };
	scanDir.refresh ();
	if (refreshAll) {
	    // Changes individual process.
	    for (Iterator i = procPool.values ().iterator(); i.hasNext (); ) {
		LinuxProc proc = (LinuxProc) i.next ();
		proc.sendRefresh ();
	    }
	}
	// Tell each process that no longer exists that it has been
	// destroyed.
	for (Iterator i = procChanges.removed.values().iterator();
	     i.hasNext();) {
	    Proc proc = (Proc) i.next ();
	    // XXX: Should there be a ProcEvent.schedule(), instead of
	    // Manager .eventLoop .appendEvent for injecting the event
	    // into the event loop?
	    proc.performRemoval ();
	    remove (proc);
	}
    }

    void sendCreateAttachedProc (boolean running, String in, String out,
				 String err, String[] args)
    {
	int pid = Ptrace.child (in, out, err, args);
	new LinuxProc (getSelf (), new ProcId (pid), running);
    }

    // When there's a SIGCHLD, poll the kernel's waitpid() queue
    // appending all the read events to event-queue as WaitEvents.
    // The are then later processed by the event-loop.  The two step
    // process ensures that the underlying event-loop doesn't suffer
    // starvation - some actions such as continue lead to further
    // waitpid events and those new events should only be processed
    // after all existing events have been handled.
    
    class PollWaitOnSigChld
	extends SignalEvent
    {
	PollWaitOnSigChld ()
	{
	    super (Sig.CHLD);
	}
	Wait.Observer waitObserver = new Wait.Observer ()
	    {
		public void cloneEvent (int pid, int clone)
		{
		    // Tell the task that it cloned, and the
		    // containing process that there is a new
		    // task.
		    TaskId taskId = new TaskId (pid);
		    TaskId cloneId = new TaskId (clone);
		    eventLoop.add (new TaskEvent.Cloned (taskId, cloneId));
		    eventLoop.add (new ProcEvent.TaskCloned (taskId, cloneId));
		}
		public void forkEvent (int pid, int child)
		{
		    // Notify both the forking task, and the
		    // containing process that a fork occured.
		    TaskId taskId = new TaskId (pid);
		    ProcId childId = new ProcId (child);
		    eventLoop.add (new TaskEvent.Forked (taskId, childId));
		    eventLoop.add (new ProcEvent.TaskForked (taskId, childId));
		}
		public void exitEvent (int pid, int status)
		{
		    eventLoop.add (new TaskEvent.Exiting (new TaskId (pid),
							  status));
		}
		public void execEvent (int pid)
		{
		    eventLoop.add (new TaskEvent.Execed (new TaskId (pid)));
		}
		public void disappeared (int pid)
		{
		    eventLoop.add (new TaskEvent.Zombied (new TaskId (pid)));
		}
		public void syscallEvent (int pid)
		{
		    eventLoop.add (new TaskEvent.Syscall (new TaskId (pid)));
		}
		public void stopped (int pid, int sig)
		{
		    switch (sig) {
		    case Sig.STOP:
			eventLoop.add (new TaskEvent.Stopped (new TaskId (pid)));
			break;
		    case Sig.TRAP:
			eventLoop.add (new TaskEvent.Trapped (new TaskId (pid)));
			break;
		    default:
			eventLoop.add (new TaskEvent.Signaled (new TaskId (pid), sig));
			break;
		    }
		}
		public void exited (int pid, int status, boolean coreDumped)
		{
		    eventLoop.add (new TaskEvent.Exited (new TaskId (pid),
							 status));
		}
		public void terminated (int pid, int signal, boolean coreDumped)
		{
		    eventLoop.add (new TaskEvent.Terminated (new TaskId (pid),
							     signal));
		}
	    };
	public final void execute ()
	{
	    Wait.waitAllNoHang (waitObserver);
	}
    }

    public Proc getSelf ()
    {
	if (self == null) {
	    ProcChanges procChanges = new ProcChanges ();
	    self = procChanges.update (Pid.get ());
	}
	return self;
    }
    private Proc self;
}
