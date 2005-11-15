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
import org.gnu.pango.Style;
import org.gnu.pango.Weight;

import frysk.gui.common.Messages;
import frysk.gui.srcwin.PreferenceConstants.Background;
import frysk.gui.srcwin.PreferenceConstants.Classes;
import frysk.gui.srcwin.PreferenceConstants.CurrentLine;
import frysk.gui.srcwin.PreferenceConstants.ExecMarks;
import frysk.gui.srcwin.PreferenceConstants.Functions;
import frysk.gui.srcwin.PreferenceConstants.Variables;
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

	private static final String CLASS_ITALICS_BUTTON = "classItalicsButton";

	private static final String KEYWORD_ITALICS_BUTTON = "keywordItalicsButton";

	private static final String FUNCTION_ITALICS_BUTTON = "functionItalicsButton";

	private static final String VARIABLE_ITALICS_BUTTON = "variableItalicsButton";

	private static final String CLASS_BOLD_BUTTON = "classBoldButton";

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
	public static final String KEYWORD_LABEL = "keywordLabel"; //$NON-NLS-1$
	public static final String KEYWORD_COLOR = "keywordColorButton"; //$NON-NLS-1$
	public static final String KEYWORD_BOLD_BUTTON = "keywordBoldButton"; //$NON-NLS-1$
	public static final String VARIABLE_LABEL = "variableLabel"; //$NON-NLS-1$
	public static final String VARIABLE_COLOR = "variableColorButton"; //$NON-NLS-1$
	public static final String VARIABLE_BOLD_BUTTON = "variableBoldButton"; //$NON-NLS-1$
	public static final String FUNCTION_LABEL = "functionLabel"; //$NON-NLS-1$
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
			this.glade = new LibGlade(gladePath+"/"+GLADE_FILE, this);
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
			this.saveColorButton(Text.COLOR_PREFIX, PreferenceWindow.TEXT_COLOR);
			this.saveColorButton(Background.COLOR_PREFIX, PreferenceWindow.BACKGROUND_COLOR);
			this.saveColorButton(Margin.COLOR_PREFIX, PreferenceWindow.SIDEBAR_COLOR);
			this.saveColorButton(LineNumbers.COLOR_PREFIX, PreferenceWindow.LINE_NUM_COLOR);
			this.saveColorButton(ExecMarks.COLOR_PREFIX, PreferenceWindow.MARK_COLOR);
			this.saveColorButton(CurrentLine.COLOR_PREFIX, PreferenceWindow.CURRENT_LINE_COLOR);
			this.saveColorButton(Keywords.COLOR_PREFIX, PreferenceWindow.KEYWORD_COLOR);
			this.saveColorButton(Variables.COLOR_PREFIX, PreferenceWindow.VARIABLE_COLOR);
			this.saveColorButton(Functions.COLOR_PREFIX, PreferenceWindow.FUNCTION_COLOR);
			
			// Save settings
			boolean flag = ((CheckButton) this.glade.getWidget(LINE_NUM_CHECK)).getState();
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putBoolean(LineNumbers.SHOW, flag);
			
			flag = ((CheckButton) this.glade.getWidget(MARKER_CHECK)).getState();
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putBoolean(ExecMarks.SHOW, flag);
			
			flag = ((CheckButton) this.glade.getWidget(TOOLBAR_CHECK)).getState();
			this.myPrefs.node(PreferenceConstants.LNF_NODE).putBoolean(PreferenceConstants.SHOW_TOOLBAR, flag);
			
			// save weights
			this.saveWeightCheck(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
					PreferenceWindow.KEYWORD_BOLD_BUTTON, Keywords.WEIGHT);
			this.saveWeightCheck(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
					PreferenceWindow.VARIABLE_BOLD_BUTTON, Variables.WEIGHT);
			this.saveWeightCheck(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
					PreferenceWindow.FUNCTION_BOLD_BUTTON, Functions.WEIGHT);
			this.saveWeightCheck(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
					PreferenceWindow.CLASS_BOLD_BUTTON, Classes.WEIGHT);
			
			// save italics
			this.saveStyleCheck(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
					PreferenceWindow.KEYWORD_ITALICS_BUTTON, Keywords.ITALICS);
			this.saveStyleCheck(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
					PreferenceWindow.VARIABLE_ITALICS_BUTTON, Variables.ITALICS);
			this.saveStyleCheck(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
					PreferenceWindow.FUNCTION_ITALICS_BUTTON, Functions.ITALICS);
			this.saveStyleCheck(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
					PreferenceWindow.CLASS_ITALICS_BUTTON, Classes.ITALICS);
		}
		
		// For either button, hide the window
		this.glade.getWidget(PREF_WIN).hideAll();
	}
	
	public void attachLifeCycleListener(LifeCycleListener l){
		this.glade.getWidget(PREF_WIN).addListener(l);
	}
	
	/*------------------------------------
	 *  PRIVATE METHODS
	 *------------------------------------*/
	
	/*
	 * Adds all the appropriate listeners to the widgets in the preferences window
	 */
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
	
	/*
	 * Sets the colors of all the ColorChooserButtons as well as the labels and states
	 * of all other appropriate widgets.
	 */
	private void setupButtons(){
		// Setup Colors
		this.setupColorButton(Text.COLOR_PREFIX, PreferenceWindow.TEXT_COLOR, Text.DEFAULT);
		this.setupColorButton(Background.COLOR_PREFIX, PreferenceWindow.BACKGROUND_COLOR,
				Background.DEFAULT);
		this.setupColorButton(Margin.COLOR_PREFIX, PreferenceWindow.SIDEBAR_COLOR, Margin.DEFAULT);
		this.setupColorButton(LineNumbers.COLOR_PREFIX, PreferenceWindow.LINE_NUM_COLOR,
				LineNumbers.DEFAULT);
		this.setupColorButton(ExecMarks.COLOR_PREFIX, PreferenceWindow.MARK_COLOR, ExecMarks.DEFAULT);
		this.setupColorButton(CurrentLine.COLOR_PREFIX, PreferenceWindow.CURRENT_LINE_COLOR,
				CurrentLine.DEFAULT);
		this.setupColorButton(Keywords.COLOR_PREFIX, PreferenceWindow.KEYWORD_COLOR,
				Keywords.DEFAULT);
		this.setupColorButton(Variables.COLOR_PREFIX, PreferenceWindow.VARIABLE_COLOR, Variables.DEFAULT);
		this.setupColorButton(Functions.COLOR_PREFIX, PreferenceWindow.FUNCTION_COLOR,
				Functions.DEFAULT);
		
		// Set the label text
		((Label) this.glade.getWidget(TEXT_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.0")); //$NON-NLS-1$
		((Label) this.glade.getWidget(BG_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.1")); //$NON-NLS-1$
		((Label) this.glade.getWidget(SIDEBAR_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.2")); //$NON-NLS-1$
		((Label) this.glade.getWidget(LINE_NUM_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.3")); //$NON-NLS-1$
		((Label) this.glade.getWidget(EXEC_MARK_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.4")); //$NON-NLS-1$
		((Label) this.glade.getWidget(CURRENT_LINE_COLOR_LABEL)).setLabel(Messages.getString("PreferenceWindow.7")); //$NON-NLS-1$
		((Label) this.glade.getWidget(KEYWORD_LABEL)).setLabel(Messages.getString("PreferenceWindow.8")); //$NON-NLS-1$
		((Label) this.glade.getWidget(VARIABLE_LABEL)).setLabel(Messages.getString("PreferenceWindow.9")); //$NON-NLS-1$
		((Label) this.glade.getWidget(FUNCTION_LABEL)).setLabel(Messages.getString("PreferenceWindow.10")); //$NON-NLS-1$
		((Label) this.glade.getWidget("classLabel")).setLabel("Class Color:");
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
		
		flag = this.myPrefs.node(PreferenceConstants.LNF_NODE).getBoolean(PreferenceConstants.SHOW_TOOLBAR, false);
		cb2 = (CheckButton) this.glade.getWidget(TOOLBAR_CHECK);
		cb2.setLabel("Show Toolbar");
		cb2.setState(flag);
		
		this.setupBoldCheckBox(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
				Variables.WEIGHT, PreferenceWindow.VARIABLE_BOLD_BUTTON);
		this.setupBoldCheckBox(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
				Functions.WEIGHT, PreferenceWindow.FUNCTION_BOLD_BUTTON);
		this.setupBoldCheckBox(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
				Keywords.WEIGHT, PreferenceWindow.KEYWORD_BOLD_BUTTON);
		this.setupBoldCheckBox(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
				Classes.WEIGHT, PreferenceWindow.CLASS_BOLD_BUTTON);
		
		this.setupItalicsCheckBox(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
				Variables.ITALICS, PreferenceWindow.VARIABLE_ITALICS_BUTTON);
		this.setupItalicsCheckBox(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
				Functions.ITALICS, PreferenceWindow.FUNCTION_ITALICS_BUTTON);
		this.setupItalicsCheckBox(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
				Keywords.ITALICS, PreferenceWindow.KEYWORD_ITALICS_BUTTON);
		this.setupItalicsCheckBox(this.myPrefs.node(PreferenceConstants.SYNTAX_NODE),
				Classes.ITALICS, PreferenceWindow.CLASS_ITALICS_BUTTON);
	}
	
	/*
	 * Helper method to get whether or not something is bold in the preference model
	 * and set the provided CheckButton accordingly. buttonName MUST correspond to
	 * a CheckButton
	 */
	private void setupBoldCheckBox(Preferences node, String weightKey, String buttonName){
		CheckButton cb = (CheckButton) this.glade.getWidget(buttonName);
		cb.setLabel(Messages.getString("PreferenceWindow.16"));
		int weight = node.getInt(weightKey, Weight.NORMAL.getValue());
		if(Weight.intern(weight).equals(Weight.BOLD))
			cb.setState(true);
	}
	
	/*
	 * Same as setupBoldCheckBox, except for Italics
	 */
	private void setupItalicsCheckBox(Preferences node, String italicsKey, String buttonName){
		CheckButton cb = (CheckButton) this.glade.getWidget(buttonName);
		cb.setLabel("Italics");
		int style = node.getInt(italicsKey, Style.NORMAL.getValue());
		if(Style.intern(style).equals(Style.ITALIC))
			cb.setState(true);
	}
	
	/*
	 * Takes the color for a given item from the preference model and sets
	 * the color of the ColorButton accordingly
	 */
	private void setupColorButton(String prefix, String buttonName, Color defaultColor){
		int r = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(prefix+"R", defaultColor.getRed());
		int g = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(prefix+"G", defaultColor.getGreen());
		int b = this.myPrefs.node(PreferenceConstants.LNF_NODE).getInt(prefix+"B", defaultColor.getBlue());
		ColorButton cb = (ColorButton) this.glade.getWidget(buttonName);
		cb.setColor(new Color(r,g,b));
	}
	
	/*
	 * Does the inverse of setupColorButton: gets the color from the button and
	 * puts it in the preference model
	 */
	private void saveColorButton(String prefix, String buttonName){
		Color c = ((ColorButton) this.glade.getWidget(buttonName)).getColor();
		this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(prefix+"R", c.getRed());
		this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(prefix+"G", c.getGreen());
		this.myPrefs.node(PreferenceConstants.LNF_NODE).putInt(prefix+"B", c.getBlue());
	}
	
	/*
	 * Does the inverse of setupBoldCheckBox: Takes the status of the button and
	 * puts it in the preference model
	 */
	private void saveWeightCheck(Preferences node, String buttonName, String key){
		boolean flag = ((CheckButton) this.glade.getWidget(buttonName)).getState();
		Weight w = null;
		if(flag)
			w = Weight.BOLD;
		else
			w = Weight.NORMAL;
		this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(key, w.getValue());
	}
	
	/*
	 * Same as saveWeightCheck, except for Italics
	 */
	private void saveStyleCheck(Preferences node, String buttonName, String key){
		boolean flag = ((CheckButton) this.glade.getWidget(buttonName)).getState();
		Style s = null;
		if(flag)
			s = Style.ITALIC;
		else
			s = Style.NORMAL;
		this.myPrefs.node(PreferenceConstants.SYNTAX_NODE).putInt(key, s.getValue());
	}
}
