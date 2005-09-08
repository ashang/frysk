// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util.eio;

import java.io.*;

class ByteFileBuffer
    extends ByteBuffer
{
    private RandomAccessFile file;

    protected int peek (long caret)
    {
	try {
	    file.seek (caret);
	    return file.readByte ();
	}
	catch (java.io.IOException e) {
	    System.out.println (e);
	}
	return -1;
    }

    protected void poke (long caret, int value)
    {
	try {
	    file.seek (caret);
	    file.write (value);
	}
	catch (java.io.IOException e) {
	    System.out.println (e);
	}
    }

    public ByteFileBuffer (File file, String mode)
	throws FileNotFoundException
    {
	super (0, file.length ());
	this.file = new RandomAccessFile (file, mode);
    }

    public boolean isReadOnly ()
    {
	return false;
    }
}

