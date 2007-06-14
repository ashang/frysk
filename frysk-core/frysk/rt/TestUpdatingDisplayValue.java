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
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
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
  
  public void testUpdateTaskStopped()
  {
    BreakpointManager bpManager = createDaemon();
    
    /*
     * First breakpoint
     */
    LineBreakpoint brk1 = 
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                 49, 0);
    brk1.addObserver(new BreakpointBlocker());
    bpManager.enableBreakpoint(brk1, myTask);
    
    LinkedList list = new LinkedList();
    list.add(myTask);
    steppingEngine.continueExecution(list);
    process.resume();
    assertRunUntilStop("First breakpoint");
    
    UpdatingDisplayValue uDisp = new UpdatingDisplayValue("x", myTask,
                                                          StackFactory.createFrame(myTask).getFrameIdentifier(),
                                                          steppingEngine);
    DisplayObserver obs = new DisplayObserver();
    uDisp.addObserver(obs);
    
    /*
     * Second breakpoint
     */
    LineBreakpoint brk2 =
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                  51, 0);
    brk2.addObserver(new BreakpointBlocker());
    brk2.enableBreakpoint(myTask, steppingEngine);
    
    // Run until we hit the second breakpoint
    list = new LinkedList();
    list.add(myTask);
    steppingEngine.continueExecution(list);
    assertRunUntilStop("Second breakpoint");
    
    assertTrue("Observer was notified of a stop", obs.hitStopped);
  }
  
  public void testUpdateUnavailableFuncReturn()
  {
    if(brokenXXX(4639))
      return;
  }
  
  public void testUpdateUnavailableExceptionThrown()
  {
    if(brokenXXX(4639))
      return;
  }
  
  public void testUpdateUnavailableLongJump()
  {
    if(brokenXXX(4639))
      return;
  }
  
  public void testUpdateUnavailableTaskDead()
  {
    if(brokenXXX(4639))
      return;
  }
  
  private BreakpointManager createDaemon()
  {
    //  Start the daemon process
    process = 
      new AttachedDaemonProcess(new String[]{Config.getPkgLibDir() + "/funit-rt-varchange"});
    
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
  
  private class DisplayObserver implements DisplayValueObserver
  {
    boolean hitStopped;
    boolean hitResumed;
    
    DisplayObserver()
    {
      hitStopped = false;
      hitResumed = false;
    }
    
    public void updateAvailableTaskStopped (DisplayValue value)
    {
      assertNotNull("DisplayValue passed to the observer", value);
      assertFalse("Task should have blockers", myTask.getBlockers().length == 0);
      hitStopped = true;
    }

    public void updateUnavailbeResumedExecution (DisplayValue value)
    {
      assertNotNull("DisplayValue passed to the observer", value);
      assertTrue("Task should not be blocked", myTask.getBlockers().length == 0);
      hitResumed = true;
    }
    
  }
}
