// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
import frysk.proc.Action;
import frysk.proc.MachineType;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.proc.TaskObserver;
import frysk.proc.TestLib;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;
import frysk.sys.proc.MapsBuilder;
import frysk.sys.Sig;
import frysk.core.Build;

public class TestStackBacktrace
    extends TestLib
{
  private Task myTask;

  public void testBacktrace () throws TaskException
  {
     // Backtraces only work on x86 for now.
     if (MachineType.getMachineType() != MachineType.IA32)
       {
	  brokenXXX(2936);
	  return;
       }
    
    class TaskCreatedObserver extends TaskObserverBase
        implements TaskObserver.Attached
    {

      public Action updateAttached (Task task)
      {
        // TestStackBacktrace.this.myTask = task;
        Manager.eventLoop.requestStop();
        return Action.BLOCK;
      }
    }

    TaskCreatedObserver obs = new TaskCreatedObserver();
    AckDaemonProcess process = new AckDaemonProcess(
                                                    Sig.POLL,
                                                    new String[] {
                                                                  Build.ABS_BUILDDIR + "/frysk/pkglibexecdir/funit-rt-looper",
                                                                      ""+ frysk.rt.tests.TestLib.getMyPid(),
                                                                      ""+ Sig.POLL_});
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
                            boolean permExecute, boolean permPrivate,
                            long offset, int devMajor, int devMinor, int inode,
                            int pathnameOffset, int pathnameLength)
      {
        ByteBuffer buffer = myTask.getMemory();

        for (long i = addressLow; i < addressHigh; i++)
          {
            System.err.println(Long.toHexString(i) + " is in the Mmap!");
            buffer.getByte(i);
          }
      }

    }

//     MyBuilder builder = new MyBuilder();
//     System.out.println("Before maps test");
//     builder.construct(myTask.getTid());
//     System.out.println("After maps test");

    StackFrame frame = StackFactory.createStackFrame(myTask);

    assertNotNull(frame);

    //int level = 0;
    while (frame != null)
      {
//        System.out.println("Frame " + (++level));
//        System.out.println("\tFile: " + frame.getSourceFile());
//        System.out.println("\tFunc: " + frame.getMethodName());
//        System.out.println("\tLine: " + frame.getLineNumber());
//        System.out.println("\tCol: " + frame.getColumn());
//        System.out.println("\tAddr: " + frame.getAddress());
        frame = frame.getOuter();
      }

//     MyBuilder builder2 = new MyBuilder();
//     System.out.println("Before maps test");
//     builder2.construct(myTask.getTid());
//     System.out.println("After maps test");
  }

}
