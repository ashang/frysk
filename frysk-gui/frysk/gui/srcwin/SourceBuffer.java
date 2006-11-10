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
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.gnu.gdk.Color;
import org.gnu.glib.JGException;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextChildAnchor;
import org.gnu.gtk.TextIter;
import org.gnu.gtk.TextMark;
import org.gnu.gtk.TextTag;
import org.gnu.pango.Style;
import org.gnu.pango.Weight;
import org.jdom.Element;

import frysk.cli.hpd.SymTab;
import frysk.dom.DOMInlineInstance;
import frysk.dom.DOMLine;
import frysk.dom.DOMSource;
import frysk.dom.DOMTag;
import frysk.dom.DOMTagTypes;
import frysk.gui.common.prefs.ColorPreference;
import frysk.gui.common.prefs.PreferenceManager;
import frysk.gui.common.prefs.ColorPreference.ColorPreferenceListener;
import frysk.gui.srcwin.prefs.SourceWinPreferenceGroup;
import frysk.gui.srcwin.prefs.SyntaxPreference;
import frysk.gui.srcwin.prefs.SyntaxPreferenceGroup;
import frysk.gui.srcwin.prefs.SyntaxPreference.SyntaxPreferenceListener;
import frysk.lang.Variable;
import frysk.proc.Task;
import frysk.rt.StackFrame;

/**
 * This class is a wrapper around TextBuffer, it allows for extra functionality
 * needed by the SourceViewWidget. It will need to be retrofited to use correct
 * model objects once these are set, instead of SourceCodeLine and so on.
 * 
 * @author ifoox
 */
public class SourceBuffer
    extends TextBuffer
{

  protected static final String CLASS_TAG = "CLASS";

  /* CONSTANTS */
  protected static final String INLINE_TAG = "INLINE";

  protected static final String COMMENT_TAG = "COMMENT";

  protected static final String MEMBER_TAG = "MEMBER";

  protected static final String CURRENT_LINE = "currentLine";

  protected static final String FOUND_TEXT = "foundText";

  protected static final int SOURCE_MODE = 0;

  protected static final int ASM_MODE = 1;

  protected static final int MIXED_MODE = 2;

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

  private TextTag commentTag;

  private TextTag classTag;

  private TextTag optimizedVarTag;

  private TextTag oosVarTag;

  private TextTag namespaceTag;

  private TextTag includeTag;

  private TextTag macroTag;

  private TextTag templateTag;

  // Hashmap of comments for each file
  protected static HashMap comments = new HashMap();
  
  //private HashMap MarkMap;
  
  private int tagFlag = 0;

  // Since conceptually each sourcebuffer will only be viewing one file, we
  // don't
  // need any information higher than this
  protected StackFrame scope;

  protected TextChildAnchor anchor;

  private int mode = SOURCE_MODE;

  private boolean firstLoad = true;
  
  private String fileName = "";

  /**
   * Creates a new SourceBuffer
   */
  public SourceBuffer ()
  {
    super();
    this.init();
  }

  /**
   * Creates a new SourceBuffer with the given scope
   * 
   * @param scope
   */
  public SourceBuffer (StackFrame scope)
  {
    this();
    this.functions = new Vector();
    this.setScope(scope, SOURCE_MODE);
  }

  public SourceBuffer (StackFrame scope, int mode)
  {
    this();
    this.functions = new Vector();
    this.setScope(scope, mode);
  }

  /**
   * Returns true if the given line is executable.
   * 
   * @param lineNo The line to check
   * @return Whether or not the line is executable
   */
  public boolean isLineExecutable (int lineNo)
  {
    if (mode != SOURCE_MODE)
      return lineNo <= this.getLineCount();

    if (this.scope == null || this.scope.getData() == null)
      return false;

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
  public boolean isLineBroken (int lineNo)
  {
    // TODO: How do we deal with breakpoints for assembly code?
    if (mode != SOURCE_MODE)
      return false;

    if (this.scope == null || this.scope.getData() == null)
      return false;
    
    DOMLine line = this.scope.getData().getLine(lineNo + 1);
    if (line == null)
      return false;

    return line.hasBreakPoint();
  }

  /**
   * Toggles the breakpoint on line lineNum, returning the previous state of the
   * breakpoint on that line.
   * 
   * @param lineNum the number of the line to toggle the breakpoint for,
   *          starting at 0
   * @return true if breakpoint was previously set on lineNum, false otherwise
   * @throws ArrayIndexOutOfBoundsException thrown if the line number given does
   *           not correspond to a valid line number
   */
  public boolean toggleBreakpoint (int lineNum)
  {
    // TODO: Assembly breakpoints?
    if (mode != SOURCE_MODE)
      return false;

    if (! this.isLineExecutable(lineNum))
      return false;

    DOMLine line = this.scope.getData().getLine(lineNum + 1);
    if (line == null)
      return false;

    boolean status = line.hasBreakPoint();
    line.setBreakPoint(! status);
    return ! status;
  }

  /**
   * @return The current PC line
   */
  public int getCurrentLine ()
  {
    if (this.startCurrentLine == null)
      return - 1;

    return this.getIter(this.startCurrentLine).getLineNumber();
  }
  
  /**
   * Sets the current 'line' to the given range
   * 
   * @param startLine The line the current instruction starts on
   * @param startCol The offset (wrt. the start of the line) that the
   *          instruction starts on
   * @param endLine The line the current instruction ends on
   * @param endCol The offset (wrt. the start of the line) that the instruction
   *          ends on
   */
  protected void setCurrentLine (StackFrame frame)
  {

    int startLine = frame.getStartLine();
    int startCol = frame.getStartOffset();
    int endLine = frame.getEndLine();
    int endCol = frame.getEndOffset();
    
    this.startCurrentLine = this.createMark(
                                            "currentLineStart",
                                            this.getIter(this.getLineIter(
                                                                          startLine - 1).getOffset()
                                                         + startCol), true);
    if (endCol != StackLevel.EOL)
      {
        this.endCurrentLine = this.createMark(
                                              "currentLineEnd",
                                              this.getIter(this.getLineIter(
                                                                            endLine - 1).getOffset()
                                                           + endCol), false);
      }
    else
      {
        TextIter lineStart = this.getLineIter(endLine - 1);
        this.endCurrentLine = this.createMark(
                                              "currentLineEnd",
                                              this.getIter(lineStart.getOffset()
                                                           + lineStart.getCharsInLine()),
                                              true);
      }

    if (frame.getInner() == null)
      {
    if (this.tagFlag == 0)
      {
        //System.out.println("currnetlineapplytag " + frame.getMethodName() + " " + frame.getLineNumber());
        this.applyTag(this.currentLine, this.getIter(this.startCurrentLine),
                      this.getIter(this.endCurrentLine));
        this.tagFlag = 1;
      }
      }
    
    // Apply the next sections of the 'current line'
    frame = frame.getOuter();
    if (frame != null)
      setCurrentLine(frame);
    else
      this.tagFlag = 0;
  }
  
  protected void highlightLine (StackFrame frame, boolean newFrame)
  {
    int startLine = frame.getStartLine();
    int startCol = frame.getStartOffset();
    int endLine = frame.getEndLine();
    int endCol = frame.getEndOffset();

    //System.out.println("HIghlighting " + frame.getMethodName() + " " + frame.getLineNumber() + " " + newFrame);
    
    TextMark start = this.createMark(
                                     frame.getMethodName(),
                                     this.getIter(this.getLineIter(
                                                                   startLine - 1).getOffset()
                                                  + startCol), true);
    TextMark end = null;
    if (endCol != StackLevel.EOL)
      {
        end = this.createMark(
                              "end",
                              this.getIter(this.getLineIter(endLine - 1).getOffset()
                                           + endCol), false);
      }
    else
      {
        TextIter lineStart = this.getLineIter(endLine - 1);
        end = this.createMark("end",
                              this.getIter(lineStart.getOffset()
                                           + lineStart.getCharsInLine()),
                              true);
      }
    
        if (newFrame == true)
          {
            this.applyTag(this.currentLine, this.getIter(start), this.getIter(end));
          }
        else
          {
            this.removeTag(this.currentLine, this.getIter(start),
                                                          this.getIter(end));
          }
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
  public boolean findNext (String toFind, boolean matchCase, boolean findAll)
  {

    this.checkReset(toFind, matchCase);

    if (this.startCurrentFind == null || findAll)
      {
        this.startCurrentFind = this.getStartIter();
        this.endCurrentFind = this.getStartIter();
      }

    // clear the already found work
    this.removeTag(FOUND_TEXT, this.getStartIter(), this.getEndIter());

    // Work through the whole buffer from the start
    for (int i = this.endCurrentFind.getLineNumber(); i < this.getLineCount(); i++)
      {
        TextIter currentLine;
        /*
         * If we're on the same line as the last search and the last search
         * doesn't end the line, get the remainder of that line. If it does end
         * the line, continue to the next one
         */
        if (i == this.endCurrentFind.getLineNumber())
          if (! this.endCurrentFind.getEndsLine())
            currentLine = this.getIter(i, this.endCurrentFind.getLineOffset());
          else
            continue;
        else
          {
            currentLine = this.getIter(i, 0);
          }

        int lineLength = this.getText(currentLine, this.getEndIter(), true).indexOf(
                                                                                    "\n");

        // Last line will have no newline at the end, use length of
        // remaining buffer
        if (lineLength == - 1)
          lineLength = this.getText(currentLine, this.getEndIter(), true).length();

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
        if (index != - 1)
          {
            this.startCurrentFind = this.getIter(
                                                 i,
                                                 index
                                                     + currentLine.getLineOffset());
            this.endCurrentFind = this.getIter(i, index + toFind.length()
                                                  + currentLine.getLineOffset());

            this.applyTag(FOUND_TEXT, this.startCurrentFind,
                          this.endCurrentFind);
            // Scroll only if we aren't in 'highlight all' mode

            // Search the rest of the line for multiple occurances
            if (findAll)
              {
                String rest = lineText.substring(index, lineText.length());
                int before = index;
                index = rest.indexOf(toFind);

                // look for other matches on the same line
                while (rest.length() > 0 && index != - 1)
                  {
                    this.startCurrentFind = this.getIter(i, before + index);
                    this.endCurrentFind = this.getIter(i, before + index
                                                          + toFind.length());

                    this.applyTag(FOUND_TEXT, this.startCurrentFind,
                                  this.endCurrentFind);

                    rest = rest.substring(index + toFind.length(),
                                          rest.length());
                    before += index + toFind.length();

                    if (matchCase)
                      index = rest.indexOf(toFind);
                    else
                      index = rest.toLowerCase().indexOf(toFind.toLowerCase());
                  }
              }
            else
              {
                return true;
              }
          }
      }

    // For finding everything, getting this far means success
    if (findAll)
      {
        this.startCurrentFind = this.getStartIter();
        this.endCurrentFind = this.getStartIter();

        return true;
      }
    // If we're only trying to find one, getting this far is bad
    else
      {
        this.startCurrentFind = this.getEndIter();
        this.endCurrentFind = this.getEndIter();

        return false;
      }
  }

  /**
   * Finds the previous entry of toFind in the buffer and highlights it. Returns
   * true if successful, false otherwise
   * 
   * @param toFind The string to find
   * @param matchCase Whether to do a case sensitive search
   * @return Whether the search was successful
   */
  public boolean findPrevious (String toFind, boolean matchCase)
  {
    this.checkReset(toFind, matchCase);

    if (this.startCurrentFind == null)
      {
        this.startCurrentFind = this.getEndIter();
        this.endCurrentFind = this.getEndIter();
      }

    this.removeTag(FOUND_TEXT, this.getStartIter(), this.getEndIter());

    for (int i = this.endCurrentFind.getLineNumber(); i >= 0; i--)
      {

        TextIter currentLine = this.getIter(i, 0);

        int lineLength;
        if (i == this.endCurrentFind.getLineNumber())
          lineLength = this.getText(currentLine, this.startCurrentFind, true).length();
        else
          lineLength = this.getText(currentLine, this.getEndIter(), true).indexOf(
                                                                                  "\n");

        if (lineLength == - 1)
          lineLength = this.getText(currentLine, this.getEndIter(), true).length();

        if (lineLength == 0)
          continue;

        TextIter endLine = this.getIter(i, currentLine.getLineOffset()
                                           + lineLength);

        String lineText = this.getText(currentLine, endLine, true);

        int index;
        if (matchCase)
          index = lineText.lastIndexOf(toFind);
        else
          index = lineText.toLowerCase().lastIndexOf(toFind.toLowerCase());

        if (index != - 1)
          {
            this.startCurrentFind = this.getIter(i, currentLine.getLineOffset()
                                                    + index);
            this.endCurrentFind = this.getIter(i, currentLine.getLineOffset()
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
  public TextIter getStartCurrentFind ()
  {
    return startCurrentFind;
  }

  /**
   * Searches for a variable at the location specified by iter, and returns a
   * VariableLocation if a variable was found there. Returns null otherwise
   * 
   * @param iter The location to look for a variable
   * @return The variable at that location, or null
   */
  public Variable getVariable (TextIter iter)
  {
    if (this.scope == null || this.scope.getData() == null)
      return null;

    DOMSource source = this.scope.getData();

    if (mode != SOURCE_MODE || source == null)
      return null;

    DOMLine line = source.getLine(iter.getLineNumber());
    
    if (line == null)
      return null;
    
    DOMTag tag = line.getTag(iter.getLineOffset());

    // No var (or no tag), do nothing
    if (tag == null || ! tag.getType().equals(DOMTagTypes.LOCAL_VAR))
      return null;

    Task myTask = this.scope.getTask();
    SymTab stab = new SymTab(myTask.getTid(), myTask.getProc(), myTask, scope);
    stab.toString();
    Variable var;
    try
      {
        var = SymTab.print(line.getText().substring(
                                                    tag.getStart(),
                                                    tag.getStart()
                                                        + tag.getLength()));
      }
    catch (ParseException e)
      {
        System.out.println(e.getMessage());
        return null;
      }

    return var;
  }
  
  /**
   * Searches for a variable at the location specified by tag and line and returns a
   * VariableLocation if a variable was found there. Returns null otherwise
   * 
   * @param tag The DOMTag to examine for a variable
   * @param line  The DOMLine containing Source information
   * @return The variable at that location, or null
   */
  public Variable getVariable (DOMTag tag, DOMLine line)
  {
    // No var (or no tag), do nothing
    if (tag == null || ! tag.getType().equals(DOMTagTypes.LOCAL_VAR))
      return null;

    Task myTask = this.scope.getTask();
    SymTab stab = new SymTab(myTask.getTid(), myTask.getProc(), myTask, scope);
    stab.toString();
    Variable var;
    try
      {
        var = SymTab.print(line.getText().substring(
                                                    tag.getStart(),
                                                    tag.getStart()
                                                        + tag.getLength()));
      }
    catch (ParseException e)
      {
        System.out.println(e.getMessage());
        return null;
      }

    return var;
  }
  
  /**
   * @return The list of functions found by the parser
   */
  public Vector getFunctions ()
  {
    return functions;
  }

  /**
   * Adds a comment to be highlighted in the text
   * 
   * @param lineStart The line the comment starts on
   * @param colStart The offset from the the start of the line that the comment
   *          starts on
   * @param lineEnd The line the comment ends on
   * @param colEnd the offset from the start of the line that the comment ends
   *          on
   */
  public void addComment (int lineStart, int colStart, int lineEnd, int colEnd)
  {
    CommentList comment = new CommentList(lineStart, colStart, lineEnd, colEnd);

    CommentList list = (CommentList) comments.get(this.scope.getData().getFileName());

    if (list == null)
      comments.put(this.scope.getData().getFileName(), comment);
    else
      {
        while (list.getNextComment() != null)
          list = list.getNextComment();

        list.setNextComment(comment);
      }
  }

  /**
   * Returns the number of lines in the given file, with the option to include
   * inlined code in that count
   * 
   * @param includeInlindes Whether to include inlined code in the line count
   * @return The number of lines in the file
   */
  public int getLineCount ()
  {
    if (this.scope == null)
      return 0;

    DOMSource source = this.scope.getData();

    if (mode == SOURCE_MODE && source != null)
      return this.scope.getData().getLineCount();
    else
      return this.getEndIter().getLineNumber();
  }

  /**
   * @return The number of the last line in the buffer, which may or may not be
   *         the same as the number of lines in the buffer due to an initial
   *         offset
   */
  public int getLastLine ()
  {
    return this.getLineCount();
  }

  /**
   * @param lineNumber the line to check
   * @return true iff the given line has inlined code
   */
  public boolean hasInlineCode (int lineNumber)
  {
    if (this.scope == null)
      return false;

    DOMSource source = this.scope.getData();
    // TODO: Inline code with assembly?
    if (mode != SOURCE_MODE || source == null)
      return false;

    DOMLine line = source.getLine(lineNumber + 1);
    if (line == null)
      return false;

    return line.hasInlinedCode();
  }

  /**
   * @param lineNumber The line number to get Inline information from
   * @return The DOMInlineInstance containing the inlined code information, or
   *         null if no information exists
   */
  public DOMInlineInstance getInlineInstance (int lineNumber)
  {
    if (this.scope == null)
      return null;

    Iterator iter = this.scope.getData().getLine(lineNumber + 1).getInlines();
    if (! iter.hasNext())
      return null;

    return new DOMInlineInstance((Element) iter.next());
  }

  public void setScope (StackFrame scope)
  {
    this.setScope(scope, SOURCE_MODE);
  }

  /**
   * Sets the scope that will be displayed by this buffer to the provided scope.
   * 
   * @param scope The stack frame to be displayed
   */
  private void setScope (StackFrame scope, int mode)
  {
    Iterator i = this.functions.iterator();
    while (i.hasNext())
      {
        String del = (String) i.next();
        //System.out.println(">>> " + del);
        this.deleteMark(del);
      }
    
    this.mode = mode;

    this.anchor = null;
    this.functions.clear();
    this.scope = scope;
    DOMSource data = scope.getData();
    String file = "";
    
    if (data != null)
      file = data.getFileName();
    
    try
      {
        switch (mode)
          {
          case SOURCE_MODE:
            if (this.fileName.equals("") || ! this.fileName.equals(file))
              {
                this.firstLoad = true;
                loadFile();
              }
            else
              createTags();
            break;
          case ASM_MODE:
            this.loadAssembly();
            break;
          case MIXED_MODE:
            break;
          }

      }
    catch (Exception e)
      {
        e.printStackTrace();
      }

    if (scope != null)
      {
        this.fileName = file;
        this.setCurrentLine(scope);
      }
  }

  public void setMode (int mode)
  {
    this.setScope(this.getScope(), mode);
  }

  /**
   * Creates the anchor at the current line that will to which the inlined code
   * will be attached. If a previous anchor exists it will be overridden.
   * 
   * @return The created anchor.
   */
  public TextChildAnchor createAnchorAtCurrentLine ()
  {
    TextIter line = this.getLineIter(this.getCurrentLine() + 1);

    if (this.anchor != null)
      this.deleteText(line, this.getIter(line.getOffset() + 1));
    else
      this.insertText(line, "\n");
    this.anchor = this.createChildAnchor(this.getLineIter(this.getCurrentLine() + 1));

    return this.anchor;
  }

  /**
   * Removed the anchor created by
   * {@link SourceBuffer#createAnchorAtCurrentLine()}
   */
  public void clearAnchorAtCurrentLine ()
  {
    // do nothing if there's nothing to clear
    if (this.anchor == null)
      return;

    TextIter line = this.getLineIter(this.getCurrentLine() + 1);
    this.deleteText(line, this.getIter(line.getOffset() + 2));

    this.anchor = null;
  }

  /**
   * @return The stack frame currently being displayed
   */
  public StackFrame getScope ()
  {
    return scope;
  }

  /**
   * Returns the number of lines of assembly code for each line of source code.
   * This is used mostly internally to aid in drawing line numbers.
   * 
   * @param sourceLine The line of source code
   * @return The number of assembly instructions associated with the line.
   */
  public int getNumberOfAssemblyLines (int sourceLine)
  {
    if (this.mode == MIXED_MODE)
      return 0;
    return 3;
  }

  /*-------------------*
   * PRIVATE METHODS   *
   *-------------------*/

  /*
   * Initializes the tags, loads the file from the current scope, and sets the
   * current line.
   */
  private void init ()
  {
    functions = new Vector();
    this.currentLine = this.createTag(CURRENT_LINE);
    this.foundText = this.createTag(FOUND_TEXT);
    this.commentTag = this.createTag(COMMENT_TAG);

    this.functionTag = this.createTag(DOMTagTypes.FUNCTION);
    this.variableTag = this.createTag(DOMTagTypes.LOCAL_VAR);
    this.keywordTag = this.createTag(DOMTagTypes.KEYWORD);
    this.classTag = this.createTag(DOMTagTypes.CLASS_DECL);
    this.optimizedVarTag = this.createTag(DOMTagTypes.OPTIMIZED_VAR);
    this.oosVarTag = this.createTag(DOMTagTypes.OUT_OF_SCOPE_VAR);
    this.namespaceTag = this.createTag(DOMTagTypes.NAMESPACE);
    this.includeTag = this.createTag(DOMTagTypes.INCLUDE);
    this.macroTag = this.createTag(DOMTagTypes.MACRO);
    this.templateTag = this.createTag(DOMTagTypes.TEMPLATE);

    // TODO: This tag has no corresponding DOM tag
    this.globalTag = this.createTag(MEMBER_TAG);

    this.commentTag.setForeground(ColorConverter.colorToHexString(Color.GREEN));

    // We have to set this manually since this isn't controlled by the
    // preferences
    this.optimizedVarTag.setStrikethrough(true);

    // Initialize preferences

    ((ColorPreference) PreferenceManager.sourceWinGroup.getSubgroup(
                                                                    "Look and Feel").getPreference(
                                                                                                   SourceWinPreferenceGroup.CURRENT_LINE)).addListener(new ColorPreferenceListener()
    {
      public void preferenceChanged (String prefName, Color newColor)
      {
        SourceBuffer.this.currentLine.setBackground(ColorConverter.colorToHexString(newColor));
      }
    });

    ((ColorPreference) PreferenceManager.sourceWinGroup.getSubgroup(
                                                                    "Look and Feel").getPreference(
                                                                                                   SourceWinPreferenceGroup.SEARCH)).addListener(new ColorPreferenceListener()
    {
      public void preferenceChanged (String prefName, Color newColor)
      {
        SourceBuffer.this.foundText.setBackground(ColorConverter.colorToHexString(newColor));
      }
    });

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.CLASSES)).addListener(new TagPreferenceListener(
                                                                                                                                                      this.classTag));

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.FUNCTIONS)).addListener(new TagPreferenceListener(
                                                                                                                                                        this.functionTag));

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.GLOBALS)).addListener(new TagPreferenceListener(
                                                                                                                                                      this.globalTag));

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.KEYWORDS)).addListener(new TagPreferenceListener(
                                                                                                                                                       this.keywordTag));

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.OPTIMIZED)).addListener(new TagPreferenceListener(
                                                                                                                                                        this.oosVarTag));

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.VARIABLES)).addListener(new TagPreferenceListener(
                                                                                                                                                        this.variableTag));

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.OUT_OF_SCOPE)).addListener(new TagPreferenceListener(
                                                                                                                                                           this.oosVarTag));

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.COMMENTS)).addListener(new TagPreferenceListener(
                                                                                                                                                       this.commentTag));

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.NAMESPACE)).addListener(new TagPreferenceListener(
                                                                                                                                                        this.namespaceTag));

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.INCLUDES)).addListener(new TagPreferenceListener(
                                                                                                                                                       this.includeTag));

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.MACRO)).addListener(new TagPreferenceListener(
                                                                                                                                                    this.macroTag));

    ((SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.TEMPLATE)).addListener(new TagPreferenceListener(
                                                                                                                                                       this.templateTag));
  }

  /**
   * Loads a file at the given location into the buffer, clearing any previously
   * loaded files.
   * 
   * @param filename The file to load
   * @throws FileNotFoundException
   * @throws JGException
   */
  protected void loadFile () throws FileNotFoundException, JGException
  {
    if (this.scope == null)
      return;

    DOMSource source = this.scope.getData();
    
    if (source == null)
      {
        if (! this.firstLoad)
          return;

        StackFrame curr = this.scope;
        while (curr != null)
          {
            if (curr.getData() != null)
              {
                source = curr.getData();
                break;
              }
            curr = curr.getOuter();
          }

        /* There really were no frames with debuginfo */
        if (curr == null)
          {
            this.insertText("No debug information available for this stack frame");
            this.firstLoad = false;
            return;
          }
        else
          {
            Iterator lines = source.getLines();

            String bufferText = loadLines(lines);

            this.deleteText(this.getStartIter(), this.getEndIter());
            this.insertText(bufferText);
            this.createTags();
            return;
          }
      }

    Iterator lines = source.getLines();

    String bufferText = loadLines(lines);

    this.deleteText(this.getStartIter(), this.getEndIter());
    this.insertText(bufferText);

    this.createTags();
  }

  protected void loadAssembly ()
  {
    this.deleteText(this.getStartIter(), this.getEndIter());

    String text = "";

    for (int i = 10000; i < 10100; i++)
      text += "0x" + Integer.toHexString(i) + " mv $a0 $a1\n";

    this.insertText(text);
  }

  /**
   * @param lines Iterator of the lines to store in this SourceBuffer
   * @return What should be the contents of the buffer
   */
  protected String loadLines (Iterator lines)
  {
    String bufferText = "";

    // First get the text, append it all together, and add it to ourselves
    while (lines.hasNext())
      {
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
  protected void createTags ()
  {
    //System.out.println("Creating tags for " + this.scope.getMethodName());
    Iterator lines = this.scope.getData().getLines();
    
//    StackFrame curr = this.scope;
//    if (curr.getInner() != null)
//      {
//        while (curr.getInner() != null)
//          curr = curr.getInner();
//      }

    // Iterate through all the lines
    while (lines.hasNext())
      {
        DOMLine line = new DOMLine((Element) lines.next());

        Iterator tags = line.getTags();
        int lineOffset = line.getOffset();

        // Iterator though all the tags on the line
        while (tags.hasNext())
          {
            Element e = (Element) tags.next();
            DOMTag tag = new DOMTag(e);

            String type = tag.getType();

            if (type.equals(DOMTagTypes.FUNCTION_BODY))
              {
                String funcName = tag.getToken();
                
                String[] nameArray = funcName.split("\\s*");
                StringBuffer buffer = new StringBuffer();
                
                for (int i = 0; i < nameArray.length; i++)
                  buffer.append(nameArray[i]);
                
                funcName = buffer.toString();
                this.functions.add(funcName);
                this.createMark(funcName, this.getLineIter(line.getLineNum()),
                                true);

              }
            else
              {
                this.applyTag(type, this.getIter(lineOffset + tag.getStart()),
                              this.getIter(lineOffset + tag.getStart()
                                           + tag.getLength()));
              }
          }

        Iterator inlines = line.getInlines();

        while (inlines.hasNext())
          {
            DOMInlineInstance func = new DOMInlineInstance(
                                                           (Element) inlines.next());

            this.applyTag(DOMTagTypes.FUNCTION,
                          this.getIter(lineOffset + func.getStart()),
                          this.getIter(lineOffset + func.getStart()
                                       + func.getEnd()));
          }
      }// end lines.hasNext()

    // Now iterate through the comments
    CommentList list = (CommentList) comments.get(this.scope.getData().getFileName());

    while (list != null)
      {
        // this.applyTag(COMMENT_TAG, this.getIter(list.getStartLine(),
        // list.getStartCol()),
        // this.getIter(list.getEndLine(), list.getEndCol()));

        list = list.getNextComment();
      }
  }

  /*
   * Checks whether or not the current search should be restarted by comparing
   * the string last searched with the new string
   */
  private void checkReset (String newString, boolean matchCase)
  {
    if (this.startCurrentFind == null)
      return;

    String found = this.getText(this.startCurrentFind, this.endCurrentFind,
                                false);

    if (matchCase && (found.equals("") || ! found.equals(newString)))
      {
        this.startCurrentFind = null;
        this.endCurrentFind = null;
      }
    else if (found.equals("") || ! found.equalsIgnoreCase(newString))
      {
        this.startCurrentFind = null;
        this.endCurrentFind = null;
      }
  }

  private class TagPreferenceListener
      implements SyntaxPreferenceListener
  {
    private TextTag myTag;

    public TagPreferenceListener (TextTag myTag)
    {
      this.myTag = myTag;
    }

    public void preferenceChanged (String name, Color newColor,
                                   Weight newWeight, Style newStyle)
    {
      this.myTag.setForeground(ColorConverter.colorToHexString(newColor));
      this.myTag.setWeight(newWeight);
      this.myTag.setStyle(newStyle);
    }
  }

  static class CommentList
  {
    private int startLine;

    private int endLine;

    private int startCol;

    private int endCol;

    private CommentList nextComment;

    public CommentList (int startLine, int startCol, int endLine, int endCol)
    {
      this.startLine = startLine;
      this.startCol = startCol;
      this.endLine = endLine;
      this.endCol = endCol;
    }

    public CommentList getNextComment ()
    {
      return nextComment;
    }

    public void setNextComment (CommentList nextComment)
    {
      this.nextComment = nextComment;
    }

    public int getEndCol ()
    {
      return endCol;
    }

    public int getEndLine ()
    {
      return endLine;
    }

    public int getStartCol ()
    {
      return startCol;
    }

    public int getStartLine ()
    {
      return startLine;
    }
  }
}
