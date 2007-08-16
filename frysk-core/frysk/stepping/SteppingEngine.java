// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.stepping;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import lib.dwfl.DwflLine;
import frysk.debuginfo.DebugInfoFrame;
import frysk.event.RequestStopEvent;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.rt.Breakpoint;
import frysk.rt.BreakpointManager;
import frysk.stack.Frame;
import frysk.stack.FrameIdentifier;
import frysk.stack.StackFactory;
import frysk.sys.Sig;
import frysk.sys.Signal;

/**
 * State machine for thread and process stepping. Provides static methods for
 * manipulating groups of threads by running, blocking, and various types of 
 * stepping.
 */
public class SteppingEngine {
    protected Logger logger = Logger.getLogger("frysk");

    /* Set of Tasks currently running, or unblocked.
     Package access so Breakpoint.PersistentBreakpoint can get at it. */
    HashSet runningTasks;

    /* Map from tasks to breakpoints set for stepping purposes */
    private HashMap breakpointMap;

    /* Map from tasks to blockers that the stepping engine can unblock
     * to continue stepping. */
    private HashMap blockerMap;

    /* Maps a Proc to an Integer, where the Integer represents the number of
     * Tasks associated with the current context of the process - the number
     * of stepping or running threads of that process. */
    private Map contextMap;

    /* Maps Tasks to TaskStepEngines, which contain information about the 
     * Task State, line number, and other useful things. */
    private Map taskStateMap;

    /* Observer used to block Tasks, as well as providing the mechanism to 
     * step them, by an instruction each time. */
    private SteppingObserver steppingObserver;

    /* Ensures that newly spawned Tasks are maintained by the SteppingEngine
     * class, and also makes sure that exiting Tasks are taken care of and cleaned
     * up after. */
    private ThreadLifeObservable threadLifeObservable;

    /* List of Tasks which require attachment from the this.steppingObserver */
    private LinkedList threadsList;

    private BreakpointManager breakpointManager;

    public SteppingEngine() {
	this.runningTasks = new HashSet();
	this.breakpointMap = new HashMap();
	this.blockerMap = new HashMap();
	this.contextMap = Collections.synchronizedMap(new HashMap());
	this.taskStateMap = Collections.synchronizedMap(new HashMap());
	this.breakpointManager = new BreakpointManager(this);
	this.steppingObserver = new SteppingObserver();
	this.threadLifeObservable = new ThreadLifeObservable();
    }

    /**
     * Sets the initial processes for this SteppingEngine. Maps the initial keys 
     * in this.contextMap and this.taskStateMap for each Task from each Proc, and adds the 
     * necessary observers to each of the Tasks.
     * 
     * @param procs The Procs to be managed by SteppingEngine
     */
    public SteppingEngine(Proc[] procs) {
	this();

	init(procs);
    }

    public SteppingEngine(Proc[] procs, Observer o) {
	this();

	addObserver(o);

	init(procs);
    }

    private void init(Proc[] procs) {
	Task t = null;
	LinkedList tasksList;

	for (int i = procs.length - 1; i >= 0; i--) {
	    tasksList = procs[i].getTasks();
	    this.threadsList.addAll(tasksList);

	    Iterator iter = tasksList.iterator();
	    while (iter.hasNext()) {
		t = (Task) iter.next();
		t.requestAddTerminatingObserver(this.threadLifeObservable);
		t.requestAddClonedObserver(this.threadLifeObservable);
		this.taskStateMap.put(t, new TaskStepEngine(t, this));
	    }

	    this.contextMap.put(t.getProc(), new Integer(tasksList.size()));
	}

	requestAdd();
    }

    /**
     * Once SteppingEngine is already managing one or more Tasks, appends
     * the given process to the data structures in SteppingEngine.
     * 
     * @param proc The Proc to be added to SteppingEngine
     */
    public boolean addProc(Proc proc) {
	Task t = null;

	LinkedList tasksList = proc.getTasks();
	this.threadsList.addAll(tasksList);

	Iterator iter = tasksList.iterator();
	while (iter.hasNext()) {
	    t = (Task) iter.next();
	    t.requestAddTerminatingObserver(this.threadLifeObservable);
	    t.requestAddClonedObserver(this.threadLifeObservable);
	    this.taskStateMap.put(t, new TaskStepEngine(t, this));
	}

	this.contextMap.put(t.getProc(), new Integer(tasksList.size()));
	requestAdd();
	return true;
    }

    /***********************************************************************
     * STEP HANDLING METHODS
     **********************************************************************/

    /**
     * Perform an instruction step on the given Task.
     * 
     * @param task The Task to be stepped a single instruction.
     * @return false The Task is not currently blocked
     * @return true The step request was successful
     */
    public boolean stepInstruction(Task task) {
	/* Check to make sure this thread is not already involved with another 
	 * operation before asking it to step an instruction. */
	TaskStepEngine tse = (TaskStepEngine) this.taskStateMap.get(task);
	if (!tse.isStopped())
	    return false;

	tse.setState(new InstructionStepState(task));
	this.steppingObserver.notifyNotBlocked(tse);
	this.contextMap.put(task.getProc(), new Integer(1));

	continueForStepping(task, true);
	return true;
    }

    /**
     * Perform an instruction step on a list of Tasks.
     * 
     * @param tasks The Tasks to be stepped a single instruction.
     * @return false Not all tasks are currently blocked.
     * @return true The step requests were successful
     */
    public boolean stepInstruction(LinkedList tasks) {
	/*
	 * Check to make sure these threads are not already involved with
	 * another operation before asking them to step an instruction.
	 */
	if (isProcRunning(tasks))
	    return false;

	Task t = (Task) tasks.getFirst();
	this.contextMap.put(t.getProc(), new Integer(tasks.size()));

	Iterator iter = tasks.iterator();
	while (iter.hasNext()) {
	    t = (Task) iter.next();
	    TaskStepEngine tse = (TaskStepEngine) this.taskStateMap.get(t);
	    tse.setState(new InstructionStepState(t));
	    this.steppingObserver.notifyNotBlocked(tse);
	    continueForStepping(t, true);
	}

	return true;
    }

    /**
     * Sets up SteppingEngine to perform a line step on the given Task.
     * 
     * @param task The Task to be line-stepped
     * @return false The Task is not currently blocked
     * @return true The step request was successful
     */
    public boolean stepLine(Task task) {
	/* Check to make sure these threads are not already involved with another 
	 * operation before asking them to step an instruction. */
	if (isTaskRunning(task))
	    return false;

	this.contextMap.put(task.getProc(), new Integer(1));
	TaskStepEngine tse = (TaskStepEngine) this.taskStateMap.get(task);

	/* Check to see if either the TaskStepEngine for this Task has not had its
	 * line number set yet, or its previous state was in non-debuginfo code. */
	if (tse.getLine() == 0) {
	    DwflLine line = tse.getDwflLine();

	    if (line == null) {
		tse.setState(new InstructionStepState(task));
		if (continueForStepping(task, true)) {
		    this.steppingObserver.notifyNotBlocked(tse);
		}
		return true; // XXX
	    } else {
		tse.setLine(line.getLineNum());
	    }
	}

	tse.setState(new LineStepState(task));
	if (continueForStepping(task, true)) {
	    this.steppingObserver.notifyNotBlocked(tse);
	}
	return true; // XXX
    }

    /**
     * Perform a line-step on a list of Tasks
     * 
     * @param tasks The Tasks to line step
     * @return false Not all tasks are currently blocked.
     * @return true The step requests were successful
     */
    public boolean stepLine(LinkedList tasks) {
	/* Check to make sure these threads are not already involved with another 
	 * operation before asking them to step an instruction. */
	if (isProcRunning(tasks))
	    return false;

	/* Do all the work here */
	setUp(tasks, true);

	return true;
    }

    /**
     * Sets up stepping information for a list of Tasks. Checks line numbers and 
     * whether the Tasks have any debuginfo at this point in time, before proceeding.
     * 
     * @param tasks The Tasks to begin stepping
     * @param isLine Whether this set up is for a line step or otherwise.
     */
    public void setUp(LinkedList tasks, boolean isLine) {
	/* Make sure there's actually something to step before going any further. */
	if (tasks.size() == 0)
	    return;

	TaskStepEngine tse;

	Task t = null;
	Iterator i = tasks.iterator();
	while (i.hasNext()) {
	    t = (Task) i.next();
	    tse = (TaskStepEngine) this.taskStateMap.get(t);

	    if (tse.getLine() == 0) {
		DwflLine line = tse.getDwflLine();

		/* Check to see if any line debugging information can be resolved
		 * from the Task at this point. If not, there's no point in doing
		 * 'line stepping' since there are no 'lines' to step. */
		if (line == null) {
		    tse.setState(new InstructionStepState(t));
		} else {
		    tse.setLine(line.getLineNum());
		    tse.setState(new LineStepState(t));
		}

		this.steppingObserver.notifyNotBlocked(tse);
	    } else {
		tse.setState(new LineStepState(t));
		this.steppingObserver.notifyNotBlocked(tse);
	    }
	}

	this.contextMap.put(t.getProc(), new Integer(tasks.size()));

	i = tasks.iterator();
	while (i.hasNext()) {
	    t = (Task) i.next();
	    continueForStepping(t, true);
	}
    }

    /**
     * Steps the Task until it reaches the given frame's current address.
     * Intended to be used when the given frame is a frame on the stack 
     * which is outer to the innermost frame; thus the Task will continue until
     * the frames inner to this one have all returned.
     * 
     * @param task  The Task to be stepped.
     * @param frame  The frame on the stack to be continued to.
     */
    public void stepAdvance(Task task, DebugInfoFrame frame) {
	/* There's nowhere to advance to - this is already the innermost frame */
	if (frame.getInnerDebugInfoFrame() == null) {
	    stepLine(task);
	    return;
	}

	TaskStepEngine tse = (TaskStepEngine) this.taskStateMap.get(task);
	tse.setState(new StepAdvanceState(task));
	this.steppingObserver.notifyNotBlocked(tse);

	int i = ((Integer) this.contextMap.get(task.getProc())).intValue();
	this.contextMap.put(task.getProc(), new Integer(++i));

	/*
	 * Set a breakpoint on the current address of the given frame, which is the
	 * return address of its inner frame(s).
	 */
	this.breakpoint = new SteppingBreakpoint(this, frame.getAddress());
	task.requestAddCodeObserver(this.breakpoint, frame.getAddress());
    }

    /**
     * Instruction stepping, but stepping-over any function calls.
     * 
     * @param task The stepping Task
     * @param lastFrame The current innermost frame of the Task
     */
    public void stepNextInstruction(Task task, DebugInfoFrame lastFrame) {
	this.frameIdentifier = lastFrame.getFrameIdentifier();

	TaskStepEngine tse = (TaskStepEngine) this.taskStateMap.get(task);
	tse.setState(new NextInstructionStepTestState(task));
	tse.setFrameIdentifier(this.frameIdentifier);
	this.steppingObserver.notifyNotBlocked(tse);

	int i = ((Integer) this.contextMap.get(task.getProc())).intValue();
	this.contextMap.put(task.getProc(), new Integer(++i));
	continueForStepping(task, true);
    }

    /**
     * Instruction steps the list of Tasks, but ensures that none enter any 
     * new frames on the stack. 
     * 
     * @param tasks The list of Tasks to be stepped
     */
    public void stepNextInstruction(LinkedList tasks) {
	if (tasks.size() < 1)
	    return;

	Task t = (Task) tasks.getFirst();
	int i = ((Integer) this.contextMap.get(t.getProc())).intValue();
	this.contextMap.put(t.getProc(), new Integer(i += tasks.size()));

	Iterator iter = tasks.iterator();
	while (iter.hasNext()) {
	    t = (Task) iter.next();
	    Frame frame = StackFactory.createFrame(t);

	    /* This is trouble. Need to figure out a way to map these properly,
	     * *hopefully* not requiring another HashMap. */
	    this.frameIdentifier = frame.getFrameIdentifier();

	    TaskStepEngine tse = (TaskStepEngine) this.taskStateMap.get(t);
	    tse.setFrameIdentifier(this.frameIdentifier);
	    tse.setState(new NextInstructionStepTestState(t));

	    if (continueForStepping(t, true)) {
		this.steppingObserver.notifyNotBlocked(tse);
	    }
	}
    }

    Breakpoint breakpoint;

    FrameIdentifier frameIdentifier;

    /**
     * Performs a step-operation - line-steps the given Task, unless presented
     * with an entry to a new frame on the stack, which will not be stepped
     * into.
     * 
     * @param task   The Task to be stepped-over
     * @param lastFrame	The current innermost StackFrame of the given Task
     */
    public void stepOver(Task task, DebugInfoFrame lastFrame) {
	this.frameIdentifier = lastFrame.getFrameIdentifier();

	TaskStepEngine tse = (TaskStepEngine) this.taskStateMap.get(task);
	tse.setFrameIdentifier(this.frameIdentifier);
	tse.setState(new StepOverTestState(task));

	int i = ((Integer) this.contextMap.get(task.getProc())).intValue();
	this.contextMap.put(task.getProc(), new Integer(++i));

	if (continueForStepping(task, true)) {
	    this.steppingObserver.notifyNotBlocked(tse);
	}
    }

    /**
     * Performs a step-over operation on a list of tasks; none of the given
     * Tasks will line-step into any new frames.
     * 
     * @param tasks The Tasks to step.
     */
    public void stepOver(LinkedList tasks) {
	if (tasks.size() < 1)
	    return;

	Task t = (Task) tasks.getFirst();
	int i = ((Integer) this.contextMap.get(t.getProc())).intValue();
	this.contextMap.put(t.getProc(), new Integer(i += tasks.size()));

	Iterator iter = tasks.iterator();
	while (iter.hasNext()) {
	    t = (Task) iter.next();
	    Frame frame = StackFactory.createFrame(t);

	    /* This is trouble. Need to figure out a way to map these properly,
	     * *hopefully* not requiring another HashMap. */
	    this.frameIdentifier = frame.getFrameIdentifier();

	    TaskStepEngine tse = (TaskStepEngine) this.taskStateMap.get(t);
	    tse.setFrameIdentifier(this.frameIdentifier);
	    tse.setState(new StepOverTestState(t));

	    if (continueForStepping(t, true)) {
		this.steppingObserver.notifyNotBlocked(tse);
	    }
	}
    }

    /**
     * Sets the stage for stepping out of a frame. Runs until a breakpoint on the
     * return address is hit. 
     * 
     * @param task   The Task to be stepped
     * @param frame The frame to step out of.
     */
    public void stepOut(Task task, DebugInfoFrame frame) {
	long address = frame.getOuterDebugInfoFrame().getAddress();

	TaskStepEngine tse = (TaskStepEngine) this.taskStateMap.get(task);
	tse.setState(new StepOutState(task));
	this.steppingObserver.notifyNotBlocked(tse);

	int i = ((Integer) this.contextMap.get(task.getProc())).intValue();
	this.contextMap.put(task.getProc(), new Integer(++i));

	this.breakpoint = new SteppingBreakpoint(this, address);
	this.breakpointMap.put(task, this.breakpoint);
	task.requestAddCodeObserver(this.breakpoint, address);
    }

    /**
     * Performs a step-out operation on a list of Tasks - runs each Task until 
     * it returns from its current frame.
     * 
     * @param tasks The Tasks to step-out
     */
    public void stepOut(LinkedList tasks) {
	if (tasks.size() < 1)
	    return;

	Task t = (Task) tasks.getFirst();
	int i = ((Integer) this.contextMap.get(t.getProc())).intValue();
	this.contextMap.put(t.getProc(), new Integer(i += tasks.size()));

	Iterator iter = tasks.iterator();
	while (iter.hasNext()) {
	    t = (Task) iter.next();
	    Frame frame = StackFactory.createFrame(t);

	    TaskStepEngine tse = (TaskStepEngine) this.taskStateMap.get(t);
	    tse.setState(new StepOutState(t));
	    this.steppingObserver.notifyNotBlocked(tse);

	    /* This is trouble. Need to figure out a way to map these properly, and
	     * *hopefully* not requiring another HashMap. */
	    //        this.breakpoint = new SteppingBreakpoint(this, frame.getOuter().getAddress());
	    SteppingBreakpoint sb = new SteppingBreakpoint(this, frame
		    .getOuter().getAddress());
	    this.breakpointMap.put(t, sb);
	    t.requestAddCodeObserver(sb, frame.getOuter().getAddress());
	}
    }

    public void cleanUpBreakPoint(Task task) {
	this.breakpoint = null;
	this.addy = 0;
    }

    /*****************************************************************************
     * STOP/CONTINUE HANDLING METHODS
     ****************************************************************************/

    /**
     * When implemented, re-runs the process from the beginning of the
     * debugging session if proper breakpoints are in place; otherwise just 
     * re-executes the process from its start. 
     */
    public void run(LinkedList tasks) {
	// for now point to continueExecution.

	continueExecution(tasks);
    }

    /**
     * Deletes the blocking observer from each of the incoming tasks,
     * effectively 'running', or continuing, the process.
     * 
     * @param tasks   The list of Tasks to be run
     */
    public void continueExecution(LinkedList list) {
	TaskStepEngine tse = null;
	this.contextMap.put(((Task) list.getFirst()).getProc(), new Integer(
		list.size()));

	Iterator i = list.iterator();
	while (i.hasNext()) {
	    Task t = (Task) i.next();
	    if (!this.runningTasks.contains(t)) {
		this.runningTasks.add(t);
		tse = (TaskStepEngine) this.taskStateMap.get(t);

		if (tse != null) {
		    tse.setState(new RunningState(t));
		    this.steppingObserver.notifyNotBlocked(tse);
		}
		continueForStepping(t, false);
		t.requestDeleteInstructionObserver(this.steppingObserver);
	    }
	}
    }

    public void continueExecution(Task task) {
	LinkedList l = new LinkedList();
	l.add(task);
	continueExecution(l);
    }

    /**
     * Unblock a task so that, from the point of view of the stepping
     * engine, execution can continue. For now this unblocks
     * instruction observers and code observers for breakpoints, but
     * ultimately I (timoore) think it should unblock all blockers.
     */
    public boolean continueForStepping(Task task, boolean unblockStepper) {
	if (unblockStepper) {
	    task.requestUnblock(this.steppingObserver);
	}
	LinkedList blockers = getAndClearBlockers(task);
	if (blockers != null) {
	    ListIterator iter = blockers.listIterator();
	    while (iter.hasNext()) {
		TaskObserver blocker = (TaskObserver) iter.next();
		task.requestUnblock(blocker);
	    }
	}
	return true;
    }

    /**
     * Re-blocks all running Tasks except for those in the incoming LinkedList.
     * If the list is null or has size zero, than by default all Tasks are 
     * blocked by the ProcBlockObserver. Otherwise, the list is compared to the
     * set of running tasks and those Tasks not in the list are stopped.
     * 
     * @param keepRunning    The list of Tasks to not block
     */
    public void stop(LinkedList keepRunning, LinkedList stopTasks) {
	if (keepRunning == null || keepRunning.size() == 0) {
	    if (this.threadsList.size() > 0) {
		Iterator iter = stopTasks.iterator();
		while (iter.hasNext()) {
		    Task t = (Task) iter.next();
		    if (!this.threadsList.contains(t))
			this.threadsList.addLast(t);
		}
	    } else
		this.threadsList.addAll(stopTasks);

	    TaskStepEngine tse;
	    Iterator i = stopTasks.iterator();
	    while (i.hasNext()) {
		Task t = (Task) i.next();
		tse = (TaskStepEngine) this.taskStateMap.get(t);
		tse.setState(new StoppedState(t));
	    }
	    requestAdd();
	} else {
	    synchronized (this.threadsList) {
		Iterator i = this.runningTasks.iterator();
		while (i.hasNext()) {
		    Task t = (Task) i.next();
		    if (!keepRunning.contains(t)) {
			this.threadsList.add(t);
			i.remove();
		    }
		}
		requestAdd();
	    }
	}
	this.runningTasks.clear();
    }

    /**
     * Method to handle am incoming list of tasks to be run, with four cases.
     * 
     * Case 1:    List of tasks is empty, and there are no running tasks.
     * Case 2:    List of tasks is empty, and there are running tasks.
     * Case 3:    List of tasks is not empty, and there are no running tasks.
     * Case 4:    List of tasks is not empty, and there are running tasks.
     * 
     * It is assumed that any running Tasks not in the incoming list are to be
     * blocked, and all tasks in that list are to be unblocked or run. If any of
     * those Tasks are already running, leave them be. Return an int representing
     * which state the calling class should be in after this method is executed.
     * 
     * @param tasks   The list of tasks to be run
     */
    public synchronized void executeTasks(LinkedList tasks) {
	/* No incoming Tasks and no Tasks already running */
	if (tasks.size() == 0 && this.runningTasks.size() == 0) {
	    return;
	}

	/* No incoming Tasks, but some Tasks are running. Block them. */
	else if (tasks.size() == 0 && this.runningTasks.size() != 0) {
	    Iterator i = this.runningTasks.iterator();
	    while (i.hasNext()) {
		Task t = (Task) i.next();
		this.threadsList.addLast(t);
		i.remove();
	    }
	    requestAdd();
	    return;
	}

	TaskStepEngine tse = null;
	/* There are incoming Tasks to be run, and no Tasks already running */
	if (this.runningTasks.size() == 0) {
	    Iterator i = tasks.iterator();
	    while (i.hasNext()) {
		Task t = (Task) i.next();
		this.runningTasks.add(t);
		tse = (TaskStepEngine) this.taskStateMap.get(t);
		tse.setState(new RunningState(t));
		this.steppingObserver.notifyNotBlocked(tse);
		t.requestDeleteInstructionObserver(this.steppingObserver);
	    }
	    return;
	} else
	/* There are incoming Tasks to be run, and some Tasks are already running.
	 * If they are not already running, unblock the incoming Tasks, and block
	 * any running Task not in the incoming list. */
	{
	    HashSet temp = new HashSet();
	    int numRunning = 0;
	    Iterator i = tasks.iterator();
	    while (i.hasNext()) {
		Task t = (Task) i.next();

		/* If this thread has not already been unblocked, do it */
		if (!this.runningTasks.remove(t)) {
		    ++numRunning;
		    tse = (TaskStepEngine) this.taskStateMap.get(t);
		    tse.setState(new RunningState(t));
		    this.steppingObserver.notifyNotBlocked(tse);
		    t.requestDeleteInstructionObserver(this.steppingObserver);
		} else {
		    /* Put all threads back into a master list */
		    temp.add(t);
		}
	    }

	    /* Now catch the threads which have a block request */
	    if (this.runningTasks.size() != 0) {
		i = this.runningTasks.iterator();
		while (i.hasNext()) {
		    Task t = (Task) i.next();
		    --numRunning;
		    this.threadsList.addLast(t);
		}
		requestAdd();
	    }

	    this.contextMap.put(((Task) tasks.getFirst()).getProc(),
		    new Integer(numRunning));
	    this.runningTasks = temp;
	}
	return;
    }

    /**
     * Iterates through the given list of Tasks and returns true if any have 
     * a non-stopped state. Tasks are assumed be Tasks from the same Proc.
     * 
     * @param tasks The Tasks to check states for.
     * 
     * @return true If any of the Tasks are not stopped
     * @retruen false If all the Tasks are stopped
     */
    public boolean isProcRunning(LinkedList tasks) {
	TaskStepEngine tse = null;
	Iterator iter = tasks.iterator();
	while (iter.hasNext()) {
	    Task t = (Task) iter.next();
	    tse = (TaskStepEngine) this.taskStateMap.get(t);
	    if (tse != null && !tse.isStopped())
		return true;
	}

	return false;
    }

    /**
     * Checks to see if the state of the given task is not a stopped state.
     * 
     * @param task The Task to check the state of
     * @return true If the Task is not stopped
     * @return false If the Task is stopped
     */
    public boolean isTaskRunning(Task task) {
	TaskStepEngine tse = (TaskStepEngine) this.taskStateMap.get(task);

	if (tse == null)
	    return false;

	return !tse.isStopped();
    }

    /**
     * Detaches all observers and breakpoints from all Tasks of the given Proc.
     * 
     * @param proc The Proc to be detached
     * @param kill Whether the Proc should be killed after detaching
     */
    public void detachProc(Proc proc, boolean kill) {
	LinkedList list = proc.getTasks();
	Task t;

	if (kill) {
	    Integer context = new Integer(list.size());
	    this.contextMap.put(proc, context);
	    this.threadLifeObservable.setExitingTasks(list);
	}

	Iterator i = list.iterator();
	while (i.hasNext()) {
	    t = (Task) i.next();

	    SteppingBreakpoint bpt = (SteppingBreakpoint) this.breakpointMap
		    .get(t);

	    if (bpt != null) {
		this.breakpointMap.remove(t);
		t.requestUnblock(bpt);
	    }

	    t.requestDeleteTerminatingObserver(this.threadLifeObservable);
	    t.requestDeleteClonedObserver(this.threadLifeObservable);
	    t.requestDeleteInstructionObserver(this.steppingObserver);
	    cleanTask(t);
	}
    }

    /**
     * Clears information out of SteppingEngine data structures which are mapped
     * to the given Task.
     * 
     * @param task The Task to clear information for
     */
    public void cleanTask(Task task) {
	this.taskStateMap.remove(task);
	this.threadsList.remove(task);
	this.runningTasks.remove(task);
	this.breakpointMap.remove(task);
    }

    /**
     * Removes all information from all SteppingEngine data structures.
     */
    public void clear() {
	this.taskStateMap.clear();
	this.breakpointMap.clear();
	this.contextMap.clear();
	this.runningTasks.clear();
	this.threadLifeObservable.deleteObservers();
	this.threadLifeObservable = new ThreadLifeObservable();
	this.threadsList.clear();
	breakpoint = null;
	this.steppingObserver.deleteObservers();
	this.steppingObserver = new SteppingObserver();
    }

    /*****************************************************************************
     * GETTERS AND SETTERS
     ****************************************************************************/

    /**
     * Set the state of a particular Task.
     * 
     * @param task The Task to set the state of
     * @param state The State to set it to
     */
    public void setTaskState(Task task, State state) {
	((TaskStepEngine) this.taskStateMap.get(task)).setState(state);
    }

    /**
     * Get the state of a particular Task.
     * 
     * @param task The Task to return the State of
     * @return state The State of the given Task
     */
    public State getTaskState(Task task) {
	return ((TaskStepEngine) this.taskStateMap.get(task)).getState();
    }

    /**
     * Gets the this.steppingObserver currently being used by the SteppingEngine.
     * 
     * @return this.steppingObserver The this.steppingObserver currently in use
     */
    public SteppingObserver getSteppingObserver() {
	return this.steppingObserver;
    }

    /**
     * Sets a SteppingBreakpoint on the given Task at the given address.
     * 
     * @param task The Task to breakpoint
     * @param address The address to set the breakpoint at
     */
    public void setBreakpoint(Task task, long address) {
	this.breakpoint = new SteppingBreakpoint(this, address);
	this.breakpointMap.put(task, this.breakpoint);
	task.requestAddCodeObserver(this.breakpoint, address);
    }

    /**
     * Removes the set SteppingBreakpoint from the given Task.
     * 
     * @param task The Task to remove the SteppingBreakpoint from
     */
    public void removeBreakpoint(Task task) {
	if (this.breakpointMap.containsKey(task)) {
	    SteppingBreakpoint sbp = (SteppingBreakpoint) this.breakpointMap
		    .remove(task);
	    task.requestUnblock(sbp);
	    task.requestDeleteCodeObserver(sbp, sbp.getAddress());
	}
    }

    /**
     * Returns the Breakpoint set on the given Task
     * 
     * @param task The Task whose breakpoint is requested
     * @return bp The Breakpoint set at the given Task
     */
    public Breakpoint getTaskBreakpoint(Task task) {
	return (Breakpoint) this.breakpointMap.get(task);
    }

    /**
     * Adds the given Breakpoint on the given Task.
     * 
     * @param task The Task to have the Breakpoint added to
     * @param bp The Breakpoint to add to the given Task
     */
    public void addBreakpoint(Task task, Breakpoint bp) {
	this.breakpoint = bp;
	task.requestAddCodeObserver(bp, bp.getAddress());
    }

    /**
     * Deletes the given Breakpoint from the given Task
     * 
     * @param task The Task to delete the Breakpoint from
     * @param bp The Breakpoint to delete from the given Task
     */
    public void deleteBreakpoint(Task task, Breakpoint bp) {
	task.requestDeleteCodeObserver(bp, bp.getAddress());
    }

    /**
     * Set the current state of the given tasks as running. Used when the running
     * of these Tasks was out of the scope of control for SteppingEngine.
     * 
     * @param tasks The Tasks to be set as running.
     */
    public void setRunning(LinkedList tasks) {
	TaskStepEngine tse = null;
	Iterator i = tasks.iterator();
	while (i.hasNext()) {
	    Task t = (Task) i.next();
	    tse = (TaskStepEngine) this.taskStateMap.get(t);
	    tse.setState(new RunningState(t));
	}
    }

    /**
     * Adds the given Observer to this.steppingObserver's Observer list.
     * 
     * @param o The Observer to be added.
     */
    public void addObserver(Observer o) {
	this.steppingObserver.addObserver(o);
    }

    /**
     * Supplies an Observer to be added to the ThreadLifeObserver's Observer list.
     * 
     * @param o The Observer to be added.
     */
    public void setThreadObserver(Observer o) {
	this.threadLifeObservable.addObserver(o);
    }

    /**
     * Returns a Set of tasks currently in an executing state.
     * 
     * @return runningTasks	The Tasks in an executing state.
     */
    public HashSet getRunningTasks() {
	return this.runningTasks;
    }

    /***********************************************************************
     * TASKOBSERVER.INSTRUCTION OBSERVER CLASS
     **********************************************************************/

    /**
     * Remove the incoming Observer object from the this.steppingObserver's list of 
     * Observers to notify. Unblocks the given process if required.
     * 
     * @param o The Observer to delete
     * @param p The Proc to delete the Observer from
     * @param unblock Whether the given Proc should be unblocked
     */
    public void removeObserver(Observer o, Proc p, boolean unblock) {
	this.steppingObserver.deleteObserver(o);
	if (unblock) {
	    continueExecution(p.getTasks());
	}
    }

    protected class SteppingObserver extends Observable implements
	    TaskObserver.Instruction {

	/**
	 * Callback for TaskObserver.Instruction. Each time a Task is blocked, either
	 * after this Observer was added to it for the first time, or it was
	 * unblocked to allow execution of a single instruction, it passes through
	 * here. 
	 * Depending on what the state of this object is at the time and how
	 * many Tasks are left to be blocked, we'll either do nothing and return 
	 * a blocking Action, or notify our Observers that this task came through,
	 * and they can perform the necessary operations.
	 * 
	 * @param task The Task which was just blocked
	 * @return Action.BLOCK Continue blocking this incoming Task
	 */
	public synchronized Action updateExecuted(Task task) {
	    //       System.err.println("SE.SO.updateEx: " + task);
	    /* Check to see if acting upon this event produces a stopped state
	     * change. If so, decrement the number of Tasks active in the Task's 
	     * process context. If there are no Tasks left, then notify the this 
	     * Object's observers that all work has been completed. */
	    TaskStepEngine tse = (TaskStepEngine) SteppingEngine.this.taskStateMap
		    .get(task);
	    if (tse.handleUpdate()) {
		
		if (!tse.isAlive())
		    cleanTask(task);
		
		Proc proc = task.getProc();
		int i = ((Integer) SteppingEngine.this.contextMap.get(proc))
			.intValue();
		if (--i <= 0) {
		    if (threadsList.size() > 0) {
			SteppingEngine.this.contextMap.put(proc, new Integer(
				threadsList.size() + i));
			requestAdd();
			return Action.BLOCK;
		    }

		    this.setChanged();
		    this.notifyObservers(tse);
		}

		SteppingEngine.this.contextMap.put(proc, new Integer(i));
	    }

	    return Action.BLOCK;
	}

	public void addedTo(Object o) {
	    //      System.err.println("SE.addedTo");
	}

	public void deletedFrom(Object o) {
	    //      System.err.println("SE.deletedFrom");
	}

	/**
	 * The add to the Object failed
	 */
	public void addFailed(Object o, Throwable w) {
	    w.printStackTrace();
	    ((Task) o).getProc().requestAbandonAndRunEvent(
		    new RequestStopEvent(Manager.eventLoop));
	}

	/**
	 * Notify certain classes observing this object's Proc that it will 
	 * immanently no longer be blocked.
	 */
	public void notifyNotBlocked(TaskStepEngine tse) {
	    //System.err.println("notifyNotBlocked");
	    this.setChanged();
	    this.notifyObservers(tse);
	}
    }

    public void requestAdd() {
	LinkedList list = new LinkedList();
	Task t;

	while (this.threadsList.size() > 0) {
	    t = (Task) this.threadsList.removeFirst();

	    if (t == null)
		continue;

	    list.add(t);
	    Proc proc = t.getProc();

	    if (!(proc.getUID() == Manager.host.getSelf().getUID() || proc
		    .getGID() == Manager.host.getSelf().getGID())) {
		System.err.println("Process " + proc
			+ " is not owned by user/group.");
		continue;
	    }
	}

	Iterator i = list.iterator();
	while (i.hasNext()) {
	    t = (Task) i.next();
	    t.requestAddInstructionObserver(this.steppingObserver);
	}
    }

    /***********************************************************************
     * TASKOBSERVER.CLONED/TERMINATED OBSERVER CLASS
     **********************************************************************/

    protected class ThreadLifeObservable extends Observable implements
	    TaskObserver.Cloned, TaskObserver.Terminating {

	private LinkedList exitingTasks;

	public ThreadLifeObservable() {
	    SteppingEngine.this.threadsList = new LinkedList();
	    this.exitingTasks = new LinkedList();
	}

	public Action updateClonedParent(Task parent, Task offspring) {
	    return Action.CONTINUE;
	}

	public Action updateClonedOffspring(Task parent, Task offspring) {
	    Integer i = (Integer) SteppingEngine.this.contextMap.get(parent
		    .getProc());
	    SteppingEngine.this.contextMap.put(parent.getProc(), new Integer(i
		    .intValue() + 1));
	    SteppingEngine.this.taskStateMap.put(offspring, new TaskStepEngine(
		    offspring, SteppingEngine.this));
	    SteppingEngine.this.threadsList.addLast(offspring);
	    offspring.requestAddClonedObserver(this);
	    offspring.requestAddTerminatingObserver(this);
	    return Action.CONTINUE;
	}

	public Action updateTerminating(Task task, boolean signal, int value) {
	    //      System.err.println("threadlife.updateTerminating " + task + " " + value);
	    Integer context = (Integer) SteppingEngine.this.contextMap.get(task
		    .getProc());
	    SteppingEngine.this.contextMap.put(task.getProc(), new Integer(
		    context.intValue() - 1));

	    TaskStepEngine tse = (TaskStepEngine) SteppingEngine.this.taskStateMap.get(task);
	    tse.setState(new StepTerminatedState(task));
	    
	    if (signal)
		tse.setMessage("Task " + task.getTid() + " terminated from signal " + value);
	    else
		tse.setMessage ("Task " + task.getTid() + " terminated");

	    steppingObserver.notifyNotBlocked(tse);
	    
	    return Action.CONTINUE;
	}

	public void addedTo(Object observable) {
	    //        System.err.println("ThreadLife addedTo");
	}

	public void addFailed(Object observable, Throwable w) {
	    throw new RuntimeException("Failed to attach to created proc", w);
	}

	public void deletedFrom(Object observable) {
	    //      	Task task = (Task) observable;
	    if (this.exitingTasks.remove(observable)) {
		Task task = (Task) observable;
		int i = ((Integer) SteppingEngine.this.contextMap.get(task
			.getProc())).intValue();
		if (--i > 0) {
		    contextMap.put(task.getProc(), new Integer(i));
		} else {
		    contextMap.remove(task.getProc());
		    Signal.kill(task.getProc().getPid(), Sig.KILL);
		}
	    }
	}

	public void setExitingTasks(LinkedList tasks) {
	    this.exitingTasks.addAll(tasks);
	}
    }

    /*****************************************************************************
     * TASKOBSERVER.CODE BREAKPOINT CLASS
     ****************************************************************************/

    long addy;

    protected class SteppingBreakpoint extends Breakpoint implements
	    TaskObserver.Code {
	protected long address;

	protected int triggered;

	protected boolean added;

	protected boolean removed;

	public SteppingBreakpoint(SteppingEngine steppingEngine, long address) {
	    super(steppingEngine, address);
	    //      System.out.println("Setting address to 0x" + Long.toHexString(address));
	    this.address = address;
	    if (monitor == null)
		monitor = new Object();
	}

	protected void logHit(Task task, long address, String message) {
	    if (logger.isLoggable(Level.FINEST)) {
		Object[] logArgs = { task, Long.toHexString(address),
			Long.toHexString(task.getIsa().pc(task)),
			Long.toHexString(this.address) };
		logger.logp(Level.FINEST, "SteppingEngine.SteppingBreakpoint",
			"updateHit", message, logArgs);
	    }
	}

	public Action updateHit(Task task, long address) {
	    //      System.err.println("SteppingBreakpoint.updateHIt " + task);
	    logHit(task, address, "task {0} at 0x{1}\n");
	    if (address != this.address) {
		logger.logp(Level.WARNING, "SteppingEngine.SteppingBreakpoint",
			"updateHit", "Hit wrong address!");
		return Action.CONTINUE;
	    } else {
		addy = address;
		logHit(task, address, "adding instructionobserver {0} 0x{2}");
		task
			.requestAddInstructionObserver(SteppingEngine.this.steppingObserver);
	    }

	    ++triggered;
	    return Action.BLOCK;
	}

	public int getTriggered() {
	    return triggered;
	}

	public void addFailed(Object observable, Throwable w) {
	    w.printStackTrace();
	}

	public void addedTo(Object observable) {
	    //    System.err.println("BreakPoint.addedTo");
	    synchronized (monitor) {
		added = true;
		removed = false;
		monitor.notifyAll();
	    }

	    Task t = (Task) observable;
	    t.requestDeleteInstructionObserver(steppingObserver);
	    continueForStepping(t, false);
	}

	public boolean isAdded() {
	    return added;
	}

	public void deletedFrom(Object observable) {
	    synchronized (monitor) {
		removed = true;
		added = false;
		monitor.notifyAll();
	    }
	}

	public boolean isRemoved() {
	    return removed;
	}

	public long getAddress() {
	    return address;
	}
    }

    public BreakpointManager getBreakpointManager() {
	return breakpointManager;
    }

    /**
     * Add a blocker to the list of blockers that the stepping engine
     * must unblock before continuing.
     * @param task the task
     * @param observer the blocker
     */
    public synchronized void addBlocker(Task task, TaskObserver observer) {
	LinkedList blockerList = (LinkedList) blockerMap.get(task);
	if (blockerList == null) {
	    blockerList = new LinkedList();
	    blockerMap.put(task, blockerList);
	}
	blockerList.add(observer);
    }

    /**
     * Get the list of blocking observers that have notified the
     * stepping engine.
     * @param task the task
     * @return list of blockers
     */
    public LinkedList getBlockers(Task task) {
	return (LinkedList) blockerMap.get(task);
    }

    /**
     * Get the list of blocking observers that have notified the
     * stepping engine and clear it.
     * @param task the task
     * @return list of blockers
     */
    public synchronized LinkedList getAndClearBlockers(Task task) {
	LinkedList blockers = (LinkedList) blockerMap.get(task);
	if (blockers != null) {
	    blockerMap.remove(task);
	}
	return blockers;
    }
}
