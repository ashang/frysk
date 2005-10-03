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

package inua.elf;

import inua.eio.ByteBuffer;
import inua.eio.MmapByteBuffer;

public class ElfBuffer
{
    ByteBuffer buffer;

    ElfBuffer (ByteBuffer b)
    {
	buffer = b;
    }

    ElfBuffer (String n)
    {
	this (new MmapByteBuffer (n));
    }

    public ElfBuffer slice (long offset, long length)
    {
	return new ElfBuffer (buffer.slice (offset, length));
    }

    public long position ()
    {
	return buffer.position ();
    }

    public ElfBuffer position (long pos)
    {
	buffer.position (pos);
	return this;
    }

    public boolean hasRemaining ()
    {
	return buffer.hasRemaining ();
    }

    public long capacity ()
    {
	return buffer.capacity ();
    }

    public long limit ()
    {
	return buffer.limit ();
    }

    public ElfBuffer get (byte[] bytes)
    {
	buffer.get (bytes);
	return this;
    }

    public ElfBuffer get (byte[] bytes, int off, int len)
    {
	buffer.get (bytes, off, len);
	return this;
    }

    public boolean getBooleanByte () { return buffer.getByte () != 0; }
    public byte getSignedByte () { return buffer.getByte (); }
    public short getUnsignedByte () { return buffer.getUByte (); }
    public short getSignedHalf () { return buffer.getShort (); }
    public int getUnsignedHalf () { return buffer.getUShort (); }
    public long getSignedWord () { return buffer.getInt (); }
    public long getUnsignedWord () { return buffer.getUInt (); }
    public long getFileOffset () { return buffer.getUInt (); }
    public long getAddress () { return buffer.getUInt (); }
    public int getSection () { return buffer.getUShort (); }

    public void putBooleanByte (boolean b) { buffer.putByte ((byte) (b ? 1 : 0)); }
    public void putSignedByte (byte b) { buffer.putByte (b); }
    public void putUnsignedByte (short b) { buffer.putUByte (b); }
    public void putSignedHalf (short b) { buffer.putShort (b); }
    public void putUnsignedHalf (int b) { buffer.putUShort (b); }
    public void putSignedWord (long b) { buffer.putInt ((int) b); }
    public void putUnsignedWord (long b) { buffer.putUInt (b); }
    public void putFileOffset (long b) { buffer.putUInt (b); }
    public void putAddress (long b) { buffer.putUInt (b); }
    public void putSection (int b) { buffer.putUShort (b); }

    public byte getSignedByte (long p) { return buffer.getByte (p); }

    public ElfBuffer get (StringBuffer string)
    {
	string.setLength (0);
	while (hasRemaining ()) {
	    byte b = getSignedByte ();
	    if (b == 0)
		break;
	    string.append ((char) b);
	}
	return this;
    }

    public ElfBuffer get (long p, StringBuffer string)
    {
	string.setLength (0);
	long offset = p;
	while (offset < buffer.limit ()) {
	    byte b = getSignedByte (offset);
	    if (b == 0)
		break;
	    string.append ((char) b);
	    offset++;
	}
	return this;
    }

    public ElfBuffer get (long p, long l, StringBuffer string)
    {
	string.setLength (0);
	long offset = p;
	long limit = p + l;
	while (offset < limit && offset < buffer.limit ()) {
	    byte b = getSignedByte (offset);
	    if (b == 0) // Still valid?
		break;
	    string.append ((char) b);
	    offset++;
	}
	return this;
    }

    public String getString ()
    {
	StringBuffer buf = new StringBuffer ();
	get (buf);
	return buf.toString ();
    }

    public String getString (long p)
    {
	StringBuffer buf = new StringBuffer ();
	get (p, buf);
	return buf.toString ();
    }

    public String getString (long p, long l)
    {
	StringBuffer buf = new StringBuffer ();
	get (p, l, buf);
	return buf.toString ();
    }

}
