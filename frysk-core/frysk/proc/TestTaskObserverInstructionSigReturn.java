// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.proc;

import java.io.File;

import frysk.Config;
import frysk.sys.Sig;

public class TestTaskObserverInstructionSigReturn
  extends TestLib
  implements TaskObserver.Attached,
  TaskObserver.Instruction,
  TaskObserver.Terminating,
  TaskObserver.Signaled
{
  // Counter for instruction observer hits.
  long hit;

  // How the process exited.
  int exit;

  // What process task are we talking about?
  Task task;

  // How many times have we been signaled?
  int signaled;

  public void testStepSigReturn()
  {
    // Init.
    hit = 0;
    signaled = 0;
    exit = -1;

    // Start and attach.
    String command = new File(Config.getPkgLibDir(), "funit-alarm").getPath();
    Manager.host.requestCreateAttachedProc(new String[] {command}, this);
    assertRunUntilStop("Creating process");

    // Add a terminated and signaled observer.
    task.requestAddTerminatingObserver(this);
    task.requestAddSignaledObserver(this);
    task.requestUnblock(this);
    assertRunUntilStop("Waiting for first PROF signal");

    // Add a stepping observer.
    task.requestAddInstructionObserver(this);
    task.requestUnblock(this);
    assertRunUntilStop("Stepping through till completion");

    assertTrue("steps were made",
	       hit > 5 * signaled /* random very low bound guess really */);
    assertEquals("signaled", 3, signaled);
    assertEquals("process exited nicely", 0, exit);
  }

  // Common interface methods
  public void addedTo (Object observable)
  {
    // ignored
  }

  public void addFailed (Object observable, Throwable w)
  {
    w.printStackTrace();
  }

  public void deletedFrom (Object observable)
  {
    // ignored
  }

  // TaskObserver.Attached interface
  public Action updateAttached(Task task)
  {
    this.task = task;
    Manager.eventLoop.requestStop();
    return Action.BLOCK;
  }

  // TaskObserver.Instruction interface
  public Action updateExecuted(Task task)
  {
    hit++;
    return Action.CONTINUE;
  }

  // TaskObserver.Terminated interface
  public Action updateTerminating(Task task, boolean signal, int exit)
  {
    Manager.eventLoop.requestStop();

    this.exit = exit;
    return Action.CONTINUE;
  }

  // TaskObserver.Signaled interface
  public Action updateSignaled (Task task, int signal)
  {
    if (signal != Sig.PROF_)
      fail("Wrong signal received: " + signal);
    
    signaled++;
    if (signaled == 1)
      {
	Manager.eventLoop.requestStop();
	return Action.BLOCK;
      }

    return Action.CONTINUE;
  }
}
