// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008, Red Hat Inc.
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
import java.io.IOException;

import frysk.config.Config;
import frysk.testbed.CorefileFactory;
import frysk.testbed.TearDownExpect;
import frysk.testbed.TestLib;

/**
 * This performs a "sniff" test of Fstack, confirming basic
 * functionality.
 */

public class TestFstack extends TestLib {
    /**
     * Start FSTACK with both a core file and an executable; avoids
     * problems with Linux's bone-head COREFILE format storing only
     * the first 50 characters of the executable.
     */
    private TearDownExpect fstack(String program, String[] args) {
	File coreExe = Config.getPkgLibFile(program);
	File coreFile = CorefileFactory.constructCoreAtSignal(coreExe);
	String[] argv = new String[args.length + 3];
	int argc = 0;
	argv[argc++] = Config.getBinFile("fstack").getAbsolutePath();
	argv[argc++] = coreFile.getAbsolutePath();
	argv[argc++] = coreExe.getAbsolutePath();
	for (int i = 0; i < args.length; i++) {
	    argv[argc + i] = args[i];
	}
	return new TearDownExpect(argv);
    }

    private String getCanonicalAbsRootSrcDir () {
	try {
	    return new File(Config.getAbsRootSrcDir()).getCanonicalPath();
	} catch (IOException e) {
	    return Config.getAbsRootSrcDir();
	}
    }
    
    public void testBackTrace () {
	TearDownExpect e = fstack("funit-stack-outlined", new String[0]);
	// Just look for main.
	e.expect ("main");
    }
    
    public void testBackTraceWithDebugNamesAndParams() {
	TearDownExpect e = fstack("funit-stack-outlined", new String[] {
		"-print", "debug-names,params"
	    });
	e.expect("\\#0 .* in third\\(int arg3\\) .*\\/funit-stack-outlined\\.c#");
	e.expect("\\#1");
    }

    public void testBackTraceWithFullPath () {
	TearDownExpect e = fstack("funit-stack-outlined", new String[] {
		"-rich", "-print", "full-path"
	    });
        e.expect (getCanonicalAbsRootSrcDir()
		  + ".*"
		  + "funit-stack-outlined"
		  + ".c#");
    }

    public void testBackTraceWithInline() {
	TearDownExpect e = fstack("funit-stack-inlined",
				  new String[] { "--print", "inline" });
	e.expect("\\#0 .* third");
	e.expect("\\#1 .* second");
	e.expect("\\#2 .* first");
	e.expect("\\#3 .* main");
    }

    public void testBackTraceWithLocals() {
	TearDownExpect e = fstack("funit-stack-outlined",
			  new String[] { "-print", "locals" });
        e.expect("\\#0 .* third\\(");
        e.expect("int var3 = ");
        e.expect("\\#1 .* second\\(");
        e.expect("int var2 = ");
        e.expect("\\#2 .* first\\(");
        e.expect("int var1 = ");
        e.expect("\\#3 .* main\\(");
        e.expect("int some_int = ");
    }

    public void testBackTraceWithParams () {
	TearDownExpect e = fstack("funit-stack-outlined",
			  new String[] { "-print", "params" });
        e.expect("\\#0 .* third\\(int arg3.*\\)");
        e.expect("\\#1 .* second\\(int arg2.*\\)");
        e.expect("\\#2 .* first\\(int arg1.*\\)");
        e.expect("\\#3 .* main\\(\\)");
    }

    public void testBackTraceWithRich() {
	TearDownExpect e = fstack("funit-stack-inlined",
				  new String[] { "-rich" });
        e.expect("\\#0 .* third\\(int arg3.*\\)");
        e.expect("\\#1 .* second\\(int arg2.*\\)");
        e.expect("\\#2 .* first\\(int arg1.*\\)");
        e.expect("\\#3 .* main\\(\\)");
    }

    public void testBackTraceWithRichWithoutInline() {
	TearDownExpect e = fstack("funit-stack-inlined", new String[] {
		"-rich", "-print", "-inline"
	    });
        e.expect("\\#0 .* main\\(\\)");
    }

    public void testBackTraceWithLite() {
	TearDownExpect e = fstack("funit-stack-inlined",
				  new String[] { "-lite" });
        e.expect("\\#0 .*main");
    }

    public void testBackTraceWithNumberFrames5() {
	TearDownExpect e = fstack("funit-long-stack", new String[]{
		"-number-of-frames", "5"
	    });
	e.expect("\\#0 .*crash[^\\r\\n]*");
	e.expect("\\#1 [^\r\n]*first[^\\r\\n]*");
	e.expect("\\#2 [^\r\n]*first[^\\r\\n]*");
	e.expect("\\#3 [^\r\n]*first[^\\r\\n]*");
	e.expect("\\#4 [^\r\n]*first[^\\r\\n]*");
	e.expect("...");
    }
	
    public void testBackTraceWithNumberFrames4() {
	TearDownExpect e = fstack("funit-long-stack", new String[] {
		"-number-of-frames", "4"
	    });
	e.expect("\\#0 .*crash[^\\r\\n]*");
	e.expect("\\#1 [^\r\n]*first[^\\r\\n]*");
	e.expect("\\#2 [^\r\n]*first[^\\r\\n]*");
	e.expect("\\#3 [^\r\n]*first[^\\r\\n]*");
	e.expect("...");
    }
	
    public void testBackTraceWithNumberFrames0() {
	TearDownExpect e = fstack("funit-long-stack", new String[]{
		"-number-of-frames", "0"
	    });
	e.expect("\\#51 .*first[^\\r\\n]*");
    }
    
    public void testBackTraceWithNumberFramesAll() {
	TearDownExpect e = fstack("funit-long-stack", new String[]{
		"-number-of-frames", "all"
	    });
	e.expect("\\#51 .*first[^\\r\\n]*");
    }
    
    public void testBackTraceWithRichNumberOfFrames() {
	TearDownExpect e = fstack("funit-long-stack", new String[] {
		"-rich", "-number-of-frames", "5"
	    });
	e.expect("\\#0 .*crash[^\\r\\n]*");
	e.expect("\\#1 .*first[^\\r\\n]*");
	e.expect("\\#2 .*first[^\\r\\n]*");
	e.expect("\\#3 .*first[^\\r\\n]*");
	e.expect("\\#4 .*first[^\\r\\n]*");
	e.expect("...");
	e.close();
    }
}
