/*
 * Created on 5-Jul-05
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextView;
import org.gnu.gtk.Window;

import frysk.proc.Manager;
import frysk.proc.Proc;

/**
 * @author sami wagiaalla
 * Generic log window, just prints out events it recieves
 * */
public class LogWindow extends Window implements Observer, Saveable{
	
	public TextView logTextView;
	
	public LogWindow(LibGlade glade){
		super(((Window)glade.getWidget("logWindow")).getHandle());
		this.logTextView = (TextView) glade.getWidget("logTextView");
	}
	
	static int count = 0;
	public void update(Observable observable, Object obj) {
		Proc proc = (Proc) obj;
		
		TextBuffer tb = this.logTextView.getBuffer();
		tb.insertText("event "+(count++)+" : ");
		
		if(observable == Manager.procDiscovered){
			//FIXME: tb.insertText("Attached to process " + proc.getPid() );
			FIXME: tb.insertText("Attached to process " );
		}
		
		tb.insertText("\n");
		
	}
	
	public void save(Preferences prefs) {
		// TODO Auto-generated method stub
		
	}
	public void load(Preferences prefs) {
		// TODO Auto-generated method stub
		
	}
}
