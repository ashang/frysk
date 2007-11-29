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

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a map between an internal and external register format.
 */
public class RegisterMap {
    
    private final Map integerToRegister = new HashMap();
    private final Map registerToNumber = new HashMap();
    private final Map numberToRegister = new HashMap();
    private final String what;

    public RegisterMap(String what) {
	this.what = what;
    }
    
    public final RegisterMap add(Register register,
				 Number number) {
	registerToNumber.put(register, number);
	integerToRegister.put(new Integer(number.intValue()), register);
	numberToRegister.put(number, register);
	return this;
    }
    
    public Number getRegisterNumber(Register register) {
	Number number = (Number) registerToNumber.get(register);
	if (number == null)
	    throw new NullPointerException("register <" + register
					   + "> not found in " + what
					   + " register map");
	return number;
    }
    
    public Register getRegister(int regNum) {
	Register register
	    = (Register) integerToRegister.get(new Integer(regNum));
	if (register == null)
	    throw new NullPointerException("register number <" + regNum
					   + "> not found in " + what
					   + " register map");
	return register;
    }

    public Register getRegister(Number number) {
	Register register = (Register) numberToRegister.get(number);
	if (register == null)
	    throw new NullPointerException("register number <" + number
					   + "> not found in " + what
					   + " register map");
	return register;
    }
}
