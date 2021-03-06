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
import java.util.Observable;
import java.util.Observer;

import org.freedesktop.cairo.Point;
import org.gnu.gdk.GdkCairo;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.MouseEvent;

import frysk.gui.monitor.GuiProc;
import frysk.gui.srcwin.SourceWindowFactory;

public class ProcTimeLine extends TimeLine
{

  private static final int MINIMUM_HEIGHT = 50;
  private GuiProc guiProc;

  private static SizeGroup procTimeLineSizeGroup = new SizeGroup(SizeGroupMode.VERTICAL);
  
  public ProcTimeLine (GuiProc guiProc, TimeLineSelectionManager manager)
  {
    super(guiProc.getExecutableName()+"  ", manager);
    this.guiProc = guiProc;
    addToProcTimeLineSizeGroup(this);
    guiProc.executablePathChanged.addObserver(new Observer()
    {
      public void update (Observable observable, Object object){
	ProcTimeLine.this.setName(((GuiProc)object).getExecutableName());
      }
    });
  }

  protected TimeLineDrawingArea getTimeLineDrawingArea(){
    return new ProcTimeLineDrawingArea();
  }
  
  public GuiProc getGuiProc ()
  {
    return this.guiProc;
  }

  public boolean ownsEvent (Event event)
  {
    return event.getGuiTask().getTask().getProc().getPid() == this.getGuiProc().getProc().getPid();
  }

  
  protected class ProcTimeLineDrawingArea extends TimeLine.TimeLineDrawingArea{
    public ProcTimeLineDrawingArea ()
    {
      super();
      this.setMinimumSize(100 , 60);
    }
    public boolean exposeEvent(ExposeEvent exposeEvent) {
      super.exposeEvent(exposeEvent);
      
      if(exposeEvent.isOfType(ExposeEvent.Type.NO_EXPOSE) || !exposeEvent.getWindow().equals(this.getWindow()))
        return false;
      
      GdkCairo cairo = new GdkCairo(this.getWindow());
      
      int x = 0;
      int y = 0;
      int w = this.getWindow().getWidth();
      int h = exposeEvent.getArea().getHeight();
      
      cairo.setSourceColor(SELECTED_COLOR);
      cairo.rectangle(new Point(x,y+h), new Point(w,y+h-2));
      cairo.fill();
      
      // draw events
      Iterator iterator = EventManager.theManager.getEventsList().iterator();
      while (iterator.hasNext())
        {
          Event event = (Event) iterator.next();        
          if(ProcTimeLine.this.ownsEvent(event)){
            event.drawText(cairo);
          }
        }
      
      return true;
    }
    
    public int getMinimumHeight ()
    {
      return MINIMUM_HEIGHT;
    }

  }

  public static void addToProcTimeLineSizeGroup(ProcTimeLine timeLine){
    procTimeLineSizeGroup.addWidget(timeLine);
  }
  
  public boolean mouseEvent(MouseEvent event){
    super.mouseEvent(event);
    
    if(event.getClickType() == MouseEvent.DOUBLE_CLICK){
      SourceWindowFactory.createSourceWindow(this.getGuiProc().getProc());
    }
    
    return false;
  }
  
}
