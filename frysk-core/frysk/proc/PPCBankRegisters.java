// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 IBM Corp.
// Copyright 2007 Red Hat Inc.
// 
// Contributed by
// Jose Flavio Aguilar Paulino (joseflavio@gmail.com)
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
import frysk.isa.PPC32Registers;
import frysk.isa.PPC64Registers;

public class PPCBankRegisters {

    public static final BankRegisterMap PPC32BE = new BankRegisterMap()
	.add(new BankRegister(0, 0, 4, PPC32Registers.GPR0))
	.add(new BankRegister(0, 4, 4, PPC32Registers.GPR1))
	.add(new BankRegister(0, 8, 4, PPC32Registers.GPR2))
	.add(new BankRegister(0, 12, 4, PPC32Registers.GPR3))
	.add(new BankRegister(0, 16, 4, PPC32Registers.GPR4))
	.add(new BankRegister(0, 20, 4, PPC32Registers.GPR5))
	.add(new BankRegister(0, 24, 4, PPC32Registers.GPR6))
	.add(new BankRegister(0, 28, 4, PPC32Registers.GPR7))
	.add(new BankRegister(0, 32, 4, PPC32Registers.GPR8))
	.add(new BankRegister(0, 36, 4, PPC32Registers.GPR9))
	.add(new BankRegister(0, 40, 4, PPC32Registers.GPR10))
	.add(new BankRegister(0, 44, 4, PPC32Registers.GPR11))
	.add(new BankRegister(0, 48, 4, PPC32Registers.GPR12))
	.add(new BankRegister(0, 52, 4, PPC32Registers.GPR13))
	.add(new BankRegister(0, 56, 4, PPC32Registers.GPR14))
	.add(new BankRegister(0, 60, 4, PPC32Registers.GPR15))
	.add(new BankRegister(0, 64, 4, PPC32Registers.GPR16))
	.add(new BankRegister(0, 68, 4, PPC32Registers.GPR17))
	.add(new BankRegister(0, 72, 4, PPC32Registers.GPR18))
	.add(new BankRegister(0, 76, 4, PPC32Registers.GPR19))
	.add(new BankRegister(0, 80, 4, PPC32Registers.GPR20))
	.add(new BankRegister(0, 84, 4, PPC32Registers.GPR21))
	.add(new BankRegister(0, 88, 4, PPC32Registers.GPR22))
	.add(new BankRegister(0, 92, 4, PPC32Registers.GPR23))
	.add(new BankRegister(0, 96, 4, PPC32Registers.GPR24))
	.add(new BankRegister(0, 100, 4, PPC32Registers.GPR25))
	.add(new BankRegister(0, 104, 4, PPC32Registers.GPR26))
	.add(new BankRegister(0, 108, 4, PPC32Registers.GPR27))
	.add(new BankRegister(0, 112, 4, PPC32Registers.GPR28))
	.add(new BankRegister(0, 116, 4, PPC32Registers.GPR29))
	.add(new BankRegister(0, 120, 4, PPC32Registers.GPR30))
	.add(new BankRegister(0, 124, 4, PPC32Registers.GPR31))
	.add(new BankRegister(0, 128, 4, PPC32Registers.NIP)) //Fixme: PC I belive
	.add(new BankRegister(0, 132, 4, PPC32Registers.MSR))
	.add(new BankRegister(0, 136, 4, PPC32Registers.ORIGR3))
	.add(new BankRegister(0, 140, 4, PPC32Registers.CTR))
	.add(new BankRegister(0, 144, 4, PPC32Registers.LR))
	.add(new BankRegister(0, 148, 4, PPC32Registers.XER))
	.add(new BankRegister(0, 152, 4, PPC32Registers.CCR))
	.add(new BankRegister(0, 156, 4, PPC32Registers.MQ))
	.add(new BankRegister(0, 160, 4, PPC32Registers.TRAP))
	.add(new BankRegister(0, 164, 4, PPC32Registers.DAR))
	.add(new BankRegister(0, 168, 4, PPC32Registers.DSISR))
	.add(new BankRegister(0, 172, 4, PPC32Registers.RESULT))
	.add(new BankRegister(0, 192, 8, PPC32Registers.FPR0)) // 48*4
	.add(new BankRegister(0, 200, 8, PPC32Registers.FPR1))
	.add(new BankRegister(0, 208, 8, PPC32Registers.FPR2))
	.add(new BankRegister(0, 216, 8, PPC32Registers.FPR3))
	.add(new BankRegister(0, 224, 8, PPC32Registers.FPR4))
	.add(new BankRegister(0, 232, 8, PPC32Registers.FPR5))
	.add(new BankRegister(0, 240, 8, PPC32Registers.FPR6))
	.add(new BankRegister(0, 248, 8, PPC32Registers.FPR7))
	.add(new BankRegister(0, 256, 8, PPC32Registers.FPR8))
	.add(new BankRegister(0, 264, 8, PPC32Registers.FPR9))
	.add(new BankRegister(0, 272, 8, PPC32Registers.FPR10))
	.add(new BankRegister(0, 280, 8, PPC32Registers.FPR11))
	.add(new BankRegister(0, 288, 8, PPC32Registers.FPR12))
	.add(new BankRegister(0, 296, 8, PPC32Registers.FPR13))
	.add(new BankRegister(0, 304, 8, PPC32Registers.FPR14))
	.add(new BankRegister(0, 312, 8, PPC32Registers.FPR15))
	.add(new BankRegister(0, 320, 8, PPC32Registers.FPR16))
	.add(new BankRegister(0, 328, 8, PPC32Registers.FPR17))
	.add(new BankRegister(0, 336, 8, PPC32Registers.FPR18))
	.add(new BankRegister(0, 344, 8, PPC32Registers.FPR19))
	.add(new BankRegister(0, 352, 8, PPC32Registers.FPR20))
	.add(new BankRegister(0, 360, 8, PPC32Registers.FPR21))
	.add(new BankRegister(0, 368, 8, PPC32Registers.FPR22))
	.add(new BankRegister(0, 376, 8, PPC32Registers.FPR23))
	.add(new BankRegister(0, 384, 8, PPC32Registers.FPR24))
	.add(new BankRegister(0, 392, 8, PPC32Registers.FPR25))
	.add(new BankRegister(0, 400, 8, PPC32Registers.FPR26))
	.add(new BankRegister(0, 408, 8, PPC32Registers.FPR27))
	.add(new BankRegister(0, 416, 8, PPC32Registers.FPR28))
	.add(new BankRegister(0, 424, 8, PPC32Registers.FPR29))
	.add(new BankRegister(0, 432, 8, PPC32Registers.FPR30))
	.add(new BankRegister(0, 440, 8, PPC32Registers.FPR31))
	//There is a pad of 4 bytes before the FPSCR reg
	.add(new BankRegister(0, 452, 4, PPC32Registers.FPSCR)) //(PT_FPR0 + 2*32 + 1)
	;

    public static final BankRegisterMap PPC64BE = new BankRegisterMap()
	.add(new BankRegister(0, 0, 8, PPC64Registers.GPR0))
	.add(new BankRegister(0, 8, 8, PPC64Registers.GPR1))
	.add(new BankRegister(0, 16, 8, PPC64Registers.GPR2))
	.add(new BankRegister(0, 24, 8, PPC64Registers.GPR3))
	.add(new BankRegister(0, 32, 8, PPC64Registers.GPR4))
	.add(new BankRegister(0, 40, 8, PPC64Registers.GPR5))
	.add(new BankRegister(0, 48, 8, PPC64Registers.GPR6))
	.add(new BankRegister(0, 56, 8, PPC64Registers.GPR7))
	.add(new BankRegister(0, 64, 8, PPC64Registers.GPR8))
	.add(new BankRegister(0, 72, 8, PPC64Registers.GPR9))
	.add(new BankRegister(0, 80, 8, PPC64Registers.GPR10))
	.add(new BankRegister(0, 88, 8, PPC64Registers.GPR11))
	.add(new BankRegister(0, 96, 8, PPC64Registers.GPR12))
	.add(new BankRegister(0, 104, 8, PPC64Registers.GPR13))
	.add(new BankRegister(0, 112, 8, PPC64Registers.GPR14))
	.add(new BankRegister(0, 120, 8, PPC64Registers.GPR15))
	.add(new BankRegister(0, 128, 8, PPC64Registers.GPR16))
	.add(new BankRegister(0, 136, 8, PPC64Registers.GPR17))
	.add(new BankRegister(0, 144, 8, PPC64Registers.GPR18))
	.add(new BankRegister(0, 152, 8, PPC64Registers.GPR19))
	.add(new BankRegister(0, 160, 8, PPC64Registers.GPR20))
	.add(new BankRegister(0, 168, 8, PPC64Registers.GPR21))
	.add(new BankRegister(0, 176, 8, PPC64Registers.GPR22))
	.add(new BankRegister(0, 184, 8, PPC64Registers.GPR23))
	.add(new BankRegister(0, 192, 8, PPC64Registers.GPR24))
	.add(new BankRegister(0, 200, 8, PPC64Registers.GPR25))
	.add(new BankRegister(0, 208, 8, PPC64Registers.GPR26))
	.add(new BankRegister(0, 216, 8, PPC64Registers.GPR27))
	.add(new BankRegister(0, 224, 8, PPC64Registers.GPR28))
	.add(new BankRegister(0, 232, 8, PPC64Registers.GPR29))
	.add(new BankRegister(0, 240, 8, PPC64Registers.GPR30))
	.add(new BankRegister(0, 248, 8, PPC64Registers.GPR31))
	.add(new BankRegister(0, 256, 8, PPC64Registers.NIP))
	.add(new BankRegister(0, 264, 8, PPC64Registers.MSR)) //in gdb: .ps_offset = 264
	.add(new BankRegister(0, 272, 8, PPC64Registers.ORIGR3))
	.add(new BankRegister(0, 280, 8, PPC64Registers.CTR))
	.add(new BankRegister(0, 288, 8, PPC64Registers.LR))
	.add(new BankRegister(0, 296, 8, PPC64Registers.XER))
	.add(new BankRegister(0, 304, 8, PPC64Registers.CCR))
	.add(new BankRegister(0, 312, 8, PPC64Registers.SOFTE))
	.add(new BankRegister(0, 320, 8, PPC64Registers.TRAP))
	.add(new BankRegister(0, 328, 8, PPC64Registers.DAR))
	.add(new BankRegister(0, 336, 8, PPC64Registers.DSISR))
	.add(new BankRegister(0, 344, 8, PPC64Registers.RESULT))
	.add(new BankRegister(0, 384, 8, PPC64Registers.FPR0)) //PT_FPR0 48 
	.add(new BankRegister(0, 392, 8, PPC64Registers.FPR1))
	.add(new BankRegister(0, 400, 8, PPC64Registers.FPR2))
	.add(new BankRegister(0, 408, 8, PPC64Registers.FPR3))
	.add(new BankRegister(0, 416, 8, PPC64Registers.FPR4))
	.add(new BankRegister(0, 424, 8, PPC64Registers.FPR5))
	.add(new BankRegister(0, 432, 8, PPC64Registers.FPR6))
	.add(new BankRegister(0, 440, 8, PPC64Registers.FPR7))
	.add(new BankRegister(0, 448, 8, PPC64Registers.FPR8))
	.add(new BankRegister(0, 456, 8, PPC64Registers.FPR9))
	.add(new BankRegister(0, 464, 8, PPC64Registers.FPR10))
	.add(new BankRegister(0, 472, 8, PPC64Registers.FPR11))
	.add(new BankRegister(0, 480, 8, PPC64Registers.FPR12))
	.add(new BankRegister(0, 488, 8, PPC64Registers.FPR13))
	.add(new BankRegister(0, 496, 8, PPC64Registers.FPR14))
	.add(new BankRegister(0, 504, 8, PPC64Registers.FPR15))
	.add(new BankRegister(0, 512, 8, PPC64Registers.FPR16))
	.add(new BankRegister(0, 520, 8, PPC64Registers.FPR17))
	.add(new BankRegister(0, 528, 8, PPC64Registers.FPR18))
	.add(new BankRegister(0, 536, 8, PPC64Registers.FPR19))
	.add(new BankRegister(0, 544, 8, PPC64Registers.FPR20))
	.add(new BankRegister(0, 552, 8, PPC64Registers.FPR21))
	.add(new BankRegister(0, 560, 8, PPC64Registers.FPR22))
	.add(new BankRegister(0, 568, 8, PPC64Registers.FPR23))
	.add(new BankRegister(0, 576, 8, PPC64Registers.FPR24))
	.add(new BankRegister(0, 584, 8, PPC64Registers.FPR25))
	.add(new BankRegister(0, 592, 8, PPC64Registers.FPR26))
	.add(new BankRegister(0, 600, 8, PPC64Registers.FPR27))
	.add(new BankRegister(0, 608, 8, PPC64Registers.FPR28))
	.add(new BankRegister(0, 616, 8, PPC64Registers.FPR29))
	.add(new BankRegister(0, 624, 8, PPC64Registers.FPR30))
	.add(new BankRegister(0, 632, 8, PPC64Registers.FPR31))
	.add(new BankRegister(0, 640, 4, PPC64Registers.FPSCR))
	// Fixme: need to implement altivec registers
	// Vector Registers are 128 bit wide
	//.add(new BankRegister(0, 656, 16, PPC64Registers.VR0)) PT_VR0 82
	//...
	//.add(new BankRegister(0, 1152, 16, PPC64Registers.V31)) PT_VR0 + 31*2), index 148
	//Need to put a 8 bytes pad here, because VSCR is 8 byte wide only 
	.add(new BankRegister(0, 1176, 8, PPC64Registers.VSCR)) // PT_VSCR (PT_VR0 + 32*2 + 1), index 147
	.add(new BankRegister(0, 1184, 8, PPC64Registers.VRSAVE)); // PT_VRSAVE (PT_VR0 + 33*2), index 148

    public static final BankRegisterMap PPC32BE_ON_PPC64BE
	= new IndirectBankRegisterMap(ByteOrder.BIG_ENDIAN,
				      PPC32BE, PPC64BE)
	.add(PPC32Registers.GPR0)
	.add(PPC32Registers.GPR1)
	.add(PPC32Registers.GPR2)
	.add(PPC32Registers.GPR3)
	.add(PPC32Registers.GPR4)
	.add(PPC32Registers.GPR5)
	.add(PPC32Registers.GPR6)
	.add(PPC32Registers.GPR7)
	.add(PPC32Registers.GPR8)
	.add(PPC32Registers.GPR9)
	.add(PPC32Registers.GPR10)
	.add(PPC32Registers.GPR11)
	.add(PPC32Registers.GPR12)
	.add(PPC32Registers.GPR13)
	.add(PPC32Registers.GPR14)
	.add(PPC32Registers.GPR15)
	.add(PPC32Registers.GPR16)
	.add(PPC32Registers.GPR17)
	.add(PPC32Registers.GPR18)
	.add(PPC32Registers.GPR19)
	.add(PPC32Registers.GPR20)
	.add(PPC32Registers.GPR21)
	.add(PPC32Registers.GPR22)
	.add(PPC32Registers.GPR23)
	.add(PPC32Registers.GPR24)
	.add(PPC32Registers.GPR25)
	.add(PPC32Registers.GPR26)
	.add(PPC32Registers.GPR27)
	.add(PPC32Registers.GPR28)
	.add(PPC32Registers.GPR29)
	.add(PPC32Registers.GPR30)
	.add(PPC32Registers.GPR31)
	.add(PPC32Registers.NIP)
        .add(PPC32Registers.MSR)
        .add(PPC32Registers.ORIGR3)
	.add(PPC32Registers.CTR)
        .add(PPC32Registers.LR)
        .add(PPC32Registers.XER)
        .add(PPC32Registers.CCR)
        .add(PPC32Registers.TRAP)
        .add(PPC32Registers.DAR)
        .add(PPC32Registers.DSISR)
	.add(PPC32Registers.FPR0)
	.add(PPC32Registers.FPR1)
	.add(PPC32Registers.FPR2)
	.add(PPC32Registers.FPR3)
	.add(PPC32Registers.FPR4)
	.add(PPC32Registers.FPR5)
	.add(PPC32Registers.FPR6)
	.add(PPC32Registers.FPR7)
	.add(PPC32Registers.FPR8)
	.add(PPC32Registers.FPR9)
	.add(PPC32Registers.FPR10)
	.add(PPC32Registers.FPR11)
	.add(PPC32Registers.FPR12)
	.add(PPC32Registers.FPR13)
	.add(PPC32Registers.FPR14)
	.add(PPC32Registers.FPR15)
	.add(PPC32Registers.FPR16)
	.add(PPC32Registers.FPR17)
	.add(PPC32Registers.FPR18)
	.add(PPC32Registers.FPR19)
	.add(PPC32Registers.FPR20)
	.add(PPC32Registers.FPR21)
	.add(PPC32Registers.FPR22)
	.add(PPC32Registers.FPR23)
	.add(PPC32Registers.FPR24)
	.add(PPC32Registers.FPR25)
	.add(PPC32Registers.FPR26)
	.add(PPC32Registers.FPR27)
	.add(PPC32Registers.FPR28)
	.add(PPC32Registers.FPR29)
	.add(PPC32Registers.FPR30)
	.add(PPC32Registers.FPR31)
	.add(PPC32Registers.FPSCR)
	;
}
