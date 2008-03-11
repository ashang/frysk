// This file is part of the program FRYSK.
//
// Copyright 2008, Red Hat Inc.
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

package frysk.sysroot;

import java.io.File;

import frysk.config.Config;
import frysk.testbed.TestLib;


public class TestSysroot extends TestLib {
    public void testGetPathViaDefaultRoot() {
	String pkgLibDir = Config.getPkgLibFile(".").getParent();
	Sysroot sysroot = new Sysroot(new File("/"));
	File testPath = sysroot.getPathViaDefaultRoot("funit-addresses", System.getenv("PATH") + ":" + pkgLibDir);
	assertEquals("getPathViaDefaultRoot relative via PATH", testPath.getPath(), pkgLibDir + "/funit-addresses");
	testPath = sysroot.getPathViaDefaultRoot("frysk/pkglibdir/funit-addresses", System.getenv("PATH") + ":" + pkgLibDir);
	assertEquals("getPathViaDefaultRoot relative", testPath.getPath(), "frysk/pkglibdir/funit-addresses");
	testPath = sysroot.getPathViaDefaultRoot(pkgLibDir + "/funit-addresses", System.getenv("PATH") + ":" + pkgLibDir);
	assertEquals("getPathViaDefaultRoot absolute", testPath.getPath(), pkgLibDir + "/funit-addresses");
    }
    
    public void testGetPathViaSysroot() {
	String sysrootPath = Config.getPkgLibFile(".").getParent() + "/test-sysroot";
	Sysroot sysroot = new Sysroot(new File(sysrootPath));
	File testPath = sysroot.getPathViaSysroot("funit-addresses", System.getenv("PATH") + ":/usr/bin");
	assertEquals("getPathViaSysroot relative via PATH", testPath.getPath(), sysrootPath + "/usr/bin/funit-addresses");
	testPath = sysroot.getPathViaDefaultRoot("frysk/pkglibdir/funit-addresses", System.getenv("PATH") + ":/usr/bin");
	assertEquals("getPathViaDefaultRoot relative", testPath.getPath(), "frysk/pkglibdir/funit-addresses");
	testPath = sysroot.getPathViaDefaultRoot(sysrootPath + "/funit-addresses", System.getenv("PATH") + ":/usr/bin");
	assertEquals("getPathViaDefaultRoot absolute", testPath.getPath(), sysrootPath + "/funit-addresses");
    }

    public void testGetSourcePathViaDefaultRoot() {
	Sysroot sysroot = new Sysroot(new File("/"));
	File absoluteSourcePath = new File("/frysk/src/frysk-core/frysk/pkglibdir/funit-addresses.c");
	File relativeSourcePath = new File("../../src/frysk-core/frysk/pkglibdir/funit-addresses.c");
	File compilationDir = new File("/frysk/bld/frysk-core");
	File testPath = sysroot.getSourcePathViaDefaultRoot (compilationDir, absoluteSourcePath);
	assertEquals("getSourcePathViaDefaultRoot absolute", testPath, absoluteSourcePath);
	testPath = sysroot.getSourcePathViaDefaultRoot (compilationDir, relativeSourcePath);
	assertEquals("getSourcePathViaDefaultRoot relative", testPath, absoluteSourcePath);
    }

    public void testGetSourcePathViaSysroot() {
	String sysrootPath = Config.getPkgLibFile(".").getParent() + "/test-sysroot";
	Sysroot sysroot = new Sysroot(new File(sysrootPath));
	File absoluteSourcePath = new File("/frysk/src/frysk-core/frysk/pkglibdir/funit-addresses.c");
	File relativeSourcePath = new File("../../src/frysk-core/frysk/pkglibdir/funit-addresses.c");
	File compilationDir = new File("/frysk/bld/frysk-core");
	File correctPath = new File(sysrootPath + absoluteSourcePath.getPath());
	File testPath = sysroot.getSourcePathViaSysroot (compilationDir, absoluteSourcePath);
	assertEquals("getSourcePathViaDefaultRoot absolute", 0, testPath.compareTo(correctPath));
	testPath = sysroot.getSourcePathViaSysroot (compilationDir, relativeSourcePath);
	assertEquals("getSourcePathViaDefaultRoot relative", 0, testPath.compareTo(correctPath));
    }
}
