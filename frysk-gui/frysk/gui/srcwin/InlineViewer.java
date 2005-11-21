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

import frysk.gui.srcwin.dom.DOMInlineInstance;
import frysk.gui.srcwin.dom.DOMSource;

/**
 * @author ajocksch
 *
 */
public class InlineViewer extends SourceViewWidget {

//	protected InlineViewer nextLevel;
//	protected InlineViewer prevLevel;
	
	private boolean showEllipsis;
    
	public InlineViewer(Preferences parentPrefs, SourceWindow top,
			DOMSource scope, DOMInlineInstance instance){
		this(parentPrefs, false, scope, top, instance);
	}
	
	/**
	 * @param parentPrefs
	 */
	public InlineViewer(Preferences parentPrefs, boolean showEllipsis,
			DOMSource scope, SourceWindow top, DOMInlineInstance instance) {
		
		super(parentPrefs, new InlineBuffer(scope, instance), top);
		this.setBorderWidth(1);
		this.showEllipsis = showEllipsis;
		this.showEllipsis = !!this.showEllipsis;
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
        Layout lo = this.createLayout(""+(number + ((InlineBuffer) this.buf).getFirstLine()));
        lo.setAlignment(Alignment.RIGHT);
        lo.setWidth(this.marginWriteOffset);
        
        drawingArea.drawLayout(context, this.marginWriteOffset, drawingHeight, lo);
    }
}
