// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

package frysk.hpd;

import frysk.config.Prefix;
import frysk.testbed.SlaveOffspring;

/**
 * This class tests the "kill" command basics.
 */

public class TestKillCommand extends TestLib {
    public void testKillCommand() {
	/* In the future when fhpd can accept parameters to pass to 
	 * programs, we should probably use the testing stuff below.
	 * until then, we'll have to use a parameterless program.
	String[] args = FunitThreadsOffspring.funitThreadsCommand(2, 
		FunitThreadsOffspring.Type.LOOP);
	String cmdLine = "";
	for (int i = 0; i < args.length; i++) {
	    cmdLine = cmdLine + args[i] + " ";
	} 
	 */
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
		"Loaded executable file.*");

	e.sendCommandExpectPrompt("run", "Attached to process.*");
	try { Thread.sleep(500); } catch (Exception e) { }
	e.sendCommandExpectPrompt("go", "Running process.*");
	try { Thread.sleep(500); } catch (Exception e) { }
	e.sendCommandExpectPrompt("kill", "Killing process.*Loaded executable file.*");

	/* Make sure you run again to make sure all has been cleaned up properly
	 * from the last run.
	 */
	/*****************************************************
	 * 
         *  There seems to be a problem with the test harness that will not allow
         *  more than the set of commands you see here in one sequence.  Just 
         *  uncommenting the next 2 statements after this comment causes this 
         *  test to fail for no good reason.  A bug will be filed on this and the
         *  lines can be uncommented when fixed. */
	/*
	e.send("run", "Attached to process*");
	e.expect("Attached to process*");
	e.send("go\n");
	e.expect("Running process*");
	e.send("kill\n");
	e.expect("Killing process*");
	e.expect("Loaded executable file*");
	/* Make sure we can quit gracefully  */
	
	/* adding the quit/Quitting statements causes this backtrace:
	 * 
	 *  frysk.expunit.EndOfFileException: end-of-file; expecting:  <<Quitting\.\.\..*>>; buffer <<java.lang.RuntimeException: {frysk.proc.live.LinuxPtraceTask@2fa6f255,pid=26380,tid=26381,state=StartClonedTask.blockedOffspring} in state "StartClonedTask.blockedOffspring" did not handle handleTerminatedEvent
   at frysk.proc.live.State.unhandled(State.java:67)
   at frysk.proc.live.LinuxPtraceTaskState.handleTerminatedEvent(LinuxPtraceTaskState.java:73)
   at frysk.proc.live.LinuxPtraceTask.processTerminatedEvent(LinuxPtraceTask.java:233)
   at frysk.proc.live.LinuxWaitBuilder.terminated(LinuxWaitBuilder.java:200)
   at frysk.sys.Wait.wait(Wait.cxx:586)
   at frysk.sys.Wait.wait(Wait.java:125)
   at frysk.event.WaitEventLoop.block(WaitEventLoop.java:87)
   at frysk.event.EventLoop.runEventLoop(EventLoop.java:377)
   at frysk.event.EventLoop.run(EventLoop.java:487)
   at frysk.bindir.fhpd.main(fhpd.java:171)

	 */
	
	//e.send("quit\n");
	//e.expect("Quitting\\.\\.\\..*");
	e.close();
    }
    
    
    public void testKillCommandTest() {
	/* 
	 * This is a copy of the testKillCommand as it should be able to be run without
	 * the delay and without leaving off the "quit" command.
	 * 
	 */
	if (unresolved(5615))
	    return;
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
		"Loaded executable file.*");

	e.sendCommandExpectPrompt("run", "Attached to process.*");
	e.sendCommandExpectPrompt("go", "Running process.*");
	e.sendCommandExpectPrompt("kill", "Killing process.*Loaded executable file.*");

	e.sendCommandExpectPrompt("run", "Attached to process.*");
	e.sendCommandExpectPrompt("go", "Running process.*");
	e.sendCommandExpectPrompt("kill", "Killing process.*Loaded executable file.*");

	e.expect("Loaded executable file*");
	/* Make sure we can quit gracefully  */
	
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\..*");
	e.close();
    }
    
    
    /**
     * Test when all you have done is loaded/run the process and not
     * have done a "go" on it.
     */
    public void testLoadKill() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("run", "Attached to process.*");
	e.sendCommandExpectPrompt("kill", "Killing process.*");
	/* Adding the quit/Quitting lines causes the following stack trace:
	
	1) testLoadKill(frysk.hpd.TestKillCommand)frysk.expunit.EndOfFileException: end-of-file; expecting:  <<Quitting\.\.\..*>>; buffer <<quit
java.lang.NullPointerException
   at frysk.stepping.SteppingEngine$SteppingObserver.updateExecuted(SteppingEngine.java:1052)
   at frysk.proc.live.LinuxPtraceProc$13.add(LinuxPtraceProc.java:690)
   at frysk.proc.live.LinuxPtraceTaskState$StartClonedTask.handleAddObservation(LinuxPtraceTaskState.java:648)
   at frysk.proc.live.LinuxPtraceTask.handleAddObservation(LinuxPtraceTask.java:418)
   at frysk.proc.live.TaskObservation.handleAdd(TaskObservation.java:87)
   at frysk.proc.live.LinuxPtraceProcState$3.handleAddObservation(LinuxPtraceProcState.java:413)
   at frysk.proc.live.LinuxPtraceProc.handleAddObservation(LinuxPtraceProc.java:426)
   at frysk.proc.live.LinuxPtraceProc$13.execute(LinuxPtraceProc.java:676)
   at frysk.event.EventLoop.runEventLoop(EventLoop.java:365)
   at frysk.event.EventLoop.run(EventLoop.java:482)
   at frysk.bindir.fhpd.main(fhpd.java:181)
>>
   at frysk.expunit.Child.expectMilliseconds(Child.java:161)
   at frysk.expunit.Expect.expect(Expect.java:158)
   at frysk.expunit.Expect.expect(Expect.java:167)
   at frysk.expunit.Expect.expect(Expect.java:176)
   at frysk.hpd.TestKillCommand.testLoadKill(TestKillCommand.java:170)
   at frysk.junit.Runner.runCases(Runner.java:197)
   at frysk.junit.Runner.runTestCases(Runner.java:424)
   at TestRunner.main(TestRunner.java:63) */

	//e.send("quit\n");
	//e.expect("Quitting\\.\\.\\..*");
	e.close();
    }
    
    /**
     * Test killing of a single proc using the PID
     */
    public void testKillByPID() {
	SlaveOffspring newProc = SlaveOffspring.createDaemon();
	int pid = newProc.getPid().intValue();
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("attach " + pid, "Attached to process " + pid + ".*");
	e.sendCommandExpectPrompt("kill " + pid, "Killing process " + pid + ".*");
	try { Thread.sleep(500); } catch (Exception e) { }
	e.sendCommandExpectPrompt("kill " + pid, "PID " + pid + " could not be found.*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\..*");
	e.close();
    }
    
    /**
     * Test killing of a single proc NOT using the PID
     */
    public void testKillAfterAttach() {
	SlaveOffspring newProc = SlaveOffspring.createDaemon();
	int pid = newProc.getPid().intValue();
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("attach " + pid, "Attached to process " + pid + ".*");
	e.sendCommandExpectPrompt("kill", "Killing process " + pid + ".*");
	try { Thread.sleep(500); } catch (Exception e) { }
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\..*");
	e.close();
    }
    
    /**
     * Test killing of a single proc and then running
     */
    public void testKillAfterAttachThenRun() {
	SlaveOffspring newProc = SlaveOffspring.createDaemon();
	int pid = newProc.getPid().intValue();
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("attach " + pid, "Attached to process " + pid + ".*");
	e.sendCommandExpectPrompt("kill", "Killing process " + pid + ".*");
	try { Thread.sleep(500); } catch (Exception e) { }
	e.sendCommandExpectPrompt("run", "running with this command.*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\..*");
	e.close();
    }
    
    /**
     * Test entering a non-integer as a PID
     */
    public void testKillError() {
	SlaveOffspring newProc = SlaveOffspring.createDaemon();
	int pid = newProc.getPid().intValue();
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("attach " + pid, "Attached to process " + pid + ".*");
	e.sendCommandExpectPrompt("kill abc", "Error: PID entered is not an integer.*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\..*");
	e.close();
    }
    
    /**
     * Test entering too many parameters
     */
    public void testKillErrorTwo() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("kill a b c", "Too many parameters.*");
    }
    
    /**
     * Test kill using HPD notation ([1.0] kill)
     */
    public void testKillHpd() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-hello").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
	"Loaded executable file.*");
	e.sendCommandExpectPrompt("run", "Attached to process.*");
	try { Thread.sleep(500); } catch (Exception e) { }
	e.sendCommandExpectPrompt("[1.0] kill", "Creating.*Killing process ([0-9]+).*funit-threads-looper.*" +
		"\\[1\\.0\\] Loaded executable.*funit-threads-looper.*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\..*");
	e.close();
    }
}
