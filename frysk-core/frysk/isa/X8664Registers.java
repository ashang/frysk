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

public class X8664Registers extends Registers {

    // General purpose registers.

    public static final Register RAX
	= new Register("rax", StandardTypes.INT64L_T);
    public static final Register RDX
	= new Register("rdx", StandardTypes.INT64L_T);
    public static final Register RCX
	= new Register("rcx", StandardTypes.INT64L_T);
    public static final Register RBX
	= new Register("rbx", StandardTypes.INT64L_T);
    public static final Register RSI
	= new Register("rsi", StandardTypes.INT64L_T);
    public static final Register RDI
	= new Register("rdi", StandardTypes.INT64L_T);
    public static final Register RBP
	= new Register("rbp", StandardTypes.VOIDPTR64L_T);
    public static final Register RSP
	= new Register("rsp", StandardTypes.VOIDPTR64L_T);
    public static final Register R8
	= new Register("r8", StandardTypes.INT64L_T);
    public static final Register R9
	= new Register("r9", StandardTypes.INT64L_T);
    public static final Register R10
	= new Register("r10", StandardTypes.INT64L_T);
    public static final Register R11
	= new Register("r11", StandardTypes.INT64L_T);
    public static final Register R12
	= new Register("r12", StandardTypes.INT64L_T);
    public static final Register R13
	= new Register("r13", StandardTypes.INT64L_T);
    public static final Register R14
	= new Register("r14", StandardTypes.INT64L_T);
    public static final Register R15
	= new Register("r15", StandardTypes.INT64L_T);

    // Flags and instruction pointer

    public static final Register RFLAGS
	= new Register("rflags", StandardTypes.INT64L_T);
    public static final Register RIP
	= new Register("rip", StandardTypes.VOIDPTR64L_T);

    // Segment registers
    
    public static final Register CS
	= new Register("cs", StandardTypes.INT16L_T);
    public static final Register DS
	= new Register("ds", StandardTypes.INT16L_T);
    public static final Register ES
	= new Register("es", StandardTypes.INT16L_T);
    public static final Register FS
	= new Register("fs", StandardTypes.INT16L_T);
    public static final Register GS
	= new Register("gs", StandardTypes.INT16L_T);
    public static final Register SS
	= new Register("ss", StandardTypes.INT16L_T);

    public static final Register FS_BASE
	= new Register("fs_base", StandardTypes.INT64L_T);
    public static final Register GS_BASE
	= new Register("gs_base", StandardTypes.INT64L_T);

    public static final Register DR0
	= new Register("dr0", StandardTypes.INT64L_T);
    public static final Register DR1
	= new Register("dr1", StandardTypes.INT64L_T);
    public static final Register DR2
	= new Register("dr2", StandardTypes.INT64L_T);
    public static final Register DR3
	= new Register("dr3", StandardTypes.INT64L_T);
    public static final Register DR4
	= new Register("dr4", StandardTypes.INT64L_T);
    public static final Register DR5
	= new Register("dr5", StandardTypes.INT64L_T);
    public static final Register DR6
	= new Register("dr6", StandardTypes.INT64L_T);
    public static final Register DR7
	= new Register("dr7", StandardTypes.INT64L_T);

    // Magic; on Linux contains EAX (or syscall number) at the start
    // of a system call.
    public static final Register ORIG_RAX
	= new Register("orig_rax", StandardTypes.INT64L_T);

    public static final RegisterGroup GENERAL = new RegisterGroup("general",
								  new Register[] { RAX, RDX, RCX, RBX, RSI, RDI, RBP, RSP, R8, R9,
										   R10, R11, R12, R13, R14, R15, RIP });

    public static final RegisterGroup ALL;
    static {
	Register[] allRegs = new Register[GENERAL.getRegisters().length];
	System.arraycopy(GENERAL.getRegisters(), 0, allRegs, 0,
			 GENERAL.getRegisters().length);
	ALL = new RegisterGroup("all", allRegs);
    }

    protected X8664Registers() {
	super(new RegisterGroup[] { GENERAL, ALL });
    }

    public Register getProgramCounter() {
	return RIP;
    }

    public Register getStackPointer() {
	return RSP;
    }

    public RegisterGroup getDefaultRegisterGroup() {
	return GENERAL;
    }

    public RegisterGroup getAllRegistersGroup() {
	return ALL;
    }
}
