// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

package frysk.hpd;

import frysk.expunit.Expect;
import frysk.Config;

public class TestBreakpoints
    extends TestLib
{
    public void testHpdBreakpoint() {
	if (unresolved(5165))
	    return;
	child = new Expect(Config.getPkgLibFile("hpd-c"));
	e = new HpdTestbed();
	// Attach
	e.send ("attach " + child.getPid () + "\n\n");
	e.expect (5, "attach.*\n" + prompt);
	// Break
	e.send("break #hpd-c.c#196\n");	// This has to break on: while (int_21)
	e.expect("breakpoint.*" + prompt);
	e.send("go\n");
	e.expect("go.*\n" + prompt + "Breakpoint.*#hpd-c.c#196");
	e.send("quit\n");
	e.expect("quit.*\nQuitting...");
	e.close();
    }
    
    public void testHpdBreakpointRunProcess() {
	e = new HpdTestbed();
	// Attach
	e.send ("run " + Config.getPkgLibFile("hpd-c") + "\n\n");
	e.expect (5, "Attached.*\n" + prompt);
	// Break
	e.send("break #hpd-c.c#196\n");	// This has to break on: while (int_21)
	e.expect("breakpoint.*" + prompt);
	e.send("go\n");
	e.expect("go.*\n" + prompt + "Breakpoint.*#hpd-c.c#196");
	e.send("quit\n");
	e.expect("quit.*\nQuitting...");
	e.close();
    }

    public void testHpdBreakpointInline() {
	child = new Expect(Config.getPkgLibFile("test1"));
	e = new HpdTestbed();
	// Attach
	e.send ("attach " + child.getPid () + " -cli\n");
	e.expect ("attach.*" + prompt);
	// Break
	e.send("break add\n");
	e.expect("break.*" + prompt);
	e.send("go\n");
	e.expect("go.*" + prompt + ".*Breakpoint.*add.*");
	e.send("where\n");
	e.expect("where.*#0.* add *\\(.+\\).*" + prompt);
	e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }

    public void testHpdBreakpointLibrary() {
	child = new Expect(Config.getPkgLibFile("test1"));
	e = new HpdTestbed();
	// Attach
	e.send ("attach " + child.getPid () + " -cli\n");
	e.expect ("attach.*" + prompt);
	// Break
	e.send("break sin\n");
	e.expect("break.*" + prompt);
	e.send("go\n");
	e.expect("go.*" + prompt + ".*Breakpoint.*sin.*");
	e.send("where\n");
	e.expect("where.*#0.* (__)?sin \\(\\).*" + prompt);
	e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }

    public void testHpdBreakStep() {
	child = new Expect(Config.getPkgLibFile("test1"));
	e = new HpdTestbed();
	// Attach
	e.send ("attach " + child.getPid () + " -cli\n");
	e.expect ("attach.*" + prompt);
	// Break
        e.send("break anotherFunction\n");
        e.expect("break.*" + prompt);
        e.send("go\n");
	e.expect("go.*" + prompt + ".*Breakpoint.*anotherFunction.*");
        e.send("step\n");
        e.expect("step.*Task stopped at.*test1\\.c.*" + prompt);
        e.send("step\n");
        e.expect("step.*Task stopped at.*test1\\.c.*" + prompt);
        e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }

    public void testHpdBreakMultiThreaded() {
	e = new HpdTestbed();
        e.send ("run " + Config.getPkgLibFile("funit-fib-clone") + " 3\n\n");
        	e.expect (5, "Attached.*\n" + prompt);
	// Break
	e.send("break fib\n");	
	e.expect("breakpoint.*" + prompt);
	e.send("go\n");
        e.expect("go.*" + prompt + "Breakpoint.*fib.*");
        e.send("viewset\n");
        e.expect("viewset.*Target set.*pid.*id.*\\[0\\.0\\].*" + prompt);
        e.send("go\n");
        e.expect("go.*" + prompt);
        e.expect("(Breakpoint 0 fib.*){2}");
        e.send("viewset\n");
        e.expect("viewset.*Target set.*pid.*id.*\\[0\\.0\\].*\\[0\\.1\\].*\\[0\\.2\\].*"
                 + prompt);
        e.send("where\n");
        e.expect("where.*\\[0\\.0\\].*\\[0\\.1\\].*#0 0x[0-9a-f]+ in fib\\(.*\\[0\\.2\\].*#0 0x[0-9a-f]+ in fib\\(.*"
                 + prompt);
        e.send("quit\n");
        e.expect("Quitting...");
        e.close();
    }

    public void testHpdBreakMultiThreadedContinue() {
	e = new HpdTestbed();
        e.send ("run " + Config.getPkgLibFile("funit-fib-clone") + " 3\n\n");
        	e.expect (5, "Attached.*\n" + prompt);
	// Break
	e.send("break fib\n");	
	e.expect("breakpoint.*" + prompt);
	e.send("go\n");
        e.expect("go.*" + prompt + "Breakpoint.*fib.*");
        e.send("viewset\n");
        e.expect("viewset.*Target set.*pid.*id.*\\[0\\.0\\].*" + prompt);
        e.send("go\n");
        e.expect("go.*" + prompt);
        e.expect("(Breakpoint 0 fib.*){2}");
        e.send("viewset\n");
        e.expect("viewset.*Target set.*pid.*id.*\\[0\\.0\\].*\\[0\\.1\\].*\\[0\\.2\\].*"
                 + prompt);
        e.send("where\n");
        e.expect("where.*\\[0\\.0\\].*\\[0\\.1\\].*#0 0x[0-9a-f]+ in fib\\(.*\\[0\\.2\\].*#0 0x[0-9a-f]+ in fib\\(.*"
                 + prompt);
        e.send("go\n");
        e.expect("go.*" + prompt + "Task \\d+ is terminating");
        e.expect("(Breakpoint 0 fib.*){2}");
        e.send("go\n");
        e.expect("go.*" + prompt);
        e.expect("Task \\d+ is terminating");
        e.expect("Task \\d+ is terminating");
        e.expect("Task \\d+ is terminating");
        e.expect("fib \\(3\\) = 2");
        e.expect("Task \\d+ is terminating");
        e.expect("Task \\d+ terminated");
        e.send("quit\n");
        e.expect("Quitting...");
        e.close();
    }
}
