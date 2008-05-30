// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

import frysk.expr.FQIdentifier;
import frysk.symtab.DwflSymbol;
import frysk.symtab.PLTEntry;

public class SymbolRule extends Rule {

    // No globbing supported atm.
    public final FQIdentifier fqid;

    public SymbolRule(boolean addition, RuleOptions options, FQIdentifier fqid) {
	super (addition, options);
	this.fqid = fqid;
    }

    public String toString() {
	return super.toString() + fqid;
    }


    protected boolean checkVersionMatches(final DwflSymbol symbol)
    {
	return true;

	// XXX Version support didn't arrive yet.
	/*
	ElfSymbolVersion[] vers = (tp.origin == TracePointOrigin.PLT)
	    ? (ElfSymbolVersion[])tp.symbol.verneeds
	    : (ElfSymbolVersion[])tp.symbol.verdefs;

	// When there is no version assigned to symbol, we pretend it has
	// a version of ''.  Otherwise we require one of the versions to
	// match the version pattern.
	if (vers.length == 0) {
	    if (this.versionPattern.matcher("").matches())
		return true;
	}
	else
	    for (int i = 0; i < vers.length; ++i)
		if (this.versionPattern.matcher(vers[i].name).matches())
		    return true;

	return false;
	*/
    }

    protected boolean checkNameMatches(final DwflSymbol symbol)
    {
	if (fqid.symbol.equals(symbol.getName()))
	    return true;

	// XXX Alias support didn't arrive yet.
	/*
	if (symbol.aliases != null)
	    for (int i = 0; i < symbol.aliases.size(); ++i) {
		String alias = (String)symbol.aliases.get(i);
		if (this.namePattern.matcher(alias).matches())
		    return true;
	    }
	*/

	return false;
    }

    public boolean matches(Object traceable) {
	DwflSymbol sym = null;
	if (fqid.wantPlt) {
	    if (traceable instanceof PLTEntry)
		sym = ((PLTEntry)traceable).getSymbol();
	} else if (traceable instanceof DwflSymbol)
		sym = (DwflSymbol)traceable;

	if (sym != null)
	    return checkNameMatches(sym)
		&& checkVersionMatches(sym);
	else
	    return false;
    }
}
