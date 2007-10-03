// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

package frysk.gui.monitor.observers;

import frysk.gui.dialogs.DialogManager;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.actions.TaskAction;
import frysk.gui.monitor.eventviewer.Event;
import frysk.proc.Action;
import frysk.proc.Task;

/*
 * Created on Sep 29, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

public class ExitNotificationObserver extends TaskTerminatingObserver {

	public ExitNotificationObserver() {
		super();
		this.setName("Exit Notification Observer");
		this.setToolTip("Notify the user that a task is about to exit\n" +
				"and provide them the option to stop or resume the process");
		
		frysk.gui.monitor.actions.TaskAction myAction = new TaskAction(){
			public void execute(Task task, TaskObserverRoot observer, Event event) {
				if(DialogManager.showQueryDialog("Task ["+task+"] is about to exit.\n Would you like to block it")){
					setReturnAction(Action.BLOCK);
				}else{
					setReturnAction(Action.CONTINUE);
				}
			}
		
			public GuiObject getCopy() {
				return null;
			}
		
			public boolean setArgument(String argument) {
				return true;
			}
		
			public String getArgument() {
				return null;
			}
		
			public ObservableLinkedList getArgumentCompletionList() {
				return null;
			}			
		};
		myAction.dontSaveObject();
		
		this.taskActionPoint.addAction(myAction);
		
	}
	
	public ExitNotificationObserver(ExitNotificationObserver other) {
		super(other);
		
		frysk.gui.monitor.actions.TaskAction myAction = new TaskAction(){
			public void execute(Task task, TaskObserverRoot observer, Event event) {
//				if(DialogManager.showQueryDialog("Task ["+task+"] is about to exit.\n Would you like to block it")){
//					setReturnAction(Action.BLOCK);
//				}else{
//					setReturnAction(Action.CONTINUE);
//				}
			}
		
			public GuiObject getCopy() {
				return null;
			}
		
			public boolean setArgument(String argument) {
				return true;
			}
		
			public String getArgument() {
				return null;
			}
		
			public ObservableLinkedList getArgumentCompletionList() {
				return null;
			}			
		};
		myAction.dontSaveObject();
		
		this.taskActionPoint.addAction(myAction);
	}
	
	public GuiObject getCopy(){
		return new ExitNotificationObserver(this);
	}
	
}
