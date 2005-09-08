// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

public class TestGet
    extends TestLib
{
    public void testGet ()
    {
	String string;
	byte[] array = new byte[256];
	initArray (array);

	ByteBuffer b = new ArrayByteBuffer (array);

	// Start as big-endian, +ve values.
	string = bytesToString (array, b.position (), new int[] { 1, 2, 4, 8});
	assertEquals (string + ": +ve BE b.getByte ()",
		      0x01L, b.getByte ());
	assertEquals (string + ": +ve BE b.getShort ()",
		      0x0203L, b.getShort ());
	assertEquals (string + ": +ve BE b.getInt ()",
		      0x04050607L, b.getInt ());
	assertEquals (string + ": +ve BE b.getLong ()",
		      0x08090a0b0c0d0e0fL, b.getLong ());

	// Switch to little-endian, +ve values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	string = bytesToString (array, b.position (), new int[] { 1, 2, 4, 8});
	assertEquals (string + ": +ve LE b.getByte ()",
		      0x10L, b.getByte ());
	assertEquals (string + ": +ve LE b.getShort ()",
		      0x1211L, b.getShort ());
	assertEquals (string + ": +ve LE b.getInt ()",
		      0x16151413L, b.getInt ());
	assertEquals (string + ": +ve LE b.getLong ()",
		      0x1e1d1c1b1a191817L, b.getLong ());

	// Switch to big-endian, +ve unsigned values.
	b.order (ByteOrder.BIG_ENDIAN);
	string = bytesToString (array, b.position (), new int[] { 1, 2, 4, 8});
	assertEquals (string + ": +ve BE b.getUByte ()",
		      0x1fL, b.getUByte ());
	assertEquals (string + ": +ve BE b.getUShort ()",
		      0x2021L, b.getUShort ());
	assertEquals (string + ": +ve BE b.getUInt ()",
		      0x22232425L, b.getUInt ());
	assertEquals (string + ": +ve BE b.getULong ()",
		      0x262728292a2b2c2dL, b.getULong ());

	// Switch to little-endian, +ve unsigned values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	string = bytesToString (array, b.position (), new int[] { 1, 2, 4, 8});
	assertEquals (string + ": +ve LE b.getUByte ()",
		      0x2eL, b.getUByte ());
	assertEquals (string + ": +ve LE b.getUShort ()",
		      0x302fL, b.getUShort ());
	assertEquals (string + ": +ve LE b.getUInt ()",
		      0x34333231L, b.getUInt ());
	assertEquals (string + ": +ve LE b.getULong ()",
		      0x3c3b3a3938373635L, b.getULong ());


	// Jump to the second half of the buffer, it contains values
	// >0x7f which are -ve when sign extended.
	b.position (0x80); // contains 0x80+1.


	// Switch to big-endian, -ve values.
	b.order (ByteOrder.BIG_ENDIAN);
	string = bytesToString (array, b.position (), new int[] { 1, 2, 4, 8});
	assertEquals (string + ": -ve BE b.getByte ()",
		      0xffffffffffffff81L, b.getByte ());
	assertEquals (string + ": -ve BE b.getShort ()",
		      0xffffffffffff8283L, b.getShort ());
	assertEquals (string + ": -ve BE b.getInt ()",
		      0xffffffff84858687L, b.getInt ());
	assertEquals (string + ": -ve BE b.getLong ()",
		      0x88898a8b8c8d8e8fL, b.getLong ());

	// Switch to little-endian, -ve values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	string = bytesToString (array, b.position (), new int[] { 1, 2, 4, 8});
	assertEquals (string + ": -ve LE b.getByte ()",
		      0xffffffffffffff90L, b.getByte ());
	assertEquals (string + ": -ve LE b.getShort ()",
		      0xffffffffffff9291L, b.getShort ());
	assertEquals (string + ": -ve LE b.getInt ()",
		      0xffffffff96959493L, b.getInt ());
	assertEquals (string + ": -ve LE b.getLong ()",
		      0x9e9d9c9b9a999897L, b.getLong ());

	// Switch to big-endian, -ve unsigned values.
	b.order (ByteOrder.BIG_ENDIAN);
	string = bytesToString (array, b.position (), new int[] { 1, 2, 4, 8});
	assertEquals (string + ": -ve BE b.getUByte ()",
		      0x9fL, b.getUByte ());
	assertEquals (string + ": -ve BE b.getUShort ()",
		      0xa0a1L, b.getUShort ());
	assertEquals (string + ": -ve BE b.getUInt ()",
		      0xa2a3a4a5L, b.getUInt ());
	assertEquals (string + ": -ve BE b.getULong ()",
		      0xa6a7a8a9aaabacadL, b.getULong ());

	// Switch to little-endian, -ve unsigned values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	string = bytesToString (array, b.position (), new int[] {8});
	assertEquals (string + ": -ve LE b.getUByte ()",
		      0xaeL, b.getUByte ());
	assertEquals (string + ": -ve LE b.getUShort ()",
		      0xb0afL, b.getUShort ());
	assertEquals (string + ": -ve LE b.getUInt ()",
		      0xb4b3b2b1L, b.getUInt ());
	assertEquals (string + ": -ve LE b.getULong ()",
		      0xbcbbbab9b8b7b6b5L, b.getULong ());



	// Switch to big-endian, +ve values.
	b.order (ByteOrder.BIG_ENDIAN);
	string = bytesToString (array, 0x43, new int[] {8});
	assertEquals (string + ": +ve BE b.getByte (0x43)",
		      0x44L, b.getByte (0x43));
	assertEquals (string + ": +ve BE b.getShort (0x43)",
		      0x4445L, b.getShort (0x43));
	assertEquals (string + ": +ve BE b.getInt (0x43)",
		      0x44454647L, b.getInt (0x43));
	assertEquals (string + ": +ve BE b.getLong (0x43)",
		      0x4445464748494a4bL, b.getLong (0x43));

	// Switch to little-endian, +ve values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	string = bytesToString (array, 0x61, new int[] {8});
	assertEquals (string + ": +ve LE b.getByte (0x61)",
		      0x62L, b.getByte (0x61));
	assertEquals (string + ": +ve LE b.getShort (0x61)",
		      0x6362L, b.getShort (0x61));
	assertEquals (string + ": +ve LE b.getInt (0x61)",
		      0x65646362L, b.getInt (0x61));
	assertEquals (string + ": +ve LE b.getLong (0x61)",
		      0x6968676665646362L, b.getLong (0x61));

	// Switch to big-endian, +ve unsigned values.
	b.order (ByteOrder.BIG_ENDIAN);
	string = bytesToString (array, 0x25, new int[] {8});
	assertEquals (string + ": +ve BE b.getUByte (0x25)",
		      0x26L, b.getUByte (0x25));
	assertEquals (string + ": +ve BE b.getUShort (0x25)",
		      0x2627L, b.getUShort (0x25));
	assertEquals (string + ": +ve BE b.getUInt (0x25)",
		      0x26272829L, b.getUInt (0x25));
	assertEquals (string + ": +ve BE b.getULong (0x25)",
		      0x262728292a2b2c2dL, b.getULong (0x25));

	// Switch to little-endian, +ve unsigned values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	string = bytesToString (array, 0x37, new int[] {8});
	assertEquals (string + ": +ve LE b.getUByte (0x37)",
		      0x38L, b.getUByte (0x37));
	assertEquals (string + ": +ve LE b.getUShort (0x37)",
		      0x3938L, b.getUShort (0x37));
	assertEquals (string + ": +ve LE b.getUInt (0x37)",
		      0x3b3a3938L, b.getUInt (0x37));
	assertEquals (string + ": +ve LE b.getULong (0x37)",
		      0x3f3e3d3c3b3a3938L, b.getULong (0x37));


	// Jump to the second half of the buffer, it contains values
	// >0x7f which are -ve when sign extended.
	b.position (0x80); // contains 0x80+1.


	// Switch to big-endian, -ve values.
	b.order (ByteOrder.BIG_ENDIAN);
	string = bytesToString (array, 0x84, new int[] {8});
	assertEquals (string + ": -ve BE b.getByte (0x84)",
		      0xffffffffffffff85L, b.getByte (0x84));
	assertEquals (string + ": -ve BE b.getShort (0x84)",
		      0xffffffffffff8586L, b.getShort (0x84));
	assertEquals (string + ": -ve BE b.getInt (0x84)",
		      0xffffffff85868788L, b.getInt (0x84));
	assertEquals (string + ": -ve BE b.getLong (0x84)",
		      0x85868788898a8b8cL, b.getLong (0x84));

	// Switch to little-endian, -ve values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	string = bytesToString (array, 0xaa, new int[] {8});
	assertEquals (string + ": -ve LE b.getByte (0xaa)",
		      0xffffffffffffffabL, b.getByte (0xaa));
	assertEquals (string + ": -ve LE b.getShort (0xaa)",
		      0xffffffffffffacabL, b.getShort (0xaa));
	assertEquals (string + ": -ve LE b.getInt (0xaa)",
		      0xffffffffaeadacabL, b.getInt (0xaa));
	assertEquals (string + ": -ve LE b.getLong (0xaa)",
		      0xb2b1b0afaeadacabL, b.getLong (0xaa));

	// Switch to big-endian, -ve unsigned values.
	b.order (ByteOrder.BIG_ENDIAN);
	string = bytesToString (array, 0xbb, new int[] {8});
	assertEquals (string + ": -ve BE b.getUByte (0xbb)",
		      0xbcL, b.getUByte (0xbb));
	assertEquals (string + ": -ve BE b.getUShort (0xbb)",
		      0xbcbdL, b.getUShort (0xbb));
	assertEquals (string + ": -ve BE b.getUInt (0xbb)",
		      0xbcbdbebfL, b.getUInt (0xbb));
	assertEquals (string + ": -ve BE b.getULong (0xbb)",
		      0xbcbdbebfc0c1c2c3L, b.getULong (0xbb));

	// Switch to little-endian, -ve unsigned values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	string = bytesToString (array, 0xcc, new int[] {8});
	assertEquals (string + ": -ve LE b.getUByte (0xcc)",
		      0xcdL, b.getUByte (0xcc));
	assertEquals (string + ": -ve LE b.getUShort (0xcc)",
		      0xcecdL, b.getUShort (0xcc));
	assertEquals (string + ": -ve LE b.getUInt (0xcc)",
		      0xd0cfcecdL, b.getUInt (0xcc));
	assertEquals (string + ": -ve LE b.getULong (0xcc)",
		      0xd4d3d2d1d0cfcecdL, b.getULong (0xcc));
    }
}
