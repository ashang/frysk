package frysk.gui.monitor.eventviewer;

import frysk.gui.monitor.GuiTask;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.gui.monitor.observers.ObserverRoot;

/**
 * This is a list of all the events that the time line widget is meant to draw,
 * and the timeline's.
 * TimeLine widgets are to iterate over this list to find out which events
 * belongs to them. This way a global index of an event can be kept, mapping of
 * event to TimeLine is easyer, and event objects can be shared by different
 * TimeLines.
 */
public class EventManager
{
  public static EventManager theManager = new EventManager();
  
  private ObservableLinkedList events;
  int index;
  
  public EventManager(){
    this.events = new ObservableLinkedList();
    this.index = 0;
  }
  
  public void addEvent(Event event){
    event.setSize(event.getWidth()*index, 0, 2, 10);
//    event.setX(event.width*index);
//    event.setY(event.height); 
    event.setIndex(index);
    events.add(event);
    index++;
  }
  
  public ObservableLinkedList getEventsList(){
    return this.events;
  }
  public void observerAdded(GuiTask guiTask, ObserverRoot observer){}
  public void observerRemoved(GuiTask guiTask, ObserverRoot observer){}
  
  
}
