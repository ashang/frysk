/**
 * 
 */
package frysk.gui.srcwin.dom;

import java.util.Iterator;
import java.util.Vector;

import org.jdom.Element;
import org.jdom.filter.Filter;

/**
 * DOMSource represents a source code file within the frysk source window dom
 * @author ajocksch
 */
public class DOMSource {
	/**
	 * Path to this file
	 */
	public static final String FILEPATH_ATTR = "filepath";
	/**
	 * Name of this file
	 */
	public static final String FILENAME_ATTR = "filename";
	/**
	 * Name of this node in the DOM tree
	 */
	public static final String SOURCE_NODE = "source";
	
/*	public static DOMSource createDOMSource(String filename, String path){
		Element source = new Element(SOURCE_NODE);
		source.setAttribute(FILENAME_ATTR, filename);
		source.setAttribute(FILEPATH_ATTR, path);
		
		return new DOMSource(source);
	}
	
	public static DOMSource createDOMSource(DOMImage parent, String filename, String path){
		Element source = new Element(SOURCE_NODE);
		source.setAttribute(FILENAME_ATTR, filename);
		source.setAttribute(FILEPATH_ATTR, path);
		parent.getElement().addContent(source);
		
		return new DOMSource(source);
	}  */
	
	private Element myElement;
	
	/**
	 * Creates a new DOMSource object with the given data as it's Element. data must be a 
	 * node with name "source"
	 * @param data
	 */
	public DOMSource(Element data){
		this.myElement = data;
	}
	
	/**
	 * @param name to set the filename to
	 * @return true = set name worked, false if not
	 */
	
	public void setFileName(String name) {
		this.myElement.setAttribute(FILENAME_ATTR, name);
	}
	/**
	 * @return The name of the file
	 */
	public String getFileName(){
		return this.myElement.getAttributeValue(FILENAME_ATTR);
	}
	
	/**
	 * @param new path to set the FILEPATH_ATTR to
	 * @return  true if successful, false if not
	 */
	public void setFilePath(String path) {
		this.myElement.setAttribute(FILEPATH_ATTR, path);
	}
	
	/**
	 * @return The path to the file
	 */
	public String getFilePath(){
		return this.myElement.getAttributeValue(FILEPATH_ATTR);
	}
	
	/**
	 * @return An iterator over all of the lines in this file
	 */
	public Iterator getLines(){
		Iterator iter = this.myElement.getChildren(DOMLine.LINE_NODE).iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMLine((Element) iter.next()));
		
		return v.iterator();
	}
	
	/**
	 * Attempts to return the DOMLine corresponding to the given line in the file. If no 
	 * tags exist on that line then null is returned.
	 * @param num The line number to get
	 * @return The DOMLine corresponding to the line, or null if no tags exist on that line
	 */
	public DOMLine getLineNum(int num){
		final int lineNum = num;
		
		Iterator iter = this.myElement.getContent(new Filter() {
			static final long serialVersionUID = 1L;			
			public boolean matches(Object arg0) {
				Element elem = (Element) arg0;
				
				if(elem.getName().equals(DOMLine.LINE_NODE) && 
						Integer.parseInt(elem.getAttributeValue(DOMLine.NUMBER_ATTR)) == lineNum)
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
	
	public void addLine(DOMLine line){
		this.myElement.addContent(line.getElement());
	}
	
	/**
	 * @return An iterator to all the inlined function declarations in this source file
	 */
	public Iterator getInlinedFunctions(){
		Iterator iter = this.myElement.getChildren(DOMInlineFunc.INLINE_NODE).iterator();
		Vector v = new Vector();
		
		while(iter.hasNext())
			v.add(new DOMInlineFunc((Element) iter.next()));
		
		return v.iterator();
	}
	
	/**
	 * Adds an inline function to this source. By convention the inline function declarations
	 * are added earlier in the xml schema than the line table
	 * 
	 * @param function The inlined function declaration to add
	 */
	public void addInlineFunction(DOMInlineFunc function){
		// Add the functions at the top, lines on the bottom
		this.myElement.addContent(0, function.getElement());
	}
	
	/**
	 * This method should only be used internally from the frysk source window dom
	 * @return The Jdom element at the core of this node 
	 */
	protected Element getElement(){
		return this.myElement;
	}
}
