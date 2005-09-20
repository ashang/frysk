/**
 * 
 */
package frysk.gui.srcwin.dom;

import org.jdom.Element;

/**
 * @author ajocksch
 *
 */
public class DOMSource {
	private Element myElement;
	
	public DOMSource(Element data){
		this.myElement = data;
	}
	
	public String getFile(){
		return this.myElement.getAttributeValue("file");
	}
	
}
