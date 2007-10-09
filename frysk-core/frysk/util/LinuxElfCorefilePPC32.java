//This file is part of the program FRYSK.

//Copyright 2006, 2007, IBM Corp.
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

import java.math.BigInteger;

import frysk.proc.Proc;
import frysk.proc.Task;
import inua.eio.ByteBuffer;

import lib.dwfl.ElfNhdr;
import lib.dwfl.ElfNhdrType;
import lib.dwfl.ElfEMachine;
import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfPrAuxv;
import lib.dwfl.ElfPrpsinfo;
import lib.dwfl.ElfPrstatus;
import lib.dwfl.ElfPrFPRegSet;
import frysk.sys.proc.AuxvBuilder;
import frysk.sys.proc.CmdLineBuilder;
import frysk.sys.proc.Stat;
import frysk.sys.proc.Status;

/**
 * LinuxElfCorefilePPC32. Extends LinuxCorefile. Fill in
 * specific PPC32 information for corefiles.
 * 
 */
public class LinuxElfCorefilePPC32 extends LinuxElfCorefile {

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
    public LinuxElfCorefilePPC32(Proc process, Task blockedTasks[]) {
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
	prpsInfo.setPrPpid(processStat.ppid);
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
	cmdLine.construct(pid);

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
	prStatus.setPrPpid(processStat.ppid);
	prStatus.setPrPgrp(processStat.pgrp);
	prStatus.setPrSid(processStat.session);
	prStatus.setPrSigPending(processStat.signal);

	// Order for these registers is found in /usr/include/asm/user.h
	// This is not the same order that frysk iterators print out, nor
	// are the names are the same. Create a string[] map to bridge
	// gap between frysk and core file register order.

        // The number of general purpose regiser.
        int gprSize = 32;

        // The number of total common registers in PPC/PPC64 including nip, msr,
        // etc. Defined in the asm-ppc64/elf.h.
        int elfNGREG = 48;
        int blankRegisterIndex = elfNGREG;

        byte[] zeroVal = new byte[] { 0 };

        // XXX: if one register's offset is not defined in asm-ppc/ptrace.h or
        // asm-ppc64/ptrace.h,we did not dump it out and fill give the null Name.
        String ppcRegMap[] = { "nip", "msr", "orig_r3", "ctr", "lnk", "xer",
                              "ccr", "mq", "trap", "dar", "dsisr", "result" };
 

        // Set the general purpose registers.
	for (int index = 0; index < gprSize; index++)
	    prStatus.setPrGPReg
		(index,
		 task.getBigIntegerRegisterFIXME("gpr" + index));

	// Set the PPC32 specific registers
	for (int index = 0; index < ppcRegMap.length; index++)
	    prStatus.setPrGPReg
		(index + gprSize,
		 task.getBigIntegerRegisterFIXME(ppcRegMap[index]));

        blankRegisterIndex = gprSize + ppcRegMap.length;

        BigInteger bigInt = new BigInteger(zeroVal);

        // On ppc, some register indexes are not defined in
	// asm-<ISA>/ptrace.h.        
        for (int index = blankRegisterIndex; index < elfNGREG; index++)
          prStatus.setPrGPReg(index, bigInt);
        
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
	ElfPrFPRegSet fpRegSet = new ElfPrFPRegSet();

	// Write FP Register info over wholesae. Do not interpret.
	ByteBuffer registerMaps[] = task.getRegisterBanks();
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
	builder.construct(proc.getPid());
	nhdrEntry.setNhdrDesc(ElfNhdrType.NT_AUXV, prAuxv);
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
	return ElfEHeader.PHEADER_ELFCLASS32;
    }
}
