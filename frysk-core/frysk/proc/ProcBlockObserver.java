

package frysk.proc;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.event.Event;

abstract public class ProcBlockObserver
    implements  TaskObserver.Instruction, ProcObserver
{
  protected static final Logger logger = Logger.getLogger("frysk");

  private final Proc proc;
  
  private Task mainTask;

  public ProcBlockObserver (Proc theProc)
  {
    logger.log(Level.FINE, "{0} new\n", this);
    proc = theProc;

    // The rest of the construction must be done synchronous to
    // the EventLoop, schedule it.
    Manager.eventLoop.add(new Event()
    {
      public void execute ()
      {
        // Get a preliminary list of tasks - XXX: hack really.
        proc.sendRefresh();

        mainTask = Manager.host.get(new TaskId(proc.getPid()));
        if (mainTask == null)
          {
            logger.log(Level.FINE, "Could not get main thread of "
                                   + "this process\n {0}", proc);
            addFailed(
                      proc,
                      new RuntimeException(
                                           "Process lost: could not "
                                               + "get the main thread of this process.\n"
                                               + proc));
            return;
          }

        requestAddObservers(mainTask);
      }
    });
  }

  private void requestAddObservers (Task task)
  {

    task.requestAddInstructionObserver(this);
  }

  public Action updateExecuted (Task task)
  {
    existingTask(task);
    return Action.BLOCK;
  }

  abstract public void existingTask (Task task);

  abstract public void addFailed (Object observable, Throwable w);

  boolean isAdded = false;

  public void addedTo (Object observable)
  {
    if (! isAdded)
      {
        isAdded = true;
        // XXX: Is there a race here with a rapidly cloning task?
        for (Iterator iterator = proc.getTasks().iterator(); iterator.hasNext();)
          {
            Task task = (Task) iterator.next();
            //existingTask(task);
            if (task != mainTask)
              {
                logger.log(Level.FINE, "{0} Inside if not mainTask\n", this);
                requestAddObservers(task);
              }
          }
      }
  }

  abstract public void deletedFrom (Object observable);

}
