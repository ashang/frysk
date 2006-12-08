import java.io.*;
import java.lang.Character;
import org.gnu.gdk.EventMask;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.Window;
import org.gnu.gtk.WindowType;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gnomevte.Terminal;
import frysk.sys.Pty;

public class VtePtyTest {

    Terminal term = null;
    Window window;

    static {
	System.loadLibrary ("frysk-junit");
	System.loadLibrary ("frysk-imports");
    }


    class readPty implements Runnable {
	String fname;
	FileReader fr;
	FileWriter lfw;
	char[] is = new char[256];
	
	readPty(String fn, FileWriter ifw) {
	    this.fname = fn;
	    this.lfw = ifw;
	}
 
	public void run() {
	    try {
		fr = new FileReader (this.fname);
		while(true) {
		    int ic = fr.read (is, 0, 250);

		    for (int i = 0; i < ic; i++) {
			char cc = is[i];
			is[i] = Character.toUpperCase (cc);
		    }
		    
		    if ((lfw != null) && (0 < ic)) {
			lfw.write (is, 0, ic);
			lfw.flush();
		    }
		}
	    } catch (IOException ioe) {
		System.out.println ("new FileReaader failed: " + ioe);
	    }
	}
    }

    VtePtyTest() {
	FileWriter fw = null;
	final String os = "FILEWRITER output\n";
	
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


	Pty pty = new Pty();
        int master = pty.getFd();
	String name = pty.getName();

	term = new Terminal();
	term.setPty (master);
	term.setDefaultColors();
	term.setSize (80, 6);
	term.feed ("Hi, there!  This is VTE!\r\n");

	window.add(term);
	window.showAll();
	    
	try {
	    fw = new FileWriter (name);
	    fw.write (os, 0, os.length());
	    fw.flush();
	} catch (IOException ioe) {
	    System.out.println ("new FileWriter failed: " + ioe);
	}

	readPty p = new readPty(name, fw);
	new Thread(p).start();
	
    }

    public static void main(String[] args) {
	Gtk.init(args);
	new VtePtyTest();
	Gtk.main();
    }
}
