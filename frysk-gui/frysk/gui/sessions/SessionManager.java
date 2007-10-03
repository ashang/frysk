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


package frysk.gui.sessions;

import java.io.File;
import java.util.Iterator;

import org.jdom.Element;

import frysk.Config;
import frysk.gui.monitor.GuiObservable;
import frysk.gui.monitor.ObjectFactory;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.UniqueHashMap;

/**
 * SessionManager is a simple singleton design pattern that keeps it own
 * static reference. Stores a list of sessions, can load and save those
 * sessions, and allows addition and deletion of sessions.
 */
public class SessionManager
{
  
  private static final File SESSIONS_DIR = new File(Config.getFryskDir() + "/Sessions" + "/");
  public static SessionManager theManager = new SessionManager(SESSIONS_DIR);

  ObservableLinkedList sessions = new ObservableLinkedList();
//  private Stack backupSessionStack;
  
  private Session currentSession;
  public final GuiObservable currentSessionChanged;
  
  private final UniqueHashMap nameHash = new UniqueHashMap();

  private File sessionsDir;

  public SessionManager (File sessionsDir)
  {
    sessionsDir.mkdirs();
    if(!sessionsDir.isDirectory()){
      throw new IllegalArgumentException("The given file ["+sessionsDir.getPath()+"] is not a directory");
    }
    this.sessionsDir = sessionsDir;
    this.currentSessionChanged = new GuiObservable();
    sessionsDir.mkdir();
    load();
  }

  /**
   * Add a session to the manager
   * 
   * @param session - the session to be added.
   */
  public void addSession (final Session session)
  {
    if(this.sessions.contains(session)){
      throw new IllegalArgumentException("You are adding a session which has already been added");
    }
    nameHash.add(session);
    sessions.add(session);
  }

  /**
   * Clear all sessions from the manager. Will not delete or alter the sessions
   * themselves.
   */
  public void clear ()
  {
    nameHash.clear();
    sessions.clear();
  }

  /**
   * Returns a session object via the name of the session
   * 
   * @param name - the name of the session to lookup
   * @return Session - the session found by the lookup.
   */
  public Session getSessionByName (final String name)
  {
    return (Session) nameHash.get(name);
  }

  /**
   * Returns a list of all sessions the manager has knowledge about.
   * 
   * @return ObservableLinkedList of sessions.
   */
  public ObservableLinkedList getSessions ()
  {
    return sessions;
  }

  /**
   * Determine if there already is a session in the manager via the name of that
   * session.
   * 
   * @param name - the name of that session.
   * @return boolean, true if found.
   */
  public boolean nameIsUsed (final String name)
  {
    return nameHash.nameIsUsed(name);
  }

  /**
   * Remove a session from the manager. This will delete the session from disk,
   * also.
   * 
   * @param session - the session object to remove.
   */
  public void removeSession (final Session session)
  {
    ObjectFactory.theFactory.deleteNode(new File(sessionsDir, session.getName()));
    nameHash.remove(session);
    sessions.remove(session);
  }

  public void renameSession(final Session session, String newName){
    if(this.sessions.contains(session)){
      this.removeSession(session);
      session.setName(newName);
      this.addSession(session);
      this.saveSession(session);
    }else{
      session.setName(newName);
    }
  }
  
  /**
   * Sets the session that is to be currently examined by frysk.
   * @param session
   */
  public void setCurrentSession(Session session){
    if(session == this.currentSession){
      return;
    }
    this.currentSession = session;
    this.currentSessionChanged.notifyObservers(currentSession);
  }
  
  /**
   * Returns the session currently being examined by Frysk.
   * @return
   */
  public Session getCurrentSession(){
    return currentSession;
  }
    
  private void saveSession(Session session){
    final Element node = new Element("Session");
    ObjectFactory.theFactory.saveObject(session, node);
    ObjectFactory.theFactory.exportNode(new File(sessionsDir,
                                        session.getName()), node);
  }
  
  public void save ()
  {
    final Iterator iterator = getSessions().iterator();
    while (iterator.hasNext())
      {
        final Session session = (Session) iterator.next();
        if (session.shouldSaveObject())
          {
            saveSession(session);
          }
      }
  }

  public void load ()
  {
    clear();
    Element node = new Element("Session");
    final File[] array = sessionsDir.listFiles();
    Session loadedSession = null;
    for (int i = 0; i < array.length; i++)
      {
        if (array[i].getName().startsWith("."))
          {
            continue;
          }
        try
          {
            node = ObjectFactory.theFactory.importNode(array[i]);
            loadedSession = (Session) ObjectFactory.theFactory.loadObject(node);
          }
        catch (final Exception e)
          {
            continue;
          }
        addSession(loadedSession);
      }

  }
  
}
