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

import inua.PrintWriter;

public class PrintSym
{
    Elf elf;
    public PrintSym (Elf elf)
    {
	this.elf = elf;
    }

    class PrintBuilder
	extends SymSection.Builder
    {
	PrintWriter o;
	int y = 0;
	Shdr shdr;
	PrintBuilder (PrintWriter o, Shdr shdr)
	{
	    this.o = o;
	    this.shdr = shdr;
	}

	String infoTypeToString (long info)
	{
	    return STT.toShortString (info & 0xf);
	}

	String infoBindToString (long info)
	{
	    return STB.toShortString (info >> 4);
	}
    
	String otherVisibilityToString (long other)
	{
	    return STV.toShortString (other & 0x3);
	}

	String shndxToString (int shndx)
	{
	    return SHN.toPrintString (shndx, Long.toString (shndx));
	}

	void buildSym (long position,
		       long name,
		       long value,
		       long size,
		       long info,
		       long other,
		       int shndx)
	{
	    o.print (6, y);
	    o.print (": ");
	    o.printx (8,'0',value);
	    o.print (6,size);
	    o.print (' ');
	    o.print (-7, infoTypeToString (info));
	    o.print (' ');
	    o.print (-6, infoBindToString (info));
	    o.print (' ');
	    o.print (otherVisibilityToString (other));
	    o.print ("  ");
	    o.print (3, shndxToString (shndx));
	    o.print (' ');
	    if (name > 0)
		o.print (shdr.symbolName (name));
	    o.println ();
	    y++;
	}
    }

    public void print (PrintWriter o)
	throws java.lang.Exception
    {
	Shdr[] shdrs = elf.getShdrs ();
	for (int s = 0; s < shdrs.length; s++) {
	    Shdr shdr = shdrs[s];
	    if (shdr.type == SHT.SYMTAB
		|| shdr.type == SHT.DYNSYM) {
		o.println ();
		o.print ("Symbol table '");
		o.print (shdr.name ());
		o.print ("' contains ");
		o.print (shdr.size / shdr.entsize);
		o.print (" entries:");
		o.println ();
		o.print ("   Num:    Value  Size Type    Bind   Vis      Ndx Name");
		o.println ();
		new SymSection (elf, shdr)
		    .construct (new PrintBuilder (o, shdr));
	    }
	}
    }
}
