/**
 * 
 */

package frysk.gui.srcwin.dom;

import frysk.gui.srcwin.dom.DOMCreate;
import java.math.BigInteger;

public class DOMTestCreate {
	private static BigInteger pc;
	private static String test = "test";
	
	
	public static void main(String[] args) {
	
	
	pc = new BigInteger("25");

		DOMCreate dom = new DOMCreate(test, pc);
	}
}
