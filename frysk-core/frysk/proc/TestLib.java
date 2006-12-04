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
import frysk.junit.TestCase;
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
import frysk.sys.proc.ProcBuilder;
import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for JUnit tests.
 */

public class TestLib
    extends TestCase
{
    protected final static Logger logger = Logger.getLogger ("frysk");//.proc");
    /**
     * Log the integer ARG squeezed between PREFIX and SUFFIX.
     */
    protected void log (String prefix, int arg, String suffix)
    {
	if (logger.isLoggable (Level.FINE))
	    logger.log (Level.FINE,
			"{0} " + prefix + "{1,number,integer}" + suffix,
			new Object[] {
			    this,
			    new Integer (arg)
			});
    }

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
    static void assertRunUntilStop (int timeout, String reason)
    {
	logger.log (Level.FINE, "{0} assertRunUntilStop start: {1}\n",
		    new Object[] { TestLib.class, reason });
	assertTrue ("event loop run explictly stopped (" + reason + ")",
		    Manager.eventLoop.runPolling (timeout * 1000));
	logger.log (Level.FINE, "{0} assertRunUntilStop stop: {1}\n",
		    new Object[] { TestLib.class, reason });
    }

    /**
     * Run the event loop for a short period of time until it is
     * explicitly stopped (using EventLoop . requestStop).  During
     * this period poll for external events.
     *
     * XXX: Static to avoid gcc bugs.
     */
    protected static void assertRunUntilStop (String reason)
    {
	assertRunUntilStop (5, reason);
    }

    /**
     * Process all the pending events; no polling of external events
     * is performed.
     *
     * XXX: Static to avoid gcc bugs.
     */
    protected static void runPending ()
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
     * Sets up a set of signal handlers that stop the event loop when
     * the child sends this process the required set of ack signals
     * (indicating that it has completed a requested operation).
     */
    protected class AckHandler
    {
	private int acksRemaining;
	private String reason;
	private class AckSignal
	    extends SignalEvent
	{
	    AckSignal (Sig sig)
	    {
		super (sig);
	    }
	    public void execute ()
	    {
		logger.log (Level.FINE, "{0} execute ({1})\n",
			    new Object[] { this, reason });
		Manager.eventLoop.requestStop ();
		Manager.eventLoop.remove (this);
		acksRemaining--;
	    }
	}

	AckHandler (Sig[] sigs, String why)
	{
	    StringBuffer reason = new StringBuffer (why);
	    reason.append (" (");
	    acksRemaining = sigs.length;
	    for (int i = 0; i < sigs.length; i++) {
		Sig sig = sigs[i];
		if (i > 0)
		    reason.append (",");
		reason.append (sig);
		Manager.eventLoop.add (new AckSignal (sig));
	    }
	    reason.append (")");
	    this.reason = reason.toString ();
	}
	AckHandler (Sig sig, String why)
	{
	    this (new Sig[] { sig }, why);
	}
	public String toString ()
	{
	    return super.toString () + "," + reason;
	}
	private void assertAwait (String why)
	{
	    while (acksRemaining > 0) {
		assertRunUntilStop (why);
	    }
	}
	void await ()
	{
	    assertAwait (reason);
	}
	void await (String why)
	{
	    assertAwait (why + " (" + reason + ")");
	}
    }

    // NOTE: Use a different signal to thread add/del.  Within this
    // process the signal is masked and Linux appears to propogate the
    // mask all the way down to the exec'ed child.
    protected final Sig ackSignal = Sig.HUP;
    protected final Sig childAck = Sig.USR1;
    protected final Sig parentAck = Sig.USR2;
    protected final Sig addCloneSig = Sig.USR1;
    protected final Sig delCloneSig = Sig.USR2;
    protected final Sig stopSig = Sig.STOP;
    protected final Sig addForkSig = Sig.HUP;
    protected final Sig delForkSig = Sig.INT;
    protected final Sig zombieForkSig = Sig.URG;
    protected final Sig execSig = Sig.PWR;
    protected final Sig execCloneSig = Sig.FPE;
    protected final Sig[] spawnAck = new Sig[] { childAck, parentAck };
    protected final Sig[] execAck = new Sig[] { childAck };

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
	public void signal (Sig sig)
	{
	    Signal.tkill (pid, sig);
	}
	public void signal (int tid, Sig sig)
	{
	    Signal.tkill (tid, sig);
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
	protected Child (Sig sig, String[] argv)
	{
	    AckHandler ack = new AckHandler (sig, "startChild");
	    this.argv = argv;
	    this.pid = startChild (null,
				   (logger.isLoggable (Level.FINE)
				    ? null
				    : "/dev/null"),
				   null, argv);
	    killDuringTearDown (pid);
	    ack.await ();
	}
	/**
	 * Create a child process (using startChild), return once the
	 * process is running.
	 */
	protected Child (String[] argv)
	{
	    this (ackSignal, argv);
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
	public Proc assertFindProcAndTasks ()
	{
	    class FindProc
		implements Host.FindProc
	    {
		Proc proc;
		public void procFound (ProcId procId)
		{
		    proc = Manager.host.getProc (procId);
		    Manager.eventLoop.requestStop();
		}
		public void procNotFound (ProcId procId, Exception e)
		{
		    fail ("Couldn't find the given proc");
		}
	    }
	    FindProc findProc = new FindProc ();
	    Manager.host.requestFindProc( new ProcId(pid), findProc);
	    Manager.eventLoop.run();
	    return findProc.proc;
	}
	
	/**
	 * Find the child's Proc's main or non-main Task, polling
	 * /proc if necessary.
	 */
	public Task findTaskUsingRefresh (boolean mainTask)
	{
	    Proc proc = assertFindProcAndTasks();
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
     * Build an funit-child command to run.
     */
    private static String[] funitChildCommand (boolean busy,
					       String filename,
					       String[] argv)
    {
	List command = new LinkedList ();
	final String sleepTime = "10";
	command.add (getExecPrefix () + "funit-child");
	command.add (busy ? "--wait=busy-loop" : "--wait=suspend");
	if (filename != null)
	    command.add ("--filename=" + getExecPrefix () + filename);
	command.add (sleepTime);
	// Use getpid as this testsuite always runs the event loop
	// from the main thread (which has tid==pid).
	command.add (Integer.toString (Pid.get ()));
	// Append any arguments.
	if (argv != null)
	    for (int n = 0; n < argv.length; n++)
		command.add (argv[n]);
	return (String[]) command.toArray (new String[0]);
    }
    /**
     * Create an ack-process that can be manipulated using various
     * signals (see below).
     */
    protected abstract class AckProcess
	extends Child
    {
	private AckProcess (Sig ack, String[] argv)
	{
	    super (ack, argv);
	}
	/** Create an ack process.  */
	AckProcess ()
	{
	    this (childAck, funitChildCommand (false, null, null));
	}
	/**
	 * Create an AckProcess; if BUSY, the process will use a
	 * busy-loop, instead of suspending, when waiting for signal
	 * commands.
	 */
	AckProcess (boolean busy)
	{
	    this (childAck, funitChildCommand (busy, null, null));
	}
	/**
	 * Create an AckProcess; the process will use FILENAME and
	 * ARGV as the program to exec.
	 */
	AckProcess (String filename, String[] argv)
	{
	    this (childAck, funitChildCommand (false, filename, argv));
	}
	/** Create an AckProcess, and then add COUNT threads.  */
	AckProcess (int count)
	{
	    this ();
	    for (int i = 0; i < count; i++)
		assertSendAddCloneWaitForAcks ();
	}
	/** Create a possibly busy AckProcess.  Add COUNT threads.  */
	AckProcess (int count, boolean busy)
	{
	    this (busy);
	    for (int i = 0; i < count; i++)
		assertSendAddCloneWaitForAcks ();
	}
	/**
	 * Tell TID to create a new offspring.  Wait for the
	 * acknowledgment.
	 */
	private void spawn (int tid, Sig sig, String why)
	{
	    AckHandler ack = new AckHandler (spawnAck, why);
	    signal (tid, sig);
	    ack.await ();
	}
	/** Add a Task.  */
	public void assertSendAddCloneWaitForAcks ()
	{
	    spawn (getPid (), addCloneSig, "assertSendAddCloneWaitForAcks");
	}
	/** Add a Task.  */
	public void assertSendAddCloneWaitForAcks (int tid)
	{
	    spawn (tid, addCloneSig, "addClone");
	}
	/** Delete a Task.  */
	public void assertSendDelCloneWaitForAcks ()
	{
	    AckHandler ack = new AckHandler (parentAck,
					     "assertSendDelCloneWaitForAcks");
	    signal (delCloneSig);
	    ack.await ();
	}
	/** Add a child Proc.  */
	public void assertSendAddForkWaitForAcks ()
	{
	    spawn (getPid (), addForkSig, "assertSendAddForkWaitForAcks");
	}
	/** Add a child Proc.  */
	public void assertSendAddForkWaitForAcks (int tid)
	{
	    spawn (tid, addForkSig, "addFork");
	}
	/** Delete a child Proc.  */
	public void assertSendDelForkWaitForAcks ()
	{
	    AckHandler ack = new AckHandler (parentAck,
					     "assertSendDelForkWaitForAcks");
	    signal (delForkSig);
	    ack.await ();
	}
	/** Terminate a fork Proc (creates zombie).  */
	public void assertSendZombieForkWaitForAcks ()
	{
	    AckHandler ack = new AckHandler (parentAck,
					     "assertSendZombieForkWaitForAcks");
	    signal (zombieForkSig);
	    ack.await ();
	}
	/**
	 * Kill the parent, expect an ack from the child (there had
	 * better be a child).
	 */
	void assertSendFryParentWaitForAcks ()
	{
	    AckHandler ack = new AckHandler (childAck,
					     "assertSendFryParentWaitForAcks");
	    signal (Sig.KILL);
	    ack.await ();
	}
	/**
	 * Request that TID (assumed to be a child) perform an exec
	 * call.
	 */
	void assertSendExecWaitForAcks (int tid)
	{
	    AckHandler ack = new AckHandler (execAck,
					     "assertSendExecWaitForAcks:"
					     + tid);
	    Signal.tkill (tid, execSig);
	    ack.await ();
	}
	/**
	 * Request that the main task perform an exec.
	 */
	void assertSendExecWaitForAcks ()
	{
	    assertSendExecWaitForAcks (getPid ());
	}
	/**
	 * Request that the cloned task perform an exec.
	 */
	void assertSendExecCloneWaitForAcks ()
	{
	    // First the main thread acks with .parentAck, and then
	    // the execed process acks with .childAck.
	    AckHandler ack = new AckHandler (new Sig[]
		{
		    parentAck,
		    childAck
		}, "assertSendExecCloneWaitForAcks");
	    signal (execCloneSig);
	    ack.await ();
	}
	/**
	 * Stop a Task.  XXX: Nothing confirming that the task
	 * actually stopped.
	 */
	public void sendStopXXX ()
	{
	    signal (stopSig);
	    // XXX: there is nothing confirming that this operation
	    // completed!!!
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
	public AckDaemonProcess (Sig ack, String[] argv)
	{
	    super (ack, argv);
	}
	public AckDaemonProcess ()
	{
	    super ();
	}
	AckDaemonProcess (boolean busy)
	{
	    super (busy);
	}
	public AckDaemonProcess (int count)
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
	public DetachedAckProcess ()
	{
	    super ();
	}
	DetachedAckProcess (Sig ack, String[] argv)
	{
	    super (ack, argv);
	}
	DetachedAckProcess (String filename, String[] argv)
	{
	    super (filename, argv);
	}
	public DetachedAckProcess (int count)
	{
	    super (count);
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
	    class TidObserver
		extends TaskObserverBase
		implements TaskObserver.Attached
	    {
		int tid;
		public Action updateAttached (Task task)
		{
		    tid = task.getTid ();
		    Manager.eventLoop.requestStop ();
		    return Action.CONTINUE;
		}
	    }
	    TidObserver tidObserver = new TidObserver ();
	    // Start the child process, run the event loop until the
	    // tid is known.
	    Manager.host.requestCreateAttachedProc (stdin, stdout, stderr,
						    argv, tidObserver);
	    assertRunUntilStop ("starting attached child");
	    // Return that captured TID.
	    return tidObserver.tid;
	}
    }

    
    protected class AckDaemonCloneProcess
      extends AckDaemonProcess
  {
    public AckDaemonCloneProcess (int threads)
    {
      super(childAck, funitThreadsCommand(threads));
    }

  }

  /**
   * Build an funit-child command to run.
   */
  private static String[] funitThreadsCommand (int threads)
  {
    List command = new LinkedList();
    final String sleepTime = "10";

    final String sigToSend = "" + Sig.USR1_;

    command.add(getExecPrefix() + "funit-threads");
    // Use getpid as this testsuite always runs the event loop
    // from the main thread (which has tid==pid).
    command.add(Integer.toString(Pid.get()));
    command.add(sigToSend);
    command.add(sleepTime);
    command.add("" + threads);
    return (String[]) command.toArray(new String[0]);
  }
    
    
    /**
     * Creates an attached process halted at it's first instruction
     * (just after the exec call to start it running).
     */
    protected class AttachedDaemonProcess
    {
	final Task mainTask;
	TaskObserver.Execed execBlockingObserver;
	/**
	 * Create an attached process blocked at it's entry-point
	 * (i.e., just after the exec).
	 */
	public AttachedDaemonProcess (String[] argv)
	{
	    // Create the child.
	    AckProcess child = new DetachedAckProcess ((String)null, argv);
	    this.mainTask = child.findTaskUsingRefresh (true);
	    // Create and add an exec observer that blocks the task.
	    class ExecBlockingObserver
		extends TaskObserverBase
		implements TaskObserver.Execed
	    {
		public void addedTo (Object o)
		{
		    super.addedTo (o);
		    Manager.eventLoop.requestStop ();
		}
		public Action updateExeced (Task task)
		{
		    Manager.eventLoop.requestStop ();
		    return Action.BLOCK;
		}
	    }
	    execBlockingObserver = new ExecBlockingObserver ();
	    mainTask.requestAddExecedObserver (execBlockingObserver);
	    assertRunUntilStop ("add exec observer to AttachedDaemonProcess");
	    // Run to the exec call.
	    Signal.tkill (mainTask.getTid (), execSig);
	    assertRunUntilStop ("run to exec");
	}
	/**
	 * Resume the attached process.
	 */
	void resume ()
	{
	    mainTask.requestUnblock (execBlockingObserver);
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
	TaskSet add (Task task)
	{
	    tasks.add (task);
	    return this;
	}
	/**
	 * Clear the Task Set.
	 */
	TaskSet clear ()
	{
	    tasks.clear ();
	    return this;
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
	TaskSet unblock (TaskObserver observer)
	{
	    for (Iterator i = tasks.iterator (); i.hasNext(); ) {
		Task task = (Task) i.next ();
		task.requestUnblock (observer);
	    }
	    return this;
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
    public abstract class TaskObserverBase
	implements TaskObserver
    {
	/**
	 * Count of number of times that this observer was added to a
	 * Task's observer set.
	 */
	int addedCount;
	public void addedTo (Object o)
	{
	    logger.log(Level.FINE, "{0} addedTo\n", this);
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
		logger.log(Level.FINE, "{0} update {1} has been removed stopping event loop\n", new Object[]{this, proc});
		Manager.eventLoop.requestStop ();
		p = true;
	    }else{
		logger.log(Level.FINER, "{0} update {1} has been removed NOT stopping event loop because the parent of proc is {2}\n", new Object[]{this, proc, proc.getParent()});
	    }
	}
    }

    /**
     * An observer that stops the eventloop when the process
     * with the given pid is removed.
     */
    protected static class StopEventLoopWhenProcRemoved
	implements Observer
    {
	boolean p;
	int pid;
	StopEventLoopWhenProcRemoved (int pid)
	{
            this.pid = pid;
	    Manager.host.observableProcRemovedXXX.addObserver (this);
	}
	public void update (Observable o, Object obj)
	{
	    Proc proc = (Proc) obj;
	    if (proc.getPid() == this.pid) {
		// Shut things down.
		logger.log(Level.FINE, "{0} update {1} has been removed stopping event loop\n", new Object[]{this, proc});
		Manager.eventLoop.requestStop ();
		p = true;
	    }else{
	    	logger.log(Level.FINE, "{0} update {1} has been removed NOT stopping event loop\n", new Object[]{this, proc});
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

    /**
     * A set of children that are killed off after each test run.
     */
    private Set tidsToKillDuringTearDown;
    /**
     * Add the pid to the set of tidsToKillDuringTearDown that should
     * be killed off during tearDown.
     */
    protected final void killDuringTearDown (int pid)
    {
	// Had better not try to register process one.
	assertFalse ("child is process one", pid == 1);
	Integer i = new Integer (pid);
	tidsToKillDuringTearDown.add (i);
	logger.log (Level.FINE, "{0} killDuringTearDown {1,number,integer}\n",
		    new Object[] { this, i });
    }

    /**
     * The host being used by the current test.
     */
    protected Host host;

    public void setUp ()
    {
	logger.log (Level.FINE, "{0} <<<<<<<<<<<<<<<< start setUp\n", this);
	tidsToKillDuringTearDown = new HashSet ();
	// Extract a fresh new Host and EventLoop from the Manager.
	host = Manager.resetXXX ();
	// Add every descendant of this process, and all their tasks,
	// to the set of tidsToKillDuringTearDown that should be
	// killed off after the test has run.
	host.observableProcAddedXXX.addObserver (new Observer ()
	    {
		public void update (Observable o, Object obj)
		{
		    Proc proc = (Proc) obj;
		    if (isDescendantOfMine (proc))
			killDuringTearDown (proc.getPid ());
		}
	    });
	logger.log (Level.FINE, "{0} <<<<<<<<<<<<<<<< end setUp\n", this);
    }

    /**
     * Try to blow away the child, catch a failure.
     */
    private boolean capturedSendTkill (int pid)
    {
	try {
	    Signal.tkill (pid, Sig.KILL);
	    log ("{0} tkill -KILL ", pid, "\n");
	}
	catch (Errno.Esrch e) {
	    // Toss it.
	    log ("tkill -KILL ", pid, " (failed - ESRCH)\n");
	    return false;
	}
	return true;
    }

    /**
     * Sequence a task through CONT, detach, and KILL.  Return false
     * if it is suspected that the task no longer exists.
     *
     * Detaching with KILL on early utrace kernels has proven
     * problematic - nothing happened (now fixed) - avoid any such
     * issues by doing a simple detach followed by a KILL.
     *
     * There is a problem with both stopped and attached tasks.  The
     * Sig.KILL won't be delivered, and consequently the task won't
     * exit, until that task has been continued.  Work around this by
     * first sending all tasks a continue ...
     */
    private boolean capturedSendDetachContKill (int pid)
    {
	boolean exists = true;
	// Do the detach
	try {
	    Ptrace.detach (pid, 0);
	    log ("detach ", pid, "\n");
	}
	catch (Errno.Esrch e) {
	    // Toss it.
	    log ("detach ", pid, " (failed - ESRCH)\n");
	}
	// Unblock the thread
	try {
	    Signal.tkill (pid, Sig.CONT);
	    log ("tkill -CONT ", pid, "\n");
	}
	catch (Errno.Esrch e) {
	    // Toss it.
	    log ("tkill -CONT ", pid, " (failed - ESRCH)\n");
	}
	// Finally send it a kill to finish things off.
	exists = capturedSendTkill (pid) && exists;
	return exists;
    }

    public void tearDown ()
    {
	logger.log (Level.FINE, "{0} >>>>>>>>>>>>>>>> start tearDown\n", this);

	// Check that there are no pending signals that should have
	// been drained as part of testing.  Do this <em>before</em>
	// any tasks are killed off so that the check isn't confused
	// by additional signals generated by the dieing tasks.
	Sig[] checkSigs = new Sig[] { Sig.USR1, Sig.USR2 };
	SigSet pendingSignals = new SigSet ().getPending ();
	for (int i = 0; i < checkSigs.length; i++) {
	    Sig sig = checkSigs[i];
	    assertFalse ("pending signal " + sig,
			 pendingSignals.contains (sig));
	}

	// Make a preliminary pass through all the registered
	// tidsToKillDuringTearDown trying to simply kill
	// each. Someone else may have waited on their deaths already.
	for (Iterator i = tidsToKillDuringTearDown.iterator ();
	     i.hasNext (); ) {
	    Integer child = (Integer) i.next ();
	    capturedSendTkill (child.intValue ());
	}

	// Go through all registered processes / tasks adding any of
	// their clones to the kill-list.  Do this after the initial
	// blast as, hopefully, that has stopped many of the threads
	// dead in their tracks.
	{
	    ProcBuilder missingTidsToKillDuringTearDown = new ProcBuilder ()
		{
		    public void buildId (int id)
		    {
			killDuringTearDown (id);		
		    }
		};
	    // Iterate over a copy of the tids's collection as the
	    // missingTidsToKillDuringTearDown may modify the
	    // underlying collection.
	    Object[] tidsToKill = tidsToKillDuringTearDown.toArray ();
	    for (int i = 0; i < tidsToKill.length; i++) {
		Integer child = (Integer) tidsToKill[i];
		int pid = child.intValue ();
		missingTidsToKillDuringTearDown.construct (pid);
	    }
	}

	// Blast all the processes for real.
	for (Iterator i = tidsToKillDuringTearDown.iterator ();
	     i.hasNext (); ) {
	    Integer child = (Integer) i.next ();
	    capturedSendDetachContKill (child.intValue ());
	}

	// Drain the wait event queue.  This ensures that: there are
	// no outstanding events to confuse the next test run; all
	// child zombies have been reaped (and eliminated); and
	// finally makes certain that all attached tasks have been
	// terminated.
	//
	// For attached tasks, which will generate non-exit wait
	// events (clone et.al.), the task is detached / killed.
	// Doing that frees up the task so that it can run to exit.
	try {
	    while (!tidsToKillDuringTearDown.isEmpty()) {
		logger.log (Level.FINE, "{0} waitAll -1 ...\n", this);
	    	Wait.waitAll (-1, new Wait.Observer ()
		    {
			public void cloneEvent (int pid, int clone)
			{
			    capturedSendDetachContKill (pid);
			}
			public void forkEvent (int pid, int child)
			{
			    capturedSendDetachContKill (pid);
			}
			public void exitEvent (int pid, boolean signal,
					       int value, boolean coreDumped)
			{
			    capturedSendDetachContKill (pid);
			    // Do not remove PID from
			    // tidsToKillDuringTearDown list; need to
			    // let the terminated event behind it
			    // bubble up.
			}
			public void execEvent (int pid)
			{
			    capturedSendDetachContKill (pid);
			}
			public void syscallEvent (int pid)
			{
			    capturedSendDetachContKill (pid);
			}
			public void stopped (int pid, int signal)
			{
			    capturedSendDetachContKill (pid);
			}
			private void drainTerminated (int pid)
			{
			    // To be absolutly sure, again make
			    // certain that the thread is detached.
			    capturedSendDetachContKill (pid);
			    // True tidsToKillDuringTearDown can have
			    // a second exit status behind this first
			    // one, drain that also.  Give up when
			    // this PID has no outstanding events.
			    try {
				while (true) {
				    log ("waitAll ", pid, " ...\n");
				    Wait.waitAll (pid,
						  new IgnoreWaitObserver ());
				    log ("{0} waitAll ", pid, " ok\n");
				}
			    }
			    catch (Errno.Echild e) {
				log ("waitAll ", pid, " (failed - ECHLD)\n");
			    }
			    // Hopefully done with this PID.
			    tidsToKillDuringTearDown.remove(new Integer(pid));
			}
			public void terminated (int pid, boolean signal,
						int value, boolean coreDumped)
			{
			    drainTerminated (pid);
			}
			public void disappeared (int pid, Throwable w)
			{
			    // The task vanished somehow, drain it.
			    drainTerminated (pid);
			}
		    });
  	    }
	}
        catch (Errno.Echild e) {
	    // No more events.
        }

	// Remove any stray files.
	deleteTmpFiles ();

	// Drain the set of pending signals.  Note that the process of
	// killing off the processes used in the test can generate
	// extra signals - for instance a SIGUSR1 from a detached
	// child that notices that it's parent just exited.
	class SignalDrain
	    implements Poll.Observer
	{
	    SigSet pending = new SigSet ();
	    public void signal (Sig sig) { pending.add (sig); }
	    public void pollIn (int in) { }

	}
	SignalDrain signalDrain = new SignalDrain ();
	Poll.poll (signalDrain, 0);

	logger.log (Level.FINE, "{0} >>>>>>>>>>>>>>>> end tearDown\n", this);
    }
}
