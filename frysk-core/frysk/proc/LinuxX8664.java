// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

package frysk.proc;

import inua.eio.ByteOrder;

class LinuxX8664
    extends Isa
{
    static final int I387_OFFSET = 28 * 8;
    static final int DBG_OFFSET = 106 * 8;
    
    Register r15 = new Register (this, 0, 0*8, 8, "r15");
    Register r14 = new Register (this, 0, 1*8, 8, "r14");
    Register r13 = new Register (this, 0, 2*8, 8, "r13");
    Register r12 = new Register (this, 0, 3*8, 8, "r12");
    Register rbp = new Register (this, 0, 4*8, 8, "rbp");
    Register rbx = new Register (this, 0, 5*8, 8, "rbx");
    Register r11 = new Register (this, 0, 6*8, 8, "r11");
    Register r10 = new Register (this, 0, 7*8, 8, "r10");
    Register r9 = new Register (this, 0, 8*8, 8, "r9");
    Register r8 = new Register (this, 0, 9*8, 8, "r8");
    Register rax = new Register (this, 0, 10*8, 8, "rax");
    Register rcx = new Register (this, 0, 11*8, 8, "rcx");
    Register rdx = new Register (this, 0, 12*8, 8, "rdx");
    Register rsi = new Register (this, 0, 13*8, 8, "rsi");
    Register rdi = new Register (this, 0, 14*8, 8, "rdi");
    Register orig_rax = new Register (this, 0, 15*8, 8, "orig_rax");
    Register rip = new Register (this, 0, 16*8, 8, "rip");
    Register cs = new Register (this, 0, 17*8, 8, "cs");
    Register eflags = new Register (this, 0, 18*8, 8, "eflags");
    Register rsp = new Register (this, 0, 19*8, 8, "rsp");
    Register ss = new Register (this, 0, 20*8, 8, "ss");
    Register fs_base = new Register (this, 0, 21*8, 8, "fs_base");
    Register gs_base = new Register (this, 0, 22*8, 8, "gs_base");
    Register ds = new Register (this, 0, 23*8, 8, "ds");
    Register es = new Register (this, 0, 24*8, 8, "es");
    Register fs = new Register (this, 0, 25*8, 8, "fs");
    Register gs = new Register (this, 0, 26*8, 8, "gs");
    
    private Register[] fprs ()
    {
	// Each floating-point register has 10 significant bytes but
	// takes up 16 bytes in the user area for alignment purposes
	Register[] fprs = new Register[8];
	for (int i = 0; i < fprs.length; i++) {
	    fprs[i] = new Register (this, 0, I387_OFFSET + 4*8 + i*16, 
				    10, "st" + i);
	}
	return fprs;
    }
    Register[] st = fprs ();  // floating-point registers
    
    private Register[] dbg_regs ()
    {
	Register[] dbg_regs = new Register[8];
	for (int i = 0; i < dbg_regs.length; i++) {
	    dbg_regs[i] = new Register (this, 0, DBG_OFFSET + i*8, 
					8, "d" + i);
	}
	return dbg_regs;
    }
    Register[] dbg = dbg_regs ();  // debug registers
    
    long pc (Task task)
    {
	return rip.get (task);
    }
    
    LinuxX8664 ()
    {
	wordSize = 8;
	byteOrder = ByteOrder.LITTLE_ENDIAN;
    }

    private static Isa isa;
    static Isa isaSingleton ()
    {
	if (isa == null)
	    isa = new LinuxX8664 ();
	return isa;
    }

    private SyscallEventInfo info;
    SyscallEventInfo getSyscallEventInfo ()
    {
	if (info == null)
	    info = new SyscallEventInfo ()
		{
		    int number (Task task)
		    {
			return (int)orig_rax.get (task);
		    }
		    long returnCode (Task task)
		    {
			return rax.get (task);
		    }
		    long arg (Task task, int n)
		    {
			switch (n) {
			case 0:
			    return (long)number (task);
			case 1:
			    return rdi.get (task);
			case 2:
			    return rsi.get (task);
			case 3:
			    return rdx.get (task);
			case 4:
			    return r10.get (task);
			case 5:
			    return r8.get (task);
			case 6:
			    return r9.get (task);
			default:
			    throw new RuntimeException ("unknown syscall arg");
			}
		    }
		};
	return info;
    }
}
