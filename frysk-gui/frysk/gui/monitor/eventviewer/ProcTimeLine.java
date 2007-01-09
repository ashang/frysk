package frysk.gui.monitor.eventviewer;

import java.util.Iterator;

import org.gnu.gdk.GdkCairo;
import org.gnu.gtk.event.ExposeEvent;

import frysk.gui.monitor.GuiProc;

public class ProcTimeLine extends TimeLine
{

  private GuiProc guiProc;

  public ProcTimeLine (GuiProc guiProc)
  {
    super(guiProc.getExecutableName()+ " " + guiProc.getProc().getPid());
    this.guiProc = guiProc;
    
    this.setMinimumSize(0 , 100);
  }

  public boolean exposeEvent(ExposeEvent exposeEvent) {
    super.exposeEvent(exposeEvent);
    
    if(exposeEvent.isOfType(ExposeEvent.Type.NO_EXPOSE) || !exposeEvent.getWindow().equals(this.getWindow()))
      return false;
    
    GdkCairo cairo = new GdkCairo(this.getWindow());
    // draw events
    Iterator iterator = EventManager.theManager.getEventsList().iterator();
    while (iterator.hasNext())
      {
        Event event = (Event) iterator.next();
        
        if(this.ownsEvent(event)){
          event.drawText(cairo);
        }
      }
    
    return true;
  }

  private GuiProc getProc ()
  {
    return this.guiProc;
  }

  public boolean ownsEvent (Event event)
  {
    return event.getGuiTask().getTask().getProc().getPid() == this.getProc().getProc().getPid();
  }
}
