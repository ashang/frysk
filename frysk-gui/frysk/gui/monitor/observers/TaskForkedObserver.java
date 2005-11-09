/*
 * Created on Oct 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import java.util.LinkedList;

import frysk.proc.Action;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskForkedObserver extends TaskObserverRoot implements TaskObserver.Forked{

	LinkedList forkedActions;
	LinkedList forkedFilters;
	
	public TaskForkedObserver() {
		super("Fork Observer", "Fires when a proc forks");
	}

	public TaskForkedObserver(TaskForkedObserver observer) {
		super(observer.name, observer.toolTip);
	}

	public Action updateForked(Task task, Proc child) {
		System.out.println("TaskForkedObserver.updateForked()");
		final Task myTask = task;
		final Proc myChild = child;
		org.gnu.glib.CustomEvents.addEvent(new Runnable(){
			public void run() {
				bottomHalf(myTask, myChild);
			}
		});
		return Action.BLOCK;
	}
	
	private void bottomHalf(Task task, Proc child){
		task.requestUnblock(this);
	}
	
	public void apply(Task task){
		task.requestAddForkedObserver(this);
	}
	
	public TaskObserverRoot getCopy(){
		return new TaskForkedObserver(this);
	}
	
	public void addForkedAction(TaskObserver.Forked forked){
		this.forkedActions.add(forked);
	}
	
	public void addForkedFilter(TaskObserver.Forked forked){
		this.forkedFilters.add(forked);
		
	}
	
//	private void runForkedAction(){
//		
//	}
		
}
