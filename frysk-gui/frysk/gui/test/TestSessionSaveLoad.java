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

import java.util.Iterator;

import org.gnu.gtk.Gtk;

import junit.framework.TestCase;

import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;
import frysk.gui.sessions.SessionManager;

public class TestSessionSaveLoad extends TestCase {
	
	public void testSaveLoad(){
		Gtk.init(new String[]{});
		
		SessionManager sessionManager = new SessionManager();
		Session mySavedSession = new Session("1", "2");
		DebugProcess debugProcess = new DebugProcess("3");
		
		Iterator iterator = ObserverManager.theManager.getTaskObservers().iterator();
		while (iterator.hasNext()) {
			ObserverRoot observer = (ObserverRoot) iterator.next();
			debugProcess.addObserver(observer);
		}
		mySavedSession.addProcess(debugProcess);
		sessionManager.addSession(mySavedSession);
		sessionManager.save();
		
		
		SessionManager loadedSessionManager = new SessionManager();
		Session myLoadedSession = loadedSessionManager.getSessionByName(mySavedSession.getName());
		
		assertNotNull("loaded session", myLoadedSession);
		assertEquals("session name", myLoadedSession.getName(), mySavedSession.getName());
		assertEquals("session tooltip", myLoadedSession.getToolTip(), mySavedSession.getToolTip());
		assertEquals("number of DebugProcessies", mySavedSession.getProcesses().size(), myLoadedSession.getProcesses().size());
		
		DebugProcess savedProc = (DebugProcess) mySavedSession.getProcesses().getFirst();
		DebugProcess loadedProc = (DebugProcess) myLoadedSession.getProcesses().getFirst();
		
		
		Iterator savedIter = savedProc.getObservers().iterator();
		Iterator loadedIter = loadedProc.getObservers().iterator();
		
		assertEquals("number of observers", loadedProc.getObservers().size(), savedProc.getObservers().size());
		
		while(savedIter.hasNext()){
			ObserverRoot savedObserver = (ObserverRoot) savedIter.next();
			ObserverRoot loadedObserver = (ObserverRoot) loadedIter.next();
			
			assertEquals("name of observer", savedObserver.getName(), loadedObserver.getName());
		}
		
		sessionManager.removeSession(mySavedSession);
	}
}
