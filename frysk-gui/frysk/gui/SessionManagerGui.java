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

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.RadioButton;
import org.gnu.gtk.ResponseType;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.ToggleEvent;
import org.gnu.gtk.event.ToggleListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;
import org.gnu.gtk.event.TreeViewEvent;
import org.gnu.gtk.event.TreeViewListener;

import frysk.gui.common.IconManager;
import frysk.gui.common.Util;
import frysk.gui.monitor.ListView;
import frysk.gui.monitor.WindowManager;
import frysk.gui.sessions.Session;
import frysk.gui.sessions.SessionManager;
import frysk.gui.srcwin.SourceWindowFactory;
import frysk.proc.Proc;

/**
 * @author pmuldoon
 * 
 * SessionManagerGui - Manager all entry workflows into the UI
 * 
 * 
 */
public class SessionManagerGui
    extends org.gnu.gtk.Dialog
    implements LifeCycleListener
{

  private Button quitButton;

  private Session currentSession;

  ListView previousSessions;

  RadioButton terminalSession;

  RadioButton previousSession;

  RadioButton debugSingleProcess;

  Button debugSingleProcessAction;

  private Button editSession;

  private Button copySession;

  private Button deleteSession;

  private Button newSession;

  private Button openButton;

  public SessionManagerGui (LibGlade glade)
  {
    super(glade.getWidget("SessionManager").getHandle());
    this.addListener(this);
    this.setIcon(IconManager.windowIcon);

    getManagerControls(glade);
    getSessionManagementControls(glade);
    // getDebugExecutableControls(glade);
    getDebugSingleProcess(glade);
	getTerminalSession(glade);
    setButtonStates();
  }

  private void getDebugSingleProcess (LibGlade glade)
  {
    debugSingleProcess = (RadioButton) glade.getWidget("SessionManager_debugSingleProcessButton");
    debugSingleProcess.setState(false);
	debugSingleProcess.addListener(new ToggleListener()
    {
      public void toggleEvent (ToggleEvent arg0)
      {
        setButtonStates();
      }
    });

    debugSingleProcessAction = (Button) glade.getWidget("SessionManager_singleProcessChooser");
    debugSingleProcessAction.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          {
            WindowManager.theManager.pickProcDialog.showAll();
            int response = WindowManager.theManager.pickProcDialog.run();
            Proc chosenProc = WindowManager.theManager.pickProcDialog.getChoosenProc();
            if (response == ResponseType.OK.getValue())
              if (chosenProc != null)
                SourceWindowFactory.createSourceWindow(chosenProc.getMainTask());
          }
      }
    });

  }

  private void getTerminalSession(LibGlade glade)
  {
  	terminalSession = (RadioButton) glade.getWidget("SessionManager_startTerminalSessionButton");
	terminalSession.setState(false);
   	terminalSession.addListener(new ToggleListener()
    {
      public void toggleEvent (ToggleEvent arg0)
      {
        setButtonStates();
      }
    });

  }

  private void setButtonStates ()
  {
    if (previousSession.getState())
	{
		previousSessions.setSensitive(true);
       	this.newSession.setSensitive(true);
      if ((previousSessions.getSelectedObject() == null))
        {
          this.editSession.setSensitive(false);
          this.copySession.setSensitive(false);
          this.deleteSession.setSensitive(false);
        }
      else
        {
          this.editSession.setSensitive(true);
          this.copySession.setSensitive(true);
          this.deleteSession.setSensitive(true);
        }
	}
	else
	{
		previousSessions.setSensitive(false);
        this.editSession.setSensitive(false);
        this.copySession.setSensitive(false);
        this.deleteSession.setSensitive(false);
       	this.newSession.setSensitive(false);
	}

    if ((previousSessions.getSelectedObject() != null
        && previousSession.getState())
		|| terminalSession.getState())
      this.openButton.setSensitive(true);
    else
      this.openButton.setSensitive(false);

	if (debugSingleProcess.getState())
    	debugSingleProcessAction.setSensitive(true);
	else
    	debugSingleProcessAction.setSensitive(false);
  }

  private void getSessionManagementControls (LibGlade glade)
  {

    previousSessions = new ListView(
                                    glade.getWidget(
                                                    "SessionManager_previousSessionsListView").getHandle());
    previousSessions.watchLinkedList(SessionManager.theManager.getSessions());

    previousSessions.addListener(new TreeViewListener()
    {
      public void treeViewEvent (TreeViewEvent arg0)
      {
        if (arg0.isOfType(TreeViewEvent.Type.ROW_ACTIVATED))
          {
            WindowManager.theManager.mainWindow.showAll();
            WindowManager.theManager.mainWindow.setSession((Session) previousSessions.getSelectedObject());
            hideAll();
          }
      }
    });

    previousSessions.getSelection().addListener(new TreeSelectionListener()
    {
      public void selectionChangedEvent (TreeSelectionEvent arg0)
      {
        setButtonStates();
      }
    });

    previousSessions.setStickySelect(true);
    previousSessions.setSort();

    editSession = (Button) glade.getWidget("SessionManager_editSessionButton");
    copySession = (Button) glade.getWidget("SessionManager_copySessionButton");
    deleteSession = (Button) glade.getWidget("SessionManager_deleteSessionButton");
    newSession = (Button) glade.getWidget("SessionManager_newSessionButton");

    newSession.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          {
            WindowManager.theManager.createFryskSessionDruid.setNewSessionMode();
            WindowManager.theManager.createFryskSessionDruid.show();

          }
      }
    });

    previousSession = (RadioButton) glade.getWidget("SessionManager_startSessionButton");
    previousSession.setState(true);
    previousSession.addListener(new ToggleListener()
    {
      public void toggleEvent (ToggleEvent arg0)
      {
        setButtonStates();
      }
    });

    editSession.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          {
            currentSession = (Session) previousSessions.getSelectedObject();
            if (currentSession != null)
              {
                WindowManager.theManager.createFryskSessionDruid.setEditSessionMode(currentSession);
                WindowManager.theManager.createFryskSessionDruid.show();
              }

          }

      }
    });

    copySession.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          {
            Session selected = (Session) previousSessions.getSelectedObject();
            if (selected != null)
              {
                SessionManager.theManager.addSession(copySession(selected));
                SessionManager.theManager.save();
              }
          }
      }
    });

    deleteSession.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          {
            Session selected = (Session) previousSessions.getSelectedObject();
            if (selected != null)
              SessionManager.theManager.removeSession(selected);
          }
      }
    });

  }

  private void getManagerControls (LibGlade glade)
  {

    this.quitButton = (Button) glade.getWidget("SessionManager_quitButton");
    this.quitButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          Gtk.mainQuit();
      }
    });

    this.openButton = (Button) glade.getWidget("SessionManager_openButton");
    this.openButton.setSensitive(false);
    this.openButton.addListener(new ButtonListener()
    {
      public void buttonEvent (ButtonEvent arg0)
      {
        if (arg0.isOfType(ButtonEvent.Type.CLICK))
          {
            WindowManager.theManager.mainWindow.showAll();
			if (previousSession.getState())
			{
            	WindowManager.theManager.mainWindow.setSession((Session) previousSessions.getSelectedObject());
				WindowManager.theManager.mainWindow.hideTerminal();
			}
			if (terminalSession.getState())
			{
				WindowManager.theManager.mainWindow.buildTerminal();
			}
            hideAll();
          }
      }
    });

  }

 private Session copySession (Session source)
  {
    String session_name = source.getName();
    String name[] = { session_name + " (copy)",
                     session_name + " (another copy)" };
    Session dest = (Session) source.getCopy();

    for (int i = 0; i < name.length; i++)
      if (SessionManager.theManager.getSessionByName(name[i]) == null)
        {
          dest.setName(name[i]);
          return dest;
        }
    for (int i = 3; i < Integer.MAX_VALUE - 1; i++)
      if (SessionManager.theManager.getSessionByName(session_name + " (" + i
                                                     + Util.getNumberSuffix(i)
                                                     + " copy)") == null)
        {

          dest.setName(session_name + " (" + i + Util.getNumberSuffix(i) + " copy)");
          return dest;
        }

    try
      {
        dest.setName(session_name + "_"
                     + File.createTempFile("zxc", "dfg").getName());
      }
    catch (IOException e)
      {
      }
    return dest;
  }

  public void lifeCycleEvent (LifeCycleEvent event)
  {

  }

  public boolean lifeCycleQuery (LifeCycleEvent event)
  {
    if (event.isOfType(LifeCycleEvent.Type.DESTROY)
        || event.isOfType(LifeCycleEvent.Type.DELETE))
      {
        Gtk.mainQuit();
        return true;
      }
    return false;
  }

}
