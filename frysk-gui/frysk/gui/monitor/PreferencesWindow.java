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
/*
 * Created on Oct 6, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.HashMap;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Frame;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Widget;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

import frysk.gui.common.Messages;

/**
 * The preference window. This also provides a place
 * to extend the preferences window.
 * */
public class PreferencesWindow extends Window {
	
	private TreeView treeView;
	
	private TreeStore treeStore;
	
	private DataColumnString nameDC;
	private DataColumnObject widgetDC;
	
	private HashMap iterHashMap;
	
	private VBox prefsWidget;
	
	public PreferencesWindow(LibGlade glade){
		super(((Window)glade.getWidget("preferencesWindow")).getHandle()); //$NON-NLS-1$
		this.treeView  = (TreeView)glade.getWidget("prefsTreeView"); //$NON-NLS-1$
		this.prefsWidget = (VBox)glade.getWidget("prefsWidget"); //$NON-NLS-1$
		
		this.iterHashMap = new HashMap();
		
		DataColumn[] columns = new DataColumn[2];
		
		this.nameDC = new DataColumnString();
		this.widgetDC = new DataColumnObject();
		
		columns[0] = nameDC; 
		columns[1] = widgetDC;
		
		this.treeStore = new TreeStore(columns);
		TreeViewColumn nameCol = new TreeViewColumn();
		CellRendererText cellRendererText = new CellRendererText();
		nameCol.packStart(cellRendererText, false);
		nameCol.addAttributeMapping(cellRendererText, CellRendererText.Attribute.TEXT ,this.nameDC);
		this.treeView.setModel(treeStore);
		this.treeView.appendColumn(nameCol);
		
		this.treeView.getSelection().addListener(new TreeSelectionListener(){
			public void selectionChangedEvent(TreeSelectionEvent event) {
				if(treeView.getSelection().getSelectedRows().length > 0){
					TreePath selected = treeView.getSelection().getSelectedRows()[0];
					
					Frame widget = (Frame) treeStore.getValue(treeStore.getIter(selected), widgetDC);
					
					Widget widgets[] = prefsWidget.getChildren();
					for (int i = 0; i < widgets.length; i++) {
						prefsWidget.remove(widgets[i]);
					}
					
					prefsWidget.add(widget);
				}
			}
		});
		
		
		Button button = (Button) glade.getWidget("prefsOkButton"); //$NON-NLS-1$
		button.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.getType() == ButtonEvent.Type.CLICK){
					WindowManager.theManager.prefsWindow.hideAll();
				}
			}
		});
		
		button = (Button) glade.getWidget("prefsCancelButton"); //$NON-NLS-1$
		button.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.getType() == ButtonEvent.Type.CLICK){
					WindowManager.theManager.prefsWindow.hideAll();
				}
			}
		});
		
		this.hideAll();
	}
	
	/**
	 * Adds a new category page in the preference window
	 * @param name the name of the page to create.
	 */
	public void addPage(String path, PreferenceWidget page){
		TreeIter iter = this.treeStore.appendRow(null);
		String name = page.getLabel();
		
		if(this.iterHashMap.get(path) == null){
			this.iterHashMap.put(path, iter);
		}else{
			throw new IllegalArgumentException(Messages.getString("PreferencesWindow.5") + name + Messages.getString("PreferencesWindow.6")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		this.treeStore.setValue(iter, this.nameDC, name);
		this.treeStore.setValue(iter, this.widgetDC, page);
	}
	
}
