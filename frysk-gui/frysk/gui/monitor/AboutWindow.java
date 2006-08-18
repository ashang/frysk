package frysk.gui.monitor;

import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.AboutDialog;
import org.gnu.gtk.Button;
import org.gnu.gtk.HButtonBox;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

public class AboutWindow extends AboutDialog implements Saveable {

	public AboutWindow(LibGlade glade){

		super(glade.getWidget("fryskAboutDialog").getHandle());
		this.addListener(new LifeCycleListener() {
			public void lifeCycleEvent(LifeCycleEvent event) {}
			public boolean lifeCycleQuery(LifeCycleEvent event) {
				if (event.isOfType(LifeCycleEvent.Type.DESTROY) || 
						event.isOfType(LifeCycleEvent.Type.DELETE)) {
					hideAll();
				}	
				return true;
			}
		});




		HButtonBox actionArea = getActionArea();
		Widget[] buttons = actionArea.getChildren();
		for(int z=0; z < buttons.length; z++)
		{
			Button dialogButton = (Button) buttons[z];
			if (dialogButton.getLabel().equals("gtk-close"))
			{
				dialogButton.addListener(new ButtonListener() {
			      public void buttonEvent (ButtonEvent event) {
			        if (event.isOfType(ButtonEvent.Type.CLICK)) 
			        	hideAll();
			      }
				});
			}
			
		}

	}
	
	public void save(Preferences prefs) {
		// TODO Auto-generated method stub

	}
	public void load(Preferences prefs) {
		// TODO Auto-generated method stub

	}


}
