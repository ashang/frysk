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

import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
import org.gnu.gdk.Drawable;
import org.gnu.gdk.GC;
import org.gnu.gdk.Point;
import org.gnu.gdk.Window;
import org.gnu.gtk.Label;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.StateType;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextChildAnchor;
import org.gnu.gtk.TextIter;
import org.gnu.gtk.TextView;
import org.gnu.gtk.TextWindowType;
import org.gnu.gtk.WindowType;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;
import org.gnu.pango.Alignment;
import org.gnu.pango.Layout;
import org.jdom.Element;

import frysk.gui.srcwin.PreferenceConstants.Background;
import frysk.gui.srcwin.PreferenceConstants.CurrentLine;
import frysk.gui.srcwin.PreferenceConstants.ExecMarks;
import frysk.gui.srcwin.PreferenceConstants.LineNumbers;
import frysk.gui.srcwin.PreferenceConstants.Margin;
import frysk.gui.srcwin.PreferenceConstants.Text;
import frysk.gui.srcwin.dom.DOMImage;
import frysk.gui.srcwin.dom.DOMInlineInstance;
import frysk.gui.srcwin.dom.DOMSource;

/** 
 * This class is used to add some functionality to TextView that may be needed
 * by the source window but is not directly available or easily accessible in
 * TextView.
 * 
 * This widget uses a SourceBuffer instead of a TextBuffer. A SourceBuffer
 * allows some extras that are needed by the source window such as break-
 * point management.
 * 
 * @author ifoox, ajocksch
 *
 */
public class SourceViewWidget extends TextView implements ExposeListener, MouseListener{

	// my SourceBuffer
	protected SourceBuffer buf;
	
	// preferences model nodes
	protected Preferences topPrefs;
	
	protected Preferences lnfPrefs;
	
	// How far to start writing breakpoints, etc. from the left side of the margin
	protected int marginWriteOffset;
	
	protected TextChildAnchor anchor;
	private SourceWindow parent;
	
	protected boolean expanded = false;

	/**
	 * Constructs a new SourceViewWidget. If you don't specify a buffer before using it,
	 * a default one will be created for you.
	 * 
	 * @param parentPrefs The root node of the preference model to use
	 * @param scope The source file that this widget will be displaying
	 */
	public SourceViewWidget(Preferences parentPrefs, StackLevel scope, SourceWindow parent) {
		this(parentPrefs, new SourceBuffer(scope), parent);
	}
	
	public SourceViewWidget(Preferences parentPrefs, SourceBuffer buffer, SourceWindow parent){
		super(gtk_text_view_new());
		this.parent = parent;
		this.buf = buffer;
		this.setBuffer(this.buf);
		this.topPrefs = parentPrefs;
		this.lnfPrefs = parentPrefs.node(PreferenceConstants.LNF_NODE);
		this.initialize();
	}
	
	/**
	 * Redraws the SourceViewWidget on screen, taking changes in the preference model
	 * into account
	 */
	public void refresh(){
		// Look & Feel
		int r = this.lnfPrefs.getInt(Text.COLOR_PREFIX+"R", Text.DEFAULT.getRed());
		int g = this.lnfPrefs.getInt(Text.COLOR_PREFIX+"G", Text.DEFAULT.getGreen());
		int b = this.lnfPrefs.getInt(Text.COLOR_PREFIX+"B", Text.DEFAULT.getBlue());
		this.setTextColor(StateType.NORMAL, new Color(r,g,b));
		
		r = this.lnfPrefs.getInt(Background.COLOR_PREFIX+"R", Background.DEFAULT.getRed());
		g = this.lnfPrefs.getInt(Background.COLOR_PREFIX+"G", Background.DEFAULT.getGreen());
		b = this.lnfPrefs.getInt(Background.COLOR_PREFIX+"B", Background.DEFAULT.getBlue());
		this.setBaseColor(StateType.NORMAL, new Color(r,g,b));
	
		this.buf.updatePreferences(this.topPrefs);
		
		// Sidebar
		if(this.lnfPrefs.getBoolean(LineNumbers.SHOW, true)){
			Layout lo = new Layout(this.getContext());
			lo.setText(""+(this.buf.getLastLine()+1));
			this.marginWriteOffset = lo.getPixelWidth();
		}
		else{
			this.setBorderWindowSize(TextWindowType.LEFT, 20);
			Layout lo = new Layout(this.getContext());
			lo.setText(" i ");
			this.marginWriteOffset = 0;
		}
		
		if(this.lnfPrefs.getBoolean(ExecMarks.SHOW, true)){
			this.setBorderWindowSize(TextWindowType.LEFT, this.marginWriteOffset+40);
		}
		else{
			this.setBorderWindowSize(TextWindowType.LEFT, this.marginWriteOffset+20);
		}
			
	}

	/**
	 * Returns the SourceBuffer being used
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
		if(event.isOfType(ExposeEvent.Type.NO_EXPOSE) ||
				!event.getWindow().equals(this.getWindow(TextWindowType.LEFT)))
			return false;
		
		this.drawMargin();
		
		return false;
	} 
	
	/**
	 * Called in response to the user clicking on the text area. In the future
	 * this will be used to be able to mouse-over variables and show their contents
	 */
	public boolean mouseEvent(MouseEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();
		
		// Middle click over the main text area will trigger the variable-finding
		if(event.getButtonPressed() == MouseEvent.BUTTON3 
				&& event.isOfType(MouseEvent.Type.BUTTON_PRESS) &&
				event.getWindow().equals(this.getWindow(TextWindowType.TEXT))){
			
			Point p = this.windowToBufferCoords(TextWindowType.TEXT, x, y);
			
			TextIter iter = this.getIterAtLocation(p.getX(), p.getY());
			
			final Variable var = this.buf.getVariable(iter);
			
			Menu m = new Menu();
			MenuItem mi = new MenuItem("Display variable value...", false);
			MenuItem mi2 = new MenuItem("Add Trace", false);
			m.append(mi);
			m.append(mi2);
			if(var != null){
				mi.addListener(new MenuItemListener() {
					public void menuItemEvent(MenuItemEvent arg0) {
						org.gnu.gtk.Window popup = new org.gnu.gtk.Window(WindowType.TOPLEVEL);
						popup.add(new Label(var.getName()+ " = 0xfeedcalf"));
						popup.showAll();
					}
				});
				mi2.addListener(new MenuItemListener() {
				
					public void menuItemEvent(MenuItemEvent arg0) {
						SourceViewWidget.this.parent.addVariableTrace(var);
					}
				
				});
			}
			else{
				mi.setSensitive(false);
				mi2.setSensitive(false);
			}

			m.showAll();
			m.popup();
			
			return true;
		}
		// clicked on the border
		else if(event.getWindow().equals(this.getWindow(TextWindowType.LEFT))
				&& event.isOfType(MouseEvent.Type.BUTTON_PRESS)){
			Point p = this.windowToBufferCoords(TextWindowType.TEXT, 0, y);
			
			TextIter iter = this.getIterAtLocation(p.getX(), p.getY());
			
			int theLine = iter.getLineNumber();
			boolean overNested = false;
			
			// We want to ignore mouse clicks in the margin next to 
			// expanded inline code
			if(theLine == this.buf.getCurrentLine() + 1 && expanded)
				return false;
			
			if(theLine > this.buf.getCurrentLine() && expanded){
				theLine--;
				overNested = true;
			}
			
			final int lineNum = theLine;
			
			// only popup a window if the line is executable
			if(event.getButtonPressed() == MouseEvent.BUTTON3 &&
					this.buf.isLineExecutable(lineNum) &&
					(!expanded || overNested)){
				Menu m = new Menu();
				MenuItem mi = new MenuItem("Breakpoint information...", false);
				mi.addListener(new MenuItemListener() {
					public void menuItemEvent(MenuItemEvent arg0) {
						org.gnu.gtk.Window popup = new org.gnu.gtk.Window(WindowType.TOPLEVEL);
						popup.add(new Label("Line: " + (lineNum + 1)));
						popup.showAll();
					}
				});
				m.append(mi);
				MenuItem mi2 = new MenuItem("Customize breakpoint actions...", false);
				m.append(mi2);
				if(!this.buf.isLineBroken(lineNum)){ // no breakpoint, no info to show
					mi.setSensitive(false);
					mi2.setSensitive(false);
				}
				m.append(new MenuItem()); // Separator
				mi = new MenuItem("Toggle Breakpoint", false);
				m.append(mi);
				mi.addListener(new MenuItemListener() {
					public void menuItemEvent(MenuItemEvent event) {
						SourceViewWidget.this.buf.toggleBreakpoint(lineNum);
					}
				});
				m.popup();
				m.showAll();
			}
			
			// Left click in the margin for a line with inline code - toggle the display of it
			if(event.getButtonPressed() == MouseEvent.BUTTON1 &&
					lineNum == this.buf.getCurrentLine() &&
					this.buf.hasInlineCode(lineNum)){
				this.toggleChild();
			}
		}
		
		return false;
	}
	
	/**
	 * Scrolls the TextView so that the given line is visible in the widget
	 * @param lineNum The line to scroll to
	 */
	public void scrollToLine(int lineNum){
		this.scrollToIter(this.buf.getLineIter(lineNum), 0);
	}
	
	/**
	 * Finds the next instance of toFind in the buffer and highlights it.
	 * returns true if succesful, false otherwise
	 * 
	 * @param toFind The string to find
	 * @param caseSensitive Whether to do a case sensitive search
	 * @return If the search was successful
	 */
	public boolean findNext(String toFind, boolean caseSensitive){
		return this.buf.findNext(toFind, caseSensitive, false);
	}
	
	/**
	 * Finds the previous instance of toFind in the buffer and highlights it.
	 * Returns true if succesful, false otherwise
	 * 
	 * @param toFind The string to find
	 * @param caseSensitive Whether to do a case sensitive search
	 * @return If the search was successful
	 */
	public boolean findPrevious(String toFind, boolean caseSensitive){
		return this.buf.findPrevious(toFind, caseSensitive);
	}
	
	/**
	 * Finds all instances of toFind in the current buffer and highlights
	 * them. Returns true if successful, false otherwise
	 * 
	 * @param toFind The string to find in the buffer
	 * @param caseSensitive Whether to do a case sensitive search
	 * @return If the search was successful
	 */
	public boolean highlightAll(String toFind, boolean caseSensitive){
		return this.buf.findNext(toFind, caseSensitive, true);
	}
	
	/**
	 * Scrolls the TextView to show the current location of the found text
	 */
	public void scrollToFound(){
		this.scrollToIter(this.buf.getStartCurrentFind(), 0);
	}
	
	public void load(StackLevel data){
		this.buf.setScope(data);
		this.expanded = false;
		this.anchor = null;
	}
	
	public void setSubscopeAtCurrentLine(SourceViewWidget child){
		TextIter line = buf.getLineIter(buf.getCurrentLine() + 1);
		
		if(anchor != null)
			buf.deleteText(line, buf.getIter(line.getOffset()+1));
		else
			buf.insertText(line, "\n");
		this.anchor = buf.createChildAnchor(buf.getLineIter(buf.getCurrentLine() + 1));
		
		this.expanded = true;
		this.addChild(child, anchor);
	}
	
	public void clearSubscopeAtCurrentLine(){
		TextIter line = buf.getLineIter(buf.getCurrentLine() + 1);
		buf.deleteText(line, buf.getIter(line.getOffset()+2));
		this.expanded = false;
		this.anchor = null;
	}
	
	/*---------------------------*
	 * PRIVATE METHODS           *
	 *---------------------------*/
	
	/*
	 * Performs some operations before the window is shown
	 */
	private void initialize(){
		// Set all preference-related data
		this.refresh();
		
		// Stuff that never changes
		this.setLeftMargin(3);
		this.setEditable(false);
		this.setCursorVisible(false);
		
		// Listeners
		this.addListener((ExposeListener) this);
		this.addListener((MouseListener) this);
		
		this.showAll();
	}

	public void toggleChild() {
		if(!expanded){
			expanded = true;
			
			DOMInlineInstance instance = this.buf.getInlineInstance(this.buf.getCurrentLine());
			
			Element node = instance.getElement();
			while(!node.getName().equals(DOMImage.IMAGE_NODE))
				node = node.getParentElement();
			
			DOMSource scope = new DOMImage(node).getSource(instance.getDeclaration().getSourceName());
			InlineViewer nested = new InlineViewer(this.topPrefs, this.parent, 
					scope, instance);
			this.setSubscopeAtCurrentLine(nested);
		}
		else{
			expanded = false;
			this.clearSubscopeAtCurrentLine();
		}
			
	}
	
	/*
	 * Function responsible for drawing the side area where breakpoints, etc. are
	 * drawn. Called either in response to an expose event or when a preference 
	 * changes
	 */
	protected void drawMargin(){
		Window drawingArea = this.getWindow(TextWindowType.LEFT);
		
		// draw the background for the margin
		GC context = new GC((Drawable) drawingArea);
		int r = this.lnfPrefs.getInt(Margin.COLOR_PREFIX+"R", Margin.DEFAULT.getRed());
		int g = this.lnfPrefs.getInt(Margin.COLOR_PREFIX+"G", Margin.DEFAULT.getGreen());
		int b = this.lnfPrefs.getInt(Margin.COLOR_PREFIX+"B", Margin.DEFAULT.getBlue());
		context.setRGBForeground(new Color(r, g, b));
		drawingArea.drawRectangle(context, true, 0, 0, drawingArea.getWidth(), drawingArea.getHeight());
		
		// get preference settings
		boolean showLines = this.lnfPrefs.getBoolean(LineNumbers.SHOW, true);
		boolean showMarks = this.lnfPrefs.getBoolean(ExecMarks.SHOW, true);
		
		// get the y coordinates for the top and bottom of the window
		int minY = drawingArea.getClipRegion().getClipbox().getY();
		int maxY = minY+drawingArea.getClipRegion().getClipbox().getHeight();
		
		// find out what the actual starting coordinates of the first line on screen is
		TextIter firstIter = this.getIterAtLocation(this.windowToBufferCoords(TextWindowType.LEFT, 0, minY));
		VerticalLineRange firstRange = this.getLineYRange(firstIter);
		int actualFirstStart = this.bufferToWindowCoords(TextWindowType.LEFT, 0, firstRange.getY()).getY();
		
		// get the line numbers we'll be drawing
		int firstLine = firstIter.getLineNumber();
		int lastLine = this.getIterAtLocation(this.windowToBufferCoords(TextWindowType.LEFT, 0, maxY)).getLineNumber();

		// Get Color to draw the text in
		r = this.lnfPrefs.getInt(LineNumbers.COLOR_PREFIX+"R", LineNumbers.DEFAULT.getRed());
		g = this.lnfPrefs.getInt(LineNumbers.COLOR_PREFIX+"G", LineNumbers.DEFAULT.getGreen());
		b = this.lnfPrefs.getInt(LineNumbers.COLOR_PREFIX+"B", LineNumbers.DEFAULT.getBlue());
		context.setRGBForeground(new Color(r,g,b));
		
		// gets current line color
		int lineR = this.lnfPrefs.getInt(CurrentLine.COLOR_PREFIX+"R", CurrentLine.DEFAULT.getRed());
		int lineG = this.lnfPrefs.getInt(CurrentLine.COLOR_PREFIX+"G", CurrentLine.DEFAULT.getGreen());
		int lineB = this.lnfPrefs.getInt(CurrentLine.COLOR_PREFIX+"B", CurrentLine.DEFAULT.getBlue());
		
		// gets executable mark color
		int markR = this.lnfPrefs.getInt(ExecMarks.COLOR_PREFIX+"R", ExecMarks.DEFAULT.getRed());
		int markG = this.lnfPrefs.getInt(ExecMarks.COLOR_PREFIX+"G", ExecMarks.DEFAULT.getGreen());
		int markB = this.lnfPrefs.getInt(ExecMarks.COLOR_PREFIX+"B", ExecMarks.DEFAULT.getBlue());
		
		int currentHeight = 0;		
		int actualIndex = firstLine;
		boolean skipNextLine = false;
		
		int drawingHeight = 0;
        int lineHeight = 0;
		int gapHeight = 0;
		
		// If the refresh is starting after the current line, we have to add that offset in to
		// make sure the gap in line numbers is maintained
		if(expanded && firstLine > this.buf.getCurrentLine())
			gapHeight = this.getLineYRange(this.getBuffer().getLineIter(this.buf.getCurrentLine() + 1)).getHeight();
		
		for(int i = firstLine; i <= lastLine && i < this.buf.getLineCount(); i++){
			
			if(i > this.buf.getCurrentLine()){
				drawingHeight = currentHeight + gapHeight;
                if(expanded)
                    lineHeight = this.getLineYRange(this.getBuffer().getLineIter(i+1)).getHeight();
                else
                    lineHeight = this.getLineYRange(this.getBuffer().getLineIter(i)).getHeight();
            }
			else{
				drawingHeight = currentHeight;
                lineHeight = this.getLineYRange(this.getBuffer().getLineIter(i)).getHeight();
            }
			
			int iconStart = lineHeight/2;
			
			if(skipNextLine){
				skipNextLine = false;
				
				gapHeight = this.getLineYRange(this.getBuffer().getLineIter(actualIndex++)).getHeight();
				
				i--;
				continue;
			}
			
			// For the current line, do some special stuff
			if(i == this.buf.getCurrentLine()){
					
				context.setRGBForeground(new Color(lineR, lineG, lineB));
				if(showMarks)
					drawingArea.drawRectangle(context, true, 0, actualFirstStart+drawingHeight, 
							this.marginWriteOffset+40, lineHeight);
				else
					drawingArea.drawRectangle(context, true, 0, actualFirstStart+drawingHeight, 
							this.marginWriteOffset+20, lineHeight);
				context.setRGBForeground(new Color(r,g,b));
				
				if(this.buf.hasInlineCode(i)){
					context.setRGBForeground(new Color(markR,markG,markB));
					Layout lo = this.createLayout("i");
					lo.setAlignment(Alignment.RIGHT);
					if(showMarks)
						drawingArea.drawLayout(context, this.marginWriteOffset+25, actualFirstStart+drawingHeight, lo);
					else
						drawingArea.drawLayout(context, this.marginWriteOffset+5, actualFirstStart+drawingHeight, lo);
					context.setRGBForeground(new Color(r,g,b));
					
					if(this.expanded)
						skipNextLine = true;
				}
			}
			
			
			// If it is executable, draw a mark
			if(showMarks && this.buf.isLineExecutable(i)){
				context.setRGBForeground(new Color(markR,markG,markB));
				drawingArea.drawLine(context, this.marginWriteOffset+5, actualFirstStart+drawingHeight+iconStart, 
						this.marginWriteOffset+12, actualFirstStart+drawingHeight+iconStart);
				context.setRGBForeground(new Color(r,g,b));
			}
			
			// Draw line numbers
			if(showLines)
				drawLineNumber(drawingArea, context, actualFirstStart + drawingHeight, i);
			
			// draw breakpoints
			if(this.buf.isLineBroken(i)){
				int iconHeight = lineHeight - 8;
				
				context.setRGBForeground(new Color(65535,0,0));
				drawingArea.drawRectangle(context, true, this.marginWriteOffset+5, actualFirstStart+drawingHeight+4, iconHeight, iconHeight);
				context.setRGBForeground(new Color(r,g,b));
			}
			
			// update height for next line
			currentHeight += this.getLineYRange(this.getBuffer().getLineIter(actualIndex++)).getHeight();
		}
	}

    protected void drawLineNumber(Window drawingArea, GC context, int drawingHeight, int number) {
        Layout lo = this.createLayout(""+(number+1));
        lo.setAlignment(Alignment.RIGHT);
        lo.setWidth(this.marginWriteOffset);
        
        drawingArea.drawLayout(context, this.marginWriteOffset, drawingHeight, lo);
    }
	
}
