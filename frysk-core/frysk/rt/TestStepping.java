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


package frysk.rt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

//import frysk.proc.Action;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.TaskObserver;
//import frysk.proc.SyscallEventInfo;
import frysk.proc.Task;
//import frysk.proc.TaskObserver;
import frysk.proc.TestLib;
import frysk.sys.Sig;
import frysk.sys.Pid;
import frysk.event.Event;

public class TestStepping extends TestLib
{
  
  private Task myTask;
  private Task myAsmTask;
  private Proc myProc;
  
  private int testState = 0;
  
  private RunState runState;

  private HashMap lineMap;
  
  private boolean initial;
  
  private int count = 0;
  
  protected static final int INSTRUCTION_STEP = 0;
  protected static final int STEP_IN = 1;
  protected static final int STEP_OVER = 2;
  protected static final int STEP_OUT = 3;
  protected static final int ASM_INSTRUCTION_STEP = 4;
  protected static final int ASM_STEP_IN = 5;
  
  private LockObserver lock;
  
  private AttachedObserver attachedObserver;
  
  
  public void testRecursiveLineStepping ()
  {

      if (brokenPpcXXX (3277))
	  return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    runState = new RunState();
    runState.addObserver(lock);
    
    testState = STEP_IN;
    
    AckDaemonProcess process = new AckDaemonProcess
	(Sig.POLL,
	 new String[] {
	    getExecPath ("funit-rt-threadstepper"),
	    "" + Pid.get (),
	    "" + Sig.POLL_
	});
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    runState.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
  }
  
  public void testASMStepping ()
  {
    
    if (brokenPpcXXX (3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = ASM_INSTRUCTION_STEP;
    
    runState = new RunState();
    runState.addObserver(lock);
    
    String[] cmd = new String[1];
    cmd[0] = getExecPath ("funit-rt-asmstepper");
    
    attachedObserver = new AttachedObserver();
    Manager.host.requestCreateAttachedProc(cmd, attachedObserver);
    
    assertRunUntilStop("Attempting to add instructionObserver");
  }
  
  
  public void setUpTest ()
  {
    Iterator i = myProc.getTasks().iterator();

    while (i.hasNext())
      {
        Task t = (Task) i.next();
        StackFrame frame = StackFactory.createStackFrame(t, 1);

        if (frame.getLines().length == 0)
          {
            this.lineMap.put(t, new Integer(0));
            continue;
          }

        this.lineMap.put(t, new Integer(frame.getLines()[0].getLine()));
      }
    
    count = 0;

    if (testState == INSTRUCTION_STEP)
      {
        runState.stepInstruction(myProc.getTasks());
      }
    else if (testState == (STEP_IN))
      {
        runState.setUpLineStep(myProc.getTasks());
      }
    else if (testState == ASM_INSTRUCTION_STEP)
      {
        runState.stepInstruction(myProc.getTasks());
      }
  }
  
  public synchronized void stepAssertions (LinkedList tasks)
  {
//   System.out.println("Test.stepAssertions " + tasks.size());
    Iterator i = tasks.iterator();
    while (i.hasNext())
      {
        Task task = (Task) i.next();
        myTask = task;
        StackFrame frame = StackFactory.createStackFrame(task, 1);
        int lineNum;
        
        if (frame.getLines().length == 0)
          {
//           System.out.println("line null - assigning 0");
            lineNum = 0;
          }
        else
          {
            lineNum = frame.getLines()[0].getLine();
          }
        
        int prev = ((Integer) this.lineMap.get(task)).intValue();
       
//        System.out.println("About to assert " + prev + " " + lineNum);
       if (lineNum == 244 || lineNum == 0)
         {
           continue;
         }
        if (testState == STEP_IN)
          {
            switch (prev)
              {
                case 0:
                  break;
                
                /* Thread one */
              case 90:
                assertEquals(91, lineNum);
                break;
              case 91:
                assertEquals(66, lineNum);
                break;
              case 66:
                assertTrue(lineNum == 66 || lineNum == 67);
                break;
              case 67:
                assertTrue(lineNum == 69 || lineNum == 74);
                break;
              case 68:
                assertEquals(69, lineNum);
                break;
              case 69:
                assertEquals(70, lineNum);
                break;
              case 70:
                assertTrue(lineNum == 66 || lineNum == 67 || lineNum == 77 || lineNum == 92);
                break;
              case 73:
                assertEquals(74, lineNum);
                break;
              case 74:
                assertTrue(lineNum == 70 || lineNum == 91 || lineNum == 77);
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
                assertTrue(lineNum == 122 || lineNum == 123);
                break;
              case 122:
                assertTrue(lineNum == 122 || lineNum == 123);
                break;
              case 123:
                assertTrue(lineNum == 124 || lineNum == 126);
                break;
              case 124:
                assertEquals(126, lineNum);
                break;
              case 126:
                assertTrue(lineNum == 128 || lineNum == 134);
                break;
              case 127:
                assertEquals(128, lineNum);
                break;
              case 128:
                assertEquals(129, lineNum);
                break;
              case 129:
                assertTrue(lineNum == 136 || lineNum == 138 || lineNum == 151);
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
                assertTrue(lineNum == 123 || lineNum == 122 || lineNum == 151 || lineNum == 136);
                break;
                
                /* Main thread */
              case 100:
                assertEquals(101, lineNum);
                break;
              case 101:
                assertEquals(103, lineNum);
                break;
              case 103:
                assertTrue(lineNum == 104 || lineNum == 106 || lineNum == 103);
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
        else if (testState == ASM_INSTRUCTION_STEP)
          {
            switch (prev)
            {
              case 0:
                break;
              
              case 53:
                assertEquals(54, lineNum);
                break;
              case 54:
                assertEquals(55, lineNum);
                break;
              case 55:
                assertEquals(56, lineNum);
                break;
              case 56:
                assertTrue(lineNum == 56 || lineNum == 57);
                break;
              case 57:
                assertEquals(58, lineNum);
                break;
              case 58:
                assertTrue(lineNum == 58 || lineNum == 59);
                break;
              case 59:
                assertEquals(60, lineNum);
                break;
              case 60:
                assertEquals(61, lineNum);
                break;
              case 61:
                assertEquals(62, lineNum);
                break;
              case 62:
                assertEquals(63, lineNum);
                break;
              case 63:
                assertEquals(64, lineNum);
                break;
              case 64:
                assertEquals(65, lineNum);
                break;
              case 65:
                assertEquals(66, lineNum);
                break;
              case 66:
                assertEquals(53, lineNum);
                break;
                
                default:
                  break;
            }
          }
        this.lineMap.put(task, new Integer(lineNum));
      }
    //System.out.println("After assertions");
    
    count++;

    runState.stepCompleted();

    if (count != 40)
      {
        if (testState == STEP_IN)
          runState.setUpLineStep(tasks);
        else if (testState == ASM_INSTRUCTION_STEP)
          runState.stepInstruction(tasks);
      }
    else
      {
        Manager.eventLoop.requestStop();
        return;
      }
  }
  
  protected class AttachedObserver implements TaskObserver.Attached
  {
    public void addedTo (Object o)
    {
      
    }
    
    public Action updateAttached (Task task)
    {
      myAsmTask = task;
      myProc = task.getProc();
      runState.setProc(myProc);
      return Action.CONTINUE;
    }
    
    public void addFailed  (Object observable, Throwable w)
    {
      
    }
    
    public void deletedFrom (Object o)
    {
      
    }
  }
  
  class LockObserver implements Observer
  {
    
    /**
     * Builtin Observer method - called whenever the Observable we're concerned
     * with - in this case the RunState - has changed.
     * 
     * @param o The Observable we're watching
     * @param arg An Object argument, usually a Task when important
     */
    public synchronized void update (Observable o, Object arg)
    {//System.out.println("LockObserver.update " + arg);
      if (arg == null)// && testState != ASM_INSTRUCTION_STEP)
        return;
      
      if (testState != ASM_INSTRUCTION_STEP)
        myTask = (Task) arg;
      
      Manager.eventLoop.add(new Event()
      {
        public void execute ()
        {
          if (initial == true)
            {
              //System.out.println("initial");
              
              if (attachedObserver != null)
                myAsmTask.requestDeleteAttachedObserver(attachedObserver);
              
              initial = false;
              setUpTest();
              return;
            }
          else
            {
              //System.out.println("LockObserver.update " + (Task) myTask);
              if (testState != ASM_INSTRUCTION_STEP)
                stepAssertions(myTask.getProc().getTasks());
              else
                stepAssertions(myAsmTask.getProc().getTasks());
            }
        }
      });
    }
    
  } 
}
