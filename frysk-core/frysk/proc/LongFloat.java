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

/** Object representing 80 bit floating point value from x87 registers.
 */
public class LongFloat 
{
  private static long mantissaMask = (1 << 52) - 1;
  
  private BigInteger bits;
  
  /**
   * Constructor. Initialize object with the raw floating point bits.
   * @param bits raw bits
   */
  public LongFloat(BigInteger bits) 
  {
    this.bits = bits;
  }
  
  /**
   * Constructor using a double value. Convert the bits in the double
   * to the corresponding long float bits.
   *
   * @param doubleVal double floating point value to convert
   */
  public LongFloat(double doubleVal) 
  {
    long doubleBits = Double.doubleToLongBits(doubleVal);
    // Must use long float exponent bias
    long exponent = (doubleBits >> 52) & 0x7ff - 1023;
    
    bits = BigInteger.valueOf(doubleBits & mantissaMask).shiftLeft(12)
      .or(BigInteger.valueOf(exponent + 16383).shiftLeft(64));
    if (doubleBits < 0)
      bits = bits.or(BigInteger.ONE.shiftLeft(79));
  }
  
  /**
   * Return the bit representation of the long float.
   *
   * @return the bits
   */
  public BigInteger getBits()
  {
    return bits;
  }

  /**
   * Return value as a double.
   *
   * @return the double value
   */
  public double asDouble() 
  {
    long exponent = (bits.shiftRight(64).longValue() & 0x7fff) - 16383;
    if (exponent > 127) 
      {
	if (bits.compareTo(BigInteger.ZERO) < 0) 
	  {
	    return Double.NEGATIVE_INFINITY;
	  }
	else 
	  {
	    return Double.POSITIVE_INFINITY;
	  }
      }
    else if (exponent < -128) 
      {
	// underflow instead?
	if (bits.compareTo(BigInteger.ZERO) < 0) 
	  {
	    return -0.0;
	  }
	else 
	  {
	    return 0.0;
	  }
      }
    long doubleBits = bits.shiftRight(12).longValue() & ((1 << 52) -1);
    doubleBits |= (exponent + 1023) << 52;
    if (bits.compareTo(BigInteger.ZERO) < 0)
      doubleBits |= 1 << 63;
    return Double.longBitsToDouble(doubleBits);
  }
}

    
    
	
    
    


  

  