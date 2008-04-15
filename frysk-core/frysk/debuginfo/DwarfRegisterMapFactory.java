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

package frysk.debuginfo;

import lib.dwfl.DwarfRegistersX86;
import lib.dwfl.DwarfRegistersX8664;
import lib.dwfl.DwarfRegistersPPC64;
import frysk.isa.ISA;
import frysk.isa.ISAMap;
import frysk.isa.registers.IA32Registers;
import frysk.isa.registers.RegisterMap;
import frysk.isa.registers.X8664Registers;
import frysk.isa.registers.PPC64Registers;

public class DwarfRegisterMapFactory {

    private static final RegisterMap IA32
	= new RegisterMap("IA-32 DWARF")
	.add(IA32Registers.EAX, DwarfRegistersX86.EAX)
	.add(IA32Registers.ECX, DwarfRegistersX86.ECX)
	.add(IA32Registers.EDX, DwarfRegistersX86.EDX)
	.add(IA32Registers.EBX, DwarfRegistersX86.EBX)
	.add(IA32Registers.ESP, DwarfRegistersX86.ESP)
	.add(IA32Registers.EBP, DwarfRegistersX86.EBP)
	.add(IA32Registers.ESI, DwarfRegistersX86.ESI)
	.add(IA32Registers.EDI, DwarfRegistersX86.EDI)
	;

    private static final RegisterMap X8664
	= new RegisterMap("X86-64 DWARF")
	.add(X8664Registers.RAX, DwarfRegistersX8664.RAX)
	.add(X8664Registers.RDX, DwarfRegistersX8664.RDX)
	.add(X8664Registers.RCX, DwarfRegistersX8664.RCX)
	.add(X8664Registers.RBX, DwarfRegistersX8664.RBX)
	.add(X8664Registers.RSI, DwarfRegistersX8664.RSI)
	.add(X8664Registers.RDI, DwarfRegistersX8664.RDI)
	.add(X8664Registers.RBP, DwarfRegistersX8664.RBP)
	.add(X8664Registers.RSP, DwarfRegistersX8664.RSP)
	.add(X8664Registers.R8, DwarfRegistersX8664.R8)
	.add(X8664Registers.R9, DwarfRegistersX8664.R9)
	.add(X8664Registers.R10, DwarfRegistersX8664.R10)
	.add(X8664Registers.R11, DwarfRegistersX8664.R11)
	.add(X8664Registers.R12, DwarfRegistersX8664.R12)
	.add(X8664Registers.R13, DwarfRegistersX8664.R13)
	.add(X8664Registers.R14, DwarfRegistersX8664.R14)
	.add(X8664Registers.R15, DwarfRegistersX8664.R15)
	.add(X8664Registers.RIP, DwarfRegistersX8664.RIP)
	;

    private static final RegisterMap PPC64BE
	= new RegisterMap("PowerPC64 DWARF")
	.add(PPC64Registers.GPR0, DwarfRegistersPPC64.GPR0)
	.add(PPC64Registers.GPR1, DwarfRegistersPPC64.GPR1)
	.add(PPC64Registers.GPR2, DwarfRegistersPPC64.GPR2)
	.add(PPC64Registers.GPR3, DwarfRegistersPPC64.GPR3)
	.add(PPC64Registers.GPR4, DwarfRegistersPPC64.GPR4)
	.add(PPC64Registers.GPR5, DwarfRegistersPPC64.GPR5)
	.add(PPC64Registers.GPR6, DwarfRegistersPPC64.GPR6)
	.add(PPC64Registers.GPR7, DwarfRegistersPPC64.GPR7)
	.add(PPC64Registers.GPR8, DwarfRegistersPPC64.GPR8)
	.add(PPC64Registers.GPR9, DwarfRegistersPPC64.GPR9)
	.add(PPC64Registers.GPR10, DwarfRegistersPPC64.GPR10)
	.add(PPC64Registers.GPR11, DwarfRegistersPPC64.GPR11)
	.add(PPC64Registers.GPR12, DwarfRegistersPPC64.GPR12)
	.add(PPC64Registers.GPR13, DwarfRegistersPPC64.GPR13)
	.add(PPC64Registers.GPR14, DwarfRegistersPPC64.GPR14)
	.add(PPC64Registers.GPR15, DwarfRegistersPPC64.GPR15)
	.add(PPC64Registers.GPR16, DwarfRegistersPPC64.GPR16)
	.add(PPC64Registers.GPR17, DwarfRegistersPPC64.GPR17)
	.add(PPC64Registers.GPR18, DwarfRegistersPPC64.GPR18)
	.add(PPC64Registers.GPR19, DwarfRegistersPPC64.GPR19)
	.add(PPC64Registers.GPR20, DwarfRegistersPPC64.GPR20)
	.add(PPC64Registers.GPR21, DwarfRegistersPPC64.GPR21)
	.add(PPC64Registers.GPR22, DwarfRegistersPPC64.GPR22)
	.add(PPC64Registers.GPR23, DwarfRegistersPPC64.GPR23)
	.add(PPC64Registers.GPR24, DwarfRegistersPPC64.GPR24)
	.add(PPC64Registers.GPR25, DwarfRegistersPPC64.GPR25)
	.add(PPC64Registers.GPR26, DwarfRegistersPPC64.GPR26)
	.add(PPC64Registers.GPR27, DwarfRegistersPPC64.GPR27)
	.add(PPC64Registers.GPR28, DwarfRegistersPPC64.GPR28)
	.add(PPC64Registers.GPR29, DwarfRegistersPPC64.GPR29)
	.add(PPC64Registers.GPR30, DwarfRegistersPPC64.GPR30)
	.add(PPC64Registers.GPR31, DwarfRegistersPPC64.GPR31)
	.add(PPC64Registers.FPR0, DwarfRegistersPPC64.FPR0)
	.add(PPC64Registers.FPR1, DwarfRegistersPPC64.FPR1)
	.add(PPC64Registers.FPR2, DwarfRegistersPPC64.FPR2)
	.add(PPC64Registers.FPR3, DwarfRegistersPPC64.FPR3)
	.add(PPC64Registers.FPR4, DwarfRegistersPPC64.FPR4)
	.add(PPC64Registers.FPR5, DwarfRegistersPPC64.FPR5)
	.add(PPC64Registers.FPR6, DwarfRegistersPPC64.FPR6)
	.add(PPC64Registers.FPR7, DwarfRegistersPPC64.FPR7)
	.add(PPC64Registers.FPR8, DwarfRegistersPPC64.FPR8)
	.add(PPC64Registers.FPR9, DwarfRegistersPPC64.FPR9)
	.add(PPC64Registers.FPR10, DwarfRegistersPPC64.FPR10)
	.add(PPC64Registers.FPR11, DwarfRegistersPPC64.FPR11)
	.add(PPC64Registers.FPR12, DwarfRegistersPPC64.FPR12)
	.add(PPC64Registers.FPR13, DwarfRegistersPPC64.FPR13)
	.add(PPC64Registers.FPR14, DwarfRegistersPPC64.FPR14)
	.add(PPC64Registers.FPR15, DwarfRegistersPPC64.FPR15)
	.add(PPC64Registers.FPR16, DwarfRegistersPPC64.FPR16)
	.add(PPC64Registers.FPR17, DwarfRegistersPPC64.FPR17)
	.add(PPC64Registers.FPR18, DwarfRegistersPPC64.FPR18)
	.add(PPC64Registers.FPR19, DwarfRegistersPPC64.FPR19)
	.add(PPC64Registers.FPR20, DwarfRegistersPPC64.FPR20)
	.add(PPC64Registers.FPR21, DwarfRegistersPPC64.FPR21)
	.add(PPC64Registers.FPR22, DwarfRegistersPPC64.FPR22)
	.add(PPC64Registers.FPR23, DwarfRegistersPPC64.FPR23)
	.add(PPC64Registers.FPR24, DwarfRegistersPPC64.FPR24)
	.add(PPC64Registers.FPR25, DwarfRegistersPPC64.FPR25)
	.add(PPC64Registers.FPR26, DwarfRegistersPPC64.FPR26)
	.add(PPC64Registers.FPR27, DwarfRegistersPPC64.FPR27)
	.add(PPC64Registers.FPR28, DwarfRegistersPPC64.FPR28)
	.add(PPC64Registers.FPR29, DwarfRegistersPPC64.FPR29)
	.add(PPC64Registers.FPR30, DwarfRegistersPPC64.FPR30)
	.add(PPC64Registers.FPR31, DwarfRegistersPPC64.FPR31)
	.add(PPC64Registers.CCR, DwarfRegistersPPC64.CCR)
	.add(PPC64Registers.FPSCR, DwarfRegistersPPC64.FPSCR)
	.add(PPC64Registers.XER, DwarfRegistersPPC64.XER)
	.add(PPC64Registers.LR, DwarfRegistersPPC64.LR)
	.add(PPC64Registers.CTR, DwarfRegistersPPC64.CTR)
	;

    private static final ISAMap isaToMap
	= new ISAMap("DwarfRegisterMapFactory")
	.put(ISA.IA32, IA32)
	.put(ISA.X8664, X8664)
	.put(ISA.PPC64BE, PPC64BE)
	;

    public static RegisterMap getRegisterMap(ISA isa) {
	return (RegisterMap) isaToMap.get(isa);
    }
}
