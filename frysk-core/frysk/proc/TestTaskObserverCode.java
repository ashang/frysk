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
import frysk.testbed.*;
import frysk.sys.*;
import frysk.dwfl.*;
import lib.dwfl.*;
import java.util.*;

import frysk.testbed.TestLib;

public class TestTaskObserverCode extends TestLib
{
  private Offspring child;
  private Task task;
  private Proc proc;

  public void testCode() throws Exception
  {
    // Create a child.
    child = LegacyOffspring.createDaemon();
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

  // testcase for bug #4747
  public void testCodeSignalInterrupt() throws Exception
  {
    // Create a child.
    child = LegacyOffspring.createDaemon();
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

    SignaledObserver signaled = new SignaledObserver();
    task.requestAddSignaledObserver(signaled);
    assertRunUntilStop("Add signaled observer");

    // We are at the breakpoint now, but have not yet stepped over it.
    // The dummySig is currently blocked in the funit-child.  So
    // sending another request will make it pending, but not get
    // immediately delivered (we will get notified in the signaled
    // observer though as soon as the signal gets unblocked). We
    // expect to get a notification about the PROF signal, right after
    // the step over the breakpoint without trouble and then hit the
    // breakpoint again.
    code.hit = false;
    requestDummyRun();
    task.requestUnblock(code);
    assertRunUntilStop("signal and wait for signaled observer to hit");
    assertFalse("not hit again (after second prof)", code.hit);
    assertEquals("Prof signaled", Sig.PROF_, signaled.sig);

    signaled.sig = -1;
    task.requestUnblock(signaled);
    assertRunUntilStop("wait for hit after sigprof");
    assertTrue("hit again (after second prof)", code.hit);
    assertEquals("signaled not again", -1, signaled.sig);

    // The TERM signal however isn't blocked. So making that pending
    // will immediately jump into the signal handler, bypassing the
    // step over the currently pending breakpoint. And will then kill
    // the process when delivered.
    code.hit = false;
    Signal.tkill(task.getTid(), Sig.TERM);
    task.requestUnblock(code);
    assertRunUntilStop("wait for TERM signal"); 
    assertEquals("term signaled", Sig.TERM_, signaled.sig);
    assertFalse("no hit after term", code.hit);

    TerminatingObserver terminatingObserver = new TerminatingObserver();
    task.requestAddTerminatingObserver(terminatingObserver);
    assertRunUntilStop("TerminatingObserver");

    task.requestUnblock(signaled);
    assertRunUntilStop("waiting for terminate...");
    assertFalse(code.hit);
  }

  // Testcase for bug #4889
  public void testInstallCodeDuringCode() throws Exception
  {
    // Create a child.
    child = LegacyOffspring.createDaemon();
    task = child.findTaskUsingRefresh (true);
    proc = task.getProc();

    // Make sure we are attached.
    AttachedObserver attachedObserver = new AttachedObserver();
    task.requestAddAttachedObserver(attachedObserver);
    assertRunUntilStop("adding AttachedObserver");

    long address1 = getFunctionEntryAddress("bp1_func");
    CodeObserver code1 = new CodeObserver(task, address1);
    task.requestUnblock(attachedObserver);
    task.requestAddCodeObserver(code1, address1);
    assertRunUntilStop("add breakpoint observer");

    assertFalse(code1.hit);

    // Request a run and watch the breakpoint get hit.
    requestDummyRun();
    assertRunUntilStop("signal and wait for hit");

    assertTrue(code1.hit);

    // Now install a second code observer while simultaniously
    // unblocking the task from the first code observer.
    long address2 = getFunctionEntryAddress("bp2_func");
    CodeObserver code2 = new CodeObserver(task, address2);
    code1.block = false;
    task.requestUnblock(code1);
    task.requestAddCodeObserver(code2, address2);
    assertRunUntilStop("add breakpoint observer 2");

    assertFalse(code2.hit);

  }

  public void testCodeRemovedInHit() throws Exception
  {
    // Create a child.
    child = LegacyOffspring.createDaemon();
    task = child.findTaskUsingRefresh (true);
    proc = task.getProc();

    // Make sure we are attached.
    AttachedObserver attachedObserver = new AttachedObserver();
    task.requestAddAttachedObserver(attachedObserver);
    assertRunUntilStop("adding AttachedObserver");

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
   * Returns the address of a global label by quering the the Proc
   * main Task's Dwlf.
   */
  long getGlobalLabelAddress(String label)
  {
    Dwfl dwfl = DwflCache.getDwfl(task);
    Symbol sym = Symbol.get(dwfl, label);
    return sym.getAddress();
  }

  private void breakTest(final int argc)
  {
    // Translate testname to argc (used by test to select where to jump),
    // label name to put breakpoint on, expected signal and whether or
    // not the exit will be clean.
    String testName;
    int signal;
    boolean cleanExit;
    switch (argc)
      {
      case 1:
	testName = "div_zero";
	signal = Sig.FPE_;
	cleanExit = false;
	break;
      case 2:
	testName = "bad_addr_segv";
	signal = Sig.SEGV_;
	cleanExit = false;
	break;
      case 3:
	testName = "bad_inst_ill";
	signal = Sig.ILL_;
	cleanExit = false;
	break;
      case 4:
	testName = "term_sig_hup";
	signal = Sig.HUP_;
	cleanExit = false;
	break;
      case 5:
	testName = "ign_sig_urg";
	signal = Sig.URG_;
	cleanExit = true;
	break;
      default:
	throw new RuntimeException("No such test: " + argc);
      }
    String label = testName + "_label";

    String[] command = new String[argc + 1];
    command[0] =  getExecPath("funit-raise");
    for (int i = 1; i < argc + 1; i++)
      command[i] = Integer.toString(i);

    AttachedObserver ao = new AttachedObserver();
    Manager.host.requestCreateAttachedProc("/dev/null",
                                           "/dev/null",
                                           "/dev/null", command, ao);
    assertRunUntilStop("attach then block");
    assertTrue("AttachedObserver got Task", ao.task != null);

    task = ao.task;

    long address = getGlobalLabelAddress(label);
    CodeObserver code = new CodeObserver(task, address);
    task.requestAddCodeObserver(code, address);
    assertRunUntilStop("add breakpoint observer");

    // Delete and unblock
    task.requestDeleteAttachedObserver(ao);
    assertRunUntilStop("wait for breakpoint hit");

    SignaledObserver so = new SignaledObserver();
    task.requestAddSignaledObserver(so);
    assertRunUntilStop("add signal observer");

    task.requestUnblock(code);
    assertRunUntilStop("wait for signal observer hit");
    assertEquals(signal, so.sig);

    TerminatingObserver to = new TerminatingObserver();
    task.requestAddTerminatingObserver(to);
    assertRunUntilStop("add terminating observer");

    task.requestUnblock(so);
    assertRunUntilStop("wait for terminating observer hit");
    assertEquals("killed by signal", ! cleanExit, to.signal);
    assertEquals("exit/signal value", cleanExit ? 0 : signal, to.value);

    // And let it go...
    task.requestDeleteTerminatingObserver(to);
  }

  public void testBreakDivZero()
  {
    breakTest(1);
  }

  public void testBreakIllegalAddress()
  {
    breakTest(2);
  }

  public void testBreakIllegalInstruction()
  {
    breakTest(3);
  }

  public void testBreakSignalTerminate()
  {
    breakTest(4);
  }

  public void testBreakSignalIgnore()
  {
    breakTest(5);
  }

  // Tests that breakpoint instructions are not visible to
  // normal users of task memory (only through raw memory view).
  public void testViewBreakpointMemory() throws Exception
  {
    // Create a child.
    child = LegacyOffspring.createDaemon();
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
    child = LegacyOffspring.createDaemon();
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

  // Tests whether two Tasks registered on different addresses
  // get separate update events. bug #4895
  public void testMultiTaskUpdate() throws Exception
  {
    // Create a child.
    LegacyOffspring child = LegacyOffspring.createDaemon();

    // Add a Task; wait for acknowledgement.
    child.assertSendAddCloneWaitForAcks();

    task = child.findTaskUsingRefresh (true);
    proc = task.getProc();

    Collection tasks = proc.getTasks();
    Iterator it = tasks.iterator();
    assertTrue("task one", it.hasNext());
    Task task1 = (Task) it.next();
    assertTrue("task two", it.hasNext());
    Task task2 = (Task) it.next();
    long address1 = getFunctionEntryAddress("bp1_func");
    long address2 = getFunctionEntryAddress("bp2_func");
    CodeObserver code1 = new CodeObserver(task1, address1);
    CodeObserver code2 = new CodeObserver(task2, address2);
    task1.requestAddCodeObserver(code1, address1);
    assertRunUntilStop("add breakpoint observer at address1");
    task2.requestAddCodeObserver(code2, address2);
    assertRunUntilStop("add breakpoint observer at address2");

    assertFalse(code1.hit);
    assertFalse(code2.hit);

    // Request a run and watch the breakpoints get hit.
    requestDummyRun(task1);
    assertRunUntilStop("wait for hit 1");
    assertTrue("hit 1", code1.hit);
    assertFalse("not hit 2", code2.hit);

    code1.hit = false;

    requestDummyRun(task2);
    assertRunUntilStop("wait for hit 2");
    assertFalse("not hit 1", code1.hit);
    assertTrue("hit 2", code2.hit);

    code2.hit = false;

    task1.requestDeleteCodeObserver(code1, address1);
    assertRunUntilStop("remove code observer 1");

    task2.requestDeleteCodeObserver(code2, address2);
    assertRunUntilStop("remove code observer 2");

    assertFalse("unblocked unhit 1", code1.hit);
    assertFalse("unblocked unhit 2", code2.hit);
  }

  // Tests whether two Tasks registered on same address
  // get separate update events. bug #5234
  public void testMultiTaskUpdateCalledSeveralTimes() throws Exception
  {
    if (unresolved(5234))
      return;

    // Create a child.
    LegacyOffspring child = LegacyOffspring.createDaemon();

    // Add a Task; wait for acknowledgement.
    child.assertSendAddCloneWaitForAcks();

    task = child.findTaskUsingRefresh (true);
    proc = task.getProc();

    Collection tasks = proc.getTasks();
    Iterator it = tasks.iterator();

    assertTrue("task one", it.hasNext());
    Task task1 = (Task) it.next();

    assertTrue("task two", it.hasNext());
    Task task2 = (Task) it.next();

    long address1 = getFunctionEntryAddress("bp1_func");
    long address2 = getFunctionEntryAddress("bp2_func");
    CountingCodeObserver observer = new CountingCodeObserver(new Task[]{task1, task2});
    task1.requestAddCodeObserver(observer, address1);
    assertRunUntilStop("add breakpoint observer at task1, address1");
    task1.requestAddCodeObserver(observer, address2);
    assertRunUntilStop("add breakpoint observer at task1, address2");
    task2.requestAddCodeObserver(observer, address1);
    assertRunUntilStop("add breakpoint observer at task2, address1");
    task2.requestAddCodeObserver(observer, address2);
    assertRunUntilStop("add breakpoint observer at task2, address2");

    // Request a run and watch the breakpoints get hit.  The essence
    // of the bug is that update gets called several times for a
    // single breakpoint; twice in this case.
    requestDummyRun(task1);
    assertRunUntilStop("wait for hit 1");
    assertEquals("number of hits for task1", 1, observer.hitsFor(task1));
    assertEquals("number of hits for task2", 0, observer.hitsFor(task2));

    requestDummyRun(task2);
    assertRunUntilStop("wait for hit 2");
    assertEquals("number of hits for task1", 1, observer.hitsFor(task1));
    assertEquals("number of hits for task2", 1, observer.hitsFor(task2));

    task1.requestDeleteCodeObserver(observer, address1);
    task1.requestDeleteCodeObserver(observer, address2);
    assertRunUntilStop("remove code observer from task1");

    task2.requestDeleteCodeObserver(observer, address1);
    task2.requestDeleteCodeObserver(observer, address2);
    assertRunUntilStop("remove code observer from task2");
  }

  // Same as the above, but resets the code observers by unblocking
  // before deleting, which triggers bug #5229
  public void testMultiTaskUpdateUnblockDelete() throws Exception
  {
    // XXX We cannot run this test at all since on FC6/x86_64 it
    // completely hangs the frysk-core and TestRunner.
    if (unresolved(5229))
      return;

    // Create a child.
    LegacyOffspring child = LegacyOffspring.createDaemon();

    // Add a Task; wait for acknowledgement.
    child.assertSendAddCloneWaitForAcks();

    task = child.findTaskUsingRefresh (true);
    proc = task.getProc();

    Collection tasks = proc.getTasks();
    Iterator it = tasks.iterator();
    assertTrue("task one", it.hasNext());
    Task task1 = (Task) it.next();
    assertTrue("task two", it.hasNext());
    Task task2 = (Task) it.next();
    long address1 = getFunctionEntryAddress("bp1_func");
    long address2 = getFunctionEntryAddress("bp2_func");
    CodeObserver code1 = new CodeObserver(task1, address1);
    CodeObserver code2 = new CodeObserver(task2, address2);
    task1.requestAddCodeObserver(code1, address1);
    assertRunUntilStop("add breakpoint observer at address1");
    task2.requestAddCodeObserver(code2, address2);
    assertRunUntilStop("add breakpoint observer at address2");

    assertFalse(code1.hit);
    assertFalse(code2.hit);

    // Request a run and watch the breakpoints get hit.
    requestDummyRun(task1);
    assertRunUntilStop("wait for hit 1");
    assertTrue("hit 1", code1.hit);
    assertFalse("not hit 2", code2.hit);

    // Reset 1
    code1.hit = false;
    task1.requestUnblock(code1);

    requestDummyRun(task2);
    assertRunUntilStop("wait for hit 2");
    assertFalse("not hit 1", code1.hit);
    assertTrue("hit 2", code2.hit);

    // Reset 2
    code2.hit = false;
    // XXX - FIXME - See bug #5229 why unblocking here doesn't work.
    if (! unresolved(5229))
      task2.requestUnblock(code2);

    task1.requestDeleteCodeObserver(code1, address1);
    assertRunUntilStop("remove code observer 1");

    task2.requestDeleteCodeObserver(code2, address2);
    assertRunUntilStop("remove code observer 2");

    assertFalse("unblocked unhit 1", code1.hit);
    assertFalse("unblocked unhit 2", code2.hit);
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
  
  void requestDummyRun(Task t)
  {
    Signal.tkill(t.getTid(), dummySig);
  }

  /**
   * Request that that given thread of the child runs its dummy
   * function which will call the pb1 and pb1 functions. Done by
   * sending it the dummySig. To observe this event one needs to put
   * a code observer on the dummy (), pb1_func () and/or pb2_func ()
   * functions.
   */
  void requestDummyRun(int tid) throws Errno
  {
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
    boolean block = true;

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

      if (block)
	{
	  Manager.eventLoop.requestStop();
	  return Action.BLOCK;
	}
      else
	return Action.CONTINUE;
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

  static class CountingCodeObserver
    implements TaskObserver.Code
  {
      // Map<Task, Integer>
      Map hitmap = new HashMap();

      CountingCodeObserver(Task[] tasks)
      {
	  for (int i = 0; i < tasks.length; ++i)
	      hitmap.put(tasks[i], new Integer(0));
      }

      public Action updateHit (Task task, long addr)
      {
	  hitmap.put(task, new Integer(hitsFor(task) + 1));
	  Manager.eventLoop.requestStop();
	  return Action.BLOCK;
      }

      public int hitsFor (Task task)
      {
	  return ((Integer)hitmap.get(task)).intValue();
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

  static class AttachedObserver
    implements TaskObserver.Attached
  {
    Task task;

    public Action updateAttached(Task task)
    {
      this.task = task;
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


  static class SignaledObserver
    implements TaskObserver.Signaled
  {
    int sig;

    public Action updateSignaled (Task task, int signal)
    {
      this.sig = signal;
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }

    public void addFailed(Object observable, Throwable w)
    {
      fail(w.toString());
    }

    public void addedTo(Object observable)
    {
      Manager.eventLoop.requestStop();
    }

    public void deletedFrom(Object observable)
    {
      // Ignored
    }
  }

  class TerminatingObserver
    implements TaskObserver.Terminating
  {
    Task task;
    boolean signal;
    int value;

    public Action updateTerminating (Task task, boolean signal, int value)
    {
      this.task = task;
      this.signal = signal;
      this.value = value;

      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }
    public void addFailed(Object observable, Throwable w)
    {
      fail(w.getMessage());
    }
    public void addedTo(Object observable)
    {
      Manager.eventLoop.requestStop();
    }
    public void deletedFrom(Object observable){}
  }
}
