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
import java.util.logging.Level;
import java.util.logging.Logger;
import frysk.gui.Gui;
import frysk.gui.monitor.WindowManager;
import java.util.Date;

import org.jdom.Element;

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.GuiProc;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.datamodels.DataModelManager;
import frysk.gui.monitor.observers.ObserverManager;
import frysk.gui.monitor.observers.ObserverRoot;
import frysk.gui.monitor.observers.TaskObserverRoot;
import frysk.gui.srcwin.tags.Tagset;
import frysk.gui.srcwin.tags.TagsetManager;
import frysk.proc.Action;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

/**
 * @author pmuldoon, swagiaal, A container that refers to an executable there
 *         could be zero or many instances of these executable. This keeps track
 *         of those too.
 */

public class DebugProcess
    extends GuiObject
{

  String executablePath;

  ObservableLinkedList procs;

  ObservableLinkedList observers;

  ObservableLinkedList tagsets;

  ObservableLinkedList allProcsList;

  String alternativeDisplayName = "";

  private String realName;

  private Logger errorLog = Logger.getLogger(Gui.ERROR_LOG_ID);

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

    allProcsList = DataModelManager.theManager.flatProcObservableLinkedList;

    addProgramObserver();
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
    executablePath = other.executablePath;

    procs = new ObservableLinkedList();

    observers = new ObservableLinkedList(other.observers);
    tagsets = new ObservableLinkedList(other.tagsets);

    allProcsList = DataModelManager.theManager.flatProcObservableLinkedList;

    addProgramObserver();
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
    this.executablePath = executablePath;

    procs = new ObservableLinkedList();

    observers = new ObservableLinkedList();
    tagsets = new ObservableLinkedList();

    allProcsList = DataModelManager.theManager.flatProcObservableLinkedList;

    addProgramObserver();
  }

  /**
   * Adds an obsever to the list of observers to be added to the debug process
   * on load.
   * 
   * @param observer - ObserverRoot to add.
   */
  public void addObserver (ObserverRoot observer)
  {
    observers.add(observer);
  }

  /**
   * Adds all observers from the observer list to the process represented by
   * this debug process
   */
  public void addObservers ()
  {
    Iterator procIter = allProcsList.iterator();
    while (procIter.hasNext())
      {
        GuiProc guiProc = (GuiProc) procIter.next();
        if (guiProc.getNiceExecutablePath().equals(executablePath))
          {
            Iterator obIter = observers.iterator();
            while (obIter.hasNext())
              {
                TaskObserverRoot observer = (TaskObserverRoot) obIter.next();
                guiProc.add(observer);
              }
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
    // Add terminated observer to catch the procs exit
    guiProc.getProc().getMainTask().requestAddTerminatedObserver(
                                                                 new TaskObserver.Terminated()
                                                                 {

                                                                   public void addedTo (
                                                                                        Object observable)
                                                                   {
                                                                     // TODO
                                                                      // Auto-generated
                                                                      // method
                                                                      // stub

                                                                   }

                                                                   public void addFailed (
                                                                                          Object observable,
                                                                                          Throwable w)
                                                                   {
                                                                     // TODO
                                                                      // Auto-generated
                                                                      // method
                                                                      // stub

                                                                   }

                                                                   public void deletedFrom (
                                                                                            Object observable)
                                                                   {
                                                                     // TODO
                                                                      // Auto-generated
                                                                      // method
                                                                      // stub

                                                                   }

                                                                   public Action updateTerminated (
                                                                                                   Task task,
                                                                                                   boolean signal,
                                                                                                   int value)
                                                                   {
                                                                     removeProc(GuiProc.GuiProcFactory.getGuiProc(task.getProc()));
                                                                     return Action.CONTINUE;
                                                                   }

                                                                 });

    Iterator iterator = observers.iterator();
    while (iterator.hasNext())
      {
        TaskObserverRoot observer = (TaskObserverRoot) iterator.next();
        guiProc.add(observer);
      }

    procs.add(guiProc);
  }

  public void addProcsMinusObserver ()
  {
    Iterator iterator = allProcsList.iterator();
    while (iterator.hasNext())
      {
        GuiProc guiProc = (GuiProc) iterator.next();
        if (guiProc.getNiceExecutablePath().equals(executablePath))
          {
            procs.add(guiProc);
          }
      }
  }

  /**
   * Adds a program observer to the debug process
   */
  private void addProgramObserver ()
  {
    ObserverManager.theManager.programObserver.getProcsList().itemAdded.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {

        GuiProc guiProc = (GuiProc) object;
        if (guiProc.getNiceExecutablePath().equals(executablePath))
          {
            addProc(guiProc);
          }
      }
    });
  }

  /**
   * Adds the listed debug process observers to the processes that this debug
   * container represents.
   */
  public void addRemoveObservers ()
  {
    allProcsList.itemAdded.addObserver(new Observer()
    {
      public void update (Observable observable, Object arg)
      {
        GuiProc guiProc = (GuiProc) arg;
        if (guiProc.getNiceExecutablePath().equals(executablePath))
          {
            // XXX: Hack this out. Needs core investigation. This will be caught
            // when bash forks().The child fork is named /bin/bash and
            // it will try to be added to a session here if the parent
            // had a fork process observer added. However the child will
            // very soon be exec'd and in a short lived process, die.
            // This will cause havoc in core state machine, because
            // we are asking it to do continue attaching observer to a detached
            // process.
            // Double Jeopardy.

            // addProc(guiProc);
          }
      }
    });

    allProcsList.itemRemoved.addObserver(new Observer()
    {
      public void update (Observable observable, Object arg)
      {
        // System.out.println(".update()");
        GuiProc guiProc = (GuiProc) arg;
        // if (guiProc.getNiceExecutablePath().equals(executablePath))
        // {
        removeProc(guiProc);
        // }
      }
    });
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
  public ObservableLinkedList getObservers ()
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
   * Removes an obsever from the list of observers to be added to the debug
   * process on load.
   * 
   * @param observer - ObserverRoot to remove.
   */
  public void removeObserver (ObserverRoot observer)
  {

    observers.remove(observer);
  }
  
  /**
   * Removes an obsever from the list of observers to be added to the debug
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

    Iterator iterator = observers.iterator();
    while (iterator.hasNext())
      {
        TaskObserverRoot observer = (TaskObserverRoot) iterator.next();
        guiProc.add(observer);
      }

    procs.remove(guiProc);
  }

  public void removeProcsMinusObserver ()
  {
    Iterator iterator = allProcsList.iterator();
    while (iterator.hasNext())
      {
        GuiProc guiProc = (GuiProc) iterator.next();
        if (guiProc.getNiceExecutablePath().equals(executablePath))
          {
            procs.remove(guiProc);
          }
      }
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

    node.setAttribute("executablePath", executablePath);
    Element observersXML = new Element("observers");

    Iterator iterator = observers.iterator();
    while (iterator.hasNext())
      {
        GuiObject object = (GuiObject) iterator.next();
        Element elementXML = new Element("element");
        elementXML.setAttribute("name", object.getName());
        observersXML.addContent(elementXML);
      }

    node.addContent(observersXML);

    //save tagsets
    Element tagSetsXML = new Element("tagsets");

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

    executablePath = node.getAttribute("executablePath").getValue();
    Element observersXML = node.getChild("observers");
    List list = observersXML.getChildren("element");
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
            WindowManager.theManager.logWindow.print(new Date()
                                                     + " DebugProcess.load(Element node): observer "
                                                     + elementXML.getAttributeValue("name")
                                                     + " not found in configuration \n");
          }
        else
          {
            observers.add(observer);
          }
      }

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
}
