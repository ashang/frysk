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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom.Element;

import frysk.gui.Gui;
import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;

/**
 * A Session object is used to hold and save user preferences with respect to a
 * debug session.
 */
public class Session
    extends GuiObject
{

  private ObservableLinkedList procs;
  private ObservableLinkedList observers;
  
  private Logger errorLog = Logger.getLogger(Gui.ERROR_LOG_ID);
  
  /**
   * Creates a new empty session object, with an empty list processes. Debug
   * processes should be added to this session.
   */
  public Session ()
  {
    super();
    procs = new ObservableLinkedList();
    observers = new ObservableLinkedList();
    this.initListObservers();
    this.initDefaultObservers();
  }

  /**
   * Creates a new session, which is clone of the session that is passed in as a
   * paraemter
   * 
   * @param other - the session you want this session to clone.
   */
  public Session (final Session other)
  {
    super(other);

    procs = new ObservableLinkedList(other.procs);
    observers = new ObservableLinkedList(other.observers);
    this.initListObservers();
    this.initDefaultObservers();
  }

  /**
   * Creates a new empty session object, with an empty list processes. Debug
   * processes should be added to this session.
   * 
   * @param name - the name of the session
   * @param toolTip - the tool-tip or additional associative information.
   */
  public Session (final String name, final String toolTip)
  {
    super(name, toolTip);
    procs = new ObservableLinkedList();
    observers = new ObservableLinkedList();
    this.initListObservers();
    this.initDefaultObservers();
  }

  private void initDefaultObservers(){
    Iterator iterator = ObserverManager.theManager.getDefaultObservers().iterator();
    while (iterator.hasNext())
      {
        ObserverRoot observer = (ObserverRoot) iterator.next();
        this.observers.add(observer);
      }
  }
  
  private void initListObservers(){
    //Every time a process is added add all existing observers to it
    this.procs.itemAdded.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {
        addAllObservers((DebugProcess) object);
      }
    });
    
    //Every time a process is removed remove all existing observers from it
    this.procs.itemRemoved.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {
        removeAllObservers((DebugProcess) object);
      }
    });
    
    //Every time an observer is added add it to all existing processes
    this.observers.itemAdded.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {
        addObserverToAllProcs( (ObserverRoot) object);
      }
    });
    
    //Every time an observer is removed remove it from all existing processes
    this.observers.itemRemoved.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {
        removeObserverFromAllProcs((ObserverRoot) object);
      }
    });
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see frysk.gui.monitor.GuiObject#setName(java.lang.String)
   */
  public void setName (final String name)
  {
    super.setName(name);
  }

  public void addObserver(ObserverRoot observer){
    this.observers.add(observer);
  }
  
  public void removeObserver(ObserverRoot observer){
    this.observers.remove(observer);
  }
  
  private void addAllObservers(DebugProcess debugProcess){
    Iterator iterator = this.observers.iterator();
    while (iterator.hasNext())
      {
        ObserverRoot observer = (ObserverRoot) iterator.next();
        debugProcess.addObserver(observer);
      }
  }
  
  private void removeAllObservers(DebugProcess debugProcess){
    Iterator iterator = this.observers.iterator();
    while (iterator.hasNext())
      {
        ObserverRoot observer = (ObserverRoot) iterator.next();
        debugProcess.removeObserver(observer);
      }
  }

  private void addObserverToAllProcs(ObserverRoot observer){
    Iterator iterator = this.procs.iterator();
    while (iterator.hasNext())
      {
        DebugProcess debugProcess = (DebugProcess) iterator.next();
        debugProcess.addObserver(observer);
      }
  }
  
  private void removeObserverFromAllProcs(ObserverRoot observer){
    Iterator iterator = this.procs.iterator();
    while (iterator.hasNext())
      {
        DebugProcess debugProcess = (DebugProcess) iterator.next();
        debugProcess.removeObserver(observer);
      }
  }
  
  /**
   * Add a debug process to this session
   * 
   * @param process - The Debug Process that is to be added.
   */
  public void addProcess (final DebugProcess process)
  {
    procs.add(process);
  }

  /**
   * Remove a debug process from this session.
   * 
   * @param process - a reference to the debug process that is to be removed.
   */
  public void removeProcess (final DebugProcess process)
  {
    this.removeAllObservers(process);
  }

  /*
   * (non-Javadoc)
   * 
   * @see frysk.gui.monitor.GuiObject#getCopy()
   */
  public GuiObject getCopy ()
  {
    return new Session(this);
  }

  /**
   * Return a list of debug process that are contained within this session
   * object
   * 
   * @return ObservableLinkedList of Debug Processes.
   */
  public ObservableLinkedList getProcesses ()
  {
    return procs;
  }

  public void clearProcesses ()
  {
    this.procs.clear();
  }

  private void saveObservers(Element node){
    Iterator iterator = observers.iterator();
    while (iterator.hasNext())
      {
        GuiObject object = (GuiObject) iterator.next();
        Element elementXML = new Element("element");
        elementXML.setAttribute("name", object.getName());
        node.addContent(elementXML);
      }
  }
  
  private void loadObservers(Element node){
    List list = node.getChildren("element");
    Iterator i = list.iterator();

    while (i.hasNext())
      {
        Element elementXML = (Element) i.next();
        ObserverRoot observer = ObserverManager.theManager.getObserverCopy(ObserverManager.theManager.getObserverByName(elementXML.getAttributeValue("name")));
        if (observer == null)
          {
            errorLog.log(Level.SEVERE,
                         new Date()
                             + " DebugProcess.load(Element node): observer "
                             + elementXML.getAttributeValue("name")
                             + " not found in configuration \n");
          }else{
          observers.add(observer);
        }
      }
  }
  
  public void load (final Element node)
  {
    super.load(node);

    final Element procsXML = node.getChild("procs");
    procs.load(procsXML);
    
    final Element observersXML = node.getChild("observers");
    observers.clear();
    this.loadObservers(observersXML);
  }

  public void save (final Element node)
  {
    super.save(node);
    final Element procsXML = new Element("procs");
    procs.save(procsXML);
    node.addContent(procsXML);
    
    final Element observersXML = new Element("observers");
    this.saveObservers(observersXML);
    node.addContent(observersXML);
  }

  public ObservableLinkedList getObservers ()
  {
    return this.observers;
  }

}
