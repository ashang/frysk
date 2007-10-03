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

import frysk.event.Event;
import frysk.event.EventLoop;
import frysk.event.SignalEvent;
import frysk.sys.Ptrace;
import frysk.sys.Wait;
import frysk.sys.Sig;
import frysk.sys.Pid;
import frysk.sys.Tid;
import frysk.sys.proc.Stat;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import frysk.sys.proc.ProcBuilder;
import java.util.logging.Level;

/**
 * A Linux Host.
 */

public class LinuxHost
    extends Host
{
  EventLoop eventLoop;

  /**
   * Construct an instance of the LinuxHost that uses the specified eventLoop.
   */
  LinuxHost (EventLoop eventLoop)
  {
    this.eventLoop = eventLoop;
    eventLoop.add(new PollWaitOnSigChld());
  }

  /**
   * Either add or update a process, however, before doing that determine the
   * parent and ensure that it has been updated.
   */
  private class ProcChanges
  {
    /**
     * ADDED accumulates all the tasks added as things are updated.
     */
    List added = new LinkedList();

    /**
     * REMOVED starts with the full list of processes and then works backwards
     * removing any that are processed, by the end it contains processes that no
     * longer exist.
     */
    HashMap removed = (HashMap) ((HashMap) procPool).clone();

    /**
     * Update PROCID, either adding it
     */
    Proc update (int pid)
    {
      ProcId procId = new ProcId(pid);
      Proc proc = (Proc) procPool.get(procId);
      if (proc == null)
        {
          // New, unknown process. Try to find both the process
          // and its parent. In the case of a daemon process, a
          // second attempt may be needed.
          Stat stat = new Stat();
          Proc parent = null;
          int attempt = 0;
          while (true)
            {
              // Should take no more than two attempts - one for
              // a normal process, and one for a daemon.
              if (attempt++ >= 2)
                return null;
              // Scan in the process's stat file. Of course, if
              // the stat file disappeared indicating that the
              // process exited, return NULL.
              if (! stat.refresh(procId.id))
                return null;
              // Find the parent, every process, except process
              // 1, has a parent.
              if (pid <= 1)
                break;
              parent = update(stat.ppid);
              if (parent != null)
                break;
            }
          // .. and then add this process.
          proc = new LinuxProc(LinuxHost.this, parent, procId, stat);
          added.add(proc);
        }
      else if (removed.get(procId) != null)
        {
          // Process 1 never gets a [new] parent.
          if (pid > 1)
            {
              Stat stat = ((LinuxProc) proc).getStat();
              // An existing process that hasn't yet been
              // updated. Still need check that its parent
              // didn't change (assuming there is one).
              if (! stat.refresh(pid))
                // Oops, just disappeared.
                return null;
              Proc oldParent = proc.getParent();
              if (oldParent.getPid() != stat.ppid)
                {
                  // Transfer ownership
                  Proc newParent = update(stat.ppid);
                  oldParent.remove(proc);
                  proc.parent = newParent;
                  newParent.add(proc);
                }
            }
          removed.remove(procId);
        }
      return proc;
    }
  }

  void sendRefresh (boolean refreshAll)
  {
    // Iterate (build) the /proc tree, passing each found PID to
    // procChanges where it can update the /proc tree.
    final ProcChanges procChanges = new ProcChanges();
    ProcBuilder pidBuilder = new ProcBuilder()
    {
      public void buildId (int pid)
      {
        procChanges.update(pid);
      }
    };
    pidBuilder.construct();
    // If requested, tell each process that it too should refresh.
    if (refreshAll)
      {
        // Changes individual process.
        for (Iterator i = procPool.values().iterator(); i.hasNext();)
          {
            LinuxProc proc = (LinuxProc) i.next();
            proc.sendRefresh();
          }
      }
    // Tell each process that no longer exists that it has been
    // destroyed.
    for (Iterator i = procChanges.removed.values().iterator(); i.hasNext();)
      {
        Proc proc = (Proc) i.next();
        // XXX: Should there be a ProcEvent.schedule(), instead of
        // Manager .eventLoop .appendEvent for injecting the event
        // into the event loop?
        proc.performRemoval();
        remove(proc);
      }
  }
  
  void sendRefresh (boolean refreshAll, ProcId procId, FindProc finder)
  {
    //XXX: Should only refresh the pool with the given pid and possibly its parents.
    sendRefresh(refreshAll);
    
    if (procPool.containsKey(procId))
      finder.procFound(procId);
    else
      finder.procNotFound(procId, new RuntimeException("Couldn't find the proc" + procId));
  }

  /**
   * Create an attached process that is a child of this process (and this task).
   */
  void sendCreateAttachedProc (String in, String out, String err,
                               String[] args, TaskObserver.Attached attached)
  {
    logger.log(Level.FINE, "{0} sendCreateAttachedProc\n", this);
    int pid = Ptrace.child(in, out, err, args);
    // See if the Host knows about this task.
    TaskId myTaskId = new TaskId(Tid.get());
    Task myTask = get(myTaskId);
    try
      {
	if (myTask == null)
	  {
	    // If not, find this process and add this task to it.
	    Proc myProc = getSelf();
	    myTask = new LinuxTask(myProc, myTaskId);
	  }
	Proc proc = new LinuxProc(myTask, new ProcId(pid));
	new LinuxTask(proc, attached);
      }
    catch (TaskFileException e) 
      {
	// Not a problem; if we can't access the task's executable,
	// we're not interested.
      }
    catch (TaskException e)
      {
	throw new RuntimeException("got TaskException", e);
      }

  }

  // When there's a SIGCHLD, poll the kernel's waitpid() queue
  // appending all the read events to event-queue as WaitEvents.
  // The are then later processed by the event-loop. The two step
  // process ensures that the underlying event-loop doesn't suffer
  // starvation - some actions such as continue lead to further
  // waitpid events and those new events should only be processed
  // after all existing events have been handled.

  class PollWaitOnSigChld
      extends SignalEvent
  {
    PollWaitOnSigChld ()
    {
      super(Sig.CHLD);
      logger.log(Level.FINE, "{0} PollWaitOnSigChld\n", this);
    }

    Wait.Observer waitObserver = new Wait.Observer()
    {
	/**
	 * Maintain a list of fscked up kernel waitpid events - where
	 * an event for a pid arrives before it has been created - so
	 * that they can be re-processed when there's a fork.
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
	    fsckedOrderedKernelEvents.add (new Event ()
		{
		    final int pid = aPid;
		    final int signal = aSignal;
		    public void execute ()
		    {
			waitObserver.stopped (pid, signal);
		    }
		});
	    
	}

      // Hold onto a scratch ID; avoids overhead of
      // allocating a new taskId everytime a new event
      // arrives -- micro optimization..
      private TaskId scratchId = new TaskId(0);

      /**
       * Looks up and returns task corresponding to PID; logs reason for lookup.
       */
      Task getTask (int pid, String why)
      {
        scratchId.id = pid;
        logger.log(Level.FINE, why, scratchId);
        return get(scratchId);
      }

      public void cloneEvent (int pid, int clonePid)
      {
        // Find the task, create its new peer, and then
        // tell the task what happened. Note that hot on
        // the heels of this event is a clone.stopped
        // event, and the clone Task must be created
        // before that event arrives.
        Task task = getTask(pid, "{0} cloneEvent\n");
        // Create an attached, and running, clone of TASK.
        Task clone;
	try
	  {
	    clone = new LinuxTask(task, new TaskId(clonePid));
	  }
	catch (TaskException e)
	  {
	    throw new RuntimeException("caught TaskException", e);
	  }
        task.processClonedEvent(clone);
      }

      public void forkEvent (int pid, int childPid)
      {
        // Find the task, create the new process under it
        // (well ok the task's process) and then notify
        // the task of what happened. Note that hot on
        // the heels of this fork event is the child's
        // stop event, the fork Proc must be created
        // before that event arrives.
        Task task = getTask(pid, "{0} forkEvent\n");
        // Create an attached and running fork of TASK.
        ProcId forkId = new ProcId(childPid);
        Proc forkProc = new LinuxProc(task, forkId);
        // The main task.
        Task forkTask;
	try
	  {
	    forkTask = new LinuxTask(forkProc, (TaskObserver.Attached) null);
	  }
	catch (TaskException e)
	  {
	    throw new RuntimeException("caught TaskException", e);
	  }
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
        switch (sig)
          {
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
	  logger.log(Level.WARNING, "No task for pid {0}\n", new Integer(pid));
	else
	  task.processTerminatedEvent(signal, value);
      }
    };

    public final void execute ()
    {
      logger.log(Level.FINE, "{0} execute\n", this);
      Wait.waitAllNoHang(waitObserver);
    }
  }

  /**
   * Return a pointer to this <em>frysk</em> instance.
   */
  protected Proc sendrecSelf ()
  {
    ProcChanges procChanges = new ProcChanges();
    return procChanges.update(Pid.get());
  }
}
