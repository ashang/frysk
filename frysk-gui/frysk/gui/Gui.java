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

import gnu.classpath.tools.getopt.FileArgumentCallback;
//import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

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
import frysk.gui.dialogs.DialogManager;
import frysk.gui.dialogs.ErrorDialog;
import frysk.gui.dialogs.WarnDialog;
import frysk.gui.prefs.BooleanPreference;
import frysk.gui.prefs.ColorPreference;
import frysk.gui.prefs.IntPreference;
import frysk.gui.prefs.PreferenceGroup;
import frysk.gui.prefs.PreferenceManager;
import frysk.gui.disassembler.DisassemblyWindowFactory;
import frysk.gui.memory.MemoryWindowFactory;
import frysk.gui.monitor.ConsoleWindow;
import frysk.gui.monitor.CoreDebugLogViewer;
import frysk.gui.monitor.FryskErrorFileHandler;
import frysk.gui.monitor.Saveable;
import frysk.gui.monitor.TrayIcon;
import frysk.gui.monitor.WindowManager;
import frysk.gui.monitor.datamodels.CoreDebugHandler;
import frysk.gui.monitor.datamodels.DataModelManager;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.register.RegisterWindowFactory;
import frysk.gui.srcwin.SourceWindowFactory;
import frysk.gui.srcwin.prefs.SourceWinPreferenceGroup;
import frysk.gui.srcwin.prefs.SyntaxPreference;
import frysk.gui.srcwin.prefs.SyntaxPreferenceGroup;
import frysk.gui.common.FryskHelpManager;

import frysk.proc.Manager;
import frysk.sys.Signal;
import frysk.sys.Sig;
import frysk.sys.Pid;


public class Gui implements LifeCycleListener, Saveable {

	private static Logger errorLogFile = null;

	private static final String SETTINGSFILE = ".settings";

	private static final String GLADE_FILE = "procpop.glade";

	private static final String CREATE_SESSION_GLADE = "frysk_create_session_druid.glade";

	private static final String SESSION_MANAGER_GLADE = "frysk_session_manager.glade";

	public static final String ERROR_LOG_ID = "frysk.gui.errorlog";

	LibGlade glade;

	LibGlade create_session_glade;

	LibGlade register_window;

	LibGlade memory_window;

	LibGlade disassembler_window;

	LibGlade session_glade;

	LibGlade process_picker_glade;

	static Logger logger;
    
    private static boolean flag = true;
    
    private static String argString = "";
    
    private static int pid = 0;

	/**
	 * gui - FryskUI main program. Loads the window from glade,
	 * sets logging and preference, signal handlers for duplicate instances. 
	 * Initializes gtk, and runs the main event loop.
	 * @param args - args passed from FryskUI
	 * @param glade_dirs - where the glade files can be found
	 * @param imagePaths - where the images are
	 * @param messagePaths - where the message paths are.
	 * @param testfilePaths - where the testfiles are located
     * @param helpdocPaths - where the online help docs are
	 */
	public static void gui (String[] args, String[] glade_dirs,
                          String[] imagePaths, String[] messagePaths,
                          String[] testfilePaths, String[] help_dirs)
  {
    
    Parser parser = new Parser("frysk", "1..23", true);
    parser.setHeader("usage: frysk [options]");

    addOptions(parser);

    parser.parse(args, new FileArgumentCallback()
    {
      public void notifyFile (String arg) throws OptionException
      {
      }
    });

    Gui procpop = null;
    Preferences prefs = null;

    // Set Frysk Name
    System.setProperty("gnome.appName", "Frysk");

    // Check Frysk data location is created
    createFryskDataLocation(Config.FRYSK_DIR);

    // Make sure that a Frysk invocation is not already running
    if (isFryskRunning())
      {
        System.err.println("Frysk is already running!");
        System.exit(0);
      }

    // Create a Frysk lock file
    createFryskLockFile(Config.FRYSK_DIR + "lock" + Pid.get());

    Gtk.init(args);

    // XXX: a hack to make sure the DataModelManager
    // is initialized early enough. Should probably
    // have an entitiy that initializes all Managers
    DataModelManager.theManager.flatProcObservableLinkedList.getClass();

    // Setup Icon Manager singleton
    IconManager.setImageDir(imagePaths);
    IconManager.loadIcons();
    IconManager.useSmallIcons();

    // Bootstraps Core logging
    setupCoreLogging();

    // Bootsraps Error Logging
    setupErrorLogging();

    // Sets transaltion bundle paths
    Messages.setBundlePaths(messagePaths);

    if (flag == false)
      {
        gui (args, glade_dirs);
        return;
      }
    
    // Load glade, and setup WindowManager
    try
      {
        procpop = new Gui(glade_dirs);
      }
    catch (GladeXMLException e1)
      {
        errorLogFile.log(Level.SEVERE, "glade XML is badly formed", //$NON-NLS-1$
                         e1);
        System.exit(1);
      }
    catch (FileNotFoundException e1)
      {
        errorLogFile.log(Level.SEVERE, "glade XML files not found", //$NON-NLS-1$
                         e1);
        System.exit(1);
      }
    catch (IOException e1)
      {
        errorLogFile.log(Level.SEVERE, "IOException: ", e1); //$NON-NLS-1$
        System.exit(1);
      }

    // Hide main for now
    WindowManager.theManager.mainWindow.setIcon(IconManager.windowIcon);
    WindowManager.theManager.mainWindow.hideAll();

    // Now that we now the glade paths are good, send the paths to
    // the SourceWindowFactory
    SourceWindowFactory.setGladePaths(glade_dirs);
    RegisterWindowFactory.setPaths(glade_dirs);
    MemoryWindowFactory.setPaths(glade_dirs);
    DisassemblyWindowFactory.setPaths(glade_dirs);
    FryskHelpManager.setHelpPaths(help_dirs);

    // Find and load preferences.
    prefs = importPreferences(Config.FRYSK_DIR + SETTINGSFILE);
    PreferenceManager.setPreferenceModel(prefs);
    initializePreferences();

    // Startup Trayicon Manager right click menu.
    buildTrayManager();

    // Bootstrap Core Event Loop
    startCoreEventLoop();

    // Assign
    final Gui myGui = procpop;
    final Preferences myPrefs = prefs;

    // Load preferences
    myGui.load(myPrefs);

    // Add interruption and multiple
    // invocations handlers
    addInvocationEvents();

    // Show wndow and run
    WindowManager.theManager.sessionManager.showAll();
    Gtk.main();

    WindowManager.theManager.mainWindow.killTerminalShell();
    // Gtk main loop exited, stop core event loop.
    Manager.eventLoop.requestStop();

    // Save preferences
    procpop.save(prefs);
    WindowManager.theManager.mainWindow.killTerminalShell();

    // XXX: Save Observers
    ObserverManager.theManager.save();

    try
      {
        // Export the node to a file
        prefs.exportSubtree(new FileOutputStream(Config.FRYSK_DIR
                                                 + SETTINGSFILE));
      }
    catch (Exception e)
      {
        errorLogFile.log(Level.SEVERE, "Errors exporting preferences", e); //$NON-NLS-1$

      }
  }
    
    private static void addOptions (Parser parser)
    {
//    parser.add(new Option("new", 'n', "debug new exec", "EXECUTABLE")
//        {
//            public void parsed (String arg) throws OptionException
//            {
//                File file = null;
//                try 
//                {
//                    file = new File(arg);
//                    if (!file.exists())
//                    {
//                        System.out.println("Cannot find executable!");
//                        System.exit(1);
//                    }
//                    else
//                      argString = arg;
//                    
//                    flag = false;
//                }
//                catch (Exception e)
//            {
//                OptionException oe = new OptionException("couldn't parse executable: " + arg);
//                oe.initCause(e);
//                throw oe;
//            }
//        }
//        });
    
//    parser.add(new Option("pid", 'p', "debug running pid", "EXECUTABLE")
//        {
//        public void parsed (String arg) throws OptionException
//        {
//          flag = false;
//            try 
//            {
//                pid = Integer.parseInt(arg);
//            }
//            catch (NumberFormatException nfe)
//            {
//                OptionException oe = new OptionException("couldn't parse pid: " + arg);
//                oe.initCause(nfe);
//                throw oe;
//            }
//        }
//        });
    }
    
    public static void gui (String[] args, String[] glade_dirs)
      {
        
        Preferences prefs = null;
        
        SourceWindowFactory.setGladePaths(glade_dirs);
        RegisterWindowFactory.setPaths(glade_dirs);
        MemoryWindowFactory.setPaths(glade_dirs);
        DisassemblyWindowFactory.setPaths(glade_dirs);

        // Find and load preferences.
        prefs = importPreferences(Config.FRYSK_DIR + SETTINGSFILE);
        PreferenceManager.setPreferenceModel(prefs);
        initializePreferences();

        // Startup Trayicon Manager right click menu.
        buildTrayManager();

        // Bootstrap Core Event Loop
        startCoreEventLoop();
        
//      Add interruption and multiple
        // invocations handlers
        addInvocationEvents();
        
        if (pid != 0)
          SourceWindowFactory.attachToPID(pid);
        else
          SourceWindowFactory.startNewProc(argString);
        
        Gtk.main();

        // Gtk main loop exited, stop core event loop.
        Manager.eventLoop.requestStop();

        try
          {
            // Export the node to a file
            prefs.exportSubtree(new FileOutputStream(Config.FRYSK_DIR
                                                     + SETTINGSFILE));
          }
        catch (Exception e)
          {
            errorLogFile.log(Level.SEVERE, "Errors exporting preferences", e); //$NON-NLS-1$

          }
      }
	
	/**
	 * Creates a lock file in ~/.frysk
	 * 
	 * @param lockFile - name and location of lock file
	 */
	private static void createFryskLockFile(String lockFile) {
		File lock = new File(lockFile);
		try {
			lock.createNewFile();
			lock.deleteOnExit();
		} catch (IOException ioe) {
			System.err.println(ioe.getMessage());
		}
	}
	
	/**
	 * 
	 * Creates a data dir for Frysk
	 * 
	 * @param config - location for dir
	 * 
	 */
	private static void createFryskDataLocation(String config) {
		File dir = new File(config);
		if (!dir.exists())
			dir.mkdir();
	}
	
	/**
	 * 
	 * Checks to see if there already is a Frysk invocation running
	 * 
	 * @return - true if there is a Frysk invocation already running.
	 * 
	 */
	private static boolean isFryskRunning() {
		// Make sure that a Frysk invocation is not already running
		int currentlyRunningPID;
		File dir = new File(Config.FRYSK_DIR);

		if (dir.exists()) {
			String[] contents = dir.list();
			for (int i = 0; i < contents.length; i++) {
				if (contents[i].startsWith("lock")) {
					currentlyRunningPID = Integer.parseInt(contents[i]
							.substring(4));
					try {
						Signal.kill(currentlyRunningPID, Sig.USR1);
					} catch (Exception e) {
						/* The lock file shouldn't be there */
						File f = new File(Config.FRYSK_DIR + contents[i]);
						f.delete();
						break;
					}
					return true;
				}
			}
		} 
		
		return false;
	}

	/**
	 * Adds Multiple and Interruption events to the 
	 * core event loop.
	 */
	private static void addInvocationEvents() {
		CustomEvents.addEvent(new Runnable() {
			public void run() {
				MultipleInvocationEvent mie = new MultipleInvocationEvent();
				Manager.eventLoop.add(mie);
			}
		});

		CustomEvents.addEvent(new Runnable() {
			public void run() {
				InterruptEvent ie = new InterruptEvent();
				Manager.eventLoop.add(ie);
			}
		});
	}
	
	
	/**
	 * Starts the core event loop inside a Runnable. 
	 * 
	 * Also sets up the error handler for core errors,
	 * and initiates a refresh timer for events from
	 * the core.
	 * 
	 */
	private static void startCoreEventLoop() {
		final Thread backendStarter = new Thread(new Runnable() {
			public void run() {
				try {
					// EventLoop eventLoop = new EventLoop();
					// eventLoop.run();
					Manager.eventLoop.run();
				} catch (Exception e) {
					errorLogFile
							.log(
									Level.SEVERE,
									"Frysk Core Warnings. The Frysk Core has encountered problems: ", e); //$NON-NLS-1$
					int response = DialogManager
							.showErrorDialog(
									"Frysk Core Warnings",
									"The Frysk Core has encountered problems with the last request.\n"
											+ "It has reported the following conditions. You can either ignore\n"
											+ "the condition and continue, or Quit Frysk.", e); //$NON-NLS-1$ //$NON-NLS-2$
					if (response == ErrorDialog.QUIT)
						System.exit(1);
					else
						run();
				}
			}
		});
		backendStarter.start();
		
		TimerEvent refreshTimer = new TimerEvent(0, 3000) {
			public void execute() {
				CustomEvents.addEvent(new Runnable() {
					public void run() {
						Manager.host.requestRefreshXXX(true);
					}
				});
			}
		};
		Manager.eventLoop.add(refreshTimer);

	}
	
	/**
	 * Function to quit frysk. All requests to quit frysk should be funneled
	 * through this function.
	 */
	public static void quitFrysk() {
		Gtk.mainQuit();
	}
	
	/**
	 * Builds the tray icon, and the menus that are associated with the icon.
	 *
	 */
	private static void buildTrayManager()
	{
		
		IconManager.trayIcon.setMenuButton(TrayIcon.BUTTON_3);
		IconManager.trayIcon.setWindowButton(TrayIcon.BUTTON_1);
//		IconManager.trayIcon
//				.addPopupWindow(WindowManager.theManager.mainWindow);
		
		Menu popupMenu = new Menu();
		IconManager.trayIcon.setPopupMenu(popupMenu);

		// Quit
		MenuItem quitItem = new MenuItem("Quit", false); //$NON-NLS-1$
		quitItem.addListener(new MenuItemListener() {
			public void menuItemEvent(MenuItemEvent arg0) {
				Gui.quitFrysk();
			}
		});
		popupMenu.add(quitItem);

		// Console Window
		MenuItem consoleWindowItem = new MenuItem("Console Window", false); //$NON-NLS-1$
		consoleWindowItem.addListener(new MenuItemListener() {
			public void menuItemEvent(MenuItemEvent arg0) {
				new ConsoleWindow();
			}
		});

		popupMenu.prepend(consoleWindowItem);
	}

	Gui(String[] glade_dirs) throws GladeXMLException, FileNotFoundException,
			IOException {
		// The location of the glade file may need to be modified
		// here, depending on where the program is being run from. If
		// the directory that the src directory is in is used as the
		// root, this should work without modification
		String searchPath = new String();
		for (int i = 0; i < glade_dirs.length; i++) {
			try {// command line glade_dir
				glade = new LibGlade(glade_dirs[i] + GLADE_FILE, this);
				create_session_glade = new LibGlade(glade_dirs[i]
						+ CREATE_SESSION_GLADE, this);
				register_window = new LibGlade(glade_dirs[i]
						+ "/registerwindow.glade", null);
				memory_window = new LibGlade(glade_dirs[i]
						+ "/memorywindow.glade", null);
				disassembler_window = new LibGlade(glade_dirs[i]
						+ "/disassemblywindow.glade", null);
				session_glade = new LibGlade(glade_dirs[i]
						+ SESSION_MANAGER_GLADE, this);
				process_picker_glade = new LibGlade(glade_dirs[i]
						+ "/processpicker.glade", null);
			} catch (FileNotFoundException missingFile) {
				searchPath += glade_dirs[i] + "\n";
				if (i == glade_dirs.length - 1) {
					throw new FileNotFoundException(
							"Glade file not found in path " + searchPath); //$NON-NLS-1$
				} else {
					continue;
				}
			}
			break;
		}

		try {
			WindowManager.theManager.initLegacyProcpopWindows(glade);
			WindowManager.theManager
					.initSessionDruidWindow(create_session_glade);
			WindowManager.theManager.initSessionManagerWindow(session_glade);
			WindowManager.theManager.initProcessPicker(process_picker_glade);
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * Builds a file handler for the frysk ui error logs
	 * 
	 * @return FileHandler - instance of the frysk ui error handler.
	 */
	private static FileHandler buildHandler() {
		FileHandler handler = null;
		File log_dir = new File(Config.FRYSK_DIR + "logs" + "/");

		if (!log_dir.exists())
			log_dir.mkdirs();

		try {
			handler = new FryskErrorFileHandler(log_dir.getAbsolutePath() + "/"
					+ "frysk_monitor_error.log", true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return handler;
	}

	/**
	 * Starts up the logger that records errors encountered
	 * when running Frysk. This log can capture both ui
	 * and core based errors.
	 */
	private static void setupErrorLogging() {
		// Get a logger; the logger is automatically created if it
		// doesn't already exist
		errorLogFile = Logger.getLogger(ERROR_LOG_ID);
		errorLogFile.setUseParentHandlers(false);
		// errorLogFile.setUseParentHandlers(true);
		errorLogFile.addHandler(buildHandler());
	}

	/**
	 * Bootstraps the core logging according to the properties
	 * found in logging.properties
	 */
	private static void setupCoreLogging() {
		// Get Core logger

		logger = EventLogger.get("logs/", "frysk_core_event.log");
		Handler consoleHandler = new ConsoleHandler();
		Handler guiHandler = new CoreDebugHandler();
		logger.addHandler(consoleHandler);
		logger.addHandler(guiHandler);

		// Set the location of the level sets
		System.setProperty("java.util.logging.config.file", Config.FRYSK_DIR
				+ "logging.properties");
		LogManager logManager = LogManager.getLogManager();
		logManager.addLogger(logger);

		Level loggerLevel = Level.OFF;
		try {
			loggerLevel = Level.parse(logManager
					.getProperty("java.util.logging.FileHandler.level"));
		} catch (IllegalArgumentException e) {
			loggerLevel = Level.OFF;
		} catch (NullPointerException e1) {
			loggerLevel = Level.OFF;
		}

		Level consoleLoggerLevel = Level.OFF;
		try {
			consoleLoggerLevel = Level.parse(logManager
					.getProperty("java.util.logging.ConsoleHandler.level"));
		} catch (IllegalArgumentException e) {
			consoleLoggerLevel = Level.OFF;
		} catch (NullPointerException e1) {
			consoleLoggerLevel = Level.OFF;
		}

		Level guiLoggerLevel = Level.OFF;
		try {
			guiLoggerLevel = Level.parse(logManager
					.getProperty("frysk.core.debug.WindowHandler.level"));
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

	/**
	 * Handle a signal if another instance of Frysk is started
	 */
	static class MultipleInvocationEvent extends SignalEvent {

		public MultipleInvocationEvent() {
			super(Sig.USR1);
			logger.log(Level.FINE, "{0} MultipleInvocationEvent\n", this);
		}

		public final void execute() {
			logger.log(Level.FINE, "{0} execute\n", this);
			CustomEvents.addEvent(new Runnable() {
				public void run() {
                  WarnDialog dialog = new WarnDialog(
                  " An instance of Frysk is already running! ");
                  dialog.showAll();
                  dialog.run();
				}
			});
		}
	}

	/**
	 * If the user cntl-c interrupts, handle it cleanly
	 */
	static class InterruptEvent extends SignalEvent {

		public InterruptEvent() {
			super(Sig.INT);
			logger.log(Level.FINE, "{0} InterruptEvent\n", this);
		}

		public final void execute() {
			logger.log(Level.FINE, "{0} execute\n", this);
			Gui.quitFrysk();
		}
	}

	/**
	 * Imports the user preferences in frysk data dir. This will
	 * be replaced with gconf soon
	 * 
	 * @param location - location of preferences
	 * @return - Preferences objct
	 */
	private static Preferences importPreferences(String location) {
		InputStream is = null;
		Preferences prefs = null;

		File checkFile = new File(location);
		if (checkFile.exists()) {
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

	/**
	 * Initializes the preferences for the source window
	 * 
	 */
	private static void initializePreferences() {
		PreferenceManager.sourceWinGroup.addPreference(new IntPreference(
				SourceWinPreferenceGroup.INLINE_LEVELS, 0, 10, 2));

		PreferenceManager.sourceWinGroup.addPreference(new BooleanPreference(
				SourceWinPreferenceGroup.EXEC_MARKS, true));
		PreferenceManager.sourceWinGroup.addPreference(new BooleanPreference(
				SourceWinPreferenceGroup.LINE_NUMS, true));
		PreferenceManager.sourceWinGroup.addPreference(new BooleanPreference(
				SourceWinPreferenceGroup.TOOLBAR, true));

		PreferenceManager.sourceWinGroup.addPreference(new ColorPreference(
				SourceWinPreferenceGroup.EXEC_MARKS_COLOR, Color.BLACK));
		PreferenceManager.sourceWinGroup.addPreference(new ColorPreference(
				SourceWinPreferenceGroup.LINE_NUMBER_COLOR, Color.BLACK));

		PreferenceGroup lnfGroup = new PreferenceGroup("Look and Feel", 2);
		lnfGroup.addPreference(new ColorPreference(
				SourceWinPreferenceGroup.BACKGROUND, Color.WHITE));
		lnfGroup.addPreference(new ColorPreference(
				SourceWinPreferenceGroup.CURRENT_LINE, Color.GREEN));
		lnfGroup.addPreference(new ColorPreference(
				SourceWinPreferenceGroup.SEARCH, Color.ORANGE));
		lnfGroup.addPreference(new ColorPreference(
				SourceWinPreferenceGroup.TEXT, Color.BLACK));
		lnfGroup
				.addPreference(new ColorPreference(
						SourceWinPreferenceGroup.MARGIN, new Color(37779,
								40349, 50115)));

		PreferenceManager.sourceWinGroup.addSubgroup(lnfGroup);

		PreferenceManager.addPreferenceGroup(PreferenceManager.sourceWinGroup);

		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.CLASSES, Color.RED, Weight.BOLD,
						Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.FUNCTIONS, new Color(4369, 6939,
								51914), Weight.BOLD, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.GLOBALS, new Color(8224, 36494,
								16191), Weight.NORMAL, Style.ITALIC));
		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.KEYWORDS, new Color(22102, 4112,
								31868), Weight.BOLD, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.OPTIMIZED, new Color(38293,
								38293, 38293), Weight.NORMAL, Style.ITALIC));
		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.OUT_OF_SCOPE, new Color(38293,
								38293, 38293), Weight.NORMAL, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.VARIABLES, new Color(15677,
								49601, 17990), Weight.NORMAL, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.COMMENTS, new Color(47031, 40606,
								32125), Weight.NORMAL, Style.ITALIC));
		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.NAMESPACE, Color.RED,
						Weight.BOLD, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.INCLUDES, new Color(15677, 49601,
								17990), Weight.NORMAL, Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.MACRO, Color.BLACK, Weight.BOLD,
						Style.NORMAL));
		PreferenceManager.syntaxHighlightingGroup
				.addPreference(new SyntaxPreference(
						SyntaxPreferenceGroup.TEMPLATE, new Color(42102, 24112,
								51868), Weight.BOLD, Style.NORMAL));

		PreferenceManager
				.addPreferenceGroup(PreferenceManager.syntaxHighlightingGroup);
	}

	/* (non-Javadoc)
	 * @see org.gnu.gtk.event.LifeCycleListener#lifeCycleEvent(org.gnu.gtk.event.LifeCycleEvent)
	 */
	public void lifeCycleEvent(LifeCycleEvent arg0) {
		Gui.quitFrysk();
	}

	/* (non-Javadoc)
	 * @see org.gnu.gtk.event.LifeCycleListener#lifeCycleQuery(org.gnu.gtk.event.LifeCycleEvent)
	 */
	public boolean lifeCycleQuery(LifeCycleEvent arg0) {
		Gui.quitFrysk();
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
