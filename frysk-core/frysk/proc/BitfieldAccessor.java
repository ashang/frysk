// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

package frysk.proc;

import java.math.BigInteger;

/**
 * A BitfieldAccessor treats a BigInteger as being a fixed length with one
 * or more fields of equal length. The bit patterns in the field can
 * be treated as integer values, 32 bit floating point values, or 64
 * bit floating point values.
 */
public class BitfieldAccessor 
{
  protected int fields;
  protected int fieldLength;
  protected BigInteger fieldMask;

  /**
   * Constructor for an accessor.
   *
   * @param fields the number of fields in the integer
   * @param fieldLength the length of each field
   */
  public BitfieldAccessor(int fields, int fieldLength)
  {
    this.fields = fields;
    this.fieldLength = fieldLength;
    fieldMask = UBigInteger.makeMask(fieldLength);
  }
  
  /**
   * @return number of fields
   */
  public int getFields() 
  {
    return fields;
    
  }
  
  /**
   * @returns field length
   */
  public int getFieldLength() 
  {
    return fieldLength;
  }
     
  /**
   * Returns the unsigned integer value of the field fieldNum in the
   * integer value. Fields are numbered from the low-order bits of the
   * integer. 
   *
   * @param value the integer to access
   * @param fieldNum the field
   * @return the value of the field as an unsigned integer
   */
  public BigInteger getIntField(BigInteger value, int fieldNum) 
  {
    if (fieldNum >= fields)
      throw new RuntimeException("invalid field " + fieldNum);
    return value.shiftRight(fieldNum * fieldLength).and(fieldMask);
  }

  /**
   * Returns the single float value of the field fieldNum in the
   * integer value. Fields are numbered from the low-order bits of the
   * integer. 
   *
   * @param value the integer to access
   * @param fieldNum the field
   * @return the value of the field as a float
   */
  public float getFloatField(BigInteger value, int fieldNum) 
  {
    if (fieldNum >= fields)
      throw new RuntimeException("invalid field " + fieldNum);
    return Float.intBitsToFloat(value.shiftRight(fieldNum * fieldLength)
				.and(fieldMask).intValue());
  }
  
  /**
   * Returns the double float value of the field fieldNum in the
   * integer value. Fields are numbered from the low-order bits of the
   * integer. 
   *
   * @param value the integer to access
   * @param fieldNum the field
   * @return the value of the field as a double float
   */
  public double getDoubleField(BigInteger value, int fieldNum) 
  {
    if (fieldNum >= fields)
      throw new RuntimeException("invalid field " + fieldNum);
    return Double.longBitsToDouble(value.shiftRight(fieldNum * fieldLength)
				   .and(fieldMask).longValue());
  }
}
