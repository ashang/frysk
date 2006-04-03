// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.gui.srcwin;

import java.util.Iterator;

import org.gnu.gtk.TextChildAnchor;
import org.gnu.gtk.TextIter;
import org.jdom.Element;

import frysk.dom.DOMFunction;
import frysk.dom.DOMInlineInstance;
import frysk.dom.DOMLine;
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
	public InlineBuffer(DOMInlineInstance instance) {
		super();
		this.instance = instance;
		this.declaration = this.instance.getDeclaration();
		StackLevel myScope = new StackLevel(this.declaration, instance.getPCLine());
		this.setScope(myScope);
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

    	if(line == null)
    		return null;
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
    protected void setCurrentLine(CurrentLineSection currentLine){
    		currentLine.setStartLine(currentLine.getStartLine() - this.declaration.getStartingLine()+1);
    		currentLine.setEndLine(currentLine.getEndLine() - this.declaration.getStartingLine()+1);
    		super.setCurrentLine(currentLine);
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

                if(type.equals(DOMTagTypes.FUNCTION_BODY)){
                	// do nothing
                }
                else{
					this.applyTag(type, 
							this.getIter(line.getLineNum() - this.getFirstLine(), tag.getStart()),
							this.getIter(line.getLineNum() - this.getFirstLine(), tag.getStart() + tag.getLength()));
                }
               
            } // end tags.hasNext()
            
            Iterator inlines = line.getInlines();
            
            while(inlines.hasNext()){
                DOMInlineInstance func = new DOMInlineInstance((Element) inlines.next());
                
                this.applyTag(DOMTagTypes.FUNCTION,
                        this.getIter(line.getLineNum() - this.getFirstLine(), func.getStart()),
                        this.getIter(line.getLineNum() - this.getFirstLine(), func.getStart() + func.getEnd()));
            }
        }// end lines.hasNext()
        
//      Now iterate through the comments
		CommentList list = (CommentList) comments.get(this.scope.getData().getFileName());
		
		while(list != null){
			if(list.getEndLine() < this.declaration.getStartingLine()){
				list = list.getNextComment();
				continue;
			}
			
			if(list.getStartLine() >= this.declaration.getStartingLine()){
//				this.applyTag(COMMENT_TAG, this.getIter(list.getStartLine() - this.getFirstLine() + 1, list.getStartCol()),
//						this.getIter(list.getEndLine() - this.getFirstLine() + 1, list.getEndCol()));
			}
			else{
				// We have to get the first iter this way since we may have a header at the beginning of the
				// file for hidden inlined levels that shouldn't be displayed
//				this.applyTag(COMMENT_TAG, this.getLineIter(this.getFirstLine() - this.declaration.getStartingLine()),
//						this.getIter(list.getEndLine() - this.getFirstLine() + 1, list.getEndCol()));
			}
			
			list = list.getNextComment();
		}
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
    		StackLevel myScope = new StackLevel(this.declaration, instance.getPCLine());
    		this.setScope(myScope);
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
    		StackLevel myScope = new StackLevel(this.declaration, instance.getPCLine());
    		this.setScope(myScope);
    	}
    	// Same connundrum as above method...
    }
}
