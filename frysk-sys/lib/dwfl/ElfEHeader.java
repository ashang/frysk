// This file is part of the program FRYSK.
//
// Copyright 2005, 2008, Red Hat Inc.
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

package lib.dwfl;

import inua.eio.ByteOrder;

/**
 * An ElfEHeader is a header for and Elf file. This appears at the
 * start of every Elf file.
 */
public final class ElfEHeader {

    public ElfEHeader(){
    }

	// Version
	//public static final int PHEADER_EV_VERSION = parent.getElfVersion();

    // ident[CLASS} contains the word-size
    public static final int CLASSNONE = 0;
    public static final int CLASS32 = 1;
    public static final int CLASS64 = 2;
    public final static int CLASS = 4;

    // Data class
    public static final int DATANONE = 0;
    public static final int DATA2LSB = 1;
    public static final int DATA2MSB = 2;
    public static final int DATA = 5;
    
	// Type
	public static final int PHEADER_ET_NONE = 0;
	public static final int PHEADER_ET_REL = 1;
	public static final int PHEADER_ET_EXEC = 2;
	public static final int PHEADER_ET_DYN = 3;
	public static final int PHEADER_ET_CORE = 4;
	public static final int PHEADER_ET_NUM = 5;
	public static final int PHEADER_ET_LOOS = 0xfe00;
	public static final int PHEADER_ET_HIOS = 0xfeff;
	public static final int PHEADER_ET_LOPROC =  0xff00;
	public static final int PHEADER_ET_HIPROC = 0xffff;

    public static final int NIDENT = 16;
    public byte[] ident = new byte[NIDENT];

	public int type;
	public int machine;
	public long version;
	public long entry;
	public long phoff;
	public long shoff;
	public int flags;
	public int ehsize;
	public int phentsize;
	public int phnum;
	public int shentsize;
	public int shnum;
        public int shstrndx;
	
    /**
     * Interpret the IDENT field; extracting the word-size.
     */
    public int getWordSize() {
        switch (ident[CLASS]) {
	case CLASSNONE:
	    return 0;
	case CLASS32:
	    return 4;
	case CLASS64:
	    return 8;
	default:
	    // This is a programmer error; not a an Elf file or format error.
	    throw new RuntimeException("Unknown ELF class " + ident[CLASS]);
	}
    }

    /**
     * Encode SIZE into the IDENT field.
     */
    public ElfEHeader setWordSize(int size) {
	switch (size) {
	case 0:
	    ident[CLASS] = CLASSNONE;
	    return this;
	case 4:
	    ident[CLASS] = CLASS32;
	    return this;
	case 8:
	    ident[CLASS] = CLASS64;
	    return this;
	default:
	    // This is a programmer error; not an Elf file format error.
	    throw new RuntimeException("No class for word-size " + size);
	}
    }
	
    /**
     * Interpret the IDENT field; extracting the byte order.
     */
    public ByteOrder getByteOrder() {
        switch (ident[DATA]) {
	case DATANONE:
	    return null;
	case DATA2LSB:
	    return ByteOrder.LITTLE_ENDIAN;
	case DATA2MSB:
	    return ByteOrder.BIG_ENDIAN;
	default:
	    // This is a programmer error; not a an Elf file or format error.
	    throw new RuntimeException("Unknown ELF class " + ident[DATA]);
	}
    }

    /**
     * Encode SIZE into the IDENT field.
     */
    public ElfEHeader setByteOrder(ByteOrder order) {
	if (order == null)
	    ident[DATA] = DATANONE;
	else if (order == ByteOrder.LITTLE_ENDIAN)
	    ident[DATA] = DATA2LSB;
	else if (order == ByteOrder.BIG_ENDIAN)
	    ident[DATA] = DATA2MSB;
	else
	    // This is a programmer error; not an Elf file format error.
	    throw new RuntimeException("No data for byte-order " + order);
	return this;
    }
}
