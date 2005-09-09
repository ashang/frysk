package frysk.gui;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import org.gnu.glade.GladeXMLException;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.Image;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;

import frysk.gui.monitor.FryskErrorFileHandler;
import frysk.gui.monitor.Saveable;
import frysk.gui.monitor.TrayIcon;
import frysk.gui.monitor.WindowManager;
import frysk.proc.Manager;

public class FryskGui implements LifeCycleListener, Saveable{
	
	LibGlade glade;
	private static Logger errorLogFile = null;
	
	private static final String SETTINGSFILE = ".settings";
	private static final String GLADE_FILE = "procpop.glade";
	private static final String GLADE_DEV_PATH = "frysk/gui/glade/";
	private static final String GLADE_PKG_PATH = "glade/"; 
	public static final String ERROR_LOG_ID = "frysk.gui.errorlog";

	
	static {
		System.loadLibrary("EggTrayIcon");
    }
	
	
	
	FryskGui() throws GladeXMLException, FileNotFoundException {
		
		/*
		 * The location of the glade file may need to be modified here,
		 * depending on where the program is being run from. If the directory
		 * that the src directory is in is used as the root, this should work
		 * without modification
		 */
		
		try {
			glade = new LibGlade(GLADE_DEV_PATH + GLADE_FILE, this);
		} catch (FileNotFoundException missingFile) {
			try {
				glade = new LibGlade(GLADE_PKG_PATH + GLADE_FILE, this);
			} catch (FileNotFoundException missingFile2) {
				throw missingFile2;

			} catch (GladeXMLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (GladeXMLException malformedXML) {
			throw malformedXML;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			WindowManager.theManager.initWindows(glade);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	
	public static void main(String[] args) {

		Gtk.init(args);
		
		//-------------------GUInea pigs-------------------
		// System.out.println("infLoop PID : " + SysUtils.infLoop ());
		// System.out.println("infThreadLoop PID : " + SysUtils.infThreadLoop (2));
		//-------------------------------------------------

		TrayIcon trayIcon;
		FryskGui procpop = null;
		Preferences prefs = null;
		
		
		setupErrorLogging();


		try {
			procpop = new FryskGui();
		} catch (GladeXMLException e1) {
			errorLogFile.log(Level.SEVERE,"procpop.glade XML is badly formed",e1);
			System.exit(1);
		} catch (FileNotFoundException e1) {
			errorLogFile.log(Level.SEVERE,"ProcPop glade XML file not found",e1);
			System.exit(1);
		} catch (IOException e1) {
			errorLogFile.log(Level.SEVERE,"IOException: ",e1);
			System.exit(1);
		}
		
		prefs = importPreferences(SETTINGSFILE);
		
		trayIcon = new TrayIcon("Accudog", "", 
				new Image("gui/images/accudog.jpg"));

		trayIcon.setMenuButton(TrayIcon.BUTTON_3);
		trayIcon.setWindowButton(TrayIcon.BUTTON_1);
		trayIcon.addPopupWindow(WindowManager.theManager.procpopWindow);
		
		// right click menu
		Menu popupMenu = new Menu();
		MenuItem quitItem = new MenuItem("Quit", false);
		quitItem.addListener(new MenuItemListener() {
			public void menuItemEvent(MenuItemEvent arg0) {
				Gtk.mainQuit();
			}
		});
		popupMenu.add(quitItem);

		trayIcon.setPopupMenu(popupMenu);
		
		Thread backendStarter = new Thread(new Runnable(){
	         public void run() {
				 Manager.eventLoop.run();
	         }
	    });
		backendStarter.start();
		
		procpop.load(prefs);
		
		Gtk.main();
		
		Manager.eventLoop.requestStop();
		procpop.save(prefs);
		
		try {
	        // Export the node to a file
	        prefs.exportSubtree(new FileOutputStream(SETTINGSFILE));
	    } catch (Exception e) {
	    	errorLogFile.log(Level.SEVERE,"Errors exporting preferences",e);

	    }

	}
	
	private static FileHandler buildHandler() {
	     FileHandler handler = null;
	        SimpleDateFormat format = 
		            new SimpleDateFormat("yyyyMMdd-hhmmss");
	        String date_handle = null;
	        date_handle = "frysk_gui_error_log_"+format.format(new Date()).toString();
			
	        try {

				handler = new FryskErrorFileHandler(date_handle,true);
				
			
	    	} catch (Exception e) {
				e.printStackTrace();
			}
	    	
	    	return handler;
			
	}

	private static void setupErrorLogging() {
	    // Get a logger; the logger is automatically created if
        // it doesn't already exist

   
	        errorLogFile = Logger.getLogger(ERROR_LOG_ID);
	        errorLogFile.addHandler(buildHandler());
	
        // Add to the desired logger
	}
	
	private static  Preferences importPreferences(String location) {
		InputStream is = null;
		Preferences prefs = null;
		
        try {
			is = new BufferedInputStream(new FileInputStream(location));
	        Preferences.importPreferences(is);
		} catch (FileNotFoundException e1) {
			errorLogFile.log(Level.WARNING, location + " not found. Will be created on program exit", e1);
		} catch (IOException e) {
			errorLogFile.log(Level.SEVERE, location + " io error", e);
		} catch (InvalidPreferencesFormatException e) {
			errorLogFile.log(Level.SEVERE, location + " Invalid Format", e);
		}
		
		prefs = Preferences.userRoot();
		return prefs;
		
	}
	public void lifeCycleEvent(LifeCycleEvent arg0) {
		Gtk.mainQuit();
	}

	public boolean lifeCycleQuery(LifeCycleEvent arg0) {
		Gtk.mainQuit();
		return false;
	}

	public void save(Preferences prefs) {
		WindowManager.theManager.save(Preferences.userRoot().node(prefs.absolutePath() + "theManager"));
	}

	public void load(Preferences prefs) {
		WindowManager.theManager.load(Preferences.userRoot().node(prefs.absolutePath() + "theManager"));		
	}
}
