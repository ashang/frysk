package lib.elf.tests;

import java.io.File;

import junit.framework.TestCase;
import lib.elf.Elf;
import lib.elf.ElfCommand;
import lib.elf.ElfEHeader;
import lib.elf.ElfKind;
import lib.elf.ElfSection;
import lib.elf.ElfSectionHeader;

public class TestElf extends TestCase {

	public void testElf(){
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
		System.err.println(header.getSectionHeaderEntryCount());
		
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
		
//		assertEquals(true, false);
	}
	
}
