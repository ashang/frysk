// This file is part of the program FRYSK.
// 
// Copyright 2006, 2007, 2008, IBM Corp.
// Copyright 2007, 2008, Red Hat Inc.
// 
// Contributed by
// Jose Flavio Aguilar Paulino (joseflavio@gmail.com)
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
import java.util.Iterator;
import inua.eio.ArrayByteBuffer;
import inua.eio.ByteBuffer;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfEMachine;
import lib.dwfl.ElfNhdr;
import lib.dwfl.ElfNhdrType;
import lib.dwfl.ElfPrFPRegSet;
import lib.dwfl.ElfPrpsinfo;
import lib.dwfl.ElfPrstatus;
import frysk.isa.registers.PPC32Registers;
import frysk.isa.registers.Register;
import frysk.isa.banks.BankRegister;
import frysk.isa.banks.LinuxPPCRegisterBanks;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.sys.proc.CmdLineBuilder;
import frysk.sys.proc.Stat;

/**
 * PPC32LinuxElfCorefile. Extends LinuxCorefile. Fill in
 * specific PPC32 information for corefiles.
 * 
 */
public class PPC32LinuxElfCorefile extends LinuxElfCorefile {

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
    public PPC32LinuxElfCorefile(Proc process, Task blockedTasks[]) {
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

        // The number of total common registers in PPC/PPC64 including nip, msr,
        // etc. Defined in the asm-ppc/elf.h.
        Register[] ptracePpcRegMap = { 
	  PPC32Registers.NIP, PPC32Registers.MSR, PPC32Registers.ORIGR3,
	  PPC32Registers.CTR, PPC32Registers.LR, PPC32Registers.XER,
	  PPC32Registers.CCR, PPC32Registers.MQ, PPC32Registers.TRAP,
	  PPC32Registers.DAR, PPC32Registers.DSISR, PPC32Registers.RESULT,
	  PPC32Registers.GPR0, PPC32Registers.GPR1, PPC32Registers.GPR2,
	  PPC32Registers.GPR3, PPC32Registers.GPR4, PPC32Registers.GPR5,
	  PPC32Registers.GPR6, PPC32Registers.GPR7, PPC32Registers.GPR8,
	  PPC32Registers.GPR9, PPC32Registers.GPR10, PPC32Registers.GPR11,
	  PPC32Registers.GPR12, PPC32Registers.GPR13, PPC32Registers.GPR14,
	  PPC32Registers.GPR15, PPC32Registers.GPR16, PPC32Registers.GPR17,
	  PPC32Registers.GPR18, PPC32Registers.GPR19, PPC32Registers.GPR20,
	  PPC32Registers.GPR21, PPC32Registers.GPR22, PPC32Registers.GPR23,
	  PPC32Registers.GPR24, PPC32Registers.GPR25, PPC32Registers.GPR26,
	  PPC32Registers.GPR27, PPC32Registers.GPR28, PPC32Registers.GPR29,
	  PPC32Registers.GPR30, PPC32Registers.GPR31};
	
        
    	// Set GP register info
    	int index = 0;
    	int arraySize = 0;
    	int regSize;
    	int wordSize = task.getISA().wordSize();
        int elfNGREG = 48;
	
    	// Allocate space in array. Even though there are some registers < wordSize, they still have
    	// to sit in a wordSize area
    	for (int l = 0; l < ptracePpcRegMap.length; l++)
	  if (ptracePpcRegMap[l].getType().getSize() < wordSize)
	    arraySize +=wordSize;
	  else
	    arraySize +=ptracePpcRegMap[l].getType().getSize();
	
    	int blankRegisterIndex = (elfNGREG - ptracePpcRegMap.length) * wordSize;
	
    	byte[] byteOrderedRegister= new byte[arraySize+blankRegisterIndex];
	
    	// Populate array, using wordSize as minimum size
    	for (int i = 0; i < ptracePpcRegMap.length; i++) {
	  regSize = ptracePpcRegMap[i].getType().getSize();
	  if (regSize < wordSize)
	    regSize = wordSize;
	  task.access(ptracePpcRegMap[i], 0, regSize, byteOrderedRegister, index, false);
	  index += regSize;
    	}
	
    	prStatus.setPrGPRegisterBuffer(byteOrderedRegister);
	
	// Write it
	nhdrEntry.setNhdrDesc(ElfNhdrType.NT_PRSTATUS, prStatus);
    }

    /*
         * (non-Javadoc)
         * 
         * @see frysk.util.LinuxElfCorefile#writeNoteFPRegset(lib.dwfl.ElfNhdr,
         *      frysk.proc.Task)
         */
    protected void writeNoteFPRegset(ElfNhdr nhdrEntry, Task task) {
	
   	ElfPrFPRegSet xfpRegSet = new ElfPrFPRegSet();
	
    	final int bankSize = 260;
    	final int maxRegSize = 8;
    	byte[] scratch = new byte[maxRegSize];
    	byte[] byteOrderedRegister= new byte[bankSize];
    	ArrayByteBuffer byteOrderedBuffer = new ArrayByteBuffer(byteOrderedRegister);
    	
    	Iterator registerIterator =  LinuxPPCRegisterBanks.FPREGS32.entryIterator();
    	while (registerIterator.hasNext()) {
	    BankRegister bankRegister = ((BankRegister)registerIterator.next());
	    Register register = bankRegister.getRegister();
	    task.access(register, 0, register.getType().getSize(), scratch, 0, false);
	    bankRegister.access(byteOrderedBuffer, 0, maxRegSize, scratch, 0, true);
    	}
	
    	byteOrderedBuffer.get(byteOrderedRegister);
	
    	xfpRegSet.setFPRegisterBuffer(byteOrderedRegister);
	
    	// Write it
    	nhdrEntry.setNhdrDesc(ElfNhdrType.NT_FPREGSET, xfpRegSet);

	ElfPrFPRegSet fpRegSet = new ElfPrFPRegSet();

	// Write FP Register info over wholesae. Do not interpret.
	ByteBuffer registerMaps[] = task.getRegisterBuffersFIXME();
	byte[] regBuffer = new byte[(int) registerMaps[1].capacity()];
	registerMaps[1].get(regBuffer);

	fpRegSet.setFPRegisterBuffer(regBuffer);

	// Write it
	nhdrEntry.setNhdrDesc(ElfNhdrType.NT_FPREGSET, fpRegSet);
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#writeNotePRXFPRegset(lib.dwfl.ElfNhdr, frysk.proc.Task)
     */
    protected boolean writeNotePRXFPRegset(ElfNhdr nhdrEntry, Task task) {
	return false;
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#getElfMachineType()
     */
    protected byte getElfMachineType() {
	return ElfEMachine.EM_PPC;
    }

    /* (non-Javadoc)
     * @see frysk.util.LinuxElfCorefile#getElfMachineClass()
     */
    protected byte getElfMachineClass() {
	return ElfEHeader.CLASS32;
    }
}
