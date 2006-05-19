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
 * Created on 8-Jul-05
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
import org.gnu.glib.GObject;
import org.gnu.glib.PropertyNotificationListener;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.SpinButton;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreeModelFilterVisibleMethod;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeSelection;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.VBox;
import org.gnu.gtk.VPaned;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;
import org.gnu.gtk.event.SpinEvent;
import org.gnu.gtk.event.SpinListener;
import org.gnu.gtk.event.TreeModelEvent;
import org.gnu.gtk.event.TreeModelListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;
import org.gnu.gtk.event.TreeViewColumnEvent;
import org.gnu.gtk.event.TreeViewColumnListener;

import frysk.gui.Gui;
import frysk.gui.sessions.Session;

public class SessionProcTreeView extends Widget implements ButtonListener, Saveable{

	private SpinButton refreshSpinButton;
	private Button refreshButton;
	
	private TreeView procTreeView;
	private TreeView threadTreeView;
		
//	private ProcDataModel procDataModel;
	private SessionProcDataModel procDataModel;
	
	private VPaned vPane;
	private TreeModelFilter procFilter;
	private TreeModelFilter threadFilter;
	
	private VBox statusWidget;
	private InfoWidget infoWidget;
	
	private Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);
	
	private LibGlade glade;
	
	public SessionProcTreeView(LibGlade libGlade) throws IOException {
		super((libGlade.getWidget("allProcVBox")).getHandle()); //$NON-NLS-1$
		this.glade = libGlade;
		
		this.refreshSpinButton   = (SpinButton)  glade.getWidget("refreshSpinButton"); //$NON-NLS-1$
		this.refreshButton       = (Button)      glade.getWidget("refreshButton"); //$NON-NLS-1$
		
		this.procTreeView        = (TreeView)    glade.getWidget("procTreeView"); //$NON-NLS-1$
		this.threadTreeView      = (TreeView)    glade.getWidget("threadTreeView"); //$NON-NLS-1$
		
		this.vPane               = (VPaned)  glade.getWidget("vPane"); //$NON-NLS-1$
		
		this.statusWidget          = (VBox)        glade.getWidget("statusWidget"); //$NON-NLS-1$
		
		this.infoWidget = new InfoWidget();
		this.statusWidget.add(infoWidget);
		
		this.refreshButton.addListener(this);
		this.refreshSpinButton.addListener(new SpinListener(){
			public void spinEvent(SpinEvent event) {
				if(event.getType() == SpinEvent.Type.VALUE_CHANGED){
					procDataModel.setRefreshTime(refreshSpinButton.getIntValue());
				}
			}			
		});
		
		
		this.procDataModel = new SessionProcDataModel();
		
		this.mountProcModel(this.procDataModel);
		this.threadViewInit(procDataModel);
		
		this.procTreeView.getSelection().addListener(new TreeSelectionListener(){
			public void selectionChangedEvent(TreeSelectionEvent event) {
				if(procTreeView.getSelection().getSelectedRows().length > 0){
					TreePath selected = procTreeView.getSelection().getSelectedRows()[0];
					mountThreadModel(procDataModel, selected);
					GuiProc data = (GuiProc) procFilter.getValue(procFilter.getIter(selected), procDataModel.getProcDataDC());
					if(!data.hasWidget()){
						data.setWidget(new ProcStatusWidget(data));
					}
					
					infoWidget.setSelectedProc(data);
					
					if(threadTreeView.getModel().getFirstIter() != null){
						threadTreeView.getSelection().select(threadTreeView.getModel().getFirstIter());
					}
				}else{
					infoWidget.setSelectedProc(null);
				}
			}
		});
		
		
		this.threadTreeView.getSelection().addListener(new TreeSelectionListener(){
			public void selectionChangedEvent(TreeSelectionEvent event) {
				if(procTreeView.getSelection().getSelectedRows().length > 0 &&
						threadTreeView.getSelection().getSelectedRows().length > 0	){
					TreePath selected = threadTreeView.getSelection().getSelectedRows()[0];
					GuiTask data = (GuiTask) threadFilter.getValue(threadFilter.getIter(selected), procDataModel.getProcDataDC());
					if(!data.hasWidget()){
						data.setWidget(new TaskStatusWidget(data));
					}
					
					infoWidget.setSelectedTask(data);
				}else{
					infoWidget.setSelectedTask(null);
				}
			}
		});
		
		
		
		this.procTreeView.setHeadersClickable(true);
		
		this.procTreeView.addListener(new MouseListener(){

			public boolean mouseEvent(MouseEvent event) {
				if(event.getType() == MouseEvent.Type.BUTTON_PRESS 
						& event.getButtonPressed() == MouseEvent.BUTTON3){
					
					GuiProc data = getSelectedProc();
					if(data != null) ProcMenu.getMenu().popup(data);
					
                    //System.out.println("click : " + data); //$NON-NLS-1$
                    return true;
				}
				return false;
			}
		});
		
		this.threadTreeView.addListener(new MouseListener(){

			public boolean mouseEvent(MouseEvent event) {
				if(event.getType() == MouseEvent.Type.BUTTON_PRESS 
						& event.getButtonPressed() == MouseEvent.BUTTON3){
					
					GuiTask data = getSelectedThread();
					if(data != null) ThreadMenu.getMenu().popup(data);
					
                    //System.out.println("click : " + data); //$NON-NLS-1$
                    return true;
				}
				return false;
			}
		});
		
	}
	
	
	public void mountProcModel(final SessionProcDataModel dataModel){
		
//		this.procTreeView.setModel(psDataModel.getModel());
		
		this.procFilter = new TreeModelFilter(dataModel.getModel());
		
		procFilter.setVisibleMethod(new TreeModelFilterVisibleMethod(){

			public boolean filter(TreeModel model, TreeIter iter) {

				if(model.getValue(iter, dataModel.getSensitiveDC()) == false){
					return false;
				}
				
				if(model.getValue(iter, dataModel.getHasParentDC()) == false){
					return true;
				}else{
					return false;
				}
			}
			
		});
		
		this.procTreeView.setModel(procFilter);
		this.procTreeView.setSearchDataColumn(dataModel.getCommandDC());
		
		TreeViewColumn pidCol = new TreeViewColumn();
		TreeViewColumn commandCol = new TreeViewColumn();
		
		CellRendererText cellRendererText3 = new CellRendererText();
		pidCol.packStart(cellRendererText3, false);
		
		pidCol.addAttributeMapping(cellRendererText3, CellRendererText.Attribute.TEXT ,dataModel.getPidDC());
		pidCol.addAttributeMapping(cellRendererText3, CellRendererText.Attribute.FOREGROUND ,dataModel.getColorDC());		
		pidCol.addAttributeMapping(cellRendererText3, CellRendererText.Attribute.WEIGHT ,dataModel.getWeightDC());		
//		pidCol.addAttributeMapping(cellRendererText3, CellRendererText.Attribute.STRIKETHROUGH,psDataModel.getSensitiveDC());		

		CellRendererText cellRendererText4 = new CellRendererText();
		commandCol.packStart(cellRendererText4, false);
		commandCol.addAttributeMapping(cellRendererText4, CellRendererText.Attribute.TEXT ,dataModel.getCommandDC());
		commandCol.addAttributeMapping(cellRendererText4, CellRendererText.Attribute.FOREGROUND ,dataModel.getColorDC());
		commandCol.addAttributeMapping(cellRendererText4, CellRendererText.Attribute.WEIGHT ,dataModel.getWeightDC());				
//		commandCol.addAttributeMapping(cellRendererText4, CellRendererText.Attribute.STRIKETHROUGH ,psDataModel.getSensitiveDC());		

		pidCol.setTitle("PID"); //$NON-NLS-1$
		pidCol.addListener(new TreeViewColumnListener(){
			public void columnClickedEvent(TreeViewColumnEvent arg0) {
				procTreeView.setSearchDataColumn(dataModel.getPidDC());
			}
		});
		commandCol.setTitle("Command"); //$NON-NLS-1$
		commandCol.addListener(new TreeViewColumnListener(){
			public void columnClickedEvent(TreeViewColumnEvent arg0) {
				procTreeView.setSearchDataColumn(dataModel.getCommandDC());
			}
		});
		
		
		pidCol.setVisible(true);
		commandCol.setVisible(true);

		this.procTreeView.appendColumn(pidCol);
		this.procTreeView.appendColumn(commandCol);
		
		dataModel.getModel().addListener(new PropertyNotificationListener(){
			public void notify(GObject arg0, String arg1) {
				//System.out.println("Notification : " + arg1); //$NON-NLS-1$
			}
		});
		
		dataModel.getModel().addListener(new TreeModelListener(){

			public void treeModelEvent(TreeModelEvent event) {
				procTreeView.expandAll();
			}
			
		});
		
		this.procTreeView.expandAll();
	}
	
	
	public void mountThreadModel(final SessionProcDataModel dataModel, final TreePath relativeRoot ){
		final TreePath root = this.procFilter.convertPathToChildPath(relativeRoot);
		this.threadFilter = new TreeModelFilter(dataModel.getModel(), root);
		
		threadFilter.setVisibleMethod(new TreeModelFilterVisibleMethod(){

			public boolean filter(TreeModel model, TreeIter iter) {
				
				if(relativeRoot == null ) {
					return false;
				}
				if(model.getValue(iter, dataModel.getThreadParentDC()) == procFilter.getValue(procFilter.getIter(relativeRoot), dataModel.getPidDC())){
					return true;
				}else{
					return false;
				}
				
//				if(model.getValue(iter, psDataModel.getSensitiveDC()) == false){
//					return false;
//				}
				
			}
		});
		
		this.threadTreeView.setModel(threadFilter);
	}

	private void threadViewInit(SessionProcDataModel procDataModel){
		TreeViewColumn pidCol = new TreeViewColumn();
		TreeViewColumn commandCol = new TreeViewColumn();
		
		CellRendererText cellRendererText3 = new CellRendererText();
		pidCol.packStart(cellRendererText3, false);
		pidCol.addAttributeMapping(cellRendererText3, CellRendererText.Attribute.TEXT ,procDataModel.getPidDC());
		pidCol.addAttributeMapping(cellRendererText3, CellRendererText.Attribute.FOREGROUND ,procDataModel.getColorDC());		
		pidCol.addAttributeMapping(cellRendererText3, CellRendererText.Attribute.WEIGHT ,procDataModel.getWeightDC());		

		CellRendererText cellRendererText4 = new CellRendererText();
		commandCol.packStart(cellRendererText4, false);
		commandCol.addAttributeMapping(cellRendererText4, CellRendererText.Attribute.TEXT ,procDataModel.getCommandDC());
		commandCol.addAttributeMapping(cellRendererText4, CellRendererText.Attribute.FOREGROUND ,procDataModel.getColorDC());
		commandCol.addAttributeMapping(cellRendererText4, CellRendererText.Attribute.WEIGHT ,procDataModel.getWeightDC());				

		pidCol.setTitle("PID"); //$NON-NLS-1$
		commandCol.setTitle("Entry Functions"); //$NON-NLS-1$
		
		pidCol.setVisible(true);
		commandCol.setVisible(true);

		this.threadTreeView.appendColumn(pidCol);
		this.threadTreeView.appendColumn(commandCol);
		
		procDataModel.getModel().addListener(new TreeModelListener(){

			public void treeModelEvent(TreeModelEvent event) {
				threadTreeView.expandAll();
			}
			
		});
		
		this.threadTreeView.expandAll();
	}
	
	public void buttonEvent(ButtonEvent event) {
		if(this.refreshButton.equals(event.getSource()) 
				&& event.getType() == ButtonEvent.Type.CLICK){
			this.refresh();
		}
	
	}
	
	private void refresh(){
		try {
			this.procDataModel.refresh();
		} catch (IOException e) {
			errorLog.log(Level.SEVERE,"Cannot refresh",e); //$NON-NLS-1$
		}
	}

	private GuiProc getSelectedProc(){
		TreeSelection ts = this.procTreeView.getSelection();
		TreePath[] tp = ts.getSelectedRows();

		if(tp.length == 0){ 
			return null;
		}
		
		TreeModel model = this.procFilter;
		GuiProc data   = (GuiProc)model.getValue(model.getIter(tp[0]), this.procDataModel.getProcDataDC());
		model.getValue(model.getIter(tp[0]), this.procDataModel.getPidDC());

		return data;
	}

	private GuiTask getSelectedThread(){
		TreeSelection ts = this.threadTreeView.getSelection();
		TreePath[] tp = ts.getSelectedRows();

		if(tp.length == 0){ 
			return null;
		}
		
		TreeModel model = this.threadFilter;
		GuiTask data   = (GuiTask)model.getValue(model.getIter(tp[0]), this.procDataModel.getProcDataDC());
		model.getValue(model.getIter(tp[0]), this.procDataModel.getPidDC());

		return data;
	}

	public void save(Preferences prefs) {
		prefs.putInt("vPane.position", this.vPane.getPosition()); //$NON-NLS-1$
		prefs.putInt("refreshSpinButton", (int) this.refreshSpinButton.getValue());
	}


	public void load(Preferences prefs) {
		int position = prefs.getInt("vPane.position", this.vPane.getPosition()); //$NON-NLS-1$
		int refreshTime = prefs.getInt("refreshSpinButton", (int) this.refreshSpinButton.getValue());
		
		this.vPane.setPosition(position);
		this.refreshSpinButton.setValue(refreshTime);
	}


	public void setSession(Session session) {
		this.procDataModel.setSession(session);
	}
	
}
