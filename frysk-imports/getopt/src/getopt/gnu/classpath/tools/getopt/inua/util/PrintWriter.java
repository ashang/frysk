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

package inua.util;

import java.io.Writer;
import java.io.OutputStream;

/**
 * An extension to Java's Writer object adding lots of convenient
 * print methods.
 *
 * Some background.  Java has three I/O models: streams (1.0), readers
 * and writers (1.1), and nio (1.4).  What isn't so obvious is which
 * is prefered (especially given that System.out is a stream).  It
 * turns out that the latter two two, readers and writers for serial
 * I/O, and nio for block I/O random access are the way to go.
 *
 * This class extends the <quote>prefered</quote> serial interface -
 * PrintWriter - interface adding mechanisms to simplify the printing
 * and formatting of decimal, hexadecimal, and string values.  It also
 * adds stdin, stdout, and stderr statics for printing.
 *
 * You'll also notice that this adopts the 1.5 style of returning the
 * object rather than the 1.1 style of returning void.
 *
 * This code makes the simplistic assumption that characters and
 * padding characters both occupy a single space on the terminal (I
 * think this is true :-).
 */

public class PrintWriter
    extends java.io.PrintWriter
{
    // PrintWriter stdout = new PrintWriter (System.out);
    // PrintWriter stderr = new PrintWriter (System.err);

    /**
     * Construct from a Writer.
     */
    public PrintWriter (Writer writer)
    {
	super (writer);
    }
    /**
     * Construct from a Writer, possibly enable auto-flush.
     */
    public PrintWriter (Writer writer, boolean flush)
    {
	super (writer, flush);
    }
    /**
     * Construct from an OutputStream.
     */
    public PrintWriter (OutputStream out)
    {
	super (out);
    }
    /**
     * Construct from an OutputStream, possibly enable auto-flush.
     */
    public PrintWriter (OutputStream out, boolean flush)
    {
	super (out, flush);
    }

    /**
     * Pad the output with WIDTH characters of PADDING.
     */
    private final PrintWriter pad (int width, char padding)
    {
	for (int i = 0; i < width; i++)
	    write (padding);
	return this;
    }

    /**
     * Print a String in a field at least |WIDTH| characters wide,
     * padding with PADDING when necessary.  If WIDTH is +ve, add
     * padding on the left, otherwize add padding on the right.
     */
    public final PrintWriter print (int width, char padding, String s)
    {
	if (width >= 0) {
	    pad (width - s.length (), padding);
	    write (s);
	}
	else {
	    write (s);
	    pad (-width - s.length (), padding);
	}
	return this;
    }
    /**
     * Print a String in a field at least |WIDTH| characters wide,
     * padding with spaces when necessary.  If WIDTH is +ve, add
     * padding on the left, otherwize add padding on the right.
     */
    public final PrintWriter print (int width, String s)
    {
	return print (width, ' ', s);
    }

    /**
     * Print a character in a field at least |WIDTH| characters wide,
     * padding with PADDING when necessary.  If WIDTH is +ve, add
     * padding on the left, otherwize add padding on the right.
     */
    public final PrintWriter print (int width, char padding, char c)
    {
	if (width >= 0) {
	    pad (width - 1, padding);
	    write (c);
	}
	else {
	    write (c);
	    pad (-width - 1, padding);
	}
	return this;
    }
    /**
     * Print a character in a field at least |WIDTH| characters wide,
     * padding with spaces when necessary.  If WIDTH is +ve, add
     * padding on the left, otherwize add padding on the right.
     */
    public final PrintWriter print (int width, char c)
    {
	return print (width, ' ', c);
    }

    /**
     * Scratch buffer (used for printing numbers).
     */
    private char[] buffer = new char[64];
    /**
     * Print buffer[offset...], padding out to |WIDTH| when necessary.
     */
    private final PrintWriter printBuffer (int width, char padding, int offset)
    {
	int length = buffer.length - offset;
	if (width >= 0) {
	    pad (width - length, padding);
	    write (buffer, offset, length);
	}
	else {
	    write (buffer, offset, length);
	    pad (-width - length, padding);
	}
	return this;
    }

    /**
     * Decimal to hex converter.
     */
    static private final char[] dec = {
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };
    /**
     * Convert V into a decimal string stored in BUFFER, return the
     * offset of the first character.
     */
    private final int toDec (long v)
    {
	int start = buffer.length;
	long l;
	if (v < 0)
	    l = -v;
	else
	    l = v;
	do {
	    buffer[--start] = dec[(int) (l % 10)];
	    l /= 10;
	} while (l != 0);
	if (v < 0)
	    buffer[--start] = '-';
	return start;
    }
    /**
     * Print L in decimal, if L is less than |WIDTH| characters pad to
     * WIDTH with PADDING.
     */
    public final PrintWriter print (int width, char padding, long l)
    {
	return printBuffer (width, padding, toDec (l));
    }
    /**
     * Print L in decimal, if L is less than |WIDTH| characters pad to
     * WIDTH with spaces.
     */
    public final PrintWriter print (int width, long l)
    {
	return print (width, ' ', l);
    }

    /**
     * Print V in decimal, always include a leading sign.  If L is
     * less than |WIDTH| characters pad with PADDING.
     */
    public final PrintWriter printp (int width, char padding, long l)
    {
	int length = toDec (l);
	if (l > 0)
	    buffer[--length] = '+';
	return printBuffer (width, ' ', length);
    }
    /**
     * Print V in decimal, always include a leading sign.  If L is
     * less than |WIDTH| characters pad with spaces.
     */
    public final PrintWriter printp (int width, long l)
    {
	return printp (width, ' ', l);
    }
    /**
     * Print V in decimal, always include a leading sign.
     */
    public final PrintWriter printp (long l)
    {
	return printp (0, l);
    }

    /**
     * Hex to char converter.
     */
    static private final char[] hex = {
	'0', '1', '2', '3', '4', '5', '6', '7',
	'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    /**
     * Convert V to an unsigned hexadecimal string, of up to WIDTH
     * characters, stored in buffer, return the start of the string.
     */
    private final int toHex (int width, long v)
    {
	int start = buffer.length;
	do {
	    buffer[--start] = hex[(int) (v & 0xf)];
	    v >>= 4;
	    // If -ve, padd out to width with "ff".
	} while (v != 0 && (v != -1 || start + width > buffer.length));
	return start;
    }
    /**
     * Print L as a hexadecimal string, padding out to |WIDTH| with
     * PADDING when necessary.
     */
    public final PrintWriter printx (int width, char padding, long l)
    {
	return printBuffer (width, padding, toHex (16, l));
    }
    /**
     * Print L as a hexadecimal string, padding out to |WIDTH| with
     * spaces when necessary.
     */
    public final PrintWriter printx (int width, long l)
    {
	return printx (width, ' ', l);
    }
    /**
     * Print L as a hexadecimal string.
     */
    public final PrintWriter printx (long l)
    {
	return printx (0, ' ', l);
    }
}
