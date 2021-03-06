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
import java.util.Observable;
import java.util.Observer;

import frysk.testbed.Offspring;
import frysk.testbed.SynchronizedOffspring;
import frysk.config.Prefix;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rt.BreakpointManager;
import frysk.rt.LineBreakpoint;
import frysk.rt.SourceBreakpoint;
import frysk.rt.SourceBreakpointObserver;
import frysk.scopes.SourceLocation;
import frysk.sys.Pid;
import frysk.sys.Signal;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.TestLib;
import frysk.testbed.TestfileTokenScanner;

/**
 * Main testcase for frysk.stepping.
 * 
 * Test cases work as follows:
 * 
 * 1. Define a class implementing SteppingTest, with whatever
 * assertions are to be made in the runAssertions() method.
 * 
 * 2. Define the source file, initialize the test string token scanner
 * Object, and declare the starting and ending lines for the test.
 * 
 * 3. Create a new DaemonBlockedAtEntry for the process to be tested
 * on.
 * 
 * 4. Call initTaskWithTask() to initialize the LockObserver and other
 * data structures, as well as set the initial breakpoints for the
 * test. This method will run the program to the initial breakpoint.
 * 
 * 5. Perform the actual stepping operation -
 * i.e. SteppingEngine.stepLine(Task).
 * 
 * 6. When the step has completed, the LockObserver will update and
 * call the runAssertions() method in the current TestStepping class
 * defined above.
 */

public class TestStepping extends TestLib {

    private HashMap lineMap = new HashMap();

    private SteppingEngine se;

    private TestfileTokenScanner scanner;

    private LockObserver lock;

    private SteppingTest currentTest;

    public void testLineStepFunctionCall() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-iftester.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_lineStepIfFail_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_lineStepIfPass_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix.pkgLibFile("funit-iftester"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(theTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testLineStepIfStatementPass() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */
	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-iftester.S");
	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_lineStepIfPass_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_lineStepIfPassFinish_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix.pkgLibFile("funit-iftester"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(theTask);

	this.testStarted = true;
	//	System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testLineStepIfStatementFail() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-iftester.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_lineStepIfFail_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_lineStepIfPass_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix.pkgLibFile("funit-iftester"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(theTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testLineStepFunctionReturn() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-iftester.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_lineStepFunctionEnd_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_lineStepFunctionEndReturn_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix.pkgLibFile("funit-iftester"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(theTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testStepSigLongJmp() {

	if (unresolvedOnPPC(3277))
	    return;

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class LineStepSigLongJmpTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public LineStepSigLongJmpTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
		    .createDebugInfoStackTrace(testTask);
		if (frame.getLine() == SourceLocation.UNKNOWN) {
		    se.stepInstruction(testTask);
		    return;
		}
		int lineNr = frame.getLine().getLine();
		assertEquals("line number", success, lineNr);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-rt-siglongjmp.c");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int start = this.scanner.findTokenLine("_SigLongJumpCall_");

	/* The line number the test should end up at */
	int end = this.scanner.findTokenLine("_SigSetJmpReturn_");

	/* The test process */
	SynchronizedOffspring process
	    = new SynchronizedOffspring(Signal.USR1,
					new String[] {
					    getExecPath("funit-rt-siglongjmp"),
					    Integer.toString(Pid.get().intValue()),
					    Integer.toString(Signal.USR1.intValue())
					});
	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new LineStepSigLongJmpTest(end, myTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(myTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(myTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testStepGoto() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-rt-goto.c");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int start = this.scanner.findTokenLine("_lineStepGotoEntry_");

	/* The line number the test should end up at */
	int end = this.scanner.findTokenLine("_lineStepGotoExit_");

	/* The test process */
	SynchronizedOffspring process
	    = new SynchronizedOffspring(Signal.USR1,
					new String[] {
					    getExecPath("funit-rt-goto"),
					    Integer.toString(Pid.get().intValue()),
					    Integer.toString(Signal.USR1.intValue())
					});
	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new AssertLine(end, myTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(myTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(myTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testStepSigRaise() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-rt-sigraise.c");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int start = this.scanner.findTokenLine("_lineStepSigRaiseCall_");

	/* The line number the test should end up at */
	int end = this.scanner.findTokenLine("_lineStepSigHandlerEntry_");

	/* The test process */
	SynchronizedOffspring process
	    = new SynchronizedOffspring(Signal.USR1,
					new String[] {
					    getExecPath("funit-rt-sigraise"),
					    Integer.toString(Pid.get().intValue()),
					    Integer.toString(Signal.USR1.intValue())
					});
	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new AssertLine(end, myTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(myTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(myTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testASMSingleStep() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-rt-asmstepper.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_asmSingleStepStart_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_asmSingleStepFinish_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
					.pkgLibFile("funit-rt-asmstepper"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepInstruction(theTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testASMMultiStep() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-rt-asmstepper.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_asmMultiStepStart_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_asmMultiStepFinish_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
					.pkgLibFile("funit-rt-asmstepper"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(theTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testASMJump() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-rt-asmstepper.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_asmStepJump_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_asmStepJumpTo_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
					.pkgLibFile("funit-rt-asmstepper"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(theTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testFunctionEntry() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepASMFunctionCall_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepASMFunctionEntry_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
					.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepInstruction(theTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testASMFunctionReturn() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepASMFunctionExit_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepASMFunctionReturned_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
					.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepInstruction(theTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testASMFunctionStepOver() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepASMFunctionCall_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepASMFunctionReturned_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
					.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepOver(theTask, frame);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testASMFunctionStepOut() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepASMFunctionStepOut_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepASMFunctionReturned_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
					.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepOut(theTask, frame);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testFramelessFunctionEntry() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-frameless.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepFramelessEntry_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepFramelessEntered_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix.pkgLibFile("funit-frameless"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepInstruction(theTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testASMFramelessFunctionStepOver() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-frameless.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepFramelessEntry_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepFramelessReturn_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix.pkgLibFile("funit-frameless"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepOver(theTask, frame);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testASMFramelessFunctionStepOut() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-frameless.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepFramelessEntered_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepFramelessReturn_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix.pkgLibFile("funit-frameless"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepOut(theTask, frame);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testASMFramelessFunctionCall() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-frameless.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepFramelessEntry_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepFramelessEntered_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix.pkgLibFile("funit-frameless"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(theTask);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    public void testASMFramelessFunctionReturn() {

	if (unresolvedOnPPC(3277))
	    return;

	/** Variable setup */

	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-frameless.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepFramelessLeave_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepFramelessReturn_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix.pkgLibFile("funit-frameless"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(theTask);

	this.testStarted = true;
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

  /**
   * Tests that the line stepper steps OK even when stack pointer
   * changes.
   */                                                                          
    public void testASMFunctionStepOverPrologue() {

	/** Variable setup */
	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-stepping-asm.S");

	this.scanner = new TestfileTokenScanner(source);

	/* The line number where the test begins, prologue of fifth
	   function. */
	int startLine = this.scanner.findTokenLine("_stepOverPrologue_");

	/* The line number the test should end up at, still the same
	   line (only prologue is stepped over, not the NO_OP) */
	int endLine = this.scanner.findTokenLine("_stepOverPrologue_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix
					.pkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new AssertLine(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
	    .createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepOver(theTask, frame);

	this.testStarted = true;
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }
    
    public void testInstructionStepThroughSection() {

	class InstructionStepThroughSectionTest implements SteppingTest {

	    Task testTask = null;
	    String callingFrame = "foo";
	    int endLine = 0;
	    
	    public InstructionStepThroughSectionTest(Task task, int lineNum) {
		testTask = task;
		endLine = lineNum;
	    }

	    public void runAssertions() {
		
		DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(testTask);
	
		String s = frame.getSymbol().getDemangledName();
		
		assertEquals("calling frame", callingFrame, s);
		assertEquals("line number", endLine, frame.getLine().getLine());
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */
	File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-libcall.c");

	this.scanner = new TestfileTokenScanner(source);

	int startLine = this.scanner.findTokenLine("_testIStepThrough_");

	int endLine = startLine + 1;

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Prefix.pkgLibFile("funit-libcall"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new InstructionStepThroughSectionTest(theTask, endLine);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present",
		frame.getLine() != SourceLocation.UNKNOWN);

	/** The stepping operation */
	this.se.stepLine(theTask);

	this.testStarted = true;
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }


    boolean genericUpdate = false;

    public Task initTask(Offspring process, File source, int startLine,
			 int endLine) {
	Task myTask = process.findTaskUsingRefresh(true);
	initTaskWithTask(myTask, source, startLine, endLine);
	return myTask;
    }

    DaemonBlockedAtEntry dbae = null;

    public void initTaskWithTask(Task myTask, File source, int startLine,
				 int endLine) {

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

	this.se.addObserver(lock);
	if (dbae != null)
	    dbae.requestUnblock();
	this.se.continueExecution(myTask);

	//	System.err.println("Continuing to initial breakpoint");
	assertRunUntilStop("Continuing to initial breakpoint");
    }

    public void runTest(String source, int line, Task task) {

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

    private static class AssertLine implements SteppingTest {
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

    class LockObserver implements Observer {
	/**
	 * Builtin Observer method - called whenever the Observable
	 * we're concerned with - in this case the SteppingEngine's
	 * steppingObserver - has changed.
	 * 
	 * @param o
	 *                The Observable we're watching
	 * @param arg
	 *                A TaskStepEngine
	 */
	public synchronized void update(Observable o, Object arg) {
	    TaskStepEngine tse = (TaskStepEngine) arg;
	    //	    System.err.println("Lock.update " + tse.isStopped() + " " + testStarted);
	    if (testStarted == true && tse.isStopped()) {
		currentTest.runAssertions();
	    } else
		return;
	}
    }

    /**
     * A custom SourceBreakpintObserver for stopping the event loop
     * when hit.
     *
     */
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
