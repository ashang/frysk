// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

public abstract class ByteBuffer
    extends Buffer
{
    protected ByteBuffer (long lowWater, long highWater)
    {
	super (lowWater, highWater);
	order (ByteOrder.BIG_ENDIAN);
	wordSize (4);
    }

    protected abstract int peek (long caret);
    protected abstract void poke (long caret, int val);

    protected long peek (long caret, byte[] bytes,
			 long off, long len)
    {
	for (long i = 0; i < len; i++) {
	    bytes[(int) (off + i)] = (byte) peek (caret + i);
	}
	return len;
    }
    protected long poke (long caret, byte[] bytes, long off, long len)
    {
	for (long i = 0; i < len; i++) {
	    poke (caret + i, bytes[(int) (off + i)]);
	}
	return len;
    }

    protected final void peekFully (long caret, byte[] bytes,
				    long off, long len)
    {
	while (len > 0) {
	    long xfer = peek (caret, bytes, off, len);
	    off += xfer;
	    len -= xfer;
	    caret += xfer;
	}
    }

    protected final void pokeFully (long caret, byte[] bytes,
				    long off, long len)
    {
	while (len > 0) {
	    long xfer = poke (caret, bytes, off, len);
	    off += xfer;
	    len -= xfer;
	    caret += xfer;
	}
    }

    byte[] scratch = new byte[8];
    protected final long peekBig (long caret, int len)
    {
	peekFully (caret, scratch, 0, len);
	long value = 0;
	for (int i = 0; i < len; i++) {
	    value = (value << 8) | (scratch[i] & 0xffL);
	}
	return value;
    }
    protected final void pokeBig (long caret, int len, long value)
    {
	for (int i = len - 1; i >= 0; i--) {
	    scratch[i] = (byte) (value & 0xff);
	    value >>= 8;
	}
	pokeFully (caret, scratch, 0, len);
    }

    protected final long peekLittle (long caret, int len)
    {
	peekFully (caret, scratch, 0, len);
	long value = 0;
	for (int i = 0; i < len; i++) {
	    value |=  (scratch[i] & 0xffL) << i * 8;
	}
	return value;
    }
    protected final void pokeLittle (long caret, int len, long value)
    {
	for (int i = 0; i < len; i++) {
	    scratch[i] = (byte) (value & 0xff);
	    value >>= 8;
	}
	pokeFully (caret, scratch, 0, len);
    }

    protected final long peekBig (int len)
    {
	long v = peekBig (cursor, len);
	cursor += len;
	return v;
    }
    protected final void pokeBig (int len, long value)
    {
	pokeBig (cursor, len, value);
	cursor += len;
    }

    protected final long peekLittle (int len)
    {
	long v = peekLittle (cursor, len);
	cursor += len;
	return v;
    }
    protected final void pokeLittle (int len, long value)
    {
	pokeLittle (cursor, len, value);
	cursor += len;
    }

    public static ByteBuffer allocateDirect (long capacity)
	throws IllegalArgumentException
    {
	return wrap (new byte[(int) capacity]);
    }
    public static ByteBuffer allocate (long capacity)
	throws IllegalArgumentException
    {
	return allocateDirect (capacity);
    }
    public static ByteBuffer wrap (byte[] bytes, long off, long len)
	throws IndexOutOfBoundsException
    {
	// return new ByteArrayBuffer (bytes, off, len);
	return null;
    }
    public static ByteBuffer wrap (byte[] bytes)
    {
	return wrap (bytes, 0, bytes.length);
    }

    public ByteBuffer slice (long off, long len)
    {
	ByteBuffer newSlice;
	try {
	    Class[] constructorArgsClass = new Class[] {
		long.class, long.class
	    };
	    java.lang.reflect.Constructor constructor =
		getClass ().getConstructor (constructorArgsClass);
	    Object[] constructorArgs = new Object[] {
		new Long (lowWater + off), new Long (lowWater + off + len)
	    };
	    newSlice = (ByteBuffer) constructor.newInstance (constructorArgs);
	}
	catch (Exception e) {
	    throw new RuntimeException (e);
	}
	newSlice.order (order ());
	newSlice.wordSize (wordSize ());
	return newSlice;
    }
    public ByteBuffer slice ()
    {
	return slice (position (), limit ());
    }

    public final byte get ()
    {
	return (byte) peek (cursor++);
    }
    public final byte get (long index)
    {
	return (byte) peek (lowWater + index);
    }

    public ByteBuffer get (long index, byte[] dst, int off, int len)
	throws BufferUnderflowException
    {
	if (index + len > remaining ())
	    throw new BufferUnderflowException ();
	if (index < 0)
	    throw new BufferOverflowException ();
	peek (lowWater + index, dst, off, len);
	return this;
    }
    public ByteBuffer get (byte[] dst, int off, int len)
	throws BufferUnderflowException
    {
	if (len > remaining ())
	    throw new BufferUnderflowException ();
	peek (cursor, dst, off, len);
	cursor += len;
	return this;
    }
    public final ByteBuffer get (byte[] dst)
	throws BufferUnderflowException
    {
	return get (dst, 0, dst.length);
    }

    protected ByteOrdered byteOrdered;
    public final ByteOrder order ()
    {
	return byteOrdered.byteOrder;
    }

    public final ByteBuffer order (ByteOrder bo)
    {
	byteOrdered = ByteOrdered.order (bo);
	return this;
    }

    public final byte getByte () { return (byte) peek (cursor++); }
    public final byte getByte (long index) { return (byte) peek (lowWater + index); }
    public final short getUByte () { return (short) peek (cursor++); }
    public final short getUByte (long index) { return (short) peek (lowWater + index); }

    public final void putByte (byte v) { poke (cursor++, v); }
    public final void putByte (long index,  byte v) { poke (lowWater + index, v); }
    public final void putUByte (short v) { poke (cursor++, v); }
    public final void putUByte (long index,  short v) { poke (lowWater + index, v); }

    public final short getShort () { return byteOrdered.peekShort (this); }
    public final short getShort (long index) { return byteOrdered.peekShort (this, lowWater + index); }
    public final int getUShort () { return byteOrdered.peekUShort (this); }
    public final int getUShort (long index) { return byteOrdered.peekUShort (this, lowWater + index); }
    public final int getInt () { return byteOrdered.peekInt (this); }
    public final int getInt (long index) { return byteOrdered.peekInt (this, lowWater + index); }
    public final long getUInt () { return byteOrdered.peekUInt (this); }
    public final long getUInt (long index) { return byteOrdered.peekUInt (this, lowWater + index); }
    public final long getULong () { return byteOrdered.peekULong (this); }
    public final long getULong (long index) { return byteOrdered.peekULong (this, lowWater + index); }
    public final long getLong () { return byteOrdered.peekLong (this); }
    public final long getLong (long index) { return byteOrdered.peekLong (this, lowWater + index); }

    public final void putShort (short v) { byteOrdered.pokeShort (this, v); }
    public final void putShort (long index,  short v) { byteOrdered.pokeShort (this, lowWater + index, v); }
    public final void putUShort (int v) { byteOrdered.pokeUShort (this, v); }
    public final void putUShort (long index,  int v) { byteOrdered.pokeUShort (this, lowWater + index, v); }
    public final void putInt (int v) { byteOrdered.pokeInt (this, v); }
    public final void putInt (long index,  int v) { byteOrdered.pokeInt (this, lowWater + index, v); }
    public final void putUInt (long v) { byteOrdered.pokeUInt (this, v); }
    public final void putUInt (long index,  long v) { byteOrdered.pokeUInt (this, lowWater + index, v); }
    public final void putULong (long v) { byteOrdered.pokeULong (this, v); }
    public final void putULong (long index,  long v) { byteOrdered.pokeULong (this, lowWater + index, v); }
    public final void putLong (long v) { byteOrdered.pokeLong (this, v); }
    public final void putLong (long index,  long v) { byteOrdered.pokeLong (this, lowWater + index, v); }


    public final long getWord () { return wordSized.getWord (this); }
    public final long getUWord () { return wordSized.getUWord (this); }
    public final void putWord (long w) { wordSized.putWord (this, w); }
    public final void putUWord (long w) { wordSized.putUWord (this, w); }

    protected WordSized wordSized;
    public ByteBuffer wordSize (int w)
    {
	wordSized = WordSized.wordSize (w);
	return this;
    }
    public int wordSize ()
    {
	return wordSized.wordSize;
    }

    public ByteBuffer get (StringBuffer string)
    {
	string.setLength (0);
	while (hasRemaining ()) {
	    byte b = (byte) peek (cursor++);
	    if (b == 0)
		break;
	    string.append ((char) b);
	}
	return this;
    }

    public ByteBuffer get (long index, StringBuffer string)
    {
	string.setLength (0);
	long offset = lowWater + index;
	while (offset < bound) {
	    byte b = (byte) peek (offset);
	    if (b == 0)
		break;
	    string.append ((char) b);
	    offset++;
	}
	return this;
    }

    public ByteBuffer get (long index, long len, StringBuffer string)
    {
	string.setLength (0);
	long offset = lowWater + index;
	while (offset < bound && len-- > 0) {
	    byte b = (byte) peek (offset);
	    if (b == 0)
		break;
	    string.append ((char) b);
	    offset++;
	}
	return this;
    }

}

