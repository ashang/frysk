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

package frysk.testbed;

import frysk.proc.Task;
import frysk.isa.Register;
import frysk.symtab.SymbolFactory;

/**
 * frysk-asm.h provides a simple macro language for writing assembler;
 * this class provides corresponding local definitions that match that
 * assembler.
 */

public class TestFryskAsm extends TestLib {

    private Task task;
    private FryskAsm regs;
    public void setUp() {
	super.setUp();
	task = new DaemonBlockedAtSignal("funit-asm").getMainTask();
	regs = FryskAsm.createFryskAsm(task.getISA());
    }
    public void tearDown() {
	task = null;
	regs = null;
	super.tearDown();
    }

    private void check(String name, long value, Register reg) {
	assertEquals(name, value, task.getRegister(reg));
    }

    public void testREG0() {
	check("REG0", 1, regs.REG0);
    }
    public void testREG1() {
	check("REG1", 2, regs.REG1);
    }
    public void testREG2() {
	check("REG2", 3, regs.REG2);
    }
    public void testREG3() {
	check("REG3", 4, regs.REG3);
    }

    public void testREG() {
	for (int i = 0; i < regs.REG.length; i++) {
	    check("REG" + i, i + 1, regs.REG[i]);
	}
    }

    public void testPC() {
	assertEquals("PC", "crash",
		     SymbolFactory.getSymbol(task, task.getRegister(regs.PC))
		     .getName());
    }

    public void testSP() {
	check("SP", 5, regs.SP);
    }

}
