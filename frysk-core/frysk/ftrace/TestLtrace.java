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

package frysk.ftrace;

import java.io.File;
import java.util.*;
import java.util.regex.*;

import frysk.Config;
import frysk.junit.TestCase;
import frysk.proc.Syscall;
import frysk.proc.Task;

/**
 * This is a test for basic ltrace capabilities.
 */
public class TestLtrace
    extends TestCase
{
  class DummyLtraceObserver
    implements LtraceObserver
  {
    public void funcallEnter(Task task, Symbol symbol, Object[] args) { }
    public void funcallLeave(Task task, Symbol symbol, Object retVal) { }
    public void syscallEnter(Task task, Syscall syscall, Object[] args) { }
    public void syscallLeave(Task task, Syscall syscall, Object retVal) { }
    public void fileMapped(Task task, File file) { }
    public void fileUnmapped(Task task, File file) { }
    public void taskAttached(Task task) { }
    public void taskTerminated(Task task, boolean signal, int value) { }
  }

  public void performTestAllLibrariesGetDetected()
  {
    class MyController1
      implements LtraceController
    {
      public ArrayList allLibraries = new ArrayList();
      public void fileMapped(final Task task, final ObjectFile objf, final Ltrace.Driver driver)
      {
	allLibraries.add(objf.getSoname());
      }
    }

    String[] cmd = {Config.getPkgLibFile("funit-empty").getPath()};
    MyController1 controller = new MyController1();
    new Ltrace(controller).trace(cmd);

    String[] expectedSonames = {"libc\\.so\\.6", "ld-linux.*\\.so\\.2", "funit-empty"};
    for (int i = 0; i < expectedSonames.length; ++i)
      {
	boolean found = false;
	for (Iterator it = controller.allLibraries.iterator(); it.hasNext(); )
	  {
	    String soname = (String)it.next();
	    if (Pattern.matches(expectedSonames[i], soname))
	      {
		found = true;
		break;
	      }
	  }
	assertTrue("library with pattern `" + expectedSonames[i] + "' found", found);
      }
    assertEquals("number of recorded libraries", expectedSonames.length, controller.allLibraries.size());
  }

    public void testDebugStateMappingGuard()
    {
	boolean save = MappingGuard.enableSyscallObserver;
	MappingGuard.enableSyscallObserver = false;
	assertTrue("debugstate observer enabled", MappingGuard.enableDebugstateObserver);
	performTestAllLibrariesGetDetected();
	MappingGuard.enableSyscallObserver = save;
    }

    public void testSyscallMappingGuard()
    {
	boolean save = MappingGuard.enableDebugstateObserver;
	MappingGuard.enableDebugstateObserver = false;
	assertTrue("syscall observer enabled", MappingGuard.enableSyscallObserver);
	performTestAllLibrariesGetDetected();
	MappingGuard.enableDebugstateObserver = save;
    }

  public void testCallRecorded()
  {
    if(unresolvedOffUtrace(5053))
      return;

    class MyController2
      implements LtraceController
    {
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
    }

    Ltrace ltrace = new Ltrace(new MyController2());

    class MyObserver extends DummyLtraceObserver {
      public ArrayList events = new ArrayList();
      public void funcallEnter(Task task, Symbol symbol, Object[] args) {
	events.add("enter " + symbol.name);
      }
      public void funcallLeave(Task task, Symbol symbol, Object retVal) {
	events.add("leave " + symbol.name);
      }
    }
    MyObserver observer = new MyObserver();

    String[] cmd = {Config.getPkgLibFile("funit-syscalls").getPath()};
    ltrace.addObserver(observer);
    ltrace.trace(cmd);

    String[] expectedEvents = {
      "enter __libc_start_main",
      "enter open(64)?",
      "leave open(64)?",
      "enter close",
      "leave close"
    };
    for (int i = 0; i < expectedEvents.length; ++i)
      {
	String event = (String)observer.events.get(i);
	assertTrue("event `" + event + "' detected",
		   Pattern.matches(expectedEvents[i], event));
      }
    assertEquals("number of recorded events", expectedEvents.length, observer.events.size());
  }

    public void testArgumentsCorrect1()
    {
	if(unresolvedOffUtrace(5053))
	    return;

	final Set registeredSymbols = new HashSet();
	final LinkedList expectedEvents = new LinkedList();
	class ExpectedEvent {
	    String name;
	    long[] arguments;
	    long retval;

	    ExpectedEvent(String name, long[] arguments, long retval) {
		this.name = name;
		this.retval = retval;
		this.arguments = arguments;
		registeredSymbols.add(name);
		expectedEvents.addLast(this);
	    }
	}

	new ExpectedEvent("trace_me_1", new long[]{3, 5, 7, 11, 13, 17}, 56);
	new ExpectedEvent("trace_me_2", new long[]{3, 5, 7, 11, 13, 17}, 56);

	class MyController3
	    implements LtraceController
	{
	    public void fileMapped(final Task task, final ObjectFile objf, final Ltrace.Driver driver)
	    {
		if (!task.getProc().getExe().equals(objf.getFilename().getPath()))
		    return;

		try {
		    objf.eachTracePoint(new ObjectFile.TracePointIterator() {
			    public void tracePoint(TracePoint tp) {
				if (registeredSymbols.contains(tp.symbol.name))
				    driver.tracePoint(task, tp);
			    }
			}, TracePointOrigin.SYMTAB);
		}
		catch (lib.dwfl.ElfException ee) {
		    ee.printStackTrace();
		}
	    }
	}
	Ltrace ltrace = new Ltrace(new MyController3());

	class MyObserver3 extends DummyLtraceObserver {
	    LinkedList expectedReturns = new LinkedList();
	    public void funcallEnter(Task task, Symbol symbol, Object[] args) {
		ExpectedEvent ee = (ExpectedEvent)expectedEvents.removeFirst();
		assertEquals("enter function name", ee.name, symbol.name);
		for (int i = 0; i < ee.arguments.length; ++i) {
		    assertTrue("argument #" + i + " of function " + ee.name + " is a number",
			       args[i] instanceof Number);
		    // If ^^ this fails, ltrace probably grew brans to
		    // answer all kinds of objects, not just sixtuples
		    // of integers.

		    assertEquals("argument #" + i + " of function " + ee.name,
				 ee.arguments[i], ((Number)args[i]).longValue());
		}
		expectedReturns.add(ee);
	    }
	    public void funcallLeave(Task task, Symbol symbol, Object retVal) {
		ExpectedEvent ee = (ExpectedEvent)expectedReturns.removeLast();
		assertEquals("leave function name", ee.name, symbol.name);
		assertTrue("return value of function " + ee.name + " is a number",
			   retVal instanceof Number);
		assertEquals("return value of function " + ee.name,
			     ee.retval, ((Number)retVal).longValue());
	    }
	}
	MyObserver3 observer = new MyObserver3();

	String[] cmd = {Config.getPkgLibFile("funit-calls").getPath()};
	ltrace.addObserver(observer);
	ltrace.trace(cmd);

	assertEquals("number of unprocessed expects", 0, expectedEvents.size());
	assertEquals("number of unprocessed returns", 0, observer.expectedReturns.size());
    }

    public void tearDown()
    {
    }
}
