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

import java.math.BigDecimal;
import java.math.BigInteger;

public class FloatingPoint854Format
     extends FloatingPointFormat
{
    public static final FloatingPointFormat IEEE32  = new FloatingPoint854Format (4, 1, 8, 9, 23);
    public static final FloatingPointFormat IEEE64  = new FloatingPoint854Format (8, 1, 11, 12, 52);
    public static final FloatingPointFormat IEEE128 = new FloatingPoint854Format (16, 1+48, 15, 16+48, 64);
    public static final FloatingPointFormat IEEE80  = new FloatingPoint854Format (10, 1, 15, 16, 64);
    public static final FloatingPointFormat IEEE96  = new FloatingPoint854Format (12, 1+16, 15, 16+16, 64);
        
    private Packing packExponent;
    private Packing packFraction;
    private int sizeF;
    private int sizeE;
    private int size;
    private int integralOfMantissa;
    private static final BigDecimal two = BigDecimal.ONE.add(BigDecimal.ONE);
    
    /**
    * @param size - size of floating point in bytes
    * @param idxE - begin bit index of exponent field
    * @param sizeE - bit size of exponent field
    * @param idxF - begin bit index of fraction field
    * @param sizeF - size of fraction field
    *  Note: include j bit in fraction field, where applicable.
    */ 
   FloatingPoint854Format(int size,
	                  int idxE, int sizeE,
	                  int idxF, int sizeF) {
       packExponent = new Packing (size, idxE, sizeE);
       packFraction = new Packing (size, idxF, sizeF);
       this.sizeF = sizeF;
       this.sizeE = sizeE;
       this.size = size;
   }
   
   BigFloatingPoint unpack (byte[] bytes) {
       int s = getSign (bytes);
       BigInteger e = getBiasedExponent(bytes);
       BigInteger f = getFraction(bytes);
       BigInteger maxE = getMaxEValue();
       return toBigFP (s, e, f, maxE);              
   }

   /**
    * @return 0 for negative and 1 for positive values FPs.
    */
   int getSign (byte[] bytes) {
       int sIndex = 0;
       if (this.size == 12)
	   sIndex = 2;
       else if (this.size == 16)
	   sIndex = 6;
       return (((bytes[sIndex] >> 7) & 0x01) == 0) ? 0:1;
   }
   
   BigInteger getBiasedExponent (byte[] bytes) {
       return packExponent.unpackUnsigned(bytes);
   }
   
   BigInteger getFraction (byte[] bytes) {
       return packFraction.unpackUnsigned(bytes);
   }
   
   int getIntegralOfMantissa (byte[] bytes) {
       getMantissa (getFraction(bytes), getBiasedExponent(bytes), sizeF);
       return integralOfMantissa;
   }
   
   BigInteger getMaxEValue () {
       return (BigInteger.valueOf(2).pow(sizeE)).subtract(BigInteger.ONE);
   }

   /**
    * Gets mantissa value according to IEEE 754/854 floating point rules
    * 
    * @param f - value of fraction field (including j bit where applicable)
    * @param e - value of exponent field
    * @param sizeOfF - bit size of fraction field 
    *                  (including j bit where applicable)
    * @return mantissa
    */
   private BigDecimal getMantissa (BigInteger f, BigInteger e, int sizeOfF) {
       if (sizeOfF == 64)
	   return getMantissaExtended (f, sizeOfF);

       int trailingZeroes = f.getLowestSetBit();
       BigDecimal m = new BigDecimal (f.shiftRight(trailingZeroes));
       m = divide (m, two.pow(sizeOfF-trailingZeroes));
       //return (e.compareTo(BigInteger.ZERO) == 0)? m : BigDecimal.ONE.add(m);
       if (e.compareTo(BigInteger.ZERO) == 0) {
	   integralOfMantissa = 0;
	   return m;
       } 
       integralOfMantissa = 1;
       return BigDecimal.ONE.add(m);
   }

   private BigDecimal getMantissaExtended (BigInteger f, int sizeOfF){
       int trailingZeroes = f.getLowestSetBit();
       boolean j = f.testBit(f.bitLength()-1);
       f = f.clearBit(f.bitLength()-1);
       BigDecimal m = new BigDecimal (f.shiftRight(trailingZeroes));
       m = divide (m, two.pow(sizeOfF-trailingZeroes-1));
       //return (j == false)? m : BigDecimal.ONE.add(m);
       if (j == false) {
	   integralOfMantissa = 0;
	   return m;
       } 
       integralOfMantissa = 1;
       return BigDecimal.ONE.add(m);
       
   }
   
   /**
    * Calculate floating point value according to IEEE 754/854 rules.
    * @param s - sign bit
    * @param e - value of exponent field 
    * @param m - mantissa
    * @param maxE - max possible value of exponent field
    */
   private BigFloatingPoint toBigFP (int s, 
	                             BigInteger e, BigInteger f, 
	                             BigInteger maxE) {
       BigDecimal m = getMantissa(f, e, sizeF);
       BigDecimal result = BigDecimal.ZERO;
       BigDecimal one = BigDecimal.ONE;
       int halfMaxE = maxE.intValue()/2;

       if (e.compareTo(maxE) == 0) {
           if (f.compareTo(BigInteger.ZERO) != 0) {
               // FIXME: Should NaNs retain value of fraction
               // or mantissa?
               return new BigFloatingPoint(m, BigFloatingPoint.NaN);
           }
           else {
               // FIXME: 0 or m?
               return (s == 0)? new BigFloatingPoint (m, BigFloatingPoint.posInf):
        	                new BigFloatingPoint (m, BigFloatingPoint.negInf);
           }
       }
       else if (e.compareTo(BigInteger.ZERO) == 0) {
           if (f.compareTo(BigInteger.ZERO) != 0) {
               result = divide(one, two.pow(halfMaxE-1)).multiply(m);
           }
           else {
               result = BigDecimal.ZERO;
           }
           return (s == 0)? new BigFloatingPoint(result):
                            new BigFloatingPoint(result.negate());
       }
       else if (e.compareTo(BigInteger.ZERO) > 0 && e.compareTo(maxE) < 0) {
           if (e.intValue()-halfMaxE < 0)
               result = divide (one, two.pow(-e.intValue()+halfMaxE)).multiply(m);
           else
               result = two.pow(e.intValue()-halfMaxE).multiply(m);
           return (s == 0)? new BigFloatingPoint(result):
                            new BigFloatingPoint(result.negate());
       }
       else {
           throw new RuntimeException 
                    ("IEEE 854 Floating Point conversion error.");
       }
   }
   
   private BigDecimal divide (BigDecimal a, BigDecimal b) {
       BigDecimal result[] = a.divideAndRemainder(b);
       // FIXME: Use long division? Use BigDecimal's 
       // divide(BigDecimal,MathContext) when frysk 
       // moves to java 1.5.0.
       double fraction = result[1].doubleValue()/b.doubleValue();
       return result[0].add(BigDecimal.valueOf(fraction));
   }
      
   /**
    * FIXME: Convert to byte[] using Packing methods. Need to
    * store mantissa and exponent value?
    */
   byte[] pack (BigFloatingPoint value, int size){
	   switch (size) {
	   case 4:
		   return BigInteger.valueOf(Float.floatToRawIntBits(value.floatValue()))
		   .toByteArray();
	   case 8:
		   return BigInteger.valueOf(Double.doubleToRawLongBits(value.doubleValue()))
		   .toByteArray();
	   default:
		   return new byte[0];
	   }
   }
}
