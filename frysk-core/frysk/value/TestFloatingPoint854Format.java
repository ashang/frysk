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
import frysk.junit.TestCase;

public class TestFloatingPoint854Format
    extends TestCase
{
    static void checkEquals (String what, 
	                     BigFloatingPoint correct, BigFloatingPoint expect) {
	if (!correct.equals(expect)) {
	    fail (what + "expected <" + correct.toString() + ">"
		       + "got <" + expect.toString() + ">");
	}
    }
    static final byte[] FLOAT_HALF = new byte[] {
        0x3f, 0x00, 0x00, 0x00
    };
    static final byte[] FLOAT_NEGATIVE_ZERO = new byte[] {
        (byte)0x80, 0x00, 0x00, 0x00
    };
    static final byte[] FLOAT_POSITIVE_ZERO = new byte[] {
        0x00, 0x00, 0x00, 0x00
    };
    static final byte[] FLOAT_ONE = new byte[] {
        0x3f, (byte)0x80, 0x00, 0x00
    };
    static final byte[] FLOAT_TWO = new byte[] {
        0x40, 0x00, 0x00, 0x00
    };
    static final byte[] FLOAT_NINE = new byte[] {
        0x41, 0x10, 0x00, 0x00
    };       

    static final byte[] DOUBLE_ONE = new byte[] {
        0x3f, (byte)0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    static final byte[] DOUBLE_HALF = new byte[] {
        0x3f, (byte)0xe0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    static final byte[] DOUBLE_NEGATIVE_ZERO = new byte[] {
        (byte)0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    static final byte[] DOUBLE_POSITIVE_ZERO = new byte[] {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    static final byte[] DOUBLE_TWO = new byte[] {
        0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    static final byte[] DOUBLE_NINE = new byte[] {
        0x40, 0x22, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };    
    static final byte[] DOUBLE_FIVEPOINT12 = new byte[] {
        (byte)0xc0, 0x14, 0x7a, (byte)0xe1, 0x47, (byte)0xae, 0x14, 0x7b
    };  
    static final byte[] DOUBLE_FIVEPOINT769 = new byte[] {
        (byte)0xc0, 0x17, 0x13, 0x74, (byte)0xbc, 0x6a, 0x7e, (byte)0xfa
    };  
    static final byte[] NEG_INF = new byte[] {
    	(byte)0xff, (byte)0xf0, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 
    };
    
    static final byte[] FP_80_SEVENPOINT5 = new byte[] {
	0x40, 0x01, (byte)0xf0, 0x00, 0x00, 
	0x00, 0x00, 0x00, 0x00, 0x00
    };
	
    static final byte[] FP_96_ONE = new byte[] {
        0x00, 0x00, 0x3f, (byte)0xff, (byte)0x80, 
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  
    };  
    static final byte[] FP_96_NINE = new byte[] {
        0x00, 0x00, 0x40, 0x02, (byte)0x90, 
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  
    };  
    static final byte[] FP_96_SEVENPOINT5 = new byte[] {
        0x00, 0x00, 0x40, 0x01, (byte)0xf0, 
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  
    }; 
    
    static final byte[] FP_128_SEVENPOINT5 = new byte[] {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
        0x40, 0x01, (byte)0xf0, 0x00, 0x00, 0x00, 
        0x00, 0x00, 0x00, 0x00
    };
    static final byte[] FP_128_NEGATIVE_TWO = new byte[] {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        (byte)0xc0, 0x00, (byte)0x80, 0x00, 0x00, 0x00, 
        0x00, 0x00, 0x00, 0x00
    };

    public void testFromBigDecimal() {
        BigFloatingPoint f = new BigFloatingPoint(new BigDecimal(1.0));
        checkEquals("float", new BigFloatingPoint("1"), f);
    }

    public void testFromBytes() {
        checkEquals("float 1", new BigFloatingPoint("1.0"),
                     FloatingPoint854Format.IEEE32.unpack(FLOAT_ONE));
        checkEquals("float .5", new BigFloatingPoint("0.5"),
        	     FloatingPoint854Format.IEEE32.unpack(FLOAT_HALF));
        checkEquals("float -0", new BigFloatingPoint("-0.0"),
        	     FloatingPoint854Format.IEEE32.unpack(FLOAT_NEGATIVE_ZERO));
        checkEquals("float +0", new BigFloatingPoint("0.0"), 
        	     FloatingPoint854Format.IEEE32.unpack(FLOAT_POSITIVE_ZERO));
        checkEquals("float 2", new BigFloatingPoint("2.0"),
        	     FloatingPoint854Format.IEEE32.unpack(FLOAT_TWO));
        checkEquals("float 9", new BigFloatingPoint("9.0"),
        	     FloatingPoint854Format.IEEE32.unpack(FLOAT_NINE));

        checkEquals("double 1", new BigFloatingPoint("1.0"),
                     FloatingPoint854Format.IEEE64.unpack(DOUBLE_ONE));
        checkEquals("double .5", new BigFloatingPoint("0.5"),
                     FloatingPoint854Format.IEEE64.unpack(DOUBLE_HALF));
        checkEquals("double -0", new BigFloatingPoint("-0.0"),
                     FloatingPoint854Format.IEEE64.unpack(DOUBLE_NEGATIVE_ZERO));
        checkEquals("double +0", new BigFloatingPoint("0.0"),
                     FloatingPoint854Format.IEEE64.unpack(DOUBLE_POSITIVE_ZERO));
        checkEquals("double 2", new BigFloatingPoint("2.0"),
                     FloatingPoint854Format.IEEE64.unpack(DOUBLE_TWO));
        checkEquals("double 9", new BigFloatingPoint("9.0"),
                     FloatingPoint854Format.IEEE64.unpack(DOUBLE_NINE));
        checkEquals("double -5.12", new BigFloatingPoint("-5.12"),
                     FloatingPoint854Format.IEEE64.unpack(DOUBLE_FIVEPOINT12));
        assertEquals("double -Inf", "Negative Infinity", 
        		     FloatingPoint854Format.IEEE64.unpack(NEG_INF).toString(8));
      
        checkEquals("FP_80 7.5", new BigFloatingPoint("7.5"),
        	    FloatingPoint854Format.IEEE80.unpack(FP_80_SEVENPOINT5));

        checkEquals("FP_96 1", new BigFloatingPoint("1.0"),
                    FloatingPoint854Format.IEEE96.unpack(FP_96_ONE));
        checkEquals("FP_96 9", new BigFloatingPoint("9.0"),
        	    FloatingPoint854Format.IEEE96.unpack(FP_96_NINE));
        checkEquals("FP_96 7.5", new BigFloatingPoint("7.5"),
        	    FloatingPoint854Format.IEEE96.unpack(FP_96_SEVENPOINT5));
        
        checkEquals("FP_128 7.5", new BigFloatingPoint("7.5"),
        	    FloatingPoint854Format.IEEE128.unpack(FP_128_SEVENPOINT5));
        checkEquals("FP_128 -2.0", new BigFloatingPoint("-2.0"),
        	    FloatingPoint854Format.IEEE128.unpack(FP_128_NEGATIVE_TWO));
    }
    
    static final BigFloatingPoint two = new BigFloatingPoint ("2.0");
    
    public void testToBytes () {
    	assertEquals ("FP_32 2", FLOAT_TWO, FloatingPoint854Format.IEEE32.pack(two, 4));
    }
}

