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

import frysk.config.Prefix;

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
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-quicksort").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*");
	e.sendCommandExpectPrompt("break #funit-quicksort.c#98", "breakpoint.*");
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	// Matching the list expected output is a trial and error process
	// as the matching tends to be greedy.
	e.send("list\n");
	e.expect("88.*88.*littlest = sortlist");
	e.expect("89.*89");
	e.expect("90.*90");
	e.expect(".*91.*91.*int sortlist.element_count . 1.;");
	e.expect("92.*92");
	e.expect("93.*93.*main");
	e.expect("94.*94.*{");
	e.expect("95.*95");
	e.expect("96.*96.*biggest, littlest");
	e.expect("97.*97");
	e.expect("->  98.*98.*init_array");
	e.expect("99.*99.*quicksort");
        e.send("quit\n");
        e.expect("quit.*\nQuitting...");
        e.close();
    }



    public void testListFunction() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-quicksort").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*");
	e.sendCommandExpectPrompt("break main", "breakpoint.*");
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	e.send("list\n");
	e.expect(prompt);
	e.send("list quicksort\n");
	e.expect("51.*51");
	e.expect("52.*52.*int");
	e.expect("53.*53");
        e.expect("54.*54.*i = l;");
	e.expect("55.*55.*j = r;");
	e.expect("56.*56.*x = a");
	e.expect("57.*57.*do");
	e.expect("58.*58");
	e.expect("59.*59.*while..a");
	e.expect("60.*60.*while..x");
	e.expect("61.*61.*if");
	e.expect("62.*62");
	e.expect("63.*63.*w = a");
	e.expect("64.*64");
	e.expect("65.*65.* = w;");
	e.expect("66.*66.*i = i");
	e.expect("67.*67.*j = j - 1;");
	e.expect("68.*68");
	e.expect("69.*69.*while");
	e.expect("70.*70.*if");
	e.send("list 76 -length 10\n");
	e.expect("71.*71.*quicksort");
	e.expect("72.*72.*if");
	e.expect("73.*73 .*quicksort");
	e.expect("74.*74");
	e.expect("75.*75");
	e.expect("76.*76.*void");
	e.expect("77.*77.*init_array");
	e.expect("78.*78.*{");
	e.expect("79.*79.*int i");
	e.expect("80.*80.*unsigned int seed;");
        e.send("quit\n");
        e.expect("quit.*\nQuitting...");
        e.close();
    }

    public void testListReverse() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-quicksort").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*");
	e.sendCommandExpectPrompt("break main", "breakpoint.*");
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	e.send("list -length 3\n");
	e.expect("97.*97");
	e.expect("->  98.*98.*init_array");
	e.expect("99.*99.*quicksort");
	e.send("list 83 -length -10\n");
	e.expect("78.*78");
	e.expect("79.*79");
	e.expect("80.*80.*unsigned int seed");
	e.expect("81.*81.*element_count");
	e.expect("82.*82");
	e.expect("83.*83.*temp = rand_r");
	e.expect("84.*84.*sortlist");
	e.expect("85.*85.*if");
	e.expect("86.*86.*biggest");
	e.expect("87.*87.*else if");
	e.send("list 73 -length -10\n");
	e.expect("68.*68");
	e.expect("69.*69.*while");
	e.expect("70.*70");
	e.expect("71.*71");
	e.expect("72.*72");
	e.expect("73.*73.*quicksort");
	e.expect("74.*74");
	e.expect("75.*75");
	e.expect("76.*76.*void");
	e.expect("77.*77.*init_array");
        e.send("quit\n");
        e.expect("quit.*\nQuitting...");
        e.close();
    }

    public void testListFrames() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-quicksort").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*");
	e.sendCommandExpectPrompt("break #funit-bubblesort.c#49", "breakpoint.*");
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	e.send("list\n");
	e.expect("39.*39");
	e.expect("40.*40");
	e.expect("41.*41");
	e.expect("42.*42");
	e.expect("43.*43");
	e.expect("44.*44");
	e.expect("45.*45");
	e.expect("46.*46.*void");
	e.expect("47.*47.*bubblesort");
	e.expect("48.*48");
	e.expect(".*->  49.*49.*while");
	e.expect("50.*50");
	e.expect("51.*51.*int i");
	e.expect("52.*52.*int j");
	e.expect("53.*53.*while");
	e.expect("54.*54");
	e.expect("55.*55.*if");
	e.expect("56.*56.*j = a");
	e.expect("57.*57.*a.*= a");
	e.expect("58.*58.*a.*= j");
	e.send("down\n");
	e.send("list\n");
	e.expect("96.*96.*int");
	e.expect("97.*97");
	e.expect("98.*98.*init_array");
	e.expect("99.*99.*quicksort");
	e.expect("100.*100.*if");
	e.expect("101.*101");
	e.expect("102.*102.*return");
	e.expect("103.*103");
	e.expect("104.*104");
	e.expect("105.*105.*init_array");
	e.expect("-> 106.*106.*bubblesort");
	e.expect("107.*107.*if");
	e.expect("108.*108");
	e.expect("109.*109.*return 1");
	e.expect("110.*110");
	e.expect("111.*111.*return 0");
	e.expect("112.*112");
	e.send("up\n");
	e.send("list\n");
	e.expect("45.*45");
	e.expect("46.*46.*void");
	e.expect("47.*47.*bubblesort");
	e.expect("48.*48");
	e.expect("->.*49.*49.*while");
	e.expect("50.*50");
	e.expect("51.*51.*int i");
	e.expect("52.*52.*int j");
	e.expect("53.*53.*while");
	e.expect("54.*54");
	e.expect("55.*55.*if");
	e.expect("56.*56.*j = a");
	e.expect("57.*57.*a.*= a");
	e.expect("58.*58.*a.*= j");
        e.send("quit\n");
        e.expect("quit.*\nQuitting...");
        e.close();
    }

    public void testListErrors() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("load " + Prefix.pkgLibFile("funit-quicksort").getPath(),
		"Loaded executable file.*");
	e.sendCommandExpectPrompt("start", "Attached to process.*");
	e.sendCommandExpectPrompt("break main", "breakpoint.*");
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	e.sendCommandExpectPrompt("list x", ".*function x not found.*"); 
	e.sendCommandExpectPrompt("list xyz", ".*xyz not found.*");
	e.send("quit\n");
	e.expect("quit.*\nQuitting...");
        e.close();
    }
}
