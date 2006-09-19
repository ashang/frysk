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

class FStack
{
  private static PrintWriter writer;

  private static Proc proc;

  private static int pid;

  public static void main (String[] args)
  {
    FStack stacker = new FStack();
    stacker.run(args);
  }

  private void run (String[] args)
  {
    writer = new PrintWriter(System.out, true);

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

    System.out.println("Finding the proc");
    Manager.host.requestRefreshXXX(true);

    // XXX: Should get a message back when the refresh has finished and the
    // proc has been found.
    Manager.eventLoop.runPending();
    proc = Manager.host.getProc(new ProcId(pid));

    System.out.println("Proc is: " + proc);

    new ProcTasksObserver(proc, new StackTasksObserver(proc));
    // proc.getMainTask().requestAddAttachedObserver(
    // new ProcAttachedObserver(proc));
    Manager.eventLoop.start();
  }

  private static class StackTasksObserver
      implements ProcObserver.ProcTasks
  {
    // private Proc proc;

    private LinkedList taskList;

    int counter;

    public StackTasksObserver (Proc proc)
    {
      // this.proc = proc;
      taskList = proc.getTasks();
      counter = 2;
    }

    public void existingTask (Task task)
    {
      // Remove this task from the list of tasks.
      if (taskList.contains(task))
        taskList.remove(task);

      // Print the stack frame for this stack.

      
      
      handleTask(task, writer);

      if (0 == taskList.size())
        {
          Manager.eventLoop.requestStop();
        }
    }

    public void taskAdded (Task task)
    {
      // TODO Auto-generated method stub

    }

    public void taskRemoved (Task task)
    {
      // TODO Auto-generated method stub
      if (taskList.contains(task))
        {
          System.out.println("Task was removed before stacktrace could occur");
          taskList.remove(task);
        }
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

    }

  }

  public final static void handleTask (Task task, PrintWriter writer)
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

}
