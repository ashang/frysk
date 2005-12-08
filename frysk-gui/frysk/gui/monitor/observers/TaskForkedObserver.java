/*
 * Created on Oct 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.actions.TaskActionPoint;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskForkedObserver extends TaskObserverRoot implements TaskObserver.Forked{

	ObservableLinkedList forkedActions;
	
	public TaskFilterPoint forkingTaskFilterPoint;
	public TaskFilterPoint forkedTaskFilterPoint;

	public TaskActionPoint forkingTaskActionPoint;
	
	public TaskActionPoint forkedTaskActionPoint;
	
	public TaskForkedObserver() {
		super("Fork Observer", "Fires when a proc forks");
		
		this.forkingTaskFilterPoint = new TaskFilterPoint("Forking Thread", "Thread that performed the fork");
		this.forkedTaskFilterPoint = new TaskFilterPoint("Forked Thread","Main thread of newly forked process");
		
		this.addFilterPoint(this.forkingTaskFilterPoint);
		this.addFilterPoint(this.forkedTaskFilterPoint);
		
		this.forkingTaskActionPoint = new TaskActionPoint("Forking Thread", "Thread that performed the fork");
		this.forkedTaskActionPoint = new TaskActionPoint("Forked Thread","Main thread of newly forked process");
		
		this.addActionPoint(this.forkingTaskActionPoint);
		this.addActionPoint(this.forkedTaskActionPoint);
		
	}

	public TaskForkedObserver(TaskForkedObserver other) {
		super(other);
		
		this.forkingTaskFilterPoint = new TaskFilterPoint(other.forkingTaskFilterPoint);
		this.forkedTaskFilterPoint = new TaskFilterPoint(other.forkedTaskFilterPoint);

//		this.addFilterPoint(this.forkingTaskFilterPoint); not needed done by parent const.
//		this.addFilterPoint(this.forkedTaskFilterPoint);

		this.forkingTaskActionPoint = new TaskActionPoint("Forking Thread", "Thread that performed the fork");
		this.forkedTaskActionPoint  = new TaskActionPoint("Forked Thread","Main thread of newly forked process");
		
//		this.addActionPoint(this.forkingTaskActionPoint); not needed done by parent const.
//		this.addActionPoint(this.forkedTaskActionPoint);

	}

	public Action updateForked(Task task, Task child) {
//		WarnDialog dialog = new WarnDialog("Fork ya'll");
//		dialog.showAll();
//		dialog.run();

		System.out.println("TaskForkedObserver.updateForked() " + child.getTid());
		final Task myTask = task;
		final Task myChild = child;
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				bottomHalf(myTask, myChild);
			}
		});
		return Action.BLOCK;
	}
	
	private void bottomHalf(Task task, Task child){
		if(this.runFilters(task, child)){
			this.runActions(task, child);
		}
		//child.requestAddForkedObserver(new TaskForkedObserver());
		
		task.requestUnblock(this);
		child.requestUnblock(this);
	}
	
	public void apply(Task task){
		task.requestAddForkedObserver(this);
	}
	
	public TaskObserverRoot getCopy(){
		return new TaskForkedObserver(this);
	}
	
	private boolean runFilters(Task task, Task child){
		if(!this.forkingTaskFilterPoint.filter(task )) return false;
		if(!this.forkedTaskFilterPoint.filter(child)) return false;
		return true;
	}
	
	private void runActions(Task task, Task child){
		super.runActions();
		this.forkingTaskActionPoint.runActions(task);
		this.forkedTaskActionPoint.runActions(child);
	}
	
}
