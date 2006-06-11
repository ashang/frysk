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
package frysk.gui.monitor;

import java.io.IOException;
import java.util.prefs.Preferences;

import org.gnu.glade.LibGlade;

import frysk.gui.SessionManagerGui;
import frysk.gui.druid.CreateFryskSessionDruid;
import frysk.gui.register.RegisterWindow;


/**
 * @author sami wagiaalla
 * Singleton; one window manager.
 * Provies an instance to our major components so that they can
 * be manipulated from any where in the gui code.
 * Add your windows here.
 * */

public class WindowManager implements Saveable{

	public static WindowManager theManager = new WindowManager();
	
	/**
	 * Public instances of the windows
	 * {*/
	public MenuBar    menuBar;
	public MainWindow mainWindow;
	public LogWidget logWindow;
	public PreferencesWindow prefsWindow;
	public CustomObserverDialog customeObserverDialog;
	public ProgramAddWindow programAddWindow;
	public AboutWindow aboutWindow;
	public SplashScreenWindow splashScreen;
	public CreateFryskSessionDruid createFryskSessionDruid;
	public ObserversDialog observersDialog;
	public EditObserverDialog editObserverDialog; 
	public RegisterWindow registerWindow;
//	public PickProcsDialog pickProcsDialog;
	public MainWindowStatusBar mainWindowStatusBar;
	public SessionManagerGui sessionManager;
	/**}*/
	
	public WindowManager(){
		
	}
	
	// Eventually we want to move away from one big glade file
	// Renamed initWindows to below, as we migrate each window
	// from the massive glade file
	public void initLegacyProcpopWindows(LibGlade glade) throws IOException{
		this.splashScreen = new SplashScreenWindow();
		this.mainWindow = new MainWindow(glade);
		this.aboutWindow = new AboutWindow(glade);
		this.logWindow = new LogWidget(glade);
		this.prefsWindow = new PreferencesWindow(glade);
		this.customeObserverDialog = new CustomObserverDialog(glade);
		//this.programAddWindow = new ProgramAddWindow(glade);
		this.observersDialog = new ObserversDialog(glade);
		this.editObserverDialog = new EditObserverDialog(glade);
		this.menuBar = new MenuBar(glade);
		
//		this.pickProcsDialog = new PickProcsDialog(null);
		
		this.mainWindowStatusBar = new MainWindowStatusBar(glade);
	}
	

	public void initSessionManagerWindow(LibGlade glade)
	{
		this.sessionManager = new SessionManagerGui(glade);
	}
	public void initSessionDruidWindow(LibGlade session) throws IOException{
		this.createFryskSessionDruid = new CreateFryskSessionDruid(session);
	}

	public void initRegisterWindow(LibGlade gladeFile){
		this.registerWindow = new RegisterWindow(gladeFile);
	}

	public void save(Preferences prefs) {
		mainWindow.save(Preferences.userRoot().node(prefs.absolutePath() + "/mainWindow"));
		logWindow.save(Preferences.userRoot().node(prefs.absolutePath() + "/logWindow"));
		customeObserverDialog.save(Preferences.userRoot().node(prefs.absolutePath() + "/customObserverWindow"));
		registerWindow.save(Preferences.userRoot().node(prefs.absolutePath() + "/registers"));
		//programAddWindow.save(Preferences.userRoot().node(prefs.absolutePath() + "/programAddWindow"));
	}

	public void load(Preferences prefs) {
		mainWindow.load(Preferences.userRoot().node(prefs.absolutePath() + "/mainWindow"));
		logWindow.load(Preferences.userRoot().node(prefs.absolutePath() + "/logWindow"));
		customeObserverDialog.load(Preferences.userRoot().node(prefs.absolutePath() + "/customObserverWindow"));
		registerWindow.load(Preferences.userRoot().node(prefs.absolutePath() + "/registers"));
		//programAddWindow.load(Preferences.userRoot().node(prefs.absolutePath() + "/programAddWindow"));
	}
}
