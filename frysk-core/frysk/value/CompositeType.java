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

import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.LocationExpression;
import frysk.debuginfo.PieceLocation;
import frysk.isa.ISA;
import frysk.scopes.SourceLocation;

/**
 * Type for a composite object.
 */
public abstract class CompositeType
    extends Type
{
    
    public static abstract class Member extends ObjectDeclaration {
	
	private final SourceLocation sourceLocation;
	final int index;
	final String name;
	final Type type;
	final Access access;
	final boolean inheritance;
	final int bitOffset;
	final int bitSize;
	
	public Member(int index, String name, SourceLocation sourceLocation, Type type, Access access,int bitOffset, int bitSize,
		boolean inheritance) {
	    this.index = index;
	    this.type = type;
	    this.name = name;
	    this.access = access;
	    this.inheritance = inheritance;
	    this.bitOffset = bitOffset;
	    this.bitSize = bitSize;
	    this.sourceLocation = sourceLocation;
	}
	
	public String getName(){
	    return this.name;
	}
	

	public Type getType(ISA isa) {
	    return this.type;
	}

	public abstract Value getValue(DebugInfoFrame frame);
	
	public SourceLocation getSourceLocation(){
	    return sourceLocation;
	}
    }
    
    public static class StaticMember extends Member{
	private final LocationExpression locationExpression;
	public StaticMember(LocationExpression locationExpression, int index, String name, SourceLocation sourceLocation, Type type, Access access,
		int bitOffset, int bitSize,
		boolean inheritance) {
           super(index, name, sourceLocation, type, access, bitOffset, bitSize, inheritance);
           this.locationExpression = locationExpression;
       }
	     
	public Value getValue(DebugInfoFrame frame) {
	    ISA isa = frame.getTask().getISA();
	    PieceLocation pieceLocation
	    = new PieceLocation(locationExpression.decode(frame, this.getType(isa)
	                                                                     .getSize()));
	    Value value = new Value(this.getType(isa), pieceLocation);
	    return value;
	}

    }
    
    /**
     * Class members; package private.
     */
    public static class DynamicMember extends Member{
	// XXX: To keep getValue working.
	final long offset;
	DynamicMember(int index, String name, SourceLocation sourceLocation, Type type, long offset, Access access, 
		int bitOffset, int bitSize,
	       boolean inheritance) {
	    super(index, name, sourceLocation, type, access, bitOffset, bitSize, inheritance);
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

	/**
	 * @deprecated
	 */
	public Value getValue(DebugInfoFrame frame) {
	    //this should not be used. without an instance non static members
	    // do not have a value.
	    throw new NullPointerException("trying to get a value of a non static member");
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
    
    public CompositeType addMember(String name, SourceLocation sourceLocation, Type type, long offset,
				   Access access) {
	DynamicMember member = new DynamicMember(members.size(),name, sourceLocation, type, offset, access, -1, -1,false);
	return addMemberToMap(member);
    }
    
    public CompositeType addBitFieldMember(String name, SourceLocation sourceLocation, Type type, long offset,
				   Access access, int bitOffset,
				   int bitLength) {
	type = type.pack(bitOffset, bitLength);
	DynamicMember member = new DynamicMember(members.size(),name, sourceLocation, type, offset, access, bitOffset, bitLength,false);
	return addMemberToMap(member);
    }
    
    public CompositeType addInheritance(String name, SourceLocation sourceLocation, Type type, long offset,
					Access access) {
	DynamicMember member = new DynamicMember(members.size(),name, sourceLocation, type, offset, access, -1,-1,true);
	return addMemberToMap(member);
    }
    
    public CompositeType addStaticMember(LocationExpression locationExpression, String name, SourceLocation sourceLocation, Type type, long offset,
		   Access access){
	StaticMember member = new StaticMember(locationExpression, members.size(), name, sourceLocation, type,
		   access, -1, -1, false);
	return addMemberToMap(member);
    }
    
    public CompositeType addStaticBitFieldMember(LocationExpression locationExpression, String name, SourceLocation sourceLocation, Type type, long offset,
		   Access access, int bitOffset,
		   int bitLength) {
	type = type.pack(bitOffset, bitLength);
	StaticMember member = new StaticMember(locationExpression, members.size(), name, sourceLocation, type,
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

    public void toPrint(StringBuilder stringBuilderParm, int indentation) {
	if (indentation == 0)
	    indentation = 2;
	String indentPrefix = "";
	for (int indent = 1; indent <= indentation; indent++)
	    indentPrefix = indentPrefix + " ";

	// {class,union,struct} NAME
	StringBuilder stringBuilder = new StringBuilder();
	stringBuilder.append(getPrefix());
	if (getName() != null && getName().length() > 0) {
	    stringBuilder.append(" ");
	    stringBuilder.append(getName());
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
		stringBuilder.append(" : ");
		first = false;
	    } else {
		stringBuilder.append(", ");
	    }
	    if (member.access != null) {
		stringBuilder.append(member.access.toPrint());
		stringBuilder.append(" ");
	    }
	    stringBuilder.append(member.type.getName());
	    member = null;
	}
	// { content ... }
	Access previousAccess = null;
	stringBuilder.append(" {\n");

	StringBuilder memberStringBuilder = new StringBuilder();
	while (member != null) {
	    if (member.access != previousAccess) {
		previousAccess = member.access;
		if (member.access != null) {
		    stringBuilder.append(" ");
		    stringBuilder.append(member.access.toPrint());
		    stringBuilder.append(":\n");
		}
	    }
	    memberStringBuilder.delete(0, memberStringBuilder.length());
	    memberStringBuilder.append(" " + member.name);
	    member.type.toPrint(memberStringBuilder, indentation + 2);
	    memberStringBuilder.insert(0, indentPrefix);
	    stringBuilder.append(memberStringBuilder);
	    if (member.bitSize > 0) {
		stringBuilder.append(":");
		stringBuilder.append(member.bitSize);
	    }
	    stringBuilder.append(";\n");
	    // Advance
	    if (i.hasNext())
		member = (DynamicMember)i.next();
	    else
		member = null;
	}
	for (int indent = 1; indent <= indentation - 2; indent++)
	    stringBuilder.append(" ");
	stringBuilder.append("}");
	stringBuilderParm.insert(0, stringBuilder);
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
    
    public ObjectDeclaration getDeclaredObjectByName(String name){
	return (ObjectDeclaration) nameToMember.get(name);
    }
	 
}
