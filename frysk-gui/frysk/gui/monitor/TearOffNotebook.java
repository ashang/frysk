/*
 * Created on Oct 18, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package frysk.gui.monitor;

import java.util.Hashtable;

import org.gnu.gdk.DragAction;
import org.gnu.gdk.ModifierType;
import org.gnu.glib.Handle;
import org.gnu.gtk.DestDefaults;
import org.gnu.gtk.Label;
import org.gnu.gtk.Notebook;
import org.gnu.gtk.TargetEntry;
import org.gnu.gtk.TargetFlags;
import org.gnu.gtk.Widget;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.DeleteDragDataEvent;
import org.gnu.gtk.event.DragMotionEvent;
import org.gnu.gtk.event.DragOriginListener;
import org.gnu.gtk.event.DragTargetListener;
import org.gnu.gtk.event.DropDragEvent;
import org.gnu.gtk.event.EndDragEvent;
import org.gnu.gtk.event.LeaveDragDestinationEvent;
import org.gnu.gtk.event.ReceiveDragDataEvent;
import org.gnu.gtk.event.RequestDragDataEvent;
import org.gnu.gtk.event.StartDragEvent;


public class TearOffNotebook extends Notebook {

	/**
	 * If the window containing this Nonebook was created as 
	 * a result of a drag and drop this will store a reference 
	 * to that window, null otherwise;
	 * */
	private Window window;
	
	/**
	 * A hash table of all the notebooks that were created as a
	 * result of a drop even from this table. uses gdk window as
	 * a hash key.
	 * */
	private static Hashtable children = new Hashtable();
	
	public TearOffNotebook(Handle handle){
		super(handle);
		this.window = null;
		children.put(getRootWindow(), this);
		setupDranAndDrop();
	}
	
	public TearOffNotebook(Window window){
		super();
		this.window = window;
		setupDranAndDrop();
	}
	
	public Window getNotebookWindow() {
		return window;
	}

	private void setupDranAndDrop(){
		final TargetEntry[] entries = new TargetEntry[1];
		entries[0] = new TargetEntry("tap", TargetFlags.NO_RESTRICTION, 0);
		
		this.setDragSource(ModifierType.BUTTON1_MASK, entries , DragAction.COPY);
		this.setDragDestination(DestDefaults.ALL, entries, DragAction.COPY);
		
		this.addListener(new DragOriginListener(){

			public void dragStarted(StartDragEvent event) {
//				System.out.println("ORIGIN: dragStarted");
			}

			public void dragEnded(EndDragEvent event) {
//				System.out.println();
//				System.out.println("ORIGIN: dropped");
//				System.out.println("ORIGIN: source " + event.getDragContext().getSourceWindow());
//				System.out.println("ORIGIN: dest   " + event.getDragContext().getDestinationWindow());
//				System.out.println("ORIGIN: dest*  " + event.getSource());
//				System.out.println("ORIGIN: this   " + getRootWindow());
//				System.out.println();
				
				if(event.getDragContext().getDestination() == null){
					Window window = new Window();
					TearOffNotebook newNotebook = new TearOffNotebook(window);

					final Widget widget = getPage(getCurrentPage());
//					final Label  label  = new Label(getTabLabelText(widget));
					final Label  label  = new Label("new");
					removePage(getCurrentPage());
					newNotebook.appendPage(widget, label);
			
					window.add(newNotebook);
					System.out.println("newNotebook window: " + window.getRootWindow());
					children.put(window.getRootWindow(), newNotebook);
					
					
					org.gnu.gdk.Window gdkWindow = event.getDragContext().getSource();

					window.resize(200, 300);
					window.realize();
					window.showAll();
					
					window.setTitle(newNotebook.toString());
				}
			}

			public void dataRequested(RequestDragDataEvent arg0) {
			}

			public void dataDeleted(DeleteDragDataEvent arg0) {
			}
			
		});
		
		this.addListener(new DragTargetListener(){

			public void destinationLeft(LeaveDragDestinationEvent arg0) {
			}

			public boolean dropped(DropDragEvent event) {
//				System.out.println();
//				System.out.println("TARGET: dropped");
//				System.out.println("TARGET: source " + event.getDragContext().getSource());
//				System.out.println("TARGET: dest   " + event.getDragContext().getDestination());
//				System.out.println("TARGET: dest*  " + event.getSource());
//				System.out.println("TARGET: this   " + getRootWindow());
//				System.out.println();
//				
//				TearOffNotebook source = (TearOffNotebook) event.getSource();
//
//				final Widget widget = source.getPage(source.getCurrentPage());
//				//final Label  label  = new Label(source.getTabLabelText(widget));
//				final Label  label  = new Label("*****");
//				source.removePage(source.getCurrentPage());
//				appendPage(widget, label);
//
//				Widget topWidget = getToplevel();
//				if(getNumPages() == 0){
//					topWidget.destroy();
//				}
				
				return false;
			}
			
			public void dataReceived(ReceiveDragDataEvent arg0) {
			}

			public boolean motionOcurred(DragMotionEvent arg0) {
				return false;
			}
			
		});
	}
	
	
}
