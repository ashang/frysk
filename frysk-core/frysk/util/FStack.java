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

import inua.util.PrintWriter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;
import java.util.logging.Logger;

import frysk.EventLogger;

import frysk.event.RequestStopEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcAttachedObserver;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;

public class FStack
{
  private PrintWriter writer;

  private TreeMap sortedTasks;

  private Proc proc;

  public ProcAttachedObserver procAttachedObserver;

  protected static final Logger logger = EventLogger.get("logs/",
                                                         "frysk_core_event.log");

  public void setWriter (PrintWriter writer)
  {
    this.writer = writer;
  }

  public void run (int pid)
  {
    Manager.host.requestRefreshXXX(true);

    // XXX: Should get a message back when the refresh has finished and the
    // proc has been found.
    Manager.eventLoop.runPending();
    proc = Manager.host.getProc(new ProcId(pid));

    if (null == proc)
      {
        System.out.println("Couldn't get the proc");
        System.exit(1);
      }

    procAttachedObserver = new ProcAttachedObserver(
                                                    proc,
                                                    new StackTasksObserver(proc));
    Manager.eventLoop.start();
  }

  private final void removeObservers (Proc proc)
  {
    proc.requestRemoveAllObservations();
    proc.observableDetached.addObserver(new Observer()
    {

      public void update (Observable o, Object arg)
      {
        Manager.eventLoop.add(new RequestStopEvent(Manager.eventLoop));
      }
    });
  }

  public final void storeTask (Task task)
  {
    if (null != task)
      {
        try
          {
            LinkedList list = new LinkedList();
            list.add(new String("Task #" + task.getTid()));
            int count = 0;
            for (StackFrame frame = StackFactory.createStackFrame(task); frame != null; frame = frame.getOuter())
              {
                // FIXME: do valgrind-like '=== PID ===' ?
                String output = "#" + count + " 0x"
                                + Long.toHexString(frame.getAddress()) + " in "
                                + frame.getMethodName() + " ()";

                if (frame.getSourceFile() != null)
                  output = output + " from " + frame.getSourceFile();

                list.add(output);
                count++;
              }

            if (null == sortedTasks)
              sortedTasks = new TreeMap();

            sortedTasks.put(new Integer(task.getTid()), list);
          }
        catch (TaskException _)
          {
            // FIXME: log exception, or rethrow?
            writer.println("... couldn't print stack trace");
          }
      }
  }

  public final void printTasks ()
  {
    Iterator iter = sortedTasks.values().iterator();
    while (iter.hasNext())
      {
        LinkedList output = (LinkedList) iter.next();
        Iterator i = output.iterator();
        while (i.hasNext())
          {
            String s = (String) i.next();
            writer.println(s);
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
      storeTask(task);
      // Remove this task from the list of tasks.
      if (taskList.contains(task))
        {
          taskList.remove(task);
        }

      if (0 == taskList.size())
        {
          // Print all the tasks in order.
          printTasks();
          // Remove the observer from this proc.
          removeObservers(task.getProc());
        }
    }

    public void taskAdded (Task task)
    {
      // TODO Auto-generated method stub

    }

    public void taskRemoved (Task task)
    {
      // TODO Auto-generated method stub

    }

    public void addFailed (Object observable, Throwable w)
    {
      // TODO Auto-generated method stub
      System.err.println(w);
      Manager.eventLoop.requestStop();
      System.exit(2);
    }

    public void addedTo (Object observable)
    {
      // TODO Auto-generated method stub

    }

    public void deletedFrom (Object observable)
    {
       Iterator iter = proc.observationsIterator();
       while (iter.hasNext())
       {
       System.out.println(iter.next());
       }
       System.out.println();
    }

  }
}
