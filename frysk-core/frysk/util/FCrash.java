package frysk.util;

import inua.util.PrintWriter;

import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;

import frysk.event.RequestStopEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcAttachedObserver;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.Task;

public class FCrash
{

  private Proc proc;
  
  private int numProcesses = 0;

  public ProcAttachedObserver pao;
  
  PrintWriter writer;
  
  //True if we're tracing children as well.
  boolean traceChildren;
  
  //Set of ProcId objects we trace; if traceChildren is set, we also
  // look for their children.
  HashSet tracedParents = new HashSet();
  
  public void trace (String[] command)
  {
    init();
    Manager.host.requestCreateAttachedProc(command, new ProcAttachedObserver(proc, new TasksCrashObserver(proc)));
    Manager.host.requestRefreshXXX(true);
    Manager.eventLoop.start();
  }

  private void init ()
  {
    if (writer == null)
      writer = new PrintWriter(System.out);

    Manager.host.observableProcAddedXXX.addObserver(new Observer()
    {
      public void update (Observable observable, Object arg)
      {
        Proc proc = (Proc) arg;
        ProcId id = proc.getId();
        if (tracedParents.contains(id)
            || (traceChildren && tracedParents.contains(proc.getParent().getId())))
          {
            // In case we're tracing a new child, add it.
            tracedParents.add(proc.getId());
            // Weird API... unfortunately we can't fetch the
            // Proc's main task here, as it will be null. Instead
            // we have to request it and handle it in a callback.
            new ProcAttachedObserver(proc, new WaitForTask());
          }
      }
    });
  }
  
  synchronized void handleTask (Task task)
    {
        Proc proc = task.getProc();
        if (traceChildren)
            tracedParents.add(proc.getId());
        writer.flush();
        ++numProcesses;
    }

  public final void removeObservers (Proc proc)
  {
    proc.requestAbandon();
    proc.observableDetached.addObserver(new Observer()
    {
      public void update (Observable o, Object arg)
      {
        Manager.eventLoop.add(new RequestStopEvent(Manager.eventLoop));
      }
    });
  }
  
  public void setWriter (PrintWriter pw)
  {
    
  }
  
  public void setEnterHandler (StracePrinter sp)
  {
    
  }
  
  public void setExitHandler (StracePrinter sp)
  {
    
  }

  private class TasksCrashObserver
      implements ProcObserver.ProcTasks
  {

    public TasksCrashObserver (Proc proc)
    {
    }

    public void existingTask (Task task)
    {
      handleTask(task);
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
    }

  }
  
  class WaitForTask
      implements ProcObserver.ProcTasks
  {
    public void addedTo (Object arg0)
    {
    }

    public void addFailed (Object arg0, Throwable arg1)
    {
    }

    public void deletedFrom (Object arg0)
    {
    }

    public void existingTask (Task arg)
    {
      taskAdded(arg);
    }

    public void taskAdded (Task task)
    {
      handleTask(task);
    }

    public void taskRemoved (Task arg0)
    {
    }
  }

}
