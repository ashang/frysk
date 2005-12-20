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

public class InlineBuffer extends SourceBuffer {

	private DOMFunction declaration;
	private DOMInlineInstance instance; 
	
	private TextChildAnchor ellipsisAnchor;
	
	public InlineBuffer(DOMSource scope, DOMInlineInstance instance) {
		super();
		this.instance = instance;
		this.declaration = this.instance.getDeclaration();
		StackLevel myScope = new StackLevel(scope, instance.getPCLine());
		this.setScope(myScope);
	}
	
	
	protected String loadLines(Iterator lines){
		// I know this seems really bad, but we're just going to
		// drop the iterator on the ground... we don't need it
		
		String[] funcLines = this.declaration.getLines();
		String result = "";
		
		for(int i = 0; i < funcLines.length; i++)
			result += funcLines[i];
		
		return result;
	}
	
	public boolean hasInlineCode(int lineNumber){
		
		// MAKE SURE WE DON'T LOOK IN THE DOMSOURCE FOR INLINE CODE!!
		return (this.instance.hasInlineInstance() && lineNumber == this.getCurrentLine());
	}
	
	public int getLineCount(){
		return this.declaration.getEndingLine() - this.declaration.getStartingLine();
	}

    public int getLastLine(){
        return this.declaration.getEndingLine();
    }
    
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
    
    public void clearEllipsisAnchor(){
    	
    }
    
    protected void setCurrentLine(int startLine, int startCol, int endLine, int endCol){
    	super.setCurrentLine(startLine - this.declaration.getStartingLine() + 1,
    			startCol, endLine - this.declaration.getStartingLine() + 1, endCol);
    }
    
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
    
    public void moveDown(){
    	if(this.instance.hasInlineInstance()){
    		this.instance = instance.getInlineInstance();
    		this.declaration = this.instance.getDeclaration(); 
    		StackLevel myScope = new StackLevel(this.declaration.getSource(), instance.getPCLine());
    		this.setScope(myScope);
    	}
    	// Can we even get a case where there is no next inline instance and this method
    	// is called?
    }
    
    public void moveUp(){
    	if(this.instance.hasParentInlineInstance()){
    		this.instance = instance.getPreviousInstance();
    		this.declaration = this.instance.getDeclaration();
    		StackLevel myScope = new StackLevel(this.declaration.getSource(), instance.getPCLine());
    		this.setScope(myScope);
    	}
    	// Same connundrum as above method...
    }
}
