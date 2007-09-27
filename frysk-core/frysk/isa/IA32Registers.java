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

public class IA32Registers {

    public final static Register EAX = new Register("eax", StandardTypes.intLittleEndianType);

    public final static Register EDX = new Register("edx", StandardTypes.intLittleEndianType);

    public final static Register ECX = new Register("ecx", StandardTypes.intLittleEndianType);

    public final static Register EBX = new Register("ebx", StandardTypes.intLittleEndianType);

    public final static Register ESI = new Register("esi", StandardTypes.intLittleEndianType);

    public final static Register EDI = new Register("edi", StandardTypes.intLittleEndianType);

    public final static Register EBP = new Register("ebp", StandardTypes.intLittleEndianType);

    public final static Register ESP = new Register("esp", StandardTypes.intLittleEndianType);

    public final static Register EIP = new Register("eip", StandardTypes.intLittleEndianType);

    public final static Register EFLAGS = new Register("eflags", StandardTypes.intLittleEndianType);

    public final static Register TRAPS = new Register("traps", StandardTypes.intLittleEndianType);

    /* MMX/stacked-fp registers */

    public final static Register ST0 = new Register("st0", StandardTypes.floatLittleEndianType);

    public final static Register ST1 = new Register("st1", StandardTypes.floatLittleEndianType);

    public final static Register ST2 = new Register("st2", StandardTypes.floatLittleEndianType);

    public final static Register ST3 = new Register("st3", StandardTypes.floatLittleEndianType);

    public final static Register ST4 = new Register("st4", StandardTypes.floatLittleEndianType);

    public final static Register ST5 = new Register("st5", StandardTypes.floatLittleEndianType);

    public final static Register ST6 = new Register("st6", StandardTypes.floatLittleEndianType);

    public final static Register ST7 = new Register("st7", StandardTypes.floatLittleEndianType);

    public final static Register FCW = new Register("fcw", StandardTypes.floatLittleEndianType);

    public final static Register FSW = new Register("fsw", StandardTypes.floatLittleEndianType);

    public final static Register FTW = new Register("ftw", StandardTypes.floatLittleEndianType);

    public final static Register FOP = new Register("fop", StandardTypes.floatLittleEndianType);

    public final static Register FCS = new Register("fcs", StandardTypes.floatLittleEndianType);

    public final static Register FIP = new Register("fip", StandardTypes.floatLittleEndianType);

    public final static Register FEA = new Register("fea", StandardTypes.floatLittleEndianType);

    public final static Register FDS = new Register("fds", StandardTypes.floatLittleEndianType);

    /* SSE registers */

    public final static Register XMM0 = new Register("xmm0", StandardTypes.intLittleEndianType);

    public final static Register XMM1 = new Register("xmm1", StandardTypes.intLittleEndianType);

    public final static Register XMM2 = new Register("xmm2", StandardTypes.intLittleEndianType);

    public final static Register XMM3 = new Register("xmm3", StandardTypes.intLittleEndianType);

    public final static Register XMM4 = new Register("xmm4", StandardTypes.intLittleEndianType);

    public final static Register XMM5 = new Register("xmm5", StandardTypes.intLittleEndianType);

    public final static Register XMM6 = new Register("xmm6", StandardTypes.intLittleEndianType);

    public final static Register XMM7 = new Register("xmm7", StandardTypes.intLittleEndianType);

    public final static Register MXCSR = new Register("mxcsr", StandardTypes.intLittleEndianType);

    /* segment registers */

    public final static Register GS = new Register("gs", StandardTypes.intLittleEndianType);

    public final static Register FS = new Register("fs", StandardTypes.intLittleEndianType);

    public final static Register ES = new Register("es", StandardTypes.intLittleEndianType);

    public final static Register DS = new Register("ds", StandardTypes.intLittleEndianType);

    public final static Register SS = new Register("ss", StandardTypes.intLittleEndianType);

    public final static Register CS = new Register("cs", StandardTypes.intLittleEndianType);

    public final static Register TSS = new Register("tss", StandardTypes.intLittleEndianType);

    public final static Register LDT = new Register("ldt", StandardTypes.intLittleEndianType);

    /* frame info (read-only) */
    public final static Register CFA = new Register("cfa", StandardTypes.intLittleEndianType);

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
