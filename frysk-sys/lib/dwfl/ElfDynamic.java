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
package lib.dwfl;

/**
 * ElfDynamic is a container class with functions related to reading
 * and interpretation of DYNAMIC section of ELF files.
 */
public class ElfDynamic {

    public static interface Builder {
	/** This is called for all types of dynamic entries, and value
	 * may be either d_val or d_ptr. */
	void entry(int tag, long value);
    }

    // Values for the `tag' argument of Builder.entry.  Copied off
    // from elf.h.
    public static int ELF_DT_NULL = 0;
    public static int ELF_DT_NEEDED = 1;
    public static int ELF_DT_PLTRELSZ = 2;
    public static int ELF_DT_PLTGOT = 3;
    public static int ELF_DT_HASH = 4;
    public static int ELF_DT_STRTAB = 5;
    public static int ELF_DT_SYMTAB = 6;
    public static int ELF_DT_RELA = 7;
    public static int ELF_DT_RELASZ = 8;
    public static int ELF_DT_RELAENT = 9;
    public static int ELF_DT_STRSZ = 10;
    public static int ELF_DT_SYMENT = 11;
    public static int ELF_DT_INIT = 12;
    public static int ELF_DT_FINI = 13;
    public static int ELF_DT_SONAME = 14;
    public static int ELF_DT_RPATH = 15;
    public static int ELF_DT_SYMBOLIC = 16;
    public static int ELF_DT_REL = 17;
    public static int ELF_DT_RELSZ = 18;
    public static int ELF_DT_RELENT = 19;
    public static int ELF_DT_PLTREL = 20;
    public static int ELF_DT_DEBUG = 21;
    public static int ELF_DT_TEXTREL = 22;
    public static int ELF_DT_JMPREL = 23;
    public static int ELF_DT_BIND_NOW = 24;
    public static int ELF_DT_INIT_ARRAY = 25;
    public static int ELF_DT_FINI_ARRAY = 26;
    public static int ELF_DT_INIT_ARRAYSZ = 27;
    public static int ELF_DT_FINI_ARRAYSZ = 28;
    public static int ELF_DT_RUNPATH = 29;
    public static int ELF_DT_FLAGS = 30;
    public static int ELF_DT_ENCODING = 32;
    public static int ELF_DT_PREINIT_ARRAY = 32;
    public static int ELF_DT_PREINIT_ARRAYSZ = 33;
    public static int ELF_DT_NUM = 34;
    public static int ELF_DT_LOOS = 0x6000000d;
    public static int ELF_DT_HIOS = 0x6ffff000;
    public static int ELF_DT_VALRNGLO = 0x6ffffd00;
    public static int ELF_DT_GNU_PRELINKED = 0x6ffffdf5;
    public static int ELF_DT_GNU_CONFLICTSZ = 0x6ffffdf6;
    public static int ELF_DT_GNU_LIBLISTSZ = 0x6ffffdf7;
    public static int ELF_DT_CHECKSUM = 0x6ffffdf8;
    public static int ELF_DT_PLTPADSZ = 0x6ffffdf9;
    public static int ELF_DT_MOVEENT = 0x6ffffdfa;
    public static int ELF_DT_MOVESZ = 0x6ffffdfb;
    public static int ELF_DT_FEATURE_1 = 0x6ffffdfc;
    public static int ELF_DT_POSFLAG_1 = 0x6ffffdfd;
    public static int ELF_DT_SYMINSZ = 0x6ffffdfe;
    public static int ELF_DT_SYMINENT = 0x6ffffdff;
    public static int ELF_DT_VALRNGHI = 0x6ffffdff;
    public static int ELF_DT_ADDRRNGLO = 0x6ffffe00;
    public static int ELF_DT_GNU_HASH = 0x6ffffef5;
    public static int ELF_DT_TLSDESC_PLT = 0x6ffffef6;
    public static int ELF_DT_TLSDESC_GOT = 0x6ffffef7;
    public static int ELF_DT_GNU_CONFLICT = 0x6ffffef8;
    public static int ELF_DT_GNU_LIBLIST = 0x6ffffef9;
    public static int ELF_DT_CONFIG = 0x6ffffefa;
    public static int ELF_DT_DEPAUDIT = 0x6ffffefb;
    public static int ELF_DT_AUDIT = 0x6ffffefc;
    public static int ELF_DT_PLTPAD = 0x6ffffefd;
    public static int ELF_DT_MOVETAB = 0x6ffffefe;
    public static int ELF_DT_SYMINFO = 0x6ffffeff;
    public static int ELF_DT_ADDRRNGHI = 0x6ffffeff;
    public static int ELF_DT_VERSYM = 0x6ffffff0;
    public static int ELF_DT_RELACOUNT = 0x6ffffff9;
    public static int ELF_DT_RELCOUNT = 0x6ffffffa;
    public static int ELF_DT_FLAGS_1 = 0x6ffffffb;
    public static int ELF_DT_VERDEF = 0x6ffffffc;
    public static int ELF_DT_VERDEFNUM = 0x6ffffffd;
    public static int ELF_DT_VERNEED = 0x6ffffffe;
    public static int ELF_DT_VERNEEDNUM = 0x6fffffff;
    public static int ELF_DT_LOPROC = 0x70000000;
    public static int ELF_DT_AUXILIARY = 0x7ffffffd;
    public static int ELF_DT_FILTER = 0x7fffffff;
    public static int ELF_DT_HIPROC = 0x7fffffff;

   
    /**
     * Calls {@see Builder.symbol} with each symbol in given section.
     * Only makes sense for DYNAMIC section.
     */
    public static void loadFrom(ElfSection section, ElfDynamic.Builder builder)
	    throws ElfException
    {
	ElfSectionHeader header = section.getSectionHeader();
	if (header.type != ElfSectionHeader.ELF_SHT_DYNAMIC)
	    throw new ElfException("Section " + header.name + " doesn't contain DYNAMIC table.");

	Elf parent = section.getParent();
	long data_pointer = section.getData().getPointer();
	long count = header.size / header.entsize;
	for (long i = 0; i < count; ++i)
	    if (!elf_buildentry(parent, data_pointer, i, builder))
		throw new ElfException("Dynamic entry #" + i + " couldn't be retrieved.");
    }

    protected static native boolean elf_buildentry(Elf parent, long data_pointer,
						   long entry_index, ElfDynamic.Builder builder);
}
