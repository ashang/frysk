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

import java.util.LinkedList;

import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.rsl.Log;
import frysk.rsl.LogFactory;

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

	DwflModule module = dwfl.getModule(address);
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
		    symbol = new DwflSymbol (value, size, name);
	    }
	}
	Builder builder = new Builder();

	module.getSymbol(address, builder);
	if (builder.symbol == null)
	    return UNKNOWN;

	return builder.symbol;
    }

    /**
     * Get address list by symbol name.
     * @param task
     * @param name
     * @return address list
     */
    public static LinkedList getSymbol(Task task, String name) {
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
	    module.getSymbolByName(name, builder);
	}
	if (addrs.size() == 0)
	    throw new RuntimeException("Couldn't find symbol " + name);
	else
	    return addrs;
    }
}
