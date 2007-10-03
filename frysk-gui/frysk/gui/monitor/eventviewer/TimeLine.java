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

import org.freedesktop.cairo.Point;
import org.gnu.gdk.Color;
import org.gnu.gdk.EventMask;
import org.gnu.gdk.GdkCairo;
import org.gnu.gtk.Adjustment;
import org.gnu.gtk.EventBox;
import org.gnu.gtk.HBox;
import org.gnu.gtk.Justification;
import org.gnu.gtk.Label;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.StateType;
import org.gnu.gtk.VBox;
import org.gnu.gtk.Viewport;
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;

import com.redhat.ftk.CustomAtkObject;
import com.redhat.ftk.CustomDrawingArea;

import frysk.gui.monitor.GuiObservable;
import frysk.gui.srcwin.SourceWindowFactory;

public abstract class TimeLine
    extends HBox implements MouseListener
{
  
  public final GuiObservable selected;
  public final GuiObservable unSelected;
  
  static int eventSpacing = 30;
  
  private static SizeGroup labelsSizeGroup = new SizeGroup(SizeGroupMode.HORIZONTAL);
  
  private static final int MINIMUM_HEIGHT = 15;
  private static int MINIMUM_WIDTH = 0 ;
  
  String labelString;
  
  private Viewport viewport;
  private boolean isSelected;
  
  private boolean isDead;
  Label label;
  
  int startIndex = 0;
  int endIndex;
  
  private static Color SELECTED_COLOR = new Color(55535, 55535, 55535);
  private static Color UNSELECTED_COLOR = Color.WHITE;
  
  public TimeLine(String name, TimeLineSelectionManager manager){
    super(false,0);
  
    this.selected = new GuiObservable();
    this.unSelected = new GuiObservable();
    
    this.setBorderWidth(1);
    
    this.labelString = name;
    
    label = new Label(name);
    label.setAlignment(0.4,0.5);
    label.setJustification(Justification.RIGHT);
    
    EventBox labelEventBox = new EventBox();
    labelEventBox.add(label);
    label.getParent().setBackgroundColor(StateType.NORMAL, UNSELECTED_COLOR);
    
    labelEventBox.addListener((MouseListener)this);
    this.addListener((MouseListener)this);
    Viewport labeViewport = new Viewport(null,null);
    labeViewport.add(labelEventBox);
    
    addToLabelsSizeGroup(label);
    
    final TimeLineDrawingArea drawingArea = getTimeLineDrawingArea();
    
    viewport = new Viewport(null,null);
    viewport.add(drawingArea);
    viewport.setMinimumSize(0, drawingArea.getMinimumHeight());
    
    VBox vBox = new VBox(false,0);
    
    this.packStart(labeViewport, false, false, 1);
    this.packStart(viewport,true,true,1);
    this.packEnd(vBox, false, false, 0);
    
    manager.addTimeLine(this);
        
  }
  
  public void setStartIdnex(int index){
    this.startIndex = index;
  }
  
  public void setEndIndex(int index){
    this.endIndex = index;
  }
  
  public void setHAdjustment(Adjustment adjustment){
    this.viewport.setHAdjustment(adjustment);
  }
  
  protected TimeLineDrawingArea getTimeLineDrawingArea(){
    return new TimeLineDrawingArea();
  }
  
  public void setLabel(String string){
    this.labelString = string;
    this.label.setMarkup(string);
  }
  
  protected class TimeLineDrawingArea extends CustomDrawingArea implements ExposeListener, MouseListener{
    
    public TimeLineDrawingArea ()
    {
      CustomAtkObject atkObject = new CustomAtkObject(this);
      
      atkObject.setName(labelString+"TimeLine");
      atkObject.setDescription("TimeLine");
      
      this.setAcessible(atkObject);
      
      
      this.addListener((ExposeListener)this);
      this.addListener((MouseListener) this);
      this.setEvents(EventMask.ALL_EVENTS_MASK);
          
      this.setMinimumSize(MINIMUM_WIDTH , MINIMUM_HEIGHT);
    }
    
    public int getMinimumHeight ()
    {
      return MINIMUM_HEIGHT;
    }

    /**
     * Given x and y coordinates returns the Event which
     * has been drawn there.
     */
    private Event xy2Event(double x, double y){
      int index = (int) ((x)/(Event.getWidth() + eventSpacing));
      return EventManager.theManager.eventAtIndex(index);
    }
    
    public boolean mouseEvent (MouseEvent mouseEvent)
    {
      if(mouseEvent.isOfType(MouseEvent.Type.BUTTON_PRESS)){
        Event event = this.xy2Event(mouseEvent.getX(), mouseEvent.getY());
        if(event != null && ownsEvent(event)){
          event.select();
        }
      }
      
      if(mouseEvent.getClickType() == MouseEvent.DOUBLE_CLICK && mouseEvent.isOfType(MouseEvent.Type.BUTTON_PRESS)){
        Event event = this.xy2Event(mouseEvent.getX(), mouseEvent.getY());
        if(event != null && event.getStackFrame()!=null && ownsEvent(event)){
           SourceWindowFactory.createSourceWindow(event.getStackFrame());
          return true;
        }
      }
      return false;
    }

    public boolean exposeEvent(ExposeEvent exposeEvent) {
//      if(exposeEvent.isOfType(ExposeEvent.Type.NO_EXPOSE) || !exposeEvent.getWindow().equals(this.getWindow()))
//        return false;
    
      this.setMinimumSize(MINIMUM_WIDTH , MINIMUM_HEIGHT);
      
      GdkCairo cairo = new GdkCairo(this.getWindow());
      
      int x = 0;
      int y = 0;
      int w = this.getWindow().getWidth();
      int h = exposeEvent.getArea().getHeight();  
      
      // White background
      if(isSelected){
	cairo.setSourceColor(SELECTED_COLOR);
      }else{
	cairo.setSourceColor(UNSELECTED_COLOR);
      }
      
      cairo.rectangle(new Point(x,y), new Point(w, this.getWindow().getHeight()));
      cairo.fill();
            
      // draw events
      Iterator iterator = EventManager.theManager.getEventsList().iterator();
      
      int eventX = 0;
      int eventY = 0;
      
      while (iterator.hasNext())
        {
          Event event = (Event) iterator.next();
          
          if(TimeLine.this.ownsEvent(event)){
            eventX = x + eventSpacing/2 + (event.getIndex())*(Event.getWidth() + eventSpacing);
            eventY = y + h - Event.getHeight();
            event.setXY(eventX, eventY);
            event.draw(cairo);
          }
        }
      
      if(eventX >= w){
        MINIMUM_WIDTH = w + MINIMUM_HEIGHT;
        this.setMinimumSize(MINIMUM_WIDTH , MINIMUM_HEIGHT);
      }
     
      if(isDead){
        // White background
        cairo.setSourceRGBA(1,1,1, 0.5);
        cairo.rectangle(new Point(x,y), new Point(w, this.getWindow().getHeight()));
        cairo.fill();
      }
      
      return false;
    }
  }
    
  public boolean mouseEvent(MouseEvent event){
    if(event.isOfType(MouseEvent.Type.BUTTON_PRESS)){
      this.select();
    }
    
    return false;
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
    return this.labelString;
  }
  
  public void select(){
    this.isSelected = true;
    this.label.getParent().setBackgroundColor(StateType.NORMAL, SELECTED_COLOR);
    this.selected.notifyObservers();
    this.draw();
  }
  
  public void unselect(){
    this.isSelected = false;
    this.label.setMarkup(this.labelString);
    this.label.getParent().setBackgroundColor(StateType.NORMAL, UNSELECTED_COLOR);
    this.unSelected.notifyObservers();
  }
  
  public boolean isSelected(){
    return this.isSelected;
  }
  
  public void timeLineDead(){
    this.isDead = true;
    
    int grayFactor = 3;
    this.label.setForegroundColor(StateType.NORMAL, new Color(65535/grayFactor, 65535/grayFactor, 65535/grayFactor));
    
    this.label.setMarkup(this.label.getText() + "\n<i>terminated</i>");
  }
  
  public static void addToLabelsSizeGroup(Widget widget){
    labelsSizeGroup.addWidget(widget);
  }
}
