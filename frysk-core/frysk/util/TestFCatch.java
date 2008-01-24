// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008, Red Hat Inc.
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
import java.io.StringWriter;
import java.util.Iterator;
import java.util.logging.Level;
import frysk.proc.FindProc;
import frysk.stack.Frame;
import frysk.stack.StackFactory;
import frysk.isa.signals.Signal;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.testbed.TestLib;
import frysk.testbed.SlaveOffspring;

public class TestFCatch
    extends TestLib
{

  String mainThread = "(#[\\d]+ 0x[\\da-f]+ in .*\n)*"
                      + "#[\\d]+ 0x[\\da-f]+ in server \\(\\).*\n"
                      + "#[\\d]+ 0x[\\da-f]+ in main \\(\\).*\n"
                      + "#[\\d]+ 0x[\\da-f]+ in __libc_start_main \\(\\).*\n"
                      + "#[\\d]+ 0x[\\da-f]+ in _start \\(\\).*\n";

  public void testSingleThreadedCatch ()
  {
    SlaveOffspring ackProc = SlaveOffspring.createChild();
    Proc proc = ackProc.assertFindProcAndTasks();

    FCatchTester catcher = new FCatchTester();
    Manager.eventLoop.runPending();

    catcher.addTracePid(proc.getPid());
    catcher.trace(new String[1], true);

    assertRunUntilStop("Adding all observers");

    frysk.sys.Signal.SEGV.kill(proc.getPid());

    assertRunUntilStop("Building stacktrace");

    String trace = catcher.getStackTrace();

    assertTrue(trace + "should match: " + this.mainThread,
               trace.matches(this.mainThread));
  }

  class FCatchTester
      extends FCatch
  {
    private StringBuffer stackTrace = new StringBuffer();

    protected int numAdds;

    private Proc proc;

    protected SignalObserver signalObserver;

    protected int numTasks;

    protected Task sigTask;

    private Blocker blocker;

    public FCatchTester ()
    {
      super();
    }

    public void trace (String[] command, boolean attach)
    {
      logger.log(Level.FINE, "{0} trace", this);
      if (attach == true)
        init();
      else
        Manager.host.requestCreateAttachedProc(command, new CatchObserver());
    }

    private void init ()
    {
      logger.log(Level.FINE, "{0} init", this);

      Manager.host.requestProc(this.procID, new FindProc()
      {
        public void procFound(Proc p) {
	    proc = p;
	    iterateTasks();
        }
        public void procNotFound(ProcId procId) {
	    System.err.println("Couldn't find the process: " + procId.toString());
	    Manager.eventLoop.requestStop();
        }
      });
      logger.log(Level.FINE, "{0} exiting init", this);
    }

    private void iterateTasks ()
    {
      Iterator i = proc.getTasks().iterator();
      while (i.hasNext())
        {
          ((Task) i.next()).requestAddAttachedObserver(new CatchObserver());
        }
    }

    /**
     * An observer that sets up things once frysk has set up the requested proc
     * and attached to it.
     */
    class CatchObserver
        implements TaskObserver.Attached, TaskObserver.Cloned,
        TaskObserver.Terminating, TaskObserver.Terminated
    {
      public Action updateAttached (Task task)
      {
        logger.log(Level.FINE, "{0} updateAttached", task);
        // System.err.println("CatchObserver.updateAttached on" + task);
        if (signalObserver == null)
          signalObserver = new SignalObserver();

        task.requestAddSignaledObserver(signalObserver);
        task.requestAddClonedObserver(this);
        task.requestAddTerminatingObserver(this);
        task.requestAddTerminatedObserver(this);
        task.requestUnblock(this);
        return Action.BLOCK;
      }

      public Action updateClonedParent (Task parent, Task offspring)
      {
        logger.log(Level.FINE, "{0} updateClonedParent", parent);
        // System.out.println("Cloned.updateParent");
        parent.requestUnblock(this);
        return Action.BLOCK;
      }

      public Action updateClonedOffspring (Task parent, Task offspring)
      {
        logger.log(Level.FINE, "{0} updateClonedOffspring", offspring);
        // System.err.println("CatchObserver.updateClonedOffspring " +
        // offspring);
        numTasks = offspring.getProc().getTasks().size();
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
        logger.log(Level.FINE, "{0} updateTerminating", task);
        // System.err.println("CatchObserver.updateTerminating on " + task + " "
        // + value + " " + numTasks);
        return Action.CONTINUE;
      }

      public Action updateTerminated (Task task, boolean signal, int value)
      {
        logger.log(Level.FINE, "{0} updateTerminated", task);
        // System.err.println("CatchObserver.updateTerminated " + task);
        // if (--FCatch.this.numTasks <= 0)
        // Manager.eventLoop.requestStop();

        return Action.CONTINUE;
      }

      public void addedTo (Object observable)
      {
        logger.log(Level.FINE, "{0} CatchObserver.addedTo", (Task) observable);
        // System.out.println("CatchObserver.addedTo " + (Task) observable);
        ++numAdds;
        if (numAdds == ((Task) observable).getProc().getTasks().size() * 4)
          Manager.eventLoop.requestStop();
      }

      public void addFailed (Object observable, Throwable w)
      {
        throw new RuntimeException("Failed to attach to created proc", w);
      }

      public void deletedFrom (Object observable)
      {
        logger.log(Level.FINE, "{0} deletedFrom", (Task) observable);
      }
    }

    class SignalObserver implements TaskObserver.Signaled {
	public Action updateSignaled (Task task, Signal signal) {
	    logger.log(Level.FINE, "{0} updateSignaled", task);
	    sigTask = task;
	    numTasks = task.getProc().getTasks().size();
	    // stackTrace.append("fcatch: from PID " +
	    // task.getProc().getPid() + " TID " + task.getTid() +
	    // ":\n");
	    blocker = new Blocker();
	    Iterator i = task.getProc().getTasks().iterator();
	    while (i.hasNext()) {
		Task t = (Task) i.next();
		t.requestAddInstructionObserver(blocker);
	    }
	    return Action.BLOCK;
	}

      public void addFailed (Object observable, Throwable w)
      {
        w.printStackTrace();
      }

      public void addedTo (Object observable)
      {
        logger.log(Level.FINE, "{0} SignalObserver.addedTo", (Task) observable);
        // System.err.println("SignalObserver.addedTo");
      }

      public void deletedFrom (Object observable)
      {
        logger.log(Level.FINE, "{0} deletedFrom", (Task) observable);
      }
    }

    class Blocker
        implements TaskObserver.Instruction
    {
      public Action updateExecuted (Task task)
      {
        handleTaskBlock(task);
        return Action.BLOCK;
      }

      public void addFailed (Object observable, Throwable w)
      {
        w.printStackTrace();
      }

      public void addedTo (Object observable)
      {
        logger.log(Level.FINE, "{0} SignalObserver.addedTo", (Task) observable);
      }

      public void deletedFrom (Object observable)
      {
        logger.log(Level.FINE, "{0} deletedFrom", (Task) observable);
      }
    }

      private void generateStackTrace (Task task) {
	  logger.log(Level.FINE, "{0} generateStackTrace", task);
	  --numTasks;
	  Frame frame = null;
	  try {
	      frame = StackFactory.createFrame(task);
	  } catch (Exception e) {
	      System.out.println(e.getMessage());
	      System.exit(1);
	  }

	  StringWriter stringWriter = new StringWriter();
	  PrintWriter printWriter = new PrintWriter(stringWriter);
	  StackFactory.printStack(printWriter, frame);
	  this.stackTrace.append(stringWriter.getBuffer());
      
	  logger.log(Level.FINE, "{0} exiting generateStackTrace", task);
      }

    public String getStackTrace ()
    {
      return this.stackTrace.toString();
    }

    public String toString ()
    {
      String trace = this.stackTrace.toString();
      System.out.println(trace);
      return trace;
    }

    public void handleTaskBlock (Task task)
    {
      generateStackTrace(task);

      if (numTasks <= 0)
        {
          Manager.eventLoop.requestStop();
          sigTask.requestUnblock(signalObserver);
          Iterator i = task.getProc().getTasks().iterator();
          while (i.hasNext())
            {
              Task t = (Task) i.next();
              t.requestDeleteInstructionObserver(blocker);
            }
        }
    }
  }
}
