package frysk.gui.monitor.observers;

import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.glib.CustomEvents;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.actions.ActionPoint;
import frysk.gui.monitor.actions.GenericActionPoint;
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
		private ObservableLinkedList actionPoints;
		
		private final String baseName;
		
		public GenericActionPoint genericActionPoint;
		
		public ObserverRoot(String name, String toolTip){
			super(name, toolTip);
			
			this.actions      = new ObservableLinkedList();
			this.info         = new String();
			this.filterPoints = new ObservableLinkedList();			
			this.actionPoints = new ObservableLinkedList();			
			this.baseName     = name;			
			
			this.genericActionPoint = new GenericActionPoint("Generic Actions", "Actions that dont take any arguments" );
			this.addActionPoint(genericActionPoint);

		}
		
		public ObserverRoot(ObserverRoot other) {
			super(other);

			this.actions      = new ObservableLinkedList(other.actions);
			this.info         = new String(other.info);
			this.filterPoints = new ObservableLinkedList(other.filterPoints);			
			this.actionPoints = new ObservableLinkedList(other.actionPoints);			
			this.baseName     = other.baseName;
			
			this.genericActionPoint = new GenericActionPoint(other.genericActionPoint);
//			this.addActionPoint(genericActionPoint);

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
		
//		/**
//		 * Add and action to be performed when this observers
//		 * update function is called.
//		 * */
//		public void addAction(Action action){
//			this.genericActionPoint.addAction(action);
//		}
//		
		/**
		 * Add and action to be performed when this observers
		 * update function is called.
		 * */
		public void addRunnable(ObserverRunnable action){
			this.runnables.add(action);
		}
			
		public void addedTo (Object o) {
			if(this.onAdded != null){
				CustomEvents.addEvent(this.onAdded);
			}
		}

		public void deletedFrom (Object o) {
			if(this.onDeleted != null){
				CustomEvents.addEvent(this.onDeleted);
			}
		}

		public void addFailed (Object o, Throwable w) {
			throw new RuntimeException (w);
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
			this.genericActionPoint.runActions();
		}
		
		public ObservableLinkedList getFilterPoints(){
			return this.filterPoints;
		}
		
		public ObservableLinkedList getActionPoints(){
			return this.actionPoints;
		}
		
		protected void addFilterPoint(FilterPoint filterPoint){
			this.filterPoints.add(filterPoint);
		}
		
		protected void addActionPoint(ActionPoint actionPoint){
			this.actionPoints.add(actionPoint);
		}

		public String getBaseName() {
			return baseName;
		}
		
	}
