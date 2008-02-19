// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.isa;

import lib.dwfl.ElfEMachine;
import lib.dwfl.ElfEHeader;
import java.io.File;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import java.util.Map;
import java.util.HashMap;

/**
 * Factory returning an ISA based on ELF header information.
 */
public final class ElfMap {
    private static final Map isaToMachine;
    private static void add(int m, ISA isa) {
	Integer machine = new Integer(m);
	isaToMachine.put(isa, machine);
    }
    static {
	isaToMachine = new HashMap();
	add(ElfEMachine.EM_PPC, ISA.PPC32BE);
	add(ElfEMachine.EM_PPC64, ISA.PPC64BE);
	add(ElfEMachine.EM_386, ISA.IA32);
	add(ElfEMachine.EM_X86_64, ISA.X8664);
    }

    public static int getElfMachine(ISA isa) {
	Integer machine = (Integer)isaToMachine.get(isa);
	if (machine == null)
	    throw new RuntimeException("no ELF machine for " + isa);
	return machine.intValue();
    }

    public static ISA getISA(ElfEHeader header) {
	// XXX: Little endian PPC?
	switch (header.machine) {
	case ElfEMachine.EM_PPC:
	    return ISA.PPC32BE;
	case ElfEMachine.EM_PPC64:
	    return ISA.PPC64BE;
	case ElfEMachine.EM_386:
	    return ISA.IA32;
	case ElfEMachine.EM_X86_64:
	    return ISA.X8664;
	default:
	    throw new RuntimeException("unhandled elf machine "
				       + header.machine);
	}
    }

    public static ISA getISA(File exe) {
	Elf elfFile;
	elfFile = new Elf(exe, ElfCommand.ELF_C_READ);
	try {
	    ElfEHeader header = elfFile.getEHeader();
	    return getISA(header);
	} finally {
	    elfFile.close();
	}
    }
}
