// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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
package lib.elf;

/**
 * Types for Section Header {@see ElfSectionHeader}
 *
 */


public  class ElfSectionHeaderTypes {

	// Section header flags;
	public static final int SHFLAG_WRITE = (1 << 0);
	public static final int SHFLAG_ALLOC = (1 << 1); 
	public static final int SHFLAG_EXECINSTR = (1 << 2); 
	public static final int SHFLAG_MERGE  = (1 << 4); 
	public static final int SHFLAG_STRINGS = (1 << 5);
	public static final int SHFLAG_INFO_LINK = (1 << 6);  
	public static final int SHFLAG_LINK_ORDER = (1 << 7);  
	public static final int SHFLAG_OS_NONCONFORMING = (1 << 8);   
	public static final int SHFLAG_GROUP = (1 << 9);   
	public static final int SHFLAG_TLS = (1 << 10);  
	public static final int SHFLAG_MASKOS = 0x0ff00000;
	public static final int SHFLAG_MASKPROC = 0xf0000000;
	public static final int SHFLAG_ORDERED = (1 << 30);  
	public static final int SHFLAG_EXCLUDE = (1 << 31);  

	// Section type flags;
	public static final int SHTYPE_NULL = 0;
	public static final int SHTYPE_PROGBITS = 1;
	public static final int SHTYPE_SYMTAB = 2;
	public static final int SHTYPE_STRTAB = 3;
	public static final int SHTYPE_RELA = 4;
	public static final int SHTYPE_HASH = 5;
	public static final int SHTYPE_DYNAMIC = 6;
	public static final int SHTYPE_NOTE = 7;
	public static final int SHTYPE_NOBITS = 8;
	public static final int SHTYPE_REL = 9;
	public static final int SHTYPE_SHLIB = 10;
	public static final int SHTYPE_DYNSYM = 11;
	public static final int SHTYPE_INIT_ARRAY = 14;
	public static final int SHTYPE_FINI_ARRAY = 15;
	public static final int SHTYPE_PREINIT_ARRAY = 16;
	public static final int SHTYPE_GROUP = 17;
	public static final int SHTYPE_SYMTAB_SHNDX = 18;
	public static final int SHTYPE_NUM = 19;
	public static final int SHTYPE_LOOS = 0x60000000;
	public static final int SHTYPE_GNU_HASH = 0x6ffffff6;
	public static final int SHTYPE_GNU_LIBLIST = 0x6ffffff7;
	public static final int SHTYPE_CHECKSUM = 0x6ffffff8;
	public static final int SHTYPE_LOSUNW = 0x6ffffffa;
	public static final int SHTYPE_SUNW_move = 0x6ffffffa;
	public static final int SHTYPE_SUNW_COMDAT = 0x6ffffffb;
	public static final int SHTYPE_SUNW_syminfo = 0x6ffffffc;
	public static final int SHTYPE_GNU_verdef = 0x6ffffffd;
	public static final int SHTYPE_GNU_verneed = 0x6ffffffe;
	public static final int SHTYPE_GNU_versym = 0x6fffffff;
	public static final int SHTYPE_HISUNW = 0x6fffffff;
	public static final int SHTYPE_HIOS = 0x6fffffff;
	public static final int SHTYPE_LOPROC = 0x70000000;
	public static final int SHTYPE_HIPROC = 0x7fffffff;
	public static final int SHTYPE_LOUSER = 0x80000000;
	public static final int SHTYPE_HIUSER = 0x8fffffff;
}
