// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

package frysk.proc.dead;

import java.io.File;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfData;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPHeader;
import lib.dwfl.ElfPrpsinfo;
import frysk.proc.Proc;
import frysk.rsl.Log;

/**
 * Data needed to construct a core file; shared between the core Host,
 * Proc And task.
 */

public class LinuxCoreFactory {
    private static final Log fine = Log.fine(LinuxCoreFactory.class);

    static Elf core;
    static Elf exe;

    /**
     * Construct the core file; if the EXE is non-null use it for
     * meta-data; otherwise use the executable extracted from the
     * core.
     *
     * All File paths <b>must</b> be canonical.
     */
    public static Proc create(File coreFile, File exeFile,
			      boolean includeMetaData) {
	try {
	    // Open the core file; validate it.
	    core = new Elf(coreFile, ElfCommand.ELF_C_READ);
	    ElfEHeader eHeader = core.getEHeader();
	    if (eHeader.type != ElfEHeader.PHEADER_ET_CORE) {
		throw new RuntimeException("'" + coreFile
					   + "' is not a corefile.");
	    }
	
	    // Find the note section; there is only ever one note
	    // section and it must be present.
	    ElfData noteData = null;
	    for (int i = 0; i < eHeader.phnum; i++) {
		// Test if pheader is of types notes..
		ElfPHeader pHeader = core.getPHeader(i);
		if (pHeader.type == ElfPHeader.PTYPE_NOTE) {
		    // if so, copy, break an leave.
		    noteData = core.getRawData(pHeader.offset,
					       pHeader.filesz);
		    break;
		}
	    }
	    if (noteData == null)
		throw new RuntimeException("'" + coreFile
					   + "' is corrupt; no note section");
	    
	    // Extract the pr/ps information from the note.
	    ElfPrpsinfo prpsInfo = ElfPrpsinfo.decode(noteData);
	    String[] args = prpsInfo.getPrPsargs().split(" ");
	    fine.log("args", args);

	    if (exeFile == null) {
		// Only place to find full path + exe is in the args
		// list. Remove ./ if present.
		if (args.length > 0) {
		    if (args[0].startsWith("./"))
			exeFile = new File(args[0].substring(2));
		    else
			exeFile = new File(args[0]);
		} else {
		    exeFile = new File(prpsInfo.getPrFname());
		}
		fine.log("exe from core", exeFile);
	    } else {
		fine.log("exe for core", exeFile);
	    }
	    if (includeMetaData)
		exe = new Elf(exeFile, ElfCommand.ELF_C_READ);

	    return null;
	} finally { 
	    if (core != null)
		core.close();
	    if (exe != null)
		exe.close();
	}
    }
}
