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
package inua.dwarf;

import inua.eio.ByteBuffer;

abstract class Format
    implements Comparable
{
    // Private to stop subclassing, there are only two formats.
    private int  wordSize;
    private Format (int wordSize)
    {
	this.wordSize = wordSize;
    }

    // Probe the initial length to determine the format.  The length
    // proper is read in a separate call.

    static final Format getFormat (ByteBuffer buffer)
    {
	// For 32-bit this results in the length being read twice.  An
	// alternative would be to store the length in a static
	// variable (ulgh!).
	long length = buffer.getUInt (buffer.position ());
	if (length == 0xffffffffL)
	    return format64;
	else
	    return format32;
    }

    abstract long getUWORD (ByteBuffer buffer);
    abstract long getInitialLength (ByteBuffer buffer);
    final long getSectionOffset (ByteBuffer buffer)
    {
	return getUWORD (buffer);
    }
    final long getSectionLength (ByteBuffer buffer)
    {
	return getUWORD (buffer);
    }
    final int wordSize ()
    {
	return wordSize;
    }

    static private Format format32 = new Format (4)
	{
	    final long getUWORD (ByteBuffer buffer)
	    {
		return buffer.getUInt ();
	    }
	    final long getInitialLength (ByteBuffer buffer)
	    {
		long length = buffer.getUInt ();
		if (length == 0xffffffffL)
		    throw new RuntimeException ("botched 32-bit format");
		return length;
	    }
	};

    static private Format format64 = new Format (8)
	{
	    final long getUWORD (ByteBuffer buffer)
	    {
		return buffer.getULong ();
	    }
	    final long getInitialLength (ByteBuffer buffer)
	    {
		long length = buffer.getUInt ();
		if (length != 0xffffffffL)
		    throw new RuntimeException ("botched 64-bit format");
		return buffer.getULong ();
	    }
	};

    // This is compareable and ordered.

    public int hashCode ()
    {
	return wordSize ();
    }
    public boolean equal (Object obj)
    {
	return compareTo (obj) == 0;
    }
    public int compareTo (Object obj)
    {
	if (! (obj instanceof Format))
	    return -1;
	Format rhs = (Format) obj;
	return this.wordSize () - rhs.wordSize ();
    }

    public String toString ()
    {
	return ("[Format"
		+ "," + wordSize ()
		+ "]");
    }
}
