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
// type filter text
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

package frysk.gui.monitor;

import org.gnu.gtk.Button;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.HBox;
import org.gnu.gtk.HButtonBox;
import org.gnu.gtk.PolicyType;
import org.gnu.gtk.ResponseType;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.TreeViewEvent;
import org.gnu.gtk.event.TreeViewListener;

import frysk.gui.common.dialogs.Dialog;
import frysk.gui.monitor.datamodels.DataModelManager;
import frysk.proc.Proc;

/**
 * 
 * @author pmuldoon
 *
 * A Dialog that displays a list of procs matching
 * the given path.
 */
public class PickProcDialog extends Dialog {
	
	PickProcListView ListView;
	
	public PickProcDialog(String path){
		super();
		this.setTitle("Debug Process List");
		
		this.ListView = new PickProcListView(path);
		this.ListView.addListener(new TreeViewListener() {
			public void treeViewEvent(TreeViewEvent event) {
				if (event.isOfType(TreeViewEvent.Type.ROW_ACTIVATED)) {
					// Subtle .. it is not.
					// On double click, simulate OK click
					HButtonBox actionArea = getActionArea();
					Widget[]buttons = actionArea.getChildren();
					if (buttons.length == 1)
						if (buttons[0] instanceof Button)
							((Button) buttons[0]).click();

				}
			}
		});
		
		this.setHasSeparator(false);
		this.ListView.watchLinkedList(DataModelManager.theManager.flatProcObservableLinkedList);
        this.ListView.setSort();
		
 
        
		HBox mainBox = new HBox(false,0);
		this.getDialogLayout().add(mainBox);
		
		ScrolledWindow sWindow = new ScrolledWindow(null,null);
		sWindow.setMinimumSize(500, 500);
		sWindow.setBorderWidth(10);
		sWindow.setPolicy(PolicyType.NEVER,PolicyType.AUTOMATIC);
		
		sWindow.addWithViewport(ListView);
		mainBox.packEnd(sWindow);
		
		this.addButton(GtkStockItem.OPEN, ResponseType.OK.getValue());
		this.addButton(GtkStockItem.CANCEL, ResponseType.CANCEL.getValue());

		//this.showAll();
	}
	
	public Proc getChoosenProc()
	{
		if (this.ListView.getSelectedObject() == null)
			return null;
		
		return ((GuiProc) this.ListView.getSelectedObject()).getProc();
	}

	
	
}
