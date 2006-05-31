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


import java.util.Date;

/**
 * An ElfArchiveHeader is a header for an archive member
 * @author ajocksch
 *
 */
public class ElfArchiveHeader {

	private long pointer;
	private Elf parent;
	
	protected ElfArchiveHeader(long ptr, Elf parent){
		this.pointer = ptr;
		this.parent = parent;
	}
	
	/**
	 * 
	 * @return The name of the archive member
	 */
	public String getName(){
		return elf_ar_get_name();
	}
	
	/**
	 * 
	 * @return The file date
	 */
	public Date getDate(){
		return new Date(elf_ar_get_date());
	}
	
	/**
	 * 
	 * @return The user id of the file
	 */
	public int getUid(){
		return elf_ar_get_uid();
	}
	
	/**
	 * 
	 * @return The group id of the file 
	 */
	public int getGid(){
		return elf_ar_get_gid();
	}
	
	/**
	 * 
	 * @return The file mode
	 */
	public int getMode(){
		return elf_ar_get_mode();
	}
	
	/**
	 * 
	 * @return The file size
	 */
	public int getSize(){
		return elf_ar_get_size();
	}
	
	/**
	 * 
	 * @return The original name of the archive member
	 */
	public String getRawName(){
		return elf_ar_get_raw_name();
	}
	
	protected long getPointer(){
		return this.pointer;
	}

	protected Elf getParent(){
		return this.parent;
	}
	
	protected native String elf_ar_get_name();
	protected native long elf_ar_get_date();
	protected native int elf_ar_get_uid();
	protected native int elf_ar_get_gid();
	protected native int elf_ar_get_mode();
	protected native int elf_ar_get_size();
	protected native String elf_ar_get_raw_name();
}
