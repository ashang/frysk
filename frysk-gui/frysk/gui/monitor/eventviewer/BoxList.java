package frysk.gui.monitor.eventviewer;

import java.util.Iterator;


/**
 * 
 * @author swagiaal
 * A little layout widget responsible for placing the time lines
 * on top of each other. And more complicated layouts if needed.
 */
public class BoxList
    extends EventViewerWidgetList
{
  int childY;
  int spacing;
  
  BoxList(){
    super(null, null);
    this.spacing = 10;
    this.childY = spacing;
  }
  
  public void addChild(EventViewerWidget child){
    
    child.setSize(this.getX(), childY, this.getWidth(), child.getHeight());
    
    this.childY += child.getHeight() + spacing;
     
    this.setSize(this.getX(), this.getY(), this.getWidth(), childY);
    super.addChild(child);
  }
  
  public void setSize(int x, int y, int w, int h){
    super.setSize(x, y, w, h);
    Iterator iterator = this.children.iterator();
    while (iterator.hasNext())
      {
        EventViewerWidget child = (EventViewerWidget) iterator.next();
        child.setSize(this.getX(), child.getY(), this.getWidth(), child.getHeight());
      }
  }
}
