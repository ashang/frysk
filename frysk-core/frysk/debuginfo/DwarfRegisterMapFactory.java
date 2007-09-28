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

import lib.dwfl.DwarfRegistersX86;
import lib.dwfl.DwarfRegistersX8664;
import frysk.proc.Isa;
import frysk.proc.IsaIA32;
import frysk.proc.IsaX8664;
import frysk.isa.IA32Registers;
import frysk.isa.RegisterMap;
import frysk.isa.X8664Registers;

public class DwarfRegisterMapFactory {

    public static RegisterMap getRegisterMap(Isa isa) {
	if (isa instanceof IsaIA32)
	    return IA32;
	else if (isa instanceof IsaX8664)
	    return X8664;
	else
	    throw new RuntimeException("Isa not supported");
    }

    static final RegisterMap IA32 = new RegisterMap()
	.add(IA32Registers.EAX, DwarfRegistersX86.EAX)
	.add(IA32Registers.ECX, DwarfRegistersX86.ECX)
	.add(IA32Registers.EDX, DwarfRegistersX86.EDX)
	.add(IA32Registers.EBX, DwarfRegistersX86.EBX)
	.add(IA32Registers.ESP, DwarfRegistersX86.ESP)
	.add(IA32Registers.EBP, DwarfRegistersX86.EBP)
	.add(IA32Registers.ESI, DwarfRegistersX86.ESI)
	.add(IA32Registers.EDI, DwarfRegistersX86.EDI)
	;

    static final RegisterMap X8664 = new RegisterMap()
	.add(X8664Registers.RAX, DwarfRegistersX8664.RAX)
	.add(X8664Registers.RDX, DwarfRegistersX8664.RDX)
	.add(X8664Registers.RCX, DwarfRegistersX8664.RCX)
	.add(X8664Registers.RBX, DwarfRegistersX8664.RBX)
	.add(X8664Registers.RSI, DwarfRegistersX8664.RSI)
	.add(X8664Registers.RDI, DwarfRegistersX8664.RDI)
	.add(X8664Registers.RBP, DwarfRegistersX8664.RBP)
	.add(X8664Registers.RSP, DwarfRegistersX8664.RSP)
	.add(X8664Registers.R8, DwarfRegistersX8664.R8)
	.add(X8664Registers.R9, DwarfRegistersX8664.R9)
	.add(X8664Registers.R10, DwarfRegistersX8664.R10)
	.add(X8664Registers.R11, DwarfRegistersX8664.R11)
	.add(X8664Registers.R12, DwarfRegistersX8664.R12)
	.add(X8664Registers.R13, DwarfRegistersX8664.R13)
	.add(X8664Registers.R14, DwarfRegistersX8664.R14)
	.add(X8664Registers.R15, DwarfRegistersX8664.R15)
	.add(X8664Registers.RIP, DwarfRegistersX8664.RIP)
	;
}
