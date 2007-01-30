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
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;

import org.freedesktop.cairo.Point;
import org.gnu.atk.Role;
import org.gnu.gdk.Color;
import org.gnu.gdk.EventMask;
import org.gnu.gdk.GdkCairo;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;

import com.redhat.ftk.CustomAtkObject;
import com.redhat.ftk.CustomDrawingArea;

public abstract class TimeLine
    extends CustomDrawingArea implements ExposeListener
{
  LinkedList eventBuffer;
  
  static int eventIndentation = 0;
  static int eventSpacing = 30;
  
  String label;
  
  
  public TimeLine(String label){
    super();
  
    CustomAtkObject atkObject = new CustomAtkObject(this);
    
    atkObject.setName(label+"TimeLine");
    atkObject.setDescription("TimeLine");
    
    this.setAcessible(atkObject);
    
    this.label = label;
    
    this.addListener((ExposeListener)this);
  
    this.setEvents(EventMask.ALL_EVENTS_MASK);
        
    this.setMinimumSize(0 , 20);
    
    this.eventBuffer = new LinkedList();
    
    EventManager.theManager.getEventsList().itemAdded.addObserver(new Observer()
    {
      public void update (Observable observable, Object object)
      {
        Event event = (Event)object;
        eventBuffer.add(event);
        
        CustomAtkObject customAtkObject = new CustomAtkObject(TimeLine.this);
        customAtkObject.setName(event.getName());
        customAtkObject.setRole(Role.UNKNOWN);
        
        ((CustomAtkObject)TimeLine.this.getAccessible()).addChild(customAtkObject);
        TimeLine.this.draw();
      }
    });
    
  }
  
  public boolean exposeEvent(ExposeEvent exposeEvent) {
    if(exposeEvent.isOfType(ExposeEvent.Type.NO_EXPOSE) || !exposeEvent.getWindow().equals(this.getWindow()))
      return false;
  
    GdkCairo cairo = new GdkCairo(this.getWindow());
    
    int x = exposeEvent.getArea().getX();
    int y = exposeEvent.getArea().getY();
    int w = exposeEvent.getArea().getWidth();
    int h = this.getWindow().getHeight();
   
    // White background
    cairo.setSourceColor(Color.WHITE);
    cairo.rectangle(new Point(x,y), new Point(x+w, y+h));
    cairo.fill();
    
    cairo.save();
    
    // line
    cairo.setLineWidth(0.1);
    cairo.setSourceColor(Color.BLACK);

    cairo.moveTo(x, y+h-1);
    cairo.lineTo(x + w, y+h-1);
    
    cairo.stroke();
    
    cairo.restore();
    
    // draw events
    Iterator iterator = EventManager.theManager.getEventsList().iterator();
    
    int eventY = 0;
    int eventX = 0;
    
    while (iterator.hasNext())
      {
        Event event = (Event) iterator.next();
        
        if(this.ownsEvent(event)){
          eventX = 0 + eventIndentation + (event.getIndex())*(event.getWidth() + eventSpacing) + eventSpacing;
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
