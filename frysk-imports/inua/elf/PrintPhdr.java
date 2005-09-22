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
import inua.Mask;

public class PrintPhdr
{
    Elf elf;
    public PrintPhdr (Elf elf)
    {
	this.elf = elf;
    }

    static final Mask[] flagMasks = {
	new Mask (PF.R, 'R'),
	new Mask (PF.W, 'W'),
	new Mask (PF.X, 'E'),
    };
    static String flagsToString (Phdr phdr)
    {
	StringBuffer b = new StringBuffer ();
	long flags = phdr.flags;
	for (int i = 0; i < flagMasks.length; i++) {
	    if ((flags & flagMasks[i].mask) != 0) {
		flags &= ~flagMasks[i].mask;
		b.append (flagMasks[i].ch);
	    }
	    else
		b.append (' ');
	}
	if (flags != 0)
	    b.append (Long.toHexString (flags));
	return b.toString ();
    }

    public void print (PrintWriter o, boolean headers)
	throws java.lang.Exception
    {
	Ehdr ehdr = elf.getEhdr ();
	Phdr phdrs[] = elf.getPhdrs ();
	Shdr shdrs[] = elf.getShdrs ();
	if (!headers) {
	    o.println ();
	    o.print ("Elf file type is ");
	    o.print (PrintEhdr.typeToString(ehdr));
	    o.println ();
	    o.print ("Entry point 0x");
	    o.printx (ehdr.entry);
	    o.println ();
	    o.print ("There are ");
	    o.print (ehdr.phnum);
	    o.print (" program headers, starting at offset ");
	    o.print (ehdr.phoff);
	    o.println ();
	}
	o.println ();
	o.print ("Program Headers:");
	o.println ();
	o.print ("  Type           Offset   VirtAddr   PhysAddr   FileSiz MemSiz  Flg Align");
	o.println ();
	for (int i = 0; i < phdrs.length; i++) {
	    Phdr phdr = phdrs[i];
	    o.print ("  ");
	    o.print (-14,PT.toShortString (phdr.type));
	    o.print (" 0x");
	    o.printx (6, '0', phdr.offset);
	    o.print (" 0x");
	    o.printx (8, '0', phdr.vaddr);
	    o.print (" 0x");
	    o.printx (8, '0', phdr.paddr);
	    o.print (" 0x");
	    o.printx (5, '0', phdr.filesz);
	    o.print (" 0x");
	    o.printx (5, '0', phdr.memsz);
	    o.print (' ');
	    o.print (flagsToString(phdr));
	    o.print (" 0x");
	    o.printx (phdr.align);
	    o.println ();
	    if (phdr.type == PT.INTERP) {
		// cheat
		o.print ("      [Requesting program interpreter: ");
		o.print (elf.buffer.getString (phdr.offset, phdr.filesz));
		o.print ("]");
		o.println ();
	    }
	}
	o.println ();
	o.print (" Section to Segment mapping:");
	o.println ();
	o.print ("  Segment Sections...");
	o.println ();
	for (int i = 0; i < phdrs.length; i++) {
	    Phdr phdr = phdrs[i];
	    o.print ("   ");
	    o.printx (2, '0', i);
	    o.print ("     ");
	    for (int j = 0; j < shdrs.length; j++) {
		Shdr shdr = shdrs[j];
		if (shdr.name > 0 && shdr.size > 0
		    && shdr.addr >= phdr.vaddr
		    && shdr.addr < phdr.vaddr + phdr.memsz) {
		    o.print (shdr.name ());
		    o.print (' ');
		}
	    }
	    o.println ();
	}
    }
}

