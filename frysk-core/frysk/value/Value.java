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
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Stores the type and location of a variable
 */

public class Value
{
    private final Type type;
    private final Location location;
    private final String textFIXME;

    public Value(Type type, Location location) {
	this.type = type;
	this.location = location;
	this.textFIXME = "textFIXME";
    }
 
    /**
     * FIXME: The string parameter, which is being used to track
     * location using <<by name>>, is being removed.  Try to use
     * Value(Type,Location).
     */
    public Value(Type type, String textFIXME) {
	this(type, textFIXME, new Location(new byte[type.getSize()]));
    }
    /**
     * FIXME: The string parameter, which is being used to track
     * location using <<by name>>, is being removed.  Try to use
     * Value(Type,Location).
     */
    public Value(Type type, String textFIXME, ByteBuffer byteBuffer) {
	this(type, textFIXME, (new Location(byteBuffer)));
    }    
    /**
     * FIXME: The string parameter, which is being used to track
     * location using <<by name>>, is being removed.  Try to use
     * Value(Type,Location).
     */
    public Value(Type type, String textFIXME, Location location) {
	this.type = type;
	this.location = location;
	this.textFIXME = textFIXME;
    }

    /**
     * Return the Value's Location.
     */
    public Location getLocation() {
	return location;
    }

    /**
     * Return the Value's Type.
     */
    public Type getType() {
	return type;
    }

    /**
     * Return what is probably the variable's name.
     *
     * XXX: This dates back to when Variable and Value were the same
     * class.  Code should instead ask the Variable for it's name.
     */
    public String getTextFIXME() {
	return textFIXME;
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public void putFloat(float val) {
	location.putFloat(type.getEndian(), val);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public void putDouble(double val) {
	location.putDouble(type.getEndian(), val);
    }

    /**
     * Assign VALUE to THIS; perform type conversion if necessary.
     */
    public Value assign(Value value) {
	type.assign(location, value);
	return this;
    }

    /**
     * Quick and dirty conversion of the value into a long.  Useful
     * for code wanting to use the raw value as a pointer for
     * instance.
     *
     * Code wanting to access the value's raw bytes using different
     * formats should get the location and manipulate that; and
     * shouldn't add more methods here.
     */
    public long asLong() {
	return ((ArithmeticType)type).getBigInteger(location).longValue();
    }
    
    /**
     * Quick and dirty conversion to a floating-point.
     */
    public double doubleValue() {
	return ((ArithmeticType)type).bigFloatValue(location).doubleValue();
    }

    /**
     * Dump this object into a string.
     */
    public String toString()  {
	StringBuffer b = new StringBuffer();
	b.append("{");
	b.append(super.toString());
	b.append(",location=");
	b.append(location.toString());
	b.append(",type=");
	b.append(type.toString());
	b.append("}");
	return b.toString();
    }

    /**
     * Return this as a printable string; if need be and when
     * non-NULL, use MEMORY for dereferencing pointers.
     */
    public String toPrint(Format format, ByteBuffer memory) {
	StringWriter stringWriter = new StringWriter();
	PrintWriter writer = new PrintWriter(stringWriter);
	toPrint(writer, memory, format);
	return stringWriter.toString();
    }
    /**
     * Return this as a printable string using the default formatting.
     */
    public String toPrint() {
	return toPrint(Format.NATURAL, null/*memory*/);
    }
    /**
     * Return this as a printable string using the specified FORMAT.
     */
    public String toPrint(Format format) {
	return toPrint(format, null/*memory*/);
    }

    /**
     * Write THIS value to WRITER, formatted according to FORMAT.
     */
    public void toPrint(PrintWriter writer, ByteBuffer memory,
			Format format) {
	type.toPrint(writer, location, memory, format);
    }
}
