// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

package frysk.gui;

import java.io.FileNotFoundException;
import java.io.IOException;

import frysk.Config;
import frysk.junit.TestCase;

import org.gnu.glade.GladeXMLException;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Gtk;

import frysk.gui.common.IconManager;
import frysk.gui.monitor.WindowManager;

public class TestGlade
    extends TestCase
{

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

    private final String[] imagePaths = new String[] {
	Config.getImageDir ()
    };

    public void setUp ()
    {
	Gtk.init(new String[] {});
    }

    // Test the actual loading of the glade files
    public void testGladeLoading ()
	throws FileNotFoundException, GladeXMLException, IOException
    {
	// The location of the glade file may need to be modified
	// here, depending on where the program is being run from. If
	// the directory that the src directory is in is used as the
	// root, this should work without modification
	String GLADE_PREFIX = Config.getGladeDir ();

	LibGlade glade = new LibGlade (GLADE_PREFIX + GLADE_FILE, this);
	LibGlade create_session_glade = new LibGlade(GLADE_PREFIX + CREATE_SESSION_GLADE, this);
	LibGlade register_window = new LibGlade(GLADE_PREFIX + REGISTER_WINDOW, null);
	LibGlade memory_window = new LibGlade(GLADE_PREFIX + MEMORY_WINDOW, null);
	LibGlade session_glade = new LibGlade(GLADE_PREFIX + SESSION_MANAGER_GLADE, this);

	// Checking that the LibGlade's variables are not empty. If they are
	// then bad things happened during load.
	assertNotNull(GLADE_FILE, glade);
	assertNotNull(CREATE_SESSION_GLADE, create_session_glade);
	assertNotNull(MEMORY_WINDOW, memory_window);
	assertNotNull(REGISTER_WINDOW, register_window);
	assertNotNull(SESSION_MANAGER_GLADE, session_glade);
		
	// Look for Procpop's Windows
	String[] gladeWidget = {
	    "procpopWindow",
	    "fryskAboutDialog",
	    "observersDialog",
	    "editObserverDialog"
	};
	for (int i = 0; i < gladeWidget.length; i++) {
	    assertNotNull (gladeWidget[i],
			   glade.getWidget (gladeWidget[i]));
	}

	// Look for SessionManager's Windows
	assertNotNull("Testing session manager window existence",
		      session_glade.getWidget("SessionManager"));

	// Look for Assistant's Windows
	assertNotNull("Testing session assitant window existence",
		      create_session_glade.getWidget("SessionDruid"));

	// Look for Register's Windows
	assertNotNull("Testing register window existence",
		      register_window.getWidget("registerWindow"));
	assertNotNull("Testing register format window existence",
		      register_window.getWidget("formatDialog"));

	// Look for Memory's Windows
	assertNotNull("Testing memory window existence",
		      memory_window.getWidget("memoryWindow"));
	assertNotNull("Testing memory format window existence",
		      memory_window.getWidget("formatDialog"));
		
		
	// TestWindowManager
        IconManager.setImageDir(imagePaths);
        IconManager.loadIcons();
        IconManager.useSmallIcons();
        Object defaultSet = IconManager.getFactory();
        assertNotNull("getFactory", defaultSet);
	WindowManager.theManager.initLegacyProcpopWindows(glade);
	WindowManager.theManager.initSessionDruidWindow(create_session_glade);
	WindowManager.theManager.initSessionManagerWindow(session_glade);

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
