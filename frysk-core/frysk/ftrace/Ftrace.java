// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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


package frysk.ftrace;

import frysk.proc.Action;
import frysk.proc.FindProc;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.proc.TaskObserver.Forked;

import inua.util.PrintWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Iterator;

import java.io.File;

public class Ftrace
{
    // Where to send output.
    Reporter reporter;

  // True if we're tracing children as well.
  boolean traceChildren = false;

    // True if we're tracing syscalls.
    boolean traceSyscalls = true;

    // True if we're tracing mmaps/unmaps.
    boolean traceMmapUnmap = false;

    // Non-null if we're using ltrace.
    LtraceController ltraceController = null;

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

    public void setDontTraceSyscalls ()
    {
	traceSyscalls = false;
    }

    public void setTraceMmaps ()
    {
	traceMmapUnmap = true;
    }

    public void setTraceFunctions (LtraceController functionController)
    {
	if (functionController == null)
	    throw new AssertionError("functonController != null");

	if (this.ltraceController == null)
	    this.ltraceController = functionController;
	else
	    throw new AssertionError("LtraceController already assigned.");
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
	this.reporter = new Reporter(writer);
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
    if (reporter == null)
	reporter = new Reporter(new PrintWriter(System.out));

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
    Manager.host.requestCreateAttachedProc(command, attachedObserver);
    Manager.eventLoop.run();
  }

  public void trace ()
  {
    init();
    for (Iterator it = tracedParents.iterator(); it.hasNext(); ){
      Manager.host.requestFindProc
	  ((ProcId)it.next(),
	   new FindProc() {
	       public void procFound (ProcId procId) {}
	       public void procNotFound (ProcId procId, Exception e) {
		   System.err.println("No process with ID " + procId.intValue() + " found.");
		   Manager.eventLoop.requestStop();
	       }
	   }
	   );
      Manager.eventLoop.run();
    }
  }

    private HashMap observationCounters = new HashMap();

    synchronized private void observationRequested(Task task) {
	Integer i = (Integer)observationCounters.get(task);
	if (i == null)
	    i = new Integer(1);
	else
	    i = new Integer(i.intValue() + 1);
	observationCounters.put(task, i);
    }

    synchronized private void observationRealized(Task task) {
	Integer i = (Integer)observationCounters.get(task);
	// must be non-null
	int j = i.intValue();
	if (j == 1) {
	    // Store a dummy into the map to detect errors.
	    observationCounters.put(task, new Object());
	    task.requestUnblock(attachedObserver);
	}
	else
	    observationCounters.put(task, new Integer(--j));
    }

    synchronized void handleTask (Task task)
    {
	Proc proc = task.getProc();

	if (traceSyscalls) {
	    task.requestAddSyscallObserver(new MySyscallObserver(reporter));
	    observationRequested(task);
	}

	task.requestAddForkedObserver(forkedObserver);
	observationRequested(task);

	if (ltraceController != null) {
	    Ltrace.requestAddFunctionObserver(task,
					      new MyFunctionObserver(reporter),
					      ltraceController);
	    observationRequested(task);
	}

	Manager.host.observableProcRemovedXXX.addObserver(new ProcRemovedObserver(proc));
	reporter.eventSingle(task, "attached " + proc.getExe());
	++numProcesses;
    }

    ProcObserver.ProcTasks tasksObserver = new ProcObserver.ProcTasks()
    {
	public void existingTask (Task task)
	{
	    handleTask(task);

	    if (task == task.getProc().getMainTask())
		// Unblock forked observer, which blocks main task
		// after the fork, to give us a chance to pick it up.
		task.requestUnblock(forkedObserver);
	}

	public void taskAdded (Task task)
	{
	    handleTask(task);
	}

	public void taskRemoved (Task task)
	{
	}

	public void addedTo (Object observable)	{}
	public void addFailed (Object observable, Throwable arg1) {}
	public void deletedFrom (Object observable) {}
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
  private TaskObserver.Attached attachedObserver = new TaskObserver.Attached()
  {
    public Action updateAttached (Task task)
    {
      addProc(task.getProc());

      // To make sure all the observers are attached before the task
      // continues, the attachedObserver blocks the task.  As each of
      // the observers is addedTo, they call observationRealized.
      // Task is unblocked when all requested observations are
      // realized.
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
  };

  /**
   * The syscallObserver added to the traced proc.
   */
    class MySyscallObserver
	implements TaskObserver.Syscall
    {
	Reporter reporter;
	frysk.proc.Syscall syscallCache = null;

	MySyscallObserver(Reporter reporter)
	{
	    this.reporter = reporter;
	}

	public Action updateSyscallEnter(Task task)
	{
	    frysk.proc.Syscall syscall
		= task.getSyscallEventInfo().getSyscall(task);
	    String name = syscall.getName();
	    // XXX: pass args
	    reporter.eventEntry(task, syscall, "syscall", name, new Object[]{});

	    // If this system call is in the stack tracing HashSet,
	    // get a stack trace before continuing on.
	    if (syscallStackTraceSet != null
		&& syscallStackTraceSet.contains(name))
		reporter.generateStackTrace(task);

	    /* XXX:
	    if (enterHandler != null)
		enterHandler.handleEnter(task, syscall);
	    */
	    syscallCache = syscall;
	    return Action.CONTINUE;
	}

	public Action updateSyscallExit (Task task)
	{
	    frysk.proc.Syscall syscall = syscallCache;
	    String name = syscall.getName();
	    // XXX: pass retVal
	    reporter.eventLeave(task, syscall,
				"syscall leave", name,
				new Integer(0));
	    /* XXX:
	    if (exitHandler != null)
		exitHandler.handleExit(task, syscall);
	    */

	    syscallCache = null;
	    return Action.CONTINUE;
	}

	public void addedTo (Object observable)
	{
	    Task task = (Task) observable;
	    observationRealized(task);
	}

	public void addFailed (Object observable, Throwable w)
	{
	    throw new RuntimeException("Failed to add a Systemcall observer to the process", w);
	}

	public void deletedFrom (Object observable) { }
    }

    TaskObserver.Forked forkedObserver = new Forked()
    {
	public Action updateForkedOffspring (Task parent, Task offspring)
	{
	    if(traceChildren){
		addProc(offspring.getProc());

		if (offspring != offspring.getProc().getMainTask())
		    // If this assertion doesn't hold, probably no
		    // biggie, but you have to unblock the right tasks
		    // in existingTask.
		    throw new AssertionError("assert offspring == offspring.getProc().getMainTask()");

		// Will be unblocked when existingTask picks it up,
		// otherwise we'd miss on events.
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
	    Task task = (Task) observable;
	    observationRealized(task);
	}

	public void deletedFrom (Object observable)
	{
	}
    };

    /*
    public static interface LtraceControllerObserver {
	void shouldStackTraceOn(Set symbols);
    }
    */

    private class MyFunctionObserver
	implements FunctionObserver//, LtraceControllerObserver
    {
	// Reporting tool.
	private Reporter reporter;

	MyFunctionObserver(Reporter reporter) {
	    this.reporter = reporter;
	}

	// Which symbols should yield a stack trace.
	private HashSet symbolsStackTraceSet = new HashSet();

	/*
	public synchronized void shouldStackTraceOn(Set symbols) {
	    symbolsStackTraceSet.addAll(symbols);
	}
	*/


	public synchronized Action funcallEnter(Task task, Symbol symbol, Object[] args)
	{
	    String symbolName = symbol.name;
	    String callerLibrary = symbol.getParent().getSoname();
	    String eventName = callerLibrary + "->" + /*libraryName + ":" +*/ symbolName;
	    reporter.eventEntry(task, symbol, "call", eventName, args);

	    // If this systsysem call is in the stack tracing HashSet,
	    // get a stack trace before continuing on.
	    if (symbolsStackTraceSet != null
		&& symbolsStackTraceSet.contains(symbol))
		reporter.generateStackTrace(task);

	    return Action.CONTINUE;
	}

	public synchronized Action funcallLeave(Task task, Symbol symbol, Object retVal)
	{
	    reporter.eventLeave(task, symbol, "leave", symbol.name, retVal);
	    return Action.CONTINUE;
	}

	public synchronized Action fileMapped(Task task, File file)
	{
	    if (traceMmapUnmap)
		reporter.eventSingle(task, "map " + file);
	    return Action.CONTINUE;
	}

	public synchronized Action fileUnmapped(Task task, File file)
	{
	    if (traceMmapUnmap)
		reporter.eventSingle(task, "unmap " + file);
	    return Action.CONTINUE;
	}

	public void addedTo (Object observable) {
	    Task task = (Task) observable;
	    observationRealized(task);
	}
	public void deletedFrom (Object observable) { }
	public void addFailed (Object observable, Throwable w) { }
    }
}
