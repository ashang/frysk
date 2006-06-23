/*
 * Created on Oct 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.actions.GenericActionPoint;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskSyscallObserver extends TaskObserverRoot implements TaskObserver.Syscall {
	

	public TaskFilterPoint enteringTaskFilterPoint;
	public TaskFilterPoint exitingTaskFilterPoint;
	
	public TaskActionPoint enteringTaskActionPoint;
	public TaskActionPoint exitingTaskActionPoint;
	
	public GenericActionPoint enteringGenericActionPoint;
	public GenericActionPoint exitingGenericActionPoint;

	public TaskSyscallObserver(	) {
		super("Syscall Observer", "Fires when a system call is made.");
		
		this.enteringTaskFilterPoint = new TaskFilterPoint("Task entering syscall","the Task when it is entering the syscall");
		this.exitingTaskFilterPoint =  new TaskFilterPoint("Task exiting syscall","the Task when it is exiting the syscall");
		
		this.addFilterPoint(enteringTaskFilterPoint);
		this.addFilterPoint(exitingTaskFilterPoint);
		
		this.enteringTaskActionPoint = new TaskActionPoint("Task entering syscall","the Task when it is entering the syscall");
		this.exitingTaskActionPoint =  new TaskActionPoint("Task exiting syscall","the Task when it is exiting the syscall");
		
		this.addActionPoint(enteringTaskActionPoint);
		this.addActionPoint(exitingTaskActionPoint);
		
		this.enteringGenericActionPoint = new GenericActionPoint("Enter Generic Actions","actions run when the task enters a syscall");
		this.exitingGenericActionPoint = new GenericActionPoint("Exit Generic Actions", "actions run when the task exits a syscall");
		
		this.addActionPoint(enteringGenericActionPoint);
		this.addActionPoint(exitingGenericActionPoint);
	}

	public TaskSyscallObserver(TaskSyscallObserver other) {
		super(other);
		
		this.enteringTaskFilterPoint = new TaskFilterPoint(other.enteringTaskFilterPoint);
		this.exitingTaskFilterPoint  = new TaskFilterPoint(other.exitingTaskFilterPoint);
		
		this.addFilterPoint(enteringTaskFilterPoint);
		this.addFilterPoint(exitingTaskFilterPoint);
		
		this.enteringTaskActionPoint = new TaskActionPoint(other.enteringTaskActionPoint);
		this.exitingTaskActionPoint = new TaskActionPoint(other.exitingTaskActionPoint);
		
		this.addActionPoint(enteringTaskActionPoint);
		this.addActionPoint(exitingTaskActionPoint);
		
		this.enteringGenericActionPoint = new GenericActionPoint(other.enteringGenericActionPoint);
		this.exitingGenericActionPoint  = new GenericActionPoint(other.exitingGenericActionPoint);
		
		this.addActionPoint(enteringGenericActionPoint);
		this.addActionPoint(exitingGenericActionPoint);
	}

	public Action updateSyscallEnter(Task task) {
		final Task myTask = task;
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				enterBottomHalf(myTask);
			}
		});
		return Action.BLOCK;
	}

	protected void enterBottomHalf(Task task) {
		this.setInfo(this.getName()+": "+"PID: " + task.getProc().getPid() + " TID: " + task.getTid() + " Event: enter syscall" + " Host: " + Manager.host.getName());
		if(this.runEnterFilters(task)){
			this.runEnterActions(task);
		}

        Action action = this.whatActionShouldBeReturned();
        if(action == Action.CONTINUE){
          task.requestUnblock(this);
        }
	}

	private void runEnterActions(Task task) {
		super.runActions();
		this.enteringGenericActionPoint.runActions(this);
		this.enteringTaskActionPoint.runActions(task);
	}

	private boolean runEnterFilters(Task task) {
		return this.enteringTaskFilterPoint.filter(task);
	}

	public Action updateSyscallExit(Task task) {
		final Task myTask = task;
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				exitBottomHalf(myTask);
			}
		});
		return Action.BLOCK;
	}

	protected void exitBottomHalf(Task task) {
		this.setInfo("PID: " + task.getProc().getPid() + " TID: " + task.getTid() + " Event: leave " + this.getName() + " Host: " + Manager.host.getName());
		if(this.runExitFilters(task)){
			this.runExitActions(task);
		}
		task.requestUnblock(this);
	}

	private void runExitActions(Task task) {
		super.runActions();
		this.exitingGenericActionPoint.runActions(this);
		this.exitingTaskActionPoint.runActions(task);
	}

	private boolean runExitFilters(Task task) {
		return this.exitingTaskFilterPoint.filter(task);
	}

	public void apply(Task task){
		task.requestAddSyscallObserver(this);
	}
	
	public GuiObject getCopy(){
		return new TaskSyscallObserver(this);
	}
	
}
