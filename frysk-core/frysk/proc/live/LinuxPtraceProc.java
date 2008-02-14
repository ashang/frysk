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

package frysk.proc.live;

import frysk.proc.Action;
import frysk.sys.proc.Exe;
import frysk.proc.ProcId;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.Host;
import frysk.sys.proc.Stat;
import frysk.proc.Auxv;
import frysk.sys.proc.AuxvBuilder;
import frysk.proc.MemoryMap;
import java.util.ArrayList;
import frysk.sys.proc.CmdLineBuilder;
import frysk.sys.proc.MapsBuilder;
import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;
import frysk.sys.proc.Status;
import java.util.logging.Level;
import frysk.sys.proc.ProcBuilder;
import java.util.Map;
import java.util.HashMap;
import frysk.proc.TaskId;
import java.util.Iterator;
import java.io.File;
import frysk.proc.Manager;
import frysk.proc.ProcEvent;
import frysk.proc.TaskObserver;

/**
 * A Linux Proc tracked using PTRACE.
 */

public class LinuxPtraceProc extends LiveProc {
    /**
     * Create a new detached process.  RUNNING makes no sense here.
     * Since PARENT could be NULL, also explicitly pass in the host.
     */
    public LinuxPtraceProc(Host host, Proc parent, ProcId pid, Stat stat) {
	super(host, parent, pid);
	((LinuxPtraceHost)host).putProc(ProcessIdentifierFactory.create(pid.hashCode()), this);
	this.newState = LinuxPtraceProcState.initial(false);
	this.stat = stat;
	this.breakpoints = new BreakpointAddresses(this);
    }
    /**
     * Create a new, definitely attached, definitely running fork of
     * Task.
     */
    public LinuxPtraceProc(Task task, ProcessIdentifier fork) {
	super(task, new ProcId(fork.intValue()));
	((LinuxPtraceHost)getHost()).putProc(fork, this);
	this.newState = LinuxPtraceProcState.initial(true);
	this.breakpoints = new BreakpointAddresses(this);
    }

    private Auxv[] auxv;
    public Auxv[] getAuxv() {
	if (auxv == null) {
	    class BuildAuxv extends AuxvBuilder {
		Auxv[] vec;
		public void buildBuffer (byte[] auxv) {
		}
		public void buildDimensions (int wordSize, boolean bigEndian,
					     int length) {
		    vec = new Auxv[length];
		}
		public void buildAuxiliary (int index, int type, long val) {
		    vec[index] = new Auxv (type, val);
		}
	    }
	    BuildAuxv auxv = new BuildAuxv ();
	    ProcessIdentifier pid
		= ProcessIdentifierFactory.create(getPid());
	    auxv.construct(pid);
	    this.auxv = auxv.vec;
	}
	return auxv;
    }

    private MemoryMap[] maps;

    public MemoryMap[] getMaps() {

	class BuildMaps extends MapsBuilder {

	    ArrayList mapsList = new ArrayList();
	    byte[] mapsLocalArray;

	    public void buildBuffer(byte[] mapsArray) {
		mapsLocalArray = mapsArray;
		mapsArray[mapsArray.length - 1] = 0;
	    }

	    public void buildMap(long addressLow, long addressHigh,
		    boolean permRead, boolean permWrite, boolean permExecute,
		    boolean shared, long offset, int devMajor, int devMinor,
		    int inode, int pathnameOffset, int pathnameLength) {
	
		byte[] mapFilename = new byte[pathnameLength];
		System.arraycopy(mapsLocalArray, pathnameOffset, mapFilename, 0,
			pathnameLength);
		
		MemoryMap map = new MemoryMap(addressLow, addressHigh,
			permRead, permWrite, permExecute, shared, offset,
			devMajor, devMinor, inode, pathnameOffset,
			pathnameLength, new String(mapFilename));
		mapsList.add(map);
	    }
	}
	
	BuildMaps constructedMaps = new BuildMaps();
	constructedMaps.construct(ProcessIdentifierFactory.create(getPid()));
	MemoryMap arrayMaps[] = new MemoryMap[constructedMaps.mapsList.size()];
	constructedMaps.mapsList.toArray(arrayMaps);
	this.maps = arrayMaps;

	return maps;
    }

    private String[] cmdLine;
    public String[] getCmdLine() {
	if (cmdLine == null) {
	    class BuildCmdLine extends CmdLineBuilder {
		String[] argv;
		public void buildBuffer (byte[] buf) {
		}
		public void buildArgv (String[] argv) {
		    this.argv = argv;
		}
	    }
	    BuildCmdLine cmdLine = new BuildCmdLine ();
	    cmdLine.construct(ProcessIdentifierFactory.create(getPid()));
	    this.cmdLine = cmdLine.argv;
	}
	return cmdLine;
    }

    public int getUID() {
	Status status = new Status();
	status.scan(ProcessIdentifierFactory.create(getPid()));
	return status.uid;
    }

    public int getGID() {
	Status status = new Status();
	status.scan(ProcessIdentifierFactory.create(getPid()));
	return status.gid;
    }

    /**
     * XXX: This is racy - it can miss file renames.  The alternative
     * would be to have two methods; one returning a file descriptor
     * and a second returning the exe as it should be (but possibly
     * isn't :-).  Better yet have utrace handle it :-)
     */
    private String exe;
    public String getExe() {
	if (exe == null) {
	    ProcessIdentifier pid
		= ProcessIdentifierFactory.create(getPid());
	    String exe = Exe.get(pid);
	    // Linux's /proc/$$/exe can get screwed up in several
	    // ways.  Detect each here and return null.
	    if (exe.endsWith(" (deleted)"))
		// Assume (possibly incorrectly) that a trailing
		// "(deleted)" always indicates a deleted file.
		return null;
	    if (exe.indexOf((char)0) >= 0)
		// Assume that an EXE that has somehow ended up with
		// an embedded NUL character is invalid.  This happens
		// when the kernel screws up "mv a-really-long-file
		// $exe" leaving the updated EXE string with something
		// like "$exe<NUL>ally-long-file (deleted)".
		return null;
	    if (!new File(exe).exists())
		// Final sanity check; the above two should have covered
		// all possible cases.  But one never knows.
		return null;
	    this.exe = exe;
	}
	return exe;
    }

    /**
     * If it hasn't already been read, read the stat structure.
     */
    public Stat getStat ()
    {
	if (stat == null) {
	    stat = new Stat().scan(ProcessIdentifierFactory.create(getPid()));
	}
	return stat;
    }
    private Stat stat;

    public String getCommand() {
	return getStat().comm;
    }

    /**
     * Refresh the Proc.
     */
    public void sendRefresh ()
    {
	// Compare this against the existing taskPool.  ADDED
	// accumulates any tasks added to the taskPool.  REMOVED,
	// starting with all known tasks has any existing tasks
	// removed, so that by the end it contains a set of removed
	// tasks.
	class TidBuilder extends ProcBuilder {
	    Map added = new HashMap ();
	    HashMap removed = (HashMap) ((HashMap)taskPool).clone ();
	    TaskId searchId = new TaskId ();
	    public void build(ProcessIdentifier tid) {
		searchId.id = tid.intValue();
		if (removed.containsKey (searchId)) {
		    removed.remove (searchId);
		}
		else {
		    // Add the process (it currently isn't attached).
		    Task newTask
			= new LinuxPtraceTask(LinuxPtraceProc.this, tid);
		    added.put (newTask.getTaskId(), newTask);
		}
	    }
	}
	TidBuilder tasks = new TidBuilder ();
	tasks.construct(ProcessIdentifierFactory.create(getPid()));
	// Tell each task that no longer exists that it has been
	// removed.
	for (Iterator i = tasks.removed.values().iterator(); i.hasNext();) {
	    LinuxPtraceTask task = (LinuxPtraceTask) i.next ();
	    // XXX: Should there be a TaskEvent.schedule(), instead of
	    // Manager .eventLoop .appendEvent for injecting the event
	    // into the event loop?
	    task.performRemoval ();
	    remove (task);
	}
    }


    /**
     * The current state of this Proc, during a state transition
     * newState is null.
     */
    private LinuxPtraceProcState oldState;
    private LinuxPtraceProcState newState;

    /**
     * Return the current state as a string.
     */
    protected String getStateFIXME() {
	if (newState != null)
	    return newState.toString();
	else if (oldState != null)
	    return oldState.toString();
	else
	    return "<null>";
    }

    /**
     * Return the current state while at the same time marking that
     * the state is in flux. If a second attempt to change state
     * occurs before the current state transition has completed,
     * barf. XXX: Bit of a hack, but at least this prevents state
     * transition code attempting a second recursive state transition.
     */
    private LinuxPtraceProcState oldState() {
	if (newState == null)
	    throw new RuntimeException(this + " double state transition");
	oldState = newState;
	newState = null;
	return oldState;
    }
  
    /**
     * Request that the Proc's task list be refreshed using system
     * tables.
     */
    public void requestRefresh() {
	logger.log(Level.FINE, "{0} requestRefresh\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		public void execute() {
		    newState = oldState().handleRefresh(LinuxPtraceProc.this);
		}
	    });
    }

    /**
     * (Internal) Tell the process that is no longer listed in the
     * system table remove itself.
     *
     * XXX: This should not be public.
     */
    void performRemoval() {
	logger.log(Level.FINEST, "{0} performRemoval -- no longer in /proc\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		public void execute() {
		    newState = oldState().handleRemoval(LinuxPtraceProc.this);
		}
	    });
    }

    /**
     *(Internal) Tell the process that the corresponding task has
     * completed its attach.
     *
     * XXX: Should not be public.
     */
    void performTaskAttachCompleted (final Task theTask) {
	logger.log(Level.FINE, "{0} performTaskAttachCompleted\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		Task task = theTask;

		public void execute() {
		    newState = oldState().handleTaskAttachCompleted(LinuxPtraceProc.this, (LinuxPtraceTask) task);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its detach.
     *
     * XXX: Should not be public.
     */
    void performTaskDetachCompleted(final Task theTask) {
	logger.log(Level.FINE, "{0} performTaskDetachCompleted\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		Task task = theTask;
		public void execute() {
		    newState = oldState().handleTaskDetachCompleted(LinuxPtraceProc.this, (LinuxPtraceTask) task);
		}
	    });
    }

    /**
     * (Internal) Tell the process that the corresponding task has
     * completed its detach.
     */
    void performTaskDetachCompleted(final Task theTask, final Task theClone) {
	logger.log(Level.FINE, "{0} performTaskDetachCompleted/clone\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		Task task = theTask;

		Task clone = theClone;

		public void execute() {
		    newState = oldState().handleTaskDetachCompleted(LinuxPtraceProc.this, (LinuxPtraceTask) task, (LinuxPtraceTask) clone);
		}
	    });
    }

    protected void performDetach() {
	logger.log(Level.FINE, "{0} performDetach\n", this);
	Manager.eventLoop.add(new ProcEvent() {
		public void execute() {
		    newState = oldState().handleDetach(LinuxPtraceProc.this, true);
		}
	    });
    }

    /**
     * (internal) Tell the process to add the specified Observation,
     * attaching the process if necessary.
     */
    void handleAddObservation(TaskObservation observation) {
	newState = oldState().handleAddObservation(this, observation);
    }

    /**
     * (Internal) Tell the process to add the specified Observation,
     * attaching to the process if necessary.
     */
    void requestAddObserver(Task task, TaskObservable observable,
			    TaskObserver observer) {
	logger.log(Level.FINE, "{0} requestAddObservation\n", this);
	Manager.eventLoop.add(new TaskObservation((LinuxPtraceTask)task,
						  observable, observer, true) {
		public void execute() {
		    handleAddObservation(this);
		}
	    });
    }

    /**
     * (Internal) Tell the process to delete the specified
     * Observation, detaching from the process if necessary. Removes a
     * syscallObserver exiting the task from syscall tracing mode of
     * necessary.
     */
    void requestDeleteObserver(Task task, TaskObservable observable,
			       TaskObserver observer) {
	Manager.eventLoop.add(new TaskObservation((LinuxPtraceTask)task,
						  observable, observer, false) {
		public void execute() {
		    newState = oldState().handleDeleteObservation(LinuxPtraceProc.this, this);
		}
	    });
    }

    /**
     * Class describing the action to take on the suspended Task
     * before adding or deleting a Syscall observer.
     */
    final class SyscallAction implements Runnable {
	private final LinuxPtraceTask task;
	private final boolean addition;
	SyscallAction(LinuxPtraceTask task, boolean addition) {
	    this.task = task;
	    this.addition = addition;
	}
	public void run() {
	    int syscallobs = task.syscallObservers.numberOfObservers();
	    if (addition) {
		if (syscallobs == 0)
		    task.startTracingSyscalls();
	    } else {
		if (syscallobs == 0)
		    task.stopTracingSyscalls();
	    }
	}
    }

    /**
     * (Internal) Tell the process to add the specified Observation,
     * attaching to the process if necessary. Adds a syscallObserver
     * which changes the task to syscall tracing mode of necessary.
     */
    void requestAddSyscallObserver(final Task task, TaskObservable observable,
				   TaskObserver observer) {
	logger.log(Level.FINE, "{0} requestAddSyscallObserver\n", this);
	SyscallAction sa = new SyscallAction((LinuxPtraceTask)task, true);
	TaskObservation to = new TaskObservation((LinuxPtraceTask)task,
						 observable, observer, sa,
						 true) {
		public void execute() {
		    handleAddObservation(this);
		}
		public boolean needsSuspendedAction() {
		    return ((LinuxPtraceTask)task).syscallObservers.numberOfObservers() == 0;
		}
	    };
	Manager.eventLoop.add(to);
    }

    /**
     * (Internal) Tell the process to delete the specified
     * Observation, detaching from the process if necessary.
     */
    void requestDeleteSyscallObserver(final Task task,
				      TaskObservable observable,
				      TaskObserver observer) {
	logger.log(Level.FINE, "{0} requestDeleteSyscallObserver\n", this);
	SyscallAction sa = new SyscallAction((LinuxPtraceTask)task, false);
	TaskObservation to = new TaskObservation((LinuxPtraceTask)task,
						 observable, observer, sa,
						 false) {
		public void execute() {
		    newState = oldState().handleDeleteObservation(LinuxPtraceProc.this,
								  this);
		}

		public boolean needsSuspendedAction() {
		    return ((LinuxPtraceTask)task).syscallObservers.numberOfObservers() == 1;
		}
	    };
	Manager.eventLoop.add(to);
    }

    /**
     * Class describing the action to take on the suspended Task
     * before adding or deleting a Code observer.
     */
    final class BreakpointAction implements Runnable {
	private final TaskObserver.Code code;

	private final Task task;

	private final long address;

	private final boolean addition;

	BreakpointAction(TaskObserver.Code code, Task task, long address,
			 boolean addition) {
	    this.code = code;
	    this.task = task;
	    this.address = address;
	    this.addition = addition;
	}

	public void run() {
	    if (addition) {
		boolean mustInstall = breakpoints.addBreakpoint(code, address);
		if (mustInstall) {
		    Breakpoint breakpoint;
		    breakpoint = Breakpoint.create(address, LinuxPtraceProc.this);
		    breakpoint.install(task);
		}
	    } else {
		boolean mustRemove = breakpoints.removeBreakpoint(code, address);
		if (mustRemove) {
		    Breakpoint breakpoint;
		    breakpoint = Breakpoint.create(address, LinuxPtraceProc.this);
		    breakpoint.remove(task);
		}
	    }
	}
    }

    /**
     * (Internal) Tell the process to add the specified Code
     * Observation, attaching to the process if necessary. Adds a
     * TaskCodeObservation to the eventloop which instructs the task
     * to install the breakpoint if necessary.
     */
    void requestAddCodeObserver(Task task, TaskObservable observable,
				TaskObserver.Code observer,
				final long address) {
	logger.log(Level.FINE, "{0} requestAddCodeObserver\n", this);
	BreakpointAction bpa = new BreakpointAction(observer, task, address, true);
	TaskObservation to;
	to = new TaskObservation((LinuxPtraceTask) task, observable, observer,
				 bpa, true) {
		public void execute() {
		    handleAddObservation(this);
		}
		public boolean needsSuspendedAction() {
		    return breakpoints.getCodeObservers(address) == null;
		}
	    };
	Manager.eventLoop.add(to);
    }

    /**
     * (Internal) Tell the process to delete the specified Code
     * Observation, detaching from the process if necessary.
     */
    void requestDeleteCodeObserver(Task task, TaskObservable observable,
				   TaskObserver.Code observer,
				   final long address)    {
	logger.log(Level.FINE, "{0} requestDeleteCodeObserver\n", this);
	BreakpointAction bpa = new BreakpointAction(observer, task, address, false);
	TaskObservation to;
	to = new TaskObservation((LinuxPtraceTask)task, observable, observer, bpa, false) {
		public void execute() {
		    newState = oldState().handleDeleteObservation(LinuxPtraceProc.this, this);
		}

		public boolean needsSuspendedAction() {
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
    final static class InstructionAction implements Runnable {
	public void run()
	{
	    // There is nothing in particular we need to do. We just want
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
     * necessary. As soon as the observation is added and the task
     * isn't blocked it will inform the Instruction observer of every
     * step of the task.
     */
    void requestAddInstructionObserver(final Task task,
				       TaskObservable observable,
				       TaskObserver.Instruction observer) {
	logger.log(Level.FINE, "{0} requestAddInstructionObserver\n", this);
	TaskObservation to;
	InstructionAction ia = new InstructionAction();
	to = new TaskObservation((LinuxPtraceTask)task, observable, observer, ia, true) {
		public void execute() {
		    handleAddObservation(this);
		}

		public boolean needsSuspendedAction() {
		    return ((LinuxPtraceTask)task).instructionObservers.numberOfObservers() == 0;
		}

		// Makes sure that the observer is properly added and then,
		// while the Task is still suspended, updateExecuted() is
		// called. Giving the observer a chance to inspect and
		// possibly block the Task.
		public void add() {
		    super.add();
		    TaskObserver.Instruction i = (TaskObserver.Instruction) observer;
		    if (i.updateExecuted(task) == Action.BLOCK)
			((LinuxPtraceTask)task).blockers.add(observer);
		}
	    };
	Manager.eventLoop.add(to);
    }

    /**
     * (Internal) Tell the process to delete the specified Instruction
     * Observation, detaching and/or suspending from the process if
     * necessary.
     */
    void requestDeleteInstructionObserver(final Task task,
					  TaskObservable observable,
					  TaskObserver.Instruction observer) {
	logger.log(Level.FINE, "{0} requestDeleteInstructionObserver\n", this);
	TaskObservation to;
	InstructionAction ia = new InstructionAction();
	to = new TaskObservation((LinuxPtraceTask)task, observable, observer, ia, false) {
		public void execute() {
		    newState = oldState().handleDeleteObservation(LinuxPtraceProc.this, this);
		}
		public boolean needsSuspendedAction() {
		    return ((LinuxPtraceTask)task).instructionObservers.numberOfObservers() == 1;
		}
	    };
	Manager.eventLoop.add(to);
    }

    /**
     * XXX: Should not be public.
     */
    public final BreakpointAddresses breakpoints;

    // List of available addresses for out of line stepping.
    // Used a lock in getOutOfLineAddress() and doneOutOfLine().
    private final ArrayList outOfLineAddresses = new ArrayList();

    // Whether the Isa has been asked for addresses yet.
    // Guarded by outOfLineAddresses in getOutOfLineAddress.
    private boolean requestedOutOfLineAddresses;

    /**
     * Returns an available address for out of line stepping. Blocks
     * till an address is available. Queries the Isa if not done so
     * before.  Returned addresses should be returned by calling
     * doneOutOfLine().
     */
    long getOutOfLineAddress() {
	synchronized (outOfLineAddresses) {
	    while (outOfLineAddresses.isEmpty()) {
		if (! requestedOutOfLineAddresses) {
		    Isa isa = ((LinuxPtraceTask)getMainTask()).getIsaFIXME();
		    outOfLineAddresses.addAll(isa.getOutOfLineAddresses(this));
		    if (outOfLineAddresses.isEmpty())
			throw new IllegalStateException("Isa.getOutOfLineAddresses"
							+ " returned empty List");
		    requestedOutOfLineAddresses = true;
		} else {
		    try {
			outOfLineAddresses.wait();
		    } catch (InterruptedException ignored) {
			// Just try again...
		    }
		}
	    }
	    return ((Long) outOfLineAddresses.remove(0)).longValue();
	}
    }

    /**
     * Called by Breakpoint with an address returned by
     * getOutOfLineAddress() to put it back in the pool.
     */
    void doneOutOfLine(long address) {
	synchronized (outOfLineAddresses) {
	    outOfLineAddresses.add(Long.valueOf(address));
	    outOfLineAddresses.notifyAll();
	}
    }
}
