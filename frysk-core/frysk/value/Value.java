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

import lib.dwfl.BaseTypes;
import inua.eio.ByteBuffer;

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
 
    public Value(Type type, String textFIXME)
    {
      this(type, textFIXME, (new Location(type.getSize())));
    }

    public Value(Type type, String textFIXME, ByteBuffer byteBuffer) 
    {
      this(type, textFIXME, (new Location(byteBuffer)));
    }    

    public Value(Type type, String textFIXME, Location location)
    {
       this.type = type;
       this.location = location;
       this.textFIXME = textFIXME;
    }


    public Location getLocation()
    {
      return location;
    }

    public Type getType()
    {
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
    public byte getByte() {
      return location.getByte();
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public char getChar() {
      return (char)location.getShort();
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public short getShort() {
      return location.getShort();
    }
    
    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public int getInt() {
      return location.getInt();
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public long getLong() {
      return location.getLong();
    }
    
    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public float getFloat() {
      return location.getFloat();
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public double getDouble() {
      return location.getDouble();
    }
    
    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public byte getByte(int idx) {
      return (byte)location.getByte(idx);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public char getChar(int idx) {
      return (char)location.getShort(idx);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public short getShort(int idx) {
      return location.getShort(idx);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public int getInt(int idx) {
      return location.getInt(idx);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public long getLong(int idx) {
      return location.getLong(idx);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public float getFloat(int idx) {
      return location.getFloat(idx);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public double getDouble(int idx) {
      return location.getDouble(idx);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public void putByte(byte val) {
      location.putByte(val);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public void putChar(char val) {
      location.putShort((short)val);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public void putShort(short val) {
      location.putShort(val);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public void putInt(int val) {
      location.putInt(val);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public void putLong(long val) {
      location.putLong(val);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public void putFloat(float val) {
      location.putFloat(val);
    }

    /**
     * FIXME: Do not use this.  Get the location and then use it's
     * methods.
     */
    public void putDouble(double val) {
      location.putDouble(val);
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
	switch ((int)location.getByteBuffer().capacity()) {
	case 1:
	    return location.getByte();
	case 2:
	    return location.getShort();
	case 4:
	    return location.getInt();
    	case 8:
	    return location.getLong();
	default:
	    return 0;
	}
    }
    
    /**
     * FIXME: Do not use this as it is assuming that Java's
     * floating-point double matches that of the host.
     */
    public double doubleValue()
    {
      switch (type.getTypeId())
      {
	case BaseTypes.baseTypeByte:
	  return location.getByte();
	case BaseTypes.baseTypeShort:
       	  return location.getShort();
	case BaseTypes.baseTypeInteger:
	  return location.getInt();
	case BaseTypes.baseTypeLong:
	  return location.getLong();
	case BaseTypes.baseTypeFloat:
	  return location.getFloat();
	case BaseTypes.baseTypeDouble:
	  return location.getDouble();
	default:
	  return 0;
      }
    }

    /**
     * FIXME: Do not use this.
     */
    public String toString(ByteBuffer b) {
	return type.toString(this, b);
    }
    
    public String toString()  {
      return type.toString(this);
    }

    /**
     * Write THIS value to WRITER, formatted according to FORMAT.
     */
    public void toPrint(PrintWriter writer, ByteBuffer memory,
			Format format) {
	// XXX: Shouldn't this be location?
	type.toPrint(writer, this, memory, format);
    }
}
