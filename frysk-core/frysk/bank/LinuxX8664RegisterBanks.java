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

import frysk.isa.X8664Registers;
import frysk.isa.X87Registers;

public class LinuxX8664RegisterBanks {

    public static final BankRegisterMap GENERAL_REGISTERS
	= new BankRegisterMap()
	.add(new BankRegister(80, 8, X8664Registers.RAX))
	.add(new BankRegister(40, 8, X8664Registers.RBX))
	.add(new BankRegister(88, 8, X8664Registers.RCX))
	.add(new BankRegister(96, 8, X8664Registers.RDX))
	.add(new BankRegister(104, 8, X8664Registers.RSI))
	.add(new BankRegister(112, 8, X8664Registers.RDI))
	.add(new BankRegister(32, 8, X8664Registers.RBP))
	.add(new BankRegister(152, 8, X8664Registers.RSP))
	.add(new BankRegister(72, 8, X8664Registers.R8))
	.add(new BankRegister(64, 8, X8664Registers.R9))
	.add(new BankRegister(56, 8, X8664Registers.R10))
	.add(new BankRegister(48, 8, X8664Registers.R11))
	.add(new BankRegister(24, 8, X8664Registers.R12))
	.add(new BankRegister(16, 8, X8664Registers.R13))
	.add(new BankRegister(8, 8, X8664Registers.R14))
	.add(new BankRegister(0, 8, X8664Registers.R15))
	.add(new BankRegister(128, 8, X8664Registers.RIP))
	.add(new BankRegister(144, 8, X8664Registers.RFLAGS))
	.add(new BankRegister(136, 8, "cs"))
	.add(new BankRegister(160, 8, "ss"))
	.add(new BankRegister(184, 8, "ds"))
	.add(new BankRegister(192, 8, "es"))
	.add(new BankRegister(200, 8, "fs"))
	.add(new BankRegister(208, 8, "gs"))
	.add(new BankRegister(120, 8, X8664Registers.ORIG_RAX))
	.add(new BankRegister(168, 8, X8664Registers.FS_BASE))
	.add(new BankRegister(176, 8, X8664Registers.GS_BASE))
	;
    
    public static final BankRegisterMap FLOATING_POINT_REGISTERS
	= new BankRegisterMap()
    // Format determined by FXSAVE instruction
	.add(new BankRegister(0x00, 2, X87Registers.FCW))
	.add(new BankRegister(0x02, 2, X87Registers.FSW))
	.add(new BankRegister(0x04, 1, X87Registers.FTW))
	.add(new BankRegister(0x06, 2, X87Registers.FOP))
	.add(new BankRegister(0x08, 4, X87Registers.RIP))
	.add(new BankRegister(0x10, 4, X87Registers.RDP))
	.add(new BankRegister(0x18, 2, X87Registers.MXCSR))
	.add(new BankRegister(0x1c, 2, X87Registers.MXCSR_MASK))
	.add(new BankRegister(0x20, 10, X87Registers.ST0))
	.add(new BankRegister(0x30, 10, X87Registers.ST1))
	.add(new BankRegister(0x40, 10, X87Registers.ST2))
	.add(new BankRegister(0x50, 10, X87Registers.ST3))
	.add(new BankRegister(0x60, 10, X87Registers.ST4))
	.add(new BankRegister(0x70, 10, X87Registers.ST5))
	.add(new BankRegister(0x80, 10, X87Registers.ST6))
	.add(new BankRegister(0x90, 10, X87Registers.ST7))
	.add(new BankRegister(0xa0, 16, X87Registers.XMM0))
	.add(new BankRegister(0xb0, 16, X87Registers.XMM1))
	.add(new BankRegister(0xc0, 16, X87Registers.XMM2))
	.add(new BankRegister(0xd0, 16, X87Registers.XMM3))
	.add(new BankRegister(0xe0, 16, X87Registers.XMM4))
	.add(new BankRegister(0xf0, 16, X87Registers.XMM5))
	.add(new BankRegister(0x100, 16, X87Registers.XMM6))
	.add(new BankRegister(0x110, 16, X87Registers.XMM7))
	.add(new BankRegister(0x120, 16, X87Registers.XMM8))
	.add(new BankRegister(0x130, 16, X87Registers.XMM9))
	.add(new BankRegister(0x140, 16, X87Registers.XMM10))
	.add(new BankRegister(0x150, 16, X87Registers.XMM11))
	.add(new BankRegister(0x160, 16, X87Registers.XMM12))
	.add(new BankRegister(0x170, 16, X87Registers.XMM13))
	.add(new BankRegister(0x180, 16, X87Registers.XMM14))
	.add(new BankRegister(0x190, 16, X87Registers.XMM15))
	;
    
    public static final BankRegisterMap DEBUG_REGISTERS
	= new BankRegisterMap()
	.add(new BankRegister(848, 8, X8664Registers.DR0))
	.add(new BankRegister(856, 8, X8664Registers.DR1))
	.add(new BankRegister(864, 8, X8664Registers.DR2))
	.add(new BankRegister(872, 8, X8664Registers.DR3))
	.add(new BankRegister(880, 8, X8664Registers.DR4))
	.add(new BankRegister(888, 8, X8664Registers.DR5))
	.add(new BankRegister(896, 8, X8664Registers.DR6))
	.add(new BankRegister(904, 8, X8664Registers.DR7))
	;
}
