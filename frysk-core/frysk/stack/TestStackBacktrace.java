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


package frysk.stack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

//import inua.eio.ByteBuffer;
//import inua.eio.ULong;
import frysk.event.Event;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TestLib;
//import frysk.sys.proc.MapsBuilder;
import frysk.rt.Line;
import frysk.rt.SteppingEngine;
import frysk.rt.Symbol;
import frysk.sys.Pid;
import frysk.sys.Sig;

public class TestStackBacktrace
    extends TestLib
{
  private Task myTask;
  
  private SteppingEngine steppingEngine;
  
  private int task_count;
  
  int test = 0;
  
  /*
   *    [frame.getLineber()]
   * [frame.getInner().toString()]
   *    [frame.getMethodName()]
   *    [frame.getSourceFile()]
   *            ^
   *            |  ...   ...          
   * [TID] -> [f0|f1|f2|f3...]
   * [TID] -> ....
   */
  private String[][][] frameTracker = new String[3][9][5];

  public void testBacktrace ()
  {
    if (brokenXXX (4468))
      return;
    
    // Backtraces only work on x86 and x86_64 for now.
    if (brokenPpcXXX (3277))
	return;
    
    test = 1;

    lock = new LockObserver();
    
    AckDaemonProcess process = new AckDaemonProcess
	(Sig.USR1, new String[] {
	    getExecPath ("funit-rt-looper"),
	    "" + Pid.get (),
	    "" + Sig.USR1_
	});
    
    myTask = process.findTaskUsingRefresh(true);
    assertNotNull(myTask);
    
    Proc[] temp = new Proc[1];
    temp[0] = myTask.getProc();
    steppingEngine = new SteppingEngine(temp, lock);
    
    assertRunUntilStop("Attempting to add observer");
    steppingEngine.clear();

//    class MyBuilder
//        extends MapsBuilder
//    {
//
//      public void buildBuffer (byte[] maps)
//      {
//        maps[maps.length - 1] = 0;
//      }
//
//      public void buildMap (long addressLow, long addressHigh,
//                            boolean permRead, boolean permWrite,
//                            boolean permExecute, boolean shared,
//                            long offset, int devMajor, int devMinor, int inode,
//                            int pathnameOffset, int pathnameLength)
//      {
//        ByteBuffer buffer = myTask.getMemory();
//
//        for (long i = addressLow; ULong.LT(i, addressHigh); i++)
//          {
//            System.err.println(Long.toHexString(i) + " is in the Mmap!");
//            buffer.getByte(i);
//          }
//      }
//
//    }

    // MyBuilder builder = new MyBuilder();
    // System.out.println("Before maps test");
    // builder.construct(myTask.getTid());
    // System.out.println("After maps test");
  }
  
  private void firstTestBacktraceAssertions ()
  {
    Frame frame = StackFactory.createFrame(myTask);
//    System.err.println(StackFactory.printStackTrace(frame));
    Line line;
    Symbol symbol;

    assertNotNull(frame);
    assertNull(frame.getInner());
    line = frame.getLines()[0];
    symbol = frame.getSymbol();
    assertEquals ("file name", "funit-rt-looper.c", line.getFile().getName());
    //XXX: See #3259
//    assertEquals("line number", 62, line.getLine());
    assertEquals("symbol", "baz", symbol.getDemangledName ());
    
    frame = frame.getOuter();
    assertNotNull (frame);
    assertNotNull(frame.getInner());
    line = frame.getLines()[0];
    symbol = frame.getSymbol();
    assertEquals("file name", "funit-rt-looper.c", line.getFile().getName());
    assertEquals(71, line.getLine ());
    assertEquals("symbol", "bar", symbol.getDemangledName());

    frame = frame.getOuter();
    assertNotNull(frame);
    assertNotNull(frame.getInner());
    line = frame.getLines()[0];
    symbol = frame.getSymbol();
    assertEquals("file name", "funit-rt-looper.c", line.getFile().getName());
    assertEquals("line number", 81, line.getLine());
    assertEquals("foo", symbol.getDemangledName());

    frame = frame.getOuter();
    assertNotNull(frame);
    assertNotNull(frame.getInner());
    line = frame.getLines()[0];
    symbol = frame.getSymbol();
    assertEquals("file name", "funit-rt-looper.c", line.getFile().getName());
    assertEquals("line number", 117, line.getLine());
    assertEquals("symbol name", "main", symbol.getDemangledName());

    frame = frame.getOuter();
    assertNotNull(frame);
    assertNotNull(frame.getInner());
    symbol = frame.getSymbol();
    // No check for file information - depends on glibc-debuginfo.
    assertEquals("symbol", "__libc_start_main", symbol.getDemangledName());

    frame = frame.getOuter();
    assertNotNull(frame);
    assertNotNull(frame.getInner());
    symbol = frame.getSymbol();
    // No check for line information - depends on glibc-debuginfo.
    assertEquals("symbol", "_start", symbol.getDemangledName());

    frame = frame.getOuter();
    assertNull(frame);
    
    Manager.eventLoop.requestStop();
  }
  
  public synchronized void testThreadedBacktrace ()
  {
      // Backtraces only work on x86 and x86_64 for now.
      if (brokenPpcXXX (3277))
	  return;

      test = 2;
      lock = new LockObserver();

    AckDaemonProcess process = new AckDaemonProcess
	(Sig.USR1, new String[] {
	    getExecPath ("funit-rt-threader"),
	    "" + Pid.get (),
	    "" + Sig.USR1_
	});
    
    myTask = process.findTaskUsingRefresh(true);
    
    task_count = 0;

    Proc[] temp = new Proc[1];
    temp[0] = myTask.getProc();
    steppingEngine = new SteppingEngine(temp, lock);
    
    assertRunUntilStop("testThreadedBackTrace");    
    steppingEngine.clear();
  }
  
  private boolean initial;
  private LockObserver lock;
  private HashMap lineMap;
  private int testState;
  private static int PUSH = 0;
  private static int PUSH_GO = 1;
  private static int PUSH_STEPPING = 2;
  private static int POP = 3;
  private static int POP_GO = 4;
  private static int POP_STEPPING = 5;
  private Proc myProc;
  
  /**
   * Test instruction stepping through pushing a new frame onto the stack.
   */
  public void testFramePushing ()
  {

    if (brokenPpcXXX (3277))
      return;
    
    /* Only applies to i386 */
    if (brokenXXX(4059))
      return;
  
  initial = true;
  this.lineMap = new HashMap();
  
  lock = new LockObserver();
  
  testState = PUSH;
  
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
  
  Proc[] temp = new Proc[1];
  temp[0] = myProc;
  steppingEngine = new SteppingEngine(temp, lock);

  assertRunUntilStop("Attempting to add observer");
  steppingEngine.clear();
  }
  
  /**
   * Test instruction stepping through popping a frame off the stack
   */
  public void testFramePopping ()
  {

    if (brokenPpcXXX (3277))
      return;
    
    /* Only applies to i386 */
    if (brokenXXX(4059))
      return;
  
  initial = true;
  this.lineMap = new HashMap();
  
  lock = new LockObserver();
  
  testState = POP;
  
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
  
  Proc[] temp = new Proc[1];
  temp[0] = myProc;
  steppingEngine = new SteppingEngine(temp, lock);

  assertRunUntilStop("Attempting to add observer");
  steppingEngine.clear();
  }
  
  /**
   * Set up all the HashMaps and such.
   */
  public void setUpTest ()
  {

    Frame frame = StackFactory.createFrame(myTask, 1);
    
    if (frame.getLines().length == 0)
      {
        this.lineMap.put(myTask, new Integer(0));
        steppingEngine.stepLine(myTask.getProc().getTasks());
        return;
      }
      
    Line line = frame.getLines()[0];
    this.lineMap.put(myTask, new Integer(line.getLine()));
    steppingEngine.stepLine(myTask.getProc().getTasks());
  }
  
  /**
   * Sort the matrix by TID and compare its contents to the actual source file.
   */
  public void frameAssertions()
  {
    int lowest = 0;
    int next = 0;
    int last = 0;
    
    /* The Tasks could appear in any order, and the TID of each sucessive
     * Task is not necessarily larger than the TID of the thread that spawned
     * it. The only way to be sure that we're looking at the right thread is to
     * manually check the function names in the call stack. */
    for (int i = 0; i < 3; i++)
      {
        if (this.frameTracker[i][1][2].equals("bak_two"))
          lowest = i; 
      }
    
    /* Main thread assertions */
    assertTrue(this.frameTracker[lowest][1][1].endsWith("/frysk/pkglibdir/funit-rt-threader.c"));
    assertEquals("bak_two", this.frameTracker[lowest][1][2]);
    assertNotNull(this.frameTracker[lowest][1][3]);
    assertEquals(71, Integer.parseInt(this.frameTracker[lowest][1][4]));

    assertTrue(this.frameTracker[lowest][2][1].endsWith("/frysk/pkglibdir/funit-rt-threader.c"));
    assertEquals("bak", this.frameTracker[lowest][2][2]);
    assertNotNull(this.frameTracker[lowest][2][3]);
    assertEquals(81, Integer.parseInt(this.frameTracker[lowest][2][4]));
    
    assertTrue(this.frameTracker[lowest][3][1].endsWith("/frysk/pkglibdir/funit-rt-threader.c"));
    assertEquals("baz_two", this.frameTracker[lowest][3][2]);
    assertNotNull(this.frameTracker[lowest][3][3]);
    assertEquals(91, Integer.parseInt(this.frameTracker[lowest][3][4]));
    
    assertTrue(this.frameTracker[lowest][4][1].endsWith("/frysk/pkglibdir/funit-rt-threader.c"));
    assertEquals("bar_two", this.frameTracker[lowest][4][2]);
    assertNotNull(this.frameTracker[lowest][4][3]);
    assertEquals(105, Integer.parseInt(this.frameTracker[lowest][4][4]));
    
    assertTrue(this.frameTracker[lowest][5][1].endsWith("/frysk/pkglibdir/funit-rt-threader.c"));
    assertEquals("foo_two", this.frameTracker[lowest][5][2]);
    assertNotNull(this.frameTracker[lowest][5][3]);
    assertEquals(123, Integer.parseInt(this.frameTracker[lowest][5][4]));
    
    assertTrue(this.frameTracker[lowest][6][1].endsWith("/frysk/pkglibdir/funit-rt-threader.c"));
    assertEquals("main", this.frameTracker[lowest][6][2]);
    assertNotNull(this.frameTracker[lowest][6][3]);
    assertEquals(172, Integer.parseInt(this.frameTracker[lowest][6][4]));
    
    assertEquals("", this.frameTracker[lowest][7][1]);
    assertEquals("__libc_start_main", this.frameTracker[lowest][7][2]);
    assertNotNull(this.frameTracker[lowest][7][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[lowest][7][4]));
    
    assertEquals("", this.frameTracker[lowest][8][1]);
    assertEquals("_start", this.frameTracker[lowest][8][2]);
    assertNotNull(this.frameTracker[lowest][8][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[lowest][8][4]));
    
    int index = 1;
    for (int i = 0; i < 3; i++)
      {
        if (this.frameTracker[i][1][2].equals("signal_parent"))
          next = i;
        else if (this.frameTracker[i][1][2].equals("kill"))
          {
            index = 2;
            next = i;
            break;
          }
      }
    
    /* Second thread assertions */
    assertTrue(this.frameTracker[next][index][1].endsWith("/frysk/pkglibdir/funit-rt-threader.c"));
    assertEquals("signal_parent", this.frameTracker[next][index][2]);
    assertNotNull(this.frameTracker[next][index][3]);
    //XXX: One-line looper bug. Comes back as 62.
//    assertEquals(63, Integer.parseInt(this.frameTracker[next][index][4]));
    
    index++;
    assertEquals("", this.frameTracker[next][index][1]);
    assertEquals("start_thread", this.frameTracker[next][index][2]);
    assertNotNull(this.frameTracker[next][index][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[next][index][4]));
    
    index++;
    assertEquals("", this.frameTracker[next][index][1]);
//    if (MachineType.getMachineType() == MachineType.IA32)
//      assertEquals("__clone", this.frameTracker[next][3][2]);
//    if (MachineType.getMachineType() == MachineType.X8664)
//      assertEquals("(__)?clone", this.frameTracker[next][3][2]);
    assertTrue(this.frameTracker[next][index][2].matches("(__)?clone"));
    assertNotNull(this.frameTracker[next][index][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[next][index][4]));
    
    /* Find the last thread */
    switch (lowest + next)
      {
      case 1:
        last = 2;
        break;
      case 2:
        last = 1;
        break;
      case 3:
        last = 0;
        break;
      }
    
    /* Third thread assertions */
    assertEquals("bak", this.frameTracker[last][1][2]);
    assertNotNull(this.frameTracker[last][1][3]);
    assertEquals(83, Integer.parseInt(this.frameTracker[last][1][4]));
    
    assertTrue(this.frameTracker[last][2][1].endsWith("/frysk/pkglibdir/funit-rt-threader.c"));
    assertEquals("baz", this.frameTracker[last][2][2]);
    assertNotNull(this.frameTracker[last][2][3]);
    assertEquals(98, Integer.parseInt(this.frameTracker[last][2][4]));
    
    assertTrue(this.frameTracker[last][3][1].endsWith("/frysk/pkglibdir/funit-rt-threader.c"));
    assertEquals("bar", this.frameTracker[last][3][2]);
    assertNotNull(this.frameTracker[last][3][3]);
    assertEquals(116, Integer.parseInt(this.frameTracker[last][3][4]));
    
    assertTrue(this.frameTracker[last][4][1].endsWith("/frysk/pkglibdir/funit-rt-threader.c"));
    assertEquals("foo", this.frameTracker[last][4][2]);
    assertNotNull(this.frameTracker[last][4][3]);
    assertEquals(130, Integer.parseInt(this.frameTracker[last][4][4]));
    
    assertEquals("", this.frameTracker[next][2][1]);
    assertEquals("start_thread", this.frameTracker[next][2][2]);
    assertNotNull(this.frameTracker[next][2][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[next][2][4]));
    
    assertEquals("", this.frameTracker[next][3][1]);
//    if (MachineType.getMachineType() == MachineType.IA32)
//      assertEquals("__clone", this.frameTracker[next][3][2]);
//    if (MachineType.getMachineType() == MachineType.X8664)
//      assertEquals("(__)?clone", this.frameTracker[next][3][2]);
    assertTrue(this.frameTracker[next][3][2].matches("(__)?clone"));
    assertNotNull(this.frameTracker[next][3][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[next][3][4]));
    
    Manager.eventLoop.requestStop();
  }
  
  public void pushPopAssertions ()
  {
    Frame sFrame = StackFactory.createFrame(myTask, 1);
    Line line = null; 
    
    if (this.testState == PUSH || this.testState == POP)
      {

        int lineNum;
            if (sFrame.getLines().length == 0)
              {
                lineNum = 0;
              }
            else
              {
                line = sFrame.getLines()[0];
                lineNum = line.getLine();
              }
            this.lineMap.put(myTask, new Integer(lineNum));
            if (this.testState == PUSH)
              this.testState = PUSH_GO;
            else if (this.testState == POP)
              this.testState = POP_GO;
            
            steppingEngine.stepLine(myTask.getProc().getTasks());
          }
    else
      {
        /* Stepping has been set up - now to continue line stepping until 
         * the important sections of code have been reached. */
        if (this.testState != PUSH_STEPPING && this.testState != POP_STEPPING)
          {
            int prev = ((Integer) this.lineMap.get(myTask)).intValue();
            this.lineMap.put(myTask, new Integer(line.getLine()));

            if (this.testState == PUSH_GO)
              {
                /* About to push a frame on the stack */
                if (line.getLine() == 95 && (prev < 95 && prev > 91))
                  {
                    this.testState = PUSH_STEPPING;
                    steppingEngine.stepInstruction(myTask.getProc().getTasks());
                    return;
                  }
               steppingEngine.stepLine(myTask.getProc().getTasks());
              }
            else if (this.testState == POP_GO)
              {
                /* About to pop a frame off of the stack */
                if (line.getLine() == 63)
                  {
                    this.testState = POP_STEPPING;
                    steppingEngine.stepInstruction(myTask.getProc().getTasks());
                    return;
                  }
                steppingEngine.stepLine(myTask.getProc().getTasks());
              }
            else
              {
                steppingEngine.stepLine(myTask.getProc().getTasks());
                return;
              }
          }
        
        /* Otherwise, the testcase is in the section of code critical to the test */
        else if (this.testState == PUSH_STEPPING)
          {
            if (line.getLine() > 62)
              {
                Manager.eventLoop.requestStop();
                return;
              }
            else
              {
                Frame frame = StackFactory.createFrame(myTask, 3);

                /* Make sure we're not missing any frames */
                if (frame.getLines()[0].getLine() > 95)
                  {
                    assertEquals ("demangled name", "jump",
				  frame.getSymbol().getDemangledName());
                    frame = frame.getOuter();
                  }
                assertEquals ("demangled name", "foo",
			      frame.getSymbol().getDemangledName());
                frame = frame.getOuter();
                assertEquals ("demangled name", "main",
			      frame.getSymbol().getDemangledName());
                
                steppingEngine.stepInstruction(myTask.getProc().getTasks());
                return;
              }
          }
        else if (this.testState == POP_STEPPING)
          {
            if (line.getLine() > 68)
              {
                Manager.eventLoop.requestStop();
                return;
              }
            else
              {
                Frame frame = StackFactory.createFrame(myTask, 3);

                /* Make sure we're not missing any frames */
                assertEquals ("demangled name", "jump",
			      frame.getSymbol().getDemangledName());
                frame = frame.getOuter();
                assertEquals ("demangled name", "foo",
			      frame.getSymbol().getDemangledName());
                frame = frame.getOuter();
                assertEquals ("demangled name", "main",
			      frame.getSymbol().getDemangledName());
                
                steppingEngine.stepInstruction(myTask.getProc().getTasks());
                return;
              }
          }
      }
  }
  
  
  /*****************************
   * Observer Classes          *
   *****************************/

  /**
   * Each task belonging to the process executes this in a synchronized manner.
   * The innermost StackFrame is generated for the task, which is properly
   * blocked by the time it gets here. This Task's TID and its StackFrame's 
   * information is stored into the global matrix for assertions later. 
   * 
   * @param task    The Task to save a stack trace from. 
   */
  public synchronized void handleTask (Task task)
  {
    Frame frame = null;
    myTask = task;
    
    if (task != null)
      {
        frame = StackFactory.createFrame(task);
//        System.err.println(StackFactory.printStackTrace(frame));
        
        assertNotNull(frame);
        
        frameTracker[task_count][0][0] = "" + task.getTid();
        
        int i = 1;
        while (frame != null && i < 9)
          {
            frameTracker[task_count][i][0] = "" + frame.toString();
            
            if (frame.getLines().length > 0)
              frameTracker[task_count][i][1] = frame.getLines()[0].getFile().getAbsolutePath();
            else
              frameTracker[task_count][i][1] = "";
            
            frameTracker[task_count][i][2] = frame.getSymbol().getDemangledName();
            
            if (frame.getInner() == null)
              frameTracker[task_count][i][3] = "";
            else
              frameTracker[task_count][i][3] = "" + frame.getInner().toString();
            
            if (frame.getLines().length > 0)
              frameTracker[task_count][i][4] = "" + frame.getLines()[0].getLine();
            else
              frameTracker[task_count][i][4] = "" + 0;
            
            frame = frame.getOuter();
            i++;
          }
        
        ++task_count;
        
        /* All three tasks have gone through - wake the test and finish up */
        if (task_count == 3)
          {
            frameAssertions();
            return;
          }
      }
    else
      return;
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
//      System.err.println("LockObserer.update " + arg + " " + test);
      if (arg == null)
        return;
      
      Task task = (Task) arg;
      
      if (test == 1)
	{
	  firstTestBacktraceAssertions();
	  return;
	}
      else if (test == 2)
	{
	  Iterator i = task.getProc().getTasks().iterator();
	  task_count = 0;
	  while (i.hasNext())
	    {
	      handleTask((Task) i.next());
	    }
//	  handleTask((Task) arg);
	  return;
	}
      

      if (task.getTid() == task.getProc().getPid())
        myTask = task;
      else
        {
          Proc p = (Proc) task.getProc();
          Iterator i = p.getTasks().iterator();
          while (i.hasNext())
            {
              Task t = (Task) i.next();
              if (t.getTid() == p.getPid())
                {
                  myTask = t;
                  break;
                }
            }
        }
      
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
                pushPopAssertions();
            }
        }
      });
    }
    
  } 
}
