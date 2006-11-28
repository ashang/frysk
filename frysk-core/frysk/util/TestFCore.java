// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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


package frysk.util;

import inua.eio.ByteOrder;

import java.io.File;

import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.elf.ElfEHeader;
import lib.elf.ElfEMachine;
import lib.elf.ElfException;
import lib.elf.ElfFileException;
import lib.elf.ElfKind;
import lib.elf.ElfPHeader;
import lib.elf.ElfSection;
import lib.elf.ElfSectionHeader;
import lib.elf.ElfSectionHeaderTypes;
import frysk.event.Event;
import frysk.event.RequestStopEvent;
import frysk.proc.Isa;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.TaskException;
import frysk.sys.proc.MapsBuilder;

public class TestFCore
    extends TestLib
{

  public void testCoreFileCreated ()
  {
    Proc ackProc = giveMeAProc();
    String coreFileName = constructCore(ackProc);
    File testCore = new File(coreFileName);
    assertTrue("Core file " + coreFileName + " does not exist.",testCore.exists());
    testCore.delete();
  }
  
  public void testElfCoreHeader ()
  {
 
    Proc ackProc = giveMeAProc();
    String coreFileName = constructCore(ackProc);
    File testCore = new File(coreFileName);
    assertTrue("Checking core file " + coreFileName + " exists.",testCore.exists());
    
    Isa arch = getIsa(ackProc);
    ByteOrder order = arch.getByteOrder();
    
    Elf local_elf = null;
    // Start new elf file
    try
      {
        local_elf = new Elf(coreFileName, ElfCommand.ELF_C_READ);
      }
    catch (ElfFileException e)
      {
        fail(e.getMessage());
      }
    catch (ElfException e)
      {
        fail(e.getMessage());
      }
    assertNotNull("elf variable is null", local_elf);
       
    assertEquals("Checking ELF Kind", local_elf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals("Checkin base 0",local_elf.getBase(), 0);

  
    ElfEHeader header = local_elf.getEHeader();

    // Check the elf file MSB/LSB byte according to the endian
    // level returned from Isa.
    if (order == inua.eio.ByteOrder.BIG_ENDIAN)
      assertEquals("Checking endian is appropriate to platform", 
                   header.ident[5], ElfEHeader.PHEADER_ELFDATA2MSB);
    else
      assertEquals("Checking endian is appropriate to platform",
                   header.ident[5], ElfEHeader.PHEADER_ELFDATA2LSB);
 
    // Check version written
    assertEquals("Checking elf version and ident core file version",
                 header.ident[6], (byte) local_elf.getElfVersion());
   
    // Check version written
    assertEquals("Checking elf version and non-ident core file version",
                 header.version, (byte) local_elf.getElfVersion());
    
    // Check Header Type
    assertEquals("Checking Header type is ET_CORE",
                 header.type, ElfEHeader.PHEADER_ET_CORE);
    
    // Get machine architecture
    String arch_test = getArch(arch);

    // Check machine and class
    if (arch_test.equals("frysk.proc.LinuxIa32"))
      {
        assertEquals("Checking header machine type", header.machine, ElfEMachine.EM_386);
        assertEquals("Checking elf class", header.ident[4], ElfEHeader.PHEADER_ELFCLASS32);
      }
    if (arch_test.equals("frysk.proc.LinuxPPC64"))
      {
        assertEquals("Checking header machine type", header.machine, ElfEMachine.EM_PPC64);
        assertEquals("Checking elf class", header.ident[4], ElfEHeader.PHEADER_ELFCLASS64);
       }
    if (arch_test.equals("frysk.proc.LinuxPPC32On64"))
      {
        assertEquals("Checking header machine type", header.machine, ElfEMachine.EM_PPC);
        assertEquals("Checking elf class", header.ident[4], ElfEHeader.PHEADER_ELFCLASS32);
      }
    if (arch_test.equals("frysk.proc.LinuxX8664"))
      {
        assertEquals("Checking header machine type", header.machine, ElfEMachine.EM_X86_64);
        assertEquals("Checking elf class", header.ident[4], ElfEHeader.PHEADER_ELFCLASS64);
      }
    if (arch_test.equals("frysk.proc.LinuxIa32On64"))
      {
        assertEquals("Checking header machine type", header.machine, ElfEMachine.EM_386);
        assertEquals("Checking elf class", header.ident[4], ElfEHeader.PHEADER_ELFCLASS32);
      }

    testCore.delete();
  }
  
  public void testProgramSegmentHeader ()
  {
    Proc ackProc = giveMeAProc();
    String coreFileName = constructCore(ackProc);
    File testCore = new File(coreFileName);
    assertTrue("Checking core file " + coreFileName + " exists.",testCore.exists());
    
    Elf local_elf = null;
    // Start new elf file
    try
      {
        local_elf = new Elf(coreFileName, ElfCommand.ELF_C_READ);
      }
    catch (ElfFileException e)
      {
        fail(e.getMessage());
      }
    catch (ElfException e)
      {
        fail(e.getMessage());
      }
    assertNotNull("elf variable is null", local_elf);
    

    
    // Build, and write out memory segments to sections
    final ProgramHeaderMapsTester builder = new ProgramHeaderMapsTester(local_elf);
    builder.construct(ackProc.getMainTask().getTid());
    
    testCore.delete();
  }

  /**
   * Given a Proc object, generate a core file from that
   * given proc.
   * 
   * @param ackProc - proc object to generate core from.
   * @return - name of constructed core file.
   */
  public String constructCore (final Proc ackProc)
  {

    final CoredumpAction coreDump = new CoredumpAction(ackProc, new Event()
    {

      public void execute ()
      {
        ackProc.requestAbandonAndRunEvent(new RequestStopEvent(Manager.eventLoop));
      }
    });

    assertRunUntilStop("Running event loop for core file");
    return coreDump.getConstructedFileName();
  }
  
  
  /**
   * Builder that matches the maps in the process to those in 
   * the core file, and lints the segment/program segment
   * values.
   */
  class ProgramHeaderMapsTester extends MapsBuilder {

    int numOfMaps = 0;
    int count = 0;
    Elf elf;
    
    ProgramHeaderMapsTester(Elf elf)
    {
      this.elf = elf;
      ElfEHeader header = elf.getEHeader();  
      count = header.phnum;
    }
    
    public void buildBuffer(final byte[] maps) {
      maps[maps.length - 1] = 0;
    }

    public void buildMap(final long addressLow, final long addressHigh,
                         final boolean permRead, final boolean permWrite, final boolean permExecute,
                         final boolean permPrivate, final long offset, final int devMajor, final int devMinor,
                         final int inode, final int pathnameOffset, final int pathnameLength) {
                         if (permRead == true)
                           {
                             int flags = 0;
                            
                             // Special Case For Notes. First entry in the maps and the first entry in the 
                             // file will not match. Check sanity of the notes segment, then advance counter.
                             if (numOfMaps == 0)
                               {
                                 ElfPHeader pheader = elf.getPHeader(numOfMaps);
                                 assertEquals("Checking Program Header type for NOTES", 
                                              ElfPHeader.PTYPE_NOTE,pheader.type);
                                 ElfSection section = elf.getSection(numOfMaps + 1);
                                 ElfSectionHeader sheader = section.getSectionHeader();
                                 
                                 assertEquals("Testing section header type for NOTES",
                                              ElfSectionHeaderTypes.SHTYPE_NOTE,sheader.type);
                                 numOfMaps++;
                               }
                             
                             // Continue on with normal program header segment checking.
                             ElfPHeader pheader = elf.getPHeader(numOfMaps);
                             assertEquals("Checking Program Header type",ElfPHeader.PTYPE_LOAD,pheader.type);
                             assertEquals("Checking Program Header vaddr",addressLow,pheader.vaddr);
                             assertEquals("Checking Program Header memsz", addressHigh - addressLow,pheader.memsz);
                             
                             // Check for flags. Have to build them first.
                             long sectionFlags = ElfSectionHeaderTypes.SHFLAG_ALLOC;
                             // Build flags
                             if (permRead == true)
                               flags = flags | ElfPHeader.PHFLAG_READABLE;

                             if (permWrite == true)
                               {
                                 flags = flags | ElfPHeader.PHFLAG_WRITABLE;
                                 sectionFlags = sectionFlags | ElfSectionHeaderTypes.SHFLAG_WRITE;
                               }

                             if (permExecute == true)
                               {
                                 flags = flags | ElfPHeader.PHFLAG_EXECUTABLE;
                                 sectionFlags = sectionFlags
                                 | ElfSectionHeaderTypes.SHFLAG_EXECINSTR;
                               }
                             assertEquals("Checking Program Header flags", flags, pheader.flags);

                             // Check if the should be a filez value, and if so, it is correct.
                             if (ElfPHeader.PHFLAG_WRITABLE == (flags & ElfPHeader.PHFLAG_WRITABLE))
                               assertEquals("Checking filesz",pheader.memsz, pheader.filesz);
                             
                             // Now check the corresponding section and section data
                             // mappings to ensure they match.
                             ElfSection section = elf.getSection(numOfMaps+1);
                             ElfSectionHeader sheader = section.getSectionHeader();
                             
                             assertEquals("Testing section header type",
                                          ElfSectionHeaderTypes.SHTYPE_PROGBITS,sheader.type);
                             assertEquals("Testing section header flags", sectionFlags,sheader.flags); 
                             assertEquals("Testing section size", sheader.size, pheader.memsz);
                            
                             numOfMaps++;
                           }
    }
  }
  
  /**
   * Generate a process suitable for attaching to (ie detached when returned).
   * 
   * Stop the process, check that is is found in the frysk state machine, then
   * return a proc oject corresponding to that process.
   * 
   * @return - Proc - generated process.
   */
  protected Proc giveMeAProc() 
  {
    AckProcess ackProc = new DetachedAckProcess();
    assertNotNull(ackProc);
    ackProc.sendStopXXX();
    Proc proc = ackProc.assertFindProcAndTasks();
    assertNotNull(proc);
    return proc;
  }
  
  /**
   * 
   * Return a string representing the architecture of the given ISA.
   * 
   * Really need to make a better ISA arch test.
   * 
   * @param isa - Isa to test
   * @return String - a string corresponding to the arch.
   */
  protected String getArch(Isa isa)
  {
    String arch_test = isa.toString();
    String type = arch_test.substring(0, arch_test.lastIndexOf("@"));

    return type;
  }
  
  /**
   * 
   * Returns the ISA that corresponds to the given Proc
   * @param Proc - the proc to test
   * @return Isa - the Isa that corresponds to given proc. 
   */
  protected Isa getIsa(Proc proc)
  {
    Isa arch = null;
    try
      {
        arch = proc.getMainTask().getIsa();
      }
    catch (TaskException e)
      {
        fail(e.getMessage());
      }
    return arch;
  }
}
