// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

import frysk.event.Event;
import frysk.sys.WaitBuilder;
import frysk.sys.Sig;
import java.util.List;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Handles wait events generated by the wait builder.
 */

class LinuxPtraceWaitBuilder
    implements WaitBuilder
{
    final LinuxPtraceHost host;
    LinuxPtraceWaitBuilder (LinuxPtraceHost host)
    {
	this.host = host;
    }

    static final Logger logger = Logger.getLogger("frysk");

    /**
     * Maintain a list of fscked up kernel waitpid events - where an
     * event for a pid arrives before it has been created - so that
     * they can be re-processed when there's a fork.
     */
    private List fsckedOrderedKernelEvents;
    /**
     * Run through the list of fscked up kernel waitpid events
     * attempting delivery of each in turn.
     */
    private void attemptDeliveringFsckedKernelEvents ()
    {
	if (fsckedOrderedKernelEvents != null) {
	    Event[] pending = (Event[]) fsckedOrderedKernelEvents.toArray (new Event[0]);
	    fsckedOrderedKernelEvents = null;
	    for (int i = 0; i < pending.length; i++) {
		pending[i].execute ();
	    }
	}
    }
    /**
     * Append the fscked-up stop event (it arrived when the task
     * didn't exist) to the fscked-up list.  Will get re-processed
     * later.
     */
    private void saveFsckedOrderedKernelStoppedEvent (final int aPid,
						      final int aSignal)
    {
	if (fsckedOrderedKernelEvents == null)
	    fsckedOrderedKernelEvents = new LinkedList ();
	Event rescheduled = new Event ()
	    {
		final int pid = aPid;
		final int signal = aSignal;
		public void execute ()
		{
		    LinuxPtraceWaitBuilder.this.stopped (pid, signal);
		}
		public String toString ()
		{
		    return "" + super.toString () + ",stopped,pid=" + pid;
		}
	    };
	logger.log (Level.FINE, "{0} rescheduled\n", rescheduled);
	fsckedOrderedKernelEvents.add (rescheduled);
    }
    
    // Hold onto a scratch ID; avoids overhead of allocating a new
    // taskId everytime a new event arrives -- micro optimization..
    private TaskId scratchId = new TaskId(0);
    
    /**
     * Looks up and returns task corresponding to PID; logs reason for
     * lookup.
     */
    Task getTask (int pid, String why)
    {
        scratchId.id = pid;
        logger.log(Level.FINE, why, scratchId);
        return host.get(scratchId);
    }
    
    public void cloneEvent (int pid, int clonePid)
    {
        // Find the task, create its new peer, and then tell the task
        // what happened. Note that hot on the heels of this event is
        // a clone.stopped event, and the clone Task must be created
        // before that event arrives.
        Task task = getTask(pid, "{0} cloneEvent\n");
        // Create an attached, and running, clone of TASK.
        Task clone;
	clone = new LinuxPtraceTask(task, new TaskId(clonePid));
        task.processClonedEvent(clone);
	attemptDeliveringFsckedKernelEvents ();
    }

    public void forkEvent (int pid, int childPid)
    {
        // Find the task, create the new process under it (well ok the
        // task's process) and then notify the task of what
        // happened. Note that hot on the heels of this fork event is
        // the child's stop event, the fork Proc must be created
        // before that event arrives.
        Task task = getTask(pid, "{0} forkEvent\n");
        // Create an attached and running fork of TASK.
        ProcId forkId = new ProcId(childPid);
        Proc forkProc = new LinuxPtraceProc(task, forkId);
        // The main task.
        Task forkTask;
	forkTask = new LinuxPtraceTask(forkProc, (TaskObserver.Attached) null);
        task.processForkedEvent(forkTask);
	attemptDeliveringFsckedKernelEvents ();
    }
    
    public void exitEvent (int pid, boolean signal, int value,
			   boolean coreDumped)
    {
        Task task = getTask(pid, "{0} exitEvent\n");
        task.processTerminatingEvent(signal, value);
    }
    
    public void execEvent (int pid)
    {
        Task task = getTask(pid, "{0} execEvent\n");
        task.processExecedEvent();
    }
    
    public void disappeared (int pid, Throwable w)
    {
        Task task = getTask(pid, "{0} disappeared\n");
        task.processDisappearedEvent(w);
    }
    
    public void syscallEvent (int pid)
    {
        Task task = getTask(pid, "{0} syscallEvent\n");
        task.processSyscalledEvent();
    }
    
    public void stopped (int pid, int sig)
    {
        Task task = getTask(pid, "{0} stopped\n");
	if (task == null) {
	    // If there's no Task corresponding to TID, assume that
	    // the kernel fscked up its event ordering - notifying of
	    // a child-stop before it notified us of a child-create -
	    // push the event onto a second queue where it can be
	    // processed after a fork.
	    saveFsckedOrderedKernelStoppedEvent (pid, sig);
	    return;
	}
        switch (sig) {
	case Sig.STOP_:
	    task.processStoppedEvent();
            break;
	case Sig.TRAP_:
            task.processTrappedEvent();
            break;
	default:
            task.processSignaledEvent(sig);
            break;
	}
    }
    
    public void terminated (int pid, boolean signal, int value,
			    boolean coreDumped)
    {
        Task task = getTask(pid, "{0} terminated\n");
	if (task == null)
	    logger.log(Level.WARNING, "No task for pid {0,number,integer}\n",
		       new Integer(pid));
	else
	    task.processTerminatedEvent(signal, value);
    }
}
