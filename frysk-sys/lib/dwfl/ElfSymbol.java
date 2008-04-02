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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The object-file symbol.  Typically obtained by reading ELF
 * information.
 *
 * Do not confuse this with higher-level symbolic information, such as
 * function names, obtained from debug information such as DWARF.
 */
public class ElfSymbol {

    // The symbol's fields.
    private final long address;
    private final long size;
    private final String name;

    protected ElfSymbol(long address, long size, String name) {
	this.address = address;
	this.size = size;
	this.name = name;
    }

    /**
     * Return the address of the symbol.
     */
    public long getAddress ()
    {
	return address;
    }

    /**
     * Return the size of the symbol (possibly zero).
     */
    public long getSize ()
    {
	return size;
    }

    /**
     * Return the mangled name (the raw string found in the symbol
     * table).  Or NULL, of the name is unknown.
     */
    public String getName ()
    {
	return name;
    }

    /**
     * Dump the symbol's contents.
     */
    public String toString ()
    {
	return name + "@" + Long.toHexString (address) + ":" + size;
    }


    public static interface Builder {
      /**
       * Called for each symbol.
       *
       * @param index Index of symbol in symbol table.
       * @param name Name of symbol.
       * @param value Address of symbol if symbol is defined, or 0.
       * @param size Size of object associated with the symbol.
       * @param type Symbol type.
       * @param bind Symbol binding.
       * @param visibility Symbol visibility.
       * @param shndx Associated section index, or one of the special
       *   values in ElfSectionHeader.ELF_SHN_*.
       * @param versions Version requirements and/or definitions of
       *   symbol. If there are none, null is passed instead of
       *   empty list.
       */
      void symbol (long index, String name,
		   long value, long size,
		   ElfSymbolType type, ElfSymbolBinding bind, ElfSymbolVisibility visibility,
		   long shndx, List versions);
    }

    protected static class PrivVerdef {
      int version;	// revision of version interface
      boolean base;	// version definition of file itself
      int index;	// version index
      int hash; 	// version name hash value
      String[] names;   // version or dependency names
    }

    protected static class PrivVerneed {
      public static class Aux {
	int hash;	// hash value of dependency name
	boolean weak;	// weak version reference
	String name;	// dependency name
	int index;	// version index
      }

      int version;	// revision of version interface
      String filename;	// filename for this dependency
      Aux[] aux;	// associated auxiliary entries
    }

    private static ElfSectionHeader getSymtabHeader(ElfSection symbolsS)
      throws ElfException
    {
      ElfSectionHeader symbolsH = symbolsS.getSectionHeader();
      if (!(symbolsH.type == ElfSectionHeader.ELF_SHT_SYMTAB
	    || symbolsH.type == ElfSectionHeader.ELF_SHT_DYNSYM))
	throw new ElfException("Section " + symbolsH.name + " doesn't contain symbol table.");
      return symbolsH;
    }

    public static long symbolsCount(ElfSection symbolsS)
      throws ElfException
    {
      ElfSectionHeader symbolsH = getSymtabHeader(symbolsS);
      return symbolsH.size / symbolsH.entsize;
    }

    public static class Loader
    {
      private final Elf parent;
      private final ElfSectionHeader symbolsH;
      private final long symbolsCount;
      private final long symbolsP;
      private final long versymP;

      // Contains list of all found versions (verdefs as well as
      // verneeds).  It's keyed by unique version ID.  The value is
      // list of defs and needs associated with given ID.
      private final Map versionMap = new HashMap();

      /**
       * Initializes symbol loader, assuming you have no version
       * information associated.
       *
       * @param symbolsS Section of type STRTAB or DYNSYM, contains
       * symbol table.  Must not be null.
       */
      public Loader(ElfSection symbolsS)
	throws ElfException
      {
	this(symbolsS, null,
	     null, 0,
	     null, 0);
      }

      /**
       * Initializes symbol loader.
       *
       * @param symbolsS Section of type STRTAB or DYNSYM, contains
       * symbol table.  Must not be null.
       * @param versymS Section of type GNU_versym.  May be null, but in
       * that case verdefS and verneedS have to be null, too.
       * @param verdefS Section with version definitions, typed GNU_verdef.
       * May be null, if verneed is not null.
       * @param verdefCount Number of verdef entries.  If verdefS is
       * null, this has to be 0.
       * @param verneed Section with version requirements, typed
       * GNU_verneed.  May be null, if verdef is not null.
       * @param verneedCount Number of verdneed entries.  If verneedS is
       * null, this has to be 0.
       */
      public Loader(ElfSection symbolsS, ElfSection versymS,
		    ElfSection verdefS, int verdefCount,
		    ElfSection verneedS, int verneedCount)
	throws ElfException
      {
	// Sanity checks...

	parent = symbolsS.getParent();
	symbolsH = getSymtabHeader(symbolsS);
	symbolsP = symbolsS.getData().getPointer();
	symbolsCount = symbolsH.size / symbolsH.entsize;

	if (versymS != null)
	  {
	    ElfSectionHeader versymH = versymS.getSectionHeader();
	    if (versymH.type != ElfSectionHeader.ELF_SHT_GNU_versym)
	      throw new ElfException("Section " + versymH.name + " doesn't contain versym info.");
	    ElfData d = versymS.getData();
	    versymP = d.getPointer();
	  }
	else
	  versymP = 0;


	// Load verdefs and store to versionMap.
	if (verdefS != null)
	  {
	    ElfSectionHeader verdefH = verdefS.getSectionHeader();
	    if (verdefH.type != ElfSectionHeader.ELF_SHT_GNU_verdef)
	      throw new ElfException("Section " + verdefH.name + " doesn't contain verdef info.");

	    long verdefP = verdefS.getData().getPointer();

	    PrivVerdef[] verdef = new PrivVerdef[verdefCount];
	    if (!elf_load_verdef(parent, verdefP, verdefH.link, verdef))
	      throw new ElfException("Couldn't load verdef info from section " + verdefH.name + ".");

	    for (int i = 0; i < verdefCount; ++i)
	      {
		Integer key = new Integer(verdef[i].index);
		ArrayList verList = (ArrayList)versionMap.get(key);
		if (verList == null)
		  {
		    verList = new ArrayList();
		    versionMap.put(key, verList);
		  }

		int defCount = verdef[i].names.length;
		for (int j = 0; j < defCount; ++j)
		  verList.add(new ElfSymbolVersion.Def(verdef[i].names[j], verdef[i].base));
	      }
	  }
	else if (verdefCount != 0)
	  throw new AssertionError("Inconsistent verdef count.");

	// Load verneeds and store to versionMap.
	if (verneedS != null)
	  {
	    ElfSectionHeader verneedH = verneedS.getSectionHeader();
	    if (verneedH.type != ElfSectionHeader.ELF_SHT_GNU_verneed)
	      throw new ElfException("Section " + verneedH.name + " doesn't contain verneed info.");
	    long verneedP = verneedS.getData().getPointer();

	    PrivVerneed[] verneed = new PrivVerneed[verneedCount];
	    if (!elf_load_verneed(parent, verneedP, verneedH.link, verneed))
	      throw new ElfException("Couldn't load verneed info from section " + verneedH.name + ".");

	    for (int i = 0; i < verneedCount; ++i)
	      for (int j = 0; j < verneed[i].aux.length; ++j)
		{
		  Integer key = new Integer(verneed[i].aux[j].index);
		  ArrayList verList = (ArrayList)versionMap.get(key);
		  if (verList == null)
		    {
		      verList = new ArrayList();
		      versionMap.put(key, verList);
		    }

		  PrivVerneed.Aux aux = verneed[i].aux[j];
		  String fn = verneed[i].filename;
		  verList.add(new ElfSymbolVersion.Need(fn, aux.name, aux.weak));
		}
	  }
	else if (verneedCount != 0)
	  throw new AssertionError("Inconsistent verneed count.");
      }

      private void privateLoad(long index, ElfSymbol.Builder builder)
	throws ElfException
      {
	List versions = null;

	if (versymP != 0)
	  {
	    int version = elf_getversym(versymP, index);
	    if ((version & 0x8000) != 0)
	      {
		//hidden = true;
		version ^= 0x8000;
	      }

	    versions = (List)versionMap.get(new Integer(version));
	  }

	if (!elf_buildsymbol(parent, symbolsP, index, symbolsH.link, versions, builder))
	  throw new ElfException("Symbol on index " + index + " couldn't be retrieved.");
      }

      /**
       * Load all symbols in prepared symbol tables.  Calls
       * ElfSymbol.Builder.symbol for each symbol found.
       */
      public void loadAll(ElfSymbol.Builder builder)
	throws ElfException
      {
	// Note: ignoring special symbol entry on index 0.
	for (long i = 1; i < symbolsCount; ++i)
	  privateLoad(i, builder);
      }

      /**
       * Request loading symbol on given index.
       */
      public void load(long index, ElfSymbol.Builder builder)
	throws ElfException
      {
	if (index < 0 || index >= symbolsCount)
	  throw new IllegalArgumentException("Invalid index.");
	privateLoad(index, builder);
      }
    }

    protected static native boolean elf_buildsymbol(Elf parent, long data_pointer,
						    long symbol_index, long str_sect_index,
						    List versions,
						    ElfSymbol.Builder builder);

    protected static native int elf_getversym(long data_pointer, long symbol_index);

    protected static native boolean elf_load_verneed(Elf parent, long data_pointer,
						     long str_sect_index, PrivVerneed[] ret);

    protected static native boolean elf_load_verdef(Elf parent, long data_pointer,
						    long str_sect_index, PrivVerdef[] ret);
}
