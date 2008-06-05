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

import frysk.sys.ProcessIdentifierFactory;
import inua.eio.ArrayByteBuffer;
import java.util.Iterator;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfEMachine;
import lib.dwfl.ElfNhdr;
import lib.dwfl.ElfNhdrType;
import lib.dwfl.ElfPrFPRegSet;
import lib.dwfl.ElfPrXFPRegSet;
import lib.dwfl.ElfPrpsinfo;
import lib.dwfl.ElfPrstatus;
import frysk.isa.banks.BankRegister;
import frysk.isa.banks.LinuxIA32RegisterBanks;
import frysk.isa.registers.IA32Registers;
import frysk.isa.registers.Register;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.sys.proc.CmdLineBuilder;
import frysk.sys.proc.Stat;

/**
 * LinuxElfCorefilex86. Extends LinuxCorefile. Fill in
 * specific x86 information for corefiles.
 * 
 */
public class IA32LinuxElfCorefile extends LinuxElfCorefile {

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
    public IA32LinuxElfCorefile(Proc process, Task blockedTasks[]) {
	super(process, blockedTasks);
	this.process = process;
	this.blockedTasks = blockedTasks;
	this.size = 32;
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#writeNotePrpsinfo(lib.dwfl.ElfNhdr, frysk.proc.Proc)
     */
    protected void writeNotePrpsinfo(ElfNhdr nhdrEntry, Proc process) {

	int pid = process.getPid();

	ElfPrpsinfo prpsInfo = new ElfPrpsinfo(this.size);
	Stat processStat = new Stat().scan(ProcessIdentifierFactory.create(pid));

	// Set state and name.
	prpsInfo.setPrState(processStat.state);
	prpsInfo.setPrSname(processStat.state);


	// If state = Z then set zombie flag
	if (processStat.state == 'Z')
	    prpsInfo.setPrZomb((char)1);
	else
	    prpsInfo.setPrZomb((char)0);


	String midStr = String.valueOf(processStat.nice);
	prpsInfo.setPrNice(midStr.charAt(0));

	// Set rest of prpsinfo
	prpsInfo.setPrFlag(processStat.flags);
	prpsInfo.setPrUid(process.getUID());
	prpsInfo.setPrGid(process.getGID());

	prpsInfo.setPrPid(pid);
	prpsInfo.setPrPpid(processStat.ppid.intValue());
	prpsInfo.setPrPgrp(processStat.pgrp);

	prpsInfo.setPrSid(processStat.session);
	prpsInfo.setPrFname(processStat.comm);

	// Build command line
	class BuildCmdLine extends CmdLineBuilder {
	    String prettyArgs = "";

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

	Stat processStat = new Stat().scan(ProcessIdentifierFactory.create(task.getTid()));

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
	  IA32Registers.EBX,
	  IA32Registers.ECX,
	  IA32Registers.EDX,
	  IA32Registers.ESI,
	  IA32Registers.EDI,
	  IA32Registers.EBP,
	  IA32Registers.EAX,
	  IA32Registers.DS,
	  IA32Registers.ES,
	  IA32Registers.FS,
	  IA32Registers.GS,
	  IA32Registers.ORIG_EAX,				      
	  IA32Registers.EIP,
	  IA32Registers.CS,
	  IA32Registers.EFLAGS,
	  IA32Registers.ESP,
	  IA32Registers.SS
	};

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
	
    	final int bankSize = 108;
    	byte[] scratch = new byte[10];
    	byte[] byteOrderedRegister= new byte[bankSize];
    	ArrayByteBuffer byteOrderedBuffer = new ArrayByteBuffer(byteOrderedRegister);
    	
    	Iterator registerIterator =  LinuxIA32RegisterBanks.FPREGS.entryIterator();
    	while (registerIterator.hasNext()) {
	    BankRegister bankRegister = ((BankRegister)registerIterator.next());
	    Register register = bankRegister.getRegister();
	    task.access(register, 0, register.getType().getSize(), scratch, 0, false);
	    bankRegister.access(byteOrderedBuffer, 0, register.getType().getSize(), scratch, 0, true);
    	}
	
	byteOrderedBuffer.get(byteOrderedRegister);
	
	ElfPrFPRegSet fpRegSet = new ElfPrFPRegSet();
	fpRegSet.setFPRegisterBuffer(byteOrderedRegister);
	
	// Write it
	nhdrEntry.setNhdrDesc(ElfNhdrType.NT_FPREGSET, fpRegSet);
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#writeNotePRXFPRegset(lib.dwfl.ElfNhdr, frysk.proc.Task)
     */
    protected boolean writeNotePRXFPRegset(ElfNhdr nhdrEntry, Task task) {
	ElfPrXFPRegSet xfpRegSet = new ElfPrXFPRegSet();
	
	final int bankSize = 512;
	final int maxRegSize = 16;
	byte[] scratch = new byte[maxRegSize];
	byte[] byteOrderedRegister= new byte[bankSize];
	ArrayByteBuffer byteOrderedBuffer = new ArrayByteBuffer(byteOrderedRegister);
	
	Iterator registerIterator =  LinuxIA32RegisterBanks.XFPREGS.entryIterator();
	while (registerIterator.hasNext()) {
	    BankRegister bankRegister = ((BankRegister)registerIterator.next());
	    Register register = bankRegister.getRegister();
	    task.access(register, 0, register.getType().getSize(), scratch, 0, false);
	    bankRegister.access(byteOrderedBuffer, 0, register.getType().getSize(), scratch, 0, true);
	}
	
	byteOrderedBuffer.get(byteOrderedRegister);
	
	xfpRegSet.setXFPRegisterBuffer(byteOrderedRegister);
	
	// Write it
	nhdrEntry.setNhdrDesc(ElfNhdrType.NT_PRXFPREG, xfpRegSet);
	
	return true;
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#getElfMachineType()
     */
    protected byte getElfMachineType() {
	return ElfEMachine.EM_386;
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#getElfMachineClass()
     */
    protected byte getElfMachineClass() {
	return ElfEHeader.CLASS32;
    }

}
