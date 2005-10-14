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
// 
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

import frysk.gui.common.Messages;
import frysk.gui.srcwin.PreferenceConstants.Background;
import frysk.gui.srcwin.PreferenceConstants.CurrentLine;
import frysk.gui.srcwin.PreferenceConstants.ExecMarks;
import frysk.gui.srcwin.PreferenceConstants.Functions;
import frysk.gui.srcwin.PreferenceConstants.ID;
import frysk.gui.srcwin.PreferenceConstants.Keywords;
import frysk.gui.srcwin.PreferenceConstants.LineNumbers;
import frysk.gui.srcwin.PreferenceConstants.Margin;
import frysk.gui.srcwin.PreferenceConstants.Text;

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

	/*	
	 * GLADE CONSTANTS
	 */
	// Path to and name of the glade file to use
	public static final String GLADE_FILE = "frysk_source_prefs.glade"; //$NON-NLS-1$

	// name of the top window
	public static final String PREF_WIN = "prefWin"; //$NON-NLS-1$
	
	// Information to show in the sidebar
	public static final String LINE_NUM_CHECK = "lineNumCheck"; //$NON-NLS-1$
	public static final String LINE_NUM_COLOR = "lineNumColor"; //$NON-NLS-1$
	public static final String EXEC_MARK_COLOR_LABEL = "execMarkColorLabel"; //$NON-NLS-1$
	public static final String MARKER_CHECK = "markerCheck"; //$NON-NLS-1$
	public static final String MARK_COLOR = "markColor"; //$NON-NLS-1$
	public static final String TOOLBAR_CHECK = "toolbarCheck";
	public static final String SIDEBAR_COLOR_LABEL = "sidebarColorLabel"; //$NON-NLS-1$
	public static final String SIDEBAR_COLOR = "sidebarColor"; //$NON-NLS-1$
	
	// Settings relating to the main window
	public static final String BG_COLOR_LABEL = "bgColorLabel"; //$NON-NLS-1$
	public static final String BACKGROUND_COLOR = "backgroundColor"; //$NON-NLS-1$
	public static final String TEXT_COLOR_LABEL = "textColorLabel"; //$NON-NLS-1$
	public static final String TEXT_COLOR = "textColor"; //$NON-NLS-1$
	public static final String LINE_NUM_COLOR_LABEL = "lineNumColorLabel"; //$NON-NLS-1$
	public static final String CURRENT_LINE_COLOR_LABEL = "currentLineColorLabel"; //$NON-NLS-1$
	public static final String CURRENT_LINE_COLOR = "currentLineColor"; //$NON-NLS-1$
	
	// Ok and Cancel buttons to close the window
	public static final String OK_BUTTON = "okButton"; //$NON-NLS-1$
	public static final String CANCEL_BUTTON = "cancelButton"; //$NON-NLS-1$
	
	// Tab names
	public static final String LNF_LABEL = "lnfLabel"; //$NON-NLS-1$
	public static final String SETTINGS_LABEL = "settingsLabel"; //$NON-NLS-1$
	public static final String SYNTAX_LABEL = "syntaxLabel"; //$NON-NLS-1$
	
	// Syntax highlighting related widgets
	public static final String KEYWORD_COLOR_LABEL = "keywordColorLabel"; //$NON-NLS-1$
	public static final String KEYWORD_COLOR = "keywordColorButton"; //$NON-NLS-1$
	public static final String KEYWORD_BOLD_BUTTON = "keywordBoldButton"; //$NON-NLS-1$
	public static final String ID_COLOR_LABEL = "idColorLabel"; //$NON-NLS-1$
	public static final String ID_COLOR = "idColorButton"; //$NON-NLS-1$
	public static final String ID_BOLD_BUTTON = "idBoldButton"; //$NON-NLS-1$
	public static final String FUNCTION_COLOR_LABEL = "functionColorLabel"; //$NON-NLS-1$
	public static final String FUNCTION_COLOR = "functionColorButton"; //$NON-NLS-1$
	public static final String FUNCTION_BOLD_BUTTON = "functionBoldButton"; //$NON-NLS-1$
	/*
	 * END GLADE CONSTANTS
	 */
	

	private Preferences myPrefs;
	
	private LibGlade glade;
	
	/**
	 * Creates a new PreferenceWindow
	 * @param myPrefs The preference model to load from 
	 */
	public PreferenceWindow(Preferences myPrefs, String gladePath){
		try {
			this.glade = new LibGlade(gladePath+GLADE_FILE, this);
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
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(Text.R, c.getRed());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(Text.G, c.getGreen());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(Text.B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(BACKGROUND_COLOR)).getColor();
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(Background.R, c.getRed());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(Background.G, c.getGreen());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(Background.B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(SIDEBAR_COLOR)).getColor();
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(Margin.R, c.getRed());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(Margin.G, c.getGreen());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(Margin.B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(LINE_NUM_COLOR)).getColor();
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(LineNumbers.R, c.getRed());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(LineNumbers.G, c.getGreen());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(LineNumbers.B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(MARK_COLOR)).getColor();
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(ExecMarks.R, c.getRed());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(ExecMarks.G, c.getGreen());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(ExecMarks.B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(CURRENT_LINE_COLOR)).getColor();
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(CurrentLine.R, c.getRed());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(CurrentLine.G, c.getGreen());
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(CurrentLine.B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(PreferenceWindow.KEYWORD_COLOR)).getColor();
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(Keywords.R, c.getRed());
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(Keywords.G, c.getGreen());
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(Keywords.B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(PreferenceWindow.ID_COLOR)).getColor();
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(ID.R, c.getRed());
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(ID.G, c.getGreen());
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(ID.B, c.getBlue());
			
			c = ((ColorButton) this.glade.getWidget(PreferenceWindow.FUNCTION_COLOR)).getColor();
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(Functions.R, c.getRed());
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(Functions.G, c.getGreen());
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(Functions.B, c.getBlue());
			
			// Save settings
			boolean flag = ((CheckButton) this.glade.getWidget(LINE_NUM_CHECK)).getState();
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putBoolean(LineNumbers.SHOW, flag);
			
			flag = ((CheckButton) this.glade.getWidget(MARKER_CHECK)).getState();
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putBoolean(ExecMarks.SHOW, flag);
			
			flag = ((CheckButton) this.glade.getWidget(TOOLBAR_CHECK)).getState();
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putBoolean(PreferenceConstants.SHOW_TOOLBAR, flag);
			
			Weight w = Weight.BOLD;
			flag = ((CheckButton) this.glade.getWidget(PreferenceWindow.KEYWORD_BOLD_BUTTON)).getState();
			if(!flag)
				w = Weight.NORMAL;
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(Keywords.WEIGHT, w.getValue());
			
			flag = ((CheckButton) this.glade.getWidget(PreferenceWindow.ID_BOLD_BUTTON)).getState();
			if(flag)
				w = Weight.BOLD;
			else
				w = Weight.NORMAL;
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(ID.WEIGHT, w.getValue());
			
			flag = ((CheckButton) this.glade.getWidget(PreferenceWindow.FUNCTION_BOLD_BUTTON)).getState();
			if(flag)
				w = Weight.BOLD;
			else
				w = Weight.NORMAL;
			this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(Functions.WEIGHT, w.getValue());
				
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
		int r = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(Text.R, Text.R_DEFAULT);
		int g = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(Text.G, Text.G_DEFAULT);
		int b = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(Text.B, Text.B_DEFAULT);
		ColorButton cb = (ColorButton) this.glade.getWidget(TEXT_COLOR);
		cb.setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(Background.R, Background.R_DEFAULT);
		g = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(Background.G, Background.G_DEFAULT);
		b = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(Background.B, Background.B_DEFAULT);
		cb = (ColorButton) this.glade.getWidget(BACKGROUND_COLOR);
		cb.setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(Margin.R, Margin.R_DEFAULT);
		g = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(Margin.G, Margin.G_DEFAULT);
		b = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(Margin.B, Margin.B_DEFAULT);
		cb = (ColorButton) this.glade.getWidget(SIDEBAR_COLOR);
		cb.setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(LineNumbers.R, LineNumbers.R_DEFAULT);
		g = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(LineNumbers.G, LineNumbers.G_DEFAULT);
		b = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(LineNumbers.B, LineNumbers.B_DEFAULT);
		cb = (ColorButton) this.glade.getWidget(LINE_NUM_COLOR);
		cb.setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(ExecMarks.R, ExecMarks.R_DEFAULT);
		g = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(ExecMarks.G, ExecMarks.G_DEFAULT);
		b = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(ExecMarks.B, ExecMarks.B_DEFAULT);
		cb = (ColorButton) this.glade.getWidget(MARK_COLOR);
		cb.setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(CurrentLine.R, CurrentLine.R_DEFAULT);
		g = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(CurrentLine.G, CurrentLine.G_DEFAULT);
		b = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(CurrentLine.B, CurrentLine.B_DEFAULT);
		((ColorButton) this.glade.getWidget(CURRENT_LINE_COLOR)).setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(Keywords.R, Keywords.R_DEFAULT);
		g = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(Keywords.G, Keywords.G_DEFAULT);
		b = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(Keywords.B, Keywords.B_DEFAULT);
		((ColorButton) this.glade.getWidget(PreferenceWindow.KEYWORD_COLOR)).setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(ID.R, ID.R_DEFAULT);
		g = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(ID.G, ID.G_DEFAULT);
		b = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(ID.B, ID.B_DEFAULT);
		((ColorButton) this.glade.getWidget(PreferenceWindow.ID_COLOR)).setColor(new Color(r,g,b));
		
		r = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(Functions.R, Functions.R_DEFAULT);
		g = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(Functions.G, Functions.G_DEFAULT);
		b = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(Functions.B, Functions.B_DEFAULT);
		((ColorButton) this.glade.getWidget(PreferenceWindow.FUNCTION_COLOR)).setColor(new Color(r,g,b));
		
		// Set the label text
		((Label) this.glade.getWidget(TEXT_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.0")); //$NON-NLS-1$
		((Label) this.glade.getWidget(BG_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.1")); //$NON-NLS-1$
		((Label) this.glade.getWidget(SIDEBAR_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.2")); //$NON-NLS-1$
		((Label) this.glade.getWidget(LINE_NUM_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.3")); //$NON-NLS-1$
		((Label) this.glade.getWidget(EXEC_MARK_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.4")); //$NON-NLS-1$
		((Label) this.glade.getWidget(CURRENT_LINE_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.7")); //$NON-NLS-1$
		((Label) this.glade.getWidget(KEYWORD_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.8")); //$NON-NLS-1$
		((Label) this.glade.getWidget(ID_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.9")); //$NON-NLS-1$
		((Label) this.glade.getWidget(FUNCTION_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.10")); //$NON-NLS-1$
		((Button) this.glade.getWidget(OK_BUTTON)).setLabel(Messages.getString("PreferenceWindow.11")); //$NON-NLS-1$
		((Button) this.glade.getWidget(CANCEL_BUTTON)).setLabel(Messages.getString("PreferenceWindow.12")); //$NON-NLS-1$
		
		// set tabs
		((Label) this.glade.getWidget(PreferenceWindow.LNF_LABEL)).setLabel(Messages.getString("PreferenceWindow.13")); //$NON-NLS-1$
		((Label) this.glade.getWidget(PreferenceWindow.SETTINGS_LABEL)).setLabel(Messages.getString("PreferenceWindow.14")); //$NON-NLS-1$
		((Label) this.glade.getWidget(PreferenceWindow.SYNTAX_LABEL)).setLabel(Messages.getString("PreferenceWindow.15")); //$NON-NLS-1$
		
		// Setup Checkboxes
		boolean flag = this.myPrefs.node(PreferenceConstants.LNF_NODE).getBoolean(LineNumbers.SHOW, true);
		CheckButton cb2 = (CheckButton) this.glade.getWidget(LINE_NUM_CHECK);
		cb2.setLabel(Messages.getString("PreferenceWindow.5")); //$NON-NLS-1$
		cb2.setState(flag);
		
		flag = this.myPrefs.node(PreferenceConstants.LNF_NODE).getBoolean(ExecMarks.SHOW, true);
		cb2 = (CheckButton) this.glade.getWidget(MARKER_CHECK);
		cb2.setLabel(Messages.getString("PreferenceWindow.6")); //$NON-NLS-1$
		cb2.setState(flag);
		
		flag = this.myPrefs.node(PreferenceConstants.LNF_NODE).getBoolean(PreferenceConstants.SHOW_TOOLBAR, true);
		cb2 = (CheckButton) this.glade.getWidget(TOOLBAR_CHECK);
		cb2.setLabel("Show Toolbar");
		cb2.setState(flag);
		
		flag = false;
		int weight = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(Keywords.WEIGHT, Weight.BOLD.getValue());
		if(Weight.intern(weight).equals(Weight.BOLD))
			flag = true;
		cb2 = (CheckButton) this.glade.getWidget(PreferenceWindow.KEYWORD_BOLD_BUTTON);
		cb2.setLabel(Messages.getString("PreferenceWindow.16")); //$NON-NLS-1$
		cb2.setState(flag);
		
		flag = false;
		weight = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(ID.WEIGHT, Weight.NORMAL.getValue());
		if(Weight.intern(weight).equals(Weight.BOLD))
			flag = true;
		cb2 = (CheckButton) this.glade.getWidget(PreferenceWindow.ID_BOLD_BUTTON);
		cb2.setLabel(Messages.getString("PreferenceWindow.16")); //$NON-NLS-1$
		cb2.setState(flag);
		
		flag = false;
		weight = this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).getInt(Functions.WEIGHT, Weight.BOLD.getValue());
		if(Weight.intern(weight).equals(Weight.BOLD))
			flag = true;
		cb2 = (CheckButton) this.glade.getWidget(PreferenceWindow.FUNCTION_BOLD_BUTTON);
		cb2.setLabel(Messages.getString("PreferenceWindow.16")); //$NON-NLS-1$
		cb2.setState(flag);
	}
}
