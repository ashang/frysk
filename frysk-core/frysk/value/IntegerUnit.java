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

import java.math.BigInteger;
import inua.eio.ByteBuffer;

/**
 *  Arithmetic and logical Operation handling 
 *  for integers. All arithmetic done using 
 *  BigInteger.
 */
public class IntegerUnit
     extends ArithmeticUnit
{
    public IntegerUnit (IntegerType t1, IntegerType t2, int wordSize) {
	super (wordSize);
	// Return type should be the larger type.
	retType = (t1.getSize() > t2.getSize()) ?
		  t1 : t2;
    }
    
    public IntegerUnit (IntegerType t1, int wordSize) {
	super (wordSize);
	// Return type should be the larger type.
	retType =  t1 ;
    }    

    public Value add(Value v1, Value v2) {
	return retType.createValue
	       (v1.asBigInteger().add(v2.asBigInteger()));
    }
    public Value subtract(Value v1, Value v2) {
	return retType.createValue
               (v1.asBigInteger().subtract(v2.asBigInteger()));
    }    
    public Value multiply (Value v1, Value v2) {
	return retType.createValue
	       (v1.asBigInteger().multiply(v2.asBigInteger()));
    }    
    public Value divide (Value v1, Value v2) {
	return retType.createValue
	       (v1.asBigInteger().divide(v2.asBigInteger()));
    }    
    public Value mod(Value v1, Value v2) {
	return retType.createValue
	       (v1.asBigInteger().mod(v2.asBigInteger()));
    }

    public Value shiftLeft(Value v1, Value v2) {
	return retType.createValue
	(v1.asBigInteger().shiftLeft(v2.asBigInteger().intValue()));
    } 
    public Value shiftRight(Value v1, Value v2) {
	return retType.createValue
	(v1.asBigInteger().shiftRight(v2.asBigInteger().intValue()));
    } 
    
    public Value lessThan(Value v1, Value v2) {
	return intType.createValue
	(v1.asBigInteger().compareTo(v2.asBigInteger()) < 0 ? 1 : 0);
    }   
    public Value greaterThan(Value v1, Value v2) {
	return intType.createValue
	(v1.asBigInteger().compareTo(v2.asBigInteger()) > 0 ? 1 : 0);
    }     
    public Value lessThanOrEqualTo(Value v1, Value v2) {
	return intType.createValue
	(v1.asBigInteger().compareTo(v2.asBigInteger()) <= 0 ? 1 : 0);
    }     
    public Value greaterThanOrEqualTo(Value v1, Value v2) {
	return intType.createValue
	(v1.asBigInteger().compareTo(v2.asBigInteger()) >= 0 ? 1 : 0);
    }   
    public Value equal(Value v1, Value v2) {
	return intType.createValue
	(v1.asBigInteger().compareTo(v2.asBigInteger()) == 0 ? 1 : 0);
    }     
    public Value notEqual(Value v1, Value v2) {
	return intType.createValue
	(v1.asBigInteger().compareTo(v2.asBigInteger()) != 0 ? 1 : 0);
    }    
    
    public Value bitWiseAnd(Value v1, Value v2) {
	return retType.createValue
	       (v1.asBigInteger().and(v2.asBigInteger()));
    }   
    public Value bitWiseXor(Value v1, Value v2) {
	return retType.createValue
	       (v1.asBigInteger().xor(v2.asBigInteger()));
    }    
    public Value bitWiseOr(Value v1, Value v2) {
	return retType.createValue
	       (v1.asBigInteger().or(v2.asBigInteger()));
    }    
    public Value bitWiseComplement(Value v1) {
	return retType.createValue
	       (v1.asBigInteger().not());
    }
    
    public boolean getLogicalValue (Value v1, ByteBuffer mem) {
	return ((v1.asBigInteger().compareTo(BigInteger.ZERO) == 0) 
		 ? false : true);
    }    
}