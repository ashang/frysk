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

package frysk.gui.common;

import java.math.BigInteger;

/**
 * Some utility functions for treating BigIntegers as unsigned integers.
 *
 * @author timoore
 */
public class UBigInteger
{
  private static BigInteger makeMask(int integerLength)
  {
    return BigInteger.ONE.shiftLeft(integerLength).subtract(BigInteger.ONE);
  }

  /**
   * Mask off the lower bits of a BigInteger, replacing the upper bits
   * with zeros (obviously).
   *
   * @param val BigInteger value
   * @param integerLength length of bits to preserve
   * @return masked value.
   */
  public static BigInteger mask(BigInteger val, int integerLength)
  {
    return val.and (makeMask(integerLength));
  }

  /**
   * Print the string representation of an unsigned integer.
   *
   * @param val the BigInteger holding the unsigned integer
   * @param integerLength length of unsigned integer
   * @param radix of output
   * @return string represntation
   */
  public static String toString(BigInteger val, int integerLength, int radix)
  {
    return mask(val, integerLength).toString(radix);
  }

  /**
   * Treat the MSB of an unsigned integer stored in a BigInteger as a
   * sign bit and return a positive or negative BigInteger as
   * appropriate.
   *
   * @param val the BigInteger value
   * @param integerLength length of unsigned integer, or 1 plus bit
   *   position of sign bit.
   * @return new integer with proper sign.
   */
  public static BigInteger signExtend(BigInteger val, int integerLength)
  {
    if (val.testBit(integerLength - 1))
      {
	return val.or(makeMask(integerLength).not());
      }
    else
      {
	return val;
      }
  }
}