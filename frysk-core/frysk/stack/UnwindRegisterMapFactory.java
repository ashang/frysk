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

import frysk.proc.Isa;
import frysk.proc.IsaIA32;
import frysk.proc.IsaX8664;

public class UnwindRegisterMapFactory {

    public static UnwindRegisterMap getRegisterMap(Isa isa) {
	if (isa instanceof IsaIA32)
	    return new IA32Map();
	else if (isa instanceof IsaX8664)
	    return new X8664Map();
	else
	    throw new RuntimeException("Isa not supported");
    }
}

class IA32Map extends UnwindRegisterMap {

    IA32Map() {

	numbers.put(IA32Registers.EAX, new Integer(0));
	numbers.put(IA32Registers.EDX, new Integer(1));
	numbers.put(IA32Registers.ECX, new Integer(2));
	numbers.put(IA32Registers.EBX, new Integer(3));
	numbers.put(IA32Registers.ESI, new Integer(4));
	numbers.put(IA32Registers.EDI, new Integer(5));
	numbers.put(IA32Registers.EBP, new Integer(6));
	numbers.put(IA32Registers.ESP, new Integer(7));
	numbers.put(IA32Registers.EIP, new Integer(8));
	numbers.put(IA32Registers.EFLAGS, new Integer(9));
	numbers.put(IA32Registers.TRAPS, new Integer(10));
	// MMX registers
	numbers.put(IA32Registers.ST0, new Integer(11));
	numbers.put(IA32Registers.ST1, new Integer(12));
	numbers.put(IA32Registers.ST2, new Integer(13));
	numbers.put(IA32Registers.ST3, new Integer(14));
	numbers.put(IA32Registers.ST4, new Integer(15));
	numbers.put(IA32Registers.ST5, new Integer(16));
	numbers.put(IA32Registers.ST6, new Integer(17));
	numbers.put(IA32Registers.ST7, new Integer(18));
	numbers.put(IA32Registers.FCW, new Integer(19));
	numbers.put(IA32Registers.FSW, new Integer(20));
	numbers.put(IA32Registers.FTW, new Integer(21));
	numbers.put(IA32Registers.FOP, new Integer(22));
	numbers.put(IA32Registers.FCS, new Integer(23));
	numbers.put(IA32Registers.FIP, new Integer(24));
	numbers.put(IA32Registers.FEA, new Integer(25));
	numbers.put(IA32Registers.FDS, new Integer(26));
	// SSE Registers
	//TODO: XMMx registers.
	numbers.put(IA32Registers.MXCSR, new Integer(43));
	// Segment registers
	numbers.put(IA32Registers.GS, new Integer(44));
	numbers.put(IA32Registers.FS, new Integer(45));
	numbers.put(IA32Registers.ES, new Integer(46));
	numbers.put(IA32Registers.DS, new Integer(47));
	numbers.put(IA32Registers.SS, new Integer(48));
	numbers.put(IA32Registers.CS, new Integer(49));
	numbers.put(IA32Registers.TSS, new Integer(50));
	numbers.put(IA32Registers.LDT, new Integer(51));
	// frame info
	numbers.put(IA32Registers.CFA, new Integer(52));
	
    }
}

class X8664Map extends UnwindRegisterMap {
    X8664Map() {
	numbers.put(X8664Registers.RAX, new Integer(0));
	numbers.put(X8664Registers.RDX, new Integer(1));
	numbers.put(X8664Registers.RCX, new Integer(2));
	numbers.put(X8664Registers.RBX, new Integer(3));
	numbers.put(X8664Registers.RSI, new Integer(4));
	numbers.put(X8664Registers.RDI, new Integer(5));
	numbers.put(X8664Registers.RBP, new Integer(6));
	numbers.put(X8664Registers.RSP, new Integer(7));
	numbers.put(X8664Registers.R8, new Integer(8));
	numbers.put(X8664Registers.R9, new Integer(9));
	numbers.put(X8664Registers.R10, new Integer(10));
	numbers.put(X8664Registers.R11, new Integer(11));
	numbers.put(X8664Registers.R12, new Integer(12));
	numbers.put(X8664Registers.R13, new Integer(13));
	numbers.put(X8664Registers.R14, new Integer(14));
	numbers.put(X8664Registers.R15, new Integer(15));
	numbers.put(X8664Registers.RIP, new Integer(16));
    }
}
