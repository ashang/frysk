package frysk.gui.monitor.eventviewer;

import org.gnu.gdk.GdkCairo;
import org.gnu.gtk.event.MouseListener;
import org.gnu.gtk.event.MouseMotionListener;

import frysk.gui.monitor.GuiObject;

/**
 * 
 * @author swagiaal
 *
 * an EventViewerWidget is a widget within the EventViewer.
 * This Glyph design pattern will make complex manipulations
 * of EventViewer and its children simpler.
 */
public abstract class EventViewerWidget extends GuiObject implements MouseMotionListener, MouseListener
{
  private int x;
  private int y;
  private int height;
  private int width;
  
  EventViewerWidget(String name, String tooltip){
    super(name, tooltip);
  }

  public abstract void draw(GdkCairo cairo);
  
  public void setSize(int x, int y, int w, int h){
    this.x = x;
    this.y = y;
    this.width = w;
    this.height = h;
  }
  
  public int getX ()
  {
    return x;
  }

  public int getY ()
  {
    return y;
  }

  public int getHeight ()
  {
    return height;
  }

  public int getWidth ()
  {
    return width;
  }
  
  /**
   * Return wether the given points are inside the widget.
   * useful for mouse event ownership.
   */
  public boolean isInside(int x, int y){
    return (x >= this.x && x <= this.x+this.width && y >= this.y && y <= this.y+this.height);
  }
}
