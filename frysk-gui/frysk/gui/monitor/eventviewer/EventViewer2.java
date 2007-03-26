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

import org.gnu.gtk.Adjustment;
import org.gnu.gtk.AttachOptions;
import org.gnu.gtk.HScrollBar;
import org.gnu.gtk.Label;
import org.gnu.gtk.Table;
import org.gnu.gtk.VScrollBar;
import org.gnu.gtk.Viewport;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;

import frysk.gui.monitor.GuiProc;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;

public class EventViewer2 extends Table {
	
  int x;
  int y;
  int width;
  int height;
  boolean sessionMounted = false;
//  SizeGroup lablesSizeGroup;
  
  Table bigTable;
  int bigTableNumberOfRows; 

  VScrollBar vScrollBar;
  HScrollBar hScrollBar;
  
  private Session currentSession;
  
  TimeLineSelectionManager timeLineSelectionManager;
  LinkedList procBoxes;
  
  public EventViewer2(){
		super(2, 3, false);
		this.setBorderWidth(6);
        
        this.timeLineSelectionManager = new TimeLineSelectionManager();
        
        this.procBoxes = new LinkedList();
        
        Label spacerLabel = new Label("");
        TimeLine.addToLabelsSizeGroup(spacerLabel);
        
        vScrollBar = new VScrollBar((Adjustment)null);
        hScrollBar = new HScrollBar((Adjustment)null);
        
        this.bigTableNumberOfRows = 0;
        this.bigTable = new Table(bigTableNumberOfRows,2,false);
        this.bigTable.setBorderWidth(6);
        
        Viewport bigViewport = new Viewport(null,vScrollBar.getAdjustment());
      
        bigViewport.setMinimumSize(0, 0);
        
        bigViewport.add(bigTable);
        
        AttachOptions EXPAND_AND_FILL = AttachOptions.EXPAND.or(AttachOptions.FILL);
        this.attach(bigViewport, 0, 2, 0, 1, EXPAND_AND_FILL, EXPAND_AND_FILL, 0, 0);
        this.attach(vScrollBar, 2, 3, 0, 1, AttachOptions.FILL, EXPAND_AND_FILL, 0, 0);
        this.attach(hScrollBar, 1, 2, 1, 2, EXPAND_AND_FILL, AttachOptions.FILL, 0, 0);
        this.attach(spacerLabel, 0, 1, 1, 2, AttachOptions.FILL, AttachOptions.FILL, 0, 0);
        
        this.showAll();
        this.getAccessible().setName("EventViewer");
        
        
        Observer selectionObserver = new Observer(){
          public void update (Observable observable, Object obj)
          {
              draw();
          }
        };
        timeLineSelectionManager.getSelectedTimeLines().itemAdded.addObserver(selectionObserver);
        timeLineSelectionManager.getSelectedTimeLines().itemRemoved.addObserver(selectionObserver);
        
        this.addListener(new ExposeListener()
        {
          public boolean exposeEvent (ExposeEvent event)
          {
            // this is a possible performance problem
            // but gets rid of all the artifacts.
            // see http://sourceware.org/bugzilla/show_bug.cgi?id=4269
            draw();
            return false;
          }
        });
	}

	
    public void setSession(Session session){
      this.currentSession = session;
      if(!sessionMounted){
        this.mountSession();  
      }
    }
     
    private void addProc(GuiProc guiProc){
      ProcBox procBox = new ProcBox(guiProc,this.hScrollBar.getAdjustment(), timeLineSelectionManager);
      this.procBoxes.add(procBox);
      
      this.bigTableNumberOfRows++;
      this.bigTable.resize(2,this.bigTableNumberOfRows);
      
      AttachOptions EXPAND_AND_FILL = AttachOptions.EXPAND.or(AttachOptions.FILL);
      this.bigTable.attach(procBox, 0, 1, bigTableNumberOfRows-1, bigTableNumberOfRows, EXPAND_AND_FILL,EXPAND_AND_FILL, 3 , 3);
      
      this.showAll();
    }
    
    private void mountSession(){

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
      Iterator iterator = this.procBoxes.iterator();
      while (iterator.hasNext())
        {
          ProcBox procBox = (ProcBox) iterator.next();
          if(procBox.getGuiProc() == proc){
            procBox.procIsDead();
          }
        }
    }
}
