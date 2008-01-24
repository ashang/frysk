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

package frysk.stack;

import frysk.isa.registers.RegisterMap;
import lib.unwind.UnwindRegistersX86;
import lib.unwind.UnwindRegistersX8664;
import lib.unwind.UnwindRegistersPPC32;
import lib.unwind.UnwindRegistersPPC64;
import frysk.isa.ISA;
import frysk.isa.registers.IA32Registers;
import frysk.isa.registers.PPC32Registers;
import frysk.isa.registers.PPC64Registers;
import frysk.isa.registers.X8664Registers;
import frysk.isa.registers.X87Registers;
import frysk.isa.ISAMap;

public class LibunwindRegisterMapFactory {

    private static final RegisterMap IA32
	= new RegisterMap("IA-32 libunwind") 
	.add(IA32Registers.EAX, UnwindRegistersX86.EAX)
	.add(IA32Registers.EDX, UnwindRegistersX86.EDX)
	.add(IA32Registers.ECX, UnwindRegistersX86.ECX)
	.add(IA32Registers.EBX, UnwindRegistersX86.EBX)
	.add(IA32Registers.ESI, UnwindRegistersX86.ESI)
	.add(IA32Registers.EDI, UnwindRegistersX86.EDI)
	.add(IA32Registers.EBP, UnwindRegistersX86.EBP)
	.add(IA32Registers.ESP, UnwindRegistersX86.ESP)
	.add(IA32Registers.EIP, UnwindRegistersX86.EIP)
	.add(IA32Registers.EFLAGS, UnwindRegistersX86.EFLAGS)
	.add(IA32Registers.TRAPS, UnwindRegistersX86.TRAPNO)
    // Floating-point registers
	.add(X87Registers.ST0, UnwindRegistersX86.ST0)
	.add(X87Registers.ST1, UnwindRegistersX86.ST1)
	.add(X87Registers.ST2, UnwindRegistersX86.ST2)
	.add(X87Registers.ST3, UnwindRegistersX86.ST3)
	.add(X87Registers.ST4, UnwindRegistersX86.ST4)
	.add(X87Registers.ST5, UnwindRegistersX86.ST5)
	.add(X87Registers.ST6, UnwindRegistersX86.ST6)
	.add(X87Registers.ST7, UnwindRegistersX86.ST7)
	.add(X87Registers.FCW, UnwindRegistersX86.FCW)
	.add(X87Registers.FSW, UnwindRegistersX86.FSW)
	.add(X87Registers.FTW, UnwindRegistersX86.FTW)
	.add(X87Registers.FOP, UnwindRegistersX86.FOP)
	.add(X87Registers.CS, UnwindRegistersX86.FCS)
	.add(X87Registers.EIP, UnwindRegistersX86.FIP)
	.add(X87Registers.DP, UnwindRegistersX86.FEA)
	.add(X87Registers.DS, UnwindRegistersX86.FDS)
    // XMMx registers.
	.add(X87Registers.XMM0, UnwindRegistersX86.XMM0)
	.add(X87Registers.XMM1, UnwindRegistersX86.XMM1)
	.add(X87Registers.XMM2, UnwindRegistersX86.XMM2)
	.add(X87Registers.XMM3, UnwindRegistersX86.XMM3)
	.add(X87Registers.XMM4, UnwindRegistersX86.XMM4)
	.add(X87Registers.XMM5, UnwindRegistersX86.XMM5)
	.add(X87Registers.XMM6, UnwindRegistersX86.XMM6)
	.add(X87Registers.XMM7, UnwindRegistersX86.XMM7)
    //.add(IA32Registers.MXCSR, UnwindRegistersX86.MXCSR)
    // Segment registers
	.add(IA32Registers.GS, UnwindRegistersX86.GS)
	.add(IA32Registers.FS, UnwindRegistersX86.FS)
	.add(IA32Registers.ES, UnwindRegistersX86.ES)
	.add(IA32Registers.DS, UnwindRegistersX86.DS)
	.add(IA32Registers.SS, UnwindRegistersX86.SS)
	.add(IA32Registers.CS, UnwindRegistersX86.CS)
	.add(IA32Registers.TSS, UnwindRegistersX86.TSS)
	.add(IA32Registers.LDT, UnwindRegistersX86.LDT)
	;

    private static final RegisterMap X8664
	= new RegisterMap("X86-64 libunwind") 
	.add(X8664Registers.RAX, UnwindRegistersX8664.RAX)
	.add(X8664Registers.RDX, UnwindRegistersX8664.RDX)
	.add(X8664Registers.RCX, UnwindRegistersX8664.RCX)
	.add(X8664Registers.RBX, UnwindRegistersX8664.RBX)
	.add(X8664Registers.RSI, UnwindRegistersX8664.RSI)
	.add(X8664Registers.RDI, UnwindRegistersX8664.RDI)
	.add(X8664Registers.RBP, UnwindRegistersX8664.RBP)
	.add(X8664Registers.RSP, UnwindRegistersX8664.RSP)
	.add(X8664Registers.R8, UnwindRegistersX8664.R8)
	.add(X8664Registers.R9, UnwindRegistersX8664.R9)
	.add(X8664Registers.R10, UnwindRegistersX8664.R10)
	.add(X8664Registers.R11, UnwindRegistersX8664.R11)
	.add(X8664Registers.R12, UnwindRegistersX8664.R12)
	.add(X8664Registers.R13, UnwindRegistersX8664.R13)
	.add(X8664Registers.R14, UnwindRegistersX8664.R14)
	.add(X8664Registers.R15, UnwindRegistersX8664.R15)
	.add(X8664Registers.RIP, UnwindRegistersX8664.RIP)
	;
    private static final RegisterMap PPC64
	= new RegisterMap("PPC64 libunwind")
        .add(PPC64Registers.GPR0, UnwindRegistersPPC64.R0)
	.add(PPC64Registers.GPR1, UnwindRegistersPPC64.R1)
        .add(PPC64Registers.GPR2, UnwindRegistersPPC64.R2)
	.add(PPC64Registers.GPR3, UnwindRegistersPPC64.R3)
        .add(PPC64Registers.GPR4, UnwindRegistersPPC64.R4)
	.add(PPC64Registers.GPR5, UnwindRegistersPPC64.R5)
        .add(PPC64Registers.GPR6, UnwindRegistersPPC64.R6)
	.add(PPC64Registers.GPR7, UnwindRegistersPPC64.R7)
        .add(PPC64Registers.GPR8, UnwindRegistersPPC64.R8)
	.add(PPC64Registers.GPR9, UnwindRegistersPPC64.R9)
        .add(PPC64Registers.GPR10, UnwindRegistersPPC64.R10)
	.add(PPC64Registers.GPR11, UnwindRegistersPPC64.R11)
        .add(PPC64Registers.GPR12, UnwindRegistersPPC64.R12)
	.add(PPC64Registers.GPR13, UnwindRegistersPPC64.R13)
        .add(PPC64Registers.GPR14, UnwindRegistersPPC64.R14)
	.add(PPC64Registers.GPR15, UnwindRegistersPPC64.R15)
        .add(PPC64Registers.GPR16, UnwindRegistersPPC64.R16)
	.add(PPC64Registers.GPR17, UnwindRegistersPPC64.R17)
        .add(PPC64Registers.GPR18, UnwindRegistersPPC64.R18)
	.add(PPC64Registers.GPR19, UnwindRegistersPPC64.R19)
        .add(PPC64Registers.GPR20, UnwindRegistersPPC64.R20)
	.add(PPC64Registers.GPR21, UnwindRegistersPPC64.R21)
        .add(PPC64Registers.GPR22, UnwindRegistersPPC64.R22)
	.add(PPC64Registers.GPR23, UnwindRegistersPPC64.R23)
        .add(PPC64Registers.GPR24, UnwindRegistersPPC64.R24)
	.add(PPC64Registers.GPR25, UnwindRegistersPPC64.R25)
        .add(PPC64Registers.GPR26, UnwindRegistersPPC64.R26)
	.add(PPC64Registers.GPR27, UnwindRegistersPPC64.R27)
        .add(PPC64Registers.GPR28, UnwindRegistersPPC64.R28)
	.add(PPC64Registers.GPR29, UnwindRegistersPPC64.R29)
	.add(PPC64Registers.GPR30, UnwindRegistersPPC64.R30)
	.add(PPC64Registers.GPR31, UnwindRegistersPPC64.R31)
	//Special Registers
	.add(PPC64Registers.LR   , UnwindRegistersPPC64.LR )
	.add(PPC64Registers.NIP  , UnwindRegistersPPC64.NIP)
	//Floating Point Registers
	.add(PPC64Registers.FPR0, UnwindRegistersPPC64.F0)
	.add(PPC64Registers.FPR1, UnwindRegistersPPC64.F1)
        .add(PPC64Registers.FPR2, UnwindRegistersPPC64.F2)
	.add(PPC64Registers.FPR3, UnwindRegistersPPC64.F3)
        .add(PPC64Registers.FPR4, UnwindRegistersPPC64.F4)
	.add(PPC64Registers.FPR5, UnwindRegistersPPC64.F5)
        .add(PPC64Registers.FPR6, UnwindRegistersPPC64.F6)
	.add(PPC64Registers.FPR7, UnwindRegistersPPC64.F7)
        .add(PPC64Registers.FPR8, UnwindRegistersPPC64.F8)
	.add(PPC64Registers.FPR9, UnwindRegistersPPC64.F9)
        .add(PPC64Registers.FPR10, UnwindRegistersPPC64.F10)
	.add(PPC64Registers.FPR11, UnwindRegistersPPC64.F11)
        .add(PPC64Registers.FPR12, UnwindRegistersPPC64.F12)
	.add(PPC64Registers.FPR13, UnwindRegistersPPC64.F13)
        .add(PPC64Registers.FPR14, UnwindRegistersPPC64.F14)
	.add(PPC64Registers.FPR15, UnwindRegistersPPC64.F15)
        .add(PPC64Registers.FPR16, UnwindRegistersPPC64.F16)
	.add(PPC64Registers.FPR17, UnwindRegistersPPC64.F17)
        .add(PPC64Registers.FPR18, UnwindRegistersPPC64.F18)
	.add(PPC64Registers.FPR19, UnwindRegistersPPC64.F19)
        .add(PPC64Registers.FPR20, UnwindRegistersPPC64.F20)
	.add(PPC64Registers.FPR21, UnwindRegistersPPC64.F21)
        .add(PPC64Registers.FPR22, UnwindRegistersPPC64.F22)
	.add(PPC64Registers.FPR23, UnwindRegistersPPC64.F23)
        .add(PPC64Registers.FPR24, UnwindRegistersPPC64.F24)
	.add(PPC64Registers.FPR25, UnwindRegistersPPC64.F25)
        .add(PPC64Registers.FPR26, UnwindRegistersPPC64.F26)
	.add(PPC64Registers.FPR27, UnwindRegistersPPC64.F27)
        .add(PPC64Registers.FPR28, UnwindRegistersPPC64.F28)
	.add(PPC64Registers.FPR29, UnwindRegistersPPC64.F29)
	.add(PPC64Registers.FPR30, UnwindRegistersPPC64.F30)
	.add(PPC64Registers.FPR31, UnwindRegistersPPC64.F31)
	;

    private static final RegisterMap PPC32
	= new RegisterMap("PPC32 libunwind")
        .add(PPC32Registers.GPR0, UnwindRegistersPPC32.R0)
	.add(PPC32Registers.GPR1, UnwindRegistersPPC32.R1)
        .add(PPC32Registers.GPR2, UnwindRegistersPPC32.R2)
	.add(PPC32Registers.GPR3, UnwindRegistersPPC32.R3)
        .add(PPC32Registers.GPR4, UnwindRegistersPPC32.R4)
	.add(PPC32Registers.GPR5, UnwindRegistersPPC32.R5)
        .add(PPC32Registers.GPR6, UnwindRegistersPPC32.R6)
	.add(PPC32Registers.GPR7, UnwindRegistersPPC32.R7)
        .add(PPC32Registers.GPR8, UnwindRegistersPPC32.R8)
	.add(PPC32Registers.GPR9, UnwindRegistersPPC32.R9)
        .add(PPC32Registers.GPR10, UnwindRegistersPPC32.R10)
	.add(PPC32Registers.GPR11, UnwindRegistersPPC32.R11)
        .add(PPC32Registers.GPR12, UnwindRegistersPPC32.R12)
	.add(PPC32Registers.GPR13, UnwindRegistersPPC32.R13)
        .add(PPC32Registers.GPR14, UnwindRegistersPPC32.R14)
	.add(PPC32Registers.GPR15, UnwindRegistersPPC32.R15)
        .add(PPC32Registers.GPR16, UnwindRegistersPPC32.R16)
	.add(PPC32Registers.GPR17, UnwindRegistersPPC32.R17)
        .add(PPC32Registers.GPR18, UnwindRegistersPPC32.R18)
	.add(PPC32Registers.GPR19, UnwindRegistersPPC32.R19)
        .add(PPC32Registers.GPR20, UnwindRegistersPPC32.R20)
	.add(PPC32Registers.GPR21, UnwindRegistersPPC32.R21)
        .add(PPC32Registers.GPR22, UnwindRegistersPPC32.R22)
	.add(PPC32Registers.GPR23, UnwindRegistersPPC32.R23)
        .add(PPC32Registers.GPR24, UnwindRegistersPPC32.R24)
	.add(PPC32Registers.GPR25, UnwindRegistersPPC32.R25)
        .add(PPC32Registers.GPR26, UnwindRegistersPPC32.R26)
	.add(PPC32Registers.GPR27, UnwindRegistersPPC32.R27)
        .add(PPC32Registers.GPR28, UnwindRegistersPPC32.R28)
	.add(PPC32Registers.GPR29, UnwindRegistersPPC32.R29)
	.add(PPC32Registers.GPR30, UnwindRegistersPPC32.R30)
	.add(PPC32Registers.GPR31, UnwindRegistersPPC32.R31)
	.add(PPC32Registers.LR   , UnwindRegistersPPC32.LR )
	//Floating Point Registers
	.add(PPC32Registers.FPR0, UnwindRegistersPPC32.F0)
	.add(PPC32Registers.FPR1, UnwindRegistersPPC32.F1)
        .add(PPC32Registers.FPR2, UnwindRegistersPPC32.F2)
	.add(PPC32Registers.FPR3, UnwindRegistersPPC32.F3)
        .add(PPC32Registers.FPR4, UnwindRegistersPPC32.F4)
	.add(PPC32Registers.FPR5, UnwindRegistersPPC32.F5)
        .add(PPC32Registers.FPR6, UnwindRegistersPPC32.F6)
	.add(PPC32Registers.FPR7, UnwindRegistersPPC32.F7)
        .add(PPC32Registers.FPR8, UnwindRegistersPPC32.F8)
	.add(PPC32Registers.FPR9, UnwindRegistersPPC32.F9)
        .add(PPC32Registers.FPR10, UnwindRegistersPPC32.F10)
	.add(PPC32Registers.FPR11, UnwindRegistersPPC32.F11)
        .add(PPC32Registers.FPR12, UnwindRegistersPPC32.F12)
	.add(PPC32Registers.FPR13, UnwindRegistersPPC32.F13)
        .add(PPC32Registers.FPR14, UnwindRegistersPPC32.F14)
	.add(PPC32Registers.FPR15, UnwindRegistersPPC32.F15)
        .add(PPC32Registers.FPR16, UnwindRegistersPPC32.F16)
	.add(PPC32Registers.FPR17, UnwindRegistersPPC32.F17)
        .add(PPC32Registers.FPR18, UnwindRegistersPPC32.F18)
	.add(PPC32Registers.FPR19, UnwindRegistersPPC32.F19)
        .add(PPC32Registers.FPR20, UnwindRegistersPPC32.F20)
	.add(PPC32Registers.FPR21, UnwindRegistersPPC32.F21)
        .add(PPC32Registers.FPR22, UnwindRegistersPPC32.F22)
	.add(PPC32Registers.FPR23, UnwindRegistersPPC32.F23)
        .add(PPC32Registers.FPR24, UnwindRegistersPPC32.F24)
	.add(PPC32Registers.FPR25, UnwindRegistersPPC32.F25)
        .add(PPC32Registers.FPR26, UnwindRegistersPPC32.F26)
	.add(PPC32Registers.FPR27, UnwindRegistersPPC32.F27)
        .add(PPC32Registers.FPR28, UnwindRegistersPPC32.F28)
	.add(PPC32Registers.FPR29, UnwindRegistersPPC32.F29)
	.add(PPC32Registers.FPR30, UnwindRegistersPPC32.F30)
	.add(PPC32Registers.FPR31, UnwindRegistersPPC32.F31)
	;

    private static final ISAMap isaToMap
	= new ISAMap("LibunwindRegisterMapFactory")
	.put(ISA.IA32, IA32)
	.put(ISA.X8664, X8664)
	.put(ISA.PPC64BE, PPC64)
	.put(ISA.PPC32BE, PPC32)
	;
    public static RegisterMap getRegisterMap(ISA isa) {
	return (RegisterMap)isaToMap.get(isa);
    }

}
