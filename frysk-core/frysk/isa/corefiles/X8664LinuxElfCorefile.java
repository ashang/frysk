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

package frysk.isa.corefiles;

import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;
import inua.eio.ArrayByteBuffer;
import java.util.Iterator;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfEMachine;
import lib.dwfl.ElfNhdr;
import lib.dwfl.ElfNhdrType;
import lib.dwfl.ElfPrAuxv;
import lib.dwfl.ElfPrFPRegSet;
import lib.dwfl.ElfPrpsinfo;
import lib.dwfl.ElfPrstatus;
import frysk.isa.banks.BankRegister;
import frysk.isa.banks.LinuxX8664RegisterBanks;
import frysk.isa.registers.Register;
import frysk.isa.registers.X8664Registers;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.sys.proc.AuxvBuilder;
import frysk.sys.proc.CmdLineBuilder;
import frysk.sys.proc.Stat;
import frysk.sys.proc.Status;

/**
 * X8664LinuxElfCorefile. Extends LinuxCorefile. Fill in
 * specific x8664 information for corefiles.
 * 
 */
public class X8664LinuxElfCorefile extends LinuxElfCorefile {

    Proc process;

    Task[] blockedTasks;

    int size;
    /**
     * 
     * LinuxElfCoreFile. Construct a corefile from a given process, and that process's
     * tasks that have been blocked.
     * 	
     * @param process - The parent process to construct the core from.
     * @param blockedTasks - The process's tasks, in a stopped state
     */
    public X8664LinuxElfCorefile(Proc process, Task blockedTasks[]) {
	super(process, blockedTasks);
	this.process = process;
	this.blockedTasks = blockedTasks;
	this.size = 64;
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#writeNotePrpsinfo(lib.dwfl.ElfNhdr, frysk.proc.Proc)
     */
    protected void writeNotePrpsinfo(ElfNhdr nhdrEntry, Proc process) {

	int pid = process.getPid();

	ElfPrpsinfo prpsInfo = new ElfPrpsinfo(this.size);
	Stat processStat = new Stat();

	processStat.refresh(pid);

	// Set state and name.
	prpsInfo.setPrState(processStat.state);
	prpsInfo.setPrSname(processStat.state);

	String midStr = null;

	// Transform processStat.zero(int) into char.
	if ((processStat.zero >= 0) && (processStat.zero < 10)) {
	    midStr = String.valueOf(processStat.zero);

	    prpsInfo.setPrZomb(midStr.charAt(0));
	}

	if ((processStat.nice >= 0) && (processStat.nice < 10)) {
	    midStr = String.valueOf(processStat.nice);

	    prpsInfo.setPrNice(midStr.charAt(0));
	}

	// Set rest of prpsinfo
	prpsInfo.setPrFlag(processStat.flags);
	prpsInfo.setPrUid(Status.getUID(pid));
	prpsInfo.setPrGid(Status.getGID(pid));

	prpsInfo.setPrPid(pid);
	prpsInfo.setPrPpid(processStat.ppid.intValue());
	prpsInfo.setPrPgrp(processStat.pgrp);

	prpsInfo.setPrSid(processStat.session);
	prpsInfo.setPrFname(processStat.comm);

	// Build command line
	class BuildCmdLine extends CmdLineBuilder {
	    String prettyArgs = "";

	    public void buildBuffer(byte[] buf) {

	    }

	    public void buildArgv(String[] argv) {

		for (int i = 0; i < argv.length; i++)
		    prettyArgs += (argv[i] + " ");

		// Trim end space
		if (prettyArgs.length() > 0)
		    prettyArgs = prettyArgs.substring(0,
			    prettyArgs.length() - 1);
	    }
	}
	BuildCmdLine cmdLine = new BuildCmdLine();
	cmdLine.construct(ProcessIdentifierFactory.create(pid));

	// Set it
	prpsInfo.setPrPsargs(cmdLine.prettyArgs);

	// Write entry
	nhdrEntry.setNhdrDesc(ElfNhdrType.NT_PRPSINFO, prpsInfo);
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#writeNotePrstatus(lib.dwfl.ElfNhdr, frysk.proc.Task)
     */
    protected void writeNotePrstatus(ElfNhdr nhdrEntry, Task task) {

	ElfPrstatus prStatus = new ElfPrstatus(this.size);

	Stat processStat = new Stat();
	processStat.refresh(task.getTid());

	// Set initial prstatus data
	prStatus.setPrPid(task.getTid());
	prStatus.setPrPpid(processStat.ppid.intValue());
	prStatus.setPrPgrp(processStat.pgrp);
	prStatus.setPrSid(processStat.session);
	prStatus.setPrSigPending(processStat.signal);

	// Order for these registers is found in /usr/include/asm/user.h
	// This is not the same order that frysk iterators print out, nor
	// are the names are the same. Create a string[] map to bridge
	// gap between frysk and core file register order.        
	
	Register[] ptraceRegisterMap = {
	  X8664Registers.R15,
	  X8664Registers.R14,
	  X8664Registers.R13,
	  X8664Registers.R12,
	  X8664Registers.RBP,
	  X8664Registers.RBX,
	  X8664Registers.R11,
	  X8664Registers.R10,
	  X8664Registers.R9,
	  X8664Registers.R8,
	  X8664Registers.RAX,
	  X8664Registers.RCX,
	  X8664Registers.RDX,				      
	  X8664Registers.RSI,
	  X8664Registers.RDI,
	  X8664Registers.ORIG_RAX,
	  X8664Registers.RIP,
	  X8664Registers.CS,
	  X8664Registers.RFLAGS,
	  X8664Registers.RSP,
	  X8664Registers.SS,
	  X8664Registers.FS_BASE,
	  X8664Registers.GS_BASE,
	  X8664Registers.DS,
	  X8664Registers.ES,
	  X8664Registers.FS,
	  X8664Registers.GS};

	// Set GP register info
	int index = 0;
	int arraySize = 0;
	int regSize;
	int wordSize = task.getISA().wordSize();
	
	// Allocate space in array. Even though there are some registers < wordSize, they still have
	// to sit in a wordSize area
	for (int l = 0; l < ptraceRegisterMap.length; l++)
	  if (ptraceRegisterMap[l].getType().getSize() < wordSize)
	    arraySize +=wordSize;
	  else
	    arraySize +=ptraceRegisterMap[l].getType().getSize();
	
	byte[] byteOrderedRegister= new byte[arraySize];
	
	// Populate array, using wordSize as minimum size
	for (int i = 0; i < ptraceRegisterMap.length; i++) {
	  regSize = ptraceRegisterMap[i].getType().getSize();
	  if (regSize < wordSize)
	    regSize = wordSize;
	  task.access(ptraceRegisterMap[i], 0, regSize, byteOrderedRegister, index, false);
	  index += regSize;
	}
	
	prStatus.setPrGPRegisterBuffer(byteOrderedRegister);

	// Write it
	nhdrEntry.setNhdrDesc(ElfNhdrType.NT_PRSTATUS, prStatus);
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#writeNoteFPRegset(lib.dwfl.ElfNhdr, frysk.proc.Task)
     */
    protected void writeNoteFPRegset(ElfNhdr nhdrEntry, Task task) {
	
    	ElfPrFPRegSet xfpRegSet = new ElfPrFPRegSet();
	
    	final int bankSize = 512;
    	final int maxRegSize = 16;
    	byte[] scratch = new byte[maxRegSize];
    	byte[] byteOrderedRegister= new byte[bankSize];
    	ArrayByteBuffer byteOrderedBuffer = new ArrayByteBuffer(byteOrderedRegister);
    	
    	Iterator registerIterator =  LinuxX8664RegisterBanks.FPREGS.entryIterator();
    	while (registerIterator.hasNext()) {
	    BankRegister bankRegister = ((BankRegister)registerIterator.next());
	    Register register = bankRegister.getRegister();
	    task.access(register, 0, register.getType().getSize(), scratch, 0, false);
	    bankRegister.access(byteOrderedBuffer, 0, register.getType().getSize(), scratch, 0, true);
    	}
	
    	byteOrderedBuffer.get(byteOrderedRegister);
	
    	xfpRegSet.setFPRegisterBuffer(byteOrderedRegister);
	
    	// Write it
    	nhdrEntry.setNhdrDesc(ElfNhdrType.NT_FPREGSET, xfpRegSet);
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#writeNotePRXFPRegset(lib.dwfl.ElfNhdr, frysk.proc.Task)
     */
    protected  boolean writeNotePRXFPRegset(ElfNhdr nhdrEntry, Task task) {
        return false;
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#writeNoteAuxVec(lib.dwfl.ElfNhdr, frysk.proc.Proc)
     */
    protected void writeNoteAuxVec(ElfNhdr nhdrEntry, Proc proc) {
	final ElfPrAuxv prAuxv = new ElfPrAuxv();

	// Build Process Auxilliary
	AuxvBuilder builder = new AuxvBuilder() {

	    public void buildBuffer(byte[] auxv) {
		prAuxv.setAuxvBuffer(auxv);
	    }

	    public void buildDimensions(int wordSize, boolean bigEndian,
		    int length) {
	    }

	    public void buildAuxiliary(int index, int type, long val) {
	    }
	};
	ProcessIdentifier pid = ProcessIdentifierFactory.create(proc.getPid());
	builder.construct(pid);
	nhdrEntry.setNhdrDesc(ElfNhdrType.NT_AUXV, prAuxv);
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#getElfMachineType()
     */
    protected byte getElfMachineType() {
	return ElfEMachine.EM_X86_64;
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#getElfMachineClass()
     */
    protected byte getElfMachineClass() {
	return ElfEHeader.PHEADER_ELFCLASS64;
    }
    
}
