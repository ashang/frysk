/*
 * Created on Oct 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class TaskCloneObserver extends ObserverRoot implements TaskObserver.Cloned {

	public TaskCloneObserver(){
		super("ProcCloneObserver", "Fires when a proc calls clone");
	}

	public Action updateCloned(Task task, Task clone) {
		// TODO Auto-generated method stub
		return Action.CONTINUE;
	}
}
