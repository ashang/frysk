// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
 * Elf Program segment header
 * @author ajocksch
 *
 */
public class ElfPHeader {

	public static final int PTYPE_NULL = 0;
	public static final int PTYPE_LOAD = 1;
	public static final int PTYPE_DYNAMIC = 2;
	public static final int PTYPE_INTERP = 3;
	public static final int PTYPE_NOTE = 4;
	public static final int PTYPE_SHLIB = 5;
	public static final int PTYPE_PHDR = 6;
	public static final int PTYPE_TLS = 7;
	public static final int PTYPE_NUM  = 8;
	public static final int PTYPE_LOOS = 0x60000000;
	public static final int PTYPE_GNU_EH_FRAME = 0x6474e550;
	public static final int PTYPE_GNU_STACK = 0x6474e551;
	public static final int PTYPE_GNU_RELRO = 0x6474e552;
	public static final int PTYPE_LOSUNW = 0x6ffffffa;
	public static final int PTYPE_SUNWBSS = 0x6ffffffa;
	public static final int PTYPE_SUNWSTACK = 0x6ffffffb;
	public static final int PTYPE_HISUNW = 0x6fffffff;
	public static final int PTYPE_HIOS = 0x6fffffff;
	public static final int PTYPE_LOPROC = 0x70000000;
	public static final int PTYPE_HIPROC = 0x7fffffff;


	public static final int PHFLAG_NONE = 0x0;
	public static final int PHFLAG_EXECUTABLE = 0x01;
	public static final int PHFLAG_WRITABLE = 0x02;
	public static final int PHFLAG_READABLE = 0x04;
    
	public int type;
	public int flags;
	public long offset;
	public long vaddr;
	public long paddr;
	public long filesz;
	public long memsz;
	public long align;
	
	private Elf parent;
	
	protected ElfPHeader(Elf parent){
		this.parent = parent;
	}

	protected Elf getParent(){
		return parent;
	}
}
