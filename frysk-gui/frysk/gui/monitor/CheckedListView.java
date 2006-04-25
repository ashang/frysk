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
import org.gnu.gtk.CellRendererToggle;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnBoolean;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.event.CellRendererToggleEvent;
import org.gnu.gtk.event.CellRendererToggleListener;

public class CheckedListView extends ListView {

	protected DataColumnBoolean toggleDC;
	private CellRendererToggle cellRendererToggle;
	
	public CheckedListView(){
		super();
	}
	
	public CheckedListView(Handle handle){
		super(handle);
	}
	
	protected void initListStore() {
		this.toggleDC = new DataColumnBoolean();
		this.listStore = new ListStore(new DataColumn[]{nameDC, toggleDC, objectDC});
	}

	
	// Temporarily allow Listener injection until a more robust method
	// can be implemented.
	public void addToggleListener(CellRendererToggleListener listener)
	{
		cellRendererToggle.addListener(listener);
	}
	
	protected void initTreeView() {
		super.initTreeView();
		
		cellRendererToggle = new CellRendererToggle();
		cellRendererToggle.setUserEditable(true);
		cellRendererToggle.addListener(new CellRendererToggleListener() {
			public void cellRendererToggleEvent(CellRendererToggleEvent arg0) {
				listStore.setValue(listStore.getIter(arg0.getPath()), toggleDC, !(listStore.getValue(listStore.getIter(arg0.getPath()), toggleDC)));
			}
		});
		
		TreeViewColumn col = new TreeViewColumn();
		col.packStart(cellRendererToggle, false);
		col.addAttributeMapping(cellRendererToggle, CellRendererToggle.Attribute.ACTIVE, this.toggleDC);
		this.appendColumn(col);
	}
	
	public void add(GuiObject object, TreeIter treeIter){
		listStore.setValue(treeIter, toggleDC, false;
		super.add(object, treeIter);
	}
	
	public CellRendererToggle getCellRendererToggle(){
		return this.cellRendererToggle;
	}
	
	public boolean isChecked(GuiObject object){
		TreeIter iter = (TreeIter) this.map.get(object);
		return listStore.getValue(iter, toggleDC);
	}
}
