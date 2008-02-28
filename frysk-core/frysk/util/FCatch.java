// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package frysk.util;

import frysk.isa.signals.Signal;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import frysk.rsl.Log;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.stack.Frame;
import frysk.stack.StackFactory;

public class FCatch {
    private static final Log fine = Log.fine(FCatch.class);

    private int numTasks = 0;

    //True if we're tracing children as well.
    boolean traceChildren;

    boolean firstCall = true;

    private StringBuffer stackTrace = new StringBuffer();

    private Blocker blocker;

    private SignalObserver signalObserver;

    private Signal sig;

    private int stacklevel = 0;

    private Task sigTask;

    HashSet signaledTasks = new HashSet();

    Proc proc = null;


    Frame[] frames;

    /**
     * Sets up the attached process.
     * 
     * @param command Command line arguments, including executable name
     * @param attach  Whether to create a new process with the above arguments, or
     * to attach to an already-running process.
     */
    public void trace(String[] command, boolean attach) {
	fine.log(this, "trace");

	if (attach == true)
	    init();
	else {
	    File exe = new File(command[0]);
	    // XXX: There is a race between this .exists call, a remove of
	    // the executable, and the subsequent attempt to exec it.  The
	    // only robust way to make this work is to detect the failure
	    // in the Attach observer - when it fails.
	    if (exe.exists())
		Manager.host.requestCreateAttachedProc(command,
			new CatchObserver());
	    else {
		System.err.println("fcatch: can't find executable!");
		System.exit(1);
	    }
	}

	// Run the event-loop from within this thread - no need to
	// multi-thread this application.
	Manager.eventLoop.run();
	fine.log(this, "exiting trace");
    }

    /**
     * Attaches FCatch to an already running process.
     */
    private void init() {
	fine.log(this, "init");
	iterateTasks(proc);
	fine.log(this, "exiting init");
    }

    /**
     * Adds a CatchObserver to each of the Tasks belonging to the process.
     */
    private void iterateTasks(Proc proc) {
	Iterator i = proc.getTasks().iterator();
	while (i.hasNext()) {
	    ((Task) i.next()).requestAddAttachedObserver(new CatchObserver());
	}
    }

    /**
     * Adds a PID to be traced to this class' HashSet.
     * 
     * @param id  The PID to be traced
     */
    public void addProc(Proc proc) {
	fine.log(this, "addProc", proc);
	this.proc = proc;
    }

    /**
     * Builds a stack trace from the incoming blocked task, and appends the output
     * to this class' StringBuffer. Decrements the numTasks variable to let FCatch
     * know when to unblock the signaled thread.
     * 
     * @param task    The Task to be StackTraced
     */
    private void generateStackTrace(Task task) {
	fine.log(this, "generateStackTrace", task);
	--this.numTasks;
	Frame frame = null;
	try {
	    frame = StackFactory.createFrame(task);
	} catch (Exception e) {
	    e.printStackTrace();
	}

	StringWriter stringWriter = new StringWriter();
	PrintWriter printWriter = new PrintWriter(stringWriter);
	StackFactory.printStack(printWriter, frame);
	this.stackTrace.append(stringWriter.getBuffer());

	fine.log(this, "exiting generateStackTrace", task);
    }

    /**
     * Returns a String representation of the stack trace thus far.
     * 
     * @return The stack trace thus far.
     */
    public String getStackTrace() {
	return this.stackTrace.toString();
    }

    /**
     * Prints the stack trace.
     */
    public String toString() {
	String trace = this.stackTrace.toString();
	System.out.println(trace);
	return trace;
    }

    /**
     * Depending on the signal, appends the signal type to the stack trace 
     * StringBuffer and calls generateStackTrace(Task).
     * If all Tasks have completed their trace, this method unblocks the signaled
     * thread blocked by a SignaledObserver, and then removes the Blocker
     * Objects from all the Tasks.
     * 
     * @param task    The Task recently blocked.
     */
    public synchronized void handleTaskBlock(Task task) {
	this.signaledTasks.add(task);

	stackTrace.append(sig.toString());
	stackTrace.append(" detected - dumping stack trace for TID ");
	stackTrace.append(task.getTid());
	stackTrace.append("\n");
	generateStackTrace(task);

	if (numTasks <= 0) {
	    System.out.println(this.stackTrace.toString());
	    this.stackTrace = new StringBuffer();
	    this.stackTrace.append(stacklevel++ + "\n");
	    sigTask.requestUnblock(signalObserver);
	    Iterator i = task.getProc().getTasks().iterator();
	    while (i.hasNext()) {
		Task t = (Task) i.next();
		t.requestDeleteInstructionObserver(blocker);
	    }
	}
    }

    /**
     * An observer that sets up things once frysk has set up
     * the requested proc and attached to it.
     */
    class CatchObserver implements TaskObserver.Attached, TaskObserver.Cloned,
	    TaskObserver.Terminating, TaskObserver.Terminated {
	/**
	 * This Task has been attached to and blocked - attach the rest of this
	 * Object's implemented Observers to it.
	 */
	public Action updateAttached(Task task) {
	    numTasks = task.getProc().getTasks().size();
	    fine.log(this, "updateAttached", task);
	    if (signalObserver == null)
		signalObserver = new SignalObserver();

	    task.requestAddSignaledObserver(signalObserver);
	    task.requestAddClonedObserver(this);
	    task.requestAddTerminatingObserver(this);
	    task.requestAddTerminatedObserver(this);
	    task.requestUnblock(this);
	    return Action.BLOCK;
	}

	public Action updateClonedParent(Task parent, Task offspring) {
	    fine.log(this, "updateClonedParent", parent, "offspring",
		     offspring);
	    //System.out.println("Cloned.updateParent");
	    parent.requestUnblock(this);
	    return Action.BLOCK;
	}

	/**
	 * One of the Tasks has forked - make sure that the child is also traced
	 * properly.
	 */
	public Action updateClonedOffspring(Task parent, Task offspring) {
	    fine.log(this, "updateClonedOffspring", offspring, "parent",
		     parent);
	    FCatch.this.numTasks = offspring.getProc().getTasks().size();
	    SignalObserver sigo = new SignalObserver();

	    offspring.requestAddSignaledObserver(sigo);
	    offspring.requestAddTerminatingObserver(this);
	    offspring.requestAddClonedObserver(this);
	    offspring.requestAddTerminatedObserver(this);
	    offspring.requestUnblock(this);
	    return Action.BLOCK;
	}

	public Action updateTerminating(Task task, Signal signal, int value) {
	    fine.log(this, "updateTerminating", task, "signal", signal);
	    return Action.CONTINUE;
	}

	public Action updateTerminated(Task task, Signal signal, int value) {
	    fine.log(this, "updateTerminated", task, "signal", signal);
	    if (--FCatch.this.numTasks <= 0)
		Manager.eventLoop.requestStop();

	    return Action.CONTINUE;
	}

	public void addedTo(Object observable) {
	    fine.log(this, "CatchObserver.addedTo", observable);
	}

	public void addFailed(Object observable, Throwable w) {
	    throw new RuntimeException("Failed to attach to created proc", w);
	}

	public void deletedFrom(Object observable) {
	    fine.log(this, "deletedFrom", observable);
	}
    }

    /**
     * Intercepts signals to a Task and deals with them appropriately.
     */
    class SignalObserver implements TaskObserver.Signaled {
	/**
	 * The Task received a signal - block all the other tasks, and
	 * append to the StringBuffer member of FCatch that this Task
	 * was signaled.
	 */
	public Action updateSignaled(Task task, Signal signal) {
	    fine.log(this, "updateSignaled", task, "signal", signal);
	    sigTask = task;

	    FCatch.this.sig = signal;
	    FCatch.this.numTasks = task.getProc().getTasks().size();

	    if (FCatch.this.numTasks > 1
		    && FCatch.this.signaledTasks.contains(task)) {
		FCatch.this.signaledTasks.remove(task);
		return Action.CONTINUE;
	    }

	    stackTrace.append("fcatch: from PID " + task.getProc().getPid()
		    + " TID " + task.getTid() + ":\n");
	    blocker = new Blocker();
	    Iterator i = task.getProc().getTasks().iterator();
	    while (i.hasNext()) {
		Task t = (Task) i.next();
		t.requestAddInstructionObserver(blocker);
	    }
	    return Action.BLOCK;
	}

	public void addFailed(Object observable, Throwable w) {
	    w.printStackTrace();
	}

	public void addedTo(Object observable) {
	    fine.log(this, "SignalObserver.addedTo", observable);
	}

	public void deletedFrom(Object observable) {
	    fine.log(this, "deletedFrom", observable);
	}
    }

    /**
     * Blocks threads; makes sure they get stack traced.
     */
    class Blocker implements TaskObserver.Instruction {
	public Action updateExecuted(Task task) {
	    handleTaskBlock(task);
	    return Action.BLOCK;
	}

	public void addFailed(Object observable, Throwable w) {
	    w.printStackTrace();
	}

	public void addedTo(Object observable) {
	    fine.log(this, "SignalObserver.addedTo", observable);
	}

	public void deletedFrom(Object observable) {
	    fine.log(this, "deletedFrom", observable);
	}
    }
}
