/*
 * Created on Oct 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import java.util.logging.Level;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.WindowManager;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Manager;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskForkedObserver extends TaskObserverRoot implements TaskObserver.Forked{

//	ObservableLinkedList forkedActions;
	
	public TaskFilterPoint forkingTaskFilterPoint;
	public TaskFilterPoint forkedTaskFilterPoint;

	public TaskActionPoint forkingTaskActionPoint;
	
	public TaskActionPoint forkedTaskActionPoint;
	
	public TaskForkedObserver() {
		super("Fork Observer", "Fires when a proc forks");
		
		this.forkingTaskFilterPoint = new TaskFilterPoint("forking thread", "Thread that performed the fork");
		this.forkedTaskFilterPoint = new TaskFilterPoint("forked thread","Main thread of newly forked process");
		
		this.addFilterPoint(this.forkingTaskFilterPoint);
		this.addFilterPoint(this.forkedTaskFilterPoint);
		
		this.forkingTaskActionPoint = new TaskActionPoint("forking thread", "Thread that performed the fork");
		this.forkedTaskActionPoint = new TaskActionPoint("forked thread","Main thread of newly forked process");
		
		this.addActionPoint(this.forkingTaskActionPoint);
		this.addActionPoint(this.forkedTaskActionPoint);
		
	}

	public TaskForkedObserver(TaskForkedObserver other) {
		super(other);
		
		this.forkingTaskFilterPoint = new TaskFilterPoint(other.forkingTaskFilterPoint);
		this.forkedTaskFilterPoint = new TaskFilterPoint(other.forkedTaskFilterPoint);

		this.addFilterPoint(this.forkingTaskFilterPoint);
		this.addFilterPoint(this.forkedTaskFilterPoint);

		this.forkingTaskActionPoint = new TaskActionPoint(other.forkingTaskActionPoint);
		this.forkedTaskActionPoint  = new TaskActionPoint(other.forkedTaskActionPoint);
		
		this.addActionPoint(this.forkingTaskActionPoint);
		this.addActionPoint(this.forkedTaskActionPoint);

	}

	public Action updateForkedParent (Task task, Task child) {
	    return Action.BLOCK;
	}
	
	public Action updateForkedOffspring (Task task, Task child) {
//		WarnDialog dialog = new WarnDialog("Fork ya'll");
//		dialog.showAll();
//		dialog.run();

		WindowManager.logger.log(Level.FINE, "{0} updateForkedOffspring child: {1} \n", new Object[]{this, child});
		final Task myTask = task;
		final Task myChild = child;
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
			    // This does the unblock.
				bottomHalf(myTask, myChild);
			}
		});
		
		//return this.getReturnAction();
		return Action.BLOCK;
	}

	private void bottomHalf(Task task, Task child){
		WindowManager.logger.log(Level.FINE, "{0} bottomHalf\n", this);
		this.setInfo(this.getName() + ": " + "PID: " + task.getProc().getPid() + " TID: " + task.getTid() + " Event: forked new child PID: "+ child.getProc().getPid() + " Host: " + Manager.host.getName());
		if(this.runFilters(task, child)){
			this.runActions(task, child);
		}else{
			WindowManager.logger.log(Level.FINER, "{0} bottomHalf run filters returned False\n", this);
		}
		
//		child.requestAddForkedObserver(new TaskForkedObserver());
		
        Action action = this.whatActionShouldBeReturned();
        if(action == Action.CONTINUE){
          task.requestUnblock(this);
          child.requestUnblock(this);
        }
	}
	
	public void apply(Task task){
		task.requestAddForkedObserver(this);
	}
	
	public GuiObject getCopy(){
		return new TaskForkedObserver(this);
	}
	
	private boolean runFilters(Task task, Task child){
		if(!this.forkingTaskFilterPoint.filter(task )) return false;
		if(!this.forkedTaskFilterPoint.filter(child)) return false;
		return true;
	}
	
	private void runActions(Task task, Task child){
		WindowManager.logger.log(Level.FINE, "{0} runActions\n", this);
		super.runActions();
		this.forkingTaskActionPoint.runActions(task);
		this.forkedTaskActionPoint.runActions(child);
	}
	
}
