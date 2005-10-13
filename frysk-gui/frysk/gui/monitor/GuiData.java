package frysk.gui.monitor;

import java.util.LinkedList;
import java.util.Observable;

import org.gnu.gtk.Widget;

import frysk.gui.common.dialogs.DialogManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.ObserverRunnable;
import frysk.gui.monitor.observers.TaskExecObserver;
import frysk.gui.monitor.observers.TaskExitingObserver;

/**
 * Used to store a pointer to objects in the backend, and extra data that is
 * GUI specific.
 */
public class GuiData {
	
	Widget widget;
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
		observer.addRunnable(new ObserverRunnable(){
			public void run(Observable o, Object obj) {
				DialogManager.showWarnDialog("Recieved TaskExec Event !");				
			}
		});
	}
	
	public void add(TaskExitingObserver observer) {
		this.add((ObserverRoot)observer);
		observer.addRunnable(new ObserverRunnable(){
			public void run(Observable o, Object obj) {
				DialogManager.showWarnDialog("Recieved TaskExiting Event !");				
			}
		});
	}
	
	public void remove(ObserverRoot observer){
		this.observers.remove(observer);
		this.observerRemoved.notifyObservers(observer);	
	}
	
	public void remove(TaskExecObserver observer){
		remove((ObserverRoot)observer);
		ActionPool.theActionPool.addExecObserver.removeObservers(this);
	}
	
	public void remove(TaskExitingObserver observer){
		remove((ObserverRoot)observer);
		ActionPool.theActionPool.addExitingObserver.removeObservers(this);
	}
	
	public LinkedList getObservers(){
		return this.observers;
	}
	
	public void setWidget(Widget widget){
		if(this.widget != null){
			throw new RuntimeException("Trying to set widget when widget is already set.");
		}
		this.widget = widget;
	}
	
	public Widget getWidget(){
		return this.widget;
	}
	
	public boolean hasWidget(){
		return (this.widget != null);
	}
	
	
}
