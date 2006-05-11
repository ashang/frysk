
import com.redhat.ftk.EventViewer;
import org.gnu.gdk.EventMask;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.Window;
import org.gnu.gtk.WindowType;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;

public class EVTest {

    EventViewer viewer = null;
    Window window;

    EVTest() {
	window = new Window(WindowType.TOPLEVEL);
	window.setTitle("EventViewer Example");
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

	// create a viewer
	viewer = new EventViewer();

	// initial width of display in seconds
	viewer.setTimebase(10.0);        

	// set up a trace, add as many as you like
	int trace0 = viewer.addTrace ("parent");
	int trace1 = viewer.addTrace ("thread 0");
	int trace2 = viewer.addTrace ("thread 1");

	// set the trace color in rgb, each on a 0 -65535 scale
	viewer.setTraceRGB(trace0, 65535, 0, 0); 
	viewer.setTraceRGB(trace1, 0, 65535, 0); 
	viewer.setTraceRGB(trace2, 0, 0, 65535); 

	// add as many markers as you like. 
	// in the real widget the first param is an enum:
	//    FTK_GLYPH_OPEN_CIRCLE        = 0,
	//    FTK_GLYPH_FILLED_CIRCLE      = 1,
	//    FTK_GLYPH_OPEN_SQUARE        = 2,
	//    FTK_GLYPH_FILLED_SQUARE      = 3,
	// (there will be more types later
	int marker0 = viewer.addMarker(0, "clone");
	int marker1 = viewer.addMarker(1, "event a");
	int marker2 = viewer.addMarker(2, "term");

	// set the marker color in rgb, each on a 0 -65535 scale
	viewer.setMarkerRGB(marker0, 65535, 65535, 0);
	viewer.setMarkerRGB(marker1, 65535, 0, 65535);
	viewer.setMarkerRGB(marker2, 0, 65535, 65535);
	
	window.add(viewer);

	// stuff in events
	viewer.appendEvent(trace0, marker0);
	viewer.appendEvent(trace1, marker1);
	viewer.appendEvent(trace2, marker2);
	
	viewer.addListener(new ExposeListener() {
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
	new EVTest();
	Gtk.main();
    }

}
