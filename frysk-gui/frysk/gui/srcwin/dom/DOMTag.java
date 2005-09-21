/**
 * 
 */
package frysk.gui.srcwin.dom;

import org.jdom.Element;

/**
 * DOMTag represents a tagged area (i.e. function declaration, varaible use, comment, etc) in
 * a source code file
 * @author ajocksch
 */
public class DOMTag {
	/**
	 * The end of the tag
	 */
	public static final String END_ATTR = "end";
	/**
	 * The start of the tag
	 */
	public static final String START_ATTR = "start";
	/**
	 * The type of the tag
	 */
	public static final String TYPE_ATTR = "type";
	/**
	 * The name of the DOM Element
	 */
	public static final String TAG_NODE = "tag";
	
	public Element myElement;
	
	/**
	 * Creates a new DOMTag using the given data as it's Element. data much be of name "tag"
	 * @param data
	 */
	public DOMTag(Element data){
		this.myElement = data;
	}
	
	/**
	 * @return The type of the tag
	 */
	public String getType(){
		return this.myElement.getAttributeValue(TYPE_ATTR);
	}
	
	/**
	 * @return The starting offset of the tag from the start of the file
	 */
	public int getStart(){
		return Integer.parseInt(this.myElement.getAttributeValue(START_ATTR));
	}
	
	/**
	 * @return The ending offset of the tag from the start of the file
	 */
	public int getEnd(){
		return Integer.parseInt(this.myElement.getAttributeValue(END_ATTR));
	}
	
	/**
	 * Tests to see if this tag encompasses the given index
	 * @param test The index, relative to the start of the file, to test
	 * @return true if the tag covers the index, false otherwise
	 */
	public boolean isInRange(int test){
		if(test < this.getEnd() && test > this.getStart())
			return true;
		
		return false;
	}
}
