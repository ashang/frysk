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

package frysk.bindir;

import frysk.junit.TestCase;
import frysk.expunit.Expect;
import frysk.Config;
import java.io.File;
import frysk.testbed.TearDownExpect;

public class TestFauxv
    extends TestCase
{
    
    private Expect fauxv(File coreFile) {
	String[] argv = new String[2];
	int argc = 0;
	argv[argc++] = Config.getBinFile("fauxv").getAbsolutePath();
	argv[argc++] = coreFile.getAbsolutePath();
	Expect e = new Expect(argv);
	TearDownExpect.add(e);
	return e;
    }
    
    // Basic sniff test, are we getting output that looks like a auuv?
    // getAuxv is tested in the frysk-core/proc namespace/
    public void testAuxvCore() {
	Expect e  = fauxv(Config.getPkgDataFile("test-core-x86"));
	
	e.expect("AT_SYSINFO \\(SYSINFO\\) : 6464512");
	e.expect("AT_SYSINFO_EHDR \\(SYSINFO EHDR\\) : 0x62a000");
	e.expect("AT_HWCAP \\(Machine dependent hints about\\) : 0xafe9f1bf");
	e.expect("AT_PAGESZ \\(System page size\\) : 4096");
	e.expect("AT_CLKTCK \\(Frequency of times\\(\\)\\) : 100");
	e.expect("AT_PHDR \\(Program headers for program\\) : 0x8048034");
	e.expect("AT_PHENT \\(Size of program header entry\\) : 32");
	e.expect("AT_PHNUM \\(Number of program headers\\) : 8");
	e.expect("AT_BASE \\(Base address of interpreter\\) : 0");
	e.expect("AT_FLAGS \\(Flags\\) : 0");
	e.expect("AT_ENTRY \\(Entry point of program\\) : 0x80483e0");
	e.expect("AT_UID \\(Real uid\\) : 500");
	e.expect("AT_EUID \\(Effective uid\\) : 500");
	e.expect("AT_GID \\(Real gid\\) : 500");
	e.expect("AT_EGID \\(Effective gid\\) : 500");
	e.expect("AT_0x17 \\(AT_0x17\\) : 0");
	e.expect("AT_PLATFORM \\(String identifying platform.\\) : 0xbfcfee4b");
	e.expect("AT_NULL \\(End of vector\\) : 0");
	
    }
}
