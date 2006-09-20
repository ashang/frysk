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

import inua.util.PrintWriter;
import java.util.LinkedList;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;

class fstack
{
  private PrintWriter writer;

  private Proc proc;

  private int pid;

  private ProcTasksObserver procTasksObserver;

  public static void main (String[] args)
  {
    
    fstack stacker = new fstack();
    stacker.setWriter(new PrintWriter(System.out, true));
    stacker.run(args);
  }

  private void setWriter (PrintWriter writer)
  {
    this.writer = writer;
  }

  private void run (String[] args)
  {    
    // Logger logger = EventLogger.get ("logs/", "frysk_core_event.log");
    // Handler handler = new ConsoleHandler ();
    // handler.setLevel(Level.FINEST);
    // logger.addHandler(handler);
    // logger.setLevel(Level.ALL);
    // LogManager.getLogManager().addLogger(logger);

    // FIXME: Implement option parsing, classpath option parser stuff.
    if (args.length == 0)
      {
        writer.println("Usage: fstack <pid>");
        return;
      }
    // Get the process from a pid.
    pid = Integer.parseInt(args[0]);
    Manager.host.requestRefreshXXX(true);

    // XXX: Should get a message back when the refresh has finished and the
    // proc has been found.
    Manager.eventLoop.runPending();
    proc = Manager.host.getProc(new ProcId(pid));

    if (null == proc) {
	System.out.println("Couldn't get the proc");
	System.exit(1);
}

    procTasksObserver = new ProcTasksObserver(proc, new StackTasksObserver(proc));
    Manager.eventLoop.start();
  }  

  private final  void removeObservers(Task task) 
  {
	task.requestDeleteClonedObserver(procTasksObserver);
	//task.requestDeleteTerminatedObserver(procTasksObserver);
  }

  public final void handleTask (Task task, PrintWriter writer)
  {
    if (null != task)
      {
        // writer.println("Stack trace for task " + task);
        writer.println("Task #"+ task.getTid());
        try
          {
            int count = 0;
            for (StackFrame frame = StackFactory.createStackFrame(task); frame != null; frame = frame.getOuter())
              {
                // FIXME: do valgrind-like '=== PID ===' ?
                writer.println("#" + count + " 0x"
                               + Long.toHexString(frame.getAddress()) + " in "
                               + frame.getMethodName() + " from "
                               + frame.getSourceFile());
                count++;
              }
          }
        catch (TaskException _)
          {
            // FIXME: log exception, or rethrow?
            writer.println("... couldn't print stack trace");
          }
      }
  }

  private class StackTasksObserver
      implements ProcObserver.ProcTasks
  {
    private LinkedList taskList;

    public StackTasksObserver (Proc proc)
    {
      taskList = proc.getTasks();
    }

    public void existingTask (Task task)
    {
      
      // Print the stack frame for this stack.
      handleTask(task, writer);    
// Remove this task from the list of tasks.
      if (taskList.contains(task)) {
        taskList.remove(task);
       }


      if (0 == taskList.size())
        {
           removeObservers(task);
        }  
      
    }

    public void taskAdded (Task task)
    {
      // TODO Auto-generated method stub

	//Bonus task:
	handleTask(task, writer);
	removeObservers(task);

	if (taskList.contains(task)) {
		taskList.remove(task);
	}

	if (0 == taskList.size())
	{
		removeObservers(task);
	}
    }

    public void taskRemoved (Task task)
    {
      // TODO Auto-generated method stub
      if (taskList.contains(task))
        {
          System.out.println("Task was removed before stack trace could occur");
	  
          taskList.remove(task);
        }

	if (0  == taskList.size())
	  {
	     removeObservers(task);
	  }

	//Should I be able to print a stack frame here?
	  handleTask(task, writer);

	//Do I need to remove the observer from a dead task?
	removeObservers(task);
    }

    public void addFailed (Object observable, Throwable w)
    {
      // TODO Auto-generated method stub

    }

    public void addedTo (Object observable)
    {
      // TODO Auto-generated method stub

    }

    public void deletedFrom (Object observable)
    {
      // TODO Auto-generated method stub
	System.out.println("Shutting down");
	Manager.eventLoop.requestStop();
    }

  }  
}
