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

package frysk.proc;

/**
 * Superclass for tests that examine process state at a system call.
 */

import java.util.Observer;
import java.util.Observable;
import java.util.logging.Level;

public class SyscallExaminer
  extends TestLib 
{
  //  public TestExamineSyscall() 
  //  {
  //    super();
  //  }

  // Timers, observers, counters, etc.. needed for the test.
  public class Tester
  {
    volatile int stoppedTaskEventCount;
    volatile int syscallTaskEventCount;
    volatile int syscallState;
    volatile boolean exited;
    volatile int exitedTaskEventStatus;
	
    // Need to add task observers to the process the moment it is
    // created, otherwize the creation of the very first task is
    // missed (giving a mismatch of task created and deleted
    // notifications.)
	
    class TaskEventObserver
      extends TaskObserverBase
      implements TaskObserver.Syscall, TaskObserver.Signaled
    {
      public Action updateSyscallEnter (Task task)
      {
	syscallState = 1;
	return Action.CONTINUE;
      }
      public Action updateSyscallExit (Task task)
      {
	syscallState = 0;
	return Action.CONTINUE;
      }
      public Action updateSignaled (Task task, int sig)
      {
	stoppedTaskEventCount++;
	return Action.CONTINUE;
      }
    }
	
    class ProcDestroyedObserver
      implements Observer
    {
      volatile int count;
      public void update (Observable o, Object obj)
      {
	count++;
	Proc process = (Proc) obj;
	if (isChildOfMine (process)) {
	  syscallState ^= 1;  // we won't return from exit syscall
	  exited = true;
	  Manager.eventLoop.requestStop ();
	}
      }
    }
    
    Tester ()
    {
      host.observableProcRemovedXXX.addObserver
	(new ProcDestroyedObserver ());
    }

    public void addTaskAddedObserver(TaskAddedObserver obs) 
    {
      host.observableTaskAddedXXX.addObserver (obs);
    }
  }

  public class TaskAddedObserver implements Observer 
  {
    public void update (Observable o, Object obj)
    {
      Task task = (Task) obj;
      if (!isChildOfMine (task.proc))
	return;
      logger.log(Level.FINEST, "Registering pid {0} for kill", 
		 new Integer(task.getTid()));
    }
  }
}

