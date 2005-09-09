package frysk.gui.monitor;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;


/**
 * @author sami wagiaalla
 * Singleton; one window manager.
 * Provies an instance to our major components so that they can
 * be manipulated from any where in the gui code.
 * Add your windows here.
 * */

public class WindowManager implements Saveable{

	public static WindowManager theManager = new WindowManager();
	
	/**
	 * Public instances of the windows
	 * {*/
	public ProcpopWindow procpopWindow;
	public LogWindow logWindow;
	/**}*/
	
	public WindowManager(){
		
	}
	
	public void initWindows(LibGlade glade) throws IOException{
		this.procpopWindow = new ProcpopWindow(glade);
		this.procpopWindow.showAll();
		
		this.logWindow = new LogWindow(glade);
		this.logWindow.showAll();
	}

	public void save(Preferences prefs) {
		procpopWindow.save(Preferences.userRoot().node(prefs.absolutePath() + "/procpopWindow"));
		logWindow.save(Preferences.userRoot().node(prefs.absolutePath() + "/logWindow"));
	}

	public void load(Preferences prefs) {
		procpopWindow.load(Preferences.userRoot().node(prefs.absolutePath() + "/procpopWindow"));
		logWindow.load(Preferences.userRoot().node(prefs.absolutePath() + "/logWindow"));
	}
}
