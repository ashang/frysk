package lib.elf.tests;

import java.io.File;

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

public class TestElf extends TestCase {

	public void testCore(){
		File f = new File((String) null, "tmp");
		String dir = f.getAbsolutePath();

		Elf testElf = new Elf(dir.substring(0, dir.length() - 4)+"/lib/elf/tests/test-core", ElfCommand.ELF_C_READ);
		
		assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
		assertEquals(testElf.getBase(), 0);
		
		ElfEHeader header = testElf.getEHeader();
		assertEquals(3, header.getArchitecture());
		assertEquals(1, header.getDataEncoding());
		assertEquals(52, header.getELFHeaderSize());
		assertEquals(0, header.getEntryPoint());
		assertEquals(1, header.getFileClass());
		assertEquals(1, header.getFileVersion());
		assertEquals(0, header.getFlags());
		assertEquals(4, header.getType());
		assertEquals(0, header.getSectionHeaderEntryCount());
		
		for(int i = 0; i < header.getSectionHeaderEntryCount(); i++){
			ElfSection section = testElf.getSection(i);
			assertNotNull(section);
			assertEquals(section.getIndex(), i);

			ElfSectionHeader sheader = section.getSectionHeader();
			assertNotNull(sheader);

			System.err.println("    Header\n    ========");
			System.err.println("      Address: "+sheader.getAddress());
			System.err.println("      Index: "+sheader.getOffset());
			System.err.println("      Offset: "+sheader.getOffset());
		}
		
	}
	
	public void testOFile(){
		File f = new File("lib/elf/tests/helloworld.o");
		Elf testElf = new Elf(f.getAbsolutePath(), ElfCommand.ELF_C_READ);
		
		assertEquals(testElf.getKind(), ElfKind.ELF_K_ELF);
		assertEquals(testElf.getBase(), 0);
		
		ElfEHeader header = testElf.getEHeader();
		assertEquals(3, header.getArchitecture());
		assertEquals(1, header.getDataEncoding());
		assertEquals(52, header.getELFHeaderSize());
		assertEquals(0, header.getEntryPoint());
		assertEquals(1, header.getFileClass());
		assertEquals(1, header.getFileVersion());
		assertEquals(0, header.getFlags());
		assertEquals(1, header.getType());
		assertEquals(11, header.getSectionHeaderEntryCount());
		
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
		
		for(int i = 0; i < header.getSectionHeaderEntryCount(); i++){
			ElfSection section = testElf.getSection(i);
			assertNotNull(section);
			assertEquals(section.getIndex(), i);

			ElfSectionHeader sheader = section.getSectionHeader();
			assertNotNull(sheader);

			assertEquals(0, sheader.getAddress());
			assertEquals(expectedIndices[i], sheader.getOffset());
			assertEquals(expectedInfo[i], sheader.getAdditionalInfo());
			assertEquals(expectedAlign[i], sheader.getAlignment());
			assertEquals(expectedEntrySize[i], sheader.getEntrySize());
			assertEquals(expectedFlags[i], sheader.getFlags());
			assertEquals(expectedNameIndeces[i], sheader.getNameIndex());
			assertEquals(expectedSize[i], sheader.getSize());
			assertEquals(expectedTypes[i], sheader.getType());
			
			ElfData data = section.getData();
			assertNotNull(data);
			assertEquals(0, data.getAlignment());
			assertEquals(0, data.getOffset());
			assertEquals(expectedDataSizes[i], data.getSize());
			assertEquals(expectedDataTypes[i], data.getType());
			if(data.getSize() != 0)
				assertEquals(expectedBytes[i], data.getByte(0));
		}
		
		ElfPHeader[] pheaders = testElf.getPHeaders();
		pheaders.toString();
		
	}
	
}
