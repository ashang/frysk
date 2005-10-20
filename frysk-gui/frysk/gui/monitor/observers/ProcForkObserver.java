/*
 * Created on Oct 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor.observers;

import java.util.Observable;

import frysk.proc.Proc;

public class ProcForkObserver extends ObserverRoot {
	
	private Proc expectedParent;
	
	public ProcForkObserver(Proc parent) {
		super("ProcForkObserver", "Fires when a proc forks");
		this.expectedParent = parent;
	}
	
	public void update(Observable o, Object obj) {
		final Proc proc = (Proc) obj;
		if(proc.getParent().getId() != this.expectedParent.getId()){
			return;
		}
		super.update(o, obj);
	}
	
}
