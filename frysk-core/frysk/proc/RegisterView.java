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

import java.lang.Float;
import java.lang.Double;
import java.math.BigInteger;

/**
 * A class for examining the raw bits of a register as fields of
 * different types.
 */
public class RegisterView extends BitfieldAccessor 
{
  public final static int INTEGER = 0x1;
  public final static int FLOAT = 0x2;
  public final static int DOUBLE = 0x4;
  public final static int LONGFLOAT = 0x8;
    
  private final int type;
  
  /**
   * Constructor. Similar to the superclass constructor in
   * BitfieldAcessor, with the addition of a type flag.
   * 
   * @param length the register length in bytes (unlike
   * <code>BitfieldAcessor</code>)
   * @param fieldLength the field length in bytes
   * @param type a static type constant
   */
  public RegisterView(int length, int fieldLength, int type) 
  {
    super(length * 8, fieldLength * 8);
    this.type = type;
  }
  
  /**
   * The types that are valid to fetch in this view.
   *
   * @return the logical or of the type flags for valid types.
   */
  public int getType() 
  {
    return type;
  }

  public float getFloatField(BigInteger value, int fieldNum)
  {
    return Float.intBitsToFloat(value.shiftRight(fieldNum * fieldLength)
				.and(fieldMask).intValue());
  }
  
  public double getDoubleFloatField(BigInteger value, int fieldNum)
  {
    return Double.longBitsToDouble(value.shiftRight(fieldNum * fieldLength)
				   .and(fieldMask).longValue());
  }
  
  /**
   * Get the value of the field as a LongFloat.
   *
   * @return the long float value
   */
  public LongFloat getLongFloatField(BigInteger value, int fieldNum) 
  {
    return new LongFloat(value.shiftRight(fieldNum * fieldLength)
			 .and(fieldMask));
  }
}
