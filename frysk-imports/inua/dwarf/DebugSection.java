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

class DebugSection
{
    // XXX: GCJ: I think "private" is ment to stop derived classes
    // (both within and without the package) from accessing a field.
    // It appears that GCC doesn't, or at least doesn't always,
    // enforce this - the private .section was being accessed by
    // subclasses.

    private ByteBuffer section;
    public DebugSection (ByteBuffer e)
    {
	section = e;
    }

    // Manipulation of the default read position within the section.
    // 0 <= position <= limit.

    public final long position () { return section.position (); }
    public final void position (long p) { section.position (p); }
    public final boolean hasRemaining () { return section.hasRemaining (); }
    public final long limit () { return section.limit (); }

    public final long getSignedLEB128 ()
    {
	byte b;
	long result = 0;
	int shift = 0;
	int size = 64; // Number of bits in signed integer
	while (true) {
	    b = section.getByte (); // Next byte of input
	    result |= ((b & 0x7f) << shift);
	    shift += 7;
	    // Sign bit of byte is 2nd high order byte (0x40)
	    if ((b & 0x80) == 0)
		break;
	}
	if ((shift < size)
	    && ((b & 0x40) != 0)) // Sign bit of byte is set.
	    // Sign extend.
	    result |= - (1 << shift);
	return result;
    }

    public final long getUnsignedLEB128 ()
    {
	byte b;
	long result = 0;
	int shift = 0;
	while (true) {
	    b = section.getByte (); // Next byte in input.
	    result |= (b & 0x7f) << shift;
	    if ((b & 0x80) == 0)
		break;
	    shift += 7;
	}
	return result;
    }

    public final byte getSBYTE () { return section.getByte (); }
    public final short getUBYTE () { return section.getUByte (); }
    public final int getUHALF () { return section.getUShort (); }

    public final byte getSBYTE (long index) { return section.getByte (index); }
    public final short getUBYTE (long index) { return section.getUByte (index); }
    public final int getUHALF (long index) { return section.getUShort (index); }

    public final void skipToZeroUBYTE ()
    {
	while (true) {
	    if (getUBYTE () == 0)
		break;
	}
    }


    public final long getDATA1 () { return section.getUByte (); }
    public final long getDATA2 () { return section.getUShort (); }
    public final long getDATA4 () { return section.getUInt (); }
    public final long getDATA8 () { return section.getULong (); }

    public final Format getFormat ()
    {
	return Format.getFormat (section);
    }
    public final long getInitialLength (Format format)
    {
	return format.getInitialLength (section);
    }
    public final long getSectionOffset (Format format)
    {
	return format.getSectionOffset (section);
    }
    public final long getSectionLength (Format format)
    {
	return format.getSectionLength (section);
    }
    public final long getUWORD (Format format)
    {
	return format.getUWORD (section);
    }

    public final void get (StringBuffer string) { section.get (string); }
    public final void get (long index, StringBuffer string) { section.get (index, string); }
    public final void get (byte[] bytes, int off, int len) { section.get (bytes, off, len); }
    public final byte getByte () { return section.getByte (); }
}
