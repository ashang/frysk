// From the VENUS project.  Copyright 2004, 2005, Andrew Cagney
// Licenced under the terms of the Eclipse Public Licence.
// Licenced under the terms of the GNU CLASSPATH Licence.

/** PrintWriter
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

package util;

public class PrintWriter
    extends java.io.PrintWriter
{
    // PrintWriter stdout = new PrintWriter (System.out);
    // PrintWriter stderr = new PrintWriter (System.err);

    public PrintWriter (java.io.Writer writer)
    {
	super (writer);
    }
    public PrintWriter (java.io.Writer writer, boolean flush)
    {
	super (writer, flush);
    }

    public PrintWriter (java.io.OutputStream out)
    {
	super (out);
    }

    public PrintWriter (java.io.OutputStream out, boolean flush)
    {
	super (out, flush);
    }

    private final PrintWriter pad (int width, char padding)
    {
	for (int i = 0; i < width; i++)
	    write (padding);
	return this;
    }

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
    public final PrintWriter print (int width, String s)
    {
	return print (width, ' ', s);
    }

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
    public final PrintWriter print (int width, char c)
    {
	return print (width, ' ', c);
    }

    private char[] buffer = new char[64];
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

    static private final char[] dec = {
	'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };
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
    // Print L in decimal, if L is less than |WIDTH| characters pad to
    // WIDTH with PADDING.
    public final PrintWriter print (int width, char padding, long l)
    {
	return printBuffer (width, padding, toDec (l));
    }
    public final PrintWriter print (int width, long l)
    {
	return print (width, ' ', l);
    }

    // Print V in decimal, always include a leading sign.
    public final PrintWriter printp (int width, char padding, long l)
    {
	int length = toDec (l);
	if (l > 0)
	    buffer[--length] = '+';
	return printBuffer (width, ' ', length);
    }
    public final PrintWriter printp (int width, long l)
    {
	return printp (width, ' ', l);
    }
    public final PrintWriter printp (long l)
    {
	return printp (0, l);
    }

    static final char[] hex = {
	'0', '1', '2', '3', '4', '5', '6', '7',
	'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
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
    public final PrintWriter printx (int width, char padding, long l)
    {
	return printBuffer (width, padding, toHex (16, l));
    }
    public final PrintWriter printx (int width, long l)
    {
	return printx (width, ' ', l);
    }
    public final PrintWriter printx (long l)
    {
	return printx (0, ' ', l);
    }
}
