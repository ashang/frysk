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

package frysk.bank;

import inua.eio.ByteOrder;
import frysk.isa.IA32Registers;
import frysk.isa.X8664Registers;
import frysk.isa.X87Registers;

/**
 * Factory to create either IA32 or X8664 RegisterBanks.
 */

public class X86BankRegisters {

    public static final BankArrayRegisterMap IA32 = new BankArrayRegisterMap()
	.add(0, LinuxIA32RegisterBanks.REGS)
    // Get all FP registers from FXSAVE area.
	.add(2, LinuxIA32RegisterBanks.XFPREGS)
    // debug registers come from USR section
	.add(3, LinuxIA32RegisterBanks.USR)
	;

    public static final BankArrayRegisterMap X8664 = new BankArrayRegisterMap()
	.add(0, LinuxX8664RegisterBanks.REGS)
    // Format determined by FXSAVE instruction
	.add(1, LinuxX8664RegisterBanks.FPREGS)
    // debug registers
	.add(2, LinuxX8664RegisterBanks.USR)
	;

    public static BankArrayRegisterMap IA32_ON_X8664
	= new IndirectBankArrayRegisterMap(ByteOrder.LITTLE_ENDIAN, IA32, X8664)
	.add(IA32Registers.EAX, X8664Registers.RAX)
	.add(IA32Registers.EBX, X8664Registers.RBX)
	.add(IA32Registers.ECX, X8664Registers.RCX)
	.add(IA32Registers.EDX, X8664Registers.RDX)
	.add(IA32Registers.ESI, X8664Registers.RSI)
	.add(IA32Registers.EDI, X8664Registers.RDI)
	.add(IA32Registers.EBP, X8664Registers.RBP)
	.add(IA32Registers.CS)
	.add(IA32Registers.DS)
	.add(IA32Registers.ES)
	.add(IA32Registers.FS)
	.add(IA32Registers.GS)
	.add(IA32Registers.SS)
	.add(IA32Registers.ORIG_EAX, X8664Registers.ORIG_RAX)
	.add(IA32Registers.EIP, X8664Registers.RIP)
	.add(IA32Registers.EFLAGS,X8664Registers.RFLAGS)
	.add(IA32Registers.ESP, X8664Registers.RSP)
	.add(X87Registers.FCW)
	.add(X87Registers.FSW)
	.add(X87Registers.FTW)
	.add(X87Registers.FOP)
	.add(X87Registers.EIP, 1, 0x08, 4)
	.add(X87Registers.DP, 1, 0x10, 4)
	.add(X87Registers.MXCSR)
	.add(X87Registers.MXCSR_MASK)
	.add(X87Registers.ST0)
	.add(X87Registers.ST1)
	.add(X87Registers.ST2)
	.add(X87Registers.ST3)
	.add(X87Registers.ST4)
	.add(X87Registers.ST5)
	.add(X87Registers.ST6)
	.add(X87Registers.ST7)
	.add(X87Registers.XMM0)
	.add(X87Registers.XMM1)
	.add(X87Registers.XMM2)
	.add(X87Registers.XMM3)
	.add(X87Registers.XMM4)
	.add(X87Registers.XMM5)
	.add(X87Registers.XMM6)
	.add(X87Registers.XMM7)
	.add(IA32Registers.D0, X8664Registers.DR0)
	.add(IA32Registers.D1, X8664Registers.DR1)
	.add(IA32Registers.D2, X8664Registers.DR2)
	.add(IA32Registers.D3, X8664Registers.DR3)
	.add(IA32Registers.D4, X8664Registers.DR4)
	.add(IA32Registers.D5, X8664Registers.DR5)
	.add(IA32Registers.D6, X8664Registers.DR6)
	.add(IA32Registers.D7, X8664Registers.DR7)
	;
}
