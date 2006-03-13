package frysk.gui;

import java.util.Vector;

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

/**
 * The DebugHistory widget is intended to display the events
 * that have been triggered up to an abnormal termination of a 
 * program. It allows the user to scroll backwards and forwards through
 * the lifetime of the process (up to a user-defined point), change 
 * the threshold of importance for what events are displayed, and upon
 * clicking on an event recieve more details about the event
 *
 */
public class DebugHistory extends DrawingArea implements ExposeListener, MouseMotionListener, MouseListener{
	
	boolean isOverEvent = false;
	
	private int threshold;
	
	Vector events;
	
	/**
	 * Creates a new debug history widget
	 *
	 * TODO: This is still a mockup, we need a way to get the information
	 */
	public DebugHistory(int threshold){
		super();
		
		this.threshold = threshold;
		
		this.events = new Vector();
		for(int i = 0; i < 100; i++)
			events.add(new ObserverEvent(i*25, i%10, "Test " + i));
		this.setMinimumSize(events.size()*25,0);
		
		this.addListener((ExposeListener)this);
		this.addListener((MouseMotionListener)this);
		this.addListener((MouseListener) this);
		
		this.setEvents(EventMask.ALL_EVENTS_MASK);
	}

	/**
	 * Does the actual drawing of the widget.
	 */
	public boolean exposeEvent(ExposeEvent arg0) {
		
		if(arg0.isOfType(ExposeEvent.Type.NO_EXPOSE) || !arg0.getWindow().equals(this.getWindow()))
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
		
		// Draw each visible event
		for(int i = (x/25)*25; i < x + width; i += 25){
			ObserverEvent event = ((ObserverEvent) this.events.elementAt(i/25));
			if(event.importance >= this.threshold)
				event.draw(cairo, height);
		}
		
		// Line accross the bottom
		cairo.setSourceColor(Color.BLACK);
		cairo.moveTo(x, y + height - 10);
		cairo.lineTo(x+width, y+height-10);
		cairo.stroke();
		
		this.showAll();
		
		return true;
	}

	/**
	 * Whenever the mouse is over an event, change the cursor to a hand
	 */
	public boolean mouseMotionEvent(MouseMotionEvent arg0) {
		int x = (int)arg0.getX();
		int y = (int)arg0.getY();
		
		if((y >= this.getWindow().getHeight()/2 - 10 && y <= this.getWindow().getHeight() - 10)  
				&& x%25 <= 10){
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
	
	public void setThreshold(int newThreshold){
		this.threshold = newThreshold;
		this.draw();
	}

	/**
	 * If the mouse is over an event and the user clicks, open a window
	 * with more information about the event.
	 */
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
	
	/*
	 * Private class to represent a time when a observer fired. In the future this
	 * will be replaced by some other, externally visible data structure.
	 */
	private class ObserverEvent{
		int time;
		int importance;
		String text;
		
		public ObserverEvent(int time, int importance, String text){
			this.time = time;
			this.importance = importance;
			this.text = text;
		}
		
		/*
		 * Events are responsable for drawing themselves on the
		 * provided Cairo context.
		 */
		public void draw(GdkCairo cairo, int height){
			cairo.save();
			
			
			cairo.setSourceColor(Color.BLUE);
			cairo.moveTo(time, height - 10);
			cairo.relLineTo(0, ((double)-height/2.0) * ((double)this.importance + 1.0)/10.0);
			cairo.relLineTo(10,0);
			cairo.relLineTo(0,((double)height/2.0) * ((double)this.importance + 1.0)/10.0);
			cairo.relLineTo(-10,0);
			cairo.closePath();
			cairo.fill();
			
			// Text over each event
			cairo.setSourceColor(Color.BLACK);
			cairo.newPath();
			cairo.moveTo(time+5, height/2 - 10);
			cairo.rotate(Math.PI/-4); // 45 degrees counter-clockwise
			cairo.showText(text);
			cairo.stroke();
			
			cairo.restore();
		}
	}
}
