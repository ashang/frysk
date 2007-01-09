package frysk.gui.monitor.eventviewer;

import java.util.Iterator;
import java.util.LinkedList;

import org.freedesktop.cairo.Point;
import org.gnu.gdk.Color;
import org.gnu.gdk.GdkCairo;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseMotionEvent;

public class Histogram
    extends EventViewerWidget
{

  private LinkedList timeLines;
  private int periodSize;
  private int scrollBarLoacation;
  
  Histogram (String name, String tooltip)
  {
    super(name, tooltip);
    this.timeLines = new LinkedList();
  
    this.periodSize = this.getWidth();
    this.scrollBarLoacation = 0;
  }

  private void setTimeLinePeriods(){
    Iterator iter = this.timeLines.iterator();
    while(iter.hasNext()){
      TimeLine timeLine = (TimeLine) iter.next();
      timeLine.setDrawPeriod(this.scrollBarLoacation, this.scrollBarLoacation+this.periodSize);
    }
  }
  
  public void draw (GdkCairo cairo)
  {
    cairo.setSourceColor(Color.WHITE);
    cairo.rectangle(new Point(this.getX(),this.getY()), new Point(this.getX()+this.getWidth(), this.getY()+this.getHeight()));
    cairo.fill();
    
    cairo.setSourceColor(Color.RED);
    cairo.rectangle(new Point(this.getX(),this.getY()), new Point(this.getX()+this.getWidth(), this.getY()+this.getHeight()));
    cairo.stroke();
    
    //scroll bar
    cairo.setSourceColor(Color.RED);
    cairo.rectangle(new Point(this.scrollBarLoacation,this.getY()), new Point(this.scrollBarLoacation+5, this.getY()+this.getHeight()));
    cairo.fill();
    
  }

  public boolean mouseMotionEvent (MouseMotionEvent event)
  {
    this.scrollBarLoacation = (int)event.getX() - this.getX();
    this.setTimeLinePeriods();
    return true;
  }

  public boolean mouseEvent (MouseEvent arg0)
  {
    // TODO Auto-generated method stub
    throw new RuntimeException("This method is not implemented (Dec 12, 2006)");
  }
  
  public void addTimeLine(TimeLine timeLine){
      this.timeLines.add(timeLine);
      timeLine.setDrawPeriod(scrollBarLoacation, scrollBarLoacation+periodSize);
  }
  
  public void removeTimeLine(TimeLine timeLine){
    this.timeLines.remove(timeLine);
  }
  
  public void setSize(int x, int y, int w, int h){
    System.out.println("Histogram.setSize()");
    super.setSize(x, y, w, h);
    this.periodSize = this.getWidth();
  }
}
