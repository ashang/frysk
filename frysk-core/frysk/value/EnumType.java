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

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Iterator;

/**
 * Type for an enum.
 */
public class EnumType extends IntegerTypeDecorator
{
    private static class Member {
	final String name;
	final BigInteger value;
	Member(String name, BigInteger value) {
	    this.name = name;
	    this.value = value;
	}
    }
    
    // Mapping from a Value to a member (ordered by Value (a big
    // integer).
    private SortedMap valueToMember = new TreeMap();

    // Should Format be made responsible?  So that HEX would override
    // printing the enum's name and print raw hex instead?
    void toPrint (PrintWriter writer, Location location,
		  ByteBuffer memory, Format format) {
	BigInteger value = getBigInteger(location);
	Member map = (Member)valueToMember.get(value);
	if (map != null)
	    writer.print(map.name);
	else
	    // Let the integer formater decide.
	    format.print(writer, location, this);
    }

    public void toPrint(PrintWriter writer) {
	writer.print("enum ");
	writer.print("{");
	boolean first = true;
	for (Iterator i = valueToMember.values().iterator(); i.hasNext();) {
	    Member m = (Member)i.next();
	    if (first)
		first = false;
	    else
		writer.print(",");
	    writer.print(m.name);
	    writer.print("=");
	    writer.print(m.value.toString());
	}
	writer.print("}");
    }
  
    private EnumType(ByteOrder order, int size, IntegerType accessor) {
	super("enum", order, size, accessor);
    }
    protected Type clone(IntegerType accessor) {
	return new EnumType(order(), getSize(), accessor);
    }
    /**
     * Create an Enum.
     */
    public EnumType(ByteOrder order, int size) {
	this(order, size, new SignedType("enum", order, size));
    }

    public EnumType addMember (String name, long l) {
	BigInteger value = BigInteger.valueOf(l);
	valueToMember.put(value, new Member(name, value));
	return this;
    }
}
