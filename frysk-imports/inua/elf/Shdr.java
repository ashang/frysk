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

public class Shdr
    extends Hdr
{
    public long name;
    public long type;
    public long flags;
    public long addr;
    public long offset;
    public long size;
    public int link;
    public long info;
    public long addralign;
    public long entsize;

    public Shdr (Elf elf, ElfBuffer buffer, int i)
    {
	super (elf, i);
	name = buffer.getSignedWord ();
	type = buffer.getSignedWord ();
	flags = buffer.getSignedWord ();
	addr = buffer.getSignedWord ();
	offset = buffer.getSignedWord ();
	size = buffer.getSignedWord ();
	link = (int) buffer.getSignedWord ();
	info = buffer.getSignedWord ();
	addralign = buffer.getSignedWord ();
	entsize = buffer.getSignedWord ();
	setSlice (offset, size);
    }

    public void put (ElfBuffer s)
    {
	s.putSignedWord (name);
	s.putSignedWord (type);
	s.putSignedWord (flags);
	s.putSignedWord (addr);
	s.putSignedWord (offset);
	s.putSignedWord (size);
	s.putSignedWord (link);
	s.putSignedWord (info);
	s.putSignedWord (addralign);
	s.putSignedWord (entsize);
    }

    // Interpret the section in various ways, if the interpretation
    // isn't valid, return null.

    public Sym[] syms ()
    {
	ElfBuffer symfile = elfBuffer ();
	long nsyms = size / entsize;
	Sym[] syms = new Sym[(int) nsyms];
	for (int i = 0; i < nsyms; i++) {
	    syms[i] = new Sym (symfile);
	}
	return syms;
    }

    public String name ()
    {
	Ehdr ehdr = elf.getEhdr ();
	Shdr[] shdrs = elf.getShdrs ();
	return elf.buffer.getString (shdrs[ehdr.shstrndx].offset + name);
    }

    public String symbolName (long name)
    {
	Shdr[] shdrs = elf.getShdrs ();
	return elf.buffer.getString (shdrs[link].offset + name);
    }
}
