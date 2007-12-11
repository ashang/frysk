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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import frysk.scopes.LineColPair;

/**
 * Type for a composite object.
 */
public abstract class CompositeType
    extends Type
{
    
    public static class Member{
	private final LineColPair lineColPair;
	final int index;
	final String name;
	final Type type;
	final Access access;
	final boolean inheritance;
	final int bitOffset;
	final int bitSize;
	
	public Member(int index, String name, LineColPair lineColPair, Type type, Access access,int bitOffset, int bitSize,
		boolean inheritance) {
	    this.index = index;
	    this.type = type;
	    this.name = name;
	    this.access = access;
	    this.inheritance = inheritance;
	    this.bitOffset = bitOffset;
	    this.bitSize = bitSize;
	    this.lineColPair = lineColPair;
	}
	
	public String getName(){
	    return this.name;
	}
	
	public LineColPair getLinCol(){
	    return this.lineColPair;
	}
    }
    
    public static class StaticMember extends Member{
	
	public StaticMember(int index, String name, LineColPair lineColPair, Type type, Access access,
		int bitOffset, int bitSize,
		boolean inheritance) {
	    super(index, name, lineColPair, type, access, bitOffset, bitSize, inheritance);
	}

    }
    
    /**
     * Class members; package private.
     */
    public static class DynamicMember extends Member{
	// XXX: To keep getValue working.
	final long offset;
	DynamicMember(int index, String name, LineColPair lineColPair, Type type, long offset, Access access, 
		int bitOffset, int bitSize,
	       boolean inheritance) {
	    super(index, name, lineColPair, type, access, bitOffset, bitSize, inheritance);
	    this.offset = offset;
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
	
	private Value getValue (Value v) {
	    int off = (int)offset;
	    return new Value(type, v.getLocation().slice(off, type.getSize()));
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
    DynamicMember[] members() {
	DynamicMember[] m = new DynamicMember[members.size()];
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

    private CompositeType addMemberToMap(Member member){
	nameToMember.put(member.getName(), member);
	members.add(member);
	return this;
    }
    
    public CompositeType addMember(String name, LineColPair lineColPair, Type type, long offset,
				   Access access) {
	DynamicMember member = new DynamicMember(members.size(),name, lineColPair, type, offset, access, -1, -1,false);
	return addMemberToMap(member);
    }
    
    public CompositeType addBitFieldMember(String name, LineColPair lineColPair, Type type, long offset,
				   Access access, int bitOffset,
				   int bitLength) {
	type = type.pack(bitOffset, bitLength);
	DynamicMember member = new DynamicMember(members.size(),name, lineColPair, type, offset, access, bitOffset, bitLength,false);
	return addMemberToMap(member);
    }
    
    public CompositeType addInheritance(String name, LineColPair lineColPair, Type type, long offset,
					Access access) {
	DynamicMember member = new DynamicMember(members.size(),name, lineColPair, type, offset, access, -1,-1,true);
	return addMemberToMap(member);
    }
    
    public CompositeType addStaticMember(String name, LineColPair lineColPair, Type type, long offset,
		   Access access){
	StaticMember member = new StaticMember(members.size(), name, lineColPair, type,
		   access, -1, -1, false);
	return addMemberToMap(member);
    }
    
    public CompositeType addStaticBitFieldMember(String name, LineColPair lineColPair, Type type, long offset,
		   Access access, int bitOffset,
		   int bitLength) {
	type = type.pack(bitOffset, bitLength);
	StaticMember member = new StaticMember(members.size(), name, lineColPair, type,
		   access, -1, -1, false);
	return addMemberToMap(member);
    }

    /**
     * Iterate through the class types.
     */
    class ClassIterator
	implements java.util.Iterator
    {
	private int idx;
	ClassIterator () {
	    idx = -1;
	}

	public boolean hasNext () {
	    idx += 1;
	    if (idx < members.size())
		return true;
	    return false;
	}

	public String nextName () {
	    return ((DynamicMember)members.get(idx)).name;
	}

	public Object next () {
	    return ((Member)members.get(idx));
	}

	public void remove () {
	}
    }
 
    public ClassIterator iterator () {
	return new ClassIterator();
    }

    
    
    void toPrint(PrintWriter writer, Location location,
		 ByteBuffer memory, Format format, int indentation) {
	if (indentation == 0)
	    indentation = 2;
	String indentPrefix = "";
	for (int indent = 1; indent <= indentation; indent++)
	    indentPrefix = indentPrefix + " ";

	writer.print("{\n");
	for (Iterator i = members.iterator(); i.hasNext();) {
	    DynamicMember member = (DynamicMember)i.next();
	    if (member.type instanceof frysk.value.FunctionType)
		continue;
	    else {
		writer.print(indentPrefix);
		if (member.name != null) {
		    writer.print(member.name);
		    writer.print("=");
		}
		Location loc = location.slice(member.offset,
					      member.type.getSize());
		Value val = new Value(member.type, loc);
		val.toPrint(writer, memory, format, indentation + 2);
		writer.print(",\n");
	    }
	}
	for (int indent = 1; indent <= indentation - 2; indent++)
	    writer.print(" ");
	writer.print("}");
    }

    public void toPrint(PrintWriter writer, int indentation) {
	if (indentation == 0)
	    indentation = 2;
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
	DynamicMember member = null;
	Iterator i = members.iterator();
	// Types this inherits come first; print them out.
	while (i.hasNext()) {
	    member = (DynamicMember)i.next();
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
		writer.print(" ");
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
	    if (member.type instanceof ArrayType) {
		// For handling int x[2]
		printName = false;
		((ArrayType) member.type).toPrint(writer, member.name, indentation + 2);
	    }
	    else if (member.type instanceof PointerType) {
		// For handling int (*x)[2]
		printName = false;
		((PointerType) member.type).toPrint(writer, " " + member.name,
						    indentation + 2);
	    }
	    else
		member.type.toPrint (writer, indentation + 2);
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
		member = (DynamicMember)i.next();
	    else
		member = null;
	}
	for (int indent = 1; indent <= indentation - 2; indent++)
	    writer.print(" ");
	writer.print("}");
    }
    
    public Value member(Value var1, String member)
    {
	DynamicMember mem = (DynamicMember)nameToMember.get(member);
	if (mem == null)
	    throw new RuntimeException("Invalid data member: " + member);
	return mem.getValue (var1);
    }

    public boolean completeMember(String incomplete, List candidates) {
	int completions = 0;
	for (Iterator i = nameToMember.keySet().iterator(); i.hasNext(); ) {
	    String member = (String)i.next();
	    if (member.startsWith(incomplete)) {
		completions++;
		candidates.add(member);
	    }
	}
	return completions > 0;
    }
}
