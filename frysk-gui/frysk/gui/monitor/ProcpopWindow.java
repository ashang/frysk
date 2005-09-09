package frysk.gui.monitor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.gnu.gdk.DragAction;
import org.gnu.gdk.ModifierType;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.DestDefaults;
import org.gnu.gtk.Label;
import org.gnu.gtk.Notebook;
import org.gnu.gtk.RadioButton;
import org.gnu.gtk.TargetEntry;
import org.gnu.gtk.TargetFlags;
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

import frysk.gui.FryskGui;

public class ProcpopWindow extends Window implements Saveable{
	
	private RadioButton allRadioButton;
	private RadioButton filteredRadioButton;
	
	private Notebook noteBook;
	
	private boolean refreshing;
	private LibGlade glade;
	
	private WatchMenu menu;

	private AllProcWidget allProcWidget;
	
	private Logger errorLog = Logger.getLogger(FryskGui.ERROR_LOG_ID);
	public ProcpopWindow(LibGlade glade) throws IOException {
		super(((Window)glade.getWidget("procpopWindow")).getHandle());
	
		this.glade = glade;
		
		try {
			this.allProcWidget = new AllProcWidget(glade);
		} catch (IOException e)
		{
			errorLog.log(Level.SEVERE,"IOException from Proc Widget",e);
		}
		
		this.noteBook = (Notebook) glade.getWidget("noteBook");
		
		Label label = (Label) glade.getWidget("allPageLabel");
				
		TargetEntry[] entries = new TargetEntry[1];
		entries[0] = new TargetEntry("tap", TargetFlags.NO_RESTRICTION, 0);
		
		this.noteBook.setDragSource(ModifierType.BUTTON1_MASK, entries , DragAction.COPY);
		this.noteBook.setDragDestination(DestDefaults.ALL, entries, DragAction.COPY);
		
		this.noteBook.addListener(new DragOriginListener(){

			public void dragStarted(StartDragEvent event) {
				System.out.println("dragStarted");
			}

			public void dragEnded(EndDragEvent event) {
				System.out.println("dragEnded");
			}

			public void dataRequested(RequestDragDataEvent arg0) {
				System.out.println("dataRequested");				
			}

			public void dataDeleted(DeleteDragDataEvent arg0) {
				System.out.println("dataDeleted");				
			}
			
		});
		
		this.noteBook.addListener(new DragTargetListener(){

			public void destinationLeft(LeaveDragDestinationEvent arg0) {
				System.out.println("----destinationLeft---");
			}

			public boolean dropped(DropDragEvent arg0) {
				System.out.println("----dropped---");
				return false;
			}

			public void dataReceived(ReceiveDragDataEvent arg0) {
				 System.out.println("----dataReceived---");				
			}

			public boolean motionOcurred(DragMotionEvent arg0) {
				System.out.println("motionOcurred");
				return false;
			}
			
		});

		this.menu = new WatchMenu();
		this.showAll();
	}

	public void save(Preferences prefs) {
		prefs.putInt("position.x", this.getPosition().getX());
		prefs.putInt("position.y", this.getPosition().getY());
		
		prefs.putInt("size.height", this.getSize().getHeight());
		prefs.putInt("size.width", this.getSize().getWidth());
		
		allProcWidget.save(Preferences.userRoot().node(prefs.absolutePath() + "/allProcWidget"));
	}

	public void load(Preferences prefs) {
		int x = prefs.getInt("position.x", this.getPosition().getX());
		int y = prefs.getInt("position.y", this.getPosition().getY());
		this.move(x,y);
		
		int width  = prefs.getInt("size.height", this.getSize().getHeight());
		int height = prefs.getInt("size.width", this.getSize().getWidth());
		this.resize(width, height);
		
		allProcWidget.load(Preferences.userRoot().node(prefs.absolutePath() + "/allProcWidget"));
	}
	
}

