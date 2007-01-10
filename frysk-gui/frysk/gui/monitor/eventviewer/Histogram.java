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

import org.freedesktop.cairo.Point;
import org.gnu.gdk.Color;
import org.gnu.gdk.GdkCairo;
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
