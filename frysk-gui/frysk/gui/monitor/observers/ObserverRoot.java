package frysk.gui.monitor.observers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.glib.CustomEvents;

import frysk.gui.monitor.actions.Action;
import frysk.proc.TaskObserver;

/**
 * A more sophisticated implementer of Observer.
 * provides name and tool tip strings for GUI display purposes.
 * Takes Action objects that can be used by clients to customize
 * behaviour. 
 * */
public abstract class ObserverRoot implements TaskObserver, Observer{

		private String name;
		private String toolTip;

		private LinkedList actions;
		private LinkedList runnables;
			
		Runnable onAdded;
		Runnable onDeleted;
		
		private String info;
		
		public ObserverRoot(String name, String toolTip){
			this.toolTip     = toolTip;
			this.name        = name;
			this.actions     = new LinkedList();
			this.info        = new String();
		}
		
		public ObserverRoot(ObserverRoot observer) {
			
			toolTip     = new String(observer.toolTip);
			name        = new String(observer.name);
			
			actions     = new LinkedList(observer.actions);
//			runnables   = new LinkedList(observer.runnables);
			
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
			
		public void added(Throwable e) {
			if(this.onAdded != null) this.onAdded.run();
		}

		public void deleted() {
			if(this.onDeleted != null) this.onDeleted.run();
		}
		
		public void onAdded(Runnable r){
			this.onAdded = r;
		}
		
		public void onDeleted(Runnable r){
			this.onDeleted = r;
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
		
	}
	
	
	

