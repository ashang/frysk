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

package frysk.isa;

import frysk.value.StandardTypes;

public class IA32Registers extends Registers {

    // General-Purpose registers.

    public final static Register EAX
	= new Register("eax", StandardTypes.INT32L_T);
    public final static Register EBX
	= new Register("ebx", StandardTypes.INT32L_T);
    public final static Register ECX
	= new Register("ecx", StandardTypes.INT32L_T);
    public final static Register EDX
	= new Register("edx", StandardTypes.INT32L_T);
    public final static Register ESI
	= new Register("esi", StandardTypes.INT32L_T);
    public final static Register EDI
	= new Register("edi", StandardTypes.INT32L_T);
    public final static Register EBP
	= new Register("ebp", StandardTypes.VOIDPTR32L_T);
    public final static Register ESP
	= new Register("esp", StandardTypes.VOIDPTR32L_T)
;
    // segment registers

    public final static Register GS
	= new Register("gs", StandardTypes.INT16L_T);
    public final static Register FS
	= new Register("fs", StandardTypes.INT16L_T);
    public final static Register ES
	= new Register("es", StandardTypes.INT16L_T);
    public final static Register DS
	= new Register("ds", StandardTypes.INT16L_T);
    public final static Register SS
	= new Register("ss", StandardTypes.INT16L_T);
    public final static Register CS
	= new Register("cs", StandardTypes.INT16L_T);

    // Program Status and control register.

    public final static Register EFLAGS
	= new Register("eflags", StandardTypes.INT32L_T);

    // Instruction pointer

    public final static Register EIP
	= new Register("eip", StandardTypes.VOIDPTR32L_T);

    // Floating-point registers

    public final static Register ST0
	= new Register("st0", StandardTypes.FLOAT80L_T);
    public final static Register ST1
	= new Register("st1", StandardTypes.FLOAT80L_T);
    public final static Register ST2
	= new Register("st2", StandardTypes.FLOAT80L_T);
    public final static Register ST3
	= new Register("st3", StandardTypes.FLOAT80L_T);
    public final static Register ST4
	= new Register("st4", StandardTypes.FLOAT80L_T);
    public final static Register ST5
	= new Register("st5", StandardTypes.FLOAT80L_T);
    public final static Register ST6
	= new Register("st6", StandardTypes.FLOAT80L_T);
    public final static Register ST7
	= new Register("st7", StandardTypes.FLOAT80L_T);
    public final static Register FCW
	= new Register("fcw", StandardTypes.INT32L_T);
    public final static Register FSW
	= new Register("fsw", StandardTypes.INT32L_T);
    public final static Register FTW
	= new Register("ftw", StandardTypes.INT32L_T);
    public final static Register FOP
	= new Register("fop", StandardTypes.INT32L_T);
    public final static Register FCS
	= new Register("fcs", StandardTypes.INT32L_T);
    public final static Register FIP
	= new Register("fip", StandardTypes.INT32L_T);
    public final static Register FEA
	= new Register("fea", StandardTypes.INT32L_T);
    public final static Register FDS
	= new Register("fds", StandardTypes.INT32L_T);

    // Streaming SIMD registers

    public final static Register XMM0
	= new Register("xmm0", StandardTypes.INT128L_T);
    public final static Register XMM1
	= new Register("xmm1", StandardTypes.INT128L_T);
    public final static Register XMM2
	= new Register("xmm2", StandardTypes.INT128L_T);
    public final static Register XMM3
	= new Register("xmm3", StandardTypes.INT128L_T);
    public final static Register XMM4
	= new Register("xmm4", StandardTypes.INT128L_T);
    public final static Register XMM5
	= new Register("xmm5", StandardTypes.INT128L_T);
    public final static Register XMM6
	= new Register("xmm6", StandardTypes.INT128L_T);
    public final static Register XMM7
	= new Register("xmm7", StandardTypes.INT128L_T);

    public final static Register MXCSR
	= new Register("mxcsr", StandardTypes.INT32L_T);

    public final static Register TSS
	= new Register("tss", StandardTypes.INT32L_T);
    public final static Register LDT
	= new Register("ldt", StandardTypes.INT32L_T);
    public final static Register TRAPS
	= new Register("traps", StandardTypes.INT32L_T);

    public final static RegisterGroup GENERAL
	= new RegisterGroup("general",
			    new Register[] {
				EAX, EBX, ECX, EDX, ESI, EDI, EBP, EIP,
				EFLAGS, ESP
			    });

    public final static RegisterGroup MMX
	= new RegisterGroup("mmx",
			    new Register[] {
				ST0, ST1, ST2, ST3, ST4, ST5, ST6, ST7,
				FCW, FSW, FTW, FOP, FCS, FIP, FEA, FDS
			    });

    public final static RegisterGroup SSE
	= new RegisterGroup("sse",
			    new Register[] {
				XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7,
				MXCSR
			    });

    public final static RegisterGroup SEGMENT
	= new RegisterGroup("segment",
			    new Register[] {
				GS, FS, ES, DS, SS, CS
			    });

    public final static RegisterGroup ALL;
    static {
	Register[] allRegs
	    = new Register[GENERAL.getRegisters().length
			   + MMX.getRegisters().length
			   + SSE.getRegisters().length
			   + SEGMENT.getRegisters().length];
	int count = 0;
	System.arraycopy(GENERAL.getRegisters(), 0, allRegs, count,
			 GENERAL.getRegisters().length);
	count += GENERAL.getRegisters().length;
	System.arraycopy(MMX.getRegisters(), 0, allRegs, count,
			 MMX.getRegisters().length);
	count += MMX.getRegisters().length;
	System.arraycopy(SSE.getRegisters(), 0, allRegs, count,
			 SSE.getRegisters().length);
	count += SSE.getRegisters().length;
	System.arraycopy(SEGMENT.getRegisters(), 0, allRegs, count,
			 SEGMENT.getRegisters().length);

	ALL = new RegisterGroup("all", allRegs);
    }

    IA32Registers() {
	super (new RegisterGroup[] {
		   GENERAL, MMX, SSE, SEGMENT, ALL
	       });
    }

    public Register getProgramCounter() {
	return EIP;
    }

    public Register getStackPointer() {
	return ESP;
    }

    public RegisterGroup getDefaultRegisterGroup() {
	return GENERAL;
    }

    public RegisterGroup getAllRegistersGroup() {
	return ALL;
    }
}
