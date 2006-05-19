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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.ComboBox;
import org.gnu.gtk.Dialog;
import org.gnu.gtk.Entry;
import org.gnu.gtk.FileChooserButton;
import org.gnu.gtk.Notebook;
import org.gnu.gtk.RadioButton;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.CellRendererTextEvent;
import org.gnu.gtk.event.CellRendererTextListener;
import org.gnu.gtk.event.CellRendererToggleEvent;
import org.gnu.gtk.event.CellRendererToggleListener;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.ToggleEvent;
import org.gnu.gtk.event.ToggleListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

import frysk.gui.monitor.CheckedListView;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ListView;
import frysk.gui.monitor.ProcData;
import frysk.gui.monitor.ProcWiseDataModel;
import frysk.gui.monitor.ProcWiseTreeView;
import frysk.gui.monitor.WindowManager;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;
import frysk.gui.sessions.SessionManager;
import frysk.gui.srcwin.tags.Tagset;
import frysk.gui.srcwin.tags.TagsetManager;
import frysk.proc.Proc;

public class CreateFryskSessionDruid extends Dialog implements LifeCycleListener {

	private ProcWiseDataModel dataModel;
	private ProcWiseTreeView procWiseTreeView;
	private ListView processTagSetSelectionTreeView;
	private ListView addedProcsTreeView;
	private CheckedListView tagSetSelectionTreeView;
	private CheckedListView  observerSelectionTreeView;
	private ListView processObserverSelectionTreeView;	
	
	
	private Button copySession;
	private Button editSession;
	private Button deleteSession;
	
	private Session currentSession = new Session();

	private Notebook notebook;
	private Button nextButton;
	private Button backButton;
	private Button finishButton;
	private Button saveButton;
	private Button cancelButton;

	
	private boolean newSessionSelected = true;
	private boolean editSessionSelected = false;
	private int processSelected = 0;

	public CreateFryskSessionDruid(LibGlade glade) {
		super(glade.getWidget("SessionDruid").getHandle());	
		this.addListener(this);
		getDruidStructureControls(glade);
		getDruidManagerControls(glade);
		getProcessSelectionControls(glade);
		getTagsetObserverControls(glade);
		getProcessObserverControls(glade);
    }
	
	private void setTreeSelected(TreeIter selected, boolean setSelected, boolean setChildren)
	{
		this.dataModel.setSelected(selected,setSelected,setChildren);
	}
	
	private void addProcessParent(TreeIter unfilteredProcessIter) {
		
		if (unfilteredProcessIter == null)
			return;
		
		if (!this.dataModel.getModel().isIterValid(unfilteredProcessIter))
			return;
			
		ProcData proc = (ProcData) this.dataModel.getModel().getValue(unfilteredProcessIter,this.dataModel.getPathDC());
		if (proc != null)
		{
			Proc coreProc = proc.getProc();
			if (coreProc == null)
				if (unfilteredProcessIter.getChildCount() > 0)
				{
					TreeIter childIter = unfilteredProcessIter.getChild(0);
					coreProc = ((ProcData)this.dataModel.getModel().getValue(childIter,this.dataModel.getPathDC())).getProc();
				}
			DebugProcess debugProcess = new DebugProcess(coreProc.getCommand(), proc.getFullExecutablePath());
			debugProcess.setProc(coreProc);
			currentSession.addProcess(debugProcess);
		}
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
		if (processCount > 0)
			this.nextButton.setSensitive(true);
		else
			this.nextButton.setSensitive(false);
	}
	
	private String getNumberSuffix(int i)
	{
		String iString = ""+i;
		if ((i <= 10) || (i >= 20))
			switch (iString.charAt(iString.length()-1)) {
				case '1': return "st";
				case '2': return "nd";
				case '3': return "rd";
				default: return "th";
			}	
		if ((i>=11) && (i<=19))
			return "th";		
		return "";
	}

	private Session copySession(Session source) {
		String session_name = source.getName();
		String name[] = { session_name + " (copy)",
				session_name + " (another copy)" };
		Session dest = (Session) source.getCopy();
		
		for (int i = 0; i < name.length; i++)
			if (SessionManager.theManager.getSessionByName(name[i]) == null) {
				dest.setName(name[i]);
				return dest;
			}
		for (int i = 3; i < Integer.MAX_VALUE - 1; i++)
			if (SessionManager.theManager.getSessionByName(session_name + " ("
				+ i + getNumberSuffix(i) + " copy)") == null) {
				
				dest.setName(session_name + " (" + i + getNumberSuffix(i) + " copy)");
				return dest;
			}

		try {
			dest.setName(session_name + "_"
					+ File.createTempFile("zxc", "dfg").getName());
		} catch (IOException e) {
		}
		return dest;
	}
	
	private void getDruidManagerControls(LibGlade glade) {
		
		RadioButton debugExecutable = (RadioButton) glade.getWidget("sessionDruid_debugExecutableButton");
		debugExecutable.setState(false);
		debugExecutable.setSensitive(false);
	
		FileChooserButton executableChooser = (FileChooserButton) glade.getWidget("sessionDruid_execChooser");
		executableChooser.setSensitive(false);
	
		final ListView previousSessions = new ListView( glade.getWidget("sessionDruid_previousSessionsListView").getHandle());
		previousSessions.watchLinkedList(SessionManager.theManager.getSessions());		
		previousSessions.setSensitive(false);
		previousSessions.addEditListener(new CellRendererTextListener() {
				public void cellRendererTextEvent(CellRendererTextEvent arg0) {
					if (arg0.getType() == CellRendererTextEvent.Type.EDITED)
					{
						Session selected = (Session) previousSessions.getSelectedObject();
						SessionManager.theManager.removeSession(selected);
						selected.setName(arg0.getText());
						SessionManager.theManager.addSession(selected);
						SessionManager.theManager.save();
					}
				}
		});

		final RadioButton newSession = (RadioButton) glade.getWidget("sessionDruid_newSessionButton");
		newSession.setState(true);
		newSession.addListener(new ToggleListener(){
			public void toggleEvent(ToggleEvent arg0) {
				if (arg0.getType() == ToggleEvent.Type.TOGGLED)
					if (newSession.getState())
						newSessionSelected = true;
					else
						newSessionSelected = false;
			}});
					
		final Entry nameEntry = (Entry) glade.getWidget("sessionDruid_sessionName");
		nameEntry.addListener(new EntryListener() {
			public void entryEvent(EntryEvent arg0) {
				currentSession.setName(nameEntry.getText());
				if (nameEntry.getText().length() > 0)
					nextButton.setSensitive(true);
				else
					nextButton.setSensitive(false);
			}});		
		
		final RadioButton previousSession = (RadioButton) glade.getWidget("sessionDruid_previousDebugButton");
		previousSession.setState(false);
		previousSession.addListener(new ToggleListener(){
			public void toggleEvent(ToggleEvent arg0) {
				if (arg0.getType() == ToggleEvent.Type.TOGGLED)
				{
					previousSessions.setSensitive(!previousSessions.getSensitive());
					editSession.setSensitive(!editSession.getSensitive());
					copySession.setSensitive(!copySession.getSensitive());
					deleteSession.setSensitive(!deleteSession.getSensitive());
					if (previousSession.getState())
					{
						nextButton.hideAll();
						finishButton.showAll();
					}
					else
					{
						nextButton.showAll();
						finishButton.hideAll();
					}
				}
			}});

		previousSessions.getSelection().addListener(new TreeSelectionListener(){
			public void selectionChangedEvent(TreeSelectionEvent arg0) {
				Session selected = (Session)previousSessions.getSelectedObject();		
				if (selected != null)
					finishButton.setSensitive(true);
				else
				{
					finishButton.setSensitive(false);
					currentSession = (Session) previousSessions.getSelectedObject();
				}
				
			}});
		
		editSession = (Button) glade.getWidget("sessionDruid_editSessionButton");
		editSession.setSensitive(false);
		editSession.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
				{
					editSessionSelected= true;
					currentSession = (Session)previousSessions.getSelectedObject();
					attachLinkedListsToWidgets();
					notebook.setShowTabs(true);
					notebook.setCurrentPage(1);
					finishButton.hideAll();
					cancelButton.showAll();
					saveButton.showAll();
	
				}
				
			}});
		
		copySession = (Button) glade.getWidget("sessionDruid_copySessionButton");
		copySession.setSensitive(false);
		copySession.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
				{
					Session selected = (Session)previousSessions.getSelectedObject();
					SessionManager.theManager.addSession(copySession(selected));
					SessionManager.theManager.save();
				}
			}});
		
		deleteSession = (Button) glade.getWidget("sessionDruid_deleteSessionButton");
		deleteSession.setSensitive(false);
		deleteSession.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
				{
					Session selected = (Session)previousSessions.getSelectedObject();
					if (selected != null)
						SessionManager.theManager.removeSession(selected);
				}
			}});

		SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.HORIZONTAL);
		sizeGroup.addWidget(nameEntry);
		sizeGroup.addWidget(executableChooser);
		
		SizeGroup buttonSizeGroup = new SizeGroup(SizeGroupMode.HORIZONTAL);
		buttonSizeGroup.addWidget(newSession);
		buttonSizeGroup.addWidget(debugExecutable);
		buttonSizeGroup.addWidget(previousSession);
	}
	
	private void getProcessSelectionControls(LibGlade glade) {
		
		Button addProcessGroupButton;
		Button removeProcessGroupButton;
		ComboBox hostSelection;
		
		// Page 1 of the Druid. Initial Process Selection.
		
		// Create New Live Data Model and mount on the TreeView
		this.dataModel = new ProcWiseDataModel();
		procWiseTreeView = new ProcWiseTreeView(glade.getWidget("sessionDruid_procWiseTreeView").getHandle(),this.dataModel);
		
		// Create a New ListView and mount the Linked List from Session data 
		addedProcsTreeView = new ListView(glade.getWidget("sessionDruid_addedProcsTreeView").getHandle());
		addedProcsTreeView.watchLinkedList(currentSession.getProcesses());
		
		hostSelection = (ComboBox) glade.getWidget("sessionDruid_hostComboBox");
		this.setUpCurrentPage();
			
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
		hostSelection.setActive(0);	
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
					if (editSessionSelected) {
						editSessionSelected = false;
						notebook.setShowTabs(false);
						notebook.setCurrentPage(0);
						SessionManager.theManager.save();
						return;
					}
					
					if (newSessionSelected)
						SessionManager.theManager.addSession(currentSession);
					SessionManager.theManager.save();
					WindowManager.theManager.mainWindow.setSession(currentSession);
					hideAll();
				}
			}
		});
		
		this.saveButton = (Button) glade.getWidget("sessionDruid_saveEditSessionButton");
		this.saveButton.hideAll();
		this.saveButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
						editSessionSelected = false;
						notebook.setShowTabs(false);
						notebook.setCurrentPage(0);
						SessionManager.theManager.save();
						saveButton.hideAll();
						cancelButton.hideAll();
						finishButton.showAll();
						currentSession = new Session();
					}
				}});
		
		this.cancelButton = (Button) glade.getWidget("sessionDruid_cancelButton");
		this.cancelButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
						editSessionSelected = false;
						notebook.setShowTabs(false);
						notebook.setCurrentPage(0);
						SessionManager.theManager.load();
						saveButton.hideAll();
						cancelButton.hideAll();
						finishButton.showAll();
						currentSession = new Session();
					}
				}});
	}
	
	private void attachLinkedListsToWidgets()
	{
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
					this.hideAll();
					return true;
		}
		return false;
	}
	
}
