// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

import java.util.logging.Level;

import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;

public class StressTestFStack
    extends TestLib
{

  static String mainClone = "Task #\\d+\n"
                            + "(#[\\d]+ 0x[\\da-f]+ in .*\n)*"
                            + "#[\\d]+ 0x[\\da-f]+ in __libc_start_main \\(\\)\n"
                            + "#[\\d]+ 0x[\\da-f]+ in _start \\(\\)\n";

  static String clone = "Task #\\d+\n" + "(#[\\d]+ 0x[\\da-f]+ in .*\n)*";

  public void testStressMultiThreadedDetach ()
  {
    int clones = 20;
    AckProcess ackProc = new DetachedAckProcess(clones);
    TestFStack.multiThreaded(ackProc, clones);
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
    })
    {

      public void addFailed (Object observable, Throwable w)
      {
        fail("Proc add failed" + w.getMessage());
      }
    };
    assertRunUntilStop("perform backtrace");

    String regex = new String();

    regex += "(" + mainClone + ")(" + clone + ")*";

    String result = stacker.toPrint();
    logger.log(Level.FINE, result);
    assertTrue(result + "should match: " + regex, result.matches(regex));

  }

}
