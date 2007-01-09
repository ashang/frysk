package frysk.gui.monitor.eventviewer;

import org.freedesktop.cairo.Point;
import org.gnu.gdk.Color;
import org.gnu.gdk.GdkCairo;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseMotionEvent;

import frysk.gui.monitor.GuiTask;
import frysk.gui.monitor.observers.ObserverRoot;

public class Event extends EventViewerWidget
{

  String label;
  GuiTask guiTask;
  ObserverRoot observer;
  private int index;
  
  public Event(String name, String tooltip, GuiTask guiTask, ObserverRoot observer){
    super(name, tooltip);
    this.guiTask = guiTask;
    this.label = new String();
  }
  
  public GuiTask getGuiTask(){
    return this.guiTask;
  }
  
//  /**
//   * Returns weather this evet is associated with the given GuiObject
//   * @param object the object to check the association of.
//   * @return true of the event is assoicated with the given object, false
//   * other wise.
//   */
//  public boolean belongs(GuiObject object){
//    return (object == this.guiTask);
//  }

  public ObserverRoot getObserver(){
    return this.observer;
  }
  
  public void draw (GdkCairo cairo)
  {
    cairo.save();
    
    cairo.setSourceColor(Color.BLUE);
//    cairo.moveTo(x, y);
//    cairo.relLineTo(0,0);
//    cairo.relLineTo(width,0);
//    cairo.relLineTo(width,height+20);
//    cairo.relLineTo(0,height+20);
//    cairo.closePath();
//    cairo.fill();
    
    cairo.rectangle(new Point(this.getX(),this.getY()), new Point(this.getX()+this.getWidth(), this.getY()+this.getHeight()));
    
    cairo.fill();
    
    cairo.restore();
  }

  public void drawText(GdkCairo cairo){
    cairo.save();
    
//  Text over each event
    cairo.setSourceColor(Color.BLACK);
    cairo.newPath();
    //cairo.moveTo(this.getX(), this.getY() - this.getHeight()/2 - 10);
    cairo.moveTo(this.getX(), this.getY());
    cairo.rotate(Math.PI/-4); // 45 degrees counter-clockwise
    cairo.showText(this.getName());
    cairo.stroke();
    
    cairo.restore();
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

  public void setIndex (int index)
  {
    this.index = index;
  }

  public int getIndex(){
    return this.index;
  }
}
