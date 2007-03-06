// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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


package lib.elf.tests;

import java.math.BigInteger;
import java.io.File;
import frysk.Config;
import frysk.junit.TestCase;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.elf.ElfData;
import lib.elf.ElfEHeader;
import lib.elf.ElfKind;
import lib.elf.ElfSection;
import lib.elf.ElfSectionHeader;
import lib.elf.ElfPHeader;
import lib.elf.ElfType;
import lib.elf.ElfException;
import lib.elf.ElfFileException;
import lib.elf.ElfPrpsinfo;
import lib.elf.ElfPrAuxv;
import lib.elf.ElfPrstatus;

import frysk.sys.proc.AuxvBuilder;

public class TestElf
    extends TestCase
{
  public void testCore () throws ElfException, ElfFileException
  {
      Elf testElf = new Elf (new File (Config.getPkgDataDir (), "test-core")
			     .getAbsolutePath (),
			     ElfCommand.ELF_C_READ);

    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfEHeader header = testElf.getEHeader();
    assertEquals(3, header.machine);
    assertEquals(52, header.ehsize);
    assertEquals(0, header.entry);
    assertEquals(1, header.version);
    assertEquals(0, header.flags);
    assertEquals(4, header.type);
    assertEquals(0, header.shnum);
    assertEquals(0, header.shentsize);
    assertEquals(0, header.shoff);
    assertEquals(14, header.phnum);
    assertEquals(32, header.phentsize);
    assertEquals(52, header.phoff);

    int count = header.phnum;

    int[] pheaderFlags = { 0, 5, 5, 4, 6, 5, 4, 6, 6, 5, 6, 6, 6, 6 };
    int[] pheaderOffsets = { 500, 4096, 8192, 8192, 12288, 16384, 16384, 24576,
                            28672, 40960, 40960, 45056, 49152, 53248 };
    int[] pheaderSegSizeFile = { 472, 4096, 0, 4096, 4096, 0, 8192, 4096,
                                12288, 0, 4096, 4096, 4096, 90112 };
    int[] pheaderSegSizeMem = { 0, 4096, 102400, 4096, 4096, 1232896, 8192,
                               4096, 12288, 4096, 4096, 4096, 4096, 90112 };
    BigInteger[] pheaderAddr = { new BigInteger("0", 10),
                                new BigInteger("1507328", 10),
                                new BigInteger("1511424", 10),
                                new BigInteger("1613824", 10),
                                new BigInteger("1617920", 10),
                                new BigInteger("10477568", 10),
                                new BigInteger("11710464", 10),
                                new BigInteger("11718656", 10),
                                new BigInteger("11722752", 10),
                                new BigInteger("134512640", 10),
                                new BigInteger("134516736", 10),
                                new BigInteger("3085901824", 10),
                                new BigInteger("3085983744", 10),
                                new BigInteger("3220111360", 10) };

    for (int i = 0; i < count; i++)
      {
        ElfPHeader pheader = testElf.getPHeader(i);
        assertNotNull(pheader);

        if (i == 0)
          assertEquals(0, pheader.align);
        else
          assertEquals(4096, pheader.align);
        assertEquals(pheaderFlags[i], pheader.flags);
        assertEquals(pheaderOffsets[i], pheader.offset);
        assertEquals(0, pheader.paddr);
        assertEquals(pheaderSegSizeFile[i], pheader.filesz);
        assertEquals(pheaderSegSizeMem[i], pheader.memsz);
        if (i == 0)
          assertEquals(4, pheader.type);
        else
          assertEquals(1, pheader.type);
        assertEquals(pheaderAddr[i], new BigInteger("" + pheader.vaddr, 10));
      }

    
  }

  /**
   * Test Prpsinfo note info. Read the note data segment, pass it to
   * ElfPrpsinfo to find the relative pstatus data, and parse. 
   *
   */
  public void testElfCorePrpsNotes () throws ElfException, ElfFileException
  {
    
    Elf testElf = new Elf (new File (Config.getPkgDataDir (), "test-core")
			   .getAbsolutePath (), ElfCommand.ELF_C_READ);
    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfData noteData = findNoteSegment(testElf);
    
    assertNotNull("Cannot find notes section", noteData);
    
    ElfPrpsinfo elfPrpsinfo = new ElfPrpsinfo(noteData);
    //assertEquals("note: state", 'R', elfPrpsinfo.getPrState());
    assertEquals("note zombie", 0, elfPrpsinfo.getPrZomb());
    assertEquals("note: nice", 0, elfPrpsinfo.getPrNice());
    assertEquals("note: flags",8390144, elfPrpsinfo.getPrFlag());  //0x00800600
    assertEquals("note: uid", 500, elfPrpsinfo.getPrUid());
    assertEquals("note: gid", 100, elfPrpsinfo.getPrGid());
    assertEquals("note: pid", 31497, elfPrpsinfo.getPrPid());
    assertEquals("note: ppid", 20765, elfPrpsinfo.getPrPpid());
    assertEquals("note: pgrp", 31497, elfPrpsinfo.getPrPgrp());
    assertEquals("note: sid", 20765, elfPrpsinfo.getPrSid());
    assertEquals("note: fname","a.out", elfPrpsinfo.getPrFname());
    assertEquals("note: args","./a.out ", elfPrpsinfo.getPrPsargs());

  }

  /**
   * Test Prstatus  note info. Read the note data segment, pass it to
   * ElfPrstatus to find the relative pstatus data, and parse. 
   *
   */

  public void testElfCorePrstatusNotes () throws ElfException,  ElfFileException
  {

    // Disable while being investigated.
    if (brokenX8664XXX(4047) || brokenPpcXXX(40477))
	return;

    // Matched against eu-read -n on the core file.
    // XXX: This tests need an x86_64 core file, as well.

    // Note segment of 472 bytes at offset 0x1f4:
    //  Owner          Data size  Type
    //  CORE                 144  PRSTATUS
    //    SIGINFO:  signo: 6, code = 0, errno = 0
    //    signal: 6, pending: 00000000, holding:        0
    //    pid: 31497, ppid = 20765, pgrp = 31497, sid = 20765
    //     utime:      0.000000s,  stime:      0.000000s
    //    cutime:      0.000000s, cstime:      0.000000s
    //    eax: 00000000  ebx: 00007b09  ecx: 00007b09  edx: 00000006
    //    esi: bff0284c  edi: 00b2cff4  ebp: bff027ac  esp: bff02794
    //    eip: 00170410  eflags: 00000246  original eax: 0000010e
    //    cs: 0073  ds: 007b  es: 007b  fs: 0000  gs: 0033  ss: 007b

    Elf testElf = new Elf (new File (Config.getPkgDataDir (), "test-core")
			   .getAbsolutePath (), ElfCommand.ELF_C_READ);
    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfData noteData = findNoteSegment(testElf);

    ElfPrstatus threads =   new ElfPrstatus(noteData);

    // Should only be one thread in this core file.
    assertEquals("Number of  threads",1,threads.getThreadData().size());

    ElfPrstatus elfPrstatusInfo = (ElfPrstatus) threads.getThreadData().get(0);
    
    assertEquals("note: Sig Info -> Sig No",6,elfPrstatusInfo.getPrInfoSiSigno());
    assertEquals("note: Sig Info -> Sig code",0,elfPrstatusInfo.getPrInfoSiCode());
    assertEquals("note: Sig Info -> Sig errno",0,elfPrstatusInfo.getPrInfoSiErrno());
    assertEquals("note: Current signal",6,elfPrstatusInfo.getPrCurSig());
    assertEquals("note: Pending signal",0,elfPrstatusInfo.getPrSigPending());
    assertEquals("note: Holding signal",0,elfPrstatusInfo.getPrSigHold());
    assertEquals("note: Pid",31497,elfPrstatusInfo.getPrPid());
    assertEquals("note: PPid",20765,elfPrstatusInfo.getPrPpid());
    assertEquals("note: Pgrp",31497,elfPrstatusInfo.getPrPgrp());
    assertEquals("note: Sid",20765,elfPrstatusInfo.getPrSid());

    // Order of registers in the raw buffer is defined in 
    // usr/include/asm.user.h
    // ebc, ecx, edx, esi, edi and so on.
    //
    //	long ebx, ecx, edx, esi, edi, ebp, eax;
    //  unsigned short ds, __ds, es, __es;
    //  unsigned short fs, __fs, gs, __gs;
    //  long orig_eax, eip;
    //  unsigned short cs, __cs;
    //  long eflags, esp;
    //  unsigned short ss, __ss;

    // Get raw register buffer.
    byte[] rawRegisterBuffer = elfPrstatusInfo.getRawCoreRegisters();

    assertEquals("note: ebx",0x00007b09,
		 getRegisterByOffset(rawRegisterBuffer,0,4,ByteOrder.LITTLE_ENDIAN));
    assertEquals("note: ecx",0x00007b09,
		 getRegisterByOffset(rawRegisterBuffer,4,4,ByteOrder.LITTLE_ENDIAN));
    assertEquals("note: edx",0x00000006,
		 getRegisterByOffset(rawRegisterBuffer,8,4,ByteOrder.LITTLE_ENDIAN));

    BigInteger tmp = new BigInteger("3220187212"); //0xbff0284c
    assertEquals("note: esi",tmp.longValue(), 
		 getRegisterByOffset(rawRegisterBuffer,12,4,ByteOrder.LITTLE_ENDIAN));
    assertEquals("note: edi",0x00b2cff4,
		 getRegisterByOffset(rawRegisterBuffer,16,4,ByteOrder.LITTLE_ENDIAN));

    tmp = new BigInteger("3220187052"); //0xbff027ac
    assertEquals("note: ebp",tmp.longValue(),
		 getRegisterByOffset(rawRegisterBuffer,20,4,ByteOrder.LITTLE_ENDIAN));
    assertEquals("note: eax",0x00000000,
		 getRegisterByOffset(rawRegisterBuffer,24,4,ByteOrder.LITTLE_ENDIAN));

    assertEquals("note: ds",0x0000007b,
		 getRegisterByOffset(rawRegisterBuffer,28,2,ByteOrder.LITTLE_ENDIAN));
    assertEquals("note: es",0x0000007b,
		 getRegisterByOffset(rawRegisterBuffer,32,2,ByteOrder.LITTLE_ENDIAN));
    assertEquals("note: fs",0x00000000,
		 getRegisterByOffset(rawRegisterBuffer,36,2,ByteOrder.LITTLE_ENDIAN));
    assertEquals("note: gs",0x00000033,
		 getRegisterByOffset(rawRegisterBuffer,40,2,ByteOrder.LITTLE_ENDIAN));
    assertEquals("note: oeax",0x0000010e,
		 getRegisterByOffset(rawRegisterBuffer,44,4,ByteOrder.LITTLE_ENDIAN));
    assertEquals("note: eip",0x00170410,
		 getRegisterByOffset(rawRegisterBuffer,48,4,ByteOrder.LITTLE_ENDIAN));
    assertEquals("note: cs",0x00000073,
		 getRegisterByOffset(rawRegisterBuffer,52,4,ByteOrder.LITTLE_ENDIAN));
    assertEquals("note: eflags",0x00000246,
		 getRegisterByOffset(rawRegisterBuffer,56,4,ByteOrder.LITTLE_ENDIAN));

    tmp = new BigInteger("3220187028");
    assertEquals("note: esp",tmp.longValue(),
		 getRegisterByOffset(rawRegisterBuffer,60,4,ByteOrder.LITTLE_ENDIAN));

  }

  /**
   * Test PrAuxv  note info. Read the note data segment, pass it to
   * ElfPrAuxv to find the relative pstatus data, and parse. 
   *
   */

  public void testElfCorePrAuxvNotes () throws ElfException,
    ElfFileException 
  {

    Elf testElf = new Elf (new File (Config.getPkgDataDir (), "test-core")
			   .getAbsolutePath (), ElfCommand.ELF_C_READ);
    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfData noteData = findNoteSegment(testElf);

    final ElfPrAuxv prAuxv = new ElfPrAuxv(noteData);
    final int[] expectedIndex = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17};
    final int[] expectedType = {32,33,16,6,17,3,4,5,7,8,9,11,12,13,14,23,15,0};
    final BigInteger[] expectedVal = {new BigInteger("1508352",10),
				      new BigInteger("1507328",10), 				
				      new BigInteger("3219782655",10),						    	
				      new BigInteger("4096",10),
				      new BigInteger("100",10),
				      new BigInteger("134512692",10),
				      new BigInteger("32",10),
				      new BigInteger("7",10),
				      new BigInteger("0",10),
				      new BigInteger("0",10),
				      new BigInteger("134513376",10),
				      new BigInteger("500",10),
				      new BigInteger("500",10),
				      new BigInteger("100",10),
				      new BigInteger("100",10),
				      new BigInteger("0",10),
				      new BigInteger("3220187851",10),
				      new BigInteger("0",10)};
				

    AuxvBuilder builder = new AuxvBuilder()
    {

      int auxvIndex=0;
      public void buildBuffer (byte[] auxv)
      {
      }

      public void buildDimensions (int wordSize, boolean bigEndian, int length)
      {
      }

      public void buildAuxiliary (int index, int type, long val)
      {
	// Test the entries in the core file against the entries
	// stored in the arrays
	assertEquals("AuxV Index "+auxvIndex,expectedIndex[auxvIndex],index);
	assertEquals("AuxV Type "+auxvIndex,expectedType[auxvIndex],type);
	assertEquals("AuxV Val "+auxvIndex,expectedVal[auxvIndex],new BigInteger("" + val, 10));
	auxvIndex++;
      }
    };
    builder.construct(prAuxv.getAuxvBuffer());


  }

  public void testObjectFile () throws ElfException, ElfFileException
  {
      Elf testElf = new Elf (new File (Config.getPkgDataDir (), "helloworld.o")
			     .getAbsolutePath (),
			     ElfCommand.ELF_C_READ);

    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfEHeader header = testElf.getEHeader();
    assertEquals("machine", 3, header.machine);
    assertEquals("ehsize", 52, header.ehsize);
    assertEquals("entry", 0, header.entry);
    assertEquals("version", 1, header.version);
    assertEquals("flags", 0, header.flags);
    assertEquals("type", 1, header.type);
    assertEquals("shnum", 11, header.shnum);
    assertEquals("shentsize", 40, header.shentsize);
    assertEquals("shoff", 236, header.shoff);
    assertEquals("phnum", 0, header.phnum);
    assertEquals("phentsize", 0, header.phentsize);
    assertEquals("phoff", 0, header.phoff);

    int[] expectedIndices = { 0, 52, 864, 96, 96, 96, 110, 155, 155, 676, 836 };
    int[] expectedInfo = { 0, 0, 1, 0, 0, 0, 0, 0, 0, 8, 0 };
    int[] expectedAlign = { 0, 4, 4, 4, 4, 1, 1, 1, 1, 4, 1 };
    int[] expectedEntrySize = { 0, 0, 8, 0, 0, 0, 0, 0, 0, 16, 0 };
    int[] expectedFlags = { 0, 6, 0, 3, 3, 2, 0, 0, 0, 0, 0 };
    String[] expectedNames = {"", ".text", ".rel.text", ".data", ".bss",
                              ".rodata", ".comment", ".note.GNU-stack",
                              ".shstrtab", ".symtab", ".strtab" };
    int[] expectedSize = { 0, 43, 16, 0, 0, 14, 45, 0, 81, 160, 25 };
    int[] expectedTypes = { 0, 1, 9, 1, 8, 1, 1, 1, 3, 2, 3 };

    int[] expectedDataSizes = { 0, 43, 16, 0, 0, 14, 45, 0, 81, 160, 25 };
    int[] expectedDataAlignments = { 0, 4, 4, 4, 4, 1, 1, 1, 1, 4, 1 };
    ElfType[] expectedDataTypes = { ElfType.ELF_T_BYTE, ElfType.ELF_T_BYTE,
                                   ElfType.ELF_T_REL, ElfType.ELF_T_BYTE,
                                   ElfType.ELF_T_BYTE, ElfType.ELF_T_BYTE,
                                   ElfType.ELF_T_BYTE, ElfType.ELF_T_BYTE,
                                   ElfType.ELF_T_BYTE, ElfType.ELF_T_SYM,
                                   ElfType.ELF_T_BYTE };
    int[] expectedBytes = { 0, - 115, 20, 0, 0, 72, 0, 0, 0, 0, 0 };

    for (int i = 0; i < header.shnum; i++)
      {
        ElfSection section = testElf.getSection(i);
        assertNotNull("section-" + i, section);
        assertEquals("section-" + i + ".getIndex()", section.getIndex(), i);

        ElfSectionHeader sheader = section.getSectionHeader();
        assertNotNull(sheader);

        assertEquals("section-" + i + "-addr", 0, sheader.addr);
        assertEquals("section-" + i + "-offset", expectedIndices[i], sheader.offset);
        assertEquals("section-" + i + "-info", expectedInfo[i], sheader.info);
        assertEquals("section-" + i + "-addralign", expectedAlign[i], sheader.addralign);
        assertEquals("section-" + i + "-entsize", expectedEntrySize[i], sheader.entsize);
        assertEquals("section-" + i + "-flags", expectedFlags[i], sheader.flags);
        assertEquals("section-" + i + "-name", expectedNames[i], sheader.name);
        assertEquals("section-" + i + "-size", expectedSize[i], sheader.size);
        assertEquals("section-" + i + "-type", expectedTypes[i], sheader.type);

        ElfData data = section.getData();
        assertNotNull(data);
        assertEquals("section-" + i + "-alignment", expectedDataAlignments[i],
                     data.getAlignment());
        assertEquals("section-" + i + "-offset", 0, data.getOffset());
        assertEquals("section-" + i + "-size", expectedDataSizes[i], data.getSize());
        assertEquals("section-" + i + "-type", expectedDataTypes[i], data.getType());
        if (data.getSize() != 0)
          assertEquals("section-" + i + "-byte", expectedBytes[i], data.getByte(0));
      }
  }

  // Copied from frysk-core/Register.java and reused here.
  // Does this really not exist somewhere else?
  private static void reverseArray(byte[] array) 
  {
    for (int left = 0, right = array.length - 1; left < right; left++, right--)
      {
	byte temp = array[right];
	array[right] = array[left];
	array[left] = temp;
      }
  }

  /**
   * Helper routine that given a buffer, offset and length returns a
   * register value as a long
   */
  private long getRegisterByOffset(byte[] buffer, int offset, int length, ByteOrder endian)
  {
    // Wrap buffer.
    ByteBuffer regBuffer = ByteBuffer.wrap(buffer);
    regBuffer.order(endian);
    long val = 0;
    byte[] regBytes = new byte[length];
    
    // Get the bytes at the offset and length, and 
    // reverse if necessary.
    
    regBuffer.position(offset);
    regBuffer.get(regBytes, 0,length);
    if (regBuffer.order() == ByteOrder.LITTLE_ENDIAN)
      reverseArray(regBytes);

    // Convert byte array to long value.
    for (int i = 0; i < length; i++) 
	val = val << 8 | (regBytes[i] & 0xff);

    return val;
  }
  
  /**
   * Helper routine that give an Elf object, find the
   * note segment and returns the contents in ElfData
   */
  private ElfData findNoteSegment(Elf testElf)
  {
    // Get Elf Header.
    ElfEHeader eHeader = testElf.getEHeader();
    ElfData noteData = null;
    
    // Get number of program header entries.
    long phSize = eHeader.phnum;
    for (int i=0; i<phSize; i++)
      {
	// Test if pheader is of types notes..
	ElfPHeader pHeader = testElf.getPHeader(i);
	if (pHeader.type == ElfPHeader.PTYPE_NOTE)
	  {
	    // if so, copy, break an leave.
	    noteData = testElf.getRawData(pHeader.offset,pHeader.filesz);
	    break;
	  }
      }
    
    return noteData;
  }
}
