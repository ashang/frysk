// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

import java.io.File;

import frysk.Config;
import frysk.expunit.Expect;
import frysk.testbed.CoreFileAtSignal;
import frysk.testbed.TearDownExpect;
import frysk.testbed.TestLib;

/**
 * This performs a "sniff" test of Fstack, confirming basic
 * functionality.
 */

public class TestFstack
    extends TestLib
{
    /**
     * Start FSTACK with both a core file and an executable; avoids
     * problems with Linux's bone-head COREFILE format storing only
     * the first 50 characters of the executable.
     */
    private Expect fstack(String program, String[] args) {
	File coreExe = Config.getPkgLibFile(program);
	File coreFile = CoreFileAtSignal.constructCore(coreExe);
	String[] argv = new String[args.length + 3];
	int argc = 0;
	argv[argc++] = Config.getBinFile("fstack").getAbsolutePath();
	argv[argc++] = coreFile.getAbsolutePath();
	argv[argc++] = coreExe.getAbsolutePath();
	for (int i = 0; i < args.length; i++) {
	    argv[argc + i] = args[i];
	}
	Expect e = new Expect(argv);
	TearDownExpect.add(e);
	return e;
    }

    public void testBackTrace () {
	Expect e = fstack("funit-stack-outlined", new String[0]);
	// Just look for main.
	e.expect ("main");
    }
    
    public void testBackTraceWithParams () {
	Expect e = fstack("funit-stack-outlined",
			  new String[] { "-print", "params" });
        e.expect("\\#0 .* third\\(int arg3 = 3\\)");
        e.expect("\\#1 .* second\\(int arg2 = 2\\)");
        e.expect("\\#2 .* first\\(int arg1 = 1\\)");
        e.expect("\\#3 .* main\\(\\)");
    }

    public void testBackTraceWithScopes () {
	Expect e = fstack("funit-stack-outlined",
			  new String[] { "--print", "scopes" });
	e.expect("\\#0 .* third\\(\\)");
        e.expect("int var3");
	e.expect("\\#1 .* second\\(\\)");
        e.expect("int var2");
	e.expect("\\#2 .* first\\(\\)");
        e.expect("int var1");
	e.expect("\\#3 .* main\\(\\)");
        e.expect("int some_int");
    }

    public void testBackTraceWithFullpath () {
	Expect e = fstack("funit-stack-outlined",
			  new String[] { "--print", "fullpath" });
        e.expect (Config.getRootSrcDir()
		  + ".*"
		  + "funit-stack-outlined"
		  + ".c#");
    }

    public void testBackTraceWithDashA () {
	Expect e = fstack("funit-stack-outlined", new String[] { "-a" });
	e.expect("\\#0 .* in third\\(int arg3 = 3\\)"
		 + ".*" + Config.getRootSrcDir()
		 + ".*" + "funit-stack-outlined" + "\\.c#" 
		 + ".*int var3.*");
	e.expect("\\#1");
    }

    public void testBackTraceWithDashC () {
	Expect e = fstack("funit-stack-outlined", new String[] { "-c" });
	e.expect("\\#0 .* in third\\(int arg3 = 3\\)"
		 + ".*" + Config.getRootSrcDir()
		 + ".*" + "funit-stack-outlined" + "\\.c#");
	e.expect("\\#1");
  }

    public void testBackTraceWithDashV () {
	Expect e = fstack("funit-stack-inlined", new String[] { "-v", "-a" });
        e.expect("\\#0 .*third[^\\r\\n]*\\[inline\\]");
	e.expect("\\#1 .*second[^\\r\\n]*\\[inline\\]");
	e.expect("\\#2 .*first[^\\r\\n]*\\[inline\\]");
	e.expect("\\#3 .*main");
    }

}
