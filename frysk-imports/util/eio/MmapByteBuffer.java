// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

public class MmapByteBuffer
    extends ByteBuffer
{
    private static final class Mmap
    {
	static private native final long mmap (String name, long length);
	long capacity;
	long byteData;
	private Mmap (String name)
	{
	    java.io.File file = new java.io.File (name);
	    capacity = file.length ();
	    byteData = mmap (name, capacity);
	}
    }

    Mmap map;

    private MmapByteBuffer (Mmap map, long lowExtreem, long highExtreem)
    {
	super (lowExtreem, highExtreem);
	this.map = map;
    }

    private MmapByteBuffer (Mmap map)
    {
	this (map, 0, map.capacity);
    }

    public MmapByteBuffer (String name)
    {
	this (new Mmap (name));
    }

    public ByteBuffer slice (long off, long len)
    {
	return new MmapByteBuffer (map, lowWater + off,
				   lowWater + off + len);
    }


    native protected final int peek (long index);
    native protected final void poke (long index, int val);
    
    native protected final long peek (long index, byte[] bytes,
				      long off, long len);
}
