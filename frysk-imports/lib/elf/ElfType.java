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
 * An ElfType is a known Elf translation type
 * @author ajocksch
 *
 */
public class ElfType {
	
	public static ElfType ELF_T_BYTE = new ElfType(0);
	public static ElfType ELF_T_ADDR = new ElfType(1);
	public static ElfType ELF_T_DYN = new ElfType(2);
	public static ElfType ELF_T_EHDR = new ElfType(3);
	public static ElfType ELF_T_HALF = new ElfType(4);
	public static ElfType ELF_T_OFF = new ElfType(5);
	public static ElfType ELF_T_PHDR = new ElfType(6);
	public static ElfType ELF_T_RELA = new ElfType(7);
	public static ElfType ELF_T_REL = new ElfType(8);
	public static ElfType ELF_T_SHDR = new ElfType(9);
	public static ElfType ELF_T_SWORD = new ElfType(10);
	public static ElfType ELF_T_SYM = new ElfType(11);
	public static ElfType ELF_T_WORD = new ElfType(12);
	public static ElfType ELF_T_XWORD = new ElfType(13);
	public static ElfType ELF_T_SXWORD = new ElfType(14);
	public static ElfType ELF_T_VDEF = new ElfType(15);
	public static ElfType ELF_T_VDAUX = new ElfType(16);
	public static ElfType ELF_T_VNEED = new ElfType(17);
	public static ElfType ELF_T_VNAUX = new ElfType(18);
	public static ElfType ELF_T_NHDR = new ElfType(19);
	public static ElfType ELF_T_SYMINFO = new ElfType(20);
	public static ElfType ELF_T_MOVE = new ElfType(21);
	public static ElfType ELF_T_LIB = new ElfType(22);
	
	private static ElfType[] types = {ELF_T_BYTE, ELF_T_ADDR, ELF_T_DYN,
		ELF_T_EHDR, ELF_T_HALF, ELF_T_OFF, ELF_T_PHDR, ELF_T_RELA, ELF_T_REL,
		ELF_T_SHDR, ELF_T_SWORD, ELF_T_SYM, ELF_T_WORD, ELF_T_XWORD, ELF_T_SXWORD,
		ELF_T_VDEF, ELF_T_VDAUX, ELF_T_VNEED, ELF_T_VNAUX, ELF_T_NHDR, 
		ELF_T_SYMINFO, ELF_T_MOVE, ELF_T_LIB
	};
	
	private int value;
	
	private ElfType(int val){
		this.value = val;
	}
	
	/**
	 * @return true iff the object is an ElfType and equal to this object
	 */
	public boolean equals(Object obj){
		if(!(obj instanceof ElfType))
			return false;
		
		return ((ElfType)obj).value == this.value;
	}
	
	protected int getValue(){
		return this.value;
	}
	
	protected static ElfType intern(int type){
		return types[type];
	}
}
