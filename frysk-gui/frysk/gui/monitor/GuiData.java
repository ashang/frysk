package frysk.gui.monitor;

import java.util.LinkedList;

/**
 * Used to store a pointer to objects in the backend, and extra data that is
 * GUI specific.
 */
public class GuiData {
	
	InfoWidget infoWidget;
	LinkedList observers;
	
	public GuiObservable observerAdded;
	public GuiObservable observerRemoved;
	
	public GuiData(){
		this.infoWidget = null;
		this.observerAdded = new GuiObservable();
		this.observerRemoved = new GuiObservable();
		this.observers = new LinkedList();
	}
	
	public void add(TaskExecObserver observer){

		observer.setRunnable(new Runnable(){
			public void run() {
				System.out.println("GuiData: Recieved taskForkedEvent");
			}
		});

		this.observers.add(observer);
		this.observerAdded.notifyObservers(observer);
	}
	
	public void remove(ObserverRoot observer){
		this.observers.remove(observer);
		this.observerRemoved.notifyObservers(observer);
	}
	
	public LinkedList getObservers(){
		return this.observers;
	}
	
	public void setStatusWidget(InfoWidget widget){
		this.infoWidget = widget;
	}
	
	public InfoWidget getStatusWidget(){
		return this.infoWidget;
	}
	
	public boolean hasStatusWidget(){
		return (this.infoWidget != null);
	}
	
	
}
