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

import frysk.Config;

/**
 * Test the functionality of the alias and unalias commands.
 */

public class TestListCommand extends TestLib {
    public void setUp() {
	super.setUp();
	e = new HpdTestbed();
    }

    public void testListPC() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Config.getPkgLibFile("funit-quicksort").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*");
	e.sendCommandExpectPrompt("break main", "breakpoint.*");
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	// Matching the list expected output is a trial and error process
	// as the matching tends to be greedy.
	e.send("list\n");
	e.expect("114.*114");
	e.expect("115.*115.*printf");
	e.expect("116.*116.*return 1");
	e.expect("117.*117");
	e.expect("118.*118.*return 0");
	e.expect("119.*119");
	e.expect("120.*120");
	e.expect("121.*121");
	e.expect("122.*122.*main");
	e.expect("123.*123");
	e.expect("-> 124.*124");
	e.expect("125.*125");
        e.send("quit\n");
        e.expect("quit.*\nQuitting...");
        e.close();
    }



    public void testListFunction() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Config.getPkgLibFile("funit-quicksort").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*");
	e.sendCommandExpectPrompt("break main", "breakpoint.*");
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	e.send("list\n");
	e.expect(prompt);
	e.send("list quicksort\n");
	e.expect("81.*81");
	e.expect("82.*82.*int");
	e.expect("83.*83");
	e.expect("84.*84");
	e.expect("85.*85.*j =");
	e.expect("86.*86.*x =");
	e.expect("87.*87.*do");
	e.expect("88.*88");
	e.expect("89.*89");
	e.expect("90.*90.*while");
	e.expect("91.*91.*if");
	e.expect("92.*92");
	e.expect("93.*93.*w =");
	e.expect("94.*94");
	e.expect("95.*95.*a.j.");
	e.expect("96.*96.*i =");
	e.expect("97.*97.*j =");
	e.expect("98.*98");
	e.expect("99.*99.*while");
	e.expect("100.*100.*if");
	e.send("list 101 -length 10\n");
	e.expect("101.*101.*quicksort..a,l,j");
	e.expect("102.*102.*if");
	e.expect("103.*103.*quicksort..a,i,r");
	e.expect("104.*104");
	e.expect("105.*105");
	e.expect("106.*106");
	e.expect("107.*107");
	e.expect("108.*108.*int");
	e.expect("109.*109");
	e.expect("110.*110");
        e.send("quit\n");
        e.expect("quit.*\nQuitting...");
        e.close();
    }

    public void testListReverse() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Config.getPkgLibFile("funit-quicksort").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*");
	e.sendCommandExpectPrompt("break main", "breakpoint.*");
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	e.send("list\n");
	e.expect(prompt);
	e.send("list 104 -length -10\n");
	e.expect("104.*104");
	e.expect("105.*105");
	e.expect("106.*106");
	e.expect("107.*107");
	e.expect("108.*108.*int");
	e.expect("109.*109.*quick ");
	e.expect("110.*110");
	e.expect("111.*111.*init_array");
	e.expect("112.*112.*quicksort ");
	e.expect("113.*113.*if");
	e.send("list 94 -length -10\n");
	e.expect("94.*94.*a.i. =");
	e.expect("95.*95.*a.j. =");
	e.expect("96.*96.*i =");
	e.expect("97.*97.*j =");
	e.expect("98.*98");
	e.expect("99.*99.*while");
	e.expect("100.*100.*if .l");
	e.expect("101.*101.*quicksort .a,l,j");
	e.expect("102.*102.*if .i");
	e.expect("103.*103.*quicksort .a,i,r");
        e.send("quit\n");
        e.expect("quit.*\nQuitting...");
        e.close();
    }

    public void testListErrors() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Config.getPkgLibFile("funit-quicksort").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*");
	e.sendCommandExpectPrompt("break main", "breakpoint.*");
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	e.sendCommandExpectPrompt("list x", ".*function x not found.*"); 
	e.sendCommandExpectPrompt("list xyz", ".*symbol xyz not found.*");
	e.send("quit\n");
	e.expect("quit.*\nQuitting...");
        e.close();
    }
}
