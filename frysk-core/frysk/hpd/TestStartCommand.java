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
* This class tests the "start" command.
*/

public class TestStartCommand extends TestLib {
    public void testStartCommand() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
		"\\[0\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*");
	e.sendCommandExpectPrompt("focus", "\\[0\\.0\\].*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    /**
     * testStartCommandParameter tests to make sure parameters are properly passed
     * to the inferior when it is activated.  In this case, the inferior gets the
     * parameters and writes them to a file which this test case them compares to 
     * what it thinks it sent.
     */
    public void testStartCommandParameter() {
	e = new HpdTestbed();
	String[] param = { "teststart", "parameter2start", "-g"};
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-parameters").getPath(),
	"\\[0\\.0\\] Loaded executable file.*");
	String parameters = "";
	for (int i = 0; i < param.length; i++) {
	    parameters = parameters + param[i] + " ";
	}
	e.sendCommandExpectPrompt("start " + parameters, "Attached to process.*");
	e.sendCommandExpectPrompt("go",
		"Running process ([0-9]+).*");
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
     * This test case tests to make sure the start command pays attention to the "focus"
     * command.
     */
    
    public void testStartFocus() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-hello").getPath(),
	"\\[0\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-threads-looper").getPath(),
	"\\[1\\.0\\] Loaded executable file.*");
	e.sendCommandExpectPrompt("focus [1.0]", "Creating new HPD notation set.*");
	e.sendCommandExpectPrompt("start", "Attached to process ([0-9]+).*" +
		"starting.*");
	e.sendCommandExpectPrompt("load", "\\[0\\.0\\].*funit-hello.*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    /**
     * This test case gives the start command a specific targetset to run
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
	e.sendCommandExpectPrompt("[1.0] start", "Attached to process ([0-9]+).*" + "Creating new.*" +
		"starting.*");
	try { Thread.sleep(1000); } catch (Exception e) {}
	e.sendCommandExpectPrompt("focus", "Target set.*\\[0\\.0\\]\t\t0\t0.*" +
		"\\[1\\.0\\]\t\t([0-9]+).*\\t([0-9]+).*\\[2\\.0\\]\t\t0\t0.*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
}