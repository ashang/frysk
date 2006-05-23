package frysk.gui.monitor.actions;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.TaskObserverRoot;
import frysk.proc.Task;

public class AddTaskObserverAction extends TaskAction {

	TaskObserverRoot observer;
	
	public AddTaskObserverAction() {
		super("Add observer to", "Add given observer to the given task"); 
		this.observer = null;
	}

	public AddTaskObserverAction(AddTaskObserverAction other) {
		super(other);
		this.observer = other.observer;
	}

	public void execute(Task task) {
		observer.apply(task.getProc());
	}

	public GuiObject getCopy() {
		return new AddTaskObserverAction(this);
	}

	public void setObserver(TaskObserverRoot taskObserver){
		this.observer = taskObserver;
	}

	public boolean setArgument(String argument) {
		TaskObserverRoot observer = ObserverManager.theManager.getObserverByName(argument);
		if(observer != null){
			this.observer = observer;
			return true;
		}
		return false;
	}

	public String getArgument() {
		if(this.observer != null){
			return this.observer.getName();
		}else{
			return "";
		}
	
	}

	public ObservableLinkedList getArgumentCompletionList() {
		return ObserverManager.theManager.getTaskObservers();
	}
	
}
