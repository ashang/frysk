package frysk.gui.monitor.observers;

import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

/**
 * Added to observe Exec events.
 * */
public class TaskExecObserver extends ObserverRoot implements TaskObserver.Execed {
	public TaskExecObserver(){
		super("Exec Observer", "Fires everytime this task executes an exec call");
	}

	public Action updateExeced(Task task) {
		// TODO Auto-generated method stub
		return Action.CONTINUE;
	}
	
}
	