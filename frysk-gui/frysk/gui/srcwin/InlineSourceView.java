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

import org.gnu.gdk.GC;
import org.gnu.gdk.Point;
import org.gnu.gdk.Window;
import org.gnu.gtk.EventBox;
import org.gnu.gtk.Label;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.TextIter;
import org.gnu.gtk.TextWindowType;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.pango.Alignment;
import org.gnu.pango.Layout;

import frysk.dom.DOMInlineInstance;
import frysk.gui.common.prefs.IntPreference;
import frysk.gui.common.prefs.PreferenceManager;
import frysk.gui.srcwin.prefs.SourceWinPreferenceGroup;
import frysk.value.InvalidOperatorException;
import frysk.value.Variable;
import frysk.rt.StackFrame;

/**
 * The InlineViewer displays code that has been inlined. InlineViewers will always
 * appear as a child widget within a SourceViewWidget and should not be created on their
 * own. Some information such as executable information and breakpoints is deliberately
 * omitted since this information may vary due to compiler optimizations.
 */
public class InlineSourceView extends SourceView{
	
	private InlineSourceView previous;
	private InlineSourceView next;
	
	private int depth;
    
	private boolean showingEllipsis = false;
	
	private ToolTips tips;
	
	/**
	 * Creates a new InlineViewer
	 * @param parentPrefs The preference model to use
	 * @param top The SourceWindow that contains this InlineViewer and it's
	 *     siblings
	 * @param scope The DOMSource that contains the function that this
	 *     InlineViewer will be displaying
	 * @param instance The inline instance to display
	 */
	public InlineSourceView(SourceWindow top, DOMInlineInstance instance, StackFrame frame) {
		super(new InlineBuffer(instance, frame), top);
		this.setBorderWidth(1);
		this.depth = 1;
		this.tips = new ToolTips();
	}
    
	public void recalculateVisibleScopes(){
		/*
		 * If this is the case, we're the top inline node, and we need to adjust for 
		 * a possible change in depth
		 */
		if(this.previous == null){
			int depth  = 1;
			InlineSourceView bottom = this;
			while(bottom.next != null){
				bottom = bottom.next;
				depth++;
			}
			
			IntPreference iPref = (IntPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.INLINE_LEVELS);
			int maxDepth = iPref.getCurrentValue();
			
			// Less than the max number of levels is being shown
			if(depth <= maxDepth){
				
				// If we're the first one, we're ok, leave things as they are
				if(this.depth == 1)
					return;
				
				while(this.depth > 1 && depth < maxDepth){
					this.moveUp();
					this.expandLowestChild();
					depth++;
				}
			}
			// More than the max number of levels is being shown
			else{
				while(depth > maxDepth){
					this.removeLowestChild();
					this.moveDownPreOrder();
					depth--;
				}
			}
		}
		
		if(this.next != null)
			this.next.recalculateVisibleScopes();
	}
	
	/**
	 * Overrides the method from SourceViewWidget to also establish the depth of
	 * the child as well as setting up the linked list structure to keep track of it
	 */
	public void setSubscopeAtCurrentLine(InlineSourceView nested){
		InlineSourceView casted = (InlineSourceView) nested;
		casted.depth = this.depth + 1;
		this.next = casted;
		casted.previous = this;
		
		super.setSubscopeAtCurrentLine(nested);
	}
	
	/**
	 * Override the parent method to clear the linked list structure being used.
	 */
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
		IntPreference iPref = (IntPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.INLINE_LEVELS);
		int limit = iPref.getCurrentValue();
		
		if(!this.expanded){
			// Case 1: depth less than max, this level is not expanded.
			// Action: Add a new inline viewer to this subscope (delegate to superclass)
			if(this.depth < limit){
				super.toggleChild();
			}
			// Case 2: depth greater than max, this level not expanded.
			// Action: Move down ourselves, then tell our anscestor to move down as well
			else{
				this.moveDownPostOrder();
			}
		}
		else{
			// Find the top inline viewer
			InlineSourceView top = this;
			while(top.previous != null)
				top = top.previous;
			
			int numToMove = limit - (top.depth - this.depth + 1);
			
			int targetBottom = this.depth;
			
			InlineSourceView bottom = this;
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
	
	/**
	 * Overriden to remove breakpoint functionality, etc. Otherwise operation is unchanged
	 */
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
            MenuItem traceItem = new MenuItem("Add Trace", false);
            m.append(traceItem);
            if (var != null)
              {
                MenuItem valueItem;
                try
                  {
                    valueItem = new MenuItem("Value: " + var.getType().longValue(var), true);
                    valueItem.setSensitive(false);
                    m.append(valueItem);
                  }
                catch (InvalidOperatorException e)
                  {
                    // TODO: What to do if this fails?
                  }
                
                
                traceItem.addListener(new MenuItemListener()
                {
                  public void menuItemEvent (MenuItemEvent arg0)
                  {
                    InlineSourceView.this.parent.addVariableTrace(var);
                  }
                });
              }
            else
              {
                traceItem.setSensitive(false);
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
	
	/**
	 * Draws the line number, taking into account the presence of ellipsis, offset from the
	 * start of the file being displayed, and presence of other inline scopes
	 */
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
	
	/*
	 * Expands the last node in the tree
	 */
	private void expandLowestChild(){
		if(this.next != null)
			this.next.expandLowestChild();
		else
			this.toggleChild();
	}
	/**
	 * This causes the inline scope to move down a level. This is propagated up
	 * through the InlineViewers that are currently being used, with the topmost scope
	 * becomming hidden. This method performs the operation in pre-order method,
	 * meaning that the parents are moved down before the children
	 * 
	 * This method assumes that everyone above us has already moved down
	 */
	private void moveDownPreOrder(){
		// We have to save the inline viewer before moving down, otherwise stupid GTK
		// clears it
		org.gnu.gtk.Window tmp = new org.gnu.gtk.Window();
		tmp.hideAll();
		if(this.next != null)
			this.next.reparent(tmp);
		
		((InlineBuffer) this.buf).moveDown();
		
		depth++;
		
		if(this.next != null){
			this.next.moveDownPreOrder();
			// We need to reset the subscope at the current line since by changing
			// the text of this one we've cleared it
			this.setSubscopeAtCurrentLine(this.next);
		}
		
		if(this.previous == null){
			this.showingEllipsis = true;
			this.createEllipsis();
		}
	}
	
	/**
	 * This causes the inline scope to move down a level. This is propagated up
	 * through the InlineViewers that are currently being used, with the topmost scope
	 * becomming hidden. This method performs the operation in post-order method,
	 * meaning that the children are moved down before the parents are
	 * 
	 * This method assumes that everyone below us has already moved down
	 */
	private void moveDownPostOrder(){
		// We have to save the inline viewer before moving down, otherwise stupid GTK
		// clears it
		org.gnu.gtk.Window tmp = new org.gnu.gtk.Window();
		tmp.hideAll();
		if(this.next != null)
			this.next.reparent(tmp);
		
		((InlineBuffer) this.buf).moveDown();
		
		depth++;
		
		if(this.previous != null){
			this.previous.moveDownPostOrder();
		}
		else{
			this.showingEllipsis = true;
			this.createEllipsis();
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
			this.createEllipsis();
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
	
	/**
	 * Creates the ellipsis at the first line of the viewer to indicate that levels
	 * of inlined code have been hidden
	 *
	 */
	private void createEllipsis(){
		EventBox box = new EventBox();
		Label tag = new Label("... " + (this.depth - 1) + " levels hidden");
		box.add(tag);
		this.tips.setTip(box, "Levels of inlined scope have been hidden. "+
				"To view these levels collapse lower levels or change the number of visible scopes under preferences", "");
		box.showAll();
		this.addChild(box, ((InlineBuffer) this.buf).createEllipsisAnchor());
	}
}
