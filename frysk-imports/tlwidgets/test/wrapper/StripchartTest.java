//package stripchartTest;

import com.redhat.ftk.Stripchart;
import org.gnu.gdk.EventMask;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.Window;
import org.gnu.gtk.WindowType;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;

public class StripchartTest {

    Stripchart area = null;
    Window window;

    StripchartTest() {
	window = new Window(WindowType.TOPLEVEL);
	window.setTitle("Stripchart Example");
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
	area = new Stripchart();
	area.resize (500, 150);
	//area.setEventTitle(1, "Knife");
	//area.setEventRGB(1, 65535, 65535, 0); /* red + green = yellow */
	area.createEvent("knife", 65535, 65535, 0); /* red + green = yellow */
	area.createEvent("fork",  65535, 0, 65535); /* red + green = yellow */
	area.setUpdate (1111);
	area.setRange (60000);
	area.appendEvent (0);
	area.appendEvent (1);

	window.add(area);
	area.addListener(new ExposeListener() {
	public boolean exposeEvent(ExposeEvent event) {
	    System.out.println("Expose event: " + window.getWindow());
	//	window.getWindow().drawPoint(20, 20);
	    return false;
	}
	});
	window.showAll();
	System.out.println("Should be printed before expose event");
    }

    public static void main(String[] args) {
	Gtk.init(args);
	new StripchartTest();
	Gtk.main();
    }

}
