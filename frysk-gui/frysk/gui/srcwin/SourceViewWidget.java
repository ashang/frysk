package frysk.gui.srcwin;

import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
import org.gnu.gdk.Drawable;
import org.gnu.gdk.GC;
import org.gnu.gdk.Point;
import org.gnu.gdk.Window;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.StateType;
import org.gnu.gtk.TextBuffer;
import org.gnu.gtk.TextIter;
import org.gnu.gtk.TextView;
import org.gnu.gtk.TextWindowType;
import org.gnu.gtk.event.ExposeEvent;
import org.gnu.gtk.event.ExposeListener;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;
import org.gnu.javagnome.Handle;
import org.gnu.pango.Alignment;
import org.gnu.pango.Layout;

/** 
 * This class is used to add some functionality to TextView that may be needed
 * by the source window but is not directly available or easily accessible in
 * TextView.
 * 
 * This widget uses a SourceBuffer instead of a TextBuffer. A SourceBuffer
 * allows some extras that are needed by the source window such as break-
 * point management.
 * 
 * @author ifoox, ajocksch
 *
 */
public class SourceViewWidget extends TextView implements ExposeListener, MouseListener{

	/* BEGIN CONSTANTS */
	public static final String LNF_NODE = "lnf";
	public static final String SYNTAX_NODE = "syntax";
	
	public static final String TEXT_B = "textB";
	public static final String TEXT_G = "textG";
	public static final String TEXT_R = "textR";

	public static final String MARK_B = "markB";
	public static final String MARK_G = "markG";
	public static final String MARK_R = "markR";	
	
	public static final String BG_B = "bgB";
	public static final String BG_G = "bgG";
	public static final String BG_R = "bgR";

	public static final String MARGIN_B = "marginB";
	public static final String MARGIN_G = "marginG";
	public static final String MARGIN_R = "marginR";

	public static final String LINE_NUM_B = "lineNumB";
	public static final String LINE_NUM_G = "lineNumG";
	public static final String LINE_NUM_R = "lineNumR";
	
	public static final String CURRENT_LINE_R = "currentLineR";
	public static final String CURRENT_LINE_G = "currentLineG";
	public static final String CURRENT_LINE_B = "currentLineB";

	public static final String SEARCH_G = "searchG";
	public static final String SEARCH_B = "searchB";
	public static final String SEARCH_R = "searchR";
	
	public static final String SHOW_LINE_NUMBERS = "showLineNumbers";
	public static final String SHOW_EXEC_MARKERS = "showExecMarkers";
	/* END CONSTANTS */
	
	// my SourceBuffer
	protected SourceBuffer buf;
	
	// preferences model nodes
	private Preferences topPrefs;
	private Preferences lnfPrefs;
	
	// How far to start writing breakpoints, etc. from the left side of the margin
	private int marginWriteOffset;
	public static final String FUNCTION_WEIGHT = "function_weight";
	public static final String FUNCTION_R = "function_r";
	public static final String FUNCTION_G = "function_b";
	public static final String FUNCTION_B = "function_g";
	public static final String ID_WEIGHT = "id_weight";
	public static final String LITERAL_WEIGHT = "literal_weight";
	public static final String ID_B = "id_b";
	public static final String ID_G = "id_g";
	public static final String ID_R = "id_r";
	public static final String LITERAL_B = "literal_b";
	public static final String LITERAL_G = "literal_g";
	public static final String LITERAL_R = "literal_r";

	
	/**
	 * Constructs a new SourceViewWidget. If you don't specify a buffer before using it,
	 * a default one will be created for you.
	 * 
	 * @param parentPrefs The root node of the preference model to use
	 */
	public SourceViewWidget(Preferences parentPrefs) {
		super(gtk_text_view_new());
		this.buf = new SourceBuffer();
		this.setBuffer(this.buf);
		this.topPrefs = parentPrefs;
		this.lnfPrefs = parentPrefs.node(LNF_NODE);
		this.initialize();
	}

	/**
	 * Creates a new SourceViewWidget widget displaying the buffer buffer. One buffer
	 * can be shared among many widgets. 
	 * 
	 * @param buffer Buffer to use
	 * @param parentPrefs The root node of the preference model to use
	 */
	public SourceViewWidget(SourceBuffer buffer, Preferences parentPrefs) {
		super(gtk_text_view_new_with_buffer(buffer.getHandle()));
		this.buf = buffer;
		this.topPrefs = parentPrefs;
		this.lnfPrefs = parentPrefs.node(LNF_NODE);
		this.initialize();
	}

	/**
	 * Construct a SourceViewWidget from a handle to a native resource.
	 * 
	 * @param handle Handle to a native resource
	 * @param parentPrefs The root node of the preference model to use
	 */
	public SourceViewWidget(Handle handle, Preferences parentPrefs) {
		super(handle);
	}
	
	/**
	 * Redraws the SourceViewWidget on screen, taking changes in the preference model
	 * into account
	 */
	public void refresh(){
		// Look & Feel
		int r = this.lnfPrefs.getInt(TEXT_R, 0);
		int g = this.lnfPrefs.getInt(TEXT_G, 0);
		int b = this.lnfPrefs.getInt(TEXT_B, 0);
		this.setTextColor(StateType.NORMAL, new Color(r,g,b));
		
		r = this.lnfPrefs.getInt(BG_R, 65535);
		g = this.lnfPrefs.getInt(BG_G, 65535);
		b = this.lnfPrefs.getInt(BG_B, 65535);
		this.setBaseColor(StateType.NORMAL, new Color(r,g,b));
	
		this.buf.updatePreferences(this.topPrefs);
		
		// Sidebar
		if(this.lnfPrefs.getBoolean(SHOW_LINE_NUMBERS, true)){
			Layout lo = new Layout(this.getContext());
			lo.setText(""+this.buf.getLineCount()+1);
			this.marginWriteOffset = lo.getPixelWidth();
			this.setBorderWindowSize(TextWindowType.LEFT, this.marginWriteOffset+20);
		}
		else{
			this.setBorderWindowSize(TextWindowType.LEFT, 20);
			this.marginWriteOffset = 0;
		}
	}

	/**
	 * Returns the SourceBuffer being used
	 * @return The SourceBuffer used in the widget.
	 */
	public TextBuffer getBuffer() {
	    return buf;
	}

	/**
	 * Implementation from ExposeListener Interface. When the 
	 */
	public boolean exposeEvent(ExposeEvent event) {
		// Ignore events that aren't expose events or don't have anything
		// to do with the sidebar
		if(event.isOfType(ExposeEvent.Type.NO_EXPOSE) ||
				!event.getWindow().equals(this.getWindow(TextWindowType.LEFT)))
			return false;
		
		this.drawMargin();
		
		return false;
	} 
	
	/**
	 * Called in response to the user clicking on the text area. In the future
	 * this will be used to be able to mouse-over variables and show their contents
	 */
	public boolean mouseEvent(MouseEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();
		
		// Middle click over the main text area will trigger the variable-finding
		if(event.getButtonPressed() == MouseEvent.BUTTON2 
				&& event.isOfType(MouseEvent.Type.BUTTON_PRESS)){
			
			Point p = this.windowToBufferCoords(TextWindowType.TEXT, x, y);
			
			TextIter iter = this.getIterAtLocation(p.getX(), p.getY());
			
			Variable var = this.buf.getVariable(iter);
			
			if(var == null)
				return false;
			
			System.out.println(var.getName());
			return true;
		}
		// right-click on the border means toggle a breakpoint
		else if(event.getButtonPressed() == MouseEvent.BUTTON3 
				&& event.getWindow().equals(this.getWindow(TextWindowType.LEFT))
				&& event.isOfType(MouseEvent.Type.BUTTON_PRESS)){
			Point p = this.windowToBufferCoords(TextWindowType.TEXT, 0, y);
			
			TextIter iter = this.getIterAtLocation(p.getX(), p.getY());
			final int lineNum = iter.getLineNumber();
			
			// only popup a window if the line is executable
			if(this.buf.isLineExecutable(lineNum)){
				Menu m = new Menu();
				MenuItem mi = new MenuItem("Toggle Breakpoint", false);
				m.append(mi);
				mi.addListener(new MenuItemListener() {
					public void menuItemEvent(MenuItemEvent event) {
						SourceViewWidget.this.buf.toggleBreakpoint(lineNum);
					}
				});
				m.popup();
				m.showAll();
			}
		}
		
		return false;
	}
	
	/**
	 * Sets the current line for the buffer
	 * @param lineNum the current PC line
	 */
	public void setCurrentLine(int lineNum){
		this.buf.setCurrentLine(lineNum);
	}
	
	/**
	 * Scrolls the TextView so that the given line is visible in the widget
	 * @param lineNum The line to scroll to
	 */
	public void scrollToLine(int lineNum){
		this.scrollToIter(this.buf.getLineIter(lineNum), 0);
	}
	
	/**
	 * Finds the next instance of toFind in the buffer and highlights it.
	 * returns true if succesful, false otherwise
	 * 
	 * @param toFind The string to find
	 * @param caseSensitive Whether to do a case sensitive search
	 * @return If the search was successful
	 */
	public boolean findNext(String toFind, boolean caseSensitive){
		return this.buf.findNext(toFind, caseSensitive, false);
	}
	
	/**
	 * Finds the previous instance of toFind in the buffer and highlights it.
	 * Returns true if succesful, false otherwise
	 * 
	 * @param toFind The string to find
	 * @param caseSensitive Whether to do a case sensitive search
	 * @return If the search was successful
	 */
	public boolean findPrevious(String toFind, boolean caseSensitive){
		return this.buf.findPrevious(toFind, caseSensitive);
	}
	
	/**
	 * Finds all instances of toFind in the current buffer and highlights
	 * them. Returns true if successful, false otherwise
	 * 
	 * @param toFind The string to find in the buffer
	 * @param caseSensitive Whether to do a case sensitive search
	 * @return If the search was successful
	 */
	public boolean highlightAll(String toFind, boolean caseSensitive){
		return this.buf.findNext(toFind, caseSensitive, true);
	}
	
	/**
	 * Scrolls the TextView to show the current location of the found text
	 */
	public void scrollToFound(){
		this.scrollToIter(this.buf.getStartCurrentFind(), 0);
	}
	
	/*---------------------------*
	 * PRIVATE METHODS           *
	 *---------------------------*/
	
	/*
	 * Performs some operations before the window is shown
	 */
	private void initialize(){
		// Set all preference-related data
		this.refresh();
		
		// Stuff that never changes
		this.setLeftMargin(3);
		this.setEditable(false);
		this.setCursorVisible(false);
		
		// Listeners
		this.addListener((ExposeListener) this);
		this.addListener((MouseListener) this);
		
		this.showAll();
	}
	
	/*
	 * Function responsible for drawing the side area where breakpoints, etc. are
	 * drawn. Called either in response to an expose event or when a preference 
	 * changes
	 */
	private void drawMargin(){
		Window drawingArea = this.getWindow(TextWindowType.LEFT);
		
		// draw the background for the margin
		GC context = new GC((Drawable) drawingArea);
		int r = this.lnfPrefs.getInt(MARGIN_R, 54741);
		int g = this.lnfPrefs.getInt(MARGIN_G, 56283);
		int b = this.lnfPrefs.getInt(MARGIN_B, 65535);
		context.setRGBForeground(new Color(r, g, b));
		drawingArea.drawRectangle(context, true, 0, 0, drawingArea.getWidth(), drawingArea.getHeight());
		
		// get preference settings
		boolean showLines = this.lnfPrefs.getBoolean(SHOW_LINE_NUMBERS, true);
		boolean showMarks = this.lnfPrefs.getBoolean(SHOW_EXEC_MARKERS, true);
		
		// get the y coordinates for the top and bottom of the window
		int minY = drawingArea.getClipRegion().getClipbox().getY();
		int maxY = minY+drawingArea.getClipRegion().getClipbox().getHeight();
		
		// find out what the actual starting coordinates of the first line on screen is
		TextIter firstIter = this.getIterAtLocation(this.windowToBufferCoords(TextWindowType.LEFT, 0, minY));
		VerticalLineRange firstRange = this.getLineYRange(firstIter);
		int actualFirstStart = this.bufferToWindowCoords(TextWindowType.LEFT, 0, firstRange.getY()).getY();
		
		// get the line numbers we'll be drawing
		int firstLine = firstIter.getLineNumber();
		int lastLine = this.getIterAtLocation(this.windowToBufferCoords(TextWindowType.LEFT, 0, maxY)).getLineNumber();
		
		// Get Color to draw the text in
		r = this.lnfPrefs.getInt(LINE_NUM_R, 0);
		g = this.lnfPrefs.getInt(LINE_NUM_G, 0);
		b = this.lnfPrefs.getInt(LINE_NUM_B, 0);
		context.setRGBForeground(new Color(r,g,b));
		
		int currentHeight = 0;
		
		for(int i = firstLine; i <= lastLine && i < this.buf.getLineCount() -1; i++){
			// For the current line, draw background using the currentLine color
			if(i == this.buf.getCurrentLine()){
				int lineHeight = this.getLineYRange(this.getBuffer().getLineIter(i)).getHeight();
				
				int lineR = this.lnfPrefs.getInt(CURRENT_LINE_R, 30000);
				int lineG = this.lnfPrefs.getInt(CURRENT_LINE_G, 65535);
				int lineB = this.lnfPrefs.getInt(CURRENT_LINE_B, 30000);
				context.setRGBForeground(new Color(lineR, lineG, lineB));
				drawingArea.drawRectangle(context, true, 0, actualFirstStart+currentHeight, 
					this.marginWriteOffset+20, lineHeight);
				context.setRGBForeground(new Color(r,g,b));
			}
			
			// Draw line numbers
			if(showLines){
				Layout lo = new Layout(this.getContext());
				lo.setAlignment(Alignment.RIGHT);
				lo.setWidth(this.marginWriteOffset);
				lo.setText(""+(i+1));
				
				drawingArea.drawLayout(context, this.marginWriteOffset, actualFirstStart+currentHeight, lo);
			}
			
			// If it is executable, draw a mark
			if(showMarks && this.buf.isLineExecutable(i)){
				int lineHeight = this.getLineYRange(this.getBuffer().getLineIter(i)).getHeight();
				int iconStart = lineHeight/2;
				
				int markR = this.lnfPrefs.getInt(MARK_R, 0);
				int markG = this.lnfPrefs.getInt(MARK_G, 0);
				int markB = this.lnfPrefs.getInt(MARK_B, 0);
				context.setRGBForeground(new Color(markR,markG,markB));
				drawingArea.drawLine(context, this.marginWriteOffset+5, actualFirstStart+currentHeight+iconStart, 
						this.marginWriteOffset+12, actualFirstStart+currentHeight+iconStart);
				context.setRGBForeground(new Color(r,g,b));
			}
			
			// draw breakpoints
			if(this.buf.isLineBroken(i)){
				int lineHeight = this.getLineYRange(this.getBuffer().getLineIter(i)).getHeight();
				int iconHeight = lineHeight - 8;
				
				context.setRGBForeground(new Color(65535,0,0));
				drawingArea.drawRectangle(context, true, this.marginWriteOffset+5, actualFirstStart+currentHeight+4, iconHeight, iconHeight);
				context.setRGBForeground(new Color(r,g,b));
			}
			
			// update height for next line
			currentHeight += this.getLineYRange(this.getBuffer().getLineIter(i)).getHeight();
		}
	}
	
}
