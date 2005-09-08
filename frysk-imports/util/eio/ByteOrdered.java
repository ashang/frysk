// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

abstract class ByteOrdered
{
    ByteOrder byteOrder;
    ByteOrdered (ByteOrder byteOrder)
    {
	this.byteOrder = byteOrder;
    }
    abstract short peekShort (ByteBuffer b);
    abstract short peekShort (ByteBuffer b, long cursor);
    abstract int peekUShort (ByteBuffer b);
    abstract int peekUShort (ByteBuffer b, long cursor);
    abstract int peekInt (ByteBuffer b);
    abstract int peekInt (ByteBuffer b, long cursor);
    abstract long peekUInt (ByteBuffer b);
    abstract long peekUInt (ByteBuffer b, long cursor);
    abstract long peekLong (ByteBuffer b);
    abstract long peekLong (ByteBuffer b, long cursor);
    abstract long peekULong (ByteBuffer b);
    abstract long peekULong (ByteBuffer b, long cursor);
    
    abstract void pokeShort (ByteBuffer b, short v);
    abstract void pokeShort (ByteBuffer b, long cursor, short v);
    abstract void pokeUShort (ByteBuffer b, int v);
    abstract void pokeUShort (ByteBuffer b, long cursor, int v);
    abstract void pokeInt (ByteBuffer b, int v);
    abstract void pokeInt (ByteBuffer b, long cursor, int v);
    abstract void pokeUInt (ByteBuffer b, long v);
    abstract void pokeUInt (ByteBuffer b, long cursor, long v);
    abstract void pokeLong (ByteBuffer b, long v);
    abstract void pokeLong (ByteBuffer b, long cursor, long v);
    abstract void pokeULong (ByteBuffer b, long v);
    abstract void pokeULong (ByteBuffer b, long cursor, long v);

    static final ByteOrdered[] byteOrders = {
	new ByteOrdered (ByteOrder.BIG_ENDIAN)
	{
	    short peekShort (ByteBuffer b) { return (short) b.peekBig (2); }
	    short peekShort (ByteBuffer b, long cursor) { return (short) b.peekBig (cursor, 2); }
	    int peekUShort (ByteBuffer b) { return (int) b.peekBig (2); }
	    int peekUShort (ByteBuffer b, long cursor) { return (int) b.peekBig (cursor, 2); }
	    int peekInt (ByteBuffer b) { return (int) b.peekBig (4); }
	    int peekInt (ByteBuffer b, long cursor) { return (int) b.peekBig (cursor, 4); }
	    long peekUInt (ByteBuffer b) { return (long) b.peekBig (4); }
	    long peekUInt (ByteBuffer b, long cursor) { return (long) b.peekBig (cursor, 4); }
	    long peekULong (ByteBuffer b) { return (long) b.peekBig (8); }
	    long peekULong (ByteBuffer b, long cursor) { return (long) b.peekBig (cursor, 8); }
	    long peekLong (ByteBuffer b) { return (long) b.peekBig (8); }
	    long peekLong (ByteBuffer b, long cursor) { return (long) b.peekBig (cursor, 8); }
	    
	    void pokeShort (ByteBuffer b, short v) { b.pokeBig (2, v); }
	    void pokeShort (ByteBuffer b, long cursor, short v) { b.pokeBig (cursor, 2, v); }
	    void pokeUShort (ByteBuffer b, int v) { b.pokeBig (2, v); }
	    void pokeUShort (ByteBuffer b, long cursor, int v) { b.pokeBig (cursor, 2, v); }
	    void pokeInt (ByteBuffer b, int v) { b.pokeBig (4, v); }
	    void pokeInt (ByteBuffer b, long cursor, int v) { b.pokeBig (cursor, 4, v); }
	    void pokeUInt (ByteBuffer b, long v) { b.pokeBig (4, v); }
	    void pokeUInt (ByteBuffer b, long cursor, long v) { b.pokeBig (cursor, 4, v); }
	    void pokeLong (ByteBuffer b, long v) { b.pokeBig (8, v); }
	    void pokeLong (ByteBuffer b, long cursor, long v) { b.pokeBig (cursor, 8, v); }
	    void pokeULong (ByteBuffer b, long v) { b.pokeBig (8, v); }
	    void pokeULong (ByteBuffer b, long cursor, long v) { b.pokeBig (cursor, 8, v); }
	},
	new ByteOrdered (ByteOrder.LITTLE_ENDIAN)
	{
	    short peekShort (ByteBuffer b) { return (short) b.peekLittle (2); }
	    short peekShort (ByteBuffer b, long cursor) { return (short) b.peekLittle (cursor, 2); }
	    int peekUShort (ByteBuffer b) { return (int) b.peekLittle (2); }
	    int peekUShort (ByteBuffer b, long cursor) { return (int) b.peekLittle (cursor, 2); }
	    int peekInt (ByteBuffer b) { return (int) b.peekLittle (4); }
	    int peekInt (ByteBuffer b, long cursor) { return (int) b.peekLittle (cursor, 4); }
	    long peekUInt (ByteBuffer b) { return (long) b.peekLittle (4); }
	    long peekUInt (ByteBuffer b, long cursor) { return (long) b.peekLittle (cursor, 4); }
	    long peekULong (ByteBuffer b) { return (long) b.peekLittle (8); }
	    long peekULong (ByteBuffer b, long cursor) { return (long) b.peekLittle (cursor, 8); }
	    long peekLong (ByteBuffer b) { return (long) b.peekLittle (8); }
	    long peekLong (ByteBuffer b, long cursor) { return (long) b.peekLittle (cursor, 8); }
	    void pokeShort (ByteBuffer b, short v) { b.pokeLittle (2, v); }
	    void pokeShort (ByteBuffer b, long cursor, short v) { b.pokeLittle (cursor, 2, v); }
	    void pokeUShort (ByteBuffer b, int v) { b.pokeLittle (2, v); }
	    void pokeUShort (ByteBuffer b, long cursor, int v) { b.pokeLittle (cursor, 2, v); }
	    void pokeInt (ByteBuffer b, int v) { b.pokeLittle (4, v); }
	    void pokeInt (ByteBuffer b, long cursor, int v) { b.pokeLittle (cursor, 4, v); }
	    void pokeUInt (ByteBuffer b, long v) { b.pokeLittle (4, v); }
	    void pokeUInt (ByteBuffer b, long cursor, long v) { b.pokeLittle (cursor, 4, v); }
	    void pokeLong (ByteBuffer b, long v) { b.pokeLittle (8, v); }
	    void pokeLong (ByteBuffer b, long cursor, long v) { b.pokeLittle (cursor, 8, v); }
	    void pokeULong (ByteBuffer b, long v) { b.pokeLittle (8, v); }
	    void pokeULong (ByteBuffer b, long cursor, long v) { b.pokeLittle (cursor, 8, v); }
	}
    };

    static final ByteOrdered order (ByteOrder bo)
    {
	for (int i = 0; i < byteOrders.length; i++) {
	    if (bo == byteOrders[i].byteOrder) {
		return byteOrders[i];
	    }
	}
	throw new RuntimeException ("Unknown byte-order " + bo);
    }

}
