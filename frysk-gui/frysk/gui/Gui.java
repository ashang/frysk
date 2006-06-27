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
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
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
import org.gnu.pango.Style;
import org.gnu.pango.Weight;

import frysk.Config;
import frysk.EventLogger;
import frysk.event.TimerEvent;
import frysk.event.SignalEvent;
import frysk.gui.common.IconManager;
import frysk.gui.common.Messages;
import frysk.gui.common.dialogs.DialogManager;
import frysk.gui.common.dialogs.ErrorDialog;
import frysk.gui.common.prefs.BooleanPreference;
import frysk.gui.common.prefs.ColorPreference;
import frysk.gui.common.prefs.IntPreference;
import frysk.gui.common.prefs.PreferenceGroup;
import frysk.gui.common.prefs.PreferenceManager;
import frysk.gui.monitor.ConsoleWindow;
import frysk.gui.monitor.CoreDebugLogViewer;
import frysk.gui.monitor.FryskErrorFileHandler;
import frysk.gui.monitor.Saveable;
import frysk.gui.monitor.TrayIcon;
import frysk.gui.monitor.WindowManager;
import frysk.gui.monitor.datamodels.CoreDebugHandler;
import frysk.gui.monitor.datamodels.DataModelManager;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.srcwin.SourceWindowFactory;
import frysk.gui.srcwin.prefs.SourceWinPreferenceGroup;
import frysk.gui.srcwin.prefs.SyntaxPreference;
import frysk.gui.srcwin.prefs.SyntaxPreferenceGroup;
import frysk.gui.srcwin.tags.Tagset;
import frysk.gui.srcwin.tags.TagsetManager;
import frysk.proc.Manager;
import frysk.sys.Signal;
import frysk.sys.Sig;
import frysk.sys.Pid;

public class Gui
implements LifeCycleListener, Saveable
{
	LibGlade glade;
	LibGlade create_session_glade;
	LibGlade register_window;
	LibGlade session_glade;
	
	private static Logger errorLogFile = null;
	private static final String SETTINGSFILE = ".settings";
	private static final String GLADE_FILE = "procpop.glade";
	private static final String CREATE_SESSION_GLADE = "frysk_create_session_druid.glade";
	private static final String SESSION_MANAGER_GLADE = "frysk_session_manager.glade";
	
	static Logger logger;
	
	public static final String ERROR_LOG_ID = "frysk.gui.errorlog";
	
	private static void initializePreferences(){    	
		PreferenceManager.sourceWinGroup.addPreference(new IntPreference(SourceWinPreferenceGroup.INLINE_LEVELS, 0, 10, 2));
		
		PreferenceManager.sourceWinGroup.addPreference(new BooleanPreference(SourceWinPreferenceGroup.EXEC_MARKS, true));
		PreferenceManager.sourceWinGroup.addPreference(new BooleanPreference(SourceWinPreferenceGroup.LINE_NUMS, true));
		PreferenceManager.sourceWinGroup.addPreference(new BooleanPreference(SourceWinPreferenceGroup.TOOLBAR, true));
		
		PreferenceManager.sourceWinGroup.addPreference(new ColorPreference(SourceWinPreferenceGroup.EXEC_MARKS_COLOR,Color.BLACK));
		PreferenceManager.sourceWinGroup.addPreference(new ColorPreference(SourceWinPreferenceGroup.LINE_NUMBER_COLOR,Color.BLACK));
		
		PreferenceGroup lnfGroup = new PreferenceGroup("Look and Feel", 2);
		lnfGroup.addPreference(new ColorPreference(SourceWinPreferenceGroup.BACKGROUND,Color.WHITE));
		lnfGroup.addPreference(new ColorPreference(SourceWinPreferenceGroup.CURRENT_LINE,Color.GREEN));
		lnfGroup.addPreference(new ColorPreference(SourceWinPreferenceGroup.SEARCH,Color.ORANGE));
		lnfGroup.addPreference(new ColorPreference(SourceWinPreferenceGroup.TEXT,Color.BLACK));
		lnfGroup.addPreference(new ColorPreference(SourceWinPreferenceGroup.MARGIN,new Color(37779, 40349, 50115)));
		
		PreferenceManager.sourceWinGroup.addSubgroup(lnfGroup);
		
		PreferenceManager.addPreferenceGroup(PreferenceManager.sourceWinGroup);
		
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.CLASSES, Color.RED, Weight.BOLD, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.FUNCTIONS, new Color(4369, 6939, 51914), Weight.BOLD, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.GLOBALS, new Color(8224, 36494, 16191), Weight.NORMAL, Style.ITALIC));
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.KEYWORDS, new Color(22102, 4112, 31868), Weight.BOLD, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.OPTIMIZED, new Color(38293, 38293, 38293), Weight.NORMAL, Style.ITALIC));
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.OUT_OF_SCOPE, new Color(38293, 38293, 38293), Weight.NORMAL, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.VARIABLES, new Color(15677, 49601, 17990), Weight.NORMAL, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.COMMENTS, new Color(47031, 40606, 32125), Weight.NORMAL, Style.ITALIC));
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.NAMESPACE, Color.RED, Weight.BOLD, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.INCLUDES, new Color(15677, 49601, 17990), Weight.NORMAL, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.MACRO, Color.BLACK, Weight.BOLD, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup.addPreference(new SyntaxPreference(SyntaxPreferenceGroup.TEMPLATE, new Color(42102, 24112, 51868), Weight.BOLD, Style.NORMAL));
		
		PreferenceManager.addPreferenceGroup(PreferenceManager.syntaxHighlightingGroup);
	}
	
	private static void createDummyTagsets(){
		Tagset ts = new Tagset("httpd network layer", "For debugging aspects of httpd relating to the low level network layer", "httpd", "1.2");
		TagsetManager.manager.addTagset(ts);
		
		ts = new Tagset("httpd error messages", "Tags for dealing with httpd errors before they're sent to the client", "httpd", "1.2.1");
		TagsetManager.manager.addTagset(ts);
		
		ts = new Tagset("eclipse Copy and Pase bug finder", "Attaches hooks into eclipse's copy and paste mechnaism", "eclipse", "3.2M6");
		TagsetManager.manager.addTagset(ts);
		
		ts = new Tagset("Firefox plugin loader", "Aids in debugging plugin loading in firefox", "firefox", "1.5.0");
		TagsetManager.manager.addTagset(ts);
		
		ts = new Tagset("Firefox network layer", "Tags for debugging the low level network code in firefox", "firefox", "1.5.0");
	}
	
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
				create_session_glade = new LibGlade (glade_dirs[i] + CREATE_SESSION_GLADE, this);
				register_window = new LibGlade (glade_dirs[i] + "/registerwindow.glade", null);
				session_glade = new LibGlade(glade_dirs[i] + SESSION_MANAGER_GLADE, this);
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
			WindowManager.theManager.initLegacyProcpopWindows(glade);
			WindowManager.theManager.initRegisterWindow(register_window);
			WindowManager.theManager.initSessionDruidWindow(create_session_glade);
			WindowManager.theManager.initSessionManagerWindow(session_glade);	    
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
		//errorLogFile.setUseParentHandlers(true);
		errorLogFile.addHandler(buildHandler());
	}
	
	private static Preferences importPreferences (String location)
	{
		InputStream is = null;
		Preferences prefs = null;
		
		File checkFile = new File (location);
		if (checkFile.exists()){
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
		
		/* Make sure that a Frysk invocation is not already running */
		int currentlyRunningPID;
    	File dir = new File(Config.FRYSK_DIR);
    	
    	if (dir.exists()) {
    		String[] contents = dir.list();
    		for (int i = 0; i < contents.length; i++) {
    			if (contents[i].startsWith("lock")) {
    				currentlyRunningPID = Integer.parseInt(contents[i].substring(4));
    				System.out.println("Frysk is already running!");
    				try {
    				Signal.kill(currentlyRunningPID, Sig.USR1);
    				} catch (Exception e) {
    					/* The lock file shouldn't be there */
    					File f = new File(Config.FRYSK_DIR + contents[i]);
    					f.delete();
    					break;
    				}
    				System.exit(0);
    			}
    		}
    	}
    	else
    		dir.mkdir();
    	
    	File lock = new File(Config.FRYSK_DIR + "lock" + Pid.get());
    	try {
    		lock.createNewFile();
    		lock.deleteOnExit();
    	} catch (IOException ioe) {
    		System.out.println(ioe.getMessage());
    		System.exit(1);
    	}
		
		Gtk.init(args);
		

		//XXX: a hack to make sure the DataModelManager
		// is initialized early enough. Should probably
		// have an entitiy that initializes all Managers
		DataModelManager.theManager.flatProcObservableLinkedList.getClass();

		IconManager.setImageDir(imagePaths);
		IconManager.loadIcons();
		IconManager.useSmallIcons();

		// Creates example tagsets until we can have real ones.
		createDummyTagsets();
	
		setupCoreLogging();

		
		TrayIcon trayIcon;
		Gui procpop = null;
		Preferences prefs = null;
		
		setupErrorLogging(); 
		
		
		Messages.setBundlePaths(messagePaths);
		
		
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
		PreferenceManager.setPreferenceModel(prefs);
		initializePreferences();
		
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
		
		MenuItem cliWindowItem = new MenuItem("Command Interface Window", false);
		cliWindowItem.addListener (new MenuItemListener()
				{
			public void menuItemEvent(MenuItemEvent arg0){
				new frysk.vtecli.ConsoleWindow();
			}
				});
		
		popupMenu.prepend(consoleWindowItem);
		popupMenu.prepend(cliWindowItem);
		
		final Thread backendStarter  = new Thread(new Runnable(){
			public void run(){
				try {
					// EventLoop eventLoop = new EventLoop();
					// eventLoop.run();
					Manager.eventLoop.run();
				}
				catch (Exception e) {
					errorLogFile.log(Level.SEVERE, "Frysk Core Warnings. The Frysk Core has encountered problems: ", e); //$NON-NLS-1$
					int response = DialogManager.showErrorDialog("Frysk Core Warnings", "The Frysk Core has encountered problems with the last request.\n"+
							"It has reported the following conditions. You can either ignore\n"+
							"the condition and continue, or Quit Frysk.", e); //$NON-NLS-1$ //$NON-NLS-2$
					if (response == ErrorDialog.QUIT)
						System.exit(1);
					else
						run();
				}
			}
		});
		backendStarter.start();
			
		WindowManager.theManager.mainWindow.setIcon(IconManager.windowIcon);
		
		final Gui myGui = procpop;
		final Preferences myPrefs = prefs;
		
		myGui.load(myPrefs);
			
		CustomEvents.addEvent(new Runnable() {
			public void run() {
				WindowManager.theManager.splashScreen.showAll();
			}
		});
		

		
		TimerEvent timerEvent = new TimerEvent(0, 5000){
			public void execute() {
				CustomEvents.addEvent(new Runnable() {
					public void run() {
						WindowManager.theManager.splashScreen.hideAll();
					}
				});
				Manager.eventLoop.remove(this);
			}
		};
		Manager.eventLoop.add (timerEvent);
		
				
		CustomEvents.addEvent(new Runnable() {
			public void run() {
				TimerEvent refreshTimer = new TimerEvent(0, 5000){
					public void execute() {
						Manager.host.requestRefreshXXX (true);
					}
				};
				
				Manager.eventLoop.add (refreshTimer);
			}
		});
		
		CustomEvents.addEvent(new Runnable() {
			public void run() {
				MultipleInvocationEvent mie = new MultipleInvocationEvent();
				Manager.eventLoop.add (mie);
			}
		});
		
		CustomEvents.addEvent(new Runnable() {
			public void run() {
				InterruptEvent ie = new InterruptEvent();
				Manager.eventLoop.add (ie);
			}
		});
		
		Gtk.main();
		
		Manager.eventLoop.requestStop();
		procpop.save(prefs);
		
		//XXX:
		ObserverManager.theManager.save();
		
		try {
			// Export the node to a file
			prefs.exportSubtree (new FileOutputStream (Config.FRYSK_DIR + SETTINGSFILE));
		} catch (Exception e) {
			errorLogFile.log(Level.SEVERE, "Errors exporting preferences", e); //$NON-NLS-1$
			
		}
		
	}
	
	private static void setupCoreLogging() {
		// Get Core logger

		logger = EventLogger.get ("logs/", "frysk_core_event.log");
	    Handler consoleHandler = new ConsoleHandler ();
	    Handler guiHandler = new CoreDebugHandler();
        logger.addHandler (consoleHandler);		
        logger.addHandler(guiHandler);
        
		// Set the location of the level sets
		System.setProperty("java.util.logging.config.file", Config.FRYSK_DIR+"logging.properties");
		LogManager logManager = LogManager.getLogManager();
		logManager.addLogger(logger);
		
		Level loggerLevel = Level.OFF;	
		try {
			loggerLevel = Level.parse(logManager.getProperty("java.util.logging.FileHandler.level"));
		} catch (IllegalArgumentException e) {
			loggerLevel = Level.OFF;
		} catch (NullPointerException e1) {
			loggerLevel = Level.OFF;
		}
		
		Level consoleLoggerLevel = Level.OFF;	
		try {
			consoleLoggerLevel = Level.parse(logManager.getProperty("java.util.logging.ConsoleHandler.level"));
		} catch (IllegalArgumentException e) {
			consoleLoggerLevel = Level.OFF;
		} catch (NullPointerException e1) {
			consoleLoggerLevel = Level.OFF;
		}
		
		Level guiLoggerLevel = Level.OFF;	
		try {
			guiLoggerLevel = Level.parse(logManager.getProperty("frysk.core.debug.WindowHandler.level"));
		} catch (IllegalArgumentException e) {
			guiLoggerLevel = Level.OFF;
		} catch (NullPointerException e1) {
			guiLoggerLevel = Level.OFF;
		}
		
		if (guiLoggerLevel != Level.OFF) {
			CoreDebugLogViewer logShow = new CoreDebugLogViewer();
			logShow.showAll();
		
		}
		
		logger.setLevel(loggerLevel);
		consoleHandler.setLevel(consoleLoggerLevel);
		guiHandler.setLevel(guiLoggerLevel);
	}
	
	/* Handle a signal if another instance of Frysk is started */
	static class MultipleInvocationEvent extends SignalEvent {
		
		public MultipleInvocationEvent() {
			super(Sig.USR1);
			logger.log (Level.FINE, "{0} MultipleInvocationEvent\n", this);
		}
		
		public final void execute ()
		{
		    logger.log (Level.FINE, "{0} execute\n", this); 
		    WindowManager.theManager.mainWindow.showAll();
		}
	}
	
	/* If the user cntl-c interrupts, handle it cleanly */
	static class InterruptEvent extends SignalEvent {
		
		public InterruptEvent() {
			super(Sig.INT);
			logger.log (Level.FINE, "{0} InterruptEvent\n", this);
		}
		
		public final void execute ()
		{
		    logger.log (Level.FINE, "{0} execute\n", this); 
		    Gtk.mainQuit();
		}
	}
}
