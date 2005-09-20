/**
 * 
 */
package frysk.gui.srcwin.dom;

import java.util.Iterator;
import java.util.Vector;

import org.jdom.Element;
import org.jdom.filter.Filter;

/**
 * @author ajocksch
 *
 */
public class DOMLine {
	private Element myElement;
	
	public DOMLine(Element data){
		this.myElement = data;
	}
	
	public int getLineNum(){
		return Integer.parseInt(this.myElement.getAttributeValue("number"));
	}
	
	public int getLength(){
		return Integer.parseInt(this.myElement.getAttributeValue("length"));
	}
	
	public int getOffset(){
		return Integer.parseInt(this.myElement.getAttributeValue("offset"));
	}
	
	public boolean hasInlinedCode(){
		return Boolean.getBoolean(this.myElement.getAttributeValue("has_inline"));
	}
	
	public boolean isExecutable(){
		return Boolean.getBoolean(this.myElement.getAttributeValue("executable"));
	}
	
	public int getInlinedCodeCount(){
		// TODO: does this need to be an attribute or refer to the earlier nodes? 
		return 0;
	}
	
	public Iterator getTags(){
		Iterator iter = this.myElement.getChildren("tag").iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMTag((Element) iter.next()));
		
		return v.iterator();
	}
	
	public Iterator getTags(String type){
		final String theType = type;
		
		Iterator iter = this.myElement.getContent(new Filter() {
			public boolean matches(Object arg0) {
				Element elem = (Element) arg0;
				
				if(elem.getName().equals("tag") && 
						elem.getAttributeValue("type").equals(theType))
					return true;
				return false;
			}
		}).iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMTag((Element) iter.next()));
		
		return v.iterator();
	}
}
