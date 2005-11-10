package frysk.gui.monitor.observers;

import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

/**
 * Added to observe Exec events.
 * */
public class TaskExecObserver extends TaskObserverRoot implements TaskObserver.Execed {
	
	TaskFilterPoint taskFilterPoint;
	
	public TaskExecObserver(){
		super("Exec Observer", "Fires everytime this task executes an exec call");
		this.taskFilterPoint = new TaskFilterPoint("Exec'ing Thread", "The thread that is calling exec");
	}

	public TaskExecObserver(TaskExecObserver observer) {
		super(observer.getName(), observer.getToolTip());
	}

	public Action updateExeced(Task task) {
		System.out.println("TaskExecObserver.updateExeced() " + task.getProc().getCommand());
		final Task myTask = task;
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				bottomHalf(myTask);
			}
		});
		return Action.BLOCK;
	}
	
	private void bottomHalf(Task task){
		if(this.runFilters(task)){
			this.runActions();
			this.runExecActions(task);
		}
		task.requestUnblock(this);
	}
	
	private void runExecActions(Task task) {
		// TODO Auto-generated method stub		
	}

	private boolean runFilters(Task task) {
		return this.filter(task);
	}

	public void apply(Task task){
		task.requestAddExecedObserver(this);
	}
	
	public TaskObserverRoot getCopy(){
		return new TaskExecObserver(this);
	}
	
	private boolean filter(Task task){
		return this.taskFilterPoint.filter(task);
	}
	
}
	