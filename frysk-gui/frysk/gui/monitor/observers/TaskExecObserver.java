package frysk.gui.monitor.observers;

import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

/**
 * Added to observe Exec events.
 * */
public class TaskExecObserver extends TaskObserverRoot implements TaskObserver.Execed {
	public TaskExecObserver(){
		super("Exec Observer", "Fires everytime this task executes an exec call");
	}

	public TaskExecObserver(TaskExecObserver observer) {
		super(observer.getName(), observer.getToolTip());
	}

	public Action updateExeced(Task task) {
		System.out.println("TaskExecObserver.updateExeced()");
		return Action.CONTINUE;
	}
	
	public void apply(Task task){
		task.requestAddExecedObserver(this);
	}
	
	public TaskObserverRoot getCopy(){
		return new TaskExecObserver(this);
	}
	
}
	