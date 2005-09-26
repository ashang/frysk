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
	int pid;
	/**
	 * Set up a handler that stops the event loop once the child
	 * is up and running (the child signals this using ackSignal).
	 */
	protected void setupAckHandler ()
	{
	    Manager.eventLoop.addHandler (new SignalEvent (ackSignal) {
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
	 * Attempt to kill the child.  Return false if the child
	 * doesn't appear to exist.
	 */
	boolean kill ()
	{
	    try {
		Signal.tkill (pid, Sig.KILL);
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
	 * Create a daemon process (one that's parent has exited
	 * causing it to have process one as the parent).  Also create
	 * CLONES tasks and possibly use POLLING.
	 */
	DaemonChild (int clones, boolean polling)
	{
	    super (new String[] {
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
	    Signal.tkill (pid, Sig.USR1);
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
	    Signal.tkill (pid, Sig.USR2);
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
		    Wait.waitAll (pid, new FailWaitObserver ("killing child")
			{
			    public void terminated (int pid, int signal,
						    boolean coreDumped)
			    {
				// Terminated is ok.
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
	    Signal.tkill (pid, Sig.USR1);
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
	    Signal.tkill (pid, Sig.USR2);
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
	    Signal.tkill (pid, Sig.KILL);
	    waitForAck ();
	}
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
     * Observer that counts the number of tasks added and removed.
     *
     * This automatically wires itself in using the Proc's procAdded
     * observer.
     */
    protected class TaskCounter
    {
	LinkedList added = new LinkedList ();
	LinkedList removed = new LinkedList ();
	int numberAdded ()
	{
	    return added.size ();
	}
	int numberRemoved ()
	{
	    return removed.size ();
	}
	protected TaskCounter ()
	{
	    Manager.host.procAdded.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Proc proc = (Proc) obj;
			proc.taskAdded.addObserver (new Observer ()
			    {
				public void update (Observable o, Object obj)
				{
				    Task task = (Task) obj;
				    added.add (task);
				}
			    });
			proc.taskRemoved.addObserver (new Observer ()
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
    }

    /**
     * Observer that counts the number of processes added and
     * removed.
     *
     * Automaticaly registers itself.
     */
    protected class ProcCounter
	implements Cloneable
    {
	LinkedList added = new LinkedList ();
	int numberAdded ()
	{
	    return added.size ();
	}
	LinkedList removed = new LinkedList ();
	int numberRemoved ()
	{
	    return removed.size ();
	}
	/**
	 * Create a new ProcCounter.
	 */
	ProcCounter ()
	{
	    Manager.host.procAdded.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Proc proc = (Proc) obj;
			added.add (proc);
			// Need to tell system that you want to track
			// fork events.
			proc.taskAdded.addObserver (new Observer () {
				public void update (Observable o, Object obj)
				{
				    Task task = (Task) obj;
				    task.traceFork = true;
				}
			    });
		    }
		});
	    Manager.procRemoved.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Proc proc = (Proc) obj;
			removed.add (proc);
		    }
		});
	}
	/**
	 * Return a copy of the ProcCounter.  The copy will not be
	 * updated as further processes are created and deleted.
	 */
	ProcCounter save ()
	{
	    ProcCounter copy = null;
	    try {
		copy = (ProcCounter) clone ();
		copy.added = (LinkedList) (added.clone ());
		copy.removed = (LinkedList) (removed.clone ());
	    }
	    catch (CloneNotSupportedException e) {
		fail ("CloneNotSupportedException");
	    }
	    return copy;
	}
    }

    /**
     * Count the number of task exec calls.
     */
    protected class ExecCounter
    {
	int numberExecs;
	ExecCounter ()
	{
	    Manager.host.procAdded.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Proc proc = (Proc) obj;
			proc.taskAdded.addObserver (new Observer ()
			    {
				public void update (Observable o, Object obj)
				{
				    Task task = (Task) obj;
				}
			    });
			proc.taskExeced.addObserver (new Observer ()
			    {
				public void update (Observable o, Object obj)
				{
				    numberExecs++;
				}
			    });
		    }
		});
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
 	public void exitEvent (int pid, int status) { }
 	public void execEvent (int pid) { }
 	public void syscallEvent (int pid) { }
 	public void stopped (int pid, int signal) { }
 	public void exited (int pid, int status, boolean coreDumped) { }
 	public void terminated (int pid, int signal, boolean coreDumped) { }
	public void disappeared (int pid) { }
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
 	public void exitEvent (int pid, int status) { fail (message); }
 	public void execEvent (int pid) { fail (message); }
 	public void syscallEvent (int pid) { fail (message); }
 	public void stopped (int pid, int signal) { fail (message); }
 	public void exited (int pid, int status, boolean coreDumped) { fail (message); }
 	public void terminated (int pid, int signal, boolean coreDumped) { fail (message); }
	public void disappeared (int pid) { fail (message); }
    }

    /**
     * Send a host SIGKILL to the specified task;
     */
    void sigKill (Task task)
    {
	Signal.tkill (task.id.id, Sig.KILL);
    }

    /**
     * Observer that tells the event loop to stop when the "top"
     * process has been removed.
     *
     * XXX: The heuristic being used here needs to be improved; It
     * won't work well when the process pool includes unattached
     * processes addeved via refresh.
     */
    protected class StopEventLoopOnProcDestroy
    {
	StopEventLoopOnProcDestroy ()
	{
	    Manager.procRemoved.addObserver (new Observer ()
		{
		    public void update (Observable o, Object obj)
		    {
			Proc process = (Proc) obj;
			if (process.parent == null) {
			    Manager.eventLoop.requestStop ();
			}
		    }
		});
	}
    }

    // Maintain a list of children that are killed off after each test
    // run.
    List children;
    void registerChild (int child)
    {
	children.add (new Integer (child));
    }

    public void setUp ()
    {
	children = new LinkedList ();
	Manager.resetXXX ();
    }

    public void tearDown ()
    {
	IgnoreWaitObserver ignoreWaits = new IgnoreWaitObserver ();

	// Sig.KILL all the registered children.  Once that signal is
	// processed the task will die.

	for (Iterator i = children.iterator (); i.hasNext (); ) {
	    Integer child = (Integer) i.next ();
	    int pid = child.intValue ();
	    try {
		Signal.kill (child.intValue (), Sig.KILL);
	    }
	    catch (Errno.Esrch e) {
		// Toss it (expecting the occasional kill).
	    }
	    // Note that there's a problem here with both stopped and
	    // attached tasks.  The Sig.KILL won't be delivered, and
	    // consequently the task won't exit, until that task has
	    // been continued.  Work around this by also sending each
	    // task a continue.
	    try {
		Signal.kill (child.intValue (), Sig.CONT);
	    }
	    catch (Errno.Esrch e) {
		// Toss it (expecting the occasional kill).
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
			public void exitEvent (int pid, int status)
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
			public void exited (int pid, int status, boolean coreDumped)
			{
			}
			public void terminated (int pid, int signal, boolean coreDumped)
			{
			}
			public void disappeared (int pid)
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
