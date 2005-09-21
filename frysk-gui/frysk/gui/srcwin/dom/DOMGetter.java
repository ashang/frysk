package frysk.gui.srcwin.dom;

import java.util.Iterator;
import java.util.Vector;
import org.jdom.Document;
import org.jdom.Element;

/**
 * DOMGetter serves as an access point to the document object model for the frysk source
 * window.
 * 
 * @author ajocksch
 */

public class DOMGetter {
	
	/**
	 * The pid of the process this DOM represents
	 */
	public static final String PID_ATTR = "pid";
	private Document data;
	
	/**
	 * Creates a new DOMGetter using the DOM contained in data
	 * @param data The Document to use as the source window DOM
	 */
	public DOMGetter(Document data){
		this.data = data;
	}
	
	/**
	 * Retreives all the images contained in the DOM as an iterator
	 * @return
	 */
	public Iterator getImages(){
		Iterator i = this.data.getRootElement().getChildren().iterator();
		Vector v = new Vector();		
		
		while(i.hasNext()){
			Element elem = (Element) i.next();
			v.add(new DOMImage(elem));
		}
		
		return v.iterator();		
	}
	
	/**
	 * Attempts to fetch an image of the given name from the DOM. If no image is found
	 * returns null
	 * @param name The name of the image to look for
	 * @return The DOMImage corresponding to the element, or null if no such element exists
	 */
	public DOMImage getImage(String name){
		Iterator i = this.data.getRootElement().getChildren().iterator();
		
		while(i.hasNext()){
			Element elem = (Element) i.next();
			if(elem.getAttributeValue(DOMImage.NAME_ATTR).equals(name))
				return new DOMImage(elem);
		}
		
		return null;
	}
	
	/**
	 * @return The PID of the process that this DOM represents
	 */
	public int getPID(){
		return Integer.parseInt(this.data.getRootElement().getAttribute(PID_ATTR).getValue()); 
	}
}
