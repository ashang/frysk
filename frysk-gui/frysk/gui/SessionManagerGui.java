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
import org.gnu.gtk.Dialog;
import org.gnu.gtk.FileChooserButton;
import org.gnu.gtk.RadioButton;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.CellRendererTextEvent;
import org.gnu.gtk.event.CellRendererTextListener;
import org.gnu.gtk.event.FileChooserEvent;
import org.gnu.gtk.event.FileChooserListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.ToggleEvent;
import org.gnu.gtk.event.ToggleListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;
import org.gnu.gtk.event.TreeViewEvent;
import org.gnu.gtk.event.TreeViewListener;

import frysk.gui.monitor.ListView;
import frysk.gui.monitor.WindowManager;
import frysk.gui.sessions.Session;
import frysk.gui.sessions.SessionManager;
import frysk.gui.srcwin.SourceWindowFactory;
import frysk.proc.Proc;

/**
 * @author pmuldoon
 *
 */
public class SessionManagerGui extends Dialog implements LifeCycleListener{
	
	private Button quitButton;
	private Session currentSession;
	
	ListView previousSessions;
	
	RadioButton previousSession;
	RadioButton debugExecutable;
	RadioButton debugSingleProcess;
	Button debugSingleProcessAction;
	
	FileChooserButton executableChooser;
	
	private Button editSession;
	private Button copySession;
	private Button deleteSession;
	private Button newSession;
	private Button openButton;

	public SessionManagerGui(LibGlade glade) 
	{
		super(glade.getWidget("SessionManager").getHandle());
		this.addListener(this);
		
		getManagerControls(glade);
		getSessionManagementControls(glade);
		getDebugExecutableControls(glade);
		getDebugSingleProcess(glade);
		setButtonStates();
	}
	
	private void toggleControls()
	{
		previousSessions.setSensitive(!previousSessions.getSensitive());
		editSession.setSensitive(!editSession.getSensitive());
		copySession.setSensitive(!copySession.getSensitive());
		deleteSession.setSensitive(!deleteSession.getSensitive());
		newSession.setSensitive(!newSession.getSensitive());
		executableChooser.setSensitive(!executableChooser.getSensitive());
	}

	private void getDebugSingleProcess(LibGlade glade) {
		debugSingleProcess = (RadioButton) glade.getWidget("SessionManager_debugSingleProcessButton");
		debugSingleProcess.setState(false);
		debugSingleProcessAction = (Button) glade.getWidget("SessionManager_singleProcessChooser");
		debugSingleProcessAction.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
				{
					WindowManager.theManager.pickProcDialog.showAll();
					WindowManager.theManager.pickProcDialog.run();
					Proc chosenProc = WindowManager.theManager.pickProcDialog.getChoosenProc();
					if (chosenProc != null)
						SourceWindowFactory.createSourceWindow(chosenProc.getMainTask());
				}
			}});
		
		SizeGroup labelGroup = new SizeGroup((SizeGroupMode.BOTH));
		labelGroup.addWidget(debugExecutable);
		labelGroup.addWidget(debugSingleProcess);
		
		SizeGroup chooserGroup = new SizeGroup((SizeGroupMode.BOTH));
		chooserGroup.addWidget(executableChooser);
		chooserGroup.addWidget(debugSingleProcessAction);
	}
	
	private void getDebugExecutableControls(LibGlade glade) {
		debugExecutable = (RadioButton) glade.getWidget("SessionManager_startSessionButton");
		debugExecutable.setState(false);
		executableChooser = (FileChooserButton) glade.getWidget("SessionManager_execChooser");
		executableChooser.addListener(new FileChooserListener() {

			public void currentFolderChanged(FileChooserEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void fileActivated(FileChooserEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void selectionChanged(FileChooserEvent arg0) {
					setButtonStates();
				
			}

			public void updatePreview(FileChooserEvent arg0) {
				// TODO Auto-generated method stub
				
			}});
		executableChooser.setSensitive(false);

	}
	
	private void setButtonStates()
	{
		if (debugExecutable.getState() == false)
		{
			if (executableChooser.getFilename()!= null)
				if (!executableChooser.getFilename().equals(""))
				{
					this.openButton.setSensitive(true);
					return;
				}
			this.openButton.setSensitive(false);
		}
		else
			if (previousSessions.getSelectedObject() !=null)
				this.openButton.setSensitive(true);
			else
				this.openButton.setSensitive(false);
	}

	private void getSessionManagementControls(LibGlade glade) {

		previousSessions = new ListView( glade.getWidget("SessionManager_previousSessionsListView").getHandle());
		previousSessions.watchLinkedList(SessionManager.theManager.getSessions());		
		previousSessions.addEditListener(new CellRendererTextListener() {
				public void cellRendererTextEvent(CellRendererTextEvent arg0) {
					if (arg0.getType() == CellRendererTextEvent.Type.EDITED)
					{
						Session selected = (Session) previousSessions.getSelectedObject();

						/* There may be nothing selected, and we may get a blank name string */
						if (selected != null && !arg0.getText().equals("")) {
							SessionManager.theManager.removeSession(selected);
							selected.setName(arg0.getText());
							SessionManager.theManager.addSession(selected);
							SessionManager.theManager.save();
						}
					}
				}
		});
		

		previousSessions.addListener( new TreeViewListener() {
			public void treeViewEvent(TreeViewEvent arg0) {
			      if (arg0.isOfType(TreeViewEvent.Type.ROW_ACTIVATED ))
			      {
			    	  WindowManager.theManager.mainWindow.showAll();
			    	  WindowManager.theManager.mainWindow.setSession(
			    			  (Session)previousSessions.getSelectedObject());
			    	  hideAll();  
			      }
			}});
		
		previousSessions.getSelection().addListener(new TreeSelectionListener() {
			public void selectionChangedEvent(TreeSelectionEvent arg0) {
				setButtonStates();
			}});

					

		
		editSession = (Button) glade.getWidget("SessionManager_editSessionButton");
		copySession = (Button) glade.getWidget("SessionManager_copySessionButton");
		deleteSession = (Button) glade.getWidget("SessionManager_deleteSessionButton");
		newSession = (Button) glade.getWidget("SessionManager_newSessionButton");

		newSession.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
				{
					WindowManager.theManager.createFryskSessionDruid.setNewSessionMode();
					WindowManager.theManager.createFryskSessionDruid.show();

				}
			}});
		
		previousSession = (RadioButton) glade.getWidget("SessionManager_startSessionButton");
		previousSession.setState(true);
		previousSession.addListener(new ToggleListener(){
			public void toggleEvent(ToggleEvent arg0) {
				if (arg0.getType() == ToggleEvent.Type.TOGGLED)
					toggleControls();
					setButtonStates();
			}});

		
		editSession.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
				{
					currentSession = (Session)previousSessions.getSelectedObject();
					if (currentSession != null) {
						WindowManager.theManager.createFryskSessionDruid.setEditSessionMode(currentSession);
						WindowManager.theManager.createFryskSessionDruid.show();
					}
	
				}
				
			}});
		
		copySession.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
				{
					Session selected = (Session)previousSessions.getSelectedObject();
					if (selected != null) {
						SessionManager.theManager.addSession(copySession(selected));
						SessionManager.theManager.save();
					}
				}
			}});
		
		deleteSession.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
				{
					Session selected = (Session)previousSessions.getSelectedObject();
					if (selected != null)
						SessionManager.theManager.removeSession(selected);
				}
			}});

	}

	private void getManagerControls(LibGlade glade) {
		
		this.quitButton = (Button) glade.getWidget("SessionManager_quitButton");
		this.quitButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
					System.exit(0);
			}});
		
		this.openButton = (Button) glade.getWidget("SessionManager_openButton");
		this.openButton.setSensitive(false);
		this.openButton.addListener(new ButtonListener() {
			public void buttonEvent(ButtonEvent arg0) {
				if (arg0.isOfType(ButtonEvent.Type.CLICK))
				{
					WindowManager.theManager.mainWindow.showAll();
					WindowManager.theManager.mainWindow.setSession(
							(Session)previousSessions.getSelectedObject());
					hideAll();
				}
			}});
		
	}
	
	private String getNumberSuffix(int i)
	{
		String iString = ""+i;
		if ((i <= 10) || (i >= 20))
			switch (iString.charAt(iString.length()-1)) {
				case '1': return "st";
				case '2': return "nd";
				case '3': return "rd";
				default: return "th";
			}	
		if ((i>=11) && (i<=19))
			return "th";		
		return "";
	}

	private Session copySession(Session source) {
		String session_name = source.getName();
		String name[] = { session_name + " (copy)",
				session_name + " (another copy)" };
		Session dest = (Session) source.getCopy();
		
		for (int i = 0; i < name.length; i++)
			if (SessionManager.theManager.getSessionByName(name[i]) == null) {
				dest.setName(name[i]);
				return dest;
			}
		for (int i = 3; i < Integer.MAX_VALUE - 1; i++)
			if (SessionManager.theManager.getSessionByName(session_name + " ("
				+ i + getNumberSuffix(i) + " copy)") == null) {
				
				dest.setName(session_name + " (" + i + getNumberSuffix(i) + " copy)");
				return dest;
			}

		try {
			dest.setName(session_name + "_"
					+ File.createTempFile("zxc", "dfg").getName());
		} catch (IOException e) {
		}
		return dest;
	}


	public void lifeCycleEvent(LifeCycleEvent event) {
		
	}

	public boolean lifeCycleQuery(LifeCycleEvent event) {
		if (event.isOfType(LifeCycleEvent.Type.DESTROY) || 
                event.isOfType(LifeCycleEvent.Type.DELETE)) {
					System.exit(0);
					return true;
		}
		return false;
	}

}
