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
import java.io.PrintWriter;

/**
 * Holds the type of a Variable and also defines possible operations. Classes
 * extended from this type will have to define the individual operation that are
 * defined on those types. e.g. addition operation may be defined for the
 * integer type.
 */

public abstract class Type
{
  protected int size;

  protected final ByteOrder endian;
  
  protected final int typeId;

  protected final String name;
  
  protected boolean isTypedef;

  Type (int size, ByteOrder endian, int typeId)
  {
    this(size, endian, typeId, "", false);
  }

  Type (int size, ByteOrder endian, int typeId, String name)
  {
    this(size, endian, typeId, name, false);
  }

  Type (int size, ByteOrder endian, int typeId, String name, boolean typedef)
  {
    this.size = size;
    this.endian = endian;
    this.typeId = typeId;
    this.name = name;
    this.isTypedef = false;
  }

  public int getSize ()
  {
    return size;
  }

  public ByteOrder getEndian ()
  {
    return endian;
  }

  public int getTypeId ()
  {
    return typeId;
  }

  public String getName ()
  {
    return name;
  }

  public String toString ()
  {
    return name;
  }

  public abstract String toString (Value v, ByteBuffer b);

  public abstract String toString (Value v);

    public void toPrint(PrintWriter writer, Value value, ByteBuffer memory,
			Format format) {
	// XXX: Override this!
	writer.print(toString(value, memory));
    }

  public abstract Value add (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value subtract (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value multiply (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value divide (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value mod (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value shiftLeft (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value shiftRight (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value lessThan (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value greaterThan (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value lessThanOrEqualTo (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value greaterThanOrEqualTo (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value equal (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value notEqual (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value bitWiseAnd (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value bitWiseXor (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value bitWiseOr (Value var1, Value var2)
  throws InvalidOperatorException;

  public abstract Value bitWiseComplement (Value var1)
  throws InvalidOperatorException;

  public abstract Value logicalAnd (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value logicalOr (Value var1, Value var2)
      throws InvalidOperatorException;
  
  public abstract Value logicalNegation(Value var1) 
      throws InvalidOperatorException;

  public abstract Value assign (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value timesEqual (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value divideEqual (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value modEqual (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value plusEqual (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value minusEqual (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value shiftLeftEqual (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value shiftRightEqual (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value bitWiseOrEqual (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value bitWiseXorEqual (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract Value bitWiseAndEqual (Value var1, Value var2)
      throws InvalidOperatorException;

  public abstract boolean getLogicalValue (Value var) throws InvalidOperatorException;

  public boolean isTypedef ()
  {
    return isTypedef;
  }

  public void setTypedef (boolean isTypedef)
  {
    this.isTypedef = isTypedef;
  }
  
}
