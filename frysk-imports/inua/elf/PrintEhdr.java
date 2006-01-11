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

public class PrintEhdr
{
    Elf elf;
    public PrintEhdr (Elf elf)
    {
	this.elf = elf;
    }

    static String identToString (Ehdr ehdr)
    {
	StringBuffer bytes = new StringBuffer ();
	for (int i = 0; i < ehdr.ident.length; i++) {
	    String s = Integer.toHexString (ehdr.ident[i]);
	    if (s.length () > 1)
		bytes.append (s).append (" ");
	    else
		bytes.append ("0").append (s).append (" ");
	}
	return bytes.toString ();
    }

    static String identClassToString (Ehdr ehdr)
    {
	return ELF.CLASS.toPrintString (ehdr.ident[EI.CLASS]);
    }

    static String identDataToString (Ehdr ehdr)
    {
	return ELF.DATA.toPrintString (ehdr.ident[EI.DATA]);
    }

    static String identVersionToString (Ehdr ehdr)
    {
	return EV.toPrintString (ehdr.ident[EI.VERSION]);
    }

    static String identOsAbiToString (Ehdr ehdr)
    {
	return ELF.OSABI.toPrintString (ehdr.ident[EI.OSABI]);
    }

    static String typeToString (Ehdr ehdr)
    {
	return ET.toPrintString (ehdr.type);
    }

    static String machineToString (Ehdr ehdr)
    {
	return EM.toPrintString (ehdr.machine);
    }

    public void print (PrintWriter o)
	throws java.lang.Exception
    {
	Ehdr ehdr = elf.getEhdr ();
	o.print ("ELF Header:\n");
	o.print ("  Magic:   ");
	o.print (identToString (ehdr));
	o.println ();
	o.print ("  Class:                             ");
	o.print (identClassToString(ehdr));
	o.println ();
	o.print ("  Data:                              ");
	o.print (identDataToString(ehdr));
	o.println ();
	o.print ("  Version:                           ");
	o.print (identVersionToString (ehdr));
	o.println ();
	o.print ("  OS/ABI:                            ");
	o.print (identOsAbiToString (ehdr));
	o.println ();
	o.print ("  ABI Version:                       ");
	o.print (ehdr.ident[EI.ABIVERSION]);
	o.println ();
	o.print ("  Type:                              ");
	o.print (typeToString (ehdr));
	o.println ();
	o.print ("  Machine:                           ");
	o.print (machineToString (ehdr));
	o.println ();
	o.print ("  Version:                           ");
	o.print ("0x");
	o.printx (ehdr.version);
	o.println ();
	o.print ("  Entry point address:               ");
	o.print ("0x");
	o.printx (ehdr.entry);
	o.println ();
	o.print ("  Start of program headers:          ");
	o.print (ehdr.phoff);
	o.print (" (bytes into file)");
	o.println ();
	o.print ("  Start of section headers:          ");
	o.print (ehdr.shoff);
	o.print (" (bytes into file)");
	o.println ();
	o.print ("  Flags:                             ");
	o.print ("0x");
	o.printx (ehdr.flags);
	o.println ();
	o.print ("  Size of this header:               ");
	o.print (ehdr.ehsize);
	o.print (" (bytes)");
	o.println ();
	o.print ("  Size of program headers:           ");
	o.print (ehdr.phentsize);
	o.print (" (bytes)");
	o.println ();
	o.print ("  Number of program headers:         ");
	o.print (ehdr.phnum);
	o.println ();
	o.print ("  Size of section headers:           ");
	o.print (ehdr.shentsize);
	o.print (" (bytes)");
	o.println ();
	o.print ("  Number of section headers:         ");
	o.print (ehdr.shnum);
	o.println ();
	o.print ("  Section header string table index: ");
	o.print (ehdr.shstrndx);
	o.println ();
    }

}
