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
		super("TaskCloneObserver", "Fires when a proc calls clone");

		this.cloningTaskFilterPoint = new TaskFilterPoint("Cloning Thread","Thread that made the clone system call");
		this.clonedTaskFilterPoint = new TaskFilterPoint("Cloned Thread","New thread that has just been created as a result of clone call");
		
		this.addFilterPoint(cloningTaskFilterPoint);
		this.addFilterPoint(clonedTaskFilterPoint);
		
		this.cloningTaskActionPoint = new TaskActionPoint("Cloning Thread","Thread that made the clone system call");
		this.clonedTaskActionPoint = new TaskActionPoint("Cloned Thread","New thread that has just been created as a result of clone call");
		
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
		task.requestAddClonedObserver(this);
	}
	
	public GuiObject getCopy(){
		return new TaskCloneObserver(this);
	}
	
}
