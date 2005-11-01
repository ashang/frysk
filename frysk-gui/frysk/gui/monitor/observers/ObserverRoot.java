package frysk.gui.monitor.observers;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.glib.CustomEvents;

import frysk.proc.Task;
import frysk.proc.TaskObserver;
/**
 * A more sofisticate implementer of Observer.
 * provides name and tooltip strings for gui display perposis.
 * Takes an Runnable object that can be used by instanciaters to 
 * customize the behaviour of the observer.
 * */
public class ObserverRoot implements TaskObserver, Observer{

		protected String toolTip;
		protected String name;

		TaskObserver bouncee;
		LinkedList runnables;
			
		Runnable onAdded;
		Runnable onDeleted;
		
		public ObserverRoot(String name, String toolTip){
			this.toolTip = toolTip;
			this.name = name;
			this.runnables = new LinkedList();
		}
		
//		public ObserverRoot(ObserverRoot root) {
//			throw new RuntimeException("This should not happen");
//		}

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
			final Observable myObservable = o;
			final Object     myObj = obj;
			
			CustomEvents.addEvent(new Runnable(){
				public void run() {
					ListIterator iter = runnables.listIterator();
					while(iter.hasNext()){
						ObserverRunnable runnable = (ObserverRunnable) iter.next();
						runnable.run(myObservable, myObj);
					}
				}
			});
		}
		
		public void setBouncee(TaskObserver observer){
			bouncee = observer;
		}
		
		public void addRunnable(ObserverRunnable runnable){
			this.runnables.add(runnable);
		}
			
		public void added(Throwable e) {
			if(this.bouncee != null) this.bouncee.added(e);
			if(this.onAdded != null) this.onAdded.run();
		}

		public void deleted() {
			if(this.bouncee != null) this.bouncee.deleted();
			if(this.onDeleted != null) this.onDeleted.run();
		}
		
		public void onAdded(Runnable r){
			this.onAdded = r;
		}
		
		public void onDeleted(Runnable r){
			this.onDeleted = r;
		}
		
		
		/**
		 * apply this observer to the given task.
		 * */
		public ObserverRoot getCopy(){
			throw new RuntimeException("This function should have been overwritten by a child");
		}
		
		/**
		 * apply this observer to the given task.
		 * XXX: this should be moved to appropriet child
		 * */
		public void apply(Task task){
			throw new RuntimeException("Not implemented");
		}
		
	}
	
	
	

