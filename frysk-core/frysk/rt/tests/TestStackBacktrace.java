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

import inua.eio.ByteBuffer;
import inua.eio.ULong;
import frysk.proc.Action;
import frysk.proc.Host;
import frysk.proc.MachineType;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.proc.TaskObserver;
import frysk.proc.TestLib;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;
import frysk.sys.proc.MapsBuilder;
import frysk.sys.Sig;
import frysk.junit.Paths;

public class TestStackBacktrace
    extends TestLib
{
  private Task myTask;
  
  private int task_count = 0;
  
  /*
   *    [frame.getLineNumber()]
   * [frame.getInner().toString()]
   *    [frame.getMethodName()]
   *    [frame.getSourceFile()]
   *            ^
   *            |  ...   ...          
   * [TID] -> [f0|f1|f2|f3...]
   * [TID] -> ....
   */
  private String[][][] frameTracker = new String[3][9][5];

  public void testBacktrace () throws TaskException
  {
    
    // Backtraces only work on x86 and x86_64 for now.
    if (MachineType.getMachineType() == MachineType.PPC
        || MachineType.getMachineType() == MachineType.PPC64)
      {
        brokenXXX(3277);
        return;
      }

    TaskCreatedObserver obs = new TaskCreatedObserver();
    AckDaemonProcess process = new AckDaemonProcess
	(Sig.POLL, new String[] {
	    Paths.getExecPrefix () + "/funit-rt-looper",
	    "" + frysk.rt.tests.TestLib.getMyPid(),
	    "" + Sig.POLL_
	});
    
    myTask = process.findTaskUsingRefresh(true);
    assertNotNull(myTask);
    myTask.requestAddAttachedObserver(obs);

    assertRunUntilStop("Attempting to add observer");

    class MyBuilder
        extends MapsBuilder
    {

      public void buildBuffer (byte[] maps)
      {
        maps[maps.length - 1] = 0;
      }

      public void buildMap (long addressLow, long addressHigh,
                            boolean permRead, boolean permWrite,
                            boolean permExecute, boolean permPrivate, boolean permShared,
                            long offset, int devMajor, int devMinor, int inode,
                            int pathnameOffset, int pathnameLength)
      {
        ByteBuffer buffer = myTask.getMemory();

        for (long i = addressLow; ULong.LT(i, addressHigh); i++)
          {
            System.err.println(Long.toHexString(i) + " is in the Mmap!");
            buffer.getByte(i);
          }
      }

    }

    // MyBuilder builder = new MyBuilder();
    // System.out.println("Before maps test");
    // builder.construct(myTask.getTid());
    // System.out.println("After maps test");

    StackFrame frame = StackFactory.createStackFrame(myTask);

    assertNotNull(frame);

    assertTrue(frame.getSourceFile().endsWith(
                                              "/frysk/pkglibexecdir/funit-rt-looper.c"));
    assertEquals("baz", frame.getMethodName());
    assertNull(frame.getInner());
    
    if (!brokenXXX(3259))
      assertEquals(62, frame.getLineNumber());

    frame = frame.getOuter();
    assertTrue(frame.getSourceFile().endsWith(
                                              "/frysk/pkglibexecdir/funit-rt-looper.c"));
    assertEquals("bar", frame.getMethodName());
    assertNotNull(frame.getInner());
    assertEquals(71, frame.getLineNumber());

    frame = frame.getOuter();
    assertTrue(frame.getSourceFile().endsWith(
                                              "/frysk/pkglibexecdir/funit-rt-looper.c"));
    assertEquals("foo", frame.getMethodName());
    assertNotNull(frame.getInner());
    assertEquals(81, frame.getLineNumber());

    frame = frame.getOuter();
    assertTrue(frame.getSourceFile().endsWith(
                                              "/frysk/pkglibexecdir/funit-rt-looper.c"));
    assertEquals("main", frame.getMethodName());
    assertNotNull(frame.getInner());
    assertEquals(117, frame.getLineNumber());

    frame = frame.getOuter();
    assertNull(frame.getSourceFile());
    assertEquals("__libc_start_main", frame.getMethodName());
    assertNotNull(frame.getInner());
    assertEquals(0, frame.getLineNumber());

    frame = frame.getOuter();
    assertNull(frame.getSourceFile());
    assertEquals("_start", frame.getMethodName());
    assertNotNull(frame.getInner());
    assertEquals(0, frame.getLineNumber());

    frame = frame.getOuter();

    assertNull(frame);

    // MyBuilder builder2 = new MyBuilder();
    // System.out.println("Before maps test");
    // builder2.construct(myTask.getTid());
    // System.out.println("After maps test");
  }

  
  public synchronized void testThreadedBacktrace () throws TaskException
  {
    
    // Backtraces only work on x86 and x86_64 for now.
    if (MachineType.getMachineType() == MachineType.PPC
        || MachineType.getMachineType() == MachineType.PPC64)
      {
        brokenXXX(3277);
        return;
      }

    AckDaemonProcess process = new AckDaemonProcess
	(Sig.POLL, new String[] {
	    Paths.getExecPrefix () + "/funit-rt-threader",
	    "" + frysk.rt.tests.TestLib.getMyPid(),
	    "" + Sig.POLL_
	});
    
    myTask = process.findTaskUsingRefresh(true);
    
    Manager.host.requestFindProc(new ProcId(process.getPid()), new Host.FindProc() {

      public void procFound (ProcId procId)
      {
        Proc proc = Manager.host.getProc(procId);
        new ProcTasksObserver(proc, new StackTasksObserver());
      }

      public void procNotFound (ProcId procId, Exception e)
      {
      }});   
    
    assertRunUntilStop("testThreadedBackTrace");    
  
    frameAssertions();
  }
  
  /**
   * Sort the matrix by TID and compare its contents to the actual source file.
   */
  public void frameAssertions()
  {
    
    int tid = Integer.parseInt(this.frameTracker[0][0][0]);
    int lowest = 0;
    int next = 0;
    int last = 0;
    int temp = 0;
    
    /* Find the numerically lowest TID (== main task) because these tasks may
     * have come in any order, and we need to know which task is which to
     * perform assertions */
    for (int i = 0; i < 3; i++)
      {
        temp = Integer.parseInt(this.frameTracker[i][0][0]);
        if (temp < tid)
          {
            tid = temp;
            lowest = i;
          }
      }
    
    /* Main thread assertions */
    
    assertTrue(this.frameTracker[lowest][1][1].endsWith("/frysk/pkglibexecdir/funit-rt-threader.c"));
    assertEquals("bak_two", this.frameTracker[lowest][1][2]);
    assertNotNull(this.frameTracker[lowest][1][3]);
    assertEquals(71, Integer.parseInt(this.frameTracker[lowest][1][4]));
    
    assertTrue(this.frameTracker[lowest][2][1].endsWith("/frysk/pkglibexecdir/funit-rt-threader.c"));
    assertEquals("bak", this.frameTracker[lowest][2][2]);
    assertNotNull(this.frameTracker[lowest][2][3]);
    assertEquals(81, Integer.parseInt(this.frameTracker[lowest][2][4]));
    
    assertTrue(this.frameTracker[lowest][3][1].endsWith("/frysk/pkglibexecdir/funit-rt-threader.c"));
    assertEquals("baz_two", this.frameTracker[lowest][3][2]);
    assertNotNull(this.frameTracker[lowest][3][3]);
    assertEquals(91, Integer.parseInt(this.frameTracker[lowest][3][4]));
    
    assertTrue(this.frameTracker[lowest][4][1].endsWith("/frysk/pkglibexecdir/funit-rt-threader.c"));
    assertEquals("bar_two", this.frameTracker[lowest][4][2]);
    assertNotNull(this.frameTracker[lowest][4][3]);
    assertEquals(105, Integer.parseInt(this.frameTracker[lowest][4][4]));
    
    assertTrue(this.frameTracker[lowest][5][1].endsWith("/frysk/pkglibexecdir/funit-rt-threader.c"));
    assertEquals("foo_two", this.frameTracker[lowest][5][2]);
    assertNotNull(this.frameTracker[lowest][5][3]);
    assertEquals(123, Integer.parseInt(this.frameTracker[lowest][5][4]));
    
    assertTrue(this.frameTracker[lowest][6][1].endsWith("/frysk/pkglibexecdir/funit-rt-threader.c"));
    assertEquals("main", this.frameTracker[lowest][6][2]);
    assertNotNull(this.frameTracker[lowest][6][3]);
    assertEquals(172, Integer.parseInt(this.frameTracker[lowest][6][4]));
    
    assertNull(this.frameTracker[lowest][7][1]);
    assertEquals("__libc_start_main", this.frameTracker[lowest][7][2]);
    assertNotNull(this.frameTracker[lowest][7][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[lowest][7][4]));
    
    assertNull(this.frameTracker[lowest][8][1]);
    assertEquals("_start", this.frameTracker[lowest][8][2]);
    assertNotNull(this.frameTracker[lowest][8][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[lowest][8][4]));
    
    
    /* Find the first thread the main thread created - the signal thread. */
    for (int i = 0; i < 3; i++)
      {
        temp = Integer.parseInt(this.frameTracker[i][0][0]);
        if (temp == (tid + 1))
          {
            next = i;
          }
      }
    
    /* Second thread assertions */
    
    assertTrue(this.frameTracker[next][1][1].endsWith("/frysk/pkglibexecdir/funit-rt-threader.c"));
    assertEquals("signal_parent", this.frameTracker[next][1][2]);
    assertNotNull(this.frameTracker[next][1][3]);
    assertEquals(63, Integer.parseInt(this.frameTracker[next][1][4]));
    
    assertNull(this.frameTracker[next][2][1]);
    assertEquals("start_thread", this.frameTracker[next][2][2]);
    assertNotNull(this.frameTracker[next][2][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[next][2][4]));
    
    assertNull(this.frameTracker[next][3][1]);
//    if (MachineType.getMachineType() == MachineType.IA32)
//      assertEquals("__clone", this.frameTracker[next][3][2]);
//    if (MachineType.getMachineType() == MachineType.X8664)
//      assertEquals("(__)?clone", this.frameTracker[next][3][2]);
    assertTrue(this.frameTracker[next][3][2].matches("(__)?clone"));
    assertNotNull(this.frameTracker[next][3][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[next][3][4]));
    
    
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
    
    assertTrue(this.frameTracker[last][2][1].endsWith("/frysk/pkglibexecdir/funit-rt-threader.c"));
    assertEquals("baz", this.frameTracker[last][2][2]);
    assertNotNull(this.frameTracker[last][2][3]);
    assertEquals(98, Integer.parseInt(this.frameTracker[last][2][4]));
    
    assertTrue(this.frameTracker[last][3][1].endsWith("/frysk/pkglibexecdir/funit-rt-threader.c"));
    assertEquals("bar", this.frameTracker[last][3][2]);
    assertNotNull(this.frameTracker[last][3][3]);
    assertEquals(116, Integer.parseInt(this.frameTracker[last][3][4]));
    
    assertTrue(this.frameTracker[last][4][1].endsWith("/frysk/pkglibexecdir/funit-rt-threader.c"));
    assertEquals("foo", this.frameTracker[last][4][2]);
    assertNotNull(this.frameTracker[last][4][3]);
    assertEquals(130, Integer.parseInt(this.frameTracker[last][4][4]));
    
    assertNull(this.frameTracker[next][2][1]);
    assertEquals("start_thread", this.frameTracker[next][2][2]);
    assertNotNull(this.frameTracker[next][2][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[next][2][4]));
    
    assertNull(this.frameTracker[next][3][1]);
//    if (MachineType.getMachineType() == MachineType.IA32)
//      assertEquals("__clone", this.frameTracker[next][3][2]);
//    if (MachineType.getMachineType() == MachineType.X8664)
//      assertEquals("(__)?clone", this.frameTracker[next][3][2]);
    assertTrue(this.frameTracker[next][3][2].matches("(__)?clone"));
    assertNotNull(this.frameTracker[next][3][3]);
    assertEquals(0, Integer.parseInt(this.frameTracker[next][3][4]));
  }
  
  
  /*****************************
   * Observer Classes          *
   *****************************/

  /**
   * Used by the single-threaded test - blocks the task and stops the event loop.
   */
  class TaskCreatedObserver
      extends TaskObserverBase
      implements TaskObserver.Attached
  {

    public synchronized Action updateAttached (Task task)
    {
      Manager.eventLoop.requestStop();
      return Action.BLOCK;
    }
  }

  /** 
   * Used by the multi-threaded test - blocks all tasks and retreives stack
   * information into the global matrix. 
   */
  private class StackTasksObserver
      implements ProcObserver.ProcTasks
  {

    public void existingTask (Task task)
    {
      handleTask(task);
    }

    public void taskAdded (Task task)
    {
    }

    public void taskRemoved (Task task)
    {
    }

    public void addFailed (Object observable, Throwable w)
    {
    }

    public void addedTo (Object observable)
    {
    }

    public void deletedFrom (Object observable)
    {
      Manager.eventLoop.requestStop();
    }

  }

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

    StackFrame frame = null;
    
    if (task != null)
      {
        try
        {
          frame = StackFactory.createStackFrame(task);
        }
        catch (TaskException te)
        {
          System.out.println(te.getMessage());
        }
        
        assertNotNull(frame);
        
        frameTracker[task_count][0][0] = "" + task.getTid();
        
        int i = 1;
        while (frame != null)
          {
            frameTracker[task_count][i][0] = "" + frame.toString();
            frameTracker[task_count][i][1] = frame.getSourceFile();
            frameTracker[task_count][i][2] = frame.getMethodName();
            
            if (frame.getInner() == null)
              frameTracker[task_count][i][3] = "";
            else
              frameTracker[task_count][i][3] = "" + frame.getInner().toString();
            
            frameTracker[task_count][i][4] = "" + frame.getLineNumber();
            
            frame = frame.getOuter();
            i++;
          }
        
        ++task_count;
        
        /* All three tasks have gone through - wake the test and finish up */
        if (task_count == 3)
          {
            Manager.eventLoop.requestStop();
            return;
          }
      }
    else
      return;

  }
}
