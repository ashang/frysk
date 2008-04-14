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

package frysk.proc;

import lib.dwfl.Dwfl;
import lib.dwfl.DwflModule;
import lib.dwfl.ElfSymbolBinding;
import lib.dwfl.ElfSymbolType;
import lib.dwfl.ElfSymbolVisibility;
import lib.dwfl.SymbolBuilder;
import frysk.config.Config;
import frysk.dwfl.DwflCache;
import frysk.isa.signals.Signal;
import frysk.rsl.Log;
import frysk.testbed.DaemonBlockedAtEntry;
import frysk.testbed.TestLib;

public class TestTaskObserverWatchpoint
extends TestLib
{

    private static final Log fine = Log.fine(TestTaskObserverWatchpoint.class);

    // This test case test whether watchpoints are caught when stepping. This is an important yet subtle
    // test. As a sigtrap is generated from ptrace each time "an event" happens, when one steps over a 
    // watchpoint "hitting" instruction there will be two sigtraps. One for the single-step completing
    // and one for the watchpoint being triggered. This test tests the notion that both events occurred,
    // that one of the two states was not starved of its sigtrap, that the watchpoint is hit, and the
    // process blocked.
    public void testSteppingInstructionAndWatchpoint()
    {
	if (unresolvedOnPPC(5991)) 
	    return;
	
	// Create a blocked child.
	DaemonBlockedAtEntry ackProc = new DaemonBlockedAtEntry(
		Config.getPkgLibFile("funit-watchpoint"));
	assertNotNull(ackProc);

	// Get Proc/Task.
	Proc proc = ackProc.getMainTask().getProc();
	Task task = proc.getMainTask();

	// Watch for any unexpected terminations of the child process.
	TerminatedObserver to = new TerminatedObserver();
	task.requestAddTerminatedObserver(to);

	// Break at main
	long mainAddress = getGlobalSymbolAddress(task, "main");
	CodeObserver co = new CodeObserver();
	task.requestAddCodeObserver(co, mainAddress);
	ackProc.requestUnblock();
	assertRunUntilStop("Run to main");

	// Add a stepping observer.
	InstructionObserver instr = new InstructionObserver(task);
	task.requestAddInstructionObserver(instr);
	instr.setContinue(true);

	// Find Variable source for watch
	long address = getGlobalSymbolAddress(task,"source");

	// Add watch observer
	WatchObserver watch = new WatchObserver(task, address, 4);
	task.requestAddWatchObserver(watch, address, 4, true);

	task.requestUnblock(co);
	assertRunUntilStop("Run and test watchpoint ");

	// Make sure it triggered.
	assertTrue("added", watch.added);
	assertEquals("hit code", 1, watch.hit);

	// Delete both observers.
	task.requestDeleteInstructionObserver(instr);
	task.requestDeleteCodeObserver(co, mainAddress);
	task.requestDeleteWatchObserver(watch, address, 4, true);
	runPending();

	// Verify they were removed.
	assertTrue("deleted instr", instr.deleted);
	assertTrue("deleted watch", watch.deleted);
	assertTrue("deleted code", co.deleted);
	assertEquals("hit code", 1, watch.hit);

    }

    // This test case tests whether watchpoints are caught when a task is in a straight
    // "running" condition. This really tests the basic and advertised functionality of watchpoints:
    // to be caught by hardware, not software. In this  test:  set up the watchpoint, set up
    // a terminated observer to guard the watchpoint was caught, and simply set the task to run.
    // If the watchpoint observer is called, and the test is blocked then the test passes. If the
    // process terminates and the watchpoint is not caught, then this signified an error condition.
    public void testRunningAndWatchpoint () {
	if (unresolvedOnPPC(5991)) 
	    return;
	
	DaemonBlockedAtEntry ackProc = new DaemonBlockedAtEntry(
		Config.getPkgLibFile("funit-watchpoint"));
	assertNotNull(ackProc);

	// Get Proc/Task.
	Proc proc = ackProc.getMainTask().getProc();
	Task task = proc.getMainTask();

	// Watch for any unexpected terminations of the child process.
	TerminatedObserver to = new TerminatedObserver();
	task.requestAddTerminatedObserver(to);

	// Break at main
	long mainAddress = getGlobalSymbolAddress(task, "main");
	CodeObserver co = new CodeObserver();
	task.requestAddCodeObserver(co, mainAddress);
	ackProc.requestUnblock();
	assertRunUntilStop("Run to main");

	// Find Variable source for watch
	long address = getGlobalSymbolAddress(task,"source");

	// Add watch observer
	WatchObserver watch = new WatchObserver(task, address, 4);
	task.requestAddWatchObserver(watch, address, 4, true);

	task.requestUnblock(co);
	assertRunUntilStop("Run and test watchpoint ");

	// Make sure it triggered.
	assertTrue("added", watch.added);
	assertEquals("hit code", 1, watch.hit);

	// Delete both observers.
	task.requestDeleteCodeObserver(co, mainAddress);
	task.requestDeleteWatchObserver(watch, address, 4, true);
	runPending();

	// Verify they were removed.
	assertTrue("deleted watch", watch.deleted);
	assertTrue("deleted code", co.deleted);
	assertEquals("hit code", 1, watch.hit);

    }


    // This test case tests whether 'read or write' watchpoints are caught when a task is in a straight
    // "running" condition.  In this  test:  set up the 'read or write' watchpoint, set up
    // a terminated observer to guard the watchpoint was caught, and simply set the task to run.
    // If the watchpoint observer is called on address read, and the test is blocked then the 
    // test passes. If the process terminates and the watchpoint is not caught, 
    // then this signified an error condition.
    public void testRunningAndReadOnlyWatchpoint () {
	if (unresolvedOnPPC(5991)) 
	    return;
	
	DaemonBlockedAtEntry ackProc = new DaemonBlockedAtEntry(
		Config.getPkgLibFile("funit-watchpoint"));
	assertNotNull(ackProc);

	// Get Proc/Task.
	Proc proc = ackProc.getMainTask().getProc();
	Task task = proc.getMainTask();

	// Watch for any unexpected terminations of the child process.
	TerminatedObserver to = new TerminatedObserver();
	task.requestAddTerminatedObserver(to);

	// Break at main
	long mainAddress = getGlobalSymbolAddress(task, "main");
	CodeObserver co = new CodeObserver();
	task.requestAddCodeObserver(co, mainAddress);
	ackProc.requestUnblock();
	assertRunUntilStop("Run to main");

	// Find Variable source for watch
	long address = getGlobalSymbolAddress(task,"read_only");

	// Add watch observer. Set it to fire on read OR write.
	WatchObserver watch = new WatchObserver(task, address, 4);
	task.requestAddWatchObserver(watch, address, 4, false);

	task.requestUnblock(co);
	assertRunUntilStop("Run and test watchpoint ");

	// Make sure it triggered.
	assertTrue("added", watch.added);
	assertEquals("hit code", 1, watch.hit);

	// Delete both observers.
	task.requestDeleteCodeObserver(co, mainAddress);
	task.requestDeleteWatchObserver(watch, address, 4, true);
	runPending();

	// Verify they were removed.
	assertTrue("deleted watch", watch.deleted);
	assertTrue("deleted code", co.deleted);
	assertEquals("hit code", 1, watch.hit);

    }

    // This test tests that a watchpoint is properly deleted: from the othe observer lists
    // and the hardware. In this case, if the watchpoint is caught, then it fails. It should have
    // been deleted. This test adds the watchpoint, then runs to main, then deletes the watchpoint.
    // After that, the test then steps the thread until termination. If the Terminated observer is
    // called, and the watchpoint did not "hit", then the test passes.
    public void testAddthenDeleteWatchpoint()
    {
	if (unresolvedOnPPC(5991)) 
	    return;
	
	// Create a blocked child.
	DaemonBlockedAtEntry ackProc = new DaemonBlockedAtEntry(
		Config.getPkgLibFile("funit-watchpoint"));
	assertNotNull(ackProc);

	// Get Proc/Task.
	Proc proc = ackProc.getMainTask().getProc();
	Task task = proc.getMainTask();

	// Very early on, find Variable source for watch
	long address = getGlobalSymbolAddress(task,"source");

	// Very early on, add watch observer
	FailingWatchObserver watch = new FailingWatchObserver();
	task.requestAddWatchObserver(watch, address, 4, true);

	
	// Watch for any expected terminations of the child process.
	OkToTerminateObserver to = new OkToTerminateObserver();
	task.requestAddTerminatedObserver(to);

	// Break at main
	long mainAddress = getGlobalSymbolAddress(task, "main");
	CodeObserver co = new CodeObserver();
	task.requestAddCodeObserver(co, mainAddress);
	ackProc.requestUnblock();
	assertRunUntilStop("Run to main");

	// Test the watch was added
	assertTrue("added", watch.added);
	
	// Test the bp was added, and hit
	assertTrue("added", co.added);
	assertEquals("code bp hit", 1, co.hit);

	// Now Delete the watch observer, it should never be hit.
	task.requestDeleteWatchObserver(watch, address, 4, true);

	// Add a stepping observer.
	InstructionObserver instr = new InstructionObserver(task);
	task.requestAddInstructionObserver(instr);
	instr.setContinue(true);

	// Remove the bp, and run.
	task.requestUnblock(co);
	assertRunUntilStop("Run and test watchpoint ");

	// Task should have terminated now.
	// Make sure the watchpoint was deleted
	assertTrue("deleted watch", watch.deleted);

	// Make sure the watchpoint never triggered.
	assertEquals("hit code", 0, watch.hit);

	// Make sure the task has terminated
	assertEquals("task terminated", 1, to.hit);
	
    }

    // This test case tests whether watchpoints are caught when a task is in a straight
    // "running" condition. When caught, the observer sets the task to Action.CONTINUE.
    // Test that the task does continue, the the hardware debug registers have been
    // properly set to allow this to happen, and that the task does continue 
    // until termination.
    public void testWatchpointActionContinue () {
	if (unresolvedOnPPC(5991)) 
	    return;
	
	DaemonBlockedAtEntry ackProc = new DaemonBlockedAtEntry(
		Config.getPkgLibFile("funit-watchpoint"));
	assertNotNull(ackProc);

	// Get Proc/Task.
	Proc proc = ackProc.getMainTask().getProc();
	Task task = proc.getMainTask();

	// Watch for expected terminations of the child process.
	OkToTerminateObserver to = new OkToTerminateObserver();
	task.requestAddTerminatedObserver(to);

	// Break at main
	long mainAddress = getGlobalSymbolAddress(task, "main");
	CodeObserver co = new CodeObserver();
	task.requestAddCodeObserver(co, mainAddress);
	ackProc.requestUnblock();
	assertRunUntilStop("Run to main");

	// Find Variable source for watch
	long address = getGlobalSymbolAddress(task,"source");

	// Add watch observer
	WatchObserver watch = new WatchObserver(task, address, 4);

	// Do not block on trigger
	watch.setBlock(false);
	task.requestAddWatchObserver(watch, address, 4, true);

	// Unblock task from breakpoint
	task.requestUnblock(co);
	assertRunUntilStop("Run and test watchpoint ");

	// Make sure it triggered.
	assertTrue("added", watch.added);
	assertEquals("hit code", 1, watch.hit);
	assertEquals("Task terminated", 1, to.hit);
    }

    
    // Base class for this tests observers.
    // Keeps track of added, deleted and hit counts.
    static abstract class TestObserver implements TaskObserver
    {
	boolean added;
	boolean deleted;

	int hit;

	public void addedTo(Object o)
	{
	    added = true;
	}

	public void deletedFrom(Object o)
	{
	    deleted = true;
	}

	public void addFailed (Object o, Throwable w)
	{
	    fail("add to " + o + " failed, because " + w);
	}
    }

    // Code observer. Run to a point in the program (normally main)
    // then block
    static class CodeObserver extends TestObserver
    implements TaskObserver.Code {

	public Action updateHit(Task task, long address) {
	    fine.log("Hit breakpoint at ", address);
	    hit++;
	    Manager.eventLoop.requestStop();
	    return Action.BLOCK;
	}

    }
    // Simple observer to alert when child process dies unexpectedly
    static class TerminatedObserver extends TestObserver
    implements TaskObserver.Terminated
    {
	// Shouldn't be triggered ever.
	public Action updateTerminated(Task task, Signal signal, int value) {
	    Manager.eventLoop.requestStop();
	    fail(task + " terminated; signal=" + signal
		    + " value=" + value);
	    return null; // not reached
	}
    }

    // Simple observer to count termination of a thread when it 
    // is ok to do so.
    static class OkToTerminateObserver extends TestObserver
    implements TaskObserver.Terminated
    {
	// Shouldn't be triggered ever.
	public Action updateTerminated(Task task, Signal signal, int value) {
	    hit++;
	    Manager.eventLoop.requestStop();
	    return Action.BLOCK;
	}
	
    }


    // Instruction Observer to simulate stepping
    static class InstructionObserver
    	extends TestObserver
    implements TaskObserver.Instruction
    {
	private final Task task;
	private boolean cont;

	long lastAddress;

	InstructionObserver(Task task)
	{
	    this.task = task;
	}

	public Action updateExecuted(Task task)
	{

	    fine.log("Executing instruction at ", task.getPC());
	    if (! task.equals(this.task))
		throw new IllegalStateException("Wrong Task, given " + task
			+ " not equals expected "
			+ this.task);

	    hit++;
	    lastAddress = task.getPC();

	    if (cont)
		return Action.CONTINUE;
	    else
	    {
		Manager.eventLoop.requestStop();
		return Action.BLOCK;
	    }
	}

	public void setContinue(boolean cont)
	{
	    this.cont = cont;
	}
    }

    // Watchpoint observer to block our process
    // when it (should) trigger.
    static class WatchObserver
    extends TestObserver
    implements TaskObserver.Watch
    {
	private final Task task;
	private final long address;
	private final long length;
	private boolean block = true;
	
	public WatchObserver(Task task, long address, int length)
	{
	    this.task = task;
	    this.address = address;
	    this.length = length;
	}


	public Action updateHit(Task task, long address, int length) {
	    fine.log("Hit watchpoint, addess ", address);
	    if (! task.equals(this.task))
		throw new IllegalStateException("Wrong Task, given " + task
			+ " not equals expected "
			+ this.task);
	    if (address != this.address)
		throw new IllegalStateException("Wrong address, given " + address
			+ " not equals expected "
			+ this.address);

	    if (length != this.length)
		throw new IllegalStateException("Wrong length, given " + length
			+ " not equals expected "
			+ this.length);

	    hit++;
	    if (this.block) {
		Manager.eventLoop.requestStop();
		return Action.BLOCK;
	    }
	    else
		return Action.CONTINUE;
	}
	
	public void setBlock(boolean block) {
	    this.block = block;
	}
    }


    // Add watchpoint observer to block our process
    // when it (should) trigger. This is a test case
    // observer that should never fire. Used to test if 
    // a watchpoint is really deleted.
    static class FailingWatchObserver
    extends TestObserver
    implements TaskObserver.Watch
    {


	public Action updateHit(Task task, long address, int length) {
	    fine.log("Hit watchpoint, addess ", address);
	    hit++;
	    Manager.eventLoop.requestStop();
	    fail(task + " hit watchpoint at 0x"+Long.toHexString(address)+
		    " but should not as the watchpoint was deleted");
	    return null; // not reached
	}
    }

    /**
     * Returns the address of a global label by quering the the Proc
     * main Task's Dwlf.
     */
    long getGlobalSymbolAddress(Task task, String label)  {
	Dwfl dwfl = DwflCache.getDwfl(task);
	Symbol sym = Symbol.get(dwfl, label);
	return sym.getAddress();
    }


    // Helper class since there there isn't a get symbol method in Dwfl,
    // so we need to wrap it all in a builder pattern.
    static class Symbol implements SymbolBuilder {
	private String name;
	private long address;

	private boolean found;

	private Symbol()  {
	    // properties get set in public static get() method.
	}

	static Symbol get(Dwfl dwfl, String name)  {
	    Symbol sym = new Symbol();
	    sym.name = name;
	    DwflModule[] modules = dwfl.getModules();
	    for (int i = 0; i < modules.length && ! sym.found; i++)
		modules[i].getSymbolByName(name, sym);

	    if (sym.found)
		return sym;
	    else
		return null;
	}

	String getName() {
	    return name;
	}

	long getAddress() {
	    return address;
	}

	public void symbol(String name, long value, long size, ElfSymbolType type,
		ElfSymbolBinding bind, ElfSymbolVisibility visibility) {
	    if (name.equals(this.name)) {
		this.address = value;
		this.found = true;
	    }

	}
    } 
}
