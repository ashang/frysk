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


package frysk.util;

import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;

import frysk.core.Build;

public class TestFStack
    extends TestLib
{

  String[] mainThread = {
                         "Task #\\d+\\n"
                             + "#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)\\n"
                             + "#1 0x[\\da-f]+ in (__)?sigsuspend \\(\\)\\n"
                             + "#2 0x[\\da-f]+ in "
                             + Build.ABS_SRCDIR
                             + "/frysk/pkglibexecdir/funit-child.c server \\(\\): line #249\\n"
                             + "#3 0x[\\da-f]+ in "
                             + Build.ABS_SRCDIR
                             + "/frysk/pkglibexecdir/funit-child.c main \\(\\): line #505\\n"
                             + "#4 0x[\\da-f]+ in __libc_start_main \\(\\)\\n"
                             + "#5 0x[\\da-f]+ in _start \\(\\)\\n",

                         "Task #\\d+\\n"
                             + "#0 0x[\\da-f]+ in (__)?sigsuspend \\(\\)\\n"
                             + "#1 0x[\\da-f]+ in "
                             + Build.ABS_SRCDIR
                             + "/frysk/pkglibexecdir/funit-child.c server \\(\\): line #249\\n"
                             + "#2 0x[\\da-f]+ in "
                             + Build.ABS_SRCDIR
                             + "/frysk/pkglibexecdir/funit-child.c main \\(\\): line #505\\n"
                             + "#3 0x[\\da-f]+ in __libc_start_main \\(\\)\\n"
                             + "#4 0x[\\da-f]+ in _start \\(\\)\\n",

                         "Task #\\d+\\n"
                             + "#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)\\n"
                             + "#1 0x[\\da-f]+ in syscall \\(\\)\\n"
                             + "#2 0x[\\da-f]+ in "
                             + Build.ABS_SRCDIR
                             + "/frysk/pkglibexecdir/funit-child.c tkill \\(\\): line #47\\n"
                             + "#3 0x[\\da-f]+ in "
                             + Build.ABS_SRCDIR
                             + "/frysk/pkglibexecdir/funit-child.c notify_manager \\(\\): line #126\\n"
                             + "#4 0x[\\da-f]+ in "
                             + Build.ABS_SRCDIR
                             + "/frysk/pkglibexecdir/funit-child.c server \\(\\): line #235\\n"
                             + "#5 0x[\\da-f]+ in "
                             + Build.ABS_SRCDIR
                             + "/frysk/pkglibexecdir/funit-child.c main \\(\\): line #505\\n"
                             + "#6 0x[\\da-f]+ in __libc_start_main \\(\\)\\n"
                             + "#7 0x[\\da-f]+ in _start \\(\\)\\n" };

  String[] secondaryThread = {
                              "Task #\\d+\\n"
                                  + "#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)\\n"
                                  + "#1 0x[\\da-f]+ in (__)?sigsuspend \\(\\)\\n"
                                  + "#2 0x[\\da-f]+ in "
                                  + Build.ABS_SRCDIR
                                  + "/frysk/pkglibexecdir/funit-child.c server \\(\\): line #249\\n"
                                  + "#3 0x[\\da-f]+ in start_thread \\(\\)\\n"
                                  + "#4 0x[\\da-f]+ in (__)?clone \\(\\)\\n",

                              "Task #\\d+\\n"
                                  + "#0 0x[\\da-f]+ in (__)?sigsuspend \\(\\)\\n"
                                  + "#1 0x[\\da-f]+ in "
                                  + Build.ABS_SRCDIR
                                  + "/frysk/pkglibexecdir/funit-child.c server \\(\\): line #249\\n"
                                  + "#2 0x[\\da-f]+ in start_thread \\(\\)\\n"
                                  + "#3 0x[\\da-f]+ in (__)?clone \\(\\)\\n",

                              "Task #\\d+\\n"
                                  + "#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)\\n"
                                  + "#1 0x[\\da-f]+ in syscall \\(\\)\\n"
                                  + "#2 0x[\\da-f]+ in "
                                  + Build.ABS_SRCDIR
                                  + "/frysk/pkglibexecdir/funit-child.c tkill \\(\\): line #47\\n"
                                  + "#3 0x[\\da-f]+ in "
                                  + Build.ABS_SRCDIR
                                  + "/frysk/pkglibexecdir/funit-child.c notify_manager \\(\\): line #126\\n"
                                  + "#4 0x[\\da-f]+ in "
                                  + Build.ABS_SRCDIR
                                  + "/frysk/pkglibexecdir/funit-child.c server \\(\\): line #235\\n"
                                  + "#5 0x[\\da-f]+ in start_thread \\(\\)\\n"
                                  + "#6 0x[\\da-f]+ in (__)?clone \\(\\)\\n" };

  String[] mainClone = {
                        "Task #\\d+\\n"
                            + "#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)\\n"
                            + "#1 0x[\\da-f]+ in __nanosleep_nocancel \\(\\)\\n"
                            + "#2 0x[\\da-f]+ in sleep \\(\\)\\n"
                            + "#3 0x[\\da-f]+ in "
                            + Build.ABS_SRCDIR
                            + "/frysk/pkglibexecdir/funit-child.c main \\(\\): line #177\\n"
                            + "#4 0x[\\da-f]+ in __libc_start_main \\(\\)\\n"
                            + "#5 0x[\\da-f]+ in _start \\(\\)\\n",

                        "Task #\\d+\\n"
                            + "#0 0x[\\da-f]+ in __nanosleep_nocancel \\(\\)\\n"
                            + "#1 0x[\\da-f]+ in sleep \\(\\)\\n"
                            + "#2 0x[\\da-f]+ in "
                            + Build.ABS_SRCDIR
                            + "/frysk/pkglibexecdir/funit-threads.c main \\(\\): line #177\\n"
                            + "#3 0x[\\da-f]+ in __libc_start_main \\(\\)\\n"
                            + "#4 0x[\\da-f]+ in _start \\(\\)\\n",

                        "Task #\\d+\\n"
                            + "#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)\\n"
                            + "#1 0x[\\da-f]+ in kill \\(\\)\\n"
                            + "#2 0x[\\da-f]+ in "
                            + Build.ABS_SRCDIR
                            + "/frysk/pkglibexecdir/funit-threads.c main \\(\\): line #175\\n"
                            + "#3 0x[\\da-f]+ in __libc_start_main \\(\\)\\n"
                            + "#4 0x[\\da-f]+ in _start \\(\\)\\n",

                        "Task  #\\d+\\n" + "#0 0x[\\da-f]+ in sleep \\(\\)\\n"
                            + "#1 0x[\\da-f]+ in __libc_start_main \\(\\)\\n"
                            + "#2 0x[\\da-f]+ in _start \\(\\)\\n"

  };

  String[] secondaryClone = {
                             "Task #\\d+\\n"
                                 + "#0 0x[\\da-f]+ in (__)?clone \\(\\)\\n"
                                 + "#1 0x[\\da-f]+ in "
                                 + Build.ABS_SRCDIR
                                 + "/frysk/pkglibexecdir/funit-threads.c op_clone \\(\\): line #105\\n"
                                 + "#2 0x[\\da-f]+ in start_thread \\(\\)\\n"
                                 + "#3 0x[\\da-f]+ in (__)?clone \\(\\)\\n",

                             "Task #\\d+\\n"
                                 + "#0 0x[\\da-f]+ in memset \\(\\)\\n"
                                 + "#1 0x[\\da-f]+ in "
                                 + Build.ABS_SRCDIR
                                 + "/frysk/pkglibexecdir/funit-threads.c op_clone \\(\\): line #105\\n"
                                 + "#2 0x[\\da-f]+ in start_thread \\(\\)\\n"
                                 + "#3 0x[\\da-f]+ in (__)?clone \\(\\)\\n",

                             "Task #\\d+\\n"
                                 + "#0 0x[\\da-f]+ in (__)?clone \\(\\)\\n"
                                 + "#1 0x[\\da-f]+ in "
                                 + Build.ABS_SRCDIR
                                 + "/frysk/pkglibexecdir/funit-threads.c main \\(\\): line #177\\n"
                                 + "#2 0x[\\da-f]+ in __libc_start_main \\(\\)\\n"
                                 + "#3 0x[\\da-f]+ in _start \\(\\)\\n",

                             "Task #\\d+\\n"
                                 + "#0 0x[\\da-f]+ in __kernel_vsyscall \\(\\)\\n"
                                 + "#1 0x[\\da-f]+ in pthread_join \\(\\)\\n"
                                 + "#2 0x[\\da-f]+ in "
                                 + Build.ABS_SRCDIR
                                 + "/frysk/pkglibexecdir/funit-threads.c op_clone \\(\\): line #100\\n"
                                 + "#3 0x[\\da-f]+ in start_thread \\(\\)\\n"
                                 + "#4 0x[\\da-f]+ in (__)?clone \\(\\)\\n",

                             "Task #\\d+\\n"
                                 + "#0 0x[\\da-f]+ in start_thread \\(\\)\\n"
                                 + "#1 0x[\\da-f]+ in "
                                 + Build.ABS_SRCDIR
                                 + "/frysk/pkglibexecdir/funit-threads.c (__)?clone \\(\\)\\n",

                             "Task #\\d+\\n"
                                 + "#0 0x[\\da-f]+ in "
                                 + Build.ABS_SRCDIR
                                 + "/frysk/pkglibexecdir/funit-threads.c (__)?clone \\(\\)\\n",

  };

  public void testSingleThreadedDetached ()
  {
    AckProcess ackProc = new DetachedAckProcess();
    multiThreaded(ackProc, 0);
  }

  public void testSingleThreadedAckDaemon ()
  {
    AckProcess ackProc = new AckDaemonProcess();
    multiThreaded(ackProc, 0);
  }

  public void testMultiThreadedDetached ()
  {
    AckProcess ackProc = new DetachedAckProcess(2);
    multiThreaded(ackProc, 2);
  }

  public void testMultiThreadedAckDaemon ()
  {
    AckProcess ackProc = new AckDaemonProcess(2);
    multiThreaded(ackProc, 2);
  }

  public void testStressMultiThreadedDetach ()
  {
    int clones = 7;
    AckProcess ackProc = new DetachedAckProcess(clones);
    multiThreaded(ackProc, clones);
  }

  public void multiThreaded (AckProcess ackProc, int numSecondaryThreads)
  {

    final Proc proc = ackProc.assertFindProcAndTasks();

    StacktraceAction stacker = new StacktraceAction(proc, new Event()
    {

      public void execute ()
      {
        proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));
      }
    });

    assertRunUntilStop("test");

    String regex = new String();
    regex += "(" + mainThread[0];
    for (int i = 1; i < mainThread.length; i++)
      {
        regex += "|" + mainThread[i];
      }

    regex += ")";
    regex += "(" + secondaryThread[0];

    for (int i = 1; i < secondaryThread.length; i++)
      {
        regex += "|" + secondaryThread[i];
      }

    regex += "){" + numSecondaryThreads + "}";

    String result = stacker.toPrint();
    assertTrue(result + "did not match: " + regex, result.matches(regex));

  }

  public void testClone ()
  {
    int threads = 2;
    AckProcess ackProc = new AckDaemonCloneProcess(threads);

    final Proc proc = ackProc.assertFindProcAndTasks();

    StacktraceAction stacker = new StacktraceAction(proc, new Event()
    {

      public void execute ()
      {
        proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));
      }
    });
    assertRunUntilStop("test");

    String regex = new String();

    regex += "(" + mainClone[0];

    for (int i = 1; i < mainClone.length; i++)
      {
        regex += "|" + mainClone[i];
      }

    regex += ")(" + secondaryClone[0];

    for (int i = 1; i < secondaryClone.length; i++)
      {
        regex += "|" + secondaryClone[i];
      }

    regex += ")*";

    String result = stacker.toPrint();
    assertTrue(result + "did not match: " + regex, result.matches(regex));
  }

}
