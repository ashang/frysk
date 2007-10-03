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

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.Dialog;
import org.gnu.gtk.Notebook;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModelFilter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;

import frysk.gui.monitor.ProcWiseDataModel;
import frysk.gui.monitor.ProcWiseTreeView;

public class CreateFryskSessionDruid extends Dialog {
	
	Notebook notebook;
	
	ProcWiseTreeView procWiseTreeView;
	ProcWiseDataModel dataModel;
	AddedProcTreeView addedProcsTreeView;
	
	Button nextButton;
	Button backButton;
	Button finishButton;
	
	Button addProcessGroupButton;
	Button removeProcessGroupButton;
	
	public CreateFryskSessionDruid(LibGlade glade){
		super(glade.getWidget("SessionDruid").getHandle());
		
		getDruidStructureControls(glade);
		getProcessSelectionControls(glade);
		

    }
	
	private void getProcessSelectionControls(LibGlade glade) {
		this.dataModel = new ProcWiseDataModel();
		this.procWiseTreeView = new ProcWiseTreeView(glade.getWidget("sessionDruid_procWiseTreeView").getHandle(),this.dataModel);
		this.addedProcsTreeView = new AddedProcTreeView(glade.getWidget("sessionDruid_addedProcsTreeView").getHandle(),this.dataModel);
		this.setUpCurrentPage();
		
		//this.addedProcsTreeView = (TreeView) glade.getWidget("sessionDruid_addedProcsTreeView");
		
		SizeGroup sizeGroup = new SizeGroup(SizeGroupMode.BOTH);
		sizeGroup.addWidget(procWiseTreeView);
		sizeGroup.addWidget(addedProcsTreeView);
		
		
		this.addProcessGroupButton = (Button) glade.getWidget("sessionDruid_addProcessGroupButton");
		this.removeProcessGroupButton = (Button) glade.getWidget("sessionDruid_removeProcessGroupButton");
		
		this.addProcessGroupButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){

					TreePath[] tp = procWiseTreeView.getSelection().getSelectedRows();
					TreeModelFilter ts = (TreeModelFilter) procWiseTreeView.getModel();

					if (tp.length > 0)
					{
						for(int i=0; i<tp.length;i++)
						{
							TreePath unfiltered = ts.convertPathToChildPath(tp[i]);
							TreeIter item2 = procWiseTreeView.psDataModel.getModel().getIter(unfiltered);
							
							// Check if he clicks on a process with children. If so, move the children over
							if (item2.getChildCount() > 0)
							{
								procWiseTreeView.psDataModel.setSelected(item2,true);
								System.out.println("This one has children");
								int children = item2.getChildCount();
								for (int z=0; z<children;z++)
									procWiseTreeView.psDataModel.setSelected(item2.getChild(z),true);
								return;
							}
							else
							{
								// Check if the node has a parent. 
								// TreePath.up() seems to have some usses
								// iter.hasParent() seems to have some issues
								// Check Path directly.
								TreePath parent_path = item2.getPath();
								if (parent_path.toString().split(":").length > 1)
								{
									// we are a child that has a parent
									// move sibilings and parent
	
									parent_path.up();
									
									// Save parent iter
									TreeIter parent_iter = procWiseTreeView.psDataModel.getModel().getIter(parent_path);

									// Check if he clicks on a process with children. If so, move the children over
									if (parent_iter.getChildCount() > 0)
									{
										procWiseTreeView.psDataModel.setSelected(parent_iter,true);
										System.out.println("This one has children");
										int children = parent_iter.getChildCount();
										for (int z=0; z<children;z++)
											procWiseTreeView.psDataModel.setSelected(parent_iter.getChild(z),true);
										return;
									}
								}
								else
								{
									
									procWiseTreeView.psDataModel.setSelected(item2,true);
								}
							}
							
						}
					}
					
				}
				
			}
			
		});
		
		this.removeProcessGroupButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					System.out.println("Got a remove process group Event");
				}
				
			}
			
		});

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
	}
	
	public void showAll(){
		super.showAll();
		this.setUpCurrentPage();
	}
}
