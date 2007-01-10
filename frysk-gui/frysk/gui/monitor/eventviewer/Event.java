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

import org.freedesktop.cairo.Point;
import org.gnu.gdk.Color;
import org.gnu.gdk.GdkCairo;

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
  
  public void setIndex (int index)
  {
    this.index = index;
  }

  public int getIndex(){
    return this.index;
  }
}
