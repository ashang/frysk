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

import java.util.ArrayList;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.ComboBox;
import org.gnu.gtk.Dialog;
import org.gnu.gtk.Notebook;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;

import frysk.gui.monitor.ProcWiseDataModel;
import frysk.gui.monitor.ProcWiseTreeView;

public class CreateFryskSessionDruid extends Dialog {
	
	Notebook notebook;

	ProcWiseDataModel dataModel;
	ExitProcessGroupsDataModel processExitListStore;

	Button nextButton;
	Button backButton;
	Button finishButton;
	
	ArrayList processGroupSelection;
	
	int processSelected = 0;
	

	
	public CreateFryskSessionDruid(LibGlade glade){
		super(glade.getWidget("SessionDruid").getHandle());		
		getDruidStructureControls(glade);
		getProcessSelectionControls(glade);
		getProcessExitControls(glade);
    }
	
	private void setTreeSelected(TreeIter selected, boolean setSelected, boolean setChildren)
	{
		this.dataModel.setSelected(selected,setSelected,setChildren);
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
	
		
	private void changeGroupState(TreeView tree, TreePath[] selectedProcs, boolean state)
	{
		if (selectedProcs.length > 0)
		{
			for(int i=0; i<selectedProcs.length;i++)
			{
				// Convert a filetered iterator to a non filtered iterator.
				TreeIter unfilteredProcessIter = this.dataModel.getModel().getIter(deFilterPath(tree,selectedProcs[i]));
				
				// Scenario 1: Tree iter has children (a process group)
				if (unfilteredProcessIter.getChildCount() > 0)
				{
					if (state)
						processSelected += unfilteredProcessIter.getChildCount() +1;
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
								processSelected += unfilteredProcessIter.getChildCount() +1;
							else
								processSelected -= unfilteredProcessIter.getChildCount() +1;
							setTreeSelected(parent_iter,state,true);
						}
					}
					else
					{
						// Scenario 3: No children, or siblings
						setTreeSelected(unfilteredProcessIter,state,false);
						if (state)
							processSelected++;
						else
							processSelected--;
						
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
	
	private void getProcessSelectionControls(LibGlade glade) {
		
		Button addProcessGroupButton;
		Button removeProcessGroupButton;
		ComboBox hostSelection;
		
		final ProcWiseTreeView procWiseTreeView;
		final AddedProcTreeView addedProcsTreeView;

		// Page 1 of the Druid. Initial Process Selection.
		
		this.dataModel = new ProcWiseDataModel();
		procWiseTreeView = new ProcWiseTreeView(glade.getWidget("sessionDruid_procWiseTreeView").getHandle(),this.dataModel);
		addedProcsTreeView = new AddedProcTreeView(glade.getWidget("sessionDruid_addedProcsTreeView").getHandle(),this.dataModel);
		hostSelection = (ComboBox) glade.getWidget("sessionDruid_hostComboBox");
		this.setUpCurrentPage();
			
		SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.BOTH);
		sizeGroup.addWidget(procWiseTreeView);
		sizeGroup.addWidget(addedProcsTreeView);
			
		addProcessGroupButton = (Button) glade.getWidget("sessionDruid_addProcessGroupButton");
		removeProcessGroupButton = (Button) glade.getWidget("sessionDruid_removeProcessGroupButton");
		
		addProcessGroupButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					changeGroupState(procWiseTreeView,procWiseTreeView.getSelection().getSelectedRows(),true);
				}
			}
			
		});
		
		removeProcessGroupButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					changeGroupState(addedProcsTreeView,addedProcsTreeView.getSelection().getSelectedRows(),false);
				}
			}
		});
		
		hostSelection.setActive(0);
		
	}
	
	private void getProcessExitControls(LibGlade glade)
	{
	
		ProcessExitSelectionTreeView processExitSelectionTreeView;
		this.processExitListStore = new ExitProcessGroupsDataModel();
		processExitSelectionTreeView = new ProcessExitSelectionTreeView(
				glade.getWidget("sessionDruid_unexpectedExitTreeView").getHandle(),this.processExitListStore);	
		
		processExitSelectionTreeView.expandAll();
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

		this.backButton = (Button) glade.getWidget("sessionDruid_backButton");
		this.backButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					previousPage();
				}
			}
		});
		
		this.finishButton = (Button) glade.getWidget("sessionDruid_finishButton");
		
	}
	
	
	private void nextPage(){
		
		// Process previous page data
		int page = this.notebook.getCurrentPage();
		if (page == 1){
			processGroupSelection = this.dataModel.dumpSelectedProcesses();
			this.processExitListStore.populateData(processGroupSelection);
		}
		if (page == 2)
		{
			
		}

		
		this.notebook.setCurrentPage(this.notebook.getCurrentPage()+1);
		this.setUpCurrentPage();
	}
	

	private void previousPage(){
		this.notebook.setCurrentPage(this.notebook.getCurrentPage()-1);
		this.setUpCurrentPage();
	}

	private void setUpCurrentPage(){
		int page = this.notebook.getCurrentPage();
		
		if(page == 0){
			this.backButton.setSensitive(false);
		}else{
			this.backButton.setSensitive(true);
		}
		
		if(page == this.notebook.getNumPages()-1){
			this.nextButton.hideAll();
			this.finishButton.showAll();
		}else{
			this.nextButton.showAll();
			this.finishButton.hideAll();
		}
		
		if (page == 1)
			setProcessNext(processSelected);
	}
	
	public void showAll(){
		super.showAll();
		this.setUpCurrentPage();
	}
}
