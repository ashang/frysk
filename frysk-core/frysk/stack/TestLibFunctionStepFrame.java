// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

package frysk.stack;

import java.io.*;
import java.util.*;

import frysk.config.*;

import frysk.testbed.*;

import frysk.proc.*;
import frysk.rt.*;
import lib.dwfl.ElfSymbol;

/**
 * Test making sure all frames are available when stepping (twice)
 * through a shared library call that goes through a plt entry. It
 * checks on each step that both the calling function and the main
 * function are outer frames (in that order) of the inner frame.
 *
 * Note this checks the low level (libunwind) frames walking.  It
 * doesn't test anything at a higher level or with the SteppingEngine
 * (currently the SteppingEngine tries to explicitly step through the
 * plt entries even when instruction stepping).
 */
public class TestLibFunctionStepFrame
  extends TestLib
  implements TaskObserver.Code, TaskObserver.Instruction
{
  public void testStepIntoLibFunctionCall()
  {
    if (unresolvedOnPPC(5259))
      return;

    String source = Config.getRootSrcDir()
      + "frysk-core/frysk/pkglibdir/funit-libfunccall.c";

    TestfileTokenScanner scanner = new TestfileTokenScanner(new File(source));
    int firstLine = scanner.findTokenLine("_libfunccall_");
    int secondLine = scanner.findTokenLine("_libfunccall2_");
    int lastLine = scanner.findTokenLine("_last_line_");

    File exe = Config.getPkgLibFile("funit-libfunccall");
    DaemonBlockedAtEntry dbae = new DaemonBlockedAtEntry(exe);
    
    Task task = dbae.getMainTask();

    LineBreakpoint bp1 = new  LineBreakpoint(-1, source, firstLine, 0);
    LineBreakpoint bp2 = new  LineBreakpoint(-1, source, secondLine, 0);
    LineBreakpoint bpLast = new  LineBreakpoint(-1, source, lastLine, 0);

    List addresses = bp1.getBreakpointRawAddresses(task);
    List addresses2 = bp2.getBreakpointRawAddresses(task);
    List addressesLast = bpLast.getBreakpointRawAddresses(task);

    // Test sanity checks
    assertTrue("expecting one raw address1", addresses.size() == 1);
    assertTrue("expecting one raw address2", addresses2.size() == 1);

    long address1 = ((Long) addresses.get(0)).longValue();
    long address2 = ((Long) addresses2.get(0)).longValue();
    long addressLast = ((Long) addressesLast.get(0)).longValue();

    task.requestAddCodeObserver(this, address1);
    assertRunUntilStop("adding Code observer");

    dbae.requestUnblock();
    assertRunUntilStop("Continuing to initial breakpoint");

    // From here on we will step through to check all frames.
    task.requestAddInstructionObserver(this);
    assertRunUntilStop("adding Instruction observer");

    // One step and we should be at the first PLT instruction.
    task.requestUnblock(this);
    assertRunUntilStop("Do first step");

    // We are at the first plt intruction (for the first time).
    long firstPLTAddress = task.getPC();
    Frame frame = StackFactory.createFrame(task);
    assertFooAndMainOuterFrames("First entry in PLT", frame);

    // Count your steps. And bail out when there are too many.
    int steps = 1;
    boolean seenSecondCall = false;
    long currentPC = task.getPC();
    while (currentPC != addressLast && steps < 1000)
      {
	task.requestUnblock(this);
	assertRunUntilStop("Do step: "
			   + (seenSecondCall ? "second" : "")
			   + steps);

	currentPC = task.getPC();
	if (currentPC == address2)
	  {
	    seenSecondCall = true;
	    steps = 1;
	    // When we step we should be at the first PLTAddress again.
	    // One step and we should be at the first PLT instruction.
	    task.requestUnblock(this);
	    assertRunUntilStop("Do first 'second' step");
	    currentPC = task.getPC();
	    assertEquals("Second time in PLT", currentPC, firstPLTAddress);

	    frame = StackFactory.createFrame(task);
	    assertFooAndMainOuterFrames("Second entry in PLT", frame);
	  }
	else if (currentPC != addressLast && steps < 24)
	  {
	    // Only check first 24
	    frame = StackFactory.createFrame(task);
	    assertFooAndMainOuterFrames("Stepping "
					+ (seenSecondCall ? "second" : "")
					+ ", #" + steps
					+ " through (plt) call", frame);
	  }
	steps++;
      }

    assertTrue("less than a thousand steps", steps < 1000);
    assertTrue("seen second", seenSecondCall);
  }


  // Walks up the call stack and checks that both foo() and main() are
  // in there, in that order, and that they are not the first inner
  // frame.
  private void assertFooAndMainOuterFrames(String message, Frame frame)
  {
    ElfSymbol sym = frame.getSymbol();
    String name = sym.getName();
    boolean ok = name.indexOf("foo") == -1 && name.indexOf("main") == -1;
    if (! ok)
      printBacktrace(frame);
    assertTrue(message + " first inner frame should not be foo or main", ok);

    boolean foo_seen = false;
    boolean main_seen = false;
    Frame outer = frame.getOuter();
    while (ok && outer != null)
      {
	sym = outer.getSymbol();
	name = sym.getName();
	boolean sym_is_foo = name.indexOf("foo") != -1;
	if (! foo_seen && sym_is_foo)
	  foo_seen = true;
	else if (foo_seen && sym_is_foo)
	  {
	    ok = false;
	    printBacktrace(frame);
	    assertTrue(message
		       + " foo should be in the backtrace only once",
		       ok);
	  }

	boolean sym_is_main = name.indexOf("main") != -1;
	if (foo_seen && sym_is_main)
	  {
	    // Hurray done!
	    main_seen = true;
	    break;
	  }

	if (! foo_seen && sym_is_main)
	  {
	    ok = false;
	    printBacktrace(frame);
	    assertTrue(message
		       + " foo should appear before main in backtrace",
		       ok);
	  }
	outer = outer.getOuter();
      }

    ok = ok && foo_seen && main_seen && outer != null;
    if (! ok)
      printBacktrace(frame);
    assertTrue(message
	       + " both foo and main should be in the backtrace",
	       ok);
  }

  // Helper function to print backtrace from given frame.
  // Useful while debugging this test.
  private void printBacktrace(Frame frame)
  {
    System.out.println(frame);
    Frame outer = frame.getOuter();
    while (outer != null)
      {
	System.out.println("\t" + outer);
	outer = outer.getOuter();
      }
  }

  // Code observer method, just stop and block when breakpoint hit.
  public Action updateHit(Task task, long address)
  {
    Manager.eventLoop.requestStop();
    return Action.BLOCK;
  }

  // Instruction observer method, just stop and block after each step.
  public Action updateExecuted(Task task)
  {
    Manager.eventLoop.requestStop();
    return Action.BLOCK;
  }

  // TaskObserver methods, only addedTo is interesting.

  public void addFailed(Object observable, Throwable w)
  {
    // Whoa!
    w.printStackTrace();
  }
  
  public void addedTo(Object observable)
  {
    Manager.eventLoop.requestStop();
  }

  public void deletedFrom(Object observable)
  {
    // We never delete anything...
    System.err.println("deletedFrom: " + observable);
  }

}
