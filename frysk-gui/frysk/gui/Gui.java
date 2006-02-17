// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
// 
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

package frysk.gui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import org.gnu.glade.GladeXMLException;
import org.gnu.glade.LibGlade;
import org.gnu.glib.CustomEvents;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;

import frysk.Config;
import frysk.event.TimerEvent;
import frysk.gui.common.IconManager;
import frysk.gui.common.Messages;
import frysk.gui.common.dialogs.DialogManager;
import frysk.gui.monitor.ConsoleWindow;
import frysk.gui.monitor.FryskErrorFileHandler;
import frysk.gui.monitor.Saveable;
import frysk.gui.monitor.TrayIcon;
import frysk.gui.monitor.WindowManager;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.srcwin.SourceWindowFactory;
import frysk.proc.Manager;

public class Gui
    implements LifeCycleListener, Saveable
{
    LibGlade glade;
    
    private static Logger errorLogFile = null;
    private static final String SETTINGSFILE = ".settings";
    private static final String GLADE_FILE = "procpop.glade";

    public static final String ERROR_LOG_ID = "frysk.gui.errorlog";

    Gui (String[] glade_dirs)
	throws GladeXMLException, FileNotFoundException, IOException
    {
	// The location of the glade file may need to be modified
	// here, depending on where the program is being run from. If
	// the directory that the src directory is in is used as the
	// root, this should work without modification
	String searchPath = new String();
	for (int i = 0; i < glade_dirs.length; i++) {
	    try {// command line glade_dir
		glade = new LibGlade (glade_dirs[i] + GLADE_FILE, this);
	    }
	    catch (FileNotFoundException missingFile) {
		searchPath += glade_dirs[i] + "\n";
		if(i == glade_dirs.length -1){
		    throw new FileNotFoundException ("Glade file not found in path " + searchPath); //$NON-NLS-1$
		}else{
		    continue;
		}
	    }
	    break;
	}

	try {
	    WindowManager.theManager.initWindows (glade);
	} catch (IOException e) {
	    throw e;
	}
    }

    private static FileHandler buildHandler ()
    {
	FileHandler handler = null;
	File log_dir = new File (Config.FRYSK_DIR + "logs" + "/");

	if (!log_dir.exists())
	    log_dir.mkdirs();

	try {
	    handler = new FryskErrorFileHandler(log_dir.getAbsolutePath()
						+ "/" + "frysk_monitor_error.log", true);
	}
	catch (Exception e) {
	    e.printStackTrace();
	}

	return handler;
    }

    private static void setupErrorLogging ()
    {
	// Get a logger; the logger is automatically created if it
	// doesn't already exist
	errorLogFile = Logger.getLogger(ERROR_LOG_ID);
	errorLogFile.setUseParentHandlers(false);
	errorLogFile.addHandler(buildHandler());
    }

    private static Preferences importPreferences (String location)
    {
	InputStream is = null;
	Preferences prefs = null;

	try {
	    is = new BufferedInputStream(new FileInputStream(location));
	    Preferences.importPreferences(is);
	} catch (FileNotFoundException e1) {
	    errorLogFile.log(Level.WARNING, location
			     + " not found. Will be created on program exit", e1); //$NON-NLS-1$
	} catch (IOException e) {
	    errorLogFile.log(Level.SEVERE, location + " io error", e); //$NON-NLS-1$
	} catch (InvalidPreferencesFormatException e) {
	    errorLogFile.log(Level.SEVERE, location + " Invalid Format", e); //$NON-NLS-1$
	}

	prefs = Preferences.userRoot();
	return prefs;
    }

    public void lifeCycleEvent (LifeCycleEvent arg0)
    {
	Gtk.mainQuit();
    }

    public boolean lifeCycleQuery (LifeCycleEvent arg0)
    {
	Gtk.mainQuit();
	return false;
    }

    public void save (Preferences prefs)
    {
	WindowManager.theManager.save(Preferences.userRoot().node(prefs.absolutePath() + "theManager"));
    }

    public void load (Preferences prefs)
    {
	WindowManager.theManager.load(Preferences.userRoot().node(prefs.absolutePath() + "theManager"));
    }

    public static void gui (String[] args, String[] glade_dirs,
			    String[] imagePaths, 
			    String[] messagePaths,
			    String[] testfilePaths)
    {
	Gtk.init(args);

	//-------------------GUInea pigs-------------------
	// System.out.println("infLoop PID : " + SysUtils.infLoop ());
	// System.out.println("infThreadLoop PID : " + SysUtils.infThreadLoop (2));
	//-------------------------------------------------

	TrayIcon trayIcon;
	Gui procpop = null;
	Preferences prefs = null;

	setupErrorLogging(); 
		
	IconManager.setImageDir(imagePaths);
	IconManager.loadIcons();
	IconManager.useSmallIcons();
		
	Messages.setBundlePaths(messagePaths);
		
	SourceWindowFactory.setTestFilesPath(testfilePaths);
			
		
	try {
	    procpop = new Gui (glade_dirs);
	} catch (GladeXMLException e1) {
	    errorLogFile.log(Level.SEVERE, "procpop.glade XML is badly formed", //$NON-NLS-1$
			     e1);
	    System.exit(1);
	} catch (FileNotFoundException e1) {
	    errorLogFile.log(Level.SEVERE, "ProcPop glade XML file not found", //$NON-NLS-1$
			     e1);
	    System.exit(1);
	} catch (IOException e1) {
	    errorLogFile.log(Level.SEVERE, "IOException: ", e1); //$NON-NLS-1$
	    System.exit(1);
	}

	WindowManager.theManager.mainWindow.hideAll();

	// Now that we now the glade paths are good, send the paths to
	// the SourceWindowFactory
	SourceWindowFactory.setGladePaths(glade_dirs);

	prefs = importPreferences (Config.FRYSK_DIR + SETTINGSFILE);

	trayIcon = new TrayIcon("Frysk Monitor/Debugger", false); //$NON-NLS-1$
		
	trayIcon.setMenuButton(TrayIcon.BUTTON_3);
	trayIcon.setWindowButton(TrayIcon.BUTTON_1);
	trayIcon.addPopupWindow(WindowManager.theManager.mainWindow);
		
	// Right click menu.
	Menu popupMenu = new Menu();
	trayIcon.setPopupMenu(popupMenu);
		
	// Quit 
	MenuItem quitItem = new MenuItem("Quit", false); //$NON-NLS-1$
	quitItem.addListener (new MenuItemListener()
	    {
		public void menuItemEvent(MenuItemEvent arg0) {
		    Gtk.mainQuit();
		}
	    });
	popupMenu.add(quitItem);
		
	// Console Window
	MenuItem consoleWindowItem = new MenuItem("Console Window", false); //$NON-NLS-1$
	consoleWindowItem.addListener (new MenuItemListener()
	    {
		public void menuItemEvent(MenuItemEvent arg0){
		    new ConsoleWindow();
		}
	    });
	popupMenu.prepend(consoleWindowItem);

	Thread backendStarter =  null;
	backendStarter = new Thread(new Runnable()
	    {
		public void run()
		{
		    try {
			// EventLoop eventLoop = new EventLoop();
			// eventLoop.run();
			Manager.eventLoop.run();
		    }
		    catch (Exception e) {
			DialogManager.showErrorDialog("Frysk Core Errors", "Frysk Core has reported the following errors", e); //$NON-NLS-1$ //$NON-NLS-2$
			System.exit(1);
		    }
		}
	    });
	
	backendStarter.start();

//	WindowManager.theManager.prefsWindow.addPage("One", new PreferenceWidget("One"));
//	WindowManager.theManager.prefsWindow.addPage("two", new PreferenceWidget("Two"));
//	WindowManager.theManager.prefsWindow.addPage("Three", new PreferenceWidget("Three"));

	procpop.load(prefs);
		
	WindowManager.theManager.mainWindow.setIcon(IconManager.windowIcon);

	
	CustomEvents.addEvent(new Runnable() {
		public void run() {
			WindowManager.theManager.splashScreen.showAll();

			TimerEvent timerEvent = new TimerEvent(0, 5000){
				public void execute() {
					WindowManager.theManager.splashScreen.hideAll();
					WindowManager.theManager.mainWindow.showAll();
					Manager.eventLoop.remove(this);
				}
			};
			
			Manager.eventLoop.add (timerEvent);
		}
	});
	
	
//	CustomEvents.addEvent(new Runnable() {
//		public void run() {
//			WindowManager.theManager.splashScreen.showAll();
//			Timer timer = new Timer(2000, new Fireable() {
//				public boolean fire() {
//					WindowManager.theManager.splashScreen.hideAll();
//					WindowManager.theManager.mainWindow.showAll();
//					return false;
//				}
//			});
//			timer.start();
//		}
//	});
	
	Gtk.main();
		
	Manager.eventLoop.requestStop();
	procpop.save(prefs);

	//XXX:
	ObserverManager.theManager.save();
	
	try {
	    // Export the node to a file
	    File checkFrysk = new File (Config.FRYSK_DIR);
	    if (!checkFrysk.exists())
		checkFrysk.mkdirs();
	    prefs.exportSubtree (new FileOutputStream (Config.FRYSK_DIR + SETTINGSFILE));
	} catch (Exception e) {
	    errorLogFile.log(Level.SEVERE, "Errors exporting preferences", e); //$NON-NLS-1$

	}

    }
}
