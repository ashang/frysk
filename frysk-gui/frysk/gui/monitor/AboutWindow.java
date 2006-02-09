package frysk.gui.monitor;

import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Widget;

public class AboutWindow extends Widget implements Saveable {
	   
		public AboutWindow(LibGlade glade){
			super(glade.getWidget("fryskAboutDialog").getHandle());
	    }

		
		public void save(Preferences prefs) {
			// TODO Auto-generated method stub
			
		}
		public void load(Preferences prefs) {
			// TODO Auto-generated method stub
			
		}
}
