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
import frysk.gui.monitor.GuiTask;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.eventviewer.Event;
import frysk.gui.monitor.eventviewer.EventManager;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskCloneObserver extends TaskObserverRoot implements TaskObserver.Cloned {

	public TaskFilterPoint parentTaskFilterPoint;
	public TaskFilterPoint offspringTaskFilterPoint;

	public TaskActionPoint parentTaskActionPoint;
	public TaskActionPoint offspringTaskActionPoint;

	public TaskCloneObserver(){
		super("Clone Observer", "Fires when a proc calls clone");

		this.parentTaskFilterPoint = new TaskFilterPoint("cloning thread","Thread that made the clone system call");
		this.offspringTaskFilterPoint = new TaskFilterPoint("cloned thread","New thread that has just been created as a result of clone call");
		
		this.addFilterPoint(parentTaskFilterPoint);
		this.addFilterPoint(offspringTaskFilterPoint);
		
		this.parentTaskActionPoint = new TaskActionPoint("cloning thread","Thread that made the clone system call");
		this.offspringTaskActionPoint = new TaskActionPoint("cloned thread","New thread that has just been created as a result of clone call");
		
		this.addActionPoint(parentTaskActionPoint);
		this.addActionPoint(offspringTaskActionPoint);
		
	}

	public TaskCloneObserver(TaskCloneObserver other) {
		super(other);
		
		this.parentTaskFilterPoint = new TaskFilterPoint(other.parentTaskFilterPoint);
		this.offspringTaskFilterPoint = new TaskFilterPoint(other.offspringTaskFilterPoint);
		
		this.addFilterPoint(parentTaskFilterPoint);
		this.addFilterPoint(offspringTaskFilterPoint);
		
		this.parentTaskActionPoint = new TaskActionPoint(other.parentTaskActionPoint);
		this.offspringTaskActionPoint = new TaskActionPoint(other.parentTaskActionPoint);
		
		this.addActionPoint(parentTaskActionPoint);
		this.addActionPoint(offspringTaskActionPoint);

	}

	
	public Action updateClonedParent(Task task, Task clone) {
        final Task myTask = task;
        final Task myClone = clone;
        org.gnu.glib.CustomEvents.addEvent(new Runnable(){
            public void run() {
                bottomHalfParent(myTask, myClone);
            }
        });
        return Action.BLOCK;
	}
	
    // XXX: Sami, take a look at frysk.proc.TestTaskObserverBlocked,
    // in particular how it has generic "spawnParent" and
    // "spawnOffspring" methods called by sub-classes that implement
    // TaskObserver.Forked and TaskObserver.Cloned.

	public Action updateClonedOffspring (Task task, Task clone) {
		//System.out.println("TaskCloneObserver.updateCloned()");
		final Task myTask = task;
		final Task myClone = clone;
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				bottomHalfOffspring(myTask, myClone);
			}
		});
		return Action.BLOCK;
	}
	
	private void bottomHalfParent(Task task, Task offspring){
		this.setInfo(this.getName() + ": " + "PID: " + task.getProc().getPid() + " TID: " + task.getTid() + " Event: cloned new task TID: "+ offspring.getTid() + " Host: " + Manager.host.getName());
		if(this.runFiltersParent(task, offspring)){
			this.runActionsParent(task, offspring);
		}
		
        Action action = this.whatActionShouldBeReturned();
        if(action == Action.CONTINUE){
          task.requestUnblock(this);
        }
	}
	
    private void bottomHalfOffspring(Task task, Task offspring){
        this.setInfo(this.getName() + ": " + "PID: " + task.getProc().getPid() + " TID: " + task.getTid() + " Event: cloned new task TID: "+ offspring.getTid() + " Host: " + Manager.host.getName());
        if(this.runFiltersOffspring(task, offspring)){
            this.runActionsOffspring(task, offspring);
        }
        
        Action action = this.whatActionShouldBeReturned();
        if(action == Action.CONTINUE){
          offspring.requestUnblock(this);
        }
    }
    
    private boolean runFiltersParent(Task task, Task clone){
        if(!this.parentTaskFilterPoint.filter(task )) return false;
        return true;
    }
    
    private boolean runFiltersOffspring(Task task, Task clone){
        if(!this.offspringTaskFilterPoint.filter(clone)) return false;
        return true;
    }
    
    private void runActionsParent(Task task, Task clone){
        super.runActions();
        this.parentTaskActionPoint.runActions(task);
        this.offspringTaskActionPoint.runActions(clone);
        EventManager.theManager.addEvent(new Event("cloning " + clone.getTid(), "thread called clone and created " + clone, GuiTask.GuiTaskFactory.getGuiTask(task), this));
    }
    
    private void runActionsOffspring(Task task, Task clone){
        super.runActions();
        this.offspringTaskActionPoint.runActions(clone);
        EventManager.theManager.addEvent(new Event("cloned by " + task.getTid(), "this thread was cloned by " + task, GuiTask.GuiTaskFactory.getGuiTask(clone), this));
    }
    
	public void apply(Task task){
		task.requestAddClonedObserver(this);
	}
	
	public GuiObject getCopy(){
		return new TaskCloneObserver(this);
	}

    public void unapply (Task task)
    {
      task.requestDeleteClonedObserver(this);
    }
	
}
