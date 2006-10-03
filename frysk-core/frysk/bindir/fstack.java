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

//package frysk.bindir;

import inua.util.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import frysk.EventLogger;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.ProcObserver;
import frysk.proc.ProcTasksObserver;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;
import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

public class fstack
{
  private PrintWriter writer;
  private TreeMap sortedTasks;

  private Proc proc;

  private static int pid = 0;

  private ProcTasksObserver procTasksObserver;

  private static Parser parser;

  protected static final Logger logger = EventLogger.get("logs/",
                                                         "frysk_core_event.log");

  private static String levelValue;

  private static Level level;

  public void setWriter (PrintWriter writer)
  {
    this.writer = writer;
  }

  private static void addOptions (Parser parser)
  {
    parser.add(new Option(
                          "console",
                          'c',
                          "Set the console level. The console-level can be "
                              + "[ OFF | SEVERE | WARNING | INFO | CONFIG | FINE | FINER | FINEST]",
                          "<console-level>")
    {
      public void parsed (String consoleValue) throws OptionException
      {
        try
          {
            Level consoleLevel = Level.parse(consoleValue);
            // Need to set both the console and the main logger as
            // otherwize the console won't see the log messages.

            System.out.println("console " + consoleLevel);
            Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(consoleLevel);
            logger.addHandler(consoleHandler);
            logger.setLevel(consoleLevel);
            System.out.println(consoleHandler);

          }
        catch (IllegalArgumentException e)
          {
            throw new OptionException("Invalid log console: " + consoleValue);
          }

      }
    });
    parser.add(new Option(
                          "level",
                          'l',
                          "Set the log level. The log-level can be "
                              + "[ OFF | SEVERE | WARNING | INFO | CONFIG | FINE | FINER | FINEST]",
                          "<log-level>")
    {
      public void parsed (String arg0) throws OptionException
      {
        levelValue = arg0;
        try
          {
            level = Level.parse(levelValue);
            logger.setLevel(level);
          }
        catch (IllegalArgumentException e)
          {
            throw new OptionException("Invalid log level: " + levelValue);
          }
      }
    });
  }

  public void run (int pid)
  {
    Manager.host.requestRefreshXXX(true);

    // XXX: Should get a message back when the refresh has finished and the
    // proc has been found.
    Manager.eventLoop.runPending();
    proc = Manager.host.getProc(new ProcId(pid));

    if (null == proc)
      {
        System.out.println("Couldn't get the proc");
        System.exit(1);
      }

    procTasksObserver = new ProcTasksObserver(proc,
                                              new StackTasksObserver(proc));
    Manager.eventLoop.start();
  }

  private final void removeObservers (Task task)
  {
    task.requestDeleteClonedObserver(procTasksObserver);
    // task.requestDeleteTerminatedObserver(procTasksObserver);
  }

  public final void storeTask (Task task)
  {
    if (null != task)
      {  
        try
          {
            LinkedList list = new LinkedList();
		list.add(new String("Task #" + task.getTid()));
            int count = 0;
            for (StackFrame frame = StackFactory.createStackFrame(task); frame != null; frame = frame.getOuter())
              {
                // FIXME: do valgrind-like '=== PID ===' ?
                String output = "#" + count + " 0x"
                                + Long.toHexString(frame.getAddress()) + " in "
                                + frame.getMethodName() + " ()";

                if (frame.getSourceFile() != null)
                  output = output + " from " + frame.getSourceFile();
                
                list.add(output);
                count++;
              }

		if (null == sortedTasks)
			sortedTasks = new TreeMap();
            sortedTasks.put(task.getName(), list);
          }
        catch (TaskException _)
          {
            // FIXME: log exception, or rethrow?
            writer.println("... couldn't print stack trace");
          }
      }
  }

  public final void printTasks () 
  {
    Iterator iter = sortedTasks.values().iterator();
    while (iter.hasNext())
    {      
      LinkedList output = (LinkedList) iter.next();
      Iterator i = output.iterator();
      while (i.hasNext())
        {
          String s = (String) i.next();
          writer.println(s);
        }
    }
    
  
  }
  
  private class StackTasksObserver
      implements ProcObserver.ProcTasks
  {
    private LinkedList taskList;

    public StackTasksObserver (Proc proc)
    {
      taskList = proc.getTasks();
    }

    public void existingTask (Task task)
    {

      // Print the stack frame for this stack.
      storeTask(task);
      // Remove this task from the list of tasks.
      if (taskList.contains(task))
        {
          taskList.remove(task);
        }

      if (0 == taskList.size())
        {
          //Print all the tasks in order.
          printTasks();
          //Remove the observer from this proc.
          removeObservers(task);
    	}
    }
    public void taskAdded (Task task)
    {
      // TODO Auto-generated method stub

      // Bonus task:
      storeTask(task);
      removeObservers(task);

      if (taskList.contains(task))
        {
          taskList.remove(task);
        }

      if (0 == taskList.size())
        {
          removeObservers(task);
        }
    }

    public void taskRemoved (Task task)
    {
      // TODO Auto-generated method stub
      if (taskList.contains(task))
        {
          System.out.println("Task was removed before stack trace could occur");

          taskList.remove(task);
        }

      if (0 == taskList.size())
        {
          removeObservers(task);
        }

      // Should I be able to print a stack frame here?
      storeTask(task);

      // Do I need to remove the observer from a dead task?
      removeObservers(task);
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
      // TODO Auto-generated method stub
      Manager.eventLoop.requestStop();
    }

  }

  public static void main (String[] args)
  {

    parser = new Parser("fstack", "1.0", true)
    {
      protected void validate() throws OptionException{
        if (0 == pid) {
          throw new OptionException ("no pid provided");
        }
      }
    };
    addOptions(parser);
    parser.setHeader("Usage: fstack <PID>");

    parser.parse(args, new FileArgumentCallback()
    {
      public void notifyFile (String arg) throws OptionException
      {
        try
          {
            if (0 == pid)
              {
                pid = Integer.parseInt(arg);
              }
            else
              {
                throw new OptionException("too many pids");
              }

          }
        catch (Exception _)
          {
            throw new OptionException("couldn't parse pid");
          }
      }
    });

    if (null != levelValue)
      {
        logger.setLevel(level);
      }

    fstack stacker = new fstack();

    stacker.setWriter(new PrintWriter(System.out, true));
    stacker.run(pid);

  }
}
