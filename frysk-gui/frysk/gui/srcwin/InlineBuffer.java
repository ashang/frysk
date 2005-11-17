package frysk.gui.srcwin;

import java.util.Iterator;

import frysk.gui.srcwin.dom.DOMFunction;
import frysk.gui.srcwin.dom.DOMInlineInstance;

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
}
