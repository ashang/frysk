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

package frysk.rt;

import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import frysk.Config;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.proc.TestLib;
import frysk.stack.StackFactory;

public class TestUpdatingDisplayValue extends TestLib
{
  private AttachedDaemonProcess process;
  private Task myTask;
  private Proc myProc;
  private SteppingEngine steppingEngine;
  
  public void setUp()
  {
    super.setUp();
  }
  
  public void tearDown()
  {
    super.tearDown();
  }
  
  /*
   * Create a display of 'x', then resume/stop the process and see that we get
   * notification of the stop. The Value of x should not change
   */
  public void testTaskStopped()
  {
    BreakpointManager bpManager = createDaemon("funit-rt-varchange");
    
    /*
     * First breakpoint
     */
    LineBreakpoint brk1 = 
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                 51, 0);
    brk1.addObserver(new BreakpointBlocker());
    bpManager.enableBreakpoint(brk1, myTask);
    
    LinkedList list = new LinkedList();
    list.add(myTask);
    steppingEngine.continueExecution(list);
    process.resume();
    assertRunUntilStop("First breakpoint");
    
    UpdatingDisplayValue uDisp = DisplayManager.createDisplay(myTask,
	    StackFactory.createFrame(myTask).getFrameIdentifier(), steppingEngine,
	    "x"); 

    DisplayObserver obs = new DisplayObserver();
    uDisp.addObserver(obs);
    
    /*
     * Second breakpoint
     */
    LineBreakpoint brk2 =
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                  52, 0);
    brk2.addObserver(new BreakpointBlocker());
    brk2.enableBreakpoint(myTask, steppingEngine);
    
    // Run until we hit the second breakpoint
    list = new LinkedList();
    list.add(myTask);
    steppingEngine.continueExecution(list);
    assertRunUntilStop("Second breakpoint");
    
    
    assertTrue("Observer was not notified of a stop", obs.hitStopped);
    assertFalse("Observer was notified of a variable change", obs.hitChanged);
  }
  
  /*
   * Create a display of 'x', then resume/stop the program in the place where
   * the value of x has changed. We should recieve notification of the event
   */
  public void testValueChanged()
  {
    BreakpointManager bpManager = createDaemon("funit-rt-varchange");
    
    /*
     * First breakpoint
     */
    LineBreakpoint brk1 = 
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                 48, 0);
    brk1.addObserver(new BreakpointBlocker());
    bpManager.enableBreakpoint(brk1, myTask);
    
    LinkedList list = new LinkedList();
    list.add(myTask);
    steppingEngine.continueExecution(list);
    process.resume();
    assertRunUntilStop("First breakpoint");
    
    UpdatingDisplayValue uDisp = DisplayManager.createDisplay(
	    myTask, StackFactory.createFrame(myTask).getFrameIdentifier(),
	    steppingEngine, "x");
    
    DisplayObserver obs = new DisplayObserver();
    uDisp.addObserver(obs);
    int oldValue = uDisp.getValue().getInt();
    
    /*
     * Second breakpoint
     */
    LineBreakpoint brk2 =
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                  52, 0);
    brk2.addObserver(new BreakpointBlocker());
    brk2.enableBreakpoint(myTask, steppingEngine);
    bpManager.enableBreakpoint(brk2, myTask);
    
    // Run until we hit the second breakpoint
    list = new LinkedList();
    list.add(myTask);
    steppingEngine.continueExecution(list);
    assertRunUntilStop("Second breakpoint");
    
    assertTrue("Value did not change", oldValue != uDisp.getValue().getInt());
    assertTrue("Observer was not notified of a value change", obs.hitChanged);
  }
  
  /*
   * Create a display on 'x', then check to make sure we recieve
   * notification when 'x' goes out of scope do to a function returning
   * normally
   */
  public void testFuncReturn()
  {
    BreakpointManager bpManager = createDaemon("funit-rt-varchange");
    
    /*
     * First breakpoint
     */
    LineBreakpoint brk1 = 
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                 63, 0);
    brk1.addObserver(new BreakpointBlocker());
    bpManager.enableBreakpoint(brk1, myTask);
    
    LinkedList list = new LinkedList();
    list.add(myTask);
    steppingEngine.continueExecution(list);
    process.resume();
    assertRunUntilStop("First breakpoint");
    
    UpdatingDisplayValue uDisp = DisplayManager.createDisplay(
	    myTask, StackFactory.createFrame(myTask).getFrameIdentifier(),
	    steppingEngine, "x");
    
    DisplayObserver obs = new DisplayObserver();
    uDisp.addObserver(obs);
    assertTrue("Display is not available", uDisp.isAvailable());
    
    /*
     * Second breakpoint
     */
    LineBreakpoint brk2 =
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                  49, 0);
    brk2.addObserver(new BreakpointBlocker());
    brk2.enableBreakpoint(myTask, steppingEngine);
    
    // Run until we hit the second breakpoint
    list = new LinkedList();
    list.add(myTask);
    steppingEngine.continueExecution(list);
    assertRunUntilStop("Second breakpoint");
    
    assertTrue("Observer was not notified", obs.hitOutOfScope);
    assertFalse("Display is available", uDisp.isAvailable());
  }
  
  public void testExceptionThrown()
  {
    if(brokenXXX(4639))
      return;
  }
  
  /*
   * Create a display on 'y', then make sure we receive an event when 'y' and
   * 'y's scope disappears due to a call to siglongjmp.
   */
  public void testLongjmp()
  {
    BreakpointManager bpManager = createDaemon("funit-rt-varlongjmp");
    
    /*
     * First breakpoint
     */
    LineBreakpoint brk1 = 
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varlongjmp.c",
                                 61, 0);
    brk1.addObserver(new BreakpointBlocker());
    bpManager.enableBreakpoint(brk1, myTask);
    
    LinkedList list = new LinkedList();
    list.add(myTask);
    steppingEngine.continueExecution(list);
    process.resume();
    assertRunUntilStop("First breakpoint");
    
    UpdatingDisplayValue uDisp = DisplayManager.createDisplay(
	    myTask, StackFactory.createFrame(myTask).getFrameIdentifier(), 
	    steppingEngine, "y");
    
    DisplayObserver obs = new DisplayObserver();
    uDisp.addObserver(obs);
    assertTrue("Display is not available", uDisp.isAvailable());
    
    /*
     * Second breakpoint
     */
    LineBreakpoint brk2 =
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varlongjmp.c",
                                  51, 0);
    brk2.addObserver(new BreakpointBlocker());
    brk2.enableBreakpoint(myTask, steppingEngine);
    
    // Run until we hit the second breakpoint
    list = new LinkedList();
    list.add(myTask);
    steppingEngine.continueExecution(list);
    assertRunUntilStop("Second breakpoint");
    
    assertTrue("Observer was not notified", obs.hitOutOfScope);
    assertFalse("Display is available", uDisp.isAvailable());
  }
  
  /*
   * Create a watch on a variable and make sure that we receive an event
   * when the task disappears.
   */
  public void testTaskDead()
  {
      BreakpointManager bpManager = createDaemon("funit-rt-varsegv");
      
      myTask.requestAddTerminatedObserver(new TaskObserver.Terminated() {
        public void deletedFrom(Object observable) {}
        public void addedTo(Object observable) {}
        public void addFailed(Object observable, Throwable w) {}
    
        /* When the task dies, stop the event loop */
        public Action updateTerminated(Task task, boolean signal, int value) {
            	Manager.eventLoop.requestStop();
    		return Action.CONTINUE;
        }
    
    });
      
      /*
       * First breakpoint
       */
      LineBreakpoint brk1 = 
        bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varsegv.c",
                                   52, 0);
      brk1.addObserver(new BreakpointBlocker());
      bpManager.enableBreakpoint(brk1, myTask);
      
      LinkedList list = new LinkedList();
      list.add(myTask);
      steppingEngine.continueExecution(list);
      process.resume();
      assertRunUntilStop("First breakpoint");
      
      UpdatingDisplayValue uDisp = DisplayManager.createDisplay(
	      myTask, StackFactory.createFrame(myTask).getFrameIdentifier(), 
	      steppingEngine, "y");
      
      DisplayObserver obs = new DisplayObserver();
      uDisp.addObserver(obs);
      assertTrue("Display is not available", uDisp.isAvailable());
      
      // Run until we hit the second breakpoint
      list = new LinkedList();
      list.add(myTask);
      steppingEngine.continueExecution(list);
      assertRunUntilStop("Second breakpoint");
      
      assertTrue("Observer was not notified", obs.hitOutOfScope);
      assertFalse("Display is available", uDisp.isAvailable());
  }
  
  public void testDisabled()
  {
      BreakpointManager bpManager = createDaemon("funit-rt-varchange");
      
      /*
       * First breakpoint
       */
      LineBreakpoint brk1 = 
        bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                   48, 0);
      brk1.addObserver(new BreakpointBlocker());
      bpManager.enableBreakpoint(brk1, myTask);
      
      LinkedList list = new LinkedList();
      list.add(myTask);
      steppingEngine.continueExecution(list);
      process.resume();
      assertRunUntilStop("First breakpoint");
      
      UpdatingDisplayValue uDisp = DisplayManager.createDisplay(
  	    myTask, StackFactory.createFrame(myTask).getFrameIdentifier(),
  	    steppingEngine, "x");
      
      DisplayObserver obs = new DisplayObserver();
      uDisp.addObserver(obs);
      // This should prevent us from seeing any events
      uDisp.disable();
      
      /*
       * Second breakpoint
       */
      LineBreakpoint brk2 =
        bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                    52, 0);
      brk2.addObserver(new BreakpointBlocker());
      bpManager.enableBreakpoint(brk2, myTask);
      
      // Run until we hit the second breakpoint
      list = new LinkedList();
      list.add(myTask);
      steppingEngine.continueExecution(list);
      assertRunUntilStop("Second breakpoint");
      
      assertTrue("Observer was notified of a value change", !obs.hitChanged);
  }
  
  private BreakpointManager createDaemon(String program)
  {
    //  Start the daemon process
    process = 
      new AttachedDaemonProcess(new String[]{Config.getPkgLibDir() + "/" + program});
    
    myTask = process.getMainTask();
    myProc = myTask.getProc();
    assertNotNull("Daemon's Task", myTask);
    assertNotNull("Daemon's proc", myProc);
   
    // Set up the stepping engine, breakpoint manager, and symbol table
    Proc[] p = new Proc[1];
    p[0] = myProc;
    steppingEngine = new SteppingEngine(p, new Observer()
    {
      public void update (Observable observable, Object arg)
      {
        Manager.eventLoop.requestStop();
      }
    });
    BreakpointManager bpManager = steppingEngine.getBreakpointManager();
    assertRunUntilStop("Adding to Stepping Engine");
    return bpManager;
  }
  
  /*
   * Observer that asks the event loop to stop when a breakpoint is hit
   */
  private class BreakpointBlocker implements SourceBreakpointObserver
  {
    public void updateHit (SourceBreakpoint breakpoint, Task task, long address)
    {
      Manager.eventLoop.requestStop();
    }
    public void addFailed (Object observable, Throwable w){}
    public void addedTo (Object observable){}
    public void deletedFrom (Object observable){}
  }
  
  /*
   * A simple observer that tracks when certain events have been fired
   * and performs some simple assertions
   */
  private class DisplayObserver implements DisplayValueObserver
  {
    boolean hitStopped;
    boolean hitResumed;
    boolean hitChanged;
    boolean hitOutOfScope;
    
    DisplayObserver()
    {
      hitStopped = false;
      hitResumed = false;
      hitChanged = false;
      hitOutOfScope = false;
    }
    
    public void updateAvailableTaskStopped (UpdatingDisplayValue value)
    {
      assertNotNull("DisplayValue passed to the observer", value);
      assertFalse("Task should have blockers", myTask.getBlockers().length == 0);
      hitStopped = true;
    }

    public void updateUnavailbeResumedExecution (UpdatingDisplayValue value)
    {
      assertNotNull("DisplayValue passed to the observer", value);
      assertTrue("Task should not be blocked", myTask.getBlockers().length == 0);
      hitResumed = true;
    }

    public void updateValueChanged (UpdatingDisplayValue value)
    {
      assertNotNull("DisplayValue passed to the observer", value);
      hitChanged = true;
    }

    public void updateUnavailableOutOfScope (UpdatingDisplayValue value)
    {
      assertNotNull("DisplayValue passed to the observer", value);
      hitOutOfScope = true;
    }
    
  }
}
