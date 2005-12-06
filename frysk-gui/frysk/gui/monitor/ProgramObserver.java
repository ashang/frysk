package frysk.gui.monitor;

import java.util.ArrayList;

import frysk.gui.common.dialogs.WarnDialog;
import frysk.gui.monitor.actions.TaskAction;
import frysk.gui.monitor.observers.TaskExecObserver;
import frysk.gui.monitor.observers.TaskForkedObserver;
import frysk.proc.Proc;
import frysk.proc.Task;

public class ProgramObserver {

	ArrayList observerList;
	
	public ProgramObserver(String executable, ArrayList observerList) {
		this.observerList = observerList;
		

	}

	public void apply(Proc proc) {
		final TaskForkedObserver forkedObserver = new TaskForkedObserver(); 
		final TaskExecObserver   execObserver = new TaskExecObserver();
		
		final TaskAction myTaskAction = new TaskAction("", "") {
			public void execute(Task task) {
				WarnDialog dialog = new WarnDialog("Fork ya'll");
				dialog.showAll();
				dialog.run();

				Proc newProc = task.getProc();
				TaskForkedObserver newForkedObserver = (TaskForkedObserver) forkedObserver.getCopy();
				TaskExecObserver   newExecObserver   = (TaskExecObserver) execObserver.getCopy();
				newForkedObserver.forkedTaskActionPoint.addTaskAction(this);
				
				newForkedObserver.apply(newProc);
				newExecObserver.apply(newProc);
			}
		};
		
		forkedObserver.forkedTaskActionPoint.addTaskAction(myTaskAction);
		
		forkedObserver.apply(proc);
		execObserver.apply(proc);
	}
}