// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
// type filter text
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

package frysk.gui.test;

import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.TestCase;

import org.gnu.glade.GladeXMLException;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Gtk;

import frysk.gui.common.IconManager;
import frysk.gui.monitor.WindowManager;
import frysk.gui.Build;

public class TestGlade extends TestCase {

	// Not very pretty but statically note the glade files of interest
	private static final String GLADE_FILE = "procpop.glade";

	private static final String CREATE_SESSION_GLADE = "frysk_create_session_druid.glade";

	private static final String SESSION_MANAGER_GLADE = "frysk_session_manager.glade";

	private static final String MEMORY_WINDOW = "memorywindow.glade";

	private static final String REGISTER_WINDOW = "registerwindow.glade";

	// Need to store them, so we can inspect them later.
	LibGlade glade = null;

	LibGlade create_session_glade = null;

	LibGlade register_window = null;

	LibGlade memory_window = null;

	LibGlade session_glade = null;

	// Set up locations to look for them
	private static final String BASE_PATH = "frysk/gui/";

	private static final String GLADE_PKG_PATH = "glade/";

	private String[] glade_dirs = new String[] { GLADE_PKG_PATH,
			BASE_PATH + GLADE_PKG_PATH,
			// Check both relative ...
			Build.SRCDIR + "/" + BASE_PATH + GLADE_PKG_PATH,
			// ... and absolute.
			Build.ABS_SRCDIR + "/" + BASE_PATH + GLADE_PKG_PATH, };

    String[] imagePaths = new String[] { Build.ABS_SRCDIR + "/" + BASE_PATH
                                        + "images/" };

	public void setUp() {
		Gtk.init(new String[] {});
		
	}

	// Test the actual loading of the glade files
	public void testGladeLoading() {


		// The location of the glade file may need to be modified
		// here, depending on where the program is being run from. If
		// the directory that the src directory is in is used as the
		// root, this should work without modification
		String properGladeDir = null;
		boolean loadedFiles = false;
		for (int i = 0; i < glade_dirs.length; i++) {
			try {// command line glade_dir
				glade = new LibGlade(glade_dirs[i] + GLADE_FILE, this);
				create_session_glade = new LibGlade(glade_dirs[i]
						+ CREATE_SESSION_GLADE, this);
				register_window = new LibGlade(glade_dirs[i] + REGISTER_WINDOW,
						null);
				memory_window = new LibGlade(glade_dirs[i] + MEMORY_WINDOW,
						null);
				session_glade = new LibGlade(glade_dirs[i]
						+ SESSION_MANAGER_GLADE, this);
				loadedFiles = true;
			} catch (FileNotFoundException missingFile) {
				// we have to iterate through several paths, so a fail here is not 
				// big deal.
				continue;
			} catch (GladeXMLException exception) {
				// a fail here is a big deal however.
				fail(exception.getMessage());
			} catch (IOException ioException) {
				// ... and here.
				fail(ioException.getMessage());
			}

			// If the loadedFiles check was set, store the path to 
			// the real glade location.
			if (loadedFiles) {
				properGladeDir = glade_dirs[i];
				break;
			}
		}

		// Run some basic checks
		assertNotNull("Check GLADE directory found", properGladeDir);
		assertEquals("Check files were loaded ", true, loadedFiles);

		// Checking that the LibGlade's variables are not empty. If they are
		// then bad things happened during load.
		assertNotNull("Checking that " + GLADE_FILE + " loaded.", glade);
		assertNotNull("Checking that " + CREATE_SESSION_GLADE + " loaded.",
				create_session_glade);
		assertNotNull("Checking that " + MEMORY_WINDOW + " loaded.",
				memory_window);
		assertNotNull("Checking that " + REGISTER_WINDOW + " loaded.",
				register_window);
		assertNotNull("Checking that " + SESSION_MANAGER_GLADE + " loaded.",
				session_glade);
		
		// Look for Procpop's Windows
		assertNotNull("Test glade is null", glade);
		assertNotNull("Testing procpop window existence", glade
				.getWidget("procpopWindow"));
		assertNotNull("Testing about dialog window existence", glade
				.getWidget("fryskAboutDialog"));
		assertNotNull("Testing about observers window existence", glade
				.getWidget("observersDialog"));
		assertNotNull("Testing about edit observers window existence", glade
				.getWidget("editObserverDialog"));

		// Look for SessionManager's Windows
		assertNotNull("Testing session manager window existence", session_glade
				.getWidget("SessionManager"));

		// Look for Assistant's Windows
		assertNotNull("Testing session assitant window existence",
				create_session_glade.getWidget("SessionDruid"));

		// Look for Register's Windows
		assertNotNull("Testing register window existence", register_window
				.getWidget("registerWindow"));
		assertNotNull("Testing register format window existence",
				register_window.getWidget("formatDialog"));

		// Look for Memory's Windows
		assertNotNull("Testing memory window existence", memory_window
				.getWidget("memoryWindow"));
		assertNotNull("Testing memory format window existence", memory_window
				.getWidget("formatDialog"));
		
		
		// TestWindowManager
        IconManager.setImageDir(imagePaths);
        IconManager.loadIcons();
        IconManager.useSmallIcons();
        Object defaultSet = IconManager.getFactory();
        assertNotNull("Testing getFactory", defaultSet);
        try
          {
            WindowManager.theManager.initLegacyProcpopWindows(glade);
            WindowManager.theManager.initSessionDruidWindow(create_session_glade);
            WindowManager.theManager.initSessionManagerWindow(session_glade);
          }
        catch (Exception e)
          {
            e.printStackTrace();
          }

        assertNotNull("menuBar",WindowManager.theManager.menuBar);
		assertNotNull("mainWindow",WindowManager.theManager.mainWindow);
		assertNotNull("logWindow",WindowManager.theManager.logWindow);
		assertNotNull("prefsWindow",WindowManager.theManager.prefsWindow);
		assertNotNull("aboutWindow",WindowManager.theManager.aboutWindow);
		assertNotNull("splashScreen",WindowManager.theManager.splashScreen);
		assertNotNull("createFryskSessionDruid",WindowManager.theManager.createFryskSessionDruid);
		assertNotNull("observersDialog",WindowManager.theManager.observersDialog);
		assertNotNull("editObserverDialog",WindowManager.theManager.editObserverDialog);
		assertNotNull("pickProcDialog",WindowManager.theManager.pickProcDialog);
		assertNotNull("mainWindowStatusBar",WindowManager.theManager.mainWindowStatusBar);
		assertNotNull("sessionManager",WindowManager.theManager.sessionManager);
         
	}

}
