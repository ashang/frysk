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

import frysk.config.Prefix;
import java.io.File;
import frysk.testbed.TearDownExpect;
import frysk.testbed.CorefileFactory;
import frysk.testbed.TestLib;

public class TestFmaps extends TestLib {
  
    /**
     * Start fmaps with both a core file and an executable; avoids
     * problems with Linux's bone-head COREFILE format storing only
     * the first 50 characters of the executable.
     */
    private TearDownExpect fmaps(String program, String[] args) {
	File coreExe = Prefix.pkgLibFile(program);
	File coreFile = CorefileFactory.constructCoreAtSignal(coreExe);
	String[] argv = new String[args.length + 3];
	int argc = 0;
	argv[argc++] = Prefix.binFile("fmaps").getAbsolutePath();
	argv[argc++] = coreFile.getAbsolutePath();
	argv[argc++] = coreExe.getAbsolutePath();
	for (int i = 0; i < args.length; i++) {
	    argv[argc + i] = args[i];
	}
	return new TearDownExpect(argv);
    }
    
    // Basic sniff test, are we getting output that looks like a map?
    // getMaps is tested in the frysk-core/proc namespalce
    public void testExeOfPid() {
	TearDownExpect e = fmaps("funit-stack-outlined", new String[0]);
	e.expect("funit-stack-outlined");
	
    }
}
