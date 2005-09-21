/**
 * 
 */
package frysk.gui.srcwin.dom;

import java.util.Iterator;
import java.util.Vector;

import org.jdom.Element;
import org.jdom.filter.Filter;

/**
 * DOMLine represents a line of code (not assembly instruction) in a file.
 * 
 * @author ajocksch
 */
public class DOMLine {
	/**
	 * Whether this line is executable or not
	 */
	public static final String EXECUTABLE_ATTR = "executable";
	/**
	 * Whether this line has inlined code or not
	 */
	public static final String HAS_INLINE_ATTR = "has_inline";
	/**
	 * The offset in characters from the start of the file
	 */
	public static final String OFFSET_ATTR = "offset";
	/**
	 * The length of the line
	 */
	public static final String LENGTH_ATTR = "length";
	/**
	 * The name of the Element node
	 */
	public static final String LINE_NODE = "line";
	/**
	 * The number of this line
	 */
	public static final String NUMBER_ATTR = "number";
	
	private Element myElement;
	private Vector inlines;
	
	/**
	 * Creates a new DOMLine using the given data as it's element. data must be a node with
	 * name "line".
	 * @param data
	 */
	public DOMLine(Element data){
		this.myElement = data;
		this.inlines = new Vector();
	}
	
	/**
	 * @return The number of this line
	 */
	public int getLineNum(){
		return Integer.parseInt(this.myElement.getAttributeValue(NUMBER_ATTR));
	}
	
	/** 
	 * @return The length of this line in characters
	 */
	public int getLength(){
		return Integer.parseInt(this.myElement.getAttributeValue(LENGTH_ATTR));
	}
	
	/**
	 * @return The offset of this line from the start of the file in characters
	 */
	public int getOffset(){
		return Integer.parseInt(this.myElement.getAttributeValue(OFFSET_ATTR));
	}
	
	/**
	 * @return Whether or not this line contains inlined code
	 */
	public boolean hasInlinedCode(){
		return Boolean.getBoolean(this.myElement.getAttributeValue(HAS_INLINE_ATTR));
	}
	
	/**
	 * @return Whether or not this line is executable
	 */
	public boolean isExecutable(){
		return Boolean.getBoolean(this.myElement.getAttributeValue(EXECUTABLE_ATTR));
	}
	
	/**
	 * @return The number of lines of inlined code contained within this line
	 */
	public int getInlinedCodeCount(){
		// TODO: does this need to be an attribute or refer to the earlier nodes? 
		return 0;
	}
	
	/**
	 * @return An iterator to all the of tags contained on this line of code
	 */
	public Iterator getTags(){
		Iterator iter = this.myElement.getChildren(DOMTag.TAG_NODE).iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMTag((Element) iter.next()));
		
		return v.iterator();
	}
	
	/**
	 * Tries to find all the tags on this line of a given type
	 * @param type The type of tag to look for
	 * @return An iterator to all the tags of that type on the line
	 */
	public Iterator getTags(String type){
		final String theType = type;
		
		Iterator iter = this.myElement.getContent(new Filter() {
			static final long serialVersionUID = 1L;

			public boolean matches(Object arg0) {
				Element elem = (Element) arg0;
				
				if(elem.getName().equals(DOMTag.TAG_NODE) && 
						elem.getAttributeValue(DOMTag.TYPE_ATTR).equals(theType))
					return true;
				return false;
			}
		}).iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMTag((Element) iter.next()));
		
		return v.iterator();
	}
	
	/**
	 * Returns the tag at the given index of the line. Index can either be from the start
	 * of the file or from the start of the line
	 * @param index Offset to look for a tag at
	 * @return The tag (if any) at that position
	 */
	public DOMTag getTag(int index){
		int lineStart = this.getOffset();
		if(index < lineStart)
			index += lineStart;
		
		Iterator iter = this.myElement.getChildren().iterator();
		
		while(iter.hasNext()){
			DOMTag tag = new DOMTag((Element) iter.next());
			if(tag.isInRange(index))
				return tag;
		}
		
		return null;
	}
	
	public Iterator getInlines(){
		return this.inlines.iterator();
	}
}
