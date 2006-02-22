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
import frysk.dom.DOMSource;
import frysk.gui.srcwin.prefs.BooleanPreference;
import frysk.gui.srcwin.prefs.ColorPreference;
import frysk.gui.srcwin.prefs.IntPreference;
import frysk.gui.srcwin.prefs.PreferenceManager;

/**
 * This class is used to add some functionality to TextView that may be needed
 * by the source window but is not directly available or easily accessible in
 * TextView.
 * 
 * This widget uses a SourceBuffer instead of a TextBuffer. A SourceBuffer
 * allows some extras that are needed by the source window such as break- point
 * management.
 * 
 * @author ifoox, ajocksch
 * 
 */
public class SourceView extends TextView implements View, ExposeListener {

	// my SourceBuffer
	protected SourceBuffer buf;

	// How far to start writing breakpoints, etc. from the left side of the
	// margin
	protected int marginWriteOffset;

	protected TextChildAnchor anchor;

	private SourceWindow parent;

	protected boolean expanded = false;

	// keep this around, we'll be needing it
	private GC myContext;

	private InlineSourceView child;

	private SourceViewListener listener;
	
	/**
	 * Constructs a new SourceViewWidget. If you don't specify a buffer before
	 * using it, a default one will be created for you.
	 * @param scope
	 *            The source file that this widget will be displaying
	 * @param parent
	 *            The SourceWindow that this SourceViewWidget is contained in
	 */
	public SourceView(StackLevel scope, SourceWindow parent) {
		this(new SourceBuffer(scope), parent);
	}

	public SourceView(StackLevel scope, SourceWindow parent, int mode){
		this(new SourceBuffer(scope, mode), parent);
	}
	
	/**
	 * Constructs a new SourceViewWidget using the previously created buffer
	 * @param buffer
	 *            The sourceBuffer to use as the data for this object
	 * @param parent
	 *            The SourceWindow this object is contained within
	 */
	public SourceView(SourceBuffer buffer, SourceWindow parent) {
		super(gtk_text_view_new());
		this.parent = parent;
		this.buf = buffer;
		this.listener = new SourceViewListener(this);
		this.setBuffer(this.buf);
		this.initialize();
	}
	
	/**
	 * Redraws the SourceViewWidget on screen, taking changes in the preference
	 * model into account
	 */
	public void refresh() {
		// Look & Feel
		Color tmpColor = PreferenceManager.getColorPreferenceValue(ColorPreference.TEXT);
		this.setTextColor(StateType.NORMAL, tmpColor);

		tmpColor = PreferenceManager.getColorPreferenceValue(ColorPreference.BACKGROUND);
		this.setBaseColor(StateType.NORMAL, tmpColor);

		// Sidebar
		if (PreferenceManager.getBooleanPreferenceValue(BooleanPreference.LINE_NUMS)) {
			Layout lo = new Layout(this.getContext());
			lo.setText("" + (this.buf.getLastLine() + 1));
			this.marginWriteOffset = lo.getPixelWidth();
		} else {
			this.setBorderWindowSize(TextWindowType.LEFT, 20);
			this.marginWriteOffset = 0;
		}

		if (PreferenceManager.getBooleanPreferenceValue(BooleanPreference.EXEC_MARKS)) {
			this.setBorderWindowSize(TextWindowType.LEFT,
					this.marginWriteOffset + 40);
		} else {
			this.setBorderWindowSize(TextWindowType.LEFT,
					this.marginWriteOffset + 20);
		}

		// refresh the inlined scopes, if they exist
		if (this.child != null)
			this.child.refresh();
	}

	/**
	 * Returns the SourceBuffer being used
	 * 
	 * @return The SourceBuffer used in the widget.
	 */
	public TextBuffer getBuffer() {
		return buf;
	}

	/**
	 * Implementation from ExposeListener Interface. When the
	 */
	public boolean exposeEvent(ExposeEvent event) {
		// Ignore events that aren't expose events or don't have anything
		// to do with the sidebar
		if (event.isOfType(ExposeEvent.Type.NO_EXPOSE)
				|| !event.getWindow().equals(
						this.getWindow(TextWindowType.LEFT)))
			return false;

		this.drawMargin();

		return false;
	}
	
	/**
	 * Scrolls the TextView so that the given line is visible in the widget
	 * 
	 * @param lineNum
	 *            The line to scroll to
	 */
	public void scrollToLine(int lineNum) {
		this.scrollToIter(this.buf.getLineIter(lineNum - 1), 0);
	}

	/**
	 * Finds the next instance of toFind in the buffer and highlights it.
	 * returns true if succesful, false otherwise
	 * 
	 * @param toFind
	 *            The string to find
	 * @param caseSensitive
	 *            Whether to do a case sensitive search
	 * @return If the search was successful
	 */
	public boolean findNext(String toFind, boolean caseSensitive) {
		boolean result =  this.buf.findNext(toFind, caseSensitive, false);
		if(result)
			this.scrollToIter(this.buf.getStartCurrentFind(), 0);
		
		return result;
	}

	/**
	 * Finds the previous instance of toFind in the buffer and highlights it.
	 * Returns true if succesful, false otherwise
	 * 
	 * @param toFind
	 *            The string to find
	 * @param caseSensitive
	 *            Whether to do a case sensitive search
	 * @return If the search was successful
	 */
	public boolean findPrevious(String toFind, boolean caseSensitive) {
		boolean result =  this.buf.findPrevious(toFind, caseSensitive);
		
		if(result)
			this.scrollToIter(this.buf.getStartCurrentFind(), 0);
		
		return result;
	}

	/**
	 * Finds all instances of toFind in the current buffer and highlights them.
	 * Returns true if successful, false otherwise
	 * 
	 * @param toFind
	 *            The string to find in the buffer
	 * @param caseSensitive
	 *            Whether to do a case sensitive search
	 * @return If the search was successful
	 */
	public boolean highlightAll(String toFind, boolean caseSensitive) {
		return this.buf.findNext(toFind, caseSensitive, true);
	}
	/**
	 * Loads the contents of the provided StackLevel into this view
	 * 
	 * @param data
	 *            The new stack frame to load.
	 */
	public void load(StackLevel data){
		this.buf.setScope(data);
		this.expanded = false;
		this.anchor = null;
	}
	
	public void setMode(int mode){
		this.buf.setMode(mode);
		this.refresh();
	}
	
	/**
	 * Sets the inlined subscope at the current line to be the object provided.
	 * 
	 * @param child
	 *            The inlined scope to display
	 */
	public void setSubscopeAtCurrentLine(InlineSourceView child) {
		// Only inline source viewers in a source viewer
		if(!(child instanceof InlineSourceView))
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
	 * 
	 */
	public void clearSubscopeAtCurrentLine() {
		this.buf.clearAnchorAtCurrentLine();
		this.expanded = false;
		this.anchor = null;
	}

	/*
	 * Toggles the visibility of the inlined code at the current line.
	 */
	public void toggleChild() {
		if (!expanded) {
			expanded = true;

			DOMInlineInstance instance = this.buf.getInlineInstance(this.buf
					.getCurrentLine());

			DOMSource scope = instance.getDeclaration().getSource();
			InlineSourceView nested = new InlineSourceView(this.parent,
					scope, instance);
			this.setSubscopeAtCurrentLine(nested);
		} else {
			expanded = false;
			this.clearSubscopeAtCurrentLine();
		}

	}

	public void scrollToFunction(String markName) {
		if (this.buf.getFunctions().contains(markName)) {
			TextMark mark = this.buf.getMark(markName);
			this.scrollToMark(mark, 0);
		}
	}

	public Vector getFunctions() {
		return this.buf.getFunctions();
	}

	public StackLevel getScope() {
		return this.buf.getScope();
	}
	
	/*
	 * Function responsible for drawing the side area where breakpoints, etc.
	 * are drawn. Called either in response to an expose event or when a
	 * preference changes
	 */
	protected void drawMargin() {
		Window drawingArea = this.getWindow(TextWindowType.LEFT);

		// draw the background for the margin
		if (this.myContext == null)
			this.myContext = new GC((Drawable) drawingArea);

		Color tmp = PreferenceManager.getColorPreferenceValue(ColorPreference.MARGIN);
		myContext.setRGBForeground(tmp);
		drawingArea.drawRectangle(this.myContext, true, 0, 0, drawingArea
				.getWidth(), drawingArea.getHeight());

		// get preference settings
		boolean showLines = PreferenceManager.getBooleanPreferenceValue(BooleanPreference.LINE_NUMS);
		boolean showMarks = PreferenceManager.getBooleanPreferenceValue(BooleanPreference.EXEC_MARKS);

		// get the y coordinates for the top and bottom of the window
		int minY = drawingArea.getClipRegion().getClipbox().getY();
		int maxY = minY + drawingArea.getClipRegion().getClipbox().getHeight();

		// find out what the actual starting coordinates of the first line on
		// screen is
		TextIter firstIter = this.getIterAtLocation(this.windowToBufferCoords(
				TextWindowType.LEFT, 0, minY));
		VerticalLineRange firstRange = this.getLineYRange(firstIter);
		int actualFirstStart = this.bufferToWindowCoords(TextWindowType.LEFT,
				0, firstRange.getY()).getY();

		// get the line numbers we'll be drawing
		int firstLine = firstIter.getLineNumber();
		int lastLine = this.getIterAtLocation(
				this.windowToBufferCoords(TextWindowType.LEFT, 0, maxY))
				.getLineNumber();

		// Get Color to draw the text in
		Color lineColor = PreferenceManager.getColorPreferenceValue(ColorPreference.LINE_NUMBER);
		this.myContext.setRGBForeground(lineColor);

		// gets current line color
		Color currentLine = PreferenceManager.getColorPreferenceValue(ColorPreference.CURRENT_LINE);

		// gets executable mark color
		Color markColor = PreferenceManager.getColorPreferenceValue(ColorPreference.EXEC_MARKS);

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
			gapHeight = this
					.getLineYRange(
							this.getBuffer().getLineIter(
									this.buf.getCurrentLine() + 1)).getHeight();

		for (int i = firstLine; i <= lastLine && i < this.buf.getLineCount(); i++) {

			if (i > this.buf.getCurrentLine()) {
				drawingHeight = currentHeight + gapHeight;
				if (expanded)
					lineHeight = this.getLineYRange(
							this.getBuffer().getLineIter(i + 1)).getHeight();
				else
					lineHeight = this.getLineYRange(
							this.getBuffer().getLineIter(i)).getHeight();
			} else {
				drawingHeight = currentHeight;
				lineHeight = this
						.getLineYRange(this.getBuffer().getLineIter(i))
						.getHeight();
			}

			int iconStart = lineHeight / 2;

			if (skipNextLine) {
				skipNextLine = false;

				gapHeight = this.getLineYRange(
						this.getBuffer().getLineIter(actualIndex++))
						.getHeight();

				i--;
				continue;
			}

			// For the current line, do some special stuff
			if (i == this.buf.getCurrentLine()) {

				this.myContext.setRGBForeground(currentLine);
				if (showMarks)
					drawingArea.drawRectangle(this.myContext, true, 0,
							actualFirstStart + drawingHeight,
							this.marginWriteOffset + 40, lineHeight);
				else
					drawingArea.drawRectangle(this.myContext, true, 0,
							actualFirstStart + drawingHeight,
							this.marginWriteOffset + 20, lineHeight);
				this.myContext.setRGBForeground(lineColor);

				if (this.buf.hasInlineCode(i)) {
					this.myContext.setRGBForeground(markColor);
					Layout lo = this.createLayout("i");
					lo.setAlignment(Alignment.RIGHT);
					if (showMarks)
						drawingArea.drawLayout(this.myContext,
								this.marginWriteOffset + 25, actualFirstStart
										+ drawingHeight, lo);
					else
						drawingArea.drawLayout(this.myContext,
								this.marginWriteOffset + 5, actualFirstStart
										+ drawingHeight, lo);
					this.myContext.setRGBForeground(lineColor);

					if (this.expanded)
						skipNextLine = true;
				}
			}

			// If it is executable, draw a mark
			if (showMarks && this.buf.isLineExecutable(i)) {
				this.myContext.setRGBForeground(markColor);
				drawingArea.drawLine(this.myContext,
						this.marginWriteOffset + 5, actualFirstStart
								+ drawingHeight + iconStart,
						this.marginWriteOffset + 12, actualFirstStart
								+ drawingHeight + iconStart);
				this.myContext.setRGBForeground(lineColor);
			}

			// Draw line numbers
			if (showLines)
				drawLineNumber(drawingArea, this.myContext, actualFirstStart
						+ drawingHeight, i);

			// draw breakpoints
			if (this.buf.isLineBroken(i)) {
				int iconHeight = lineHeight - 8;

				this.myContext.setRGBForeground(new Color(65535, 0, 0));
				drawingArea.drawRectangle(this.myContext, true,
						this.marginWriteOffset + 5, actualFirstStart
								+ drawingHeight + 4, iconHeight, iconHeight);
				this.myContext.setRGBForeground(lineColor);
			}

			// update height for next line
			currentHeight += this.getLineYRange(
					this.getBuffer().getLineIter(actualIndex++)).getHeight();
		}
	}

	/**
	 * Draws the line corresponding to the number provided. Note that because of
	 * inlined code, initial offsets or other widgets embedded in the source
	 * code this may not be the number passed to this function.
	 * 
	 * @param drawingArea
	 *            The org.gnu.gdk.Window to draw on
	 * @param context
	 *            The GC to use
	 * @param drawingHeight
	 *            The height that we should draw at within the window
	 * @param number
	 *            The number to use to calcuate what number should be drawn
	 */
	protected void drawLineNumber(Window drawingArea, GC context,
			int drawingHeight, int number) {
		Layout lo = this.createLayout("" + (number + 1));
		lo.setAlignment(Alignment.RIGHT);
		lo.setWidth(this.marginWriteOffset);

		drawingArea.drawLayout(context, this.marginWriteOffset, drawingHeight,
				lo);
	}
	
	/*---------------------------*
	 * PRIVATE METHODS           *
	 *---------------------------*/

	/*
	 * Performs some operations before the window is shown
	 */
	private void initialize() {
		FontDescription desc = new FontDescription();
		desc.setFamily("Monospace");
		this.setFont(desc);
		
		// Set all preference-related data
		this.refresh();

		// Stuff that never changes
		this.setLeftMargin(3);
		this.setEditable(false);
		this.setCursorVisible(false);

		// Listeners
		this.addListener((ExposeListener) this);
		this.addListener((MouseListener) listener);
		this.addListener((MouseMotionListener) listener);
		
		// Preferences
		PreferenceManager.addPreference(new IntPreference(IntPreference.INLINE_LEVELS), PreferenceManager.LNF_NODE);
		PreferenceManager.addPreference(new ColorPreference(ColorPreference.EXEC_MARKS), PreferenceManager.LNF_NODE);

		this.showAll();
	}
	
	private boolean isTextArea(Window win){
		return win.equals(this.getWindow(TextWindowType.TEXT));
	}
	
	private boolean isMargin(Window win){
		return win.equals(this.getWindow(TextWindowType.LEFT));
	}
	
	private boolean clickedOnMargin(MouseEvent event){
		TextIter iter = this.getIterFromWindowCoords(0, (int)event.getY());

		int theLine = iter.getLineNumber();
		boolean overNested = false;

		// We want to ignore mouse clicks in the margin next to
		// expanded inline code
		if (theLine == this.buf.getCurrentLine() + 1 && expanded)
			return false;

		if (theLine > this.buf.getCurrentLine() && expanded) {
			theLine--;
			overNested = true;
		}

		final int lineNum = theLine;

		// only popup a window if the line is executable
		if (event.getButtonPressed() == MouseEvent.BUTTON3
				&& this.buf.isLineExecutable(lineNum)
				&& (!expanded || overNested)) {
			Menu m = new Menu();
			MenuItem mi = new MenuItem("Breakpoint information...", false);
			mi.addListener(new MenuItemListener() {
				public void menuItemEvent(MenuItemEvent arg0) {
					org.gnu.gtk.Window popup = new org.gnu.gtk.Window(
							WindowType.TOPLEVEL);
					popup.add(new Label("Line: " + (lineNum + 1)));
					popup.showAll();
				}
			});
			m.append(mi);
			MenuItem mi2 = new MenuItem("Customize breakpoint actions...",
					false);
			m.append(mi2);
			if (!this.buf.isLineBroken(lineNum)) { // no breakpoint, no
				// info to show
				mi.setSensitive(false);
				mi2.setSensitive(false);
			}
			m.append(new MenuItem()); // Separator
			mi = new MenuItem("Toggle Breakpoint", false);
			m.append(mi);
			mi.addListener(new MenuItemListener() {
				public void menuItemEvent(MenuItemEvent event) {
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
				&& this.buf.hasInlineCode(lineNum)) {
			this.toggleChild();
		}
		
		return true;
	}
	
	private boolean clickedOnTextArea(MouseEvent event){
		// Right click over the main text area will trigger the
		// variable-finding
		if (event.getButtonPressed() == MouseEvent.BUTTON3){
			TextIter iter = this.getIterFromWindowCoords(
					(int)event.getX(), (int)event.getY());
			final Variable var = this.buf.getVariable(iter);

			Menu m = new Menu();
			MenuItem mi = new MenuItem("Display variable value...", false);
			MenuItem mi2 = new MenuItem("Add Trace", false);
			m.append(mi);
			m.append(mi2);
			if (var != null) {
				mi.addListener(new MenuItemListener() {
					public void menuItemEvent(MenuItemEvent arg0) {
						org.gnu.gtk.Window popup = new org.gnu.gtk.Window(
								WindowType.TOPLEVEL);
						popup.add(new Label(var.getName() + " = 0xfeedcalf"));
						popup.showAll();
					}
				});
				mi2.addListener(new MenuItemListener() {
					public void menuItemEvent(MenuItemEvent arg0) {
						SourceView.this.parent.addVariableTrace(var);
					}
				});
			} else {
				mi.setSensitive(false);
				mi2.setSensitive(false);
			}

			m.showAll();
			m.popup();

			return true;
		}
		return false;
	}
	
	private boolean mousedOverMargin(MouseMotionEvent event){
		TextIter iter = this.getIterFromWindowCoords(
				(int)event.getX(), (int)event.getY());
		
		if(this.buf.hasInlineCode(iter.getLineNumber()))
			event.getWindow().setCursor(new Cursor(CursorType.HAND1));
		else
			event.getWindow().setCursor(new Cursor(CursorType.LEFT_PTR));
			
		return false;
	}
	
	private boolean mousedOverText(MouseMotionEvent event){
		TextIter iter = this.getIterFromWindowCoords(
				(int)event.getX(), (int)event.getY());
		Variable var = this.buf.getVariable(iter);
		
		if(var != null)
			event.getWindow().setCursor(new Cursor(CursorType.HAND1));
		else
			event.getWindow().setCursor(new Cursor(CursorType.XTERM));
		
		return false;
	}
	
	private TextIter getIterFromWindowCoords(int x, int y){
		Point p = this.windowToBufferCoords(TextWindowType.TEXT, x, y);
		return this.getIterAtLocation(p.getX(), p.getY());
	}
	
	private class SourceViewListener implements MouseListener, MouseMotionListener{

		private SourceView target;
		
		public SourceViewListener(SourceView target){
			this.target = target;
		}
		
		public boolean mouseEvent(MouseEvent event) {
			if(!event.isOfType(MouseEvent.Type.BUTTON_PRESS))
				return false;
			
			// Clicked on text area
			if(target.isTextArea(event.getWindow())) 
				return target.clickedOnTextArea(event);
			// clicked on the border
			else if (target.isMargin(event.getWindow()))
				return target.clickedOnMargin(event);

			return false;
		}

		public boolean mouseMotionEvent(MouseMotionEvent event) {
			Window win = event.getWindow();
			boolean result = false;
			
			if(target.isMargin(win))
				result = target.mousedOverMargin(event);
			else if(target.isTextArea(win))
				result =  target.mousedOverText(event);
			
			event.refireIfHint();
			return result;
		}
		
	}
}
