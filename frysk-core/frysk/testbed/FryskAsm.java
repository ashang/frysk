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

import frysk.isa.Register;
import frysk.isa.IA32Registers;
import frysk.isa.X8664Registers;
import frysk.isa.ISA;
import frysk.isa.ISAMap;

/**
 * The file "include/frysk-asm.h" describes a simple abstract
 * load-store architecture implemented using native assembler.
 *
 * This class provides register definitions that match the underlying
 * frysk-asm definitions.
 */

public class FryskAsm {
    public final Register PC;
    public final Register SP;
    public final Register REG0;
    public final Register REG1;
    public final Register REG2;
    public final Register REG3;
    public final Register[] REG;
    private FryskAsm(Register PC, Register SP,
		     Register REG0, Register REG1,
		     Register REG2, Register REG3) {
	this.PC = PC;
	this.SP = SP;
	this.REG = new Register[] { REG0, REG1, REG2, REG3 };
	this.REG0 = REG0;
	this.REG1 = REG1;
	this.REG2 = REG2;
	this.REG3 = REG3;
    }
  
    public static final FryskAsm IA32 = new FryskAsm(IA32Registers.EIP,
						     IA32Registers.ESP,
						     IA32Registers.EAX,
						     IA32Registers.EBX,
						     IA32Registers.ECX,
						     IA32Registers.EDX);
    public static final FryskAsm X8664 = new FryskAsm(X8664Registers.RIP,
						      X8664Registers.RSP,
						      X8664Registers.RAX,
						      X8664Registers.RDI,
						      X8664Registers.RSI,
						      X8664Registers.RDX);
    private static final ISAMap isaToFryskAsm
	= new ISAMap("FryskAsm")
	.put(ISA.IA32, IA32)
	.put(ISA.X8664, X8664)
	;
    public static FryskAsm createFryskAsm(ISA isa) {
	return (FryskAsm) isaToFryskAsm.get(isa);
    }
}
