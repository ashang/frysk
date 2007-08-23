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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import lib.dwfl.BaseTypes;
import java.io.PrintWriter;

/**
 * Type for a class.
 */
public class ClassType
    extends Type
{
    /**
     * Class members.
     */
    private static class Member {
	// XXX: To keep getValue working.
	final int index;
	final String name;
	final Type type;
	final long offset;
	final int access;
	final int bitOffset;
	final int bitSize;
	Member(int index, String name, Type type, long offset,
	       int access, int bitOffset, int bitSize) {
	    this.index = index;
	    this.type = type;
	    this.name = name;
	    this.offset = offset;
	    this.access = access;
	    this.bitOffset = bitOffset;
	    this.bitSize = bitSize;
	}
	int maskFIXME() {
	    // System V ABI Supplements discuss bit field layout
	    if (bitSize < 0)
		return 0;
	    else {
		return (0xffffffff
			>>> (type.getSize() * 8 - bitSize)
			<< (4 * 8 - bitOffset - bitSize));
	    }
	}
	public String toString() {
	    return ("{"
		    + "index=" + index
		    + ",name=" + name
		    + ",type=" + type
		    + ",offset=" + offset
		    + ",access=" + access
		    + ",bitOffset=" + bitOffset
		    + ",bitSize=" + bitSize
		    + ",mask()=" + Integer.toHexString(maskFIXME())
		    + "}");
	}
    }
    /**
     * A mapping from NAME to Member; only useful for named members.
     */
    private final Map nameToMember = new HashMap();
    /**
     * A list of all members, in insertion order.
     */
    private final ArrayList members = new ArrayList();

    // DW_AT_inheritance
    private boolean inheritance;
  
    /**
     * Create an ClassType
     * 
     * @param endian - Endianness of class
     * @param name TODO
     */
    public ClassType (ByteOrder endian, String name) {
	super(0, endian, 0, name);
	inheritance = false;
    }

    /**
     * Dump the contents of this object.
     */
    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("{");
	buf.append(super.toString());
	buf.append(",inheritance=" + inheritance);
	buf.append(",members={");
	for (Iterator i = members.iterator(); i.hasNext(); ) {
	    buf.append(i.next().toString());
	}
	buf.append("}}");
	return buf.toString();
    }

    /**
     * Add NAME as a member of the class.
     */
    public ClassType addMember(String name, Type type, long offset,
			       int access) {
	return addMember(name, type, offset, access, -1, -1);
    }
    public ClassType addMember(String name, Type type, long offset,
			       int access, int bitOffset, int bitLength) {
	Member member = new Member(members.size(), name, type, offset,
				   access, bitOffset, bitLength);
	nameToMember.put(name, member);
	members.add(member);
	return this;
    }

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
	    if (idx < members.size())
		return true;
	    return false;
	}

	public String nextName () {
	    return ((Member)members.get(idx)).name;
	}

	public Object next () {
	    return getValue(v, idx);
	}

	public void remove () {
	}
    }
    private Value getValue (Value v, int idx)
    {
	Member member = (Member)(members.get(idx));
	Type type = member.type;
	int off = (int)member.offset;

	switch (type.getTypeIdFIXME()) {
	case BaseTypes.baseTypeByte:
	    return ((ArithmeticType)type).createValue(v.getByte(off));
	case BaseTypes.baseTypeShort:
	    return ((ArithmeticType)type).createValue(v.getShort(off));
	case BaseTypes.baseTypeInteger:
	    int mask = member.maskFIXME();
	    if (mask != 0)
		return bitValueFIXME(v.getLocation(), member);
	    int val = v.getInt(off);
	    return ((ArithmeticType)type).createValue(val);
	case BaseTypes.baseTypeLong:
	    return ((ArithmeticType)type).createValue(v.getLong(off));
	case BaseTypes.baseTypeFloat:
	    return ((ArithmeticType)type).createValue(v.getFloat(off));
	case BaseTypes.baseTypeDouble:
	    return ((ArithmeticType)type).createValue(v.getDouble(off));
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
        return new Value(new UnknownType(type.name), type.name);
    }
    
    public ClassIterator iterator (Value v) {
	return new ClassIterator(v);
    }

    public Value get (Value v, int componentsIdx, ArrayList components) {
	while (componentsIdx < components.size()) {
	    String component = (String)components.get(componentsIdx);
	    Member member = (Member)nameToMember.get(component);
	    if (member != null) {
		// XXX: What about the null case?  Just iterates :-/
		v = getValue (v, member.index);
		if (v.getType() instanceof ClassType)
		    return ((ClassType)v.getType()).get(v, componentsIdx, components);
		else if (v.getType() instanceof ArrayType)
		    v = ((ArrayType)v.getType()).get(v, ++componentsIdx, components);
	    }
	    componentsIdx += 1;
	}
	return v;
    }
    
    /**
     * This bit manipulation should be pushed into Location.
     */
    private Value bitValueFIXME(Location location, Member member) {
	int val = location.getInt(member.type.getEndian(), (int)member.offset);
	int mask = member.maskFIXME();
	int shift = 0;
	for (int tmpMask = mask;
	     (tmpMask & 0x1) == 0;
	     tmpMask = tmpMask>>>1) {
	    shift += 1;
	}
	int res = (val & mask) >>> shift;
	return ((ArithmeticType)member.type).createValue(res);
    }

    void toPrint(PrintWriter writer, Location location, ByteBuffer memory,
		 Format format) {
	writer.print("{");
	boolean first = true;
	for (Iterator i = members.iterator(); i.hasNext();) {
	    Member member = (Member)i.next();
	    if (member.type instanceof frysk.value.FunctionType)
		continue;
	    else {
		if (first)
		    first = false;
		else
		    writer.print(" ");
		Value val;
		if (member.maskFIXME() != 0) {
		    val = bitValueFIXME(location, member);
		} else {
		    Location loc = location.slice(member.offset,
						  member.type.getSize());
		    val = new Value(member.type, member.name, loc);
		}
		if (member.name != null) {
		    writer.print(member.name);
		    writer.print("=");
		}
		val.toPrint(writer, memory, format);
		writer.print(",\n");
	    }
	}
	writer.print("}");
    }

    public void toPrint(PrintWriter writer) {
	if (this.isTypedef && this.name != null && this.name.length() > 0) {
	    writer.print(this.name);
	    return;
	}
	boolean first = true;
	Member member = null;
	Iterator i = members.iterator();
	// Types this inherits come first; print them out.
	while (i.hasNext()) {
	    member = (Member)i.next();
	    if (!(member.type instanceof frysk.value.ClassType)
		|| !((ClassType)(member.type)).inheritance)
		break;
	    if (first)
		first = false;
	    else
		writer.print(", ");
	    switch (member.access) {
	    case 1: writer.print("public "); break;
	    case 2: writer.print("protected "); break;
	    case 3: writer.print("private "); break;
	    }
	    writer.print(member.type.name);
	    member = null;
	}
	int previousAccess = 0;
	writer.print(" {\n");
	while (member != null) {
	    if (member.access != previousAccess) {
		previousAccess = member.access;
		switch (member.access) {
		case 1: writer.print("  public:\n"); break;
		case 2: writer.print("  protected:\n"); break;
		case 3: writer.print("  private:\n"); break;
		}
	    }
	    writer.print("  ");
	    if (member.type.isTypedef())
		writer.print(member.type.name);
	    else
		member.type.toPrint(writer);
	    if (!(member.type instanceof frysk.value.FunctionType)) {
		writer.print(" ");
		writer.print(member.name);
	    }
	    if (member.bitSize > 0) {
		writer.print(":");
		writer.print(member.bitSize);
	    }
	    writer.print(";\n  ");
	    // Advance
	    if (i.hasNext())
		member = (Member)i.next();
	    else
		member = null;
	}
	writer.print("}");
    }

    public void setSize (int size) {
	this.size = size;
    }
  
    public void setInheritance(boolean inheritance) {
	this.inheritance = inheritance;
    }
}
