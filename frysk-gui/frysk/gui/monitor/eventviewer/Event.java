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

import frysk.gui.monitor.GuiObject;
import frysk.gui.monitor.GuiTask;
import frysk.gui.monitor.observers.ObserverRoot;

public class Event extends GuiObject
{
 
  boolean selected;
  
  String label;
  GuiTask guiTask;
  ObserverRoot observer;
  private int index;
  
  private int textSlant;
  
  private int x,y;
  public static int w,h;
  
  static{
    w = 3;
    h = 10;
  }
  
  public Event(String name, String tooltip, GuiTask guiTask, ObserverRoot observer){
    super(name, tooltip);
  
    this.guiTask = guiTask;
    this.label = new String();
    this.textSlant = 25;
    
    this.selected = false;
  }
  
  public GuiTask getGuiTask(){
    return this.guiTask;    
  }
  
  public void select(){
    EventManager.theManager.eventSelected(this);
    this.selected = true;
  }
  
  public void unselect(){
    EventManager.theManager.eventUnselected(this);
    this.selected = false;
  }
  
  public ObserverRoot getObserver(){
    return this.observer;
  }
  
  public void draw (GdkCairo cairo)
  {
    cairo.save();
    
    if(this.selected){
      cairo.setSourceColor(Color.RED);
    }else{
      cairo.setSourceColor(Color.BLUE);
    }
    
    cairo.rectangle(new Point(this.getX(),this.getY()), new Point(this.getX()+ getWidth(), this.getY()+ getHeight()));
    
    cairo.fill();
    
    cairo.restore();
  }

  public void drawText(GdkCairo cairo){
    cairo.save();
    
    //  Text over each event
    cairo.setSourceColor(Color.BLACK);
    cairo.newPath();
    
    cairo.moveTo(this.getX(), this.getY()-5);
    cairo.rotate(-Math.PI*textSlant/180);
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
  
  public void setXY(int x, int y){
    this.x = x;
    this.y = y;
  }
  
  public static void setSize(int w, int h){
    Event.w = w;
    Event.h = h;
  }
  
  public static int getWidth(){
    return w;
  }
  
  public static int getHeight(){
    return h;
  }
  
  public int getX (){
    return x;
  }

  public int getY (){
    return y;
  }

}
