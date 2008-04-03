// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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

package frysk.dwfl;

import java.io.File;
import java.util.HashMap;

import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfData;
import lib.dwfl.ElfDynamic;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPHeader;
import lib.dwfl.ElfSection;
import lib.dwfl.ElfSectionHeader;
import lib.dwfl.ElfFileException;
import lib.dwfl.ElfException;

import frysk.rsl.Log;
import frysk.rsl.LogFactory;

/**
 * What ltrace needs to know about each loaded executable or shared
 * library.
 */
public class ObjectFile
{
    static private final Log warning = LogFactory.warning(ObjectFile.class);
    static private final Log fine = LogFactory.fine(ObjectFile.class);
    static private final Log finest = LogFactory.finest(ObjectFile.class);

    private File filename;
    private String soname = null;
    private String interp = null;
    private File resolvedInterp = null;
    private ElfSection dynamicStrtab = null;

    private static HashMap cachedFiles = new HashMap();

    private static void assertFitsToInt(long num, String context) {
	int numi = (int)num;
	if ((long)numi != num)
	    throw new ArithmeticException(context + ": " + num + " doesn't fit into int.");
    }

    protected ObjectFile(File file, final Elf elfFile, ElfEHeader eh) {
	this.filename = file;

	long offDynamic = -1;
	for (int i = 0; i < eh.phnum; ++i) {
	    ElfPHeader ph = elfFile.getPHeader(i);
	    if (ph.type == ElfPHeader.PTYPE_DYNAMIC) {
		offDynamic = ph.offset;
		finest.log("Found DYNAMIC segment.");
		if (this.interp != null) // interp already loaded?
		    break;
	    }
	    else if (ph.type == ElfPHeader.PTYPE_INTERP) {
		ElfData interpData = elfFile.getRawData(ph.offset, ph.filesz - 1); // -1 for trailing zero
		String interp = new String(interpData.getBytes());
		this.setInterp(interp);
		finest.log("Found INTERP `" + interp + "'.");
		if (offDynamic != -1) // already seen dynamic?
		    break;
	    }
	}

	if (eh.type != ElfEHeader.PHEADER_ET_EXEC
	    && eh.type != ElfEHeader.PHEADER_ET_DYN)
	    throw new ElfFileException(file, "Failed, unsupported ELF file type.");

	boolean foundDynamic = false;

	class Locals {
	    public int dynamicSonameIdx = -1;
	}
	final Locals locals = new Locals();

	// Find & interpret DYNAMIC section.
	if (offDynamic != -1) {
	    for (ElfSection section = elfFile.getSection(0);
		 section != null;
		 section = elfFile.getNextSection(section))
	    {
		ElfSectionHeader sheader = section.getSectionHeader();
		if (sheader.offset == offDynamic) {
		    finest.log("Processing DYNAMIC section.");
		    foundDynamic = true;
		    ElfDynamic.loadFrom(section, new ElfDynamic.Builder() {
			    public void entry (int tag, long value) {
				if (tag == ElfDynamic.ELF_DT_STRTAB) {
				    finest.log(" * dynamic strtab at 0x" + Long.toHexString(value));
				    ObjectFile.this.dynamicStrtab = getElfSectionWithAddr(elfFile, value);
				}
				else if (tag == ElfDynamic.ELF_DT_SONAME) {
				    finest.log(" * soname index = 0x" + Long.toHexString(value));
				    assertFitsToInt(value, "SONAME index");
				    locals.dynamicSonameIdx = (int)value;
				}
			    }
			});
		}
	    }

	    // Elf consistency sanity checks.
	    if (!foundDynamic)
		throw new ElfFileException(file, "DYNAMIC section not found in ELF file.");
	    if (this.dynamicStrtab == null)
		throw new ElfFileException(file, "Couldn't get STRTAB from DYNAMIC section.");
	}

	// Read SONAME, if there was one.
	if (locals.dynamicSonameIdx != -1) {
	    finest.log("Reading SONAME.");
	    ElfData data = this.dynamicStrtab.getData();
	    byte[] bytes = data.getBytes();
	    int startIndex = locals.dynamicSonameIdx;
	    int endIndex = startIndex;
	    while (bytes[endIndex] != 0)
		++endIndex;
	    String name = new String(bytes, startIndex, endIndex - startIndex);
	    this.setSoname(name);
	    finest.log("Found SONAME ", name);
	}

	fine.log("Loading finished successfully.");
    }

    protected void setSoname(String soname) {
	this.soname = soname;
    }

    /**
     * Either answer primed soname, or construct soname from filename.
     */
    public String getSoname() {
	if (this.soname != null)
	    return this.soname;
	else
	    return this.filename.getName();
    }

    protected void setInterp(String interp) {
	this.interp = interp;
    }

    /**
     * Answer INTERP or null if none was in file.
     */
    public String getInterp() {
	return this.interp;
    }

    /** Find a canonical path to interpreter and answer that. */
    public File resolveInterp()
    {
	if (this.resolvedInterp == null) {
	    File interppath = new File(this.interp);
	    try {
		this.resolvedInterp = interppath.getCanonicalFile();
	    }
	    catch (java.io.IOException e) {
		warning.log(
		    "Couldn't get canonical path of ELF interpreter",
		    interppath);
	    }
	}
	return this.resolvedInterp;
    }

    /**
     * Answer filename.
     */
    public File getFilename() {
	return this.filename;
    }

    private static ElfSection getElfSectionWithAddr(Elf elfFile, long addr)
    {
	for (ElfSection section = elfFile.getSection(0);
	     section != null;
	     section = elfFile.getNextSection(section)) {
	    ElfSectionHeader sheader = section.getSectionHeader();
	    if (sheader.addr == addr)
		return section;
	}
	return null;
    }

    public static ObjectFile buildFromFile(String path)
    {
	fine.log("Loading object file `" + path + "'");
	File filename;
	try {
	    filename = new File(path).getCanonicalFile();
	    // XXX sysroot!
	} catch (java.io.IOException ioexp) {
	    fine.log("Couldn't get canonical file.");
	    return null;
	}

	ObjectFile objFile = (ObjectFile)cachedFiles.get(filename);
	if (objFile != null) {
	    fine.log("Retrieved from cache.");
	    return objFile;
	}

	try {
	    Elf elfFile = new Elf(filename, ElfCommand.ELF_C_READ);
	    ElfEHeader eh = elfFile.getEHeader();
	    objFile = new ObjectFile(filename, elfFile, eh);
	} catch (ElfException eexp) {
	    return null;
	}

	cachedFiles.put(filename, objFile);
	fine.log("Done.");
	return objFile;
    }
}
