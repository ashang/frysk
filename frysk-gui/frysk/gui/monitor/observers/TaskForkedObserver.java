/*
 * Created on Oct 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import java.util.Iterator;

import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.actions.ForkedAction;
import frysk.gui.monitor.filters.ProcFilterPoint;
import frysk.gui.monitor.filters.TaskFilterPoint;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskForkedObserver extends TaskObserverRoot implements TaskObserver.Forked{

	ObservableLinkedList forkedActions;
	
	TaskFilterPoint taskFilterPoint;
	ProcFilterPoint procFilterPoint;
	
	public TaskForkedObserver() {
		super("Fork Observer", "Fires when a proc forks");
		
		this.taskFilterPoint = new TaskFilterPoint("Forking Thread", "Thread that performed the fork");
		this.procFilterPoint = new ProcFilterPoint("New Process","Newly forked created process");
		
		this.addFilterPoint(this.taskFilterPoint);
		this.addFilterPoint(this.procFilterPoint);
		
		this.forkedActions = new ObservableLinkedList();
	}

	public TaskForkedObserver(TaskForkedObserver other) {
		super(other);
		
		this.taskFilterPoint = new TaskFilterPoint(other.taskFilterPoint);
		this.procFilterPoint = new ProcFilterPoint(other.procFilterPoint);
		
		//this.addFilterPoint(this.taskFilterPoint); not needed done by parent constructor
		//this.addFilterPoint(this.procFilterPoint);
		
		this.forkedActions = new ObservableLinkedList(); // Dont copy actions
	}

	public Action updateForked(Task task, Task child) {
//		WarnDialog dialog = new WarnDialog("Fork ya'll");
//		dialog.showAll();
//		dialog.run();

		System.out.println("TaskForkedObserver.updateForked() "
				   + child.getTid());
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
			this.runActions();
			this.runForkedActions(task, child);
		}
		child.requestAddForkedObserver(new TaskForkedObserver());
		
		task.requestUnblock(this);
		child.requestUnblock(this);
	}
	
	public void apply(Task task){
		task.requestAddForkedObserver(this);
	}
	
	public TaskObserverRoot getCopy(){
		return new TaskForkedObserver(this);
	}
	
	public void addForkedAction(ForkedAction action){
		this.forkedActions.add(action);
	}
	
	public void runForkedActions(Task task, Task child){
		Iterator iter = this.forkedActions.iterator();
		while (iter.hasNext()) {
			ForkedAction action = (ForkedAction) iter.next();
			action.execute(task, child);
		}
	}
	
	private boolean runFilters(Task task, Task child){
//XXX		if(!this.procFilterPoint.filter(child)) return false;
		if(!this.taskFilterPoint.filter(task )) return false;
		return true;
	}
	
}
