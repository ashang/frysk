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

package frysk.value;

import inua.eio.ByteOrder;

/**
 * Operation handling for pointers and addresses.
 */
public class AddressUnit
extends ArithmeticUnit
{   
    public AddressUnit (ArrayType t, int wordSize) {
	super (wordSize);
	retType = new PointerType(t.getName(), ByteOrder.BIG_ENDIAN, 
		                  wordSize, t.getType());
    }
    
    public AddressUnit (PointerType t, int wordSize) {
	super (wordSize);
	retType = t;
    }
    
    /**
     * Pointer and Address Addition
     */
    public Value add(Value v1, Value v2) {  
	
	if (v1.getType() instanceof ArrayType | 
            v2.getType() instanceof ArrayType )
	    return addArray(v1, v2);
	
	// Commutative operation, so identify pointer 
	// and integer value.
        PointerType ptrType;
        Value ptrValue;
        Value intValue;
        if (v1.getType() instanceof PointerType) {
            ptrType = (PointerType)retType;
            ptrValue = v1;
            intValue = v2;
        } else {
            ptrType = (PointerType)retType;
            ptrValue = v2;
            intValue = v1;
        }
	
        // Handle address of array types
        if (ptrType.getType() instanceof ArrayType) {
            // Create pointer to array element type
            Type eType = ((ArrayType)ptrType.getType()).getType();           
            PointerType pType = new PointerType
                                (eType.getName(), ptrType.order(), wordSize, eType);
            return (pType.createValue
        	          (ptrValue.asLong() + eType.getSize()*intValue.asLong()));		
        }
        else 
           return retType.createValue
                          (ptrValue.asLong() + ptrType.getType().getSize()*intValue.asLong());
    } 
    
    private Value addArray (Value v1, Value v2) {
        ArrayType arrType;
        Value arrValue;
        Value intValue;
        if (v1.getType() instanceof ArrayType) {
            arrType = (ArrayType)v1.getType();
            arrValue = v1;
            intValue = v2;
        } else {
            arrType = (ArrayType)v2.getType();
            arrValue = v2;
            intValue = v1;
        }
        return retType.createValue
        (arrValue.getLocation().getAddress() + arrType.getType().getSize()*intValue.asLong());        

    }  
    
    public Value subtract(Value v1, Value v2) {	
	// v1-v2 = v1+(-v2)
	Location l = new ScratchLocation (v2.asBigInteger().negate().toByteArray());
	Value v2Neg = new Value (v2.getType(), l);
	return add (v1, v2Neg);
    }     
}