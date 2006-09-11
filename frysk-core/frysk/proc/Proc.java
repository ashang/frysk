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

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    /**
     * The set of observations that currently apply to this task.
     */
    Set observations = new HashSet ();

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
	Manager.eventLoop.add (new TaskObservation (task, observable, observer)
	    {
		public void execute ()
		{
		    handleAddObservation (this);
		}
	    });
    }
    
    /**
     * (Internal) Tell the process to add the specified Observation,
     * attaching to the process if necessary.
     * Adds a syscallObserver which changes the task to syscall
     * tracing mode of necessary.
     */
    void requestAddSyscallObserver (Task task,
			     TaskObservable observable,
			     TaskObserver observer)
    {
	logger.log (Level.FINE, "{0} requestAddSyscallObserver\n", this); 
	Manager.eventLoop.add (new TaskSyscallObservation (task, observable, observer)
	    {
		public void execute ()
		{
		    handleAddObservation (this);
		}
	    });
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
	Manager.eventLoop.add (new TaskObservation (task, observable, observer)
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
    void requestDeleteSyscallObserver (Task task,
				TaskObservable observable,
				TaskObserver observer)
    {
	Manager.eventLoop.add (new TaskSyscallObservation (task, observable, observer)
	    {
		public void execute ()
		{
		    newState = oldState ().handleDeleteObservation
			(Proc.this, this);
		}
	    });
    }

    /**
     * (Internal) Tell the process to add the specified Code Observation,
     * attaching to the process if necessary.
     * Adds a TaskCodeObservation to the eventloop which instructs the
     * task to install the breakpoint if necessary.
     */
    void requestAddCodeObserver (Task task,
				 TaskObservable observable,
				 TaskObserver.Code observer)
    {
	logger.log (Level.FINE, "{0} requestAddCodeObserver\n", this); 
	TaskCodeObservation tco;
	tco = new TaskCodeObservation(task, observable, observer)
	  {
	    public void execute ()
	    {
	      handleAddObservation (this);
	    }
	  };
	Manager.eventLoop.add(tco);
    }
    
    /**
     * (Internal) Tell the process to delete the specified
     * Code Observation, detaching from the process if necessary.
     */
    void requestDeleteCodeObserver (Task task,
				    TaskObservable observable,
				    TaskObserver.Code observer)
    {
      TaskCodeObservation tco;
      tco = new TaskCodeObservation(task, observable, observer)
	{
	  public void execute()
	  {
	    newState = oldState().handleDeleteObservation(Proc.this, this);
	  }
	};

      Manager.eventLoop.add(tco);
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
	return ("{" + super.toString ()
		+ ",pid=" + getPid ()
		+ ",state=" + getState ()
		+ "}");
    }
}
