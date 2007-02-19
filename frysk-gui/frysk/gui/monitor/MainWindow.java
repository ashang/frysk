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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.lang.Throwable;

import org.gnu.glade.LibGlade;
import org.gnu.glib.CustomEvents;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.Window;
import org.gnu.gtk.Notebook;
import org.gnu.gdk.Color;
import org.gnu.gnomevte.Terminal;
import frysk.gui.Gui;
import frysk.gui.sessions.Session;

import frysk.proc.*;
import frysk.sys.PseudoTerminal;
import frysk.sys.Signal;
import frysk.sys.Sig;

public class MainWindow extends Window implements Saveable{
	
	//private ProcViewPage procViewPage;
	private SessionProcTreeView sessionProcTreeView;
  	private Notebook statusNotebook;

	//terminal things
	private ScrolledWindow terminalWidget;
	private Task shellTask = null;
	
	private Logger errorLog = Logger.getLogger (Gui.ERROR_LOG_ID);

	public MainWindow(LibGlade glade) throws IOException {
		super(((Window)glade.getWidget("procpopWindow")).getHandle()); //$NON-NLS-1$
		
		this.statusNotebook = (Notebook) glade.getWidget("statusNoteBook");
		this.terminalWidget = (ScrolledWindow) glade.getWidget("terminalScrolledWindow");

		try {
	//		this.procViewPage = new ProcViewPage(glade);
			this.sessionProcTreeView = new SessionProcTreeView(glade);
			
			//procViewPage.getClass();
			this.sessionProcTreeView.getClass();
		} catch (IOException e){
			errorLog.log(Level.SEVERE,"IOException from Proc Widget",e); //$NON-NLS-1$
		}
		
		TearOffNotebook noteBook = new TearOffNotebook((glade.getWidget("noteBook")).getHandle()); //$NON-NLS-1$
		//XXX:
		noteBook.getClass();
		noteBook.removePage(1);
		//this.showAll();
	}

	public void setSession(Session session){
		this.sessionProcTreeView.setSession(session);
	}
	
	public void save(Preferences prefs) {
		prefs.putInt("position.x", this.getPosition().getX()); //$NON-NLS-1$
		prefs.putInt("position.y", this.getPosition().getY()); //$NON-NLS-1$
		
		prefs.putInt("size.height", this.getSize().getHeight()); //$NON-NLS-1$
		prefs.putInt("size.width", this.getSize().getWidth()); //$NON-NLS-1$
		
//		procViewPage.save(Preferences.userRoot().node(prefs.absolutePath() + "/allProcWidget")); //$NON-NLS-1$
		sessionProcTreeView.save(Preferences.userRoot().node(prefs.absolutePath() + "/allProcWidget")); //$NON-NLS-1$
	}

	public void load(Preferences prefs) {
		int x = prefs.getInt("position.x", this.getPosition().getX()); //$NON-NLS-1$
		int y = prefs.getInt("position.y", this.getPosition().getY()); //$NON-NLS-1$
		if ((x >=0) && (y >=0))
			this.move(x,y);
		
		int width  = prefs.getInt("size.width", this.getSize().getWidth()); //$NON-NLS-1$
		int height = prefs.getInt("size.height", this.getSize().getHeight()); //$NON-NLS-1$
		
		if ((width > 0) && (height > 0))
			this.resize(width, height);
		
//		procViewPage.load(Preferences.userRoot().node(prefs.absolutePath() + "/allProcWidget")); //$NON-NLS-1$
		sessionProcTreeView.load(Preferences.userRoot().node(prefs.absolutePath() + "/allProcWidget")); //$NON-NLS-1$
	}
	
  	public void hideTerminal()
	{
		if (statusNotebook.getNumPages() == 2)
			statusNotebook.removePage(1);
	}
  	
  	public void buildTerminal()
  	{
		PseudoTerminal pty = new PseudoTerminal ();
		String name = pty.getFile ().getPath ();
	  	final Terminal term = new Terminal();
		
		//System.out.println("pty fd = " + pty.getFd() + "   name = " + pty.getName());
		Manager.host.requestCreateAttachedProc(name, name, name, new String[] {"/bin/sh"},
												new TaskObserver.Attached()
												{
													public Action updateAttached(Task task)
													{
														
														shellTask = task;
														final GuiProc shellProc = new GuiProc(task.getProc());
														shellProc.setName("Frysk Terminal Process");
                                                        CustomEvents.addEvent(new Runnable() {
                                                          GuiProc realShellProc = shellProc; 
                                                          public void run() {
                                                            sessionProcTreeView.procDataModel.addProc(realShellProc);
                                                          }
                                                        });
														return Action.CONTINUE;
													}
													public void addedTo(Object observable)
													{
													}
													public void addFailed(Object observable, Throwable w)
													{
													}
													public void deletedFrom(Object observable)
													{
													}
												});
		term.setPty(pty.getFd());
		

		term.setDefaultColors();
		term.setForegroundColor(Color.BLACK);
		term.setBackgroudColor(Color.WHITE);

		
		terminalWidget.addWithViewport(term);
		term.showAll();
		terminalWidget.showAll();  	
	}

	public void killTerminalShell()
	{
		if (shellTask != null)
			try {
				Signal.kill(shellTask.getTid(), Sig.HUP);
			} catch (Exception e) {
				errorLog.log(Level.WARNING, "Could not kill process" +  shellTask.getTid(),e);
			}
	}
}

