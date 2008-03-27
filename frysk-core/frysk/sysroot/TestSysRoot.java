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

public class TestSysRoot extends TestLib {
    public void testGetPathViaDefaultRoot() {
	String pkgLibDir = Config.getPkgLibFile(null).getPath();
	SysRoot sysroot = new SysRoot(new File("/"));
	SysRootFile testPath = sysroot.getPathViaSysRoot("funit-addresses", 
		"/alpha/dir:/beta/dir:/gamma/dir:" + pkgLibDir);
	int testValue = testPath.getSysRootedFile().getPath().compareTo(pkgLibDir + "/funit-addresses");
	assertEquals("getPathViaSysRoot default root relative via PATH", 0, testValue);
	testPath = sysroot.getPathViaSysRoot(
		"frysk/pkglibdir/funit-addresses", 
		"/alpha/dir:/beta/dir:/gamma/dir:" + pkgLibDir);
	testValue = testPath.getSysRootedFile().getPath().compareTo("frysk/pkglibdir/funit-addresses");
	assertEquals("getPathViaSysRoot default root relative", 0, testValue);
	testPath = sysroot.getPathViaSysRoot(
		pkgLibDir + "/funit-addresses", 
		"/alpha/dir:/beta/dir:/gamma/dir:" + pkgLibDir);
	testValue = testPath.getSysRootedFile().getPath().compareTo(pkgLibDir + "/funit-addresses");
	assertEquals("getPathViaSysRoot default root absolute", 0, testValue);
    }

    public void testGetPathViaSysRoot() {
	String sysRootPath = Config.getPkgLibFile(".").getParent()
		+ "/test-sysroot";
	SysRoot sysRoot = new SysRoot(new File(sysRootPath));
	SysRootFile testPath = sysRoot.getPathViaSysRoot("funit-addresses", 
		"/alpha/dir:/beta/dir:/gamma/dir:/usr/bin");
	int testValue = testPath.getSysRootedFile().compareTo(new File(sysRootPath + "/usr/bin/funit-addresses"));
	assertEquals("getPathViaSysroot relative via PATH", 0, testValue);
	testPath = sysRoot.getPathViaSysRoot("frysk/pkglibdir/funit-addresses", 
		"/alpha/dir:/beta/dir:/gamma/dir:/usr/bin");
	testValue = testPath.getSysRootedFile().compareTo(new File("frysk/pkglibdir/funit-addresses"));
	assertEquals("getPathViaSysRoot relative", 0, testValue);
	testPath = sysRoot.getPathViaSysRoot(sysRootPath + "/funit-addresses", 
		"/alpha/dir:/beta/dir:/gamma/dir:/usr/bin");
	testValue = testPath.getSysRootedFile().compareTo(new File(sysRootPath + "/funit-addresses"));
	assertEquals("getPathViaSysRoot absolute including SysRoot", 0, testValue);
	testPath = sysRoot.getPathViaSysRoot("/usr/bin/funit-addresses", 
		"/alpha/dir:/beta/dir:/gamma/dir:/usr/bin");
	testValue = testPath.getFile().compareTo(new File("/usr/bin/funit-addresses"));
	assertEquals("getPathViaSysRoot absolute", 0, testValue);
    }

    public void testGetSourcePathViaDefaultRoot() {
	SysRoot sysRoot = new SysRoot(new File("/"));
	File absoluteSourcePath = new File(
		"/frysk/src/frysk-core/frysk/pkglibdir/funit-addresses.c");
	File relativeSourcePath = new File(
		"../../src/frysk-core/frysk/pkglibdir/funit-addresses.c");
	File compilationDir = new File("/frysk/bld/frysk-core");
	SysRootFile testPath = sysRoot.getSourcePathViaSysRoot(compilationDir,
		absoluteSourcePath);
	assertEquals("getSourcePathViaSysRoot default root absolute", 
		testPath.getSysRootedFile(),
		absoluteSourcePath);
	testPath = sysRoot.getSourcePathViaSysRoot(compilationDir,
		relativeSourcePath);
	assertEquals("getSourcePathViaSysRoot default root relative", 
		testPath.getSysRootedFile(),
		absoluteSourcePath);
    }

    public void testGetSourcePathViaSysRoot() {
	String sysRootPath = Config.getPkgLibFile(".").getParent()
		+ "/test-sysroot";
	SysRoot sysRoot = new SysRoot(new File(sysRootPath));
	File absoluteSourcePath = new File(
		"/frysk/src/frysk-core/frysk/pkglibdir/funit-addresses.c");
	File relativeSourcePath = new File(
		"../../src/frysk-core/frysk/pkglibdir/funit-addresses.c");
	File compilationDir = new File("/frysk/bld/frysk-core");
	File correctPath = new File(sysRootPath + absoluteSourcePath.getPath());
	SysRootFile testPath = sysRoot.getSourcePathViaSysRoot(compilationDir,
		absoluteSourcePath);
	assertEquals("getSourcePathViaSysRoot absolute", 0, 
		testPath.getSysRootedFile().compareTo(correctPath));
	testPath = sysRoot.getSourcePathViaSysRoot(compilationDir,
		relativeSourcePath);
	assertEquals("getSourcePathViaSysRoot relative", 0, 
		testPath.getSysRootedFile().compareTo(correctPath));
    }
}