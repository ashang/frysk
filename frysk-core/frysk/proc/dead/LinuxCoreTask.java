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

import lib.dwfl.ElfPrstatus;
import lib.dwfl.ElfPrFPRegSet;
import lib.dwfl.ElfPrXFPRegSet;
import inua.eio.ByteBuffer;
import inua.eio.ArrayByteBuffer;
import inua.eio.ByteOrder;
import frysk.proc.TaskId;
import frysk.isa.ISA;
import frysk.isa.banks.RegisterBanks;
import frysk.isa.RegistersFactory;

public class LinuxCoreTask extends DeadTask {

    private final LinuxCoreProc parent;

    public ByteBuffer getMemory() {
	// XXX: Get the Proc's memory (memory maps). Task and register
	// information is handled differently (through notes) in core
	// files. There's a potential here for task to have its own
	// memory maps in some architectures, but not in the current
	// ISAs. In an attempt to save system resources, get a
	// reference to the proc's maps for now.
	return parent.getMemory();
    }

    static private RegisterBanks simulateRegisterBanks(ElfPrstatus elfTask,
						       ElfPrFPRegSet elfFPRegs,
						       ElfPrXFPRegSet elfXFPRegs,
						       ISA isa) {
	// XXX: Potentially this information should be constructed in
	// CorefileRegisterBanksFactory. However that would require
	// the factory to know about elf constructs which is not
	// desirable.
	ByteBuffer[] bankBuffers = new ByteBuffer[4];

	// Create an empty page
	byte[] emptyBuffer = new byte[4096];
	for (int i=0; i<emptyBuffer.length; i++)
	    emptyBuffer[i]=0;

	// Get ISA specific data
	ByteOrder byteOrder = isa.order();
	int  wordSize = isa.wordSize();
    
	// Set GP Registers
	bankBuffers[0] = new ArrayByteBuffer(elfTask.getRawCoreRegisters());
	bankBuffers[0].order(byteOrder);
	bankBuffers[0].wordSize(wordSize);

	// The following register banks are either fake (blank page) or
	// actual core data. As corefiles may or may not contain various
	// parts of register data, and there is an expectation throughout
	// Frysk that each task will always provide register data
	// regardless, we have to preset an empty page to avoid NPEs.

	// If Floating Point Register are present
	if (elfFPRegs != null)
	    bankBuffers[1] = new ArrayByteBuffer(elfFPRegs.getFPRegisterBuffer());
	else
	    bankBuffers[1] = new ArrayByteBuffer(emptyBuffer);

	bankBuffers[1].order(byteOrder);
	bankBuffers[1].wordSize(wordSize);

	// If X Floating Point Register are present
	if (elfXFPRegs != null)
	    bankBuffers[2] = new ArrayByteBuffer(elfXFPRegs.getXFPRegisterBuffer());
	else
	    bankBuffers[2] = new ArrayByteBuffer(emptyBuffer);

	bankBuffers[2].order(byteOrder);
	bankBuffers[2].wordSize(wordSize);

	// XXX: Other register banks need to be filled in.
	bankBuffers[3] = new ArrayByteBuffer(emptyBuffer);

	return CorefileRegisterBanksFactory.create(isa, bankBuffers);
    }

    /**
     * Create a new unattached Task.
     */
    LinuxCoreTask(LinuxCoreProc proc, ElfPrstatus elfTask, ElfPrFPRegSet
		  elfFPRegs, ElfPrXFPRegSet elfXFPRegs, ISA isa) {
	super(proc, new TaskId(elfTask.getPrPid()), isa,
	      simulateRegisterBanks(elfTask, elfFPRegs, elfXFPRegs, isa));
	this.parent = proc;
    }

    public long getPC() {
	return getRegister(RegistersFactory
			   .getRegisters(getISA())
			   .getProgramCounter());
    }
}
