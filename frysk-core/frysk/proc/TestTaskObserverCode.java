// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
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

import inua.eio.*;

import frysk.sys.*;

import lib.dwfl.*;

import java.util.*;
import frysk.testbed.TestLib;

public class TestTaskObserverCode extends TestLib
{
  private Child child;
  private Task task;
  private Proc proc;

  public void testCode() throws Exception
  {
    // Create a child.
    child = new AckDaemonProcess();
    task = child.findTaskUsingRefresh (true);
    proc = task.getProc();

    // Make sure we are attached.
    AttachedObserver attachedObserver = new AttachedObserver();
    task.requestAddAttachedObserver(attachedObserver);
    assertRunUntilStop("adding AttachedObserver");

    long address = getFunctionEntryAddress("bp1_func");
    CodeObserver code = new CodeObserver(task, address);
    task.requestUnblock(attachedObserver);
    task.requestAddCodeObserver(code, address);
    assertRunUntilStop("add breakpoint observer");

    assertFalse(code.hit);

    // Request a run and watch the breakpoint get hit.
    requestDummyRun();
    assertRunUntilStop("signal and wait for hit");

    assertTrue(code.hit);

    // Try that again.
    task.requestDeleteCodeObserver(code, address);
    assertRunUntilStop("remove code observer");

    task.requestAddCodeObserver(code, address);
    assertRunUntilStop("readd breakpoint observer");

    code.hit = false;

    requestDummyRun();
    assertRunUntilStop("signal and wait for next hit");

    assertTrue(code.hit);

    // Now try it running without deleting the observer a couple of times.
    for (int i = 0; i < 12; i++)
      {
	code.hit = false;
	task.requestUnblock(code);
	Manager.eventLoop.runPending();
	
	requestDummyRun();
	assertRunUntilStop("hit it again Sam: " + i);
	
	assertTrue(code.hit);
      }

    // Another remove, add, hit.
    task.requestDeleteCodeObserver(code, address);
    assertRunUntilStop("remove code observer again");

    task.requestAddCodeObserver(code, address);
    assertRunUntilStop("readd breakpoint observer again");

    code.hit = false;

    requestDummyRun();
    assertRunUntilStop("signal and wait for next hit again");

    assertTrue(code.hit);

    // Remove, run again, insert and hit
    code.hit = false;
    task.requestDeleteCodeObserver(code, address);
    requestDummyRun();
    Manager.eventLoop.runPending();

    assertFalse(code.hit);

    task.requestAddCodeObserver(code, address);
    assertRunUntilStop("readd breakpoint observer after run");

    requestDummyRun();
    assertRunUntilStop("signal and wait for next hit after run");

    assertTrue(code.hit);

    // And cleanup
    task.requestDeleteCodeObserver(code, address);
    assertRunUntilStop("cleanup");
  }

  public void testCodeRemovedInHit() throws Exception
  {
    // Create a child.
    child = new AckDaemonProcess();
    task = child.findTaskUsingRefresh (true);
    proc = task.getProc();

    // Make sure we are attached.
    AttachedObserver attachedObserver = new AttachedObserver();
    task.requestAddAttachedObserver(attachedObserver);
    TerminatingObserver terminatingObserver = new TerminatingObserver();
    task.requestAddTerminatingObserver(terminatingObserver);
    assertRunUntilStop("adding AttachedObserver & TerminatingObserver");

    long address1 = getFunctionEntryAddress("bp1_func");
    long address2 = getFunctionEntryAddress("bp2_func");
    RemovingCodeObserver code = new RemovingCodeObserver();
    task.requestUnblock(attachedObserver);
    task.requestAddCodeObserver(code, address1);
    assertRunUntilStop("add breakpoint observer at address1");
    task.requestAddCodeObserver(code, address2);
    assertRunUntilStop("add breakpoint observer at address1");

    assertEquals(code.hits, 0);

    // Request a run and watch the breakpoint get hit.
    requestDummyRun();
    assertRunUntilStop("wait for delete 1");
    assertRunUntilStop("wait for delete 2");

    assertEquals(code.hits, 2);
    assertEquals(code.deletes, 2);
  }

  // Tests that breakpoint instructions are not visible to
  // normal users of task memory (only through raw memory view).
  public void testViewBreakpointMemory() throws Exception
  {
    // Create a child.
    child = new AckDaemonProcess();
    task = child.findTaskUsingRefresh (true);
    proc = task.getProc();

    // Make sure we are attached.
    AttachedObserver attachedObserver = new AttachedObserver();
    task.requestAddAttachedObserver(attachedObserver);
    assertRunUntilStop("adding AttachedObserver");

    ByteBuffer memory = task.getMemory();
    ByteBuffer raw_memory = task.getRawMemory();
    long address = getFunctionEntryAddress("bp1_func");
    DwarfDie func1_die = getFunctionDie("bp1_func");
    long func1_start = func1_die.getLowPC();
    long func1_end = func1_die.getHighPC();

    // Get the original byte and byte[] for the breakpoint address and
    // whole function. Raw and logical should be similar.
    byte bp1_orig;
    memory.position(address);
    bp1_orig = memory.getByte();

    byte bp1_orig_raw;
    raw_memory.position(address);
    bp1_orig_raw = raw_memory.getByte();
    assertEquals("orig and raw", bp1_orig, bp1_orig_raw);

    byte[] func1_orig = new byte[(int) (func1_end - func1_start)];
    memory.position(func1_start);
    memory.get(func1_orig);

    byte[] func1_orig_raw = new byte[(int) (func1_end - func1_start)];
    raw_memory.position(func1_start);
    raw_memory.get(func1_orig_raw);
    assertTrue("func_orig and func_raw",
	       Arrays.equals(func1_orig, func1_orig_raw));

    // Insert breakpoint and check that (non-raw) view is the same.
    CodeObserver code = new CodeObserver(task, address);
    task.requestAddCodeObserver(code, address);
    assertRunUntilStop("add breakpoint observer");

    // Get the byte and byte[] for the breakpoint address and
    // whole function. Raw should show the breakpoint.
    byte bp1_insert;
    memory.position(address);
    bp1_insert = memory.getByte();
    assertEquals("orig and insert", bp1_orig, bp1_insert);

    byte bp1_insert_raw;
    raw_memory.position(address);
    bp1_insert_raw = raw_memory.getByte();
    assertTrue("insert and raw", bp1_insert != bp1_insert_raw);

    byte[] func1_insert = new byte[(int) (func1_end - func1_start)];
    memory.position(func1_start);
    memory.get(func1_insert);
    assertTrue("func_orig and func_insert",
	       Arrays.equals(func1_orig, func1_insert));
    
    byte[] func1_insert_raw = new byte[(int) (func1_end - func1_start)];
    raw_memory.position(func1_start);
    raw_memory.get(func1_insert_raw);
    assertFalse("func_insert and func_insert_raw",
		Arrays.equals(func1_insert, func1_insert_raw));

    // And remove it again.
    task.requestDeleteCodeObserver(code, address);
    assertRunUntilStop("remove code observer again");

    // Get the byte and byte[] for the breakpoint address and
    // whole function. Neither memory view should show the breakpoint.
    byte bp1_new;
    memory.position(address);
    bp1_new = memory.getByte();
    assertEquals("orig and new", bp1_orig, bp1_new);

    byte[] func1_new = new byte[(int) (func1_end - func1_start)];
    memory.position(func1_start);
    memory.get(func1_new);
    assertTrue("func_orig and func_new",
	       Arrays.equals(func1_orig, func1_new));

    byte bp1_new_raw;
    raw_memory.position(address);
    bp1_new_raw = raw_memory.getByte();
    assertEquals("new and raw",
		 bp1_new, bp1_new_raw);

    byte[] func1_new_raw = new byte[(int) (func1_end - func1_start)];
    raw_memory.position(func1_start);
    raw_memory.get(func1_new_raw);
    assertTrue("func_new and func_new_raw",
	       Arrays.equals(func1_new, func1_new_raw));
  }

  // Tests that breakpoint instructions are not visible in the entire
  // code text map of the program.
  public void testViewBreakpointMap() throws Exception
  {
    // Create a child.
    child = new AckDaemonProcess();
    task = child.findTaskUsingRefresh (true);
    proc = task.getProc();

    // Make sure we are attached.
    AttachedObserver attachedObserver = new AttachedObserver();
    task.requestAddAttachedObserver(attachedObserver);
    assertRunUntilStop("adding AttachedObserver");

    ByteBuffer memory = task.getMemory();
    ByteBuffer raw_memory = task.getRawMemory();

    DwarfDie func1_die = getFunctionDie("bp1_func");
    long func1_start = func1_die.getLowPC();
    long func1_end = func1_die.getHighPC();

    DwarfDie func2_die = getFunctionDie("bp2_func");
    long func2_start = func2_die.getLowPC();
    long func2_end = func2_die.getHighPC();

    long address = func1_start;
    MemoryMap map = proc.getMap(func1_start);

    int map_len = (int) (map.addressHigh - map.addressLow);

    byte[] mem_orig = new byte[map_len];
    byte[] raw_orig = new byte[map_len];
    
    memory.position(map.addressLow);
    memory.get(mem_orig);
    raw_memory.position(map.addressLow);
    raw_memory.get(raw_orig);

    assertTrue("mem_orig and raw_orig",
	       Arrays.equals(mem_orig, raw_orig));

    // Put breakpoints inside the map and at the beginning and the end
    // to test the corner cases.
    address = func1_start;
    CodeObserver code1 = new CodeObserver(task, address);
    task.requestAddCodeObserver(code1, address);
    assertRunUntilStop("add breakpoint observer func1 start");

    address = func1_end;
    CodeObserver code2 = new CodeObserver(task, address);
    task.requestAddCodeObserver(code2, address);
    assertRunUntilStop("add breakpoint observer func1 end");

    address = func2_start;
    CodeObserver code3 = new CodeObserver(task, address);
    task.requestAddCodeObserver(code3, address);
    assertRunUntilStop("add breakpoint observer func2 start");

    address = func2_end;
    CodeObserver code4 = new CodeObserver(task, address);
    task.requestAddCodeObserver(code4, address);
    assertRunUntilStop("add breakpoint observer func2 end");

    address = map.addressLow;
    CodeObserver code5 = new CodeObserver(task, address);
    task.requestAddCodeObserver(code5, address);
    assertRunUntilStop("add breakpoint observer addressLow");

    address = map.addressHigh - 1;
    CodeObserver code6 = new CodeObserver(task, address);
    task.requestAddCodeObserver(code6, address);
    assertRunUntilStop("add breakpoint observer addressHigh");

    byte[] bp_mem = new byte[map_len];
    byte[] bp_raw = new byte[map_len];
    
    memory.position(map.addressLow);
    memory.get(bp_mem);
    raw_memory.position(map.addressLow);
    raw_memory.get(bp_raw);

    assertTrue("mem_orig and bp_mem",
	       Arrays.equals(mem_orig, bp_mem));
    assertFalse("raw_orig and bp_raw",
		Arrays.equals(raw_orig, bp_raw));

    // See if only the breakpoint addresses were affected.
    bp_raw[(int) (func1_start - map.addressLow)] = memory.getByte(func1_start);
    bp_raw[(int) (func1_end - map.addressLow)] = memory.getByte(func1_end);
    bp_raw[(int) (func2_start - map.addressLow)] = memory.getByte(func2_start);
    bp_raw[(int) (func2_end - map.addressLow)] = memory.getByte(func2_end);
    bp_raw[0] = memory.getByte(map.addressLow);
    bp_raw[map_len - 1] = memory.getByte(map.addressHigh - 1);

    assertTrue("bp_mem and bp_raw",
	       Arrays.equals(bp_mem, bp_raw));

    // Remove all breakpoints and all memory should revert to be the same.
    task.requestDeleteCodeObserver(code1, code1.address);
    assertRunUntilStop("delete 1");
    task.requestDeleteCodeObserver(code2, code2.address);
    assertRunUntilStop("delete 2");
    task.requestDeleteCodeObserver(code3, code3.address);
    assertRunUntilStop("delete 3");
    task.requestDeleteCodeObserver(code4, code4.address);
    assertRunUntilStop("delete 4");
    task.requestDeleteCodeObserver(code5, code5.address);
    assertRunUntilStop("delete 5");
    task.requestDeleteCodeObserver(code6, code6.address);
    assertRunUntilStop("delete 6");

    memory.position(map.addressLow);
    memory.get(bp_mem);
    raw_memory.position(map.addressLow);
    raw_memory.get(bp_raw);

    assertTrue("deleted mem_orig and bp_mem",
               Arrays.equals(mem_orig, bp_mem));
    assertTrue("deleted bp_mem and bp_raw",
	       Arrays.equals(bp_mem, bp_raw));

  }

  // Tells the child to run the dummy () function
  // which calls bp1_func () and bp2_func ().
  static final Sig dummySig = Sig.PROF;

  /**
   * Request that that the child runs its dummy function which will
   * call the pb1 and pb1 functions. Done by sending it the
   * dummySig. To observe this event one needs to put a code observer
   * on the dummy (), bp1_func () and/or bp2_func () functions.
   */
  void requestDummyRun() throws Errno
  {
    child.signal(dummySig);
  }
  
    /**
     * Request that that given thread of the child runs its dummy
     * function which will call the pb1 and pb1 functions. Done by
     * sending it the dummySig. To observe this event one needs to put
     * a code observer on the dummy (), pb1_func () and/or pb2_func ()
     * functions.
     */
    void requestDummyRun(int tid) throws Errno {
	Signal.tkill(tid, dummySig);
    }

  /**
   * Returns the address of the requested function through
   * query the Proc Elf and DwarfDie. Asserts that the function
   * has only 1 entry point.
   */
  long getFunctionEntryAddress(String func) throws ElfException
  {
    DwarfDie die = getFunctionDie(func);
    ArrayList entryAddrs = die.getEntryBreakpoints();

    // We really expect just one entry point.
    assertEquals(entryAddrs.size(), 1);
    return ((Long) entryAddrs.get(0)).longValue();
  }

  DwarfDie getFunctionDie(String func) throws ElfException
  {
    Elf elf = new Elf(proc.getExe(), ElfCommand.ELF_C_READ);
    Dwarf dwarf = new Dwarf(elf, DwarfCommand.READ, null);
    return DwarfDie.getDecl(dwarf, func);
  }

  static class CodeObserver
    implements TaskObserver.Code
  {
    private final Task task;
    final long address;

    boolean hit;

    public CodeObserver(Task task, long address)
    {
      this.task = task;
      this.address = address;
    }

    public Action updateHit (Task task, long address)
    {
      if (! task.equals(this.task))
        throw new IllegalStateException("Wrong Task, given " + task
                                        + " not equals expected "
                                        + this.task);
      if (address != this.address)
        throw new IllegalStateException("Wrong address, given " + address
                                        + " not equals expected "
                                        + this.address);

      hit = true;

      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public void addedTo(Object o)
    {
      Manager.eventLoop.requestStop();
    }

    public void deletedFrom(Object o)
    {
      Manager.eventLoop.requestStop();
    }

    public void addFailed (Object o, Throwable w)
    {
      fail("add to " + o + " failed, because " + w);
    }
  }

  static class RemovingCodeObserver
    implements TaskObserver.Code
  {
    int hits = 0;
    int deletes = 0;

    public Action updateHit (Task task, long addr)
    {
      hits++;
      task.requestDeleteCodeObserver(this, addr);
      task.requestUnblock(this);
      return Action.BLOCK;
    }

    public void addedTo(Object o)
    {
      Manager.eventLoop.requestStop();
    }

    public void deletedFrom(Object o)
    {
      deletes++;
      Manager.eventLoop.requestStop();
    }

    public void addFailed (Object o, Throwable w)
    {
      fail("add to " + o + " failed, because " + w);
    }
  }

  static class AttachedObserver
    implements TaskObserver.Attached
  {
    public Action updateAttached(Task task)
    {
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public void addFailed(Object observable, Throwable w)
    {
      fail(w.toString());
    }

    public void addedTo(Object observable)
    {
      // Ignored
    }

    public void deletedFrom(Object observable)
    {
      // Ignored
    }
  }

  private class TerminatingObserver
    implements TaskObserver.Terminating
  {
    public boolean terminating = false;
    public Action updateTerminating (Task task, boolean signal, int value)
    {
	terminating = true;
	Manager.eventLoop.requestStop();
	return Action.CONTINUE;
    }
    public void addFailed(Object observable, Throwable w)
    {
      fail(w.getMessage());
    }
    public void addedTo(Object observable){}
    public void deletedFrom(Object observable){}
  }
}
