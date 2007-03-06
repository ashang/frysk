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

package frysk.gui.monitor.eventviewer;

import java.util.Iterator;

import frysk.gui.monitor.GuiTask;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.observers.ObserverRoot;

/**
 * This is a list of all the events that the time line widget is meant to draw,
 * and the timeline's.
 * TimeLine widgets are to iterate over this list to find out which events
 * belongs to them. This way a global index of an event can be kept, mapping of
 * event to TimeLine is easier, and event objects can be shared by different
 * TimeLines.
 */
public class EventManager
{
  public static EventManager theManager = new EventManager();
  
  private ObservableLinkedList selectedEvents;
  private ObservableLinkedList eventsList;
  int index;

  private boolean allowMultipleSelection;
  
  private EventManager(){
    this.allowMultipleSelection = false;
    this.eventsList = new ObservableLinkedList();
    this.selectedEvents = new ObservableLinkedList();
    this.index = 0;
  }
  
  public void addEvent(Event event){
    event.setIndex(index);
    eventsList.add(event);
    index++;
  }
 
  public synchronized ObservableLinkedList getEventsList(){
    return this.eventsList;
  }
  
  public void observerAdded(GuiTask guiTask, ObserverRoot observer){}
  public void observerRemoved(GuiTask guiTask, ObserverRoot observer){}

  public Event eventAtIndex (int index)
  {
    if(index < eventsList.size()){
      return (Event) this.eventsList.get(index);
    }else{
      return null;
    }
  }
  
  public void eventSelected(Event event){
    if(!this.allowMultipleSelection){
      this.unselectAll();
    }
    this.selectedEvents.add(event);
  }
  
  public void eventUnselected(Event event){
    this.selectedEvents.remove(event);
  }
  
  private void unselectAll(){
    Iterator iterator = this.selectedEvents.iterator();
    while(iterator.hasNext()){
      Event event = (Event) iterator.next();
      event.unselect();
    }
  }
  
  public ObservableLinkedList getSelectedEvents(){
    return this.selectedEvents;
  }
  
}
