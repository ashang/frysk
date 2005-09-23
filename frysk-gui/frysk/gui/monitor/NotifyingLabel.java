/**
 * A label that changes color when its update method is called.
 * once the Lable is selected it changes back to black.
 * */
package frysk.gui.monitor;

import java.util.Observable;
import java.util.Observer;

import org.gnu.gdk.Color;
import org.gnu.gtk.Label;
import org.gnu.gtk.StateType;

public class NotifyingLabel extends Label implements Observer {

	public NotifyingLabel(String label) {
		super(label);

//		this.addListener(new ExposeListener(){
//			public boolean exposeEvent(ExposeEvent arg0) {
//				setForegroundColor(StateType.NORMAL, Color.BLACK);
//				return false;
//			}
//		});
	}

	public void update(Observable arg0, Object arg1) {
		this.setForegroundColor(StateType.NORMAL, Color.RED);
	}
}
