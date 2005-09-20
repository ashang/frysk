/**
 * 
 */
package frysk.gui.srcwin.dom;

import java.util.Iterator;
import java.util.Vector;

import org.jdom.Element;

/**
 * @author ajocksch
 *
 */
public class DOMImage {
	private Element myElement;
	
	public DOMImage(Element data){
		this.myElement = data;
	}
	
	public String getName(){
		return this.myElement.getAttributeValue("name");
	}
	
	public String getCCPath(){
		return this.myElement.getAttributeValue("CCPATH");
	}
	
	public Iterator getSources(){
		Iterator iter = this.myElement.getChildren("source").iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMSource((Element) iter.next()));
		
		return v.iterator();
	}
	
}
