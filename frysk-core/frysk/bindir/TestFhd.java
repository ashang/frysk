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

import frysk.junit.TestCase;
import frysk.expunit.Expect;
import frysk.Config;
import java.io.File;

/**
 * This performs a "sniff" test of Fstack, confirming basic
 * functionality.
 */

public class TestFhd
    extends TestCase
{
    Expect e;
    Expect child;
    public void tearDown ()
    {
	if (e != null)
	    e.close ();
	e = null;
	if (child != null)
	    child.close ();
	child = null;
    }

    public void testHpd ()
    {
	if (brokenXXX (4001))
	    return;
	child = new Expect (new String[]
	    {
		new File (Config.getPkgLibDir (), "hpd-c").getPath ()
	    });
	String prompt = "\\(fhpd\\) ";
	e = new Expect (new String[]
	    {
		new File (Config.getBinDir (), "fhpd").getPath ()
	    });
	e.expect (prompt);
	// Add
	e.send ("print 2+2\n");
	e.expect ("\r\n4\r\n" + prompt);
	// Attach
	e.send ("attach loop.x " + child.getPid () + " -cli\n");
	e.expect ("attach.*" + prompt);
	// Where
	e.send ("where\n");
	e.expect ("where.*#0.*" + prompt);
	// int_21
	e.send ("print int_21\n");
	e.expect ("print.*2.*" + prompt);
	// Up
	e.send ("up\n");
	e.expect ("up.*#1.*" + prompt);
	// int_21
	e.send ("print int_21\n");
	e.expect ("print.*21.*(fhpd)");
	// char_21
	e.send ("print char_21\n");
	e.expect ("print.*a.*" + prompt);
	// Down
	e.send ("down\n");
	e.expect ("down.*#0.*" + prompt);
	// long_21
	e.send ("print long_21\n");
	e.expect ("print.*10.*" + prompt);
	// float_21
	e.send ("print float_21\n");
	e.expect ("print.*1\\.1.*" + prompt);
	// double_21
	e.send ("print double_21\n");
	e.expect ("print.*1\\.2.*" + prompt);
	// static_int
	e.send ("print static_int\n");
	e.expect ("print.*4.*" + prompt);
	// static_class
	e.send ("print static_class\n");
	e.expect ("print.*12\\.34.*" + prompt);
	// class
	e.send ("print class_1\n");
	e.expect ("print.*15.*" + prompt);
	// arr_1
	e.send ("print arr_1\n");
	e.expect ("print.*30.=1.31.=2.*" + prompt);
	// arr_2
	e.send ("print arr_2\n");
	e.expect ("print.*4,4.=9.4,5.=0.*" + prompt);
	// arr_3
	e.send ("print arr_3\n");
	e.expect ("print.*3,3.=10\\.8.3,4.=1.9.*" + prompt);
	e.send ("print arr_4\n");
	// arr_4
	e.expect ("print.*" + prompt);
    }

    public void testRunCppParser ()
    {
	if (brokenXXX (4002))
	    return;
	// XXX: This is not good must be able to run in both build and
	// install trees.
	File runCppParser = new File ("./frysk/expr/RunCppParser");
	String prompt = "\\$ ";
	e = new Expect (new String[]
	    {
		runCppParser.getAbsolutePath ()
	    });
	e.expect (prompt);
	// rcp assign
	e.send ("xyz=3\n");
	e.expect ("xyz=3.*" + prompt);
	// rcp assign
	e.send ("xya=4\n");
	e.expect ("xya=4.*" + prompt);
	// rcp tab
	e.send ("xy\t");
	e.expect ("xyz.*xya.*" + prompt);
    }
}
