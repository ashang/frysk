// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

package frysk.gui.prefs;

import java.util.Iterator;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.CheckButton;
import org.gnu.gtk.ColorButton;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Notebook;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.SortType;
import org.gnu.gtk.SpinButton;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeStore;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.ColorButtonEvent;
import org.gnu.gtk.event.ColorButtonListener;
import org.gnu.gtk.event.SpinEvent;
import org.gnu.gtk.event.SpinListener;
import org.gnu.gtk.event.ToggleEvent;
import org.gnu.gtk.event.ToggleListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;
import org.gnu.pango.Style;
import org.gnu.pango.Weight;

import frysk.gui.common.IconManager;
import frysk.gui.srcwin.prefs.SourceWinPreferenceGroup;
import frysk.gui.srcwin.prefs.SyntaxPreference;
import frysk.gui.srcwin.prefs.SyntaxPreferenceGroup;

/**
 * The PreferenceWindow allows the user to display and edit any of the
 * preferences in any of the groups registered with the preference
 * manager. The list of preferences is constructed dynamically so in
 * order for a preference to be visible here all that needs to be done
 * is add that Preference to a {@see frysk.gui.prefs.PreferenceGroup}
 * and then add that group to the {@see
 * frysk.gui.prefs.PreferenceManager}.
 *
 */
public class PreferenceWindow extends Window implements TreeSelectionListener, ButtonListener{

	private LibGlade glade; // My glade file
	private TreeView prefView; // The view that will display the preference groups
	
	private DataColumn[] cols = {new DataColumnString(), new DataColumnObject()};
	
	/**
	 * Creates a new Preference Window
	 * @param glade The glade object for the preference window
	 */
	public PreferenceWindow(LibGlade glade){
		super(((Window) glade.getWidget("prefWin")).getHandle());
		
		this.setTitle("Preferences");
		this.setIcon(IconManager.windowIcon);
		
		this.glade = glade;
		this.prefView = (TreeView) this.glade.getWidget("preferenceTree");
		this.prefView.getSelection().addListener(this);
		
		((Button) this.glade.getWidget("okButton")).addListener(this);
		((Button) this.glade.getWidget("cancelButton")).addListener(this);
		
		this.setupPreferenceTree();
		this.attachEvents();
	}
	
	/*
	 * Generates the list of preference groups based on the information in 
	 * PreferenceManager
	 */
	private void setupPreferenceTree(){
		TreeStore model = new TreeStore(cols);
		Iterator groups = PreferenceManager.getPreferenceGroups();
		
		model.setSortColumn(cols[0], SortType.ASCENDING);
		
		while(groups.hasNext()){
			PreferenceGroup group = (PreferenceGroup) groups.next();

			this.addGroup(model, null, group);
		}
		
		this.prefView.setModel(model);
		
		TreeViewColumn column = new TreeViewColumn();
		CellRenderer renderer = new CellRendererText();
		column.packStart(renderer, true);
		column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
				cols[0]);
		this.prefView.appendColumn(column);
		
		this.prefView.getSelection().unselectAll();
		this.prefView.getSelection().setMode(SelectionMode.SINGLE);
	}

	private void attachEvents(){
		CheckButton cButton = (CheckButton) this.glade.getWidget("toolbarCheck");
		BooleanPreference bPref = (BooleanPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.TOOLBAR);
		cButton.setState(bPref.getCurrentValue());
		cButton.addListener(new BoolPrefListener(bPref));
		
		cButton = (CheckButton) this.glade.getWidget("linenumCheck");
		bPref = (BooleanPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.LINE_NUMS);
		cButton.setState(bPref.getCurrentValue());
		cButton.addListener(new BoolPrefListener(bPref));
		
		ColorPreference cPref = (ColorPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.LINE_NUMBER_COLOR);
		this.initColorPreference(cPref, "linenum");
		
		cButton = (CheckButton) this.glade.getWidget("execCheck");
		bPref = (BooleanPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.EXEC_MARKS);
		cButton.setState(bPref.getCurrentValue());
		cButton.addListener(new BoolPrefListener(bPref));
		
		cPref = (ColorPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.EXEC_MARKS_COLOR);
		this.initColorPreference(cPref, "exec");
		
		SpinButton sButton = (SpinButton) this.glade.getWidget("inlineSpin");
		IntPreference iPref = (IntPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.INLINE_LEVELS);
		sButton.setValue((double) iPref.getCurrentValue());
		sButton.addListener(new IntPrefListener(iPref));
		
		cPref = (ColorPreference) PreferenceManager.sourceWinGroup.getSubgroup("Look and Feel").getPreference(SourceWinPreferenceGroup.TEXT);
		this.initColorPreference(cPref, "text");
		
		cPref = (ColorPreference) PreferenceManager.sourceWinGroup.getSubgroup("Look and Feel").getPreference(SourceWinPreferenceGroup.BACKGROUND);
		this.initColorPreference(cPref, "background");
		
		cPref = (ColorPreference) PreferenceManager.sourceWinGroup.getSubgroup("Look and Feel").getPreference(SourceWinPreferenceGroup.MARGIN);
		this.initColorPreference(cPref, "margin");
		
		cPref = (ColorPreference) PreferenceManager.sourceWinGroup.getSubgroup("Look and Feel").getPreference(SourceWinPreferenceGroup.SEARCH);
		this.initColorPreference(cPref, "search");
		
		cPref = (ColorPreference) PreferenceManager.sourceWinGroup.getSubgroup("Look and Feel").getPreference(SourceWinPreferenceGroup.CURRENT_LINE);
		this.initColorPreference(cPref, "currentline");
		
		SyntaxPreference sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.CLASSES);
		this.initSyntaxPreference(sPref, "class");
		
		sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.FUNCTIONS);
		this.initSyntaxPreference(sPref, "func");
		
		sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.KEYWORDS);
		this.initSyntaxPreference(sPref, "key");
		
		sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.VARIABLES);
		this.initSyntaxPreference(sPref, "local");
		
		sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.GLOBALS);
		this.initSyntaxPreference(sPref, "global");
		
		sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.OUT_OF_SCOPE);
		this.initSyntaxPreference(sPref, "oos");
		
		sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.OPTIMIZED);
		this.initSyntaxPreference(sPref, "opt");
		
		sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.COMMENTS);
		this.initSyntaxPreference(sPref, "comment");
		
		sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.NAMESPACE);
		this.initSyntaxPreference(sPref, "namespace");
		
		sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.INCLUDES);
		this.initSyntaxPreference(sPref, "include");
		
		sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.MACRO);
		this.initSyntaxPreference(sPref, "macro");
		
		sPref = (SyntaxPreference) PreferenceManager.syntaxHighlightingGroup.getPreference(SyntaxPreferenceGroup.TEMPLATE);
		this.initSyntaxPreference(sPref, "template");
	}
	
	private void initSyntaxPreference(SyntaxPreference sPref, String prefix){
		SynPrefListener listener = new SynPrefListener(sPref);
		this.initColorPreference(sPref, prefix);
		
		CheckButton cButton = (CheckButton) this.glade.getWidget(prefix + "Bold");
		cButton.setState(sPref.getCurrentWeight().equals(Weight.BOLD));
		cButton.addListener(listener);
		
		cButton = (CheckButton) this.glade.getWidget(prefix + "Ital");
		cButton.setState(sPref.getCurrentStyle().equals(Style.ITALIC));
		cButton.addListener(listener);
	}
	
	private void initColorPreference(ColorPreference cPref, String prefix){
		ColorButton colButton = (ColorButton) this.glade.getWidget(prefix + "Color");
		colButton.setColor(cPref.getCurrentColor());
		colButton.addListener(new ColorPrefListener(cPref));
	}
	
	/*
	 * Adds the provided group as a child of the provided row, and recursively adds this
	 * groups subgroups below it
	 */
	private void addGroup(TreeStore model, TreeIter parent, PreferenceGroup group){
		TreeIter row = model.appendRow(parent);
		
		model.setValue(row, (DataColumnString) cols[0], group.getName());
		model.setValue(row, (DataColumnObject) cols[1], group);
		
		Iterator subGroups = group.getSubgroups();
		
		while(subGroups.hasNext()){
			PreferenceGroup subGroup = (PreferenceGroup) subGroups.next();
			
			this.addGroup(model, row, subGroup);
		}
	}
	
	/*
	 * Called when the group selection changes, it finds all the preferences in that
	 * group and then creates preferenceEditors for them.
	 * (non-Javadoc)
	 * @see org.gnu.gtk.event.TreeSelectionListener#selectionChangedEvent(org.gnu.gtk.event.TreeSelectionEvent)
	 */
	public void selectionChangedEvent(TreeSelectionEvent arg0) {
		TreePath[] paths = this.prefView.getSelection().getSelectedRows();
		
		if(paths.length == 0 || paths[0].getDepth() < 1)
			return;
		
		TreeModel model = this.prefView.getModel();
		
		TreeIter selectedRow = model.getIter(paths[0]);
		PreferenceGroup group = (PreferenceGroup) model.getValue(selectedRow, (DataColumnObject) cols[1]);
		
		((Notebook) this.glade.getWidget("prefNotebook")).setCurrentPage(group.getTabNum());
		
		this.showAll();
	}

	/*
	 * Whenever the user clicks on a button (there's only 2, apply and cancel),
	 * perform the appropriate action and then close the window
	 * (non-Javadoc)
	 * @see org.gnu.gtk.event.ButtonListener#buttonEvent(org.gnu.gtk.event.ButtonEvent)
	 */
	public void buttonEvent(ButtonEvent arg0) {
		// Ignore non-clicks
		if(!arg0.isOfType(ButtonEvent.Type.CLICK))
			return;
		
		String buttonText = ((Button) arg0.getSource()).getName();
		
		if(buttonText.equals("okButton")){
			PreferenceManager.saveAll();
		}
		else{
			PreferenceManager.revertAll();
		}
		
		this.hideAll();
	}

	
	private static class BoolPrefListener implements ToggleListener{

		private BooleanPreference pref;
		
		public BoolPrefListener(BooleanPreference pref){
			this.pref = pref;
		}
		
		public void toggleEvent(ToggleEvent arg0) {
			pref.setCurrentValue(((CheckButton) arg0.getSource()).getState());
		}
		
	}
	
	private static class ColorPrefListener implements ColorButtonListener{
		private ColorPreference color;
		
		public ColorPrefListener(ColorPreference pref){
			color = pref;
		}

		public boolean colorButtonEvent(ColorButtonEvent arg0) {
			color.setCurrentColor(((ColorButton) arg0.getSource()).getColor()); 
			
			return false;
		}	
	}
	
	private static class IntPrefListener implements SpinListener{
		private IntPreference iPref;
		
		public IntPrefListener(IntPreference pref){
			iPref = pref;
		}

		public void spinEvent(SpinEvent arg0) {
			iPref.setCurrentValue((int) ((SpinButton) arg0.getSource()).getValue());
		}
		
	}
	
	private static class SynPrefListener implements ToggleListener{

		private SyntaxPreference pref;
		
		public SynPrefListener(SyntaxPreference myPref){
			pref = myPref;
		}

		public void toggleEvent(ToggleEvent arg0) {
			CheckButton button = (CheckButton) arg0.getSource();
			if(button.getLabel().equals("Bold"))
				pref.toggleBold();
			else
				pref.toggleItalics();
		}
		
	}
}
