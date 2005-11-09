package frysk.gui.monitor.observers;

import frysk.proc.Task;

public abstract class TaskObserverRoot extends ObserverRoot {

	public TaskObserverRoot(String name, String toolTip) {
		super(name, toolTip);
	}

	public TaskObserverRoot(TaskObserverRoot observer) {
		super(observer);
	}

	public abstract void apply(Task task);
	
	public abstract TaskObserverRoot getCopy();
}
	
	
	

