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

public class Elf
{
    ElfBuffer buffer;

    public Elf (String name)
    {
	buffer = new ElfBuffer (name);
    }

    byte[] getBytes (int numBytes)
    {
	byte[] bytes = new byte[numBytes];
	buffer.get (bytes);
	return bytes;
    }

    private Ehdr ehdr;
    public Ehdr getEhdr ()
    {
	if (ehdr == null)
	    ehdr = new Ehdr (buffer);
	return ehdr;
    }
    public void setEhdr (Ehdr ehdr)
    {
	this.ehdr = ehdr;
    }

    private Shdr[] shdrs;
    public Shdr[] getShdrs ()
    {
	if (shdrs == null) {
	    Ehdr ehdr = getEhdr ();
	    if (ehdr.shnum > 0) {
		buffer.position (ehdr.shoff);
		shdrs = new Shdr[ehdr.shnum];
		for (int i = 0; i < ehdr.shnum; i++) {
		    shdrs[i] = new Shdr (this, buffer, i);
		}
	    }
	}
	return shdrs;
    }
    public void setShdrs (Shdr[] shdrs)
    {
	this.shdrs = shdrs;
    }

    private Phdr[] phdrs;
    public Phdr[] getPhdrs ()
    {
	if (phdrs == null) {
	    Ehdr ehdr = getEhdr ();
	    if (ehdr.phnum > 0) {
		buffer.position (ehdr.phoff);
		phdrs = new Phdr[ehdr.phnum];
		for (int i = 0; i < ehdr.phnum; i++) {
		    phdrs[i] = new Phdr (this, buffer, i);
		}
	    }
	}

	return phdrs;
    }
    public void setPhdrs (Phdr[] phdrs)
    {
	this.phdrs = phdrs;
    }

    public Shdr getShdrByName (String name)
    {
	Shdr[] shdrs = getShdrs ();
	for (int i = 0; i < shdrs.length; i++) {
	    if (name.equals (shdrs[i].name ()))
		return shdrs[i];
	}
	throw new RuntimeException ("Section " + name + " not found");
    }

    public Shdr shdrByType (int type)
    {
	Shdr[] shdrs = getShdrs ();
	for (int i = 0; i < shdrs.length; i++) {
	    if (shdrs[i].type == type)
		return shdrs[i];
	}
	return null;
    }

    public Shdr shdrByName (String name)
    {
	Shdr[] shdrs = getShdrs ();
	for (int i = 0; i < shdrs.length; i++) {
	    if (name.equals (shdrs[i].name ()))
		return shdrs[i];
	}
	throw new RuntimeException ("Section " + name + " not found");
    }

}

