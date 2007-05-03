// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

import frysk.testbed.TearDownFile;
import frysk.testbed.TearDownProcess;
// import frysk.event.SignalEvent;
import frysk.junit.TestCase;
import frysk.Config;
import frysk.sys.Errno;
import frysk.sys.Fork;
import frysk.sys.Pid;
import frysk.sys.Sig;
import frysk.sys.SignalSet;
import frysk.sys.Signal;
import frysk.sys.Wait;
import frysk.sys.UnhandledWaitBuilder;
import frysk.sys.proc.Stat;
import frysk.testbed.SignalWaiter;
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
  protected final static Logger logger = Logger.getLogger("frysk");

  /**
   * Log the integer ARG squeezed between PREFIX and SUFFIX.
   */
  protected void log (String prefix, int arg, String suffix)
  {
    if (logger.isLoggable(Level.FINE))
      logger.log(Level.FINE, "{0} " + prefix + "{1,number,integer}" + suffix,
                 new Object[] { this, new Integer(arg) });
  }

    /**
     * Return an absolute path as a string, or null of FILE is null.
     */
    private static String getAbsolutePath (File file, String suffix)
    {
	if (file != null)
	    return new File (file, suffix).getAbsolutePath ();
	else
	    return null;
    }

    /**
     * Return a String specifying the absolute path of the executable.
     */
    protected static String getExecPath (String program)
    {
	return getAbsolutePath (Config.getPkgLibDir (), program);
    }
  
    /**
     * Return a String specifying the absolute path of the executable.
     */
    protected static String getExec32Path (String program)
    {
	return getAbsolutePath (Config.getPkgLib32Dir (), program);
    }
  
    /**
     * Return a String specifying the absolute path of the executable.
     */
    protected static String getExec64Path (String program)
    {
	return getAbsolutePath (Config.getPkgLib64Dir (), program);
    }


  /**
   * Run the event loop for a short period of time until it is explicitly
   * stopped (using EventLoop . requestStop). During this period poll for
   * external events. XXX: Static to avoid gcc bugs.
   */
  static void assertRunUntilStop (long timeout, String reason)
  {
    logger.log(Level.FINE, "{0} assertRunUntilStop start: {1}\n",
               new Object[] { TestLib.class, reason });
    assertTrue("event loop run explictly stopped (" + reason + ")",
               Manager.eventLoop.runPolling(timeout * 1000));
    logger.log(Level.FINE, "{0} assertRunUntilStop stop: {1}\n",
               new Object[] { TestLib.class, reason });
  }

  /**
   * Run the event loop for a short period of time until it is explicitly
   * stopped (using EventLoop . requestStop). During this period poll for
   * external events. XXX: Static to avoid gcc bugs.
   */
  protected static void assertRunUntilStop (String reason)
  {
      assertRunUntilStop(getTimeoutSeconds (), reason);
  }

  /**
   * Process all the pending events; no polling of external events is performed.
   * XXX: Static to avoid gcc bugs.
   */
  protected static void runPending ()
  {
    Manager.eventLoop.runPending();
  }

  /**
   * Is the Proc an immediate child of PID? XXX: Static to avoid gcc bugs.
   */
  static public boolean isChildOf (int pid, Proc proc)
  {
    logger.log(Level.FINE, "isChildOf pid: {0} proc: {1}\n",
               new Object[] { new Integer(pid), proc });

    // Process 1 has no parent so can't be a child of mine.
    if (proc.getPid() == 1)
      {
        logger.log(Level.FINE, "isChildOf proc is init\n");
        return false;
      }

    // If the parent's pid matches this processes pid, assume that
    // is sufficient. Would need a very very long running system
    // for that to not be the case.

    Stat stat = new Stat();
    stat.refresh(proc.getPid());

    if (stat.ppid == pid)
      {
        logger.log(Level.FINE, "isChildOf proc is child\n");
        return true;
      }
    logger.log(
               Level.FINE,
               "isChildOf proc not child pid: {0} ppid: {1} parent: {2} proc: {3}\n",
               new Object[] { new Integer(pid), new Integer(stat.ppid),
                             proc.getParent(), proc });

    return false;
  }

  /**
   * Is the Proc an immediate child of this Proc? Do not use host.getSelf() as
   * that, in certain situtations, can lead to infinite recursion. XXX: Static
   * to avoid gcc bugs.
   */
  static public boolean isChildOfMine (Proc proc)
  {
    return isChildOf(Pid.get(), proc);
  }

  /**
   * Is Proc a descendant of PID? XXX: Static to avoid gcc bugs.
   */
  static public boolean isDescendantOf (int pid, Proc proc)
  {
    // Climb the process tree looking for this process.
    while (proc.getPid() > 1)
      {
        // The parent's pid match this process, assume that is
        // sufficient. Would need a very very long running system
        // for that to not be the case.
        if (proc.parent.getPid() == pid)
          return true;
        proc = proc.parent;
      }
    // Process 1 has no parent so can't be a child of mine. Do
    // this first as no parent implies .parent==null and that
    // would match a later check.
    return false;
  }

  /**
   * Is the process a descendant of this process? Do not use host.getSelf() as
   * that, in certain situtations, can lead to infinite recursion. XXX: Static
   * to avoid gcc bugs.
   */
  static public boolean isDescendantOfMine (Proc proc)
  {
    return isDescendantOf(Pid.get(), proc);
  }

  // NOTE: Use a different signal to thread add/del. Within this
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
   * Manage a child process. Create a child process and then block until the
   * child has reported back that it has started (using a ackSignal). Permit
   * various operations on the process, see also the various extensions.
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
      Signal.tkill(pid, sig);
    }

    public void signal (int tid, Sig sig)
    {
      Signal.tkill(tid, sig);
    }

    /**
     * Start CHILD as a running process.
     */
    abstract protected int startChild (String stdin, String stdout,
                                       String stderr, String[] argv);

    /**
     * Create a child process (using startChild), return once the process is
     * running. Wait for acknowledge SIG.
     */
    protected Child (Sig sig, String[] argv)
    {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, sig,
					    "startChild");
	this.argv = argv;
	this.pid = startChild(null, (logger.isLoggable(Level.FINE) ? null
				     : "/dev/null"),
			      null, argv);
	TearDownProcess.add(pid);
	ack.assertRunUntilSignaled();
    }

    /**
     * Create a child process (using startChild), return once the process is
     * running.
     */
    protected Child (String[] argv)
    {
      this(ackSignal, argv);
    }

    /**
     * Attempt to kill the child. Return false if the child doesn't appear to
     * exist.
     */
    boolean kill ()
    {
      try
        {
          signal(Sig.KILL);
          return true;
        }
      catch (Errno.Esrch e)
        {
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
          proc = Manager.host.getProc(procId);
          Manager.eventLoop.requestStop();
        }

        public void procNotFound (ProcId procId, Exception e)
        {
          fail("Couldn't find the given proc");
        }
      }
      FindProc findProc = new FindProc();
      Manager.host.requestFindProc(new ProcId(pid), findProc);
      Manager.eventLoop.run();
      return findProc.proc;
    }

    /**
     * Find the child's Proc's main or non-main Task, polling /proc if
     * necessary.
     */
    public Task findTaskUsingRefresh (boolean mainTask)
    {
      Proc proc = assertFindProcAndTasks();
      for (Iterator i = proc.getTasks().iterator(); i.hasNext();)
        {
          Task task = (Task) i.next();
          if (task.getTid() == proc.getPid())
            {
              if (mainTask)
                return task;
            }
          else
            {
              if (! mainTask)
                return task;
            }
        }
      return null;
    }
  }

  /**
   * Build an funit-child command to run.
   */
  private static String[] funitChildCommand (boolean busy, String filename,
                                             String[] argv)
  {
    List command = new LinkedList();
    final String sleepTime = "10";
    command.add (getExecPath ("funit-child"));
    command.add(busy ? "--wait=busy-loop" : "--wait=suspend");
    if (filename != null)
	command.add("--filename=" + getExecPath (filename));
    command.add(sleepTime);
    // Use getpid as this testsuite always runs the event loop
    // from the main thread (which has tid==pid).
    command.add(Integer.toString(Pid.get()));
    // Append any arguments.
    if (argv != null)
      for (int n = 0; n < argv.length; n++)
        command.add(argv[n]);
    return (String[]) command.toArray(new String[0]);
  }

  /**
   * Create an ack-process that can be manipulated using various signals (see
   * below).
   */
  protected abstract class AckProcess
      extends Child
  {
    private AckProcess (Sig ack, String[] argv)
    {
      super(ack, argv);
    }

    /** Create an ack process. */
    AckProcess ()
    {
      this(childAck, funitChildCommand(false, null, null));
    }

    /**
     * Create an AckProcess; if BUSY, the process will use a busy-loop, instead
     * of suspending, when waiting for signal commands.
     */
    AckProcess (boolean busy)
    {
      this(childAck, funitChildCommand(busy, null, null));
    }

    /**
     * Create an AckProcess; the process will use FILENAME and ARGV as the
     * program to exec.
     */
    AckProcess (String filename, String[] argv)
    {
      this(childAck, funitChildCommand(false, filename, argv));
    }

    /** Create an AckProcess, and then add COUNT threads. */
    AckProcess (int count)
    {
      this();
      for (int i = 0; i < count; i++)
        assertSendAddCloneWaitForAcks();
    }

    /** Create a possibly busy AckProcess. Add COUNT threads. */
    AckProcess (int count, boolean busy)
    {
      this(busy);
      for (int i = 0; i < count; i++)
        assertSendAddCloneWaitForAcks();
    }

    /**
     * Tell TID to create a new offspring. Wait for the acknowledgment.
     */
    private void spawn (int tid, Sig sig, String why)
    {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, spawnAck, why);
	signal(tid, sig);
	ack.assertRunUntilSignaled();
    }

    /** Add a Task. */
    public void assertSendAddCloneWaitForAcks ()
    {
      spawn(getPid(), addCloneSig, "assertSendAddCloneWaitForAcks");
    }

    /** Add a Task. */
    public void assertSendAddCloneWaitForAcks (int tid)
    {
      spawn(tid, addCloneSig, "addClone");
    }

    /** Delete a Task. */
    public void assertSendDelCloneWaitForAcks ()
    {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, parentAck,
					    "assertSendDelCloneWaitForAcks");
	signal(delCloneSig);
	ack.assertRunUntilSignaled();
    }

    /** Add a child Proc. */
    public void assertSendAddForkWaitForAcks ()
    {
      spawn(getPid(), addForkSig, "assertSendAddForkWaitForAcks");
    }

    /** Add a child Proc. */
    public void assertSendAddForkWaitForAcks (int tid)
    {
      spawn(tid, addForkSig, "addFork");
    }

    /** Delete a child Proc. */
    public void assertSendDelForkWaitForAcks ()
    {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, parentAck,
					    "assertSendDelForkWaitForAcks");
	signal(delForkSig);
	ack.assertRunUntilSignaled();
    }

    /** Terminate a fork Proc (creates zombie). */
    public void assertSendZombieForkWaitForAcks ()
    {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, parentAck,
					    "assertSendZombieForkWaitForAcks");
	signal(zombieForkSig);
	ack.assertRunUntilSignaled();
    }

    /**
     * Kill the parent, expect an ack from the child (there had better be a
     * child).
     */
    void assertSendFryParentWaitForAcks ()
    {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, childAck,
					    "assertSendFryParentWaitForAcks");
	signal(Sig.KILL);
	ack.assertRunUntilSignaled();
    }

    /**
     * Request that TID (assumed to be a child) perform an exec call.
     */
    void assertSendExecWaitForAcks (int tid)
    {
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop, execAck,
					    "assertSendExecWaitForAcks:" + tid);
	Signal.tkill(tid, execSig);
	ack.assertRunUntilSignaled();
    }

    /**
     * Request that the main task perform an exec.
     */
    void assertSendExecWaitForAcks ()
    {
      assertSendExecWaitForAcks(getPid());
    }

    /**
     * Request that the cloned task perform an exec.
     */
    void assertSendExecCloneWaitForAcks ()
    {
	// First the main thread acks with .parentAck, and then the
	// execed process acks with .childAck.
	SignalWaiter ack = new SignalWaiter(Manager.eventLoop,
					    new Sig[] { parentAck, childAck },
					    "assertSendExecCloneWaitForAcks");
	signal(execCloneSig);
	ack.assertRunUntilSignaled();
    }

    /**
     * Stop a Task.
     */
    public void assertSendStop ()
    {
      signal(stopSig);

      Stat stat = new Stat();
      stat.refresh(this.getPid());
      for (int i = 0; i < 10; i++)
        {
          if (stat.state == 'T')
            return;
          Thread.yield();
          stat.refresh();
        }

      fail("Stop signal not handled by process, in state: " + stat.state);
    }
  }

  /**
   * Create an ack daemon. An ack daemon has process 1, and not this process, as
   * its parent. Since this a daemon, this process won't have to contend with
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
      return Fork.daemon(stdin, stdout, stderr, argv);
    }

    public AckDaemonProcess (Sig ack, String[] argv)
    {
      super(ack, argv);
    }

    public AckDaemonProcess ()
    {
      super();
    }

    AckDaemonProcess (boolean busy)
    {
      super(busy);
    }

    public AckDaemonProcess (int count)
    {
      super(count);
    }

    AckDaemonProcess (int count, boolean busy)
    {
      super(count, busy);
    }
  }

  /**
   * Create a detached child ack process. Since the created process is a direct
   * child of this process, this process will see a wait event when this exits.
   * It is most useful when a controlled process exit is required (see reap).
   */
  protected class DetachedAckProcess
      extends AckProcess
  {
    public DetachedAckProcess ()
    {
      super();
    }

    DetachedAckProcess (Sig ack, String[] argv)
    {
      super(ack, argv);
    }

    DetachedAckProcess (String filename, String[] argv)
    {
      super(filename, argv);
    }

    public DetachedAckProcess (int count)
    {
      super(count);
    }

    /**
     * Create a detached process that is a child of this one.
     */
    protected int startChild (String stdin, String stdout, String stderr,
                              String[] argv)
    {
      return Fork.exec(stdin, stdout, stderr, argv);
    }

    /**
     * Reap the child. Kill the child, wait for and consume the child's exit
     * event.
     */
    void reap ()
    {
      kill();
      try {
          while (true) {
	      Wait.waitAll(getPid(), new UnhandledWaitBuilder ()
		  {
		      protected void unhandled (String why)
		      {
			  fail ("killing child (" + why + ")");
		      }
		      public void terminated (int pid, boolean signal,
					      int value, boolean coreDumped)
		      {
			  // Termination with signal is ok.
			  assertTrue("terminated with signal", signal);
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
    public AttachedAckProcess ()
    {
      super();
    }

    AttachedAckProcess (int count)
    {
      super(count);
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
          tid = task.getTid();
          Manager.eventLoop.requestStop();
          return Action.CONTINUE;
        }
      }
      TidObserver tidObserver = new TidObserver();
      // Start the child process, run the event loop until the
      // tid is known.
      Manager.host.requestCreateAttachedProc(stdin, stdout, stderr, argv,
                                             tidObserver);
      assertRunUntilStop("starting attached child");
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

    command.add (getExecPath ("funit-threads"));
    // Use getpid as this testsuite always runs the event loop
    // from the main thread (which has tid==pid).
    command.add(Integer.toString(Pid.get()));
    command.add(sigToSend);
    command.add(sleepTime);
    command.add("" + threads);
    return (String[]) command.toArray(new String[0]);
  }

  /**
   * Creates an attached process halted at it's first instruction (just after
   * the exec call to start it running).
   */
  protected class AttachedDaemonProcess
  {
    final Task mainTask;

    TaskObserver.Execed execBlockingObserver;

    /**
     * Create an attached process blocked at it's entry-point (i.e., just after
     * the exec).
     */
    public AttachedDaemonProcess (String[] argv)
    {
      // Create the child.
      AckProcess child = new DetachedAckProcess((String) null, argv);
      this.mainTask = child.findTaskUsingRefresh(true);
      // Create and add an exec observer that blocks the task.
      class ExecBlockingObserver
          extends TaskObserverBase
          implements TaskObserver.Execed
      {
        public void addedTo (Object o)
        {
          super.addedTo(o);
          Manager.eventLoop.requestStop();
        }

        public Action updateExeced (Task task)
        {
          Manager.eventLoop.requestStop();
          return Action.BLOCK;
        }
      }
      execBlockingObserver = new ExecBlockingObserver();
      mainTask.requestAddExecedObserver(execBlockingObserver);
      assertRunUntilStop("add exec observer to AttachedDaemonProcess");
      // Run to the exec call.
      Signal.tkill(mainTask.getTid(), execSig);
      assertRunUntilStop("run to exec");
    }

    /**
     * Resume the attached process.
     */
    public void resume ()
    {
      mainTask.requestUnblock(execBlockingObserver);
    }
  }

  /**
   * A Task set. In addition to methods for managing the set, there is a method
   * for unblocking all members.
   */
  static class TaskSet
  {
    /**
     * Set of tasks being managed.
     */
    private Set tasks = new HashSet();

    /**
     * Return the task set as an array.
     */
    Task[] toArray ()
    {
      return (Task[]) tasks.toArray(new Task[0]);
    }

    /**
     * Add the Task to the Set of Task's.
     */
    TaskSet add (Task task)
    {
      tasks.add(task);
      return this;
    }

    /**
     * Clear the Task Set.
     */
    TaskSet clear ()
    {
      tasks.clear();
      return this;
    }

    /**
     * Return the number of Task's currently in the Task Set.
     */
    int size ()
    {
      return tasks.size();
    }

    /**
     * Unblock all members of the Task Set.
     */
    TaskSet unblock (TaskObserver observer)
    {
      for (Iterator i = tasks.iterator(); i.hasNext();)
        {
          Task task = (Task) i.next();
          task.requestUnblock(observer);
        }
      return this;
    }

  }

  /**
   * A TaskObserver base class. This provides a framework for both automatically
   * adding and implementing TaskObserver's. The client supplied .updateClass
   * method is called as each new task is found. It should register itself with
   * the applicable observer.
   */
  public abstract class TaskObserverBase
      implements TaskObserver
  {
    /**
     * Count of number of times that this observer was added to a Task's
     * observer set.
     */
    int addedCount;

    public void addedTo (Object o)
    {
      logger.log(Level.FINE, "{0} addedTo\n", this);
      addedCount++;
    }

    /**
     * Count of number of times this observer was deleted from a Task's observer
     * set.
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
      fail("add to " + o + " failed");
    }
  }

  /**
   * Observer that counts the number of tasks <em>frysk</em> reports as added
   * and removed to the system.. This automatically wires itself in using the
   * Proc's procAdded observer.
   */
  protected class TaskCounter
  {
    /**
     * List of tasks added.
     */
    List added = new LinkedList();

    /**
     * List of tasks removed.
     */
    List removed = new LinkedList();

    /**
     * Only count descendants of this process?
     */
    private boolean descendantsOnly;

    /**
     * Create a task counter that monitors task added and removed events. If
     * descendantsOnly, limit the count to tasks belonging to descendant
     * processes.
     */
    protected TaskCounter (boolean descendantsOnly)
    {
      this.descendantsOnly = descendantsOnly;
      Manager.host.observableTaskAddedXXX.addObserver(new Observer()
      {
        public void update (Observable o, Object obj)
        {
          Task task = (Task) obj;
          if (TaskCounter.this.descendantsOnly
              && ! isDescendantOfMine(task.proc))
            return;
          added.add(task);
        }
      });
      Manager.host.observableTaskRemovedXXX.addObserver(new Observer()
      {
        public void update (Observable o, Object obj)
        {
          Task task = (Task) obj;
          if (TaskCounter.this.descendantsOnly
              && ! isDescendantOfMine(task.proc))
            return;
          removed.add(task);
        }
      });
    }

    /**
     * Create a task counter that counts all task add and removed events.
     */
    TaskCounter ()
    {
      this(false);
    }
  }

  /**
   * Observer that counts the number of processes added and removed.
   * Automaticaly registers itself.
   */
  protected class ProcCounter
  {
    // Base count.
    LinkedList added = new LinkedList();

    LinkedList removed = new LinkedList();

    private boolean descendantsOnly;

    /**
     * Create a new ProcCounter counting processes added and removed. If
     * descendantsOnly, only count children of this process.
     */
    ProcCounter (boolean descendantsOnly)
    {
      this.descendantsOnly = descendantsOnly;
      // Set up observers to count proc add and delete events.
      Manager.host.observableProcAddedXXX.addObserver(new Observer()
      {
        public void update (Observable o, Object obj)
        {
          Proc proc = (Proc) obj;
          if (ProcCounter.this.descendantsOnly && ! isDescendantOfMine(proc))
            return;
          added.add(proc);
        }
      });
      Manager.host.observableProcRemovedXXX.addObserver(new Observer()
      {
        public void update (Observable o, Object obj)
        {
          Proc proc = (Proc) obj;
          if (ProcCounter.this.descendantsOnly && ! isDescendantOfMine(proc))
            return;
          removed.add(proc);
        }
      });
    }

    /**
     * Count all proc's added and removed.
     */
    ProcCounter ()
    {
      this(false);
    }
  }

  /**
   * Compute the Fibonacci number of N. The class contains both the computed
   * value, and the number of resursive calls required to compute that value.
   */
  static protected class Fibonacci
  {
    int callCount;

    int value;

    private int fib (int n)
    {
      callCount++;
      switch (n)
        {
        case 0:
          return 0;
        case 1:
          return 1;
        default:
          return fib(n - 1) + fib(n - 2);
        }
    }

    Fibonacci (int n)
    {
      value = fib(n);
    }
  }

  /**
   * Watch for events involving the specified PID process; count the number of
   * events seen.
   */
  class PidCounter
      implements Observer
  {
    List what = new LinkedList(); // XXX:

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
     * Create a pid counter bound to PID, and attached to observable.
     */
    PidCounter (int pid, Observable observable)
    {
      this(pid);
      observable.addObserver(this);
    }

    public void update (Observable o, Object obj)
    {
      if (obj instanceof Proc)
        {
          Proc proc = (Proc) obj;
          if (proc.getPid() == pid)
            {
              count++;
              what.add(new RuntimeException()); // XXX:
            }
        }
    }
  }

  /**
   * An observer that stops the eventloop when the process with the given pid is
   * removed.
   */
  protected static class StopEventLoopWhenProcRemoved
      implements Observer
  {
    boolean p;

    int pid;

    StopEventLoopWhenProcRemoved (int pid)
    {
      this.pid = pid;
      Manager.host.observableProcRemovedXXX.addObserver(this);
    }

    public void update (Observable o, Object obj)
    {
      Proc proc = (Proc) obj;
      if (proc.getPid() == this.pid)
        {
          // Shut things down.
          logger.log(Level.FINE,
                     "{0} update {1} has been removed stopping event loop\n",
                     new Object[] { this, proc });
          Manager.eventLoop.requestStop();
          p = true;
        }
      else
        {
          logger.log(
                     Level.FINE,
                     "{0} update {1} has been removed NOT stopping event loop\n",
                     new Object[] { this, proc });
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
      Manager.eventLoop.requestStop();
      o.deleteObserver(this);
    }
  }

  /**
   * The host being used by the current test.
   */
  protected Host host;

  public void setUp ()
  {
    logger.log(Level.FINE, "{0} <<<<<<<<<<<<<<<< start setUp\n", this);
    // Extract a fresh new Host and EventLoop from the Manager.
    host = Manager.resetXXX();
    // Detect all test processes added to the process tree,
    // registering each with TearDownProcess list. Look both for
    // children of this process, and children of any processes already
    // marked to be killed. The latter is to catch children of
    // children, such as daemons.
    //
    // Note that, in addition to this, the Child code also directly
    // registers its process. That is to ensure that children that
    // never get entered into the process tree also get registered
    // with TearDownProcess.
    host.observableProcAddedXXX.addObserver(new Observer()
    {
      public void update (Observable o, Object obj)
      {
        Proc proc = (Proc) obj;
        if (isChildOfMine(proc))
          {
            TearDownProcess.add(proc.getPid());
            return;
          }
        Proc parent = proc.getParent();
        if (parent != null)
          {
            int parentPid = proc.getParent().getPid();
            if (TearDownProcess.contains(parentPid))
              {
                TearDownProcess.add(proc.getPid());
                return;
              }
          }
      }
    });
    logger.log(Level.FINE, "{0} <<<<<<<<<<<<<<<< end setUp\n", this);
  }

  public void tearDown ()
  {
    logger.log(Level.FINE, "{0} >>>>>>>>>>>>>>>> start tearDown\n", this);

    // Check that there are no pending signals that should have
    // been drained as part of testing. Do this <em>before</em>
    // any tasks are killed off so that the check isn't confused
    // by additional signals generated by the dieing tasks.
    Sig[] checkSigs = new Sig[] { Sig.USR1, Sig.USR2 };
    SignalSet pendingSignals = new SignalSet().getPending();
    for (int i = 0; i < checkSigs.length; i++)
      {
        Sig sig = checkSigs[i];
        assertFalse("pending signal " + sig, pendingSignals.contains(sig));
      }

    // Remove any stray files.
    TearDownFile.tearDown();
    TearDownProcess.tearDown();

    // Drain all the pending signals. Note that the process of killing
    // off the processes used in the test can generate extra signals -
    // for instance a SIGUSR1 from a detached child that notices that
    // it's parent just exited.
    Signal.drain (Sig.CHLD);
    Signal.drain (Sig.HUP);
    Signal.drain (Sig.USR1);
    Signal.drain (Sig.USR2);

    logger.log(Level.FINE, "{0} >>>>>>>>>>>>>>>> end tearDown\n", this);
  }

  /** Don't use me yet */
  public class AttachedSyscallDaemonProcess
      extends AckDaemonProcess
  {
    final Task mainTask;

    TaskObserver.Execed execBlockingObserver;

    TaskObserver.Syscall syscallObserver;

    public AttachedSyscallDaemonProcess (String[] argv)
    {
      // Create the child.
      AckProcess child = new DetachedAckProcess((String) null, argv);
      this.mainTask = child.findTaskUsingRefresh(true);
      // Create and add an exec observer that blocks the task.
      class ExecObserver
          extends TaskObserverBase
          implements TaskObserver.Execed
      {
        public void addedTo (Object o)
        {
          super.addedTo(o);
          Manager.eventLoop.requestStop();
        }

        public Action updateExeced (Task task)
        {
          Manager.eventLoop.requestStop();
          return Action.BLOCK;
        }
      }

      class SyscallObserver
          extends TaskObserverBase
          implements TaskObserver.Syscall
      {
        public void addedTo (Object o)
        {
          Manager.eventLoop.requestStop();
        }

        public Action updateSyscallEnter (Task task)
        {
          Manager.eventLoop.requestStop();
          return Action.BLOCK;
        }

        public Action updateSyscallExit (Task task)
        {
          return Action.CONTINUE;
        }
      }
      execBlockingObserver = new ExecObserver();
      syscallObserver = new SyscallObserver();
      mainTask.requestAddExecedObserver(execBlockingObserver);
      assertRunUntilStop("add exec observer to AttachedDaemonProcess");
      // Run to the exec call.
      Signal.tkill(mainTask.getTid(), execSig);
      assertRunUntilStop("run to exec");
      mainTask.requestAddSyscallObserver(syscallObserver);
      assertRunUntilStop("add syscall observer to AttachedDaemonProcess");
      resume();
      assertRunUntilStop("run to syscall");
    }

    public void deleteObservers ()
    {
      mainTask.requestDeleteExecedObserver(execBlockingObserver);
      mainTask.requestDeleteSyscallObserver(syscallObserver);
    }

    /**
     * Resume the attached process.
     */
    public void resume ()
    {
      mainTask.requestUnblock(execBlockingObserver);
    }
  }
}
