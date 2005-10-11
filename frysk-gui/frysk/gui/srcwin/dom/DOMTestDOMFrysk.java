/**
 * 
 */

package frysk.gui.srcwin.dom;

import frysk.gui.srcwin.dom.DOMFrysk;
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

	private static int[] start_index = { 1, 12, 28 };

	private static int[] end_index = { 11, 27, 29 };

	private static String[] inline_funcs = { "do_something", "b", "f" };

	private static Boolean[] is_inline = { Boolean.valueOf(false),
			Boolean.valueOf(true), Boolean.valueOf(false) };

	private static String[] do_something = { "void do_something(){", "   b();",
			"}" };

	private static String[] b = { "void b() {", "f();", "}" };

	private static String[] f = { "void f(){", "syscall_here();", "}" };

	public static void main(String[] args) {

		testDOMFrysk();
		testDOMImage();
		testDOMSource();
		System.out.println("\n\n");
		printDOM();
	}

	/**
	 * tests the DOMFrysk Class methods
	 * 
	 */

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
					.println("passed...DOMFrysk.addPID trying to add second PID");
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

	/**
	 * tests the DOMImage Class methods
	 * 
	 */

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
		testDOMImage.addInlineFunction(inline_funcs[0], do_something);
		if (testDOMImage.getInlineFunction(inline_funcs[0]) != null) {
			System.out.println("passed...DOMImage.addInlineFunction..."
					+ inline_funcs[0]);
		} else {
			System.out.println("failed...DOMImage.addInlineFunction..."
					+ inline_funcs[0]);
		}
		testDOMImage.addInlineFunction(inline_funcs[1], b);
		if (testDOMImage.getInlineFunction(inline_funcs[1]) != null) {
			System.out.println("passed...DOMImage.addInlineFunction..."
					+ inline_funcs[1]);
		} else {
			System.out.println("failed...DOMImage.addInlineFunction..."
					+ inline_funcs[1]);
		}
		testDOMImage.addInlineFunction("f", f);

		Iterator iter = testDOMImage.getInlinedFunctions();
		int ctr = 0;
		while (iter.hasNext()) {
			Element test_inlined = (Element) iter.next();
			ctr++;
			String inlinename = test_inlined.getAttributeValue(
					DOMImage.INLINENAME_ATTR).toString();
			if (ctr == 1 && (inlinename == inline_funcs[ctr - 1])) {
				System.out.println("passed...DOMImage.getInlinedFunctions..."
						+ inlinename);
				continue;
			}
			if (ctr == 2 && (inlinename == inline_funcs[ctr - 1])) {
				System.out.println("passed...DOMImage.getInlinedFunctions..."
						+ inlinename);
				continue;
			}
			if (ctr == 3 && (inlinename == inline_funcs[ctr - 1])) {
				System.out.println("passed...DOMImage.getInlinedFunctions..."
						+ inlinename);
				continue;
			}
			System.out.println("failed...DOMImage.getInlinedFunctions");
		}
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

	/**
	 * test the DOMSource class methods
	 * 
	 */
	public static void testDOMSource() {

		DOMImage testDOMImage = dom.getImage("test_image_2");
		DOMSource testDOMSource = testDOMImage.getSource("test_source2");

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
		Boolean is_executable = Boolean.valueOf(true);
		BigInteger pc = BigInteger.valueOf(25842);
		for (int ctr = 0; ctr < main_prog.length; ctr++) {
			testDOMSource.addLine(ctr + 1, main_prog[ctr], is_executable,
					is_inline[ctr], start_index[ctr], end_index[ctr], pc);
			pc = pc.add(no_bytes);
		}

		Iterator line_iter = testDOMSource.getLines();
		int line_ctr = 0;
		while (line_iter.hasNext()) {
			Element line = (Element) line_iter.next();
			String linetext = line.getAttributeValue(DOMSource.TEXT_ATTR);
			if (linetext == main_prog[line_ctr]) {
				line_ctr++;
			} else {
				System.out.println("failed...DOMSource.addLine/getLines");
				break;
			}
		}
		System.out.println("passed...DOMSource.addLine/getLines");
		
		DOMLine testDOMLine = testDOMSource.getLineNum1(2);
		if (testDOMLine.getElement().getAttributeValue(DOMSource.TEXT_ATTR)
				== main_prog[1]) {
			System.out.println("passed...DOMSource.getLineNum1");
		} else {
			System.out.println("failed...DOMSource.getLineNum1");
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
