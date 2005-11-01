/*
 * Created on Oct 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import frysk.proc.Action;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskForkedObserver extends ObserverRoot implements TaskObserver.Forked{
	
	public TaskForkedObserver() {
		super("ProcForkObserver", "Fires when a proc forks");
	}

	public TaskForkedObserver(TaskForkedObserver observer) {
		super(observer.getName(), observer.getToolTip());
	}

	public Action updateForked(Task task, Proc child) {
		System.out.println("TaskForkedObserver.updateForked()");
		return Action.CONTINUE;
	}
	
	public void apply(Task task){
		task.requestAddForkedObserver(this);
	}
	
	public ObserverRoot getCopy(){
		return new TaskForkedObserver(this);
	}
	
}
