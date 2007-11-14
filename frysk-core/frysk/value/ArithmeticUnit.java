//This file is part of the program FRYSK.

//Copyright 2007, Red Hat Inc.

//FRYSK is free software; you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by
//the Free Software Foundation; version 2 of the License.

//FRYSK is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with FRYSK; if not, write to the Free Software Foundation,
//Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.

//In addition, as a special exception, Red Hat, Inc. gives You the
//additional right to link the code of FRYSK with code not covered
//under the GNU General Public License ("Non-GPL Code") and to
//distribute linked combinations including the two, subject to the
//limitations in this paragraph. Non-GPL Code permitted under this
//exception must only link to the code of FRYSK through those well
//defined interfaces identified in the file named EXCEPTION found in
//the source code files (the "Approved Interfaces"). The files of
//Non-GPL Code may instantiate templates or use macros or inline
//functions from the Approved Interfaces without causing the
//resulting work to be covered by the GNU General Public
//License. Only Red Hat, Inc. may make changes or additions to the
//list of Approved Interfaces. You must obey the GNU General Public
//License in all respects for all of the FRYSK code and other code
//used in conjunction with FRYSK except the Non-GPL Code covered by
//this exception. If you modify this file, you may extend this
//exception to your version of the file, but you are not obligated to
//do so. If you do not wish to provide this exception without
//modification, you must delete this exception statement from your
//version and license this file solely under the GPL without
//exception.

package frysk.value;

import inua.eio.ByteOrder;
import inua.eio.ByteBuffer;

/**
 * Arithmetic and Logical Operation handling
 * for different types.
 */
public abstract class ArithmeticUnit 
{
    // Return type based on the type of operands - used for 
    // most operations.
    protected ArithmeticType retType;
    
    // Integer return type for relational, equality
    // and logical operations.
    protected IntegerType intType;

    protected int wordSize;
    
    protected ArithmeticUnit(int wordSize) {
	// XXX: Is endianness okay?
	// Create an int type with size equal to word
	// size of machine.
	intType = new UnsignedType ("int", ByteOrder.LITTLE_ENDIAN, 
		                    wordSize);
	this.wordSize = wordSize;
    }
    
    // Multiplicative and Additive expressions
    public Value add(Value v1, Value v2) {
	throw new InvalidOperatorException
	          (v1.getType(), v2.getType(), "+");
    }
    public Value subtract(Value v1, Value v2) {
	throw new InvalidOperatorException
	          (v1.getType(), v2.getType(), "-");
    }
    public Value multiply (Value v1, Value v2) {
        throw new InvalidOperatorException
                  (v1.getType(), v2.getType(), "*");
    }
    public Value divide(Value v1, Value v2) {
	throw new InvalidOperatorException
	          (v1.getType(), v2.getType(), "/");
    }
    public Value mod(Value v1, Value v2) {
	throw new InvalidOperatorException
	          (v1.getType(), v2.getType(), "%");
    }    
    
    // Shift expressions
    public Value shiftLeft (Value v1, Value v2) {
	throw new InvalidOperatorException
	          (v1.getType(), v2.getType(), "<<");
    }
    public Value shiftRight (Value v1, Value v2) {
	throw new InvalidOperatorException
                  (v1.getType(), v2.getType(), ">>");
    }
    
    // Relational expressions
    public Value lessThan (Value v1, Value v2) {
        throw new InvalidOperatorException
                  (v1.getType(), v2.getType(), "<");
    }
    public Value greaterThan (Value v1, Value v2) {
        throw new InvalidOperatorException
                  (v1.getType(), v2.getType(), ">");
    }
    public Value lessThanOrEqualTo (Value v1, Value v2) {
        throw new InvalidOperatorException
                  (v1.getType(), v2.getType(), "<=");
    }
    public Value greaterThanOrEqualTo (Value v1, Value v2) {
        throw new InvalidOperatorException
                  (v1.getType(), v2.getType(), ">=");
    }    
    public Value equal (Value v1, Value v2) {
        throw new InvalidOperatorException
                  (v1.getType(), v2.getType(), "==");
    }
    public Value notEqual (Value v1, Value v2) {
        throw new InvalidOperatorException
                  (v1.getType(), v2.getType(), "!=");
    }
    
    // Bit wise expressions.
    public Value bitWiseAnd (Value v1, Value v2) {
        throw new InvalidOperatorException
                  (v1.getType(), v2.getType(), "&");
    }
    public Value bitWiseXor (Value v1, Value v2) {
        throw new InvalidOperatorException
                  (v1.getType(), v2.getType(), "^");
    }
    public Value bitWiseOr (Value v1, Value v2) {
        throw new InvalidOperatorException
                  (v1.getType(), v2.getType(), "|");
    }
    public Value bitWiseComplement (Value v1) {
        throw new InvalidOperatorException
                  (v1.getType(), "~");
    }
    
    // Logical expressions - valid for any scalar types.
    public Value logicalAnd (Value v1, Value v2, ByteBuffer mem) {
	boolean op1 = v1.getType().getALU(wordSize).getLogicalValue(v1, mem);
	boolean op2 = v2.getType().getALU(wordSize).getLogicalValue(v2, mem);
	return intType.createValue( (op1 && op2) ? 1:0);
    }
    public Value logicalOr (Value v1, Value v2, ByteBuffer mem) {
	boolean op1 = v1.getType().getALU(wordSize).getLogicalValue(v1, mem);
	boolean op2 = v2.getType().getALU(wordSize).getLogicalValue(v2, mem);
	return intType.createValue( op1 || op2 ? 1:0);
    }
    public Value logicalNegation(Value v1, ByteBuffer mem) {
	boolean op1 = v1.getType().getALU(wordSize).getLogicalValue(v1, mem);
	return intType.createValue( op1 ? 0:1);
    }     
    public boolean getLogicalValue (Value v1, ByteBuffer mem) {
        throw new InvalidOperatorException
                  (v1.getType(), "getLogicalValue");
    }  
    
    // Assigment expressions.
    public Value plusEqual(Value v1, Value v2) {
	return v1.assign(add(v1, v2));
    }    
    public Value minusEqual(Value v1, Value v2) {
	return v1.assign(subtract(v1, v2));
    }    
    public Value timesEqual(Value v1, Value v2) {
	return v1.assign(multiply(v1, v2));
    }    
    public Value divideEqual (Value v1, Value v2) {
	return v1.assign(divide(v1, v2));
    }    
    public Value modEqual(Value v1, Value v2) {
	return v1.assign(mod(v1, v2));
    }        
    public Value shiftLeftEqual (Value v1, Value v2) {
	return v1.assign(shiftLeft(v1, v2));
    }
    public Value shiftRightEqual (Value v1, Value v2) {
	return v1.assign(shiftRight(v1, v2));
    }
    public Value bitWiseOrEqual (Value v1, Value v2) {
	return v1.assign(bitWiseOr(v1, v2));
    }
    public Value bitWiseXorEqual (Value v1, Value v2) {
	return v1.assign(bitWiseXor(v1, v2));
    }
    public Value bitWiseAndEqual (Value v1, Value v2) {
	return v1.assign(bitWiseAnd(v1, v2));
    }
}