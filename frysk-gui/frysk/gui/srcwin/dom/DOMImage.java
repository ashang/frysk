/**
 * 
 */
package frysk.gui.srcwin.dom;

import java.util.Iterator;
import java.util.Vector;

import org.jdom.Element;

/**
 * DOMImage represents an image within the source window document object model
 * 
 * @author ajocksch
 */
public class DOMImage {
	/**
	 * CCPATH for the image
	 */
	public static final String CCPATH_ATTR = "CCPATH";
	/**
	 * name of the image
	 */
	public static final String NAME_ATTR = "name";
	/**
	 * name of the node in the tree
	 */
	public static final String IMAGE_NODE = "image";
	
	public static DOMImage createDOMImage(String name, String ccpath){
		Element image = new Element(IMAGE_NODE);
		image.setAttribute(NAME_ATTR, name);
		image.setAttribute(CCPATH_ATTR, ccpath);
		
		return new DOMImage(image);
	}
	
	private Element myElement;
	
	/**
	 * Creates a new DOMImage from the given Element. Data must be of name "image".
	 * @param data An Element of name "image"
	 */
	public DOMImage(Element data){
		this.myElement = data;
	}
	
	/**
	 * @return The name of the image
	 */
	public String getName(){
		return this.myElement.getAttributeValue(NAME_ATTR);
	}
	
	/**
	 * @return The CCPATH of the image
	 */
	public String getCCPath(){
		return this.myElement.getAttributeValue(CCPATH_ATTR);
	}
	
	/**
	 * @return an iterator to all the source files contained in this image.
	 */
	public Iterator getSources(){
		Iterator iter = this.myElement.getChildren(DOMSource.SOURCE_NODE).iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMSource((Element) iter.next()));
		
		return v.iterator();
	}
	
	/**
	 * Adds a source file to this image
	 * @param source The source file to add
	 */
	public void addSource(DOMSource source){
		this.myElement.addContent(source.getElement());
	}
	
	/**
	 * This function should only be used internally within the frysk source dom
	 * @return The JDom element at the core of this node
	 */
	protected Element getElement(){
		return this.myElement;
	}
}
