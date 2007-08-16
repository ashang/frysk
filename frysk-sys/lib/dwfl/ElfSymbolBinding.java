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
 * Enum representing symbol binding types.
 * Used by {@sa ElfSymbol.Builder}.
 */
public class ElfSymbolBinding
{
  public static final ElfSymbolBinding ELF_STB_LOCAL = new ElfSymbolBinding(0);
  public static final ElfSymbolBinding ELF_STB_GLOBAL = new ElfSymbolBinding(1);
  public static final ElfSymbolBinding ELF_STB_WEAK = new ElfSymbolBinding(2);

  public static final ElfSymbolBinding ELF_STB_OS_0 = new ElfSymbolBinding(10);
  public static final ElfSymbolBinding ELF_STB_OS_1 = new ElfSymbolBinding(11);
  public static final ElfSymbolBinding ELF_STB_OS_2 = new ElfSymbolBinding(12);
  public static final ElfSymbolBinding ELF_STB_PROC_0 = new ElfSymbolBinding(13);
  public static final ElfSymbolBinding ELF_STB_PROC_1 = new ElfSymbolBinding(14);
  public static final ElfSymbolBinding ELF_STB_PROC_2 = new ElfSymbolBinding(15);

  private static ElfSymbolBinding[] bindings = {
    ELF_STB_LOCAL,
    ELF_STB_GLOBAL,
    ELF_STB_WEAK,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    ELF_STB_OS_0,
    ELF_STB_OS_1,
    ELF_STB_OS_2,
    ELF_STB_PROC_0,
    ELF_STB_PROC_1,
    ELF_STB_PROC_2
  };

  private int value;

  private ElfSymbolBinding(int value)
  {
    this.value = value;
  }

  public boolean equals(Object obj)
  {
    if(!(obj instanceof ElfSymbolBinding))
      return false;
    return ((ElfSymbolBinding)obj).value == this.value;
  }

  /**
   * Returns true if this binding is from OS-specific range.
   */
  public boolean isOsSpecific()
  {
    return this.value >= 10 && this.value <= 12;
  }

  /**
   * Returns true if this binding is from processor-specific range.
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
   * Given an integral value, answer associated ElfSymbolBinding
   * object.  This should only ever be used by ElfSymbolBuilder
   * mechanism.
   */
  static ElfSymbolBinding intern(int value)
  {
    return bindings[value];
  }
}
