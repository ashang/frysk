package frysk.gui.monitor;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;

/**
 * A more sofisticate implementer of Observer.
 * provides name and tooltip strings for gui display perposis.
 * Takes a Runnable object that can be used by instanciaters to 
 * customize the behaviour of the observer.
 * */
public class ObserverRoot implements Observer{

		protected String toolTip;
		protected String name;

		LinkedList runnables;
		Runnable onUpdate;
		
		public ObserverRoot(String name, String toolTip){
			this.toolTip = toolTip;
			this.name = name;
			this.runnables = new LinkedList();
		}
		
		public String getToolTip() {
			return toolTip;
		}

		public String getName() {
			return name;
		}

		public void setToolTip(String toolTip) {
			this.toolTip = toolTip;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		public void update(Observable o, Object obj) {
			ListIterator iter = runnables.listIterator();
			while(iter.hasNext()){
				Runnable runnable = (Runnable) iter.next();
				runnable.run();
			}
		}
		
		public void addRunnable(Runnable runnable){
			this.runnables.add(runnable);
		}
		
	}
	
	
	

