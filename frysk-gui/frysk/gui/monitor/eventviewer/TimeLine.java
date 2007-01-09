package frysk.gui.monitor.eventviewer;


import java.util.Iterator;

import org.freedesktop.cairo.Point;
import org.gnu.gdk.Color;
import org.gnu.gdk.EventMask;
import org.gnu.gdk.GdkCairo;
import org.gnu.gtk.DrawingArea;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseMotionEvent;

public abstract class TimeLine
    extends DrawingArea implements ExposeListener
{
  
  static int eventIndentation = 0;
  static int eventSpacing = 15;
  
  protected int startIndex;
//  protected int endIndex;
  String label;
  
  public TimeLine(String label){
    super();
    
    this.label = label;
    
  this.addListener((ExposeListener)this);
//  this.addListener((MouseMotionListener)this);
//  this.addListener((MouseListener) this);
  
  this.setEvents(EventMask.ALL_EVENTS_MASK);
    
//    if(name.length() > eventIndentation/characterSize - 5){
//      eventIndentation = name.length() * characterSize + 5;
//    }
    
    this.setDrawPeriod(0, 10);
    this.setMinimumSize(0 , 20);
  }
  
  public boolean exposeEvent(ExposeEvent exposeEvent) {
    if(exposeEvent.isOfType(ExposeEvent.Type.NO_EXPOSE) || !exposeEvent.getWindow().equals(this.getWindow()))
      return false;
  
    GdkCairo cairo = new GdkCairo(this.getWindow());
    
    int x = exposeEvent.getArea().getX();
    int y = exposeEvent.getArea().getY();
//    int x = 0;
//    int y = 0;
    int w = exposeEvent.getArea().getWidth();
    int h = this.getWindow().getHeight();
    // White background
    cairo.setSourceColor(Color.WHITE);
    cairo.rectangle(new Point(x,y), new Point(x+w, y+h));
    cairo.fill();
    
    cairo.save();
    
    // text
//    cairo.setSourceColor(Color.BLUE);
//    cairo.newPath();
//    cairo.moveTo(this.getX(),this.getY()-1);
//    cairo.showText(this.getName());
//    cairo.stroke();
    
    // line
    cairo.setLineWidth(0.1);
    cairo.setSourceColor(Color.BLACK);

    cairo.moveTo(x, y+h-1);
    cairo.lineTo(x + w, y+h-1);
    
    cairo.stroke();
    
    cairo.restore();
    
//  draw events
    Iterator iterator = EventManager.theManager.getEventsList().iterator();
    int eventY = 0;
    int eventX = 0;
    
    while (iterator.hasNext())
      {
        Event event = (Event) iterator.next();
        
//        if(event.getIndex() > this.endIndex){
//          break;
//        }
        
        if(this.ownsEvent(event)){
          eventX = 0 + eventIndentation + (event.getIndex() - this.startIndex)*(event.getWidth() + eventSpacing) + eventSpacing;
          eventY = 0 + h - event.getHeight();
          event.setSize(eventX, eventY, event.getWidth(), event.getHeight());
          event.draw(cairo);
        }
      }
    
    if(eventX >= w){
      this.setMinimumSize(eventX + 3 , h);
    }
    
    this.showAll();
    
    return true;
  }
  
  public boolean mouseMotionEvent (MouseMotionEvent arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean mouseEvent (MouseEvent arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }
  
  public void setDrawPeriod(int start, int end){
    this.startIndex = 0;
    //this.endIndex = end;
  }
  
  /**
   * Returns wether this time line is associated with the given
   * event
   * @param event the event to be checked
   * @return true of the given event is associated with this TimeLine
   * false otherwise.
   */
  public abstract boolean ownsEvent(Event event);
  
  public String getLabel ()
  {
    return this.label;
  }
}
