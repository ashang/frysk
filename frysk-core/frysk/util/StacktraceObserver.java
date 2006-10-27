

package frysk.util;

import inua.util.PrintWriter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.EventLogger;
import frysk.event.Event;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcBlockObserver;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;

public class StacktraceObserver
    extends ProcBlockObserver
{
  private LinkedList taskList;

  protected PrintWriter writer;

  private TreeMap sortedTasks;

  private Event event;

  protected static final Logger logger = EventLogger.get("logs/",
                                                         "frysk_core_event.log");

  /**
   *  Runs a stacktrace on the given process.
   *  @param theProc the process to run the stack trace on.
   *  @param theEvent an event to run on completion of the stack trace. 
   *  For example: Stop the eventLoop and exit the program.
   *  @param theWriter the PrintWriter to send output to.
   */
  public StacktraceObserver (Proc theProc, Event theEvent, PrintWriter theWriter)
  {
    super(theProc);

    writer = theWriter;   
    taskList = proc.getTasks();
    event = theEvent;
  }

  public final void existingTask (Task task)
  {

    logger.log(Level.FINE, "{0} existingTask", this);
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
            String s = (String) i.next();
            writer.println(s);
          }
      }
    logger.log(Level.FINE, "{0} exiting printTasks", this);
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
}
