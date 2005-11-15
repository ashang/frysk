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
package frysk.gui.srcwin.dom;

import java.math.BigInteger;
import java.util.Iterator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class DOMTestDOMFrysk {
	private static BigInteger pc;

	private static Element root = new Element("Frysk_test");

	private static Document data = new Document(root);

	private static DOMFrysk dom = new DOMFrysk(data);

	private static String[] main_prog = { "int c(){\n", "   do_something();\n",
			"}\n" };

	private static int[] offset_index = { 1, 12, 28 };
	
//	private static int[] inline_start = { 12, 33, 66 };
//	
//	private static int[] inline_end = { 20, 45, 78 };
//
//	private static String[] inline_funcs = { "do_something", "b", "f" };
//
	private static boolean[] is_inline = { false, true, false };
//
//	private static String[] do_something = { "void do_something(){", "   b();",
//			"}" };
//
//	private static String[] b = { "void b() {", "f();", "}" };
//
//	private static String[] f = { "void f(){", "syscall_here();", "}" };

	public static void main(String[] args) {

		testDOMFrysk();
		testDOMImage();
		testDOMFunction();
		testDOMsource();
		testDOMLine();
		testDOMInlineInstance();
		System.out.println("\n\n");
		printDOM();
	}

	/**************************************************************************
	 * tests the DOMFrysk Class methods
	 * 
	 *************************************************************************/
	public static void testDOMFrysk() {

		pc = new BigInteger("25");
		dom.setPC(pc);
		if (dom.addImage("test_image", "/home/xyz", "/usr/src/redhat")) {
			System.out
					.println("passed...DOMFrysk.addImage - adding first image(test_image)");
		} else {
			System.out
					.println("failed...DOMFrysk.addImage - adding first image(test_image)");
		}
		if (dom.addImage("test_image_2", "/usr/src/redhat", "/tmp")) {
			System.out
					.println("passed...DOMFrysk.addImage - adding first image(test_image_2)");
		} else {
			System.out
					.println("failed...DOMFrysk.addImage - adding first image(test_image_2)");
		}
		if (!dom.addImage("test_image", "nada", "nada")) {
			System.out
					.println("passed...DOMFrysk.addImage - add duplicate image");
		} else {
			System.out
					.println("failed...DOMFrysk.addImage - add duplicate image");
		}
		if (dom.getPC().equals(pc)) {
			System.out.println("passed...DOMFrysk.getPC");
		} else {
			System.out.println("failed...DOMFrysk.getPC");
		}
		if (dom.addPID(256)) {
			System.out.println("passed...DOMFrysk.addPID");
		} else {
			System.out.println("failed...DOMFrysk.addPID");
		}
		if (dom.addPID(12)) {
			System.out
					.println("failed...DOMFrysk.addPID trying to add second PID");
		} else {
			System.out
					.println("passed...DOMFrysk.addPID trying to add second PID");
		}
		if (dom.getPID() == 256) {
			System.out.println("passed...DOMFrysk.getPID");
		} else {
			System.out.println("failed...DOMFrysk.getPID");
		}
		if (dom.getImage("abc_xyz") == null) {
			System.out
					.println("passed...DOMFrysk.getImage test for invalid image name");
		} else {
			System.out
					.println("failed...DOMFrysk.getImage test for invalid image name");
		}
	}
	
	/**************************************************************************
	 * tests the DOMImage Class methods
	 * 
	 *************************************************************************/
	public static void testDOMImage() {
		DOMImage testDOMImage = dom.getImage("test_image_2");
		if (testDOMImage.getName() == "test_image_2") {
			System.out.println("\npassed...DOMImage.getName");
		} else {
			System.out.println("\nfailed...DOMImage.getName");
		}
		if (testDOMImage.getCCPath() != "/usr/src/redhat") {
			System.out.println("failed...DOMImage.getCCPath");
		} else {
			System.out.println("passed...DOMImage.getCCPath");
		}
		if (testDOMImage.getName() == "test_image_2") {
			System.out.println("passed...DOMImage.getName");
		} else {
			System.out.println("failed...DOMImage.getName");
		}

		testDOMImage.addSource("test_source1", "/home/xyz");
		if (testDOMImage.getSource("test_source1") != null) {
			System.out.println("passed...DOMImage.addSource...test_source1");
		} else {
			System.out.println("failed...DOMImage.addsource...test_source1");
		}
		testDOMImage.addSource("test_source2", "/var/tmp");
		if (testDOMImage.getSource("test_source2") != null) {
			System.out.println("passed...DOMImage.addSource...test_source2");
		} else {
			System.out.println("failed...DOMImage.addsource...test_spurce2");
		}
		testDOMImage.setCCPath("/usr/local/share");
		if (testDOMImage.getCCPath() == "/usr/local/share") {
			System.out.println("passed...DOMImage.setCCPath");
		} else {
			System.out.println("failed...DOMImage.setCCPath");
		}
//		testDOMImage.addFunction(inline_funcs[0], do_something,
//				inline_start[0], inline_end[0]);
//		if (testDOMImage.getFunction(inline_funcs[0]) != null) {
//			System.out.println("passed...DOMImage.addInlineFunction..."
//					+ inline_funcs[0]);
//		} else {
//			System.out.println("failed...DOMImage.addInlineFunction..."
//					+ inline_funcs[0]);
//		}
//		testDOMImage.addFunction(inline_funcs[1], b, inline_start[0],
//				inline_end[1]);
//		if (testDOMImage.getFunction(inline_funcs[1]) != null) {
//			System.out.println("passed...DOMImage.addInlineFunction..."
//					+ inline_funcs[1]);
//		} else {
//			System.out.println("failed...DOMImage.addInlineFunction..."
//					+ inline_funcs[1]);
//		}
//		testDOMImage.addFunction("f", f, inline_start[2],
//				inline_end[2]);
//
//		Iterator iter = testDOMImage.getInlinedFunctions();
		int ctr = 0;
//		while (iter.hasNext()) {
//			Element test_inlined = (Element) iter.next();
//			ctr++;
//			String inlinename = test_inlined.getAttributeValue(
//					DOMFunction.INLINENAME_ATTR).toString();
//			if (ctr == 1 && (inlinename == inline_funcs[ctr - 1])) {
//				System.out.println("passed...DOMImage.getInlinedFunctions..."
//						+ inlinename);
//				continue;
//			}
//			if (ctr == 2 && (inlinename == inline_funcs[ctr - 1])) {
//				System.out.println("passed...DOMImage.getInlinedFunctions..."
//						+ inlinename);
//				continue;
//			}
//			if (ctr == 3 && (inlinename == inline_funcs[ctr - 1])) {
//				System.out.println("passed...DOMImage.getInlinedFunctions..."
//						+ inlinename);
//				continue;
//			}
//			System.out.println("failed...DOMImage.getInlinedFunctions");
//		}
		if (testDOMImage.getSource("test_source1") != null) {
			System.out.println("passed...DOMImage.getSource");
		} else {
			System.out.println("failed...DOMImage.getSource");
		}
		Iterator iter_sources = testDOMImage.getSources();
		ctr = 0;
		while (iter_sources.hasNext()) {
			Element test_sources = (Element) iter_sources.next();
			ctr++;
			String inlinename = test_sources
					.getAttributeValue(DOMSource.FILENAME_ATTR);
			if (ctr == 1 && (inlinename == "test_source1")) {
				System.out.println("passed...DOMImage.getSources..."
						+ inlinename);
				continue;
			}
			if (ctr == 2 && (inlinename == "test_source2")) {
				System.out.println("passed...DOMImage.getSources..."
						+ inlinename);
				continue;
			}
			System.out.println("failed...DOMImage.getSources..." + inlinename);
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

		if (testDOMSource.getFileName() == "test_source2") {
			System.out.println("\npassed...DOMSource.getFileName");
		} else {
			System.out.println("\nfailed...DOMSource.getFileName");
		}

		testDOMSource.setFileName("test_source1.1");
		if (testDOMSource.getFileName() == "test_source1.1") {
			System.out.println("passed...DOMSource.setFileName");
		} else {
			System.out.println("failed...DOMSource.setFileName");
		}

		if (testDOMSource.getFilePath() == "/var/tmp") {
			System.out.println("passed...DOMSource.getFilePath");
		} else {
			System.out.println("failed...DOMSource.getFilePath");
		}

		testDOMSource.setFilePath("/opt/share/java");
		if (testDOMSource.getFilePath() == "/opt/share/java") {
			System.out.println("passed...DOMSource.setFilePath");
		} else {
			System.out.println("failed...DOMSource.setFilePath");
		}

		BigInteger no_bytes = BigInteger.valueOf(4);
		BigInteger pc = BigInteger.valueOf(25842);
		for (int ctr = 0; ctr < main_prog.length; ctr++) {
			testDOMSource.addLine(ctr + 1, main_prog[ctr], true,
					is_inline[ctr], false, offset_index[ctr], pc);
			pc = pc.add(no_bytes);
		}

		Iterator line_iter = testDOMSource.getLines();
		int line_ctr = 0;
		while (line_iter.hasNext()) {
			Element line = (Element) line_iter.next();
			String linetext = line.getText();
			if (linetext == main_prog[line_ctr]) {
				line_ctr++;
			} else {
				System.out.println("failed...DOMSource.addLine/getLines");
				break;
			}
		}
		System.out.println("passed...DOMSource.addLine/getLines");
		
		if (testDOMSource.getLineCount() == main_prog.length) {
			System.out.println("passed...DOMSource.getLineCount");
		} else {
			System.out.println("failed...DOMSource.getLineCount");
		}

		final DOMLine testDOMLine = testDOMSource.getLine(2);
		if (testDOMLine.getElement().getText() == main_prog[1]) {
			System.out.println("passed...DOMSource.getLine/DOMLine.getElement");
		} else {
			System.out.println("failed...DOMSource.getLine/DOMLine.getElement");
		}
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

		if (testDOMLine.getLineNum() == line_no) {
			System.out.println("\npassed...DOMLine.getLineNum");
		} else {
			System.out.println("\nfailed...DOMLine.getLineNum");
		}
		if (testDOMLine.getLength() == main_prog[line_no-1].length()) {
			System.out.println("passed...DOMLine.getLength");
		} else {
			System.out.println("failed...DOMLine.getLength");
		}

		if (testDOMLine.getText() == main_prog[line_no-1]) {
			System.out.println("passed...DOMLine.getText");
		} else {
			System.out.println("failed...DOMLine.getText");
		}

		String text = "   do_something_else();\n";
		testDOMLine.setText(text);
		if (testDOMLine.getText() == text) {
			System.out.println("passed...DOMLine.setText");
		} else {
			System.out.println("failed...DOMLine.setText");
		}

		if (testDOMLine.getOffset() == offset_index[1]) {
			System.out.println("passed...DOMLine.getOffset");
		} else {
			System.out.println("failed...DOMLine.getOffset");
		}

		int offset = offset_index[1] + 1;
		testDOMLine.setOffset(offset);
		if (testDOMLine.getOffset() == (offset_index[1] + 1)) {
			System.out.println("passed...DOMLine.setOffset");
		} else {
			System.out.println("failed...DOMLine.setOffset");
		}
		testDOMLine.setOffset(offset_index[1]);
		
		if (testDOMLine.isExecutable()) {
			System.out.println("passed...DOMLine.isExecutable");
		} else {
			System.out.println("failed...DOMLine.isExecutable");
		}

		testDOMLine.setExecutable(false);
		if (!testDOMLine.isExecutable()) {
			System.out.println("passed...DOMLine.setExecutable");
		} else {
			System.out.println("failed...DOMLine.setExecutable");
		}
		testDOMLine.setExecutable(true);

		if (testDOMLine.hasInlinedCode()) {
			System.out.println("passed...DOMLine.hasInlineCode");
		} else {
			System.out.println("failed...DOMLine.hasInlineCode");
		}

		if (!testDOMLine.hasBreakPoint()) {
			System.out.println("passed...DOMLine.hasBreakPoint");
		} else {
			System.out.println("failed...DOMLine.hasBreakPoint");
		}

		testDOMLine.setBreakPoint(true);
		if (testDOMLine.hasBreakPoint()) {
			System.out.println("passed...DOMLine.setBreakPoint");
		} else {
			System.out.println("failed...DOMLine.setBreakPoint");
		}
		
		String test_inline = "do_something";
		int start_inline = main_prog[1].indexOf(test_inline) + main_prog[0].length();
		int end_inline = main_prog[1].indexOf(test_inline) + test_inline.length() +
				main_prog[0].length();
		testDOMLine.addInlineInst(test_inline, start_inline, end_inline);
		if (testDOMLine.getInlineElement(test_inline).
				getAttributeValue(DOMInlineInstance.LINEINST_ATTR)
				== test_inline) {
			System.out.println("passed...DOMLine.addInstance/getInlineInst");
		} else {
			System.out.println("failed...DOMLine.addInstance/getInlineInst");
		}
		
		String tag_type = "inline";
		DOMTag tag = new DOMTag(testDOMLine, tag_type,
				main_prog[1].indexOf(test_inline),
				main_prog[1].indexOf(test_inline) + test_inline.length());
		System.out.println("passed...DOMLine.addtag");
				
		Iterator iter_gettags = testDOMLine.getTags();
		while (iter_gettags.hasNext()) {
			Element gettags = (Element) iter_gettags.next();
			if (gettags.getAttributeValue(DOMTag.TYPE_ATTR) 
					== tag_type) {
				System.out.println("passed...DOMLine.getTags");
			} else {
				System.out.println("failed...DOMLine.getTags");
			}
		}
		
		Iterator iter_getinlines = testDOMLine.getInlines();
		while (iter_getinlines.hasNext()) {
			Element getinlines = (Element) iter_getinlines.next();
			String inline_name = getinlines.getAttributeValue
				(DOMInlineInstance.LINEINST_ATTR);
			if (inline_name
					== test_inline) {
				System.out.println("passed...DOMLine.getInlines");
			} else {
				System.out.println("failed...DOMLine.getInlines");
			}
			DOMFunction test_func = testDOMImage.getFunction(inline_name);
			if (test_func.getName() == test_inline) {
				System.out.println("passed...DOMFunction.getFunction");
			} else {
				System.out.println("failed...DOMFunction.getFunction");
			}
		}
		
		if (tag.getType() == tag_type) {
			System.out.println("\npassed...DOMTag.getType");
		} else {
			System.out.println("\nfailed...DOMTag.getType");
		}
		
		String new_tag_type = "keyword";
		tag.setType(new_tag_type);
		if (tag.getType() == new_tag_type) {
			System.out.println("passed...DOMTag.setType");
		} else {
			System.out.println("failed...DOMTag.setType");
		}
		
		if (tag.getStart() == main_prog[1].indexOf(test_inline)) {
			System.out.println("passed...DOMTag.getStart");
		} else {
			System.out.println("failed...DOMTag.getStart");
		}
		
		int new_start = 25;
		tag.setStart(new_start);
		if (tag.getStart() == new_start) {
			System.out.println("passed...DOMTag.setStart");
		} else {
			System.out.println("failed...DOMTag.setStart");
		}
		
		if (tag.getLength() == 
			main_prog[1].indexOf(test_inline) + test_inline.length()) {
			System.out.println("passed...DOMTag.getEnd");
		} else {
			System.out.println("failed...DOMTag.getEnd");
		}
		
		int new_end = 35;
		tag.setLength(new_end);
		if (tag.getLength() == new_end) {
			System.out.println("passed...DOMTag.setEnd");
		} else {
			System.out.println("failed...DOMTag.setEnd");
		}
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
		if (testDOMInlineInstance.getStart() == start_index) {
			System.out.println("\npassed...DOMInlineInstance.setStart/getStart");
		} else {
			System.out.println("\nfailed...DOMInlineInstance.setStart/getStart");
		}
		
		testDOMInlineInstance.setEnd(end_index);
		if (testDOMInlineInstance.getEnd() == end_index) {
			System.out.println("passed...DOMInlineInstance.setEnd/getEnd");
		} else {
			System.out.println("failed...DOMInlineInstance.setEnd/getEnd");
		}
	}

	/**
	 * Print out the DOM in XML format
	 */
	public static void printDOM() {
		try {
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			outputter.output(data, System.out);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
	}
}
