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
public class DOMSource {
	private Element myElement;
	
	public DOMSource(Element data){
		this.myElement = data;
	}
	
	public String getFileName(){
		return this.myElement.getAttributeValue("filename");
	}
	
	public String getFilePath(){
		return this.myElement.getAttributeValue("filepath");
	}
	
	public Iterator getLines(){
		Iterator iter = this.myElement.getChildren("line").iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMLine((Element) iter.next()));
		
		return v.iterator();
	}
	
	public DOMLine getLineNum(int num){
		final int lineNum = num;
		
		Iterator iter = this.myElement.getContent(new Filter() {
			static final long serialVersionUID = 1L;			
			public boolean matches(Object arg0) {
				Element elem = (Element) arg0;
				
				if(elem.getName().equals("line") && 
						Integer.parseInt(elem.getAttributeValue("number")) == lineNum)
					return true;
				
				return false;
			}
		}).iterator();
		
		if(!iter.hasNext())
			return null;
		
		DOMLine val = new DOMLine((Element) iter.next());
		
		if(iter.hasNext()){
			// TODO: Throw exception? 
			// This should not be happening: duplicate source lines!
		}
		
		return val;
	}
}
