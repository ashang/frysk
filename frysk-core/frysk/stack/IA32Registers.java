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

import frysk.value.ArithmeticType;

public class IA32Registers {

    public final static Register EAX = new Register("eax", ArithmeticType.int32Type);

    public final static Register EDX = new Register("edx", ArithmeticType.int32Type);

    public final static Register ECX = new Register("ecx", ArithmeticType.int32Type);

    public final static Register EBX = new Register("ebx", ArithmeticType.int32Type);

    public final static Register ESI = new Register("esi", ArithmeticType.int32Type);

    public final static Register EDI = new Register("edi", ArithmeticType.int32Type);

    public final static Register EBP = new Register("ebp", ArithmeticType.int32Type);

    public final static Register ESP = new Register("esp", ArithmeticType.int32Type);

    public final static Register EIP = new Register("eip", ArithmeticType.int32Type);

    public final static Register EFLAGS = new Register("eflags", ArithmeticType.int32Type);

    public final static Register TRAPS = new Register("traps", ArithmeticType.int32Type);

    /* MMX/stacked-fp registers */

    public final static Register ST0 = new Register("st0", ArithmeticType.float32Type);

    public final static Register ST1 = new Register("st1", ArithmeticType.float32Type);

    public final static Register ST2 = new Register("st2", ArithmeticType.float32Type);

    public final static Register ST3 = new Register("st3", ArithmeticType.float32Type);

    public final static Register ST4 = new Register("st4", ArithmeticType.float32Type);

    public final static Register ST5 = new Register("st5", ArithmeticType.float32Type);

    public final static Register ST6 = new Register("st6", ArithmeticType.float32Type);

    public final static Register ST7 = new Register("st7", ArithmeticType.float32Type);

    public final static Register FCW = new Register("fcw", ArithmeticType.float32Type);

    public final static Register FSW = new Register("fsw", ArithmeticType.float32Type);

    public final static Register FTW = new Register("ftw", ArithmeticType.float32Type);

    public final static Register FOP = new Register("fop", ArithmeticType.float32Type);

    public final static Register FCS = new Register("fcs", ArithmeticType.float32Type);

    public final static Register FIP = new Register("fip", ArithmeticType.float32Type);

    public final static Register FEA = new Register("fea", ArithmeticType.float32Type);

    public final static Register FDS = new Register("fds", ArithmeticType.float32Type);

    /* SSE registers */

    public final static Register XMM0 = new Register("xmm0", ArithmeticType.int32Type);

    public final static Register XMM1 = new Register("xmm1", ArithmeticType.int32Type);

    public final static Register XMM2 = new Register("xmm2", ArithmeticType.int32Type);

    public final static Register XMM3 = new Register("xmm3", ArithmeticType.int32Type);

    public final static Register XMM4 = new Register("xmm4", ArithmeticType.int32Type);

    public final static Register XMM5 = new Register("xmm5", ArithmeticType.int32Type);

    public final static Register XMM6 = new Register("xmm6", ArithmeticType.int32Type);

    public final static Register XMM7 = new Register("xmm7", ArithmeticType.int32Type);

    public final static Register MXCSR = new Register("mxcsr", ArithmeticType.int32Type);

    /* segment registers */

    public final static Register GS = new Register("gs", ArithmeticType.int32Type);

    public final static Register FS = new Register("fs", ArithmeticType.int32Type);

    public final static Register ES = new Register("es", ArithmeticType.int32Type);

    public final static Register DS = new Register("ds", ArithmeticType.int32Type);

    public final static Register SS = new Register("ss", ArithmeticType.int32Type);

    public final static Register CS = new Register("cs", ArithmeticType.int32Type);

    public final static Register TSS = new Register("tss", ArithmeticType.int32Type);

    public final static Register LDT = new Register("ldt", ArithmeticType.int32Type);

    /* frame info (read-only) */
    public final static Register CFA = new Register("cfa", ArithmeticType.int32Type);

    public final static RegisterGroup GENERAL = new RegisterGroup("general",
	    new Register[] { EAX, EBX, ECX, EDX, ESI, EDI, EBP, EIP, EFLAGS,
		    ESP});

    public final static RegisterGroup MMX = new RegisterGroup("mmx",
	    new Register[] { ST0, ST1, ST2, ST3, ST4, ST5, ST6, ST7, FCW, FSW,
		    FTW, FOP, FCS, FIP, FEA, FDS });

    public final static RegisterGroup SSE = new RegisterGroup("sse",
	    new Register[] { XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7,
		    MXCSR });

    public final static RegisterGroup SEGMENT = new RegisterGroup("segment",
	    new Register[] { GS, FS, ES, DS, SS, CS});

    public final static RegisterGroup ALL;
    static {
	Register[] allRegs = new Register[GENERAL.registers.length
		+ MMX.registers.length + SSE.registers.length
		+ SEGMENT.registers.length + 1 /* cfa */];

	int count = 0;
	System.arraycopy(GENERAL.registers, 0, allRegs, count,
		GENERAL.registers.length);
	count += GENERAL.registers.length;
	System
		.arraycopy(MMX.registers, 0, allRegs, count,
			MMX.registers.length);
	count += MMX.registers.length;
	System
		.arraycopy(SSE.registers, 0, allRegs, count,
			SSE.registers.length);
	count += SSE.registers.length;
	System.arraycopy(SEGMENT.registers, 0, allRegs, count,
		SEGMENT.registers.length);

	allRegs[allRegs.length - 1] = CFA;
	ALL = new RegisterGroup("all", allRegs);
    }

}
