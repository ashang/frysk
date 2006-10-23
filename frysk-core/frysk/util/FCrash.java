package frysk.util;

import inua.util.PrintWriter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
//import java.util.logging.Level;

import frysk.event.RequestStopEvent;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcAttachedObserver;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.rt.StackFrame;
import frysk.rt.StackFactory;

public class FCrash
{

  private Proc proc;
  
  //private int numProcesses = 0;

  public ProcAttachedObserver pao;
  
  PrintWriter writer;
  
  //True if we're tracing children as well.
  boolean traceChildren;
  
  boolean firstCall = true;
  
  //Set of ProcId objects we trace; if traceChildren is set, we also
  // look for their children.
  HashSet tracedParents = new HashSet();
  
  StackFrame[] frames;
  
  public void trace (String[] command)
  {
    init();
    Manager.host.requestCreateAttachedProc(command, new AttachedObserver());
    Manager.host.requestRefreshXXX(true);
    Manager.eventLoop.start();
  }
  
  public void trace()
    {
        init();
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
        proc = (Proc) arg;
        ProcId id = proc.getId();
        if (tracedParents.contains(id)
            || (traceChildren && tracedParents.contains(proc.getParent().getId())))
          {
            // In case we're tracing a new child, add it.
            tracedParents.add(proc.getId());
            new ProcAttachedObserver(proc, new TasksCrashObserver());
          }
      }
    });
  }
  
  synchronized void handleTask (Task task)
    {
    
    if (firstCall == true)
      {
        firstCall = false;
        proc = task.getProc();
      }
    }
  
  public void addTracePid(int id)
    {
        tracedParents.add(new ProcId(id));
    }

  private void generateStackTrace (Task task)
  {
    int i = 0;
    frames = new StackFrame[proc.getTasks().size()];
    Iterator iter = proc.getTasks().iterator();
    while (iter.hasNext())
      {
        try
          {
            frames[i] = StackFactory.createStackFrame((Task) iter.next());
          }
        catch (Exception e)
          {
            System.out.println(e.getMessage());
            System.exit(1);
          }
        StackFrame curr = frames[i];

        while (curr != null)
          {
            System.out.println(curr.toString());
            curr = curr.getOuter();
          }
        i++;
      }
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

    public Action updateAttached (Task task)
    {
      return Action.CONTINUE;
    }
    
    public void deletedFrom (Object arg0)
    {
    }

    public void existingTask (Task task)
    {
      handleTask(task);
    }

    public void taskAdded (Task task)
    {
      
    }

    public void taskRemoved (Task arg0)
    {
    }
  }
  
  /**
     * An observer that sets up things once frysk has set up
     * the requested proc and attached to it.
     */
    private class AttachedObserver implements TaskObserver.Attached{
        public Action updateAttached (Task task)
        {
            SignalObserver sigo = new SignalObserver();
            task.requestAddSignaledObserver(sigo);
            handleTask(task);
            task.requestUnblock(this);
            return Action.BLOCK;
        }
        
        public void addedTo (Object observable){}
        
        public void addFailed (Object observable, Throwable w){
            throw new RuntimeException("Failed to attach to created proc", w);
        }
        
        public void deletedFrom (Object observable){}
    }
    
    private Object monitor = new Object();
    
    class SignalObserver
      implements TaskObserver.Signaled
  {
//    private final int sig;

    private int triggered;

    private boolean added;

    private boolean removed;

    public Action updateSignaled (Task task, int signal)
    {

      switch (signal)
        {
        case 2:
          System.out.println("SIGHUP detected: dumping stack trace");
          generateStackTrace(task);
          break;
        case 3:
          System.out.println("SIGQUIT detected: dumping stack trace");
          generateStackTrace(task);
          //System.exit(0);
          break;
        case 6:
          System.out.println("SIGABRT detected: dumping stack trace");
          generateStackTrace(task);
          //System.exit(0);
          break;
        case 9:
          System.out.println("SIGKILL detected: dumping stack trace");
          generateStackTrace(task);
          //System.exit(0);
          break;
        case 11:
          System.out.println("SIGSEGV detected: dumping stack trace");
          generateStackTrace(task);
          //System.exit(0);
          break;
        case 15:
          System.out.println("SIGTERM detected: dumping stack trace");
          //System.exit(0);
          break;
        default:
          System.out.println("Signal detected: dumping stack trace");
          generateStackTrace(task);
        }

      return Action.CONTINUE;
    }

    int getTriggered ()
    {
      return triggered;
    }

    public void addFailed (Object observable, Throwable w)
    {
      w.printStackTrace();
    }

    public void addedTo (Object observable)
    {
      // Hurray! Lets notify everybody.
      synchronized (monitor)
        {
          added = true;
          removed = false;
          monitor.notifyAll();
        }
    }

    public boolean isAdded ()
    {
      return added;
    }

    public void deletedFrom (Object observable)
    {
      synchronized (monitor)
        {
          removed = true;
          added = true;
          monitor.notifyAll();
        }
    }

    public boolean isRemoved ()
    {
      return removed;
    }
  }

}
