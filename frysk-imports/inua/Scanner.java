// This file is part of INUA.  Copyright 2004, 2005, Andrew Cagney
//
// INUA is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// INUA is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with INUA; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Andrew Cagney. gives You the
// additional right to link the code of INUA with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of INUA through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Andrew Cagney may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the INUA code and other code
// used in conjunction with INUA except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package inua;

import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.CharConversionException;

public class Scanner
{

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
	int count = 0;
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

