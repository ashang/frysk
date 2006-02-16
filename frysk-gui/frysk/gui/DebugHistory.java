package frysk.gui;

import org.freedesktop.cairo.Point;
import org.gnu.gdk.Color;
import org.gnu.gdk.Cursor;
import org.gnu.gdk.CursorType;
import org.gnu.gdk.EventMask;
import org.gnu.gdk.GdkCairo;
import org.gnu.gtk.DrawingArea;
import org.gnu.gtk.Label;
import org.gnu.gtk.Window;
import org.gnu.gtk.WindowType;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;
import org.gnu.gtk.event.MouseMotionEvent;
import org.gnu.gtk.event.MouseMotionListener;

public class DebugHistory extends DrawingArea implements ExposeListener, MouseMotionListener, MouseListener{
	
	boolean isOverEvent = false;
	
	public DebugHistory(){
		super();
		this.setEvents(EventMask.ALL_EVENTS_MASK);
		this.setMinimumSize(10000,0);
		this.addListener((ExposeListener)this);
		this.addListener((MouseMotionListener)this);
		this.addListener((MouseListener) this);
	}

	public boolean exposeEvent(ExposeEvent arg0) {
		if(arg0.isOfType(ExposeEvent.Type.NO_EXPOSE))
			return false;
		
		GdkCairo cairo = new GdkCairo(this.getWindow());
		
		int x = arg0.getArea().getX();
		int y = arg0.getArea().getY();
		int width = arg0.getArea().getWidth();
		int height = this.getWindow().getHeight();
		
		// White background
		cairo.setSourceColor(Color.WHITE);
		cairo.rectangle(new Point(x,y), new Point(x+width, y+height));
		cairo.fill();
		
		// Events every 100 pixels, 20 pixels wide
		cairo.setSourceColor(Color.BLUE);
		for(int i = (x/100)*100; i < x + width; i += 100){
			cairo.moveTo(i, height - 10);
			cairo.lineTo(i, height/2 - 10);
			cairo.relLineTo(10,0);
			cairo.relLineTo(0,height/2);
			cairo.relLineTo(-10,0);
			cairo.closePath();
			cairo.fill();
		}
		cairo.stroke();
		
		// Line accross the bottom
		cairo.setSourceColor(Color.BLACK);
		cairo.moveTo(x, y + height - 10);
		cairo.lineTo(x+width, y+height-10);
		cairo.stroke();
		
		this.showAll();
		
		return true;
	}

	public boolean mouseMotionEvent(MouseMotionEvent arg0) {
		int x = (int)arg0.getX();
		int y = (int)arg0.getY();
		
		if((y >= this.getWindow().getHeight()/2 - 10 && y <= this.getWindow().getHeight() - 10)  
				&& x%100 <= 10){
			this.getWindow().setCursor(new Cursor(CursorType.HAND1));
			isOverEvent = true;
		}
		else{
			this.getWindow().setCursor(new Cursor(CursorType.LEFT_PTR));
			isOverEvent = false;
		}
		
		arg0.refireIfHint();
		
		return false;
	}

	public boolean mouseEvent(MouseEvent arg0) {
	
		if(isOverEvent && arg0.getButtonPressed() == MouseEvent.BUTTON1){
			Window win = new Window(WindowType.TOPLEVEL);
			win.setModal(true);
			win.add(new Label("You've clicked an event!"));
			win.setMinimumSize(400,200);
			win.showAll();
		}
		
		return false;
	}
}
