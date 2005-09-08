// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

public class PtraceByteBuffer
    extends ByteBuffer
{
    static public class Area
    {
	protected int area;
	private Area (int area)
	{
	    this.area = area;
	}
	static private native Area textArea ();
	static private native Area dataArea ();
	static private native Area usrArea ();
	static public final Area TEXT = textArea ();
	static public final Area DATA = dataArea ();
	static public final Area USR = usrArea ();
    }

    protected Area area;
    protected int pid;

    public PtraceByteBuffer (int pid, Area area)
    {
	super (0, 0);
	this.pid = pid;
	this.area = area;
    }
    public PtraceByteBuffer (int pid, Area area, long maxOffset)
    {
	super (0, maxOffset);
	this.pid = pid;
	this.area = area;
    }
    protected native int peek (long index);
    protected void poke (long index, int value)
    {
	throw new RuntimeException ("not implemented");
    }
    protected native long peek (long index, byte[] bytes, long off,
				long len);
}

