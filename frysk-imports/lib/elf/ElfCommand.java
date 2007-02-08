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
 * And ElfCommand is a Command 
 *
 * XXX: Something more desctiptive please?!?!?
 */
public class ElfCommand 
{

  public static final ElfCommand ELF_C_NULL = new ElfCommand(0);
  public static final ElfCommand ELF_C_READ = new ElfCommand(1);
  public static final ElfCommand ELF_C_RDWR = new ElfCommand(2);
  public static final ElfCommand ELF_C_WRITE = new ElfCommand(3);
  public static final ElfCommand ELF_C_CLR = new ElfCommand(4);
  public static final ElfCommand ELF_C_SET = new ElfCommand(5);
  public static final ElfCommand ELF_C_FDDONE = new ElfCommand(6);
  public static final ElfCommand ELF_C_FDREAD = new ElfCommand(7);
  public static final ElfCommand ELF_C_READ_MMAP = new ElfCommand(8);
  public static final ElfCommand ELF_C_RDWR_MMAP = new ElfCommand(9);
  public static final ElfCommand ELF_C_READ_MMAP_PRIVATE = new ElfCommand(10);
  public static final ElfCommand ELF_C_EMPTY = new ElfCommand(11);
	
  private static ElfCommand[] commands = {ELF_C_NULL, 
					  ELF_C_READ, 
					  ELF_C_RDWR,
					  ELF_C_WRITE, 
					  ELF_C_CLR,
					  ELF_C_SET,   
					  ELF_C_FDDONE, 
					  ELF_C_FDREAD,
					  ELF_C_READ_MMAP,
					  ELF_C_RDWR_MMAP, 
					  ELF_C_READ_MMAP_PRIVATE, 
					  ELF_C_EMPTY
  };
	
  private int value;
	
  private ElfCommand(int value)
  {
    this.value = value;
  }
	
  public boolean equals(Object obj)
  {
    if(!(obj instanceof ElfCommand))
      return false;
		
    return ((ElfCommand)obj).value == this.value;
  }
	
  protected int getValue()
  {
    return this.value;
  }
	
  protected static ElfCommand intern(int command)
  {
    return commands[command];
  }
}

