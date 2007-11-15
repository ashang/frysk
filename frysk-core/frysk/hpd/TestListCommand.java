// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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


/**
 * Test the functionality of the alias and unalias commands.
 */

public class TestListCommand extends TestLib {
    public void setUp() {
	super.setUp();
	e = new HpdTestbed();
    }

    public void testListPC() {
	if (unresolved(5332))
	    return;
	e = HpdTestbed.run("hpd-c");
        e.send("break main\n");
        e.expect("breakpoint.*" + prompt);
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	e.send("list\n");
	e.expect(".*208.*int.*209.*int.*210.*int_21.*211.*int_21.*215.*int.*" +
		 "216.*main.*\\*.*218.*if.*219.*fprintf" +
		 ".*221.*int.*222.*int.*.*223.*return.*224.*");
        e.send("quit\n");
        e.expect("quit.*\nQuitting...");
        e.close();
    }

    public void testListFunction() {
	if (unresolved(5333))
	    return;
	e = HpdTestbed.run("hpd-c");
        e.send("break main\n");
        e.expect("breakpoint.*" + prompt);
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	e.send("list\n");
	e.expect(prompt);
	e.send("list func_2\n");
	e.expect(
		 ".*158.*159.*typedef.*160.*static_class_t.*161.*enum.*" +
		 "162.*static_class_t.*163.*struct.*164.*asm.*165.*struct.*" +
		 "166.*asm.*167.*struct.*168.*struct.*169.*typedef.*" +
		 "170.*volatile.*171.*long.*172.*int.*173.*float.*174.*char.*" +
		 "175.*static_class_t.*176.*volatile.*177.*sportscar.*" +
		 prompt);
	e.send("list -length 10\n");
	e.expect(
		".*178.*char.*179.*short.*180.*int.*181.*long.*182.*float.*" +
		"183.*double.*184.*assign_long_arr.*185.*assign_int_arr.*");
        e.send("quit\n");
        e.expect("quit.*\nQuitting...");
        e.close();
    }

    public void testListReverse() {
	e = HpdTestbed.run("hpd-c");
        e.send("break main\n");
        e.expect("breakpoint.*" + prompt);
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	e.send("list\n");
	e.expect(prompt);
	e.send("list -length -10\n");
	e.expect(".*198.*while.*202.*return.*205.*static.*206.*func_1.*207");
	e.send("list -length -10\n");
	e.expect(".*188.*sportscar.*190.*int_p.*191.*class_p.*192.*class_p.*" +
		 "193.*class_p.*194.*class_1.*196.*int_21.*197.*");
        e.send("quit\n");
        e.expect("quit.*\nQuitting...");
        e.close();
    }

    public void testListErrors() {
	e = HpdTestbed.run("hpd-c");
        e.send("break main\n");
        e.expect("breakpoint.*" + prompt);
        e.send("go\n");
        e.expect("go.*\n" + prompt + "Breakpoint");
	e.send("list x\n");
	e.expect(".*function x not found.*" + prompt);
	e.send("list xyz\n");
	e.expect(".*symbol xyz not found.*" + prompt);
        e.send("quit\n");
        e.expect("quit.*\nQuitting...");
        e.close();
    }
}



		
