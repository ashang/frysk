// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

import junit.framework.TestCase;
import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.elf.ElfData;
import lib.elf.ElfEHeader;
import lib.elf.ElfKind;
import lib.elf.ElfPHeader;
import lib.elf.ElfSection;
import lib.elf.ElfSectionHeader;
import lib.elf.ElfType;
import lib.elf.ElfException;
import lib.elf.ElfFileException;

import frysk.imports.Build;

public class TestElf extends TestCase {
	
	public void testCore()
	throws ElfException, ElfFileException {
		Elf testElf = new Elf(Build.ABS_BUILDDIR+"/lib/elf/tests/test-core", ElfCommand.ELF_C_READ);
		
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
		
		int[] pheaderFlags = {0, 5, 5, 4, 6, 5, 4, 6, 6, 5, 6, 6, 6, 6};
		int[] pheaderOffsets = {500, 4096, 8192, 8192, 12288, 16384, 16384, 24576,
				28672, 40960, 40960, 45056, 49152, 53248};
		int[] pheaderSegSizeFile = {472, 4096, 0, 4096, 4096, 0, 8192, 4096, 12288, 0,
				4096, 4096, 4096, 90112};
		int[] pheaderSegSizeMem = {0, 4096, 102400, 4096, 4096, 1232896, 8192, 4096, 
				12288, 4096, 4096, 4096, 4096, 90112};
		BigInteger[] pheaderAddr = {new BigInteger("0", 10), new BigInteger("1507328", 10),
				new BigInteger("1511424", 10), new BigInteger("1613824", 10),
				new BigInteger("1617920", 10), new BigInteger("10477568", 10), 
				new BigInteger("11710464", 10), new BigInteger("11718656", 10),
				new BigInteger("11722752", 10), new BigInteger("134512640", 10),
				new BigInteger("134516736", 10), new BigInteger("3085901824", 10),
				new BigInteger("3085983744", 10), new BigInteger("3220111360", 10)};
		
		for(int i = 0; i < count; i++){
			ElfPHeader pheader = testElf.getPHeader(i);
			assertNotNull(pheader);
			
			if(i == 0)
				assertEquals(0, pheader.align);
			else
				assertEquals(4096, pheader.align);
			assertEquals(pheaderFlags[i], pheader.flags);
			assertEquals(pheaderOffsets[i], pheader.offset);
			assertEquals(0, pheader.paddr);
			assertEquals(pheaderSegSizeFile[i], pheader.filesz);
			assertEquals(pheaderSegSizeMem[i], pheader.memsz);
			if(i == 0)
				assertEquals(4, pheader.type);
			else
				assertEquals(1, pheader.type);
			assertEquals(pheaderAddr[i], new BigInteger(""+pheader.vaddr, 10));
		}
	}
	
	public void testObjectFile()
	throws ElfException, ElfFileException {
		Elf testElf = new Elf(Build.ABS_BUILDDIR+"/lib/elf/tests/helloworld.o", ElfCommand.ELF_C_READ);
		
		assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
		assertEquals(testElf.getBase(), 0);
		
		ElfEHeader header = testElf.getEHeader();
		assertEquals(3, header.machine);
		assertEquals(52, header.ehsize);
		assertEquals(0, header.entry);
		assertEquals(1, header.version);
		assertEquals(0, header.flags);
		assertEquals(1, header.type);
		assertEquals(11, header.shnum);
		assertEquals(40, header.shentsize);
		assertEquals(236, header.shoff);
		assertEquals(0, header.phnum);
		assertEquals(0, header.phentsize);
		assertEquals(0, header.phoff);
		
		int[] expectedIndices = {0, 52, 864, 96, 96, 96, 110, 155, 155, 676, 836};
		int[] expectedInfo = {0, 0, 1, 0, 0, 0, 0, 0, 0, 8, 0};
		int[] expectedAlign = {0, 4, 4, 4, 4, 1, 1, 1, 1, 4, 1};
		int[] expectedEntrySize = {0, 0, 8, 0, 0, 0, 0, 0, 0, 16, 0};
		int[] expectedFlags = {0, 6, 0, 3, 3, 2, 0, 0, 0, 0, 0};
		int[] expectedNameIndeces = {0, 31, 27, 37, 43, 48, 56, 65, 17, 1, 9};
		int[] expectedSize = {0, 43, 16, 0, 0, 14, 45, 0, 81, 160, 25};
		int[] expectedTypes = {0, 1, 9, 1, 8, 1, 1, 1, 3, 2, 3};
		
		int[] expectedDataSizes = {0, 43, 16, 0, 0, 14, 45, 0, 81, 160, 25};
		ElfType[] expectedDataTypes = {ElfType.ELF_T_BYTE, ElfType.ELF_T_BYTE, 
				ElfType.ELF_T_REL, ElfType.ELF_T_BYTE, ElfType.ELF_T_BYTE,
				ElfType.ELF_T_BYTE, ElfType.ELF_T_BYTE, ElfType.ELF_T_BYTE,
				ElfType.ELF_T_BYTE, ElfType.ELF_T_SYM, ElfType.ELF_T_BYTE
		};
		int[] expectedBytes = {0, -115, 20, 0, 0, 72, 0, 0, 0, 0, 0};
		
		for(int i = 0; i < header.shnum; i++){
			ElfSection section = testElf.getSection(i);
			assertNotNull(section);
			assertEquals(section.getIndex(), i);

			ElfSectionHeader sheader = section.getSectionHeader();
			assertNotNull(sheader);

			assertEquals(0, sheader.addr);
			assertEquals(expectedIndices[i], sheader.offset);
			assertEquals(expectedInfo[i], sheader.info);
			assertEquals(expectedAlign[i], sheader.addralign);
			assertEquals(expectedEntrySize[i], sheader.entsize);
			assertEquals(expectedFlags[i], sheader.flags);
			assertEquals(expectedNameIndeces[i], sheader.name);
			assertEquals(expectedSize[i], sheader.size);
			assertEquals(expectedTypes[i], sheader.type);
			
			ElfData data = section.getData();
			assertNotNull(data);
			assertEquals(0, data.getAlignment());
			assertEquals(0, data.getOffset());
			assertEquals(expectedDataSizes[i], data.getSize());
			assertEquals(expectedDataTypes[i], data.getType());
			if(data.getSize() != 0)
				assertEquals(expectedBytes[i], data.getByte(0));
		}
	}
	
}
