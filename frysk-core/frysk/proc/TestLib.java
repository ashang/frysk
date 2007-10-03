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

import frysk.sys.Errno;
import frysk.sys.Ptrace;
import frysk.sys.Sig;
import frysk.sys.Pid;
import frysk.sys.Fork;
import frysk.sys.Signal;
import frysk.sys.Wait;
import frysk.event.SignalEvent;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import junit.framework.TestCase;

/**
 * Utility for JUnit tests.
 */

public class TestLib
    extends TestCase
{
    /**
     * Run the event loop for a short period of time until it is
     * explicitly stopped (using EventLoop . requestStop).  During
     * this period poll for external events.
     */
    void assertRunUntilStop (String reason)
    {
	assertTrue ("Event loop run was explictly stopped (" + reason + ")",
		    Manager.eventLoop.runPolling (5000));
    }

    /**
     * Process all the pending events; no polling of external events
     * is performed.
     */
    void runPending ()
    {
	Manager.eventLoop.runPending ();
    }

    /**
     * Is the Proc an immediate child of PID?
     */
    public boolean isChildOf (int pid, Proc proc)
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
     */
    public boolean isChildOfMine (Proc proc)
    {
	return isChildOf (Pid.get (), proc);
    }

    /**
     * Is Proc a descendant of PID?
     */
    public boolean isDescendantOf (int pid, Proc proc)
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
     */
    public boolean isDescendantOfMine (Proc proc)
    {
	return isDescendantOf (Pid.get (), proc);
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
	// NOTE: Use a different signal to thread add/del.  Within
	// this process the signal is masked and Linux appears to
	// propogate the mask all the way down to the exec'ed child.
	protected final static int ackSignal = Sig.HUP;
	private int pid;
	/**
	 * Return the ProcessID of the child.
	 */
	public int getPid ()
	{
	    return pid;
	}
	/**
	 * Send the child the sig.
	 */
	public void signal (int sig)
	{
	    Signal.tkill (pid, sig);
	}
	/**
	 * Set up a handler that stops the event loop once the child
	 * is up and running (the child signals this using ackSignal).
	 */
	protected void setupAckHandler ()
	{
	    Manager.eventLoop.add (new SignalEvent (ackSignal)
		{
		    public void execute ()
		    {
			Manager.eventLoop.requestStop ();
			Manager.eventLoop.remove (this);
		    }
		});
	}
	/**
	 * Wait for the child to acknowledge the operation (using a
	 * signal)
	 */
	protected void waitForAck ()
	{
	    assertRunUntilStop ("waiting for ack");
	}
	/**
	 * Start CHILD as a running process.
	 */
	abstract protected int startChild (String stdin, String stdout,
					   String stderr, String[] argv);
	/**
	 * Create a child process (using startChild), return once the
	 * process is running.
	 */
	protected Child (String[] argv)
	{
	    setupAckHandler ();
	    pid = startChild (null, "/dev/null", null, argv);
	    registerChild (pid);
	    waitForAck ();
	}
	/**
	 * Fudge up a Child object using PID.
	 */
	protected Child (int pid)
	{
	    this.pid = pid;
	    registerChild (pid);
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
	    // See if it is already known.
	    if (proc == null) {
		proc = Manager.host.getProc (new ProcId (pid));
	    }
	    // Try polling /proc.
	    if (proc == null) {
		Manager.host.requestRefresh (refreshTasks);
		Manager.eventLoop.runPending ();
		proc = Manager.host.getProc (new ProcId (pid));
	    }
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
     * Create a child object corresponding to an existing PID.
     */
    protected class PidChild
	extends Child
    {
	PidChild (int pid)
	{
	    super (pid);
	}
	protected int startChild (String stdin, String stdout, String stderr,
				  String[] argv)
	{
	    throw new RuntimeException ("should not be here");
	}
    }

    /**
     * Create a daemon child process.
     *
     * Since a daemon process has process 1 as its parent, it won't
     * lead to any wait events (or at least events that this process
     * can see).
     */
    protected class DaemonChild
	extends Child
    {
	protected int startChild (String stdin, String stdout, String stderr,
				  String[] argv)
	{
	    return Fork.daemon (stdin, stdout, stderr, argv);
	}
	/**
	 * Create a daemon child running ARGV.
	 */
	DaemonChild (String[] argv)
	{
	    super (argv);
	}
	/**
	 * Create a daemon process (one that's parent has exited
	 * causing it to have process one as the parent).  Also create
	 * CLONES tasks and possibly use POLLING.
	 */
	DaemonChild (int clones, boolean polling)
	{
	    this (new String[] {
		      "./prog/kill/detach",
		      Integer.toString (Pid.get ()),
		      Integer.toString (ackSignal),
		      "20",
		      Integer.toString (clones),
		      "0",
		      polling ? "1" : "0"
		  });
	}
	/**
	 * Create a daemon child process that includes CLONES
	 * threads.  Block until both the process and all threads have
	 * been started.
	 */
	DaemonChild (int clones)
	{
	    this (clones, false);
	}
	/**
	 * Create a daemon child process.  Block until the process
	 * has started.
	 */
	DaemonChild ()
	{
	    this (0);
	}
	/**
	 * Ask the daemon process to add another task.  Run the
	 * eventLoop until the daemon acknowledges that it has
	 * completed the operation.
	 */
	void addTask ()
	{
	    setupAckHandler ();
	    // Sig.USR2 indicates add a thread.
	    signal (Sig.USR1);
	    waitForAck ();
	}
	/**
	 * Ask the daemon process to Delete a task.  Run the eventLoop
	 * until the child acknowledges it has completed the
	 * operation.
	 */
	void delTask ()
	{
	    setupAckHandler ();
	    // Sig.USR2 indicates drop a thread.
	    signal (Sig.USR2);
	    waitForAck ();
	}
    }

    /**
     * Create a detached child process.
     *
     * Since the created process is a direct child of this process,
     * this process will see a wait event when this exits.  It is most
     * useful when a controlled process exit is required (see reap).
     */
    protected class DetachedChild
	extends DaemonChild
    {
	protected int startChild (String stdin, String stdout, String stderr,
				  String[] argv)
	{
	    return Fork.exec (stdin, stdout, stderr, argv);
	}
	/**
	 * Create a child process with no threads.
	 */
	DetachedChild ()
	{
	    super ();
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
				assertTrue ("terminated with signal",
					    signal);
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
     * Create an attached child process.
     */
    protected class AttachedChild
	extends DaemonChild
    {
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
		    Manager.host.observableProcAdded.deleteObserver (this);
		}
	    }
	    PidObserver pidObserver = new PidObserver ();
	    Manager.host.observableProcAdded.addObserver (pidObserver);
	    // Start the child process, run the event loop until the
	    // pid is known.
	    Manager.host.requestCreateAttachedProc (stdin, stdout,
						    stderr, argv);
	    assertRunUntilStop ("starting attached child");
	    // Return that captured PID.
	    return pidObserver.pid;
	}
	AttachedChild ()
	{
	    super ();
	}
	AttachedChild (int clones)
	{
	    super (clones);
	}
	AttachedChild (int clones, boolean polling)
	{
	    super (clones, polling);
	}
    }

    /**
     * Create a daemon process that, on demand, creates a child
     * process that can, when requested, be turned into a zombie.
     */
    protected class ZombieChild
	extends Child
    {
	/**
	 * Create a child process as per request from super-object.
	 */
	protected int startChild (String stdin, String stdout, String stderr,
				  String[] argv)
	{
	    return Fork.daemon (stdin, stdout, stderr, argv);
	}
	/**
	 * Create a zombied child process.
	 */
	ZombieChild ()
	{
	    super (new String[] {
		       "./prog/kill/zombie",
		       Integer.toString (Pid.get ()),
		       Integer.toString (ackSignal),
		       "20"
		   });
	}
	/**
	 * Sanity check, no sense in trying to do a kill when there is
	 * no child.
	 */
	private boolean hasChild = false;
	/**
	 * Tell the zombie maker to create one child.
	 */
	void addChild ()
	{
	    setupAckHandler ();
	    signal (Sig.USR1);
	    waitForAck ();
	    hasChild = true;
	}
	/**
	 * Tell the zombie maker to kill it's child turning it into a
	 * zombie.
	 */
	void fryChild ()
	{
	    assertTrue ("Zombie has a child", hasChild);
	    setupAckHandler ();
	    signal (Sig.USR2);
	    waitForAck ();
	    hasChild = false;
	}
	/**
	 * Kill the parent, expect an ack from the child (there had
	 * better be a child).
	 */
	void fryParent ()
	{
	    assertTrue ("Zombie has a child", hasChild);
	    setupAckHandler ();
	    signal (Sig.KILL);
	    waitForAck ();
	}
    }

    /**
     * A Task set.
     *
     * In addition to methods for managing the set, there is a method
     * for unblocking all members.
     */
    class TaskSet
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
	public void added (Throwable e)
	{
	    assertNull ("Observer successfully added", e);
	    addedCount++;
	}
	/**
	 * Count of number of times this observer was deleted from a
	 * Task's observer set.
	 */
	int deletedCount;
	public void deleted ()
	{
	    deletedCount++;
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
	    Manager.host.observableProcAdded.addObserver (new Observer ()
		{
		    public void update (Observable obj, Object o)
		    {
			Proc proc = (Proc) o;
			if (!isDescendantOfMine (proc))
			    return;
			proc.observableTaskAdded.addObserver (new Observer ()
			    {
				public void update (Observable obj, Object o)
				{
				    Task task = (Task) o;
				    updateTaskAdded (task);
				}
			    });
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
	    Manager.host.observableProcAdded.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Proc proc = (Proc) obj;
			if (TaskCounter.this.descendantsOnly
			    && !isDescendantOfMine (proc))
			    return;
			proc.observableTaskAdded.addObserver (new Observer ()
			    {
				public void update (Observable o, Object obj)
				{
				    Task task = (Task) obj;
				    added.add (task);
				}
			    });
			proc.observableTaskRemoved.addObserver (new Observer ()
			    {
				public void update (Observable o, Object obj)
				{
				    Task task = (Task) obj;
				    removed.add (task);
				}
			    });
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
	    Manager.host.observableProcAdded.addObserver (new Observer ()
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
	    Manager.host.observableProcRemoved.addObserver (new Observer ()
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
    protected class Fibonacci
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
	    Manager.host.observableProcRemoved.addObserver (this);
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
	assertTrue ("child is not process 1", child != 1);
	children.add (new Integer (child));
    }

    public void setUp ()
    {
	children = new HashSet ();
	Manager.resetXXX ();
	// Add every descendant of this process, and all their tasks,
	// to the set of children that should be killed off after the
	// test has run.
	Manager.host.observableProcAdded.addObserver (new Observer ()
	    {
		public void update (Observable o, Object obj)
		{
		    Proc proc = (Proc) obj;
		    if (isDescendantOfMine (proc)) {
			registerChild (proc.getPid ());
			proc.observableTaskAdded.addObserver (new Observer ()
			    {
				public void update (Observable o, Object obj)
				{
				    Task task = (Task) obj;
				    registerChild (task.getTid ());
				}
			    });
		    }
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
    }
}
