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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.event.Event;

/**
 * A UNIX Process, containing tasks, memory, ...
 */

public abstract class Proc
{
    protected static final Logger logger = Logger.getLogger ("frysk");//.proc");
    final ProcId id;
    public ProcId getId ()
    {
	return id;
    }
    
    /**
     * If known, due to the tracing of a fork, the Task that created
     * this process.
     */
    final Task creator;

    Proc parent;
    public Proc getParent ()
    {
	// XXX: This needs to be made on-demand.
    	return this.parent;
    }

    final Host host;
    public Host getHost ()
    {
	return host;
    }

    public int getPid ()
    {
	return id.hashCode ();
    }

    /**
     * Return the basename of the program that this process is
     * running.
     */
    public String getCommand ()
    {
	command = sendrecCommand ();
	return command;
    }
    private String command;
    protected abstract String sendrecCommand ();

    /**
     * Return the full path of the program that this process is
     * running.
     */
    public String getExe ()
    {
	exe = sendrecExe ();
	return exe;
    }
    private String exe;
    protected abstract String sendrecExe ();

    /**
     * Return the UID of the Proc.
     */
    public int  getUID()
    {
	uid = sendrecUID ();
	return uid;
    }
    protected abstract int sendrecUID ();
    private int uid;

    /**
     * Return the GID of the Proc.
     */
     public int getGID()
     {
        gid = sendrecGID();
        return gid;
     }     
     protected abstract int sendrecGID ();
     private int gid;

     /**
      * 
      * @return The main task for this process
      */
     public Task getMainTask(){
    	 return this.host.get(new TaskId(this.getPid()));
     }
     
    /**
     * Return the Proc's command line argument list
     */
    public String[] getCmdLine ()
    {
	argv = sendrecCmdLine ();
	return argv;
    }
    protected abstract String[] sendrecCmdLine ();
    private String[] argv;

    final BreakpointAddresses breakpoints;

    /**
     * Create a new Proc skeleton.  Since PARENT could be NULL,
     * explicitly specify the HOST.
     */
    private Proc (ProcId id, Proc parent, Host host, Task creator)
    {
	this.host = host;
	this.id = id;
	this.parent = parent;
	this.creator = creator;
	this.breakpoints = new BreakpointAddresses(this);
	// Keep parent informed.
	if (parent != null)
	    parent.add (this);
	// Keep host informed.
	host.add (this);
    }
    /**
     * Create a new, unattached, running, Proc.  Since PARENT could be
     * NULL, explicitly specify the HOST.
     */
    protected Proc (Host host, Proc parent, ProcId id)
    {
	this (id, parent, host, null);
	newState = ProcState.initial (this, false);
	logger.log (Level.FINEST, "{0} new - create unattached running proc\n",
		    this); 
    }
    /**
     * Create a new, attached, running, process forked by Task.  For
     * the moment assume that the process will be immediatly detached;
     * if this isn't the case the task, once it has been created, will
     * ram through an attached observer.
     *
     * Note the chicken-egg problem here: to add the initial
     * observation the Proc needs the Task (which has the Observable).
     * Conversely, for a Task, while it has the Observable, it doesn't
     * have the containing proc.
     */
    protected Proc (Task task, ProcId forkId)
    {
	this (forkId, task.proc, task.proc.host, task);
	newState = ProcState.initial (this, true);
	logger.log (Level.FINE, "{0} new - create attached running proc\n",
		    this); 
    }

    abstract void sendRefresh ();

    /**
     * The current state of this Proc, during a state transition
     * newState is null.
     */
    private ProcState oldState;
    private ProcState newState;
    /**
     * Return the current state.
     */
    ProcState getState ()
    {
	if (newState != null)
	    return newState;
	else
	    return oldState;
    }
    /**
     * Return the current state while at the same time marking that
     * the state is in flux.  If a second attempt to change state
     * occurs before the current state transition has completed,
     * barf.  XXX: Bit of a hack, but at least this prevents state
     * transition code attempting a second recursive state transition.
     */
    private ProcState oldState ()
    {
	if (newState == null)
	    throw new RuntimeException (this + " double state transition");
	oldState = newState;
	newState = null;
	return oldState;
    }

  /**
   * Request that the Proc be forcefully detached. Quickly.
   */
  public void requestAbandon ()
  {
    logger.log(Level.FINE, "{0} abandon", this);
    performDetach();
    observations.clear();
  }
  
  /**
   * Request that the Proc be forcefully detached.
   * Upon detach run the given event.  
   * @param e The event to run upon successfull detach.
   */
  public void requestAbandonAndRunEvent(final Event e)
  {
    logger.log(Level.FINE, "{0} abandonAndRunEvent", this);
    performDetach();
    observations.clear();
    observableDetached.addObserver(new Observer()
    {

      public void update (Observable o, Object arg)
      {
        Manager.eventLoop.add(e);
      }
    });
  }

    /**
     * Request that the Proc's task list be refreshed using system
     * tables.
     */
    public void requestRefresh ()
    {
	logger.log (Level.FINE, "{0} requestRefresh\n", this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		public void execute ()
		{
		    newState = oldState ().handleRefresh (Proc.this);
		}
	    });
    }
    /**
     * (Internal) Tell the process that is no longer listed in the
     * system table remove itself.
     */
    void performRemoval ()
    {
	logger.log (Level.FINEST, "{0} performRemoval -- no longer in /proc\n",
		    this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		public void execute ()
		{
		    newState = oldState ().handleRemoval (Proc.this);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its attach.
     */
    void performTaskAttachCompleted (final Task theTask)
    {
	logger.log (Level.FINE, "{0} performTaskAttachCompleted\n", this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Task task = theTask;
		public void execute ()
		{
		    newState = oldState ().handleTaskAttachCompleted (Proc.this,
								      task);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its detach.
     */
    void performTaskDetachCompleted (final Task theTask)
    {
	logger.log (Level.FINE, "{0} performTaskDetachCompleted\n", this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Task task = theTask;
		public void execute ()
		{
		    newState = oldState ().handleTaskDetachCompleted (Proc.this,
								      task);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its detach.
     */
    void performTaskDetachCompleted (final Task theTask, final Task theClone)
    {
	logger.log (Level.FINE, "{0} performTaskDetachCompleted/clone\n",
		    this); 
	Manager.eventLoop.add (new ProcEvent ()
	    {
		Task task = theTask;
		Task clone = theClone;
		public void execute ()
		{
		    newState = oldState ().handleTaskDetachCompleted
			(Proc.this, task, clone);
		}
	    });
    }

  void performDetach()
  {
    logger.log(Level.FINE, "{0} performDetach\n", this);
    Manager.eventLoop.add(new ProcEvent()
    {
      public void execute ()
      {
        newState = oldState().handleDetach(Proc.this);
      }
    });
  }
  
    /**
     * The set of observations that currently apply to this task.
     */
    private Set observations = new HashSet ();
    
    public boolean addObservation(Object o)
    {
      return observations.add(o);
    }

    public boolean removeObservation (Object o)
    {
      return observations.remove(o);
    }
    
    public int observationsSize()
    {
      return observations.size();
    }
    
    public Iterator observationsIterator()
    {
      return observations.iterator();
    }
    
    public void requestUnblock(TaskObserver observerArg)
    {
      Iterator iter = getTasks().iterator();
      while (iter.hasNext())
        {
          Task task = (Task) iter.next();
          task.requestUnblock(observerArg);
        }
    }
    
    /**
     * (internal) Tell the process to add the specified Observation,
     * attaching the process if necessary.
     */
    void handleAddObservation (TaskObservation observation)
    {
	newState = oldState ().handleAddObservation (this, observation);
    }

    /**
     * (Internal) Tell the process to add the specified Observation,
     * attaching to the process if necessary.
     */
    void requestAddObserver (Task task,
			     TaskObservable observable,
			     TaskObserver observer)
    {
	logger.log (Level.FINE, "{0} requestAddObservation\n", this); 
	Manager.eventLoop.add (new TaskObservation (task, observable, observer,
						    true)
	    {
		public void execute ()
		{
		    handleAddObservation (this);
		}
	    });
    }
    
    /**
     * Class describing the action to take on the suspended Task before
     * adding or deleting a Syscall observer.
     */
    final class SyscallAction implements Runnable
    {
      private final Task task;
      private final boolean addition;
      
      SyscallAction(Task task, boolean addition)
      {
	this.task = task;
	this.addition = addition;
      }

      public void run()
      {
	int syscallobs = task.syscallObservers.numberOfObservers();
	if (addition)
	  {
	    if (syscallobs == 0)
	      task.startTracingSyscalls();
	  }
	else
	  {
	    if (syscallobs == 0)
	      task.stopTracingSyscalls();
	  }
      }
    }

    /**
     * (Internal) Tell the process to add the specified Observation,
     * attaching to the process if necessary.
     * Adds a syscallObserver which changes the task to syscall
     * tracing mode of necessary.
     */
    void requestAddSyscallObserver (final Task task,
			     TaskObservable observable,
			     TaskObserver observer)
    {
	logger.log (Level.FINE, "{0} requestAddSyscallObserver\n", this); 
        SyscallAction sa = new SyscallAction(task, true);
        TaskObservation to = new TaskObservation(task, observable,
						 observer, sa, true)
	    {
		public void execute ()
		{
		    handleAddObservation (this);
		}

	        public boolean needsSuspendedAction()
	        {
		  return task.syscallObservers.numberOfObservers() == 0;
		}
	    };
	Manager.eventLoop.add(to);
    }
    
    /**
     * (Internal) Tell the process to delete the specified
     * Observation, detaching from the process if necessary.
     * Removes a syscallObserver exiting the task from syscall tracing
     * mode of necessary.
     */
    void requestDeleteObserver (Task task,
				TaskObservable observable,
				TaskObserver observer)
    {
        Manager.eventLoop.add (new TaskObservation (task, observable, observer,
						    false)
	    {
		public void execute ()
		{
		    newState = oldState ().handleDeleteObservation
			(Proc.this, this);
		}
	    });
    }

    /**
     * (Internal) Tell the process to delete the specified
     * Observation, detaching from the process if necessary.
     */
    void requestDeleteSyscallObserver (final Task task,
				TaskObservable observable,
				TaskObserver observer)
    {
	logger.log (Level.FINE, "{0} requestDeleteSyscallObserver\n", this); 
        SyscallAction sa = new SyscallAction(task, false);
        TaskObservation to = new TaskObservation(task, observable,
						 observer, sa, false)
	    {
		public void execute ()
		{
		    newState = oldState ().handleDeleteObservation
			(Proc.this, this);
		}

	        public boolean needsSuspendedAction()
	        {
		  return task.syscallObservers.numberOfObservers() == 1;
		}
	    };
	Manager.eventLoop.add(to);
    }

    /**
     * Class describing the action to take on the suspended Task before
     * adding or deleting a Code observer.
     */
    final class BreakpointAction implements Runnable
    {
      private final TaskObserver.Code code;
      private final Task task;
      private final long address;
      private final boolean addition;
      
      BreakpointAction(TaskObserver.Code code,
		       Task task,
		       long address,
		       boolean addition)
      {
	this.code = code;
	this.task = task;
	this.address = address;
	this.addition = addition;
      }

      public void run()
      {
	if (addition)
	  {
	    boolean mustInstall = breakpoints.addBreakpoint(code, address);
	    if (mustInstall)
	      {
		Breakpoint breakpoint;
		breakpoint = Breakpoint.create(address, Proc.this);
		breakpoint.install(task);
	      }
	  }
	else
	  {
	    boolean mustRemove = breakpoints.removeBreakpoint(code, address);
	    if (mustRemove)
	      {
		Breakpoint breakpoint;
		breakpoint = Breakpoint.create(address, Proc.this);
		breakpoint.remove(task);
	      }
	  }
      }
    }

    /**
     * (Internal) Tell the process to add the specified Code Observation,
     * attaching to the process if necessary.
     * Adds a TaskCodeObservation to the eventloop which instructs the
     * task to install the breakpoint if necessary.
     */
    void requestAddCodeObserver (Task task,
				 TaskObservable observable,
				 TaskObserver.Code observer,
				 final long address)
    {
	logger.log (Level.FINE, "{0} requestAddCodeObserver\n", this); 
	BreakpointAction bpa = new BreakpointAction(observer, task, address,
						    true);
	TaskObservation to;
	to = new TaskObservation(task, observable, observer, bpa, true)
	  {
	    public void execute ()
	    {
	      handleAddObservation (this);
	    }

	    public boolean needsSuspendedAction()
	    {
	      return breakpoints.getCodeObservers(address) == null;
	    }
	  };
	Manager.eventLoop.add(to);
    }
    
    /**
     * (Internal) Tell the process to delete the specified
     * Code Observation, detaching from the process if necessary.
     */
    void requestDeleteCodeObserver (Task task,
				    TaskObservable observable,
				    TaskObserver.Code observer,
				    final long address)
    {
        logger.log (Level.FINE, "{0} requestDeleteCodeObserver\n", this); 
	BreakpointAction bpa = new BreakpointAction(observer, task, address,
						    false);
	TaskObservation to;
	to = new TaskObservation(task, observable, observer, bpa, false)
	{
	  public void execute()
	  {
	    newState = oldState().handleDeleteObservation(Proc.this, this);
	  }

	  public boolean needsSuspendedAction()
	  {
	    return breakpoints.getCodeObservers(address).size() == 1;
	  }
	};

      Manager.eventLoop.add(to);
    }


    /**
     * Class describing the action to take on the suspended Task
     * before adding or deleting an Instruction observer. No
     * particular actions are needed, but we must make sure the Task
     * is suspended.
     */
    final static class InstructionAction implements Runnable
    {
      public void run()
      {
	// There is nothing in particular we need to do.  We just want
	// to make sure the Task is stopped so we can send it a step
	// instruction or, when deleted, start resuming the process
	// normally.

	// We do want an explicit updateExecuted() call, after adding
	// the observer, but while still suspended. This is done by
	// overriding the add() method in the TaskObservation
	// below. No such action is required on deletion.
      }
    }

    /**
     * (Internal) Tell the process to add the specified Instruction
     * Observation, attaching and/or suspending the process if
     * necessary.  As soon as the observation is added and the task
     * isn't blocked it will inform the Instruction observer of every
     * step of the task.
     */
    void requestAddInstructionObserver (final Task task,
					TaskObservable observable,
					TaskObserver.Instruction observer)
    {
      logger.log (Level.FINE, "{0} requestAddInstructionObserver\n", this); 
      TaskObservation to;
      InstructionAction ia = new InstructionAction();
      to = new TaskObservation(task, observable, observer, ia, true)
	{
	  public void execute ()
	  {
	    handleAddObservation (this);
	  }
	  
	  public boolean needsSuspendedAction()
	  {
	    return task.instructionObservers.numberOfObservers() == 0;
	  }

	  // Makes sure that the observer is properly added and then,
	  // while the Task is still suspended, updateExecuted() is
	  // called. Giving the observer a chance to inspect and
	  // possibly block the Task.
	  public void add()
	  {
	    super.add();
	    TaskObserver.Instruction i = (TaskObserver.Instruction) observer;
	    if (i.updateExecuted(task) == Action.BLOCK)
	      task.blockers.add(observer);
	  }
	};
      Manager.eventLoop.add(to);
    }
    
    /**
     * (Internal) Tell the process to delete the specified Instruction
     * Observation, detaching and/or suspending from the process if
     * necessary.
     */
    void requestDeleteInstructionObserver (final Task task,
					   TaskObservable observable,
					   TaskObserver.Instruction observer)
    {
      logger.log (Level.FINE, "{0} requestDeleteInstructionObserver\n", this); 
      TaskObservation to;
      InstructionAction ia = new InstructionAction();
      to = new TaskObservation(task, observable, observer, ia, false)
	{
	  public void execute ()
	  {
	    newState = oldState().handleDeleteObservation(Proc.this, this);
	  }
	  
	  public boolean needsSuspendedAction()
	  {
	    return task.instructionObservers.numberOfObservers() == 1;
	  }
	};
      Manager.eventLoop.add(to);
    }

    /**
     * Table of this processes child processes.
     */
    private Set childPool = new HashSet ();
    /**
     * Add Proc as a new child
     */
    void add (Proc child)
    {
	logger.log (Level.FINEST, "{0} add(Proc) -- a child process\n", this); 
	childPool.add (child);
    }
    /**
     * Remove Proc from this processes children.
     */
    void remove (Proc child)
    {
	logger.log (Level.FINEST, "{0} remove(Proc) -- a child process\n", this); 
	childPool.remove (child);
    }
    /**
     * Get the children as an array.
     */
    public LinkedList getChildren ()
    {
	return new LinkedList (childPool);
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
	    logger.log (Level.FINE, "{0} notify -- all observers\n", o); 
	    setChanged ();
	    notifyObservers (o);
	}
    }

    /**
     * Pool of tasks belonging to this Proc.
     */
    Map taskPool = new HashMap ();
    /**
     * Add the Task to this Proc.
     */
    void add (Task task)
    {
	taskPool.put (task.id, task);
	host.observableTaskAddedXXX.notify (task);
    }
    /**
     * Remove Task from this Proc.
     */
    void remove (Task task)
    {
	logger.log (Level.FINEST, "{0} remove(Task) -- within this Proc\n",
		    this); 
	host.observableTaskRemovedXXX.notify (task);
	taskPool.remove (task.id);
	host.remove (task);
    }
    /**
     * Remove all but Task from this Proc.
     */
    void retain (Task task)
    {
	logger.log (Level.FINE, "{0} retain(Task) -- remove all but task\n",
		    this); 
	HashMap new_tasks = new HashMap();
	new_tasks = (HashMap)((HashMap)taskPool).clone ();
	new_tasks.values().remove( task);
	taskPool.values().removeAll (new_tasks.values());
	host.removeTasks (new_tasks.values());
    }
    
    /**
     * Return this Proc's Task's as a list.
     */
    public LinkedList getTasks ()
    {
	return new LinkedList (taskPool.values ());
    }

    /**
     * The Process Auxiliary Vector.
     */
    public Auxv[] getAuxv ()
    {
	if (auxv == null) {
	    auxv = sendrecAuxv ();
	}
	return auxv;
    }
    private Auxv[] auxv;
    /**
     * Extract the auxv from the inferior.
     */
    abstract Auxv[] sendrecAuxv ();

    /**
     * The process has transitioned to the attached state.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ObservableXXX observableAttached = new ObservableXXX ();

    /**
     * The process has transitioned to the detached.
     *
     * XXX: Should be made private and instead accessor methods added.
     * Should more formally define the observable and the event.
     */
    public ObservableXXX observableDetached = new ObservableXXX ();

    public String toString ()
    {
      if (newState != null) {
	return ("{" + super.toString ()
		+ ",pid=" + getPid ()
		+ ",state=" + getState ()       
		+ "}");
      }
      return ("{" + super.toString()
          + ",pid=" + getPid()
          + ",oldState=" + getState()
          + "}");
    }
}
