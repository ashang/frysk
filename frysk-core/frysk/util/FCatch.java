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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
//import java.util.logging.Level;

import frysk.event.RequestStopEvent;
import frysk.proc.Action;
import frysk.proc.Host;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcObserver;
//import frysk.proc.ProcAttachedObserver;
import frysk.proc.ProcId;
//import frysk.proc.ProcObserver;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.rt.StackFrame;
import frysk.rt.StackFactory;

public class FCatch
{

  private Proc proc;
  
  private int numTasks = 0;

  //public ProcAttachedObserver pao;
  
  PrintWriter writer;
  
  //True if we're tracing children as well.
  boolean traceChildren;
  
  boolean firstCall = true;
  
//  private Object monitor = new Object();
  
  //Set of ProcId objects we trace; if traceChildren is set, we also
  // look for their children.
  HashSet tracedParents = new HashSet();
  
  StackFrame[] frames;
  
  public void trace (String[] command, boolean attach)
  {
    Manager.host.requestRefreshXXX(true);
    if (attach == true)
      init();
    else
      Manager.host.requestCreateAttachedProc(command, new AttachedObserver());
    Manager.eventLoop.start();
  }
  
  public void trace ()
  {
    //System.out.println("trace");
    Manager.host.requestRefreshXXX(true);
    init();
    Manager.eventLoop.start();
  }

  private void init ()
  {
    //System.out.println("in Init()");
    if (writer == null)
      writer = new PrintWriter(System.out);

    Manager.host.observableProcAddedXXX.addObserver(new Observer()
    {
      
      public void update (Observable observable, Object arg)
      {
        proc = (Proc) arg;
        //proc.sendRefresh();
        ProcId id = proc.getId();
        if (tracedParents.contains(id))
            //|| (traceChildren && tracedParents.contains(proc.getParent().getId())))
          {
            //System.out.println("manager.update");
            // In case we're tracing a new child, add it.
            //tracedParents.add(proc.getId());
            
            Manager.host.requestFindProc(id, new Host.FindProc()
            {
              public void procFound (ProcId procId)
              {
                proc = Manager.host.getProc(procId);
                iterateTasks();
              }
              public void procNotFound (ProcId procId, Exception e)
              {
                System.err.println("Couldn't find the process: "
                                   + procId.toString());
                Manager.eventLoop.requestStop();
              }
            });
            
//            Iterator i = proc.getTasks().iterator();
//            System.out.println("proc: " + proc + proc.getMainTask() + " tasks.size: " + proc.getTasks().size());
//            while (i.hasNext())
//              {
//                System.out.println("iterating tasks");
//                ((Task) i.next()).requestAddAttachedObserver(new AttachedObserver());
//              }
          }
      }
    });
  }
  
  private void iterateTasks ()
  {
    Iterator i = proc.getTasks().iterator();
    //System.out.println("proc: " + proc + proc.getMainTask() + " tasks.size: " + proc.getTasks().size());
    while (i.hasNext())
      {
        ((Task) i.next()).requestAddAttachedObserver(new AttachedObserver());
      }
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
      //System.out.println("addtracepid " + id);
        tracedParents.add(new ProcId(id));
    }

  private void generateStackTrace (Task task)
  {
      //int i = 0;
    //frames = new StackFrame[proc.getTasks().size()];
    StackFrame frame = null;
    //Iterator iter = proc.getTasks().iterator();
    //while (iter.hasNext())
    //  {
        try
          {
            frame = StackFactory.createStackFrame(task);
          }
        catch (Exception e)
          {
            System.out.println(e.getMessage());
            System.exit(1);
          }

        while (frame != null)
          {
            System.out.println(frame.toPrint(false));
            frame = frame.getOuter();
          }
	//  i++;
	//  }
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
  
  /**
     * An observer that sets up things once frysk has set up
     * the requested proc and attached to it.
     */
    class AttachedObserver
    implements TaskObserver.Attached, TaskObserver.Cloned,
    TaskObserver.Terminating, TaskObserver.Terminated
    {
        public Action updateAttached (Task task)
        {
          //System.out.println("attached.updateattached");
            SignalObserver sigo = new SignalObserver();
            task.requestAddSignaledObserver(sigo);
            task.requestAddClonedObserver(this);
            task.requestAddTerminatingObserver(this);
            task.requestAddTerminatedObserver(this);
//            TermObserver termo = new TermObserver();
//            task.requestAddTerminatingObserver(termo);
            //handleTask(task);
            task.requestUnblock(this);
            return Action.BLOCK;
        }
        
        public Action updateClonedParent (Task parent, Task offspring)
        {
          //System.out.println("Cloned.updateParent");
          parent.requestUnblock(this);
          return Action.BLOCK;
        }

        public Action updateClonedOffspring (Task parent, Task offspring)
        {
          //System.out.println("Cloned.updateOffspring " + offspring);
          FCatch.this.numTasks =offspring.getProc().getTasks().size();
          //TerminatingObserver to = new TerminatingObserver();
          //offspring.requestAddTerminatingObserver(to);
          SignalObserver sigo = new SignalObserver();
          offspring.requestAddSignaledObserver(sigo);
          offspring.requestAddTerminatingObserver(this);
          offspring.requestAddClonedObserver(this);
          offspring.requestAddTerminatedObserver(this);
          offspring.requestUnblock(this);
          return Action.BLOCK;
        }
        
        public Action updateTerminating (Task task, boolean signal, int value)
        {
          //System.out.println("TermObserver.updateTerminating " + task + " " +  numTasks);
//          if (--FCatch.this.numTasks <= 0)
//            System.exit(0);

          return Action.CONTINUE;
        }

        public Action updateTerminated (Task task, boolean signal, int value)
        {
          //System.out.println("TermObserver.updateTerminated " + task + " " + numTasks);
          if (--FCatch.this.numTasks <= 0)
            System.exit(0);
          
          return Action.CONTINUE;
        }
        
        public void addedTo (Object observable)
        {
            //System.out.println("AttachedObserver.addedTo " + (Task) observable);
        }
        
        public void addFailed (Object observable, Throwable w)
        {
            throw new RuntimeException("Failed to attach to created proc", w);
        }
        
        public void deletedFrom (Object observable)
        {
          
        }
    }
    
    
    
    
    class SignalObserver
      implements TaskObserver.Signaled
  {
    private int triggered;

    private boolean added;

    private boolean removed;

    public Action updateSignaled (Task task, int signal)
    {
      FCatch.this.numTasks = task.getProc().getTasks().size();
      //System.out.println("From PID: " + task.getProc().getPid() + " TID: " + task.getTid());
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
      //System.out.println("sig.addedTo");
//      Task t = (Task) observable;
//      TaskObserver[] to = t.getBlockers();
//      for (int j = 0; j < to.length -1; j++)
//        t.requestUnblock(to[j]);
      // Hurray! Lets notify everybody.
//      synchronized (monitor)
//        {
//          System.out.println("--> In sync");
//          added = true;
//          removed = false;
//          monitor.notifyAll();
//        }
    }

    public boolean isAdded ()
    {
      return added;
    }

    public void deletedFrom (Object observable)
    {
//      synchronized (monitor)
//        {
//          removed = true;
//          added = true;
//          monitor.notifyAll();
//        }
    }

    public boolean isRemoved ()
    {
      return removed;
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
    
// class TermObserver
//	implements TaskObserver.Terminating, TaskObserver.Terminated, TaskObserver.Cloned
//  {
//    public void addedTo(Object o)
//    {
//      System.out.println("TermObserver.addedTo " + (Task) o);
//    }
//
//      public Action updateClonedParent (Task parent, Task offspring)
//    {
//      System.out.println("Cloned.updateParent");
//      parent.requestUnblock(this);
//      return Action.BLOCK;
//    }
//
//    public Action updateClonedOffspring (Task parent, Task offspring)
//    {
//      System.out.println("Cloned.updateOffspring");
//      //FCatch.this.numTasks =offspring.getProc().getTasks().size();
//      //TerminatingObserver to = new TerminatingObserver();
//      //offspring.requestAddTerminatingObserver(to);
//      SignalObserver sigo = new SignalObserver();
//      offspring.requestAddSignaledObserver(sigo);
//      offspring.requestAddClonedObserver(this);
//      offspring.requestUnblock(this);
//      return Action.BLOCK;
//    }
//    
//    public Action updateTerminating (Task task, boolean signal, int value)
//    {
//      System.out.println("TermObserver.updateTerminating " + numTasks);
//      if (--FCatch.this.numTasks <= 0)
//        System.exit(0);
//
//      return Action.CONTINUE;
//    }
//
//    public Action updateTerminated (Task task, boolean signal, int value)
//    {
//      System.out.println("TermObserver.updateTerminated " + numTasks);
//      if (--FCatch.this.numTasks <= 0)
//        System.exit(0);
//      
//      return Action.CONTINUE;
//    }
//    
//    public void addFailed (Object o, Throwable t)
//    {
//      
//    }
//    
//    public void deletedFrom (Object o)
//    {
//      
//    }
//  }

}
