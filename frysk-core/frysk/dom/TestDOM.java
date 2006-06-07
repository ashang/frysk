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
package frysk.dom;

import java.math.BigInteger;
import java.util.Iterator;

import junit.framework.TestCase;

import org.jdom.Document;
import org.jdom.Element;

/**
 * This is a test for the DOM implementation in Frysk.
 * A dummy DOM is built with this test suite and results
 * checked.
 */

public class TestDOM extends TestCase {
	private static BigInteger pc;

	private static Element root = new Element("Frysk_test");

	private static Document data = new Document(root);

	private static DOMFrysk dom = new DOMFrysk(data);

	private static String[] main_prog = { "int c(){\n", "   do_something();\n",
			"}\n" };

	private static int[] offset_index = { 1, 12, 28 };

	/**************************************************************************
	 * tests the DOMFrysk Class methods
	 * 
	 *************************************************************************/
	
	public static void testDOMFrysk() {

		pc = new BigInteger("25");
		dom.setPC(pc);
		assertTrue("testing DOMFrysk.addImage() - adding first image",
				  dom.addImage("test_image", 
				  "/home/xyz", "/usr/src/redhat"));

		assertTrue("testing DOMFrysk.addImage() - adding second image", 
				dom.addImage("test_image_2", 
				"/usr/src/redhat", "/tmp"));
		
		assertFalse("testing DOMFrysk.addimage() - adding duplicate image", 
				dom.addImage("test_image", "nada", "nada"));
		
		assertTrue("testing DOMFrysk.getPC()", dom.getPC().equals(pc));
		
		assertTrue("testing DOMFrysk.addPID() - 256", dom.addPID(256));
		
		assertFalse("testing DOMFrysk.addPID() - 12", dom.addPID(12));
		
		assertEquals("testing DOMFrysk.getPID()", dom.getPID(), 256);
		
		assertNull("testing DOMFrysk.getImage()", dom.getImage("abc_xyz"));
		
	}
	
	/**************************************************************************
	 * tests the DOMImage Class methods
	 * 
	 *************************************************************************/
	public static void testDOMImage() {
		DOMImage testDOMImage = dom.getImage("test_image_2");
		assertEquals("testing DOMImage.getName()", 
				testDOMImage.getName(), "test_image_2");
		
		assertEquals("testing DOMImage.getCCPath()", 
				testDOMImage.getCCPath(), "/usr/src/redhat");
		
		assertEquals("testing DOMImage.getName()", 
				testDOMImage.getName(), "test_image_2");

		testDOMImage.addSource("test_source1", "/home/xyz");
		
		assertNotNull("testing DOMImage.assSource- test_source1",
				testDOMImage.getSource("test_source1"));
		
		testDOMImage.addSource("test_source2", "/var/tmp");
		
		assertNotNull("testing DOMImage.addSource() - test_source2",
				testDOMImage.getSource("test_source2"));
		
		testDOMImage.setCCPath("/usr/local/share");
		
		assertEquals("testing DOMImage.setCCPath()", testDOMImage.getCCPath(),
				"/usr/local/share");

		assertNotNull("testing DOMImage.getSource() - test_source1", 
				testDOMImage.getSource("test_source1"));
		
		Iterator iter_sources = testDOMImage.getSources();
		int ctr = 0;
		while (iter_sources.hasNext()) {
			Element test_sources = (Element) iter_sources.next();
			ctr++;
			String inlinename = test_sources
					.getAttributeValue(DOMSource.FILENAME_ATTR);
			if (ctr == 1) {
				assertEquals("testing DOMImage.getSources - ctr = 1",
						inlinename, "test_source1");
				continue;
			}
			
			if (ctr == 2) {
				assertEquals("testing DOMImage.getSources - ctr = 2",
						inlinename, "test_source2");
				continue;
			}
		}
	}
	
	/**************************************************************************
	 * test the DOMFunction class methods
	 **************************************************************************/
	public static void testDOMFunction() {
		
//		final String func_name = "do_something";
//		final DOMImage testDOMImage = dom.getImage("test_image_2");
//		final DOMFunction testDOMFunction = testDOMImage.getFunction(func_name);
//		
//		if (testDOMFunction.getName() == func_name) {
//			System.out.println("\npassed...DOMFunction.getName");
//		} else {
//			System.out.println("\nfailed...DOMFunction.getName");
//		}
//		
//		if (testDOMFunction.getLineCount() == do_something.length) {
//			System.out.println("passed...DOMFunction.getLineCount");
//		} else {
//			System.out.println("passed...DOMFunction.getLineCount");
//		}
//		
//		String[] lines = testDOMFunction.getLines();
//		for (int i=0; i < do_something.length; i++) {
//			if (lines[i] != do_something[i]) {
//				System.out.println("lines[i] = " + lines[i]);
//				System.out.println("failed...DOMFunction.getLines");
//				break;	
//			}
//		}
//		System.out.println("passed...DOMFunction.getLines");
//		
//		Iterator getline_iter = testDOMFunction.getLinesIter();
//		int line_ctr = 0;
//		boolean failed = false;
//		while (getline_iter.hasNext()) {
//			Element get_line = (Element) getline_iter.next();
//			if (get_line.getText() != do_something[line_ctr]) {
//				failed = true;
//			}
//			line_ctr++;
//		}
//		if (failed) {
//			System.out.println("failed...DOMFunction.getLineIter");
//		} else {
//			System.out.println("passed...DOMFunction.getLineIter");
//		}
//		
//		if (testDOMFunction.getStart() == inline_start[0]) {
//			System.out.println("passed...DOMFunction.getStart");
//		} else {
//			System.out.println("failed...DOMFunction.getStart");
//		}
//		
//		if (testDOMFunction.getEnd() == inline_end[0]) {
//			System.out.println("passed...DOMFunction.getEnd");
//		} else {
//			System.out.println("failed...DOMFunction.getEnd");
//		}
//		
//		int start_line = testDOMFunction.getStartingLine();
//		if (start_line == 1) {
//			System.out.println("passed...DOMFunction.getStartLine");
//		} else {
//			System.out.println("failed...DOMFunction.getStartLine");
//		}
//		
//		int end_line = testDOMFunction.getEndingLine();
//		if (end_line == 3) {
//			System.out.println("passed...DOMFunction.getEndLine");
//		} else {
//			System.out.println("failed...DOMFunction.getEndLine");
//		}
	}

	/**************************************************************************
	 * test the DOMSource class methods
	 *************************************************************************/
	public static void testDOMsource() {

		final DOMImage testDOMImage = dom.getImage("test_image_2");
		final DOMSource testDOMSource = testDOMImage.getSource("test_source2");
		
		assertEquals("testing DOMSource.getFileName()", testDOMSource.getFileName(),
				"test_source2");

		testDOMSource.setFileName("test_source1.1");
		
		assertEquals("testing DOMSource.setFileName()", testDOMSource.getFileName(),
				"test_source1.1");
		
		assertEquals("testing DOMSource.getFilePath()", testDOMSource.getFilePath(),
				"/var/tmp");

		testDOMSource.setFilePath("/opt/share/java");
		
		assertEquals("testing DOMSource.setFilePath()", testDOMSource.getFilePath(),
				"/opt/share/java");

		long no_bytes = 4;
		long pc = 25842;
		for (int ctr = 0; ctr < main_prog.length; ctr++) {
			testDOMSource.addLine(ctr + 1, main_prog[ctr], true,
					false, offset_index[ctr], pc);
			pc += no_bytes;
		}

		Iterator line_iter = testDOMSource.getLines();
		int line_ctr = 0;
		while (line_iter.hasNext()) {
			Element line = (Element) line_iter.next();
			String linetext = line.getText();
			assertEquals("testing DOMSource.getLines()", linetext, 
					main_prog[line_ctr]);
			line_ctr++;
		}
		
		assertEquals("testing DOMSource.getLineCount()",testDOMSource.getLineCount(), 
				main_prog.length);

		final DOMLine testDOMLine = testDOMSource.getLine(2);
		
		assertEquals("testing DOMSource.getLine()", 
				testDOMLine.getElement().getText(), main_prog[1]);
	}

	/**************************************************************************
	 * Test the DOMLine class
	 *************************************************************************/
	public static void testDOMLine() {
		final int line_no = 2;
		final DOMImage testDOMImage = dom.getImage("test_image_2");
		final DOMSource testDOMSource = testDOMImage
				.getSource("test_source1.1");
		final DOMLine testDOMLine = testDOMSource.getLine(line_no);

		assertEquals("testing DOMLine.getLineNum()", testDOMLine.getLineNum(),
				line_no);
		
		assertEquals("testing DOMLine.getLength()", testDOMLine.getLength(),
				main_prog[line_no-1].length());
		
		assertEquals("testing DOMLine.getText()", testDOMLine.getText(), 
				main_prog[line_no-1]);

		String text = "   do_something_else();\n";
		testDOMLine.setText(text);
		
		assertEquals("testing DOMLine.setText()", testDOMLine.getText(), text);
		
		assertEquals("testing DOMLine.getOffset()", testDOMLine.getOffset(),
				offset_index[1]);

		int offset = offset_index[1] + 1;
		testDOMLine.setOffset(offset);
		
		assertEquals("testing DOMLine.setOffset()", 
				testDOMLine.getOffset(), offset_index[1] + 1);
		
		testDOMLine.setOffset(offset_index[1]);
		
		assertTrue("testing DOMLine.isExecutable()", testDOMLine.isExecutable());

		testDOMLine.setExecutable(false);
		
		assertFalse("testing DOMLine.setExecutable()", testDOMLine.isExecutable());
		
		testDOMLine.setExecutable(true);

		assertFalse("testing DOMLine.hasInlinedCode()", testDOMLine.hasInlinedCode());

		assertFalse("testing DOMLine.hasBreakPoint() - false", testDOMLine.hasBreakPoint());

		testDOMLine.setBreakPoint(true);
		
		assertTrue("testing DOMLine.setBreakPoint() - true", testDOMLine.hasBreakPoint());
		
		String test_inline = "do_something";
		int start_inline = main_prog[1].indexOf(test_inline) + main_prog[0].length();
		int end_inline = main_prog[1].indexOf(test_inline) + test_inline.length() +
				main_prog[0].length();
		testDOMLine.addInlineInst(test_inline, start_inline, end_inline, 0);
		
		assertEquals("testing DOMLine.addInlineInst()", 
				testDOMLine.getInlineInst(test_inline).getElement().
				getAttributeValue(DOMInlineInstance.LINEINST_ATTR), test_inline);
		
		String tag_type = "inline";
		DOMTag tag = new DOMTag(tag_type, test_inline,
				main_prog[1].indexOf(test_inline));
		testDOMLine.addTag(tag);
				
		Iterator iter_gettags = testDOMLine.getTags();
		while (iter_gettags.hasNext()) {
			Element gettags = (Element) iter_gettags.next();
			assertEquals("testing DOMTag.getTags()", 
					gettags.getAttributeValue(DOMTag.TYPE_ATTR), tag_type);
		}
				
		assertEquals("testing DOMTag.getType()", tag.getType(), tag_type);
		
		String new_tag_type = "keyword";
		tag.setType(new_tag_type);
		
		assertEquals("testing DOMTag.setType()", tag.getType(), 
				new_tag_type);
		
		assertEquals("testing DOMTag.getStart()", tag.getStart(), 
				main_prog[1].indexOf(test_inline));
		
		int new_start = 25;
		tag.setStart(new_start);
		
		assertEquals("testing DOMTag.setStart()", tag.getStart(), new_start);
		
		assertEquals("testing DOMTag.getLength()", tag.getLength(), test_inline.length());
		
		int new_end = 35;
		tag.setLength(new_end);
		
		assertEquals("testing DOMTag.setEnd()", tag.getLength(), new_end);
		
	}
	
	/**************************************************************************
	 * Test the DOMInlineInstance class
	 **************************************************************************/
	
	public static void testDOMInlineInstance() {
		
		final String inst = "do_something";
		final int line_no = 2;
		final int start_index = 10;
		final int end_index = 20;
		final DOMImage testDOMImage = dom.getImage("test_image_2");
		final DOMSource testDOMSource = testDOMImage
				.getSource("test_source1.1");
		final DOMLine testDOMLine = testDOMSource.getLine(line_no);
		final DOMInlineInstance testDOMInlineInstance =
			testDOMLine.getInlineInst(inst);

		testDOMInlineInstance.setStart(start_index);
		
		assertEquals("testing DOMInlineInstance.getStart()", 
				testDOMInlineInstance.getStart(), start_index);
		
		testDOMInlineInstance.setEnd(end_index);
		
		assertEquals("testing DOMInlineInstance.setEnd()", 
				testDOMInlineInstance.getEnd(), end_index);

	}

}
