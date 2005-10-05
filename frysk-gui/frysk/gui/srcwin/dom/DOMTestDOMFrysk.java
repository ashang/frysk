/**
 * 
 */

package frysk.gui.srcwin.dom;

import frysk.gui.srcwin.dom.DOMFrysk;
import java.math.BigInteger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class DOMTestDOMFrysk {
	private static BigInteger pc;

	private static Element root = new Element("Frysk_test");

	private static Document data = new Document(root);

	private static DOMFrysk dom = new DOMFrysk(data);
	
	private static String[] func1_lines = { "testfunc() {", "x=y", "int i =x", "}" }; 

	public static void main(String[] args) {

		testDOMFrysk();
		testDOMImage();
		System.out.println("\n\n");
		printDOM();
	}

	public static void testDOMFrysk() {

		pc = new BigInteger("25");
		dom.setPC(pc);
		if (dom.addImage("test_image", "/home/xyz", "/usr/src/redhat")) {
			System.out.println("passed...dom.addImage - adding first image(test_image)");
		} else {
			System.out.println("failed...dom.addImage - adding first image(test_image)");
		}
		if (dom.addImage("test_image_2", "/usr/src/redhat", "/tmp")) {
			System.out.println("passed...dom.addImage - adding first image(test_image_2)");
		} else {
			System.out.println("failed...dom.addImage - adding first image(test_image_2)");
		}
		if (!dom.addImage("test_image", "nada", "nada")) {
			System.out.println("passed...dom.addImage - add duplicate image");
		} else {
			System.out.println("failed...dom.addImage - add duplicate image");
		}
		if (dom.getPC().equals(pc)) {
			System.out.println("passed...dom.getPC");
		} else {
			System.out.println("failed...dom.getPC");
		}
		if (dom.addPID(256)) {
			System.out.println("passed...addPID");
		} else {
			System.out.println("failed...addPID");
		}
		if (dom.addPID(12)) {
			System.out.println("passed...dom.addPID trying to add second PID");
		} else {
			System.out.println("passed...dom.addPID trying to add second PID");
		}
		if (dom.getPID() == 256) {
			System.out.println("passed...dom.getPID");
		} else {
			System.out.println("failed...dom.getPID");
		}
		if (dom.getImage("abc_xyz") == null) {
			System.out
					.println("passed...DOMGetImage test for invalid image name");
		} else {
			System.out
					.println("failed...DOMGetImage test for invalid image name");
		}
	}

	public static void testDOMImage() {
		DOMImage testDOMImage = dom.getImage("test_image_2");
		if (testDOMImage.getName() == "test_image_2") {
			System.out.println("passed...DOMGetImage");
		} else {
			System.out.println("failed...DOMGetImage");
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

		if (testDOMImage.addSource("test_source", "/home/xyz")) {
			System.out.println("passed...DOMImage.addSource");
		} else {
			System.out.println("failed...DOMImage.addsource");
		}
		testDOMImage.setCCPath("/usr/local/share");
		if (testDOMImage.getCCPath() == "/usr/local/share") {
			System.out.println("passed...DOMImage.setCCPath");
		} else {
			System.out.println("failed...DOMImage.setCCPath");
		}
		if (testDOMImage.addInlineFunction("func1", func1_lines)) {
			System.out.println("passed...DOMImage.addInlineFunction...first one");
		} else {
			System.out.println("failed...DOMImage.addInlineFunction...first one");
		}

	}
	
	/**
	 * Print out the DOM in XML format
	 */
	public static void printDOM() {
			try {
				XMLOutputter outputter = new XMLOutputter(Format
						.getPrettyFormat());
				outputter.output(data, System.out);
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
	}
}
