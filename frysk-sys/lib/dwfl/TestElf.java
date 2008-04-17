// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008 Red Hat Inc.
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

package lib.dwfl;

import frysk.config.Prefix;
import frysk.junit.TestCase;
import inua.eio.ArrayByteBuffer;
import inua.eio.ByteOrder;
import frysk.sys.proc.AuxvBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;

public class TestElf
    extends TestCase
{

    public void testCore_x8664() {
	Elf testElf = new Elf (Prefix.pkgDataFile("test-core-x8664"),
			       ElfCommand.ELF_C_READ);

    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfEHeader header = testElf.getEHeader();
    assertEquals("word-size", 8, header.getWordSize());
    assertEquals("byte-order", ByteOrder.LITTLE_ENDIAN, header.getByteOrder());
    assertEquals(62, header.machine);
    assertEquals(64, header.ehsize);
    assertEquals(0, header.entry);
    assertEquals(1, header.version);
    assertEquals(0, header.flags);
    assertEquals(4, header.type);
    assertEquals(0, header.shnum);
    assertEquals(0, header.shentsize);
    assertEquals(0, header.shoff);
    assertEquals(25, header.phnum);
    assertEquals(56, header.phentsize);
    assertEquals(64, header.phoff);

    int count = header.phnum;

    int[] pheaderFlags = { 0, 5, 6, 6, 0, 6, 0, 6, 5, 4, 6, 5, 0, 4,
			   6, 6, 5, 0, 4, 6, 6, 6, 6, 6, 5};

    int[] pheaderOffsets = { 0x5b8, 0x2000, 0x2000, 0x3000, 0x24000,
			     0x25000, 0xa25000, 0xa26000, 0x1426000,
			     0x1426000, 0x1427000, 0x1428000, 0x1428000,
			     0x1428000, 0x142c000, 0x142d000, 0x1432000,
			     0x1432000, 0x1432000, 0x1433000, 0x1434000,
			     0x1438000, 0x1439000, 0x143c000, 0x1451000};

    int[] pheaderSegSizeFile = {0xc18, 0x0, 0x1000, 0x21000,
				0x1000,0xa00000, 0x1000, 0xa00000, 0x0,
				0x1000, 0x1000, 0x0, 0x0, 0x4000,
				0x1000, 0x5000, 0x0, 0x0, 0x1000,
				0x1000, 0x4000,0x1000,0x3000,0x15000,0x0 };

    int[] pheaderSegSizeMem = {0x0, 0x1000, 0x1000, 0x21000, 0x1000,
			       0xa00000, 0x1000, 0xa00000, 0x1a000,
			       0x1000, 0x1000, 0x147000, 0x1ff000,
			       0x4000, 0x1000, 0x5000, 0x15000,
			       0x1ff000, 0x1000, 0x1000, 0x4000,
			       0x1000, 0x3000, 0x15000, 0x1000};

    long[] pheaderAddr = {0x0L, 0x400000L, 0x600000L, 0x601000L,
			  0x40000000L, 0x40001000L, 0x40a01000L,
			  0x40a02000L, 0x316c600000L,
			  0x316c819000L, 0x316c81a000L,
			  0x316ca00000L, 0x316cb47000L,
			  0x316cd46000L, 0x316cd4a000L,
			  0x316cd4b000L, 0x3173c00000L,
			  0x3173c15000L, 0x3173e14000L,
			  0x3173e15000L, 0x3173e16000L,
			  0x2aaaaaaab000L, 0x2aaaaaaca000L,
			  0x7fffc1ca5000L, 0xffffffffff600000L};
			  
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

        assertEquals(pheaderAddr[i], pheader.vaddr);
      }

  }

  public void testCore_x86() {
      Elf testElf = new Elf(Prefix.pkgDataFile ("test-core-x86"),
			    ElfCommand.ELF_C_READ);

    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfEHeader header = testElf.getEHeader();
    assertEquals("word size", 4, header.getWordSize());
    assertEquals("byte-order", ByteOrder.LITTLE_ENDIAN, header.getByteOrder());
    assertEquals(3, header.machine);
    assertEquals(52, header.ehsize);
    assertEquals(0, header.entry);
    assertEquals(1, header.version);
    assertEquals(0, header.flags);
    assertEquals(4, header.type);
    assertEquals(0, header.shnum);
    assertEquals(0, header.shentsize);
    assertEquals(0, header.shoff);
    assertEquals(21, header.phnum);
    assertEquals(32, header.phentsize);
    assertEquals(52, header.phoff);

    int count = header.phnum;

    int[] pheaderFlags = { 0, 5, 5, 6, 6, 5, 4, 6, 5, 4, 6, 6, 5, 4,6,6,0,6,0,6,6};
    int[] pheaderOffsets = { 0x2d4, 0x1000,0x2000, 0x2000, 0x3000,
			     0x24000,0x24000, 0x25000, 0x26000,
			     0x26000, 0x28000, 0x29000, 0x2c000,
			     0x2c000, 0x2d000, 0x2e000, 0x30000,
			     0x31000, 0xa31000, 0xa32000, 0x1434000};

    int[] pheaderSegSizeFile = {0xadc, 0x1000, 0x0, 0x1000,
				0x21000,0x0, 0x1000, 0x1000, 0x0,
				0x2000, 0x1000, 0x3000, 0x0, 0x1000,
				0x1000, 0x2000, 0x1000, 0xa00000,
				0x1000, 0xa02000, 0x15000 };

    int[] pheaderSegSizeMem = {0x0, 0x1000, 0x1000, 0x1000, 0x21000,
			       0x1b000, 0x1000, 0x1000, 0x14e000,
			       0x2000, 0x1000, 0x3000, 0x14000,
			       0x1000, 0x1000,0x2000, 0x1000,
			       0xa00000, 0x1000,0xa02000, 0x15000 };

    long[] pheaderAddrx86 = {0x0L,0x62a000L, 0x8048000L, 0x8049000L, 0x8bd5000L,
			  0x4104c000L, 0x41067000L, 0x41068000L, 0x4106b000L,
			  0x411b9000L, 0x411bb000L, 0x411bc000L, 0x4147b000L,
			  0x4148f000L, 0x41490000L, 0x41491000L, 0xb6b60000L,
			  0xb6b61000L, 0xb7561000L, 0xb7562000L, 0xbfceb000L };

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

        assertEquals(pheaderAddrx86[i], pheader.vaddr);
      }

    
  }

    /**
     * Test x8664 Prpsinfo note info. Read the note data segment, pass
     * it to ElfPrpsinfo to find the relative pstatus data, and parse.
     */
    public void testElfCorePrpsNotes_x8664() {
	Elf testElf = new Elf (Prefix.pkgDataFile ("test-core-x8664"),
			       ElfCommand.ELF_C_READ);
    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfData noteData = findNoteSegment(testElf);
    
    assertNotNull("Cannot find notes section", noteData);
    
    ElfPrpsinfo elfPrpsinfo =  ElfPrpsinfo.decode(noteData);
    assertEquals("note: zombie", 0, elfPrpsinfo.getPrZomb());
    assertEquals("note: nice", 0, elfPrpsinfo.getPrNice());
    assertEquals("note: flags",0x00402600, elfPrpsinfo.getPrFlag());  
    assertEquals("note: uid", 500, elfPrpsinfo.getPrUid());
    assertEquals("note: gid", 500, elfPrpsinfo.getPrGid());
    assertEquals("note: pid", 3158, elfPrpsinfo.getPrPid());
    assertEquals("note: ppid", 2966, elfPrpsinfo.getPrPpid());
    assertEquals("note: pgrp", 3158, elfPrpsinfo.getPrPgrp());
    assertEquals("note: sid", 2966, elfPrpsinfo.getPrSid());
    assertEquals("note: fname","segfault", elfPrpsinfo.getPrFname());
    assertEquals("note: args","/home/pmuldoon/segfault ", elfPrpsinfo.getPrPsargs());
  }


  /**
   * Test i386 Prpsinfo note info. Read the note data segment, pass it to
   * ElfPrpsinfo to find the relative pstatus data, and parse. 
   *
   */
    public void testElfCorePrpsNotes_x86() {
	Elf testElf = new Elf (Prefix.pkgDataFile ("test-core-x86"),
			       ElfCommand.ELF_C_READ);
    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfData noteData = findNoteSegment(testElf);
    
    assertNotNull("Cannot find notes section", noteData);
    
    ElfPrpsinfo elfPrpsinfo =  ElfPrpsinfo.decode(noteData);
    assertEquals("note: zombie", 0, elfPrpsinfo.getPrZomb());
    assertEquals("note: nice", 0, elfPrpsinfo.getPrNice());
    assertEquals("note: flags",0x00402600, elfPrpsinfo.getPrFlag());  
    assertEquals("note: uid", 500, elfPrpsinfo.getPrUid());
    assertEquals("note: gid", 500, elfPrpsinfo.getPrGid());
    assertEquals("note: pid", 26799, elfPrpsinfo.getPrPid());
    assertEquals("note: ppid", 2859, elfPrpsinfo.getPrPpid());
    assertEquals("note: pgrp", 26799, elfPrpsinfo.getPrPgrp());
    assertEquals("note: sid", 2859, elfPrpsinfo.getPrSid());
    assertEquals("note: fname","segfault", elfPrpsinfo.getPrFname());
    assertEquals("note: args","/home/pmuldoon/segfault ", elfPrpsinfo.getPrPsargs());

  }

    /**
     * Test i386 Prstatus note info. Read the note data segment, pass
     * it to ElfPrstatus to find the relative pstatus data, and parse.
     */
    public void testElfCorePrstatusNotes_x86() {
	Elf testElf = new Elf (Prefix.pkgDataFile ("test-core-x86"),
			       ElfCommand.ELF_C_READ);
    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfData noteData = findNoteSegment(testElf);

    ElfPrstatus threads[] =  ElfPrstatus.decode(noteData);
    ElfPrFPRegSet fpthreads [] = ElfPrFPRegSet.decode(noteData);
    
    assertEquals("Number of threads in notes",3,threads.length);
    // Should only be one thread in this core file.
    assertEquals("Number of  floating point thread data in notes ",3,fpthreads.length);

    // Preload 3 threads worth of data into the static data structure

    int[] threadSigNo = {11, 11, 11};
    int[] threadSigCode = {0, 0, 0};
    int[] threadSigErrno = {0, 0, 0};
    int[] threadCurrentSig = {11, 11, 11};
    int[] threadPendingSig = {0, 0, 0};
    int[] threadHoldingSig = {0, 0, 0};
    int[] threadPid = {26799, 26801, 26800};
    int[] threadPPid = {2859, 2859, 2859};
    int[] threadPgrp = {26799, 26799, 26799};
    int[] threadSid = {2859, 2859, 2859};
    long[] eax = {0x80486a8L, 0xfffffffcL, 0xfffffffcL};
    long[] ebx = {0x411baff4L, 0x080498ecL, 0x080498ecL};
    long[] ecx = {0x2L, 0x0L, 0x0L};
    long[] edx = {0x1L, 0x2L, 0x2L};
    long[] esi = {0x41067ca0L, 0x0L, 0x0L};
    long[] edi = {0x0, 0x080498ecL, 0x080498ecL};
    long[] ebp = {0xbfcfec68L, 0xb75603a8L, 0xb7f613a8L};
    long[] esp = {0xbfcfec20L, 0xb7560350L, 0xb7f61350L};
    long[] eip = {0x0804854aL, 0x0062a402L, 0x0062a402L};
    long[] eflags = {0x00210286L, 0x00200246L, 0x00200202L};
    long[] oeax = {0xffffffffL, 0xf0L, 0xf0L};
    long[] cs = {0x73L, 0x73L, 0x73L};
    long[] ds = {0x7bL, 0x7bL, 0x7bL};
    long[] es = {0x7b, 0x7b, 0x7b};
    long[] fs = {0x0L, 0x0L, 0x0L};
    long[] gs = {0x33L, 0x33L, 0x33L};

    // Loop through the expected three threads
    for (int i=0; i<threads.length; i++)
      {
	ElfPrstatus elfPrstatusInfo = threads[i];
	
	assertEquals("note: Sig Info -> Sig No",threadSigNo[i],elfPrstatusInfo.getPrInfoSiSigno());
	assertEquals("note: Sig Info -> Sig code",threadSigCode[i],elfPrstatusInfo.getPrInfoSiCode());
	assertEquals("note: Sig Info -> Sig errno",threadSigErrno[i],elfPrstatusInfo.getPrInfoSiErrno());
	assertEquals("note: Current signal",threadCurrentSig[i],elfPrstatusInfo.getPrCurSig());
	assertEquals("note: Pending signal",threadPendingSig[i],elfPrstatusInfo.getPrSigPending());
	assertEquals("note: Holding signal",threadHoldingSig[i],elfPrstatusInfo.getPrSigHold());
	assertEquals("note: Pid",threadPid[i],elfPrstatusInfo.getPrPid());
	assertEquals("note: PPid",threadPPid[i],elfPrstatusInfo.getPrPpid());
	assertEquals("note: Pgrp",threadPgrp[i],elfPrstatusInfo.getPrPgrp());
	assertEquals("note: Sid",threadSid[i],elfPrstatusInfo.getPrSid());
	
      
	// Get raw register buffer.
	byte[] rawRegisterBuffer = elfPrstatusInfo.getRawCoreRegisters();
	
	assertEquals("note: ebx",ebx[i],
		     getRegisterByOffset(rawRegisterBuffer,0,4,ByteOrder.LITTLE_ENDIAN));
	assertEquals("note: ecx",ecx[i],
		     getRegisterByOffset(rawRegisterBuffer,4,4,ByteOrder.LITTLE_ENDIAN));
	assertEquals("note: edx",edx[i], 
		     getRegisterByOffset(rawRegisterBuffer,8,4,ByteOrder.LITTLE_ENDIAN));
	
	assertEquals("note: esi",esi[i], 
		     getRegisterByOffset(rawRegisterBuffer,12,4,ByteOrder.LITTLE_ENDIAN));
	assertEquals("note: edi",edi[i],
		     getRegisterByOffset(rawRegisterBuffer,16,4,ByteOrder.LITTLE_ENDIAN));
	
	assertEquals("note: ebp",ebp[i],
		 getRegisterByOffset(rawRegisterBuffer,20,4,ByteOrder.LITTLE_ENDIAN));
	assertEquals("note: eax",eax[i],
		     getRegisterByOffset(rawRegisterBuffer,24,4,ByteOrder.LITTLE_ENDIAN));
	
	assertEquals("note: ds",ds[i],  
		     getRegisterByOffset(rawRegisterBuffer,28,2,ByteOrder.LITTLE_ENDIAN));
	assertEquals("note: es",es[i],
		     getRegisterByOffset(rawRegisterBuffer,32,2,ByteOrder.LITTLE_ENDIAN));
	assertEquals("note: fs",fs[i],
		     getRegisterByOffset(rawRegisterBuffer,36,2,ByteOrder.LITTLE_ENDIAN));
	assertEquals("note: gs",gs[i],
		     getRegisterByOffset(rawRegisterBuffer,40,2,ByteOrder.LITTLE_ENDIAN));
	assertEquals("note: oeax",oeax[i],
		     getRegisterByOffset(rawRegisterBuffer,44,4,ByteOrder.LITTLE_ENDIAN));
	assertEquals("note: eip",eip[i],
		     getRegisterByOffset(rawRegisterBuffer,48,4,ByteOrder.LITTLE_ENDIAN));
	assertEquals("note: cs",cs[i],
		     getRegisterByOffset(rawRegisterBuffer,52,4,ByteOrder.LITTLE_ENDIAN));
	assertEquals("note: eflags",eflags[i],
		     getRegisterByOffset(rawRegisterBuffer,56,4,ByteOrder.LITTLE_ENDIAN));
	
	assertEquals("note: esp",esp[i],
		     getRegisterByOffset(rawRegisterBuffer,60,4,ByteOrder.LITTLE_ENDIAN));
	
      }
  }

    /**
     * Test x8664 Prstatus note info. Read the note data segment, pass
     * it to ElfPrstatus to find the relative pstatus data, and parse.
     */
    public void testElfCorePrstatusNotes_x8664() {
	ByteOrder order = ByteOrder.LITTLE_ENDIAN;
	Elf testElf = new Elf (Prefix.pkgDataFile ("test-core-x8664"),
			       ElfCommand.ELF_C_READ);
    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfData noteData = findNoteSegment(testElf);

    ElfPrstatus threads[] =  ElfPrstatus.decode(noteData);
    ElfPrFPRegSet fpthreads [] = ElfPrFPRegSet.decode(noteData);
    
    assertEquals("Number of threads in notes",3,threads.length);
    // Should only be one thread in this core file.
    assertEquals("Number of  floating point thread data in notes ",3,fpthreads.length);

    // Preload 3 threads worth of data into the static data structure

    int[] threadSigNo = {11, 11, 11};
    int[] threadSigCode = {0, 0, 0};
    int[] threadSigErrno = {0, 0, 0};
    int[] threadCurrentSig = {11, 11, 11};
    int[] threadPendingSig = {0, 0, 0};
    int[] threadHoldingSig = {0, 0, 0};
    int[] threadPid = {3158, 3160, 3159};
    int[] threadPPid = {2966, 2966, 2966};
    int[] threadPgrp = {3158, 3158, 3158};
    int[] threadSid = {2966, 2966, 2966};
    long[] rax = {0x400808L, 0xfffffffffffffffcL, 0xfffffffffffffffcL};
    long[] rbx = {0x316c819bc0L, 0x600c00L, 0x600c00L};
    long[] rcx = {0x0L, 0xffffffffffffffffL, 0xffffffffffffffffL};
    long[] rdx = {0x2L, 0x2L, 0x2L};
    long[] rsi = {0x7fffc1cb9200L, 0x0L, 0x0L};
    long[] rdi = {0x0L, 0x600c00L, 0x600c00L};
    long[] rbp = {0x7fffc1cb9280L, 0xc58L, 0xc57L};
    long[] rsp = {0x7fffc1cb9240L, 0x41401048L, 0x40a00048L};
    long[] r8 =  {0x0L, 0x41401950L, 0x40a00950};
    long[] r9 =  {0x00007fffc1cb9060L, 0x41401950L, 0x40a00950L};
    long[] r10 = {0x8L, 0x0L, 0x0L};
    long[] r11 = {0x202L, 0x246L, 0x202L};
    long[] r12 = {0x0L, 0x0L, 0x0L};
    long[] r13 = {0x7fffc1cb9360L, 0x41401260L, 0x40a00260L};
    long[] r14 = {0x0L, 0x41402000L, 0x40a01000L};
    long[] r15 = {0x0L, 0x0L, 0x0L};
    long[] rip = {0x400676L, 0x3173c0c728L, 0x3173c0c728L};
    long[] eflags = {0x10202L,0x246L, 0x202};
    long[] orax = {0xffffffffffffffffL, 0xcaL, 0xcaL};
    long[] cs =   {0x33L,0x33L, 0x0033L};
    long[] ds =   {0x0L, 0x0L, 0x0L};
    long[] es = {0x0L, 0x0L, 0x0L};
    long[] ss = {0x2bL, 0x2bL, 0x2bL};
    long[] fs = {0x0L, 0x0L, 0x0L};
    long[] fs_base = {0x2aaaaaacb8b0L,0x2aaaaaacb8b0L,0x2aaaaaacb8b0L};
    long[] gs = {0x0L, 0x0L, 0x0L};
    long[] gs_base = {0x0L, 0x0L, 0x0L};

    // Loop through the expected three threads
    for (int i=0; i<threads.length; i++)
      {
	ElfPrstatus elfPrstatusInfo = threads[i];
	

	assertEquals("note: Sig Info -> Sig No",threadSigNo[i],elfPrstatusInfo.getPrInfoSiSigno());
	assertEquals("note: Sig Info -> Sig code",threadSigCode[i],elfPrstatusInfo.getPrInfoSiCode());
	assertEquals("note: Sig Info -> Sig errno",threadSigErrno[i],elfPrstatusInfo.getPrInfoSiErrno());
	assertEquals("note: Current signal",threadCurrentSig[i],elfPrstatusInfo.getPrCurSig());
	assertEquals("note: Pending signal",threadPendingSig[i],elfPrstatusInfo.getPrSigPending());
	assertEquals("note: Holding signal",threadHoldingSig[i],elfPrstatusInfo.getPrSigHold());
	assertEquals("note: Pid",threadPid[i],elfPrstatusInfo.getPrPid());
	assertEquals("note: PPid",threadPPid[i],elfPrstatusInfo.getPrPpid());
	assertEquals("note: Pgrp",threadPgrp[i],elfPrstatusInfo.getPrPgrp());
	assertEquals("note: Sid",threadSid[i],elfPrstatusInfo.getPrSid());
	
      
	// Get raw register buffer.
	byte[] rawRegisterBuffer = elfPrstatusInfo.getRawCoreRegisters();
	
	assertEquals("note: rax",rax[i],
		     getRegisterByOffset(rawRegisterBuffer,80,8,order));
	assertEquals("note: rbx",rbx[i],
		     getRegisterByOffset(rawRegisterBuffer,40,8,order));
	assertEquals("note: rcx",rcx[i], 
		     getRegisterByOffset(rawRegisterBuffer,88,8,order));
	assertEquals("note: rdx",rdx[i], 
		     getRegisterByOffset(rawRegisterBuffer,96,8,order));
	assertEquals("note: rsi",rsi[i],
		     getRegisterByOffset(rawRegisterBuffer,104,8,order));
	assertEquals("note: rdi",rdi[i],
		     getRegisterByOffset(rawRegisterBuffer,112,8,order));
	assertEquals("note: rbp",rbp[i],
		     getRegisterByOffset(rawRegisterBuffer,32,8,order));
	assertEquals("note: rsp",rsp[i],  
	     getRegisterByOffset(rawRegisterBuffer,152,8,order));
	assertEquals("note: r8",r8[i],
		     getRegisterByOffset(rawRegisterBuffer,72,8,order));
	assertEquals("note: r9",r9[i],
		     getRegisterByOffset(rawRegisterBuffer,64,8,order));
	assertEquals("note: r10",r10[i],
		     getRegisterByOffset(rawRegisterBuffer,56,8,order));
	assertEquals("note: r11",r11[i],
		     getRegisterByOffset(rawRegisterBuffer,48,8,order));
	assertEquals("note: r12",r12[i],
		     getRegisterByOffset(rawRegisterBuffer,24,8,order));
	assertEquals("note: r13",r13[i],
		     getRegisterByOffset(rawRegisterBuffer,16,8,order));
	assertEquals("note: r14",r14[i],
		     getRegisterByOffset(rawRegisterBuffer,8,8,order));
	assertEquals("note: r15",r15[i],
		     getRegisterByOffset(rawRegisterBuffer,0,8,order));
	assertEquals("note: rip",rip[i],
		     getRegisterByOffset(rawRegisterBuffer,128,8,order));
	assertEquals("note: eflags",eflags[i],
		     getRegisterByOffset(rawRegisterBuffer,144,8,order));
	assertEquals("note: cs",cs[i],
		     getRegisterByOffset(rawRegisterBuffer,136,4,order));
	assertEquals("note: ss",ss[i],
		     getRegisterByOffset(rawRegisterBuffer,160,4,order));
	assertEquals("note: ds",ds[i],
		     getRegisterByOffset(rawRegisterBuffer,184,4,order));
	assertEquals("note: es",es[i],
		     getRegisterByOffset(rawRegisterBuffer,192,4,order));
	assertEquals("note: fs",fs[i],
		     getRegisterByOffset(rawRegisterBuffer,200,4,order));
	assertEquals("note: gs",gs[i],
		     getRegisterByOffset(rawRegisterBuffer,208,4,order));
	assertEquals("note: orig_rax",orax[i],
		     getRegisterByOffset(rawRegisterBuffer,120,8,order));
	assertEquals("note: fs_base",fs_base[i],
		     getRegisterByOffset(rawRegisterBuffer,168,8,order));
	assertEquals("note: gs_base",gs_base[i],
		     getRegisterByOffset(rawRegisterBuffer,176,8,order));

	
      }
  }

    /**
     * Test 64 bit PrAuxv note info. Read the note data segment, pass
     * it to ElfPrAuxv to find the relative pstatus data, and parse.
     */
    public void testElfCorePrAuxvNotes_x8664() {
	Elf testElf = new Elf (Prefix.pkgDataFile ("test-core-x8664"),
			       ElfCommand.ELF_C_READ);
    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfData noteData = findNoteSegment(testElf);

    final ElfPrAuxv prAuxv = ElfPrAuxv.decode(noteData);

    final int[] expectedIndex = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
    final int[] expectedType = {16,6,17,3,4,5,7,8,9,11,12,13,14,23,15,0};
    final long[] expectedVal = {0xbfebfbffL,
				0x1000L,
				0x64L,
				0x400040L,
				0x38L,
				0x8L,
				0x0L,
				0x0L,
				0x400510L,
				0x1f4L,
				0x1f4L,
				0x1f4L,
				0x1f4L,
				0x0L,
				0x7fffc1cb95d9L,
				0x0L
    };
    

				

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
	assertEquals("AuxV Val "+auxvIndex,expectedVal[auxvIndex],val);
	auxvIndex++;
      }
    };
    builder.construct(prAuxv.getAuxvBuffer());

  }

    /**
     * Test 32 bit PrAuxv note info. Read the note data segment, pass
     * it to ElfPrAuxv to find the relative pstatus data, and parse.
     */
    public void testElfCorePrAuxvNotes_x86() {
	Elf testElf = new Elf (Prefix.pkgDataFile ("test-core-x86"),
			       ElfCommand.ELF_C_READ);
    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfData noteData = findNoteSegment(testElf);

    final ElfPrAuxv prAuxv = ElfPrAuxv.decode(noteData);

    final int[] expectedIndex = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17};
    final int[] expectedType = {32,33,16,6,17,3,4,5,7,8,9,11,12,13,14,23,15,0};
    final long[] expectedVal = {0x62a400L,
				 0x62a000L,
				 0xafe9f1bfL,
				 0x1000L,
				 0x64L,
				 0x8048034L,
				 0x20L,
				 0x8L,
				 0x0L,
				 0x0L,
				 0x80483e0L,
				 0x1f4L,
				 0x1f4L,
				 0x1f4L,
				 0x1f4L,
				 0x0L,
				 0xbfcfee4bL,
				 0x0
    };
    

				

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
	assertEquals("AuxV Val "+auxvIndex,expectedVal[auxvIndex],val);
	auxvIndex++;
      }
    };
    builder.construct(prAuxv.getAuxvBuffer());

  }


  public void testXFPRegSet () throws ElfException, ElfFileException
  {

    int[] expectedXFP = {0x7f, 0x03, 0x20, 0x00,0x00, 0x00, 0x5d,
			 0x05, 0xd9, 0x84, 0x04,0x08, 0x73, 0x00,
			 0x00, 0x00, 0x48, 0xec, 0xcf, 0xbf, 0x7b, 
			 00, 00, 00, 0x80, 0x1f, 00, 00, 0xff, 0xff,
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 0xc0, 0xca, 0xa1,0x45, 0xb6, 0xf3, 0x9d,
			 0xff, 0x3f, 00, 00, 00, 00, 00, 00, 00, 0xa3,
			 0x9b, 0xc4, 0x20, 0xb0, 0x72, 0xca, 0x05, 0x40, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,
			 00, 00, 00, 00, 00, 00, 0x05, 00, 00, 00,
			 0x90, 00, 00, 00, 0x01, 00, 00, 00, 0x43,
			 0x4f, 0x52, 0x45, 00, 00, 00, 00, 0x0b, 00,
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 0x0b,
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,
			 0xb1, 0x68, 00, 00, 0x2b, 0x0b, 00, 00, 0xaf,
			 0x68, 00, 00, 0x2b, 0x0b, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 
			 00, 00, 00, 00, 00, 00} ;

    Elf testElf = new Elf(Prefix.pkgDataFile ("test-core-x86"),
			  ElfCommand.ELF_C_READ);
    assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
    assertEquals(testElf.getBase(), 0);

    ElfData noteData = findNoteSegment(testElf);

    ElfPrXFPRegSet xfpthreads [] = ElfPrXFPRegSet.decode(noteData);
    
    // Should only be one thread in this core file.
    assertEquals("Number of  floating point thread data in notes",3,xfpthreads.length);
    byte[] raw = new byte[500];
    raw = xfpthreads[0].getXFPRegisterBuffer();
    for (int i=0; i< 500; i++)
      {
	assertEquals("Expected byte xfp " + i,(byte)raw[i],(byte)expectedXFP[i]);
      }

  }

    public void testObjectFile() {
	Elf testElf = new Elf(Prefix.pkgDataFile ("helloworld.o"),
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
    int[] expectedTypes = { ElfSectionHeader.ELF_SHT_NULL, ElfSectionHeader.ELF_SHT_PROGBITS,
			    ElfSectionHeader.ELF_SHT_REL, ElfSectionHeader.ELF_SHT_PROGBITS,
			    ElfSectionHeader.ELF_SHT_NOBITS, ElfSectionHeader.ELF_SHT_PROGBITS,
			    ElfSectionHeader.ELF_SHT_PROGBITS, ElfSectionHeader.ELF_SHT_PROGBITS,
			    ElfSectionHeader.ELF_SHT_STRTAB, ElfSectionHeader.ELF_SHT_SYMTAB,
			    ElfSectionHeader.ELF_SHT_STRTAB };
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

    public void testObjectFileTables() {
	Elf testElf = new Elf(Prefix.pkgDataFile ("helloworld.o"),
			      ElfCommand.ELF_C_READ);

    ElfEHeader header = testElf.getEHeader();
    for (int i = 0; i < header.shnum; i++)
      {
        ElfSection section = testElf.getSection(i);
        ElfSectionHeader sheader = section.getSectionHeader();
	if (sheader.type == ElfSectionHeader.ELF_SHT_SYMTAB)
	  {
	    new ElfSymbol.Loader(section).loadAll(symbolChecker);
	    symbolChecker.postCheck();
	  }
	else if (sheader.type == ElfSectionHeader.ELF_SHT_REL)
	  {
	    ElfRel[] relocs = ElfRel.loadFrom(section);
	    assertEquals("relocation count", 2, relocs.length);
	    final long[] expectedRelOffsets = {0x00000014, 0x00000019};
	    final int[] expectedRelTypes = {1/*386_32*/, 2/*386_PC32*/};
	    final long[] expectedRelSymbols = {5/*.rodata*/, 9/*prinf*/};

	    for (int j = 0; j < relocs.length; ++j)
	      {
		assertEquals("relocation-" + j + "-offset", expectedRelOffsets[j], relocs[j].offset);
		assertEquals("relocation-" + j + "-type", expectedRelTypes[j], relocs[j].type);
		assertEquals("relocation-" + j + "-symbol", expectedRelSymbols[j], relocs[j].symbolIndex);
		assertEquals("relocation-" + j + "-addend", 0, relocs[j].addend);
	      }
	  }
      }
  }

  private static ElfSection getElfSectionWithAddr(Elf elfFile, long addr)
  {
    for (ElfSection section = elfFile.getSection(0);
	 section != null;
	 section = elfFile.getNextSection(section))
      {
	ElfSectionHeader sheader = section.getSectionHeader();
	if (sheader.addr == addr)
	  return section;
      }
    return null;
  }

    public void testLibraryVersions() {
	final Elf elfFile = new Elf(Prefix.pkgDataFile("libtest.so"),
				    ElfCommand.ELF_C_READ);
    final ElfEHeader eh = elfFile.getEHeader();

    boolean haveDynamic = false;
    long offDynamic = 0;
    for (int i = 0; i < eh.phnum; ++i)
      {
	ElfPHeader ph = elfFile.getPHeader(i);
	if (ph.type == ElfPHeader.PTYPE_DYNAMIC)
	  {
	    haveDynamic = true;
	    offDynamic = ph.offset;
	  }
      }

    assertTrue("DYNAMIC section found in ELF file", haveDynamic);

    haveDynamic = false;

    class Locals
    {
      public ElfSection dynamicStrtab = null;
      public ElfSection dynamicSymtab = null;

      public ElfSection dynamicVersym = null;
      public ElfSection dynamicVerdef = null;
      public ElfSection dynamicVerneed = null;
      public int dynamicVerdefCount = 0;
      public int dynamicVerneedCount = 0;

      public int dynamicSonameIdx = -1;
    }
    final Locals locals = new Locals();
    final int[] expectedDynamicTags = {
         ElfDynamic.ELF_DT_NEEDED,
	 ElfDynamic.ELF_DT_SONAME,
	 ElfDynamic.ELF_DT_INIT,
	 ElfDynamic.ELF_DT_FINI,
	 ElfDynamic.ELF_DT_GNU_HASH,
	 ElfDynamic.ELF_DT_STRTAB,
	 ElfDynamic.ELF_DT_SYMTAB,
	 ElfDynamic.ELF_DT_STRSZ,
	 ElfDynamic.ELF_DT_SYMENT,
	 ElfDynamic.ELF_DT_PLTGOT,
	 ElfDynamic.ELF_DT_PLTRELSZ,
	 ElfDynamic.ELF_DT_PLTREL,
	 ElfDynamic.ELF_DT_JMPREL,
	 ElfDynamic.ELF_DT_REL,
	 ElfDynamic.ELF_DT_RELSZ,
	 ElfDynamic.ELF_DT_RELENT,
	 ElfDynamic.ELF_DT_VERDEF,
	 ElfDynamic.ELF_DT_VERDEFNUM,
	 ElfDynamic.ELF_DT_VERNEED,
	 ElfDynamic.ELF_DT_VERNEEDNUM,
	 ElfDynamic.ELF_DT_VERSYM,
	 ElfDynamic.ELF_DT_RELCOUNT,
	 ElfDynamic.ELF_DT_NULL,
	 ElfDynamic.ELF_DT_NULL,
	 ElfDynamic.ELF_DT_NULL,
	 ElfDynamic.ELF_DT_NULL,
	 ElfDynamic.ELF_DT_NULL
       };

    for (ElfSection section = elfFile.getSection(0);
	 section != null;
	 section = elfFile.getNextSection(section))
      {
	ElfSectionHeader sheader = section.getSectionHeader();
	if (sheader.offset == offDynamic)
	  {
	    haveDynamic = true;
	    ElfDynamic.loadFrom(section, new ElfDynamic.Builder() {
	        private int entryIndex = 0;
		public void entry (int tag, long value)
		{
		  assertEquals("Dynamic entry #" + entryIndex,
			       tag, expectedDynamicTags[entryIndex]);
		  entryIndex++;
		  if (tag == ElfDynamic.ELF_DT_STRTAB)
		    locals.dynamicStrtab = getElfSectionWithAddr(elfFile, value);
		  else if (tag == ElfDynamic.ELF_DT_SONAME)
		    locals.dynamicSonameIdx = (int)value;
		  else if (tag == ElfDynamic.ELF_DT_SYMTAB)
		    locals.dynamicSymtab = getElfSectionWithAddr(elfFile, value);
		  else if (tag == ElfDynamic.ELF_DT_VERSYM)
		    locals.dynamicVersym = getElfSectionWithAddr(elfFile, value);
		  else if (tag == ElfDynamic.ELF_DT_VERDEF)
		    locals.dynamicVerdef = getElfSectionWithAddr(elfFile, value);
		  else if (tag == ElfDynamic.ELF_DT_VERDEFNUM)
		    locals.dynamicVerdefCount = (int)value;
		  else if (tag == ElfDynamic.ELF_DT_VERNEED)
		    locals.dynamicVerneed = getElfSectionWithAddr(elfFile, value);
		  else if (tag == ElfDynamic.ELF_DT_VERNEEDNUM)
		    locals.dynamicVerneedCount = (int)value;
		}
	    });
	  }
      }

    assertTrue("DYNAMIC section found in ELF file", haveDynamic);
    assertNotNull("Couldn't get SYMTAB from DYNAMIC section",
		  locals.dynamicSymtab);
    assertNotNull("Couldn't get STRTAB from DYNAMIC section",
		  locals.dynamicStrtab);
    assertTrue("Versym section present when verdef or verneed present",
	       ((locals.dynamicVerneed != null || locals.dynamicVerdef != null)
		&& locals.dynamicVersym != null));
    assertTrue("VERDEFNUM tag present with VERDEF",
	       locals.dynamicVerdefCount != 0 && locals.dynamicVerdef != null);
    assertTrue("VERNEEDNUM tag present with VERNEED",
	       locals.dynamicVerneedCount != 0
	       && locals.dynamicVerneed != null);
    assertTrue("Soname found", locals.dynamicSonameIdx != -1);

    // Check SONAME.
    {
      ElfData data = locals.dynamicStrtab.getData();
      byte[] bytes = data.getBytes();
      int startIndex = locals.dynamicSonameIdx;
      int endIndex = startIndex;
      while (bytes[endIndex] != 0)
	++endIndex;
      String soname = new String(bytes, startIndex, endIndex - startIndex);
      assertEquals("soname", "libtest.so.1", soname);
    }

    // Check DT_SYMTAB.
    {
      final String[] expectedSymNames = {"__gmon_start__", "_Jv_RegisterClasses",
					 "puts",           "__cxa_finalize",
					 "oldtest",        "test",
					 "test",           "TESTVER_1.0",
					 "TESTVER_2.0"};
      final long[] expectedSymValues = {0x00000000, 0x00000000, 0x00000000, 0x00000000,
					0x00000478, 0x0000042c, 0x00000452, 0x00000000,
					0x00000000};
      final long[] expectedSymSizes = {0, 0, 399, 373, 29,  38, 38,  0, 0};
      final ElfSymbolType[] expectedSymTypes = {
	ElfSymbolType.ELF_STT_NOTYPE, ElfSymbolType.ELF_STT_NOTYPE,
	ElfSymbolType.ELF_STT_FUNC, ElfSymbolType.ELF_STT_FUNC,
	ElfSymbolType.ELF_STT_FUNC, ElfSymbolType.ELF_STT_FUNC,
	ElfSymbolType.ELF_STT_FUNC, ElfSymbolType.ELF_STT_OBJECT,
	ElfSymbolType.ELF_STT_OBJECT};
      final ElfSymbolBinding[] expectedSymBinds = {
	ElfSymbolBinding.ELF_STB_WEAK, ElfSymbolBinding.ELF_STB_WEAK,
	ElfSymbolBinding.ELF_STB_GLOBAL, ElfSymbolBinding.ELF_STB_WEAK,
	ElfSymbolBinding.ELF_STB_GLOBAL, ElfSymbolBinding.ELF_STB_GLOBAL,
	ElfSymbolBinding.ELF_STB_GLOBAL, ElfSymbolBinding.ELF_STB_GLOBAL,
	ElfSymbolBinding.ELF_STB_GLOBAL};
      final long[] expectedSymShndxs = {
	ElfSectionHeader.ELF_SHN_UNDEF, ElfSectionHeader.ELF_SHN_UNDEF,
	ElfSectionHeader.ELF_SHN_UNDEF, ElfSectionHeader.ELF_SHN_UNDEF,
	11,                             11,
	11,                             ElfSectionHeader.ELF_SHN_ABS,
	ElfSectionHeader.ELF_SHN_ABS};
      final String[][] expectedSymVersions = {
	null,          null,
	{"GLIBC_2.0"},                  {"GLIBC_2.1.3"},
	{"TESTVER_2.0", "TESTVER_1.0"}, {"TESTVER_1.0"},
	{"TESTVER_2.0", "TESTVER_1.0"}, {"TESTVER_1.0"},
	{"TESTVER_2.0", "TESTVER_1.0"}};
      final boolean[] expectedVerRequired = {
	false, false, true, true, false, false, false, false, false
      };

      new ElfSymbol.Loader(locals.dynamicSymtab, locals.dynamicVersym,
			   locals.dynamicVerdef, locals.dynamicVerdefCount,
			   locals.dynamicVerneed, locals.dynamicVerneedCount)
	.loadAll(new ElfSymbol.Builder() {
	  private int counter = 0;
	  public void symbol (long index, String name,
			      long value, long size,
			      ElfSymbolType type, ElfSymbolBinding bind, ElfSymbolVisibility visibility,
			      long shndx, List versions)
	  {
	    assertEquals("symbol-" + counter + "-name", expectedSymNames[counter], name);
	    assertEquals("symbol-" + counter + "-value", expectedSymValues[counter], value);
	    assertEquals("symbol-" + counter + "-size", expectedSymSizes[counter], size);
	    assertEquals("symbol-" + counter + "-type", expectedSymTypes[counter], type);
	    assertEquals("symbol-" + counter + "-bind", expectedSymBinds[counter], bind);
	    assertEquals("symbol-" + counter + "-visibility", ElfSymbolVisibility.ELF_STV_DEFAULT, visibility);
	    assertEquals("symbol-" + counter + "-shndx", expectedSymShndxs[counter], shndx);

	    String[] expVersions = expectedSymVersions[counter];
	    if (expVersions == null)
	      assertEquals("symbol-" + counter + "-version#1", expVersions, versions);
	    else
	      {
		HashSet expVersionSet = new HashSet();
		for (int i = 0; i < expVersions.length; ++i)
		  expVersionSet.add(expVersions[i]);

		HashSet haveVersionSet = new HashSet();
		for (Iterator it = versions.iterator(); it.hasNext();)
		  {
		    ElfSymbolVersion ver = (ElfSymbolVersion)it.next();
		    haveVersionSet.add(ver.name);
		    ver.visit(new ElfSymbolVersion.Visitor() {
			public Object def(ElfSymbolVersion.Def verdef) {
			  assertEquals("symbol-" + counter + "-version-type",
				       expectedVerRequired[counter], false);
			  return null;
			}
			public Object need(ElfSymbolVersion.Need verneed) {
			  assertEquals("symbol-" + counter + "-version-type",
				       expectedVerRequired[counter], true);
			  return null;
			}
		      });
		  }

		assertTrue("symbol-" + counter + "-version#2", expVersionSet.equals(haveVersionSet));
	      }

	    counter++;
	  }
	});
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
    ArrayByteBuffer regBuffer = new ArrayByteBuffer(buffer);
    regBuffer.order(endian);
    regBuffer.wordSize(length);
    long val = 0;
    byte[] regBytes = new byte[length];
    
    // Get the bytes at the offset and length, and 
    // reverse if necessary.
    
    regBuffer.position(offset);
    regBuffer.get(regBytes, 0,length);
    if (regBuffer.order() == ByteOrder.LITTLE_ENDIAN)
      reverseArray(regBytes);

    //Convert byte array to long value.
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

  class SymbolChecker implements ElfSymbol.Builder {
    String[] expectedSymbolNames
      = {null,
	 "helloworld.c", "", "",
	 "", "", "",
	 "", "main", "prinf"};
    ElfSymbolType[] expectedSymbolTypes
      = {null,
	 ElfSymbolType.ELF_STT_FILE,    ElfSymbolType.ELF_STT_SECTION, ElfSymbolType.ELF_STT_SECTION,
	 ElfSymbolType.ELF_STT_SECTION, ElfSymbolType.ELF_STT_SECTION, ElfSymbolType.ELF_STT_SECTION,
	 ElfSymbolType.ELF_STT_SECTION, ElfSymbolType.ELF_STT_FUNC,    ElfSymbolType.ELF_STT_NOTYPE};
    ElfSymbolBinding[] expectedSymbolBinds
      = {null,
	 ElfSymbolBinding.ELF_STB_LOCAL, ElfSymbolBinding.ELF_STB_LOCAL,  ElfSymbolBinding.ELF_STB_LOCAL,
	 ElfSymbolBinding.ELF_STB_LOCAL, ElfSymbolBinding.ELF_STB_LOCAL,  ElfSymbolBinding.ELF_STB_LOCAL,
	 ElfSymbolBinding.ELF_STB_LOCAL, ElfSymbolBinding.ELF_STB_GLOBAL, ElfSymbolBinding.ELF_STB_GLOBAL};
    long[] expectedSymbolShndxs
      = {-1,
	 ElfSectionHeader.ELF_SHN_ABS, 1, 3,
	 4, 5, 7,
	 6, 1, ElfSectionHeader.ELF_SHN_UNDEF};
    long[] expectedSymbolSizes
      = {-1,
	 0, 0,  0,
	 0, 0,  0,
	 0, 43, 0};

    int counter = 1; // Special first entry is skipped.
    public void symbol (long idx, String name,
			long value, long size,
			ElfSymbolType type, ElfSymbolBinding bind, ElfSymbolVisibility visibility,
			long shndx, List versions)
    {
      int index = (int)idx; // Java can't index arrays with long.  It
			    // doesn't matter here though...
      assertTrue("symbol table length", index < expectedSymbolNames.length);

      assertEquals("symbol-" + index + "-name", expectedSymbolNames[index], name);
      assertEquals("symbol-" + index + "-value", 0, value);
      assertEquals("symbol-" + index + "-size", 0, expectedSymbolSizes[index], size);
      assertEquals("symbol-" + index + "-type", expectedSymbolTypes[index], type);
      assertEquals("symbol-" + index + "-binding", expectedSymbolBinds[index], bind);
      assertEquals("symbol-" + index + "-visibility", ElfSymbolVisibility.ELF_STV_DEFAULT, visibility);
      assertEquals("symbol-" + index + "-shndx", expectedSymbolShndxs[index], shndx);

      counter++;
    }

    public void postCheck()
    {
      assertEquals("symbol table length", expectedSymbolNames.length, counter);
    }
  }
  private SymbolChecker symbolChecker = new SymbolChecker();

    /**
     * Verify write/read of word-size is sane; that 32- and 64-bit
     * word sizes are correct decoded is checked in the core-file
     * tests above.
     */
    public void testSetWordSize() {
	ElfEHeader header = new ElfEHeader();
	assertEquals("word size", 0, header.getWordSize());
	header.setWordSize(4);
	assertEquals("word size", 4, header.getWordSize());
	header.setWordSize(8);
	assertEquals("word size", 8, header.getWordSize());
	header.setWordSize(0);
	assertEquals("word size", 0, header.getWordSize());
    }

    /**
     * Verify write/read of word-size is sane; that 32- and 64-bit
     * word sizes are correct decoded is checked in the core-file
     * tests above.
     */
    public void testSetByteOrder() {
	ElfEHeader header = new ElfEHeader();
	assertEquals("word size", null, header.getByteOrder());
	header.setByteOrder(ByteOrder.BIG_ENDIAN);
	assertEquals("word size", ByteOrder.BIG_ENDIAN, header.getByteOrder());
	header.setByteOrder(ByteOrder.LITTLE_ENDIAN);
	assertEquals("word size", ByteOrder.LITTLE_ENDIAN,
		     header.getByteOrder());
	header.setByteOrder(null);
	assertEquals("word size", null, header.getByteOrder());
    }
}
