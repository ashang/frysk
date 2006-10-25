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

import frysk.EventLogger;
import frysk.event.Event;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcBlockObserver;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;

import inua.util.PrintWriter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.logging.Logger;

public class FStack
{
  private PrintWriter writer;

  private TreeMap sortedTasks;

  private Proc proc;

  public StackTasksObserver procObserver;

  protected static final Logger logger = EventLogger.get("logs/",
                                                         "frysk_core_event.log");

  public void setWriter (PrintWriter writer)
  {
    this.writer = writer;
  }

  public void scheduleStackAndRunEvent (Proc p, Event theEvent)
  {
    proc = p;
    if (proc == null)
      {
        System.out.println("Couldn't get the proc");
        System.exit(1);
      }

    boolean isOwned = (this.proc.getUID() == Manager.host.getSelf().getUID() || this.proc.getGID() == Manager.host.getSelf().getGID());

    if (! isOwned)
      {
        System.err.println("Process " + proc
                           + " is not owned by user/group. Cannot stacktrace.");
        System.exit(- 1);
      }

    procObserver = new StackTasksObserver(proc, theEvent);
  }

  public final void storeTask (Task task)
  {
    if (task != null)
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

            if (sortedTasks == null)
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
      extends ProcBlockObserver
  {
    private LinkedList taskList;

    private Event event;

    public StackTasksObserver (Proc proc, Event theEvent)
    {
      super(proc);
      taskList = proc.getTasks();
      event = theEvent;
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

          // Run the given Event.
          Manager.eventLoop.add(event);
        }
    }

    public void addFailed (Object observable, Throwable w)
    {
      System.err.println(w);
      Manager.eventLoop.requestStop();
      System.exit(2);
    }

    public void deletedFrom (Object observable)
    {
    }

  }
}
