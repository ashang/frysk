package frysk.gui.monitor.observers;

import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.ProcObserver.Tasks;

public abstract class TaskObserverRoot extends ObserverRoot {

	public TaskObserverRoot(String name, String toolTip) {
		super(name, toolTip);
	}

	public TaskObserverRoot(TaskObserverRoot observer) {
		super(observer);
	}

	public void apply(Proc proc){
		proc.requestAddTasksObserver(new Tasks() {
		
			public void taskAdded(Task task) {
				apply(task);
			}
			
			public void addFailed(Object observable, Throwable w) {
				// TODO Auto-generated method stub
				throw new RuntimeException("You fogot to implement this method :D ");
			}
		
			public void existingTask(Task task) {
				apply(task);
			}
		
			public void addedTo(Object observable){}
			public void taskRemoved(Task task){}
			public void deletedFrom(Object observable){}

		});
		
		onAdded.run();
	}
	
	public abstract void apply(Task task);
	
	public abstract TaskObserverRoot getCopy();
}
