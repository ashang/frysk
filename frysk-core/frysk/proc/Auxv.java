// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
package frysk.proc;

import util.eio.ByteBuffer;
import util.PrintWriter;

public class Auxv
{
    public int type;
    public long val;
    private Auxv (ByteBuffer buffer)
    {
	type = (int) buffer.getWord ();
	val = buffer.getUWord ();
    }
    public String toString ()
    {
	return ("[Auxv"
		+ "type=" + util.elf.AT.toString (type)
		+ "val=" + val
		+ "]");
    }
    public PrintWriter print (PrintWriter w)
    {
	w.print (-5, type);
	w.print (-21, util.elf.AT.toString (type));
	w.print (-32, util.elf.AT.toPrintString (type));
	w.print (val);
	w.println ();
	return w;
    }

    static public Auxv[] parse (ByteBuffer b)
    {
	// Assume the entire byte buffer is the AUXV; each entry
	// occupies two words.
	int numEntries = (int) (b.capacity () / (b.wordSize () * 2));
	Auxv[] entries = new Auxv[numEntries];
	for (int i = 0; i < numEntries; i++) {
	    entries[i] = new Auxv (b);
	}
	return entries;
    }
}
