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

import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.gnu.gdk.Color;
import org.gnu.gdk.KeyValue;
import org.gnu.gdk.ModifierType;
import org.gnu.gdk.Pixbuf;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.AccelGroup;
import org.gnu.gtk.AccelMap;
import org.gnu.gtk.Action;
import org.gnu.gtk.Button;
import org.gnu.gtk.CellRenderer;
import org.gnu.gtk.CellRendererText;
import org.gnu.gtk.CheckButton;
import org.gnu.gtk.ComboBox;
import org.gnu.gtk.ComboBoxEntry;
import org.gnu.gtk.Container;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnObject;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Entry;
import org.gnu.gtk.Gtk;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.IconFactory;
import org.gnu.gtk.IconSet;
import org.gnu.gtk.Image;
import org.gnu.gtk.Label;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuBar;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.SeparatorToolItem;
import org.gnu.gtk.StateType;
import org.gnu.gtk.TextMark;
import org.gnu.gtk.ToggleAction;
import org.gnu.gtk.ToolBar;
import org.gnu.gtk.ToolItem;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ActionEvent;
import org.gnu.gtk.event.ActionListener;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.TreeSelectionEvent;
import org.gnu.gtk.event.TreeSelectionListener;

import frysk.gui.common.Messages;

//import frysk.Config;

public class SourceWindow implements ButtonListener, EntryListener, 
									ComboBoxListener, TreeSelectionListener{
	/*
	 * GLADE CONSTANTS
	 */
	// Search bar widgets
	public static final String LINE_ENTRY = "lineEntry";
	public static final String FIND_TEXT = "findText";
	public static final String FIND_BOX = "findBox";
	public static final String FIND_LABEL = "findLabel"; //$NON-NLS-1$
	public static final String LINE_LABEL = "lineLabel"; //$NON-NLS-1$
	public static final String NEXT_FIND = "nextFind"; //$NON-NLS-1$
	public static final String PREV_FIND = "prevFind"; //$NON-NLS-1$
	public static final String HIGHLIGHT_FIND = "highlightFind"; //$NON-NLS-1$
	public static final String CASE_FIND = "caseFind"; //$NON-NLS-1$
	public static final String GOTO_BUTTON = "gotoButton"; //$NON-NLS-1$
	public static final String CLOSE_FIND = "closeFind"; //$NON-NLS-1$
	
	// Widget names - toolbar
	public static final String GLADE_TOOLBAR_NAME = "toolbar"; //$NON-NLS-1$
	public static final String FILE_SELECTOR = "fileSelector";
	public static final String VIEW_COMBO_BOX = "viewComboBox";
	public static final String FUNC_SELECTOR = "funcSelector";

	// Directory where images are stored
	public static String IMAGES_DIR = null; //$NON-NLS-1$
	
	// Image files - search bar
	public static final String FIND_NEXT_PNG = "findNext.png"; //$NON-NLS-1$
	public static final String FIND_PREV_PNG = "findPrev.png"; //$NON-NLS-1$
	public static final String FIND_GO_PNG = "findGo.png"; //$NON-NLS-1$
	public static final String HIGHLIGHT_PNG = "highlight.png"; //$NON-NLS-1$

	// Names of the image files - Toolbar
	public static final String STACK_BOTTOM_PNG = "stack_bottom.png"; //$NON-NLS-1$
	public static final String RUN_PNG = "run.png";
	public static final String STEP_PNG = "step.png"; //$NON-NLS-1$
	public static final String NEXT_PNG = "next.png"; //$NON-NLS-1$
	public static final String FINISH_PNG = "finish.png"; //$NON-NLS-1$
	public static final String CONTINUE_PNG = "continue.png"; //$NON-NLS-1$
	public static final String STEP_ASM_PNG = "step_asm.png"; //$NON-NLS-1$
	public static final String NEXT_ASM_PNG = "next_asm.png"; //$NON-NLS-1$
	public static final String STACK_DOWN_PNG = "stack_down.png"; //$NON-NLS-1$
	public static final String STACK_UP_PNG = "stack_up.png"; //$NON-NLS-1$

	// Widget that the SourceViewWidget will be placed in
	public static final String TEXT_WINDOW = "textWindow";
	
	// Name of the top level window
	public static final String SOURCE_WINDOW = "sourceWindow";
	
	// Glade file to use 
	public static final String GLADE_FILE = "frysk_source.glade";
	/*
	 * END GLADE CONSTANTS
	 */
	
	private LibGlade glade;
	
	private Preferences prefs;
	
	private SourceViewWidget view;
	
	private PreferenceWindow prefWin;
	
	private boolean running = false;
	
	// ACTIONS
	private Action close;
	
	private Action copy;
	private Action find;
	private Action prefsLaunch;
	
	private ToggleAction run;
	private Action step;
	private Action next;
	private Action cont;
	private Action finish;
	private Action terminate;
	private Action stepAsm;
	private Action nextAsm;
	
	private Action stackUp;
	private Action stackDown;
	private Action stackBottom;
	private DataColumn[] dataColumns;
	
	public SourceWindow(String[] gladePaths, String imagePath) {
		for(int i = 0; i < gladePaths.length; i++){
			try{
				this.glade = new LibGlade(gladePaths[i]+SourceWindow.GLADE_FILE, this);
			}
			catch (Exception e){
				// If we don't find the glade file, continue looking
				this.glade = null;
				continue;
			}
			// If we've found it, break
			break;
		}
		
		// If we don't have a glade file by this point, bail
		if(glade == null){
			System.err.println("Could not file source window glade file! Exiting.");
			System.exit(1);
		}

		IMAGES_DIR = imagePath;
		
		this.glade.getWidget(SourceWindow.SOURCE_WINDOW).hideAll();
		
		AccelGroup ag = new AccelGroup();
		((Window) this.glade.getWidget(SourceWindow.SOURCE_WINDOW)).addAccelGroup(ag);
		
		this.createActions(ag);
		this.createMenus();
		this.createToolBar();
		this.createSearchBar();
		this.attachEvents();
		
		// instantiate preference model
		this.prefs = Preferences.userRoot();
		try {
			prefs.clear();
		} catch (BackingStoreException e1) {
			e1.printStackTrace();
		}
		/*--------------------------------------*
		 *  INSERT LOADING PREFS FROM FILE HERE *
		 *--------------------------------------*/
		
		// create the actual sourceview widget
		this.view = new SourceViewWidget(this.prefs);
		
		Vector funcs = ((SourceBuffer) this.view.getBuffer()).getFunctions();
		for(int i = 0; i < funcs.size(); i++)
			((ComboBoxEntry) this.glade.getWidget(SourceWindow.FUNC_SELECTOR)).appendText(((String) funcs.get(i)).split("_")[0]);
		
		((ComboBoxEntry) this.glade.getWidget(SourceWindow.FILE_SELECTOR)).setActive(0); //$NON-NLS-1$
		((ComboBox) this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX)).setActive(0); //$NON-NLS-1$
		
		((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).add(this.view);
		this.glade.getWidget(SourceWindow.SOURCE_WINDOW).showAll();
	}

	/**
	 * Called in response to buttons being clicked on the widget. Not meant to be
	 * called manually, should only be called from within Gtk event loop
	 */
	public void buttonEvent(ButtonEvent event) {
		if(event.isOfType(ButtonEvent.Type.CLICK))
			this.handleButtonClicked(event);
	}
	
	/**
	 * Called in response to a change in the value of the textbox. Automatically
	 * searches for the string as entered in the text
	 */
	public void entryEvent(EntryEvent event) {
		if(event.isOfType(EntryEvent.Type.DELETE_TEXT))
			this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(StateType.NORMAL, Color.WHITE);
		
		if(event.isOfType(EntryEvent.Type.CHANGED)){
			String text = ((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT)).getText();
			boolean matchCase = ((CheckButton) this.glade.getWidget(SourceWindow.CASE_FIND)).getState();
			
			if(!this.view.findNext(text, matchCase))
				this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(StateType.NORMAL, Color.RED);
			else
				this.view.scrollToFound();
		}
	}
	
	/**
	 * Called in response to an entry in one of the combo boxes being changed
	 */
	public void comboBoxEvent(ComboBoxEvent event) {
		String text = ((ComboBox) event.getSource()).getActiveText();
		if(((SourceBuffer) this.view.getBuffer()).getFunctions().contains(text+"_FUNC")){
			TextMark mark = this.view.getBuffer().getMark(text+"_FUNC");
			this.view.scrollToMark(mark, 0);
		}
	}
	
	/**
	 * To be called internally when a change in the preference model occurs. Updates
	 * the window and children to reflect the new changes
	 */
	public void refresh(){
		this.view.refresh();
		
		if(this.prefs.node(PreferenceConstants.LNF_NODE).getBoolean(PreferenceConstants.SHOW_TOOLBAR, true))
			this.glade.getWidget(SourceWindow.GLADE_TOOLBAR_NAME).showAll();
		else
			this.glade.getWidget(SourceWindow.GLADE_TOOLBAR_NAME).hideAll();
	}
	
	/**
	 * Populates the stack browser window
	 * @param top
	 */
	public void populateStackBrowser(PCLocation top){
		((Container) this.view.getParent()).remove(this.view);
		this.view = new SourceViewWidget(this.prefs);
		((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).add(this.view);
		this.glade.getWidget(SourceWindow.SOURCE_WINDOW).showAll();
		
		TreeView stackList = (TreeView) this.glade.getWidget("stackBrowser");
		
		dataColumns = new DataColumn[] {new DataColumnString(), new DataColumnObject()};
		ListStore listModel = new ListStore(dataColumns);
		
		TreeIter iter  = null;
		TreeIter last = null;
		
		while(top != null){
			iter = listModel.appendRow();
			System.out.println(top.getFunction());
			
			if(top.inlineScope == null)			
				listModel.setValue(iter, (DataColumnString) dataColumns[0], top.getFunction());
			else
				listModel.setValue(iter, (DataColumnString) dataColumns[0], top.getFunction()+"  (i)");
			listModel.setValue(iter, (DataColumnObject) dataColumns[1], top);
			
			// Save the last node so we can select it
			if(top.nextScope == null)
				last = iter;
			
			top = top.nextScope;
		}
		stackList.setModel(listModel);
				
		TreeViewColumn column = new TreeViewColumn();
		CellRenderer renderer = new CellRendererText();
		column.packStart(renderer, true);
		column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT, dataColumns[0]);
		column.setTitle("Function");
		stackList.appendColumn(column);
		
		stackList.getSelection().setMode(SelectionMode.SINGLE);
		stackList.getSelection().select(last);
		stackList.showAll();
	}
	
	/***********************************
	 * PRIVATE METHODS
	 ***********************************/
	/**
	 * Creates the menus and assigns hotkeys
	 */
	private void createActions(AccelGroup ag) {
		// Before we make actions, register the icons
		IconFactory fac = new IconFactory();
		
		IconSet set = null;
		try {
			set = new IconSet(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.RUN_PNG));
			fac.addIconSet("frysk-run", set);
			set = new IconSet(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.STEP_PNG));
			fac.addIconSet("frysk-step", set);
			set = new IconSet(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.NEXT_PNG));
			fac.addIconSet("frysk-next", set);
			set = new IconSet(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.FINISH_PNG));
			fac.addIconSet("frysk-finish", set);
			set = new IconSet(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.CONTINUE_PNG));
			fac.addIconSet("frysk-continue", set);
			set = new IconSet(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.NEXT_ASM_PNG));
			fac.addIconSet("frysk-next-asm", set);
			set = new IconSet(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.STEP_ASM_PNG));
			fac.addIconSet("frysk-step-asm", set);
			set = new IconSet(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.STACK_BOTTOM_PNG));
			fac.addIconSet("frysk-stack-bottom", set);
			set = new IconSet(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.STACK_DOWN_PNG));
			fac.addIconSet("frysk-stack-down", set);
			set = new IconSet(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.STACK_UP_PNG));
			fac.addIconSet("frysk-stack-up", set);
		} catch (Exception e){
			System.err.println("Error loading images! Exiting");
			System.exit(1);
		}
		fac.addDefault();
		
		// Close action
		this.close = new Action("close", "Close", "Close Window", GtkStockItem.CLOSE.getString());
		this.close.setAccelGroup(ag);
		this.close.setAccelPath("<sourceWin>/File/Close");
		this.close.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				Gtk.mainQuit();
			}
		});
		AccelMap.changeEntry("<sourceWin>/File/Close", KeyValue.x, ModifierType.CONTROL_MASK, true);
		this.close.connectAccelerator();
		
		// Copy action
		this.copy = new Action("copy", "Copy", "Copy Selected Text to the Clipboard",
							GtkStockItem.COPY.getString());
		this.copy.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				System.out.println("<copy />");
			}
		});
		this.copy.setAccelGroup(ag);
		this.copy.setAccelPath("<sourceWin>/Edit/Copy");
		AccelMap.changeEntry("<sourceWin>/Edit/Copy", KeyValue.c, ModifierType.CONTROL_MASK, true);
		this.copy.connectAccelerator();
		
		// Find action
		this.find = new Action("find", "Find", "Find Text in the Current Buffer",
							GtkStockItem.FIND.getString());
		this.find.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.glade.getWidget(SourceWindow.FIND_BOX).showAll();
			}
		});		
		this.find.setAccelGroup(ag);
		this.find.setAccelPath("<sourceWin>/Edit/Find");
		AccelMap.changeEntry("<sourceWin>/Edit/Find", KeyValue.f, ModifierType.CONTROL_MASK, true);
		this.find.connectAccelerator();
		
		// Launch preference window action
		this.prefsLaunch = new Action("prefs", "Preferences", "Edit Preferences",
							GtkStockItem.PREFERENCES.getString());
		this.prefsLaunch.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.launchPreferencesWindow();
			}
		});
		
		// Run program action
		this.run = new ToggleAction("run", "Run", Messages.getString("SourceWindow.26"), "frysk-run");
		this.run.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doRun();
			}
		});
		this.run.setAccelGroup(ag);
		this.run.setAccelPath("<sourceWin>/Program/Run");
		AccelMap.changeEntry("<sourceWin>/Program/Run", KeyValue.r, ModifierType.MOD1_MASK, true);
		this.run.connectAccelerator();
		
		// Step action
		this.step = new Action("step", "Step", Messages.getString("SourceWindow.28"), "frysk-step");
		this.step.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doStep();
			}
		});
		this.step.setAccelGroup(ag);
		this.step.setAccelPath("<sourceWin>/Program/Step");
		AccelMap.changeEntry("<sourceWin>/Program/Step", KeyValue.s, ModifierType.MOD1_MASK, true);
		this.step.connectAccelerator();
		
		// Next action
		this.next = new Action("next", "Next", Messages.getString("SourceWindow.30"), "frysk-next");
		this.next.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doNext();
			}
		});
		this.next.setAccelGroup(ag);
		this.next.setAccelPath("<sourceWin>/Program/Next");
		AccelMap.changeEntry("<sourceWin>/Program/Next", KeyValue.n, ModifierType.MOD1_MASK, true);
		this.next.connectAccelerator();
		
		// Finish action
		this.finish = new Action("finish", "Finish", Messages.getString("SourceWindow.32"), "frysk-finish");
		this.finish.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doFinish();		
			}
		});
		this.finish.setAccelGroup(ag);
		this.finish.setAccelPath("<sourceWin>/Program/Finish");
		AccelMap.changeEntry("<sourceWin>/Program/Finish", KeyValue.f, ModifierType.MOD1_MASK, true);
		this.finish.connectAccelerator();
		
		// Continue action
		this.cont = new Action("continue", "Continue", Messages.getString("SourceWindow.34"), "frysk-continue");
		this.cont.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doContinue();
			}
		});
		this.cont.setAccelGroup(ag);
		this.cont.setAccelPath("<sourceWin>/Program/Continue");
		AccelMap.changeEntry("<sourceWin>/Program/Continue", KeyValue.c, ModifierType.MOD1_MASK, true);
		this.cont.connectAccelerator();
		
		// Terminate action
		this.terminate = new Action("terminate", "Terminate", "Kill Currently Executing Program", "");
		this.terminate.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doTerminate();
			}
		});
		this.terminate.setAccelGroup(ag);
		this.terminate.setAccelPath("<sourceWin>/Program/Terminate");
		AccelMap.changeEntry("<sourceWin>/Program/Terminate", KeyValue.t, ModifierType.MOD1_MASK, true);
		
		// Step assembly instruction action
		this.stepAsm = new Action("stepAsm", "Step Assembly Instruction", Messages.getString("SourceWindow.36"), "frysk-step-asm");
		this.stepAsm.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doAsmStep();
			}
		});
		this.stepAsm.setAccelGroup(ag);
		this.stepAsm.setAccelPath("<sourceWin>/Program/Step Assembly");
		AccelMap.changeEntry("<sourceWin>/Program/Step Assembly", KeyValue.s, ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK), true);
		this.stepAsm.connectAccelerator();
		
		// Next assembly instruction action
		this.nextAsm = new Action("nextAsm", "Next Assembly Instruction", Messages.getString("SourceWindow.38"), "frysk-next-asm");
		this.nextAsm.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doAsmNext();
			}
		});
		this.nextAsm.setAccelGroup(ag);
		this.nextAsm.setAccelPath("<sourceWin>/Program/Next Assembly");
		AccelMap.changeEntry("<sourceWin>/Program/Next Assembly", KeyValue.n, ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK), true);
		this.nextAsm.connectAccelerator();
		
		// Bottom of stack action
		this.stackBottom = new Action("stackBottom", "To Bottom of Stack", Messages.getString("SourceWindow.44"), "frysk-stack-bottom");
		this.stackBottom.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doStackBottom();
			}
		});
		this.stackBottom.setAccelGroup(ag);
		this.stackBottom.setAccelPath("<sourceWin>/Stack/Bottom");
		AccelMap.changeEntry("<sourceWin>/Stack/Bottom", KeyValue.Down, ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK), true);
		this.stackBottom.connectAccelerator();
	
		// Stack down action
		this.stackDown = new Action("stackDown", "Down One Stack Frame", Messages.getString("SourceWindow.40"), "frysk-stack-down");
		this.stackDown.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doStackDown();
			}
		});
		this.stackDown.setAccelGroup(ag);
		this.stackDown.setAccelPath("<sourceWin>/Stack/Down");
		AccelMap.changeEntry("<sourceWin>/Stack/Down", KeyValue.Down, ModifierType.MOD1_MASK, true);
		this.stackDown.connectAccelerator();
		
		// Stack up action
		this.stackUp = new Action("stack Up", "Up One Stack Frame", Messages.getString("SourceWindow.42"), "frysk-stack-up");
		this.stackUp.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doStackUp();
			}
		});
		this.stackUp.setAccelGroup(ag);
		this.stackUp.setAccelPath("<sourceWin>/Stack/Up");
		AccelMap.changeEntry("<sourceWin>/Stack/Up", KeyValue.Up, ModifierType.MOD1_MASK, true);
		this.stackUp.connectAccelerator();
	}
	
	
	private void createMenus(){
		// File menu
		MenuItem menu = new MenuItem("File", true);
		
		MenuItem mi = (MenuItem) this.close.createMenuItem();
		Menu tmp = new Menu();
		tmp.append(mi);
		
		menu.setSubmenu(tmp);
		
		((MenuBar) this.glade.getWidget("menubar")).append(menu);
		
		
		// Edit Menu
		menu = new MenuItem("Edit", true);
		tmp = new Menu();
		
		mi = (MenuItem) this.copy.createMenuItem();
		tmp.append(mi);

		mi = new MenuItem(); // Seperator
		tmp.append(mi);
		
		mi = (MenuItem) this.find.createMenuItem();
		tmp.append(mi);
		
		mi = new MenuItem(); // Seperator
		tmp.append(mi);
		
		mi = (MenuItem) this.prefsLaunch.createMenuItem();
		tmp.append(mi);
		
		menu.setSubmenu(tmp);
		((MenuBar) this.glade.getWidget("menubar")).append(menu);
		
		
		// Program Menu
		menu = new MenuItem("Program", false);
		tmp = new Menu();
		
		mi = (MenuItem) this.run.createMenuItem();
		tmp.append(mi);
		
		mi = (MenuItem) this.step.createMenuItem();
		tmp.append(mi);
		
		mi = (MenuItem) this.next.createMenuItem();
		tmp.append(mi);
		
		mi = (MenuItem) this.finish.createMenuItem();
		tmp.append(mi);
		
		mi = (MenuItem) this.cont.createMenuItem();
		tmp.append(mi);
		
		mi = (MenuItem) this.terminate.createMenuItem();
		tmp.append(mi);
		
		mi = new MenuItem(); // Seperator
		tmp.append(mi);
		
		mi = (MenuItem) this.stepAsm.createMenuItem();
		tmp.append(mi);
		
		mi = (MenuItem) this.nextAsm.createMenuItem();
		tmp.append(mi);
		
		menu.setSubmenu(tmp);
		((MenuBar) this.glade.getWidget("menubar")).append(menu);
		
		// Stack menu
		menu = new MenuItem("Stack", false);
		tmp = new Menu();
		
		mi = (MenuItem) this.stackUp.createMenuItem();
		tmp.append(mi);
		
		mi = (MenuItem) this.stackDown.createMenuItem();
		tmp.append(mi);
		
		mi = (MenuItem) this.stackBottom.createMenuItem();
		tmp.append(mi);
		
		menu.setSubmenu(tmp);
		((MenuBar) this.glade.getWidget("menubar")).append(menu);
		
		((MenuBar) this.glade.getWidget("menubar")).showAll();
	}
	
	/**
	 * Adds the icons and assigns tooltips to the toolbar items
	 */
	private void createToolBar(){
		ToolBar toolbar = (ToolBar) this.glade.getWidget(SourceWindow.GLADE_TOOLBAR_NAME);

		toolbar.insert((ToolItem) this.run.createToolItem(), 0);
		toolbar.insert((ToolItem) this.step.createToolItem(), 1);
		toolbar.insert((ToolItem) this.next.createToolItem(), 2);
		toolbar.insert((ToolItem) this.cont.createToolItem(), 3);
		toolbar.insert((ToolItem) this.finish.createToolItem(), 4);
		toolbar.insert((ToolItem) new SeparatorToolItem(),5);
		toolbar.insert((ToolItem) this.stepAsm.createToolItem(), 6);
		toolbar.insert((ToolItem) this.nextAsm.createToolItem(), 7);
		toolbar.insert((ToolItem) new SeparatorToolItem(), 8);
		toolbar.insert((ToolItem) this.stackUp.createToolItem(), 9);
		toolbar.insert((ToolItem) this.stackDown.createToolItem(), 10);
		toolbar.insert((ToolItem) this.stackBottom.createToolItem(), 11);
		
		this.glade.getWidget(SourceWindow.GLADE_TOOLBAR_NAME).showAll();
	}
	
	/**
	 * Adds icons, text, and tooltips to the widgets in the search bar
	 */
	private void createSearchBar(){
		// Add text to widgets
		((Label) this.glade.getWidget(SourceWindow.FIND_LABEL)).setLabel(Messages.getString("SourceWindow.12")); //$NON-NLS-1$
		((Label) this.glade.getWidget(SourceWindow.LINE_LABEL)).setLabel(Messages.getString("SourceWindow.13")); //$NON-NLS-1$
		
		((Button) this.glade.getWidget(SourceWindow.NEXT_FIND)).setLabel(Messages.getString("SourceWindow.14")); //$NON-NLS-1$
		((Button) this.glade.getWidget(SourceWindow.PREV_FIND)).setLabel(Messages.getString("SourceWindow.15")); //$NON-NLS-1$
		((Button) this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND)).setLabel(Messages.getString("SourceWindow.16")); //$NON-NLS-1$
		((Button) this.glade.getWidget(SourceWindow.CASE_FIND)).setLabel(Messages.getString("SourceWindow.17")); //$NON-NLS-1$
		((Button) this.glade.getWidget(SourceWindow.GOTO_BUTTON)).setLabel(Messages.getString("SourceWindow.18")); //$NON-NLS-1$
		
		// Add icons
		try {
			((Button) this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND)).setImage(new Image(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.HIGHLIGHT_PNG)));
			((Button) this.glade.getWidget(SourceWindow.NEXT_FIND)).setImage(new Image(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.FIND_NEXT_PNG)));
			((Button) this.glade.getWidget(SourceWindow.PREV_FIND)).setImage(new Image(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.FIND_PREV_PNG)));
			((Button) this.glade.getWidget(SourceWindow.GOTO_BUTTON)).setImage(new Image(new Pixbuf(SourceWindow.IMAGES_DIR+SourceWindow.FIND_GO_PNG)));
		} catch (Exception e){
			e.printStackTrace();
		}
		
		// add Tooltips
		ToolTips t = new ToolTips();
		t.setTip(this.glade.getWidget(SourceWindow.NEXT_FIND), Messages.getString("SourceWindow.19"), Messages.getString("SourceWindow.20")); //$NON-NLS-1$ //$NON-NLS-2$
		t.setTip(this.glade.getWidget(SourceWindow.PREV_FIND), Messages.getString("SourceWindow.21"), Messages.getString("SourceWindow.22")); //$NON-NLS-1$ //$NON-NLS-2$
		t.setTip(this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND), Messages.getString("SourceWindow.23"), Messages.getString("SourceWindow.24")); //$NON-NLS-1$ //$NON-NLS-2$
		t.setTip(this.glade.getWidget(SourceWindow.GOTO_BUTTON), Messages.getString("SourceWindow.25"), Messages.getString("SourceWindow.46")); //$NON-NLS-1$ //$NON-NLS-2$
		t.setTip(this.glade.getWidget(SourceWindow.CLOSE_FIND), Messages.getString("SourceWindow.47"), Messages.getString("SourceWindow.48")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Assigns Listeners to the widgets that we need to listen for events from
	 */
	private void attachEvents(){
		
		// Buttons in searchBar
		((Button) this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND)).addListener(this);
		((Button) this.glade.getWidget(SourceWindow.PREV_FIND)).addListener(this);
		((Button) this.glade.getWidget(SourceWindow.NEXT_FIND)).addListener(this);
		((Button) this.glade.getWidget(SourceWindow.CLOSE_FIND)).addListener(this);
		((Button) this.glade.getWidget(SourceWindow.GOTO_BUTTON)).addListener(this);
		
		// Text field in search bar
		((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT)).addListener(this);
		
		// function jump box
		((ComboBoxEntry) this.glade.getWidget(SourceWindow.FUNC_SELECTOR)).addListener(this);
		
//		// Stack browser
		((TreeView) this.glade.getWidget("stackBrowser")).getSelection().addListener(this);
	}
	
	/**
	 * Displays the preference window, or creates it if it is the first time this
	 * method is called
	 */
	private void launchPreferencesWindow(){
		if(this.prefWin == null)
			this.prefWin = new PreferenceWindow(this.prefs);
		else
			this.prefWin.show();
		
		// find out when the window closes and refresh the source
		this.prefWin.attachLifeCycleListener(new LifeCycleListener() {
			public boolean lifeCycleQuery(LifeCycleEvent event) {
				return false;
			}
		
			public void lifeCycleEvent(LifeCycleEvent event) {
				if(event.isOfType(LifeCycleEvent.Type.HIDE))
					SourceWindow.this.refresh();
			}
		});
	}
	
	/**
	 * Handles a ButtonEvent that is know to be of type ButtonEvent.Type.CLICK. Should
	 * not be called except from handleEvent(ButtonEvent event)
	 * 
	 * @param event ButtonEvent of type CLICK
	 */
	private void handleButtonClicked(ButtonEvent event){
		String buttonName = ((Button) event.getSource()).getName();
		
		if(buttonName.equals(SourceWindow.CLOSE_FIND))
			this.glade.getWidget(SourceWindow.FIND_BOX).hideAll();
		
		else if(buttonName.equals(SourceWindow.GOTO_BUTTON)){
			int gotoLine = Integer.parseInt(((Entry) this.glade.getWidget(SourceWindow.LINE_ENTRY)).getText());
			this.view.scrollToLine(gotoLine);
		}
		
		boolean findNext = buttonName.equals(SourceWindow.NEXT_FIND);
		boolean findPrevious = buttonName.equals(SourceWindow.PREV_FIND);
		boolean highlightAll = buttonName.equals(SourceWindow.HIGHLIGHT_FIND);
		
		if(findNext || findPrevious || highlightAll){
			boolean caseSensitive = ((CheckButton) this.glade.getWidget(SourceWindow.CASE_FIND)).getState();
			String text = ((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT)).getText();
			
			this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(StateType.NORMAL, Color.WHITE);
			
			boolean result = false;
			
			if(findNext)
				result = this.view.findNext(text, caseSensitive);
			
			else if(findPrevious)
				result = this.view.findPrevious(text, caseSensitive);
			
			else if(highlightAll)
				result = this.view.highlightAll(text, caseSensitive);
			
			if(!result)
				this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(StateType.NORMAL, Color.RED);
			else
				if(!highlightAll)
					this.view.scrollToFound();
		}
	}
	
	/**
	 * Tells the debugger to run the program
	 */
	private void doRun(){
		this.running = !this.running;
		
		// Toggle status of toolbar buttons
		this.glade.getWidget(SourceWindow.FILE_SELECTOR).setSensitive(!running);
		this.glade.getWidget(SourceWindow.FUNC_SELECTOR).setSensitive(!running);
		this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX).setSensitive(!running);
		
		// Toggle status of actions
		this.step.setSensitive(!running);
		this.next.setSensitive(!running);
		this.finish.setSensitive(!running);
		this.cont.setSensitive(!running);
		this.nextAsm.setSensitive(!running);
		this.stepAsm.setSensitive(!running);
		
		this.stackBottom.setSensitive(!running);
		this.stackUp.setSensitive(!running);
		this.stackDown.setSensitive(!running);
		
		this.copy.setSensitive(!running);
		this.find.setSensitive(!running);
		this.prefsLaunch.setSensitive(!running);
	}
	
	/**
	 * Tells the debugger to step the program
	 */
	private void doStep(){
		System.out.println("Step");
	}
	
	/**
	 * Tells the debugger to execute next
	 *
	 */
	private void doNext(){
		System.out.println("Next");
	}
	
	/**
	 * Tells the debugger to continue execution
	 *
	 */
	private void doContinue(){
		System.out.println("Continue");
	}
	
	/**
	 * Tells the debugger to finish executing the current function
	 *
	 */
	private void doFinish(){
		System.out.println("Finish");
	}
	
	/**
	 * Tells the debugger to terminate the program being debugged
	 */
	private void doTerminate(){
		System.out.println("Terminate");
	}
	
	/**
	 * Tells the debugger to step an assembly instruction
	 *
	 */
	private void doAsmStep(){
		System.out.println("Asm Step");
	}
	
	/**
	 * Tells the debugger to execute the next assembly instruction
	 *
	 */
	private void doAsmNext(){
		System.out.println("Asm Next");
	}
	
	/**
	 * Tells the debugger to move to the previous stack frame
	 *
	 */
	private void doStackUp(){
		System.out.println("Stack up");
	}
	
	/**
	 * Tells the debugger to move to the following stack frame
	 *
	 */
	private void doStackDown(){
		System.out.println("Stack down");
	}
	
	/**
	 * Tells the debugger to move to the newest stack frame
	 *
	 */
	private void doStackBottom(){
		System.out.println("Stack bottom");
	}

	public void selectionChangedEvent(TreeSelectionEvent arg0) {
		TreeView view = (TreeView) this.glade.getWidget("stackBrowser");
		TreeModel model = view.getModel();
		
		PCLocation selected = (PCLocation) model.getValue(model.getIter(view.getSelection().getSelectedRows()[0]), (DataColumnObject) dataColumns[1]);
		
		this.view.load(selected);
	}
}
