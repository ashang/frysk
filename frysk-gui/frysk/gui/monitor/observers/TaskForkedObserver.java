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
import frysk.proc.Proc;
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

	public TaskForkedObserver(TaskForkedObserver observer) {
		super(observer);
	}

	public Action updateForked(Task task, Task child) {
		System.out.println("TaskForkedObserver.updateForked() "
				   + child.getTid());
		final Task myTask = task;
		final Proc myChild = child.getProc ();
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				bottomHalf(myTask, myChild);
			}
		});
		return Action.BLOCK;
	}
	
	private void bottomHalf(Task task, Proc child){
		if(this.runFilters(task, child)){
			this.runActions();
			this.runForkedActions(task, child);
		}
		task.requestUnblock(this);
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
	
	public void runForkedActions(Task task, Proc child){
		Iterator iter = this.forkedActions.iterator();
		while (iter.hasNext()) {
			ForkedAction action = (ForkedAction) iter.next();
			action.execute(task, child);
		}
	}
	
	private boolean runFilters(Task task, Proc child){
		if(!this.procFilterPoint.filter(child)) return false;
		if(!this.taskFilterPoint.filter(task )) return false;
		return true;
	}
	
}
