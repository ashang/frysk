/**
 * 
 */

package frysk.gui.srcwin.dom;

import frysk.gui.srcwin.dom.DOMFrysk;
import java.math.BigInteger;
import org.jdom.Document;
import org.jdom.Element;

public class DOMTestDOMFrysk {
	private static BigInteger pc;

	public static void main(String[] args) {

		Element root = new Element("Frysk_test");
		Document data = new Document(root);
		DOMFrysk dom = new DOMFrysk(data);
		pc = new BigInteger("25");
		dom.setPC(pc);
		if (dom.addImage("test_image", "/home/xyz", "/usr/src/redhat")) {
			System.out.println("Adding first image(test_image) passed.");
		} else {
			System.out.println("Adding first image(test_image) failed.");
		}
		if (dom.addImage("test_image_2", "/usr/src/redhat", "/tmp")) {
			System.out.println("Adding first image(test_image_2) passed.");
		} else {
			System.out.println("Adding first image(test_image_2) failed.");
		}
		if (!dom.addImage("test_image", "nada", "nada")) {
			System.out.println("Tried to add duplicate image test passed.");
		} else {
			System.out.println("Duplicate imaqe add test failed.");
		}
		if (dom.getPC().equals(pc)) {
			System.out.println("Get PC test passed.");
		} else {
			System.out.println("Get PC test failed.");
		}
		if (dom.getImage("test_image_2").getName() == "test_image_2") {
			System.out.println("DOMGetImage test passed.");
		} else {
			System.out.println("DOMGetImage test failed.");
		}
		if (dom.addPID(256)) {
			System.out.println("addPID test passed.");
		} else {
			System.out.println("addPID test failed.");
		}
		if (dom.addPID(12)) {
			System.out.println("Trying to add second PID test failed.");
		} else {
			System.out.println("Trying to add second PID test passed.");
		}
		if (dom.getPID() == 256) {
			System.out.println("Trying to get PID value test passed.");
		} else {
			System.out.println("Trying to get PID value test failed.");
		}
		if (dom.getImage("abc_xyz") == null) {
			System.out.println("DOMGetImage test for invalid image name passed.");
		} else {
			System.out.println("DOMGetImage test for invalid image name failed.");
		}
		System.out.println("\n\n");
		dom.printDOM();
	}
}
