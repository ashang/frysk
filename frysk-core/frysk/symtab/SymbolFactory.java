// This file is part of the program FRYSK.
//
// Copyright 2007 Oracle Corporation.
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

package frysk.symtab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.rsl.Log;
import frysk.rsl.LogFactory;

import lib.dwfl.DwarfDie;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflModule;
import lib.dwfl.SymbolBuilder;

/**
 * The object-file symbol.  Typically obtained by reading ELF
 * information.
 *
 * Do not confuse this with higher-level symbolic information, such as
 * function names, obtained from debug information such as DWARF.
 */

public class SymbolFactory
{
    private static final Log fine = LogFactory.fine(SymbolFactory.class);
    private static final Log warning = LogFactory.warning(SymbolFactory.class);

    /**
     * A special unknown symbol.
     */
    public static final Symbol UNKNOWN = new UnknownSymbol ();

   /**
    * Return the symbol at the specified address within task.
    */
    public static Symbol getSymbol(Task task, long address) {
	Dwfl dwfl = DwflCache.getDwfl(task);
	if (dwfl == null)
	    return UNKNOWN;

	final DwflModule module = dwfl.getModule(address);
	if (module == null)
	    return UNKNOWN;

	class Builder implements SymbolBuilder {
	    public DwflSymbol symbol = null;
	    public void symbol(String name, long value, long size,
			       lib.dwfl.ElfSymbolType type,
			       lib.dwfl.ElfSymbolBinding bind,
			       lib.dwfl.ElfSymbolVisibility visibility)
	    {
		if (symbol != null)
		    warning.log("Symbol", name, "reported on address", value,
				"where symbol was already reported:", symbol.getName());
		else if (name != null)
		    symbol = new DwflSymbol (value, size, name, type, null, module);
	    }
	}
	Builder builder = new Builder();

	module.getSymbol(address, builder);
	if (builder.symbol == null)
	    return UNKNOWN;

	return builder.symbol;
    }

    private static Map getPublicTable(final DwflModule module) {
	final Map dwSymbols = new HashMap();
	for (Iterator it = module.getPubNames().iterator(); it.hasNext(); ) {
	    DwarfDie die = (DwarfDie)it.next();
	    dwSymbols.put(die.getName(), die);
	}
	return dwSymbols;
    }

    public static Map getSymbolTable(final DwflModule module) {

	final Map publicTable = getPublicTable(module);

	final Map table = new HashMap();
	SymbolBuilder builder = new SymbolBuilder() {
		public void symbol(String name, long value, long size,
				   lib.dwfl.ElfSymbolType type,
				   lib.dwfl.ElfSymbolBinding bind,
				   lib.dwfl.ElfSymbolVisibility visibility)
		{
		    DwarfDie die = publicTable == null ? null
			: (DwarfDie)publicTable.get(name);
		    int index;
		    if ((index = name.indexOf('@')) != -1)
			name = name.substring(0, index);
		    DwflSymbol sym
			= new DwflSymbol(value, size, name, type, die, module);
		    table.put(name, sym);
		}
	};
	module.getSymtab(builder);
	fine.log("Got", table.size(), "symbols after sweep over symtab.");

	// This will probably not add anything, but just to make sure...
	for (Iterator it = publicTable.entrySet().iterator(); it.hasNext(); ) {
	    Map.Entry entry = (Map.Entry)it.next();
	    String name = (String)entry.getKey();
	    if (!table.containsKey(name)) {
		DwarfDie die = (DwarfDie)entry.getValue();
		ArrayList entries = die.getEntryBreakpoints();
		if (entries != null) {
		    long addr = ((Long)entries.get(0)).longValue();
		    long size = die.getHighPC() - die.getLowPC();
		    lib.dwfl.ElfSymbolType type = null; // XXX fixme
		    table.put(name, new DwflSymbol(addr, size, die.getName(),
						   type, die, module));
		}
	    }
	}
	fine.log("Got", table.size(), "symbols after sweep over debuginfo.");

	return table;
    }

    /**
     * Return symbols in given DwflModule.
     * @return List&lt;DwflSymbol&gt;
     */
    public static List getSymbols(final DwflModule module) {

	Map symbolTable = getSymbolTable(module);
	List symbols = new ArrayList();
	symbols.addAll(symbolTable.values());

	return symbols;
    }

    /**
     * Return list of PLTSymbol objects representing PLT entries in
     * this module.
     *
     * @param symbols Symbol table previously loaded through
     * getSymbolTable.  May be null, in that case new symbol table
     * will be loaded by the method.
     */
    public static List getPLTEntries(final DwflModule module,
				     final Map symbols) {

	final Map symtab = symbols != null ? symbols : getSymbolTable(module);

	final List entries = new LinkedList();
	SymbolBuilder builder = new SymbolBuilder() {
		public void symbol(String name, long value, long size,
				   lib.dwfl.ElfSymbolType type,
				   lib.dwfl.ElfSymbolBinding bind,
				   lib.dwfl.ElfSymbolVisibility visibility)
		{
		    DwflSymbol ref = (DwflSymbol)symtab.get(name);
		    PLTEntry sym = new PLTEntry(value, ref);
		    entries.add(sym);
		}
	};
	module.getPLTEntries(builder);

	return entries;
    }

    /**
     * Get address list by symbol name.
     * @param task
     * @param name
     * @return address list
     */
    public static LinkedList getAddresses(Task task, String name,
					  ModuleMatcher matcher) {
	Dwfl dwfl = DwflCache.getDwfl(task);
	DwflModule[] modules = dwfl.getModules();
	final LinkedList addrs = new LinkedList();
	SymbolBuilder builder = new SymbolBuilder() {
		public void symbol(String name, long value, long size,
				   lib.dwfl.ElfSymbolType type,
				   lib.dwfl.ElfSymbolBinding bind,
				   lib.dwfl.ElfSymbolVisibility visibility)
		{
		    addrs.add(new Long(value));
		}
	};
	for (int i = 0; i < modules.length; i++) {
	    DwflModule module = modules[i];
	    if (matcher != null && !matcher.moduleMatches(module.getName()))
		continue;
	    module.getSymbolByName(name, builder);
	}
	if (addrs.size() == 0)
	    throw new RuntimeException("Couldn't find symbol " + name);
	else
	    return addrs;
    }

    public static LinkedList getAddresses(Task task, String name) {
	return getAddresses(task, name, null);
    }
}
