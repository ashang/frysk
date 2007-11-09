// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
// Copyright (C) 2006-2007 IBM
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

import inua.eio.ByteOrder;

/**
 * Searchable, hashable key sufficient for identifying the supported
 * Instruction Set Architectures
 *
 * Client code, rather than extending this key should implement local
 * or more global structures indexed by this key.
 */

public final class ISA {

    private final ByteOrder order;
    private final int wordSize;
    private final String family;
    private final int hash;
    private final String name;

    private ISA(ByteOrder order, int wordSize, String family) {
	this.order = order;
	this.wordSize = wordSize;
	this.family = family;
	this.hash = (family.hashCode() << 2
		     + (wordSize / 4) << 1
		     + (this.order == ByteOrder.BIG_ENDIAN ? 1 : 0));
	name = (wordSize * 8 + "-bit"
		+ " " + (order == ByteOrder.BIG_ENDIAN
			 ? "big-endian" : "little-endian")
		+ " " + family
		);
    }

    /*
     * PowerPC is a Bi-Endian archtecture, it supports little and big
     * endianness. But, usually (99.9%) it is used as a big endian,
     * in truth in memory the data is stored always in big-endian format
     */
    public static final ISA PPC32BE
	= new ISA(ByteOrder.BIG_ENDIAN, 4, "PowerPC");
    public static final ISA PPC64BE
	= new ISA(ByteOrder.BIG_ENDIAN, 8, "PowerPC");

    public static final ISA IA32
	= new ISA(ByteOrder.LITTLE_ENDIAN, 4, "IA32");
    public static final ISA X8664
	= new ISA(ByteOrder.LITTLE_ENDIAN, 8, "X86-64");

    public String toString() {
	return name;
    }

    public int hashCode() {
	return hash;
    }

    public ByteOrder order() {
	return order;
    }

    public int wordSize() {
	return wordSize;
    }

    public String getFamily() {
	return family;
    }
}
