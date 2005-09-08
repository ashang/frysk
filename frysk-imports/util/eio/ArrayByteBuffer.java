// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

public class ArrayByteBuffer
    extends ByteBuffer
{
    private byte[] array;

    protected int peek (long caret)
    {
	return array[(int) caret] & 0xff;
    }

    protected void poke (long caret, int value)
    {
	array [(int) caret] = (byte) value;
    }

    public ArrayByteBuffer (long capacity)
    {
	super (0, capacity);
	array = new byte[(int) capacity];
    }

    public ArrayByteBuffer (byte[] bytes, long off, long len)
    {
	super (off, len);
	array = bytes;
    }

    public ArrayByteBuffer (byte[] bytes)
    {
	this (bytes, 0, bytes.length);
    }

    public boolean isReadOnly ()
    {
	return false;
    }
}

