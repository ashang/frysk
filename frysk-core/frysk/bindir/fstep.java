// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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


import frysk.Config;
import frysk.proc.*;
import lib.opcodes.*;
import gnu.classpath.tools.getopt.*;
import java.util.*;

public class fstep
  implements TaskObserver.Attached,
  TaskObserver.Instruction,
  TaskObserver.Terminated
{
  // The (shared) Disassembler for all Tasks.
  private static Disassembler disassembler;

  // When to print instructions.  Zero means always, any positive
  // number is the number of instructions after which we print another
  // sample.
  private static int sample;

  // How many instructions to print with each sample.
  // Defaults to 1.
  private static int instrs;

  // The command to execute
  static String[] command;

  // The process id to trace
  static int pid;

  // Tasks being observed mapped to the total number of steps.
  private static final HashMap tasks = new HashMap();

  public static void main(String[] args)
  {
    sample = 0;
    instrs = 1;

    final Parser parser = new Parser("fstep", Config.getVersion ());
    parser.add(new Option("sample", 's',
			  "how often to print the current instruction",
			  "samples")
      {
	public void parsed(String argument) throws OptionException
	{
	  try
	    {
	      sample = Integer.parseInt(argument);
	    }
	  catch (NumberFormatException nfe)
	    {
	      OptionException ex;
	      ex = new OptionException("sample must be a number");
	      ex.initCause(nfe);
	      throw ex;
	    }
	}
      });

    parser.add(new Option("instructions", 'i',
			  "how many instructions to print at each step/sample",
			  "instructions")
      {
	public void parsed(String argument) throws OptionException
	{
	  try
	    {
	      instrs = Integer.parseInt(argument);
	    }
	  catch (NumberFormatException nfe)
	    {
	      OptionException ex;
	      ex = new OptionException("instructions must be a number");
	      ex.initCause(nfe);
	      throw ex;
	    }
	}
      });

    parser.add(new Option("pid", 'p',
			  "the running process to step",
			  "pid")
      {
	public void parsed(String argument) throws OptionException
	{
	  try
	    {
	      pid = Integer.parseInt(argument);
	    }
	  catch (NumberFormatException nfe)
	    {
	      OptionException ex;
	      ex = new OptionException("pid must be a number");
	      ex.initCause(nfe);
	      throw ex;
	    }
	}
      });

    command = parser.parse(args);
    if ((command == null || command.length == 0)
	&& pid == 0)
      {
	System.err.println("fstep: Neither command line nor pid provided");
	parser.printHelp();
	System.exit(-1);
      }
    if (command != null && command.length != 0 && pid != 0)
      {
	System.err.println("fstep: Provide either a command line or a pid");
	parser.printHelp();
	System.exit(-1);
      }

    final fstep step = new fstep();
    if (pid != 0)
      {
	Manager.host.requestFindProc(new ProcId(pid), new Host.FindProc() {
	    
	    public void procFound (ProcId procId)
	    {
	      Proc proc = Manager.host.getProc(procId);
	      Task mainTask = proc.getMainTask();
	      mainTask.requestAddAttachedObserver(step);
	    }
	    
	    public void procNotFound (ProcId procId, Exception e)
	    {
	      System.err.println("no such process (" + pid + ") " + e);
	      parser.printHelp();
	      System.exit(-1);
	    }});
      }
    else
      Manager.host.requestCreateAttachedProc(command, step);
    
    Manager.eventLoop.start();
  }

  // TaskObserver.Attached interface
  public Action updateAttached(Task task)
  {
    // We only need one disassembler since all Tasks share their memory.
    if (disassembler == null)
      disassembler = new Disassembler(task.getMemory());

    tasks.put(task, Long.valueOf(0));

    task.requestAddTerminatedObserver(this);
    task.requestAddInstructionObserver(this);
    return Action.CONTINUE;
  }

  // TaskObserver.Terminated interface
  public Action updateTerminated(Task task, boolean signal, int exit)
  {
    int tid = task.getTid();
    long steps = ((Long) tasks.get(task)).longValue();
    System.err.println("Total steps [" + tid + "]: " + steps);
    if (signal)
      System.err.println("[" + tid + "] Terminated by signal: " + exit);
    else
      System.err.println("[" + tid + "] Exited: " + exit);

    tasks.remove(task);
    if (tasks.isEmpty())
      Manager.eventLoop.requestStop();
    return Action.CONTINUE;
  }

  // TaskObserver.Instruction interface
  public Action updateExecuted(Task task)
  {
    long steps = ((Long) tasks.get(task)).longValue();
    steps++;
    tasks.put(task, Long.valueOf(steps));

    try
      {
	if (sample == 0 || steps % sample == 0)
	  {
	    int tid = task.getTid();
	    long pc = task.getIsa().pc(task);
	    Iterator it;
	    it = disassembler.disassembleInstructions(pc, instrs).iterator();
	    while (it.hasNext())
	      System.out.println("[" + tid + "]\t" + it.next());
	  }
      }
    catch (OpcodesException oe)
      {
	// Bad location?
	oe.printStackTrace();
	System.exit(-1);
      }
    return Action.CONTINUE;
  }


  // Common interface methods

  public void addedTo (Object observable)
  {
    // Unused
  }

  public void addFailed (Object observable, Throwable w)
  {
    w.printStackTrace();
    System.exit(-1);
  }

  public void deletedFrom (Object observable)
  {
    // Unused
  }
}
