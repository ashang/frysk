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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.PrintWriter;

/**
 * Type for a composite object.
 */
public abstract class CompositeType
    extends Type
{
    /**
     * Class members; package private.
     */
    static class Member {
	// XXX: To keep getValue working.
	final int index;
	final String name;
	final Type type;
	final long offset;
	final Access access;
	final int bitOffset;
	final int bitSize;
	final boolean inheritance;

	Member(int index, String name, Type type, long offset,
	       Access access, int bitOffset, int bitSize, 
	       boolean inheritance) {
	    this.index = index;
	    this.type = type;
	    this.name = name;
	    this.offset = offset;
	    this.access = access;
	    this.bitOffset = bitOffset;
	    this.bitSize = bitSize;
	    this.inheritance = inheritance;
	}

	public String toString() {
	    return ("{"
		    + "index=" + index
		    + ",name=" + name
		    + ",type=" + type
		    + ",offset=" + offset
		    + ",inheritance=" + inheritance
		    + ",access=" + access
		    + ",bitOffset=" + bitOffset
		    + ",bitSize=" + bitSize
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
    /**
     * Return the composit's members as an array.  PACKAGE PRIVATE.
     */
    Member[] members() {
	Member[] m = new Member[members.size()];
	members.toArray(m);
	return m;
    }

    /**
     * Create a composite type.
     *
     * XXX: only allow package-local extension.
     */
    CompositeType(String name, int size) {
	super(name, size);
    }

    /**
     * Return the prefix (class, union, struct), or null.
     */
    abstract protected String getPrefix();

    /**
     * Dump the contents of this object.
     */
    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("{");
	buf.append(super.toString());
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
    private CompositeType add(String name, Type type, long offset,
			      Access access, int bitOffset, int bitLength,
			      boolean inheritance) {
	if (bitOffset >= 0 && bitLength > 0)
	    type = type.pack(bitOffset, bitLength);
	Member member = new Member(members.size(), name, type, offset,
				   access, bitOffset, bitLength, inheritance);
	nameToMember.put(name, member);
	members.add(member);
	return this;
    }

    public CompositeType addMember(String name, Type type, long offset,
				   Access access) {
	return add(name, type, offset, access, -1, -1, false);
    }

    public CompositeType addMember(String name, Type type, long offset,
				   Access access, int bitOffset,
				   int bitLength) {
	return add(name, type, offset, access, bitOffset, bitLength, false);
    }

    public CompositeType addInheritance(String name, Type type, long offset,
				        Access access) {
	return add(name, type, offset, access, -1, -1, true);
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
	    idx = -1;
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

    private Value getValue (Value v, int idx) {
	Member member = (Member)(members.get(idx));
	Type type = member.type;
	int off = (int)member.offset;
	return new Value(type, v.getLocation().slice(off, type.getSize()));
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
		if (v.getType() instanceof CompositeType)
		    return ((CompositeType)v.getType()).get(v, componentsIdx,
			    components);
		else if (v.getType() instanceof ArrayType)
		    v = ((ArrayType)v.getType()).get(v, ++componentsIdx,
			    components);
	    }
	    componentsIdx += 1;
	}
	return v;
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
		if (member.name != null) {
		    writer.print(member.name);
		    writer.print("=");
		}
		Location loc = location.slice(member.offset, 
					      member.type.getSize());
		Value val = new Value(member.type, loc);
		val.toPrint(writer, memory, format);
		writer.print(",\n");
	    }
	}
	writer.print("}");
    }

    public void toPrint(int indentation, PrintWriter writer) {
	// FIXME: going away.
	if (this.isTypedef() && this.getName() != null
	    && this.getName().length() > 0) {
	    writer.print(this.getName());
	    return;
	}

	String indentPrefix = "";
	for (int indent = 1; indent <= indentation; indent++)
	    indentPrefix = indentPrefix + " ";

	// {class,union,struct} NAME
	writer.print(getPrefix());
	if (getName() != null && getName().length() > 0) {
	    writer.print(" ");
	    writer.print(getName());
	}
	// : public PARENT ...
	boolean first = true;
	Member member = null;
	Iterator i = members.iterator();
	// Types this inherits come first; print them out.
	while (i.hasNext()) {
	    member = (Member)i.next();
	    if (!member.inheritance)
		break;
	    if (first) {
		writer.print(" : ");
		first = false;
	    } else {
		writer.print(", ");
	    }
	    if (member.access != null) {
		writer.print(member.access.toPrint());
	    }
	    writer.print(member.type.getName());
	    member = null;
	}
	// { content ... }
	Access previousAccess = null;
	writer.print(" {\n");

	while (member != null) {
	    boolean printName = true;
	    if (member.access != previousAccess) {
		previousAccess = member.access;
		if (member.access != null) {
		    writer.print(" ");
		    writer.print(member.access.toPrint());
		    writer.print(":\n");
		}
	    }
	    writer.print(indentPrefix);
	    
	    if (member.type instanceof TypeDef)
		writer.print(member.type.getName());
	    else if (member.type instanceof CompositeType)
		((CompositeType) member.type).toPrint(indentation + 2, writer);
	    else if (member.type instanceof ArrayType) {
		printName = false;
		((ArrayType) member.type).toPrint(member.name, writer);
	    }
	    else if (member.type instanceof PointerType) {
		printName = false;
		((PointerType) member.type).toPrint(" " + member.name, writer);
	    }
	    else
		member.type.toPrint(writer);
	    if (member.type instanceof frysk.value.FunctionType)
		printName = false;
	    if (printName) {
		writer.print(" ");
		writer.print(member.name);
	    }
	    if (member.bitSize > 0) {
		writer.print(":");
		writer.print(member.bitSize);
	    }
	    writer.print(";\n");
	    // Advance
	    if (i.hasNext())
		member = (Member)i.next();
	    else
		member = null;
	}
	for (int indent = 1; indent <= indentation - 2; indent++)
	    writer.print(" ");
	writer.print("}");
    }
    
    public void toPrint( PrintWriter writer) {
	this.toPrint(2, writer);
    }
    public Value member(Value var1, String member)
    {
	Member mem = (Member)nameToMember.get(member);
	if (mem == null)
	    throw new RuntimeException("Invalid data member: " + member);
	return getValue (var1, mem.index);
    }
}
