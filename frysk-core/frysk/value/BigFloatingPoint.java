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

/**
 * Floating point type - uses java.math.BigDecimal.
 */
public class BigFloatingPoint 
{
    private BigDecimal value;
    private int encoding;
        
    public static final int proper = 0;
    public static final int NaN = 1;
    public static final int posInf = 2;
    public static final int negInf = 3;
    
    // Packing objects for each IEEE FP type.
    private static final Packing packExponent32 = new Packing (4, 1, 8);
    private static final Packing packFraction32 = new Packing (4, 9, 23);
    private static final Packing packExponent64 = new Packing (8, 1, 11);
    private static final Packing packFraction64 = new Packing (8, 12, 52);
    // FIXME: 128 bit long doubles on x86_64 follow 80 bit "format"
    private static final Packing packExponent128 = new Packing (16, 1, 15);
    private static final Packing packFraction128 = new Packing (16, 16, 112);    
    private static final Packing packExponent96 = new Packing (12, 1+16, 15);
    private static final Packing packFraction96 = new Packing (12, 16+16, 64);
    private static final Packing packExponent80 = new Packing (10, 1, 15);
    private static final Packing packFraction80 = new Packing (10, 16, 64);
    
    private static final BigDecimal two = BigDecimal.ONE.add(BigDecimal.ONE);
    
    /**
     * Create a BigFloatingPoint from a BigDecimal.
     */
    BigFloatingPoint (BigDecimal value) {
        this.value = value;
        this.encoding = proper;
    }
    
    /**
     * Create a BigFloatingPoint from the big-endian raw 
     * bytes. bytes[] contain three fields:
     *    sign, exponent and fraction
     */
    BigFloatingPoint (byte[] bytes) {    
        Packing e;
        Packing f;
        int maxE;
        int sizeofF;

        switch (bytes.length) {
        case 4: 
             e = packExponent32;
             f = packFraction32;
             sizeofF = 23;
             maxE = 0xff;          
             break;
        case 8: 
             e = packExponent64;
             f = packFraction64;
             sizeofF = 52;
             maxE = 0x7ff;
             break;   
        case 16: 
             e = packExponent128;
             f = packFraction128;
             sizeofF = 112;
             maxE = 0x7fff;
             break;  
        case 12: 
             e = packExponent96;
             f = packFraction96;
             sizeofF = 64;
             maxE = 0x7fff;
             break;  
        case 10: 
             e = packExponent80;
             f = packFraction80;
             sizeofF = 64;
             maxE = 0x7fff;
             break;          
        default:
            throw new RuntimeException ("Unsupported Floating Point size");
        }
        int s = (((bytes[0] >> 7) & 0x01) == 0) ? -1:1;

        BigDecimal m = getMantissa(f.unpackUnsigned(bytes), 
                                   e.unpackUnsigned(bytes), 
                                   sizeofF);
        calculateValue (s, e.unpackUnsigned(bytes), m, BigInteger.valueOf(maxE));
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
        return (e.compareTo(BigInteger.ZERO) == 0)? m : BigDecimal.ONE.add(m);
    }
    
    private BigDecimal getMantissaExtended (BigInteger f, int sizeOfF){
        int trailingZeroes = f.getLowestSetBit();
        boolean j = f.testBit(f.bitLength()-1);
        f = f.clearBit(f.bitLength()-1);
        BigDecimal m = new BigDecimal (f.shiftRight(trailingZeroes));
        m = divide (m, two.pow(sizeOfF-trailingZeroes-1));
        return (j == false)? m : BigDecimal.ONE.add(m);
    }

    /**
     * Calculate floating point value according to IEEE 754/854 rules.
     * @param s - sign bit
     * @param e - value of exponent field 
     * @param m - mantissa
     * @param maxE - max possible value of exponent field
     */
    private void calculateValue (int s, BigInteger e, BigDecimal m, BigInteger maxE) {
        BigDecimal result = BigDecimal.ZERO;
        BigDecimal one = BigDecimal.ONE;
        int halfMaxE = maxE.intValue()/2;

        if (e.compareTo(maxE) == 0) {
            if (m.compareTo(BigDecimal.ZERO) != 0) {
                // FIXME: Should NaNs retain value of fraction
                // or mantissa?
                this.value = m;
                this.encoding = NaN;
            }
            else {
                // FIXME: Should this retain any value here?
                this.value = null;
                this.encoding = ( s == -1 )? posInf:negInf;
            }
        }
        else if (e.compareTo(BigInteger.ZERO) == 0) {
            if (m.compareTo(BigDecimal.ZERO) != 0) {
                result = divide(one, two.pow(halfMaxE-1)).multiply(m);
            }
            else {
                result = BigDecimal.ZERO;
            }
            this.value = (s == -1)? result:result.negate();
            this.encoding = proper;
        }
        else if (e.compareTo(BigInteger.ZERO) > 0 && e.compareTo(maxE) < 0) {
            if (e.intValue()-halfMaxE < 0)
                result = divide (one, two.pow(-e.intValue()+halfMaxE)).multiply(m);
            else
                result = two.pow(e.intValue()-halfMaxE).multiply(m);
            this.value = ( s == -1 )? result:result.negate();
            this.encoding = proper;
        }
        else {
            throw new RuntimeException 
                     ("IEEE 754/854 Floating Point conversion error.");
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
        
    BigDecimal getValue () {
        return value;
    }

    int getEncoding () {
        return encoding;
    }

    double doubleValue() {
        if (encoding == proper)
            return value.doubleValue();
        else
            return 0; 
    }
}
