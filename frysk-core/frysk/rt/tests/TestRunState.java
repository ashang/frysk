// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

package frysk.rt.tests;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import frysk.proc.MachineType;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.proc.TestLib;
import frysk.rt.RunState;
import frysk.sys.Sig;
import frysk.junit.Paths;

import lib.dw.Dwfl;
import lib.dw.DwflLine;

public class TestRunState extends TestLib
{
  
  private Task myTask;
  private Proc myProc;
  
  private int testState = 0;
  
  private RunState runState;
  
  //private StackFrame frame;
  
  private HashMap dwflMap;

  private HashMap lineMap;
  
  private boolean initial;
  
  private int count = 0;
  
  protected static final int INSTRUCTION_STEP = 0;
  protected static final int STEP_IN = 1;
  protected static final int STEP_OVER = 2;
  protected static final int STEP_OUT = 3;
  
  private LockObserver lock;
  
  
  public void testInstructionStepping ()
  {
    if (MachineType.getMachineType() == MachineType.PPC
        || MachineType.getMachineType() == MachineType.PPC64)
      {
        brokenXXX(3277);
        return;
      }
    
    initial = true;
    this.dwflMap = new HashMap();
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL, new String[] {
        Paths.getExecPrefix () + "/funit-rt-stepper",
        "" + frysk.rt.tests.TestLib.getMyPid(),
        "" + Sig.POLL_
    });
    
    testState = INSTRUCTION_STEP;
    
    Manager.host.requestRefreshXXX(true);
    Manager.eventLoop.runPending();
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    //System.out.println(initial);
    
    runState = new RunState();
    runState.addObserver(lock);
    runState.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
  }
  
  public void testLineStepping ()
  {
    if (MachineType.getMachineType() == MachineType.PPC
        || MachineType.getMachineType() == MachineType.PPC64)
      {
        brokenXXX(3277);
        return;
      }

    initial = true;
    this.dwflMap = new HashMap();
    this.lineMap = new HashMap();
    
    runState = new RunState();
    lock = new LockObserver();
    runState.addObserver(lock);
    testState = STEP_IN;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL, new String[] {
        Paths.getExecPrefix () + "/funit-rt-stepper",
        "" + frysk.rt.tests.TestLib.getMyPid(),
        "" + Sig.POLL_
    });
    
    Manager.host.requestRefreshXXX(true);
    Manager.eventLoop.runPending();
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    runState.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
  }
  
  
  public void setUpTest ()
  {
    Iterator i = myProc.getTasks().iterator();
    
    while (i.hasNext())
      {
        Task t = (Task) i.next();
        if (this.dwflMap.get(t) == null)
          {
            Dwfl d = new Dwfl(t.getTid());
           //System.out.println("setupTest " + t + " " + d);
            DwflLine line = null;
            try
              {
                line = d.getSourceLine(t.getIsa().pc(t));
              }
            catch (TaskException te)
              {
                continue;
              }
            
            if (line == null)
              {
               //System.out.println("DwflLine null");
                continue;
              }
            else
             //System.out.println("setupTest " + line.getLineNum());

            this.dwflMap.put(t, d);
            this.lineMap.put(t, new Integer(line.getLineNum()));
          }
        
        //stepper.requestUnblock(t);
        //t.requestUnblock(this.stepper);
      }
    count = 0;
    
   //System.out.println("About to step from testsetup: " + myProc.getMainTask());
    if (testState == INSTRUCTION_STEP)
      {
        runState.stepInstruction(myProc.getTasks());
      }
    else
      runState.setUpStep(myProc.getTasks());
  }
  
  public synchronized void stepAssertions (Task task)
  { 
   //System.out.println("In stepAssertions " + task);
    myTask = task;
    DwflLine line = null;
    try
      {
        line = ((Dwfl) this.dwflMap.get(myTask)).getSourceLine(myTask.getIsa().pc(myTask));
      }
    catch (TaskException te)
      {
       //System.out.println("task execption");
        return;
      }
    catch (NullPointerException npe)
      {
       //System.out.println("NPE {");
        Dwfl d = new Dwfl(task.getTid());
       //System.out.println("setupTest " + task + " " + d);
        line = null;
        try
        {
          line = d.getSourceLine(task.getIsa().pc(task));
        }
      catch (TaskException te)
        {
          
        }
      if (line != null)
        {
          this.dwflMap.put(task, d);
          this.lineMap.put(task, new Integer(line.getLineNum()));
        }
      else
       //System.out.println("Second attempt - line still null");
      
     //System.out.println("}");
        return;
      }

    if (line == null)
      return;

   //System.out.println("Nothing is null");
    int lineNum = line.getLineNum();
    int prev = ((Integer) this.lineMap.get(myTask)).intValue();

    if (testState == INSTRUCTION_STEP)
      {
       //System.out.println("------> (instruction) About to assert " + line + " " + prev + " " + lineNum);
        switch (prev)
          {
          case 56:
            assertTrue(lineNum == 56);
            break;
          case 78:
            assertTrue(lineNum == 78 || lineNum == 79);
            break;
          case 79:
            assertTrue(lineNum == 79 || lineNum == 80);
            break;
          case 80:
            assertTrue(lineNum == 80 || lineNum == 81);
            break;
          case 81:
            assertTrue(lineNum == 81 || lineNum == 82);
            break;
          case 82:
            assertTrue(lineNum == 82 || lineNum == 84);
            break;
          case 84:
            assertTrue(lineNum == 84 || lineNum == 86);
            break;
          case 86:
            assertTrue(lineNum == 86 || lineNum == 87);
            break;
          case 87:
            assertTrue(lineNum == 87 || lineNum == 88);
            break;
          case 88:
            assertTrue(lineNum == 88 || lineNum == 89);
            break;
          case 89:
            assertTrue(lineNum == 89 || lineNum == 90);
            break;
          case 90:
            assertTrue(lineNum == 90 || lineNum == 91);
            break;
          // case 94:
          // assertTrue(lineNum == 94 || lineNum == 60);
          // break;
          case 60:
            assertTrue(lineNum == 60 || lineNum == 61);
            break;
          case 61:
            assertTrue(lineNum == 61 || lineNum == 62);
            break;
          case 62:
            assertTrue(lineNum == 62 || lineNum == 63);
            break;
          case 63:
            assertTrue(lineNum == 63 || lineNum == 64);
            break;
          case 64:
            assertTrue(lineNum == 64 || lineNum == 65);
            break;
          case 65:
            assertTrue(lineNum == 65 || lineNum == 67);
            break;
          case 67:
            assertTrue(lineNum == 67 || lineNum == 94);
            break;
          //        case 94:
          //          assertTrue(lineNum == 94 || lineNum == 95);
          //          break;
          default:
            break;
          }
        count++;
        
        runState.stepCompleted();
        
        if (count != 50)
          {
            this.lineMap.put(task, new Integer(lineNum));
            LinkedList l = new LinkedList();
            l.add(task);
            runState.stepInstruction(l);
          }
      }
    else if (testState == STEP_IN)
      {
       //System.out.println("------> (stepin) About to assert " + line + " " + prev + " " + lineNum);
        
        switch (prev)
          {
          case 56:
            assertEquals(lineNum, 56);
            break;
          case 78:
            assertEquals(lineNum, 79);
            break;
          case 79:
            assertEquals(lineNum, 80);
            break;
          case 80:
            assertEquals(lineNum, 81);
            break;
          case 81:
            assertEquals(lineNum, 82);
            break;
          case 82:
            assertEquals(lineNum, 84);
            break;
          case 84:
            assertEquals(lineNum, 86);
            break;
          case 86:
            assertEquals(lineNum, 87);
            break;
          case 87:
            assertEquals(lineNum, 88);
            break;
          case 88:
            assertEquals(lineNum, 89);
            break;
          case 89:
            assertEquals(lineNum, 90);
            break;
          case 90:
            assertEquals(lineNum, 91);
            break;
          case 91:
            assertEquals(lineNum, 94);
            break;
//          case 94:
//            assertEquals(lineNum, 60);
//            break;
          case 95:
            assertEquals(lineNum, 78);
            break;
          case 60:
            assertEquals(lineNum, 61);
            break;
          case 61:
            assertEquals(lineNum, 62);
            break;
          case 62:
            assertEquals(lineNum, 63);
            break;
          case 63:
            assertEquals(lineNum, 64);
            break;
          case 64:
            assertEquals(lineNum, 65);
            break;
          case 65:
            assertEquals(lineNum, 67);
            break;
//          case 67:
//            assertEquals(lineNum, 95); // put this back in when breakage is figured out.
//            break;
          default:
            break;
          }
        count++;
        
        runState.stepCompleted();
        
        if (count != 50)
          {
            this.lineMap.put(task, new Integer(line.getLineNum()));
            LinkedList tasks = new LinkedList();
            tasks.add(task);
            runState.setUpStep(tasks);
          }
      }
    if (count == 50)
      {
        Manager.eventLoop.requestStop();
        return;
      }
  }
  
  class LockObserver implements Observer
  {
    
    /**
     * Builtin Observer method - called whenever the Observable we're concerned
     * with - in this case the RunState - has changed.
     * 
     * @param o The Observable we're watching
     * @param arg An Object argument
     */
    public synchronized void update (Observable o, Object arg)
    {
      if (arg == null)
        return;
      
      if (initial == true)
        {
          initial = false;
          setUpTest();
          return;
        }

      stepAssertions(myProc.getMainTask());
    }
    
  }
  
}
