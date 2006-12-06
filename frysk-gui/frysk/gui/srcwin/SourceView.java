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

import java.util.HashMap;
import java.util.Vector;

import org.gnu.gdk.Color;
import org.gnu.gdk.Cursor;
import org.gnu.gdk.CursorType;
import org.gnu.gdk.Drawable;
import org.gnu.gdk.GC;
import org.gnu.gdk.Point;
import org.gnu.gdk.Window;
import org.gnu.gtk.Container;
import org.gnu.gtk.Label;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.StateType;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextChildAnchor;
import org.gnu.gtk.TextIter;
import org.gnu.gtk.TextMark;
import org.gnu.gtk.TextView;
import org.gnu.gtk.TextWindowType;
import org.gnu.gtk.WindowType;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;
import org.gnu.gtk.event.MouseMotionEvent;
import org.gnu.gtk.event.MouseMotionListener;
import org.gnu.pango.Alignment;
import org.gnu.pango.FontDescription;
import org.gnu.pango.Layout;

import frysk.dom.DOMInlineInstance;
import frysk.dom.DOMLine;
import frysk.dom.DOMSource;
import frysk.dom.DOMTag;
import frysk.gui.common.prefs.BooleanPreference;
import frysk.gui.common.prefs.ColorPreference;
import frysk.gui.common.prefs.IntPreference;
import frysk.gui.common.prefs.PreferenceManager;
import frysk.gui.common.prefs.BooleanPreference.BooleanPreferenceListener;
import frysk.gui.common.prefs.ColorPreference.ColorPreferenceListener;
import frysk.gui.common.prefs.IntPreference.IntPreferenceListener;
import frysk.gui.srcwin.prefs.SourceWinPreferenceGroup;
//import frysk.value.InvalidOperatorException;
import frysk.value.Variable;
import frysk.rt.StackFrame;

/**
 * This class is used to add some functionality to TextView that may be needed
 * by the source window but is not directly available or easily accessible in
 * TextView. This widget uses a SourceBuffer instead of a TextBuffer. A
 * SourceBuffer allows some extras that are needed by the source window such as
 * break- point management.
 */
public class SourceView
    extends TextView
    implements View, ExposeListener
{

  // my SourceBuffer
  protected SourceBuffer buf;

  // How far to start writing breakpoints, etc. from the left side of the
  // margin
  protected int marginWriteOffset;

  protected TextChildAnchor anchor;

  protected SourceWindow parent;

  protected boolean expanded = false;

  protected boolean showingLineNums;

  protected boolean showingExecMarks;

  protected Color marginColor;

  protected Color lineColor;

  protected Color execMarkColor;

  protected Color currentLineColor;

  // keep this around, we'll be needing it
  private GC myContext;

  private InlineSourceView child;

  private SourceViewListener listener;

  private Variable hoveredVar;
  
  private HashMap varMap = new HashMap();

  private DOMTag lastTag;
  
  // private int hoverX;
  //  
  // private int hoverY;

  /**
   * Constructs a new SourceViewWidget. If you don't specify a buffer before
   * using it, a default one will be created for you.
   * 
   * @param scope The source file that this widget will be displaying
   * @param parent The SourceWindow that this SourceViewWidget is contained in
   */
  public SourceView (StackFrame scope, SourceWindow parent)
  {
    this(new SourceBuffer(scope), parent);
  }

  public SourceView (StackFrame scope, SourceWindow parent, int mode)
  {
    this(new SourceBuffer(scope, mode), parent);
  }

  /**
   * Constructs a new SourceViewWidget using the previously created buffer
   * 
   * @param buffer The sourceBuffer to use as the data for this object
   * @param parent The SourceWindow this object is contained within
   */
  public SourceView (SourceBuffer buffer, SourceWindow parent)
  {
    super(gtk_text_view_new());

    this.setName("sourceView");
    this.getAccessible().setName("sourceView_showsSourceCode");
    this.getAccessible().setDescription(
                                        "Displays the source code for the currently selected stack level");

    this.parent = parent;
    this.buf = buffer;
    this.listener = new SourceViewListener(this);
    this.setBuffer(this.buf);
    this.initialize();
  }

  /**
   * Returns the SourceBuffer being used
   * 
   * @return The SourceBuffer used in the widget.
   */
  public TextBuffer getBuffer ()
  {
    return buf;
  }

  /**
   * Implementation from ExposeListener Interface.
   */
  public boolean exposeEvent (ExposeEvent event)
  {
    // Ignore events that aren't expose events or don't have anything
    // to do with the sidebar
    if (! event.isOfType(ExposeEvent.Type.NO_EXPOSE))
      {
        if (event.getWindow().equals(this.getWindow(TextWindowType.LEFT)))
          this.drawMargin();
        else if (event.getWindow().equals(this.getWindow(TextWindowType.TEXT))
                 && this.hoveredVar != null)
          {
            // Do mouse-over-variable drawing stuff here
          }

      }

    return false;
  }

  /**
   * Scrolls the TextView so that the given line is visible in the widget
   * 
   * @param lineNum The line to scroll to
   */
  public void scrollToLine (int lineNum)
  {
    this.scrollToIter(this.buf.getLineIter(lineNum - 1), 0.35);
  }

  /**
   * Finds the next instance of toFind in the buffer and highlights it. returns
   * true if succesful, false otherwise
   * 
   * @param toFind The string to find
   * @param caseSensitive Whether to do a case sensitive search
   * @return If the search was successful
   */
  public boolean findNext (String toFind, boolean caseSensitive)
  {
    boolean result = this.buf.findNext(toFind, caseSensitive, false);
    if (result)
      this.scrollToIter(this.buf.getStartCurrentFind(), 0);

    return result;
  }

  /**
   * Finds the previous instance of toFind in the buffer and highlights it.
   * Returns true if succesful, false otherwise
   * 
   * @param toFind The string to find
   * @param caseSensitive Whether to do a case sensitive search
   * @return If the search was successful
   */
  public boolean findPrevious (String toFind, boolean caseSensitive)
  {
    boolean result = this.buf.findPrevious(toFind, caseSensitive);

    if (result)
      this.scrollToIter(this.buf.getStartCurrentFind(), 0);

    return result;
  }

  /**
   * Finds all instances of toFind in the current buffer and highlights them.
   * Returns true if successful, false otherwise
   * 
   * @param toFind The string to find in the buffer
   * @param caseSensitive Whether to do a case sensitive search
   * @return If the search was successful
   */
  public boolean highlightAll (String toFind, boolean caseSensitive)
  {
    return this.buf.findNext(toFind, caseSensitive, true);
  }

  /**
   * Loads the contents of the provided StackFrame into this view
   * 
   * @param data The new stack frame to load.
   */
  public void load (StackFrame data)
  {
    this.buf.setScope(data);
    this.expanded = false;
    this.anchor = null;
  }

  public void setMode (int mode)
  {
    this.buf.setMode(mode);
  }

  /**
   * Sets the inlined subscope at the current line to be the object provided.
   * 
   * @param child The inlined scope to display
   */
  public void setSubscopeAtCurrentLine (InlineSourceView child)
  {
    // Only inline source viewers in a source viewer
    if (! (child instanceof InlineSourceView))
      return;

    this.child = (InlineSourceView) child;
    Container parent = (Container) this.child.getParent();
    if (parent != null)
      parent.remove(this.child);

    this.expanded = true;
    this.addChild(this.child, this.buf.createAnchorAtCurrentLine());
    this.child.show();
  }

  /**
   * Removes the inline subscope that is currently being displayed from the
   * view.
   */
  public void clearSubscopeAtCurrentLine ()
  {
    this.buf.clearAnchorAtCurrentLine();
    this.expanded = false;
    this.anchor = null;
  }

  /*
   * Toggles the visibility of the inlined code at the current line.
   */
  public void toggleChild ()
  {
    if (! expanded)
      {
        expanded = true;

        DOMInlineInstance instance = this.buf.getInlineInstance(this.buf.getCurrentLine());

        InlineSourceView nested = new InlineSourceView(
                                                       this.parent,
                                                       instance,
                                                       CurrentStackView.getCurrentFrame());
        this.setSubscopeAtCurrentLine(nested);
      }
    else
      {
        expanded = false;
        this.clearSubscopeAtCurrentLine();
      }

  }

  public void scrollToFunction (String markName)
  {

    if (this.buf.getFunctions().contains(markName))
      {
        TextMark mark = this.buf.getMark(markName);
        this.scrollToMark(mark, 0.49);
      }
  }

  public Vector getFunctions ()
  {
    return this.buf.getFunctions();
  }

  public StackFrame getScope ()
  {
    return this.buf.getScope();
  }

  /*
   * Function responsible for drawing the side area where breakpoints, etc. are
   * drawn. Called either in response to an expose event or when a preference
   * changes
   */
  protected void drawMargin ()
  {
    Window drawingArea = this.getWindow(TextWindowType.LEFT);

    // draw the background for the margin
    if (this.myContext == null)
      this.myContext = new GC((Drawable) drawingArea);

    myContext.setRGBForeground(marginColor);
    drawingArea.drawRectangle(this.myContext, true, 0, 0,
                              drawingArea.getWidth(), drawingArea.getHeight());

    // get the y coordinates for the top and bottom of the window
    int minY = drawingArea.getClipRegion().getClipbox().getY();
    int maxY = minY + drawingArea.getClipRegion().getClipbox().getHeight();

    // find out what the actual starting coordinates of the first line on
    // screen is
    TextIter firstIter = this.getIterAtLocation(this.windowToBufferCoords(
                                                                          TextWindowType.LEFT,
                                                                          0,
                                                                          minY));
    VerticalLineRange firstRange = this.getLineYRange(firstIter);
    int actualFirstStart = this.bufferToWindowCoords(TextWindowType.LEFT, 0,
                                                     firstRange.getY()).getY();

    // get the line numbers we'll be drawing
    int firstLine = firstIter.getLineNumber();
    int lastLine = this.getIterAtLocation(
                                          this.windowToBufferCoords(
                                                                    TextWindowType.LEFT,
                                                                    0, maxY)).getLineNumber();

    // Get Color to draw the text in
    this.myContext.setRGBForeground(lineColor);

    int currentHeight = 0;
    int actualIndex = firstLine;
    boolean skipNextLine = false;

    int drawingHeight = 0;
    int lineHeight = 0;
    int gapHeight = 0;

    // If the refresh is starting after the current line, we have to add
    // that offset in to
    // make sure the gap in line numbers is maintained
    if (expanded && firstLine > this.buf.getCurrentLine())
      gapHeight = this.getLineYRange(
                                     this.getBuffer().getLineIter(
                                                                  this.buf.getCurrentLine() + 1)).getHeight();

    for (int i = firstLine; i <= lastLine && i < this.buf.getLineCount(); i++)
      {

        // Make adjustments if we are after the current line - there may have
        // been inline code
        if (i > this.buf.getCurrentLine())
          {
            drawingHeight = currentHeight + gapHeight;
            if (expanded)
              lineHeight = this.getLineYRange(
                                              this.getBuffer().getLineIter(
                                                                           i + 1)).getHeight();
            else
              lineHeight = this.getLineYRange(this.getBuffer().getLineIter(i)).getHeight();
          }
        else
          {
            drawingHeight = currentHeight;
            lineHeight = this.getLineYRange(this.getBuffer().getLineIter(i)).getHeight();
          }

        int iconStart = lineHeight / 2;

        // skipNextLine is true when we're on a line containing the inlined
        // code, do nothing
        if (skipNextLine)
          {
            skipNextLine = false;

            gapHeight = this.getLineYRange(
                                           this.getBuffer().getLineIter(
                                                                        actualIndex++)).getHeight();

            i--;
            continue;
          }

        // For the current line, do some special stuff
        if (i == this.buf.getCurrentLine())
          {

            // Draw executable marks
            this.myContext.setRGBForeground(this.currentLineColor);
            if (showingExecMarks)
              drawingArea.drawRectangle(this.myContext, true, 0,
                                        actualFirstStart + drawingHeight,
                                        this.marginWriteOffset + 40, lineHeight);
            else
              drawingArea.drawRectangle(this.myContext, true, 0,
                                        actualFirstStart + drawingHeight,
                                        this.marginWriteOffset + 20, lineHeight);
            this.myContext.setRGBForeground(lineColor);

            // Draw an 'i' in the margin if this line has inlined code
            if (this.buf.hasInlineCode(i))
              {
                this.myContext.setRGBForeground(this.execMarkColor);
                Layout lo = this.createLayout("i");
                lo.setAlignment(Alignment.RIGHT);
                if (showingExecMarks)
                  drawingArea.drawLayout(this.myContext,
                                         this.marginWriteOffset + 25,
                                         actualFirstStart + drawingHeight, lo);
                else
                  drawingArea.drawLayout(this.myContext,
                                         this.marginWriteOffset + 5,
                                         actualFirstStart + drawingHeight, lo);
                this.myContext.setRGBForeground(lineColor);

                // We don't want to draw anything in the margin next to the
                // inlined code
                if (this.expanded)
                  skipNextLine = true;
              }
          }

        // If it is executable, draw a mark
        if (showingExecMarks && this.buf.isLineExecutable(i))
          {
            this.myContext.setRGBForeground(this.execMarkColor);
            drawingArea.drawLine(this.myContext, this.marginWriteOffset + 5,
                                 actualFirstStart + drawingHeight + iconStart,
                                 this.marginWriteOffset + 12, actualFirstStart
                                                              + drawingHeight
                                                              + iconStart);
            this.myContext.setRGBForeground(lineColor);
          }

        // Draw line numbers
        if (showingLineNums) {
          drawLineNumber(drawingArea, this.myContext, actualFirstStart
                                                      + drawingHeight, i);
        }

        // draw breakpoints
        if (this.buf.isLineBroken(i))
          {
            int iconHeight = lineHeight - 8;

            this.myContext.setRGBForeground(new Color(65535, 0, 0));
            drawingArea.drawRectangle(this.myContext, true,
                                      this.marginWriteOffset + 5,
                                      actualFirstStart + drawingHeight + 4,
                                      iconHeight, iconHeight);
            this.myContext.setRGBForeground(lineColor);
          }

        // update height for next line
        currentHeight += this.getLineYRange(
                                            this.getBuffer().getLineIter(
                                                                         actualIndex++)).getHeight();
      }
  }

  /**
   * Draws the line corresponding to the number provided. Note that because of
   * inlined code, initial offsets or other widgets embedded in the source code
   * this may not be the number passed to this function.
   * 
   * @param drawingArea The org.gnu.gdk.Window to draw on
   * @param context The GC to use
   * @param drawingHeight The height that we should draw at within the window
   * @param number The number to use to calcuate what number should be drawn
   */
  protected void drawLineNumber (Window drawingArea, GC context,
                                 int drawingHeight, int number)
  {
    Layout lo = this.createLayout("" + (number + 1));
    lo.setAlignment(Alignment.LEFT);
    lo.setWidth(this.marginWriteOffset);
    drawingArea.drawLayout(context, this.marginWriteOffset, drawingHeight, lo);
  }
  
  public void removeVar (Variable var)
  {
    if (varMap.containsKey(var.getText()))
      varMap.remove(var.getText());
    else
      return;
    
    SourceView.this.parent.removeVariableTrace(var);
  }
  
  public Vector refreshVars (Vector vars)
  {
    return this.buf.refreshVars(vars);
  }

  /*---------------------------*
   * PRIVATE METHODS           *
   *---------------------------*/

  /*
   * Performs some operations before the window is shown
   */
  private void initialize ()
  {
    FontDescription desc = new FontDescription();
    desc.setFamily("Monospace");
    this.setFont(desc);

    // Stuff that never changes
    this.setLeftMargin(3);
    this.setEditable(false);
    this.setCursorVisible(false);

    // Listeners
    this.addListener((ExposeListener) this);
    this.addListener((MouseListener) listener);
    this.addListener((MouseMotionListener) listener);

    // PreferenceListeners
    ((ColorPreference) PreferenceManager.sourceWinGroup.getSubgroup(
                                                                    "Look and Feel").getPreference(
                                                                                                   SourceWinPreferenceGroup.TEXT)).addListener(new ColorPreferenceListener()
    {
      public void preferenceChanged (String prefName, Color newColor)
      {
        SourceView.this.setTextColor(StateType.NORMAL, newColor);
      }
    });

    ((ColorPreference) PreferenceManager.sourceWinGroup.getSubgroup(
                                                                    "Look and Feel").getPreference(
                                                                                                   SourceWinPreferenceGroup.BACKGROUND)).addListener(new ColorPreferenceListener()
    {
      public void preferenceChanged (String prefName, Color newColor)
      {
        SourceView.this.setBaseColor(StateType.NORMAL, newColor);
      }
    });

    ((ColorPreference) PreferenceManager.sourceWinGroup.getSubgroup(
                                                                    "Look and Feel").getPreference(
                                                                                                   SourceWinPreferenceGroup.MARGIN)).addListener(new ColorPreferenceListener()
    {
      public void preferenceChanged (String prefName, Color newColor)
      {
        SourceView.this.marginColor = newColor;
      }
    });

    ((ColorPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.LINE_NUMBER_COLOR)).addListener(new ColorPreferenceListener()
    {
      public void preferenceChanged (String prefName, Color newColor)
      {
        SourceView.this.lineColor = newColor;
      }
    });

    ((ColorPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.EXEC_MARKS_COLOR)).addListener(new ColorPreferenceListener()
    {
      public void preferenceChanged (String prefName, Color newColor)
      {
        SourceView.this.execMarkColor = newColor;
      }
    });

    ((ColorPreference) PreferenceManager.sourceWinGroup.getSubgroup(
                                                                    "Look and Feel").getPreference(
                                                                                                   SourceWinPreferenceGroup.CURRENT_LINE)).addListener(new ColorPreferenceListener()
    {
      public void preferenceChanged (String prefName, Color newColor)
      {
        SourceView.this.currentLineColor = newColor;
      }
    });

    ((IntPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.INLINE_LEVELS)).addListener(new IntPreferenceListener()
    {
      public void preferenceChanged (String prefName, int newValue)
      {
        if (SourceView.this.child != null)
          SourceView.this.child.recalculateVisibleScopes();
      }
    });

    // Sidebar
    ((BooleanPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.LINE_NUMS)).addListener(new BooleanPreferenceListener()
    {
      public void preferenceChanged (String prefName, boolean newValue)
      {
        SourceView.this.showingLineNums = newValue;
        SourceView.this.calculateMargin();
      }
    });

    ((BooleanPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.EXEC_MARKS)).addListener(new BooleanPreferenceListener()
    {
      public void preferenceChanged (String prefName, boolean newValue)
      {
        SourceView.this.showingExecMarks = newValue;
        SourceView.this.calculateMargin();
      }
    });
    
    this.lastTag = null;

    this.showAll();
  }

  /**
   * 
   */
  private void calculateMargin ()
  {
    if (showingLineNums)
      {
        Layout lo = new Layout(this.getContext());
        lo.setText("" + (this.buf.getLastLine() + 1));
        this.marginWriteOffset = lo.getPixelWidth();
      }
    else
      {
        this.setBorderWindowSize(TextWindowType.LEFT, 20);
        this.marginWriteOffset = 0;
      }

    if (showingExecMarks)
      {
        this.setBorderWindowSize(TextWindowType.LEFT,
                                 this.marginWriteOffset + 40);
      }
    else
      {
        this.setBorderWindowSize(TextWindowType.LEFT,
                                 this.marginWriteOffset + 20);
      }
  }

  private boolean isTextArea (Window win)
  {
    return win.equals(this.getWindow(TextWindowType.TEXT));
  }

  private boolean isMargin (Window win)
  {
    return win.equals(this.getWindow(TextWindowType.LEFT));
  }

  private boolean clickedOnMargin (MouseEvent event)
  {
    TextIter iter = this.getIterFromWindowCoords(0, (int) event.getY());

    int theLine = iter.getLineNumber();
    boolean overNested = false;

    // We want to ignore mouse clicks in the margin next to
    // expanded inline code
    if (theLine == this.buf.getCurrentLine() + 1 && expanded)
      return false;

    if (theLine > this.buf.getCurrentLine() && expanded)
      {
        theLine--;
        overNested = true;
      }

    final int lineNum = theLine;

    // only popup a window if the line is executable
    if (event.getButtonPressed() == MouseEvent.BUTTON3
        && this.buf.isLineExecutable(lineNum) && (! expanded || overNested))
      {
        Menu m = new Menu();
        MenuItem mi = new MenuItem("Breakpoint information...", false);
        mi.addListener(new MenuItemListener()
        {
          public void menuItemEvent (MenuItemEvent arg0)
          {
            org.gnu.gtk.Window popup = new org.gnu.gtk.Window(
                                                              WindowType.TOPLEVEL);
            popup.add(new Label("Line: " + (lineNum + 1)));
            popup.showAll();
          }
        });
        m.append(mi);
        MenuItem mi2 = new MenuItem("Customize breakpoint actions...", false);
        m.append(mi2);
        if (! this.buf.isLineBroken(lineNum))
          { // no breakpoint, no
            // info to show
            mi.setSensitive(false);
            mi2.setSensitive(false);
          }
        m.append(new MenuItem()); // Separator
        mi = new MenuItem("Toggle Breakpoint", false);
        m.append(mi);
        mi.addListener(new MenuItemListener()
        {
          public void menuItemEvent (MenuItemEvent event)
          {
            SourceView.this.buf.toggleBreakpoint(lineNum);
          }
        });
        m.popup();
        m.showAll();
      }

    // Left click in the margin for a line with inline code - toggle the
    // display of it
    if (event.getButtonPressed() == MouseEvent.BUTTON1
        && lineNum == this.buf.getCurrentLine()
        && this.buf.hasInlineCode(lineNum))
      {
        this.toggleChild();
      }

    return true;
  }

  private boolean clickedOnTextArea (MouseEvent event)
  {
    // Right click over the main text area will trigger the
    // variable-finding
    if (event.getButtonPressed() == MouseEvent.BUTTON3)
      {
        TextIter iter = this.getIterFromWindowCoords((int) event.getX(),
                                                     (int) event.getY());
        final Variable var = this.buf.getVariable(iter);

        Menu m = new Menu();

        /*
         * If the variable comes back non-null, set up the right-click
         * menu stuff where the variable value is shown as one item in the
         * menu and the ability to add the item to the Variable Watch
         * window is another item.
         */

        if (var != null)
          {
            MenuItem valueItem;
            valueItem = new MenuItem("Value of " + var.getText() + ": "
                                     + var.toString(), true);
            valueItem.setSensitive(false);
            m.append(valueItem);
            /*
             * Only show this item in the menu if the variable is not already
             * there
             */
            if (! varMap.containsKey(var.getText()))
              {
                MenuItem traceItem = new MenuItem("Add to Variable Watches",
                                                  false);
                m.append(traceItem);
                traceItem.setSensitive(true);
                traceItem.addListener(new MenuItemListener()
                {
                  public void menuItemEvent (MenuItemEvent arg0)
                  {
                    if (varMap.containsKey(var.getText()))
                      return;
                    else
                      varMap.put(var.getText(), var);

                    SourceView.this.parent.addVariableTrace(var);
                  }
                });
              }
            /*
             * Only show this item if the variable is indeed in the list
             */
            if (varMap.containsKey(var.getText()))
              {

                MenuItem removeItem = new MenuItem(
                                                   "Remove from Variable Watches",
                                                   false);
                m.append(removeItem);

                removeItem.setSensitive(true);
                removeItem.addListener(new MenuItemListener()
                {
                  public void menuItemEvent (MenuItemEvent arg0)
                  {
                    removeVar(var);
                  }
                });
              }

            m.showAll();
            m.popup();
          }
        else
          {
            MenuItem scopeItem = new MenuItem("Variable out of scope", false);
            m.append(scopeItem);
            scopeItem.setSensitive(false);
            m.showAll();
            m.popup();
          }

        return true;
      }
    return false;
  }

  private boolean mousedOverMargin (MouseMotionEvent event)
  {
    TextIter iter = this.getIterFromWindowCoords((int) event.getX(),
                                                 (int) event.getY());

    if (this.buf.hasInlineCode(iter.getLineNumber()))
      event.getWindow().setCursor(new Cursor(CursorType.HAND1));
    else
      event.getWindow().setCursor(new Cursor(CursorType.LEFT_PTR));

    return false;
  }

  /*
   * Called when there's mouse movement over the text area
   */
  private boolean mousedOverText (MouseMotionEvent event)
  {
    TextIter iter = this.getIterFromWindowCoords((int) event.getX(),
                                                 (int) event.getY());

    if (this.buf.getScope() == null)
      return false;

    DOMSource source = this.buf.getScope().getData();

    if (source == null)
      return false;

    DOMLine line = source.getLine(iter.getLineNumber());

    if (line == null)
      return false;

    DOMTag tag = line.getTag(iter.getLineOffset());

    if (tag == null)
      {
        event.getWindow().setCursor(new Cursor(CursorType.XTERM));
        return false;
      }

    if (this.lastTag != null)
      {
        if (this.lastTag.getToken().equals(tag.getToken()))
          return false;
      }
    
    /* The two tags are not equal */
    this.lastTag = tag;

    // Check to see if we've moused over a variable
    // Variable var = this.buf.getVariable(iter);
    Variable var = this.buf.getVariable(tag, line);

// TextIter end = this.getIterFromWindowCoords((int) event.getX(),
//                                                (int) event.getY());
//    
//    System.out.println("Event X Y: " + event.getX() + " " + event.getY());
//    iter.moveBackwardWordStart();
//    end.moveBackwardWordStart();
//    System.out.println(iter + " " + end + " |" + iter.getBuffer().getText(iter, end, false) + "| " + iter.getBuffer());
//    iter.moveBackwardLine();
//    end.moveForwardLine();
//    System.out.println("Line: " + iter + " " + end + " |" + iter.getBuffer().getText(iter, end, false) + "| " + iter.getBuffer());
    if (var != null)
      {
        event.getWindow().setCursor(new Cursor(CursorType.HAND1));
        // this.hoverX = (int) event.getX();
        // this.hoverY = (int) event.getY();
      }
    else
      {
        event.getWindow().setCursor(new Cursor(CursorType.XTERM));
      }

    // If that status changed, we need to redraw
    boolean refresh = (this.hoveredVar == null || var == null)
                      && this.hoveredVar != var;

    this.hoveredVar = var;

    if (refresh) 
      this.draw();

    return false;
  }

  private TextIter getIterFromWindowCoords (int x, int y)
  {
    Point p = this.windowToBufferCoords(TextWindowType.TEXT, x, y);
    return this.getIterAtLocation(p.getX(), p.getY());
  }

  private class SourceViewListener
      implements MouseListener, MouseMotionListener
  {

    private SourceView target;

    public SourceViewListener (SourceView target)
    {
      this.target = target;
    }

    public boolean mouseEvent (MouseEvent event)
    {
      if (! event.isOfType(MouseEvent.Type.BUTTON_PRESS))
        return false;

      // Clicked on text area
      if (target.isTextArea(event.getWindow()))
        return target.clickedOnTextArea(event);
      // clicked on the border
      else if (target.isMargin(event.getWindow()))
        return target.clickedOnMargin(event);

      return false;
    }

    public boolean mouseMotionEvent (MouseMotionEvent event)
    {
      Window win = event.getWindow();
      boolean result = false;

      if (target.isMargin(win))
        result = target.mousedOverMargin(event);
      else if (target.isTextArea(win))
        {
          /* If the SW is stopped */
          if (parent.getRunState().getState() == 0)
            result = target.mousedOverText(event);
        }

      event.refireIfHint();
      return result;
    }

  }
}
