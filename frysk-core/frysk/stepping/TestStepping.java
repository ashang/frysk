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

import frysk.Config;
//import frysk.event.Event;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.testbed.TestLib;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.TestfileTokenScanner;
import frysk.rt.BreakpointManager;
import frysk.rt.LineBreakpoint;
import frysk.rt.SourceBreakpoint;
import frysk.rt.SourceBreakpointObserver;
import frysk.stack.Frame;
import frysk.stack.StackFactory;
import frysk.sys.Pid;
import frysk.sys.Sig;

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

		Frame frame = StackFactory.createFrame(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-rt-steptester.c";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int start = this.scanner.findTokenLine("_lineStepFunctionCall_");

	/* The line number the test should end up at */
	int end = this.scanner.findTokenLine("_lineStepFunctionEntry_");

	/* The test process */
	AckDaemonProcess process = new AckDaemonProcess(Sig.USR1, new String[] {
		getExecPath("funit-rt-steptester"), "" + Pid.get(),
		"" + Sig.USR1_ });

	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new LineStepFunctionCallTest(end, myTask);

	Frame frame = StackFactory.createFrame(myTask);
	assertTrue("Line information present", frame.getLines().length > 0);

	/** The stepping operation */
	this.se.stepLine(myTask);

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

		Frame frame = StackFactory.createFrame(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-rt-steptester.c";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int start = this.scanner.findTokenLine("_lineStepIfPass_");

	/* The line number the test should end up at */
	int end = this.scanner.findTokenLine("_lineStepIfPassFinish_");

	//	System.err.println("start/end: " + start + " " + end);

	/* The test process */
	AckDaemonProcess process = new AckDaemonProcess(Sig.USR1, new String[] {
		getExecPath("funit-rt-steptester"), "" + Pid.get(),
		"" + Sig.USR1_ });

	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	//	Frame sframe = StackFactory.createFrame(myTask);

	//	System.err.println("CreateFrame: " + sframe.getLines()[0].getLine());

	this.currentTest = new LineStepIfStatementPassTest(end, myTask);

	Frame frame = StackFactory.createFrame(myTask);
	assertTrue("Line information present", frame.getLines().length > 0);

	/** The stepping operation */
	this.se.stepLine(myTask);

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

		Frame frame = StackFactory.createFrame(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-rt-steptester.c";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int start = this.scanner.findTokenLine("_lineStepIfFail_");

	/* The line number the test should end up at */
	int end = this.scanner.findTokenLine("_lineStepIfFailFinish_");

	/* The test process */
	AckDaemonProcess process = new AckDaemonProcess(Sig.USR1, new String[] {
		getExecPath("funit-rt-steptester"), "" + Pid.get(),
		"" + Sig.USR1_ });

	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new LineStepIfStatementFailTest(end, myTask);

	Frame frame = StackFactory.createFrame(myTask);
	assertTrue("Line information present", frame.getLines().length > 0);

	/** The stepping operation */
	this.se.stepLine(myTask);

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

		Frame frame = StackFactory.createFrame(testTask);
		int lineNr = frame.getLines()[0].getLine();
		assertTrue("line number", lineNr == success);
		Manager.eventLoop.requestStop();
	    }
	}

	/** Variable setup */

	String source = Config.getRootSrcDir()
		+ "frysk-core/frysk/pkglibdir/funit-rt-steptester.c";

	this.scanner = new TestfileTokenScanner(new File(source));

	/* The line number where the test begins */
	int start = this.scanner.findTokenLine("_lineStepFunctionReturn_");

	/* The line number the test should end up at */
	int end = this.scanner.findTokenLine("_lineStepFunctionCall_");

	/* The test process */
	AckDaemonProcess process = new AckDaemonProcess(Sig.USR1, new String[] {
		getExecPath("funit-rt-steptester"), "" + Pid.get(),
		"" + Sig.USR1_ });

	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new LineStepFunctionReturnTest(end, myTask);

	Frame frame = StackFactory.createFrame(myTask);
	assertTrue("Line information present", frame.getLines().length > 0);

	/** The stepping operation */
	this.se.stepLine(myTask);

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

		Frame frame = StackFactory.createFrame(testTask);
		if (frame.getLines().length == 0)
		{
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
	AckDaemonProcess process = new AckDaemonProcess(Sig.USR1, new String[] {
		getExecPath("funit-rt-siglongjmp"), "" + Pid.get(),
		"" + Sig.USR1_ });

	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new LineStepSigLongJmpTest(end, myTask);

	Frame frame = StackFactory.createFrame(myTask);
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

		Frame frame = StackFactory.createFrame(testTask);
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
	AckDaemonProcess process = new AckDaemonProcess(Sig.USR1, new String[] {
		getExecPath("funit-rt-goto"), "" + Pid.get(), "" + Sig.USR1_ });

	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new LineStepGotoTest(end, myTask);

	Frame frame = StackFactory.createFrame(myTask);
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

		Frame frame = StackFactory.createFrame(testTask);
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
	AckDaemonProcess process = new AckDaemonProcess(Sig.USR1, new String[] {
		getExecPath("funit-rt-sigraise"), "" + Pid.get(),
		"" + Sig.USR1_ });

	this.testStarted = false;

	/** Test initialization */
	Task myTask = initTask(process, source, start, end);

	this.currentTest = new LineStepSigRaiseTest(end, myTask);

	Frame frame = StackFactory.createFrame(myTask);
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

		Frame frame = StackFactory.createFrame(testTask);
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
	dbae = 
	    new DaemonBlockedAtEntry(new String[]{Config.getPkgLibDir()
		    + "/funit-rt-asmstepper"});
	
	Task theTask = dbae.getMainTask();
	
	this.testStarted = false;
	
	initTaskWithTask(theTask, source, startLine, endLine);
	
	this.currentTest = new SingleStepASMTest(endLine, theTask);

	Frame frame = StackFactory.createFrame(theTask);
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

		Frame frame = StackFactory.createFrame(testTask);
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
	dbae = 
	    new DaemonBlockedAtEntry(new String[]{Config.getPkgLibDir()
		    + "/funit-rt-asmstepper"});
	
	Task theTask = dbae.getMainTask();
	
	this.testStarted = false;
	
	initTaskWithTask(theTask, source, startLine, endLine);
	
	this.currentTest = new MultiStepASMTest(endLine, theTask);

	Frame frame = StackFactory.createFrame(theTask);
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

		Frame frame = StackFactory.createFrame(testTask);
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
	dbae = 
	    new DaemonBlockedAtEntry(new String[]{Config.getPkgLibDir()
		    + "/funit-rt-asmstepper"});
	
	Task theTask = dbae.getMainTask();
	
	this.testStarted = false;
	
	initTaskWithTask(theTask, source, startLine, endLine);
	
	this.currentTest = new ASMJumpTest(endLine, theTask);

	Frame frame = StackFactory.createFrame(theTask);
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

		Frame frame = StackFactory.createFrame(testTask);
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
	dbae = 
	    new DaemonBlockedAtEntry(new String[]{Config.getPkgLibDir()
		    + "/funit-stepping-asm"});
	
	Task theTask = dbae.getMainTask();
	
	this.testStarted = false;
	
	initTaskWithTask(theTask, source, startLine, endLine);
	
	this.currentTest = new StepFunctionEntryASMTest(endLine, theTask);

	Frame frame = StackFactory.createFrame(theTask);
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

		Frame frame = StackFactory.createFrame(testTask);
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
	dbae = 
	    new DaemonBlockedAtEntry(new String[]{Config.getPkgLibDir()
		    + "/funit-stepping-asm"});
	
	Task theTask = dbae.getMainTask();
	
	this.testStarted = false;
	
	initTaskWithTask(theTask, source, startLine, endLine);
	
	this.currentTest = new StepFunctionReturnASMTest(endLine, theTask);

	Frame frame = StackFactory.createFrame(theTask);
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
	
//	if (unresolved(4855))
//	    return;

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

		Frame frame = StackFactory.createFrame(testTask);
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
	dbae = 
	    new DaemonBlockedAtEntry(new String[]{Config.getPkgLibDir()
		    + "/funit-stepping-asm"});
	
	Task theTask = dbae.getMainTask();
	
	this.testStarted = false;
	
	initTaskWithTask(theTask, source, startLine, endLine);
	
	this.currentTest = new StepOverASMFunctionTest(endLine, theTask);

	Frame frame = StackFactory.createFrame(theTask);
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
	
//	if (unresolved(4855))
//	    return;
	
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

		Frame frame = StackFactory.createFrame(testTask);
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
	dbae = 
	    new DaemonBlockedAtEntry(new String[]{Config.getPkgLibDir()
		    + "/funit-stepping-asm"});
	
	Task theTask = dbae.getMainTask();
	
	this.testStarted = false;
	
	initTaskWithTask(theTask, source, startLine, endLine);
	
	this.currentTest = new StepOutASMFunctioNTest(endLine, theTask);

	Frame frame = StackFactory.createFrame(theTask);
	assertTrue("Line information present", frame.getLines().length > 0);

	/** The stepping operation */
	this.se.stepOut(theTask, frame);

	this.testStarted = true;
	//System.err.println("waiting for finish");
	/** Run to completion */
	assertRunUntilStop("Running test");
	cleanup();
    }
    
    boolean genericUpdate = false;

    public Task initTask(AckDaemonProcess process, String source,
	    int startLine, int endLine) {

	Task myTask = process.findTaskUsingRefresh(true);
	
	initTaskWithTask(myTask, source, startLine, endLine);
	return myTask;
    }
    
    DaemonBlockedAtEntry dbae = null;
    
    public void initTaskWithTask(Task myTask, String source,
	    int startLine, int endLine) {
	
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
	//	testStarted = true;
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

    ////////////////////////////////////////////////////////////////////////////////

    private interface SteppingTest {
	void runAssertions();
    }

    ////////////////////////////////////////////////////////////////////////////////

    protected class AttachedObserver implements TaskObserver.Attached {
	
	public void addedTo(Object o) {
	}

	public Action updateAttached(Task task) {

//	    theTask = task;
//	    initTaskWithTask(task, theSource, theStartLine, theEndLine);
//
//	    Manager.eventLoop.requestStop();
//
//	    task.requestDeleteAttachedObserver(this);
	    return Action.CONTINUE;
	}

	public void addFailed(Object observable, Throwable w) {
	}

	public void deletedFrom(Object o) {
	}
    }

    ////////////////////////////////////////////////////////////////////////////////
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
	 *                An Object argument, usually a Task when important
	 */
	public synchronized void update(Observable o, Object arg) {
	    TaskStepEngine tse = (TaskStepEngine) arg;
	    //	    System.err.println("Lock.update " + tse.isStopped() + " " + testStarted);
	    if (testStarted == true && tse.isStopped()) {//System.err.println("-----> Running ASSERTIONS");
		currentTest.runAssertions();
	    }
	    //	    else if (testStarted == false && tse.isStopped())
	    //		Manager.eventLoop.requestStop();
	    else
		return;
	}
    }

    ////////////////////////////////////////////////////////////////////////////////

    private class TestSteppingBreakpoint implements SourceBreakpointObserver {

	public void updateHit(SourceBreakpoint breakpoint, Task task,
		long address) {

	    //	    currentTest.setUp(task);
	    //	    testStarted = true;
	    Manager.eventLoop.requestStop();
	}

	public void addFailed(Object observable, Throwable w) {
	}

	public void addedTo(Object observable) { //System.err.println("Breakpoint ADDEDTO");
	}

	public void deletedFrom(Object observable) {
	}
    }

    ////////////////////////////////////////////////////////////////////////////////
}
