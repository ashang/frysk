package frysk.gui.srcwin;

import java.util.Iterator;

import org.jdom.Element;

import frysk.gui.srcwin.dom.DOMFunction;
import frysk.gui.srcwin.dom.DOMInlineInstance;
import frysk.gui.srcwin.dom.DOMLine;
import frysk.gui.srcwin.dom.DOMTag;
import frysk.gui.srcwin.dom.DOMTagTypes;

public class InlineBuffer extends SourceBuffer {

	private DOMFunction declaration;
	private DOMInlineInstance instance; 
	
	public InlineBuffer(StackLevel scope, DOMInlineInstance instance) {
		super();
		this.instance = instance;
		this.declaration = this.instance.getDeclaration();
		this.setScope(scope);
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
		// TODO: right now DOMInlineInstances don't have references to other DOMInlineInstances
		//    -fix this.
		
		
		// MAKE SURE WE DON'T LOOK IN THE DOMSOURCE FOR INLINE CODE!!
		this.instance.getInlineInstance();
		return false;
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
//        return super.isLineExecutable(lineNum + this.declaration.getStartingLine() - 1);
    }
    
    public boolean isLineBroken(int lineNum){
    	// For right now we don't have 'theoretical' breakpoints
    	// that exist in inline instances.
    	return false;
//        return super.isLineBroken(lineNum + this.declaration.getStartingLine() - 1);
    }
    
    public boolean toggleBreakpoint(int lineNum){
    	// For right now we've disabled setting breakpoints in specific inline
    	// instances
    	return false;
//        if(!this.isLineExecutable(lineNum))
//            return false;
//        
//        DOMLine line = this.scope.getData().getLine(lineNum + this.declaration.getStartingLine());
//        if(line == null)
//            return false;
//        
//        boolean status = line.hasBreakPoint();
//        line.setBreakPoint(!status);
//        return !status;
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
}
