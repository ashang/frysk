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
 * Created on Oct 20, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.io.File;
import java.io.IOException;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.CellRendererToggle;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreeModelFilterVisibleMethod;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.CellRendererToggleEvent;
import org.gnu.gtk.event.CellRendererToggleListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;
import org.gnu.gtk.event.TreeViewColumnEvent;
import org.gnu.gtk.event.TreeViewColumnListener;
import org.gnu.gtk.event.TreeViewEvent;
import org.gnu.gtk.event.TreeViewListener;

import frysk.Config;
import frysk.gui.common.dialogs.DialogManager;

/**
 * A widget representing the program view page.
 * */
public class ProgramViewPage extends Widget {

	private static final String EVENT_STORE_LOC = Config.FRYSK_DIR +
	"event_watchers_store" + "/"; //$NON-NLS-1$ //$NON-NLS-2$
	
	// private TreeView programTreeView;
	public ProgramAddWindow fileChooserDialog;
	private ProgramDataModel programDataModel;
	
	private TreeModelFilter programFilter;

	private TreeView programTreeView;
	
	private ToolTips treeTip = new ToolTips();
	
	

	public ProgramViewPage(final LibGlade glade) throws IOException {
		super((glade.getWidget("programVBox")).getHandle()); //$NON-NLS-1$
		
		
		
		this.programTreeView = (TreeView) glade.getWidget("programTreeView"); //$NON-NLS-1$
		Button browseButton = (Button) glade.getWidget("programBrowseButton"); //$NON-NLS-1$
		Button deleteButton = (Button) glade.getWidget("programDeleteButton"); //$NON-NLS-1$
		
		
		this.programDataModel = ProgramDataModel.theManager;
		mountProgramDataModel(programDataModel);
		
				
		browseButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				
				if(event.getType() == ButtonEvent.Type.CLICK){
					WindowManager.theManager.programAddWindow.showAll();
				}
			}
		});
		
		deleteButton.addListener(new ButtonListener() {

			public void buttonEvent(ButtonEvent event) {
				if(event.getType() == ButtonEvent.Type.CLICK){
					deleteSelectedMonitor();
				}
			}
			
		});
		
		loadExistingMonitors();
	
		this.initProgramTreeView();
		
	}

	
	private void loadExistingMonitors() {
		
		File monitors = new File(EVENT_STORE_LOC);
		
		File[] fMonitors = monitors.listFiles();
		
		if (fMonitors == null)
			return;
		
		
		for (int i=0; i<fMonitors.length; i++)
		{
			ProgramData temp = new ProgramData();
			temp.load(fMonitors[i].getAbsolutePath());
			programDataModel.add(temp);
			temp = null;
		}
		
	
		
	}


	public void mountProgramDataModel(final ProgramDataModel programsDataModel) {

		// this.procTreeView.setModel(psDataModel.getModel());

		this.programFilter = new TreeModelFilter(programDataModel.getModel());

		programFilter.setVisibleMethod(new TreeModelFilterVisibleMethod() {

			public boolean filter(TreeModel model, TreeIter iter) {

				return true;

			}

		});

		this.programTreeView.setModel(programFilter);
		this.programTreeView.setSearchDataColumn(programDataModel
				.getEventNameDC());
		//this.programTreeView.setHoverSelection(true);
		this.programTreeView.addListener(new TreeViewListener() {

			public void treeViewEvent(TreeViewEvent event) {
				if (event.isOfType(TreeViewEvent.Type.ROW_ACTIVATED)) {
					event.getTreePath();
					WindowManager.theManager.programAddWindow
							.populate(ProgramDataModel.theManager
									.interrogate(event.getTreePath()));
					WindowManager.theManager.programAddWindow.showAll();
				}

			}
		});

		this.programTreeView.getSelection().addListener(
				new TreeSelectionListener() {

					public void selectionChangedEvent(TreeSelectionEvent event) {
						TreePath[] selection = programTreeView.getSelection()
								.getSelectedRows();
						if (selection.length > 0) {
							treeTip.enable();
							treeTip.setTip(programTreeView, programDataModel
									.getTip(selection[0]), null);
						} else
							treeTip.disable();
					}
				});

		TreeViewColumn enabledCol = new TreeViewColumn();
		TreeViewColumn nameCol = new TreeViewColumn();

		CellRendererToggle renderToggle = new CellRendererToggle();
		renderToggle.addListener(new CellRendererToggleListener() {

			public void cellRendererToggleEvent(CellRendererToggleEvent arg0) {

				ProgramDataModel.theManager.toggle(arg0.getPath());

			}
		});
		renderToggle.setUserEditable(true);
		enabledCol.packStart(renderToggle, true);
		enabledCol.addAttributeMapping(renderToggle,
				CellRendererToggle.Attribute.ACTIVE, programDataModel
						.getEnabledDC());
		enabledCol.setTitle("Enabled?"); //$NON-NLS-1$
		enabledCol.setVisible(true);

		CellRendererText nameRender = new CellRendererText();
		nameCol.packStart(nameRender, true);
		nameCol.addAttributeMapping(nameRender,
				CellRendererText.Attribute.TEXT, programDataModel
						.getEventNameDC());
		nameCol.addAttributeMapping(nameRender,
				CellRendererText.Attribute.FOREGROUND, programDataModel
						.getColorDC());
		nameCol.addAttributeMapping(nameRender,
				CellRendererText.Attribute.WEIGHT, programDataModel
						.getWeightDC());
		nameCol.setTitle("Observed Executable"); //$NON-NLS-1$

		nameCol.addListener(new TreeViewColumnListener() {
			public void columnClickedEvent(TreeViewColumnEvent arg0) {
				programTreeView.setSearchDataColumn(programDataModel
						.getEventNameDC());
			}
		});
		nameCol.setVisible(true);

		programTreeView.appendColumn(enabledCol);
		programTreeView.appendColumn(nameCol);
	}
	
	private void initProgramTreeView() {
		this.programTreeView.getClass();//XXX: dummy call to get rid of ecj warning
	}
	
	private void deleteSelectedMonitor() {
		
		TreePath[] deleteSelection = this.programTreeView.getSelection().getSelectedRows();
		
		DialogManager.showWarnDialog("Delete Confirm", "Are you sure you wanted to delete selected monitors?"); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i=0; i<deleteSelection.length; i++)
		{
			ProgramDataModel.theManager.delete(deleteSelection[i]);
		}
		
	} 
	
}
