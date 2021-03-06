// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;

import frysk.scopes.SourceLocation;
import frysk.sys.Signal;
import frysk.testbed.Offspring;
import frysk.config.Prefix;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.event.Event;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.rt.Breakpoint;
import frysk.rt.BreakpointManager;
import frysk.rt.LineBreakpoint;
import frysk.rt.SourceBreakpoint;
import frysk.rt.SourceBreakpointObserver;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.TestLib;
import frysk.testbed.TestfileTokenScanner;

/**
 * Testsuite for testing SteppingEngine operations. See TestStepping for
 * detailed description of how tests operate.
 */
public class TestSteppingEngine extends TestLib {

    private HashMap lineMap = new HashMap();

    private SteppingEngine se;

    private TestfileTokenScanner scanner;

    private LockObserver lock;

    private SteppingTest currentTest;

    public void testInstructionStepping() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_instructionStep_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_instructionStep_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
		.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine, 0);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepInstruction(theTask);

	this.testStarted = true;
	// System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testInstructionSteppingList() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_instructionStep_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_instructionStep_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
		.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine, 0);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	LinkedList l = new LinkedList();
	l.add(theTask);
	this.se.stepInstruction(l);

	this.testStarted = true;
	// System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testLineStepping() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_instructionStep_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_lineStepEnd_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
		.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine, 0);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame sFrame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", sFrame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(theTask);

	this.testStarted = true;
	// System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testLineSteppingList() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_instructionStep_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_lineStepEnd_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
		.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine, 0);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	LinkedList l = new LinkedList();
	l.add(theTask);
	this.se.stepLine(l);

	this.testStarted = true;
	// System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testStepOver() {

	if (unresolvedOnPPC(3277))
	    return;

	/**
         * SteppingTest Object definition - tell the stepping test what to look
         * for at the completion of the test.
         */
	class StepOverTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public StepOverTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLine().getLine();
		assertEquals("line number", success, lineNr);

		assertEquals("demanged name", "second", frame.getSymbol()
			.getDemangledName());
		frame = frame.getOuterDebugInfoFrame();
		assertEquals("demanged name", "first", frame.getSymbol()
			.getDemangledName());
		frame = frame.getOuterDebugInfoFrame();
		assertEquals("demanged name", "main", frame.getSymbol()
			.getDemangledName());

		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepOverStart_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepOverEnd_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
		.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine, 0);

	this.currentTest = new StepOverTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepOver(theTask, DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask));

	this.testStarted = true;
	// System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testInstructionNext() {

	if (unresolvedOnPPC(3277))
	    return;

	/**
         * SteppingTest Object definition - tell the stepping test what to look
         * for at the completion of the test.
         */
	class InstructionNextTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public InstructionNextTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLine().getLine();
		assertEquals("line number", success, lineNr);

		assertEquals("demanged name", "second", frame.getSymbol()
			.getDemangledName());
		frame = frame.getOuterDebugInfoFrame();
		assertEquals("demanged name", "first", frame.getSymbol()
			.getDemangledName());
		frame = frame.getOuterDebugInfoFrame();
		assertEquals("demanged name", "main", frame.getSymbol()
			.getDemangledName());

		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepOverStart_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepOverEnd_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
		.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine, 0);

	this.currentTest = new InstructionNextTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	se.stepNextInstruction(theTask, DebugInfoStackFactory
			       .createDebugInfoStackTrace(theTask));

	this.testStarted = true;
	// System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testStepOut() {

	if (unresolvedOnPPC(3277))
	    return;

	/**
         * SteppingTest Object definition - tell the stepping test what to look
         * for at the completion of the test.
         */
	class StepOutTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public StepOutTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLine().getLine();
		assertEquals("line number", success, lineNr);

		assertEquals("demanged name", "second", frame.getSymbol()
			.getDemangledName());
		frame = frame.getOuterDebugInfoFrame();
		assertEquals("demanged name", "first", frame.getSymbol()
			.getDemangledName());
		frame = frame.getOuterDebugInfoFrame();
		assertEquals("demanged name", "main", frame.getSymbol()
			.getDemangledName());

		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepOutStart_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepOverEnd_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
		.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine, 0);

	this.currentTest = new StepOutTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepOut(theTask, DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask));

	this.testStarted = true;

	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testStepAdvance() {

	if (unresolvedOnPPC(3277))
	    return;

	/**
         * SteppingTest Object definition - tell the stepping test what to look
         * for at the completion of the test.
         */
	class StepAdvanceTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public StepAdvanceTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLine().getLine();
		assertEquals("line number", success, lineNr);

		assertEquals("demanged name", "second", frame.getSymbol()
			.getDemangledName());
		frame = frame.getOuterDebugInfoFrame();
		assertEquals("demanged name", "first", frame.getSymbol()
			.getDemangledName());
		frame = frame.getOuterDebugInfoFrame();
		assertEquals("demanged name", "main", frame.getSymbol()
			.getDemangledName());

		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepAdvanceStart_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepOverEnd_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
		.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine, 0);

	this.currentTest = new StepAdvanceTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepAdvance(theTask, DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask).getOuterDebugInfoFrame()
		.getOuterDebugInfoFrame());

	this.testStarted = true;

	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }


    public void testChildThreadStart() {

	if (unresolvedOnPPC(3277))
	    return;

	/**
         * SteppingTest Object definition - tell the stepping test what to look
         * for at the completion of the test.
         */
	class ChildThreadStartTest implements SteppingTest {

	    public void runAssertions() {

			Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-threads-looper.c");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_childThreadStart1_");

	int startLine2 = this.scanner.findTokenLine("_childThreadStart2_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_childThreadEnd_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
		.pkgLibFile("funit-threads-looper"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine, startLine2);

	this.currentTest = new ChildThreadStartTest();

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation. Step all tasks - if one is not added to the
	SteppingEngine, it will throw a RuntimeException.  */
	this.se.stepLine(theTask.getProc().getTasks());

	this.testStarted = true;

	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testBreakpointing() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepOutStart_");

	/* The line number the test should end up at */
	int endLine = 0;

	dbae = new DaemonBlockedAtEntry(Prefix
		.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine, 0);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	breakpointAddress = frame.getOuterDebugInfoFrame().getAddress();
	se.setBreakpoint(theTask, breakpointAddress);

	bpTask = theTask;
	Manager.eventLoop.add(new Event() {
	    public void execute() {
		Breakpoint b = se.getTaskBreakpoint(bpTask);
		assertNotNull("task breakpoint", b);
		assertEquals("isAdded", true, b.isAdded());
		assertEquals("isRemoved", false, b.isRemoved());
		assertEquals("breakpoint address", breakpointAddress,
			     b.getAddress());
		Manager.eventLoop.requestStop();
		cleanup();
	    }
	});
	assertRunUntilStop("Running test");
    }
    
    public void testStepIntoMissingThread() {

	if (unresolvedOnPPC(3277))
	    return;
	if (unresolvedOffUtrace(4956))
	    return;

	class SignalObserver implements TaskObserver.Signaled {

	    public Action updateSignaled(Task task,
					 frysk.isa.signals.Signal sig) {
		return Action.CONTINUE;
	    }

	    public void addedTo(Object observable) {
		ProcessIdentifier pid
			= ProcessIdentifierFactory.create(((Task) observable).getProc().getPid());
		Signal.KILL.kill(pid);
	    }

	    public void addFailed(Object observable, Throwable w) {
		throw new RuntimeException("Failed to attach to created proc",
			w);
	    }

	    public void deletedFrom(Object observable) {
		se.stepLine((Task) observable);
	    }
	}
	
	/**
         * SteppingTest Object definition - tell the stepping test what to look
         * for at the completion of the test.
         */
	class testMissingThreadStep implements SteppingTest {

	    Task testTask = null;
	    
	    public testMissingThreadStep(int s, Task task) {
		this.testTask = task;
	    }

	    public void runAssertions() {
		assertFalse("is alive", tse.isAlive());
		String msg = tse.getMessage();
		assertTrue("termination message",
			   msg.contains("Task " + this.testTask.getTid()
					+ " terminated by signal SIGKILL(9)"));
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepAdvanceStart_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, 0, 0);

	this.currentTest = new testMissingThreadStep(0, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);
	
	theTask.requestAddSignaledObserver(new SignalObserver());

	/** The stepping operation */
//	this.se.stepAdvance(theTask, DebugInfoStackFactory
//		.createDebugInfoStackTrace(theTask).getOuterDebugInfoFrame()
//		.getOuterDebugInfoFrame());

	this.testStarted = true;
	// System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    Task bpTask = null;

    long breakpointAddress = 0;

    boolean genericUpdate = false;

    public Task initTask(Offspring process, File source, int startLine,
	    int endLine) {
	Task myTask = process.findTaskUsingRefresh(true);
	initTaskWithTask(myTask, source, startLine, endLine, 0);
	return myTask;
    }

    DaemonBlockedAtEntry dbae = null;

    public void initTaskWithTask(Task myTask, File source, int startLine,
	    int endLine, int startLine2) {

	this.lineMap = new HashMap();
	this.lock = new LockObserver();

	Proc[] p = new Proc[1];
	p[0] = myTask.getProc();
	genericUpdate = true;
	this.se = new SteppingEngine(p, new Observer() {
	    public void update(Observable observable, Object arg) {
		if (genericUpdate == true) {
		    Manager.eventLoop.requestStop();
		    genericUpdate = false;
		}
	    }
	});

	assertRunUntilStop("Adding to Stepping Engine");

	BreakpointManager bManager = this.se.getBreakpointManager();

	LineBreakpoint lbp = bManager.addLineBreakpoint(source, startLine, 0);
	lbp.addObserver(new TestSteppingBreakpoint());
	bManager.enableBreakpoint(lbp, myTask);

	if (startLine2 != 0)
	{
	LineBreakpoint lbp2 = bManager.addLineBreakpoint(source, startLine2, 0);
	lbp2.addObserver(new TestSteppingBreakpoint());
	bManager.enableBreakpoint(lbp2, myTask);
	}

	this.se.addObserver(lock);
	if (dbae != null)
	    dbae.requestUnblock();
	this.se.continueExecution(myTask);

	assertRunUntilStop("Continuing to initial breakpoint");
    }

    public void runTest(File source, int line, Task task) {
	BreakpointManager bManager = this.se.getBreakpointManager();
	LineBreakpoint lbp2 = bManager.addLineBreakpoint(source, line, 0);
	bManager.enableBreakpoint(lbp2, task);

	this.se.addObserver(lock);
	this.se.continueExecution(task);

	assertRunUntilStop("Continuing to final breakpoint");
    }

    public void cleanup() {

	se.clear();
	lineMap.clear();
	lock = null;
	scanner = null;
	dbae = null;
    }

    static class AssertLine implements SteppingTest {
	private final Task task;
	private final int success;
	AssertLine(int success, Task task) {
	    this.task = task;
	    this.success = success;
	}
	public void runAssertions() {
	    DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(task);
	    int lineNr = frame.getLine().getLine();
	    assertEquals("line number", success, lineNr);
	    Manager.eventLoop.requestStop();
	}
    }

    private interface SteppingTest {
	void runAssertions();
    }

    boolean testStarted = false;
    TaskStepEngine tse = null;

    class LockObserver implements Observer {
	/**
         * Builtin Observer method - called whenever the Observable we're
         * concerned with - in this case the SteppingEngine's steppingObserver -
         * has changed.
         * 
         * @param o
         *                The Observable we're watching
         * @param arg
         *                A TaskStepEngine
         */
	public synchronized void update(Observable o, Object arg) {
	    
	    tse = (TaskStepEngine) arg;
	    // System.err.println("Lock.update " + tse.isStopped() + " " +
                // testStarted);
	    if (testStarted == true && tse.isStopped()) {
		currentTest.runAssertions();
	    } else
		return;
	}
    }

    private class TestSteppingBreakpoint implements SourceBreakpointObserver {

	public void updateHit(SourceBreakpoint breakpoint, Task task,
		long address) {
	    Manager.eventLoop.requestStop();
	}

	public void addFailed(Object observable, Throwable w) {
	}

	public void addedTo(Object observable) {
	}

	public void deletedFrom(Object observable) {
	}
    }
}
