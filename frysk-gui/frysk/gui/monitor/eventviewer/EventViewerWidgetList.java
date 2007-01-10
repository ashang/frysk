package frysk.gui.monitor.eventviewer;

import java.util.Iterator;
import java.util.LinkedList;

import org.gnu.gdk.GdkCairo;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseMotionEvent;

/**
 * A widget that is basically just a collection of widgets
 * and calls draw on all its children when it is told to
 * draw.
 */
public class EventViewerWidgetList
    extends EventViewerWidget
{

  LinkedList children;
  
  EventViewerWidgetList (String name, String tooltip)
  {
    super(name,tooltip);
    this.children = new LinkedList();
  }

  public void draw (GdkCairo cairo)
  {
    this.drawChildren(cairo);
  }

  protected void drawChildren(GdkCairo cairo){
    Iterator iter = children.iterator();
    while (iter.hasNext())
      {
        EventViewerWidget child = (EventViewerWidget) iter.next();
        child.draw(cairo);
      }
  }
  
  public void addChild(EventViewerWidget child){
    this.children.add(child);
  }
  
  public void removeChild(EventViewerWidget child){
    this.children.remove(child);
  }

  public boolean mouseMotionEvent (MouseMotionEvent event)
  {
    Iterator iter = children.iterator();
    while (iter.hasNext())
      {
        EventViewerWidget child = (EventViewerWidget) iter.next();
        if(child.mouseMotionEvent (event)){
          return true;
        }
      }
    return false;
  }

  public boolean mouseEvent (MouseEvent event)
  {
    Iterator iter = children.iterator();
    while (iter.hasNext())
      {
        EventViewerWidget child = (EventViewerWidget) iter.next();
        child.mouseEvent (event);
      }
    return false;
  }

//  public void setSize(int x, int y, int w, int h){
////    Iterator iter = children.iterator();
////    while (iter.hasNext())
////      {
////        EventViewerWidget child = (EventViewerWidget) iter.next();
////        child.setSize(x, y, w, h);
////      }
//  }
}
