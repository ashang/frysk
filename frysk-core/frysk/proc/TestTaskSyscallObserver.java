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

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import frysk.sys.Fork;
import frysk.sys.Pid;
import frysk.sys.Sig;
import frysk.sys.Signal;
import frysk.sys.SyscallNum;


/**
 * Check that syscall events are detected.
 *
 * This should be expanded later to support ISAs so that the syscall
 * parameters and return codes can be displayed.  That information must
 * be taken from registers specific to each platform.
 *
 * XXX: For reasons noted below, these tests are currently disabled.
 */

public class TestTaskSyscallObserver
    extends TestLib
{
    class SyscallObserver
	extends TaskObserverBase
	implements TaskObserver.Syscall
    {
	int enter = 0;
	int exit = 0;
	// boolean inSyscall = false; //XXX: this assumption cannot be made
	boolean inSyscall;
    
    boolean caughtExec = false;
    
	public void addedTo (Object o)
	{
	    super.addedTo (o);
	    Manager.eventLoop.requestStop ();
	}
	public Action updateSyscallEnter (Task task)
	{
      assertFalse ("inSyscall", inSyscall);
	  inSyscall = true;
	  enter++;
	  
      SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
      int syscallNum = syscallEventInfo.number (task);
      if (syscallNum == SyscallNum.SYSexecve) { 
        caughtExec = true;
      }
      
      return Action.CONTINUE;
	}
	public Action updateSyscallExit (Task task)
	{
	    if(enter != 0 ) assertTrue ("inSyscall", inSyscall);
	    inSyscall = false;
	    exit++;
	    return Action.CONTINUE;
	}
    }
	
    /**
     * test that the state machine can handle an exec event
     * during a a syscall.
     */
    public void testExecSyscall(){
  
  //	if (brokenXXX (2245))
  //	    return;
  //	    
    	//	  Create an unattached child process.
    	AckProcess child = new DetachedAckProcess ();
    
    	// Attach to the process using the exec observer.  The event
    	// loop is kept running until SingleExecObserver .addedTo is
    	// called indicating that the attach succeeded.
    	Task task = child.findTaskUsingRefresh (true);
    	SyscallObserver syscallObserver = new SyscallObserver ();
    	task.requestAddSyscallObserver (syscallObserver);
    	assertRunUntilStop ("adding exec observer causing attach");
    
    	// Do the exec; this call keeps the event loop running until
    	// the child process has notified this process that the exec
    	// has finished which is well after SingleExecObserver
    	// .updateExeced has been called.
    	child.assertSendExecWaitForAcks ();
    	    
    	assertEquals("Caught exec syscall",syscallObserver.caughtExec, true);
    }
 
    /**
     * test that the state machine can handle a fork event
     * during a a syscall.
     */
    public void testForkSyscall(){

//    if (brokenXXX (2245))
//        return;
//        
    // Create an unattached child process.
    AckProcess child = new DetachedAckProcess ();

    // Attach to the process using the exec observer.  The event
    // loop is kept running until SingleExecObserver .addedTo is
    // called indicating that the attach succeeded.
    Task task = child.findTaskUsingRefresh (true);
    SyscallObserver syscallObserver = new SyscallObserver ();
    task.requestAddSyscallObserver (syscallObserver);
    assertRunUntilStop ("adding exec observer causing attach");

    // Do the exec; this call keeps the event loop running until
    // the child process has notified this process that the exec
    // has finished which is well after SingleExecObserver
    // .updateExeced has been called.
    child.assertSendAddForkWaitForAcks();
        
    assertTrue(true);
    }
 
    
    /**
     * test that the state machine can handle a fork event
     * during a a syscall.
     */
    public void testCloneSyscall(){

//    if (brokenXXX (2957))
//        return;
        
    // Create an unattached child process.
    AckProcess child = new DetachedAckProcess ();

    // Attach to the process using the exec observer.  The event
    // loop is kept running until SingleExecObserver .addedTo is
    // called indicating that the attach succeeded.
    Task task = child.findTaskUsingRefresh (true);
    SyscallObserver syscallObserver1 = new SyscallObserver ();
    
    final SyscallObserver syscallObserver2 = new SyscallObserver ();
    
    task.requestAddSyscallObserver (syscallObserver1);
    task.requestAddClonedObserver(new TaskObserver.Cloned()
    {
    
      public void deletedFrom (Object observable){}
      public void addFailed (Object observable, Throwable w){ fail(); }
      public Action updateClonedParent (Task task, Task clone){ return Action.CONTINUE; }
      public void addedTo (Object observable){}
    
      public Action updateClonedOffspring (Task parent, Task offspring)
      {
        offspring.requestAddSyscallObserver(syscallObserver2);
        offspring.requestUnblock(this);
        return Action.BLOCK;
//        return Action.CONTINUE;
      }
    
    });
    assertRunUntilStop ("adding exec observer causing attach");

    // Do the exec; this call keeps the event loop running until
    // the child process has notified this process that the exec
    // has finished which is well after SingleExecObserver
    // .updateExeced has been called.
    child.assertSendAddCloneWaitForAcks();
        
//    System.out.println(this + ": TestTaskSyscallObserver.testCloneSyscall() 1: " + syscallObserver1.enter);
//    System.out.println(this + ": TestTaskSyscallObserver.testCloneSyscall() 2: " + syscallObserver2.enter);
    
    assertTrue("number of system calls", syscallObserver1.enter > 0);
    assertTrue("number of system calls", syscallObserver2.enter > 0);
     
    }
 
        
    /**
     * Test a system-call in a for loop.
     */
    public void testSyscallLoop ()
    {

//	if (brokenXXX (2245))
//	    return;

 	int count = 5;
	AttachedDaemonProcess child = new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-syscallloop",
 		Integer.toString (count),
  	    });

	// Add a syscall observer.  XXX: This doesn't work - system
	// call tracing doesn't get enabled enabled.
	SyscallObserver syscallObserver = new SyscallObserver ();
	child.mainTask.requestAddSyscallObserver (syscallObserver);
	assertRunUntilStop ("add SyscallObserver");

	// XXX: This is wrong; the task isn't a child so this will
	// never work.  What about assertRunUntilTaskRemoved (...)?
	new StopEventLoopWhenChildProcRemoved ();
	child.resume ();
	assertRunUntilStop ("run until program exits");

 	assertTrue ("enough syscall enter events",
 		    syscallObserver.enter >= count);
 	assertTrue ("enough syscall enter exit",
 		    syscallObserver.exit >= count);
 	assertTrue ("inSyscall (last call doesn't exit)",
		    syscallObserver.inSyscall);
    }

    
    /**
     * Test adding a syscall to a newly created attached
     * process created by Frysk.
     * Test that Frysk does not miss the first exec call.
     */
    SyscallObserver syscallObserver1 = new SyscallObserver(){
      public void addedTo (Object observable){
        Task task = (Task) observable;
        task.requestUnblock(attachedObserver);
      }
    };
    
    TaskObserver.Attached attachedObserver = new TaskObserver.Attached()
    {
      
      public Action updateAttached (Task task)
      {
        logger.log(Level.FINE, "{0} **updateAttached\n", task);
        new StopEventLoopWhenProcRemoved (task.getProc().getPid());
        task.requestAddSyscallObserver(syscallObserver1);
//        task.requestUnblock(this);
        return Action.BLOCK;
      }

      public void deletedFrom (Object observable){}
      public void addFailed (Object observable, Throwable w){}
      public void addedTo (Object observable){}

    };
    
    public void testCreateAttachedAddSyscallObserver ()
    {

      if (brokenXXX (2915))
        return;
    
      int count = 5;

      
      Manager.host.requestCreateAttachedProc(new String[]
                                                        {
                                                         getExecPrefix () + "funit-syscallloop",
                                                         Integer.toString (count),
                                                         }, attachedObserver);
      
      //assertRunUntilStop("run until program exits");
      assertRunUntilStop("run until program exits");

      assertTrue("enough syscall enter events", syscallObserver1.enter >= count);
      assertTrue("enough syscall enter exit", syscallObserver1.exit >= count);
      assertTrue("inSyscall (last call doesn't exit)", syscallObserver1.inSyscall);
      assertTrue("Caught exec syscall", syscallObserver1.caughtExec);
    }

    
    /**
     * Test system calls. XXX: How is this different to testSyscallLoop (other
     * than the program run). XXX: Also why is the last syscall expected to exit
     * this time ?
     */
    public void testSyscalls ()
    {
//	if (brokenXXX (2245))
//	    return;

 	// Create program making syscalls
	AttachedDaemonProcess child = new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-syscalls"
 	    });

	// Add a syscall observer.  XXX: This doesn't work - system
	// call tracing doesn't get enabled enabled.
	SyscallObserver syscallObserver = new SyscallObserver ();
	child.mainTask.requestAddSyscallObserver (syscallObserver);
 	assertRunUntilStop ("add SyscallObserver");

	// XXX: This is wrong; the task isn't a child so this will
	// never work.  What about assertRunUntilTaskRemoved (...)?
	new StopEventLoopWhenChildProcRemoved ();
	child.resume ();
	assertRunUntilStop ("run until program exits");

	assertTrue ("syscall events received >= 8",
		    syscallObserver.enter >= 8);
	//XXX: why ?	assertFalse ("syscall events", syscallObserver.inSyscall);
	assertTrue ("syscall events", syscallObserver.inSyscall);
    }

    /**
     * Need to add task observers to the process the moment it is
     * created, otherwize the creation of the very first task is
     * missed (giving a mismatch of task created and deleted
     * notifications.)
     */
	
    class SyscallOpenObserver
	extends SyscallObserver
    {
	boolean openingTestFile;
	boolean testFileOpened;
	boolean expectedRcFound;
	String openName = "a.file";
	public Action updateSyscallEnter (Task task)
	{
	    super.updateSyscallEnter (task);
	    SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
	    int syscallNum = syscallEventInfo.number (task);
	    if (syscallNum == SyscallNum.SYSopen) { 
		long addr = syscallEventInfo.arg (task, 1);
		StringBuffer x = new StringBuffer ();
		task.memory.get (addr, x);
		String name = x.toString ();
		if (name.indexOf (openName) >= 0) {
		    testFileOpened = true;
		    openingTestFile = true;
		}
	    }
	    return Action.CONTINUE;
	}
	public Action updateSyscallExit (Task task)
	{
	    super.updateSyscallExit (task);
	    SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
	    int syscallNum = syscallEventInfo.number (task);
	    if (syscallNum == SyscallNum.SYSopen && openingTestFile) {
		openingTestFile = false;
		int rc = (int)syscallEventInfo.returnCode (task);
		if (rc == -2) // ENOENT
		    expectedRcFound = true;
	    }
	    return Action.CONTINUE;
	}
    }

    /**
     * Check that a specific syscall event can be detected.  In this
     * case, an open syscall to a particular file.
     */
    public void testSyscallOpen ()
    {
	
//	if (brokenXXX (2245))
//	    return;

	SyscallOpenObserver syscallOpenObserver = new SyscallOpenObserver ();
	new StopEventLoopWhenChildProcRemoved ();

 	// Create program making syscalls
	AttachedDaemonProcess child = new AttachedDaemonProcess (new String[]
	    {
		getExecPrefix () + "funit-syscalls"
 	    });
	child.mainTask.requestAddSyscallObserver (syscallOpenObserver);
	assertRunUntilStop ("add SyscallObserver");

	child.resume ();
 	assertRunUntilStop ("run \"syscalls\" until exit");
	
	assertTrue ("syscall events received >= 8",
		    syscallOpenObserver.enter >= 8); 
	//XXX: why ? assertFalse ("in syscall", syscallOpenObserver.inSyscall);
	assertTrue ("in syscall", syscallOpenObserver.inSyscall);
	assertTrue ("attempt to open a.file",
		    syscallOpenObserver.testFileOpened);
	assertTrue ("open of a.file failed",
		    syscallOpenObserver.expectedRcFound);
    }

    /**
     * Class to create a detached child process that will be in the
     * middle of reading a pipe.
     */
    class PipeReadChild
	extends Child
    {
	protected int startChild (String stdin, String stdout, String stderr,
				  String[] argv)
	{
	    return Fork.daemon (stdin, stdout, stderr, argv);
	}
	PipeReadChild (String[] argv)
	{
	    super (argv);
	}
	PipeReadChild (boolean restart)
	{
	    this (new String[] {
		      getExecPrefix () + "funit-syscallint",
		      Integer.toString (Pid.get ()),
		      Integer.toString (ackSignal.hashCode ()),
		      Integer.toString (restart ? 1 : 0)
		  });
	}
    }

    private SyscallEventInfo getSyscallEventInfo(Task task)
    {
	try
	{
	    return task.getSyscallEventInfo();
	}
	catch (Task.TaskException e)
	{
	    fail("task exception " + e);
	    return null; // not reached
	}
    }

    // Timers, observers, counters, etc.. needed for the test.
    class TestSyscallInterruptInternals {
	int readEnter, readExit, sigusr1Count;
	
	// Need to add task observers to the process the moment it is
	// created, otherwize the creation of the very first task is
	// missed (giving a mismatch of task created and deleted
	// notifications.)
	
	class SyscallInterruptObserver
	    extends SyscallObserver
            implements TaskObserver.Signaled
	{

	    
	    public Action updateSyscallEnter (Task task)
	    {
		super.updateSyscallEnter (task);
		SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
		int syscallNum = syscallEventInfo.number (task);
		// verify that read attempted
		if (syscallNum == SyscallNum.SYSread) { 
		    long numberOfBytes = syscallEventInfo.arg (task, 3);
		    logger.log(Level.FINE, "{0} updateSyscallEnter READ\n", this);
		    if (numberOfBytes != 1)
			throw new RuntimeException ("bytes to read not 1");
		    if (readEnter == 0)
			Manager.eventLoop.add (new PausedReadTimerEvent (task, 500));
		    ++readEnter;
		}
		return Action.CONTINUE;
	    }
	    public Action updateSyscallExit (Task task)
	    {
		super.updateSyscallExit (task);
		SyscallEventInfo syscallEventInfo = getSyscallEventInfo(task);
		int syscallNum = syscallEventInfo.number (task);
		if (syscallNum == SyscallNum.SYSread) {
		    logger.log(Level.FINE, "{0} updateSyscallExit READ\n", this);
		    if (readEnter <= readExit)
			throw new RuntimeException ("Read exit before enter");
		    ++readExit;
		}
		return Action.CONTINUE;
	    }
            public Action updateSignaled (Task task, int sig)
            {
		if (sig == Sig.USR1_)
		    sigusr1Count++;
                return Action.CONTINUE;
            }
        }

	class PausedReadTimerEvent
	    extends frysk.event.TimerEvent
	{
	    Task task;
	    long milliseconds;
	    PausedReadTimerEvent (Task task, long milliseconds)
	    {
		super (milliseconds);
		this.task = task;
		this.milliseconds = milliseconds;
	    }
	    public void execute ()
	    {
		// Make sure we didn't get a read exit up to now
		// as we are expecting to interrupt a blocked read.
		if (readExit > 0)
		    throw new RuntimeException ("read exited without signal");
		// We want to signal the process so it will interrupt
		// the read.
		Signal.tkill (task.getTid (), Sig.USR1);
	    }
	}

	SyscallInterruptObserver syscallObserver = new SyscallInterruptObserver ();
	
	TestSyscallInterruptInternals (int pid)
	{
            host.requestRefreshXXX (true);
            Manager.eventLoop.runPending ();

            Proc p = host.getProc (new ProcId (pid));

            if (p != null) {
                List tasks = p.getTasks ();
                for (Iterator i = tasks.iterator (); i.hasNext (); ) {
                    Task t = (Task) i.next ();
                    if (t.getTaskId ().hashCode () == pid) {
                        t.requestAddSyscallObserver (syscallObserver);
                        assertRunUntilStop ("Add syscallObservers");
                        t.requestAddSignaledObserver (syscallObserver);
                        assertRunUntilStop ("Add signaledObservers");
                    }
                }
            }
        }
    }

    /**
     * Check that we can attach to a process currently in a syscall
     * and trace syscall events properly.
     */
    public void testSyscallInterrupt ()
    {
//	if (brokenXXX (2245))
//	    return;
	PipeReadChild prc = new PipeReadChild (false);

	TestSyscallInterruptInternals t 
	    = new TestSyscallInterruptInternals (prc.getPid ());
	new StopEventLoopWhenProcRemoved (prc.getPid());
	
 	assertRunUntilStop ("run \"syscallint\" until exit");
	assertEquals ("read enter events", 1, t.readEnter);
	assertEquals ("read exit events", 1, t.readExit);
	assertEquals ("SIGUSR1 events", 1, t.sigusr1Count);
	assertTrue ("inSyscall", t.syscallObserver.inSyscall);
    }

    /**
     * Check that we can attach to a process currently in a syscall
     * and trace syscall events properly.
     */
    public void testSyscallInterruptRestart ()
    {
//	if (brokenXXX (2245))
//	    return;
	PipeReadChild prc = new PipeReadChild (true);

	TestSyscallInterruptInternals t
	    = new TestSyscallInterruptInternals (prc.getPid ());
	new StopEventLoopWhenProcRemoved (prc.getPid());

 	assertRunUntilStop ("run \"syscallint\" with restart until exit");
	assertEquals ("restart read enter events", 2, t.readEnter);
	assertEquals ("restart read exit events", 2, t.readExit);
	assertEquals ("restart sigusr1 events", 1, t.sigusr1Count);
	assertTrue ("inSyscall", t.syscallObserver.inSyscall);
    }
}
