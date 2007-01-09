package frysk.gui.monitor.eventviewer;

import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import org.gnu.glib.Handle;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.VBox;

import frysk.gui.monitor.GuiProc;
import frysk.gui.monitor.ObservableLinkedList;
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
//        this.addListener((ExposeListener)this);
//        this.addListener((MouseMotionListener)this);
//        this.addListener((MouseListener) this);
        
//        this.setEvents(EventMask.ALL_EVENTS_MASK.xor(EventMask.POINTER_MOTION_HINT_MASK));
  
        this.lablesSizeGroup = new SizeGroup(SizeGroupMode.HORIZONTAL);
        
        // watch the event list for updates
        // redraw upon updates
        ObservableLinkedList eventList = EventManager.theManager.getEventsList();
        Observer drawObserver = new Observer()
        {
          public void update (Observable arg0, Object arg1)
          {
            EventViewer2.this.draw();
          }
        
        };
       eventList.itemAdded.addObserver(drawObserver);
       eventList.itemRemoved.addObserver(drawObserver);
       
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
      // TODO Auto-generated method stub
      throw new RuntimeException("This method is not implemented (Nov 4, 2006)");
    }
}
