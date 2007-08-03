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
 * ElfSymbol is a container class with symbol-table related functions
 * and constants.
 */
public class ElfSymbol {
    // Constants defined in this class were copied from elf.h.

    public static interface Builder
    {
      void symbol (String name, long value, long size, int type, int bind,
		   int visibility, long shndx);
    }

    // Legal values for `bind' argument of SymbolBuiler.symbol.
    public static int ELF_STB_LOCAL = 0;
    public static int ELF_STB_GLOBAL = 1;
    public static int ELF_STB_WEAK = 2;
    public static int ELF_STB_NUM = 3;
    public static int ELF_STB_LOOS = 10;
    public static int ELF_STB_HIOS = 12;
    public static int ELF_STB_LOPROC = 13;
    public static int ELF_STB_HIPROC = 15;

    // Legal values for `type' argument of SymbolBuiler.symbol.
    public static int ELF_STT_NOTYPE = 0;
    public static int ELF_STT_OBJECT = 1;
    public static int ELF_STT_FUNC = 2;
    public static int ELF_STT_SECTION = 3;
    public static int ELF_STT_FILE = 4;
    public static int ELF_STT_COMMON = 5;
    public static int ELF_STT_TLS = 6;
    public static int ELF_STT_NUM = 7;
    public static int ELF_STT_LOOS = 10;
    public static int ELF_STT_HIOS = 12;
    public static int ELF_STT_LOPROC = 13;
    public static int ELF_STT_HIPROC = 15;

    // Legal values for `visibility' argument of SymbolBuiler.symbol.
    public static int ELF_STV_DEFAULT = 0;
    public static int ELF_STV_INTERNAL = 1;
    public static int ELF_STV_HIDDEN = 2;
    public static int ELF_STV_PROTECTED = 3;

    /**
     * Calls {@see Builder.symbol} with each symbol in given
     * section.  Only makes sense for .symtab or .dynsym sections.
     */
    public static void loadFrom(ElfSection section, ElfSymbol.Builder builder)
	    throws ElfException
    {
	ElfSectionHeader header = section.getSectionHeader();
	if (!(header.type == ElfSectionHeader.ELF_SHT_SYMTAB
	      || header.type == ElfSectionHeader.ELF_SHT_DYNSYM))
	    throw new ElfException("Section " + header.name + " doesn't contain symbol table.");

	Elf parent = section.getParent();
	long data_pointer = section.getData().getPointer();
	long count = header.size / header.entsize;
	// Note: ignoring special symbol entry on index 0.
	for (long i = 1; i < count; ++i)
	    if (!elf_buildsymbol(parent, data_pointer, i, header.link, builder))
		throw new ElfException("Symbol on index " + i + " couldn't be retrieved.");
    }

    protected static native boolean elf_buildsymbol(Elf parent, long data_pointer,
						    long symbol_index, long str_sect_index,
						    ElfSymbol.Builder builder);
}
