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

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.RadioButton;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;
import org.gnu.gtk.event.TreeViewEvent;
import org.gnu.gtk.event.TreeViewListener;

import frysk.gui.common.IconManager;
import frysk.gui.common.Util;
import frysk.gui.FryskHelpManager;
import frysk.gui.monitor.ListView;
import frysk.gui.monitor.WindowManager;
import frysk.gui.sessions.Session;
import frysk.gui.sessions.SessionManager;

/**
 * 
 * SessionManagerGui - Manage all entry work-flows into the UI
 * 
 */
public class SessionManagerDialog extends org.gnu.gtk.Dialog implements
		LifeCycleListener {

	private Button quitButton;

	private Session currentSession;

	ListView previousSessions;

	RadioButton terminalSession;

	private Button editSession;

	private Button copySession;

	private Button deleteSession;

	private Button newSession;

	private Button openButton;
    
    private Button helpButton;
	
	private LinkedList popupControl = new LinkedList();
	
	private boolean sessionLaunched = false;

	/**
	 * Session Manager UI entry point. Read the glade files,
	 * setup the controls, and setup edit data if needed.
	 * 
	 * @param glade - glade file for the session manager.
	 */
	public SessionManagerDialog(LibGlade glade) {
		super(glade.getWidget("SessionManager").getHandle());
		this.addListener(this);
		setIcon(IconManager.windowIcon);

		getManagerControls(glade);
		getSessionManagementControls(glade);
		setButtonStates();
		
		popupControl.add(this);
		IconManager.trayIcon.setPopupWindows(popupControl);
	}

	/**
	 * Sets the button states depending on the state of the 
	 * Manager, and what options are set in that manager
	 * 
	 */
	private void setButtonStates() {
		
			// Set the treeview to be interactive
			previousSessions.setSensitive(true);
			
			// Set the new session button to be interactive
			newSession.setSensitive(true);
			
			// If there are no previous saved sessions, then
			// disable the edit, copy and delete buttons.
	if (previousSessions.getSelectedObject() == null)
      {
        editSession.setSensitive(false);
        copySession.setSensitive(false);
        deleteSession.setSensitive(false);
      }
    else
      {
        // If there are previous sessions, enable them.
        editSession.setSensitive(true);
        copySession.setSensitive(true);
        deleteSession.setSensitive(true);
      }

		// Set whether the open button should be interactive.
    if (previousSessions.getSelectedObject() != null)
      {
        openButton.setSensitive(true);
      }
    else
      {
        openButton.setSensitive(false);
      }
	}

	/**
	 * Retrieves the glade defined controls from the 
	 * glade file, and build the session management controls
	 * and listeners.
	 * 
	 * @param glade - - the glade file for the Session Manager.
	 */
	private void getSessionManagementControls(LibGlade glade) {
		
		// Previous Session (ie saved) List View
		previousSessions = new ListView(glade.getWidget(
				"SessionManager_previousSessionsListView").getHandle());
		previousSessions.watchLinkedList(SessionManager.theManager
				.getSessions());

		// Double click to launch a session
		previousSessions.addListener(new TreeViewListener() {
			public void treeViewEvent(TreeViewEvent arg0) {
				if (arg0.isOfType(TreeViewEvent.Type.ROW_ACTIVATED)) {
					openSession();
				}
			}
		});

		// If we change our selection in the listview, change the button states
		// to match usable states.
		previousSessions.getSelection().addListener(
				new TreeSelectionListener() {
					public void selectionChangedEvent(TreeSelectionEvent arg0) {
						setButtonStates();
					}
				});

		previousSessions.setStickySelect(true);
		previousSessions.setSort();

		// Get the edit, copy, delete and new sessions buttons
		editSession = (Button) glade
				.getWidget("SessionManager_editSessionButton");
		copySession = (Button) glade
				.getWidget("SessionManager_copySessionButton");
		deleteSession = (Button) glade
				.getWidget("SessionManager_deleteSessionButton");
		newSession = (Button) glade
				.getWidget("SessionManager_newSessionButton");

		// New Session Button will launch the new session druid.
		// Set "New Session Mode" in the druid, and open.
		newSession.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK)) {
					WindowManager.theManager.createFryskSessionDruid
							.setNewSessionMode();
					WindowManager.theManager.createFryskSessionDruid.show();

				}
			}
		});

		// Edit Session Button will launch the new session druid.
		// Set "Edit Session Mode" in the druid, and open.
		editSession.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK)) {
					currentSession = (Session) previousSessions
							.getSelectedObject();
					if (currentSession != null) {
						WindowManager.theManager.createFryskSessionDruid
								.setEditSessionMode(currentSession);
						WindowManager.theManager.createFryskSessionDruid.show();
					}

				}

			}
		});

		// Copy Session. Will copy the selected session
		// and renamed the copied session according to
		// nautilus style renaming.
		copySession.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK)) {
					final Session selected = (Session) previousSessions
							.getSelectedObject();
					if (selected != null) {
						SessionManager.theManager
								.addSession(copySession(selected));
						SessionManager.theManager.save();
					}
				}
			}
		});

		// Delete Session Button will delete the session
		// from disk and from the session manager.
		deleteSession.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK)) {
					final Session selected = (Session) previousSessions
							.getSelectedObject();
					if (selected != null) {
					  SessionManager.theManager.removeSession(selected);
					}
				}
			}
		});

	}

	/**
	 * Retrieves the glade defined controls from the 
	 * glade file, and build the session manager controls
	 * and listeners.
	 * 
	 * @param glade - - the glade file for the Session Manager.
	 */
	private void getManagerControls(LibGlade glade) {

		quitButton = (Button) glade.getWidget("SessionManager_quitButton");
		quitButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK)) {
					Gui.quitFrysk();
				}
			}
		});

		openButton = (Button) glade.getWidget("SessionManager_openButton");
		openButton.setSensitive(false);
		openButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK)) {
					openSession();
				}
			}
		});

        helpButton = (Button) glade.getWidget("SessionManager_helpButton");
        helpButton.setSensitive(true);
        helpButton.addListener(new ButtonListener() {
            public void buttonEvent(ButtonEvent arg0) {
                if (arg0.isOfType(ButtonEvent.Type.CLICK)) {
                    helpSession();
                }
            }
        });
	}

	/**
	 * Utility method that takes a session as an argument,
	 * generates a unique name based on the nautilus naming 
	 * method for copies, and returns a clone of that session
	 * with the new name.
	 * 
	 * @param source - Session to copy.
	 * @return - Session, cloned session with new name.
	 */
	private Session copySession(Session source) {
		final String session_name = source.getName();
		
		final String name[] = { session_name + " (copy)",
				session_name + " (another copy)" };
		final Session dest = (Session) source.getCopy();

		// Try {name} + (copy)
		// if that does not work, try {name} + (another copy)
		for (int i = 0; i < name.length; i++) {
			if (SessionManager.theManager.getSessionByName(name[i]) == null) {
				dest.setName(name[i]);
				return dest;
			}
		}
		
		// If neither of above work, try {name}+" "int+{st/nd/rd/th}
		for (int i = 3; i < Integer.MAX_VALUE - 1; i++) {
			if (SessionManager.theManager.getSessionByName(session_name + " ("
					+ i + Util.getNumberSuffix(i) + " copy)") == null) {

				dest.setName(session_name + " (" + i + Util.getNumberSuffix(i)
						+ " copy)");
				return dest;
			}
		}

		// last chance, just create a random name
		try {
			dest.setName(session_name + "_"
					+ File.createTempFile("zxc", "dfg").getName());
		} catch (final IOException e) {
		}
		return dest;
	}

	// Empty implementation of implementing LifeCycleListener
	// we do not care what happens here.
	
	/* (non-Javadoc)
	 * @see org.gnu.gtk.event.LifeCycleListener#lifeCycleEvent(org.gnu.gtk.event.LifeCycleEvent)
	 */
	public void lifeCycleEvent(LifeCycleEvent event) {

	}

	/* (non-Javadoc)
	 * @see org.gnu.gtk.event.LifeCycleListener#lifeCycleQuery(org.gnu.gtk.event.LifeCycleEvent)
	 */
	public boolean lifeCycleQuery(LifeCycleEvent event) {
		if (event.isOfType(LifeCycleEvent.Type.DESTROY)
				|| event.isOfType(LifeCycleEvent.Type.DELETE)) {
			Gui.quitFrysk();
			return true;
		}
		return false;
	}
	
	/**
	 * Returns whether there is a session currently running
	 * 
	 * @return - boolea, is there a session running?
	 */
	public boolean isSessionLaunched() {
		return this.sessionLaunched;
	}

	/**
   * Open a session. When the open button is clicked, run through the druid. The user will
   * then choose which specific processes they want in this session, and which observers
   * to use as well.	
   */
  public void openSession ()
  {
    final Session s = (Session) previousSessions.getSelectedObject();
    
    sessionLaunched = true;
    WindowManager.theManager.createFryskSessionDruid.loadSessionMode(s);
    WindowManager.theManager.createFryskSessionDruid.show();
  }
    
    /**
     * Start the Frysk help system.
     */
    public void helpSession() {

        FryskHelpManager.activateHelp();
        
    }

}
