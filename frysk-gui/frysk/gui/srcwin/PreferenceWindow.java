package frysk.gui.srcwin;

import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CheckButton;
import org.gnu.gtk.ColorButton;
import org.gnu.gtk.Label;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.pango.Weight;

/**
 * The aim of this window is to provide a place for the user to change various
 * preferences relating to look and feel, debugging feedback, etc. This window by
 * itself does not refresh the source window when it is closed, rather the source
 * window needs to call attachLifeCycleListener to deal with the hiding of this 
 * window
 * 
 * @author ajocksch
 *
 */

public class PreferenceWindow implements ButtonListener{

	private static final String FUNCTION_COLOR = "functionColorButton"; //$NON-NLS-1$
	private static final String ID_COLOR = "idColorButton"; //$NON-NLS-1$
	private static final String LITERAL_COLOR = "literalColorButton"; //$NON-NLS-1$
	private static final String FUNCTION_BOLD_BUTTON = "functionBoldButton"; //$NON-NLS-1$
	private static final String ID_BOLD_BUTTON = "idBoldButton"; //$NON-NLS-1$
	private static final String LITERAL_BOLD_BUTTON = "literalBoldButton"; //$NON-NLS-1$
	private static final String SYNTAX_LABEL = "syntaxLabel"; //$NON-NLS-1$
	private static final String SETTINGS_LABEL = "settingsLabel"; //$NON-NLS-1$
	private static final String LNF_LABEL = "lnfLabel"; //$NON-NLS-1$
	private static final String FUNCTION_COLOR_LABEL = "functionColorLabel"; //$NON-NLS-1$
	private static final String ID_COLOR_LABEL = "idColorLabel"; //$NON-NLS-1$
	private static final String LITERAL_COLOR_LABEL = "literalColorLabel"; //$NON-NLS-1$
	/* BEGIN CONSTANTS */
	private static final String GLADE_PATH = "frysk-gui/frysk/gui/srcwin/glade/"; //$NON-NLS-1$
	private static final String GLADE_FILE = "frysk_source_prefs.glade"; //$NON-NLS-1$

	private static final String PREF_WIN = "prefWin"; //$NON-NLS-1$
	
	private static final String LINE_NUM_CHECK = "lineNumCheck"; //$NON-NLS-1$
	private static final String MARKER_CHECK = "markerCheck"; //$NON-NLS-1$
	
	private static final String MARK_COLOR = "markColor"; //$NON-NLS-1$
	private static final String LINE_NUM_COLOR = "lineNumColor"; //$NON-NLS-1$
	private static final String SIDEBAR_COLOR = "sidebarColor"; //$NON-NLS-1$
	private static final String BACKGROUND_COLOR = "backgroundColor"; //$NON-NLS-1$
	private static final String TEXT_COLOR = "textColor"; //$NON-NLS-1$
	private static final String CURRENT_LINE_COLOR = "currentLineColor"; //$NON-NLS-1$
	
	private static final String OK_BUTTON = "okButton"; //$NON-NLS-1$
	private static final String CANCEL_BUTTON = "cancelButton"; //$NON-NLS-1$
	
	private static final String EXEC_MARK_COLOR_LABEL = "execMarkColorLabel"; //$NON-NLS-1$
	private static final String LINE_NUM_COLOR_LABEL = "lineNumColorLabel"; //$NON-NLS-1$
	private static final String SIDEBAR_COLOR_LABEL = "sidebarColorLabel"; //$NON-NLS-1$
	private static final String BG_COLOR_LABEL = "bgColorLabel"; //$NON-NLS-1$
	private static final String TEXT_COLOR_LABEL = "textColorLabel"; //$NON-NLS-1$
	private static final String CURRENT_LINE_COLOR_LABEL = "currentLineColorLabel"; //$NON-NLS-1$
	/* END CONSTANTS */
	
	private Preferences myPrefs;
	
	private LibGlade glade;
	
	/**
	 * Creates a new PreferenceWindow
	 * @param myPrefs The preference model to load from 
	 */
	public PreferenceWindow(Preferences myPrefs){
		try {
			this.glade = new LibGlade(GLADE_PATH+GLADE_FILE, this);
		} catch (Exception e){
			e.printStackTrace();
		}
		
		this.myPrefs = myPrefs;
		
		this.setupButtons();
		this.addListeners();
	}
	
	/**
	 * Redisplays the window and updates it's status to reflect the state of the
	 * preference model
	 */
	public void show(){
		this.setupButtons();
		this.glade.getWidget(PREF_WIN).showAll();
	}

	/**
	 * Called in response to either the Ok or Cancel buttons being clicked. If the
	 * Ok button was clicked settings are saved, and in either case the window
	 * is hidden
	 */
	public void buttonEvent(ButtonEvent event) {
		// Only respond to click events
		if(!event.isOfType(ButtonEvent.Type.CLICK))
			return;
		
		String buttonName = ((Button) event.getSource()).getName();
		
		// If the ok button was hit, save settings first
		if(buttonName.equals(OK_BUTTON)){
			// Save Colors
			Color c = ((ColorButton) this.glade.getWidget(TEXT_COLOR)).getColor();
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.TEXT_R, c.getRed());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.TEXT_G, c.getGreen());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.TEXT_B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(BACKGROUND_COLOR)).getColor();
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.BG_R, c.getRed());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.BG_G, c.getGreen());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.BG_B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(SIDEBAR_COLOR)).getColor();
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.MARGIN_R, c.getRed());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.MARGIN_G, c.getGreen());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.MARGIN_B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(LINE_NUM_COLOR)).getColor();
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.LINE_NUM_R, c.getRed());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.LINE_NUM_G, c.getGreen());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.LINE_NUM_B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(MARK_COLOR)).getColor();
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.MARK_R, c.getRed());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.MARK_G, c.getGreen());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.MARK_B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(CURRENT_LINE_COLOR)).getColor();
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.CURRENT_LINE_R, c.getRed());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.CURRENT_LINE_G, c.getGreen());
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putInt(SourceViewWidget.CURRENT_LINE_B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(LITERAL_COLOR)).getColor();
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.LITERAL_R, c.getRed());
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.LITERAL_G, c.getGreen());
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.LITERAL_B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(ID_COLOR)).getColor();
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.ID_R, c.getRed());
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.ID_G, c.getGreen());
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.ID_B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(FUNCTION_COLOR)).getColor();
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.FUNCTION_R, c.getRed());
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.FUNCTION_G, c.getGreen());
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.FUNCTION_B, c.getBlue());
			
			// Save settings
			boolean flag = ((CheckButton) this.glade.getWidget(LINE_NUM_CHECK)).getState();
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putBoolean(SourceViewWidget.SHOW_LINE_NUMBERS, flag);
			
			flag = ((CheckButton) this.glade.getWidget(MARKER_CHECK)).getState();
			this.myPrefs.node(SourceViewWidget.LNF_NODE).putBoolean(SourceViewWidget.SHOW_EXEC_MARKERS, flag);
			
			Weight w = Weight.BOLD;
			flag = ((CheckButton) this.glade.getWidget(LITERAL_BOLD_BUTTON)).getState();
			if(!flag)
				w = Weight.NORMAL;
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.LITERAL_WEIGHT, w.getValue());
			
			flag = ((CheckButton) this.glade.getWidget(ID_BOLD_BUTTON)).getState();
			if(flag)
				w = Weight.BOLD;
			else
				w = Weight.NORMAL;
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.ID_WEIGHT, w.getValue());
			
			flag = ((CheckButton) this.glade.getWidget(FUNCTION_BOLD_BUTTON)).getState();
			if(flag)
				w = Weight.BOLD;
			else
				w = Weight.NORMAL;
			this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).putInt(SourceViewWidget.FUNCTION_WEIGHT, w.getValue());
				
		}
		
		// For either button, hide the window
		this.glade.getWidget(PREF_WIN).hideAll();
	}
	
	public void attachLifeCycleListener(LifeCycleListener l){
		this.glade.getWidget(PREF_WIN).addListener(l);
	}
	
	private void addListeners(){
		// Hide me, dont' kill me
		this.glade.getWidget(PREF_WIN).addListener(new LifeCycleListener() {
			
			public boolean lifeCycleQuery(LifeCycleEvent event) {
				if(event.isOfType(LifeCycleEvent.Type.DELETE)){
					((Window) event.getSource()).hideAll();
					return true;
				}
				
				return false;
			}
		
			public void lifeCycleEvent(LifeCycleEvent event) {}
		
		});
		
		// Respond to cancel and Ok button events
		((Button) this.glade.getWidget(CANCEL_BUTTON)).addListener(this);
		((Button) this.glade.getWidget(OK_BUTTON)).addListener(this);
	}
	
	private void setupButtons(){
		// Setup Colors
		int r = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.TEXT_R, 0);
		int g = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.TEXT_G, 0);
		int b = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.TEXT_B, 0);
		ColorButton cb = (ColorButton) this.glade.getWidget(TEXT_COLOR);
		cb.setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.BG_R, 65535);
		g = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.BG_G, 65535);
		b = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.BG_B, 65535);
		cb = (ColorButton) this.glade.getWidget(BACKGROUND_COLOR);
		cb.setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.MARGIN_R, 54741);
		g = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.MARGIN_G, 56283);
		b = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.MARGIN_B, 65535);
		cb = (ColorButton) this.glade.getWidget(SIDEBAR_COLOR);
		cb.setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.LINE_NUM_R, 0);
		g = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.LINE_NUM_G, 0);
		b = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.LINE_NUM_B, 0);
		cb = (ColorButton) this.glade.getWidget(LINE_NUM_COLOR);
		cb.setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.MARK_R, 0);
		g = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.MARK_G, 0);
		b = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.MARK_B, 0);
		cb = (ColorButton) this.glade.getWidget(MARK_COLOR);
		cb.setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.CURRENT_LINE_R, 30000);
		g = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.CURRENT_LINE_G, 65535);
		b = this.myPrefs.node(SourceViewWidget.LNF_NODE).getInt(SourceViewWidget.CURRENT_LINE_B, 30000);
		((ColorButton) this.glade.getWidget(CURRENT_LINE_COLOR)).setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.LITERAL_R, 30000);
		g = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.LITERAL_G, 0);
		b = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.LITERAL_B, 30000);
		((ColorButton) this.glade.getWidget(LITERAL_COLOR)).setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.ID_R, 0);
		g = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.ID_G, 30000);
		b = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.ID_B, 0);
		((ColorButton) this.glade.getWidget(ID_COLOR)).setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.FUNCTION_R, 0);
		g = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.FUNCTION_G, 0);
		b = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.FUNCTION_B, 65535);
		((ColorButton) this.glade.getWidget(FUNCTION_COLOR)).setColor(new Color(r,g,b));
		
		// Set the label text
		((Label) this.glade.getWidget(TEXT_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.0")); //$NON-NLS-1$
		((Label) this.glade.getWidget(BG_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.1")); //$NON-NLS-1$
		((Label) this.glade.getWidget(SIDEBAR_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.2")); //$NON-NLS-1$
		((Label) this.glade.getWidget(LINE_NUM_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.3")); //$NON-NLS-1$
		((Label) this.glade.getWidget(EXEC_MARK_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.4")); //$NON-NLS-1$
		((Label) this.glade.getWidget(CURRENT_LINE_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.7")); //$NON-NLS-1$
		((Label) this.glade.getWidget(LITERAL_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.8")); //$NON-NLS-1$
		((Label) this.glade.getWidget(ID_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.9")); //$NON-NLS-1$
		((Label) this.glade.getWidget(FUNCTION_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.10")); //$NON-NLS-1$
		((Button) this.glade.getWidget(OK_BUTTON)).setLabel(Messages.getString("PreferenceWindow.11")); //$NON-NLS-1$
		((Button) this.glade.getWidget(CANCEL_BUTTON)).setLabel(Messages.getString("PreferenceWindow.12")); //$NON-NLS-1$
		
		// set tabs
		((Label) this.glade.getWidget(LNF_LABEL)).setLabel(Messages.getString("PreferenceWindow.13")); //$NON-NLS-1$
		((Label) this.glade.getWidget(SETTINGS_LABEL)).setLabel(Messages.getString("PreferenceWindow.14")); //$NON-NLS-1$
		((Label) this.glade.getWidget(SYNTAX_LABEL)).setLabel(Messages.getString("PreferenceWindow.15")); //$NON-NLS-1$
		
		// Setup Checkboxes
		boolean flag = this.myPrefs.node(SourceViewWidget.LNF_NODE).getBoolean(SourceViewWidget.SHOW_LINE_NUMBERS, true);
		CheckButton cb2 = (CheckButton) this.glade.getWidget(LINE_NUM_CHECK);
		cb2.setLabel(Messages.getString("PreferenceWindow.5")); //$NON-NLS-1$
		cb2.setState(flag);
		
		flag = this.myPrefs.node(SourceViewWidget.LNF_NODE).getBoolean(SourceViewWidget.SHOW_EXEC_MARKERS, true);
		cb2 = (CheckButton) this.glade.getWidget(MARKER_CHECK);
		cb2.setLabel(Messages.getString("PreferenceWindow.6")); //$NON-NLS-1$
		cb2.setState(flag);
		
		flag = false;
		int weight = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.LITERAL_WEIGHT, Weight.BOLD.getValue());
		if(Weight.intern(weight).equals(Weight.BOLD))
			flag = true;
		cb2 = (CheckButton) this.glade.getWidget(LITERAL_BOLD_BUTTON);
		cb2.setLabel(Messages.getString("PreferenceWindow.16")); //$NON-NLS-1$
		cb2.setState(flag);
		
		flag = false;
		weight = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.ID_WEIGHT, Weight.NORMAL.getValue());
		if(Weight.intern(weight).equals(Weight.BOLD))
			flag = true;
		cb2 = (CheckButton) this.glade.getWidget(ID_BOLD_BUTTON);
		cb2.setLabel(Messages.getString("PreferenceWindow.16")); //$NON-NLS-1$
		cb2.setState(flag);
		
		flag = false;
		weight = this.myPrefs.node(SourceViewWidget.SYNTAX_NODE).getInt(SourceViewWidget.FUNCTION_WEIGHT, Weight.BOLD.getValue());
		if(Weight.intern(weight).equals(Weight.BOLD))
			flag = true;
		cb2 = (CheckButton) this.glade.getWidget(FUNCTION_BOLD_BUTTON);
		cb2.setLabel(Messages.getString("PreferenceWindow.16")); //$NON-NLS-1$
		cb2.setState(flag);
	}
}
