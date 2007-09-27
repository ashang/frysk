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

package frysk.isa;

import java.util.List;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The set of registers belonging to an ISA.
 */
public abstract class Registers {

    private final SortedMap registerGroupByName = new TreeMap();
    private final SortedMap registerByName = new TreeMap();
    private final String[] registerGroupNames;

    protected Registers(RegisterGroup[] registerGroups) {
	// Build up search tables.
	List groupNames = new LinkedList();
	for (int i = 0; i < registerGroups.length; i++) {
	    RegisterGroup registerGroup = registerGroups[i];
	    groupNames.add(registerGroup.name);
	    registerGroupByName.put(registerGroup.name, registerGroup);
	    for (int j = 0; j < registerGroup.registers.length; j++) {
		Register register = registerGroup.registers[j];
		registerByName.put(register.name, register);
	    }
	}
	registerGroupNames = new String[groupNames.size()];
	groupNames.toArray(registerGroupNames);
    }

    /**
     * Return the program-counter register.
     */
    public abstract Register getProgramCounter();
    /**
     * Return the stack-pointer register.
     */
    public abstract Register getStackPointer();
    /**
     * Return the "default" register group.
     */
    public abstract RegisterGroup getDefaultRegisterGroup();
    /**
     * Return the "all" register group.
     */
    public abstract RegisterGroup getAllRegistersGroup();

    /**
     * Return the register group; searched by NAME.
     */
    public RegisterGroup getGroup(String name) {
	return (RegisterGroup) registerGroupByName.get(name);
    }

    /**
     * Return the register; identified by NAME.
     */
    public Register getRegister(String name) {
	return (Register) registerByName.get(name);
    }

    /**
     * Return all the register group names.
     */
    public String[] getGroupNames() {
	return registerGroupNames;
    }
}
