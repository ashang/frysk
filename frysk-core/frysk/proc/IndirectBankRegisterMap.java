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

package frysk.proc;

import inua.eio.ByteOrder;
import frysk.isa.Register;

/**
 * Mapping between bank registers, in particular a 32-bit register's
 * projection onto an underlying 64-bit register bank.
 */

class IndirectBankRegisterMap extends BankRegisterMap {
    private final ByteOrder order;
    private final BankRegisterMap map32;
    private final BankRegisterMap map64;
    
    IndirectBankRegisterMap(ByteOrder order, BankRegisterMap map32,
			    BankRegisterMap map64) {
	this.order = order;
	this.map32 = map32;
	this.map64 = map64;
    }

    private int offset(BankRegister reg32, BankRegister reg64) {
	if (order == ByteOrder.BIG_ENDIAN) {
	    // least significant bytes on RHS
	    return (reg64.getOffset() + reg64.getLength()
		    - reg32.getLength());
	} else {
	    // least significant bytes on LHS
	    return reg64.getOffset();
	}
    }

    IndirectBankRegisterMap add(Register reg32, int bank, int offset,
				int size) {
	add(new BankRegister(bank, offset, size, reg32.getName()));
	return this;
    }

    private IndirectBankRegisterMap add(BankRegister reg32,
					BankRegister reg64) {
	return add(reg32.getRegister(),
		   reg64.getBank(), offset(reg32, reg64), reg32.getLength());
    }

    IndirectBankRegisterMap add(Register reg32, Register reg64) {
	BankRegister map32reg = map32.get(reg32);
	if (reg32 == null)
	    throw new RuntimeException("unknown 32-bit register: " + reg32);
	BankRegister map64reg = map64.get(reg64);
	if (map64reg == null)
	    throw new RuntimeException("unknown 64-bit register: " + reg64);
	return add(map32reg, map64reg);
    }

    IndirectBankRegisterMap add(Register reg32) {
	BankRegister map32reg = map32.get(reg32);
	if (reg32 == null)
	    throw new RuntimeException("unknown 32-bit register: " + reg32);
	BankRegister map64reg = map64.get(reg32.getName());
	if (map64reg == null)
	    throw new RuntimeException("unknown 64-bit register: " + reg32);
	return add(map32reg, map64reg);
    }

    IndirectBankRegisterMap add(String map32Name, String map64Name) {
	BankRegister reg32 = map32.get(map32Name);
	if (reg32 == null)
	    throw new RuntimeException("unknown register: " + map32Name);
	BankRegister reg64 = map64.get(map64Name);
	if (reg64 == null)
	    throw new RuntimeException("unknown register: " + map64Name);
	return add(reg32, reg64);
    }

    IndirectBankRegisterMap add(String name) {
	return add(name, name);
    }
}
