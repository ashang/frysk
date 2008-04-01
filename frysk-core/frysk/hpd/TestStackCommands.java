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

public class TestStackCommands extends TestLib {

    private void checkWhere(String program, String inline) {
        e = HpdTestbed.hpdTerminatingProgram(program);
	e.send("where\n");
	e.expect("\\#0 .*third" + inline);
	e.expect("\\#1 .*second" + inline);
	e.expect("\\#2 .*first" + inline);
	e.expect("\\#3 .*main");
	e.expectPrompt(".*");
    }
    public void testWhereVirtual () {
	checkWhere("funit-stack-inlined", "[^\\r\\n]*\\[inline\\]");
    }
    public void testWherePhysical() {
	checkWhere("funit-stack-outlined", "");
    }
    
    private void checkWhereWithLocals(String program) {
	e = HpdTestbed.hpdTerminatingProgram(program);
        e.send("where -locals\n");
        e.expect(".*var3");
	e.expect(".*var2");
	e.expect(".*var1");
	e.expectPrompt(".*");
        e.close();
    }
    public void testWhereWithVirtualLocals() {
	checkWhereWithLocals("funit-stack-inlined");
    }
    public void testWhereWithPhysicalLocals() {
	checkWhereWithLocals("funit-stack-outlined");
    }

    public void testWhereOne() {
        e = HpdTestbed.hpdTerminatingProgram("funit-stack-outlined");
	e.sendCommandExpectPrompt
	    ("where 1", "\\#0 .*third[^\\r\\n]*\\r\\n\\.\\.\\.\\r\\n");
    }

    public void testDownCompletion () {
	e = HpdTestbed.hpdTerminatingProgram("funit-stack-outlined");
	e.send ("d\t");
	e.expect (".*defset.*delete.*detach.*disable.*down.*" + prompt + ".*");
	e.sendCommandExpectPrompt("own", "\\#1.*");
    }

    public void testUpDown () {
	e = HpdTestbed.hpdTerminatingProgram("funit-stack-outlined");
	e.sendCommandExpectPrompt("where 1", "\\#0.*third.*");
	e.sendCommandExpectPrompt("down", "\\#1.*second.*");
	e.sendCommandExpectPrompt("down", "\\#2.*first.*");
	e.sendCommandExpectPrompt("up", "\\#1.*second.*");
    }

    public void testFrame() {
	e = HpdTestbed.hpdTerminatingProgram("funit-stack-outlined");
	e.sendCommandExpectPrompt("frame 3", "\\#3.*main.*");
	e.sendCommandExpectPrompt("frame 1", "\\#1.*second.*");
    }
}
