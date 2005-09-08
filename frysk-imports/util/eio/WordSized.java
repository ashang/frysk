// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

abstract class WordSized
{
    WordSized (int wordSize)
    {
	this.wordSize = wordSize;
    }
    int wordSize;
    abstract long getWord (ByteBuffer b);
    abstract long getUWord (ByteBuffer b);
    abstract void putWord (ByteBuffer b, long w);
    abstract void putUWord (ByteBuffer b, long w);

    private static final WordSized[] wordSizes = {
	new WordSized (2)
	{
	    long getWord (ByteBuffer b) { return b.getShort (); }
	    long getUWord (ByteBuffer b) { return b.getUShort (); }
	    void putWord (ByteBuffer b, long w) { b.putShort ((short) w); }
	    void putUWord (ByteBuffer b, long w) { b.putUShort ((int) w); }
	},
	new WordSized (4)
	{
	    long getWord (ByteBuffer b) { return b.getInt (); }
	    long getUWord (ByteBuffer b) { return b.getUInt (); }
	    void putWord (ByteBuffer b, long w) { b.putInt ((int) w); }
	    void putUWord (ByteBuffer b, long w) { b.putUInt (w); }
	},
	new WordSized (8)
	{
	    long getWord (ByteBuffer b) { return b.getLong (); }
	    long getUWord (ByteBuffer b) { return b.getULong (); }
	    void putWord (ByteBuffer b, long w) { b.putLong (w); }
	    void putUWord (ByteBuffer b, long w) { b.putULong (w); }
	}
    };

    static WordSized wordSize (int w)
    {
	for (int i = 0; i < wordSizes.length; i++) {
	    if (w == wordSizes[i].wordSize) {
		return wordSizes[i];
	    }
	}
	throw new RuntimeException ("Bad word size " + w);
    }

}
