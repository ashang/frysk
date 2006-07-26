package frysk.gui.monitor.observers;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Manager;
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
		
		this.taskFilterPoint = new TaskFilterPoint("Execing Thread", "The thread that is calling exec");
		this.addFilterPoint(taskFilterPoint);
		
		this.taskActionPoint = new TaskActionPoint(taskFilterPoint.getName() + " (A)", taskFilterPoint.getToolTip());
		this.addActionPoint(taskActionPoint);
	}

	public TaskExecObserver(TaskExecObserver other){
		super(other);
		
		this.taskFilterPoint = new TaskFilterPoint(other.taskFilterPoint);
		this.addFilterPoint(taskFilterPoint);
		
		this.taskActionPoint = new TaskActionPoint(other.taskActionPoint);
		this.addActionPoint(taskActionPoint);
	}

	public Action updateExeced(Task task) {
		//System.out.println("TaskExecObserver.updateExeced() " + task.getProc().getCommand());
		final Task myTask = task;
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				bottomHalf(myTask);
			}
		});
		return Action.BLOCK;
	}
	
	private void bottomHalf(Task task){
		this.setInfo(this.getName() + ": " + "PID: " + task.getProc().getPid() + " TID: " + task.getTid() + " Event: called exec " + " Host: " + Manager.host.getName());
		if(this.runFilters(task)){
			this.runActions(task);
		}

        Action action = this.whatActionShouldBeReturned();
        if(action == Action.CONTINUE){
          task.requestUnblock(this);
        }
    }
	
	private void runActions(Task task) {
		super.runActions();
		this.taskActionPoint.runActions(task);
	}

	private boolean runFilters(Task task) {
		return this.filter(task);
	}

	public void apply(Task task){
		task.requestAddExecedObserver(this);
	}
	
	public GuiObject getCopy(){
		return new TaskExecObserver(this);
	}
	
	private boolean filter(Task task){
		return this.taskFilterPoint.filter(task);
	}
	
}
	