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
 * Created on Oct 24, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Entry;
import org.gnu.gtk.FileChooserAction;
import org.gnu.gtk.FileChooserDialog;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreeModelFilterVisibleMethod;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeSelection;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.DialogEvent;
import org.gnu.gtk.event.DialogListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;
import org.gnu.gtk.event.TreeModelEvent;
import org.gnu.gtk.event.TreeModelListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;
import org.gnu.gtk.event.TreeViewColumnEvent;
import org.gnu.gtk.event.TreeViewColumnListener;

import frysk.gui.FryskGui;
import frysk.gui.common.dialogs.DialogManager;

public class ProgramAddWindow extends Window implements LifeCycleListener, Saveable { 

	
	private static final int RESPONSE_OK     = 0;
	private static final int RESPONSE_CANCEL = 1;
	
	private Entry programEntry;
	private Button programOpenFileDialog;
	private TreeView programTreeView;
	private TreeView programObseverListBox;
	private Button programCancel;
	private Button programApply;
	private ProcDataModel psDataModel;
	private TreeModelFilter procFilter;
	private Logger errorLog = Logger.getLogger(FryskGui.ERROR_LOG_ID);

	public ProgramAddWindow(LibGlade glade) {
		super(((Window) glade.getWidget("programAddWindow")).getHandle());
		this.addListener(this);
		getGladeWidgets(glade);
		createDataModel();
		mountProcModel(this.psDataModel);
		setTreeListeners();
		buildObserverListBox();
		setFileButtonListener();
		setApplyCancelButtonListener();
	}

	private void setApplyCancelButtonListener() {
		
		programCancel.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.getType() == ButtonEvent.Type.CLICK){
					hideAll();
				}
			}
		});
		
		programApply.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.getType() == ButtonEvent.Type.CLICK){
					String message = doValidation();
					if (!message.equals(""))
						DialogManager.showWarnDialog("Validation Errors", message);
						
				}
			}
		});
		
	}
	
	private String doValidation() {
		
		// File Checks
		if (programEntry.getText().length() <= 0)
			return "Must have a filename to add";
		
		File existenceCheck = new File(programEntry.getText());
		
		if (existenceCheck.exists() == false)
			return "Filename specified does not exist on disk";
		if (existenceCheck.canRead() == false)
			return "Cannot read specified file";
		if (existenceCheck.isDirectory())
			return "Must be a filename, not a directory";
		
		if (programTreeView.getSelection().getSelectedRows().length < 1)
			return "Please select at least one process that will spawn filename";
		
		if (programObseverListBox.getSelection().getSelectedRows().length < 1)
			return "Please select at least one observer to apply";
		
			
		return "";
	}

	private void setFileButtonListener() {
		
		final FileChooserDialog fileChooserDialog = new FileChooserDialog(
				"Choose a program to observe", 
				(Window)getToplevel(), 
				FileChooserAction.ACTION_OPEN);

		fileChooserDialog.addButton(GtkStockItem.OK, RESPONSE_OK);
		fileChooserDialog.addButton(GtkStockItem.CANCEL, RESPONSE_CANCEL);
		
		programOpenFileDialog.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.getType() == ButtonEvent.Type.CLICK){
					fileChooserDialog.run();
					programEntry.setText(fileChooserDialog.getFilename());
					System.out.println("File name: " + fileChooserDialog.getFilenames());
				}
			}
		});
		
		fileChooserDialog.addListener(new DialogListener(){
			public boolean dialogEvent(DialogEvent event) {
			System.out.println("event: " + event + " RESPONSE: " + event.getResponse());
				if(event.getType() == DialogEvent.Type.RESPONSE){
					fileChooserDialog.hide();
				}
				return false;
			}
		});
		
		
	}


	public void mountProcModel(final ProcDataModel psDataModel) {

		this.procFilter = new TreeModelFilter(psDataModel.getModel());
		procFilter.setVisibleMethod(new TreeModelFilterVisibleMethod() {
			public boolean filter(TreeModel model, TreeIter iter) {
				if (model.getValue(iter, psDataModel.getThreadParentDC()) == -1) {
					return true;
				} else {
					return false;
				}
			}

		});

		this.programTreeView.setModel(procFilter);
		this.programTreeView.setSearchDataColumn(psDataModel.getCommandDC());

		TreeViewColumn pidCol = new TreeViewColumn();
		TreeViewColumn commandCol = new TreeViewColumn();

		CellRendererText cellRendererText3 = new CellRendererText();
		pidCol.packStart(cellRendererText3, false);
		pidCol.addAttributeMapping(cellRendererText3,
				CellRendererText.Attribute.TEXT, psDataModel.getPidDC());
		pidCol.addAttributeMapping(cellRendererText3,
				CellRendererText.Attribute.FOREGROUND, psDataModel.getColorDC());
		pidCol.addAttributeMapping(cellRendererText3,
				CellRendererText.Attribute.WEIGHT, psDataModel.getWeightDC());

		CellRendererText cellRendererText4 = new CellRendererText();
		commandCol.packStart(cellRendererText4, false);
		commandCol.addAttributeMapping(cellRendererText4,
				CellRendererText.Attribute.TEXT, psDataModel.getCommandDC());
		commandCol.addAttributeMapping(cellRendererText4,
				CellRendererText.Attribute.FOREGROUND, psDataModel.getColorDC());
		commandCol.addAttributeMapping(cellRendererText4,
				CellRendererText.Attribute.WEIGHT, psDataModel.getWeightDC());

		pidCol.setTitle("PID");
		pidCol.addListener(new TreeViewColumnListener() {
			public void columnClickedEvent(TreeViewColumnEvent arg0) {
				programTreeView.setSearchDataColumn(psDataModel.getPidDC());
			}
		});
		commandCol.setTitle("Command");
		commandCol.addListener(new TreeViewColumnListener() {
			public void columnClickedEvent(TreeViewColumnEvent arg0) {
				programTreeView.setSearchDataColumn(psDataModel.getCommandDC());
			}
		});

		pidCol.setVisible(true);
		commandCol.setVisible(true);

		this.programTreeView.appendColumn(pidCol);
		this.programTreeView.appendColumn(commandCol);

		psDataModel.getModel().addListener(new TreeModelListener() {
			public void treeModelEvent(TreeModelEvent event) {
				programTreeView.expandAll();
			}
		});

		this.programTreeView.getSelection().setMode((SelectionMode.MULTIPLE));
		this.programTreeView.expandAll();
	}
	
	private void buildObserverListBox() {

		// No observer data model to query yet
		// for list of observers. Fake it out.
		
		DataColumn[] dc = new DataColumn[1];
		dc[0] = new DataColumnString();

		ListStore ls = new ListStore(dc);

		this.programObseverListBox.setModel(ls);
		this.programObseverListBox.setAlternateRowColor(true);
		this.programObseverListBox.getSelection().setMode((SelectionMode.MULTIPLE));
		
		TreeViewColumn observerCol = new TreeViewColumn();
		 CellRendererText Obrender = new CellRendererText();
		   observerCol.packStart(Obrender, true);
		   observerCol.addAttributeMapping(Obrender,
		 CellRendererText.Attribute.TEXT, (DataColumnString)dc[0]);
		 
		 observerCol.setTitle("Available Observers");
		   
		this.programObseverListBox.appendColumn(observerCol);
		
		// No data model to query. Fake items
		
		TreeIter it = null;
		it = ls.appendRow();
		ls.setValue(it, (DataColumnString)dc[0], "Fork" ); 
		it = ls.appendRow();
		ls.setValue(it, (DataColumnString)dc[0], "Exec" ); 
		it = ls.appendRow();
		ls.setValue(it, (DataColumnString)dc[0], "Clone" ); 
		it = ls.appendRow();
		ls.setValue(it, (DataColumnString)dc[0], "Syscall" );
		it = ls.appendRow();
		ls.setValue(it, (DataColumnString)dc[0], "FooBar" ); 
	}
	

	private void getGladeWidgets(LibGlade glade) {
		this.programEntry = (Entry) glade.getWidget("programEntry");
		this.programOpenFileDialog = (Button) glade
				.getWidget("programOpenFileDialog");
		this.programTreeView = (TreeView) glade.getWidget("programWizardTreeView");
		this.programObseverListBox = (TreeView) glade.getWidget("programApplyObserversListBox");
		this.programCancel = (Button) glade.getWidget("programCancel");
		this.programApply = (Button) glade.getWidget("programApply");
	}

	private ProcData getSelectedProc() {
		TreeSelection ts = this.programTreeView.getSelection();
		TreePath[] tp = ts.getSelectedRows();

		if (tp.length == 0) {
			return null;
		}

		TreeModel model = this.procFilter;
		ProcData data = (ProcData) model.getValue(model.getIter(tp[0]),
				this.psDataModel.getProcDataDC());
		model.getValue(model.getIter(tp[0]), this.psDataModel.getPidDC());

		return data;
	}

	private void createDataModel() {
		try {
			this.psDataModel = new ProcDataModel();
		} catch (IOException e) {
			errorLog.log(Level.SEVERE,
					"Error setting data model in program tree view ", e);
		}
		psDataModel.setFilterON(true);
	}

	private void setTreeListeners() {
		this.programTreeView.getSelection().addListener(
				new TreeSelectionListener() {
					public void selectionChangedEvent(TreeSelectionEvent event) {
						if (programTreeView.getSelection().getSelectedRows().length > 0) {
							TreePath selected = programTreeView.getSelection()
									.getSelectedRows()[0];
							ProcData data = (ProcData) procFilter.getValue(
									procFilter.getIter(selected), psDataModel
											.getProcDataDC());
							if (!data.hasWidget()) {
								data.setWidget(new ProcStatusWidget(data));
							}
						}
					}
				});

		this.programTreeView.setHeadersClickable(true);

		this.programTreeView.addListener(new MouseListener() {

			public boolean mouseEvent(MouseEvent event) {
				if (event.getType() == MouseEvent.Type.BUTTON_PRESS
						& event.getButtonPressed() == MouseEvent.BUTTON3) {

					ProcData data = getSelectedProc();
					if (data != null)
						// WatchMenu.getMenu().popup(data);

						System.out.println("click : " + data);
					return true;
				}
				return false;
			}
		});
	}

	public void lifeCycleEvent(LifeCycleEvent event) {
	
	}

	public boolean lifeCycleQuery(LifeCycleEvent event) {
		// TODO Auto-generated method stub
		
		if (event.isOfType(LifeCycleEvent.Type.DESTROY) || 
                event.isOfType(LifeCycleEvent.Type.DELETE)) {
					this.hideAll();
					return true;
		}

		return false;
	}

	public void save(Preferences prefs) {
		prefs.putInt("position.x", this.getPosition().getX());
		prefs.putInt("position.y", this.getPosition().getY());
		
		prefs.putInt("size.height", this.getSize().getHeight());
		prefs.putInt("size.width", this.getSize().getWidth());
			
	}

	public void load(Preferences prefs) {
		int x = prefs.getInt("position.x", this.getPosition().getX());
		int y = prefs.getInt("position.y", this.getPosition().getY());
		this.move(x,y);
		
		int width  = prefs.getInt("size.width", this.getSize().getWidth());
		int height = prefs.getInt("size.height", this.getSize().getHeight());
		this.resize(width, height);
		
	}

}
