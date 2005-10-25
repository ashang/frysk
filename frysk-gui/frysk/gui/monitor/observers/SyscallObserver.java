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

public class SyscallObserver extends ObserverRoot implements TaskObserver.Syscall {

	public SyscallObserver() {
		super("Syscall Observer", "Fires when a system call is made.");
	}

	public Action updateSysEnter(Task task, int syscall) {
		// TODO Auto-generated method stub
		return Action.CONTINUE;
	}

	public Action updateSysExit(Task task, int syscall) {
		// TODO Auto-generated method stub
		return Action.CONTINUE;
	}
}
