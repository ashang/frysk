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

package frysk.bank;

import inua.eio.ByteOrder;
import frysk.isa.Register;

/**
 * Mapping between bank registers, in particular a 32-bit register's
 * projection onto an underlying 64-bit register bank.
 */

class IndirectRegisterBankArrayMap extends RegisterBankArrayMap {
    private final ByteOrder order;
    private final RegisterBankArrayMap map32;
    private final RegisterBankArrayMap map64;
    
    IndirectRegisterBankArrayMap(ByteOrder order, RegisterBankArrayMap map32,
				 RegisterBankArrayMap map64) {
	this.order = order;
	this.map32 = map32;
	this.map64 = map64;
    }

    private int offset(BankArrayRegister reg32, BankArrayRegister reg64) {
	if (order == ByteOrder.BIG_ENDIAN) {
	    // least significant bytes on RHS
	    return (reg64.getOffset() + reg64.getLength()
		    - reg32.getLength());
	} else {
	    // least significant bytes on LHS
	    return reg64.getOffset();
	}
    }

    IndirectRegisterBankArrayMap add(Register reg32, int bank, int offset,
				     int size) {
	add(new BankArrayRegister(bank, offset, size, reg32.getName()));
	return this;
    }

    private IndirectRegisterBankArrayMap add(BankArrayRegister reg32,
					     BankArrayRegister reg64) {
	return add(reg32.getRegister(),
		   reg64.getBank(), offset(reg32, reg64), reg32.getLength());
    }

    IndirectRegisterBankArrayMap add(Register reg32, Register reg64) {
	BankArrayRegister map32reg = map32.get(reg32);
	if (reg32 == null)
	    throw new RuntimeException("unknown 32-bit register: " + reg32);
	BankArrayRegister map64reg = map64.get(reg64);
	if (map64reg == null)
	    throw new RuntimeException("unknown 64-bit register: " + reg64);
	return add(map32reg, map64reg);
    }

    IndirectRegisterBankArrayMap add(Register reg32) {
	BankArrayRegister map32reg = map32.get(reg32);
	if (reg32 == null)
	    throw new RuntimeException("unknown 32-bit register: " + reg32);
	BankArrayRegister map64reg = map64.get(reg32.getName());
	if (map64reg == null)
	    throw new RuntimeException("unknown 64-bit register: " + reg32);
	return add(map32reg, map64reg);
    }

    IndirectRegisterBankArrayMap add(String map32Name, String map64Name) {
	BankArrayRegister reg32 = map32.get(map32Name);
	if (reg32 == null)
	    throw new RuntimeException("unknown register: " + map32Name);
	BankArrayRegister reg64 = map64.get(map64Name);
	if (reg64 == null)
	    throw new RuntimeException("unknown register: " + map64Name);
	return add(reg32, reg64);
    }

    IndirectRegisterBankArrayMap add(String name) {
	return add(name, name);
    }
}
