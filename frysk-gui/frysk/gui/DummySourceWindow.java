package frysk.gui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
import org.gnu.gtk.Gtk;
import org.gnu.pango.Style;
import org.gnu.pango.Weight;

import frysk.Config;
import frysk.gui.common.IconManager;
import frysk.gui.common.Messages;
import frysk.gui.common.prefs.BooleanPreference;
import frysk.gui.common.prefs.ColorPreference;
import frysk.gui.common.prefs.IntPreference;
import frysk.gui.common.prefs.PreferenceGroup;
import frysk.gui.common.prefs.PreferenceManager;
import frysk.gui.monitor.FryskErrorFileHandler;
import frysk.gui.srcwin.SourceWindowFactory;
import frysk.gui.srcwin.prefs.SourceWinPreferenceGroup;
import frysk.gui.srcwin.prefs.SyntaxPreference;
import frysk.gui.srcwin.prefs.SyntaxPreferenceGroup;
import frysk.proc.DummyProc;
import frysk.proc.DummyTask;

public class DummySourceWindow {

	private static final String BASE_PATH = "frysk/gui/";
    private static final String GLADE_PKG_PATH = "glade/";
    private static final String SETTINGSFILE = ".settings";
    
    private static Logger errorLogFile = null;
    
    public static final String ERROR_LOG_ID = "frysk.gui.errorlog";
    
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
    	
    	PreferenceManager.addPreferenceGroup(PreferenceManager.syntaxHighlightingGroup);
    }
    
    private static Preferences importPreferences (String location){
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
    
	public static void DummySourceWin(String[] args, String[] gladep, String[] imaged,
			String[] messaged, String[] testfiled){
		
		Gtk.init(args);
		
		setupErrorLogging();
		
		IconManager.setImageDir(imaged);
		IconManager.loadIcons();
		IconManager.useSmallIcons();
			
		Messages.setBundlePaths(messaged);
			
		SourceWindowFactory.setTestFilesPath(testfiled);
		SourceWindowFactory.setGladePaths(gladep);
		
		Preferences prefs = importPreferences (Config.FRYSK_DIR + SETTINGSFILE);
		PreferenceManager.setPreferenceModel(prefs);
		initializePreferences();
		
		DummyProc proc = new DummyProc();
		DummyTask task = new DummyTask(proc);
		
		SourceWindowFactory.createSourceWindow(task);
		
		Gtk.main();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DummySourceWindow.DummySourceWin (args, new String[] {
			     GLADE_PKG_PATH,
			     BASE_PATH + GLADE_PKG_PATH,
			     // Check both relative ...
			     Build.SRCDIR + "/" + BASE_PATH + GLADE_PKG_PATH,
			     // ... and absolute.
			     Build.ABS_SRCDIR + "/" + BASE_PATH + GLADE_PKG_PATH,
			 },
			 new String[] {
			     Build.ABS_SRCDIR + "/" + BASE_PATH + "images/"
			 }, 
			 new String[] {
			     "./common", Build.SRCDIR + "/" + BASE_PATH + "common/",
			     Build.ABS_SRCDIR + "/" + BASE_PATH + "common/"
			 },
			 new String[] {
			     "./srcwin/testfiles", Build.SRCDIR + "/" + BASE_PATH + "srcwin/testfiles",
			     Build.ABS_SRCDIR + "/" + BASE_PATH + "srcwin/testfiles"
			 });
		
	}

}
