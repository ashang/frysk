// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

public class TestWordSize
    extends TestLib
{
    static ByteOrder[] byteOrders = new ByteOrder[] {
	ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN
    };

    static int[] wordSizes = new int[] { 2, 4, 8 };

    static long[] getWordVals = new long[] {
	0x0102L, 0x0403L,
	0x05060708L, 0x0c0b0a09L,
	0x0d0e0f1011121314L, 0x1c1b1a1918171615L
    };

    static long[] getUWordVals = new long[] {
	0x898aL, 0x8c8bL,
	0x8d8e8f90L, 0x94939291L,
	0x95969798999a9b9cL, 0xa4a3a2a1a09f9e9dL
    };

    public void testGetWord ()
    {
	int idx;
	
	byte[] array = orderedArray ();
	ByteBuffer b = new ArrayByteBuffer (array);

	idx = 0;
	for (int w = 0; w < wordSizes.length; w++) {
	    int wordSize = wordSizes[w];
	    b.wordSize (wordSize);
	    for (int o = 0; o < byteOrders.length; o++) {
		ByteOrder byteOrder = byteOrders[o];
		b.order (byteOrder);
		assertEquals ("getWord() wordsize=" + wordSize + " " + byteOrder,
			      b.getWord (), getWordVals[idx]);
		idx++;
	    }
	}

    }

    public void testRest ()
    {
	int idx;

	byte[] array = orderedArray ();
	ByteBuffer b = new ArrayByteBuffer (array);
	idx = 0;
	b.position (0x88);
	for (int w = 0; w < wordSizes.length; w++) {
	    int wordSize = wordSizes[w];
	    b.wordSize (wordSize);
	    for (int o = 0; o < byteOrders.length; o++) {
		ByteOrder byteOrder = byteOrders[o];
		b.order (byteOrder);
		assertEquals ("getWord() wordsize=" + wordSize + " " + byteOrder,
			      b.getUWord (), getUWordVals[idx]);
		idx++;
	    }
	}

	clearArray (array);
	idx = 0;
	b.position (35);
	for (int w = 0; w < wordSizes.length; w++) {
	    int wordSize = wordSizes[w];
	    b.wordSize (wordSize);
	    for (int o = 0; o < byteOrders.length; o++) {
		ByteOrder byteOrder = byteOrders[o];
		b.order (byteOrder);
		b.putWord (getWordVals[idx]);
		idx++;
	    }
	}
	check ("putWord()", array, 35,
	       new int[] {
		   0x01, 0x02,
		   0x03, 0x04,
		   0x05, 0x06, 0x07, 0x08,
		   0x09, 0x0a, 0x0b, 0x0c,
		   0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14,
		   0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c
	       });

	clearArray (array);
	idx = 0;
	b.position (0x88);
	for (int w = 0; w < wordSizes.length; w++) {
	    int wordSize = wordSizes[w];
	    b.wordSize (wordSize);
	    for (int o = 0; o < byteOrders.length; o++) {
		ByteOrder byteOrder = byteOrders[o];
		b.order (byteOrder);
		b.putUWord (getUWordVals[idx]);
		idx++;
	    }
	}
	check ("putUWord()", array, 0x88,
	       new int[] {
		   0x89, 0x8a,
		   0x8b, 0x8c,
		   0x8d, 0x8e, 0x8f, 0x90,
		   0x91, 0x92, 0x93, 0x94,
		   0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0x9b, 0x9c,
		   0x9d, 0x9e, 0x9f, 0xa0, 0xa1, 0xa2, 0xa3, 0xa4
	       });
    }
}
