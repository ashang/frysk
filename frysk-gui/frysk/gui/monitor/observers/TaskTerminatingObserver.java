/*
 * Created on Sep 29, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.filters.IntFilterPoint;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskTerminatingObserver extends TaskObserverRoot implements TaskObserver.Terminating {

	public TaskFilterPoint taskFilterPoint;
	public IntFilterPoint intFilterPoint;
	
	public TaskActionPoint taskActionPoint;
	
	public TaskTerminatingObserver() {
		super("Task Terminating Observer", "Fires when this process is exiting");
		
		this.taskFilterPoint = new TaskFilterPoint("Terminating Task","The task that is terminating");
		this.intFilterPoint = new IntFilterPoint("Exit Value","the exit value of the task");

		this.addFilterPoint(taskFilterPoint);
		this.addFilterPoint(intFilterPoint);
		
		this.taskActionPoint = new TaskActionPoint("Terminating Task","The task that is terminating");
		
		this.addActionPoint(taskActionPoint);
	}
	
	public TaskTerminatingObserver(TaskTerminatingObserver other) {
		super(other);

		this.taskFilterPoint = new TaskFilterPoint(other.taskFilterPoint);
		this.intFilterPoint  = new IntFilterPoint(other.intFilterPoint);

		this.addFilterPoint(taskFilterPoint);
		this.addFilterPoint(intFilterPoint);
		
		this.taskActionPoint = new TaskActionPoint(other.taskActionPoint);
		
		this.addActionPoint(taskActionPoint);
		
	}

	public Action updateTerminating(Task task, boolean signal, int value) {
		final Task myTask = task;
		final boolean mySignal = signal;
		final int myValue = value;
		
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				bottomHalf(myTask, mySignal, myValue);
			}
		});
		return Action.BLOCK;
	}
	
	protected void bottomHalf(Task task, boolean signal, int value) {
		this.setInfo("PID: " + task.getProc().getPid() + " TID: " + task.getTid() + " Event: " + this.getName() + " Host: " + Manager.host.getName());
		if(this.runFilters(task, signal, value)){
			this.runActions(task, signal, value);
		}
		task.requestUnblock(this);
	}

	private void runActions(Task task, boolean signal, int value) {
		// TODO implement action points to take care of signal and value
		super.runActions();
		this.taskActionPoint.runActions(task);
	}

	private boolean runFilters(Task task, boolean signal, int value) {
		if(!this.taskFilterPoint.filter(task)) return false;
		//To do add boolean filterPoint
		if(!this.intFilterPoint.filter(value)) return false;
		return true;
	}

	public void apply(Task task){
		task.requestAddTerminatingObserver(this);
	}
	
	public GuiObject getCopy(){
		return new TaskTerminatingObserver(this);
	}

}
