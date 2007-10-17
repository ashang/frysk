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
    // above registers as a stack.

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

    // The "abstract" multi-media registers - again the user sees part
    // of the raw registers.

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
    
    // The floating-point control registers.

    public final static Register FCTRL // control word/register
	= new Register("fctrl", StandardTypes.INT16L_T);
    public final static Register FSTAT // status word/register
	= new Register("fstat", StandardTypes.INT16L_T);
    public final static Register FTAG // tag word/register
	= new Register("ftag", StandardTypes.INT16L_T);

    public final static Register FLIP // [last] instruction pointer
	= new Register("flip", StandardTypes.INT64L_T);
    public final static Register FLDP // [last] data (operand) pointer
	= new Register("fldp", StandardTypes.INT64L_T);

    public final static Register FOP // opcode
	= new Register("fop", StandardTypes.INT32L_T);


    public final static RegisterGroup FLOAT
	= new RegisterGroup("float",
			    new Register[] {
				ST0, ST1, ST2, ST3, ST4, ST5, ST6, ST7,
				FCTRL, FSTAT, FTAG, FLIP, FLDP, FOP
			    });

    public final static RegisterGroup MMX
	= new RegisterGroup("mmx",
			    new Register[] {
				MMX0, MMX1, MMX2, MMX3, MMX4, MMX5, MMX6, MMX7
			    });

}
