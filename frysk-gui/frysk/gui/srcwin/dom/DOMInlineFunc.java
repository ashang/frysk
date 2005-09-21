/**
 * 
 */
package frysk.gui.srcwin.dom;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom.Element;

/**
 * @author ajocksch
 *
 */
public class DOMInlineFunc {
	private static final String END_ATTR = "end";
	public static final String START_ATTR = "start";
	public static final String NAME_ATTR = "name";
	private Element myElement;
	public static final String INLINE_NODE = "inline";
	
	public DOMInlineFunc(Element data){
		this.myElement = data;
	}
	
	/**
	 * @return The name of the inlined code
	 */
	public String getName(){
		return this.myElement.getAttributeValue(NAME_ATTR);
	}
	
	/**
	 * @return The length in lines of the code block that will be inlined
	 */
	public int getLineCount(){
		return this.myElement.getChildren().size();
	}
	
	/**
	 * @return The start of the inlined code as a char offset from the start of the file
	 */
	public int getStart(){
		return Integer.parseInt(this.myElement.getAttributeValue(START_ATTR));
	}
	
	/**
	 * @return The end of the inlined code as a char offset from the start of the file
	 */
	public int getEnd(){
		return Integer.parseInt(this.myElement.getAttributeValue(END_ATTR));
	}
	
	/**
	 * @return The number of the first line of inlined code
	 */
	public int getStartLine(){
		DOMLine firstChild = new DOMLine((Element) this.myElement.getChildren().get(0));
		return firstChild.getLineNum();
	}
	
	/**
	 * @return The number of the last line of inlined code
	 */
	public int getEndLine(){
		List children = this.myElement.getChildren();
		DOMLine lastChild = new DOMLine((Element) children.get(children.size()-1));
		
		return lastChild.getLineNum();
	}
	
	/**
	 * @return An iterator over the lines of code contained within the inlined block
	 */
	public Iterator getLines(){
		Iterator iter = this.myElement.getChildren().iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMLine((Element) iter.next()));
		
		return v.iterator();
	}
}
