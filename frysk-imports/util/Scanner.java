// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

package util;

import java.io.*;

public class Scanner {

    public boolean debug;

    private DataInputStream file;
    private int lineNumber;

    public Scanner (String fileName)
    	throws IOException
    {
    	file = new DataInputStream (new FileInputStream (fileName));
    	readAhead ();
    }

    public Scanner (File f)
	throws IOException
    {
	file = new DataInputStream (new FileInputStream (f));
	readAhead ();
    }

    private char lookAhead;
    private void readAhead ()
	throws IOException
    {
	// Delay EOF by one read.
	if (endOfFile ())
	    throw new EOFException ();
	if (lookAhead == '\n')
	    lineNumber++;
	try {
	    lookAhead = (char) file.readByte ();
	} catch (EOFException e) {
	    lookAhead = java.lang.Character.MAX_VALUE;
	}
    }

    public boolean endOfFile ()
    {
	boolean eof = (lookAhead == java.lang.Character.MAX_VALUE);
	if (debug)
	    System.out.println ("endOfFile " + eof);
	return eof;
    }

    public int getLineNumber ()
    {
	return lineNumber;
    }

    public void skipByte (char c)
	throws IOException
    {
	if (lookAhead != c)
	    throw new CharConversionException ("Expecting `" + c + "' got `" + lookAhead); // Syntax error
	readAhead ();
	if (debug)
	    System.out.println ("skipByte `" + c + "'");
    }

    public void skipWhitespace ()
	throws IOException
    {
	if (debug)
	    System.out.print ("skipWhitespace `");
	while (java.lang.Character.isWhitespace (lookAhead)) {
	    readAhead ();
	    if (debug)
		System.out.print (lookAhead);
	}
	if (debug)
	    System.out.println ("'");
    }

    public char readByte ()
	throws IOException
    {
	char b = lookAhead;
	readAhead ();
	if (debug)
	    System.out.println ("readByte `" + b + "'");
	return b;
    }

    public long readHexLong ()
	throws IOException
    {
	long val = 0;
	int count = 0;;
	while (true) {
	    char c = java.lang.Character.toLowerCase (lookAhead);
	    int i = "0123456789abcdef".indexOf (c);
	    if (i < 0)
		break;
	    val = val * 16 + i;
	    readAhead ();
	    count++;
	}
	if (count == 0)
	    throw new CharConversionException ("Expecting hex value");
	if (debug)
	    System.out.println ("readHex `" + val + "'");
	return val;
    }

    public byte readHexByte ()
	throws IOException
    {
	long l = readHexLong ();
	if (l > 0xff)
	    throw new CharConversionException ("Hex Byte out of range");
	return (byte) l;
    }

    public long readDecimalLong ()
	throws IOException
    {
	long val = 0;
	int count = 0;
	while (true) {
	    char c = java.lang.Character.toLowerCase (lookAhead);
	    int i = "0123456789".indexOf (c);
	    if (i < 0)
		break;
	    val = val * 10 + i;
	    readAhead ();
	    count++;
	}
	if (count == 0)
	    throw new CharConversionException ("Expecting long value, got: "
					       + lookAhead);
	if (debug)
	    System.out.println ("readLong `" + val + "'");
	return val;
    }

    public String readLine ()
	throws IOException
    {
	StringBuffer val = new StringBuffer ();
	if (debug)
	    System.out.print ("readLine `");
	while (!java.lang.Character.isISOControl (lookAhead)) {
	    if (debug)
		System.out.print (lookAhead);
	    val.append (lookAhead);
	    readAhead ();
	}
	if (debug)
	    System.out.println ("'");
	return val.toString ();
    }
}

