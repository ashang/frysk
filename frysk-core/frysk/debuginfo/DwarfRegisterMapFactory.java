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

package frysk.debuginfo;

import frysk.proc.Isa;
import frysk.proc.IsaIA32;
import frysk.proc.IsaX8664;
import frysk.stack.IA32Registers;
import frysk.stack.RegisterMap;
import frysk.stack.X8664Registers;

public class DwarfRegisterMapFactory {

    public static RegisterMap getRegisterMap(Isa isa) {
	if (isa instanceof IsaIA32)
	    return new DwarfRegisterIA32();
	else if (isa instanceof IsaX8664)
	    return new DwarfRegisterX8664();
	else
	    throw new RuntimeException("Isa not supported");
    }

}

class DwarfRegisterIA32 extends RegisterMap {
    
    DwarfRegisterIA32 () {
	addEntry(IA32Registers.EAX, new Integer(0));
	addEntry(IA32Registers.ECX, new Integer(1));
	addEntry(IA32Registers.EDX, new Integer(2));
	addEntry(IA32Registers.EBX, new Integer(3));
	addEntry(IA32Registers.ESP, new Integer(4));
	addEntry(IA32Registers.EBP, new Integer(5));
	addEntry(IA32Registers.ESI, new Integer(6));
	addEntry(IA32Registers.EDI, new Integer(7));

    }
}

class DwarfRegisterX8664 extends RegisterMap {
    DwarfRegisterX8664() {
	addEntry(X8664Registers.RAX, new Integer(0));
	addEntry(X8664Registers.RDX, new Integer(1));
	addEntry(X8664Registers.RCX, new Integer(2));
	addEntry(X8664Registers.RBX, new Integer(3));
	addEntry(X8664Registers.RSI, new Integer(4));
	addEntry(X8664Registers.RDI, new Integer(5));
	addEntry(X8664Registers.RBP, new Integer(6));
	addEntry(X8664Registers.RSP, new Integer(7));
	addEntry(X8664Registers.R8, new Integer(8));
	addEntry(X8664Registers.R9, new Integer(9));
	addEntry(X8664Registers.R10, new Integer(10));
	addEntry(X8664Registers.R11, new Integer(11));
	addEntry(X8664Registers.R12, new Integer(12));
	addEntry(X8664Registers.R13, new Integer(13));
	addEntry(X8664Registers.R14, new Integer(14));
	addEntry(X8664Registers.R15, new Integer(15));
	addEntry(X8664Registers.RIP, new Integer(16));
    }
}