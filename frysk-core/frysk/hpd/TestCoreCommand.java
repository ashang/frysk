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
import frysk.testbed.CorefileFactory;
import frysk.testbed.SlaveOffspring;
//import frysk.testbed.DaemonBlockedAtSignal;
import frysk.config.Prefix;
//import frysk.proc.Proc;

public class TestCoreCommand extends TestLib {

    private final String corefile
	= Prefix.pkgDataFile("test-core-x86").getPath();

    public void testCoreCommand() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("core " + corefile + " -noexe",
				  "Attached to core file.*");
    }
    
    public void testCoreCommandError() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("core " + corefile,
				  "Error:.*");
    }
    
    public void testCoreCommandErrorTwo() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("core " + corefile + "foo",
				  "Error:.*");
    }

    public void testCoreExeCommand() {
	File exe = Prefix.pkgLibFile("funit-hello");
	File core = CorefileFactory.constructCoreAtSignal(exe);
	e = new HpdTestbed();
	e.sendCommandExpectPrompt(("core " + core.getPath()
				   + " " + exe.getPath()),
				  "Attached to core file.*");
    }
    
    public void testCoreThenRunCommand() {
	File exe = Prefix.pkgLibFile("funit-hello");
	File core = CorefileFactory.constructCoreAtSignal(exe);
	e = new HpdTestbed();
	e.sendCommandExpectPrompt(("core " + core.getPath()
				   + " " + exe.getPath()),
				  "Attached to core file.*");
	e.sendCommandExpectPrompt("run",
				  "Attached to process.*");
    }
    
    public void testCoreLoadedParams() {
	
	if (unresolved(6602))
	    return;
	String[] args = {"zzz", "yyy" };
	SlaveOffspring newProc = SlaveOffspring.createDaemon(args);
	int pid = newProc.getPid().intValue();
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("start", "starting.*zzz yyy.*" +
		"Attached to process ([0-9]+).*");
	e.sendCommandExpectPrompt("dump -a -o test_core." + pid, "Generating corefile.*");
	e.sendCommandExpectPrompt("unload -t 0", "Removed Target set \\[0\\].*");
	e.sendCommandExpectPrompt("core test_core." + pid, "Attached to core.*");
	e.sendCommandExpectPrompt("run", "running.*zzz yyy.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
    
    public void testCoreLoadedParamsTwo() {

	if (unresolved(6614))
	    return;
//	File exe = new File("/bin/echo");
	File exe = Prefix.pkgLibFile("funit-hello");
	File core = CorefileFactory.constructCoreAtSignal(exe, new String[] {"abcd"});
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("core " + core.getPath(), "Attached to core.*");
	e.sendCommandExpectPrompt("info args", "output");
	e.sendCommandExpectPrompt("run", "running.*abcd.*" +
		"Attached to process ([0-9]+).*" + "Running process ([0-9]+).*");
	e.send("quit\n");
	e.expect("Quitting\\.\\.\\.");
	e.close();
    }
}
