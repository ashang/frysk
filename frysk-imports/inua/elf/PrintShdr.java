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

import inua.util.PrintWriter;
import inua.util.Mask;

public class PrintShdr
{
    Elf elf;
    public PrintShdr (Elf elf)
    {
	this.elf = elf;
    }

    static final Mask[] flagMasks = {
	new Mask ( SHF.WRITE, 'W' ),
	new Mask ( SHF.ALLOC, 'A' ),
	new Mask ( SHF.EXECINSTR, 'X' ),
	new Mask ( SHF.MERGE, 'M' ),
	new Mask ( SHF.STRINGS, 'S' ),
	new Mask ( SHF.INFO_LINK, 'I' ),
	new Mask ( SHF.LINK_ORDER, 'L' ),
	new Mask ( SHF.OS_NONCONFORMING, 'O' ),
	new Mask ( SHF.GROUP, 'G' ),
	new Mask ( SHF.TLS, 'T' ),
    };
    static String flagsToString (Shdr shdr)
    {
	StringBuffer b = new StringBuffer ();
	long flags = shdr.flags;
	for (int i = 0; i < flagMasks.length; i++) {
	    if ((flags & flagMasks[i].mask) != 0) {
		flags &= ~flagMasks[i].mask;
		b.append (flagMasks[i].ch);
	    }
	}
	if (flags != 0)
	    b.append (Long.toHexString (flags));
	return b.toString ();
    }

    public void print (PrintWriter o, boolean headers)
	throws java.lang.Exception
    {
	Ehdr ehdr = elf.getEhdr ();
	Shdr[] shdrs = elf.getShdrs ();
	if (!headers) {
	    o.print ("There are ");
	    o.print (ehdr.shnum);
	    o.print (" section headers, starting at offset 0x");
	    o.printx (ehdr.shoff);
	    o.print (":\n");
	}
	o.println ();
	o.print ("Section Headers:\n");
	o.print ("  [Nr] Name              Type            Addr     Off    Size   ES Flg Lk Inf Al\n");
	for (int i = 0; i < shdrs.length; i++) {
	    o.print ("  [");
	    o.print (2, i);
	    o.print ("] ");
	    o.print (-18, shdrs[i].name ());
	    o.print (-15, SHT.toShortString (shdrs[i].type));
	    o.print (' ');
	    o.printx (8, '0', shdrs[i].addr);
	    o.print (' ');
	    o.printx (6, '0', shdrs[i].offset);
	    o.print (' ');
	    o.printx (6, '0', shdrs[i].size);
	    o.print (' ');
	    o.printx (2, '0', shdrs[i].entsize);
	    o.print (' ');
	    o.print (3, flagsToString (shdrs[i]));
	    o.print (' ');
	    o.print (2, shdrs[i].link);
	    o.print (' ');
	    o.print (3, shdrs[i].info);
	    o.print (' ');
	    o.print (2, shdrs[i].addralign);
	    o.println ();
	}
	o.print ("Key to Flags:\n");
	o.print ("  W (write), A (alloc), X (execute), M (merge), S (strings)\n");
	o.print ("  I (info), L (link order), G (group), x (unknown)\n");
	o.print ("  O (extra OS processing required) o (OS specific), p (processor specific)\n");
    }
}

