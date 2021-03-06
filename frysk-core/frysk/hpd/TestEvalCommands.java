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
import frysk.testbed.FryskAsm;
import frysk.isa.ISA;
import frysk.isa.registers.Register;
import frysk.isa.ElfMap;
import java.io.File;

/**
 * Test the functionality of the print command; for instance that the
 * formatting options work.
 *
 * The intent here is not to test underlying code such as the type
 * parser or the formatter; that is the responsibility of underlying
 * tests.
 */

public class TestEvalCommands extends TestLib {
    public void testUnattached() {
	e = new HpdTestbed();
	// Add with no process; shouldn't crash.
	e.sendCommandExpectPrompt("print 2+2", "4\r\n");
    }

    public void testFormatInteger() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("print 17 -format d", "\r\n17\r\n");
    }
    public void testFormatInteger_d() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("print 17 -format d", "\r\n17\r\n");
    }
    public void testFormatInteger_b() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("print 17 -format b", "\r\n10001\r\n");
    }
    public void testFormatInteger_o() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("print 17 -format o", "\r\n021\r\n");
    }
    public void testFormatInteger_x() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("print 17 -format x", "\r\n0x11\r\n");
    }
    public void testFormatUnknown() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("print 17 -format unknown",
				  "unrecognized format: unknown\r\n");
    }
    public void testAddressOf() {
	e = HpdTestbed.attachXXX("hpd-c");
	e.sendCommandExpectPrompt("print &static_int", "\r\n.+0x[0-9a-f]+\r\n");
    }
    public void testDereference() {
	e = HpdTestbed.attachXXX("hpd-c");
	e.sendCommandExpectPrompt("print *static_int_ptr", "\r\n4\r\n");
    }
    public void testRegister() {
	// FIXME: Should use funit-regs.
	e = HpdTestbed.attachXXX("hpd-c");
	File exe = Prefix.pkgLibFile("hpd-c");
	ISA isa = ElfMap.getISA(exe);
	Register r = FryskAsm.createFryskAsm(isa).REG0;
	e.sendCommandExpectPrompt("print $" + r.getName() + " -format d",
				  "\r\n[0-9]+\r\n");
    }

    public void testPrintLocation() {
	e = HpdTestbed.attachXXX("hpd-c");
	e.sendCommandExpectPrompt
	    ("print static_char -location", 
	     "\r\nAddress 0x[0-9a-f]+ - [0-9]+ byte\\(s\\).*");
    }
    
    public void testPrintLocationFails() {
	e = HpdTestbed.attachXXX("hpd-c");
	e.sendCommandExpectPrompt
	    ("print bogus -location\n", 
	     "\r\nError: Object bogus was not found.*");
    }

    public void testPrintType() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("print 2+2 -type", "long\r\n");
    }
    public void testPrintTypeFails() {
	e = new HpdTestbed();
	e.sendCommandExpectPrompt("print bogus -type",
				  "Error: no values\r\n");
    }
}
