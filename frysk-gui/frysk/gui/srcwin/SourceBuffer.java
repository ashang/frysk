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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import org.gnu.glib.JGException;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextChildAnchor;
import org.gnu.gtk.TextIter;
import org.gnu.gtk.TextMark;
import org.gnu.gtk.TextTag;
import org.jdom.Element;

import frysk.dom.DOMInlineInstance;
import frysk.dom.DOMLine;
import frysk.dom.DOMTag;
import frysk.dom.DOMTagTypes;
import frysk.gui.srcwin.cparser.CDTParser;
import frysk.gui.srcwin.prefs.BooleanPreference;
import frysk.gui.srcwin.prefs.ColorPreference;
import frysk.gui.srcwin.prefs.PreferenceManager;
import frysk.gui.srcwin.prefs.SyntaxPreference;

/**
 * This class is a wrapper around TextBuffer, it allows for extra functionality
 * needed by the SourceViewWidget.
 * 
 * It will need to be retrofited to use correct model objects once these are
 * set, instead of SourceCodeLine and so on.
 * 
 * @author ifoox
 * 
 */
public class SourceBuffer extends TextBuffer {

	protected static final String CLASS_TAG = "CLASS";

	/* CONSTANTS */
	protected static final String INLINE_TAG = "INLINE";
	protected static final String COMMENT_TAG = "COMMENT";
	protected static final String MEMBER_TAG = "MEMBER";
	protected static final String FUNCTION_TAG = "FUNCTION";
	protected static final String ID_TAG = "ID";
	protected static final String OPTIMIZED_VAR_TAG = "OPTIMIZED_VAR";
	protected static final String OUT_OF_SCOPE_VAR_TAG = "OOS_VAR";
	protected static final String KEYWORD_TAG = "TYPE";
	protected static final String CURRENT_LINE = "currentLine";
	protected static final String FOUND_TEXT = "foundText";

	/* END CONSTANTS */

	private Vector functions;
	private TextMark startCurrentLine;
	private TextMark endCurrentLine;
	private TextIter startCurrentFind;
	private TextIter endCurrentFind;
	private TextTag currentLine;
	private TextTag foundText;
	private TextTag functionTag;
	private TextTag variableTag;
	private TextTag globalTag;
	private TextTag keywordTag;
//	private TextTag commentTag;
	private TextTag classTag;
	private TextTag optimizedVarTag;
	private TextTag oosVarTag;

	private StaticParser staticParser;

	// Since conceptually each sourcebuffer will only be viewing one file, we
	// don't
	// need any information higher than this
	protected StackLevel scope;

	protected TextChildAnchor anchor;

	private boolean showingAssembly = false;
	
	/**
	 * Creates a new SourceBuffer
	 */
	public SourceBuffer() {
		super();
		this.init();
	}

	/**
	 * Creates a new SourceBuffer with the given scope
	 * 
	 * @param scope
	 */
	public SourceBuffer(StackLevel scope) {
		this();
		this.setScope(scope, false);
	}

	/**
	 * Returns true if the given line is executable.
	 * 
	 * @param lineNo
	 *            The line to check
	 * @return Whether or not the line is executable
	 */
	public boolean isLineExecutable(int lineNo) {
		if(showingAssembly)
			return lineNo <= this.getLineCount();
		
		DOMLine line = this.scope.getData().getLine(lineNo + 1);
		if (line == null)
			return false;
		return line.isExecutable();
	}

	/**
	 * Returns true if the given line has a breakpoint set.
	 * 
	 * @param lineNo
	 * @return
	 */
	public boolean isLineBroken(int lineNo) {
		// TODO: How do we deal with breakpoints for assembly code?
		if(showingAssembly)
			return false;
		
		DOMLine line = this.scope.getData().getLine(lineNo + 1);
		if (line == null)
			return false;

		return line.hasBreakPoint();
	}

	/**
	 * Toggles the breakpoint on line lineNum, returning the previous state of
	 * the breakpoint on that line.
	 * 
	 * @param lineNum
	 *            the number of the line to toggle the breakpoint for, starting
	 *            at 0
	 * 
	 * @return true if breakpoint was previously set on lineNum, false otherwise
	 * 
	 * @throws ArrayIndexOutOfBoundsException
	 *             thrown if the line number given does not correspond to a
	 *             valid line number
	 */
	public boolean toggleBreakpoint(int lineNum) {
		// TODO: Assembly breakpoints?
		if(showingAssembly)
			return false;
		
		if (!this.isLineExecutable(lineNum))
			return false;

		DOMLine line = this.scope.getData().getLine(lineNum + 1);
		if (line == null)
			return false;

		boolean status = line.hasBreakPoint();
		line.setBreakPoint(!status);
		return !status;
	}

	/**
	 * @return The current PC line
	 */
	public int getCurrentLine() {
		if (this.startCurrentLine == null)
			return -1;

		return this.getIter(this.startCurrentLine).getLineNumber();
	}

	/**
	 * Sets the current 'line' to the given range
	 * 
	 * @param startLine
	 *            The line the current instruction starts on
	 * @param startCol
	 *            The offset (wrt. the start of the line) that the instruction
	 *            starts on
	 * @param endLine
	 *            The line the current instruction ends on
	 * @param endCol
	 *            The offset (wrt. the start of the line) that the instruction
	 *            ends on
	 */
	protected void setCurrentLine(int startLine, int startCol, int endLine,
			int endCol) {

		this.startCurrentLine = this.createMark("currentLineStart",
				this.getIter(this.getLineIter(startLine - 1).getOffset()
						+ startCol), true);
		if (endCol != StackLevel.EOL) {
			this.endCurrentLine = this.createMark("currentLineEnd",
					this.getIter(this.getLineIter(endLine - 1).getOffset()
							+ endCol), false);
		} else {
			TextIter lineStart = this.getLineIter(endLine - 1);
			this.endCurrentLine = this.createMark("currentLineEnd",
					this.getIter(lineStart.getOffset()
							+ lineStart.getCharsInLine()), true);
		}

		this.applyTag(this.currentLine, this.getIter(this.startCurrentLine),
				this.getIter(this.endCurrentLine));
	}

	/**
	 * Find the next instance of toFind in the buffer. If findAll is set, all
	 * instances will be highlighted, otherwise will return after the first.
	 * 
	 * @param toFind
	 *            The string to locate
	 * @param matchCase
	 *            Whether to do a case sensitive search
	 * @param findAll
	 *            Whether to locate all instances
	 * @return Whether the search was successful
	 */
	public boolean findNext(String toFind, boolean matchCase, boolean findAll) {

		this.checkReset(toFind, matchCase);

		if (this.startCurrentFind == null || findAll) {
			this.startCurrentFind = this.getStartIter();
			this.endCurrentFind = this.getStartIter();
		}

		// clear the already found work
		this.removeTag(FOUND_TEXT, this.getStartIter(), this.getEndIter());

		// Work through the whole buffer from the start
		for (int i = this.endCurrentFind.getLineNumber(); i < this
				.getLineCount(); i++) {
			TextIter currentLine;
			/*
			 * If we're on the same line as the last search and the last search
			 * doesn't end the line, get the remainder of that line. If it does
			 * end the line, continue to the next one
			 */
			if (i == this.endCurrentFind.getLineNumber())
				if (!this.endCurrentFind.getEndsLine())
					currentLine = this.getIter(i, this.endCurrentFind
							.getLineOffset());
				else
					continue;
			else {
				currentLine = this.getIter(i, 0);
			}

			int lineLength = this.getText(currentLine, this.getEndIter(), true)
					.indexOf("\n");

			// Last line will have no newline at the end, use length of
			// remaining buffer
			if (lineLength == -1)
				lineLength = this.getText(currentLine, this.getEndIter(), true)
						.length();

			// searching empty lines is pointless, move on
			if (lineLength == 0)
				continue;

			TextIter endLine = this.getIter(i, currentLine.getLineOffset()
					+ lineLength);

			String lineText = this.getText(currentLine, endLine, true);

			int index;
			if (matchCase)
				index = lineText.indexOf(toFind);
			else
				index = lineText.toLowerCase().indexOf(toFind.toLowerCase());

			// If the string exists, mark it, set the TextIters to point to it
			// and scroll to the position
			if (index != -1) {
				this.startCurrentFind = this.getIter(i, index
						+ currentLine.getLineOffset());
				this.endCurrentFind = this.getIter(i, index + toFind.length()
						+ currentLine.getLineOffset());

				this.applyTag(FOUND_TEXT, this.startCurrentFind,
						this.endCurrentFind);
				// Scroll only if we aren't in 'highlight all' mode

				// Search the rest of the line for multiple occurances
				if (findAll) {
					String rest = lineText.substring(index, lineText.length());
					int before = index;
					index = rest.indexOf(toFind);

					// look for other matches on the same line
					while (rest.length() > 0 && index != -1) {
						this.startCurrentFind = this.getIter(i, before + index);
						this.endCurrentFind = this.getIter(i, before + index
								+ toFind.length());

						this.applyTag(FOUND_TEXT, this.startCurrentFind,
								this.endCurrentFind);

						rest = rest.substring(index + toFind.length(), rest
								.length());
						before += index + toFind.length();

						if (matchCase)
							index = rest.indexOf(toFind);
						else
							index = rest.toLowerCase().indexOf(
									toFind.toLowerCase());
					}
				} else {
					return true;
				}
			}
		}

		// For finding everything, getting this far means success
		if (findAll) {
			this.startCurrentFind = this.getStartIter();
			this.endCurrentFind = this.getStartIter();

			return true;
		}
		// If we're only trying to find one, getting this far is bad
		else {
			this.startCurrentFind = this.getEndIter();
			this.endCurrentFind = this.getEndIter();

			return false;
		}
	}

	/**
	 * Finds the previous entry of toFind in the buffer and highlights it.
	 * Returns true if successful, false otherwise
	 * 
	 * @param toFind
	 *            The string to find
	 * @param matchCase
	 *            Whether to do a case sensitive search
	 * @return Whether the search was successful
	 */
	public boolean findPrevious(String toFind, boolean matchCase) {
		this.checkReset(toFind, matchCase);

		if (this.startCurrentFind == null) {
			this.startCurrentFind = this.getEndIter();
			this.endCurrentFind = this.getEndIter();
		}

		this.removeTag(FOUND_TEXT, this.getStartIter(), this.getEndIter());

		for (int i = this.endCurrentFind.getLineNumber(); i >= 0; i--) {

			TextIter currentLine = this.getIter(i, 0);

			int lineLength;
			if (i == this.endCurrentFind.getLineNumber())
				lineLength = this.getText(currentLine, this.startCurrentFind,
						true).length();
			else
				lineLength = this.getText(currentLine, this.getEndIter(), true)
						.indexOf("\n");

			if (lineLength == -1)
				lineLength = this.getText(currentLine, this.getEndIter(), true)
						.length();

			if (lineLength == 0)
				continue;

			TextIter endLine = this.getIter(i, currentLine.getLineOffset()
					+ lineLength);

			String lineText = this.getText(currentLine, endLine, true);

			int index;
			if (matchCase)
				index = lineText.lastIndexOf(toFind);
			else
				index = lineText.toLowerCase()
						.lastIndexOf(toFind.toLowerCase());

			if (index != -1) {
				this.startCurrentFind = this.getIter(i, currentLine
						.getLineOffset()
						+ index);
				this.endCurrentFind = this.getIter(i, currentLine
						.getLineOffset()
						+ index + toFind.length());

				this.applyTag(FOUND_TEXT, this.startCurrentFind,
						this.endCurrentFind);

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
	 * Searches for a variable at the location specified by iter, and returns a
	 * VariableLocation if a variable was found there. Returns null otherwise
	 * 
	 * @param iter
	 *            The location to look for a variable
	 * @return The variable at that location, or null
	 */
	public Variable getVariable(TextIter iter) {
		if(showingAssembly)
			return null;
		
		DOMLine line = this.scope.getData().getLine(iter.getLineNumber() + 1);
		DOMTag tag = line.getTag(iter.getLineOffset());

		// No var (or no tag), do nothing
		if (tag == null || !tag.getType().equals(DOMTagTypes.LOCAL_VAR))
			return null;

		Variable var = new Variable(line.getText().substring(tag.getStart(),
				tag.getStart() + tag.getLength()), iter.getLineNumber(), tag
				.getStart(), false);
		return var;
	}

	/**
	 * @return The list of functions found by the parser
	 */
	public Vector getFunctions() {
		return functions;
	}

	/**
	 * Adds a function to the buffer to be highlighted. If declaration is set
	 * the function will be added to the list of functions that the user can
	 * jump to using the menu. Note that this function differens from
	 * addFunction(String name, int offset, boolean declaration) in that offset
	 * is from the start of lineNum, not from the start of the file
	 * 
	 * @param name
	 *            The name of the function
	 * @param lineNum
	 *            The line the function occurs on
	 * @param col
	 *            The offset from the start of the line
	 * @param declaration
	 *            Whether this is a declaration
	 */
	public void addFunction(String name, int lineNum, int col,
			boolean declaration) {
		this.addFunction(name, this.getLineIter(lineNum).getOffset() + col,
				declaration);
	}

	/**
	 * Adds a function to the buffer to be highlighted. If declaration is set
	 * the function will be added to the list of functions that the user can
	 * jump to using the menu. Note that this function differs from
	 * addFunction(String name, int lineNum, int col, boolean declaration) in
	 * that offset is the offset from the beginning of the file (in characters),
	 * not the start of the current line
	 * 
	 * @param name
	 *            The name of the function
	 * @param offset
	 *            The offset from the start of the file
	 * @param declaration
	 *            Whether the function is a declaration
	 */
	public void addFunction(String name, int offset, boolean declaration) {
		DOMLine line = this.scope.getData().getLine(
				this.getIter(offset).getLineNumber() + 1);
		if (!declaration)
			line.addTag(DOMTagTypes.FUNCTION, name, this.getIter(offset)
					.getLineOffset());
		else
			line.addTag(DOMTagTypes.FUNCTION_BODY, name, this.getIter(offset)
					.getLineOffset());

	}

	/**
	 * Adds a variable to be highlighted in the window
	 * 
	 * @param offset
	 *            The offset from the start of the file of the variable
	 * @param length
	 *            The length of the variable name
	 */
	public void addVariable(int offset, int length) {
		DOMLine line = this.scope.getData().getLine(
				this.getIter(offset).getLineNumber() + 1);
		/* TODO: Don't hardcode this, we need a way to tell what type of
		 * variable this is */
		if (line.getLineNum() == 21)
			line.addTag(DOMTagTypes.LOCAL_VAR, this.getText(this
					.getIter(offset), this.getIter(offset + length), true),
					this.getIter(offset).getLineOffset());
		else if (line.getLineNum() < 20)
			line.addTag(DOMTagTypes.OUT_OF_SCOPE_VAR, this.getText(this
					.getIter(offset), this.getIter(offset + length), true),
					this.getIter(offset).getLineOffset());
		else
			line.addTag(DOMTagTypes.OPTIMIZED_VAR, this.getText(this
					.getIter(offset), this.getIter(offset + length), true),
					this.getIter(offset).getLineOffset());
	}

	public void addVariable(int lineNum, int lineOffset, int length) {
		this.addVariable(this.getLineIter(lineNum).getOffset() + lineOffset,
				length);
	}

	/**
	 * Adds a literal to be highlighted in the text
	 * 
	 * @param lineNum
	 *            The line number
	 * @param col
	 *            The offset from the start of the line
	 * @param length
	 *            The length of the literal
	 */
	public void addKeyword(int lineNum, int col, int length) {
		DOMLine line = this.scope.getData().getLine(lineNum + 1);
		line.addTag(DOMTagTypes.KEYWORD, this.getText(this
				.getIter(lineNum, col), this.getIter(lineNum, col + length),
				false), col);
	}

	/**
	 * Adds a literal to be highlighted in the text
	 * 
	 * @param offset
	 *            The offset from the start of the file
	 * @param length
	 *            The length of the literal
	 */
	public void addKeyword(int offset, int length) {
		DOMLine line = this.scope.getData().getLine(
				this.getIter(offset).getLineNumber() + 1);
		line.addTag(DOMTagTypes.KEYWORD, this.getText(this.getIter(offset),
				this.getIter(offset + length), true), this.getIter(offset)
				.getLineOffset());
	}

	/**
	 * Adds a comment to be highlighted in the text
	 * 
	 * @param lineStart
	 *            The line the comment starts on
	 * @param colStart
	 *            The offset from the the start of the line that the comment
	 *            starts on
	 * @param lineEnd
	 *            The line the comment ends on
	 * @param colEnd
	 *            the offset from the start of the line that the comment ends on
	 * 
	 * TODO: right now nothing calls this, the CDTParser can't find comments
	 */
	public void addComment(int lineStart, int colStart, int lineEnd, int colEnd) {
		// this.applyTag(COMMENT_TAG, this.getIter(lineStart, colStart),
		// this.getIter(lineEnd, colEnd));
	}

	/**
	 * Adds a class identifier to be highlighted in the text
	 * 
	 * @param offset
	 *            The offset from the beginning of the file
	 * @param length
	 *            The length of the identifier
	 */
	public void addClass(int offset, int length) {
		DOMLine line = this.scope.getData().getLine(
				this.getIter(offset).getLineNumber() + 1);
		line.addTag(DOMTagTypes.CLASS_DECL, this.getText(this.getIter(offset),
				this.getIter(offset + length), true), this.getIter(offset)
				.getLineOffset());
	}

	/**
	 * Returns the number of lines in the given file, with the option to include
	 * inlined code in that count
	 * 
	 * @param includeInlindes
	 *            Whether to include inlined code in the line count
	 * @return The number of lines in the file
	 */
	public int getLineCount() {
		if(!showingAssembly)
			return this.scope.getData().getLineCount();
		else
			return this.getEndIter().getLineNumber();
	}

	/**
	 * @return The number of the last line in the buffer, which may or may not
	 *         be the same as the number of lines in the buffer due to an
	 *         initial offset
	 */
	public int getLastLine() {
		return this.getLineCount();
	}

	/**
	 * @param lineNumber
	 *            the line to check
	 * @return true iff the given line has inlined code
	 */
	public boolean hasInlineCode(int lineNumber) {
		// TODO: Inline code with assembly?
		if(showingAssembly)
			return false;
		
		return this.scope.getData().getLine(lineNumber + 1).hasInlinedCode();
	}

	/**
	 * 
	 * @param lineNumber
	 *            The line number to get Inline information from
	 * @return The DOMInlineInstance containing the inlined code information, or
	 *         null if no information exists
	 */
	public DOMInlineInstance getInlineInstance(int lineNumber) {
		Iterator iter = this.scope.getData().getLine(lineNumber + 1)
				.getInlines();
		if (!iter.hasNext())
			return null;

		return new DOMInlineInstance((Element) iter.next());
	}

	/**
	 * Sets the scope that will be displayed by this buffer to the provided
	 * scope.
	 * 
	 * @param scope
	 *            The stack frame to be displayed
	 * @param isAssembly TODO
	 */
	public void setScope(StackLevel scope, boolean isAssembly) {
		for (int i = 0; i < functions.size(); i++)
			if(this.markExists((String) functions.get(i)))
				this.deleteMark(((String) functions.get(i)));

		this.showingAssembly = isAssembly;
		
		this.anchor = null;
		this.functions = new Vector();
		this.scope = scope;
		try {
			if(!isAssembly)
				this.loadFile();
			else
				this.loadAssembly();
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.setCurrentLine(this.scope.getStartingLineNum(), this.scope
						.getColStart(), this.scope.getEndLine(), this.scope
						.getColEnd());
	}

	/**
	 * Creates the anchor at the current line that will to which the inlined
	 * code will be attached. If a previous anchor exists it will be overridden.
	 * 
	 * @return The created anchor.
	 */
	public TextChildAnchor createAnchorAtCurrentLine() {
		TextIter line = this.getLineIter(this.getCurrentLine() + 1);

		if (this.anchor != null)
			this.deleteText(line, this.getIter(line.getOffset() + 1));
		else
			this.insertText(line, "\n");
		this.anchor = this.createChildAnchor(this.getLineIter(this
				.getCurrentLine() + 1));

		return this.anchor;
	}

	/**
	 * Removed the anchor created by
	 * {@link SourceBuffer#createAnchorAtCurrentLine()}
	 * 
	 */
	public void clearAnchorAtCurrentLine() {
		// do nothing if there's nothing to clear
		if (this.anchor == null)
			return;

		TextIter line = this.getLineIter(this.getCurrentLine() + 1);
		this.deleteText(line, this.getIter(line.getOffset() + 2));

		this.anchor = null;
	}

	/**
	 * 
	 * @return The stack frame currently being displayed
	 */
	public StackLevel getScope() {
		return scope;
	}

	/*-------------------*
	 * PRIVATE METHODS   *
	 *-------------------*/

	/*
	 * Initializes the tags, loads the file from the current scope, and sets the
	 * current line.
	 */
	private void init() {
		functions = new Vector();
		this.currentLine = this.createTag(CURRENT_LINE);
		this.foundText = this.createTag(FOUND_TEXT);
		this.functionTag = this.createTag(FUNCTION_TAG);
		this.variableTag = this.createTag(ID_TAG);
		this.keywordTag = this.createTag(KEYWORD_TAG);
		this.globalTag = this.createTag(MEMBER_TAG);
//		this.commentTag = this.createTag(COMMENT_TAG);
		this.classTag = this.createTag(CLASS_TAG);
		this.optimizedVarTag = this.createTag(OPTIMIZED_VAR_TAG);
		this.oosVarTag = this.createTag(OUT_OF_SCOPE_VAR_TAG);
		
		// We have to set this manually since this isn't controlled by the preferences
		this.optimizedVarTag.setStrikethrough(true);

		// Initialize preferences
		PreferenceManager.addPreference(new SyntaxPreference(
				SyntaxPreference.FUNCTIONS, this.functionTag),
				PreferenceManager.SYNTAX_NODE);
		PreferenceManager.addPreference(new SyntaxPreference(
				SyntaxPreference.VARIABLES, this.variableTag),
				PreferenceManager.SYNTAX_NODE);
		PreferenceManager.addPreference(new SyntaxPreference(
				SyntaxPreference.OPTIMIZED, this.optimizedVarTag),
				PreferenceManager.SYNTAX_NODE);
		PreferenceManager.addPreference(new SyntaxPreference(
				SyntaxPreference.OUT_OF_SCOPE, this.oosVarTag),
				PreferenceManager.SYNTAX_NODE);
		PreferenceManager.addPreference(new SyntaxPreference(
				SyntaxPreference.KEYWORDS, this.keywordTag),
				PreferenceManager.SYNTAX_NODE);
		PreferenceManager.addPreference(new SyntaxPreference(
				SyntaxPreference.GLOBALS, this.globalTag),
				PreferenceManager.SYNTAX_NODE);
		PreferenceManager.addPreference(new SyntaxPreference(
				SyntaxPreference.CLASSES, this.classTag),
				PreferenceManager.SYNTAX_NODE);

		PreferenceManager.addPreference(new BooleanPreference(
				BooleanPreference.LINE_NUMS), PreferenceManager.LNF_NODE);
		PreferenceManager.addPreference(new BooleanPreference(
				BooleanPreference.EXEC_MARKS), PreferenceManager.LNF_NODE);

		PreferenceManager.addPreference(new ColorPreference(
				ColorPreference.TEXT), PreferenceManager.LNF_NODE);
		PreferenceManager.addPreference(new ColorPreference(
				ColorPreference.BACKGROUND), PreferenceManager.LNF_NODE);
		PreferenceManager.addPreference(new ColorPreference(
				ColorPreference.LINE_NUMBER), PreferenceManager.LNF_NODE);
		PreferenceManager.addPreference(new ColorPreference(
				ColorPreference.MARGIN), PreferenceManager.LNF_NODE);
		PreferenceManager.addPreference(new ColorPreference(
				ColorPreference.CURRENT_LINE, false, this.currentLine),
				PreferenceManager.LNF_NODE);
		PreferenceManager.addPreference(new ColorPreference(
				ColorPreference.SEARCH, false, this.foundText),
				PreferenceManager.LNF_NODE);

	}

	/**
	 * Loads a file at the given location into the buffer, clearing any
	 * previously loaded files.
	 * 
	 * @param filename
	 *            The file to load
	 * @throws FileNotFoundException
	 * @throws JGException
	 */
	protected void loadFile() throws FileNotFoundException, JGException {
		Iterator lines = this.scope.getData().getLines();

		String bufferText = loadLines(lines);

		this.deleteText(this.getStartIter(), this.getEndIter());
		this.insertText(bufferText);

		if (!this.scope.isParsed()) {

			// now pass the resulting text to the parser
			if (this.staticParser == null)
				this.staticParser = new CDTParser();
			try {
				this.staticParser.parse(this, this.scope.getData()
						.getFilePath()
						+ "/" + this.scope.getData().getFileName());
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.scope.setParsed(true);
		}

		this.createTags();
	}
	
	protected void loadAssembly(){
		this.deleteText(this.getStartIter(), this.getEndIter());
		
		String text = "";
		
		for(int i = 10000; i < 10100; i++)
			text += "0x" + Integer.toHexString(i) + " mv $a0 $a1\n";
		
		this.insertText(text);
	}

	/**
	 * @param lines
	 *            Iterator of the lines to store in this SourceBuffer
	 * @return What should be the contents of the buffer
	 */
	protected String loadLines(Iterator lines) {
		String bufferText = "";

		// First get the text, append it all together, and add it to ourselves
		while (lines.hasNext()) {
			DOMLine line = new DOMLine((Element) lines.next());

			if (line.getText().equals(""))
				bufferText += "\n";
			else
				bufferText += line.getText();
		}
		return bufferText;
	}

	/*
	 * Reads through the DOM and creates all the tags necessary
	 */
	protected void createTags() {
		Iterator lines = this.scope.getData().getLines();

		// Iterate through all the lines
		while (lines.hasNext()) {
			DOMLine line = new DOMLine((Element) lines.next());

			Iterator tags = line.getTags();
			int lineOffset = line.getOffset();

			// Iterator though all the tags on the line
			while (tags.hasNext()) {
				DOMTag tag = new DOMTag((Element) tags.next());

				String type = tag.getType();

				if (type.equals(DOMTagTypes.KEYWORD)) {
					this.applyTag(KEYWORD_TAG, this.getIter(lineOffset
							+ tag.getStart()), this.getIter(lineOffset
							+ tag.getStart() + tag.getLength()));
				}

				else if (type.equals(DOMTagTypes.LOCAL_VAR)) {
					this.applyTag(ID_TAG, this.getIter(lineOffset
							+ tag.getStart()), this.getIter(lineOffset
							+ tag.getStart() + tag.getLength()));
				}

				else if (type.equals(DOMTagTypes.OPTIMIZED_VAR)) {
					this.applyTag(OPTIMIZED_VAR_TAG, this.getIter(lineOffset
							+ tag.getStart()), this.getIter(lineOffset
							+ tag.getStart() + tag.getLength()));
				}

				else if (type.equals(DOMTagTypes.OUT_OF_SCOPE_VAR)) {
					this.applyTag(OUT_OF_SCOPE_VAR_TAG, this.getIter(lineOffset
							+ tag.getStart()), this.getIter(lineOffset
							+ tag.getStart() + tag.getLength()));
				}

				else if (type.equals(DOMTagTypes.CLASS_DECL)) {
					this.applyTag(SourceBuffer.CLASS_TAG, this
							.getIter(lineOffset + tag.getStart()), this
							.getIter(lineOffset + tag.getStart()
									+ tag.getLength()));
				}

				else if (type.equals(DOMTagTypes.FUNCTION)) {
					this.applyTag(FUNCTION_TAG, this.getIter(lineOffset
							+ tag.getStart()), this.getIter(lineOffset
							+ tag.getStart() + tag.getLength()));
				}

				else if (type.equals(DOMTagTypes.FUNCTION_BODY)) {
					TextIter start = this.getIter(lineOffset + tag.getStart());
					TextIter end = this.getIter(lineOffset + tag.getStart()
							+ tag.getLength());
					String text = this.getText(start, end, false);
					this.applyTag(FUNCTION_TAG, start, end);
					this.createMark(text + "_FUNC", start, true);
					this.functions.add(text + "_FUNC");
				}

			} // end tags.hasNext()

			Iterator inlines = line.getInlines();

			while (inlines.hasNext()) {
				DOMInlineInstance func = new DOMInlineInstance(
						(Element) inlines.next());

				this.applyTag(FUNCTION_TAG, this.getIter(lineOffset
						+ func.getStart()), this.getIter(lineOffset
						+ func.getStart() + func.getEnd()));
			}
		}// end lines.hasNext()
	}

	/*
	 * Checks whether or not the current search should be restarted by comparing
	 * the string last searched with the new string
	 */
	private void checkReset(String newString, boolean matchCase) {
		if (this.startCurrentFind == null)
			return;

		String found = this.getText(this.startCurrentFind, this.endCurrentFind,
				false);

		if (matchCase && (found.equals("") || !found.equals(newString))) {
			this.startCurrentFind = null;
			this.endCurrentFind = null;
		} else if (found.equals("") || !found.equalsIgnoreCase(newString)) {
			this.startCurrentFind = null;
			this.endCurrentFind = null;
		}
	}

	// /*
	// * Generic method to take the given color from the preference model and
	// update the
	// * properties of the provided tag to match
	// */
	// private void updateTagColor(Preferences node, String prefix, Color
	// defaultColor, TextTag tag, boolean foreground){
	// int r = node.getInt(prefix+"_R", defaultColor.getRed());
	// int g = node.getInt(prefix+"_G", defaultColor.getGreen());
	// int b = node.getInt(prefix+"_B", defaultColor.getBlue());
	// if(foreground)
	// tag.setForeground(ColorConverter.colorToHexString(new Color(r,g,b)));
	// else
	// tag.setBackground(ColorConverter.colorToHexString(new Color(r,g,b)));
	// }
	//	
	// /*
	// * Gets the style from the preference model and uses it to update the
	// provided
	// * TextTag
	// */
	// private void updateTagStyle(Preferences node, String key, Style
	// defaultStyle, TextTag tag){
	// int style = node.getInt(key, defaultStyle.getValue());
	// tag.setStyle(Style.intern(style));
	// }
}
