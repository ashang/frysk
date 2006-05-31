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

import org.gnu.glib.Handle;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreeModelFilterVisibleMethod;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;


public class ProcWiseTreeView extends TreeView {
	

	public ProcWiseDataModel psDataModel;
	private TreeModelFilter removedProcFilter;

	public ProcWiseTreeView(Handle handle, ProcWiseDataModel model){
		super(handle);
		this.mountDataModel(model);
		this.psDataModel = model;
	}
	
	
	private void mountDataModel(final ProcWiseDataModel dataModel){
		//this.setModel(dataModel.getModel());
		
		CellRendererText cellRendererText = new CellRendererText();
		TreeViewColumn nameCol = new TreeViewColumn();
		nameCol.packStart(cellRendererText, false);
		nameCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT , dataModel.getNameDC());
//		nameCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.STRIKETHROUGH, dataModel.getSensitiveDC());
		this.appendColumn(nameCol);
		
		this.removedProcFilter = new TreeModelFilter(dataModel.getModel());
		
		removedProcFilter.setVisibleMethod(new TreeModelFilterVisibleMethod(){

			public boolean filter(TreeModel model, TreeIter iter) {

				//FIXME: this should be changed to set the row to insensitive
				// instead of completely filtering it out. Once the java-gnome
				// patch for doing so is in FC
				if(model.getValue(iter, psDataModel.getSensitiveDC()) == false){
					return false;	
				}
				
				if(model.getValue(iter, psDataModel.getSelectedDC()) == false){
					return true;
				}else{
					return false;
				}
			}
			
		});

		
		this.setModel(removedProcFilter);

//		this.getSelection().addListener(new TreeSelectionListener() {
//			public void selectionChangedEvent(TreeSelectionEvent event) {
//				System.out.println(this + ": .selectionChangedEvent() dataModel " + dataModel);
//				System.out.println(this + ": .selectionChangedEvent() dataModel.getModel() " + dataModel.getModel());
//				System.out.println(this + ": .selectionChangedEvent() selected rows[0] " + getSelection().getSelectedRows()[0]);
//				TreeIter iter = getModel().getIter(getSelection().getSelectedRows()[0]);
//				System.out.println(this + ": .selectionChangedEvent() iter " + iter);
//
//				iter = dataModel.getModel().getIter(removedProcFilter.convertPathToChildPath(iter.getPath()));
//				System.out.println(this + ": .selectionChangedEvent() iter " + iter);
//
//				
//				System.out.println("\n===========================================");
//				System.out.println(this + ": ProcWiseTreeView.mountDataModel() object: " + dataModel.getModel().getValue(iter, dataModel.getObjectDC()));
//				System.out.println(this + ": ProcWiseTreeView.mountDataModel() name: " + dataModel.getModel().getValue(iter, dataModel.getNameDC()));
//				System.out.println(this + ": ProcWiseTreeView.mountDataModel() selected: " + dataModel.getModel().getValue(iter, dataModel.getSelectedDC()));
//				System.out.println(this + ": ProcWiseTreeView.mountDataModel() sensitive: " + dataModel.getModel().getValue(iter, dataModel.getSensitiveDC()));
//				System.out.println("===========================================\n");
//			}
//		});
	}
	
}
