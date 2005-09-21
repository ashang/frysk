/**
 * 
 */
package frysk.gui.srcwin.dom;

/**
 * DOMInlineInstance represents the instance of a piece of inlined code. It contains the 
 * information specific to this instance as well as a reference to the declaration to speed up
 * parsing time and for reference.
 * @author ajocksch
 */
public class DOMInlineInstance {
	private DOMInlineFunc declaration;
	private int start;
	private int end;
	
	public DOMInlineInstance(DOMInlineFunc declaration, int start, int end){
		this.declaration = declaration;
		this.start = start;
		this.end = end;
	}
	
	/**
	 * @return The start of the inlined instance as a character offset from the start of the file
	 */
	public int getStart(){
		return this.start;
	}
	
	/**
	 * @return The end of the instance as a character offset from the start of the file
	 */
	public int getEnd(){
		return this.end;
	}
	
	/** 
	 * @return The original declaration of this inlined code
	 */
	public DOMInlineFunc getDeclaration(){
		return this.declaration;
	}
}
