//This file is part of the program FRYSK.

//Copyright 2006, Red Hat Inc.

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

import inua.eio.ByteBuffer;
import inua.eio.ByteOrder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.elf.ElfData;
import lib.elf.ElfEHeader;
import lib.elf.ElfEMachine;
import lib.elf.ElfException;
import lib.elf.ElfFileException;
import lib.elf.ElfFlags;
import lib.elf.ElfNhdr;
import lib.elf.ElfNhdrType;
import lib.elf.ElfPHeader;
import lib.elf.ElfPrAuxv;
import lib.elf.ElfPrFPRegSet;
import lib.elf.ElfPrpsinfo;
import lib.elf.ElfPrstatus;
import lib.elf.ElfSection;
import lib.elf.ElfSectionHeader;
import lib.elf.ElfSectionHeaderTypes;
import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.event.SignalEvent;
import frysk.proc.Isa;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcBlockAction;
import frysk.proc.Task;
import frysk.sys.Sig;
import frysk.sys.proc.AuxvBuilder;
import frysk.sys.proc.CmdLineBuilder;
import frysk.sys.proc.MapsBuilder;
import frysk.sys.proc.Stat;
import frysk.sys.proc.Status;

/**
 * FCore - Utility class to take a pid. Derive from that a Proc that is modelled
 * in the core, stop all the tasks, get the main tasks's maps, and each threads
 * register/status. Construct an elf core-file. Caveats associated at this time:
 * fcore ELF core file will be: ELF Header Program Segment Header Section Header
 * .notes segment Segments * n Segment String Table The NOTES section of the
 * core file, will contain several structures as defined as follows: Structure:
 * elf_prpsinfo (one per process) elf_prstatus (one per thread, main task first)
 * elf_fpregset_t (one per thread, regardless of whether fp used)
 */

public class CoredumpAction
    extends ProcBlockAction
{

  protected static final Logger logger = Logger.getLogger("frysk");

  private Elf local_elf = null;

  private String filename = "core";

  private long elfSectionOffset = 0;

  private boolean writeAllMaps = false;

  int taskArraySize = 1;

  Task[] taskArray;

  private LinkedList taskList;

  // Proc proc = null;

  private Event event;

  public CoredumpAction (Proc proc, Event theEvent, boolean writeAllMaps)
  {
    super(proc);
    // this.proc = proc;
    this.event = theEvent;
    taskList = proc.getTasks();
    taskArray = new Task[taskList.size()];
    this.writeAllMaps = writeAllMaps;
    Manager.eventLoop.add(new InterruptEvent(proc));
  }

  public CoredumpAction (Proc proc, String filename, Event theEvent,
                         boolean writeAllMaps)
  {
    this(proc, theEvent, writeAllMaps);
    this.filename = filename;

  }

  public void existingTask (Task task)
  {

    // Add task to our list. Special case for
    // main task.
    if (proc.getMainTask() == task)
      taskArray[0] = task;
    else
      {
        taskArray[taskArraySize] = task;
        taskArraySize++;
      }

    // Remove this task from the list of tasks that
    // the Proc object told us about.
    if (taskList.contains(task))
      taskList.remove(task);
  }

  public void addFailed (final Object observable, final Throwable w)
  {
    abandonCoreDump((Exception) w);
  }

  public void deletedFrom (final Object observable)
  {
  }

  /**
   * Abandon the core dump. Print out error message, then as quickly as possible
   * abandon the Proc object and exit.
   * 
   * @param e - Exception that caused the abandon
   */
  private void abandonCoreDump (Exception e)
  {
    System.err.println("ERROR: Core file abandoned. Exception message is: "
                       + e.getMessage());
    e.printStackTrace();
    proc.requestAbandon();
    proc.observableDetached.addObserver(new Observer()
    {

      public void update (Observable o, Object arg)
      {
        Manager.eventLoop.add(new RequestStopEvent(Manager.eventLoop));
      }
    });
    System.exit(- 1);
  }

  /**
   * Fill the ElfNhdr object according to Proc object.
   * 
   * @param nhdrEntry
   * @param proc
   * @return less than zero when error occurs, or return one value that is equal
   *         to zero or more than zero.
   */
  protected int fillENoteAuxv (final ElfNhdr nhdrEntry, Proc proc)
  {
    final ElfPrAuxv prAuxv = new ElfPrAuxv();

    AuxvBuilder builder = new AuxvBuilder()
    {

      public void buildBuffer (byte[] auxv)
      {
        prAuxv.setAuxvBuffer(auxv);
      }

      public void buildDimensions (int wordSize, boolean bigEndian, int length)
      {
      }

      public void buildAuxiliary (int index, int type, long val)
      {
      }
    };
    builder.construct(proc.getPid());
    nhdrEntry.setNhdrDesc(ElfNhdrType.NT_AUXV, prAuxv);
    return 0;

  }

  /**
   * Fill the ElfNhdr object according to Proc object.
   * 
   * @param nhdrEntry
   * @param proc
   * @return less than zero when error occurs, or return one value that is equal
   *         to zero or more than zero.
   */
  protected int fillENoteFPRegSet (ElfNhdr nhdrEntry, Task task)
  {
    // New PRSTATUS Note Entry.
    ElfPrFPRegSet fpRegSet = new ElfPrFPRegSet();
    Isa register = null;

    register = task.getIsa();

    ByteBuffer registerMaps[] = register.getRegisterBankBuffers(task.getProc().getPid());
    if (registerMaps[1].capacity() <= 0)
      abandonCoreDump(new RuntimeException("FP Register bank is <=0"));
    byte[] regBuffer = new byte[(int) registerMaps[1].capacity()];
    registerMaps[1].get(regBuffer);

    fpRegSet.setFPRegisterBuffer(regBuffer);
    nhdrEntry.setNhdrDesc(ElfNhdrType.NT_FPREGSET, fpRegSet);
    return 0;
  }

  /**
   * Fill the ElfNhdr object according to Proc object.
   * 
   * @param nhdrEntry
   * @param proc
   * @return less than zero when error occurs, or return one value that is equal
   *         to zero or more than zero.
   */
  protected int fillENotePrstatus (ElfNhdr nhdrEntry, Task task)
  {

    // New PRSTATUS Note Entry.
    ElfPrstatus prStatus = new ElfPrstatus();
    Isa register = null;

    // Get the Stat object of a pid
    Stat processStat = new Stat();
    processStat.refresh(task.getTid());

    // Fill in the entries of the prstatus note
    // section
    prStatus.setPrPid(task.getTid());
    prStatus.setPrPpid(processStat.ppid);
    prStatus.setPrPgrp(processStat.pgrp);
    prStatus.setPrSid(processStat.session);
    prStatus.setPrSigPending(processStat.signal);

    register = task.getIsa();

    // Fill register info. There is no generic way to do this.
    if (getArch().equals("frysk.proc.LinuxIa32")
        || getArch().equals("frysk.proc.LinuxIa32On64"))
      {
        // Order for these registers is found in /usr/include/asm/user.h
        // This is not the same order that frysk iterators print out, nor
        // are the names are the same. Create a string[] map to bridge
        // gap between frysk and core file register order.

        String regMap[] = { "ebx", "ecx", "edx", "esi", "edi", "ebp", "eax",
                           "ds", "es", "fs", "gs", "orig_eax", "eip", "cs",
                           "efl", "esp", "ss" };

        for (int i = 0; i < regMap.length; i++)
          prStatus.setPrGPReg(
                              i,
                              register.getRegisterByName(regMap[i]).getBigInteger(
                                                                                  task));
      }
    else if (getArch().equals("frysk.proc.LinuxX8664"))
      {
        String regMap[] = { "r15", "r14", "r13", "r12", "rbp", "rbx", "r11",
                           "r10", "r9", "r8", "rax", "rcx", "rdx", "rsi",
                           "rdi", "orig_rax", "rip", "cs", "eflags", "rsp",
                           "ss", "fs_base", "gs_base", "ds", "es", "fs", "gs" };

        for (int i = 0; i < regMap.length; i++)
          prStatus.setPrGPReg(
                              i,
                              register.getRegisterByName(regMap[i]).getBigInteger(
                                                                                  task));
      }
    else if (getArch().equals("frysk.proc.LinuxPPC")
             || getArch().equals("frysk.proc.LinuxPPC64")
             || getArch().equals("frysk.proc.LinuxPPC32On64"))
      {
        // The number of general purpose regiser.
        int gprSize = 32;

        // The number of total common registers in PPC/PPC64 including nip, msr,
        // etc. Defined in the asm-ppc64/elf.h.
        int elfNGREG = 48;
        int blankRegisterIndex = elfNGREG;

        byte[] zeroVal = new byte[] { 0 };

        // XXX: if one register's offset is not defined in asm-ppc/ptrace.h or
        // asm-ppc64/ptrace.h,
        // we didnot dump it out and fill give the null Name.
        String ppcRegMap[] = { "nip", "msr", "orig_r3", "ctr", "lnk", "xer",
                              "ccr", "mq", "trap", "dar", "dsisr", "result" };
        String ppc64RegMap[] = { "nip", "msr", "orig_r3", "ctr", "lnk", "xer",
                                "ccr", "softe", "trap", "dar", "dsisr",
                                "result" };
        String ppc32On64RegMap[] = { "nip", "msr", "orig_r3", "ctr", "lnk",
                                    "xer", "ccr", null, "trap", "dar", "dsisr",
                                    "result" };
        // Set the general purpose registers.
        for (int index = 0; index < gprSize; index++)
          prStatus.setPrGPReg(
                              index,
                              register.getRegisterByName("gpr" + index).getBigInteger(
                                                                                      task));

        if (getArch().equals("frysk.proc.LinuxPPC"))
          {
            for (int index = 0; index < ppcRegMap.length; index++)
              prStatus.setPrGPReg(
                                  index + gprSize,
                                  register.getRegisterByName(ppcRegMap[index]).getBigInteger(
                                                                                             task));

            // On ppc, some register indexes are not defined in
            // asm-<ISA>/ptrace.h.
            blankRegisterIndex = gprSize + ppcRegMap.length;
          }
        else if (getArch().equals("frysk.proc.LinuxPPC32On64"))
          {
            BigInteger registerVal = null;
            BigInteger zeroBigInt = new BigInteger(zeroVal);

            for (int index = 0; index < ppcRegMap.length; index++)
              {
                if ((ppc32On64RegMap[index] == null)
                    || ppc32On64RegMap[index].equals(""))
                  {
                    // Some registers which is defined on PPC, such as mq, are
                    // not defined on PPC64.
                    registerVal = zeroBigInt;
                  }
                else
                  registerVal = register.getRegisterByName(
                                                           ppc32On64RegMap[index]).getBigInteger(
                                                                                                 task);

                prStatus.setPrGPReg(index + gprSize, registerVal);
              }
            // On ppc, some register indexes are not defined in
            // asm-<ISA>/ptrace.h.
            blankRegisterIndex = gprSize + ppcRegMap.length;
          }
        else
          {
            // Must be 64-bit application on PPC64.
            for (int index = 0; index < ppc64RegMap.length; index++)
              prStatus.setPrGPReg(
                                  index + gprSize,
                                  register.getRegisterByName(ppc64RegMap[index]).getBigInteger(
                                                                                               task));

            blankRegisterIndex = gprSize + ppc64RegMap.length;
          }

        // On ppc64, some register indexes are not defined in
        // asm-<ISA>/ptrace.h.
        BigInteger bigInt = new BigInteger(zeroVal);

        for (int index = blankRegisterIndex; index < elfNGREG; index++)
          prStatus.setPrGPReg(index, bigInt);

      }

    nhdrEntry.setNhdrDesc(ElfNhdrType.NT_PRSTATUS, prStatus);
    return 0;
  }

  /**
   * Fill the ElfNhdr object according to Proc object.
   * 
   * @param nhdrEntry
   * @param proc
   * @return less than zero when error occurs, or return one value that is equal
   *         to zero or more than zero.
   */
  protected int fillENotePrpsinfo (ElfNhdr nhdrEntry, Proc proc)
  {
    // Fill the elf_prpsinfo here.
    int pid = proc.getPid();

    ElfPrpsinfo prpsInfo = new ElfPrpsinfo();

    Stat processStat = new Stat();

    processStat.refresh(pid);

    prpsInfo.setPrState(processStat.state);
    prpsInfo.setPrSname(processStat.state);

    String midStr = null;
    // Transform processStat.zero(int) into char.
    if ((processStat.zero >= 0) && (processStat.zero < 10))
      {
        midStr = String.valueOf(processStat.zero);

        prpsInfo.setPrZomb(midStr.charAt(0));
      }

    if ((processStat.nice >= 0) && (processStat.nice < 10))
      {
        midStr = String.valueOf(processStat.nice);

        prpsInfo.setPrNice(midStr.charAt(0));
      }

    prpsInfo.setPrFlag(processStat.flags);
    prpsInfo.setPrUid(Status.getUID(pid));
    prpsInfo.setPrGid(Status.getGID(pid));

    prpsInfo.setPrPid(pid);
    prpsInfo.setPrPpid(processStat.ppid);
    prpsInfo.setPrPgrp(processStat.pgrp);

    prpsInfo.setPrSid(processStat.session);
    prpsInfo.setPrFname(processStat.comm);

    class BuildCmdLine
        extends CmdLineBuilder
    {
      byte[] args;

      public void buildBuffer (byte[] buf)
      {
        args = buf;
      }

      public void buildArgv (String[] argv)
      {
        // Do nothing.
      }
    }
    BuildCmdLine cmdLine = new BuildCmdLine();
    cmdLine.construct(pid);
    // XXX: if the byte[3] = { 'l', 'o', 'o','p', 0}, then we will get one
    // string with length of 5!
    // However, the length is expected to be 4!
    int tailZeroChars = 0;
    int index = cmdLine.args.length;
    for (; index >= 0; index--)
      {
        if (cmdLine.args[index - 1] != 0)
          break;

        tailZeroChars++;
      }
    prpsInfo.setPrPsargs(new String(cmdLine.args, 0, cmdLine.args.length
                                                     - tailZeroChars).toString());
    nhdrEntry.setNhdrDesc(ElfNhdrType.NT_PRPSINFO, prpsInfo);
    return 0;
  }

  /**
   * Transform all information carried by list into ElfData object.
   * 
   * @param noteSectionData
   * @param list ElfNhdr list.
   * @return the number of invalid ElfNhdr objects.
   */
  protected int constructSectionData (ElfData noteSectionData, List nhdrList)
  {
    int size = 0;

    long secSize = 0;
    long entrySize = 0;

    size = nhdrList.size();
    if (size <= 0)
      return 0;

    // Count the size of the whole PT_NOTE section.
    for (int index = 0; index < size; index++)
      {
        ElfNhdr entry = (ElfNhdr) nhdrList.get(index);

        entrySize = entry.getNhdrEntrySize();
        if (entrySize <= 0)
          {
            // One invalid entry, ignore it.
            nhdrList.remove(index);
            size--;
            index--;
            continue;
          }

        secSize += entrySize;
      }
    // In the operation "new byte[count]", count must be "int".
    // If secSize is bigger than the max of "int' type, how can we do?
    byte[] noteSecBuffer = new byte[(int) secSize];
    long startAddress = 0;

    // Begin to fill the noteSection memory region.
    size = nhdrList.size();
    for (int index = 0; index < size; index++)
      {
        ElfNhdr entry = (ElfNhdr) nhdrList.get(index);

        entry.fillMemRegion(noteSecBuffer, startAddress);

        startAddress += entry.getNhdrEntrySize();
      }

    noteSectionData.setBuffer(noteSecBuffer);
    noteSectionData.setSize(noteSecBuffer.length);

    return size;
  }

  /**
   * Fill the note section with the construct note pieces.
   * 
   * @param noteSection - note section to fill.
   */
  public void fillENoteSection (ElfSection noteSection)
  {
    int ret = - 1;
    int entryCount = 0;

    ArrayList list = new ArrayList();

    // Fill PRPSINFO correctly.
    ElfNhdr prpsinfoNhdr = new ElfNhdr();
    ret = this.fillENotePrpsinfo(prpsinfoNhdr, this.proc);
    if (ret >= 0)
      {

        list.add(entryCount, prpsinfoNhdr);
        entryCount++;
      }

    // Loop tasks for PRSTATUS and FPREGISTERS
    for (int i = 0; i < taskArray.length; i++)
      {

        // PRSTATUS
        ElfNhdr prStatusNhdr = new ElfNhdr();
        ret = this.fillENotePrstatus(prStatusNhdr, taskArray[i]);
        if (ret >= 0)
          {
            list.add(entryCount, prStatusNhdr);
            entryCount++;
          }

        // FP REGISTERS
        ElfNhdr prFPRegSet = new ElfNhdr();
        ret = this.fillENoteFPRegSet(prFPRegSet, taskArray[i]);
        if (ret >= 0)
          {
            list.add(entryCount, prFPRegSet);
            entryCount++;
          }
      }

    // Process Auxillary (AuxV)
    ElfNhdr prAuxVNhdr = new ElfNhdr();
    ret = this.fillENoteAuxv(prAuxVNhdr, this.proc);
    if (ret >= 0)
      {
        // Fill AuxV correctly.
        list.add(entryCount, prAuxVNhdr);
        entryCount++;
      }

    if (list.size() <= 0)
      return;

    // Now all note sections are filled, write it out to
    // to section data.
    ElfData sectionData = noteSection.createNewElfData();
    constructSectionData(sectionData, list);
    sectionData.setType(0);
  }

  /**
   * This process is a utility function for writing the elf core dump in step by
   * step functions. First write the elf header, write notes, construct program
   * header and sections, write string table, and then post process the file.
   * 
   * @param tasks - List of tasks to be in included in the core.
   * @param proc - Proc object core file is to be constructed.
   * @throws ElfFileException
   * @throws ElfException
   */
  protected void write_elf_file (final Task[] tasks, final Proc proc)
      throws ElfFileException, ElfException
  {

    // Start new elf file
    local_elf = new Elf(getConstructedFileName(), ElfCommand.ELF_C_WRITE, true);

    // Build elf header
    int endianType = buildElfHeader(local_elf);

    // Count maps
    final MapsCounter counter = new MapsCounter();
    counter.construct(proc.getMainTask().getTid());

    // Build initial Program segment header including PT_NOTE program header.
    local_elf.createNewPHeader(counter.numOfMaps + 1);

    ElfEHeader elf_header = local_elf.getEHeader();

    elfSectionOffset += ((counter.numOfMaps + 1) * elf_header.phentsize);

    // Build notes section
    buildNotes(local_elf);

    local_elf.flag(ElfCommand.ELF_C_SET, ElfFlags.LAYOUT);

    // Build, and write out memory segments to sections
    final CoreMapsBuilder builder = new CoreMapsBuilder();
    builder.construct(proc.getMainTask().getTid());

    // Build string table.
    buildStringTable(local_elf);

    elf_header = local_elf.getEHeader();
    elf_header.shoff = elfSectionOffset + 2;

    // Elf Header is completed
    local_elf.updateEHeader(elf_header);

    // Write elf file
    final long i = local_elf.update(ElfCommand.ELF_C_WRITE);
    if (i < 0)
      throw new ElfException("LibElf elf_update failed with "
                             + local_elf.getLastErrorMsg());
    local_elf.close();

    // Ugly post process. Libelf will not let us write program segment when
    // class type is set to ET_CORE. Bit confusing. See bz sw 3373.

    boolean postProcess = postProcessElfFile(getConstructedFileName(),
                                             endianType);
    if (! postProcess)
      throw new ElfException(
                             "Could not post process elf core file. Stuck as ET_EXEC");
  }

  /**
   * Internal utility function to generate the standard elf header.
   * 
   * @param local_elf - Elf object to build header for, and to to store in.
   * @return int - Returns the derived endian type.
   */
  protected int buildElfHeader (Elf local_elf)
      throws ElfException
  {

    int endianType = 0;
    // Get the byte order of the architecture.
    Isa arch = proc.getMainTask().getIsa();
    ByteOrder order = arch.getByteOrder();

    // Create the elf header, with the specified word-size.
    local_elf.createNewEHeader(arch.getWordSize());
    ElfEHeader elf_header = local_elf.getEHeader();

    // Set the elf file MSB/LSB byte according to the endian
    // level returned from Isa.
    if (order == inua.eio.ByteOrder.BIG_ENDIAN)
      elf_header.ident[5] = ElfEHeader.PHEADER_ELFDATA2MSB;
    else
      elf_header.ident[5] = ElfEHeader.PHEADER_ELFDATA2LSB;

    endianType = elf_header.ident[5];

    // Version
    elf_header.ident[6] = (byte) local_elf.getElfVersion();

    // EXEC for now, ET_CORE later, during post-processing
    elf_header.type = ElfEHeader.PHEADER_ET_EXEC;

    // Version
    elf_header.version = local_elf.getElfVersion();

    // String Index
    elf_header.shstrndx = 1;

    // Get machine architecture
    String arch_test = getArch();

    if (arch_test.equals("frysk.proc.LinuxIa32"))
      {
        elf_header.machine = ElfEMachine.EM_386;
        elf_header.ident[4] = ElfEHeader.PHEADER_ELFCLASS32;
      }
    if (arch_test.equals("frysk.proc.LinuxPPC64"))
      {
        elf_header.machine = ElfEMachine.EM_PPC64;
        elf_header.ident[4] = ElfEHeader.PHEADER_ELFCLASS64;
      }
    if (arch_test.equals("frysk.proc.LinuxPPC32On64"))
      {
        elf_header.machine = ElfEMachine.EM_PPC;
        elf_header.ident[4] = ElfEHeader.PHEADER_ELFCLASS32;
      }
    if (arch_test.equals("frysk.proc.LinuxX8664"))
      {
        elf_header.machine = ElfEMachine.EM_X86_64;
        elf_header.ident[4] = ElfEHeader.PHEADER_ELFCLASS64;
      }
    if (arch_test.equals("frysk.proc.LinuxIa32On64"))
      {
        elf_header.machine = ElfEMachine.EM_386;
        elf_header.ident[4] = ElfEHeader.PHEADER_ELFCLASS32;
      }

    // Elf Header is completed
    local_elf.updateEHeader(elf_header);

    // Write elf file. Calculate offsets in elf memory.
    final long i = local_elf.update(ElfCommand.ELF_C_NULL);
    if (i < 0)
      throw new ElfException("LibElf elf_update failed with "
                             + local_elf.getLastErrorMsg());

    // Get size of written header
    elf_header = local_elf.getEHeader();
    elfSectionOffset += elf_header.ehsize;
    return endianType;
  }

  /**
   * Internal utility function to generate the NOTES section of the elf core
   * file.
   * 
   * @param local_elf - Elf object to build header for, and to to store in.
   * @throws ElfException
   */
  protected void buildNotes (Elf local_elf) throws ElfException
  {
    // Dump out PT_NOTE
    ElfSection noteSection = local_elf.createNewSection();
    ElfSectionHeader noteSectHeader = noteSection.getSectionHeader();
    ElfPHeader noteProgramHeader = null;

    this.fillENoteSection(noteSection);

    // Modify PT_NOTE section header
    noteSectHeader.type = ElfSectionHeaderTypes.SHTYPE_NOTE;
    noteSectHeader.flags = ElfSectionHeaderTypes.SHFLAG_ALLOC;
    noteSectHeader.nameAsNum = 16;
    noteSectHeader.offset = 0;
    noteSectHeader.addralign = 1;
    noteSectHeader.size = noteSection.getData().getSize();
    noteSection.update(noteSectHeader);

    // Must first ask libelf to construct offset location before
    // adding offset back to program header. Otherwise program offset
    // will be 0.
    if (local_elf.update(ElfCommand.ELF_C_NULL) < 0)
      throw new ElfException("Cannot calculate note section offset");

    // Then re-fetch the elf modified header from section. Now offset
    // is calculated and correct.
    noteSectHeader = noteSection.getSectionHeader();

    // Modify PT_NOTE program header
    noteProgramHeader = local_elf.getPHeader(0);
    noteProgramHeader.type = ElfPHeader.PTYPE_NOTE;
    noteProgramHeader.flags = ElfPHeader.PHFLAG_READABLE;
    noteProgramHeader.offset = noteSectHeader.offset;
    noteProgramHeader.filesz = noteSectHeader.size;

    // Calculate elf section offset.
    elfSectionOffset += noteSectHeader.size;
    noteProgramHeader.align = noteSectHeader.addralign;
    local_elf.updatePHeader(0, noteProgramHeader);
  }

  /**
   * Internal utility function to generate the string table for the elf file.
   * Create a very small static string section. This is needed as the actual
   * program segment data needs to be placed into Elf_Data, and that is a
   * section function. Therefore needs a section table and a section string
   * table. This is how gcore does it (and the only way using libelf).
   * 
   * @param local_elf - Elf object to build header for, and to to store in.
   */
  protected void buildStringTable (Elf local_elf)
  {

    // Make a static string table.
    String a = "\0" + "load" + "\0" + ".shstrtab" + "\0" + "note0" + "\0";
    byte[] bytes = a.getBytes();

    ElfSection lastSection = local_elf.getSection(local_elf.getSectionCount() - 1);

    // Sections need a string lookup table
    ElfSection stringSection = local_elf.createNewSection();
    ElfData data = stringSection.createNewElfData();
    ElfSectionHeader stringSectionHeader = stringSection.getSectionHeader();
    stringSectionHeader.type = ElfSectionHeaderTypes.SHTYPE_STRTAB;
    stringSectionHeader.size = bytes.length;

    stringSectionHeader.offset = lastSection.getSectionHeader().offset
                                 + lastSection.getSectionHeader().size;

    stringSectionHeader.addralign = 1;
    stringSectionHeader.nameAsNum = 6; // offset of .shrstrtab;

    // Set elf data
    data.setBuffer(bytes);
    data.setSize(bytes.length);
    // Update the section table back to elf structures.
    stringSection.update(stringSectionHeader);

    // Repoint shstrndx to string segment number
    ElfEHeader elf_header = local_elf.getEHeader();
    elf_header.shstrndx = (int) stringSection.getIndex();

    // Calculate elf section offset.
    elfSectionOffset += bytes.length;
    local_elf.updateEHeader(elf_header);

  }

  /**
   * Libelf barfs on setting type to ET_CORE, and then adding program segments.
   * So after done with libelf, change it to ET_CORE from ET_EXEC.
   * 
   * @param file_string - File location
   * @param endianType - Endian type
   * @return - Success or fail
   */
  private boolean postProcessElfFile (String file_string, int endianType)
  {

    // temporary
    final int EI_NIDENT = 16;

    File elf_file = new File(file_string);
    if (! elf_file.canRead())
      return false;

    RandomAccessFile raf = null;
    try
      {
        raf = new RandomAccessFile(elf_file, "rw");
        raf.seek(EI_NIDENT);
        // Clean EI_DATA flag, 16-bit size.
        raf.write(0);
        raf.write(0);

        if (endianType == ElfEHeader.PHEADER_ELFDATA2LSB)
          raf.seek(EI_NIDENT);
        else
          raf.seek(EI_NIDENT + 1);

        raf.write(ElfEHeader.PHEADER_ET_CORE);

        raf.close();
      }
    catch (IOException ec)
      {
        ec.printStackTrace();
        return false;
      }

    return true;
  }

  /**
   * Very basic Builder that forward counts the number of maps we have to
   * construct later, and dump to core.
   */
  class MapsCounter
      extends MapsBuilder
  {

    int numOfMaps = 0;

    public void buildBuffer (final byte[] maps)
    {
      maps[maps.length - 1] = 0;
    }

    public void buildMap (final long addressLow, final long addressHigh,
                          final boolean permRead, final boolean permWrite,
                          final boolean permExecute, final boolean permPrivate,
                          final boolean permShared, final long offset,
                          final int devMajor, final int devMinor,
                          final int inode, final int pathnameOffset,
                          final int pathnameLength)
    {
      if (permRead == true)
        numOfMaps++;
    }
  }

  /**
   * Core map file builder. Parse each map, build the section header. Once
   * section header is completed, copy the data from the mapped task memory
   * according to the parameter from the builder: addressLow -> addressHigh and
   * place in an Elf_Data section.
   */
  class CoreMapsBuilder
      extends MapsBuilder
  {

    int numOfMaps = 0;

    byte[] mapsLocal;

    Elf elf;

    public void buildBuffer (final byte[] maps)
    {
      // Safe a refernce to the raw maps.
      mapsLocal = maps;
      maps[maps.length - 1] = 0;
    }

    public void buildMap (final long addressLow, final long addressHigh,
                          final boolean permRead, final boolean permWrite,
                          final boolean permExecute, final boolean permPrivate,
                          final boolean permShared, final long offset,
                          final int devMajor, final int devMinor,
                          final int inode, final int pathnameOffset,
                          final int pathnameLength)
    {

      if (permRead == true)
        {

          // Calculate segment name
          byte[] filename = new byte[pathnameLength];
          boolean writeMap = false;

          System.arraycopy(mapsLocal, pathnameOffset, filename, 0,
                           pathnameLength);
          String sfilename = new String(filename);

          if (writeAllMaps)
            {
              writeMap = true;
            }
          else
            {
              // Should the map be written?
              if (inode == 0)
                writeMap = true;
              if ((inode > 0) && (permWrite))
                writeMap = true;
              if (sfilename.equals("[vdso]"))
                writeMap = true;
              if (sfilename.equals("[stack]"))
                writeMap = true;
              if ((permRead) && (permPrivate))
                if ((! permWrite) && (! permExecute))
                  writeMap = true;
              if (permShared)
                writeMap = true;
            }

          // Get empty progam segment header corresponding to this entry.
          // PT_NOTE's program header entry takes the index: 0. So we should
          // begin from 1.
          final ElfPHeader pheader = local_elf.getPHeader(numOfMaps + 1);

          // Get the section written before this one.
          final ElfPHeader previous = local_elf.getPHeader(numOfMaps);

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

          // Set initial section flags (always ALLOC).
          long sectionFlags = ElfSectionHeaderTypes.SHFLAG_ALLOC; // SHF_ALLOC;

          // Build flags
          if (permRead == true)
            pheader.flags = pheader.flags | ElfPHeader.PHFLAG_READABLE;

          if (permWrite == true)
            {
              pheader.flags = pheader.flags | ElfPHeader.PHFLAG_WRITABLE;
              sectionFlags = sectionFlags | ElfSectionHeaderTypes.SHFLAG_WRITE;
            }

          if (permExecute == true)
            {
              pheader.flags = pheader.flags | ElfPHeader.PHFLAG_EXECUTABLE;
              sectionFlags = sectionFlags
                             | ElfSectionHeaderTypes.SHFLAG_EXECINSTR;
            }

          // Update section header
          ElfSection section = local_elf.createNewSection();
          ElfSectionHeader sectionHeader = section.getSectionHeader();

          // sectionHeader.Name holds the string value. We also need to store
          // the offset into the
          // string table when we write data back;
          sectionHeader.nameAsNum = 1; // String offset of load string

          // Set the rest of the header
          sectionHeader.type = ElfSectionHeaderTypes.SHTYPE_PROGBITS;
          sectionHeader.flags = sectionFlags;
          sectionHeader.addr = pheader.vaddr;
          sectionHeader.offset = pheader.offset;
          sectionHeader.size = pheader.memsz;
          sectionHeader.link = 0;
          sectionHeader.info = 0;
          sectionHeader.addralign = 1;
          sectionHeader.entsize = 0;
          elfSectionOffset += sectionHeader.size;

          // Only actually write the segment if we have previously
          // decided to write that segment.
          if (writeMap)
            {

              ElfData data = section.createNewElfData();

              // Construct file size, if any
              pheader.filesz = pheader.memsz;

              // Load data. How to fail here?
              byte[] memory = new byte[(int) (addressHigh - addressLow)];
              proc.getMainTask().getMemory().get(
                                                 addressLow,
                                                 memory,
                                                 0,
                                                 (int) (addressHigh - addressLow));

              // Set and update back to native elf section
              data.setBuffer(memory);
              data.setSize(memory.length);
              data.setType(0);
            }

          section.update(sectionHeader);

          // inefficient to do this for each map, but alternative is to rerun
          // another builder
          // so for right now, less of two evil. Needs a rethinks.
          final long i = local_elf.update(ElfCommand.ELF_C_NULL);
          if (i < 0)
            System.err.println("update in memory failed with message "
                               + local_elf.getLastErrorMsg());
          sectionHeader = section.getSectionHeader();
          // pheader.offset = sectionHeader.offset;
          pheader.align = sectionHeader.addralign;
          // Write back Segment header to elf structure
          local_elf.updatePHeader(numOfMaps + 1, pheader);

          numOfMaps++;
        }

    }
  }

  /**
   * Function to return a string denoting architecture name.
   * 
   * @return String describe architecture.
   */
  public String getArch ()
  {

    // XXX: I hate this, there must be a better way to get architecture than
    // this ugly, ugly hack

    Isa arch = null;
    arch = proc.getMainTask().getIsa();

    String arch_test = arch.toString();
    String type = arch_test.substring(0, arch_test.lastIndexOf("@"));

    return type;
  }

  public String getConstructedFileName ()
  {
    return filename + "." + proc.getPid();
  }

  static class InterruptEvent
      extends SignalEvent
  {
    Proc proc;

    public InterruptEvent (Proc theProc)
    {

      super(Sig.INT);
      proc = theProc;
      logger.log(Level.FINE, "{0} InterruptEvent\n", this);
    }

    public final void execute ()
    {
      logger.log(Level.FINE, "{0} execute\n", this);
      proc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));
      try
        {
          Manager.eventLoop.join(5);
        }
      catch (InterruptedException e)
        {
          e.printStackTrace();
        }
      System.exit(1);

    }
  }

  public void allExistingTasksCompleted ()
  {

    try
      {
        write_elf_file(taskArray, proc);
        // Run the given Event.
        Manager.eventLoop.add(event);
      }
    catch (final ElfFileException e)
      {
        abandonCoreDump(e);
      }
    catch (final ElfException e)
      {
        abandonCoreDump(e);
      }
  }

}
