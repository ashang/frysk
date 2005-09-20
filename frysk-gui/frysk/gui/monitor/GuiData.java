package frysk.gui.monitor;

import java.util.LinkedList;

/**
 * Used to store a pointer to objects in the backend, and extra data that is
 * GUI specific.
 */
public class GuiData {
	
	StatusWidget statusWidget;
	LinkedList observers;
	
	public GuiData(){
		this.statusWidget = null;
		this.observers = new LinkedList();
	}
	
	public void add(TaskExecObserver observer){
		this.observers.add(observer);
		observer.setRunnable(new Runnable(){

			public void run() {
				System.out.println("GuiData: Recieved taskForkedEvent");
			}
			
		});
		
	}
	
	public LinkedList getObservers(){
		return this.observers;
	}
	
	public void setStatusWidget(StatusWidget widget){
		this.statusWidget = widget;
	}
	
	public StatusWidget getStatusWidget(){
		return this.statusWidget;
	}
	
	public boolean hasStatusWidget(){
		return (this.statusWidget != null);
	}
	
	
}
