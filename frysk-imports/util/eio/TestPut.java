// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

public class TestPut
    extends TestLib
{
    public void testPut ()
    {
	int idx;

	byte[] array = new byte[256];

	ByteBuffer b = new ArrayByteBuffer (array);

	// Start as big-endian, +ve values.
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x0+0*15, idx);
	b.putByte ((byte) 0x01);
	b.putShort ((short) 0x0203);
	b.putInt ((int) 0x04050607);
	b.putLong ((long) 0x08090a0b0c0d0e0fL);
	check ("+ve BE put... (*)", array, idx,
		     new int[] {
			 0x01,
			 0x02, 0x03,
			 0x04, 0x05, 0x06, 0x07,
			 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f
		     });
	
 	// Switch to little-endian, +ve values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x0+1*15, idx);
	b.putByte ((byte) 0x10);
	b.putShort ((short) 0x1112);
	b.putInt ((int) 0x13141516);
	b.putLong ((long) 0x1718191a1b1c1d1eL);
	check ("+ve LE put... (*)", array, idx,
		     new int[] {
			 0x10,
			 0x12, 0x11,
			 0x16, 0x15, 0x14, 0x13,
			 0x1e, 0x1d, 0x1c, 0x1b, 0x1a, 0x19, 0x18, 0x17
		     });

	// Switch to big-endian, +ve unsigned values.
	b.order (ByteOrder.BIG_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x0+2*15, idx);
	b.putUByte ((short) 0x1f);
	b.putUShort ((int) 0x2021);
	b.putUInt ((long) 0x22232425);
	b.putULong ((long) 0x262728292a2b2c2dL);
	check ("+ve BE putU... (*)", array, idx,
		     new int[] {
			 0x1f,
			 0x20, 0x21,
			 0x22, 0x23, 0x24, 0x25,
			 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d
		     });

	// Switch to little-endian, +ve unsigned values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x0+3*15, idx);
	b.putUByte ((short) 0x2e);
	b.putUShort ((int) 0x2f30);
	b.putUInt ((long) 0x31323334);
	b.putULong ((long) 0x35363738393a3b3cL);
	check ("+ve LE putU... (*)", array, idx,
		     new int[] {
			 0x2e,
			 0x30, 0x2f,
			 0x34, 0x33, 0x32, 0x31,
			 0x3c, 0x3b, 0x3a, 0x39, 0x38, 0x37, 0x36, 0x35
		     });


	// Jump to the second half of the buffer.
	b.position (0x80);


	// Switch to big-endian, -ve values.
	b.order (ByteOrder.BIG_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x80+0*15, idx);
	b.putByte ((byte) 0x81);
	b.putShort ((short) 0x8283);
	b.putInt ((int) 0x84858687);
	b.putLong ((long) 0x88898a8b8c8d8e8fL);
	check ("-ve BE put... (*)", array, idx,
		     new int[] {
			 0x81,
			 0x82, 0x83,
			 0x84, 0x85, 0x86, 0x87,
			 0x88, 0x89, 0x8a, 0x8b, 0x8c, 0x8d, 0x8e, 0x8f
		     });

	// Switch to little-endian, -ve values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x80+1*15, idx);
	b.putByte ((byte) 0x90);
	b.putShort ((short) 0x9192);
	b.putInt ((int) 0x93949596);
	b.putLong ((long) 0x9798999a9b9c9d9eL);
	check ("-ve LE put (*)", array, idx,
		     new int[] {
			 0x90,
			 0x92, 0x91,
			 0x96, 0x95, 0x94, 0x93,
			 0x9e, 0x9d, 0x9c, 0x9b, 0x9a, 0x99, 0x98, 0x97
		     });

	// Switch to big-endian, -ve unsigned values.
	b.order (ByteOrder.BIG_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x80+2*15, idx);
	b.putUByte ((short) 0x9f);
	b.putUShort ((int) 0xa0a1);
	b.putUInt ((long) 0xa2a3a4a5);
	b.putULong ((long) 0xa6a7a8a9aaabacadL);
	check ("-ve BE putU... (*)", array, idx,
		     new int[] {
			 0x9f,
			 0xa0, 0xa1,
			 0xa2, 0xa3, 0xa4, 0xa5,
			 0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xab, 0xac, 0xad
		     });

	// Switch to little-endian, -ve unsigned values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x80+3*15, idx);
	b.putUByte ((short) 0xae);
	b.putUShort ((int) 0xafb0);
	b.putUInt ((long) 0xb1b2b3b4);
	b.putULong ((long) 0xb5b6b7b8b9babbbcL);
	check ("-ve LE putU (*)", array, idx,
		     new int[] {
			 0xae,
			 0xb0, 0xaf,
			 0xb4, 0xb3, 0xb2, 0xb1,
			 0xbc, 0xbb, 0xba, 0xb9, 0xb8, 0xb7, 0xb6, 0xb5
		     });



	// Switch to big-endian, +ve values.
	b.order (ByteOrder.BIG_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x80+4*15, idx);
	b.putByte (idx + 0x0, (byte) 0x44);
	b.putShort (idx + 0x1, (short) 0x4546);
	b.putInt (idx + 0x3, (int) 0x4748494a);
	b.putLong (idx + 0x7, (long) 0x4b4c4d4e4f505152L);
	check ("+ve BE put... (*,*)", array, idx,
		     new int[] {
			 0x44,
			 0x45, 0x46,
			 0x47, 0x48, 0x49, 0x4a,
			 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52
		     });

	// Switch to little-endian, +ve values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x80+4*15, idx);
	b.putByte (idx + 0x0, (byte) 0x54);
	b.putShort (idx + 0x1, (short) 0x5556);
	b.putInt (idx + 0x3, (int) 0x5758595a);
	b.putLong (idx + 0x7, (long) 0x5b5c5d5e5f606162L);
	check ("+ve LE put... (*,*)", array, idx,
		     new int[] {
			 0x54,
			 0x56, 0x55,
			 0x5a, 0x59, 0x58, 0x57,
			 0x62, 0x61, 0x60, 0x5f, 0x5e, 0x5d, 0x5c, 0x5b
		     });

	// Switch to big-endian, +ve unsigned values.
	b.order (ByteOrder.BIG_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x80+4*15, idx);
	b.putUByte (idx + 0x0, (byte) 0x63);
	b.putUShort (idx + 0x1, (short) 0x6465);
	b.putUInt (idx + 0x3, (int) 0x66676869);
	b.putULong (idx + 0x7, (long) 0x6a6b6c6d6e6f7071L);
	check ("+ve BE putU... (*,*)", array, idx,
		     new int[] {
			 0x63,
			 0x64, 0x65,
			 0x66, 0x67, 0x68, 0x69,
			 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f, 0x70, 0x71
		     });

	// Switch to little-endian, +ve unsigned values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x80+4*15, idx);
	b.putUByte (idx + 0x0, (byte) 0x12);
	b.putUShort (idx + 0x1, (short) 0x1314);
	b.putUInt (idx + 0x3, (int) 0x15161718);
	b.putULong (idx + 0x7, (long) 0x191a1b1c1d1e1f20L);
	check ("+ve LE putU... (*,*)", array, idx,
		     new int[] {
			 0x12,
			 0x14, 0x13,
			 0x18, 0x17, 0x16, 0x15,
			 0x20, 0x1f, 0x1e, 0x1d, 0x1c, 0x1b, 0x1a, 0x19
		     });
			      

	// Jump to the second half of the buffer, it contains values
	// >0x7f which are -ve when sign extended.
	b.position (0x89); // contains (byte)0x80+1.


	// Switch to big-endian, -ve values.
	b.order (ByteOrder.BIG_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x89, idx);
	b.putByte (idx + 0x0, (byte) 0x81);
	b.putShort (idx + 0x1, (short) 0x8283);
	b.putInt (idx + 0x3, (int) 0x84858687);
	b.putLong (idx + 0x7, (long) 0x88898a8b8c8d8e8fL);
	check ("-ve BE put... (*,*)", array, idx,
		     new int[] {
			 0x81,
			 0x82, 0x83,
			 0x84, 0x85, 0x86, 0x87,
			 0x88, 0x89, 0x8a, 0x8b, 0x8c, 0x8d, 0x8e, 0x8f
		     });

	// Switch to little-endian, -ve values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x89, idx);
	b.putByte (idx + 0x0, (byte) 0x90);
	b.putShort (idx + 0x1, (short) 0x9192);
	b.putInt (idx + 0x3, (int) 0x93949596);
	b.putLong (idx + 0x7, (long) 0x9798999a9b9c9d9eL);
	check ("-ve LE put... (*,*)", array, idx,
		     new int[] {
			 0x90,
			 0x92, 0x91,
			 0x96, 0x95, 0x94, 0x93,
			 0x9e, 0x9d, 0x9c, 0x9b, 0x9a, 0x99, 0x98, 0x97
		     });

	// Switch to big-endian, -ve unsigned values.
	b.order (ByteOrder.BIG_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x89, idx);
	b.putUByte (idx + 0x0, (byte) 0x9f);
	b.putUShort (idx + 0x1, (short) 0xa0a1);
	b.putUInt (idx + 0x3, (int) 0xa2a3a4a5);
	b.putULong (idx + 0x7, (long) 0xa6a7a8a9aaabacadL);
	check ("-ve BE putU (*,*)", array, idx,
		     new int[] {
			 0x9f,
			 0xa0, 0xa1,
			 0xa2, 0xa3, 0xa4, 0xa5,
			 0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xab, 0xac, 0xad
		     });

	// Switch to little-endian, -ve unsigned values.
	b.order (ByteOrder.LITTLE_ENDIAN);
	clearArray (array);
	idx = (int) b.position ();
	assertEquals ("position", 0x89, idx);
	b.putUByte (idx + 0x0, (byte) 0xae);
	b.putUShort (idx + 0x1, (short) 0xafb0);
	b.putUInt (idx + 0x3, (int) 0xb1b2b3b4);
	b.putULong (idx + 0x7, (long) 0xb5b6b7b8b9babbbcL);
	check ("-ve LE putU... (*,*)", array, idx,
		     new int[] {
			 0xae,
			 0xb0, 0xaf,
			 0xb4, 0xb3, 0xb2, 0xb1,
			 0xbc, 0xbb, 0xba, 0xb9, 0xb8, 0xb7, 0xb6, 0xb5
		     });
    }
}
