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

package frysk.gui.druid;

import java.util.Iterator;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.Dialog;
import org.gnu.gtk.Entry;
import org.gnu.gtk.Notebook;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.CellRendererToggleEvent;
import org.gnu.gtk.event.CellRendererToggleListener;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

import frysk.gui.monitor.CheckedListView;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.GuiProc;
import frysk.gui.monitor.ListView;
import frysk.gui.monitor.ProcWiseDataModel;
import frysk.gui.monitor.ProcWiseTreeView;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;
import frysk.gui.sessions.SessionManager;
import frysk.gui.srcwin.tags.Tagset;
import frysk.gui.srcwin.tags.TagsetManager;

public class CreateFryskSessionDruid extends Dialog implements LifeCycleListener {

	private ProcWiseDataModel dataModel;
	private ProcWiseTreeView procWiseTreeView;
	private ListView processTagSetSelectionTreeView;
	private ListView addedProcsTreeView;
	private CheckedListView tagSetSelectionTreeView;
	private CheckedListView  observerSelectionTreeView;
	private ListView processObserverSelectionTreeView;	
	
	
	
	private Session currentSession = new Session();
	private Entry nameEntry;

	private Notebook notebook;
	private Button nextButton;
	private Button backButton;
	private Button finishButton;
	private Button saveButton;
	private Button cancelButton;

	

	private int processSelected = 0;
	private boolean editSession;
	private String oldSessionName;

	public CreateFryskSessionDruid(LibGlade glade) {
		super(glade.getWidget("SessionDruid").getHandle());	
		
		getDruidStructureControls(glade);
		getProcessSelectionControls(glade);
		getTagsetObserverControls(glade);
		getProcessObserverControls(glade);
		this.addListener(this);
	}
	
	public void setEditSessionMode(Session givenSession)
	{
		currentSession = givenSession;
		if (currentSession == null) 
			currentSession = new Session();
			
		processSelected = currentSession.getProcesses().size();
		attachLinkedListsToWidgets();

		notebook.setShowTabs(true);
		notebook.setCurrentPage(0);
		finishButton.hideAll();
		nextButton.hideAll();
		backButton.hideAll();
		cancelButton.showAll();
		saveButton.showAll();
		editSession = true;
		oldSessionName = currentSession.getName();
	}
	
	public void setNewSessionMode() {
		nextButton.showAll(); nextButton.setSensitive(false);
		backButton.showAll(); backButton.setSensitive(false);
		finishButton.hideAll();
		saveButton.hideAll();
		cancelButton.showAll();
		nameEntry.setText("");

		currentSession = null;
		currentSession = new Session();
		attachLinkedListsToWidgets();
		
		notebook.setShowTabs(false);
		notebook.setCurrentPage(0);
		processSelected = 0;
		editSession = false;
	}
	
	private void setTreeSelected(TreeIter selected, boolean setSelected, boolean setChildren)
	{
		this.dataModel.setSelected(selected,setSelected,setChildren);
	}
	
	private void addProcessParent(TreeIter unfilteredProcessIter) {
		if (unfilteredProcessIter == null){
			return;
		}
		
		if (!this.dataModel.getModel().isIterValid(unfilteredProcessIter)){
			return;
		}
		
		GuiProc proc = (GuiProc) this.dataModel.getModel().getValue(unfilteredProcessIter,this.dataModel.getObjectDC());
		if (proc == null){
			if (unfilteredProcessIter.getChildCount() > 0){
				TreeIter childIter = unfilteredProcessIter.getChild(0);
				proc = ((GuiProc)this.dataModel.getModel().getValue(childIter,this.dataModel.getObjectDC()));
			}
		}
		
		DebugProcess debugProcess = new DebugProcess(proc.getExecutableName(), proc.getNiceExecutablePath());
		currentSession.addProcess(debugProcess);

	}
		
	private boolean isChild(TreePath pathTest)
	{
		// Minor hack. Sometimes path.up() returns a boolean
		// that indicates it has a parent and the up() changed the 
		// state of the Path. However all operations on that then
		// return NP which indicates a false positive. Fix locally, 
		// and file upstream bug.
		
		if (pathTest.toString().split(":").length > 1)
			return true;
		else		
			return false;
	}
	
	private TreePath deFilterPath(TreeView tree, TreePath filter)
	{
		TreeModelFilter ts = (TreeModelFilter) tree.getModel();
		return ts.convertPathToChildPath(filter);
	}
		
	private void changeGroupState(TreeView tree, TreePath[] selectedProcs, boolean filtered, boolean state)
	{
		if (selectedProcs.length > 0)
		{
 			for(int i=0; i<selectedProcs.length;i++)
			{
				// Convert a filetered iterator to a non filtered iterator.
				TreeIter unfilteredProcessIter;
				if (filtered)
					unfilteredProcessIter = this.dataModel.getModel().getIter(deFilterPath(tree,selectedProcs[i]));
				else
					unfilteredProcessIter = this.dataModel.getModel().getIter(selectedProcs[i]);
				
				// Scenario 1: Tree iter has children (a process group); selected the parent
				if (unfilteredProcessIter.getChildCount() > 0)
				{
					if (state)
					{
						processSelected += unfilteredProcessIter.getChildCount() +1;
						addProcessParent(unfilteredProcessIter);
					}
					else
						processSelected -= unfilteredProcessIter.getChildCount() +1;
					setTreeSelected(unfilteredProcessIter,state,true);
				}
				else {
					// Scenario 2: A child in the process group has been selected, also select
				    // the parent and siblings.
					TreePath parent_path = unfilteredProcessIter.getPath();
					if (isChild(parent_path))
					{
						// we are a child that has a parent; sibilings and parent
						parent_path.up();						
						// Save parent iter
						TreeIter parent_iter = this.dataModel.getModel().getIter(parent_path);
						// Move the parent and children.
						if (parent_iter.getChildCount() > 0)
						{
							if (state)
							{
								processSelected += unfilteredProcessIter.getChildCount() +1;
								addProcessParent(parent_iter);
							}
							else
								processSelected -= unfilteredProcessIter.getChildCount() +1;
							setTreeSelected(parent_iter,state,true);
						}
					}
					else
					{
						if (state)
						{
							addProcessParent(unfilteredProcessIter);
							processSelected++;
						}
						else
							processSelected--;
						// Scenario 3: No children, or siblings
						setTreeSelected(unfilteredProcessIter,state,false);
					}
				}
			}
		}
		setProcessNext(processSelected);
	}
	

	
	private void setProcessNext(int processCount)
	{
		if ((processCount > 0) && (nameEntry.getText().length() > 0))
		{
			this.nextButton.setSensitive(true);
			this.saveButton.setSensitive(true);
		}
		else
		{
			this.nextButton.setSensitive(false);
			this.saveButton.setSensitive(false);
		}
	}
	

	private void getProcessSelectionControls(LibGlade glade) {
		
		Button addProcessGroupButton;
		Button removeProcessGroupButton;
		
		
		// Page 1 of the Druid. Initial Process Selection.
		
		// Create New Live Data Model and mount on the TreeView
		this.dataModel = new ProcWiseDataModel();
		procWiseTreeView = new ProcWiseTreeView(glade.getWidget("sessionDruid_procWiseTreeView").getHandle(),this.dataModel);
		
		// Create a New ListView and mount the Linked List from Session data 
		addedProcsTreeView = new ListView(glade.getWidget("sessionDruid_addedProcsTreeView").getHandle());
		addedProcsTreeView.watchLinkedList(currentSession.getProcesses());
		
		
		this.setUpCurrentPage();
		
		nameEntry = (Entry) glade.getWidget("sessionDruid_sessionName");
		nameEntry.addListener(new EntryListener() {
			public void entryEvent(EntryEvent arg0) {
				currentSession.setName(nameEntry.getText());
				setProcessNext(processSelected);
			}
		});		
			
		SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.BOTH);
		sizeGroup.addWidget(procWiseTreeView);
		sizeGroup.addWidget(addedProcsTreeView);

		addProcessGroupButton = (Button) glade.getWidget("sessionDruid_addProcessGroupButton");
		removeProcessGroupButton = (Button) glade.getWidget("sessionDruid_removeProcessGroupButton");
			
		addProcessGroupButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK))
					changeGroupState(procWiseTreeView,procWiseTreeView.getSelection().getSelectedRows(),true,true);
			}	
		});
		
		removeProcessGroupButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					Iterator i = addedProcsTreeView.getSelectedObjects();
					while (i.hasNext())
					{
						DebugProcess currentDebugProcess = (DebugProcess) i.next();
						TreePath foo = dataModel.searchName(currentDebugProcess.getName());
						changeGroupState(procWiseTreeView,new TreePath[]{foo},false,false);
						currentSession.removeProcess(currentDebugProcess);
					}
				}
			}
		});
		
	}
	
	private void getTagsetObserverControls(LibGlade glade)
	{
		processTagSetSelectionTreeView = new ListView(
				glade.getWidget("SessionDruid_processTagSetTreeView").getHandle());
		
		tagSetSelectionTreeView = new CheckedListView(
				glade.getWidget("SessionDruid_tagSetTreeView").getHandle());
		
		SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.BOTH);
		sizeGroup.addWidget(processTagSetSelectionTreeView);
		sizeGroup.addWidget(tagSetSelectionTreeView);
		
		processTagSetSelectionTreeView.watchLinkedList(currentSession.getProcesses());
		processTagSetSelectionTreeView.getSelection().addListener(new TreeSelectionListener(){
			public void selectionChangedEvent(TreeSelectionEvent arg0) {
				DebugProcess selected = (DebugProcess)processTagSetSelectionTreeView.getSelectedObject();	
				if (selected != null)
				{
					Iterator i = selected.getTagsets().iterator();
					tagSetSelectionTreeView.clearChecked();
					while (i.hasNext())
						tagSetSelectionTreeView.setChecked(((Tagset)i.next()),true);
				}
			}});
			
		tagSetSelectionTreeView.watchLinkedList(TagsetManager.manager.getListTagsets());
		tagSetSelectionTreeView.getCellRendererToggle().addListener(new CellRendererToggleListener() { 
			public void cellRendererToggleEvent(CellRendererToggleEvent arg0) {
				GuiObject selected = (GuiObject) tagSetSelectionTreeView.getSelectedObject();
				DebugProcess tagProcessSelected = (DebugProcess) processTagSetSelectionTreeView.getSelectedObject();
				if (tagSetSelectionTreeView.isChecked(selected))
				{
					if (!tagProcessSelected.getTagsets().contains(selected))
						tagProcessSelected.addTagset((Tagset)selected);
				}	
				else
				{
					tagProcessSelected.removeTagset((Tagset)selected);
				}
			}});
	}
	
	private void getProcessObserverControls(LibGlade glade) {
	
		
		observerSelectionTreeView = new CheckedListView(
				glade.getWidget("SessionDruid_observerTreeView").getHandle());
		
		observerSelectionTreeView.expandAll();
		
		processObserverSelectionTreeView = new ListView(
				glade.getWidget("SessionDruid_processObserverTreeView").getHandle());
		
		processObserverSelectionTreeView.expandAll();
		
		processObserverSelectionTreeView.watchLinkedList(currentSession.getProcesses());
		processObserverSelectionTreeView.getSelection().addListener(new TreeSelectionListener(){
			public void selectionChangedEvent(TreeSelectionEvent arg0) {
				DebugProcess selected = (DebugProcess)processObserverSelectionTreeView.getSelectedObject();
				if (selected != null)
				{
					Iterator i = selected.getObservers().iterator();
					observerSelectionTreeView.clearChecked();
					while (i.hasNext())
						observerSelectionTreeView.setChecked((ObserverRoot) i.next(),true);
				}
			}});
		
		observerSelectionTreeView.watchLinkedList(ObserverManager.theManager.getTaskObservers());
		observerSelectionTreeView.getCellRendererToggle().addListener(new CellRendererToggleListener() { 
			public void cellRendererToggleEvent(CellRendererToggleEvent arg0) {
				GuiObject selected = (GuiObject) observerSelectionTreeView.getSelectedObject();
				DebugProcess observerProcessSelected = (DebugProcess) processObserverSelectionTreeView.getSelectedObject();
				if (observerSelectionTreeView.isChecked(selected))
				{
					if (!observerProcessSelected.getObservers().contains(selected))
						observerProcessSelected.addObserver((ObserverRoot)selected);
				}
				else
				{
					observerProcessSelected.removeObserver((ObserverRoot)selected);
				}
			}});
		
		SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.BOTH);
		sizeGroup.addWidget(observerSelectionTreeView);
		sizeGroup.addWidget(processObserverSelectionTreeView);
			
		setUpCurrentPage();	
	}

	private void getDruidStructureControls(LibGlade glade)
	{
		
		this.notebook = (Notebook) glade.getWidget("sessionDruid_sessionNoteBook");
		
		this.nextButton = (Button) glade.getWidget("sessionDruid_nextButton");
		this.nextButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					nextPage();
				}
			}
		});
		this.nextButton.setSensitive(false);
		
		this.backButton = (Button) glade.getWidget("sessionDruid_backButton");
		this.backButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					previousPage();
				}
			}
		});
		
		this.finishButton = (Button) glade.getWidget("sessionDruid_finishButton");
		this.finishButton.setSensitive(true);
		this.finishButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
						SessionManager.theManager.addSession(currentSession);
						SessionManager.theManager.save();
						currentSession = null;
						currentSession = new Session();
						hide();
				}
			}
		});
		
		this.saveButton = (Button) glade.getWidget("sessionDruid_saveEditSessionButton");
		this.saveButton.hideAll();
		this.saveButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
						if (editSession)
						{
							SessionManager.theManager.save();
							
							if (!oldSessionName.equals(currentSession.getName()))
							{
								// If they edited the name of the session, reload the
								// Session Manager so it picks up the old session and
								// delete it.
								SessionManager.theManager.load();
								SessionManager.theManager.removeSession(
									SessionManager.theManager.getSessionByName(oldSessionName));
							}
						}
						hide();
					}
				}});
		
		this.cancelButton = (Button) glade.getWidget("sessionDruid_cancelButton");
		this.cancelButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
						currentSession.dontSaveObject();
						SessionManager.theManager.load();
						hide();
						
					}
				}});
	}
	
	public void attachLinkedListsToWidgets()
	{
		if (!currentSession.getName().equals("NoName"))
			nameEntry.setText(currentSession.getName());
		addedProcsTreeView.watchLinkedList(currentSession.getProcesses());
		//tagSetSelectionTreeView;
		processTagSetSelectionTreeView.watchLinkedList(currentSession.getProcesses());
		//observerSelectionTreeView;
		processObserverSelectionTreeView.watchLinkedList(currentSession.getProcesses());
	}


	private void nextPage() {
		
		// Process previous page data
		int page = this.notebook.getCurrentPage();

		this.notebook.setCurrentPage(page+1);
		this.setUpCurrentPage();
	}
	
	private void previousPage(){
		this.notebook.setCurrentPage(this.notebook.getCurrentPage()-1);
		this.setUpCurrentPage();
	}

	private void setUpCurrentPage() {	
		int page = this.notebook.getCurrentPage();

		if(page == 0)
			this.backButton.setSensitive(false);
		else
			this.backButton.setSensitive(true);
	
		if(page == this.notebook.getNumPages()-1) {
			this.nextButton.hideAll();
			this.finishButton.showAll();
		} 
		else {
			this.nextButton.showAll();
			this.finishButton.hideAll();
		}
		
		if (page == 1)
			setProcessNext(processSelected);
	}
	
	public void showAll() {
		super.showAll();
		this.setUpCurrentPage();
	}

	public void lifeCycleEvent(LifeCycleEvent event) {
	}

	public boolean lifeCycleQuery(LifeCycleEvent event) {
		if (event.isOfType(LifeCycleEvent.Type.DESTROY) || 
                event.isOfType(LifeCycleEvent.Type.DELETE)) {
					this.hide();
					return true;
		}
		return false;
	}
	
}
