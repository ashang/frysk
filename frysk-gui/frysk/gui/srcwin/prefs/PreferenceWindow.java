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
package frysk.gui.srcwin.prefs;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.HBox;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

/**
 * The aim of this window is to provide a place for the user to change various
 * preferences relating to look and feel, debugging feedback, etc. This window
 * by itself does not refresh the source window when it is closed, rather the
 * source window needs to call attachLifeCycleListener to deal with the hiding
 * of this window
 * 
 * @author ajocksch
 * 
 */

public class PreferenceWindow implements ButtonListener {

	// private static final String CLASS_ITALICS_BUTTON = "classItalicsButton";
	//
	// private static final String KEYWORD_ITALICS_BUTTON =
	// "keywordItalicsButton";
	//
	// private static final String FUNCTION_ITALICS_BUTTON =
	// "functionItalicsButton";
	//
	// private static final String VARIABLE_ITALICS_BUTTON =
	// "variableItalicsButton";
	//
	// private static final String CLASS_BOLD_BUTTON = "classBoldButton";

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

	private LibGlade glade;

	private SyntaxPreferenceViewer viewer;

	private DataColumn[] cols = { new DataColumnString() };

	/**
	 * Creates a new PreferenceWindow
	 * 
	 * @param myPrefs
	 *            The preference model to load from
	 */
	public PreferenceWindow(String gladePath) {
		try {
			this.glade = new LibGlade(gladePath + "/" + GLADE_FILE, this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.setupButtons();
		this.addListeners();
	}

	/**
	 * Redisplays the window and updates it's status to reflect the state of the
	 * preference model
	 */
	public void show() {
		this.setupButtons();
		this.glade.getWidget(PREF_WIN).showAll();
	}

	/**
	 * Called in response to either the Ok or Cancel buttons being clicked. If
	 * the Ok button was clicked settings are saved, and in either case the
	 * window is hidden
	 */
	public void buttonEvent(ButtonEvent event) {
		// Only respond to click events
		if (!event.isOfType(ButtonEvent.Type.CLICK))
			return;

		String buttonName = ((Button) event.getSource()).getName();

		// If the ok button was hit, save settings first
		if (buttonName.equals(OK_BUTTON)) {
			this.viewer.saveAll();
		}

		// For either button, hide the window
		this.glade.getWidget(PREF_WIN).hideAll();
	}

	/**
	 * Attaches the life cycle listener to the glade widget representing the
	 * window
	 * 
	 * @param l
	 *            The listener to attach
	 */
	public void attachLifeCycleListener(LifeCycleListener l) {
		this.glade.getWidget(PREF_WIN).addListener(l);
	}

	/*------------------------------------
	 *  PRIVATE METHODS
	 *------------------------------------*/

	/*
	 * Adds all the appropriate listeners to the widgets in the preferences
	 * window
	 */
	private void addListeners() {
		// Hide me, dont' kill me
		this.glade.getWidget(PREF_WIN).addListener(new LifeCycleListener() {

			public boolean lifeCycleQuery(LifeCycleEvent event) {
				if (event.isOfType(LifeCycleEvent.Type.DELETE)) {
					((Window) event.getSource()).hideAll();
					return true;
				}

				return false;
			}

			public void lifeCycleEvent(LifeCycleEvent event) {
			}

		});

		// Change what preferences are being displayed when the selection on the
		// left changes
		((TreeView) this.glade.getWidget("preferenceTree")).getSelection()
				.addListener(new TreeSelectionListener() {

					public void selectionChangedEvent(TreeSelectionEvent arg0) {
						updateShownPrefs();
					}

				});

		// Respond to cancel and Ok button events
		((Button) this.glade.getWidget(CANCEL_BUTTON)).addListener(this);
		((Button) this.glade.getWidget(OK_BUTTON)).addListener(this);
	}

	/*
	 * Sets the colors of all the ColorChooserButtons as well as the labels and
	 * states of all other appropriate widgets.
	 */
	private void setupButtons() {
		this.viewer = new SyntaxPreferenceViewer();

		((HBox) this.glade.getWidget("mainPane")).packStart(viewer);

		TreeView sidePanel = (TreeView) this.glade.getWidget("preferenceTree");

		TreeStore model = new TreeStore(cols);
		TreeIter iter = model.appendRow(null);
		model.setValue(iter, (DataColumnString) cols[0], "General Appearance");
		iter = model.appendRow(null);
		model.setValue(iter, (DataColumnString) cols[0], "Syntax Highlighting");
		sidePanel.setModel(model);

		TreeViewColumn column = new TreeViewColumn();
		CellRendererText renderer = new CellRendererText();
		column.packStart(renderer, true);
		column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
				cols[0]);
		sidePanel.appendColumn(column);

		sidePanel.getSelection().setMode(SelectionMode.SINGLE);

		this.glade.getWidget(PREF_WIN).showAll();
	}

	private void updateShownPrefs() {
		TreeView sidePanel = (TreeView) this.glade.getWidget("preferenceTree");

		if (sidePanel.getSelection().countRows() == 0)
			return;

		TreePath selectedPath = sidePanel.getSelection().getSelectedRows()[0];

		if (selectedPath != null) {
			TreeIter iter = sidePanel.getModel().getIter(selectedPath);
			String text = sidePanel.getModel().getValue(iter,
					(DataColumnString) cols[0]);
			System.out.println(text);
		}
	}
}
