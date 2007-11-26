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

package frysk.proc;

import inua.eio.ByteOrder;
import frysk.isa.IA32Registers;
import frysk.isa.X8664Registers;
import frysk.isa.X87Registers;

/**
 * Factory to create either IA32 or X8664 RegisterBanks.
 */

public class X86BankRegisters {

    public static final BankRegisterMap IA32 = new BankRegisterMap()
	.add(new BankRegister (0, 24, 4,IA32Registers.EAX))
	.add(new BankRegister (0, 0, 4, IA32Registers.EBX))
	.add(new BankRegister (0, 4, 4, IA32Registers.ECX))
	.add(new BankRegister (0, 8, 4, IA32Registers.EDX))
	.add(new BankRegister (0, 12, 4, IA32Registers.ESI))
	.add(new BankRegister (0, 16, 4, IA32Registers.EDI))
	.add(new BankRegister (0, 20, 4, IA32Registers.EBP))
	.add(new BankRegister (0, 52, 4, IA32Registers.CS))
	.add(new BankRegister (0, 28, 4, IA32Registers.DS))
	.add(new BankRegister (0, 32, 4, IA32Registers.ES))
	.add(new BankRegister (0, 36, 4, IA32Registers.FS))
	.add(new BankRegister (0, 40, 4, IA32Registers.GS))
	.add(new BankRegister (0, 64, 4, IA32Registers.SS))
	.add(new BankRegister (0, 44, 4, IA32Registers.ORIG_EAX))
	.add(new BankRegister (0, 48, 4, IA32Registers.EIP))
	.add(new BankRegister (0, 56, 4, IA32Registers.EFLAGS))
	.add(new BankRegister (0, 60, 4, IA32Registers.ESP))
    // Get all FP registers from FXSAVE area.
	.add(new BankRegister(2, 0x00, 2, X87Registers.FCW))
	.add(new BankRegister(2, 0x02, 2, X87Registers.FSW))
	.add(new BankRegister(2, 0x04, 1, X87Registers.FTW))
	.add(new BankRegister(2, 0x06, 2, X87Registers.FOP))
	.add(new BankRegister(2, 0x08, 4, X87Registers.EIP))
	.add(new BankRegister(2, 0x0c, 2, X87Registers.CS))
	.add(new BankRegister(2, 0x10, 4, X87Registers.DP))
	.add(new BankRegister(2, 0x14, 2, X87Registers.DS))
	.add(new BankRegister(2, 0x18, 2, X87Registers.MXCSR))
	.add(new BankRegister(2, 0x1c, 2, X87Registers.MXCSR_MASK))
	.add(new BankRegister(2, 0x20, 10, X87Registers.ST0))
	.add(new BankRegister(2, 0x30, 10, X87Registers.ST1))
	.add(new BankRegister(2, 0x40, 10, X87Registers.ST2))
	.add(new BankRegister(2, 0x50, 10, X87Registers.ST3))
	.add(new BankRegister(2, 0x60, 10, X87Registers.ST4))
	.add(new BankRegister(2, 0x70, 10, X87Registers.ST5))
	.add(new BankRegister(2, 0x80, 10, X87Registers.ST6))
	.add(new BankRegister(2, 0x90, 10, X87Registers.ST7))
	.add(new BankRegister(2, 0xa0, 16, X87Registers.XMM0))
	.add(new BankRegister(2, 0xb0, 16, X87Registers.XMM1))
	.add(new BankRegister(2, 0xc0, 16, X87Registers.XMM2))
	.add(new BankRegister(2, 0xd0, 16, X87Registers.XMM3))
	.add(new BankRegister(2, 0xe0, 16, X87Registers.XMM4))
	.add(new BankRegister(2, 0xf0, 16, X87Registers.XMM5))
	.add(new BankRegister(2, 0x100, 16, X87Registers.XMM6))
	.add(new BankRegister(2, 0x110, 16, X87Registers.XMM7))
    // debug registers
	.add(new BankRegister (3, 252, 4, IA32Registers.D0))
	.add(new BankRegister (3, 256, 4, IA32Registers.D1))
	.add(new BankRegister (3, 260, 4, IA32Registers.D2))
	.add(new BankRegister (3, 264, 4, IA32Registers.D3))
	.add(new BankRegister (3, 268, 4, IA32Registers.D4))
	.add(new BankRegister (3, 272, 4, IA32Registers.D5))
	.add(new BankRegister (3, 276, 4, IA32Registers.D6))
	.add(new BankRegister (3, 280, 4, IA32Registers.D7))
	;

    public static final BankRegisterMap X8664 = new BankRegisterMap()
	.add(new BankRegister(0, 80, 8, X8664Registers.RAX))
	.add(new BankRegister(0, 40, 8, X8664Registers.RBX))
	.add(new BankRegister(0, 88, 8, X8664Registers.RCX))
	.add(new BankRegister(0, 96, 8, X8664Registers.RDX))
	.add(new BankRegister(0, 104, 8, X8664Registers.RSI))
	.add(new BankRegister(0, 112, 8, X8664Registers.RDI))
	.add(new BankRegister(0, 32, 8, X8664Registers.RBP))
	.add(new BankRegister(0, 152, 8, X8664Registers.RSP))
	.add(new BankRegister(0, 72, 8, X8664Registers.R8))
	.add(new BankRegister(0, 64, 8, X8664Registers.R9))
	.add(new BankRegister(0, 56, 8, X8664Registers.R10))
	.add(new BankRegister(0, 48, 8, X8664Registers.R11))
	.add(new BankRegister(0, 24, 8, X8664Registers.R12))
	.add(new BankRegister(0, 16, 8, X8664Registers.R13))
	.add(new BankRegister(0, 8, 8, X8664Registers.R14))
	.add(new BankRegister(0, 0, 8, X8664Registers.R15))
	.add(new BankRegister(0, 128, 8, X8664Registers.RIP))
	.add(new BankRegister(0, 144, 8, X8664Registers.RFLAGS))
	.add(new BankRegister(0, 136, 8, "cs"))
	.add(new BankRegister(0, 160, 8, "ss"))
	.add(new BankRegister(0, 184, 8, "ds"))
	.add(new BankRegister(0, 192, 8, "es"))
	.add(new BankRegister(0, 200, 8, "fs"))
	.add(new BankRegister(0, 208, 8, "gs"))
	.add(new BankRegister(0, 120, 8, X8664Registers.ORIG_RAX))
	.add(new BankRegister(0, 168, 8, X8664Registers.FS_BASE))
	.add(new BankRegister(0, 176, 8, X8664Registers.GS_BASE))
    // Format determined by FXSAVE instruction
	.add(new BankRegister(1, 0x00, 2, X87Registers.FCW))
	.add(new BankRegister(1, 0x02, 2, X87Registers.FSW))
	.add(new BankRegister(1, 0x04, 1, X87Registers.FTW))
	.add(new BankRegister(1, 0x06, 2, X87Registers.FOP))
	.add(new BankRegister(1, 0x08, 4, X87Registers.RIP))
	.add(new BankRegister(1, 0x10, 4, X87Registers.RDP))
	.add(new BankRegister(1, 0x18, 2, X87Registers.MXCSR))
	.add(new BankRegister(1, 0x1c, 2, X87Registers.MXCSR_MASK))
	.add(new BankRegister(1, 0x20, 10, X87Registers.ST0))
	.add(new BankRegister(1, 0x30, 10, X87Registers.ST1))
	.add(new BankRegister(1, 0x40, 10, X87Registers.ST2))
	.add(new BankRegister(1, 0x50, 10, X87Registers.ST3))
	.add(new BankRegister(1, 0x60, 10, X87Registers.ST4))
	.add(new BankRegister(1, 0x70, 10, X87Registers.ST5))
	.add(new BankRegister(1, 0x80, 10, X87Registers.ST6))
	.add(new BankRegister(1, 0x90, 10, X87Registers.ST7))
	.add(new BankRegister(1, 0xa0, 16, X87Registers.XMM0))
	.add(new BankRegister(1, 0xb0, 16, X87Registers.XMM1))
	.add(new BankRegister(1, 0xc0, 16, X87Registers.XMM2))
	.add(new BankRegister(1, 0xd0, 16, X87Registers.XMM3))
	.add(new BankRegister(1, 0xe0, 16, X87Registers.XMM4))
	.add(new BankRegister(1, 0xf0, 16, X87Registers.XMM5))
	.add(new BankRegister(1, 0x100, 16, X87Registers.XMM6))
	.add(new BankRegister(1, 0x110, 16, X87Registers.XMM7))
	.add(new BankRegister(1, 0x120, 16, X87Registers.XMM8))
	.add(new BankRegister(1, 0x130, 16, X87Registers.XMM9))
	.add(new BankRegister(1, 0x140, 16, X87Registers.XMM10))
	.add(new BankRegister(1, 0x150, 16, X87Registers.XMM11))
	.add(new BankRegister(1, 0x160, 16, X87Registers.XMM12))
	.add(new BankRegister(1, 0x170, 16, X87Registers.XMM13))
	.add(new BankRegister(1, 0x180, 16, X87Registers.XMM14))
	.add(new BankRegister(1, 0x190, 16, X87Registers.XMM15))
    // debug registers
	.add(new BankRegister(2, 848, 8, X8664Registers.DR0))
	.add(new BankRegister(2, 856, 8, X8664Registers.DR1))
	.add(new BankRegister(2, 864, 8, X8664Registers.DR2))
	.add(new BankRegister(2, 872, 8, X8664Registers.DR3))
	.add(new BankRegister(2, 880, 8, X8664Registers.DR4))
	.add(new BankRegister(2, 888, 8, X8664Registers.DR5))
	.add(new BankRegister(2, 896, 8, X8664Registers.DR6))
	.add(new BankRegister(2, 904, 8, X8664Registers.DR7))
	;

    public static BankRegisterMap IA32_ON_X8664
	= new IndirectBankRegisterMap(ByteOrder.LITTLE_ENDIAN, IA32, X8664)
	.add(IA32Registers.EAX, X8664Registers.RAX)
	.add(IA32Registers.EBX, X8664Registers.RBX)
	.add(IA32Registers.ECX, X8664Registers.RCX)
	.add(IA32Registers.EDX, X8664Registers.RDX)
	.add(IA32Registers.ESI, X8664Registers.RSI)
	.add(IA32Registers.EDI, X8664Registers.RDI)
	.add(IA32Registers.EBP, X8664Registers.RBP)
	.add("cs", "cs")
	.add("ds", "ds")
	.add("es", "es")
	.add("fs", "fs")
	.add("gs", "gs")
	.add("ss", "gs")
	.add(IA32Registers.ORIG_EAX, X8664Registers.ORIG_RAX)
	.add(IA32Registers.EIP, X8664Registers.RIP)
	.add(IA32Registers.EFLAGS,X8664Registers.RFLAGS)
	.add(IA32Registers.ESP, X8664Registers.RSP)
	.add(X87Registers.FCW)
	.add(X87Registers.FSW)
	.add(X87Registers.FTW)
	.add(X87Registers.FOP)
	.add(X87Registers.EIP, 1, 0x08, 4)
	.add(X87Registers.CS, 1, 0x0c, 2)
	.add(X87Registers.DP, 1, 0x10, 4)
	.add(X87Registers.DS, 1, 0x14, 2)
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
