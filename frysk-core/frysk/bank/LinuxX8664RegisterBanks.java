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

    public static final RegisterBank GENERAL_REGISTERS = new RegisterBank()
	.add(new RegisterEntry(80, 8, X8664Registers.RAX))
	.add(new RegisterEntry(40, 8, X8664Registers.RBX))
	.add(new RegisterEntry(88, 8, X8664Registers.RCX))
	.add(new RegisterEntry(96, 8, X8664Registers.RDX))
	.add(new RegisterEntry(104, 8, X8664Registers.RSI))
	.add(new RegisterEntry(112, 8, X8664Registers.RDI))
	.add(new RegisterEntry(32, 8, X8664Registers.RBP))
	.add(new RegisterEntry(152, 8, X8664Registers.RSP))
	.add(new RegisterEntry(72, 8, X8664Registers.R8))
	.add(new RegisterEntry(64, 8, X8664Registers.R9))
	.add(new RegisterEntry(56, 8, X8664Registers.R10))
	.add(new RegisterEntry(48, 8, X8664Registers.R11))
	.add(new RegisterEntry(24, 8, X8664Registers.R12))
	.add(new RegisterEntry(16, 8, X8664Registers.R13))
	.add(new RegisterEntry(8, 8, X8664Registers.R14))
	.add(new RegisterEntry(0, 8, X8664Registers.R15))
	.add(new RegisterEntry(128, 8, X8664Registers.RIP))
	.add(new RegisterEntry(144, 8, X8664Registers.RFLAGS))
	.add(new RegisterEntry(136, 8, "cs"))
	.add(new RegisterEntry(160, 8, "ss"))
	.add(new RegisterEntry(184, 8, "ds"))
	.add(new RegisterEntry(192, 8, "es"))
	.add(new RegisterEntry(200, 8, "fs"))
	.add(new RegisterEntry(208, 8, "gs"))
	.add(new RegisterEntry(120, 8, X8664Registers.ORIG_RAX))
	.add(new RegisterEntry(168, 8, X8664Registers.FS_BASE))
	.add(new RegisterEntry(176, 8, X8664Registers.GS_BASE))
	;
    
    public static final RegisterBank FLOATING_POINT_REGISTERS = new RegisterBank()
    // Format determined by FXSAVE instruction
	.add(new RegisterEntry(0x00, 2, X87Registers.FCW))
	.add(new RegisterEntry(0x02, 2, X87Registers.FSW))
	.add(new RegisterEntry(0x04, 1, X87Registers.FTW))
	.add(new RegisterEntry(0x06, 2, X87Registers.FOP))
	.add(new RegisterEntry(0x08, 4, X87Registers.RIP))
	.add(new RegisterEntry(0x10, 4, X87Registers.RDP))
	.add(new RegisterEntry(0x18, 2, X87Registers.MXCSR))
	.add(new RegisterEntry(0x1c, 2, X87Registers.MXCSR_MASK))
	.add(new RegisterEntry(0x20, 10, X87Registers.ST0))
	.add(new RegisterEntry(0x30, 10, X87Registers.ST1))
	.add(new RegisterEntry(0x40, 10, X87Registers.ST2))
	.add(new RegisterEntry(0x50, 10, X87Registers.ST3))
	.add(new RegisterEntry(0x60, 10, X87Registers.ST4))
	.add(new RegisterEntry(0x70, 10, X87Registers.ST5))
	.add(new RegisterEntry(0x80, 10, X87Registers.ST6))
	.add(new RegisterEntry(0x90, 10, X87Registers.ST7))
	.add(new RegisterEntry(0xa0, 16, X87Registers.XMM0))
	.add(new RegisterEntry(0xb0, 16, X87Registers.XMM1))
	.add(new RegisterEntry(0xc0, 16, X87Registers.XMM2))
	.add(new RegisterEntry(0xd0, 16, X87Registers.XMM3))
	.add(new RegisterEntry(0xe0, 16, X87Registers.XMM4))
	.add(new RegisterEntry(0xf0, 16, X87Registers.XMM5))
	.add(new RegisterEntry(0x100, 16, X87Registers.XMM6))
	.add(new RegisterEntry(0x110, 16, X87Registers.XMM7))
	.add(new RegisterEntry(0x120, 16, X87Registers.XMM8))
	.add(new RegisterEntry(0x130, 16, X87Registers.XMM9))
	.add(new RegisterEntry(0x140, 16, X87Registers.XMM10))
	.add(new RegisterEntry(0x150, 16, X87Registers.XMM11))
	.add(new RegisterEntry(0x160, 16, X87Registers.XMM12))
	.add(new RegisterEntry(0x170, 16, X87Registers.XMM13))
	.add(new RegisterEntry(0x180, 16, X87Registers.XMM14))
	.add(new RegisterEntry(0x190, 16, X87Registers.XMM15))
	;
    
    public static final RegisterBank DEBUG_REGISTERS = new RegisterBank()
	.add(new RegisterEntry(848, 8, X8664Registers.DR0))
	.add(new RegisterEntry(856, 8, X8664Registers.DR1))
	.add(new RegisterEntry(864, 8, X8664Registers.DR2))
	.add(new RegisterEntry(872, 8, X8664Registers.DR3))
	.add(new RegisterEntry(880, 8, X8664Registers.DR4))
	.add(new RegisterEntry(888, 8, X8664Registers.DR5))
	.add(new RegisterEntry(896, 8, X8664Registers.DR6))
	.add(new RegisterEntry(904, 8, X8664Registers.DR7))
	;
}
