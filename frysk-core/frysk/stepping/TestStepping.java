// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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
import frysk.Config;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rt.BreakpointManager;
import frysk.rt.LineBreakpoint;
import frysk.rt.SourceBreakpoint;
import frysk.rt.SourceBreakpointObserver;
import frysk.sys.Pid;
import frysk.sys.Sig;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.TestLib;
import frysk.testbed.TestfileTokenScanner;

/**
 * Main testcase for frysk.stepping.
 * 
 * Test cases work as follows:
 * 
 * 1. Define a class implementing SteppingTest, with whatever assertions are to
 * be made in the runAssertions() method.
 * 
 * 2. Define the source file, initialize the test string token scanner Object,
 * and declare the starting and ending lines for the test.
 * 
 * 3. Create a new DaemonBlockedAtEntry for the process to be tested on.
 * 
 * 4. Call initTaskWithTask() to initialize the LockObserver and other data
 * structures, as well as set the initial breakpoints for the test. This method
 * will run the program to the initial breakpoint.
 * 
 * 5. Perform the actual stepping operation - i.e. SteppingEngine.stepLine(Task).
 * 
 * 6. When the step has completed, the LockObserver will update and call the 
 * runAssertions() method in the current TestStepping class defined above.
 *
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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class LineStepFunctionCallTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public LineStepFunctionCallTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-iftester.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_lineStepIfFail_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_lineStepIfPass_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config.getPkgLibFile("funit-iftester"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new LineStepFunctionCallTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class LineStepIfStatementPassTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public LineStepIfStatementPassTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-iftester.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_lineStepIfPass_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_lineStepIfPassFinish_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config.getPkgLibFile("funit-iftester"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new LineStepIfStatementPassTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class LineStepIfStatementFailTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public LineStepIfStatementFailTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-iftester.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_lineStepIfFail_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_lineStepIfPass_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config.getPkgLibFile("funit-iftester"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new LineStepIfStatementFailTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class LineStepFunctionReturnTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public LineStepFunctionReturnTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-iftester.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_lineStepFunctionEnd_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_lineStepFunctionEndReturn_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config.getPkgLibFile("funit-iftester"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new LineStepFunctionReturnTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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
		if (frame.getLines().length == 0) {
		    se.stepInstruction(testTask);
		    return;
		}
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-rt-siglongjmp.c";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int start = this.scanner.findTokenLine("_SigLongJumpCall_");

	/* The line number the test should end up at */
	int end = this.scanner.findTokenLine("_SigSetJmpReturn_");

	/* The test process */
	SynchronizedOffspring process = new SynchronizedOffspring(Sig.USR1,
		new String[] { getExecPath("funit-rt-siglongjmp"),
			"" + Pid.get(), "" + Sig.USR1_ });
	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new LineStepSigLongJmpTest(end, myTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(myTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class LineStepGotoTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public LineStepGotoTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-rt-goto.c";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int start = this.scanner.findTokenLine("_lineStepGotoEntry_");

	/* The line number the test should end up at */
	int end = this.scanner.findTokenLine("_lineStepGotoExit_");

	/* The test process */
	SynchronizedOffspring process = new SynchronizedOffspring(Sig.USR1,
		new String[] { getExecPath("funit-rt-goto"), "" + Pid.get(),
			"" + Sig.USR1_ });
	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new LineStepGotoTest(end, myTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(myTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	if (unresolved(4237))
	    return;

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class LineStepSigRaiseTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public LineStepSigRaiseTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-rt-sigraise.c";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int start = this.scanner.findTokenLine("_lineStepSigRaiseCall_");

	/* The line number the test should end up at */
	int end = this.scanner.findTokenLine("_lineStepSigHandlerEntry_");

	/* The test process */
	SynchronizedOffspring process = new SynchronizedOffspring(Sig.USR1,
		new String[] { getExecPath("funit-rt-sigraise"),
			"" + Pid.get(), "" + Sig.USR1_ });
	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new LineStepSigRaiseTest(end, myTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(myTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class SingleStepASMTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public SingleStepASMTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-rt-asmstepper.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_asmSingleStepStart_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_asmSingleStepFinish_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config
		.getPkgLibFile("funit-rt-asmstepper"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new SingleStepASMTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class MultiStepASMTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public MultiStepASMTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-rt-asmstepper.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_asmMultiStepStart_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_asmMultiStepFinish_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config
		.getPkgLibFile("funit-rt-asmstepper"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new MultiStepASMTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class ASMJumpTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public ASMJumpTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-rt-asmstepper.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_asmStepJump_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_asmStepJumpTo_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config
		.getPkgLibFile("funit-rt-asmstepper"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new ASMJumpTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class StepFunctionEntryASMTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public StepFunctionEntryASMTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-stepping-asm.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepASMFunctionCall_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepASMFunctionEntry_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config
		.getPkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new StepFunctionEntryASMTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class StepFunctionReturnASMTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public StepFunctionReturnASMTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-stepping-asm.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepASMFunctionExit_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepASMFunctionReturned_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config
		.getPkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new StepFunctionReturnASMTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class StepOverASMFunctionTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public StepOverASMFunctionTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-stepping-asm.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepASMFunctionEntry_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepASMFunctionReturned_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config
		.getPkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new StepOverASMFunctionTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class StepOutASMFunctioNTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public StepOutASMFunctioNTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-stepping-asm.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepASMFunctionStepOut_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepASMFunctionReturned_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config
		.getPkgLibFile("funit-stepping-asm"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new StepOutASMFunctioNTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class StepFramelessFunctionEntryASMTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public StepFramelessFunctionEntryASMTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-frameless.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepFramelessEntry_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepFramelessEntered_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config.getPkgLibFile("funit-frameless"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new StepFramelessFunctionEntryASMTest(endLine,
		theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class StepOverASMFramelessFunctionTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public StepOverASMFramelessFunctionTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-frameless.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepFramelessEntry_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepFramelessReturn_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config.getPkgLibFile("funit-frameless"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new StepOverASMFramelessFunctionTest(endLine,
		theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class StepOutASMFramelessFunctionTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public StepOutASMFramelessFunctionTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-frameless.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepFramelessEntered_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepFramelessReturn_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config.getPkgLibFile("funit-frameless"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new StepOutASMFramelessFunctionTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class ASMFramelessFunctionCallTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public ASMFramelessFunctionCallTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-frameless.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepFramelessEntry_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepFramelessEntered_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config.getPkgLibFile("funit-frameless"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new ASMFramelessFunctionCallTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

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

	/** SteppingTest Object definition - tell the stepping test
	 * what to look for at the completion of the test. */
	class ASMFramelessFunctionReturnTest implements SteppingTest {

	    Task testTask = null;

	    int success = 0;

	    public ASMFramelessFunctionReturnTest(int s, Task task) {
		success = s;
		testTask = task;
	    }

	    public void runAssertions() {

		DebugInfoFrame frame = DebugInfoStackFactory
			.createDebugInfoStackTrace(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-frameless.S";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int startLine = this.scanner.findTokenLine("_stepFramelessLeave_");

	/* The line number the test should end up at */
	int endLine = this.scanner.findTokenLine("_stepFramelessReturn_");

	/* The test process */
	dbae = new DaemonBlockedAtEntry(Config.getPkgLibFile("funit-frameless"));

	Task theTask = dbae.getMainTask();

	this.testStarted = false;

	initTaskWithTask(theTask, source, startLine, endLine);

	this.currentTest = new ASMFramelessFunctionReturnTest(endLine, theTask);

	DebugInfoFrame frame = DebugInfoStackFactory
		.createDebugInfoStackTrace(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

	/** The stepping operation */
	this.se.stepLine(theTask);

	this.testStarted = true;
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }

    boolean genericUpdate = false;

    public Task initTask(Offspring process, String source, int startLine,
	    int endLine) {
	Task myTask = process.findTaskUsingRefresh(true);
	initTaskWithTask(myTask, source, startLine, endLine);
	return myTask;
    }

    DaemonBlockedAtEntry dbae = null;

    public void initTaskWithTask(Task myTask, String source, int startLine,
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

    private interface SteppingTest {
	void runAssertions();
    }

    boolean testStarted = false;

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
	    TaskStepEngine tse = (TaskStepEngine) arg;
	    //	    System.err.println("Lock.update " + tse.isStopped() + " " + testStarted);
	    if (testStarted == true && tse.isStopped()) {
		currentTest.runAssertions();
	    } else
		return;
	}
    }

    /**
     * A custom SourceBreakpintObserver for stopping the event loop when hit.
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
