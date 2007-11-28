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

/**
 * The Intel i387 floating-point processor and its successors
 * including MMX extensions.
 *
 * Both the IA32 and the X86-64 provide these registers; provide a
 * common definition.
 */

public class X87Registers {

    // The physical floating-point registers.

    public final static Register FPR0
	= new Register("fpr0", StandardTypes.FLOAT80L_T);
    public final static Register FPR1
	= new Register("fpr1", StandardTypes.FLOAT80L_T);
    public final static Register FPR2
	= new Register("fpr2", StandardTypes.FLOAT80L_T);
    public final static Register FPR3
	= new Register("fpr3", StandardTypes.FLOAT80L_T);
    public final static Register FPR4
	= new Register("fpr4", StandardTypes.FLOAT80L_T);
    public final static Register FPR5
	= new Register("fpr5", StandardTypes.FLOAT80L_T);
    public final static Register FPR6
	= new Register("fpr6", StandardTypes.FLOAT80L_T);
    public final static Register FPR7
	= new Register("fpr7", StandardTypes.FLOAT80L_T);

    // The virtual floating-point register stack - the user sees the
    // above registers as a stack.  The F*SAVE instructions also dump
    // the registers into memory in this order.

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

    // 64-bit media registers (overlayed on the floating-point
    // registers).

    public final static Register MMX0
	= new Register("mmx0", StandardTypes.FLOAT80L_T);
    public final static Register MMX1
	= new Register("mmx1", StandardTypes.FLOAT80L_T);
    public final static Register MMX2
	= new Register("mmx2", StandardTypes.FLOAT80L_T);
    public final static Register MMX3
	= new Register("mmx3", StandardTypes.FLOAT80L_T);
    public final static Register MMX4
	= new Register("mmx4", StandardTypes.FLOAT80L_T);
    public final static Register MMX5
	= new Register("mmx5", StandardTypes.FLOAT80L_T);
    public final static Register MMX6
	= new Register("mmx6", StandardTypes.FLOAT80L_T);
    public final static Register MMX7
	= new Register("mmx7", StandardTypes.FLOAT80L_T);
    
    // 128-bit media registers; the IA-32 has 8; x86-64 has 16.

    public static final Register XMM0
	= new Register("xmm0", StandardTypes.INT128L_T);
    public static final Register XMM1
	= new Register("xmm1", StandardTypes.INT128L_T);
    public static final Register XMM2
	= new Register("xmm2", StandardTypes.INT128L_T);
    public static final Register XMM3
	= new Register("xmm3", StandardTypes.INT128L_T);
    public static final Register XMM4
	= new Register("xmm4", StandardTypes.INT128L_T);
    public static final Register XMM5
	= new Register("xmm5", StandardTypes.INT128L_T);
    public static final Register XMM6
	= new Register("xmm6", StandardTypes.INT128L_T);
    public static final Register XMM7
	= new Register("xmm7", StandardTypes.INT128L_T);
    public static final Register XMM8
	= new Register("xmm8", StandardTypes.INT128L_T);
    public static final Register XMM9
	= new Register("xmm9", StandardTypes.INT128L_T);
    public static final Register XMM10
	= new Register("xmm10", StandardTypes.INT128L_T);
    public static final Register XMM11
	= new Register("xmm11", StandardTypes.INT128L_T);
    public static final Register XMM12
	= new Register("xmm12", StandardTypes.INT128L_T);
    public static final Register XMM13
	= new Register("xmm13", StandardTypes.INT128L_T);
    public static final Register XMM14
	= new Register("xmm14", StandardTypes.INT128L_T);
    public static final Register XMM15
	= new Register("xmm15", StandardTypes.INT128L_T);

    // The floating-point control registers

    // control word
    public static final Register FCW
	= new Register("fcw", StandardTypes.INT16L_T);
    // status word
    public static final Register FSW
	= new Register("fsw", StandardTypes.INT16L_T);
    // tag word
    public static final Register FTW
	= new Register("ftw", StandardTypes.INT8L_T);
    // opcode
    public static final Register FOP
	= new Register("fop", StandardTypes.INT16L_T);
    // last instruction pointer; 32- and 64- are different.
    public static final Register RIP // 64-bit
	= new Register("fip", StandardTypes.INT64L_T);
    public static final Register EIP // 32-bit
	= new Register("fip", StandardTypes.INT32L_T);
    public static final Register CS // 32-bit
	= new Register("fcs", StandardTypes.INT16L_T);
    // last data pointer; 32- and 64- are different.
    public static final Register RDP // 64-bit
	= new Register("frdp", StandardTypes.INT64L_T);
    public static final Register DP // 32-bit
	= new Register("fdp", StandardTypes.INT32L_T);
    public static final Register DS // 32-bit
	= new Register("fds", StandardTypes.INT16L_T);
    // media instruction control/status register
    public static final Register MXCSR
	= new Register("mxcsr", StandardTypes.INT32L_T);
    public static final Register MXCSR_MASK
	= new Register("mxcsr_mask", StandardTypes.INT32L_T);

    public final static RegisterGroup FLOAT32_GROUP
	= new RegisterGroup("float",
			    new Register[] {
				ST0, ST1, ST2, ST3, ST4, ST5, ST6, ST7,
				FCW, FSW, FTW, FOP,
				EIP, CS, DP, DS,
				
			    });
    public final static RegisterGroup FLOAT64_GROUP
	= new RegisterGroup("float",
			    new Register[] {
				ST0, ST1, ST2, ST3, ST4, ST5, ST6, ST7,
				FCW, FSW, FTW, FOP,
				RIP, RDP
			    });

    public final static RegisterGroup MMX
	= new RegisterGroup("mmx",
			    new Register[] {
				MMX0, MMX1, MMX2, MMX3, MMX4, MMX5, MMX6, MMX7,
				MXCSR, MXCSR_MASK
			    });

    public final static RegisterGroup VECTOR32_GROUP
	= new RegisterGroup("vector",
			    new Register[] {
				XMM0, XMM1, XMM2, XMM3,
				XMM4, XMM5, XMM6, XMM7,
			    });
    public final static RegisterGroup VECTOR64_GROUP
	= new RegisterGroup("vector",
			    new Register[] {
				XMM0, XMM1, XMM2, XMM3,
				XMM4, XMM5, XMM6, XMM7,
				XMM8, XMM9, XMM10, XMM11,
				XMM12, XMM13, XMM15, XMM15,
			    });
}
