// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import frysk.testbed.TestLib;
import lib.dwfl.*;
import frysk.dwfl.*;

public class TestInstructions
  extends TestLib
{
  private Task task;

  private long start_address;
  private long end_address;

  private ArrayList addresses;

  // Created and added by setup() in blocked state.
  // Tests will need to delete (or unblock) it as soon as they are ready
  // setting up their own observer.
  private InstructionObserver io;

  /**
   * Returns the address of a global label by quering the the Proc
   * main Task's Dwlf.
   */
  long getGlobalLabelAddress(String label)
  {
    Dwfl dwfl = DwflCache.getDwfl(task);
    Symbol sym = Symbol.get(dwfl, label);
    return sym.getAddress();
  }
  
  // Helper class since there there isn't a get symbol method in Dwfl,
  // so we need to wrap it all in a builder pattern.
  static class Symbol implements SymbolBuilder
  {
    private String name;
    private long address;

    private boolean found;

    private Symbol()
    {
      // properties get set in public static get() method.
    }

    static Symbol get(Dwfl dwfl, String name)
    {
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

    String getName()
    {
      return name;
    }

    long getAddress()
    {
      return address;
    }

    public void symbol(String name, long value, long size,
		       int type, int bind, int visibility)
    {
      if (name.equals(this.name))
	{
	  this.address = value;
	  this.found = true;
	}
    }
  }

  /**
   * Launch our test program and setup clean environment with the test
   * program spawned, readers, writers setup and eventLoop and all
   * labels read.
   */
  public void setUp()
  {
    // Make sure everything is setup so spawned processes are recognized
    // and destroyed in tearDown().
    super.setUp();

    // Create a process that we want to observe.
    AttachedObserver ao = new AttachedObserver();
    String[] cmd = new String[] { getExecPath("funit-instructions") };
    Manager.host.requestCreateAttachedProc("/dev/null",
                                           "/dev/null",
                                           "/dev/null", cmd, ao);
    assertRunUntilStop("attach then block");
    assertTrue("AttachedObserver got Task", ao.task != null);

    task = ao.task;

    start_address = getGlobalLabelAddress("istart");
    end_address = getGlobalLabelAddress("iend");

    CodeObserver code = new CodeObserver(start_address);
    task.requestAddCodeObserver(code, start_address);
    assertRunUntilStop("inserting setup code observer");
    task.requestDeleteAttachedObserver(ao);
    assertRunUntilStop("setup start address code observer");

    // Read the addresses of all the instructions by stepping from
    // istart to iend.
    addresses = new ArrayList();
    addresses.add(Long.valueOf(start_address));
    io = new InstructionObserver(task);
    task.requestAddInstructionObserver(io);
    assertRunUntilStop("setup instruction observer");
    task.requestDeleteCodeObserver(code, start_address);
    assertRunUntilStop("setup remove start code observer");

    boolean iend_seen = false;
    while (! iend_seen)
      {
	task.requestUnblock(io);
	assertRunUntilStop("Step till iend");
	long addr = io.getAddr();
	Long value = Long.valueOf(addr);
	addresses.add(value);
	iend_seen = addr == end_address;
      }

    // And make one final step out of the istart-iend range
    task.requestUnblock(io);
    assertRunUntilStop("Step out of range");
  }

  /**
   * Make sure the test program is really gone and the event loop is
   * stopped.  Individual tests are responsible for nice termination
   * if the want to.
   */
  public void tearDown()
  {
    task = null;
    addresses = null;

    Manager.eventLoop.requestStop();

    // And kill off any remaining processes we spawned
    super.tearDown();
  }

  public void testBreakOnStartThenStepAllInstructions()
  {
    Long value = (Long) addresses.remove(0);
    long first = value.longValue();
    CodeObserver code = new CodeObserver(first);
    task.requestAddCodeObserver(code, first);
    assertRunUntilStop("first code observer added");

    // Go!
    task.requestDeleteInstructionObserver(io);
    assertRunUntilStop("Remove setup instruction observer");

    assertEquals("stopped at first breakpoint",
		 task.getIsa().pc(task), first);

    // Reinsert instruction observer now that we are at the start.
    task.requestAddInstructionObserver(io);
    assertRunUntilStop("add instruction observer");

    task.requestUnblock(code);
    Iterator it = addresses.iterator();
    while (it.hasNext())
      {
	long addr = ((Long) it.next()).longValue();
	task.requestUnblock(io);
	assertRunUntilStop("unblock for " + addr);
	assertEquals("step observer hit: " + addr, io.getAddr(), addr);
      }

      task.requestUnblock(io);
  }

  public void testAllBreakpoints()
  {
    // Map to make sure that even if we loop only one code observer is
    // inserted at each address.
    HashMap codeObserver = new HashMap();
    ArrayList codeObservers = new ArrayList();
    Iterator it = addresses.iterator();
    while (it.hasNext())
      {
	Long value = (Long) it.next();
	CodeObserver code = (CodeObserver) codeObserver.get(value);
	if (code == null)
	  {
	    long addr = value.longValue();
	    code = new CodeObserver(addr);
	    codeObserver.put(value, code);
	    task.requestAddCodeObserver(code, addr);
	    assertRunUntilStop("add code observer" + addr);
	  }
	codeObservers.add(code);
      }

    // Go!
    task.requestDeleteInstructionObserver(io);
    assertRunUntilStop("Remove setup instruction observer");

    it = addresses.iterator();
    while (it.hasNext())
      {
	long addr = ((Long) it.next()).longValue();
	CodeObserver code = (CodeObserver) codeObservers.remove(0);
	assertEquals("code observer hit: " + addr,
		     task.getIsa().pc(task), addr);
	task.requestUnblock(code);
	if (it.hasNext())
	  assertRunUntilStop("wait for next code observer hit after "
			     + Long.toHexString(addr));
      }
  }

  public void testInsertAllBreakpointsAndStepAll()
  {
    // Map to make sure that even if we loop only one code observer is
    // inserted at each address.
    HashMap codeObserver = new HashMap();
    ArrayList codeObservers = new ArrayList();
    Iterator it = addresses.iterator();
    while (it.hasNext())
      {
	Long value = (Long) it.next();
	CodeObserver code = (CodeObserver) codeObserver.get(value);
	if (code == null)
	  {
	    long addr = value.longValue();
	    code = new CodeObserver(addr);
	    codeObserver.put(value, code);
	    task.requestAddCodeObserver(code, addr);
	    assertRunUntilStop("add code observer" + addr);
	  }
	codeObservers.add(code);
      }

    // Go!
    io.setBlock(false);
    task.requestUnblock(io);
    assertRunUntilStop("Unblock setup instruction observer");
    
    it = addresses.iterator();
    while (it.hasNext())
      {
	long addr = ((Long) it.next()).longValue();
	CodeObserver code = (CodeObserver) codeObservers.remove(0);
	assertEquals("code observer hit: " + addr,
		     task.getIsa().pc(task), addr);
	assertEquals("step observer hit: " + addr, io.getAddr(), addr);
	task.requestUnblock(io);
	task.requestUnblock(code);
	if (it.hasNext())
	  assertRunUntilStop("wait for next code observer hit after "
			     + Long.toHexString(addr));
      }
  }
  
  class AttachedObserver implements TaskObserver.Attached
  {
    Task task;
    
    public void addedTo (Object o){ }
    
    public Action updateAttached (Task task)
    {
      this.task = task;
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }
    
    public void addFailed  (Object observable, Throwable w)
    {
      System.err.println("addFailed: " + observable + " cause: " + w);
    }
    
    public void deletedFrom (Object o) { }
  }

  private class CodeObserver
    implements TaskObserver.Code
  {
    long address;
    
    CodeObserver(long address)
    {
      this.address = address;
    }
    
    public Action updateHit (Task task, long address)
    {
      if (address != this.address)
	fail("updateHit on unknown address");
      
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }
    
    public void addFailed(Object o, Throwable w)
    {
      fail("add to " + o + " failed, because " + w);
    }
    
    public void addedTo(Object observable)
    {
      Manager.eventLoop.requestStop();
    }
    
    public void deletedFrom(Object observable)
    {
      Manager.eventLoop.requestStop();
    }
  }

  private static class InstructionObserver
    implements TaskObserver.Instruction
  { 
    private final Task task; 
    private long addr;

    private boolean block = true;

    public void addedTo(Object o)
    {
      Manager.eventLoop.requestStop();
    }

    public void deletedFrom(Object o)
    {
    }

    public void addFailed (Object o, Throwable w)
    {
      fail("add to " + o + " failed, because " + w);
    }

    InstructionObserver(Task task)
    {
      this.task = task;
    }
    
    public Action updateExecuted(Task task)
    {
      if (! task.equals(this.task))
        throw new IllegalStateException("Wrong Task, given " + task
                                        + " not equals expected "
                                        + this.task);
      
      addr = task.getIsa().pc(task);
      if (block)
	{
	  Manager.eventLoop.requestStop();
	  return Action.BLOCK;
	}
      return Action.CONTINUE;
    }

    public void setBlock(boolean block)
    {
      this.block = block;
    }

    public long getAddr()
    {
      return addr;
    }
  }
}
