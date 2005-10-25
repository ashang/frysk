/*
 * Created on Oct 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import frysk.proc.Proc;

public class TaskForkedObserver extends ObserverRoot {
	
	private Proc expectedParent;
	
	public TaskForkedObserver() {
		super("ProcForkObserver", "Fires when a proc forks");
	}
	
}
