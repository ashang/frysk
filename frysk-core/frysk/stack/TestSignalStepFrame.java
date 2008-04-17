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
import frysk.config.Prefix;
import frysk.testbed.*;
import frysk.proc.*;
import frysk.rt.*;
import frysk.symtab.*;
import frysk.isa.signals.Signal;

/**
 * Test making sure all frames are available when stepping through a
 * signal call. It checks on each step that both the interruped
 * function and the main function are outer frames (in that order) of
 * the inner frame (which is the signal processing function of the
 * task).
 *
 * Note this checks the low level (libunwind) frames walking.  It
 * doesn't test anything at a higher level or with the SteppingEngine.
 */
public class TestSignalStepFrame
  extends TestLib
  implements TaskObserver.Code, TaskObserver.Instruction, TaskObserver.Signaled
{
  // Starts the funit-loop-signal test and return the Task at the point
  // that the signal arrived.
    private Task setupLoopSignalTest() {
      File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-loop-signal.c");

    TestfileTokenScanner scanner = new TestfileTokenScanner(source);
    int foo_entry = scanner.findTokenLine("_foo_entry_");

    File exe = Prefix.pkgLibFile("funit-loop-signal");
    DaemonBlockedAtEntry dbae = new DaemonBlockedAtEntry(exe);
    
    Task task = dbae.getMainTask();

    LineBreakpoint entryBreak = new LineBreakpoint(-1, source, foo_entry, 0);

    List entryAddresses = entryBreak.getBreakpointRawAddresses(task);

    // Test sanity check
    assertTrue("expecting one entry address", entryAddresses.size() == 1);

    long entryAddress = ((Long) entryAddresses.get(0)).longValue();

    task.requestAddCodeObserver(this, entryAddress);
    assertRunUntilStop("adding Code observer");

    dbae.requestUnblock();
    assertRunUntilStop("Continuing to initial breakpoint");

    // OK, everything setup. Now wait for the signal to arrive.
    task.requestAddSignaledObserver(this);
    assertRunUntilStop("adding Signaled observer");

    task.requestUnblock(this);
    assertRunUntilStop("waiting for signal to arrive");

    // Signal arrived, on to the real test...
    return task;
  }

  public void testFirstFrameInSignalHandler()
  {
    Task task = setupLoopSignalTest();

    // Get the start address of the signal handler.
    File source = Prefix.sourceFile("frysk-core/frysk/pkglibdir/funit-loop-signal.c");
    TestfileTokenScanner scanner = new TestfileTokenScanner(source);
    int sig_entry = scanner.findTokenLine("_signal_handler_");
    LineBreakpoint signalBreak = new LineBreakpoint(-1, source, sig_entry, 0);
    List signalAddresses = signalBreak.getBreakpointRawAddresses(task);
    // Test sanity check
    assertTrue("expecting one entry address", signalAddresses.size() == 1);
    long signalAddress = ((Long) signalAddresses.get(0)).longValue();
    Symbol sym = SymbolFactory.getSymbol(task, signalAddress);
    long signalStartAddr = sym.getAddress();
    
    // One step and we should be at the first signal handler instruction.
    task.requestAddInstructionObserver(this);
    assertRunUntilStop("adding Instruction observer");
    task.requestUnblock(this);
    assertRunUntilStop("Do first step into signal handler");

    // We are at the first signal handler instruction.
    // Check out frames.
    Frame frame = StackFactory.createFrame(task);
    assertFooAndMainOuterFrames("First frame in handler", frame);
    assertEquals(frame.getAddress(), signalStartAddr);
  }

  public void testReturnFrameAfterSignalHandler()
  {
    if (unresolvedOnIA32(6044))
      return;

    Task task = setupLoopSignalTest();

    // Record current pc and outer frame to check after signal is processed.
    long returnAddress = task.getPC();
    Frame returnOuter = StackFactory.createFrame(task).getOuter();

    // From here on we will step through signal handler to see if we return.
    task.requestAddInstructionObserver(this);
    assertRunUntilStop("adding Instruction observer");

    // One step and we should be at the first signal handler instruction.
    task.requestUnblock(this);
    assertRunUntilStop("Do first step into signal handler");

    // We are at the first signal handler instruction.
    // Check out frames.
    Frame frame = StackFactory.createFrame(task);
    assertFooAndMainOuterFrames("First frame in handler", frame);

    // Count your steps. And bail out when there are too many.
    int steps = 1;
    long currentPC = task.getPC();
    while (steps < 1000)
      {
	task.requestUnblock(this);
	assertRunUntilStop("Do step: " + steps);
	currentPC = task.getPC();
	if (currentPC == returnAddress)
	  break; // back from the signal!
	steps++;
      }
    
    assertTrue("more than one step", steps > 1);
    assertTrue("less than a thousand steps", steps < 1000);

    // Check if outerframe correct after signal.
    Frame outer = StackFactory.createFrame(task).getOuter();
    assertEquals("outer frame correct",
		 returnOuter.getAddress(), outer.getAddress());
  }

  public void testStepSignalCallAllFrames()
  {
    if (unresolvedOnIA32(5961))
      return;

    Task task = setupLoopSignalTest();

    // Record return address
    long returnAddress = task.getPC();

    // From here on we will step through to check all frames.
    task.requestAddInstructionObserver(this);
    assertRunUntilStop("adding Instruction observer");

    // One step and we should be at the first signal handler instruction.
    task.requestUnblock(this);
    assertRunUntilStop("Do first step into signal handler");

    // We are at the first signal handler instruction (actually right
    // after it, a step into the signal handler executes the first
    // instruction).
    Frame frame = StackFactory.createFrame(task);
    assertFooAndMainOuterFrames("First frame in handler", frame);

    // Count your steps. And bail out when there are too many.
    int steps = 1;
    long currentPC = task.getPC();
    while (steps < 1000)
      {
	task.requestUnblock(this);
	assertRunUntilStop("Do step: " + steps);
	currentPC = task.getPC();
	if (currentPC == returnAddress)
	  break; // back from the signal!
	frame = StackFactory.createFrame(task);
	assertFooAndMainOuterFrames("In signal handler step: " + steps, frame);
	steps++;
      }
    
    assertTrue("more than one step", steps > 1);
    assertTrue("less than a thousand steps", steps < 1000);
  }


  // Walks up the call stack and checks that both foo() and main() are
  // in there, in that order, and that they are not the first inner
  // frame.
  private void assertFooAndMainOuterFrames(String message, Frame frame)
  {
    Symbol sym = frame.getSymbol();
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

  // SignaledObserver method, just stop and block when signal comes in.
  public Action updateSignaled(Task task, Signal signal)
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
