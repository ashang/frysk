package frysk.util;

import inua.util.PrintWriter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
//import java.util.logging.Level;

//import org.gnu.glib.CustomEvents;

import frysk.event.RequestStopEvent;
import frysk.event.SignalEvent;
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

//import org.gnu.glib.CustomEvents;

import frysk.sys.Sig;

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
            // Weird API... unfortunately we can't fetch the
            // Proc's main task here, as it will be null. Instead
            // we have to request it and handle it in a callback.
            
            new ProcAttachedObserver(proc, new TasksCrashObserver());
          }
      }
    });

  //  CustomEvents.addEvent(new Runnable() {
  //      public void run() {
            //InterruptEvent ie = new InterruptEvent();
            //Manager.eventLoop.add(ie);
  //      }
  //  });
  }
  
  synchronized void handleTask (Task task)
    {
    
    if (firstCall == true)
      {
        firstCall = false;
        proc = task.getProc();
        //CustomEvents.addEvent(new Runnable() {
        //        public void run() {
        InterruptEvent ie = new InterruptEvent();
        Manager.eventLoop.add(ie);
        //        }
        //});
      }
//        Proc proc = task.getProc();
//        if (traceChildren)
//            tracedParents.add(proc.getId());
//        writer.flush();
//        ++numProcesses;
    }
  
  public void addTracePid(int id)
    {
        tracedParents.add(new ProcId(id));
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
  
  class InterruptEvent
      extends SignalEvent
  {

    public InterruptEvent ()
    {
      super(Sig.INT);
      // logger.log(Level.FINE, "{0} InterruptEvent\n", this);
    }

    public final void execute ()
    {
      // logger.log(Level.FINE, "{0} execute\n", this);
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
        }
      System.exit(1);
    }
  }
  
  /**
     * An observer that sets up things once frysk has set up
     * the requested proc and attached to it.
     */
    private class AttachedObserver implements TaskObserver.Attached{
        public Action updateAttached (Task task)
        {
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

}
