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

import org.gnu.gdk.GC;
import org.gnu.gdk.Point;
import org.gnu.gdk.Window;
import org.gnu.gtk.EventBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.TextIter;
import org.gnu.gtk.TextWindowType;
import org.gnu.gtk.WindowType;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.pango.Alignment;
import org.gnu.pango.Layout;

import frysk.dom.DOMInlineInstance;
import frysk.dom.DOMSource;
import frysk.gui.srcwin.PreferenceConstants.Inline;

/**
 * @author ajocksch
 *
 */
public class InlineViewer extends SourceViewWidget {
	
	private InlineViewer previous;
	private InlineViewer next;
	
	private int depth;
    
	private boolean showingEllipsis = false;
	
	/**
	 * Creates a new InlineViewer
	 * @param parentPrefs The preference model to use
	 * @param top The SourceWindow that contains this InlineViewer and it's
	 *     siblings
	 * @param scope The DOMSource that contains the function that this
	 *     InlineViewer will be displaying
	 * @param instance The inline instance to display
	 */
	public InlineViewer(Preferences parentPrefs, SourceWindow top, 
			DOMSource scope, DOMInlineInstance instance) {
		super(parentPrefs, new InlineBuffer(scope, instance), top);
		this.setBorderWidth(1);
		this.depth = 1;
	}
    
	public void setSubscopeAtCurrentLine(SourceViewWidget nested){
		if(nested instanceof InlineViewer){
			InlineViewer casted = (InlineViewer) nested;
			casted.depth = this.depth + 1;
			this.next = casted;
			casted.previous = this;
		}
		
		super.setSubscopeAtCurrentLine(nested);
	}
	
	public void clearSubscopeAtCurrentLine(){
		this.next.previous = null;
		this.next = null;
		
		super.clearSubscopeAtCurrentLine();
	}
	
	/**
	 * Coordinates the adding and removing of child scopes, as well as moving
	 * the visible scopes up or down
	 */
	public void toggleChild(){
		int limit = this.lnfPrefs.getInt(Inline.NUM_LEVELS, Inline.NUM_LEVELS_DEFAULT);
		
		if(!this.expanded){
			// Case 1: depth less than max, this level is not expanded.
			// Action: Add a new inline viewer to this subscope (delegate to superclass)
			if(this.depth < limit){
				super.toggleChild();
			}
			// Case 2: depth greater than max, this level not expanded.
			// Action: Move down ourselves, then tell our anscestor to move down as well
			else{
				this.moveDown();
			}
		}
		else{
			// Find the top inline viewer
			InlineViewer top = this;
			while(top.previous != null)
				top = top.previous;
			
			int numToMove = limit - (top.depth - this.depth + 1);
			
			int targetBottom = this.depth;
			
			InlineViewer bottom = this;
			while(bottom.next != null)
				bottom = bottom.next;
			
			/*
			 * Whether or not we have to remove some viewers, we have to move the top
			 * of the inline scope so that the scope that was clicked on is in the lowest
			 * visible scope
			 */
			while(top.depth > 1 && numToMove > 0){
				top.moveUp();
				numToMove--;
			}
			
			/*
			 * If we've gotten to the top before we've put the clicked scope at the bottom,
			 * we have to remove nodes. Starting with the bottom one.
			 */
			while(numToMove > 0 && bottom.depth > targetBottom){
				bottom = bottom.previous;
				top.removeLowestChild();
				numToMove--;
			}
		}
	}
	
	public boolean mouseEvent(MouseEvent event){
		int x = (int) event.getX();
		int y = (int) event.getY();
		
		// Right click over the main text area will trigger the variable-finding
		if(event.getButtonPressed() == MouseEvent.BUTTON3 
				&& event.isOfType(MouseEvent.Type.BUTTON_PRESS) &&
				event.getWindow().equals(this.getWindow(TextWindowType.TEXT))){
			
			Point p = this.windowToBufferCoords(TextWindowType.TEXT, x, y);
			
			TextIter iter = this.getIterAtLocation(p.getX(), p.getY());
			
			final Variable var = this.buf.getVariable(iter);
			
			Menu m = new Menu();
			MenuItem mi = new MenuItem("Display variable value...", false);
			m.append(mi);
			if(var != null){
				mi.addListener(new MenuItemListener() {
					public void menuItemEvent(MenuItemEvent arg0) {
						org.gnu.gtk.Window popup = new org.gnu.gtk.Window(WindowType.TOPLEVEL);
						popup.add(new Label(var.getName()+ " = 0xfeedcalf"));
						popup.showAll();
					}
				});
			}
			else{
				mi.setSensitive(false);
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
//			boolean overNested = false;
			if(theLine > this.buf.getCurrentLine() && expanded){
				theLine--;
//				overNested = true;
			}
			
			final int lineNum = theLine;
			
			// Left click in the margin for a line with inline code - toggle the display of it
			if(event.getButtonPressed() == MouseEvent.BUTTON1 &&
					lineNum == this.buf.getCurrentLine() &&
					this.buf.hasInlineCode(lineNum)){
				this.toggleChild();
			}
		}
		
		return false;
	}
	
    protected void drawLineNumber(Window drawingArea, GC context, int drawingHeight, int number) {
    	Layout lo;
    	if(!this.showingEllipsis)
    		lo = this.createLayout(""+(number + ((InlineBuffer) this.buf).getFirstLine()));
    	else{
    		// We don't draw a line number next to the ellipsis
    		if(number == 0)
    			return;
    		lo = this.createLayout("" + (number + ((InlineBuffer) this.buf).getFirstLine() - 1));
    	}
        lo.setAlignment(Alignment.RIGHT);
        lo.setWidth(this.marginWriteOffset);
        
        drawingArea.drawLayout(context, this.marginWriteOffset, drawingHeight, lo);
    }
    
    /*
	 * The point of this is to remove the last child in the tree
	 */
	private void removeLowestChild(){
		// The next node is not the last one, keep calling down
		if(this.next != null && this.next.next != null){
			this.next.removeLowestChild();
		}
		// The next node is the last one, cull it
		else if(this.next != null){
			this.clearSubscopeAtCurrentLine();
		}
		// If we got here, that means this is the last node. This should *not* happen
		else{
			System.err.println("We got to the last node! This should not be happening!");
		}
	}
	
	/**
	 * This causes the inline scope to move down a level. This is propagated up
	 * through the InlineViewers that are currently being used, with the topmost scope
	 * becomming hidden
	 * 
	 * This method assumes that everyone below us has already moved down
	 */
	private void moveDown(){
		// We have to save the inline viewer before moving down, otherwise stupid GTK
		// clears it
		org.gnu.gtk.Window tmp = new org.gnu.gtk.Window();
		tmp.hideAll();
		if(this.next != null)
			this.next.reparent(tmp);
		
		((InlineBuffer) this.buf).moveDown();
		
		depth++;
		
		if(this.previous != null){
			this.previous.moveDown();
		}
		else{
			this.showingEllipsis = true;
			// Do stuff here to add/update the ellipsis
			EventBox box = new EventBox();
			Label tag = new Label("... " + (this.depth - 1) + " levels hidden");
			box.add(tag);
			box.showAll();
			this.addChild(box, ((InlineBuffer) this.buf).createEllipsisAnchor());
		}
		
		if(this.next != null)
			// We need to reset the subscope at the current line since by changing
			// the text of this one we've cleared it
			this.setSubscopeAtCurrentLine(this.next);
	}
	
	/**
	 * This causes this level of inline scope to move up a level. This assumes a number of
	 * things:
	 *  1) That there is a valid scope above us to move up to
	 *  2) Everything of higher scope than me has already moved up
	 *  
	 *  If this method is the top inlined scope (i.e. previous == null) and it's 
	 *  depth is greater than one, ellipsis are created
	 */
	private void moveUp(){		
//		 We have to save the inline viewer before moving down, otherwise stupid GTK
		// clears it
		org.gnu.gtk.Window tmp = new org.gnu.gtk.Window();
		tmp.hideAll();
		if(this.next != null)
			this.next.reparent(tmp);
		
		((InlineBuffer) this.buf).moveUp();
		
		if(this.previous == null)
			this.depth--;
		
		if(this.previous == null && this.depth > 1){
			this.showingEllipsis = true;
			// Do stuff here to add/update the ellipsis
			EventBox box = new EventBox();
			Label tag = new Label("... " + (this.depth - 1) + " levels hidden");
			box.add(tag);
			box.showAll();
			this.addChild(box, ((InlineBuffer) this.buf).createEllipsisAnchor());
		}
		else{
			this.showingEllipsis = false;
		}
		
		
		if(this.next != null){
			// we removed the subscope that was at the current line, we now have to 
			// re-add it
			this.setSubscopeAtCurrentLine(this.next);
			this.next.moveUp();
		}
	}
}
