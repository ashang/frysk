package frysk.gui.monitor.observers;

import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

/**
 * Added to observe Exec events.
 * */
public class TaskExecObserver extends TaskObserverRoot implements TaskObserver.Execed {
	
	public TaskFilterPoint taskFilterPoint;
	
	public TaskActionPoint taskActionPoint;
	
	public TaskExecObserver(){
		super("Exec Observer", "Fires every time this task executes an exec call");
		
		this.taskFilterPoint = new TaskFilterPoint("Exec'ing Thread", "The thread that is calling exec");
		this.addFilterPoint(taskFilterPoint);
		
		this.taskActionPoint = new TaskActionPoint(taskFilterPoint.getName(), taskFilterPoint.getToolTip());
		this.addActionPoint(taskActionPoint);
	}

	public TaskExecObserver(TaskExecObserver other){
		super(other);
		
		this.taskFilterPoint = new TaskFilterPoint(other.taskFilterPoint);
		// this.addFilterPoint(taskFilterPoint); not needed done by parent constructor
		
		this.taskActionPoint = new TaskActionPoint(taskFilterPoint.getName(), taskFilterPoint.getToolTip());
		// this.addActionPoint(taskActionPoint); not needed done by parent constructor
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
			this.runActions(task);
		}
		task.requestUnblock(this);
	}
	
	private void runActions(Task task) {
		this.genericActionPoint.runActions();
		this.taskActionPoint.runActions(task);
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
	