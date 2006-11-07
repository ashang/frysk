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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.EventLogger;
import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.event.SignalEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcBlockObserver;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;
import frysk.sys.Sig;

public class StacktraceObserver
    extends ProcBlockObserver
{
  private LinkedList taskList;

  protected String stackTrace= new String();

  private TreeMap sortedTasks;

  private Event event;

  protected static final Logger logger = EventLogger.get("logs/",
                                                         "frysk_core_event.log");

  /**
   * Runs a stacktrace on the given process.
   * 
   * @param theProc the process to run the stack trace on.
   * @param theEvent an event to run on completion of the stack trace. For
   *          example: Stop the eventLoop and exit the program.
   */
  public StacktraceObserver (Proc theProc, Event theEvent)
  {
    super(theProc);
    taskList = proc.getTasks();
    event = theEvent;

    Manager.eventLoop.add(new InterruptEvent(proc));
  }

  public final void existingTask (Task task)
  {

    logger.log(Level.FINE, "{0} existingTask", this);

    // Remove this task from the list of tasks we have to deal with.
    taskList.remove(task);

    // Print the stack frame for this stack.
    storeTask(task);

    /*
     * If the taskList is empty we have dealt with all the necessary tasks.
     */
    logger.log(Level.FINEST, "{0} this taskList, {1} proc.taskList",
               new Object[] { taskList, proc.getTasks() });
    if (taskList.isEmpty())
      {
        // Print all the tasks in order.
        printTasks();

        // Run the given Event.
        Manager.eventLoop.add(event);
      }
  }

  public void taskAddFailed (Object observable, Throwable w)
  {
    logger.log(Level.SEVERE, "{0} could not be added to {1}",
               new Object[] { this, observable });

    Task task = (Task) observable;

    // Remove this task from the list of tasks we have to deal with.
    taskList.remove(task);
    if (taskList.containsAll(proc.getTasks()))
      {
        // Print all the tasks in order.
        printTasks();

        // Run the given Event.
        Manager.eventLoop.add(event);
      }
  }

  public void deletedFrom (Object observable)
  {
  }

  public final void printTasks ()
  {
    logger.log(Level.FINE, "{0} printTasks", this);
    Iterator iter = sortedTasks.values().iterator();
    while (iter.hasNext())
      {
        LinkedList output = (LinkedList) iter.next();
        Iterator i = output.iterator();
        while (i.hasNext())
          {
	   stackTrace = stackTrace.concat((String) i.next()+"\n");
          }
      }
    logger.log(Level.FINE, "{0} exiting printTasks", this);
  }

  public final String toPrint ()
  {
    logger.log(Level.FINE, "{0} toPrint, stackTrace: {1}", new Object[] { this,
                                                                     stackTrace });
    return stackTrace;
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
                String output = "#" + count + " " + frame.toPrint();

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
            stackTrace.concat("... couldn't print stack trace\n");
          }
      }
  }

  /**
   * @author mcvet If the user cntl-c interrupts, handle it cleanly
   */
  static class InterruptEvent
      extends SignalEvent
  {
    Proc proc;

    public InterruptEvent (Proc theProc)
    {

      super(Sig.INT);
      proc = theProc;
      logger.log(Level.FINE, "{0} InterruptEvent\n", this);
    }

    public final void execute ()
    {
      logger.log(Level.FINE, "{0} execute\n", this);
      proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));
      try
        {
          Manager.eventLoop.join(5);
        }
      catch (InterruptedException e)
        {
          e.printStackTrace();
        }
      System.exit(1);

    }
  }

}
