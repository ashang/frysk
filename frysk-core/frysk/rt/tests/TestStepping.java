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
import frysk.event.Event;
import frysk.junit.Paths;

import lib.dw.Dwfl;
import lib.dw.DwflLine;

public class TestStepping extends TestLib
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
  
  
  public void RecursiveLineStepping ()
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
    
    testState = STEP_IN;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL, new String[] {
        Paths.getExecPrefix () + "/funit-rt-threadstepper",
        "" + frysk.rt.tests.TestLib.getMyPid(),
        "" + Sig.POLL_
    });

    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    //System.out.println(initial);
    
    runState = new RunState();
    runState.addObserver(lock);
    runState.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
  }
  
  
  public void setUpTest ()
  {
    Iterator i = myProc.getTasks().iterator();
    
    while (i.hasNext())
      {
        Task t = (Task) i.next();
        System.out.println("SetUpTest: " + t);
        if (this.dwflMap.get(t) == null)
          {
            Dwfl d = new Dwfl(t.getTid());
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
                continue;
              }

            this.dwflMap.put(t, d);
            this.lineMap.put(t, new Integer(line.getLineNum()));
          }
      }
    count = 0;
    
    if (testState == INSTRUCTION_STEP)
      {
        runState.stepInstruction(myProc.getTasks());
      }
    else
      runState.setUpStep(myProc.getTasks());
  }
  
  public synchronized void stepAssertions (LinkedList tasks)
  {
    Iterator i = tasks.iterator();
    while (i.hasNext())
      {
        
        Task task = (Task) i.next();

        myTask = task;
        DwflLine line = null;
        try
          {
            line = ((Dwfl) this.dwflMap.get(task)).getSourceLine(task.getIsa().pc(task));
          }
        catch (TaskException te)
          {
            // System.out.println("task execption");
            return;
          }
        catch (NullPointerException npe)
          {
            Dwfl d = new Dwfl(task.getTid());
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
              return;
          }

        if (line == null)
          return;

        int lineNum = line.getLineNum();
        int prev = ((Integer) this.lineMap.get(task)).intValue();

        System.out.println("----> " + prev + " " + lineNum + " " + count + " " + tasks.size());
        if (testState == STEP_IN)
          {
            switch (prev)
              {
                /* Thread one */
              case 90:
                assertEquals(91, lineNum);
                break;
              case 91:
                assertEquals(66, lineNum);
                break;
              case 66:
                assertEquals(67, lineNum);
                break;
              case 67:
                assertTrue(lineNum == 68 || lineNum == 73);
                break;
              case 68:
                assertEquals(69, lineNum);
                break;
              case 69:
                assertEquals(70, lineNum);
                break;
              case 70:
                assertEquals(66, lineNum);
                break;
              case 73:
                assertEquals(74, lineNum);
                break;
              case 74:
                assertEquals(75, lineNum);
                break;
              case 75:
                assertTrue(lineNum == 70 || lineNum == 91);
                break;
                  
                /* Thread two */
              case 196:
                assertEquals(197, lineNum);
                break;
              case 197:
                assertEquals(178, lineNum);
                break;
              case 179:
                assertEquals(180, lineNum);
                break;
              case 180:
                assertTrue(lineNum == 181 || lineNum == 183);
                break;
              case 181:
                assertEquals(180, lineNum);
                break;
              case 183:
                assertEquals(164, lineNum);
                break;
              case 164:
                assertEquals(165, lineNum);
                break;
              case 165:
                assertEquals(144, lineNum);
                break;
              case 144:
                assertEquals(145, lineNum);
                break;
              case 145:
                assertTrue(lineNum == 146 || lineNum == 148);
                break;
              case 148:
                assertEquals(150, lineNum);
                break;
              case 150:
                assertEquals(151, lineNum);
                break;
              case 151:
                assertEquals(122, lineNum);
                break;
              case 122:
                assertEquals(123, lineNum);
                break;
              case 123:
                assertTrue(lineNum == 124 || lineNum == 126);
                break;
              case 124:
                assertEquals(126, lineNum);
                break;
              case 126:
                assertTrue(lineNum == 127 || lineNum == 133);
                break;
              case 127:
                assertEquals(128, lineNum);
                break;
              case 128:
                assertEquals(129, lineNum);
                break;
              case 129:
                assertEquals(130, lineNum);
                break;
              case 130:
                assertTrue(lineNum == 136 || lineNum == 151);
                break;
              case 133:
                assertEquals(134, lineNum);
                break;
              case 134:
                assertEquals(135, lineNum);
                break;
              case 135:
                assertEquals(136, lineNum);
                break;
              case 136:
                assertEquals(122, lineNum);
                break;
                
                /* Main thread */
              case 100:
                assertEquals(101, lineNum);
                break;
              case 101:
                assertEquals(103, lineNum);
                break;
              case 103:
                assertTrue(lineNum == 104 || lineNum == 106);
                break;
              case 104:
                assertEquals(104, lineNum);
                break;
              case 106:
                assertEquals(112, lineNum);
                break;
              case 112:
                assertEquals(113, lineNum);
                break;
              case 113:
                assertEquals(114, lineNum);
                break;
              case 114:
                assertEquals(100, lineNum);
              default:
                break;
              }
          }
        this.lineMap.put(task, new Integer(line.getLineNum()));
      }
    
    count++;

    runState.stepCompleted();

    if (count != 50)
      {
        runState.setUpStep(tasks);
      }
    else
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
      myTask = (Task) arg;
      Manager.eventLoop.add(new Event()
      {
        public void execute ()
        {System.out.println(">> " + myTask);
          if (initial == true)
            {
              initial = false;
              setUpTest();
              return;
            }
          
          stepAssertions(myProc.getTasks());
        }
      });
    }
    
  }
  

}
