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
	private static final String pcElement = "PC";
	private static final Element pcName = new Element(pcElement);
	
	
	public static void main(String[] args) {
	
	Element root = new Element("Frysk_test");
	Document data = new Document(root);
	DOMFrysk dom = new DOMFrysk(data);
	pc = new BigInteger("25");
	dom.DOMSetPC(pc);
	dom.printDOM();
	
	}
}
