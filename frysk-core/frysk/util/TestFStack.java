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


package frysk.util;

import java.io.IOException;

import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;

public class TestFStack
    extends TestLib
{

  String mainThread = "Task #\\d+\\n"
                      + "#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)\\n"
                      + "#1 0x[\\da-f]+ in (__)?sigsuspend \\(\\)\\n"
                      + "#2 0x[\\da-f]+ in server \\(\\): line #249\\n"
                      + "#3 0x[\\da-f]+ in main \\(\\): line #505\\n"
                      + "#4 0x[\\da-f]+ in __libc_start_main \\(\\)\\n"
                      + "#5 0x[\\da-f]+ in _start \\(\\)\\n";

  String secondaryThread = "Task #\\d+\\n"
                           + "#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)\\n"
                           + "#1 0x[\\da-f]+ in (__)?sigsuspend \\(\\)\\n"
                           + "#2 0x[\\da-f]+ in server \\(\\): line #249\\n"
                           + "#3 0x[\\da-f]+ in start_thread \\(\\)\\n"
                           + "#4 0x[\\da-f]+ in (__)?clone \\(\\)\\n";

  public void testSingleThreadedDetached () throws IOException
  {
    AckProcess ackProc = new DetachedAckProcess();
    multiThreaded(ackProc, 1);
  }

  public void testSingleThreadedAckDaemon () throws IOException
  {
    AckProcess ackProc = new AckDaemonProcess();
    multiThreaded(ackProc, 1);
  }

  public void testMultiThreadedDetached () throws IOException
  {
    AckProcess ackProc = new DetachedAckProcess(2);
    multiThreaded(ackProc, 3);
  }

  public void testMultiThreadedAckDaemon () throws IOException
  {
    AckProcess ackProc = new AckDaemonProcess(2);
    multiThreaded(ackProc, 3);
  }

  public void testStressMultiThreadedDetach () throws IOException
  {
    int clones = 7;
    AckProcess ackProc = new DetachedAckProcess(clones);
    multiThreaded(ackProc, clones + 1);
  }

  public void multiThreaded (AckProcess ackProc, int threads)
      throws IOException
  {

    final Proc proc = ackProc.findProcUsingRefresh(true);

    StacktraceObserver stacker = new StacktraceObserver(proc, new Event()
    {

      public void execute ()
      {
        proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));
      }
    });

    assertRunUntilStop("test");

    String regex = new String();
    regex += mainThread;

    for (int i = 1; i < threads; i++)
      {
        regex += secondaryThread;
      }

    assertTrue(stacker.toPrint() + "did not match: " + regex,
               stacker.toPrint().matches(regex));

  }

}
