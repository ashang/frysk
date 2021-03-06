// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

import frysk.testbed.TearDownFile;
import frysk.testbed.TearDownExpect;
import frysk.testbed.TestLib;
import frysk.config.Prefix;
import java.io.File;

public class TestFexe extends TestLib {
    public void testExeOfPid() {
	File fexe = Prefix.binFile("fexe");
	// XXX: Some versions of bash (e.g., bash-3.2-20.fc8.x86_64)
	// will exec, instead of fork, a program if it is the only
	// command.  This leads to $$ pointing at the fexe process.
	// Work around it by forcing bash to execute two commands.
	TearDownExpect e = new TearDownExpect(new String[] {
		"/bin/bash",
		"-c",
		fexe.getAbsolutePath() + " $$ ; echo \"\""
	    });
	e.expect("/bin/bash" + "\r\n");
    }

    public void testExeOfExe() {
	TearDownExpect e = new TearDownExpect(new String[] {
		Prefix.binFile("fexe").getPath(),
		"-exe", "/bin/ls",
		"--",
		"arg0", "arg1"
	    });
	e.expect("/bin/ls" + "\r\n");
    }

    public void testExePath() {
	TearDownExpect e = new TearDownExpect(new String[] {
		"/bin/bash", "-c",
		"PATH=/bin " + Prefix.binFile("fexe").getPath() + " ls"
	    });
	e.expect("/bin/ls" + "\r\n");
    }

    public void testExeOfScript() {
	TearDownExpect e = new TearDownExpect(new String[] {
		"/bin/bash", "-c",
		Prefix.binFile("fexe").getPath()
		+ " "
		+ Prefix.binFile("fdebugrpm")
	    });
	e.expect("/bin/bash" + "\r\n");
    }

    public void testExeOfDeletedFile() {
	TearDownExpect e = new TearDownExpect();
	TearDownFile exe = TearDownFile.create();
	// Create a copy of sleep that is executable.
	e.send("cp /bin/sleep " + exe.getPath() + "\r");
	e.expect("cp .*\\$ ");
	e.send("chmod +x " + exe.getPath() + "\r");
	e.expect("chmod .*\\$ ");
	// Start sleep running; pid is in $!, save it.
	e.send(exe.getAbsolutePath() + " 1000 &\r");
	e.send("pid=$! ; echo pid=$pid\r");
	e.expect("pid=[0-9]+\r\n\\$ ");
	// Try fexe with the executable present.
	e.send(Prefix.binFile("fexe").getPath() + " $pid\r");
	e.expect(exe.getName() + "\r\n\\$ ");
	// Delete the executable
	e.send("rm -f " + exe.getPath() + "\r");
	e.expect("\\$ ");
	assertFalse("file exists", exe.stillExists());
	// Try fexe with the executable deleted
	e.send(Prefix.binFile("fexe").getPath() + " $pid\r");
	e.expect("The link /proc/[0-9]+/exe points to the deleted file .*" + exe.getName() + "\\r\n\\$ ");
    }
}
