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

  public void testAllLibrariesGetDetected()
  {
      if(unresolvedOnx8664(5048))
	  return;
      
    class MyFilter implements TracePointFilter {
      public HashSet allLibraries = new HashSet();
      public boolean tracePoint(Task task, TracePoint tp) {
	String soname = tp.symbol.getParent().getSoname();
	allLibraries.add(soname);
	return false;
      }
    }
    MyFilter filter = new MyFilter();
    Ltrace ltrace = new Ltrace(filter);

    String[] cmd = {Config.getPkgLibFile("funit-empty").getPath()};
    ltrace.addObserver(new DummyLtraceObserver() {
	public void fileMapped(Task task, File file) {
	}
      });
    ltrace.trace(cmd);

    String[] expectedSonames = {"libc.so.6", "ld-linux.so.2", "funit-empty"};
    for (int i = 0; i < expectedSonames.length; ++i)
      {
	String soname = expectedSonames[i];
	assertTrue("library `" + soname + "' found", filter.allLibraries.contains(soname));
      }
    assertEquals("number of recorded libraries", expectedSonames.length, filter.allLibraries.size());
  }

  public void testCallRecorded()
  {
      if(unresolvedOnx8664(5048))
	  return;
      
    class MyFilter2 implements TracePointFilter {
      public boolean tracePoint(Task task, TracePoint tp) {
	if (tp.origin != frysk.ftrace.TracePointOrigin.PLT)
	  return false;
	String symFilename = tp.symbol.getParent().getFilename().getPath();
	String taskFilename = task.getProc().getExe();
	return taskFilename.equals(symFilename);
      }
    }
    Ltrace ltrace = new Ltrace(new MyFilter2());

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

  public void tearDown()
  {
  }
}
