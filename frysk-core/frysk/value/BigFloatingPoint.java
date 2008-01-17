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
        
    // FIXME: Does not differentiate between 
    // signalling and non signalling NaNs.
    public static final int proper = 0;
    public static final int NaN = 1;
    public static final int posInf = 2;
    public static final int negInf = 3;
    
    /**
     * Create a normal BigFloatingPoint from a BigDecimal.
     */
    BigFloatingPoint (BigDecimal value) {
        this.value = value;
        this.encoding = proper;
    }
    
    /**
     * Create a normal BigFloatingPoint from a BigInteger.
     */
    BigFloatingPoint (BigInteger value) {
        this.value = new BigDecimal(value);
        this.encoding = proper;
    }    
    
    /**
     * Create a BigFloatingPoint from a BigDecimal and encoding.
     * Use for NaNs and infinities.
     */
    BigFloatingPoint (BigDecimal value, int encoding){
    	this.value = value;
    	this.encoding = encoding;
    }

    /**
     * Used for testing.
     */
    BigFloatingPoint (String value) {
    	this.value = new BigDecimal (value);
    	this.encoding = proper;
    }
    
    /**
     * Converts a BigDecimal into a BigInteger. Discards 
     * fractional part.
     */
    BigInteger bigIntegerValue() {
    	return value.toBigInteger();
    }
    
    BigDecimal getValue () {
        return value;
    }

    int getEncoding () {
        return encoding;
    }

    public String toString(int size) {
    	String retValue;
    	switch (encoding) {
    	        // FIXME: Use BigDecimal's toString 
    	case 0: 
    		   if (size < 8)
    		       retValue = Float.toString(floatValue());
    	        else
    		       retValue = Double.toString(doubleValue());
    	       break;
//    	       retValue = value.toString();
//    	       break;
    	case 1:retValue = "NaN";
    	       break;
    	case 2:retValue = "Positive Infinity";
    	       break;
    	case 3:retValue = "Negative Infinity";
    	       break; 
    	default: return "Unknown";
    	}
    	return retValue;
    }

    public double doubleValue() {
    	   return value.doubleValue();
    }
    
    public float floatValue() {
    	return value.floatValue();
    }

    public boolean equals (BigFloatingPoint o) {
    	return (o.getValue().compareTo(this.value) == 0
    			&& o.getEncoding() == this.encoding);
    }
}
