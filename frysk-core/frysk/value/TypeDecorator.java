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
	    ByteBuffer memory, Format format, int indent) {
	decorated.toPrint(writer, location, memory, format, 0);
    }
    /**
     * A guess; sub classes should override.
     */
    public void toPrint(StringBuilder stringBuilder, int indent) {
	if (getUltimateType() instanceof PointerType) {
	    decorated.toPrint(stringBuilder, 0);
	    stringBuilder.append(" ");
	    stringBuilder.append(getName());
	}
	else {
	    decorated.toPrint(stringBuilder, 0);
	    stringBuilder.insert(0, " ");
	    stringBuilder.insert(0, getName());
	}
    }

    public void toPrintBrief(StringBuilder stringBuilder, int indent) {
	if (getUltimateType() instanceof PointerType
	    || this instanceof ReferenceType) {
	    decorated.toPrintBrief(stringBuilder, 0);
	    stringBuilder.append(" ");
	    stringBuilder.append(getName());
	}
	else {
	    decorated.toPrintBrief(stringBuilder, 0);
	    stringBuilder.insert(0, " ");
	    stringBuilder.insert(0, getName());
	}
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
    public Value slice(Value var1, Value var2, Value var3, ByteBuffer taskMem) {
	return decorated.slice(var1, var2, var3, taskMem);
    }       
}
