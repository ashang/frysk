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

import frysk.isa.RegisterMap;
import lib.unwind.UnwindRegistersX86;
import lib.unwind.UnwindRegistersX8664;
import frysk.isa.ISA;
import frysk.isa.IA32Registers;
import frysk.isa.X8664Registers;
import frysk.isa.X87Registers;
import frysk.isa.ISAMap;

public class LibunwindRegisterMapFactory {

    private static final RegisterMap IA32 = new RegisterMap() 
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
    // MMX registers
	.add(X87Registers.ST0, UnwindRegistersX86.ST0)
	.add(X87Registers.ST1, UnwindRegistersX86.ST1)
	.add(X87Registers.ST2, UnwindRegistersX86.ST2)
	.add(X87Registers.ST3, UnwindRegistersX86.ST3)
	.add(X87Registers.ST4, UnwindRegistersX86.ST4)
	.add(X87Registers.ST5, UnwindRegistersX86.ST5)
	.add(X87Registers.ST6, UnwindRegistersX86.ST6)
	.add(X87Registers.ST7, UnwindRegistersX86.ST7)
	.add(IA32Registers.FCW, UnwindRegistersX86.FCW)
	.add(IA32Registers.FSW, UnwindRegistersX86.FSW)
	.add(IA32Registers.FTW, UnwindRegistersX86.FTW)
	.add(IA32Registers.FOP, UnwindRegistersX86.FOP)
	.add(IA32Registers.FCS, UnwindRegistersX86.FCS)
	.add(IA32Registers.FIP, UnwindRegistersX86.FIP)
	.add(IA32Registers.FEA, UnwindRegistersX86.FEA)
	.add(IA32Registers.FDS, UnwindRegistersX86.FDS)
    // SSE Registers
    //TODO: XMMx registers.
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

    private static final RegisterMap X8664 = new RegisterMap() 
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


    private static final ISAMap isaToMap
	= new ISAMap("LibunwindRegisterMapFactory")
	.put(ISA.IA32, IA32)
	.put(ISA.X8664, X8664)
	;
    public static RegisterMap getRegisterMap(ISA isa) {
	return (RegisterMap)isaToMap.get(isa);
    }

}
