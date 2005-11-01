/*
 * Created on Sep 29, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskTerminatingObserver extends ObserverRoot implements TaskObserver.Terminating {

	public TaskTerminatingObserver() {
		super("Task Terminating Observer", "Fires fires when this process is exiting");
	}
	
	public TaskTerminatingObserver(TaskTerminatingObserver observer) {
		super(observer.getName(), observer.getToolTip());
	}

	public Action updateTerminating(Task task, boolean signal, int value) {
		System.out.println("TaskTerminatingObserver.updateTerminating()");
		return Action.CONTINUE;
	}
	
	public void apply(Task task){
		task.requestAddTerminatingObserver(this);
	}
	
	public ObserverRoot getCopy(){
		return new TaskTerminatingObserver(this);
	}

}
