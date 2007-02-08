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

public class TestFcore
    extends TestCase
{
    Expect e;
    public void tearDown ()
    {
	if (e != null)
	    e.close ();
	e = null;
    }

    public void testBadArguments ()
    {
	if (brokenXXX (4004))
	    return;
	e = new Expect (new String[] {
	    new File (Config.getBinDir (), "fcore").getAbsolutePath (),
	    "this is a bad argument"
	});
	e.expect ("fcore: Argument this is a bad argument does"
		  + " not appear to be a valid pid.");
    }

//     public void testBadConsoleParameter ()
//     {
// 	e = new Expect (new String[] {
// 	    new File (Config.getBinDir (), "fcore").getAbsolutePath (),
// 	    "-console", "frysk=SILLY",
// 	    "1"
// 	});
// 	e.expect ("fcore: Invalid log console: SILLY");
//     }

//     public void testBadLogParameter ()
//     {
// 	e = new Expect (new String[] {
// 	    new File (Config.getBinDir (), "fcore").getAbsolutePath (),
// 	    "-log", "frysk=SILLY",
// 	    "1"
// 	});
// 	e.expect ("fcore: Invalid log level: SILLY");
//     }

//     public void testGoodConsoleBadLogParameter ()
//     {
// 	e = new Expect (new String[] {
// 	    new File (Config.getBinDir (), "fcore").getAbsolutePath (),
// 	    "-console", "frysk=FINE",
// 	    "-log", "frysk=SILLY",
// 	    "1"
// 	});
// 	e.expect ("fcore: Invalid log level: SILLY");
//     }

//     public void testBadConsoleBadLogParameter ()
//     {
// 	e = new Expect (new String[] {
// 	    new File (Config.getBinDir (), "fcore").getAbsolutePath (),
// 	    "-console", "frysk=SILLY",
// 	    "-log", "frysk=SILLY",
// 	    "1"
// 	});
// 	e.expect ("fcore: Invalid log console: SILLY");
//     }

//     public void testBadConsoleGoodLogParameter ()
//     {
// 	e = new Expect (new String[] {
// 	    new File (Config.getBinDir (), "fcore").getAbsolutePath (),
// 	    "-console", "frysk=SILLY",
// 	    "-log", "frysk=FINE",
// 	    "1"
// 	});
// 	e.expect ("fcore: Invalid log console: SILLY");
//     }

//     public void testInvalidArgument ()
//     {
// 	e = new Expect (new String[] {
// 	    new File (Config.getBinDir (), "fcore").getAbsolutePath (),
// 	    "-z",
// 	    "1"
// 	});
// 	e.expect ("fcore: unrecognized option");
//     }
}
