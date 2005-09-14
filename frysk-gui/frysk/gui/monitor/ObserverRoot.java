package frysk.gui.monitor;

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

		Runnable onUpdate;
		
		public ObserverRoot(String name, String toolTip){
			this.toolTip = toolTip;
			this.name = name;
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
			this.onUpdate.run();
		}
		
		public void setRunnable(Runnable runnable){
			this.onUpdate = runnable;
		}
	}
	
	
	

