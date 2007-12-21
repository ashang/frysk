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

package frysk.bank;

import inua.eio.ByteOrder;
import frysk.isa.PPC32Registers;

public class PPCBankRegisters {

    public static final BankArrayRegisterMap PPC32BE
	= new BankArrayRegisterMap()
	.add(0, LinuxPPCRegisterBanks.USR32)
	;

    public static final BankArrayRegisterMap PPC64BE
	= new BankArrayRegisterMap()
	.add(0, LinuxPPCRegisterBanks.USR64)
	;

    public static final BankArrayRegisterMap PPC32BE_ON_PPC64BE
	= new IndirectBankArrayRegisterMap(ByteOrder.BIG_ENDIAN,
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
