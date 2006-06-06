package lib.dw.tests;

import junit.framework.TestCase;
import lib.dw.Dwfl;
import lib.dw.DwflLine;

public class TestDwfl extends TestCase {
	
	public void testGetLine(){
		Dwfl dwfl = new Dwfl(TestLib.getPid());
		assertNotNull(dwfl);
		DwflLine line = dwfl.getSourceLine(TestLib.getFuncAddr());
		assertNotNull(line);
		String filename = line.getSourceFile();
		System.out.println(line.getLineNum());
		System.out.println(line.getColumn());
		assertNotNull(filename);
		assertEquals(true, false);
	}
	
}
