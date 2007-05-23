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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TestLib;
import frysk.rt.states.State;
import frysk.sys.Sig;
import frysk.sys.Pid;
import frysk.event.Event;

public class TestSteppingEngine extends TestLib
{
  
  private Task myTask;
  private Proc myProc;
  
  private int testState = 0;
  
  private HashMap lineMap;
  
  private boolean initial;
  
  private int count = 0;
  
  protected static final int INSTRUCTION_STEP = 0;
  protected static final int STEP_IN = 1;
  protected static final int STEP_OVER = 2;
  protected static final int STEP_OUT = 3;
  protected static final int INSTRUCTION_STEP_NEXT = 4;
  
  protected static final int STEP_OVER_GO = 5;
  protected static final int INSTRUCTION_STEP_NEXT_GO = 6;
  protected static final int STEP_OUT_GO = 7;
  
  protected static final int STEP_OVER_STEPPING = 8;
  protected static final int INSTRUCTION_STEP_NEXT_STEPPING = 9;
  protected static final int STEP_OUT_STEPPING = 10;
  
  protected static final int CONTINUE = 11;
  protected static final int BREAKPOINTING = 12;
  
  protected static final int INSTRUCTION_STEP_LIST = 13;
  protected static final int STEP_IN_LIST = 14;
  
  private boolean insStepFlag = true;
  
  private LockObserver lock;
  
  
  public void testInstructionStepping ()
  {
      if (brokenPpcXXX (3277))
	  return;
    
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    SteppingEngine.addObserver(lock);
    testState = INSTRUCTION_STEP;
    
    AckDaemonProcess process = new AckDaemonProcess
	(Sig.USR1,
	 new String[] {
	    getExecPath ("funit-rt-stepper"),
	    "" + Pid.get (),
	    "" + Sig.USR1_
	});
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    SteppingEngine.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    SteppingEngine.clear();
  }
  
  public void testInstructionSteppingList ()
  {
      if (brokenPpcXXX (3277))
	  return;

    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    SteppingEngine.addObserver(lock);
    testState = INSTRUCTION_STEP_LIST;
    
    AckDaemonProcess process = new AckDaemonProcess
	(Sig.USR1,
	 new String[] {
	    getExecPath ("funit-rt-stepper"),
	    "" + Pid.get (),
	    "" + Sig.USR1_
	});
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    Proc[] procs = new Proc[1];
    procs[0] = myProc;
    SteppingEngine.setProcs(procs);

    assertRunUntilStop("Attempting to add observer");
    SteppingEngine.removeObserver(lock, myProc);
    SteppingEngine.cleanTask(myTask);
    SteppingEngine.clear();
  }
  
  public void testLineStepping ()
  {
      if (brokenPpcXXX (3277))
	  return;

    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    SteppingEngine.addObserver(lock);
    testState = STEP_IN;
    
    AckDaemonProcess process = new AckDaemonProcess
	(Sig.USR1,
	 new String[] {
	    getExecPath ("funit-rt-stepper"),
	    "" + Pid.get (),
	    "" + Sig.USR1_
	});
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    SteppingEngine.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    SteppingEngine.clear();
  }
  
  public void testLineSteppingList ()
  {
      if (brokenPpcXXX (3277))
	  return;

    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    SteppingEngine.addObserver(lock);
    testState = STEP_IN_LIST;
    
    AckDaemonProcess process = new AckDaemonProcess
	(Sig.USR1,
	 new String[] {
	    getExecPath ("funit-rt-stepper"),
	    "" + Pid.get (),
	    "" + Sig.USR1_
	});
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    Proc[] procs = new Proc[1];
    procs[0] = myProc;
    SteppingEngine.setProcs(procs);

    assertRunUntilStop("Attempting to add observer");
    SteppingEngine.removeObserver(lock, myProc);
    SteppingEngine.cleanTask(myTask);
    SteppingEngine.clear();
  }
  
  public void testStepOver ()
  {
      if (brokenXXX(4083))
        return;
    
      if (brokenPpcXXX (3277))
      return;

    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    SteppingEngine.addObserver(lock);
    testState = STEP_OVER;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.USR1,
     new String[] {
        getExecPath ("funit-rt-stepper"),
        "" + Pid.get (),
        "" + Sig.USR1_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    SteppingEngine.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    SteppingEngine.clear();
  }
  
  public void testInstructionNext ()
  {
    if (brokenXXX(4083))
      return;
    
      if (brokenPpcXXX (3277))
      return;

    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    SteppingEngine.addObserver(lock);
    testState = INSTRUCTION_STEP_NEXT;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.USR1,
     new String[] {
        getExecPath ("funit-rt-stepper"),
        "" + Pid.get (),
        "" + Sig.USR1_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    SteppingEngine.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    SteppingEngine.clear();
  }
  
  
  public void testStepOut ()
  {
    if (brokenXXX(4083))
      return;
    
      if (brokenPpcXXX (3277))
      return;

    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    SteppingEngine.addObserver(lock);
    testState = STEP_OUT;
    
    AckDaemonProcess process = new AckDaemonProcess
    (Sig.USR1,
     new String[] {
        getExecPath ("funit-rt-stepper"),
        "" + Pid.get (),
        "" + Sig.USR1_
    });
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    SteppingEngine.setProc(myProc);

    assertRunUntilStop("Attempting to add observer");
    SteppingEngine.clear();
  }
  
  public void testContinue ()
  {
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    SteppingEngine.addObserver(lock);
    testState = CONTINUE;
    
    AckDaemonProcess process = new AckDaemonProcess
	(Sig.USR1,
	 new String[] {
	    getExecPath ("funit-rt-stepper"),
	    "" + Pid.get (),
	    "" + Sig.USR1_
	});
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    Proc[] procs = new Proc[1];
    procs[0] = myProc;
    SteppingEngine.setProcs(procs);

    assertRunUntilStop("Attempting to add observer");
    SteppingEngine.removeObserver(lock, myProc);
    SteppingEngine.cleanTask(myTask);
    SteppingEngine.clear();
  }

  private long breakpointAddress;
  
  public void testBreakpointing ()
  {
    initial = true;
    this.lineMap = new HashMap();
    
    lock = new LockObserver();
    SteppingEngine.addObserver(lock);
    testState = BREAKPOINTING;
    
    AckDaemonProcess process = new AckDaemonProcess
	(Sig.USR1,
	 new String[] {
	    getExecPath ("funit-rt-stepper"),
	    "" + Pid.get (),
	    "" + Sig.USR1_
	});
    
    myTask = process.findTaskUsingRefresh(true);
    myProc = myTask.getProc();
    assertNotNull(myProc);
    
    Proc[] procs = new Proc[1];
    procs[0] = myProc;
    SteppingEngine.setProcs(procs);

    assertRunUntilStop("Attempting to add observer");
    SteppingEngine.removeObserver(lock, myProc);
    SteppingEngine.cleanTask(myTask);
    SteppingEngine.clear();
  }
  
  public void setUpTest ()
  {
    myTask = myProc.getMainTask();

    Frame frame = StackFactory.createFrame(myTask, 2);
    if (frame.getLines().length == 0)
      {
	this.lineMap.put(myTask, new Integer(0));
      }
    else
      this.lineMap.put(myTask, new Integer(frame.getLines()[0].getLine()));

    count = 0;

    if (testState == INSTRUCTION_STEP)
      {
	SteppingEngine.stepInstruction(myProc.getMainTask());
      }
    else if (testState == BREAKPOINTING)
      {
	breakpointAddress = frame.getOuter().getAddress();
	SteppingEngine.setBreakpoint(myTask, breakpointAddress);
	lock.update(new Observable(), new Object());
	return;
      }
    else if (testState == STEP_IN_LIST)
      {
	LinkedList l = new LinkedList();
	l.add(myTask);
	SteppingEngine.setUpLineStep(l);
	testState = STEP_IN;
      }
    else if (testState == INSTRUCTION_STEP_LIST)
      {
	LinkedList l = new LinkedList();
	l.add(myTask);
	SteppingEngine.stepInstruction(l);
	testState = INSTRUCTION_STEP;
      }
    else
      SteppingEngine.setUpLineStep(myProc.getMainTask());
  }
  
  public synchronized void stepAssertions (Task task)
  { 
   //System.out.println("Test.stepAssertions");
    myTask = task;
    int lineNum;
    Frame frame = StackFactory.createFrame(task, 1);
    
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
          SteppingEngine.stepInstruction(l);
        else
          SteppingEngine.setUpLineStep(l);
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
        
//        SteppingEngine.stepCompleted();
        
        if (count != 50)
          {
            this.lineMap.put(task, new Integer(lineNum));
            LinkedList l = new LinkedList();
            l.add(task);
            SteppingEngine.stepInstruction(l);
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
        
//        SteppingEngine.stepCompleted();
        
        if (count != 50)
          {
            this.lineMap.put(task, new Integer(lineNum));
            LinkedList tasks = new LinkedList();
            tasks.add(task);
            SteppingEngine.setUpLineStep(tasks);
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
        Frame sFrame = StackFactory.createFrame(myTask, 1);
        
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
                SteppingEngine.setUpLineStep(l);
              }
            else if (this.testState == INSTRUCTION_STEP_NEXT)
              {
                this.testState = INSTRUCTION_STEP_NEXT_GO;
                LinkedList l = new LinkedList();
                l.add(myTask);
                SteppingEngine.setUpLineStep(l);
              }
            else if (this.testState == STEP_OUT)
              {
                this.testState = STEP_OUT_GO;
                LinkedList l = new LinkedList();
                l.add(myTask);
                SteppingEngine.setUpLineStep(l);
              }
            
          }
    else
      {
        Frame sFrame = StackFactory.createFrame(myTask, 1);
        
        if (sFrame.getLines().length == 0)
          SteppingEngine.setUpLineStep(myTask.getProc().getTasks());
        
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
                    SteppingEngine.setUpStepOver(l, StackFactory.createFrame(myTask, 3));
                    return;
                  }
               SteppingEngine.setUpLineStep(myTask.getProc().getTasks());
              }
            else if (this.testState == INSTRUCTION_STEP_NEXT_GO)
              {
                /* About to pop a frame off of the stack */
                if (line.getLine() == 95 && (prev < 95 && prev > 91))
                  {
                    if (insStepFlag)
                      {
                        insStepFlag = false;
                        SteppingEngine.stepInstruction(myTask);
                      }
                    else
                      {
                        this.testState = INSTRUCTION_STEP_NEXT_STEPPING;
                        SteppingEngine.setUpStepNextInstruction(myTask, StackFactory.createFrame(myTask, 3));
                      }
                    return;
                  }
                SteppingEngine.setUpLineStep(myTask.getProc().getTasks());
              }
            else if (this.testState == STEP_OUT_GO)
              {
                if (line.getLine() >= 60 && line.getLine() <= 67)
                  {
                    this.testState = STEP_OUT_STEPPING;
                    LinkedList l = new LinkedList();
                    l.add(myTask);
                    SteppingEngine.setUpStepOut(l, StackFactory.createFrame(myTask, 3));
                  }
                else
                  SteppingEngine.setUpLineStep(myTask.getProc().getTasks());
              }
            else
              {
                SteppingEngine.setUpLineStep(myTask.getProc().getTasks());
                return;
              }
          }
        
        /* Otherwise, the testcase is in the section of code critical to the test */
        else if (this.testState == STEP_OVER_STEPPING)
          {
                Frame frame = StackFactory.createFrame(myTask, 2);

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
            Frame frame = StackFactory.createFrame(myTask, 2);

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
            Frame frame = StackFactory.createFrame(myTask, 2);

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
  
  private boolean continueFlag = false;
  
  private void setUpContinue (Task task)
  {
    myTask = task;
    State s = SteppingEngine.getTaskState(task);
    assertNotNull(s);
    assertEquals("Stopped State", true, s.isStopped());
    assertEquals("isTaskRunning", false, SteppingEngine.isTaskRunning(task));

    LinkedList l = new LinkedList();
    l.add(task);
    assertEquals("isProcRunning", false, SteppingEngine.isProcRunning(l));

    if (continueFlag == false)
      {
	continueFlag = true;
	SteppingEngine.continueExecution(l);
      }
    else
      {
	SteppingEngine.setRunning(l);
	State r = SteppingEngine.getTaskState(myTask);
	
	assertEquals ("Is task now running", false, r.isStopped());
	assertEquals ("Is proc now running", true, SteppingEngine.isProcRunning(l));
	
	SteppingEngine.setTaskState(myTask, s);
	
	assertEquals ("Stopped State", true, s.isStopped());
	assertEquals ("isTaskRunning", false, SteppingEngine.isTaskRunning(task));
	
	Manager.eventLoop.requestStop();
      }

    return;
  }
  
  private void continueAssertions ()
  {
    State s = SteppingEngine.getTaskState(myTask);

    assertNotNull(s);
    
    while (s.isStopped())
      {
	s = SteppingEngine.getTaskState(myTask);
      }
    
    assertEquals ("Running State", false, s.isStopped());
    assertEquals ("isTaskRunning", true, SteppingEngine.isTaskRunning(myTask));
    
    LinkedList l = new LinkedList();
    l.add(myTask);
    assertEquals ("isProcRunning", true, SteppingEngine.isProcRunning(l));
    
    SteppingEngine.stop(null, l);
  }
  
  private void breakpointAssertions ()
  {
    Breakpoint b = SteppingEngine.getTaskBreakpoint(myTask);
    assertNotNull(b);
    
    assertEquals ("isAdded", true, b.isAdded());
    assertEquals ("isRemoved", false, b.isRemoved());
    assertEquals ("breakpoint address", breakpointAddress, b.getAddress());
    
    Manager.eventLoop.requestStop();
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
//      System.err.println("LockObserver.update " + o + " " + arg + " " + continueFlag);
      if (arg == null)
	{
	  if (testState == CONTINUE && continueFlag == true)
	    {
	      Manager.eventLoop.add(new Event()
	      {
	        public void execute ()
	        {
	          continueAssertions();
	        }
	      });
	      return;
	    }
	  else
	    return;
	}
      
      Manager.eventLoop.add(new Event()
      {
        public void execute ()
        {
          if (testState == CONTINUE)
            {
              initial = false;
              setUpContinue(myProc.getMainTask());
              return;
            }
          
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
          else if (testState > STEP_IN && testState <= STEP_OUT_STEPPING)
            stepOverAssertions(myProc.getMainTask());
          else if (testState == BREAKPOINTING)
            breakpointAssertions();
        }
      });
    }
    
  }
  
}
