package frysk.gui.srcwin;

import java.util.Iterator;

import org.gnu.gtk.TextChildAnchor;
import org.gnu.gtk.TextIter;
import org.jdom.Element;

import frysk.dom.DOMFunction;
import frysk.dom.DOMInlineInstance;
import frysk.dom.DOMLine;
import frysk.dom.DOMSource;
import frysk.dom.DOMTag;
import frysk.dom.DOMTagTypes;

/**
 * The InlineBuffer performs much of the same functionality as the SourceBuffer,
 * but also acts as an adapter to make the SourceWindow using it think that the 
 * function being displayed is actually a whole file, and that nothing exists beyond it.
 * It also overrides some parent methods to take into acccount ellipsis
 * @author ajocksch
 *
 */
public class InlineBuffer extends SourceBuffer {

	private DOMFunction declaration;
	private DOMInlineInstance instance; 
	
	private TextChildAnchor ellipsisAnchor;
	
	/**
	 * Creates a new InlineBuffer to reflect the provided instance
	 * @param scope The file that the declaration is in
	 * @param instance The inlined instance to display
	 */
	public InlineBuffer(DOMSource scope, DOMInlineInstance instance) {
		super();
		this.instance = instance;
		this.declaration = this.instance.getDeclaration();
		StackLevel myScope = new StackLevel(scope, instance.getPCLine());
		this.setScope(myScope, false);
	}
	
	
	/**
	 * Overriden. we ignore the iterator to the lines and instead give back the
	 * lines of the function
	 */
	protected String loadLines(Iterator lines){
		// Since we don't have/need a function to remove the anchor for 
		// the ellipsis, we do it whenever new content is loaded
		this.ellipsisAnchor = null;
		
		// I know this seems really bad, but we're just going to
		// drop the iterator on the ground... we don't need it
		
		String[] funcLines = this.declaration.getLines();
		String result = "";
		
		for(int i = 0; i < funcLines.length; i++)
			result += funcLines[i];
		
		return result;
	}
	
	/**
	 * Ignores the line number (except to check that it is the current line), and 
	 * returns whether or not this inlined code level has any further levels
	 */
	public boolean hasInlineCode(int lineNumber){
		
		return (this.instance.hasInlineInstance() && lineNumber == this.getCurrentLine());
	}
	
	/**
	 * Returns the number of lines in this function, note that this is different than the
	 * number of lines in the file it is contained in
	 */
	public int getLineCount(){
		if(this.ellipsisAnchor == null)
			return this.declaration.getEndingLine() - this.declaration.getStartingLine();
		else
			return this.declaration.getEndingLine() - this.declaration.getStartingLine() + 1;
	}

	/**
	 * Returns the last line being displayed
	 */
    public int getLastLine(){
    	if(this.ellipsisAnchor == null)
    		return this.declaration.getEndingLine();
    	else
    		return this.declaration.getEndingLine() + 1;
    }
    
    /**
     * @return The number of the first line being displayed
     */
    public int getFirstLine(){
        return this.declaration.getStartingLine();
    }
    
    public boolean isLineExecutable(int lineNum){
    	// For right now, don't let the user even try
    	// to set breakpoints. This is a little bit of a hack, but works
    	return false;
    }
    
    public boolean isLineBroken(int lineNum){
    	// For right now we don't have 'theoretical' breakpoints
    	// that exist in inline instances.
    	return false;
    }
    
    public boolean toggleBreakpoint(int lineNum){
    	// For right now we've disabled setting breakpoints in specific inline
    	// instances
    	return false;
    }
    
	public DOMInlineInstance getInlineInstance(int lineNumber){
		if(lineNumber == this.getCurrentLine())
			return this.instance.getInlineInstance();
		
		return null;
	}
	
    /**
     * We need to do a little fancier Voodoo than the superclass
     * to actually calculate variable values, since we're only
     * displaying the inline function 
     */
    public Variable getVariable(TextIter iter){
    	DOMLine line = this.scope.getData().getLine(
    			iter.getLineNumber()+this.declaration.getStartingLine());
		DOMTag tag = line.getTag(iter.getLineOffset());
		
		// No var (or no tag), do nothing
		if(tag == null || !tag.getType().equals(DOMTagTypes.LOCAL_VAR))
			return null;
		
		Variable var = new Variable(
				line.getText().substring(tag.getStart(), tag.getStart() + tag.getLength()), 
				iter.getLineNumber(), tag.getStart(), false);
		return var;
    }
    
    /**
     * Creates the anchor to hold the ellipsis.
     * @return The anchor created
     */
    public TextChildAnchor createEllipsisAnchor(){
    	/*
    	 * Note that there is no need for a "removeEllipsisAnchor" method, since if the
    	 * depth of this buffer changes in the stack, then the contents will be 
    	 * overridden, destroying the anchor anyways
    	 */
    	if(this.ellipsisAnchor != null)
    		this.deleteText(this.getStartIter(), 
    				this.getLineIter(this.getStartIter().getLineNumber()+1));
    	
    	this.insertText(this.getStartIter(), "\n");
		this.ellipsisAnchor = this.createChildAnchor(this.getStartIter());
		
		return this.ellipsisAnchor;
    }
    
    /**
     * Sets the current line, taking into account the offset from the start of the file
     */
    protected void setCurrentLine(int startLine, int startCol, int endLine, int endCol){
    	super.setCurrentLine(startLine - this.declaration.getStartingLine() + 1,
    			startCol, endLine - this.declaration.getStartingLine() + 1, endCol);
    }
    
    /**
     * Creates the tags, again taking into account the offset from the start of the file
     */
    protected void createTags(){
        Iterator lines = this.scope.getData().getLines();
        
        // Iterate through all the lines
        while(lines.hasNext()){
            DOMLine line = new DOMLine((Element) lines.next());
            
            if(line.getLineNum() < this.getFirstLine())
                continue;
            
            if(line.getLineNum() > this.getLastLine())
                break;
            
            Iterator tags = line.getTags();
            
            // Iterator though all the tags on the line
            while(tags.hasNext()){
                DOMTag tag = new DOMTag((Element) tags.next());
                
                String type = tag.getType();
                
                if(type.equals(DOMTagTypes.KEYWORD)){
                    this.applyTag(KEYWORD_TAG, 
                            this.getIter(line.getLineNum() - this.getFirstLine(), tag.getStart()),
                            this.getIter(line.getLineNum() - this.getFirstLine(), tag.getStart() + tag.getLength()));
                }
                
                else if(type.equals(DOMTagTypes.LOCAL_VAR)){
                    this.applyTag(ID_TAG, 
                            this.getIter(line.getLineNum() - this.getFirstLine(), tag.getStart()),
                            this.getIter(line.getLineNum() - this.getFirstLine(), tag.getStart() + tag.getLength()));
                }
                
                else if(type.equals(DOMTagTypes.CLASS_DECL)){
                    this.applyTag(SourceBuffer.CLASS_TAG, 
                            this.getIter(line.getLineNum() - this.getFirstLine(), tag.getStart()),
                            this.getIter(line.getLineNum() - this.getFirstLine(), tag.getStart() + tag.getLength()));
                }
                
                else if(type.equals(DOMTagTypes.FUNCTION)){
                    this.applyTag(FUNCTION_TAG, 
                            this.getIter(line.getLineNum() - this.getFirstLine(), tag.getStart()),
                            this.getIter(line.getLineNum() - this.getFirstLine(), tag.getStart() + tag.getLength()));
                }
                
            } // end tags.hasNext()
            
            Iterator inlines = line.getInlines();
            
            while(inlines.hasNext()){
                DOMInlineInstance func = new DOMInlineInstance((Element) inlines.next());
                
                this.applyTag(FUNCTION_TAG,
                        this.getIter(line.getLineNum() - this.getFirstLine(), func.getStart()),
                        this.getIter(line.getLineNum() - this.getFirstLine(), func.getStart() + func.getEnd()));
            }
        }// end lines.hasNext()
    }
    
    /**
     * Tells the buffer to move down a scope level. It is assumed that this method
     * will not be called (and if so have no effect) when this is the lowest level of
     * inlined code
     *
     */
    public void moveDown(){
    	if(this.instance.hasInlineInstance()){
    		this.instance = instance.getInlineInstance();
    		this.declaration = this.instance.getDeclaration(); 
    		StackLevel myScope = new StackLevel(this.declaration.getSource(), instance.getPCLine());
    		this.setScope(myScope, false);
    	}
    	// Can we even get a case where there is no next inline instance and this method
    	// is called?
    }
    
    /**
     * Tells the buffer to move up a scope level. It is assumed that this is not the topmost
     * level already, and if so nothing happens
     *
     */
    public void moveUp(){
    	if(this.instance.hasParentInlineInstance()){
    		this.instance = instance.getPreviousInstance();
    		this.declaration = this.instance.getDeclaration();
    		StackLevel myScope = new StackLevel(this.declaration.getSource(), instance.getPCLine());
    		this.setScope(myScope, false);
    	}
    	// Same connundrum as above method...
    }
}
