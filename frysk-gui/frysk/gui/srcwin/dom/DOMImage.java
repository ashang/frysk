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
	 * name of the inline element
	 */
	
	public static final String INLINENAME_ATTR = "inlinename";
	
	public static final String INLINE_NODE = "inline";
	private Element myElement;
	
	/**
	 * Creates a new DOMImage from the given Element. Data must be of name "image".
	 * @param data An Element of name "image"
	 */
	public DOMImage(Element data){
		this.myElement = data;
	}
	
	/**
	 * adds a source element under this image
	 * @param source_name
	 * @param path
	 * @return
	 */
	public void addSource(String source_name, String path) {
		
		Element sourceNameElement = new Element(DOMSource.SOURCE_NODE);
		sourceNameElement.setAttribute(DOMSource.FILENAME_ATTR, source_name);
		sourceNameElement.setAttribute(DOMSource.FILEPATH_ATTR, path);
		this.myElement.addContent(sourceNameElement);
	}
	
	/**
	 * adds an inline function to an image
	 * @param name of the inline function
	 * @param an array of Strings containing the lines in the function
	 */
	public void addInlineFunction(String inline_name, String[] lines) {
		
		Element inlineNameElement = new Element(INLINE_NODE);
		inlineNameElement.setAttribute(INLINENAME_ATTR, inline_name);
		this.myElement.addContent(inlineNameElement);
		for (int i=0; i<lines.length; i++) {
			Element lineNumber = new Element(DOMSource.LINENO_NODE);
			lineNumber.setAttribute(DOMLine.NUMBER_ATTR, String.valueOf(i+1));
			lineNumber.setText(lines[i]);
			inlineNameElement.addContent(lineNumber);
		}
	}
	
	/**
	 * @return The name of the image
	 */
	public String getName(){
		return this.myElement.getAttributeValue(DOMSource.FILENAME_ATTR);
	}
	
	/**
	 * Sets the CCPATH of the current image
	 * @param image_name
	 */
	public void setCCPath(String image_name) {
		this.myElement.setAttribute(CCPATH_ATTR, image_name);
		return;
	}
	/**
	 * 
	 * @param name what the name of the image will be
	 */
	public void setName(String name) {
		
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
		return this.myElement.getChildren(DOMSource.SOURCE_NODE).iterator();
	}
	
	/**
	 * Attempts to fetch an image of the given name from the DOM. If no image is
	 * found returns null
	 * 
	 * @param name
	 *            The name of the image to look for
	 * @return The DOMSource corresponding to the element, or null if no such
	 *         element exists
	 */
	public DOMSource getSource(String name) {
		Iterator i = this.myElement.getChildren().iterator();

		while (i.hasNext()) {
			Element elem = (Element) i.next();
			if (elem.getQualifiedName().equals(DOMSource.SOURCE_NODE)) {
				if (elem.getAttributeValue(DOMSource.FILENAME_ATTR)
						.equals(name))
					return new DOMSource(elem);
			}
		}

		return null;
	}
	
	/**
	 * attempts to fetch an inlined function DOM element
	 * @param name of the inlined function to return
	 * @return the DOMImage corresponding to the element, or null if no such
	 * 	element exists
	 */
	public Element getInlineFunction(String name) {
		Iterator iter = this.myElement.getChildren(INLINE_NODE).iterator();
		while (iter.hasNext()) {
			Element node = (Element) iter.next();
			if (node.getAttributeValue(INLINENAME_ATTR) == name)
				return node;
		}
		return null;
	}
	
	/**
	 * fetches all of the inlined functions for this DOMImage 
	 * @return an iterator of all of the inlined functions for this DOMImage
	 */

	public Iterator getInlinedFunctions(){
		Iterator iter = this.myElement.getChildren(INLINE_NODE).iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add((Element) iter.next());
		
		return v.iterator();
	}
	
	/**
	 * This function should only be used internally within the frysk source dom
	 * @return The JDom element at the core of this node
	 */
	protected Element getElement() {
		return this.myElement;
	}
}
