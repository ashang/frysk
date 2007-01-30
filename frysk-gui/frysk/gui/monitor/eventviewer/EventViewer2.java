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

import org.gnu.glib.Handle;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.VBox;

import frysk.gui.monitor.GuiProc;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;

public class EventViewer2 extends VBox {
	
//  private static final int MARGIN = 5;
//  private static final int MIN_WIDTH = 500;
  
  int x;
  int y;
  int width;
  int height;
  boolean sessionMounted = false;
  SizeGroup lablesSizeGroup;
  
  //HashMap procHashMap;
  private Session currentSession;
  
	public EventViewer2(Handle handle){
		super(handle);
        this.setBorderWidth(6);
  
        this.lablesSizeGroup = new SizeGroup(SizeGroupMode.HORIZONTAL);
        
//        // watch the event list for updates
//        // redraw upon updates
//        ObservableLinkedList eventList = EventManager.theManager.getEventsList();
//        Observer drawObserver = new Observer()
//        {
//          public void update (Observable arg0, Object arg1)
//          {
//            EventViewer2.this.draw();
//          }
//        
//        };
//       eventList.itemAdded.addObserver(drawObserver);
//       eventList.itemRemoved.addObserver(drawObserver);
       
       this.getAccessible().setName("EventViewer");
	}

//	public boolean exposeEvent(ExposeEvent event) {
//	  if(event.isOfType(ExposeEvent.Type.NO_EXPOSE) || !event.getWindow().equals(this.getWindow()))
//	    return false;
//
//	  GdkCairo cairo = new GdkCairo(this.getWindow());
//	  
//      if(!sessionMounted){
//        this.mountSession();  
//      }
//      
//      x = event.getArea().getX();
//      y = event.getArea().getY();
//      width = event.getArea().getWidth();
//      height = this.getWindow().getHeight();
//       
//      // White background
//      cairo.setSourceColor(Color.WHITE);
//      cairo.rectangle(new Point(x,y), new Point(x+width, y+height));
//      cairo.fill();
//      
//      this.showAll();
//      
//	  return true;
//	}

	
	
    public void setSession(Session session){
      this.currentSession = session;
      if(!sessionMounted){
        this.mountSession();  
      }
    }
    
    private void addProc(GuiProc guiProc){
      ProcBox procBox = new ProcBox(guiProc, this.lablesSizeGroup);
 //     procBox.setSize(0, 0,MIN_WIDTH,0);
       this.packStart(procBox, false, false,12);
    //  this.setMinimumSize(MIN_WIDTH, procBoxList.getHeight());
    }
    
    private void mountSession(){
//      this.procBoxList = new BoxList();
//      this.procBoxList.setSize(0 + MARGIN, 0, this.getWindow().getWidth()- 2*MARGIN, 0);
      Iterator i = this.currentSession.getProcesses().iterator();
      while (i.hasNext())
        {
          DebugProcess debugProcess = (DebugProcess) i.next();
          
          Iterator j = debugProcess.getProcs().iterator();
          while (j.hasNext())
            {
              addProc((GuiProc) j.next());
            }

          debugProcess.getProcs().itemAdded.addObserver(new Observer()
          {
            public void update (Observable arg0, Object obj)
            {
              addProc((GuiProc) obj);
            }
          });

          debugProcess.getProcs().itemRemoved.addObserver(new Observer()
          {
            public void update (Observable arg0, Object obj)
            {
              removeProc((GuiProc)obj);
            }
          });
        this.sessionMounted = true;
      }
    }

    protected void removeProc (GuiProc proc)
    {
      // TODO: Should processes be removed or or grayed out ?
      throw new RuntimeException("This method is not implemented (Nov 4, 2006)");
    }
}
