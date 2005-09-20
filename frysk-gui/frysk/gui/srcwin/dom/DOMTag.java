/**
 * 
 */
package frysk.gui.srcwin.dom;

import org.jdom.Element;

/**
 * @author ajocksch
 *
 */
public class DOMTag {
	private Element myElement;
	
	public DOMTag(Element data){
		this.myElement = data;
	}
	
	public String getType(){
		return this.myElement.getAttributeValue("type");
	}
	
	public int getStart(){
		return Integer.parseInt(this.myElement.getAttributeValue("start"));
	}
	
	public int getEnd(){
		return Integer.parseInt(this.myElement.getAttributeValue("end"));
	}
	
	public boolean isInRange(int test){
		if(test < this.getEnd() && test > this.getStart())
			return true;
		
		return false;
	}
}
