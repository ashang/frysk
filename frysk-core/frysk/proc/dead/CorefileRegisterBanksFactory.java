// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

package frysk.proc.dead;

import inua.eio.ByteBuffer;
import frysk.isa.ISA;
import frysk.isa.ISAMap;
import frysk.isa.banks.RegisterBanks;
import frysk.isa.banks.X86BankRegisters;
import frysk.isa.banks.PPCBankRegisters;
import frysk.isa.banks.BankArrayRegisterMap;

/**
 * The target has registers scattered across one or more register
 * banks.  Map register requests onto the corresponding bank-register.
 */

class CorefileRegisterBanksFactory {

    private static ISAMap isaToBankRegisters
	= new ISAMap("corefile register bank")
	.put(ISA.X8664, X86BankRegisters.X8664)
	.put(ISA.IA32, X86BankRegisters.IA32)
	.put(ISA.PPC64BE, PPCBankRegisters.PPC64BE)
	.put(ISA.PPC32BE, PPCBankRegisters.PPC32BE)
	;

    /**
     * FIXME: Should also handle the creation of banks here.
     */
    static RegisterBanks create(ISA isa, ByteBuffer[] banks) {
	BankArrayRegisterMap bankRegisterMap
	    = (BankArrayRegisterMap)isaToBankRegisters.get(isa);
	return new RegisterBanks(bankRegisterMap, banks);
    }
    
}
