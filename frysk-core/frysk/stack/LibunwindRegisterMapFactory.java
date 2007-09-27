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
import frysk.proc.Isa;
import frysk.proc.IsaIA32;
import frysk.proc.IsaX8664;
import frysk.isa.IA32Registers;
import frysk.isa.X8664Registers;

public class LibunwindRegisterMapFactory {

    public static RegisterMap getRegisterMap(Isa isa) {
	if (isa instanceof IsaIA32)
	    return IA32;
	else if (isa instanceof IsaX8664)
	    return X8664;
	else
	    throw new RuntimeException("Isa not supported");
    }

    static final RegisterMap IA32 = new RegisterMap() 
	.add(IA32Registers.EAX, new Integer(UnwindRegistersX86.EAX_))
	.add(IA32Registers.EDX, new Integer(UnwindRegistersX86.EDX_))
	.add(IA32Registers.ECX, new Integer(UnwindRegistersX86.ECX_))
	.add(IA32Registers.EBX, new Integer(UnwindRegistersX86.EBX_))
	.add(IA32Registers.ESI, new Integer(UnwindRegistersX86.ESI_))
	.add(IA32Registers.EDI, new Integer(UnwindRegistersX86.EDI_))
	.add(IA32Registers.EBP, new Integer(UnwindRegistersX86.EBP_))
	.add(IA32Registers.ESP, new Integer(UnwindRegistersX86.ESP_))
	.add(IA32Registers.EIP, new Integer(UnwindRegistersX86.EIP_))
	.add(IA32Registers.EFLAGS, new Integer(UnwindRegistersX86.EFLAGS_))
	.add(IA32Registers.TRAPS, new Integer(UnwindRegistersX86.TRAPNO_))
    // MMX registers
	.add(IA32Registers.ST0, new Integer(UnwindRegistersX86.ST0_))
	.add(IA32Registers.ST1, new Integer(UnwindRegistersX86.ST1_))
	.add(IA32Registers.ST2, new Integer(UnwindRegistersX86.ST2_))
	.add(IA32Registers.ST3, new Integer(UnwindRegistersX86.ST3_))
	.add(IA32Registers.ST4, new Integer(UnwindRegistersX86.ST4_))
	.add(IA32Registers.ST5, new Integer(UnwindRegistersX86.ST5_))
	.add(IA32Registers.ST6, new Integer(UnwindRegistersX86.ST6_))
	.add(IA32Registers.ST7, new Integer(UnwindRegistersX86.ST7_))
	.add(IA32Registers.FCW, new Integer(UnwindRegistersX86.FCW_))
	.add(IA32Registers.FSW, new Integer(UnwindRegistersX86.FSW_))
	.add(IA32Registers.FTW, new Integer(UnwindRegistersX86.FTW_))
	.add(IA32Registers.FOP, new Integer(UnwindRegistersX86.FOP_))
	.add(IA32Registers.FCS, new Integer(UnwindRegistersX86.FCS_))
	.add(IA32Registers.FIP, new Integer(UnwindRegistersX86.FIP_))
	.add(IA32Registers.FEA, new Integer(UnwindRegistersX86.FEA_))
	.add(IA32Registers.FDS, new Integer(UnwindRegistersX86.FDS_))
    // SSE Registers
    //TODO: XMMx registers.
    //.add(IA32Registers.MXCSR, new Integer(UnwindRegistersX86.MXCSR_))
    // Segment registers
	.add(IA32Registers.GS, new Integer(UnwindRegistersX86.GS_))
	.add(IA32Registers.FS, new Integer(UnwindRegistersX86.FS_))
	.add(IA32Registers.ES, new Integer(UnwindRegistersX86.ES_))
	.add(IA32Registers.DS, new Integer(UnwindRegistersX86.DS_))
	.add(IA32Registers.SS, new Integer(UnwindRegistersX86.SS_))
	.add(IA32Registers.CS, new Integer(UnwindRegistersX86.CS_))
	.add(IA32Registers.TSS, new Integer(UnwindRegistersX86.TSS_))
	.add(IA32Registers.LDT, new Integer(UnwindRegistersX86.LDT_))
    // frame info
	.add(IA32Registers.CFA, new Integer(UnwindRegistersX86.CFA_));
	
    


    static final RegisterMap X8664 = new RegisterMap() 
	.add(X8664Registers.RAX, new Integer(UnwindRegistersX8664.RAX_))
	.add(X8664Registers.RDX, new Integer(UnwindRegistersX8664.RDX_))
	.add(X8664Registers.RCX, new Integer(UnwindRegistersX8664.RCX_))
	.add(X8664Registers.RBX, new Integer(UnwindRegistersX8664.RBX_))
	.add(X8664Registers.RSI, new Integer(UnwindRegistersX8664.RSI_))
	.add(X8664Registers.RDI, new Integer(UnwindRegistersX8664.RDI_))
	.add(X8664Registers.RBP, new Integer(UnwindRegistersX8664.RBP_))
	.add(X8664Registers.RSP, new Integer(UnwindRegistersX8664.RSP_))
	.add(X8664Registers.R8, new Integer(UnwindRegistersX8664.R8_))
	.add(X8664Registers.R9, new Integer(UnwindRegistersX8664.R9_))
	.add(X8664Registers.R10, new Integer(UnwindRegistersX8664.R10_))
	.add(X8664Registers.R11, new Integer(UnwindRegistersX8664.R11_))
	.add(X8664Registers.R12, new Integer(UnwindRegistersX8664.R12_))
	.add(X8664Registers.R13, new Integer(UnwindRegistersX8664.R13_))
	.add(X8664Registers.R14, new Integer(UnwindRegistersX8664.R14_))
	.add(X8664Registers.R15, new Integer(UnwindRegistersX8664.R15_))
	.add(X8664Registers.RIP, new Integer(UnwindRegistersX8664.RIP_))
	.add(X8664Registers.CFA, new Integer(UnwindRegistersX8664.CFA_));
}
