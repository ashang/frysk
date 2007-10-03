// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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

import java.util.LinkedList;
import inua.eio.ByteBuffer;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Observer;
import java.util.Observable;

public abstract class Task
{
  protected static final Logger logger = Logger.getLogger(ProcLogger.LOGGER_ID);

  /**
   * If known, as a result of tracing clone or fork, the task that created this
   * task.
   */
  final Task creator;

  /**
   * Return the task's corresponding TaskId.
   */
  public final TaskId getTaskId ()
  {
    return id;
  }

  final TaskId id;

  /**
   * Return the task's process id.
   */
  public final int getTid ()
  {
    return id.id;
  }

  /**
   * Return the task's (derived) name
   */
  public final String getName ()
  {
    return "Task " + getTid();
  }

  /**
   * Returns this Task's Instruction Set Architecture.
   */
  public final Isa getIsa ()
  {
    if (isa == null)
      isa = sendrecIsa();
    return isa;
  }

  public final boolean hasIsa()
  {
    return (null != isa);
  }

  /**
   * This Task's Instruction Set Architecture.
   */
  private Isa isa;
 
  /**
   * Fetch this Task's Instruction Set Architecture.
   */
  protected abstract Isa sendrecIsa ();

  public final SyscallEventInfo getSyscallEventInfo ()
  {
    return ((SyscallEventDecoder)getIsa()).getSyscallEventInfo();
  }

  /**
   * Return the task's entry point address. This is the address of the first
   * instruction that the task will have executed. XXX: Not yet implemented.
   */
  public long getEntryPointAddress ()
  {
    return 0xdeadbeefL;
  }

  /**
   * Return the containing Proc.
   */
  public Proc getProc ()
  {
    return proc;
  }

  final Proc proc;

  /**
   * Create a new Task skeleton.
   */
  private Task (TaskId id, Proc proc, Task creator, TaskState state)
  {
    this.proc = proc;
    this.id = id;
    this.creator = creator;
    proc.add(this);
    proc.host.add(this);
    newState = state;
  }

  /**
   * Create a new unattached Task.
   */
  protected Task (Proc proc, TaskId id, TaskState state)
  {
    this(id, proc, null, state);
    logger.log(Level.FINEST, "{0} new -- create unattached\n", this);
  }

  /**
   * Create a new attached clone of Task.
   */
  protected Task (Task task, TaskId cloneId, TaskState state)
  {
    this(cloneId, task.proc, task, state);
    //newState = LinuxPtraceTaskState.clonedState(task.getState());
    logger.log(Level.FINE, "{0} new -- create attached clone\n", this);
  }

  /**
   * Create a new attached main Task of Proc. If Attached observer is specified
   * assume it should be attached, otherwize, assume that, as soon as the task
   * stops, it should be detached. Note the chicken-egg problem here: to add the
   * initial observation the Proc needs the Task (which has the Observable).
   * Conversely, for a Task, while it has the Observable, it doesn't have the
   * containing proc.
   */
  protected Task (Proc proc, TaskObserver.Attached attached, 
		 TaskState state)
  {
    this(new TaskId(proc.getPid()), proc, proc.creator, state);
    //newState = LinuxPtraceTaskState.mainState();
    if (attached != null)
      {
        TaskObservation ob = new TaskObservation(this, attachedObservers,
                                                 attached, true)
        {
          public void execute ()
          {
            throw new RuntimeException("oops!");
          }
        };
        proc.handleAddObservation(ob);
      }
  }

  // Send operation to corresponding underlying [kernel] task.  The
  // continue, syscall and step methods should sig_send and set/reset
  // step_send to indicate that last reques tot the Task was a single
  // step.
  protected abstract void sendContinue (int sig);

  protected abstract void sendSyscallContinue (int sig);

  protected abstract void sendStepInstruction (int sig);

  protected abstract void sendStop ();

  protected abstract void sendSetOptions ();

  protected abstract void sendAttach ();

  protected abstract void sendDetach (int sig);

  protected LinkedList queuedEvents = new LinkedList();

  /**
   * The state of this task. During a state transition newState is NULL.
   */
  private TaskState oldState;

  private TaskState newState;

  /**
   * Return the current state.
   */
  protected final TaskState getState ()
  {
    if (newState != null)
      return newState;
    else
      return oldState;
  }
  /**
   * Set the new state.
   */
  protected final void set (TaskState newState)
  {
    this.newState = newState;
  }

  /**
   * Return the current state while at the same time marking that the
   * state is in flux. If a second attempt to change state occurs
   * before the current state transition has completed, barf. XXX: Bit
   * of a hack, but at least this prevents state transition code
   * attempting a second recursive state transition.
   */
  protected TaskState oldState ()
  {
    if (newState == null)
      throw new RuntimeException(this + " double state transition");
    oldState = newState;
    newState = null;
    return oldState;
  }

  /**
   * (Internal) Add the specified observer to the observable.
   */
  void handleAddObservation(TaskObservation observation)
  {
    newState = oldState().handleAddObservation(this, observation);
  }

  /**
   * (Internal) Delete the specified observer from the observable.
   */
  void handleDeleteObservation(TaskObservation observation)
  {
    newState = oldState().handleDeleteObservation(this, observation);
  }

  /**
   * (Internal) Requesting that the task go (or resume execution).
   */
  void performContinue ()
  {
    newState = oldState().handleContinue(Task.this);
  }

  /**
   * (Internal) Tell the task to remove itself (it is no longer listed in the
   * system process table and, presumably, has exited).
   */
  void performRemoval ()
  {
    newState = oldState().handleRemoval(Task.this);
  }

  /**
   * (Internal) Tell the task to attach itself (if it isn't already). Notify the
   * containing process once the operation has been completed. The task is left
   * in the stopped state.
   */
  void performAttach ()
  {
    newState = oldState().handleAttach(Task.this);
  }

  /**
   * (Internal) Tell the task to detach itself (if it isn't already). Notify the
   * containing process once the operation has been processed; the task is
   * allowed to run free.
   * @param shouldRemoveObservers whether to remove the observers as well.
   */
  void performDetach (boolean shouldRemoveObservers)
  {
    newState = oldState().handleDetach(Task.this, shouldRemoveObservers);
  }

  /**
   *  (Internal) Request that all observers from this task be removed.
   *  Warning, should also be removed from the proc's observations.
  */
  void removeObservers()
  {
	  logger.log(Level.FINE, "{0} abandon", this);	 
		
	  attachedObservers.removeAllObservers();
	  clonedObservers.removeAllObservers();
      forkedObservers.removeAllObservers();
      terminatedObservers.removeAllObservers();
      terminatingObservers.removeAllObservers();
      execedObservers.removeAllObservers();
      syscallObservers.removeAllObservers();
      signaledObservers.removeAllObservers();
      instructionObservers.removeAllObservers();
      blockers.clear();
      pendingObservations.clear();
  }
  
  public class TaskEventObservable
      extends java.util.Observable
  {
    protected void notify (Object o)
    {
      setChanged();
      notifyObservers(o);
    }
  }

  /**
   * Return a summary of the task's state.
   */
  public String toString ()
  {
    return ("{" + super.toString() + ",pid=" + proc.getPid() + ",tid="
            + getTid() + ",state=" + getState() + "}");
  }

  /**
   * Set of interfaces currently blocking this task.
   */
  Set blockers = new HashSet();

  /**
   * Return the current set of blockers as an array. Useful when debugging.
   */
  public TaskObserver[] getBlockers ()
  {
    return (TaskObserver[]) blockers.toArray(new TaskObserver[0]);
  }

  /**
   * Request that the observer be removed from this tasks set of blockers; once
   * there are no blocking observers, this task resumes.
   */
  public void requestUnblock (final TaskObserver observerArg)
  {
    logger.log(Level.FINE, "{0} requestUnblock -- observer\n", this);
    Manager.eventLoop.add(new TaskEvent(this)
    {
      TaskObserver observer = observerArg;

      public void execute ()
      {
        newState = oldState().handleUnblock(task, observer);
      }
    });
  }

  /**
   * Set of Cloned observers.
   */
  private TaskObservable clonedObservers = new TaskObservable(this);

  /**
   * Add a TaskObserver.Cloned observer.
   */
  public void requestAddClonedObserver (TaskObserver.Cloned o)
  {
    logger.log(Level.FINE, "{0} requestAddClonedObserver\n", this);
    proc.requestAddObserver(this, clonedObservers, o);
  }

  /**
   * Delete a TaskObserver.Cloned observer.
   */
  public void requestDeleteClonedObserver (TaskObserver.Cloned o)
  {
    logger.log(Level.FINE, "{0} requestDeleteClonedObserver\n", this);
    proc.requestDeleteObserver(this, clonedObservers, o);
  }

  /**
   * Notify all cloned observers that this task cloned. Return the number of
   * blocking observers.
   */
  int notifyClonedParent (Task offspring)
  {
    for (Iterator i = clonedObservers.iterator(); i.hasNext();)
      {
        TaskObserver.Cloned observer = (TaskObserver.Cloned) i.next();
        if (observer.updateClonedParent(this, offspring) == Action.BLOCK)
          {
            blockers.add(observer);
          }
      }
    return blockers.size();
  }

  /**
   * Notify all cloned observers that this task cloned. Return the number of
   * blocking observers.
   */
  int notifyClonedOffspring ()
  {
    logger.log(Level.FINE, "{0} notifyClonedOffspring\n", this);
    for (Iterator i = creator.clonedObservers.iterator(); i.hasNext();)
      {
        TaskObserver.Cloned observer = (TaskObserver.Cloned) i.next();
        if (observer.updateClonedOffspring(creator, this) == Action.BLOCK)
          {
            blockers.add(observer);
          }
      }
    return blockers.size();
  }

  /**
   * Set of Attached observers.
   */
  private TaskObservable attachedObservers = new TaskObservable(this);

  /**
   * Add a TaskObserver.Attached observer.
   */
  public void requestAddAttachedObserver (TaskObserver.Attached o)
  {
    logger.log(Level.FINE, "{0} requestAddAttachedObserver\n", this);
    proc.requestAddObserver(this, attachedObservers, o);
  }

  /**
   * Delete a TaskObserver.Attached observer.
   */
  public void requestDeleteAttachedObserver (TaskObserver.Attached o)
  {
    logger.log(Level.FINE, "{0} requestDeleteAttachedObserver\n", this);
    proc.requestDeleteObserver(this, attachedObservers, o);
  }

  /**
   * Notify all Attached observers that this task attached. Return the number of
   * blocking observers.
   */
  int notifyAttached ()
  {
    logger.log(Level.FINE, "{0} notifyAttached\n", this);
    //Fill isa on attach.
    getIsa();
    for (Iterator i = attachedObservers.iterator(); i.hasNext();)
      {
        TaskObserver.Attached observer = (TaskObserver.Attached) i.next();
        if (observer.updateAttached(this) == Action.BLOCK)
          blockers.add(observer);
      }
    return blockers.size();
  }

  /**
   * Set of Forked observers.
   */
  private TaskObservable forkedObservers = new TaskObservable(this);

  /**
   * Add a TaskObserver.Forked observer.
   */
  public void requestAddForkedObserver (TaskObserver.Forked o)
  {
    logger.log(Level.FINE, "{0} requestAddForkedObserver\n", this);
    proc.requestAddObserver(this, forkedObservers, o);
  }

  /**
   * Delete a TaskObserver.Forked observer.
   */
  public void requestDeleteForkedObserver (TaskObserver.Forked o)
  {
    logger.log(Level.FINE, "{0} requestDeleteForkedObserver\n", this);
    proc.requestDeleteObserver(this, forkedObservers, o);
  }

  /**
   * Notify all Forked observers that this task forked. Return the number of
   * blocking observers.
   */
  int notifyForkedParent (Task offspring)
  {
    for (Iterator i = forkedObservers.iterator(); i.hasNext();)
      {
        TaskObserver.Forked observer = (TaskObserver.Forked) i.next();
        if (observer.updateForkedParent(this, offspring) == Action.BLOCK)
          {
            blockers.add(observer);
          }
      }
    return blockers.size();
  }

  /**
   * Notify all Forked observers that this task's new offspring, created using
   * fork, is sitting at the first instruction.
   */
  int notifyForkedOffspring ()
  {
    for (Iterator i = creator.forkedObservers.iterator(); i.hasNext();)
      {
        TaskObserver.Forked observer = (TaskObserver.Forked) i.next();
        if (observer.updateForkedOffspring(creator, this) == Action.BLOCK)
          {
            blockers.add(observer);
          }
      }
    return blockers.size();
  }

  /**
   * Set of Terminated observers.
   */
  private TaskObservable terminatedObservers = new TaskObservable(this);

  /**
   * Add a TaskObserver.Terminated observer.
   */
  public void requestAddTerminatedObserver (TaskObserver.Terminated o)
  {
    logger.log(Level.FINE, "{0} requestAddTerminatedObserver\n", this);
    proc.requestAddObserver(this, terminatedObservers, o);
  }

  /**
   * Delete a TaskObserver.Terminated observer.
   */
  public void requestDeleteTerminatedObserver (TaskObserver.Terminated o)
  {
    logger.log(Level.FINE, "{0} requestDeleteTerminatedObserver\n", this);
    proc.requestDeleteObserver(this, terminatedObservers, o);
  }

  /**
   * Notify all Terminated observers, of this Task's demise. Return the number
   * of blocking observers. (Does this make any sense?)
   */
  int notifyTerminated (boolean signal, int value)
  {
    logger.log(Level.FINE, "{0} notifyTerminated\n", this);
    for (Iterator i = terminatedObservers.iterator(); i.hasNext();)
      {
        TaskObserver.Terminated observer = (TaskObserver.Terminated) i.next();
        if (observer.updateTerminated(this, signal, value) == Action.BLOCK)
          {
            logger.log(Level.FINER,
                       "{0} notifyTerminated adding {1} to blockers\n",
                       new Object[] { this, observer });
            blockers.add(observer);
          }
      }
    return blockers.size();
  }

  /**
   * Set of Terminating observers.
   */
  private TaskObservable terminatingObservers = new TaskObservable(this);

  /**
   * Add TaskObserver.Terminating to the TaskObserver pool.
   */
  public void requestAddTerminatingObserver (TaskObserver.Terminating o)
  {
    logger.log(Level.FINE, "{0} requestAddTerminatingObserver\n", this);
    proc.requestAddObserver(this, terminatingObservers, o);
  }

  /**
   * Delete TaskObserver.Terminating.
   */
  public void requestDeleteTerminatingObserver (TaskObserver.Terminating o)
  {
    logger.log(Level.FINE, "{0} requestDeleteTerminatingObserver\n", this);
    proc.requestDeleteObserver(this, terminatingObservers, o);
  }

  /**
   * Notify all Terminating observers, of this Task's demise. Return the number
   * of blocking observers.
   */
  int notifyTerminating (boolean signal, int value)
  {
    for (Iterator i = terminatingObservers.iterator(); i.hasNext();)
      {
        TaskObserver.Terminating observer = (TaskObserver.Terminating) i.next();
        if (observer.updateTerminating(this, signal, value) == Action.BLOCK)
          blockers.add(observer);
      }
    return blockers.size();
  }

  /**
   * Set of Execed observers.
   */
  private TaskObservable execedObservers = new TaskObservable(this);

  /**
   * Add TaskObserver.Execed to the TaskObserver pool.
   */
  public void requestAddExecedObserver (TaskObserver.Execed o)
  {
    logger.log(Level.FINE, "{0} requestAddExecedObserver\n", this);
    proc.requestAddObserver(this, execedObservers, o);
  }

  /**
   * Delete TaskObserver.Execed.
   */
  public void requestDeleteExecedObserver (TaskObserver.Execed o)
  {
    logger.log(Level.FINE, "{0} requestDeleteExecedObserver\n", this);
    proc.requestDeleteObserver(this, execedObservers, o);
  }

  /**
   * Notify all Execed observers, of this Task's demise. Return the number of
   * blocking observers.
   */
  int notifyExeced ()
  {
    //Flush the isa in case it has changed between exec's.
    clearIsa();
    //XXX: When should the isa be rebuilt?
    for (Iterator i = execedObservers.iterator(); i.hasNext();)
      {
        TaskObserver.Execed observer = (TaskObserver.Execed) i.next();
        if (observer.updateExeced(this) == Action.BLOCK)
          blockers.add(observer);
      }
    return blockers.size();
  }

  /**
   * Set of Syscall observers. Checked in TaskState.
   */
  TaskObservable syscallObservers = new TaskObservable(this);

  /**
   * Add TaskObserver.Syscall to the TaskObserver pool.
   */
  public void requestAddSyscallObserver (TaskObserver.Syscall o)
  {
    logger.log(Level.FINE, "{0} requestAddSyscallObserver\n", this);
    proc.requestAddSyscallObserver(this, syscallObservers, o);
  }

  /**
   * Delete TaskObserver.Syscall.
   */
  public void requestDeleteSyscallObserver (TaskObserver.Syscall o)
  {
    proc.requestDeleteSyscallObserver(this, syscallObservers, o);
    logger.log(Level.FINE, "{0} requestDeleteSyscallObserver\n", this);
  }

  /**
   * Notify all Syscall observers of this Task's entry into a system call.
   * Return the number of blocking observers.
   */
  int notifySyscallEnter ()
  {
      logger.log(Level.FINE,
		 "{0} notifySyscallEnter {1}\n",
		 new Object[] 
		 { this,
		   new Integer(this.getSyscallEventInfo().number(this))
		 });
    for (Iterator i = syscallObservers.iterator(); i.hasNext();)
      {
        TaskObserver.Syscall observer = (TaskObserver.Syscall) i.next();
        if (observer.updateSyscallEnter(this) == Action.BLOCK)
          blockers.add(observer);
      }
    return blockers.size();
  }

  /**
   * Notify all Syscall observers of this Task's exit from a system call. Return
   * the number of blocking observers.
   */
  int notifySyscallExit ()
  {
      logger.log(Level.FINE,
		 "{0} notifySyscallExit {1}\n",
		 new Object[] 
		 { this,
		   new Integer(this.getSyscallEventInfo().number(this))
		 });
    for (Iterator i = syscallObservers.iterator(); i.hasNext();)
      {
        TaskObserver.Syscall observer = (TaskObserver.Syscall) i.next();
        if (observer.updateSyscallExit(this) == Action.BLOCK)
          blockers.add(observer);
      }
    return blockers.size();
  }

  /**
   * Set of Signaled observers.
   */
  private TaskObservable signaledObservers = new TaskObservable(this);

  /**
   * Add TaskObserver.Signaled to the TaskObserver pool.
   */
  public void requestAddSignaledObserver (TaskObserver.Signaled o)
  {
    logger.log(Level.FINE, "{0} requestAddSignaledObserver\n", this);
    proc.requestAddObserver(this, signaledObservers, o);
  }

  /**
   * Delete TaskObserver.Signaled.
   */
  public void requestDeleteSignaledObserver (TaskObserver.Signaled o)
  {
    logger.log(Level.FINE, "{0} requestDeleteSignaledObserver\n", this);
    proc.requestDeleteObserver(this, signaledObservers, o);
  }

  /**
   * Notify all Signaled observers of the signal. Return the number of blocking
   * observers.
   */
  int notifySignaled (int sig)
  {
    logger.log(Level.FINE, "{0} notifySignaled(int)\n", this);
    for (Iterator i = signaledObservers.iterator(); i.hasNext();)
      {
        TaskObserver.Signaled observer = (TaskObserver.Signaled) i.next();
        if (observer.updateSignaled(this, sig) == Action.BLOCK)
          blockers.add(observer);
      }
    return blockers.size();
  }

  private ByteBuffer memory;
  protected abstract ByteBuffer sendrecMemory();
  /**
   * Return the Task's memory.
   */
  public ByteBuffer getMemory ()
  {
    logger.log(Level.FINE, "{0} entering get memory {1}\n",new Object[] {this, memory});
    if (memory == null )
      memory = sendrecMemory();
    logger.log(Level.FINE, "{0} exiting get memory {1}\n", new Object[] {this, memory});
    return this.memory;
  }

  /**
   * Turns on systemcall entry and exit tracing
   */
  protected abstract void startTracingSyscalls ();

  /**
   * Turns off systemcall entry and exit tracing 
   */
  protected abstract void stopTracingSyscalls ();

  /**
   * Set of Code observers.
   */
  TaskObservable codeObservers = new TaskObservable(this);
  
  /**
   * Add TaskObserver.Code to the TaskObserver pool.
   */
  public void requestAddCodeObserver (TaskObserver.Code o, long a)
  {
    logger.log(Level.FINE, "{0} requestAddCodeObserver\n", this);
    proc.requestAddCodeObserver(this, codeObservers, o, a);
  }

  /**
   * Delete TaskObserver.Code for the TaskObserver pool.
   */
  public void requestDeleteCodeObserver (TaskObserver.Code o, long a)
  {
    logger.log(Level.FINE, "{0} requestDeleteCodeObserver\n", this);
    proc.requestDeleteCodeObserver(this, codeObservers, o, a);
  }
  
  /**
   * Whether we are currently stepping over a breakpoint.  Used in
   * the running task state when a trap event occurs after a step
   * has been issued. Null when no step is being performed.
   *
   * XXX: This variable belongs in the Linux/PTRACE state machine.
   */
  public Breakpoint steppingBreakpoint;

  /**
   * Whether the last request to the process was a step request.
   *
   * XXX: This should be a state in Linux/PTRACE state machine.
   */
  public boolean step_send;

  /**
   * The signal, or zero, send last to the task.
   *
   * XXX: This should be a state in Linux/PTRACE state machine.
   */
  public int sig_send;

  /**
   * When the last request to the process was a step request, whether
   * it was a request to step a sigreturn syscall.
   * Set by sendStepInstruction().
   *
   * XXX: This should be a state in Linux/PTRACE state machine.
   */
  public boolean syscall_sigret;

  /**
   * Notify all Code observers of the breakpoint. Return the number of
   * blocking observers or -1 if no Code observer were installed on this
   * address.
   */
  int notifyCodeBreakpoint (long address)
  {
    logger.log(Level.FINE, "{0} notifyCodeBreakpoint({1})\n",
	       new Object[] { this, Long.valueOf(address) });
    
    Collection observers = proc.breakpoints.getCodeObservers(address);
    if (observers == null)
      return -1;

    Iterator i = observers.iterator();
    while (i.hasNext())
      {
	TaskObserver.Code observer = (TaskObserver.Code) i.next();
	if (observer.updateHit(this, address) == Action.BLOCK)
	  blockers.add(observer);
      }
    return blockers.size();
  }

  /**
   * Set of Instruction observers.
   */
  TaskObservable instructionObservers = new TaskObservable(this);
  
  /**
   * Request the addition of a Instruction observer that will be
   * notified as soon as the task executes an instruction.
   * <code>o.updateExecuted</code> is called as soon as the Task
   * starts running again (is not blocked or stopped) and executes the
   * next instruction.
   */
  public void requestAddInstructionObserver(TaskObserver.Instruction o)
  {
    logger.log(Level.FINE, "{0} requestAddInstructionObserver\n", this);
    proc.requestAddInstructionObserver(this, instructionObservers, o);
  }

  /**
   * Delete TaskObserver.Instruction from the TaskObserver pool.
   */
  public void requestDeleteInstructionObserver (TaskObserver.Instruction o)
  {
    logger.log(Level.FINE, "{0} requestDeleteInstructionObserver\n", this);
    proc.requestDeleteInstructionObserver(this, instructionObservers, o);
  }
  
  /**
   * Notify all Instruction observers. Returns the total number of
   * blocking observers.
   */
  int notifyInstruction()
  {
    logger.log(Level.FINE, "{0} notifyInstruction()\n", this);
    
    Iterator i = instructionObservers.iterator();
    while (i.hasNext())
      {
	TaskObserver.Instruction observer;
	observer = (TaskObserver.Instruction) i.next();
	if (observer.updateExecuted(this) == Action.BLOCK)
	  blockers.add(observer);
      }
    return blockers.size();
  }

  // List containing the TaskObservations that are pending addition
  // or deletion (in order that they were requested). Will be dealt with
  // as soon as a stop event is received during one of the running states.
  LinkedList pendingObservations = new LinkedList();

  private ByteBuffer[] registerBanks;
  protected abstract ByteBuffer[] sendrecRegisterBanks();
  /**
   * Return the machine's register banks as an array.
   */
  public ByteBuffer[] getRegisterBanks ()
  {
    if (registerBanks == null)
      registerBanks = sendrecRegisterBanks();
    return registerBanks;
  }
  
  /**
   * The process has transitioned to the detached.
   *
   * XXX: Should be made private and instead accessor methods added.
   * Should more formally define the observable and the event.
   */
  public static TaskStateObservable taskStateDetached = new TaskStateObservable ();
  
 static
    {
      taskStateDetached.addObserver(new Observer()
      {

        public void update (Observable o, Object arg)
        {
          if (arg instanceof Task)
            {
              Task task = (Task) arg;
              task.clearIsa();
            }
        }
      });
    }
 
 public void clearIsa()
 {
   isa = null;
   memory = null;
   registerBanks = null;
 }
  
  /**
   * XXX: Temporary until .observable's are converted to .requestAddObserver.
   */
  public static class TaskStateObservable
    extends Observable
  {
    void notify (Object o)
    {
        logger.log (Level.FINE, "{0} notify -- all observers\n", o); 
        setChanged ();
        notifyObservers (o);
    }
  }
}
