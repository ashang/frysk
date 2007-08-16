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
 * Enum representing symbol types.
 * Used by {@sa ElfSymbol.Builder}.
 */
public class ElfSymbolType
{
  public static final ElfSymbolType ELF_STT_NOTYPE = new ElfSymbolType(0);
  public static final ElfSymbolType ELF_STT_OBJECT = new ElfSymbolType(1);
  public static final ElfSymbolType ELF_STT_FUNC = new ElfSymbolType(2);
  public static final ElfSymbolType ELF_STT_SECTION = new ElfSymbolType(3);
  public static final ElfSymbolType ELF_STT_FILE = new ElfSymbolType(4);
  public static final ElfSymbolType ELF_STT_COMMON = new ElfSymbolType(5);
  public static final ElfSymbolType ELF_STT_TLS = new ElfSymbolType(6);
  public static final ElfSymbolType ELF_STT_NUM = new ElfSymbolType(7);

  public static final ElfSymbolType ELF_STT_OS_0 = new ElfSymbolType(10);
  public static final ElfSymbolType ELF_STT_OS_1 = new ElfSymbolType(11);
  public static final ElfSymbolType ELF_STT_OS_2 = new ElfSymbolType(12);
  public static final ElfSymbolType ELF_STT_PROC_0 = new ElfSymbolType(13);
  public static final ElfSymbolType ELF_STT_PROC_1 = new ElfSymbolType(14);
  public static final ElfSymbolType ELF_STT_PROC_2 = new ElfSymbolType(15);

  private static ElfSymbolType[] types = {
    ELF_STT_NOTYPE,
    ELF_STT_OBJECT,
    ELF_STT_FUNC,
    ELF_STT_SECTION,
    ELF_STT_FILE,
    ELF_STT_COMMON,
    ELF_STT_TLS,
    ELF_STT_NUM,
    null,
    null,
    ELF_STT_OS_0,
    ELF_STT_OS_1,
    ELF_STT_OS_2,
    ELF_STT_PROC_0,
    ELF_STT_PROC_1,
    ELF_STT_PROC_2
  };

  private int value;

  private ElfSymbolType(int value)
  {
    this.value = value;
  }

  public boolean equals(Object obj)
  {
    if(!(obj instanceof ElfSymbolType))
      return false;
    return ((ElfSymbolType)obj).value == this.value;
  }

  /**
   * Returns true if this type is from OS-specific range.
   */
  public boolean isOsSpecific()
  {
    return this.value >= 10 && this.value <= 12;
  }

  /**
   * Returns true if this type is from processor-specific range.
   */
  public boolean isProcSpecific()
  {
    return this.value >= 13 && this.value <= 15;
  }

  protected int getValue()
  {
    return this.value;
  }

  /**
   * Given an integral value, answer associated ElfSymbolType
   * object.  This should only ever be used by ElfSymbolBuilder
   * mechanism.
   */
  static ElfSymbolType intern(int value)
  {
    return types[value];
  }
}
