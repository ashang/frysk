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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;
import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
import org.gnu.glib.Handle;
import org.gnu.glib.JGException;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextIter;
import org.gnu.gtk.TextMark;
import org.gnu.gtk.TextTag;
import org.gnu.gtk.TextTagTable;
import org.gnu.pango.Weight;

import frysk.gui.srcwin.PreferenceConstants.Classes;
import frysk.gui.srcwin.PreferenceConstants.Comments;
import frysk.gui.srcwin.PreferenceConstants.CurrentLine;
import frysk.gui.srcwin.PreferenceConstants.Functions;
import frysk.gui.srcwin.PreferenceConstants.GlobalVariables;
import frysk.gui.srcwin.PreferenceConstants.ID;
import frysk.gui.srcwin.PreferenceConstants.Inline;
import frysk.gui.srcwin.PreferenceConstants.Keywords;
import frysk.gui.srcwin.PreferenceConstants.Search;
import frysk.gui.srcwin.cparser.SimpleParser;

/**
 * This class is a wrapper around TextBuffer, it allows for extra functionality
 * needed by the SourceViewWidget.
 * 
 * It will need to be retrofited to use correct model objects once these are set,
 * instead of SourceCodeLine and so on.
 * 
 * @author ifoox
 *
 */
public class SourceBuffer extends TextBuffer {
	
	/* CONSTANTS */
	private static final String INLINE_TAG = "INLINE";
	private static final String COMMENT_TAG = "COMMENT";
	private static final String MEMBER_TAG = "MEMBER";
	private static final String FUNCTION_TAG = "FUNCTION";
	private static final String ID_TAG = "ID";
	private static final String KEYWORD_TAG = "TYPE";
	private static final String CURRENT_LINE = "currentLine";
	private static final String FOUND_TEXT = "foundText";
	/* END CONSTANTS */
	
	protected Vector lines;
	protected SourceLineReader lineParser;
	
	private Vector functions;
	
	private TextMark startCurrentLine;
	private TextMark endCurrentLine;
	
	private TextIter startCurrentFind;
	private TextIter endCurrentFind;
	
	private TextTag currentLine; 
	private TextTag foundText;
	private TextTag functionTag;
	private TextTag idTag;
	private TextTag globalTag;
	private TextTag keywordTag;
	private TextTag commentTag;
	private TextTag classTag;
	
	private TextTag inlinedTag;
	
	private VariableList varList;
		
	private StaticParser staticParser;
	
	/**
	 * Creates a new SourceBuffer
	 */
	public SourceBuffer(){ 
		super();
		this.init();
	}
	
	/**
	 * Creates a new SourceBuffer with the provided Handle to a native (TextBuffer)
	 * resource
	 * 
	 * @param handle Native resource
	 */
	public SourceBuffer(Handle handle){
		super(handle);
		this.init();
	}
	
	/**
	 * Creates a new SourceBuffer with the provided TextTagTable
	 * 
	 * @param table
	 */
	public SourceBuffer(TextTagTable table){
		super(gtk_text_buffer_new(table.getHandle()));
		this.init();
	}
	
	public void init(){
		lines = new Vector();
		functions = new Vector();
		this.currentLine = this.createTag(CURRENT_LINE);
		this.foundText = this.createTag(FOUND_TEXT);
		this.functionTag = this.createTag(FUNCTION_TAG);
		this.idTag = this.createTag(ID_TAG);
		this.keywordTag = this.createTag(KEYWORD_TAG);
		this.globalTag = this.createTag(MEMBER_TAG);
		this.commentTag = this.createTag(COMMENT_TAG);
		this.classTag = this.createTag("CLASS");	
		this.functionTag.setPriority(this.getTextTagTable().getSize() - 1);
		
		this.inlinedTag = this.createTag(INLINE_TAG);
	}
	
	/**
	 * Returns true if the given line is executable.
	 * 
	 * @param lineNo The line to check
	 * @return Whether or not the line is executable
	 */
	public boolean isLineExecutable(int lineNo){
		SourceCodeLine line = (SourceCodeLine)lines.get(lineNo);
		return line.isExecutable();
	}
	
	/** 
	 * Returns true if the given line has a breakpoint set.
	 * 
	 * @param lineNo
	 * @return
	 */
	public boolean isLineBroken(int lineNo){
		SourceCodeLine line = (SourceCodeLine)lines.get(lineNo);
		return line.isBreakpointSet();
	}
	
	/**
	 * Loads a file at the given location into the  buffer, clearing
	 * any previously loaded files.
	 * 
	 * @param filename The file to load
	 * @throws FileNotFoundException
	 * @throws JGException
	 */
	public void loadFile(String filename)throws FileNotFoundException, JGException{
		this.deleteText(this.getStartIter(), this.getEndIter());
		lines = new Vector();
		
		lineParser = new SourceLineReader(filename);
		
		SourceCodeLine line = lineParser.getNextLine();
		
		while (line != null){
			
			// append new row
			TextIter endOfText = this.getEndIter();
			this.insertText(endOfText, line.getSource());
			
			endOfText = this.getEndIter();
			this.insertText(("\n"));
			
			// add line to vector
			lines.add(line);
			
			line = lineParser.getNextLine();
		}
		
		this.varList = new VariableList(this.getLineCount());

		
		this.staticParser = new SimpleParser();
		try {
			System.out.println("Parsing");
			this.staticParser.parse(filename, this);
			System.out.println("Done parsing");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Toggles the breakpoint on line lineNum, returning the previous state
	 * of the breakpoint on that line.
	 * 
	 * @param lineNum the number of the line to toggle the breakpoint for, 
	 * starting at 0
	 * 
	 * @return true if breakpoint was previously set on lineNum, false 
	 * otherwise
	 * 
	 * @throws ArrayIndexOutOfBoundsException thrown if the line number given
	 * does not correspond to a valid line number
	 */
	public boolean toggleBreakpoint (int lineNum){
		boolean toggled = false;
		SourceCodeLine line;
		
		try {
			line = (SourceCodeLine) lines.get(lineNum);
		}catch (ArrayIndexOutOfBoundsException e){
			return false;
		}
		
		if (line.isExecutable()){
			toggled = true;
			boolean wasBroken = line.isBreakpointSet();
			line.setBreakpoint(!wasBroken);
		}else{
			// should we throw an exception here?
		}
		return toggled;
	}
	

	/**
	 * @return The current PC line
	 */
	public int getCurrentLine() {
		if(this.startCurrentLine == null)
			return -1;
		
		return this.getIter(this.startCurrentLine).getLineNumber();
	}

	
	/**
	 * Sets the current PC line to be the given line number
	 * @param currentLine The new PC line
	 */
	public void setCurrentLine(int currentLine) {
		
//		// If not an executable line, no tag should be set
//		if(!((SourceCodeLine) this.lines.get(currentLine)).isExecutable()){
//			// Clear the tag if one already existed
//			if(this.startCurrentLine != null && this.endCurrentLine != null){
//				this.removeTag(this.currentLine, this.startCurrentLine, this.endCurrentLine);
//				this.startCurrentLine = null;
//				this.endCurrentLine = null;
//			}
//			
//			return;
//		}
		
		// Line is executable
		System.out.println(this.getIter(currentLine-1, 0));
		this.startCurrentLine = this.createMark("currentLineStart", this.getIter(currentLine-1, 0), true);
		int lineLength = this.getText(this.getIter(this.startCurrentLine), this.getEndIter(), true).indexOf("\n");
		this.endCurrentLine = this.createMark("currentLineEnd", this.getIter(currentLine-1, lineLength), true);
		
		this.applyTag(this.currentLine, this.getIter(this.startCurrentLine), this.getIter(this.endCurrentLine));
	}
	
	/**
	 * Updates the appearance of the widget to reflect the data contained in the
	 * new preference model
	 * 
	 * @param topNode The new model to be displayed
	 */
	public void updatePreferences(Preferences topNode){
		Preferences currentNode = topNode.node(PreferenceConstants.LNF_NODE);
		
		// update current line color
		int r = currentNode.getInt(CurrentLine.R, CurrentLine.R_DEFAULT);
		int g = currentNode.getInt(CurrentLine.G, CurrentLine.G_DEFAULT);
		int b = currentNode.getInt(CurrentLine.B, CurrentLine.B_DEFAULT);
		this.currentLine.setBackground(ColorConverter.colorToHexString(new Color(r,g,b)));
		
		// Search color
		r = currentNode.getInt(Search.R, Search.R_DEFAULT);
		g = currentNode.getInt(Search.G, Search.G_DEFAULT);
		b = currentNode.getInt(Search.B, Search.B_DEFAULT);
		this.foundText.setBackground(ColorConverter.colorToHexString(new Color(r,g,b)));
		
		currentNode = topNode.node(PreferenceConstants.SYNTAX_NODE);
		
		// keyword syntax highlighting
		r = currentNode.getInt(Keywords.R, Keywords.R_DEFAULT);
		g = currentNode.getInt(Keywords.G, Keywords.G_DEFAULT);
		b = currentNode.getInt(Keywords.B, Keywords.B_DEFAULT);
		this.keywordTag.setForeground(ColorConverter.colorToHexString(new Color(r,g,b)));
		int weight = currentNode.getInt(Keywords.WEIGHT, Weight.BOLD.getValue());
		this.keywordTag.setWeight(Weight.intern(weight));
		
		// ID syntax highlighting
		r = currentNode.getInt(ID.R, ID.R_DEFAULT);
		g = currentNode.getInt(ID.G, ID.G_DEFAULT);
		b = currentNode.getInt(ID.B, ID.B_DEFAULT);
		this.idTag.setForeground(ColorConverter.colorToHexString(new Color(r,g,b)));
		weight = currentNode.getInt(ID.WEIGHT, Weight.NORMAL.getValue());
		this.idTag.setWeight(Weight.intern(weight));

		// Global variable syntax highlighting
		r = currentNode.getInt(GlobalVariables.R, GlobalVariables.R_DEFAULT);
		g = currentNode.getInt(GlobalVariables.G, GlobalVariables.G_DEFAULT);
		b = currentNode.getInt(GlobalVariables.B, GlobalVariables.B_DEFAULT);
		this.globalTag.setForeground(ColorConverter.colorToHexString(new Color(r,g,b)));
		weight = currentNode.getInt(GlobalVariables.WEIGHT, Weight.NORMAL.getValue());
		this.globalTag.setWeight(Weight.intern(weight));
		
		// Function syntax highlighting
		r = currentNode.getInt(Functions.R, Functions.R_DEFAULT);
		g = currentNode.getInt(Functions.G, Functions.G_DEFAULT);
		b = currentNode.getInt(Functions.B, Functions.B_DEFAULT);
		this.functionTag.setForeground(ColorConverter.colorToHexString(new Color(r,g,b)));
		weight = currentNode.getInt(Functions.WEIGHT, Weight.BOLD.getValue());
		this.functionTag.setWeight(Weight.intern(weight));
		
		// comment syntax highlighting
		r = currentNode.getInt(Comments.R, Comments.R_DEFAULT);
		g = currentNode.getInt(Comments.G, Comments.G_DEFAULT);
		b = currentNode.getInt(Comments.B, Comments.B_DEFAULT);
		this.commentTag.setForeground(ColorConverter.colorToHexString(new Color(r,g,b)));
		weight = currentNode.getInt(Comments.WEIGHT, Weight.NORMAL.getValue());
		this.commentTag.setWeight(Weight.intern(weight));
		
		// class identifier highlighting
		r = currentNode.getInt(Classes.R, Classes.R_DEFAULT);
		g = currentNode.getInt(Classes.G, Classes.G_DEFAULT);
		b = currentNode.getInt(Classes.B, Classes.B_DEFAULT);
		this.classTag.setForeground(ColorConverter.colorToHexString(new Color(r,g,b)));
		weight = currentNode.getInt(Classes.WEIGHT, Weight.BOLD.getValue());
		this.classTag.setWeight(Weight.intern(weight));
		
		// Inlined tag background
		r = currentNode.getInt(Inline.R, Inline.R_DEFAULT);
		g = currentNode.getInt(Inline.G, Inline.G_DEFAULT);
		b = currentNode.getInt(Inline.B, Inline.B_DEFAULT);
		this.inlinedTag.setBackground(ColorConverter.colorToHexString(new Color(r,g,b)));
	}
	
	/**
	 * Find the next instance of toFind in the buffer. If findAll is set, all
	 * instances will be highlighted, otherwise will return after the first.
	 * 
	 * @param toFind The string to locate
	 * @param matchCase Whether to do a case sensitive search
	 * @param findAll Whether to locate all instances
	 * @return Whether the search was successful
	 */
	public boolean findNext(String toFind, boolean matchCase, boolean findAll){
		this.checkReset(toFind, matchCase);
		
		if(this.startCurrentFind == null || findAll){
			this.startCurrentFind = this.getStartIter();
			this.endCurrentFind = this.getStartIter();
		}
		
		// clear the already found work
		this.removeTag(FOUND_TEXT, this.getStartIter(), this.getEndIter());
		
		// Work through the whole buffer from the start
		for(int i = this.endCurrentFind.getLineNumber(); i < this.getLineCount(); i++){
			TextIter currentLine;
			/*
			 * If we're on the same line as the last search and the last search doesn't end the line,
			 * get the remainder of that line. If it does end the line, continue to the next one
			 */
			if(i == this.endCurrentFind.getLineNumber())
				if(!this.endCurrentFind.getEndsLine())
					currentLine = this.getIter(i, this.endCurrentFind.getLineOffset());
				else
					continue;
			else{
				currentLine = this.getIter(i, 0);
			}
			
			int lineLength = this.getText(currentLine, this.getEndIter(), true).indexOf("\n");
			
			// Last line will have no newline at the end, use length of remaining buffer
			if(lineLength == -1)
				lineLength = this.getText(currentLine, this.getEndIter(), true).length();
			
			// searching empty lines is pointless, move on
			if(lineLength == 0)
				continue;
			
			TextIter endLine = this.getIter(i, currentLine.getLineOffset() + lineLength);
			
			String lineText = this.getText(currentLine, endLine, true);
			
			int index;
			if(matchCase)
				index = lineText.indexOf(toFind);
			else
				index = lineText.toLowerCase().indexOf(toFind.toLowerCase());
			
			// If the string exists, mark it, set the TextIters to point to it and scroll to the position
			if(index != -1){
				this.startCurrentFind = this.getIter(i, index + currentLine.getLineOffset());
				this.endCurrentFind = this.getIter(i, index + toFind.length() + currentLine.getLineOffset());
				
				this.applyTag(FOUND_TEXT, this.startCurrentFind, this.endCurrentFind);
				// Scroll only if we aren't in 'highlight all' mode
				
				// Search the rest of the line for multiple occurances
				if(findAll){
					String rest = lineText.substring(index, lineText.length());
					int before = index;
					index = rest.indexOf(toFind);
					
					// look for other matches on the same line
					while(rest.length() > 0 && index != -1){
						this.startCurrentFind = this.getIter(i, before + index);
						this.endCurrentFind = this.getIter(i, before + index + toFind.length());
						
						this.applyTag(FOUND_TEXT, this.startCurrentFind, this.endCurrentFind);
						
						rest = rest.substring(index + toFind.length(), rest.length());
						before += index + toFind.length();
						
						if(matchCase)
							index = rest.indexOf(toFind);
						else
							index = rest.toLowerCase().indexOf(toFind.toLowerCase());
					}
				}
				else{
					return true;
				}
			}
		}
		
		// For finding everything, getting this far means success
		if(findAll){
			this.startCurrentFind = this.getStartIter();
			this.endCurrentFind = this.getStartIter();
			
			return true;
		}
		// If we're only trying to find one, getting this far is bad
		else{
			this.startCurrentFind = this.getEndIter();
			this.endCurrentFind = this.getEndIter();
			
			return false;
		}
	}
	
	/**
	 * Finds the previous entry of toFind in the buffer and highlights it.
	 * Returns true if successful, false otherwise
	 * 
	 * @param toFind The string to find
	 * @param matchCase Whether to do a case sensitive search
	 * @return Whether the search was successful
	 */
	public boolean findPrevious(String toFind, boolean matchCase){
		this.checkReset(toFind, matchCase);
		
		if(this.startCurrentFind == null){
			this.startCurrentFind = this.getEndIter();
			this.endCurrentFind = this.getEndIter();
		}
		
		this.removeTag(FOUND_TEXT, this.getStartIter(), this.getEndIter());
		
		for(int i = this.endCurrentFind.getLineNumber(); i >= 0; i--){
			
			TextIter currentLine = this.getIter(i, 0);
			
			int lineLength;
			if(i == this.endCurrentFind.getLineNumber())
				lineLength = this.getText(currentLine, this.startCurrentFind, true).length();
			else
				lineLength = this.getText(currentLine, this.getEndIter(), true).indexOf("\n");
			
			if(lineLength == -1)
				lineLength = this.getText(currentLine, this.getEndIter(), true).length();
			
			if(lineLength == 0)
				continue;
			
			TextIter endLine = this.getIter(i, currentLine.getLineOffset() + lineLength);
			
			String lineText = this.getText(currentLine, endLine, true);
			
			int index;
			if(matchCase)
				index = lineText.lastIndexOf(toFind);
			else
				index = lineText.toLowerCase().lastIndexOf(toFind.toLowerCase());
			
			if(index != -1){
				this.startCurrentFind = this.getIter(i, currentLine.getLineOffset() + index);
				this.endCurrentFind = this.getIter(i, currentLine.getLineOffset() + index + toFind.length());
				
				this.applyTag(FOUND_TEXT, this.startCurrentFind, this.endCurrentFind);
				
				return true;
			}
		}
		
		// set iters to the start of the text
		this.startCurrentFind = this.getStartIter();
		this.endCurrentFind = this.getStartIter();
		
		return false;
	}
	
	/**
	 * @return The TextIter at the start of the currently searched for string
	 */
	public TextIter getStartCurrentFind() {
		return startCurrentFind;
	}
	
	/**
	 * Searches for a variable at the location specified by iter, and returns a VariableLocation
	 * if a variable was found there. Returns null otherwise
	 * @param iter The location to look for a variable
	 * @return The variable at that location, or null
	 */
	public Variable getVariable(TextIter iter){
		return this.varList.getVariable(iter.getLineNumber(), iter.getLineOffset());
	}
	
	/**
	 * @return The list of functions found by the parser
	 */
	public Vector getFunctions() {
		return functions;
	}
	
	/**
	 * Adds a function to the buffer to be highlighted. If declaration is set the function
	 * will be added to the list of functions that the user can jump to using the menu.
	 * Note that this function differens from addFunction(String name, int offset, boolean declaration)
	 * in that offset is from the start of lineNum, not from the start of the file
	 * 
	 * @param name The name of the function
	 * @param lineNum The line the function occurs on
	 * @param col The offset from the start of the line
	 * @param declaration Whether this is a declaration
	 */
	public void addFunction(String name, int lineNum, int col, boolean declaration){
		this.applyTag(FUNCTION_TAG, this.getIter(lineNum, col), this.getIter(lineNum, col+name.length()));
		if(declaration){
			this.createMark(name+"_FUNC", this.getIter(lineNum, col), true);
			this.functions.add(name+"_FUNC");
		}
	}
	
	/**
	 * Adds a function to the buffer to be highlighted. If declaration is set the function
	 * will be added to the list of functions that the user can jump to using the menu. Note
	 * that this function differs from addFunction(String name, int lineNum, int col, boolean declaration)
	 * in that offset is the offset from the beginning of the file (in characters), not the start of
	 * the current line
	 * 
	 * @param name The name of the function
	 * @param offset The offset from the start of the file
	 * @param declaration Whether the function is a declaration
	 */
	public void addFunction(String name, int offset, boolean declaration){
		this.applyTag(FUNCTION_TAG, this.getIter(offset), this.getIter(offset+name.length()));
		if(declaration){
			this.createMark(name+"_FUNC", this.getIter(offset), true);
			this.functions.add(name+"_FUNC");
		}
	}
	
	/**
	 * Adds a variable to be highlighted in the window
	 * @param v The variable to be highlighted
	 */
	public void addVariable(Variable v){
		if(!v.isMember())
			this.applyTag(ID_TAG, this.getIter(v.getCol()), this.getIter(v.getCol()+v.getName().length()));
		else
			this.applyTag(MEMBER_TAG, this.getIter(v.getCol()), this.getIter(v.getCol()+v.getName().length()));
		this.varList.addVariable(v);
	}
	
	/**
	 * Adds a literal to be highlighted in the text
	 * 
	 * @param lineNum The line number
	 * @param col The offset from the start of the line
	 * @param length The length of the literal
	 */
	public void addLiteral(int lineNum, int col, int length){
		this.applyTag(KEYWORD_TAG, this.getIter(lineNum, col), this.getIter(lineNum, col+length));
	}
	
	/**
	 * Adds a literal to be highlighted in the text
	 * 
	 * @param offset The offset from the start of the file
	 * @param length The length of the literal
	 */
	public void addLiteral(int offset, int length){
		this.applyTag(KEYWORD_TAG, this.getIter(offset), this.getIter(offset+length));
	}
	
	/**
	 * Adds a comment to be highlighted in the text
	 * 
	 * @param lineStart The line the comment starts on
	 * @param colStart The offset from the the start of the line that the comment starts on
	 * @param lineEnd The line the comment ends on
	 * @param colEnd the offset from the start of the line that the comment ends on
	 */
	public void addComment(int lineStart, int colStart, int lineEnd, int colEnd){
		this.applyTag(COMMENT_TAG, this.getIter(lineStart, colStart), this.getIter(lineEnd, colEnd));
	}
	
	/**
	 * Adds a class identifier to be highlighted in the text
	 * 
	 * @param offset The offset from the beginning of the file
	 * @param length The length of the identifier
	 */
	public void addClass(int offset, int length){
		this.applyTag("CLASS", this.getIter(offset), this.getIter(offset+length));		
	}
	
	/**
	 * Returns true if the given line contains inlined code
	 * 
	 * @param lineNum The line to check
	 * @return True if the line has inlined code, false otherwise
	 */
	public boolean hasInlinedLines(int lineNum){
		return ((SourceCodeLine) this.lines.elementAt(lineNum)).hasInlineLines();
	}
	
	/**
	 * Returns true if the given line contains inlined code and is expanded. If the
	 * given line doesn't have inlined code false is returned by default
	 * 
	 * @param lineNum The line to check
	 * @return True if the the line contains expanded inline code, false otherwise
	 */
	public boolean isExpanded(int lineNum){
		return ((SourceCodeLine) this.lines.elementAt(lineNum)).isExpanded();
	}
	
	/**
	 * Toggles the expanded state of the provided line. If the line doesn have inlined
	 * code this call has no effect 
	 * 
	 * @param lineNum The line to expand inlined code in
	 */
	
	public void toggleExpanded(int lineNum){
		SourceCodeLine line = (SourceCodeLine) this.lines.elementAt(lineNum);
		if(!line.hasInlineLines())
			return;
		
		line.setExpanded(!line.isExpanded());
		
		TextIter insertPoint = this.getLineIter(lineNum+1);
		
		if(line.isExpanded()){	
			Iterator i = line.getInlineLines();
			String toInsert = "";
			while(i.hasNext())
				toInsert += ((String) i.next())+"\n";
			
			this.insertText(insertPoint, toInsert);
			
			// insertPoint's been invalidated by the insertion, re-create it to add
			// the highlighting
			insertPoint = this.getLineIter(lineNum+1);
			TextIter end = this.getIter(insertPoint.getOffset()+toInsert.length());
			this.applyTag(INLINE_TAG, insertPoint, end);
		}
		else{
			TextIter end = this.getLineIter(lineNum+line.getNumInlinedLines()+1);
			this.removeTag(INLINE_TAG, insertPoint, end);
			this.deleteText(insertPoint, end);
		}
	}
	
	/**
	 * Returns the number of inlined lines for the gives source line.
	 * 
	 * @param lineNum The line to get info for
	 * @return The number of lines of inlined code
	 */
	public int getNumInlinedLines(int lineNum){
		return ((SourceCodeLine) this.lines.elementAt(lineNum)).getNumInlinedLines();
	}
	
	/**
	 * Returns the number of lines in the given file, with the option to include
	 * inlined code in that count
	 * 
	 * @param includeInlindes Whether to include inlined code in the line count
	 * @return The number of lines in the file
	 */
	public int getLineCount(boolean includeInlindes){
		if(includeInlindes)
			return this.getLineCount();
		else
			return this.lines.size();
	}
	
	/*-------------------*
	 * PRIVATE METHODS   *
	 *-------------------*/
	
	/*
	 * Checks whether or not the current search should be restarted by comparing the
	 * string last searched with the new string
	 */
	private void checkReset(String newString, boolean matchCase){
		if(this.startCurrentFind == null)
			return;
		
		String found = this.getText(this.startCurrentFind, this.endCurrentFind, false);

		if(matchCase && (found.equals("") || !found.equals(newString))){
			this.startCurrentFind = null;
			this.endCurrentFind = null;
		}
		else if(found.equals("") || !found.equalsIgnoreCase(newString)){
			this.startCurrentFind = null;
			this.endCurrentFind = null;
		}
	}
	
	/** 
	 * A simple parser that reads in a file, and makes up SourceCodeLine
	 * objects from it. These objects have a 'breakable' property which
	 * indicates that a breakline can be set on that line, it is set to
	 * true if the line is not empty. 
	 *  
	 * @author ifoox
	 *
	 */
	protected class SourceLineReader {
		private Vector sourceLines;
		
		private String fileName;
		
		private BufferedReader fileReader;
		
		public SourceLineReader(String filename) throws FileNotFoundException{
			this.fileName = filename;
			sourceLines = new Vector();
			
			fileReader = new BufferedReader(new FileReader(new File(fileName)));
			
		}
		
		/**
		 * 
		 * Returns a SourceCodeLine object representing
		 * the next source code line.
		 * @return
		 */
		public SourceCodeLine getNextLine(){
			SourceCodeLine codeLine = null;
			try {
				String line = fileReader.readLine();
				if (line != null){
					codeLine = new SourceCodeLine(line, false);
					sourceLines.add(codeLine);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return codeLine;
		}
		
		public int lineNumber(){
			return sourceLines.size() - 1;
		}
		
		public SourceCodeLine getLine(int lineNo){
			return (SourceCodeLine) sourceLines.get(lineNo);
		}
		
	}

}
