package frysk.gui.monitor;

import java.util.LinkedList;

import frysk.gui.common.dialogs.DialogManager;

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
		this.observerAdded = new GuiObservable();
		this.observerRemoved = new GuiObservable();
		this.observers = new LinkedList();
	}
	
	public void add(ObserverRoot observer){
		this.observers.add(observer);
		this.observerAdded.notifyObservers(observer);
	}
	
	public void add(TaskExecObserver observer){
		this.add((ObserverRoot)observer);
		observer.addRunnable(new Runnable(){
			public void run() {
				DialogManager.showWarnDialog("Recieved TaskExec Event !");
			}
		});
	}
	
	public void add(TaskExitingObserver observer) {
		this.add((ObserverRoot)observer);
		observer.addRunnable(new Runnable(){
			public void run() {
				DialogManager.showWarnDialog("Recieved TaskExiting Event !");
			}
		});
	}
	
	public void remove(ObserverRoot observer){
		this.observers.remove(observer);
		this.observerRemoved.notifyObservers(observer);
	}
	
	public LinkedList getObservers(){
		return this.observers;
	}
	
	public void setInfoWidget(InfoWidget widget){
		this.infoWidget = widget;
	}
	
	public InfoWidget getInfoWidget(){
		return this.infoWidget;
	}
	
	public boolean hasWidget(){
		return (this.infoWidget != null);
	}
	
	
}
