package frysk.gui.monitor.observers;

/**
 * Added to observe Exec events.
 * */
public class TaskExecObserver extends ObserverRoot{
	public TaskExecObserver(){
		super("Exec Observer", "Fires everytime this task executes an exec call");
	}
}
	