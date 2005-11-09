/*
 * Created on Oct 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskCloneObserver extends TaskObserverRoot implements TaskObserver.Cloned {

	public TaskCloneObserver(){
		super("ProcCloneObserver", "Fires when a proc calls clone");
	}

	public TaskCloneObserver(TaskCloneObserver observer) {
		super(observer.getName(), observer.getToolTip());
	}

	
	public Action updateCloned(Task task, Task clone) {
		// TODO Auto-generated method stub
		System.out.println("TaskCloneObserver.updateCloned()");
		return Action.CONTINUE;
	}
	
	public void apply(Task task){
		task.requestAddClonedObserver(this);
	}
	
	public TaskObserverRoot getCopy(){
		return new TaskCloneObserver(this);
	}
	
}
