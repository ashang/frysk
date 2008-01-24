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

package frysk.isa.registers;

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
	= new Register("esp", StandardTypes.VOIDPTR32L_T);

    // Segment registers

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

    public final static Register TSS
	= new Register("tss", StandardTypes.INT32L_T);
    public final static Register LDT
	= new Register("ldt", StandardTypes.INT32L_T);
    public final static Register TRAPS
	= new Register("traps", StandardTypes.INT32L_T);

    public static final Register D0
	= new Register("d0", StandardTypes.INT32L_T);
    public static final Register D1
	= new Register("d1", StandardTypes.INT32L_T);
    public static final Register D2
	= new Register("d2", StandardTypes.INT32L_T);
    public static final Register D3
	= new Register("d3", StandardTypes.INT32L_T);
    public static final Register D4
	= new Register("d4", StandardTypes.INT32L_T);
    public static final Register D5
	= new Register("d5", StandardTypes.INT32L_T);
    public static final Register D6
	= new Register("d6", StandardTypes.INT32L_T);
    public static final Register D7
	= new Register("d7", StandardTypes.INT32L_T);

    // Magic; on Linux contains EAX (or syscall number) at the start
    // of a system call.
    public static final Register ORIG_EAX
	= new Register("orig_eax", StandardTypes.INT32L_T);

    public final static RegisterGroup REGS_GROUP
	= new RegisterGroup("regs",
			    new Register[] {
				EAX, EBX, ECX, EDX, ESI, EDI, EBP, EIP,
				EFLAGS, ESP
			    });

    public final static RegisterGroup SEGMENT_GROUP
	= new RegisterGroup("segment",
			    new Register[] {
				GS, FS, ES, DS, SS, CS
			    });


    IA32Registers() {
	super (new RegisterGroup[] {
		   REGS_GROUP,
		   X87Registers.FLOAT32_GROUP,
		   X87Registers.VECTOR32_GROUP,
		   SEGMENT_GROUP,
	       });
    }

    public Register getProgramCounter() {
	return EIP;
    }

    public Register getStackPointer() {
	return ESP;
    }

    public RegisterGroup getDefaultRegisterGroup() {
	return REGS_GROUP;
    }

    public RegisterGroup getFloatRegisterGroup() {
	return X87Registers.FLOAT32_GROUP;
    }

    public RegisterGroup getVectorRegisterGroup() {
	return X87Registers.VECTOR32_GROUP;
    }
}
