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
import java.util.Observable;
import java.util.Observer;

import frysk.Config;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.TaskObserver;
import frysk.proc.Task;
import frysk.proc.TestLib;
import frysk.sys.Sig;
import frysk.sys.Pid;
import frysk.event.Event;

public class TestStepping extends TestLib
{
  
  private Task myTask;
  private Proc myProc;
  
  private int testState = 0;
  
  private RunState runState;

  private HashMap lineMap;
  
  private boolean initial;
  
  int multiCount = 0;
    
  protected static final int INITIAL = 0;
  protected static final int STEPPING = 1;
  protected static final int FINAL_STEP = 2;
  
  private int test = 0; 
  protected static final int LINE_STEP_FUNCTION_CALL = 1;
  protected static final int LINE_STEP_IF_PASS = 2;
  protected static final int LINE_STEP_IF_FAIL = 3;
  protected static final int LINE_STEP_FUNCTION_RETURN = 4;
  
  protected static final int ASM_STEP_SINGLE_INST = 10;
  protected static final int ASM_STEP_MULTI_LINE = 11;
  protected static final int ASM_STEP_JUMP = 12;
  
  protected static final int SIGLONGJMP = 20;
  protected static final int GOTO = 21;
  protected static final int SIG_RAISE_ENTER = 22;
  protected static final int SIG_RAISE_EXIT = 23;
  
  private LockObserver lock;
  
  private AttachedObserver attachedObserver;
  
  static String Cfile = Config.getRootSrcDir() 
                           + "frysk-core/frysk/pkglibdir/funit-rt-steptester.c";
  
  public void testLineStepFunctionCall ()
  {
    if (brokenPpcXXX (3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    runState = new RunState();
    runState.addObserver(lock);
    
    testState = INITIAL;
    test = LINE_STEP_FUNCTION_CALL;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL,
     new String[] {
        getExecPath ("funit-rt-steptester"),
        "" + Pid.get (),
        "" + Sig.POLL_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    runState.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    this.lineMap.clear();
  }
  
  public void testLineStepIfStatementPass ()
  {
    if (brokenPpcXXX (3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    runState = new RunState();
    runState.addObserver(lock);
    
    testState = INITIAL;
    test = LINE_STEP_IF_PASS;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL,
     new String[] {
        getExecPath ("funit-rt-steptester"),
        "" + Pid.get (),
        "" + Sig.POLL_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    runState.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    this.lineMap.clear();
  }
  
  public void testLineStepIfStatementFail ()
  {
    if (brokenPpcXXX (3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    runState = new RunState();
    runState.addObserver(lock);
    
    testState = INITIAL;
    test = LINE_STEP_IF_FAIL;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL,
     new String[] {
        getExecPath ("funit-rt-steptester"),
        "" + Pid.get (),
        "" + Sig.POLL_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    runState.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    this.lineMap.clear();
  }
  
  public void testLineStepFunctionReturn ()
  {
    if (brokenPpcXXX (3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    runState = new RunState();
    runState.addObserver(lock);
    
    testState = INITIAL;
    test = LINE_STEP_FUNCTION_RETURN;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL,
     new String[] {
        getExecPath ("funit-rt-steptester"),
        "" + Pid.get (),
        "" + Sig.POLL_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    runState.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    this.lineMap.clear();
  }
  

  public void testASMSingleStep ()
  {
    
    if (brokenPpcXXX (3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = ASM_STEP_SINGLE_INST;
    
    runState = new RunState();
    runState.addObserver(lock);
    
    String[] cmd = new String[1];
    cmd[0] = getExecPath ("funit-rt-asmstepper");
    
    attachedObserver = new AttachedObserver();
    Manager.host.requestCreateAttachedProc(cmd, attachedObserver);
    
    assertRunUntilStop("Attempting to add attachedObserver");
    this.lineMap.clear();
  }
  
  public void testASMMultiStep ()
  {
    
    if (brokenPpcXXX (3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = ASM_STEP_MULTI_LINE;
    
    runState = new RunState();
    runState.addObserver(lock);
    
    multiCount = 0;
    
    String[] cmd = new String[1];
    cmd[0] = getExecPath ("funit-rt-asmstepper");
    
    attachedObserver = new AttachedObserver();
    Manager.host.requestCreateAttachedProc(cmd, attachedObserver);
    
    assertRunUntilStop("Attempting to add attachedObserver");
    this.lineMap.clear();
  }
  
  public void testASMJump ()
  {
    
    if (brokenPpcXXX (3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = ASM_STEP_JUMP;
    
    runState = new RunState();
    runState.addObserver(lock);
    
    String[] cmd = new String[1];
    cmd[0] = getExecPath ("funit-rt-asmstepper");
    
    attachedObserver = new AttachedObserver();
    Manager.host.requestCreateAttachedProc(cmd, attachedObserver);
    
    assertRunUntilStop("Attempting to add attachedObserver");
    this.lineMap.clear();
  }
  
  public void testStepSigLongJmp ()
  {
    if (brokenPpcXXX (3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    runState = new RunState();
    runState.addObserver(lock);
    
    testState = INITIAL;
    test = SIGLONGJMP;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL,
     new String[] {
        getExecPath ("funit-rt-siglongjmp"),
        "" + Pid.get (),
        "" + Sig.POLL_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    runState.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    this.lineMap.clear();
  }
  
  public void testStepGoto ()
  {
    if (brokenPpcXXX (3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    runState = new RunState();
    runState.addObserver(lock);
    
    testState = INITIAL;
    test = GOTO;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL,
     new String[] {
        getExecPath ("funit-rt-goto"),
        "" + Pid.get (),
        "" + Sig.POLL_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    runState.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    this.lineMap.clear();
  }
  
  public void testStepSigRaise ()
  {
    if (brokenXXX(4237))
      return;
    
    if (brokenPpcXXX (3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    runState = new RunState();
    runState.addObserver(lock);
    
    testState = INITIAL;
    test = SIG_RAISE_ENTER;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL,
     new String[] {
        getExecPath ("funit-rt-sigraise"),
        "" + Pid.get (),
        "" + Sig.POLL_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    runState.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    this.lineMap.clear();
  }
  
  public void setUpTest ()
  {
    StackFrame frame = StackFactory.createStackFrame(myTask, 1);

    if (frame.getLines().length == 0)
      this.lineMap.put(myTask, new Integer(0));
    else
      this.lineMap.put(myTask, new Integer(frame.getLines()[0].getLine()));

    if (test < 10)
      {
        runState.setUpLineStep(myTask);
        return;
      }
    else if (test >= 10 && test < 20)
      {
        runState.stepInstruction(myTask);
      }
    else
      {
        switch (test)
        {
          case SIGLONGJMP:
            runState.setUpLineStep(myTask);
            return;
          
          case GOTO:
            runState.setUpLineStep(myTask);
            return;
            
          case SIG_RAISE_ENTER:
            runState.setUpLineStep(myTask);
            return;
            
          default:
            break;
        }
        
      }
  }
  
  public synchronized void assertions ()
  {
    runState.stepCompleted();

    if (this.testState == INITIAL)
      {

        int lineNum;
        StackFrame sFrame = StackFactory.createStackFrame(myTask, 1);

        if (sFrame.getLines().length == 0)
          {
            lineNum = 0;
          }
        else
          {
            lineNum = sFrame.getLines()[0].getLine();
          }

        this.lineMap.put(myTask, new Integer(lineNum));
        
        this.testState = STEPPING;

        switch (test)
          {
          case LINE_STEP_FUNCTION_CALL:
            this.runState.setUpLineStep(myTask);
            break;

          case LINE_STEP_IF_PASS:
            this.runState.setUpLineStep(myTask);
            break;

          case LINE_STEP_IF_FAIL:
            this.runState.setUpLineStep(myTask);
            break;

          case LINE_STEP_FUNCTION_RETURN:
            this.runState.setUpLineStep(myTask);
            break;

          case ASM_STEP_SINGLE_INST:
            this.runState.stepInstruction(myTask);
            break;

          case ASM_STEP_MULTI_LINE:
            this.runState.stepInstruction(myTask);
            break;
            
          case ASM_STEP_JUMP:
            this.runState.stepInstruction(myTask);
            break;
            
          case SIGLONGJMP:
            this.runState.setUpLineStep(myTask);
            break;
            
          case GOTO:
            this.runState.setUpLineStep(myTask);
            break;
            
          case SIG_RAISE_ENTER:
            this.runState.setUpLineStep(myTask);
            break;
            
          default:
            break;
          }
      }
    else if (testState == STEPPING)
      {
        StackFrame sFrame = StackFactory.createStackFrame(myTask, 1);

        if (sFrame.getLines().length == 0)
          {
            this.runState.stepInstruction(myTask);
            return;
          }

        Line line = sFrame.getLines()[0];

        switch (test)
          {
          case LINE_STEP_FUNCTION_CALL:
            if (line.getLine() == 93)
              {
                this.testState = FINAL_STEP;
              }
            this.runState.setUpLineStep(myTask);
            break;

          case LINE_STEP_IF_PASS:
            if (line.getLine() == 96)
              {
                this.testState = FINAL_STEP;
              }
            this.runState.setUpLineStep(myTask);
            break;

          case LINE_STEP_IF_FAIL:
            if (line.getLine() == 99)
              {
                this.testState = FINAL_STEP;
              }
            this.runState.setUpLineStep(myTask);
            break;

          case LINE_STEP_FUNCTION_RETURN:
            if (line.getLine() == 86)
              {
                this.testState = FINAL_STEP;
              }
            this.runState.setUpLineStep(myTask);
            break;

          case ASM_STEP_SINGLE_INST:
            if (line.getLine() == 53)
              {
                this.testState = FINAL_STEP;
              }
            this.runState.stepInstruction(myTask);
            break;
            
          case ASM_STEP_MULTI_LINE:
            if (line.getLine() == 55)
              {
                this.testState = FINAL_STEP;
              }
            this.runState.stepInstruction(myTask);
            break;
            
          case ASM_STEP_JUMP:
            if (line.getLine() == 66)
              {
                this.testState = FINAL_STEP;
              }
            this.runState.stepInstruction(myTask);
            break;

          case SIGLONGJMP:
             if (line.getLine() == 71)
               {
                 this.testState = FINAL_STEP;
               }
             this.runState.setUpLineStep(myTask);
             break;
             
          case GOTO:
            if (line.getLine() == 74)
              {
                this.testState = FINAL_STEP;
              }
            this.runState.setUpLineStep(myTask);
            break;
            
          case SIG_RAISE_ENTER:
            if (((Integer) lineMap.get(myTask)).intValue() != 90)
              {
                this.runState.setUpLineStep(myTask);
                break;
              }
            else if (line.getLine() == 91)
            {
              this.testState = FINAL_STEP;
            }
            this.runState.setUpLineStep(myTask);
            break;
            
          case SIG_RAISE_EXIT:
            if (line.getLine() == 78)
              {
                this.testState = FINAL_STEP;
              }
            this.runState.setUpLineStep(myTask);
            break;
            
          default:
            this.runState.setUpLineStep(myTask);
            break;
          }
        
        this.lineMap.put(myTask, new Integer(line.getLine()));
      }
    else if (testState == FINAL_STEP)
      {

        StackFrame frame = StackFactory.createStackFrame(myTask, 1);
        if (frame.getLines().length == 0)
          {
            this.runState.setUpLineStep(myTask);
            return;
          }

        int lineNr = frame.getLines()[0].getLine();

        switch (test)
          {
          case LINE_STEP_FUNCTION_CALL:
            assertTrue("line number", lineNr == 79 || lineNr == 80);
            Manager.eventLoop.requestStop();
            return;

          case LINE_STEP_IF_PASS:
            assertTrue("line number", lineNr == 97);
            Manager.eventLoop.requestStop();
            return;

          case LINE_STEP_IF_FAIL:
            assertTrue("line number", lineNr == 102);
            Manager.eventLoop.requestStop();
            return;

          case LINE_STEP_FUNCTION_RETURN:
            if (lineNr == 103) /* Strange end-of-function thing */
              {
                this.runState.setUpLineStep(myTask);
                return;
              }
            assertTrue("line number", lineNr == 96 || lineNr == 109);
            Manager.eventLoop.requestStop();
            return;

          case ASM_STEP_SINGLE_INST:
            assertTrue("line number", lineNr == 54);
            Manager.eventLoop.requestStop();
            return;
            
          case ASM_STEP_MULTI_LINE:
            if (lineNr == 56)
              {
                ++multiCount;
                this.runState.stepInstruction(myTask);
                return;
              }
            
            assertTrue("line instruction count", multiCount == 3);
            assertTrue("line number", lineNr == 57);
            Manager.eventLoop.requestStop();
            return;
            
          case ASM_STEP_JUMP:
            assertTrue("line number", lineNr == 53);
            Manager.eventLoop.requestStop();
            return;
            
          case SIGLONGJMP:
            assertTrue("line number", lineNr == 80);
            Manager.eventLoop.requestStop();
            return;
            
          case GOTO:
            assertTrue("line number", lineNr == 71);
            Manager.eventLoop.requestStop();
            return;

          case SIG_RAISE_ENTER:
            assertTrue("line number", lineNr == 68 || lineNr == 69);
            test = SIG_RAISE_EXIT;
            testState = STEPPING;
            runState.setUpLineStep(myTask);
            return;
            
          case SIG_RAISE_EXIT:
            assertTrue("line number", lineNr == 91);
            Manager.eventLoop.requestStop();
            
          default:
            break;
          }

      }
  }
  
  protected class AttachedObserver implements TaskObserver.Attached
  {
    public void addedTo (Object o)
    {
      
    }
    
    public Action updateAttached (Task task)
    {
      myTask = task;
      myProc = task.getProc();
      myTask.requestDeleteAttachedObserver(this);
      return Action.CONTINUE;
    }
    
    public void addFailed  (Object observable, Throwable w)
    {
      
    }
    
    public void deletedFrom (Object o)
    {
      /* Need to give the process some time to get to the looping section */
      try { Thread.sleep(200); } catch (Exception e) {}
      runState.setProc(myProc);
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
    {
      // System.err.println("LockObserver.update " + arg + " " + initial);
      if (arg == null)
        return;

      myTask = ((Task) arg).getProc().getMainTask();

      Manager.eventLoop.add(new Event()
      {
        public void execute ()
        {
          if (initial == true)
            {
              initial = false;
              setUpTest();

              return;
            }
          else
            {
              assertions();
            }
        }
      });
    }
    
  } 
}
