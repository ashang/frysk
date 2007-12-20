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

import frysk.isa.IA32Registers;
import frysk.isa.X87Registers;

public class LinuxIA32RegisterBanks {
    
    public static final BankRegisterMap REGS
	= new BankRegisterMap()
	.add(new BankRegister(24, 4,IA32Registers.EAX))
	.add(new BankRegister(0, 4, IA32Registers.EBX))
	.add(new BankRegister(4, 4, IA32Registers.ECX))
	.add(new BankRegister(8, 4, IA32Registers.EDX))
	.add(new BankRegister(12, 4, IA32Registers.ESI))
	.add(new BankRegister(16, 4, IA32Registers.EDI))
	.add(new BankRegister(20, 4, IA32Registers.EBP))
	.add(new BankRegister(52, 4, IA32Registers.CS))
	.add(new BankRegister(28, 4, IA32Registers.DS))
	.add(new BankRegister(32, 4, IA32Registers.ES))
	.add(new BankRegister(36, 4, IA32Registers.FS))
	.add(new BankRegister(40, 4, IA32Registers.GS))
	.add(new BankRegister(64, 4, IA32Registers.SS))
	.add(new BankRegister(44, 4, IA32Registers.ORIG_EAX))
	.add(new BankRegister(48, 4, IA32Registers.EIP))
	.add(new BankRegister(56, 4, IA32Registers.EFLAGS))
	.add(new BankRegister(60, 4, IA32Registers.ESP))
	;
    
    public static final BankRegisterMap FPREGS
	= new BankRegisterMap()
	.add(new BankRegister(0x00, 2, X87Registers.FCW))
	.add(new BankRegister(0x04, 2, X87Registers.FSW))
	.add(new BankRegister(0x08, 2, X87Registers.FTW))
	.add(new BankRegister(0x0c, 4, X87Registers.EIP))
	.add(new BankRegister(0x10, 2, X87Registers.CS))
	.add(new BankRegister(0x12, 2, X87Registers.FOP))
	.add(new BankRegister(0x18, 4, X87Registers.DP))
	.add(new BankRegister(0x18, 2, X87Registers.DS))
	.add(new BankRegister(0x1c, 10, X87Registers.ST0))
	.add(new BankRegister(0x26, 10, X87Registers.ST1))
	.add(new BankRegister(0x30, 10, X87Registers.ST2))
	.add(new BankRegister(0x3a, 10, X87Registers.ST3))
	.add(new BankRegister(0x44, 10, X87Registers.ST4))
	.add(new BankRegister(0x4e, 10, X87Registers.ST5))
	.add(new BankRegister(0x58, 10, X87Registers.ST6))
	.add(new BankRegister(0x62, 10, X87Registers.ST7))
	;

    public static final BankRegisterMap XFPREGS
	= new BankRegisterMap()
    //Get all FP registers from FXSAVE area.
	.add(new BankRegister(0x00, 2, X87Registers.FCW))
	.add(new BankRegister(0x02, 2, X87Registers.FSW))
	.add(new BankRegister(0x04, 1, X87Registers.FTW))
	.add(new BankRegister(0x06, 2, X87Registers.FOP))
	.add(new BankRegister(0x08, 4, X87Registers.EIP))
	.add(new BankRegister(0x0c, 2, X87Registers.CS))
	.add(new BankRegister(0x10, 4, X87Registers.DP))
	.add(new BankRegister(0x14, 2, X87Registers.DS))
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
	;
    
    public static BankRegisterMap USR = new BankRegisterMap()
	.add(new BankRegister(252, 4, IA32Registers.D0))
	.add(new BankRegister(256, 4, IA32Registers.D1))
	.add(new BankRegister(260, 4, IA32Registers.D2))
	.add(new BankRegister(264, 4, IA32Registers.D3))
	.add(new BankRegister(268, 4, IA32Registers.D4))
	.add(new BankRegister(272, 4, IA32Registers.D5))
	.add(new BankRegister(276, 4, IA32Registers.D6))
	.add(new BankRegister(280, 4, IA32Registers.D7))
	;

}
