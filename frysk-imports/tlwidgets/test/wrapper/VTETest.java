import org.gnu.gdk.EventMask;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.Window;
import org.gnu.gtk.WindowType;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gnomevte.Terminal;

public class VTETest {

    Terminal vte = null;
    Window window;
    String[] cmdargs = new String[1];

    VTETest() {
	window = new Window(WindowType.TOPLEVEL);
	window.setTitle("Vte Example");
	window.addListener(new LifeCycleListener() {
		public void lifeCycleEvent(LifeCycleEvent event) {
		}

		public boolean lifeCycleQuery(LifeCycleEvent event) {
		    if (event.isOfType(LifeCycleEvent.Type.DESTROY) ||
			event.isOfType(LifeCycleEvent.Type.DELETE)) {
			Gtk.mainQuit();
		    }
		    return true;
		}
	    });

	cmdargs[0] = "-l";
	vte = new Terminal("/bin/bash", cmdargs, ".");
	vte.setDefaultColors();
	vte.setSize (80, 6);
	vte.feed ("Hi, there!  This is VTE!\r\n");
	
	/******* 
	vte = new Terminal("/bin/sh", null, null);
	vte.setDefaultColors();
	**********/

	window.add(vte);

	
	window.showAll();
    }

    public static void main(String[] args) {
	Gtk.init(args);
	new VTETest();
	Gtk.main();
    }

}
