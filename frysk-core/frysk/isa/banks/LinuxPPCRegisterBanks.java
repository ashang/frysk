// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 IBM Corp.
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

package frysk.isa.banks;

import frysk.isa.PPC32Registers;
import frysk.isa.PPC64Registers;

public class LinuxPPCRegisterBanks {

    public static final BankRegisterMap GREGS32
	= new BankRegisterMap()
	.add(new BankRegister(0, 4, PPC32Registers.GPR0))
	.add(new BankRegister(4, 4, PPC32Registers.GPR1))
	.add(new BankRegister(8, 4, PPC32Registers.GPR2))
	.add(new BankRegister(12, 4, PPC32Registers.GPR3))
	.add(new BankRegister(16, 4, PPC32Registers.GPR4))
	.add(new BankRegister(20, 4, PPC32Registers.GPR5))
	.add(new BankRegister(24, 4, PPC32Registers.GPR6))
	.add(new BankRegister(28, 4, PPC32Registers.GPR7))
	.add(new BankRegister(32, 4, PPC32Registers.GPR8))
	.add(new BankRegister(36, 4, PPC32Registers.GPR9))
	.add(new BankRegister(40, 4, PPC32Registers.GPR10))
	.add(new BankRegister(44, 4, PPC32Registers.GPR11))
	.add(new BankRegister(48, 4, PPC32Registers.GPR12))
	.add(new BankRegister(52, 4, PPC32Registers.GPR13))
	.add(new BankRegister(56, 4, PPC32Registers.GPR14))
	.add(new BankRegister(60, 4, PPC32Registers.GPR15))
	.add(new BankRegister(64, 4, PPC32Registers.GPR16))
	.add(new BankRegister(68, 4, PPC32Registers.GPR17))
	.add(new BankRegister(72, 4, PPC32Registers.GPR18))
	.add(new BankRegister(76, 4, PPC32Registers.GPR19))
	.add(new BankRegister(80, 4, PPC32Registers.GPR20))
	.add(new BankRegister(84, 4, PPC32Registers.GPR21))
	.add(new BankRegister(88, 4, PPC32Registers.GPR22))
	.add(new BankRegister(92, 4, PPC32Registers.GPR23))
	.add(new BankRegister(96, 4, PPC32Registers.GPR24))
	.add(new BankRegister(100, 4, PPC32Registers.GPR25))
	.add(new BankRegister(104, 4, PPC32Registers.GPR26))
	.add(new BankRegister(108, 4, PPC32Registers.GPR27))
	.add(new BankRegister(112, 4, PPC32Registers.GPR28))
	.add(new BankRegister(116, 4, PPC32Registers.GPR29))
	.add(new BankRegister(120, 4, PPC32Registers.GPR30))
	.add(new BankRegister(124, 4, PPC32Registers.GPR31))
	.add(new BankRegister(128, 4, PPC32Registers.NIP)) //Fixme: PC I belive
	.add(new BankRegister(132, 4, PPC32Registers.MSR))
	.add(new BankRegister(136, 4, PPC32Registers.ORIGR3))
	.add(new BankRegister(140, 4, PPC32Registers.CTR))
	.add(new BankRegister(144, 4, PPC32Registers.LR))
	.add(new BankRegister(148, 4, PPC32Registers.XER))
	.add(new BankRegister(152, 4, PPC32Registers.CCR))
	.add(new BankRegister(156, 4, PPC32Registers.MQ))
	.add(new BankRegister(160, 4, PPC32Registers.TRAP))
	.add(new BankRegister(164, 4, PPC32Registers.DAR))
	.add(new BankRegister(168, 4, PPC32Registers.DSISR))
	.add(new BankRegister(172, 4, PPC32Registers.RESULT))
	;

    public static final BankRegisterMap FPREGS32
	= new BankRegisterMap()
	.add(new BankRegister(192, 8, PPC32Registers.FPR0)) // 48*4
	.add(new BankRegister(200, 8, PPC32Registers.FPR1))
	.add(new BankRegister(208, 8, PPC32Registers.FPR2))
	.add(new BankRegister(216, 8, PPC32Registers.FPR3))
	.add(new BankRegister(224, 8, PPC32Registers.FPR4))
	.add(new BankRegister(232, 8, PPC32Registers.FPR5))
	.add(new BankRegister(240, 8, PPC32Registers.FPR6))
	.add(new BankRegister(248, 8, PPC32Registers.FPR7))
	.add(new BankRegister(256, 8, PPC32Registers.FPR8))
	.add(new BankRegister(264, 8, PPC32Registers.FPR9))
	.add(new BankRegister(272, 8, PPC32Registers.FPR10))
	.add(new BankRegister(280, 8, PPC32Registers.FPR11))
	.add(new BankRegister(288, 8, PPC32Registers.FPR12))
	.add(new BankRegister(296, 8, PPC32Registers.FPR13))
	.add(new BankRegister(304, 8, PPC32Registers.FPR14))
	.add(new BankRegister(312, 8, PPC32Registers.FPR15))
	.add(new BankRegister(320, 8, PPC32Registers.FPR16))
	.add(new BankRegister(328, 8, PPC32Registers.FPR17))
	.add(new BankRegister(336, 8, PPC32Registers.FPR18))
	.add(new BankRegister(344, 8, PPC32Registers.FPR19))
	.add(new BankRegister(352, 8, PPC32Registers.FPR20))
	.add(new BankRegister(360, 8, PPC32Registers.FPR21))
	.add(new BankRegister(368, 8, PPC32Registers.FPR22))
	.add(new BankRegister(376, 8, PPC32Registers.FPR23))
	.add(new BankRegister(384, 8, PPC32Registers.FPR24))
	.add(new BankRegister(392, 8, PPC32Registers.FPR25))
	.add(new BankRegister(400, 8, PPC32Registers.FPR26))
	.add(new BankRegister(408, 8, PPC32Registers.FPR27))
	.add(new BankRegister(416, 8, PPC32Registers.FPR28))
	.add(new BankRegister(424, 8, PPC32Registers.FPR29))
	.add(new BankRegister(432, 8, PPC32Registers.FPR30))
	.add(new BankRegister(440, 8, PPC32Registers.FPR31))
	//There is a pad of 4 bytes before the FPSCR reg
	.add(new BankRegister(452, 4, PPC32Registers.FPSCR)) //(PT_FPR0 + 2*32 + 1)
	;

    public static final BankRegisterMap GREGS64
	= new BankRegisterMap()
	.add(new BankRegister(0, 8, PPC64Registers.GPR0))
	.add(new BankRegister(8, 8, PPC64Registers.GPR1))
	.add(new BankRegister(16, 8, PPC64Registers.GPR2))
	.add(new BankRegister(24, 8, PPC64Registers.GPR3))
	.add(new BankRegister(32, 8, PPC64Registers.GPR4))
	.add(new BankRegister(40, 8, PPC64Registers.GPR5))
	.add(new BankRegister(48, 8, PPC64Registers.GPR6))
	.add(new BankRegister(56, 8, PPC64Registers.GPR7))
	.add(new BankRegister(64, 8, PPC64Registers.GPR8))
	.add(new BankRegister(72, 8, PPC64Registers.GPR9))
	.add(new BankRegister(80, 8, PPC64Registers.GPR10))
	.add(new BankRegister(88, 8, PPC64Registers.GPR11))
	.add(new BankRegister(96, 8, PPC64Registers.GPR12))
	.add(new BankRegister(104, 8, PPC64Registers.GPR13))
	.add(new BankRegister(112, 8, PPC64Registers.GPR14))
	.add(new BankRegister(120, 8, PPC64Registers.GPR15))
	.add(new BankRegister(128, 8, PPC64Registers.GPR16))
	.add(new BankRegister(136, 8, PPC64Registers.GPR17))
	.add(new BankRegister(144, 8, PPC64Registers.GPR18))
	.add(new BankRegister(152, 8, PPC64Registers.GPR19))
	.add(new BankRegister(160, 8, PPC64Registers.GPR20))
	.add(new BankRegister(168, 8, PPC64Registers.GPR21))
	.add(new BankRegister(176, 8, PPC64Registers.GPR22))
	.add(new BankRegister(184, 8, PPC64Registers.GPR23))
	.add(new BankRegister(192, 8, PPC64Registers.GPR24))
	.add(new BankRegister(200, 8, PPC64Registers.GPR25))
	.add(new BankRegister(208, 8, PPC64Registers.GPR26))
	.add(new BankRegister(216, 8, PPC64Registers.GPR27))
	.add(new BankRegister(224, 8, PPC64Registers.GPR28))
	.add(new BankRegister(232, 8, PPC64Registers.GPR29))
	.add(new BankRegister(240, 8, PPC64Registers.GPR30))
	.add(new BankRegister(248, 8, PPC64Registers.GPR31))
	.add(new BankRegister(256, 8, PPC64Registers.NIP))
	.add(new BankRegister(264, 8, PPC64Registers.MSR)) //in gdb: .ps_offset = 264
	.add(new BankRegister(272, 8, PPC64Registers.ORIGR3))
	.add(new BankRegister(280, 8, PPC64Registers.CTR))
	.add(new BankRegister(288, 8, PPC64Registers.LR))
	.add(new BankRegister(296, 8, PPC64Registers.XER))
	.add(new BankRegister(304, 8, PPC64Registers.CCR))
	.add(new BankRegister(312, 8, PPC64Registers.SOFTE))
	.add(new BankRegister(320, 8, PPC64Registers.TRAP))
	.add(new BankRegister(328, 8, PPC64Registers.DAR))
	.add(new BankRegister(336, 8, PPC64Registers.DSISR))
	.add(new BankRegister(344, 8, PPC64Registers.RESULT))
	;

    public static final BankRegisterMap FPREGS64
	= new BankRegisterMap()
	.add(new BankRegister(384, 8, PPC64Registers.FPR0)) //PT_FPR0 48 
	.add(new BankRegister(392, 8, PPC64Registers.FPR1))
	.add(new BankRegister(400, 8, PPC64Registers.FPR2))
	.add(new BankRegister(408, 8, PPC64Registers.FPR3))
	.add(new BankRegister(416, 8, PPC64Registers.FPR4))
	.add(new BankRegister(424, 8, PPC64Registers.FPR5))
	.add(new BankRegister(432, 8, PPC64Registers.FPR6))
	.add(new BankRegister(440, 8, PPC64Registers.FPR7))
	.add(new BankRegister(448, 8, PPC64Registers.FPR8))
	.add(new BankRegister(456, 8, PPC64Registers.FPR9))
	.add(new BankRegister(464, 8, PPC64Registers.FPR10))
	.add(new BankRegister(472, 8, PPC64Registers.FPR11))
	.add(new BankRegister(480, 8, PPC64Registers.FPR12))
	.add(new BankRegister(488, 8, PPC64Registers.FPR13))
	.add(new BankRegister(496, 8, PPC64Registers.FPR14))
	.add(new BankRegister(504, 8, PPC64Registers.FPR15))
	.add(new BankRegister(512, 8, PPC64Registers.FPR16))
	.add(new BankRegister(520, 8, PPC64Registers.FPR17))
	.add(new BankRegister(528, 8, PPC64Registers.FPR18))
	.add(new BankRegister(536, 8, PPC64Registers.FPR19))
	.add(new BankRegister(544, 8, PPC64Registers.FPR20))
	.add(new BankRegister(552, 8, PPC64Registers.FPR21))
	.add(new BankRegister(560, 8, PPC64Registers.FPR22))
	.add(new BankRegister(568, 8, PPC64Registers.FPR23))
	.add(new BankRegister(576, 8, PPC64Registers.FPR24))
	.add(new BankRegister(584, 8, PPC64Registers.FPR25))
	.add(new BankRegister(592, 8, PPC64Registers.FPR26))
	.add(new BankRegister(600, 8, PPC64Registers.FPR27))
	.add(new BankRegister(608, 8, PPC64Registers.FPR28))
	.add(new BankRegister(616, 8, PPC64Registers.FPR29))
	.add(new BankRegister(624, 8, PPC64Registers.FPR30))
	.add(new BankRegister(632, 8, PPC64Registers.FPR31))
	.add(new BankRegister(640, 4, PPC64Registers.FPSCR))
	;

    public static final BankRegisterMap VRREGS
	= new BankRegisterMap()
	// Fixme: need to implement altivec registers
	// Vector Registers are 128 bit wide - in both PPC32 and PPC64
	//.add(new BankRegister(0, 656, 16, PPC64Registers.VR0)) PT_VR0 82
	//...
	//.add(new BankRegister(0, 1152, 16, PPC64Registers.V31)) PT_VR0 + 31*2), index 148
	//Need to put a 8 bytes pad here, because VSCR is 8 byte wide only 
	.add(new BankRegister(1176, 8, PPC64Registers.VSCR)) // PT_VSCR (PT_VR0 + 32*2 + 1), index 147
	.add(new BankRegister(1184, 8, PPC64Registers.VRSAVE)) // PT_VRSAVE (PT_VR0 + 33*2), index 148
	;

}
