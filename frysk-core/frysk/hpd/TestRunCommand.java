// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import frysk.config.Prefix;

/**
 * This class tests the "run" command.
 */

public class TestRunCommand extends TestLib {
    public void testRunCommand() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
	"\\[0\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("run ",
		"Attached to process ([0-9]+).*Running process ([0-9]+).*");
	try { Thread.sleep(1000); } catch (Exception e) {}
	e.sendCommandExpectPrompt("focus","Target set.*\\[0\\.0\\]\t\t([0-9]+)" +
		"\t([0-9]+).*\\[0\\.1\\]\t\t([0-9]+)\t([0-9]+).*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    /**
     * Test running the run command twice, the second time should kill the previous process
     * and restart it and run until a breakpoint is found or the process blows up.
     */
    public void testRunTimesTwo() {
	if (unresolved(5615))
	    return;
	e = new HpdTestbed();
	e.send("load " + Prefix.pkgLibFile("funit-threads-looper").getPath() + "\n");
	e.expect("\\[0\\.0\\] Loaded executable file.*" + prompt);
	e.send("run\n");
	e.expect("Attached to process ([0-9]+).*");
	e.expect("Running process ([0-9]+).*" + prompt);
	//e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
	//"Loaded executable file.*");
	//e.sendCommandExpectPrompt("run ",
	//	"Attached to process ([0-9]+).*Running process ([0-9]+).*");
	//try { Thread.sleep(1000); } catch (Exception e) {}
	e.send("viewset \\[0\\.0\\]\n");
	e.expect("Set \\[0\\.0\\].*");
	//e.expect(5,"Target set.*\\[0\\.0\\]\t\t([0-9]+)\t([0-9]+)\r\n" +
	//	"\\[0\\.1\\]\t\t([0-9]+)\t([0-9]+)\r\n" + prompt);
	//e.sendCommandExpectPrompt("focus","Target set.*\\[0\\.0\\]\t\t([0-9]+)" +
	//	"\t([0-9]+)\r\n" + "\\[0\\.1\\]\t\t([0-9]+)\t([0-9]+)\r\n");
	//try { Thread.sleep(1000); } catch (Exception e) {}
	e.sendCommandExpectPrompt("run", "Killing process ([0-9])+.*running.*" +
		"Attached to process ([0-9]+).*Running process ([0-9]+).*");
	//e.send("quit\n");
	//e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    public void testRunCommandParameter() {
	e = new HpdTestbed();
	String[] param = { "-testing", "parameter2", "-g"};
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-parameters").getPath(),
	"\\[0\\.0\\] Loaded executable file.*");
	String parameters = "";
	for (int i = 0; i < param.length; i++) {
	    parameters = parameters + param[i] + " ";
	}
	e.sendCommandExpectPrompt("run " + parameters,
		"Attached to process ([0-9]+).*Running process ([0-9]+).*");
	/*
	 * The following wait is added to make the test pass.  It seems on a dual-core
	 * machine the funit-parameters process gets put into a different CPU and gets behind
	 * the test case.  funit-parameters creates a file that this test checks and when this
	 * test gets ahead of it that, the test fails because it cannot find it.  Delaying
	 * 1 second seems to fix that problem.
	 */
	try { Thread.sleep(1000); } catch (Exception e) {}
	int file_length = 0;
	String compare = "";
	for (int i = 0; i < param.length; i++) {
	    compare = compare + param[i];
	    file_length = file_length + param[i].length();
	}
	byte[] buffer = new byte[file_length];
	String paramlist = "";
	try {
	    File f = new File("param-test");
	    FileInputStream fin = new FileInputStream(f);
	    fin.read(buffer);
	    paramlist = new String(buffer, 0, buffer.length);
	    f.delete();
	} catch (FileNotFoundException e) {
	    System.out.println("Could not find param-test");
	} catch (IOException e) {
	    System.out.println("Error reading file param-test");
	}
	assertTrue("Testing passed parameters", paramlist.equals(compare));
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    /**
     * This test case tests a kind of corner case where a single threaded process gets loaded and
     * then right after a two-threaded looping process gets loaded.  Then a 'run' command is issued
     * and the first process runs to completion.  If the 'run' command is issued again it should
     * just rerun the currently running process and place in in the same place in the target set.
     */
    public void testRunCommandTwoProcesses() {
	if (unresolved(5984))
	    return;
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-hello").getPath(),
	"\\[0\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
	"\\[1\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("focus", "Target set.*\\[0\\.0\\]\t\t0\t0.*"+
	"\\[1\\.0\\]\t\t0*\\t0.*");
	e.sendCommandExpectPrompt("run", "Attached to process ([0-9]+).*Attached to process ([0-9]+).*" +
		"running.*" + "Running process ([0-9]+).*running.*Running process ([0-9]+).*");
	e.sendCommandExpectPrompt("run", "Killing process ([0-9]+).*Killing process ([0-9]+).*" +
		"Attached to process ([0-9]+).*running.*Running process ([0-9]+).*running.*" +
		"Running process ([0-9]+).*");
	e.sendCommandExpectPrompt("focus", "Target set.*\\[0\\.0\\]\t\t([0-9]+)\t([0-9]+).*" +
		"\\[0\\.1\\]\t\t([0-9]+).*\\t([0-9]+).*\\[1\\.0\\]\t\t([0-9]+)\t([0-9]+).*" + 
		"\\[1\\.1\\]\t\t([0-9]+).*\\t([0-9]+).*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    /**
     * This test case tests to make sure the run command pays attention to the "focus"
     * command.
     */
    
    public void testRunFocus() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-hello").getPath(),
	"\\[0\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
	"\\[1\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("focus [1.0]", "Creating new HPD notation set.*");
	e.sendCommandExpectPrompt("run", "running.*" + "Attached to process ([0-9]+).*" +
		"Running process ([0-9]+).*");
	e.sendCommandExpectPrompt("load", "\\[0\\.0\\].*funit-hello.*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    /**
     * This test case gives the run command a specific targetset to run
     * 
     */
    
    public void testRunHpdParam() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-hello").getPath(),
	"\\[0\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
	"\\[1\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
	"\\[2\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("[1.0] run", "Creating new.*" + "running.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	try { Thread.sleep(1000); } catch (Exception e) {}
	//e.sendCommandExpectPrompt("load", "\\[0\\.0\\].*funit-hello.*\\[2\\.0\\].*funit-threads.*");
	e.sendCommandExpectPrompt("focus", "Target set.*\\[0\\.0\\]\t\t0\t0.*" +
		"\\[1\\.0\\]\t\t([0-9]+).*\\t([0-9]+).*\\[1\\.1\\]\t\t([0-9]+)\t([0-9]+).*" + 
		"\\[2\\.0\\]\t\t0\t0.*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    /**
     * This test case tests passing of different parameters
     * 
     */
    public void testDiffParams() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
	"\\[0\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("run -arg1 -arg2", "running.*-arg1 -arg2.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	try { Thread.sleep(500); } catch (Exception e) {}
	e.sendCommandExpectPrompt("run -arg3", "running.*-arg3.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	try { Thread.sleep(500); } catch (Exception e) {}
	e.sendCommandExpectPrompt("run -arg4 -arg5", "running.*-arg4 -arg5.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    /**
     * This test case tests passing of "--" to abort passing of any args
     * 
     */
    public void testNoParms() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
	"\\[0\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("run -arg1 -arg2", "running.*-arg1 -arg2.*" + 
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	try { Thread.sleep(500); } catch (Exception e) {}
	//e.sendCommandExpectPrompt("run --", "running.*threads-looper \\r\\n" +
	//	"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	e.send("run --\n");
	e.expect("Killing process.*");
	e.expect("\\[0\\.0\\] Loaded executable file.*");
	e.expect("running.*threads-looper\\r\\n");
	e.expect("Attached to process ([0-9]+).*");
	try { Thread.sleep(1000); } catch (Exception e) {}
	//e.expect("Running process ([0-9]+).*");
	//try { Thread.sleep(500); } catch (Exception e) {}
	//e.sendCommandExpectPrompt("run -arg4 -arg5", "running.*-arg4 -arg5.*" +
	//	"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    public void testLoadedParams() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-parameters").getPath() +
		" abc def ghi", "\\[0\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("run", "running.*abc def ghi.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	try { Thread.sleep(500); } catch (Exception e) {}
	e.sendCommandExpectPrompt("run -arg3", "running.*-arg3.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	try { Thread.sleep(500); } catch (Exception e) {}
	e.sendCommandExpectPrompt("run -arg4 -arg5", "running.*-arg4 -arg5.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    public void testFhpdLoadedParams() {
	if(unresolved(6576))
	    return;
	
	String[] command = new String[] {Prefix.pkgLibFile("funit-parameters").getPath(),
					 "zzz",
					 "yyy"};
    	e = new HpdTestbed(command);
	//e = new HpdTestbed(Prefix.pkgLibFile("funit-parameters").getPath() + " zzz yyy", 
	//	"Loaded executable file.*funit-parameters.*");
	e.sendCommandExpectPrompt("run", "running.*zzz yyy.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	try { Thread.sleep(500); } catch (Exception e) {}
	e.sendCommandExpectPrompt("run -arg3", "running.*-arg3.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	try { Thread.sleep(500); } catch (Exception e) {}
	e.sendCommandExpectPrompt("run -arg4 -arg5", "running.*-arg4 -arg5.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
}