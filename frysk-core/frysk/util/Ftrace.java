// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.Host;
import frysk.proc.TaskObserver;
import frysk.proc.TaskObserver.Forked;
import frysk.stack.Frame;
import frysk.stack.StackFactory;
import inua.util.PrintWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Iterator;

public class Ftrace
{
  // Where to send output.
  PrintWriter writer;

  // True if we're tracing children as well.
  boolean traceChildren;

  HashSet syscallStackTraceSet = null;

  // Set of ProcId objects we trace; if traceChildren is set, we also
  // look for their children.
  HashSet tracedParents = new HashSet();

  HashMap syscallCache = new HashMap();

  // The number of processes we're tracing.
  int numProcesses;

  // Enter and exit handlers.
  SyscallHandler enterHandler;

  SyscallHandler exitHandler;

  public void setTraceChildren ()
  {
    traceChildren = true;
  }

  public void addTracePid (ProcId id)
  {
    tracedParents.add(id);
  }

  public void setSyscallStackTracing (HashSet syscallSet)
  {
    syscallStackTraceSet = syscallSet;
  }

  public void setWriter (PrintWriter writer)
  {
    this.writer = writer;
  }

  public void setEnterHandler (SyscallHandler handler)
  {
    this.enterHandler = handler;
  }

  public void setExitHandler (SyscallHandler handler)
  {
    this.exitHandler = handler;
  }

  private void init ()
  {
    if (writer == null)
      writer = new PrintWriter(System.out);

    // this observer should only be used to pick up a proc if we
    // are tracing a process given a pid
    // otherwise use forkobserver.
    Manager.host.observableProcAddedXXX.addObserver(new Observer()
    {
      public void update (Observable observable, Object arg)
      {
	Proc proc = (Proc) arg;
	ProcId id = proc.getId();
	if (tracedParents.contains(id)){
	    // In case we're tracing a new child, add it.
//	    tracedParents.add(proc.getId()); XXX: why is this needed ?
	    // Weird API... unfortunately we can't fetch the
	    // Proc's main task here, as it will be null. Instead
	    // we have to request it and handle it in a callback.
	  addProc(proc);
	}
      }
    });
  }

  private void addProc(Proc proc){
    new ProcTasksObserver(proc, tasksObserver);
  }
  
  public void trace (String[] command)
  {
    init();
    Manager.host.requestCreateAttachedProc(command, new AttachedObserver());
    Manager.eventLoop.run();
  }

  public void trace ()
  {
    init();
    for (Iterator it = tracedParents.iterator(); it.hasNext(); )
      Manager.host.requestFindProc(
	(ProcId)it.next(),
	new Host.FindProc() {
	  public void procFound (ProcId procId) {}
	  public void procNotFound (ProcId procId, Exception e) {
	    System.err.println("No process with ID " + procId.intValue() + " found.");
	  }
	}
      );

    Manager.eventLoop.run();
  }

  synchronized void generateStacKTrace (Task task, String syscallStackTraceName)
  {
    Frame frame = StackFactory.createFrame(task);
    writer.println("Task: " + task.getTid() 
                   + " dumping stack trace for syscall \"" + syscallStackTraceName + "\":" );
    
    int count = 0;
    while (frame != null)
      {
	writer.print("#" + count + " ");
	frame.toPrint(writer,false,true);
	writer.println();
	frame = frame.getOuter();
	++count;
      }

    writer.flush();
  }

  synchronized void handleTask (Task task)
  {
    task.requestAddSyscallObserver(syscallObserver);
    task.requestAddForkedObserver(forkedObserver);
    Proc proc = task.getProc();
    // XXX: use forkObserver instead
//    if (traceChildren)
//      tracedParents.add(proc.getId());
    Manager.host.observableProcRemovedXXX.addObserver(new ProcRemovedObserver(
									      proc));
    writer.println("Ftrace.main() Proc.getPid() " + proc.getPid());
    writer.println("Ftrace.main() Proc.getExe() " + proc.getExe());
    writer.flush();
    ++numProcesses;
  }

  ProcObserver.ProcTasks tasksObserver = new ProcObserver.ProcTasks()
  {
    public void existingTask (Task task)
    {
      handleTask(task);
    }

    public void taskAdded (Task task)
    {
      handleTask(task);
    }

    public void taskRemoved (Task arg0)
    {
    }

    public void addedTo (Object arg0)
    {
    }

    public void addFailed (Object arg0, Throwable arg1)
    {
    }

    public void deletedFrom (Object arg0)
    {
    }
  };

  /**
   * An observer to stop the eventloop when the traced process exits.
   */
  private class ProcRemovedObserver
      implements Observer
  {
    int pid;

    ProcRemovedObserver (Proc proc)
    {
      this.pid = proc.getPid();
    }

    public void update (Observable o, Object object)
    {
      Proc proc = (Proc) object;
      if (proc.getPid() == this.pid)
	{
	  synchronized (Ftrace.this)
	    {
	      --numProcesses;
	      if (numProcesses == 0)
		Manager.eventLoop.requestStop();
	    }
	}
    }
  }

  /**
   * An observer that sets up things once frysk has set up the requested
   * proc and attached to it.
   */
  private class AttachedObserver
      implements TaskObserver.Attached
  {
    public Action updateAttached (Task task)
    {
      addProc(task.getProc());
      task.requestUnblock(this);
      return Action.BLOCK;
    }

    public void addedTo (Object observable)
    {
    }

    public void addFailed (Object observable, Throwable w)
    {
      throw new RuntimeException("Failed to attach to created proc", w);
    }

    public void deletedFrom (Object observable)
    {
    }
  }

  /**
         * The syscallObserver added to the traced proc.
         */
  TaskObserver.Syscall syscallObserver = new TaskObserver.Syscall()
  {

    public Action updateSyscallEnter (Task task)
    {
      frysk.proc.Syscall syscall = task.getSyscallEventInfo().getSyscall(task);
      syscallCache.put(task, syscall);

      /*
         * if this system call is in the stack tracing HashSet, get a stack
         * trace before continuing on.
         */
      if (syscallStackTraceSet != null
	  && syscallStackTraceSet.contains(syscall.getName()))
	generateStacKTrace(task, syscall.getName());

      if (enterHandler != null)
	enterHandler.handleEnter(task, syscall);
      return Action.CONTINUE;
    }

    public Action updateSyscallExit (Task task)
    {

      frysk.proc.Syscall syscall = (frysk.proc.Syscall) syscallCache.remove(task);

      if (exitHandler != null)
	exitHandler.handleExit(task, syscall);

      return Action.CONTINUE;
    }

    public void addedTo (Object observable)
    {

    }

    public void addFailed (Object observable, Throwable w)
    {
      throw new RuntimeException(
				 "Failed to add a Systemcall observer to the process",
				 w);
    }

    public void deletedFrom (Object observable)
    {
      throw new RuntimeException("This has not yet been implemented");
    }

  };

  TaskObserver.Forked forkedObserver = new Forked(){

    public Action updateForkedOffspring (Task parent, Task offspring)
    {
      if(traceChildren){
        addProc(offspring.getProc());
        offspring.requestUnblock(this);
        return Action.BLOCK;
      }
      
      return Action.CONTINUE;
    }

    public Action updateForkedParent (Task parent, Task offspring)
    {
      return Action.CONTINUE;
    }

    public void addFailed (Object observable, Throwable w)
    {
    }

    public void addedTo (Object observable)
    {
    }

    public void deletedFrom (Object observable)
    {
    }
  };
  
}
