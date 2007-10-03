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

import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TestLib;
import frysk.sys.Sig;
import frysk.sys.Pid;
import frysk.event.Event;

public class TestRunState extends TestLib
{
  
  private Task myTask;
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
  protected static final int INSTRUCTION_STEP_NEXT = 4;
  
  protected static final int STEP_OVER_GO = 5;
  protected static final int INSTRUCTION_STEP_NEXT_GO  =6;
  protected static final int STEP_OUT_GO =7;
  
  protected static final int STEP_OVER_STEPPING = 8;
  protected static final int INSTRUCTION_STEP_NEXT_STEPPING = 9;
  protected static final int STEP_OUT_STEPPING = 10;
  
  private boolean insStepFlag = true;
  
  private LockObserver lock;
  
  
  public void testInstructionStepping ()
  {
      if (brokenPpcXXX (3277))
	  return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    runState = new RunState();
    lock = new LockObserver();
    runState.addObserver(lock);
    testState = INSTRUCTION_STEP;
    
    AckDaemonProcess process = new AckDaemonProcess
	(Sig.POLL,
	 new String[] {
	    getExecPath ("funit-rt-stepper"),
	    "" + Pid.get (),
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
  
  public void testLineStepping ()
  {
      if (brokenPpcXXX (3277))
	  return;

    initial = true;
    this.lineMap = new HashMap();
    
    runState = new RunState();
    lock = new LockObserver();
    runState.addObserver(lock);
    testState = STEP_IN;
    
    AckDaemonProcess process = new AckDaemonProcess
	(Sig.POLL,
	 new String[] {
	    getExecPath ("funit-rt-stepper"),
	    "" + Pid.get (),
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
  
  public void testStepOver ()
  {
      if (brokenXXX(4083))
        return;
    
      if (brokenPpcXXX (3277))
      return;

    initial = true;
    this.lineMap = new HashMap();
    
    runState = new RunState();
    lock = new LockObserver();
    runState.addObserver(lock);
    testState = STEP_OVER;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL,
     new String[] {
        getExecPath ("funit-rt-stepper"),
        "" + Pid.get (),
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
  
  public void testInstructionNext ()
  {
    if (brokenXXX(4083))
      return;
    
      if (brokenPpcXXX (3277))
      return;

    initial = true;
    this.lineMap = new HashMap();
    
    runState = new RunState();
    lock = new LockObserver();
    runState.addObserver(lock);
    testState = INSTRUCTION_STEP_NEXT;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL,
     new String[] {
        getExecPath ("funit-rt-stepper"),
        "" + Pid.get (),
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
  
  
  public void testStepOut ()
  {
    if (brokenXXX(4083))
      return;
    
      if (brokenPpcXXX (3277))
      return;

    initial = true;
    this.lineMap = new HashMap();
    
    runState = new RunState();
    lock = new LockObserver();
    runState.addObserver(lock);
    testState = STEP_OUT;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.POLL,
     new String[] {
        getExecPath ("funit-rt-stepper"),
        "" + Pid.get (),
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
        runState.stepInstruction(myProc.getMainTask());
      }
    else
      runState.setUpLineStep(myProc.getMainTask());
  }
  
  public synchronized void stepAssertions (Task task)
  { 
   //System.out.println("Test.stepAssertions");
    myTask = task;
    int lineNum;
    StackFrame frame = StackFactory.createStackFrame(task, 1);
    
    if (frame.getLines().length == 0)
      {
        lineNum = 0;
      }
    else
      {
        lineNum = frame.getLines()[0].getLine();
      }

    int prev = ((Integer) this.lineMap.get(myTask)).intValue();

    if (lineNum == 0)
      {
        this.lineMap.put(task, new Integer(lineNum));
        LinkedList l = new LinkedList();
        l.add(task);
        if (testState == INSTRUCTION_STEP)
          runState.stepInstruction(l);
        else
          runState.setUpLineStep(l);
      }
    
    if (testState == INSTRUCTION_STEP)
      {
        switch (prev)
          {
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
            assertTrue(lineNum == 82 || lineNum == 83);
            break;
          case 83:
            assertTrue(lineNum == 83 || lineNum == 85);
            break;
          case 85:
            assertTrue(lineNum == 85 || lineNum == 87);
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
            assertTrue(lineNum == 67 || lineNum == 95);
            break;
          case 95:
            assertTrue(lineNum == 95 || lineNum == 79 || lineNum == 60 || lineNum == 61);
            break;
          default:
            break;
          }
        count++;
        
//        runState.stepCompleted();
        
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
            assertEquals(lineNum, 83);
            break;
          case 83:
            assertEquals(lineNum, 85);
            break;
          case 85:
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
            assertEquals(lineNum, 92);
            break;
          case 92:
            assertEquals(lineNum, 95);
            break;
          case 95:
            assertTrue(lineNum == 60 || lineNum == 61 || lineNum == 79);
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
          default:
            break;
          }
        count++;
        
//        runState.stepCompleted();
        
        if (count != 50)
          {
            this.lineMap.put(task, new Integer(lineNum));
            LinkedList tasks = new LinkedList();
            tasks.add(task);
            runState.setUpLineStep(tasks);
          }
      }
    
    if (count == 50)
      {
        Manager.eventLoop.requestStop();
        return;
      }
  }
  
  public void stepOverAssertions (Task myTask)
  {
    if (this.testState == STEP_OVER || this.testState == INSTRUCTION_STEP_NEXT
        || this.testState == STEP_OUT)
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
            if (this.testState == STEP_OVER)
              {
                this.testState = STEP_OVER_GO;
                LinkedList l = new LinkedList();
                l.add(myTask);
                this.runState.setUpLineStep(l);
              }
            else if (this.testState == INSTRUCTION_STEP_NEXT)
              {
                this.testState = INSTRUCTION_STEP_NEXT_GO;
                LinkedList l = new LinkedList();
                l.add(myTask);
                this.runState.setUpLineStep(l);
              }
            else if (this.testState == STEP_OUT)
              {
                System.err.println("Setting to step_out_go");
                this.testState = STEP_OUT_GO;
                LinkedList l = new LinkedList();
                l.add(myTask);
                this.runState.setUpLineStep(l);
              }
            
          }
    else
      {
        StackFrame sFrame = StackFactory.createStackFrame(myTask, 1);
        
        if (sFrame.getLines().length == 0)
          this.runState.setUpLineStep(myTask.getProc().getTasks());
        
        Line line = sFrame.getLines()[0];
        
        /* Stepping has been set up - now to continue line stepping until 
         * the important sections of code have been reached. */
        if (this.testState != STEP_OVER_STEPPING
            && this.testState != INSTRUCTION_STEP_NEXT_STEPPING
            && this.testState != STEP_OUT_STEPPING)
          {
            int prev = ((Integer) this.lineMap.get(myTask)).intValue();
            this.lineMap.put(myTask, new Integer(line.getLine()));

            if (this.testState == STEP_OVER_GO)
              {
                /* About to push a frame on the stack */
                if (line.getLine() == 95 && (prev < 95 && prev > 91))
                  {
                    this.testState = STEP_OVER_STEPPING;
                    LinkedList l = new LinkedList();
                    l.add(myTask);
                    this.runState.setUpStepOver(l, StackFactory.createStackFrame(myTask, 3));
                    return;
                  }
               this.runState.setUpLineStep(myTask.getProc().getTasks());
              }
            else if (this.testState == INSTRUCTION_STEP_NEXT_GO)
              {
                /* About to pop a frame off of the stack */
                if (line.getLine() == 95 && (prev < 95 && prev > 91))
                  {
                    if (insStepFlag)
                      {
                        insStepFlag = false;
                        this.runState.stepInstruction(myTask);
                      }
                    else
                      {
                        this.testState = INSTRUCTION_STEP_NEXT_STEPPING;
                        this.runState.setUpStepNextInstruction(myTask, StackFactory.createStackFrame(myTask, 3));
                      }
                    return;
                  }
                this.runState.setUpLineStep(myTask.getProc().getTasks());
              }
            else if (this.testState == STEP_OUT_GO)
              {
                if (line.getLine() >= 60 && line.getLine() <= 67)
                  {
                    this.testState = STEP_OUT_STEPPING;
                    LinkedList l = new LinkedList();
                    l.add(myTask);
                    this.runState.setUpStepOut(l, StackFactory.createStackFrame(myTask, 3));
                  }
                else
                  this.runState.setUpLineStep(myTask.getProc().getTasks());
              }
            else
              {
                this.runState.setUpLineStep(myTask.getProc().getTasks());
                return;
              }
          }
        
        /* Otherwise, the testcase is in the section of code critical to the test */
        else if (this.testState == STEP_OVER_STEPPING)
          {
                StackFrame frame = StackFactory.createStackFrame(myTask, 2);

                /* Make sure we're not missing any frames */
                  
		int lineNr = frame.getLines()[0].getLine ();
                assertTrue ("line number", (lineNr == 95 || lineNr == 96));
                
                assertEquals ("demanged name", "foo",
			      frame.getSymbol ().getDemangledName());
                frame = frame.getOuter();
                assertEquals ("demanged name", "main",
			      frame.getSymbol ().getDemangledName());
                
                Manager.eventLoop.requestStop();
                return;
          }
        else if (this.testState == INSTRUCTION_STEP_NEXT_STEPPING)
          {
            StackFrame frame = StackFactory.createStackFrame(myTask, 2);

            /* Make sure we're not missing any frames */
              
	    int lineNr = frame.getLines()[0].getLine ();
	    assertTrue ("line number", (lineNr == 95 || lineNr == 96));
            
            assertEquals ("demangled name", "foo",
			  frame.getSymbol ().getDemangledName());
            frame = frame.getOuter();
            assertEquals ("demangled name", "main",
			  frame.getSymbol ().getDemangledName());
            
            Manager.eventLoop.requestStop();
            return;
          }
        else if (this.testState == STEP_OUT_STEPPING)
          {
            StackFrame frame = StackFactory.createStackFrame(myTask, 2);

            /* Make sure we're not missing any frames */
              
	    int lineNr = frame.getLines()[0].getLine ();
	    assertTrue ("line number", (lineNr == 95 || lineNr == 96));
            
            assertEquals ("demangled name", "foo",
			  frame.getSymbol ().getDemangledName ());
            frame = frame.getOuter();
            assertEquals ("demangled name", "main",
			  frame.getSymbol ().getDemangledName ());
            
            Manager.eventLoop.requestStop();
            return;
          }
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
      
      Manager.eventLoop.add(new Event()
      {
        public void execute ()
        {
          if (initial == true)
            {
             //System.out.println("First run - Lock.update");
              initial = false;
              setUpTest();
              return;
            }
         //System.out.println("Lock.update");
          if (testState <= STEP_IN)
            stepAssertions(myProc.getMainTask());
          else
            stepOverAssertions(myProc.getMainTask());
        }
      });
    }
    
  }
  
}
