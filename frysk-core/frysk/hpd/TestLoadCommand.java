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

import frysk.Config;

/**
 * This class tests the "load" command basics of both loading a correct
 * executable and trying to load a non-existent executable.
 */

public class TestLoadCommand extends TestLib {
    public void testLoadCommand() {
	e = new HpdTestbed();
	e.send("load " + Config.getPkgDataFile("test-exe-x86").getPath()
		+ "\n");
	e.expect("Loaded executable file.*");
	e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }

    public void testLoadCommandError() {
	e = new HpdTestbed();
	e.send("load " + Config.getPkgDataFile("test-exe-x86").getPath()
		+ "foo\n");
	e.expect("File does not exist or is not readable*");
	e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }
    
    public void testLoadStart() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Config.getPkgLibFile("funit-hello").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("focus", "Target set.*\\[0\\.0\\]\t\t0\t0.*");
	e.sendCommandExpectPrompt("load " + Config.getPkgLibFile("funit-hello").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("focus", "Target set.*\\[0\\.0\\]\t\t0\t0.*"+
		"\\[1\\.0\\]\t\t0*\\t0.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*Attached to process.*");
	e.sendCommandExpectPrompt("focus", "Target set.*\\[0\\.0\\].*\\[1\\.0].*");
	e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }
    
    public void testLoadRunRun() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Config.getPkgLibFile("funit-threads-looper").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("load " + Config.getPkgLibFile("funit-threads-looper").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*Attached to process.*");
	e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }
    
    public void testLoadNoneLoaded() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load", "No loaded procs currently.*");
	e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }
    
    public void testLoadDisplay() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Config.getPkgLibFile("funit-threads-looper").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("load " + Config.getPkgLibFile("funit-hello").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("load", "Task Id ([0-9]+).*Task Id ([0-9]+).*");
	e.send("quit\n");
	e.expect("Quitting...");
	e.close();
    }
}
