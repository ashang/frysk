/**
 * Used to store a pointer to the Proc object
 * and any data that is relates to the process but is gui specific.
 * Used to pass data to ActionPool Actions.
 * Actions also manipulate data stored in here
 * to keep it up to date.
 * for example the Attach action will set proc from null
 * to point to the Proc object returned by the backend
 * attach function.
 */
package frysk.gui.monitor;

import frysk.proc.Proc;

public class ProcData {
	private Proc proc;
	
	ProcData(Proc proc){
		this.proc = proc;
	}

	public void setProc(Proc proc) {
		this.proc = proc;
	}

	public Proc getProc() {
		return proc;
	}
	
}
