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

import inua.eio.ByteOrder;

public class Ehdr
{
    public int type;
    public int machine;
    public long version;
    public long entry;
    public long phoff;
    public long shoff;
    public long flags;
    public int ehsize;
    public long phentsize;
    public int phnum;
    public long shentsize;
    public int shnum;
    public int shstrndx;

    public byte[] ident;
    public byte[] header;

    public Ehdr (ElfBuffer buffer)
    {
	// Pull in the ident and verify it.
	ident = new byte[EI.NIDENT];
	buffer.get (ident);
	if (! (ident[EI.MAG0] == ELF.MAG._0
	       && ident[EI.MAG1] == ELF.MAG._1
	       && ident[EI.MAG2] == ELF.MAG._2
	       && ident[EI.MAG3] == ELF.MAG._3))
	    throw new RuntimeException ("Bad magic number");
	if (! (ident[EI.VERSION] == EV.CURRENT))
	    throw new RuntimeException ("Bad ELF version");

	/* Get the word size.  */
	switch (ident[EI.CLASS]) {
	case ELF.CLASS._32:
	    buffer.buffer.wordSize (4);
	    break;
	case ELF.CLASS._64:
	    buffer.buffer.wordSize (8);
	    break;
	default:
	    throw new RuntimeException ("Bad class");
	}

	/* Select a reader (...).  */
	switch (ident[EI.DATA]) {
	case ELF.DATA._2LSB:
	    buffer.buffer.order (ByteOrder.LITTLE_ENDIAN);
	    break;
	case ELF.DATA._2MSB:
	    buffer.buffer.order (ByteOrder.BIG_ENDIAN);
	    break;
	default:
	    throw new RuntimeException ("Bad byte order");
	}

	type = buffer.getSignedHalf ();
	machine = buffer.getSignedHalf ();
	version = buffer.getSignedWord ();
	entry = buffer.getSignedWord ();
	phoff = buffer.getSignedWord ();
	shoff = buffer.getSignedWord ();
	flags = buffer.getSignedWord ();
	ehsize = buffer.getSignedHalf ();
	phentsize = buffer.getSignedHalf ();
	phnum = buffer.getSignedHalf ();
	shentsize = buffer.getSignedHalf ();
	shnum = buffer.getSignedHalf ();
	shstrndx = buffer.getSignedHalf ();
    }
}

