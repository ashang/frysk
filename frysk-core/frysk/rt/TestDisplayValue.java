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

import java.text.ParseException;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import javax.naming.NameNotFoundException;

import frysk.Config;
import frysk.debuginfo.DebugInfo;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TestLib;
import frysk.value.Value;

public class TestDisplayValue
    extends TestLib
{
  
  private Task myTask;
  private Proc myProc;
  
  public void setUp ()
  {
    super.setUp ();
  }
  
  public void tearDown ()
  {
    super.tearDown ();
  }
  
  public void testVarValueChanged()
  { 
    // Start the daemon process
    AttachedDaemonProcess process = 
      new AttachedDaemonProcess(new String[]{Config.getPkgLibDir() + "/funit-rt-varchange"});
    
    // Attach to the process
    myTask = process.getMainTask();//findTaskUsingRefresh(true);
    
    // Get the process
    myProc = myTask.getProc();
    assertNotNull("Daemon's Task", myTask);
    assertNotNull("Daemon's proc", myProc);
   
    // Set up the stepping engine, breakpoint manager, and symbol table
    SteppingEngine.setProc(myProc);
    BreakpointManager bpManager = SteppingEngine.getBreakpointManager();
    
    SteppingEngine.addObserver(new Observer()
    {
    
      public void update (Observable observable, Object arg)
      {
	// When our task is added to the stepping engine,
	// resume our testcase
	Manager.eventLoop.requestStop();
      }
    
    });
    assertRunUntilStop("Adding to Stepping Engine");
    
    /* 
     * Add the first breakpoint:
     * int main()
     *	{
     *    x = 0; <-- First breakpoint
     *    bar(x);
     *    x = 1; 
     *    bar(x);
     *    y = 2; 
     *    bar(x);
     *    x = 2;
     *    bar(x);
     */
    LineBreakpoint brk1 = 
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                 52, 0);
    brk1.addObserver(new SourceBreakpointObserver()
    {
      public void deletedFrom (Object observable) {}
      public void addedTo (Object observable) {
      }
      public void addFailed (Object observable, Throwable w) {}
    
      public void updateHit (SourceBreakpoint breakpoint, Task task, long address)
      {
	// Stop when we hit the breakpoint
	Manager.eventLoop.requestStop();
      }
    });
    bpManager.enableBreakpoint(brk1, myTask);
  
    
    // Let the process continue until we hit the breakpoint
    LinkedList list = new LinkedList();
    list.add(myTask);
    SteppingEngine.continueExecution(list);
    process.resume();
    assertRunUntilStop("First breakpoint");
    
    // Retrieve the Value we're testing, and encapsulate it in a Display
    DebugInfo info = new DebugInfo(myTask.getTid(), myProc, myTask, StackFactory.createFrame(myTask));
    info.refresh();
    Value val = null;
    try{
      val = DebugInfo.print("x");
    }
    catch (ParseException e)
    {
      e.printStackTrace();
    }
    catch (NameNotFoundException e)
    {
      e.printStackTrace();
    }
    assertNotNull("Variable not in scope", val);
    DisplayValue disp = new DisplayValue(val, myTask);
    
    // Check the value of the variable 'x', make sure it's equal to one
    Value firstVal = disp.getValue();
    assertNotNull("Value from Display", firstVal);
    assertEquals("Variable is in scope", true, disp.isInScope());
    assertEquals("Variable value at first breakpoint", 0, firstVal.getInt());
    
    /* 
     * Add the second breakpoint:
     * int main()
     *	{
     *    x = 0;
     *    bar(x);
     *    x = 1; <-- Second breakpoint
     *    bar(x);
     *    y = 2; 
     *    bar(x);
     *    x = 2;
     *    bar(x);
     */
    LineBreakpoint brk2 =
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                  54, 0);
    brk2.addObserver(new SourceBreakpointObserver()
    {
      public void deletedFrom (Object observable) {}
      public void addedTo (Object observable) {}
      public void addFailed (Object observable, Throwable w) {}
      public void updateHit (SourceBreakpoint breakpoint, Task task, long address)
      {
	Manager.eventLoop.requestStop();
      }
    });
    brk2.enableBreakpoint(myTask);
    
    // Run until we hit the second breakpoint
    list = new LinkedList();
    list.add(myTask);
    SteppingEngine.continueExecution(list);
    assertRunUntilStop("Second breakpoint");
    
    disp.update();
    Value secondVal = disp.getValue();
    assertNotNull("Value from display", secondVal);
    assertEquals("Variable is in scope", true, disp.isInScope());
    assertEquals("Value of variable at second breakpoint", 1, secondVal.getInt());
  }
  
  
  public void testVarValueNotChanged()
  {
    // Start the daemon process
    AttachedDaemonProcess process = 
      new AttachedDaemonProcess(new String[]{Config.getPkgLibDir() + "/funit-rt-varchange"});
    
    // Attach to the process
    myTask = process.getMainTask();//findTaskUsingRefresh(true);
    
    // Get the process
    myProc = myTask.getProc();
    assertNotNull("Daemon's Task", myTask);
    assertNotNull("Daemon's proc", myProc);
   
    // Set up the stepping engine, breakpoint manager, and symbol table
    SteppingEngine.setProc(myProc);
    BreakpointManager bpManager = SteppingEngine.getBreakpointManager();
    
    SteppingEngine.addObserver(new Observer()
    {
    
      public void update (Observable observable, Object arg)
      {
        // When our task is added to the stepping engine,
        // resume our testcase
        Manager.eventLoop.requestStop();
      }
    
    });
    assertRunUntilStop("Adding to Stepping Engine");
    
    /* 
     * Add the first breakpoint:
     * int main()
     *  {
     *    x = 0;
     *    bar(x);
     *    x = 1; 
     *    bar(x);
     *    y = 2; <-- First breakpoint
     *    bar(x);
     *    x = 2;
     *    bar(x);
     */
    LineBreakpoint brk1 = 
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                  54, 0);
    brk1.addObserver(new SourceBreakpointObserver()
    {
      public void deletedFrom (Object observable) {}
      public void addedTo (Object observable) {}
      public void addFailed (Object observable, Throwable w) {}
      public void updateHit (SourceBreakpoint breakpoint, Task task, long address)
      {
        // Stop when we hit the breakpoint
        Manager.eventLoop.requestStop();
      }
    });
    bpManager.enableBreakpoint(brk1, myTask);
  
    
    // Let the process continue until we hit the breakpoint
    LinkedList list = new LinkedList();
    list.add(myTask);
    SteppingEngine.continueExecution(list);
    process.resume();
    assertRunUntilStop("First breakpoint");
    
    // Retrieve the Value we're testing, and encapsulate it in a Display
    DebugInfo info = new DebugInfo(myTask.getTid(), myProc, myTask, StackFactory.createFrame(myTask));
    info.refresh();
    Value val = null;
    try{
      val = DebugInfo.print("x");
    }
    catch (ParseException e)
    {
      e.printStackTrace();
    }
    catch (NameNotFoundException e)
    {
      e.printStackTrace();
    }
    assertNotNull("Variable not in scope", val);
    DisplayValue disp = new DisplayValue(val, myTask);
    
    // Check the value of the variable 'thevar', make sure it's equal to one
    Value firstVal = disp.getValue();
    assertEquals("Value is in scope", true, disp.isInScope());
    assertEquals("Variable value at first breakpoint", firstVal.getInt(), 1);
    
    /* 
     * Add the second breakpoint:
     * int main()
     *  {
     *    x = 0;
     *    bar(x);
     *    x = 1; 
     *    bar(x);
     *    y = 2;
     *    bar(x);
     *    x = 2; <-- Second breakpoint
     *    bar(x);
     */
    LineBreakpoint brk2 =
      bpManager.addLineBreakpoint(Config.getRootSrcDir() + "frysk-core/frysk/pkglibdir/funit-rt-varchange.c",
                                  56, 0);
    brk2.addObserver(new SourceBreakpointObserver()
    {
    
      public void deletedFrom (Object observable) {}
      public void addedTo (Object observable) {}
      public void addFailed (Object observable, Throwable w) {}
      public void updateHit (SourceBreakpoint breakpoint, Task task, long address)
      {
        Manager.eventLoop.requestStop();
      }
    
    });
    brk2.enableBreakpoint(myTask);
    
    // Run until we hit the second breakpoint
    list = new LinkedList();
    list.add(myTask);
    SteppingEngine.continueExecution(list);
    assertRunUntilStop("Second breakpoint");
    
    disp.update();
    Value secondVal = disp.getValue();
    assertEquals("Value is in scope", true, disp.isInScope());
    assertEquals("Value of variable at second breakpoint", secondVal.getInt(), 1);
  }
}
