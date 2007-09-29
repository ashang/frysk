// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.bindir;

import java.util.*;
import java.io.File;
import inua.util.PrintWriter;

import gnu.classpath.tools.getopt.*;

//import frysk.Config;
//import frysk.EventLogger;
import frysk.proc.*;
import frysk.sys.Sig;
import frysk.util.CommandlineParser;

import frysk.ftrace.*;

public class fltrace
{
  // The command to execute
  static String[] command;

  // The process id to trace
  static int pid;

  // Command and arguments to exec.
  ArrayList commandAndArguments;

  // True if a PID was requested.
  boolean requestedPid;

  //Where to send output.
  PrintWriter writer;

  Ltrace tracer = new Ltrace(new LtraceController() {
      public void fileMapped(final Task task, final ObjectFile objf, final Ltrace.Driver driver)
      {
	if (!task.getProc().getExe().equals(objf.getFilename().getPath()))
	  return;

	try {
	  objf.eachTracePoint(new ObjectFile.TracePointIterator() {
	      public void tracePoint(TracePoint tp) {
		driver.tracePoint(task, tp);
	      }
	    }, TracePointOrigin.PLT);
	}
	catch (lib.dwfl.ElfException ee) {
	  ee.printStackTrace();
	}
      }
    });

  LtraceObserver ltraceObserver = new LtraceObserver() {
      private Map levelMap = new HashMap();

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

      private void eventEntry(Task task, Object item, String eventType,
			      String eventName, Object[] args)
      {
	int level = this.getLevel(task);
	String spaces = repeat(' ', level);
	this.setLevel(task, ++level);

        if (lineOpened())
	  System.err.println('\\');

    	System.err.print("[" + task.getTaskId().intValue() + "] "
			 + spaces + eventType + " ");
	System.err.print(eventName + "(");
	for (int i = 0; i < args.length; ++i)
	  System.err.print((i > 0 ? ", " : "") + args[i]);
    	System.err.print(")");

        updateOpenLine(task, item);
      }

      private void eventLeave(Task task, Object item, String eventType,
			      String eventName, Object retVal)
      {
	int level = this.getLevel(task);
	this.setLevel(task, --level);

        if (!myLineOpened(task, item))
	  {
            if (lineOpened())
	      System.err.println();
	    String spaces = repeat(' ', level);
	    System.err.print("[" + task.getTaskId().intValue() + "] "
			     + spaces + eventType + " " + eventName);
	  }

	System.err.println(" = " + retVal);

        updateOpenLine(null, null);
      }

      private void eventSingle(Task task, String eventName)
      {
	int pid = task.getTid();
	int level = this.getLevel(task);

        if (lineOpened())
	  System.err.println("\\");
	System.err.println("[" + pid + "] " + repeat(' ', level) + eventName);

        updateOpenLine(null, null);
      }

      public synchronized void funcallEnter(Task task, Symbol symbol, Object[] args)
      {
	String symbolName = symbol.name;
	String callerLibrary = symbol.getParent().getSoname();
	String eventName = callerLibrary + "->" + /*libraryName + ":" +*/ symbolName;
	eventEntry(task, symbol, "call", eventName, args);
      }

      public synchronized void funcallLeave(Task task, Symbol symbol, Object retVal)
      {
	eventLeave(task, symbol, "leave", symbol.name, retVal);
      }

      public synchronized void syscallEnter(Task task, Syscall syscall, Object[] args)
      {
	eventEntry(task, syscall, "syscall", syscall.getName(), args);
      }

      public synchronized void syscallLeave(Task task, Syscall syscall, Object retVal)
      {
	eventLeave(task, syscall, "syscall leave", syscall.getName(), retVal);
      }

      public synchronized void fileMapped(Task task, File file)
      {
	eventSingle(task, "map " + file);
      }

      public synchronized void fileUnmapped(Task task, File file)
      {
	eventSingle(task, "unmap " + file);
      }

      public synchronized void taskAttached(Task task)
      {
	eventSingle(task, "attached");
      }

      public synchronized void taskTerminated(Task task, boolean signal, int value)
      {
	if (lineOpened())
	  System.err.println('\\');
	int pid = task.getTid();
	System.err.print("[" + pid + "] ");
	if (signal)
	  System.err.println("killed by " + Sig.toPrintString(value));
	else
	  System.err.println("+++ exited (status " + value + ") +++");
      }
    };

  public static void main(String[] args)
  {
    (new fltrace()).run(args);
  }

  public void run(String[] args)
  {
    CommandlineParser parser = new CommandlineParser("fltrace") {
	protected void validate() throws OptionException {
	  if (! requestedPid && commandAndArguments == null)
	    throw new OptionException("no command or PID specified");
	}

	public void parseCommand (String[] command)
	{
	 commandAndArguments = new ArrayList();

	 for (int i = 0; i < command.length; i++)
	   commandAndArguments.add(command[i]);
	}

	public void parsePids (ProcId[] pids)
	{
	  for (int i = 0; i < pids.length; ++i)
	    tracer.addTracePid(pids[i]);
	  requestedPid = true;
	}
    };

    parser.add(new Option('c', "trace children as well") {
	public void parsed(String arg0) throws OptionException
	{
	    tracer.setTraceChildren();
	}
    });

    parser.add(new Option('p', "pid to trace", "PID") {
	public void parsed(String arg) throws OptionException
	{
	    try {
		int pid = Integer.parseInt(arg);
		tracer.addTracePid(new ProcId(pid));
		requestedPid = true;
	    }
	    catch (NumberFormatException e) {
		OptionException oe = new OptionException("couldn't parse pid: " + arg);
		oe.initCause(e);
		throw oe;
	    }
	}
    });

    parser.add(new Option('S', "also trace signals") {
	public void parsed(String arg) throws OptionException
	{
	  tracer.setTraceSignals();
	}
    });

    parser.setHeader("usage: fltrace [OPTIONS] COMMAND ARGS...");

    command = parser.parse(args);

    if (writer == null)
	writer = new PrintWriter(System.out);

    tracer.addObserver(ltraceObserver);
    if (commandAndArguments != null)
    {
	String[] cmd = (String[]) commandAndArguments.toArray(new String[0]);
	tracer.trace(cmd);
    }
    else
	tracer.trace();
  }
}
