package frysk.gui.monitor.actions;

import frysk.proc.Task;

public abstract class TaskAction extends Action {
	
	public TaskAction(){
		super();
	}
	
	public TaskAction(String name, String toolTip) {
		super(name, toolTip);
	}

	public TaskAction(TaskAction other){
		super(other);
	}
	
	public abstract void execute(Task task);

	public void execute(Task[] tasks){
		for (int i = 0; i < tasks.length; i++) {
			this.execute(tasks[i]);
		}
	}
	
	
}
