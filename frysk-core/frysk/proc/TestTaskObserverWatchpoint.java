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

    // And watchpoint observer to block our process
    // when it (should) trigger.
    static class WatchObserver
    extends TestObserver
    implements TaskObserver.Watch
    {
	private final Task task;
	private final long address;
	private final long length;

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
	    Manager.eventLoop.requestStop();
	    return Action.BLOCK;
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
