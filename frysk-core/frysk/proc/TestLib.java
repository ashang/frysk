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

import frysk.event.SignalEvent;
import frysk.junit.Paths;
import frysk.sys.Errno;
import frysk.sys.Fork;
import frysk.sys.Pid;
import frysk.sys.Poll;
import frysk.sys.Ptrace;
import frysk.sys.Sig;
import frysk.sys.SigSet;
import frysk.sys.Signal;
import frysk.sys.Wait;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import junit.framework.TestCase;

/**
 * Utility for JUnit tests.
 */

public class TestLib
    extends TestCase
{
    /**
     * Return the exec prefix that should be prepended to all
     * programs.
     */
    static String getExecPrefix ()
    {
	return Paths.getExecPrefix ();
    }

    /**
     * Run the event loop for a short period of time until it is
     * explicitly stopped (using EventLoop . requestStop).  During
     * this period poll for external events.
     *
     * XXX: Static to avoid gcc bugs.
     */
    static void assertRunUntilStop (String reason)
    {
	assertTrue ("event loop run explictly stopped (" + reason + ")",
		    Manager.eventLoop.runPolling (5000));
    }

    /**
     * Process all the pending events; no polling of external events
     * is performed.
     *
     * XXX: Static to avoid gcc bugs.
     */
    static void runPending ()
    {
	Manager.eventLoop.runPending ();
    }

    /**
     * Is the Proc an immediate child of PID?
     *
     * XXX: Static to avoid gcc bugs.
     */
    static public boolean isChildOf (int pid, Proc proc)
    {
	// Process 1 has no parent so can't be a child of mine.
	if (proc.getPid () == 1)
	    return false;
	// If the parent's pid matches this processes pid, assume that
	// is sufficient.  Would need a very very long running system
	// for that to not be the case.
	if (proc.parent.getPid () == pid)
	    return true;
	return false;
    }
    /**
     * Is the Proc an immediate child of this Proc?  Do not use
     * host.getSelf() as that, in certain situtations, can lead to
     * infinite recursion.
     *
     * XXX: Static to avoid gcc bugs.
     */
    static public boolean isChildOfMine (Proc proc)
    {
	return isChildOf (Pid.get (), proc);
    }

    /**
     * Is Proc a descendant of PID?
     *
     * XXX: Static to avoid gcc bugs.
     */
    static public boolean isDescendantOf (int pid, Proc proc)
    {
	// Climb the process tree looking for this process.
	while (proc.getPid () > 1) {
	    // The parent's pid match this process, assume that is
	    // sufficient.  Would need a very very long running system
	    // for that to not be the case.
	    if (proc.parent.getPid () == pid)
		return true;
	    proc = proc.parent;
	}
	// Process 1 has no parent so can't be a child of mine.  Do
	// this first as no parent implies .parent==null and that
	// would match a later check.
	return false;
    }

    /**
     * Is the process a descendant of this process?  Do not use
     * host.getSelf() as that, in certain situtations, can lead to
     * infinite recursion.
     *
     * XXX: Static to avoid gcc bugs.
     */
    static public boolean isDescendantOfMine (Proc proc)
    {
	return isDescendantOf (Pid.get (), proc);
    }

    /**
     * Set up a handler that stops the event loop when the child sends
     * this process an ack (indicating that it has completed a
     * requested operation).
     */
    protected class AckHandler
    {
	AckHandler (int sig)
	{
	    this (new int[] { sig });
	}
	// NOTE: Use a different signal to thread add/del.  Within
	// this process the signal is masked and Linux appears to
	// propogate the mask all the way down to the exec'ed child.
	protected final static int signal = Sig.HUP;
	AckHandler ()
	{
	    this (signal);
	}
	private int acksRemaining;
	AckHandler (int[] sigs)
	{
	    acksRemaining = sigs.length;
	    for (int i = 0; i < sigs.length; i++) {
		Manager.eventLoop.add (new SignalEvent (sigs[i]) {
			public void execute ()
			{
			    Manager.eventLoop.requestStop ();
			    Manager.eventLoop.remove (this);
			    acksRemaining--;
			}
		    });
	    }
	}
	void await ()
	{
	    while (acksRemaining > 0) {
		assertRunUntilStop ("waiting for ack");
	    }
	}
    }

    /**
     * Manage a child process.
     *
     * Create a child process and then block until the child has
     * reported back that it has started (using a ackSignal).  Permit
     * various operations on the process, see also the various
     * extensions.
     */
    protected abstract class Child
    {
	private int pid;
	/**
	 * Return the ProcessID of the child.
	 */
	public int getPid ()
	{
	    return pid;
	}
	private String[] argv;
	/**
	 * Return the Process's argv.
	 */
	public String[] getArgv ()
	{
	    return argv;
	}
	/**
	 * Send the child the sig.
	 */
	public void signal (int sig)
	{
	    Signal.tkill (pid, sig);
	}
	/**
	 * Start CHILD as a running process.
	 */
	abstract protected int startChild (String stdin, String stdout,
					   String stderr, String[] argv);
	/**
	 * Create a child process (using startChild), return once the
	 * process is running.  Wait for acknowledge SIG.
	 */
	protected Child (int sig, String[] argv)
	{
	    AckHandler ack = new AckHandler (sig);
	    this.argv = argv;
	    this.pid = startChild (null, "/dev/null", null, argv);
	    registerChild (pid);
	    ack.await ();
	}
	/**
	 * Create a child process (using startChild), return once the
	 * process is running.
	 */
	protected Child (String[] argv)
	{
	    this (AckHandler.signal, argv);
	}
	/**
	 * Attempt to kill the child.  Return false if the child
	 * doesn't appear to exist.
	 */
	boolean kill ()
	{
	    try {
		signal (Sig.KILL);
		return true;
	    }
	    catch (Errno.Esrch e) {
		return false;
	    }
	}
	/**
	 * Find/return the child's Proc, polling /proc if necessary.
	 */
	Proc findProcUsingRefresh (boolean refreshTasks)
	{
	    // Try polling /proc.
	    Manager.host.requestRefreshXXX (refreshTasks);
	    Manager.eventLoop.runPending ();
	    proc = Manager.host.getProc (new ProcId (pid));
	    return proc;
	}
	/**
	 * Like {@link findProcUsingRefresh (boolean)}, but do not
	 * refresh the task list.
	 */
	Proc findProcUsingRefresh ()
	{
	    return findProcUsingRefresh (false);
	}
	private Proc proc;
	/**
	 * Find the child's Proc's main or non-main Task, polling
	 * /proc if necessary.
	 */
	public Task findTaskUsingRefresh (boolean mainTask)
	{
	    Proc proc = findProcUsingRefresh (true);
	    for (Iterator i = proc.getTasks ().iterator (); i.hasNext (); ) {
		Task task = (Task) i.next ();
		if (task.getTid () == proc.getPid ()) {
		    if (mainTask)
			return task;
		}
		else {
		    if (!mainTask)
			return task;
		}
	    }
	    return null;
	}
    }

    /**
     * Create an ack-process that can be manipulated using various
     * signals (see below).
     */
    protected abstract class AckProcess
	extends Child
    {
	static final int childAck = Sig.USR1;
	static final int parentAck = Sig.USR2;
	static final int addCloneSig = Sig.USR1;
	static final int delCloneSig = Sig.USR2;
	static final int addForkSig = Sig.HUP;
	static final int delForkSig = Sig.INT;
	static final int zombieForkSig = Sig.URG;
	static final int execSig = Sig.PWR;
	static final int execCloneSig = Sig.FPE;
	static final String sleepTime = "10";

	private AckProcess (int ack, String[] argv)
	{
	    super (ack, argv);
	}
	/** Create an ack process.  */
	AckProcess ()
	{
	    this (childAck, new String[]
		{
		    getExecPrefix () + "funit-child",
		    sleepTime,
		    // Use getpid as this testsuite always runs the
		    // event loop from the main thread (which has
		    // tid==pid).
		    Integer.toString (Pid.get ()),
		});
	}
	/**
	 * Create an AckProcess; if BUSY, the process will use a
	 * busy-loop, instead of suspending, when waiting for signal
	 * commands.
	 */
	AckProcess (boolean busy)
	{
	    this (childAck, new String[]
		{
		    getExecPrefix () + "funit-child",
		    busy ? "--wait=busy-loop" : "--wait=suspend",
		    sleepTime,
		    // Use getpid as this testsuite always runs the
		    // event loop from the main thread (which has
		    // tid==pid).
		    Integer.toString (Pid.get ()),
		});
	}
	/**
	 * Create an AckProcess; the process will use FILENAME as the
	 * program to exec.
	 */
	AckProcess (String filename)
	{
	    this (childAck, new String[]
		{
		    getExecPrefix () + "funit-child",
		    "--filename=" + getExecPrefix () + filename,
		    sleepTime,
		    // Use getpid as this testsuite always runs the
		    // event loop from the main thread (which has
		    // tid==pid).
		    Integer.toString (Pid.get ()),
		});
	}
	/** Create an AckProcess, and then add COUNT threads.  */
	AckProcess (int count)
	{
	    this ();
	    for (int i = 0; i < count; i++)
		addClone ();
	}
	/** Create a possibly busy AckProcess.  Add COUNT threads.  */
	AckProcess (int count, boolean busy)
	{
	    this (busy);
	    for (int i = 0; i < count; i++)
		addClone ();
	}
	/** . */
	private void spawn (int sig)
	{
	    AckHandler ack = new AckHandler (new int[]
		{
		    childAck, parentAck
		});
	    signal (sig);
	    ack.await ();
	}
	/** Add a Task.  */
	public void addClone ()
	{
	    spawn (addCloneSig);
	}
	/** Delete a Task.  */
	public void delClone ()
	{
	    AckHandler ack = new AckHandler (parentAck);
	    signal (delCloneSig);
	    ack.await ();
	}
	/** Add a child Proc.  */
	public void addFork ()
	{
	    spawn (addForkSig);
	}
	/** Delete a child Proc.  */
	public void delFork ()
	{
	    AckHandler ack = new AckHandler (parentAck);
	    signal (delForkSig);
	    ack.await ();
	}
	/** Terminate a fork Proc (creates zombie).  */
	public void zombieFork ()
	{
	    AckHandler ack = new AckHandler (parentAck);
	    signal (zombieForkSig);
	    ack.await ();
	}
	/**
	 * Kill the parent, expect an ack from the child (there had
	 * better be a child).
	 */
	void fryParent ()
	{
	    AckHandler ack = new AckHandler (childAck);
	    signal (Sig.KILL);
	    ack.await ();
	}
	/**
	 * Request that TID (assumed to be a child) perform an exec
	 * call.
	 */
	void exec (int tid)
	{
	    AckHandler ack = new AckHandler (childAck);
	    Signal.tkill (tid, execSig);
	    ack.await ();
	}
	/**
	 * Request that the main task perform an exec.
	 */
	void exec ()
	{
	    AckHandler ack = new AckHandler (childAck);
	    signal (execSig);
	    ack.await ();
	}
	/**
	 * Request that the cloned task perform an exec.
	 */
	void execClone ()
	{
	    // First the main thread acks with .parentAck, and then
	    // the execed process acks with .childAck.
	    AckHandler ack = new AckHandler (new int[] { parentAck,
							 childAck });
	    signal (execCloneSig);
	    ack.await ();
	}
    }

    /**
     * Create an ack daemon.  An ack daemon has process 1, and not
     * this process, as its parent.
     *
     * Since this a daemon, this process won't have to contend with
     * its exit status - it will go to process 1.
     */
    protected class AckDaemonProcess
	extends AckProcess
    {
	/**
	 * Create the process as a daemon.
	 */
	protected int startChild (String stdin, String stdout, String stderr,
				  String[] argv)
	{
	    return Fork.daemon (stdin, stdout, stderr, argv);
	}
	AckDaemonProcess (int ack, String[] argv)
	{
	    super (ack, argv);
	}
	AckDaemonProcess ()
	{
	    super ();
	}
	AckDaemonProcess (int count)
	{
	    super (count);
	}
	AckDaemonProcess (int count, boolean busy)
	{
	    super (count, busy);
	}
    }


    /**
     * Create a detached child ack process.
     *
     * Since the created process is a direct child of this process,
     * this process will see a wait event when this exits.  It is most
     * useful when a controlled process exit is required (see reap).
     */
    protected class DetachedAckProcess
	extends AckProcess
    {
	DetachedAckProcess ()
	{
	    super ();
	}
	DetachedAckProcess (String filename)
	{
	    super (filename);
	}
	/**
	 * Create a detached process that is a child of this one.
	 */
	protected int startChild (String stdin, String stdout, String stderr,
				  String[] argv)
	{
	    return Fork.exec (stdin, stdout, stderr, argv);
	}
	/**
	 * Reap the child.
	 *
	 * Kill the child, wait for and consume the child's exit
	 * event.
	 */
	void reap ()
	{
	    kill ();
	    try {
		while (true) {
		    Wait.waitAll (getPid (),
				  new FailWaitObserver ("killing child")
			{
			    public void terminated (int pid, boolean signal,
						    int value,
						    boolean coreDumped)
			    {
				// Termination with signal is ok.
				assertTrue ("terminated with signal", signal);
			    }
			});
		}
	    }
	    catch (Errno.Echild e) {
		// No more waitpid events.
	    }
	}
    }

    /**
     * Create an attached child ack process.
     */
    protected class AttachedAckProcess
	extends AckProcess
    {
	AttachedAckProcess ()
	{
	    super ();
	}
	AttachedAckProcess (int count)
	{
	    super (count);
	}
	/**
	 * Create the process as an attached child.
	 */
	protected int startChild (String stdin, String stdout, String stderr,
				  String[] argv)
	{
	    // Capture the child process id as it flys past.
	    class PidObserver
		implements Observer
	    {
		int pid;
		public void update (Observable o, Object obj)
		{
		    Proc proc = (Proc) obj;
		    if (!isChildOfMine (proc))
			return;
		    pid = proc.getPid ();
		    Manager.eventLoop.requestStop ();
		    Manager.host.observableProcAddedXXX.deleteObserver (this);
		}
	    }
	    PidObserver pidObserver = new PidObserver ();
	    Manager.host.observableProcAddedXXX.addObserver (pidObserver);
	    // Start the child process, run the event loop until the
	    // pid is known.
	    Manager.host.requestCreateAttachedProc (stdin, stdout,
						    stderr, argv);
	    assertRunUntilStop ("starting attached child");
	    // Return that captured PID.
	    return pidObserver.pid;
	}
    }

    /**
     * A Task set.
     *
     * In addition to methods for managing the set, there is a method
     * for unblocking all members.
     */
    static class TaskSet
    {
	/**
	 * Set of tasks being managed.
	 */
	private Set tasks = new HashSet ();
	/**
	 * Return the task set as an array.
	 */
	Task[] toArray ()
	{
	    return (Task[]) tasks.toArray (new Task[0]);
	}
	/**
	 * Add the Task to the Set of Task's.
	 */
	void add (Task task)
	{
	    tasks.add (task);
	}
	/**
	 * Clear the Task Set.
	 */
	void clear ()
	{
	    tasks.clear ();
	}
	/**
	 * Return the number of Task's currently in the Task Set.
	 */
	int size ()
	{
	    return tasks.size ();
	}
	/**
	 * Unblock all members of the Task Set.
	 */
	void unblock (TaskObserver observer)
	{
	    for (Iterator i = tasks.iterator (); i.hasNext(); ) {
		Task task = (Task) i.next ();
		task.requestUnblock (observer);
	    }
	}

    }

    /**
     * A TaskObserver base class.  This provides a framework for both
     * automatically adding and implementing TaskObserver's.
     *
     * The client supplied .updateClass method is called as each new
     * task is found.  It should register itself with the applicable
     * observer.
     */
    abstract class TaskObserverBase
	implements TaskObserver
    {
	/**
	 * Count of number of times that this observer was added to a
	 * Task's observer set.
	 */
	int addedCount;
	public void addedTo (Object o)
	{
	    addedCount++;
	}
	/**
	 * Count of number of times this observer was deleted from a
	 * Task's observer set.
	 */
	int deletedCount;
	public void deletedFrom (Object o)
	{
	    deletedCount++;
	}
	/**
	 * The add operation failed, should never happen.
	 */
	public void addFailed (Object o, Throwable w)
	{
	    fail ("add to " + o + " failed");
	}
    }

    /**
     * A TaskObserver base class that, in addition, tracks descendant
     * processes and tasks as they are added.  The sub-class is
     * notified of each new Task as it arrives.
     */
    abstract class AutoAddTaskObserverBase
	extends TaskObserverBase
    {
	/**
	 * Create a TaskObserver, that in addition, registers itself
	 * with the TaskAdded observable so that added tasks can be
	 * tracked.
	 */
	AutoAddTaskObserverBase ()
	{
	    Manager.host.observableTaskAddedXXX.addObserver (new Observer ()
		{
		    public void update (Observable obj, Object o)
		    {
			Task task = (Task) o;
			if (isDescendantOfMine (task.proc))
			    updateTaskAdded (task);
		    }
		});
	}
	/**
	 * A new task appeared, notify the sub-class of the update.
	 */
	abstract void updateTaskAdded (Task task);
    }

    /**
     * Manipulate a temporary file..
     *
     * Creates a temporary file that is automatically removed on test
     * completion..  Unlike File, this doesn't hang onto the
     * underlying file once created.  Consequently, operations such as
     * "exists()" reflect the current state of the file.
     */
    List tmpFiles = new LinkedList ();
    protected class TmpFile
    {
	private File file;
	public String toString ()
	{
	    return file.getPath ();
	}
	public File getFile ()
	{
	    return new File (file.getPath ());
	}
	public boolean stillExists ()
	{
	    return getFile ().exists ();
	}
	public void delete ()
	{
	    file.delete ();
	}
	TmpFile ()
	{
	    String name = TestLib.this.getClass ().getName ();
	    try {
		File pwd = new File (".");
		file = File.createTempFile (name + ".", ".tmp", pwd);
		tmpFiles.add (this);
	    }
	    catch (java.io.IOException e) {
		fail ("Creating temp file " + name);
	    }
	}
    }
    private void deleteTmpFiles ()
    {
	for (Iterator i = tmpFiles.iterator (); i.hasNext (); ) {
	    TmpFile tbd = (TmpFile) i.next ();
	    i.remove ();
	    tbd.delete ();
	}
    }

     
    /**
     * Observer that counts the number of tasks <em>frysk</em> reports
     * as added and removed to the system..
     *
     * This automatically wires itself in using the Proc's procAdded
     * observer.
     */
    protected class TaskCounter
    {
	/**
	 * List of tasks added.
	 */
	List added = new LinkedList ();
	/**
	 * List of tasks removed.
	 */
	List removed = new LinkedList ();
	/**
	 * Only count descendants of this process?
	 */
	private boolean descendantsOnly;
	/**
	 * Create a task counter that monitors task added and removed
	 * events.  If descendantsOnly, limit the count to tasks
	 * belonging to descendant processes.
	 */
	protected TaskCounter (boolean descendantsOnly)
	{
	    this.descendantsOnly = descendantsOnly;
	    Manager.host.observableTaskAddedXXX.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Task task = (Task) obj;
			if (TaskCounter.this.descendantsOnly
			    && !isDescendantOfMine (task.proc))
			    return;
			added.add (task);
		    }
		});
	    Manager.host.observableTaskRemovedXXX.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Task task = (Task) obj;
			if (TaskCounter.this.descendantsOnly
			    && !isDescendantOfMine (task.proc))
			    return;
			removed.add (task);
		    }
		});
	}
	/**
	 * Create a task counter that counts all task add and removed
	 * events.
	 */
	TaskCounter ()
	{
	    this (false);
	}
    }

    /**
     * Observer that counts the number of processes added and
     * removed.
     *
     * Automaticaly registers itself.
     */
    protected class ProcCounter
    {
	// Base count.
	LinkedList added = new LinkedList ();
	LinkedList removed = new LinkedList ();
	private boolean descendantsOnly;
	/**
	 * Create a new ProcCounter counting processes added and
	 * removed.  If descendantsOnly, only count children of this
	 * process.
	 */
	ProcCounter (boolean descendantsOnly)
	{
	    this.descendantsOnly = descendantsOnly;
	    // Set up observers to count proc add and delete events.
	    Manager.host.observableProcAddedXXX.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Proc proc = (Proc) obj;
			if (ProcCounter.this.descendantsOnly
			    && !isDescendantOfMine (proc))
			    return;
			added.add (proc);
		    }
		});
	    Manager.host.observableProcRemovedXXX.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Proc proc = (Proc) obj;
			if (ProcCounter.this.descendantsOnly
			    && !isDescendantOfMine (proc))
			    return;
			removed.add (proc);
		    }
		});
	}
	/**
	 * Count all proc's added and removed.
	 */
	ProcCounter ()
	{
	    this (false);
	}
    }

    /**
     * Count the number of task exec calls.
     */
    protected class ExecCounter
	extends AutoAddTaskObserverBase
	implements TaskObserver.Execed
    {
	int numberExecs;
	public Action updateExeced (Task task)
	{
	    numberExecs++;
	    return Action.CONTINUE;
	}
	void updateTaskAdded (Task task)
	{
	    task.requestAddExecedObserver (ExecCounter.this);
	}
    }

    /**
     * Compute the Fibonacci number of N.  The class contains both the
     * computed value, and the number of resursive calls required to
     * compute that value.
     */
    static protected class Fibonacci
    {
	int callCount;
	int value;
	private int fib (int n)
	{
	    callCount++;
	    switch (n) {
	    case 0: return 0;
	    case 1: return 1;
	    default: return fib (n - 1) + fib (n - 2);
	    }
	}
	Fibonacci (int n)
	{
	    value = fib (n);
	}
    }

    /**
     * Watch for events involving the specified PID process; count the
     * number of events seen.
     */
     class PidCounter
	 implements Observer
     {
	 List what = new LinkedList (); // XXX:
	 int pid;
	 int count = 0;
	 /**
	  * Create a pid counter bound to PID.
	  */
	 PidCounter (int pid)
	 {
	     this.pid = pid;
	 }
	 /**
	  * Create a pid counter bound to PID, and attached to
	  * observable.
	  */
	 PidCounter (int pid, Observable observable)
	 {
	     this (pid);
	     observable.addObserver (this);
	 }
	 public void update (Observable o, Object obj)
	 {
	     if (obj instanceof Proc) {
		 Proc proc = (Proc) obj;
		 if (proc.getPid () == pid) {
		     count++;
		     what.add (new RuntimeException ()); // XXX:
		 }
	     }
	 }
     }

    class IgnoreWaitObserver
	implements Wait.Observer
    {
 	public void cloneEvent (int pid, int clone) { }
 	public void forkEvent (int pid, int child) { }
 	public void exitEvent (int pid, boolean signal, int value, boolean coreDumped) { }
 	public void execEvent (int pid) { }
 	public void syscallEvent (int pid) { }
 	public void stopped (int pid, int signal) { }
 	public void terminated (int pid, boolean signal, int value, boolean coreDumped) { }
	public void disappeared (int pid, Throwable w) { }
    }

    class FailWaitObserver
	implements Wait.Observer
    {
	String message;
	FailWaitObserver (String message)
	{
	    this.message = message;
	}
 	public void cloneEvent (int pid, int clone) { fail (message); }
 	public void forkEvent (int pid, int child) { fail (message); }
 	public void exitEvent (int pid, boolean signal, int value, boolean coreDumped) { fail (message); }
 	public void execEvent (int pid) { fail (message); }
 	public void syscallEvent (int pid) { fail (message); }
 	public void stopped (int pid, int signal) { fail (message); }
 	public void terminated (int pid, boolean signal, int value, boolean coreDumped) { fail (message); }
	public void disappeared (int pid, Throwable w) { fail (message); }
    }

    /**
     * A self installing observer that, when a child process
     * disappears (i.e., exits), stops the event loop.
     */
    protected class StopEventLoopWhenChildProcRemoved
	implements Observer
    {
	boolean p;
	StopEventLoopWhenChildProcRemoved ()
	{
	    Manager.host.observableProcRemovedXXX.addObserver (this);
	}
	public void update (Observable o, Object obj)
	{
	    Proc proc = (Proc) obj;
	    if (isChildOfMine (proc)) {
		// Shut things down.
		Manager.eventLoop.requestStop ();
		p = true;
	    }
	}
    }

    /**
     * Observer that just tells the event loop to stop.
     */
    protected class StopEventLoopObserver
	implements Observer
    {
	public void update (Observable o, Object obj)
	{
	    Manager.eventLoop.requestStop ();
	    o.deleteObserver (this);
	}
    }

    // Maintain a set of children that are killed off after each test
    // run.
    private Set children;
    /**
     * Add the pid to the set of children that should be killed off
     * after the test has run.
     */
    protected final void registerChild (int child)
    {
	// Had better not try to register process one.
	assertFalse ("child is process one", child == 1);
	children.add (new Integer (child));
    }

    /**
     * The host being used by the current test.
     */
    protected Host host;

    public void setUp ()
    {
	children = new HashSet ();
	// Extract a fresh new Host and EventLoop from the Manager.
	host = Manager.resetXXX ();
	// Add every descendant of this process, and all their tasks,
	// to the set of children that should be killed off after the
	// test has run.
	host.observableProcAddedXXX.addObserver (new Observer ()
	    {
		public void update (Observable o, Object obj)
		{
		    Proc proc = (Proc) obj;
		    if (isDescendantOfMine (proc)) {
			registerChild (proc.getPid ());
		    }
		}
	    });
	host.observableTaskAddedXXX.addObserver (new Observer ()
	    {
		public void update (Observable o, Object obj)
		{
		    Task task = (Task) obj;
		    if (isDescendantOfMine (task.proc))
			registerChild (task.getTid ());
		}
	    });
    }

    public void tearDown ()
    {
	// Sig.KILL all the registered children.  Once that signal is
	// processed the task will die.

	for (Iterator i = children.iterator (); i.hasNext (); ) {
	    Integer child = (Integer) i.next ();
	    int pid = child.intValue ();
	    try {
		Signal.kill (pid, Sig.KILL);
	    }
	    catch (Errno.Esrch e) {
		// Toss it.
	    }
	    // Note that there's a problem here with both stopped and
	    // attached tasks.  The Sig.KILL won't be delivered, and
	    // consequently the task won't exit, until that task has
	    // been continued.  Work around this by also sending each
	    // task a continue ...
	    try {
		Signal.kill (pid, Sig.CONT);
	    }
	    catch (Errno.Esrch e) {
		// Toss it.
	    }
	    // ... and a detach.
	    try {
		Ptrace.detach (pid, Sig.KILL);
	    }
	    catch (Errno.Esrch e) {
		// Toss it.
	    }
	}

	// Completely drain the wait event queue.  Doing this firstly
	// ensures that there are no oustanding events to confuse the
	// next test run; secondly reaps any exited childrean
	// eliminating zombies; and finally makes certain that all
	// attached tasks have been terminated.
	//
	// For attached tasks, which will generate non-exit wait
	// events (clone et.al.), the task is detached / killed.
	// Doing that frees up the task so that it can run to exit.

	try {
	    while (true) {
	    	Wait.waitAll (-1, new Wait.Observer ()
		    {
			private void detach (int pid)
			{
			    // Detach with a KILL signal which will
			    // force the task to exit.
			    Ptrace.detach (pid, Sig.KILL);
			}
			public void cloneEvent (int pid, int clone)
			{
			    detach (pid);
			}
			public void forkEvent (int pid, int child)
			{
			    detach (pid);
			}
			public void exitEvent (int pid, boolean signal,
					       int value, boolean coreDumped)
			{
			    detach (pid);
			}
			public void execEvent (int pid)
			{
			    detach (pid);
			}
			public void syscallEvent (int pid)
			{
			    detach (pid);
			}
			public void stopped (int pid, int signal)
			{
			    detach (pid);
			}
			public void terminated (int pid, boolean signal,
						int value, boolean coreDumped)
			{
			}
			public void disappeared (int pid, Throwable w)
			{
			    detach (pid);
			}
		    });
  	    }
	}
        catch (Errno.Echild e) {
	    // No more events.
        }

	// Remove any stray files.
	deleteTmpFiles ();


	// Capture any pending signals; drain them, and then turn
	// around and assert that there weren't any.
	SigSet pending = new SigSet ().getPending ();
	Poll.poll (new Poll.Fds (),
		   new Poll.Observer ()
		   {
		       public void signal (int sig) { }
		       public void pollIn (int in) { }
		   },
		   0);
	// XXX: This should be an assert; until the bugs are fixed, it
	// can't be.
	int[] checkSigs = new int[] { Sig.USR1, Sig.USR2 };
	for (int i = 0; i < checkSigs.length; i++) {
	    int sig = checkSigs[i];
	    // assertFalse ("pending signal", pending.contains (sig));
	    if (pending.contains (sig)) {
		System.out.print ("<<XXX: pending signal "
				  + Sig.toPrintString (sig)
				  + ">>");
	    }
	}
    }
}
