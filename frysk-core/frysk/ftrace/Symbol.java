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

package frysk.ftrace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lib.dwfl.ElfSymbolType;
import lib.dwfl.ElfSymbolVersion;

/**
 * Describes symbol inside {@sa ObjectFile} for purposes of Ltrace.
 */
public class Symbol
{
  public final String name;
  public final long value;
  public final long size;
  public final ElfSymbolType type;
  public final long shndx;

  public final ElfSymbolVersion.Def[] verdefs;
  public final ElfSymbolVersion.Need[] verneeds;

  protected ObjectFile parent = null;

  public long entryAddress;
  public long pltAddress;

  /**
   * Build ltrace symbol.
   *
   * @param name Name of the symbol.
   * @param type Type of the symbol, as in ElfSymbol.ELF_STT_* fields.
   * @param value Value of the symbol.
   * @param size Size of the symbol.
   * @param shndx Associated section index, or one of the special
   *   values in ElfSectionHeader.ELF_SHN_*.
   * @param versions Version requirements and/or definitions of
   *   symbol. If there are none, null is passed instead of
   *   zero-length array.
   */
  public Symbol(final String name, ElfSymbolType type, long value,
		long size, long shndx, List versions)
  {
    this.name = name;
    this.type = type;
    this.value = value;
    this.size = size;
    this.shndx = shndx;

    final ArrayList foundDefs = new ArrayList();
    final ArrayList foundNeeds = new ArrayList();

    if (versions != null)
      for (Iterator it = versions.iterator(); it.hasNext(); )
	((ElfSymbolVersion)it.next()).visit(new ElfSymbolVersion.Visitor() {
	    public Object def(ElfSymbolVersion.Def verdef) {
	      foundDefs.add(verdef);
	      return null;
	    }
	    public Object need(ElfSymbolVersion.Need verneed) {
	      foundNeeds.add(verneed);
	      return null;
	    }
	  });

    {
      this.verdefs = new ElfSymbolVersion.Def[foundDefs.size()];
      int i = 0;
      for (Iterator it = foundDefs.iterator(); it.hasNext(); )
	this.verdefs[i++] = (ElfSymbolVersion.Def)it.next();
    }

    {
      this.verneeds = new ElfSymbolVersion.Need[foundNeeds.size()];
      int i = 0;
      for (Iterator it = foundNeeds.iterator(); it.hasNext(); )
	this.verneeds[i++] = (ElfSymbolVersion.Need)it.next();
    }
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer();
    buf.append(this.name);
    return buf.toString();
  }

  public void addedTo(ObjectFile of) {
    this.parent = of;
  }

  public ObjectFile getParent() {
    return this.parent;
  }

  public void setEntryAddress(long address) {
    this.entryAddress = address;
  }

  public void setPltAddress(long address) {
    this.pltAddress = address;
  }
}
