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

import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.jdom.Element;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.GuiProc;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.TaskObserverRoot;
import frysk.gui.srcwin.tags.Tagset;
import frysk.gui.srcwin.tags.TagsetManager;

/**
 * A container that refers to an executable there
 * could be zero or many instances of these executable. This keeps track
 *  of those too.
 */

public class DebugProcess
    extends GuiObject
{

  private String executablePath;

  ObservableLinkedList procs;

  ObservableLinkedList observers;

  ObservableLinkedList tagsets;

//  ObservableLinkedList allProcsList;

  String alternativeDisplayName = "";

  WatchList watchedVars;
  
  private String realName;

//  private Logger errorLog = Logger.getLogger(Gui.ERROR_LOG_ID);

  /**
   * Create a new Debug Process. A session is comprised of n debug processes. A
   * debug process stores a process name, and what observer to apply to that
   * process when a session is loaded.
   */
  public DebugProcess ()
  {
    super();

    procs = new ObservableLinkedList();

    observers = new ObservableLinkedList();
    tagsets = new ObservableLinkedList();

    watchedVars = new WatchList();
    
    initListObservers();
  }

  /**
   * Create a new Debug Process. A session is comprised of n debug processes. A
   * debug process stores a process name, and what observer to apply to that
   * process when a session is loaded.
   * 
   * @param other - Create a debug process from the the given parameter. Used in
   *          copying a debug process.
   */
  public DebugProcess (DebugProcess other)
  {
    super(other);

    realName = other.realName;
    alternativeDisplayName = other.alternativeDisplayName;
    setExecutablePath(other.getExecutablePath());

    procs = new ObservableLinkedList();

    observers = new ObservableLinkedList(other.observers, true);
    tagsets = new ObservableLinkedList(other.tagsets, true);

    watchedVars = new WatchList(other.watchedVars);
    
    initListObservers();
  }

  /**
   * * Create a new Debug Process. A session is comprised of n debug processes.
   * A debug process stores a process name, and what observer to apply to that
   * process when a session is loaded.
   * 
   * @param name - The name of the process
   * @param altName - The display name of the process
   * @param executablePath - Where the process can be found on disk
   */
  public DebugProcess (String name, String altName, String executablePath)
  {
    super(altName, altName);

    realName = name;
    alternativeDisplayName = altName;
    this.setExecutablePath(executablePath);

    procs = new ObservableLinkedList();

    observers = new ObservableLinkedList();
    tagsets = new ObservableLinkedList();

    watchedVars = new WatchList();
    
    initListObservers();
  }

  private void initListObservers(){
    //Every time a process is added add all existing observers to it
    this.procs.itemAdded.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {
	addAllObservers((GuiProc) object);
      }
    });
    
    //Every time a process is removed remove all existing observers from it
    this.procs.itemRemoved.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {
        removeAllObservers((GuiProc) object);
      }
    });
    
    //Every time an observer is added add it to all existing processes
    this.observers.itemAdded.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {
	addObserverToAllProcs( (TaskObserverRoot) object);
      }
    });
    
    //Every time an observer is removed remove it from all existing processes
    this.observers.itemRemoved.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {
        removeObserverFromAllProcs((TaskObserverRoot) object);
      }
    });
  }
  
  private void addAllObservers(GuiProc guiProc){
    Iterator iterator = observers.iterator();
    while (iterator.hasNext())
      {
	TaskObserverRoot observer = (TaskObserverRoot) iterator.next();
        observer.apply(guiProc.getProc());
      }
  }
  
  private void removeAllObservers(GuiProc guiProc){
    Iterator iterator = observers.iterator();
    while (iterator.hasNext())
      {
        TaskObserverRoot observer = (TaskObserverRoot) iterator.next();
        observer.unapply(guiProc.getProc());
      }
  }
  
  private void addObserverToAllProcs(TaskObserverRoot observer){
    Iterator iterator = procs.iterator();
    while (iterator.hasNext())
      {
        GuiProc guiProc = (GuiProc) iterator.next();
        if(!guiProc.isDead()){
          observer.apply(guiProc.getProc());
        }
      }
  }
  
  private void removeObserverFromAllProcs(TaskObserverRoot observer){
    Iterator iterator = procs.iterator();
    while (iterator.hasNext())
      {
        GuiProc guiProc = (GuiProc) iterator.next();
        if(!guiProc.isDead()){
          observer.unapply(guiProc.getProc());
        }
      }
  }
  
  
    /**
   * Adds a GuiProc to the Debug Process
   * 
   * @param guiProc - GuiProc that is to be added
   */
  public void addProc (GuiProc guiProc)
  {
    if(this.procs.contains(guiProc)){
      throw new IllegalArgumentException("You are trying to add a GuiProc which has already been added");
    }
    procs.add(guiProc);
  }

  /**
   * Adds a tagset to the debug process
   * 
   * @param tagset - tagset to be added.
   */
  public void addTagset (Tagset tagset)
  {
    tagsets.add(tagset);
  }

  /**
   * Returns the alternative name of the debug process
   * 
   * @return String.
   */
  public String getAlternativeDisplayName ()
  {
    return alternativeDisplayName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see frysk.gui.monitor.GuiObject#getCopy()
   */
  public GuiObject getCopy ()
  {
    return new DebugProcess(this);
  }

  /**
   * Returns a list of of observers to be added to the debug process on load.
   * 
   * @return Observable linked list of observers.
   */
  protected ObservableLinkedList getObservers ()
  {
    return observers;
  }

  /**
   * getProcs() Returns a linked list of processes in a debug process.
   * 
   * @return Observable linked list of processes.
   */
  public ObservableLinkedList getProcs ()
  {
    return procs;
  }

  /**
   * Returns the real name of this process. The real name is normally the system
   * name of the process.
   * 
   * @return String.
   */
  public String getRealName ()
  {
    return realName;
  }

  /**
   * Returns a list of tag sets that have been added to this debug process.
   * 
   * @return ObservableLinkedList of tagsets.
   */
  public ObservableLinkedList getTagsets ()
  {
    return tagsets;
  }
  
  /**
   * Returns the list of variables currently being watched from this process
   * 
   * @return WatchList containing the variables from this process
   */
  public WatchList getWatchList ()
  {
    return watchedVars;
  }

  /**
   * Adds an obsever to the list of observers to be added to the debug process
   * on load.
   * 
   * @param observer - ObserverRoot to add.
   */
  protected void addObserver (ObserverRoot observer)
  {
    if(this.observers.contains(observer)){
      throw new IllegalArgumentException("You are trying to add an observer which has already been added");
    }
    observers.add(observer);    
  }

  /**
   * Removes an obsever from the list of observers to be added to the debug
   * process on load.
   * 
   * @param observer - ObserverRoot to remove.
   */
  protected void removeObserver (ObserverRoot observer)
  {
    observers.remove(observer);
  }
  
  /**
   * Removes an observer from the list of observers to be added to the debug
   * process on load.
   * 
   * @param observerName - Observer name to remove
   */
  public void removeObserverByName (String observerName)
  {
	Iterator i = observers.iterator();
	while (i.hasNext())
	{
		ObserverRoot givenObserver = (ObserverRoot)i.next();
		if (observerName.equals(givenObserver.getName()))
		{
			removeObserver(givenObserver);
			return;
		}
	}
  }

  /**
   * Removes a GuiProc from the list of GuiProcs representred by this process.
   * 
   * @param guiProc - GuiProc to remove.
   */
  public void removeProc (GuiProc guiProc)
  {
    procs.remove(guiProc);
  }

  /**
   * Remove a tagset from this debug process
   * 
   * @param tagset - the tagset to remove.
   */
  public void removeTagset (Tagset tagset)
  {
    tagsets.remove(tagset);
  }

  /**
   * Allows you to set a name that is different from the underlying process.
   * 
   * @param name - Alternative name to display in the session.
   */
  public void setAlternativeDisplayName (String name)
  {
    alternativeDisplayName = name;
  }

  /**
   * Allows you to set the real name (ie system) name of the process
   * 
   * @param name - Name of the process.
   */
  public void setRealName (String name)
  {
    realName = name;
  }
  
  public void save (Element node)
  {
    super.save(node);

    node.setAttribute("executablePath", getExecutablePath());
    
    Element observersXML = new Element("observers");
    node.addContent(observersXML);

    //save tagsets
    Element tagSetsXML = new Element("tagsets");

    // Save variable watches
    Element watchXML = new Element("watches");
    if(watchedVars.shouldSaveObject())
      watchedVars.save(watchXML);
    node.addContent(watchXML);
    
    Iterator i = tagsets.iterator();
    while (i.hasNext())
      {
        GuiObject object = (GuiObject) i.next();
        Element elementXML = new Element("element");
        elementXML.setAttribute("name", object.getName());
        tagSetsXML.addContent(elementXML);
      }

    node.addContent(tagSetsXML);
  }

  public void load (Element node)
  {
    super.load(node);

    setExecutablePath(node.getAttribute("executablePath").getValue());

    // load tagsets
    Element tagSetsXML = node.getChild("tagsets");
    List tagList = tagSetsXML.getChildren("element");
    Iterator iterator = tagList.iterator();

    while (iterator.hasNext())
      {
        Element elementXML = (Element) iterator.next();

        Tagset tag = TagsetManager.manager.getTagsetByName(elementXML.getAttributeValue("name"));
        if (tag != null)
          {
            tagsets.add(tag);
          }

      }
  }

  public void setExecutablePath (String executablePath)
  {
    this.executablePath = executablePath;
  }

  public String getExecutablePath ()
  {
    return executablePath;
  }
}
