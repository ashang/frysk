package frysk.gui.monitor.eventviewer;

import frysk.gui.monitor.GuiTask;

public class TaskTimeLine extends TimeLine
{

  private GuiTask guiTask;

  public TaskTimeLine (GuiTask guiTask)
  {
    super(""+guiTask.getTask().getTid());
    this.guiTask = guiTask;
  }

//  public boolean exposeEvent(ExposeEvent exposeEvent) {
//    if(exposeEvent.isOfType(ExposeEvent.Type.NO_EXPOSE) || !exposeEvent.getWindow().equals(this.getWindow()))
//      return false;
//  
//    super.exposeEvent(exposeEvent);
//    
//    GdkCairo cairo = new GdkCairo(this.getWindow());
//    
//    int x = exposeEvent.getArea().getX();
//    int y = exposeEvent.getArea().getY();
//     
//    //  draw events
//    Iterator iterator = EventManager.theManager.getEventsList().iterator();
//    while (iterator.hasNext())
//      {
//        Event event = (Event) iterator.next();
//        
//        if(event.getIndex() > this.endIndex){
//          break;
//        }
//        
//        if(this.ownsEvent(event)){
//          int eventX = x + eventIndentation + (event.getIndex() - this.startIndex)*(event.getWidth() + 3) + 3;
//          int eventY = y - event.getHeight() + 1;
//          event.setSize(eventX, eventY, event.getWidth(), event.getHeight());
//          event.draw(cairo);
//        }
//      }
//        
//    this.showAll();
//    
//    return true;
//  }
  

  public boolean ownsEvent (Event event)
  {
    return event.getGuiTask().getTask().getTid() == this.getGuiTask().getTask().getTid();
  }
  
  public GuiTask getGuiTask ()
  {
    return this.guiTask;
  }
}
