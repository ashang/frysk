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

import java.io.PrintWriter;

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

/**
 * Type decorator class, so that a base or composite type can be
 * decorated with various attributes.
 */

abstract class TypeDecorator extends Type {
	private Type decorated;
	TypeDecorator(String name, Type decorated) {
		super(name, decorated.getSize());
		this.decorated = decorated;
	}

	public String toString() {
		return ("{"
				+ super.toString()
				+ ",decorated=" + decorated.toString()
				+ "}");
	}

	public Type getUltimateType() {
		return decorated.getUltimateType();
	}

	public int getSize() {
		return decorated.getSize();
	}

	void toPrint(PrintWriter writer, Location location,
			ByteBuffer memory, Format format) {
		decorated.toPrint(writer, location, memory, format);
	}
	/**
	 * A guess; sub classes should override.
	 */
	public void toPrint(PrintWriter writer) {
	  if (getUltimateType() instanceof PointerType) {
 	      decorated.toPrint(writer);
 	      writer.print(" " + getName());
 	  }
 	  else {
	      writer.print(getName());
	      writer.print(" ");
	      decorated.toPrint(writer);
	  }
	}
	public Value multiply(Value var1, Value var2) {
		return decorated.multiply(var1, var2);
	}
	public Value divide(Value var1, Value var2) {
		return decorated.divide(var1, var2);
	}
	public Value mod (Value var1, Value var2) {
		return decorated.mod(var1, var2);
	}
	public Value shiftLeft(Value var1, Value var2) {
		return decorated.shiftLeft(var1, var2);
	}
	public Value shiftRight (Value var1, Value var2) {
		return decorated.shiftRight(var1, var2);
	}
	public Value lessThan(Value var1, Value var2) {
		return decorated.lessThan(var1, var2);
	}
	public Value greaterThan(Value var1, Value var2) {
		return decorated.greaterThan(var1, var2);
	}
	public Value lessThanOrEqualTo(Value var1, Value var2) {
		return decorated.lessThanOrEqualTo(var1, var2);
	}
	public Value greaterThanOrEqualTo(Value var1, Value var2) {
		return decorated.greaterThanOrEqualTo(var1, var2);
	}
	public Value equal(Value var1, Value var2) {
		return decorated.equal(var1, var2);
	}
	public Value notEqual(Value var1, Value var2) {
		return decorated.notEqual(var1, var2);
	}
	public Value bitWiseAnd(Value var1, Value var2) {
		return decorated.bitWiseAnd(var1, var2);
	}
	public Value bitWiseXor(Value var1, Value var2) {
		return decorated.bitWiseXor(var1, var2);
	}
	public Value bitWiseOr(Value var1, Value var2) {
		return decorated.bitWiseOr(var1, var2);
	}
	public Value bitWiseComplement(Value var1) {
		return decorated.bitWiseComplement(var1);
	}
	public Value logicalAnd(Value var1, Value var2) {
		return decorated.logicalAnd(var1, var2);
	}
	public Value logicalOr(Value var1, Value var2) {
		return decorated.logicalOr(var1, var2);
	}
	public Value logicalNegation(Value var1) {
		return decorated.logicalNegation(var1);
	} 
	public Value timesEqual(Value var1, Value var2) {
		return decorated.timesEqual(var1, var2);
	}
	public Value divideEqual(Value var1, Value var2) {
		return decorated.divideEqual(var1, var2);
	}
	public Value modEqual(Value var1, Value var2) {
		return decorated.modEqual(var1, var2);
	}
	public Value shiftLeftEqual(Value var1, Value var2) {
		return decorated.shiftLeftEqual(var1, var2);
	}
	public Value shiftRightEqual(Value var1, Value var2) {
		return decorated.shiftRightEqual(var1, var2);
	}
	public Value bitWiseOrEqual(Value var1, Value var2) {
		return decorated.bitWiseOrEqual(var1, var2);
	}
	public Value bitWiseXorEqual(Value var1, Value var2) {
		return decorated.bitWiseXorEqual(var1, var2);
	}
	public Value bitWiseAndEqual(Value var1, Value var2) {
		return decorated.bitWiseAndEqual(var1, var2);
	}
	public Value addressOf (Value var1, ByteOrder order, int wordSize) {
		return decorated.addressOf(var1, order, wordSize);
	}
	public Value dereference (Value var1, ByteBuffer taskMem) {
		return decorated.dereference(var1, taskMem);
	}
	public boolean getLogicalValue(Value var1) {
		return decorated.getLogicalValue(var1);
	}
	void assign(Location location, Value value) {
		decorated.assign(location, value);
	}
	public Type pack(int bitSize, int bitOffset) {
		return decorated.pack(bitSize, bitOffset);
	}
	public Value member(Value var1, String member) {
		return decorated.member(var1, member);
	} 
	public Value index(Value var1, Value var2, ByteBuffer taskMem) {
		return decorated.index(var1, var2, taskMem);
	}       
}
