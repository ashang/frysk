/*
 * Created on Oct 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskSyscallObserver extends TaskObserverRoot implements TaskObserver.Syscall {

	public TaskSyscallObserver() {
		super("Syscall Observer", "Fires when a system call is made.");
	}

	public TaskSyscallObserver(TaskSyscallObserver observer) {
		super(observer.getName(), observer.getToolTip());
	}

	public Action updateSyscallEnter(Task task) {
		// TODO Auto-generated method stub
		System.out.println("SyscallObserver.updateSyscallEnter()");
		return Action.CONTINUE;
	}

	public Action updateSyscallExit(Task task) {
		// TODO Auto-generated method stub
		System.out.println("SyscallObserver.updateSyscallExit()");
		return Action.CONTINUE;
	}

	public void apply(Task task){
		task.requestAddSyscallObserver(this);
	}
	
	public TaskObserverRoot getCopy(){
		return new TaskSyscallObserver(this);
	}
	
}
