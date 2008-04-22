// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package frysk.scopes;

import java.util.HashMap;

import lib.dwfl.DwTag;
import lib.dwfl.DwarfDie;
import frysk.debuginfo.TypeFactory;

public class ScopeFactory {

    public static final ScopeFactory theFactory = new ScopeFactory();

    private final HashMap scopes;

    private ScopeFactory() {
	this.scopes = new HashMap();
    }

    public Scope getScope(DwarfDie die, TypeFactory typeFactory) {
	// this uses the object as a key so if 
	// a second DwarfDie object is created that refers
	// to the same underlying die it will not match.
	// the problem can be solved by using an attribute of
	// the die that is constant.
	// Or DwarfDieFactory should prevent creation of
	// redundant Die objects

	Object key = die;
	Scope scope = (Scope) scopes.get(key);
	if (scope == null) {
	    scope = createScope(die, typeFactory);
	    this.scopes.put(key, scope);
	}
	return scope;
    }

    private Scope createScope(DwarfDie die, TypeFactory typeFactory) {
	
	switch (die.getTag().hashCode()) {
	
	case DwTag.INLINED_SUBROUTINE_:
	    return new ConcreteInlinedFunction(die, typeFactory);
	case DwTag.SUBPROGRAM_:
	    Function function = new Function(die, typeFactory);
	    if(function.isInlined()){
		return new InlinedFunction(die,typeFactory);
	    }
	    return new OutOfLineFunction(die,typeFactory);
	case DwTag.LEXICAL_BLOCK_:
	    return new LexicalBlock(die, typeFactory);
	case DwTag.COMPILE_UNIT_:
	case DwTag.MODULE_:
	case DwTag.WITH_STMT_:
	case DwTag.CATCH_BLOCK_:
	case DwTag.TRY_BLOCK_:
	case DwTag.ENTRY_POINT_:
	case DwTag.NAMESPACE_:
	case DwTag.IMPORTED_UNIT_:
	    return new Scope(die, typeFactory);
	default:
	    throw new IllegalArgumentException("The given die ["+die + ": " + die.getTag()+"]is not a scope die");
	}
    }

}
