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


/*
 * Created on Oct 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskCloneObserver extends TaskObserverRoot implements TaskObserver.Cloned {

	public TaskFilterPoint cloningTaskFilterPoint;
	public TaskFilterPoint clonedTaskFilterPoint;

	public TaskActionPoint cloningTaskActionPoint;
	public TaskActionPoint clonedTaskActionPoint;

	public TaskCloneObserver(){
		super("Clone Observer", "Fires when a proc calls clone");

		this.cloningTaskFilterPoint = new TaskFilterPoint("cloning thread","Thread that made the clone system call");
		this.clonedTaskFilterPoint = new TaskFilterPoint("cloned thread","New thread that has just been created as a result of clone call");
		
		this.addFilterPoint(cloningTaskFilterPoint);
		this.addFilterPoint(clonedTaskFilterPoint);
		
		this.cloningTaskActionPoint = new TaskActionPoint("cloning thread","Thread that made the clone system call");
		this.clonedTaskActionPoint = new TaskActionPoint("cloned thread","New thread that has just been created as a result of clone call");
		
		this.addActionPoint(cloningTaskActionPoint);
		this.addActionPoint(clonedTaskActionPoint);
		
	}

	public TaskCloneObserver(TaskCloneObserver other) {
		super(other);
		
		this.cloningTaskFilterPoint = new TaskFilterPoint(other.cloningTaskFilterPoint);
		this.clonedTaskFilterPoint = new TaskFilterPoint(other.clonedTaskFilterPoint);
		
		this.addFilterPoint(cloningTaskFilterPoint);
		this.addFilterPoint(clonedTaskFilterPoint);
		
		this.cloningTaskActionPoint = new TaskActionPoint(other.cloningTaskActionPoint);
		this.clonedTaskActionPoint = new TaskActionPoint(other.cloningTaskActionPoint);
		
		this.addActionPoint(cloningTaskActionPoint);
		this.addActionPoint(clonedTaskActionPoint);

	}

	
	public Action updateClonedParent(Task task, Task clone) {
		return Action.BLOCK;
	}
	
    // XXX: Sami, take a look at frysk.proc.TestTaskObserverBlocked,
    // in particular how it has generic "spawnParent" and
    // "spawnOffspring" methods called by sub-classes that implement
    // TaskObserver.Forked and TaskObserver.Cloned.

	public Action updateClonedOffspring (Task task, Task clone) {
		// TODO Auto-generated method stub
		//System.out.println("TaskCloneObserver.updateCloned()");
		final Task myTask = task;
		final Task myClone = clone;
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				bottomHalf(myTask, myClone);
			}
		});
		return Action.BLOCK;
	}
	
	private void bottomHalf(Task task, Task clone){
		this.setInfo(this.getName() + ": " + "PID: " + task.getProc().getPid() + " TID: " + task.getTid() + " Event: cloned new task TID: "+ clone.getTid() + " Host: " + Manager.host.getName());
		if(this.runFilters(task, clone)){
			this.runActions(task, clone);
		}
		
        Action action = this.whatActionShouldBeReturned();
        if(action == Action.CONTINUE){
          clone.requestUnblock(this);
        }
	}
	
	private boolean runFilters(Task task, Task clone){
		if(!this.cloningTaskFilterPoint.filter(task )) return false;
		if(!this.clonedTaskFilterPoint.filter(clone)) return false;
		return true;
	}
	
	private void runActions(Task task, Task clone){
		super.runActions();
		this.cloningTaskActionPoint.runActions(task);
		this.clonedTaskActionPoint.runActions(clone);
	}
	
	public void apply(Task task){
		task.requestAddTaskObserver(this);
	}
	
	public GuiObject getCopy(){
		return new TaskCloneObserver(this);
	}

  public void unapply (Task task)
  {
    task.requestDeleteTaskObserver(this);
  }
	
}
