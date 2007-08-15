// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, Red Hat Inc.
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

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import java.util.ArrayList;
import lib.dwfl.BaseTypes;

/**
 * Type for a class.
 */
public class ClassType
    extends Type
{
    ArrayList types;	// Type of member

    ArrayList names;	// String
  
    ArrayList offsets;	// Long offset into class
  
    ArrayList masks;	// Integer mask for bitfields
  
    ArrayList baseClass;
  
    ArrayList accessibility;	// DW_AT_accessibility

    boolean inheritance;	// DW_AT_inheritance
  
    /**
     * Iterate through the class types.
     */
    class ClassIterator
	implements java.util.Iterator
    {
	private int idx;

	Value v;

	ClassIterator (Value v) {
	    idx = - 1;
	    this.v = v;
	}

	public boolean hasNext () {
	    idx += 1;
	    if (idx < types.size())
		return true;
	    return false;
	}

	public String nextName () {
	    return (String) names.get(idx);
	}

	public Object next () {
	    return getValue(v, idx);
	}

	public void remove () {
	}
    }

    private Value getValue (Value v, int idx)
    {
	Type type = ((Type) (types.get(idx)));
	int off = ((Long)offsets.get(idx)).intValue();

	switch (((Type)types.get(idx)).typeId) {
	case BaseTypes.baseTypeByte:
	    return ArithmeticType.newByteValue((ArithmeticType)type, v.getByte(off));
	case BaseTypes.baseTypeShort:
	    return ArithmeticType.newShortValue((ArithmeticType)type, v.getShort(off));
	case BaseTypes.baseTypeInteger:
	    int val = v.getInt(off);
	    int mask = ((Integer)masks.get(idx)).intValue();
	    if (mask != 0) {
		int shift = 0;
		int tmpMask = mask;
		// ??? substitute numberOfTrailingZeros() for 1.5
		while ((tmpMask & 0x1) == 0) {
		    shift += 1;
		    tmpMask = tmpMask >>> 1;
		}
		val = (val & mask) >>> shift;
	    }
	    return ArithmeticType.newIntegerValue((ArithmeticType)type, val);
	case BaseTypes.baseTypeLong:
	    return ArithmeticType.newLongValue((ArithmeticType)type, v.getLong(off));
	case BaseTypes.baseTypeFloat:
	    return ArithmeticType.newFloatValue((ArithmeticType)type, v.getFloat(off));
	case BaseTypes.baseTypeDouble:
	    return ArithmeticType.newDoubleValue((ArithmeticType)type, v.getDouble(off));
	}
	if (type instanceof ClassType) {
	    ByteBuffer abb = v.getLocation().getByteBuffer().slice(off,type.size);
	    abb.order(type.getEndian());
	    return new Value((ClassType)type, type.name, abb);
	} else if (type instanceof ArrayType) {
	    ByteBuffer abb = v.getLocation().getByteBuffer().slice(off,type.size);
	    abb.order(type.getEndian());
	    return new Value((ArrayType)type, type.name, abb);
	} else if (type instanceof PointerType) {
	    ByteBuffer abb = v.getLocation().getByteBuffer().slice(off,type.size);
	    return new Value((PointerType)type, type.name, abb);
	} else if (type instanceof FunctionType) {
	    ByteBuffer abb = v.getLocation().getByteBuffer().slice(off,type.size);
	    return new Value((FunctionType)type, type.name, abb);
	}
	return null;
    }
    
    public ClassIterator iterator (Value v) {
	return new ClassIterator(v);
    }

    public Value get (Value v, int componentsIdx, ArrayList components) {
	while (componentsIdx < components.size()) {
	    String component = (String)components.get(componentsIdx);
	    for (int i = 0; i < names.size(); i++) {
		if (((String)names.get(i)).equals(component)) {
		    v = getValue (v, i);
		    if (v.getType() instanceof ClassType)
			return ((ClassType)v.getType()).get(v, componentsIdx, components);
		    else if (v.getType() instanceof ArrayType)
			v = ((ArrayType)v.getType()).get(v, ++componentsIdx, components);
		}
	    }
	    componentsIdx += 1;
	}
	return v;
    }
    
    public String toString (Value v, ByteBuffer b) {
	StringBuffer strBuf = new StringBuffer();
	ClassIterator e = iterator(v);
	strBuf.append("{");
	while (e.hasNext()) {
	    Value val = (Value)e.next();
	    if (val.getType() instanceof frysk.value.FunctionType == true)
		continue;
	    else {
		strBuf.append(e.nextName() + "=");
		Type valType = val.getType();
		strBuf.append(valType.toString(val, b) + ",\n ");
	    }
	}
	strBuf.replace(strBuf.length()-1, strBuf.length(), "}");
	return strBuf.toString();
    }
  
    public String getName () {
	StringBuffer strBuf = new StringBuffer();
	if (this.isTypedef && this.name != null && this.name.length() > 0) {
	    strBuf.append(this.name);
	    return strBuf.toString();
	}
	boolean putBrace = true;
	int access, previousAccess = 0;
	for (int i = 0; i < this.types.size(); i++) {
	    Type memberType = ((Type)this.types.get(i));
	    access = ((Integer)this.accessibility.get(i)).intValue();
	    if (memberType instanceof frysk.value.ClassType == true
		&& ((ClassType)memberType).inheritance == true) {
		switch (access) {
		case 1: strBuf.append("public "); break;
		case 2: strBuf.append("protected "); break;
		case 3: strBuf.append("private "); break;
		}
		strBuf.append(memberType.name + ", ");
		continue;
	    }
	    if (putBrace) {
		if (strBuf.length() > 5)	// rewind to extra ", "
		    strBuf.delete(strBuf.length()-2, strBuf.length());
		strBuf.append(" {\n  ");
		putBrace = false;
	    }
	    if (access != previousAccess) {
		previousAccess = access;
		switch (access) {
		case 1: strBuf.append("public:\n  "); break;
		case 2: strBuf.append("protected:\n  "); break;
		case 3: strBuf.append("private:\n  "); break;
		}
	    }
	    if (memberType.isTypedef())
		strBuf.append(memberType.name);
	    else
		strBuf.append(memberType.getName());
	    if (memberType instanceof frysk.value.FunctionType == false)
		strBuf.append(" " + (String) this.names.get(i));
	    int mask = ((Integer)this.masks.get(i)).intValue();
	    int bitCount = 0;
	    // ??? substitute numberOfBits() for 1.5
	    while (mask != 0) {
		if ((mask & 0x1) == 1)
		    bitCount += 1;
		mask = mask >>> 1;
	    }
	    if (bitCount > 0)
		strBuf.append(":" + bitCount);
	    strBuf.append(";\n  ");
	}
	strBuf.replace(strBuf.length(), strBuf.length(), "}");
	return strBuf.toString();
    }

    /**
     * Create an ClassType
     * 
     * @param endian - Endianness of class
     * @param name TODO
     */
    public ClassType (ByteOrder endian, String name) {
	super(0, endian, 0, name);
	types = new ArrayList();
	names = new ArrayList();
	offsets = new ArrayList();
	masks = new ArrayList();
	baseClass = new ArrayList();
	accessibility = new ArrayList();
	inheritance = false;
    }

    public void addMember (Type member, String name, long offset, int mask,
			   int access) {
	types.add(member);
	names.add(name);
	offsets.add(new Long(offset));
	masks.add(new Integer(mask));
	baseClass.add(new Boolean(false));
	accessibility.add(new Integer(access));
    }

    public void setBaseClass () {
	baseClass.add(new Boolean(true));
    }
  
    public void setSize (int size) {
	this.size = size;
    }
  
    public void setInheritance(boolean inheritance) {
	this.inheritance = inheritance;
    }

    public Value add (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value subtract (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value logicalNegation(Value var1) 
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }
  
    public Value assign (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value timesEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value divideEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value minusEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value plusEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value modEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value shiftLeftEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value shiftRightEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseAndEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseOrEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseXorEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value multiply (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value divide (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value mod (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value shiftLeft (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value shiftRight (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value lessThan (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value greaterThan (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value greaterThanOrEqualTo (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value lessThanOrEqualTo (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value equal (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value notEqual (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseAnd (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseOr (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseXor (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value bitWiseComplement (Value var1)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value logicalAnd (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public Value logicalOr (Value var1, Value var2)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

    public boolean getLogicalValue (Value var1)
	throws InvalidOperatorException
    {
	throw (new InvalidOperatorException());
    }

}
