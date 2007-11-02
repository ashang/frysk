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

package frysk.scopes;

import frysk.debuginfo.TypeEntry;
import lib.dwfl.DwAt;
import lib.dwfl.DwInl;
import lib.dwfl.DwTag;
import lib.dwfl.DwarfDie;

/**
 * In DWARF a subroutine is used to refer to an entity that can either be a
 * concrete function (Subprogram) or an inlined function (InlinedSubprogram).
 */
public class Subroutine extends Scope {

    Composite struct;
    
    public Subroutine(DwarfDie die, TypeEntry typeEntry) {
	super(die, typeEntry);
    }

    /**
     * Returns the structure that this subroutine belongs to. If this
     * subroutine does not belong to any structs/classes it returns null.
     * 
     * @return Struct containing this Subroutine or null
     */
    public Composite getComposite() {
	if (struct == null) {
	    DwarfDie die = this.getDie().getOriginalDie();
	    if (die == null) {
		die = this.getDie();
	    }

	    DwarfDie[] scopes = die.getScopesDie();
	    for (int i = 0; i < scopes.length; i++) {
		if (scopes[i].getTag().equals(DwTag.STRUCTURE_TYPE)) {
		    this.struct = new Composite(scopes[i], typeEntry);
		}
	    }
	}
	return struct;
    }

    public boolean isInlined(){
	DwTag dwTag = getDie().getTag();
	long inlineAttribute = getDie().getAttrConstant(DwAt.INLINE);
	
	// Declared inlined and inlined by compiler
	if(dwTag.equals(DwTag.INLINED_SUBROUTINE) && inlineAttribute == DwInl.DECLARED_INLINED_){
	    return true;
	}
	
	// Declared inlined and inlined by compiler... but has INLINED attribute instead
	// of DECLARED_INLINED_
	if(dwTag.equals(DwTag.INLINED_SUBROUTINE) && inlineAttribute == DwInl.INLINED_){
	    return true;
	}
	
	// Declared regular and inlined by compiler
	if(dwTag.equals(DwTag.SUBPROGRAM) && inlineAttribute == DwInl.INLINED_){
	    return true;
	}
	
	// Declared inlined and not inlined by compiler
	if(dwTag.equals(DwTag.INLINED_SUBROUTINE) && inlineAttribute == DwInl.DECLARED_NOT_INLINED_){
	    return false;
	}
	
	// Declared regular and not inlined by compiler
	if(dwTag.equals(DwTag.SUBPROGRAM) && inlineAttribute == DwInl.NOT_INLINED_){
	    return false;
	}

	// Declared regular and does not have an inline attribute
	if(dwTag.equals(DwTag.SUBPROGRAM) && inlineAttribute == -1){
	    return false;
	}

	throw new RuntimeException("Unhandled case DwTag: " + dwTag + " inline attribute " + inlineAttribute);
    }
}
