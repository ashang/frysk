package frysk.gui.monitor;

import java.util.Observable;

/**
 * A simple class that sets hasChanged automatically when
 * update is called.
 * */
public class GuiObservable extends Observable {
	public void notifyObservers(Object obj){
		this.setChanged();
		super.notifyObservers(obj);
	}
	
	public void notifyObservers(){
		this.setChanged();
		super.notifyObservers();
	}
}
