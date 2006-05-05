package frysk.gui.monitor.observers;

import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.ProcObserver.Tasks;

public abstract class TaskObserverRoot extends ObserverRoot {

	public TaskObserverRoot(String name, String toolTip) {
		super(name, toolTip);
	}

	public TaskObserverRoot(TaskObserverRoot other) {
		super(other);
	}

	public void apply(Proc proc){

		proc.requestAddTasksObserver(new Tasks() {
		
			public void taskAdded(Task task) {
				apply(task);
			}
			
			public void addFailed(Object observable, Throwable w) {
				throw new RuntimeException("Error occurred while adding observer");
			}
		
			public void existingTask(Task task) {
				apply(task);
			}
		
			public void addedTo(Object observable){}
			public void taskRemoved(Task task){}
			public void deletedFrom(Object observable){}

		});
		
	}
	
	public abstract void apply(Task task);

}
