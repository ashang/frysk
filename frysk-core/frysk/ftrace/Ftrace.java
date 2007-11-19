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

import frysk.stack.Frame;
import frysk.stack.StackFactory;

import frysk.util.SyscallHandler;

import inua.util.PrintWriter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Iterator;

import java.io.File;

public class Ftrace
{
  // Where to send output.
  PrintWriter writer;

  // True if we're tracing children as well.
  boolean traceChildren = false;

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

    synchronized void generateStackTrace (Task task,
					  String syscallStackTraceName) {
	Frame frame = StackFactory.createFrame(task);
	writer.println("Task: " + task.getTid() 
		       + " dumping stack trace for syscall \"" + syscallStackTraceName + "\":" );

	StackFactory.printStack(writer, frame);
	writer.flush();
    }

  synchronized void handleTask (Task task)
  {
    task.requestAddSyscallObserver(syscallObserver);
    task.requestAddForkedObserver(forkedObserver);
    if (ltraceController != null)
	Ltrace.requestAddFunctionObserver(task, functionObserver, ltraceController);
    Proc proc = task.getProc();
    // XXX: use forkObserver instead
//    if (traceChildren)
//      tracedParents.add(proc.getId());
    Manager.host.observableProcRemovedXXX.addObserver(new ProcRemovedObserver(proc));
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
  private TaskObserver.Attached attachedObserver = new TaskObserver.Attached()
  {
    public Action updateAttached (Task task)
    {
      addProc(task.getProc());
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
  TaskObserver.Syscall syscallObserver = new TaskObserver.Syscall()
  {

    public Action updateSyscallEnter (Task task)
    {
      frysk.proc.Syscall syscall = task.getSyscallEventInfo().getSyscall(task);
      syscallCache.put(task, syscall);

      /*
         * if this systsysem call is in the stack tracing HashSet, get a stack
         * trace before continuing on.
         */
      if (syscallStackTraceSet != null
	  && syscallStackTraceSet.contains(syscall.getName()))
	generateStackTrace(task, syscall.getName());

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
	Task task = (Task) observable;
	task.requestUnblock(attachedObserver);
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

    /*
    public static interface LtraceControllerObserver {
	void shouldStackTraceOn(Set symbols);
    }
    */

    class MyLtraceObserver
	implements LtraceObserver//, LtraceControllerObserver
    {
	//Where to send the output.
	PrintWriter writer = new PrintWriter(System.out);

	private Map levelMap = new HashMap();

	// Which symbols should yield a stack trace.
	private HashSet symbolsStackTraceSet = new HashSet();

	/*
	public synchronized void shouldStackTraceOn(Set symbols) {
	    symbolsStackTraceSet.addAll(symbols);
	}
	*/

	private Object lastItem = null;
	private Task lastTask = null;

	private int getLevel(Task task)
	{
	    int level = 0;
	    Integer l = (Integer)levelMap.get(task);
	    if (l != null)
		level = l.intValue();
	    return level;
	}

	private void setLevel(Task task, int level)
	{
	    levelMap.put(task, new Integer(level));
	}

	private boolean lineOpened()
	{
	    return lastItem != null;
	}

	private boolean myLineOpened(Task task, Object item)
	{
	    return lastItem == item && lastTask == task;
	}

	private void updateOpenLine(Task task, Object item)
	{
	    lastItem = item;
	    lastTask = task;
	}

	private String repeat(char c, int count)
	{
	    // by Stephen Friedrich
	    char[] fill = new char[count];
	    Arrays.fill(fill, c);
	    return new String(fill);
	}

	private String pidInfo(Task task)
	{
	    return "" + task.getProc().getPid() + "." + task.getTid();
	}

	private void eventEntry(Task task, Object item, String eventType,
				String eventName, Object[] args)
	{
	    int level = this.getLevel(task);
	    String spaces = repeat(' ', level);
	    this.setLevel(task, ++level);

	    if (lineOpened())
		System.err.println('\\');

	    System.err.print(pidInfo(task) + " "
			     + spaces + eventType + " ");
	    System.err.print(eventName + "(");
	    for (int i = 0; i < args.length; ++i) {
		System.err.print(i > 0 ? ", " : "");
		// Temporary hack to get proper formatting before
		// something more sane lands.
		if (args[i] instanceof Long)
		    System.err.print("0x" + Long.toHexString(((Long)args[i]).longValue()));
		else if (args[i] instanceof Integer)
		    System.err.print("0x" + Integer.toHexString(((Integer)args[i]).intValue()));
		else
		    System.err.print(args[i]);
	    }
	    System.err.print(")");

	    updateOpenLine(task, item);
	}

	private void eventLeave(Task task, Object item, String eventType,
				String eventName, Object retVal)
	{
	    int level = this.getLevel(task);
	    this.setLevel(task, --level);

	    if (!myLineOpened(task, item)) {
		if (lineOpened())
		    System.err.println();
		String spaces = repeat(' ', level);
		System.err.print(pidInfo(task) + " " + spaces + eventType + " " + eventName);
	    }

	    System.err.println(" = " + retVal);

	    updateOpenLine(null, null);
	}

	private void eventSingle(Task task, String eventName)
	{
	    int level = this.getLevel(task);

	    if (lineOpened())
		System.err.println("\\");
	    System.err.println(pidInfo(task) + " " + repeat(' ', level) + eventName);

	    updateOpenLine(null, null);
	}

	private void generateStackTrace(Task task)
	{
	    eventSingle(task, "dumping stack trace:");

	    Frame frame = StackFactory.createFrame(task);
	    StackFactory.printStack(writer, frame);
	    writer.flush();
	    updateOpenLine(null, null);
	}

	public synchronized Action funcallEnter(Task task, Symbol symbol, Object[] args)
	{
	    String symbolName = symbol.name;
	    String callerLibrary = symbol.getParent().getSoname();
	    String eventName = callerLibrary + "->" + /*libraryName + ":" +*/ symbolName;
	    eventEntry(task, symbol, "<CALL>", eventName, args);

	    // If this systsysem call is in the stack tracing HashSet,
	    // get a stack trace before continuing on.
	    if (symbolsStackTraceSet != null
		&& symbolsStackTraceSet.contains(symbol))
		generateStackTrace(task);

	    return Action.CONTINUE;
	}

	public synchronized Action funcallLeave(Task task, Symbol symbol, Object retVal)
	{
	    eventLeave(task, symbol, "<LEAVE>", symbol.name, retVal);
	    return Action.CONTINUE;
	}

	public synchronized Action fileMapped(Task task, File file)
	{
	    eventSingle(task, "<MAP> " + file);
	    return Action.CONTINUE;
	}

	public synchronized Action fileUnmapped(Task task, File file)
	{
	    eventSingle(task, "<UNMAP> " + file);
	    return Action.CONTINUE;
	}

	public void addedTo (Object observable) { }
	public void deletedFrom (Object observable) { }
	public void addFailed (Object observable, Throwable w) { }
    }

    MyLtraceObserver functionObserver = new MyLtraceObserver();
}
