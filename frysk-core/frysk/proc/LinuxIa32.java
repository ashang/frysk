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

class LinuxIa32
    extends Isa
{
    static final int I387_OFFSET = 18*4;
    static final int DBG_OFFSET = 63 * 4;

    Register eax = new Register (this, 0, 6*4, 4, "eax");
    Register ebx = new Register (this, 0, 0*4, 4, "ebx");
    Register ecx = new Register (this, 0, 1*4, 4, "ecx");
    Register edx = new Register (this, 0, 2*4, 4, "edx");
    Register esi = new Register (this, 0, 3*4, 4, "esi");
    Register edi = new Register (this, 0, 4*4, 4, "edi");
    Register ebp = new Register (this, 0, 5*4, 4, "ebp");
    Register cs  = new Register (this, 0, 13*4, 4, "cs");
    Register ds  = new Register (this, 0, 7*4, 4, "ds");
    Register es  = new Register (this, 0, 8*4, 4, "es");
    Register fs  = new Register (this, 0, 9*4, 4, "fs");
    Register gs  = new Register (this, 0, 10*4, 4, "gs");
    Register ss  = new Register (this, 0, 16*4, 4, "ss");
    Register orig_eax = new Register (this, 0, 11*4, 4, "orig_eax");
    Register eip = new Register (this, 0, 12*4, 4, "eip");
    Register efl = new Register (this, 0, 14*4, 4, "efl");
    Register esp = new Register (this, 0, 15*4, 4, "esp");
    
    private Register[] fprs ()
    {
	Register[] fprs = new Register[10];
	for (int i = 0; i < fprs.length; i++) {
	    fprs[i] = new Register (this, 0, I387_OFFSET + 7*4 + i*8, 
				    8, "st" + i);
	}
	return fprs;
    }
    Register[] st = fprs ();  // floating-point registers
    
    private Register[] dbg_regs ()
    {
	Register[] dbg_regs = new Register[8];
	for (int i = 0; i < dbg_regs.length; i++) {
	    dbg_regs[i] = new Register (this, 0, DBG_OFFSET + i*4, 
					4, "d" + i);
	}
	return dbg_regs;
    }
    Register[] dbg = dbg_regs ();  // debug registers
    
    public long pc (Task task)
    {
	return eip.get (task);
    }

    LinuxIa32 ()
    {
	wordSize = 4;
	byteOrder = ByteOrder.LITTLE_ENDIAN;
    }

    private static LinuxIa32 isa;
    static LinuxIa32 isaSingleton ()
    {
        if (isa == null)
            isa = new LinuxIa32 ();
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
			return (int)orig_eax.get (task);
		    }
		    long returnCode (Task task)
		    {
			return eax.get (task);
		    }
		    long arg (Task task, int n)
		    {
			switch (n) {
			case 0:
			    return (long)number (task);
			case 1:
			    return ebx.get (task);
			case 2:
			    return ecx.get (task);
			case 3:
			    return edx.get (task);
			case 4:
			    return esi.get (task);
			case 5:
			    return edi.get (task);
			case 6:
			    return eax.get (task);
			default:
			    throw new RuntimeException ("unknown syscall arg");
			}
		    }
		};
	return info;
    }
}
