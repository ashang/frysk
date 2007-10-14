//This file is part of the program FRYSK.

//Copyright 2007, Red Hat Inc.

//FRYSK is free software; you can redistribute it and/or modify it
//under the terms of the GNU General Public License as published by
//the Free Software Foundation; version 2 of the License.

//FRYSK is distributed in the hope that it will be useful, but
//WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//General Public License for more details.

//You should have received a copy of the GNU General Public License
//along with FRYSK; if not, write to the Free Software Foundation,
//Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.

//In addition, as a special exception, Red Hat, Inc. gives You the
//additional right to link the code of FRYSK with code not covered
//under the GNU General Public License ("Non-GPL Code") and to
//distribute linked combinations including the two, subject to the
//limitations in this paragraph. Non-GPL Code permitted under this
//exception must only link to the code of FRYSK through those well
//defined interfaces identified in the file named EXCEPTION found in
//the source code files (the "Approved Interfaces"). The files of
//Non-GPL Code may instantiate templates or use macros or inline
//functions from the Approved Interfaces without causing the
//resulting work to be covered by the GNU General Public
//License. Only Red Hat, Inc. may make changes or additions to the
//list of Approved Interfaces. You must obey the GNU General Public
//License in all respects for all of the FRYSK code and other code
//used in conjunction with FRYSK except the Non-GPL Code covered by
//this exception. If you modify this file, you may extend this
//exception to your version of the file, but you are not obligated to
//do so. If you do not wish to provide this exception without
//modification, you must delete this exception statement from your
//version and license this file solely under the GPL without
//exception.

package frysk.util;

import inua.eio.ByteOrder;
import frysk.isa.ISA;
import java.util.ArrayList;
import java.util.List;
import lib.dwfl.Dwfl;
import lib.dwfl.DwflModule;
import lib.dwfl.Elf;
import lib.dwfl.ElfCommand;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfException;
import lib.dwfl.ElfFileException;
import lib.dwfl.ElfNhdr;
import lib.dwfl.ElfPHeader;
import frysk.dwfl.DwflCache;
import frysk.proc.MemoryMap;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.sys.StatelessFile;
import frysk.sys.proc.MapsBuilder;

public abstract class LinuxElfCorefile {

    long elfSectionOffset = 0;

    String coreName = "core";

    Proc process = null;

    Task[] blockedTasks;

    boolean writeAllMaps = false;

    Elf linuxElfCorefileImage = null;

    /**
     * 
     * LinuxElfCoreFile. Construct a corefile from a given process, and that process's
     * tasks that have been block.
     * 
     * @param process - The parent process to construct the core from.
     * @param blockedTasks - The process's tasks, in a stopped state
     * 
     */
    public LinuxElfCorefile(Proc process, Task[] blockedTasks) {
	this.blockedTasks = blockedTasks;
	this.process = process;
    }

    /**
     * 
     * Defines whether to attempt to write all maps, and elide nothing.
     * 
     * @param maps - True if attempt to write all maps, false to follow
     * map writing convention.
     * 
     */
    public void setWriteAllMaps(boolean maps) {
	this.writeAllMaps = maps;
    }

    /**
     * 
     * Set the name of the corefile to be constructed. This should be
     * called before constructCorefile
     * 
     * @param name - Name of corefile.
     * 
     */
    public void setName(String name) {
	this.coreName = name;
    }

    /**
     * 
     * Constuct a core file from a process given in the constructor.
     * 
     * This is the "action" command after all parameters have
     * been set.
     * 
     */
    public void constructCorefile() {

	int mapsCount = 0;

	try {
	    linuxElfCorefileImage = openElf(getConstructedFileName(),
		    ElfCommand.ELF_C_WRITE);
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}

	// Build elf header.
	elfSectionOffset = populateElfHeader(linuxElfCorefileImage);

	// update in memory, so pheader size, no, and no of entries is valid
	linuxElfCorefileImage.update(ElfCommand.ELF_C_NULL);

	mapsCount = countMaps();
	
	// Build initial Program segment header including PT_NOTE program header.
	linuxElfCorefileImage.createNewPHeader(mapsCount + 1);
	ElfEHeader eHeader = linuxElfCorefileImage.getEHeader();

	// Build notes section
	byte[] noteData = buildNotes();
	
	// Write note header
	ElfPHeader noteSegment = linuxElfCorefileImage.getPHeader(0);
	noteSegment.offset = (eHeader.phentsize * eHeader.phnum) + eHeader.ehsize;
	noteSegment.memsz = 0;
	noteSegment.filesz = noteData.length; 
	//noteSegment.offset = 
	noteSegment.type = ElfPHeader.PTYPE_NOTE;
	noteSegment.vaddr = 0;
	noteSegment.flags = ElfPHeader.PHFLAG_NONE;
	noteSegment.align = 1;
	noteSegment.flags = ElfPHeader.PHFLAG_READABLE;
	linuxElfCorefileImage.updatePHeader(0, noteSegment);
	
	// Build, and write out memory segments to sections
	final CoreMapsBuilder builder = new CoreMapsBuilder();
	builder.construct(this.process.getMainTask().getTid());

	// Write elf file
	final long i = linuxElfCorefileImage.update(ElfCommand.ELF_C_WRITE);
	if (i < 0)
	    throw new RuntimeException("LibElf elf_update failed with "
		    + linuxElfCorefileImage.getLastErrorMsg());
	// Go home.
	linuxElfCorefileImage.close();
	
	writeNoteData(noteData, getConstructedFileName(), noteSegment.offset);
	writeSegments(mapsCount, getConstructedFileName());
    }

    private void writeSegments(int mapCount, String name)  {
	
	// chunk size to read
	int chunk_size = 0x1000;
	long numOfReads = 0;
	int currentOffset = 0;
	
	byte[] buffer = new byte[chunk_size];
	
	// Reopen corefile, to write segment data
	StatelessFile rawCore = new StatelessFile(name);

	try {
	    linuxElfCorefileImage = openElf(getConstructedFileName(),
		    ElfCommand.ELF_C_READ);
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}
	
	// Loop through maps
	for (int i=1; i<=mapCount;i++)
	{
	    final ElfPHeader pheader = linuxElfCorefileImage.getPHeader(i);

	    if (pheader.filesz <= 0)
		continue;

	    numOfReads = pheader.filesz / chunk_size;
	    currentOffset = 0;
	    
	    for (int chunkRead = 0; chunkRead < numOfReads; chunkRead++)
	    {
		process.getMainTask().getMemory().get(pheader.vaddr+currentOffset, buffer,0,chunk_size);
		rawCore.pwrite(pheader.offset+currentOffset, buffer,0,buffer.length);
		currentOffset += chunk_size;
	    }
	}
	
	linuxElfCorefileImage.close();
    }
    
    private void writeNoteData(byte[] data, String name, long offset) {
	StatelessFile rawCore = new StatelessFile(name);
	rawCore.pwrite(offset, data, 0, data.length);
    }
    /**
     * Return the endian type as associated by this ISA
     * 
     * @return byte - endian type.
     */
    protected byte getElfEndianType() {
	ISA currentArch = process.getMainTask().getISA();
	ByteOrder order = currentArch.order();
	if (order == ByteOrder.BIG_ENDIAN)
	    return ElfEHeader.PHEADER_ELFDATA2MSB;
	else
	    return ElfEHeader.PHEADER_ELFDATA2LSB;
    }

    /**
     * 
     * Return the word size as represented by this ISA
     * 
     * @return int - size of word
     * 
     */
    protected int getElfWordSize() {
	ISA currentArch = process.getMainTask().getISA();
	return currentArch.wordSize();
    }

    /**
     * writeNotePrpsInfo
     * 
     * Write a NT_PRPSINFO entry to the note entry given, from seed information
     * contained within the process.
     * 
     * @param nhdrEntry - the note header entry this function is to populate.
     * @param process - the frysk.proc.live.Proc that provides seed info to
     * populate the note header.
     */
    protected abstract void writeNotePrpsinfo(ElfNhdr nhdrEntry, Proc process);

    /**
     * writeNotePrstatus
     * 
     * Write a NT_PRSTATUS entry to  the note entry given, from seed information
     * contained within the task.
     * 
     * @param nhdrEntry - the note header entry this function is to populate.
     * @param task - the frysk.proc.live.Taskthat provides seed info to
     * populate the note header.
     */    
    protected abstract void writeNotePrstatus(ElfNhdr nhdrEntry, Task task);

    /**
     * writeNoteFPRegSet
     * 
     * Write a NT_PRFPREGSET entry to  the note entry given, from seed information
     * contained within the task.
     * 
     * @param nhdrEntry - the note header entry this function is to populate.
     * @param task - the frysk.proc.live.Taskthat provides seed info to
     * populate the note header.
     */        
    protected abstract void writeNoteFPRegset(ElfNhdr nhdrEntry, Task task);

    /**
     * writeNotePRXFPRegSet
     * 
     * Write a NT_PRFPXREG entry to  the note entry given, from seed information
     * contained within the task.
     * 
     * @param nhdrEntry - the note header entry this function is to populate.
     * @param task - the frysk.proc.live.Taskthat provides seed info to
     * populate the note header.
     * @return boolean: Reutrns whether this arch supports this note.
     */   
    protected abstract boolean writeNotePRXFPRegset(ElfNhdr nhdrEntry, Task task);
    
    /**
     * writeNoteAuxVec
     * 
     * Write a NT_AUXV entry to  the note entry given, from seed information
     * contained within the task.
     * 
     * @param nhdrEntry - the note header entry this function is to populate.
     * @param task - the frysk.proc.live.Taskthat provides seed info to
     * populate the note header.
     */ 
    protected abstract void writeNoteAuxVec(ElfNhdr nhdrEntry, Proc process);

    /**
     * 
     * Return the elf machine type as defined by this ISA
     * @return byte - machine type
     * 
     */
    protected abstract byte getElfMachineType();

    /**
     * 
     * Return the elf class type as defined by this ISA
     * @return - class type
     * 
     */
    protected abstract byte getElfMachineClass();

    /**
     * Transform all information carried by list into ElfData object.
     * 
     * @param noteSectionData - ElfData object to place constructed data
     * @param list - List of note Headers
     * @return the number of valid ElfNhdr objects.
     */
    protected byte[] constructSectionData(List nhdrList) {
	int sectionSize = 0;
	long entrySize = 0;
	long startAddress = 0;

	int size = nhdrList.size();
	if (size <= 0)
	    return null;

	// Count the size of the whole PT_NOTE section.
	for (int index = 0; index < size; index++) {
	    ElfNhdr entry = (ElfNhdr) nhdrList.get(index);

	    entrySize = entry.getNhdrEntrySize();
	    if (entrySize <= 0) {
		// One invalid entry, ignore it.
		nhdrList.remove(index);
		size--;
		index--;
		continue;
	    }

	    sectionSize += entrySize;
	}

	byte[] noteSectionBuffer = new byte[sectionSize];

	// Begin to fill the noteSection memory region.
	size = nhdrList.size();
	for (int index = 0; index < size; index++) {
	    ElfNhdr entry = (ElfNhdr) nhdrList.get(index);

	    entry.fillMemRegion(noteSectionBuffer, startAddress);

	    startAddress += entry.getNhdrEntrySize();
	}

	return noteSectionBuffer;
    }

    /**
     * 
     * Fill the note section with the constructed note pieces.
     * 
     * @param noteSection - note section to fill.
     * 
     */
    protected byte[] buildNotes() {
	int entryCount = 0;

	ArrayList list = new ArrayList();

	// Fill PRPSINFO correctly.
	ElfNhdr prpsinfoNhdr = new ElfNhdr();
	writeNotePrpsinfo(prpsinfoNhdr, this.process);
	list.add(entryCount, prpsinfoNhdr);
	entryCount++;

	// Loop tasks for PRSTATUS and FPREGISTERS
	for (int i = 0; i < blockedTasks.length; i++) {

	    // PRSTATUS
	    ElfNhdr prStatusNhdr = new ElfNhdr();
	    writeNotePrstatus(prStatusNhdr, blockedTasks[i]);
	    list.add(entryCount, prStatusNhdr);
	    entryCount++;

	    // FP REGISTERS
	    ElfNhdr prFPRegSet = new ElfNhdr();
	    writeNoteFPRegset(prFPRegSet, blockedTasks[i]);
	    list.add(entryCount, prFPRegSet);
	    entryCount++;

	    // XFP REGISTERS
	    ElfNhdr prXFPRegSet = new ElfNhdr();
	    if (writeNotePRXFPRegset(prXFPRegSet, blockedTasks[i]))
	    {
		list.add(entryCount, prXFPRegSet);
		entryCount++;
	    }
	}

	// Process Auxillary (AuxV)
	ElfNhdr prAuxVNhdr = new ElfNhdr();
	writeNoteAuxVec(prAuxVNhdr, this.process);
	list.add(entryCount, prAuxVNhdr);
	entryCount++;

	if (list.size() <= 0)
	    throw new RuntimeException("Core file .note contents empty");

	// Now all note sections are filled, write it out to
	// to section data.
	return constructSectionData(list);

    }

    /**
     * Fill the ELF header information for the ELF core file.
     * 
     * @param elfCore - The Linux Core file Elf Image
     *
     * @return int size of the elf header
     */
    protected int populateElfHeader(Elf elfCore) {

	elfCore.createNewEHeader(getElfWordSize());
	ElfEHeader elf_header = elfCore.getEHeader();

	elf_header.ident[4] = getElfMachineClass();
	elf_header.ident[5] = getElfEndianType();

	// Version
	elf_header.ident[6] = (byte) elfCore.getElfVersion();

	// EXEC for now, ET_CORE later
	elf_header.type = ElfEHeader.PHEADER_ET_CORE;

	// Version
	elf_header.version = elfCore.getElfVersion();

	// String Index
	elf_header.shstrndx = 0;

	elf_header.machine = getElfMachineType();

	elfCore.updateEHeader(elf_header);
	//Write elf file. Calculate offsets in elf memory.
	final long i = elfCore.update(ElfCommand.ELF_C_NULL);
	if (i < 0)
	    throw new RuntimeException("LibElf elf_update failed with "
		    + elfCore.getLastErrorMsg());

	// Get size of written header
	elf_header = elfCore.getEHeader();
	return elf_header.ehsize;
    }

    /**
     * 
     * Core map file builder. Parse each map, build the section header. Once
     * section header is completed, copy the data from the mapped task memory
     * according to the parameter from the builder: addressLow -> addressHigh and
     * place in an Elf_Data section.
     * 
     */
    class CoreMapsBuilder extends MapsBuilder {

	int numOfMaps = 0;

	byte[] mapsLocal;
	
	Dwfl dwfl = null;
	Elf elf;

	CoreMapsBuilder()
	{
	    dwfl = DwflCache.getDwfl(process.getMainTask());
	}
		
	public void buildBuffer(final byte[] maps) {
	    // Safe a refernce to the raw maps.
	    mapsLocal = maps;
	    maps[maps.length - 1] = 0;
	}

	public void buildMap(final long addressLow, final long addressHigh,
		final boolean permRead, final boolean permWrite,
		final boolean permExecute, final boolean shared,
		final long offset, final int devMajor, final int devMinor,
		final int inode, final int pathnameOffset,
		final int pathnameLength) {

	    if (permRead == true) {

		// Calculate segment name
		byte[] filename = new byte[pathnameLength];
		boolean writeMap = false;

		System.arraycopy(mapsLocal, pathnameOffset, filename, 0,
			pathnameLength);
		String sfilename = new String(filename);

		if (writeAllMaps) {
		    writeMap = true;
		} else {
		    // Should the map be written?
		    if (inode == 0)
			writeMap = true;
		    if ((inode > 0) && (permWrite))
			writeMap = true;
		    if (sfilename.equals("[vdso]"))
			writeMap = true;
		    if (sfilename.equals("[stack]"))
			writeMap = true;
		    if (permRead && !shared && !permWrite && !permExecute)
			writeMap = true;
		    if (shared)
			writeMap = true;
		}

		if (!writeMap) {
		    DwflModule module = null;
		    if (dwfl != null) {
			module = dwfl.getModule(addressLow);
			if (module != null)
			    if (module.getElf() == null)
				writeMap = true;
		    }	
		}
		// Get empty progam segment header corresponding to this entry.
		// PT_NOTE's program header entry takes the index: 0. So we should
		// begin from 1.
		final ElfPHeader pheader = linuxElfCorefileImage
			.getPHeader(numOfMaps + 1);

		// Get the section written before this one.
		final ElfPHeader previous = linuxElfCorefileImage
			.getPHeader(numOfMaps);

		// Calculate offset in elf file, from offset of previous section.
		if (previous.memsz > 0)
		    pheader.offset = previous.offset + previous.memsz;
		else
		    pheader.offset = previous.offset + previous.filesz;

		pheader.type = ElfPHeader.PTYPE_LOAD;
		pheader.vaddr = addressLow;
		pheader.memsz = addressHigh - addressLow;
		pheader.flags = ElfPHeader.PHFLAG_NONE;

		// If we are to write this map, annotate file size within
		// elf file.
		if (writeMap)
		    pheader.filesz = pheader.memsz;
		else
		    pheader.filesz = 0;

		// Build flags
		if (permRead == true)
		    pheader.flags = pheader.flags | ElfPHeader.PHFLAG_READABLE;

		if (permWrite == true)
		    pheader.flags = pheader.flags | ElfPHeader.PHFLAG_WRITABLE;

		if (permExecute == true) 
		    pheader.flags = pheader.flags
			    | ElfPHeader.PHFLAG_EXECUTABLE;


		// Only actually write the segment if we have previously
		// decided to write that segment.
		if (writeMap) 
		    pheader.filesz = pheader.memsz;

		pheader.align = 0x1;

		// Write back Segment header to elf structure
		linuxElfCorefileImage.updatePHeader(numOfMaps + 1, pheader);

		numOfMaps++;
	    }

	}
    }

    /**
     * Count the number of readable maps in a process
     * 
     * @return number of readable maps
     */
    private int countMaps() {
	MemoryMap[] maps = this.process.getMaps();
	int count = 0;

	if (maps.length == 0)
	    return 0;

	for (int i = 0; i < maps.length; i++)
	    if (maps[i].permRead == true)
		count++;

	return count;
    }

    /**
     * 
     * Open an elf file
     * @param coreName - Name of elf file
     * @param command - Command to open the file
     * @return - Valud elf image
     * 
     * @throws ElfFileException
     * @throws ElfException
     * 
     */
    private Elf openElf(String coreName, ElfCommand command)
	    throws ElfFileException, ElfException {
	// Start new elf file
	return new Elf(coreName, command);
    }

    /**
     * 
     * Return the constructed name (ie name + "." + pid of the 
     * core file.
     * 
     * @return - String. Name of Corefile.
     * 
     */
    public String getConstructedFileName() {
	return this.coreName + "." + this.process.getPid();
    }

}
