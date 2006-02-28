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

package frysk.gui;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.Dialog;
import org.gnu.gtk.Notebook;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;

import frysk.gui.monitor.ProcWiseTreeView;

public class StartDebugSessionDialog extends Dialog {
	
	Notebook notebook;
	
	ProcWiseTreeView procWiseTreeView;  
	Button nextButton;
	Button backButton;
	Button finishButton;
	
	public StartDebugSessionDialog(LibGlade glade){
		super(glade.getWidget("StartDebugSessionDialog").getHandle());
		
		this.notebook = (Notebook) glade.getWidget("startDebugSessionNoteBook");
	
		this.nextButton = (Button) glade.getWidget("startDebugSessionDialogNextButton");
		this.nextButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					nextPage();
				}
			}
		});

		this.backButton = (Button) glade.getWidget("startDebugSessionDialogBackButton");
		this.backButton.addListener(new ButtonListener(){
			public void buttonEvent(ButtonEvent event) {
				if(event.isOfType(ButtonEvent.Type.CLICK)){
					previousPage();
				}
			}
		});
		
		this.finishButton = (Button) glade.getWidget("startDebugSessionFinishButton");
		
		this.procWiseTreeView = new ProcWiseTreeView(glade.getWidget("procWiseTreeView").getHandle());
		this.setUpCurrentPage();
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
