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
import org.gnu.gtk.Widget;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;

import frysk.gui.monitor.GuiProc;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.Session;
import frysk.gui.sessions.SessionManager;

public class EventViewer2 extends Table {
	
  int x;
  int y;
  int width;
  int height;
  
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
        
        SessionManager.theManager.currentSessionChanged.addObserver(new Observer()
        {
          public void update (Observable observable, Object object)
          {
            setSession((Session) object); 
          }
        });
        
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

	
    private void setSession(Session session){
      this.unmountSession();
      this.currentSession = session;
      this.mountSession();  
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
    
    private void unmountSession(){

      if(currentSession == null){
	return;
      }
      
      this.currentSession.getProcesses().itemAdded.deleteObserver(debugProcessAddedObserver);
      this.currentSession.getProcesses().itemRemoved.deleteObserver(debugProcessRemovedObserver);
      
      Iterator i = this.currentSession.getProcesses().iterator();
      while (i.hasNext())
        {
          DebugProcess debugProcess = (DebugProcess) i.next();
          removeDebugProcess(debugProcess);
        }

      
      Iterator iterator = this.procBoxes.iterator();
      while (iterator.hasNext())
	{
	  Widget widget = (Widget) iterator.next();
	  this.remove(widget);
	}
      this.procBoxes.clear();
      this.bigTableNumberOfRows = 0;
      
      this.timeLineSelectionManager = new TimeLineSelectionManager();
//      timeLineSelectionManager.getSelectedTimeLines().itemAdded.addObserver(selectionObserver);
//      timeLineSelectionManager.getSelectedTimeLines().itemRemoved.addObserver(selectionObserver);
    }
    
    private void mountSession(){
      this.currentSession.getProcesses().itemAdded.addObserver(debugProcessAddedObserver);
      this.currentSession.getProcesses().itemRemoved.addObserver(debugProcessRemovedObserver);
      
      Iterator i = this.currentSession.getProcesses().iterator();
      while (i.hasNext())
        {
          DebugProcess debugProcess = (DebugProcess) i.next();
          addDebugProcess(debugProcess);          
      }
      this.showAll();
    }

    private void addDebugProcess(DebugProcess debugProcess){
      Iterator j = debugProcess.getProcs().iterator();
      while (j.hasNext())
	{
	  addProc((GuiProc) j.next());
	}
      debugProcess.getProcs().itemAdded.addObserver(procAddedObserver);
      debugProcess.getProcs().itemRemoved.addObserver(procRemovedObserver);          
    }
    
    private void removeDebugProcess(DebugProcess debugProcess){
      
      Iterator j = debugProcess.getProcs().iterator();
      debugProcess.getProcs().itemAdded.deleteObserver(procAddedObserver);
      debugProcess.getProcs().itemRemoved.deleteObserver(procRemovedObserver);
      while (j.hasNext())
	{
	  removeProc((GuiProc) j.next());
	}
    }
    
    protected void removeProc (GuiProc proc)
    {
      ProcBox procBox = null;
      Iterator iterator = this.procBoxes.iterator();
      while (iterator.hasNext())
        {
          procBox = (ProcBox) iterator.next();
          if(procBox.getGuiProc() == proc){
            this.bigTable.remove(procBox);
            
            this.bigTableNumberOfRows--;
            this.bigTable.resize(2,this.bigTableNumberOfRows);
            
            this.bigTable.showAll();
            break;
          }
        }
      this.procBoxes.remove(procBox);
    }
    
    private void procIsDead(GuiProc proc){
      Iterator iterator = this.procBoxes.iterator();
      while (iterator.hasNext())
        {
          ProcBox procBox = (ProcBox) iterator.next();
          if(procBox.getGuiProc() == proc){
            procBox.procIsDead();
          }
        }
    }
    
    private Observer procAddedObserver = new Observer()
    {
      public void update (Observable observable, Object object)
      {
	addProc((GuiProc) object);
      }
    };
    
    private Observer procRemovedObserver = new Observer()
    {
      public void update (Observable observable, Object object)
      {
	procIsDead((GuiProc)object);
      }
    };
    
    private Observer debugProcessAddedObserver = new Observer()
    {
      public void update (Observable observable, Object object)
      {
	addDebugProcess((DebugProcess) object);
      }
    };
    
    private Observer debugProcessRemovedObserver = new Observer()
    {
      public void update (Observable observable, Object object)
      {
	removeDebugProcess((DebugProcess) object);
      }
    };
    
}
