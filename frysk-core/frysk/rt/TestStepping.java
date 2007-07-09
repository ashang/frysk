// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

//import java.io.*;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import frysk.Config;
import frysk.proc.Action;
import frysk.proc.Isa;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.TaskObserver;
import frysk.proc.Task;
import frysk.proc.TestLib;
import frysk.stack.Frame;
import frysk.stack.StackFactory;
import frysk.sys.Sig;
import frysk.sys.Pid;
import frysk.event.Event;

public class TestStepping extends TestLib
{
  
  private Task myTask;
  private Proc myProc;
  
  private int testState = 0;
  
  private HashMap lineMap;
  
  private boolean initial;
  
  int multiCount = 0;
  
  private SteppingEngine se;
    
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
  
  protected static final int ASM_STEP_FUNC_ENTRY = 13;
  protected static final int ASM_STEP_FUNC_RETURN = 14;
  protected static final int ASM_STEP_FUNC_STEP_OVER = 15;
  protected static final int ASM_STEP_FUNC_STEP_OUT = 16;
  
  protected static final int SIGLONGJMP = 20;
  protected static final int GOTO = 21;
  protected static final int SIG_RAISE_ENTER = 22;
  protected static final int SIG_RAISE_EXIT = 23;
  
  private LockObserver lock;
  
  int asmTestStartVal = 0;
  int asmTestFinishVal = 0;
  
  private AttachedObserver attachedObserver;
  
  static String Cfile = Config.getRootSrcDir() 
                           + "frysk-core/frysk/pkglibdir/funit-rt-steptester.c";
  
  public void testLineStepFunctionCall ()
  {
    if (unresolvedOnPPC(3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = LINE_STEP_FUNCTION_CALL;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.USR1,
     new String[] {
        getExecPath ("funit-rt-steptester"),
        "" + Pid.get (),
        "" + Sig.USR1_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    Proc[] temp = new Proc[1];
    temp[0] = myProc;
    se = new SteppingEngine(temp, lock);

    assertRunUntilStop("Attempting to add observer");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
  }
  
  public void testLineStepIfStatementPass ()
  {
    if (unresolvedOnPPC(3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = LINE_STEP_IF_PASS;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.USR1,
     new String[] {
        getExecPath ("funit-rt-steptester"),
        "" + Pid.get (),
        "" + Sig.USR1_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    Proc[] temp = new Proc[1];
    temp[0] = myProc;
    se = new SteppingEngine(temp, lock);
    
    assertRunUntilStop("Attempting to add observer");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
    this.lineMap.clear();
  }
  
  public void testLineStepIfStatementFail ()
  {
    if (unresolvedOnPPC(3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = LINE_STEP_IF_FAIL;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.USR1,
     new String[] {
        getExecPath ("funit-rt-steptester"),
        "" + Pid.get (),
        "" + Sig.USR1_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    Proc[] temp = new Proc[1];
    temp[0] = myProc;
    se = new SteppingEngine(temp, lock);

    assertRunUntilStop("Attempting to add observer");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
    this.lineMap.clear();
  }
  
  public void testLineStepFunctionReturn ()
  {
    if (unresolvedOnPPC(3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = LINE_STEP_FUNCTION_RETURN;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.USR1,
     new String[] {
        getExecPath ("funit-rt-steptester"),
        "" + Pid.get (),
        "" + Sig.USR1_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    Proc[] temp = new Proc[1];
    temp[0] = myProc;
    se = new SteppingEngine(temp, lock);

    assertRunUntilStop("Attempting to add observer");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
    this.lineMap.clear();
  }

  public void testASMSingleStep ()
  {
    
    if (unresolvedOnPPC(3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = ASM_STEP_SINGLE_INST;
    
    String[] cmd = new String[1];
    cmd[0] = getExecPath ("funit-rt-asmstepper");
    
    attachedObserver = new AttachedObserver();
    Manager.host.requestCreateAttachedProc(cmd, attachedObserver);
    
    assertRunUntilStop("Attempting to add attachedObserver");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
    this.lineMap.clear();
  }
  
  public void testASMMultiStep ()
  {
    
    if (unresolvedOnPPC(3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = ASM_STEP_MULTI_LINE;
    
    multiCount = 0;
    
    String[] cmd = new String[1];
    cmd[0] = getExecPath ("funit-rt-asmstepper");
    
    attachedObserver = new AttachedObserver();
    Manager.host.requestCreateAttachedProc(cmd, attachedObserver);
    
    assertRunUntilStop("Attempting to add attachedObserver");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
    this.lineMap.clear();
  }
  
  public void testASMJump ()
  {
    
    if (unresolvedOnPPC(3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = ASM_STEP_JUMP;
    
    String[] cmd = new String[1];
    cmd[0] = getExecPath ("funit-rt-asmstepper");
    
    attachedObserver = new AttachedObserver();
    Manager.host.requestCreateAttachedProc(cmd, attachedObserver);
    
    assertRunUntilStop("Attempting to add attachedObserver");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
    this.lineMap.clear();
  }
  
  public void testASMFunctionEntry ()
  {
    if (unresolvedOnPPC(3277))
      return;
    
    if (unresolved(4711))
	return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
//    asmTestStartVal = 74;
    
    testState = INITIAL;
    test = ASM_STEP_FUNC_ENTRY;
    
    String[] cmd = new String[1];
    cmd[0] = getExecPath ("funit-frameinfo-looper");
    
    attachedObserver = new AttachedObserver();
    Manager.host.requestCreateAttachedProc(cmd, attachedObserver);
    
    assertRunUntilStop("Attempting to add attachedObserver");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
    this.lineMap.clear();
  }
  
  public void testASMFunctionReturn ()
  {
    if (unresolvedOnPPC(3277))
      return;
    
    if (unresolved(4711))
	return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
//    asmTestStartVal = 56;
    
    testState = INITIAL;
    test = ASM_STEP_FUNC_RETURN;
    
    String[] cmd = new String[1];
    cmd[0] = getExecPath ("funit-frameinfo-looper");
    
    attachedObserver = new AttachedObserver();
    Manager.host.requestCreateAttachedProc(cmd, attachedObserver);
    
    assertRunUntilStop("Attempting to add attachedObserver");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
    this.lineMap.clear();
  }
  
  public void testASMFunctionStepOver ()
  {
    if (unresolvedOnPPC(3277))
      return;
    
    if (unresolved(4711))
	return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
//    asmTestStartVal = 74;
    
    testState = INITIAL;
    test = ASM_STEP_FUNC_STEP_OVER;
    
    String[] cmd = new String[1];
    cmd[0] = getExecPath ("funit-frameinfo-looper");
    
    attachedObserver = new AttachedObserver();
    Manager.host.requestCreateAttachedProc(cmd, attachedObserver);
    
    assertRunUntilStop("Attempting to add attachedObserver");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
    this.lineMap.clear();
  }
  
  public void testASMFunctionStepOut ()
  {
    if (unresolvedOnPPC(3277))
      return;
    
    if (unresolved(4711))
	return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
//    asmTestStartVal = 49;
    
    testState = INITIAL;
    test = ASM_STEP_FUNC_STEP_OUT;
    
    String[] cmd = new String[1];
    cmd[0] = getExecPath ("funit-frameinfo-looper");
    
    attachedObserver = new AttachedObserver();
    Manager.host.requestCreateAttachedProc(cmd, attachedObserver);
    
    assertRunUntilStop("Attempting to add attachedObserver");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
    this.lineMap.clear();
  }
  
  public void testStepSigLongJmp ()
  {
    if (unresolvedOnPPC(3277))
      return;
    if (unresolved(4289))
    	return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = SIGLONGJMP;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.USR1,
     new String[] {
        getExecPath ("funit-rt-siglongjmp"),
        "" + Pid.get (),
        "" + Sig.USR1_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    Proc[] temp = new Proc[1];
    temp[0] = myProc;
    se = new SteppingEngine(temp, lock);

    assertRunUntilStop("Attempting to add observer");
    se.removeObserver(lock, myTask.getProc(), false);
    se.clear();
    this.lineMap.clear();
  }
  
  public void testStepGoto ()
  {
    if (unresolvedOnPPC(3277))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = GOTO;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.USR1,
     new String[] {
        getExecPath ("funit-rt-goto"),
        "" + Pid.get (),
        "" + Sig.USR1_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    Proc[] temp = new Proc[1];
    temp[0] = myProc;
    se = new SteppingEngine(temp, lock);

    assertRunUntilStop("Attempting to add observer");
    se.removeObserver(lock, myTask.getProc(), false);
    se.clear();
    this.lineMap.clear();
  }
  
  public void testStepSigRaise ()
  {
    if (unresolvedOnPPC(3277))
      return;
    if (unresolved(4237))
      return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    
    testState = INITIAL;
    test = SIG_RAISE_ENTER;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.USR1,
     new String[] {
        getExecPath ("funit-rt-sigraise"),
        "" + Pid.get (),
        "" + Sig.USR1_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    Proc[] temp = new Proc[1];
    temp[0] = myProc;
    se = new SteppingEngine(temp, lock);

    assertRunUntilStop("Attempting to add observer");
    se.clear();
    se.removeObserver(lock, myTask.getProc(), false);
    this.lineMap.clear();
  }
  
  public void setUpTest ()
  {
    Frame frame = StackFactory.createFrame(myTask);

    if (frame.getLines().length == 0)
      this.lineMap.put(myTask, new Integer(0));
    else
      this.lineMap.put(myTask, new Integer(frame.getLines()[0].getLine()));

    if (test < 10)
      {
        se.stepLine(myTask);
        return;
      }
    else if (test >= 10 && test < 20)
      {
        se.stepInstruction(myTask);
      }
    else
      {
        switch (test)
        {
          case SIGLONGJMP:
            se.stepLine(myTask);
            return;
          
          case GOTO:
            se.stepLine(myTask);
            return;
            
          case SIG_RAISE_ENTER:
            se.stepLine(myTask);
            return;
            
          default:
            break;
        }
      }
  }
  
  public synchronized void assertions ()
  {
    if (this.testState == INITIAL)
      {
        int lineNum;
        Frame sFrame = StackFactory.createFrame(myTask);

        if (sFrame.getLines().length == 0)
            lineNum = 0;
        else
            lineNum = sFrame.getLines()[0].getLine();

        this.lineMap.put(myTask, new Integer(lineNum));
        this.testState = STEPPING;

        switch (test)
          {
          case LINE_STEP_FUNCTION_CALL:
            se.stepLine(myTask);
            break;

          case LINE_STEP_IF_PASS:
            se.stepLine(myTask);
            break;

          case LINE_STEP_IF_FAIL:
            se.stepLine(myTask);
            break;

          case LINE_STEP_FUNCTION_RETURN:
            se.stepLine(myTask);
            break;

          case ASM_STEP_SINGLE_INST:
            se.stepInstruction(myTask);
            break;

          case ASM_STEP_MULTI_LINE:
            se.stepInstruction(myTask);
            break;
            
          case ASM_STEP_JUMP:
            se.stepInstruction(myTask);
            break;
              
          case ASM_STEP_FUNC_ENTRY:
            se.stepInstruction(myTask);
            break;
            
          case ASM_STEP_FUNC_RETURN:
              se.stepInstruction(myTask);
              break;
              
          case ASM_STEP_FUNC_STEP_OVER:
              se.stepInstruction(myTask);
              break;
              
          case ASM_STEP_FUNC_STEP_OUT:
              se.stepInstruction(myTask);
              break;
            
          case SIGLONGJMP:
            se.stepLine(myTask);
            break;
            
          case GOTO:
            se.stepLine(myTask);
            break;
            
          case SIG_RAISE_ENTER:
            se.stepLine(myTask);
            break;
            
          default:
            break;
          }
      }
    else if (testState == STEPPING)
      {
        Frame sFrame = StackFactory.createFrame(myTask);

        if (sFrame.getLines().length == 0)
          {
            se.stepInstruction(myTask);
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
            se.stepLine(myTask);
            break;

          case LINE_STEP_IF_PASS:
            if (line.getLine() == 96)
              {
                this.testState = FINAL_STEP;
              }
            se.stepLine(myTask);
            break;

          case LINE_STEP_IF_FAIL:
            if (line.getLine() == 99)
              {
                this.testState = FINAL_STEP;
              }
            se.stepLine(myTask);
            break;

          case LINE_STEP_FUNCTION_RETURN:
            if (line.getLine() == 86)
              {
                this.testState = FINAL_STEP;
              }
            se.stepLine(myTask);
            break;

          case ASM_STEP_SINGLE_INST:
            if (line.getLine() == 53)
              {
                this.testState = FINAL_STEP;
              }
            se.stepInstruction(myTask);
            break;
            
          case ASM_STEP_MULTI_LINE:
            if (line.getLine() == 55)
              {
                this.testState = FINAL_STEP;
              }
            se.stepInstruction(myTask);
            break;
            
          case ASM_STEP_JUMP:
            if (line.getLine() == 66)
              {
                this.testState = FINAL_STEP;
              }
            se.stepInstruction(myTask);
            break;
            
          case ASM_STEP_FUNC_ENTRY:
            if (line.getLine() == asmTestStartVal)
              {
                this.testState = FINAL_STEP;
              }
            se.stepInstruction(myTask);
            break;
            
          case ASM_STEP_FUNC_RETURN:
              if (line.getLine() == asmTestStartVal)
                {
                  this.testState = FINAL_STEP;
                }
              se.stepInstruction(myTask);
              break;
              
          case ASM_STEP_FUNC_STEP_OVER:
              if (line.getLine() == asmTestStartVal)
                {
                  this.testState = FINAL_STEP;
                  se.stepOver(myTask, sFrame);
                  break;
                }
              se.stepInstruction(myTask);
              break;
              
          case ASM_STEP_FUNC_STEP_OUT:
              if (line.getLine() == asmTestStartVal)
                {
                  this.testState = FINAL_STEP;
                  se.stepOut(myTask, sFrame);
                  break;
                }
              se.stepInstruction(myTask);
              break;

          case SIGLONGJMP:
             if (line.getLine() == 71)
               {
                 this.testState = FINAL_STEP;
               }
             se.stepLine(myTask);
             break;
             
          case GOTO:
            if (line.getLine() == 74)
              {
                this.testState = FINAL_STEP;
              }
            se.stepLine(myTask);
            break;
            
          case SIG_RAISE_ENTER:
            if (((Integer) lineMap.get(myTask)).intValue() != 90)
              {
                se.stepLine(myTask);
                break;
              }
            else if (line.getLine() == 91)
            {
              this.testState = FINAL_STEP;
            }
            se.stepLine(myTask);
            break;
            
          case SIG_RAISE_EXIT:
            if (line.getLine() == 78)
              {
                this.testState = FINAL_STEP;
              }
            se.stepLine(myTask);
            break;
            
          default:
            se.stepLine(myTask);
            break;
          }
        
        this.lineMap.put(myTask, new Integer(line.getLine()));
      }
    else if (testState == FINAL_STEP)
      {
        Frame frame = StackFactory.createFrame(myTask);
        if (frame.getLines().length == 0)
          {
            se.stepLine(myTask);
            return;
          }

        int lineNr = frame.getLines()[0].getLine();
        
        switch (test)
          {
          case LINE_STEP_FUNCTION_CALL:
            if (lineNr == 93)
              {
        	se.stepLine(myTask);
        	return;
              }
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
                se.stepLine(myTask);
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
                se.stepInstruction(myTask);
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
            
          case ASM_STEP_FUNC_ENTRY:
            assertTrue("line number", lineNr == asmTestFinishVal);
            Manager.eventLoop.requestStop();
            return;
            
          case ASM_STEP_FUNC_RETURN:
              if (lineNr == asmTestStartVal)
              {
        	  se.stepInstruction(myTask);
        	  return;
              }
              assertTrue("line number", lineNr == asmTestFinishVal);
              Manager.eventLoop.requestStop();
              return;
              
          case ASM_STEP_FUNC_STEP_OVER:
              assertTrue("line number", lineNr == asmTestFinishVal);
              Manager.eventLoop.requestStop();
              return;
              
          case ASM_STEP_FUNC_STEP_OUT:
              assertTrue("line number", lineNr == asmTestFinishVal);
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
            se.stepLine(myTask);
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
      
      Isa isa = task.getIsa();
      
      switch (test)
      {
      	case ASM_STEP_FUNC_ENTRY:
      	    if (isa instanceof frysk.proc.IsaIA32)
      	    {
      		asmTestStartVal = 210;
      		asmTestFinishVal = 184;
      	    }
      	    else
      	    {
      		asmTestStartVal = 74;
      		asmTestFinishVal = 49;
      	    }
      	    break;
      	
      	case ASM_STEP_FUNC_RETURN:
      	    if (isa instanceof frysk.proc.IsaIA32)
      	    {
      		asmTestStartVal = 195;
      		asmTestFinishVal = 211;
      	    }
      	    else
      	    {
      		asmTestStartVal = 56;
      		asmTestFinishVal = 76;
      	    }
      	    break;
      	    
      	case ASM_STEP_FUNC_STEP_OVER:
      	    if (isa instanceof frysk.proc.IsaIA32)
      	    {
      		asmTestStartVal = 210;
      		asmTestFinishVal = 211;
      	    }
      	    else
      	    {
      		asmTestStartVal = 74;
      		asmTestFinishVal = 76;
      	    }
      	    break;
      	    
      	case ASM_STEP_FUNC_STEP_OUT:
      	    if (isa instanceof frysk.proc.IsaIA32)
      	    {
      		asmTestStartVal = 184;
      		asmTestFinishVal = 211;
      	    }
      	    else
      	    {
      		asmTestStartVal = 49;
      		asmTestFinishVal = 76;
      	    }
      	    break;
      	    
      	    default:
      		break;
      }

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
      Proc[] temp = new Proc[1];
      temp[0] = myProc;
      se = new SteppingEngine(temp, lock);
    }
  }
  
  class LockObserver implements Observer
  {
    
    /**
     * Builtin Observer method - called whenever the Observable we're concerned
     * with - in this case the SteppingEngine's steppingObserver - has changed.
     * 
     * @param o The Observable we're watching
     * @param arg An Object argument, usually a Task when important
     */
    public synchronized void update (Observable o, Object arg)
    {
      TaskStepEngine tse = (TaskStepEngine) arg;
//       System.err.println("LockObserver.update " + arg + " " + initial);
      if (!tse.getState().isStopped())
        return;

      myTask = tse.getTask().getProc().getMainTask();

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
