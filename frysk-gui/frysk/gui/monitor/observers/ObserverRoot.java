package frysk.gui.monitor.observers;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.glib.CustomEvents;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.actions.Action;
import frysk.gui.monitor.filters.FilterPoint;
import frysk.proc.TaskObserver;

/**
 * A more sophisticated implementer of Observer.
 * provides name and tool tip strings for GUI display purposes.
 * Takes Action objects that can be used by clients to customize
 * behaviour. 
 * */
public class ObserverRoot extends GuiObject implements TaskObserver, Observer{

		private ObservableLinkedList actions;
		private ObservableLinkedList runnables;
			
		Runnable onAdded;
		Runnable onDeleted;
		
		private String info;
		
		private ObservableLinkedList filterPoints;
		
		private final String baseName;
		
		public ObserverRoot(String name, String toolTip){
			super(name, toolTip);
			this.actions      = new ObservableLinkedList();
			this.info         = new String();
			this.filterPoints = new ObservableLinkedList();			
			this.baseName     = name;
		}
		
		public ObserverRoot(ObserverRoot other) {
			super(other);
			this.actions      = new ObservableLinkedList(other.actions);
			this.info         = new String(other.info);
			this.filterPoints = new ObservableLinkedList(other.filterPoints);			
			this.baseName     = other.baseName;
		}

		public void update(Observable o, Object obj) {
			final Observable myObservable = o;
			final Object     myObj = obj;
			
			CustomEvents.addEvent(new Runnable(){
				public void run() {
					ListIterator iter = actions.listIterator();
					while(iter.hasNext()){
						ObserverRunnable runnable = (ObserverRunnable) iter.next();
						runnable.run(myObservable, myObj);
					}
				}
			});
		}
		
		/**
		 * Add and action to be performed when this observers
		 * update function is called.
		 * */
		public void addAction(Action action){
			System.out.println("+Action: " + action.getName());
			System.out.println("+Action: " + action.getToolTip());
			this.actions.add(action);
		}
		
		/**
		 * Add and action to be performed when this observers
		 * update function is called.
		 * */
		public void addRunnable(ObserverRunnable action){
			this.runnables.add(action);
		}
			
		public void addedTo (Object o) {
			if(this.onAdded != null) this.onAdded.run();
		}

                public void addFailed (Object o, Throwable w) {
                    	throw new RuntimeException (w);
                }

		public void deletedFrom (Object o) {
			if(this.onDeleted != null) this.onDeleted.run();
		}
		
		public void onAdded(Runnable r){
			this.onAdded = r;
		}
		
		public void onDeleted(Runnable r){
			this.onDeleted = r;
		}

		/**
		 * This string is set by the observer during its update call.
		 * It can be useful for clients interested in printing general
		 * information about a particular observer+event.
		 * @deprecated this function is only there because of theoretical
		 * need for it. it will be removed if it is provent to be needed
		 * */
		public String getInfo() {
			return info;
		}
	
		protected void runActions(){
			Iterator iter = this.actions.iterator();
			while(iter.hasNext()){
				Action action = (Action)iter.next();
				action.execute();
			}
		}
		
		public ObservableLinkedList getFilterPoints(){
			return this.filterPoints;
		}
		
		protected void addFilterPoint(FilterPoint filterPoint){
			this.filterPoints.add(filterPoint);
		}

		public String getBaseName() {
			return baseName;
		}
		
	}
