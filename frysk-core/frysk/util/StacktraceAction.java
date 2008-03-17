// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008 Red Hat Inc.
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

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.TreeMap;
import frysk.rsl.Log;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.debuginfo.PrintStackOptions;
import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.event.SignalEvent;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcBlockObserver;
import frysk.proc.Task;
import frysk.stack.StackFactory;
import frysk.sys.Signal;

public abstract class StacktraceAction implements ProcBlockObserver {
    private static final Log fine = Log.fine(StacktraceAction.class);

  public void addedTo (Object observable)
  {
  }

  public void addFailed (Object observable, Throwable w)
  {
  }

  public void deletedFrom (Object observable)
  {
  }

  protected PrintWriter printWriter;

  private TreeMap sortedTasks = new TreeMap();

  private Event event;

  PrintStackOptions options;
  
  /**
   * Runs a stacktrace on the given process.
   * 
   * @param theProc the process to run the stack trace on.
   * @param theEvent an event to run on completion of the stack trace. For
   *          example: Stop the eventLoop and exit the program.
   * @param numberOfFrames number of frames to print pass value <=0 to print all frames.
   * @param elfOnly if true print an elf only stack back trace not referring to any
   *            stack debug information. Otherwise, print a rich stack trace using
   *            debug information.
   * @param printParameters this is only valid if elfOnly is false. If this option
   *            is true then the parameters of each function are printed.
   * @param printScopes this is only valid if elfOnly is false. If this is true
   *            the scopes of the function and their respective variables are printed
   * @param fullpath this is only valid if elfOnly is false. If this is true the entire
   *            file path is printed other wise only the name of the file is printed.
   * @throws ProcException
   */
  public StacktraceAction (PrintWriter printWriter, Proc theProc, Event theEvent, PrintStackOptions options)
  {
     event = theEvent;
     
     this.options = options;
     this.printWriter = printWriter;
     Manager.eventLoop.add(new InterruptEvent(theProc));
  }  

  public final void existingTask (Task task)
  {

      fine.log(this, "existingTask, Task", task);

    // Print the stack frame for this stack.
    

    if (sortedTasks == null)
      sortedTasks = new TreeMap();

    sortedTasks.put(new Integer(task.getTid()), task);
  }

  public void taskAddFailed (Object observable, Throwable w)
  {
      fine.log(this, "could not be added to", observable,
	       "because", w);
  }

  private final void printTasks ()
  {
    fine.log(this, "printTasks");
    Iterator iter = sortedTasks.values().iterator();
    while (iter.hasNext())
      {
	Task task =  (Task) iter.next();
	
	if(options.elfOnly()){
	    StackFactory.printTaskStackTrace(printWriter,task,options);
	}else{
	    if(options.printVirtualFrames()){
		DebugInfoStackFactory.printVirtualTaskStackTrace(printWriter,task,options);
	    }else{
		DebugInfoStackFactory.printTaskStackTrace(printWriter,task,options);
	    }
	}
	printWriter.println();
      }
    fine.log(this, "exiting printTasks");
  }

  public void flush(){
      this.printWriter.flush();
  }
  /**
   * If the user cntl-c interrupts, handle it cleanly
   */
  static class InterruptEvent
      extends SignalEvent
  {
    Proc proc;

    public InterruptEvent (Proc theProc)
    {

      super(Signal.INT);
      proc = theProc;
      fine.log(this, "InterruptEvent proc", theProc);
    }

    public final void execute ()
    {
      fine.log(this, "execute");
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

  public void allExistingTasksCompleted ()
  {
    fine.log(this, "allExistingTasksCompleted");
    // Print all the tasks in order.
    printTasks();

    // Run the given Event.
    Manager.eventLoop.add(event);
  }

}
