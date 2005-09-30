// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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
import org.gnu.gtk.Gtk;
import org.gnu.gtk.Image;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;

import frysk.Config;
import frysk.gui.monitor.FryskErrorFileHandler;
import frysk.gui.monitor.Saveable;
import frysk.gui.monitor.TrayIcon;
import frysk.gui.monitor.WindowManager;
import frysk.proc.Manager;

public class FryskGui implements LifeCycleListener, Saveable {

	LibGlade glade;

	private static Logger errorLogFile = null;
	private static final String SETTINGSFILE = ".settings";
	private static final String GLADE_FILE = "procpop.glade";
	private static final String GLADE_DEV_PATH = "frysk/gui/glade/";
	private static final String GLADE_PKG_PATH = "glade/";
	private static final String FRYSK_CONFIG = System.getProperty("user.home")
			+ "/" + ".frysk" + "/";
	
	public static final String ERROR_LOG_ID = "frysk.gui.errorlog";

	static {
		System.loadLibrary("EggTrayIcon");
	}

	FryskGui(String[] glade_dirs) throws GladeXMLException, FileNotFoundException,
			IOException {

		/*
		 * The location of the glade file may need to be modified
		 * here, depending on where the program is being run from. If
		 * the directory that the src directory is in is used as the
		 * root, this should work without modification
		 */

		String searchPath = new String();
		for (int i = 0; i < glade_dirs.length; i++) {
			try {// command line glade_dir
				glade = new LibGlade(glade_dirs[i] + GLADE_FILE, this);
			} catch (FileNotFoundException missingFile) {
				searchPath += glade_dirs[i] + "\n";
				if(i == glade_dirs.length -1){
					throw new FileNotFoundException("Glade file not found in path " + searchPath);
				}else{
					continue;
				}
			}
			break;
		}
		

		try {
			WindowManager.theManager.initWindows(glade);
		} catch (IOException e) {
			throw e;
		}

	}

	public static void mainGui(String[] args, String[] glade_dirs) {

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
			procpop = new FryskGui(glade_dirs);
		} catch (GladeXMLException e1) {
			errorLogFile.log(Level.SEVERE, "procpop.glade XML is badly formed",
					e1);
			System.exit(1);
		} catch (FileNotFoundException e1) {
			errorLogFile.log(Level.SEVERE, "ProcPop glade XML file not found",
					e1);
			System.exit(1);
		} catch (IOException e1) {
			errorLogFile.log(Level.SEVERE, "IOException: ", e1);
			System.exit(1);
		}

		prefs = importPreferences(FRYSK_CONFIG + SETTINGSFILE);

		trayIcon = new TrayIcon("Accudog", "", new Image(
				"gui/images/accudog.jpg"));

		trayIcon.setMenuButton(TrayIcon.BUTTON_3);
		trayIcon.setWindowButton(TrayIcon.BUTTON_1);
		trayIcon.addPopupWindow(WindowManager.theManager.mainWindow);

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

		Thread backendStarter = new Thread(new Runnable() {
			public void run() {
				Manager.eventLoop.run();
			}
		});
		
		procpop.load(prefs);
		backendStarter.start();

		Gtk.main();
		
		Manager.eventLoop.requestStop();
		procpop.save(prefs);

		try {
			// Export the node to a file
			File checkFrysk = new File(FRYSK_CONFIG);
			if (!checkFrysk.exists())
				checkFrysk.mkdirs();
			prefs.exportSubtree(new FileOutputStream(FRYSK_CONFIG + SETTINGSFILE));
		} catch (Exception e) {
			errorLogFile.log(Level.SEVERE, "Errors exporting preferences", e);

		}

	}

	public static void main(String[] args) {
		mainGui(args, new String[]
		    { GLADE_PKG_PATH, GLADE_DEV_PATH,
		      // Check both relative ...
		      Config.SRCDIR + "/../frysk-gui/" + GLADE_DEV_PATH,
		      // ... and absolute.
		      Config.ABS_SRCDIR + "/../frysk-gui/" + GLADE_DEV_PATH,
		    });
	}

	private static FileHandler buildHandler() {
		FileHandler handler = null;
		File log_dir = new File(FRYSK_CONFIG + "logs" + "/");

		if (!log_dir.exists())
			log_dir.mkdirs();

		try {

			handler = new FryskErrorFileHandler(log_dir.getAbsolutePath()
					+ "/" + "frysk_monitor_error.log", true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return handler;
	}

	private static void setupErrorLogging() {
		// Get a logger; the logger is automatically created if it
		// doesn't already exist

		errorLogFile = Logger.getLogger(ERROR_LOG_ID);
		errorLogFile.addHandler(buildHandler());
	}

	private static Preferences importPreferences(String location) {
		InputStream is = null;
		Preferences prefs = null;

		try {
			is = new BufferedInputStream(new FileInputStream(location));
			Preferences.importPreferences(is);
		} catch (FileNotFoundException e1) {
			errorLogFile.log(Level.WARNING, location
					+ " not found. Will be created on program exit", e1);
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
		WindowManager.theManager.save(Preferences.userRoot().node(
				prefs.absolutePath() + "theManager"));
	}

	public void load(Preferences prefs) {
		WindowManager.theManager.load(Preferences.userRoot().node(
				prefs.absolutePath() + "theManager"));
	}
}
