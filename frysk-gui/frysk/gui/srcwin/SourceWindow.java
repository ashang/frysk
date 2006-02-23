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

import org.gnu.gdk.Color;
import org.gnu.gdk.KeyValue;
import org.gnu.gdk.ModifierType;
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
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.Image;
import org.gnu.gtk.Label;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuBar;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.SelectionMode;
import org.gnu.gtk.SeparatorToolItem;
import org.gnu.gtk.SizeGroup;
import org.gnu.gtk.SizeGroupMode;
import org.gnu.gtk.StateType;
import org.gnu.gtk.ToolBar;
import org.gnu.gtk.ToolItem;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreeModel;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.TreeView;
import org.gnu.gtk.TreeViewColumn;
import org.gnu.gtk.Widget;
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

import frysk.dom.DOMFrysk;
import frysk.dom.DOMLine;
import frysk.gui.common.IconManager;
import frysk.gui.common.prefs.BooleanPreference;
import frysk.gui.common.prefs.PreferenceManager;
import frysk.gui.srcwin.prefs.PreferenceWindow;
import frysk.proc.Task;

/**
 * The SourceWindow displays the source or assembly level view of a Task's
 * current state of execution. It has the ability to display code that has been
 * inlined as well as optimized out by the compiler. It also provides an
 * interface to allow to user to query for variable values, set traces on
 * variables, and perform other such traditional debugging tasks.
 */
public class SourceWindow extends Window {
	/*
	 * GLADE CONSTANTS
	 */
	// Search bar widgets
	public static final String LINE_ENTRY = "lineEntry";
	public static final String FIND_TEXT = "findText";
	public static final String FIND_BOX = "findBox";
	public static final String FIND_LABEL = "findLabel"; //$NON-NLS-1$
	public static final String LINE_LABEL = "gotoLabel"; //$NON-NLS-1$
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

	// Widget that the SourceViewWidget will be placed in
	public static final String TEXT_WINDOW = "textWindow";

	// Name of the top level window
	public static final String SOURCE_WINDOW = "sourceWindow";

	// Glade file to use
	public static final String GLADE_FILE = "frysk_source.glade";

	/*
	 * END GLADE CONSTANTS
	 */

	private String gladePath;
	private LibGlade glade;

	private View view;

	private PreferenceWindow prefWin;

	// ACTIONS
	private Action close;
	private Action copy;
	private Action find;
	private Action prefsLaunch;
	private Action run;
	private Action stop;
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
	private DOMFrysk dom;

	private Task myTask;

	private StackLevel stack;

	// Data Columns for the stack browser
	private DataColumn[] stackColumns;

	// Data Columns for the variable trace viewer
	private DataColumn[] traceColumns;
	
	// Due to java-gnome bug #319415
	private ToolTips tips;

	// Private inner class to take care of the event handling
	private SourceWindowListener listener;
	
	/**
	 * Creates a new source window with the given properties. This constructor
	 * should not be called explicitly, SourceWindow objects should be created
	 * through the {@link SourceWindowFactory} class.
	 * 
	 * @param glade
	 *            The LibGlade object that contains the window for this instance
	 * @param gladePath
	 *            The path that the .glade file for the LibGlade was on
	 * @param dom
	 *            The DOM that describes the executable being debugged
	 * @param stack
	 *            The stack frame that represents the current state of execution
	 */
	public SourceWindow(LibGlade glade, String gladePath, DOMFrysk dom,
			StackLevel stack) {
		super(((Window) glade.getWidget(SOURCE_WINDOW)).getHandle());
		
		this.setIcon(IconManager.windowIcon);
		
		this.listener = new SourceWindowListener(this);
		this.glade = glade;
		this.gladePath = gladePath;
		this.dom = dom;
		this.dom.toString();
		this.stack = stack;

		this.glade.getWidget(SourceWindow.SOURCE_WINDOW).hideAll();

		AccelGroup ag = new AccelGroup();
		((Window) this.glade.getWidget(SourceWindow.SOURCE_WINDOW))
				.addAccelGroup(ag);

		this.tips = new ToolTips();

		this.createActions(ag);
		this.createMenus();
		this.createToolBar();
		this.createSearchBar();
		this.attachEvents();

		this.populateStackBrowser(this.stack);
		
		this.traceColumns = new DataColumn[] {new DataColumnString(), new DataColumnString()};
		TreeView traceView = (TreeView) this.glade.getWidget("traceBrowser");
		traceView.setModel(new ListStore(this.traceColumns));

		TreeViewColumn col = new TreeViewColumn();
		CellRenderer renderer = new CellRendererText();
		col.packStart(renderer, true);
		col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT, this.traceColumns[0]);
		col.setTitle("Name");
		traceView.appendColumn(col);
		
		col = new TreeViewColumn();
		renderer = new CellRendererText();
		col.packStart(renderer, true);
		col.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT, this.traceColumns[1]);
		col.setTitle("Value");
		traceView.appendColumn(col);
		
		
		((Label) this.glade.getWidget("stackFrame"))
				.setText("<b>Current Stack</b>");
		((Label) this.glade.getWidget("stackFrame")).setUseMarkup(true);

		((Label) this.glade.getWidget("traceFrame"))
				.setText("<b>Variable Traces</b>");
		((Label) this.glade.getWidget("traceFrame")).setUseMarkup(true);

		this.populateFunctionBox();
		((ComboBox) this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX))
				.setActive(0); //$NON-NLS-1$

		this.glade.getWidget(SOURCE_WINDOW).showAll();
		this.glade.getWidget(FIND_BOX).hideAll();
		this.refresh();
	}

	/**
	 * To be called internally when a change in the preference model occurs.
	 * Updates the window and children to reflect the new changes
	 */
	public void refresh() {
		this.view.refresh();

		if (PreferenceManager.getBooleanPreferenceValue(BooleanPreference.TOOLBAR)){
			this.glade.getWidget(SourceWindow.GLADE_TOOLBAR_NAME).showAll();
		}
		else{
			this.glade.getWidget(SourceWindow.GLADE_TOOLBAR_NAME).hideAll();
		}
	}

	/**
	 * Adds the selected variable to the variable trace window
	 * 
	 * @param var
	 *            The variable to trace
	 */
	public void addVariableTrace(Variable var) {
		TreeView traceView = (TreeView) this.glade.getWidget("traceBrowser");
		ListStore store = (ListStore) traceView.getModel();
		
		TreeIter iter = store.appendRow();
		store.setValue(iter, (DataColumnString) this.traceColumns[0], var.getName());
		store.setValue(iter, (DataColumnString) this.traceColumns[1], "0xfeedcalf");
		
		traceView.showAll();
	}

	/**
	 * @return The Task being shown by this SourceWindow
	 */
	public Task getMyTask() {
		return myTask;
	}

	/**
	 * Sets the task that is being displayed by the SourceWindow
	 * 
	 * @param myTask
	 *            The new task
	 * 
	 * TODO: This doesn't actually update the display, all it will do (if called
	 * more than once) is screw up the removal of this SourceWindow from
	 * SourceWindowFactory's HashMap. Maybe integrate into constructor?
	 */
	public void setMyTask(Task myTask) {
		this.myTask = myTask;
		this.setTitle(this.getTitle() + " - "
				+ this.myTask.getProc().getCommand() + " "
				+ this.myTask.getName() + " - " + this.myTask.getStateString());
	}

	/***************************************************************************
	 * PRIVATE METHODS
	 **************************************************************************/
	/**
	 * 
	 */
	private void resetSearchBox() {
		this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(
				StateType.NORMAL, Color.WHITE);
	}
	
	/**
	 * Populates the stack browser window
	 * 
	 * @param top
	 */
	private void populateStackBrowser(StackLevel top) {
		TreeView stackList = (TreeView) this.glade.getWidget("stackBrowser");
		
		stackColumns = new DataColumn[] { new DataColumnString(),
				new DataColumnObject() };
		ListStore listModel = new ListStore(stackColumns);

		TreeIter iter = null;
		TreeIter last = null;

		StackLevel lastStack = null;

		while (top != null) {
			iter = listModel.appendRow();

			CurrentLineSection current = top.getCurrentLine();
			boolean hasInlinedCode = false;
				
			// Go through each segment of the current line, but once we've found
			// one stop checking
			while(current != null && !hasInlinedCode){
				// Go through each line of the segment
				for(int i = current.getStartLine(); i < current.getEndLine(); i++){
					// Check for inlined code
					DOMLine line = top.getData().getLine(i);
					if(line != null && line.hasInlinedCode()){
						hasInlinedCode = true;
						break;
					}
				}
				
				current = current.getNextSection();
			}
				
			// If we've found inlined code, update the display
			if(hasInlinedCode)
				listModel.setValue(iter, (DataColumnString) stackColumns[0], top
						.getData().getFileName()
						+ "  (i)");
			else
				listModel.setValue(iter, (DataColumnString) stackColumns[0], top
						.getData().getFileName());
				
			listModel.setValue(iter, (DataColumnObject) stackColumns[1], top);

			// Save the last node so we can select it
			if (top.getNextScope() == null) {
				last = iter;
				lastStack = top;
			}

			top = top.getNextScope();
		}
		stackList.setModel(listModel);

		TreeViewColumn column = new TreeViewColumn();
		CellRenderer renderer = new CellRendererText();
		column.packStart(renderer, true);
		column.addAttributeMapping(renderer, CellRendererText.Attribute.TEXT,
				stackColumns[0]);
		stackList.appendColumn(column);

		if (this.view != null)
			((Container) ((Widget) this.view).getParent()).remove((Widget) this.view);
		this.view = new SourceView(lastStack, this);
		((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
				.add((Widget) this.view);
		this.view.showAll();

		stackList.getSelection().setMode(SelectionMode.SINGLE);
		stackList.getSelection().select(last);
		stackList.showAll();
	}
	
	/**
	 * Creates the menus and assigns hotkeys
	 */
	private void createActions(AccelGroup ag) {

		// Close action
		this.close = new Action("close", "Close", "Close Window",
				GtkStockItem.CLOSE.getString());
		this.close.setAccelGroup(ag);
		this.close.setAccelPath("<sourceWin>/File/Close");
		this.close.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.glade.getWidget(SOURCE_WINDOW).destroy();
			}
		});
		AccelMap.changeEntry("<sourceWin>/File/Close", KeyValue.x,
				ModifierType.CONTROL_MASK, true);
		this.close.connectAccelerator();

		// Copy action
		this.copy = new Action("copy", "Copy",
				"Copy Selected Text to the Clipboard", GtkStockItem.COPY
						.getString());
		this.copy.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				System.out.println("<copy />");
			}
		});
		this.copy.setAccelGroup(ag);
		this.copy.setAccelPath("<sourceWin>/Edit/Copy");
		AccelMap.changeEntry("<sourceWin>/Edit/Copy", KeyValue.c,
				ModifierType.CONTROL_MASK, true);
		this.copy.connectAccelerator();

		// Find action
		this.find = new Action("find", "Find",
				"Find Text in the Current Buffer", GtkStockItem.FIND
						.getString());
		this.find.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.glade.getWidget(SourceWindow.FIND_BOX)
						.showAll();
			}
		});
		this.find.setAccelGroup(ag);
		this.find.setAccelPath("<sourceWin>/Edit/Find");
		AccelMap.changeEntry("<sourceWin>/Edit/Find", KeyValue.f,
				ModifierType.CONTROL_MASK, true);
		this.find.connectAccelerator();

		// Launch preference window action
		this.prefsLaunch = new Action("prefs", "Preferences",
				"Edit Preferences", GtkStockItem.PREFERENCES.getString());
		this.prefsLaunch.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.launchPreferencesWindow();
			}
		});

		// Run program action
		this.run = new Action("run", "Run", "Run Program", "frysk-run");
		this.run.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doRun();
			}
		});
		this.run.setAccelGroup(ag);
		this.run.setAccelPath("<sourceWin>/Program/Run");
		AccelMap.changeEntry("<sourceWin>/Program/Run", KeyValue.r,
				ModifierType.MOD1_MASK, true);
		this.run.connectAccelerator();

		// Stop program action
		this.stop = new Action("stop", "Stop", "Stop Program execution",
				"frysk-stop");
		this.stop.addListener(new ActionListener() {
			public void actionEvent(ActionEvent arg0) {
				SourceWindow.this.doStop();
			}
		});
		this.stop.setSensitive(false);

		// Step action
		this.step = new Action("step", "Step", "Step", "frysk-step");
		this.step.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doStep();
			}
		});
		this.step.setAccelGroup(ag);
		this.step.setAccelPath("<sourceWin>/Program/Step");
		AccelMap.changeEntry("<sourceWin>/Program/Step", KeyValue.s,
				ModifierType.MOD1_MASK, true);
		this.step.connectAccelerator();

		// Next action
		this.next = new Action("next", "Next", "Next", "frysk-next");
		this.next.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doNext();
			}
		});
		this.next.setAccelGroup(ag);
		this.next.setAccelPath("<sourceWin>/Program/Next");
		AccelMap.changeEntry("<sourceWin>/Program/Next", KeyValue.n,
				ModifierType.MOD1_MASK, true);
		this.next.connectAccelerator();

		// Finish action
		this.finish = new Action("finish", "Finish", "Finish Function Call", "frysk-finish");
		this.finish.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doFinish();
			}
		});
		this.finish.setAccelGroup(ag);
		this.finish.setAccelPath("<sourceWin>/Program/Finish");
		AccelMap.changeEntry("<sourceWin>/Program/Finish", KeyValue.f,
				ModifierType.MOD1_MASK, true);
		this.finish.connectAccelerator();

		// Continue action
		this.cont = new Action("continue", "Continue", "Continue Execution", "frysk-continue");
		this.cont.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doContinue();
			}
		});
		this.cont.setAccelGroup(ag);
		this.cont.setAccelPath("<sourceWin>/Program/Continue");
		AccelMap.changeEntry("<sourceWin>/Program/Continue", KeyValue.c,
				ModifierType.MOD1_MASK, true);
		this.cont.connectAccelerator();

		// Terminate action
		this.terminate = new Action("terminate", "Terminate",
				"Kill Currently Executing Program", "");
		this.terminate.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doTerminate();
			}
		});
		this.terminate.setAccelGroup(ag);
		this.terminate.setAccelPath("<sourceWin>/Program/Terminate");
		AccelMap.changeEntry("<sourceWin>/Program/Terminate", KeyValue.t,
				ModifierType.MOD1_MASK, true);

		// Step assembly instruction action
		this.stepAsm = new Action("stepAsm", "Step Assembly Instruction",
				"Step Assembly Instruction", "frysk-stepAI");
		this.stepAsm.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doAsmStep();
			}
		});
		this.stepAsm.setAccelGroup(ag);
		this.stepAsm.setAccelPath("<sourceWin>/Program/Step Assembly");
		AccelMap.changeEntry("<sourceWin>/Program/Step Assembly", KeyValue.s,
				ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK), true);
		this.stepAsm.connectAccelerator();

		// Next assembly instruction action
		this.nextAsm = new Action("nextAsm", "Next Assembly Instruction",
				"Next Assembly Instruction", "frysk-nextAI");
		this.nextAsm.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doAsmNext();
			}
		});
		this.nextAsm.setAccelGroup(ag);
		this.nextAsm.setAccelPath("<sourceWin>/Program/Next Assembly");
		AccelMap.changeEntry("<sourceWin>/Program/Next Assembly", KeyValue.n,
				ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK), true);
		this.nextAsm.connectAccelerator();

		// Bottom of stack action
		this.stackBottom = new Action("stackBottom", "To Bottom of Stack",
				"To Bottom of Stack", "frysk-bottom");
		this.stackBottom.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doStackBottom();
			}
		});
		this.stackBottom.setAccelGroup(ag);
		this.stackBottom.setAccelPath("<sourceWin>/Stack/Bottom");
		AccelMap.changeEntry("<sourceWin>/Stack/Bottom", KeyValue.Down,
				ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK), true);
		this.stackBottom.connectAccelerator();

		// Stack down action
		this.stackDown = new Action("stackDown", "Down One Stack Frame",
				"Down One Stack Frame", "frysk-down");
		this.stackDown.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doStackDown();
			}
		});
		this.stackDown.setAccelGroup(ag);
		this.stackDown.setAccelPath("<sourceWin>/Stack/Down");
		AccelMap.changeEntry("<sourceWin>/Stack/Down", KeyValue.Down,
				ModifierType.MOD1_MASK, true);
		this.stackDown.connectAccelerator();

		// Stack up action
		this.stackUp = new Action("stack Up", "Up One Stack Frame", 
			"Up One Stack Frame", "frysk-up");
		this.stackUp.addListener(new ActionListener() {
			public void actionEvent(ActionEvent action) {
				SourceWindow.this.doStackUp();
			}
		});
		this.stackUp.setAccelGroup(ag);
		this.stackUp.setAccelPath("<sourceWin>/Stack/Up");
		AccelMap.changeEntry("<sourceWin>/Stack/Up", KeyValue.Up,
				ModifierType.MOD1_MASK, true);
		this.stackUp.connectAccelerator();
	}

	/*
	 * Populates the menus from the actions created earlier.
	 */
	private void createMenus() {
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
		mi = (MenuItem) this.stop.createMenuItem();
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
	private void createToolBar() {
		ToolBar toolbar = (ToolBar) this.glade
				.getWidget(SourceWindow.GLADE_TOOLBAR_NAME);

		ToolItem item;

		item = (ToolItem) this.run.createToolItem();
		item.setToolTip(this.tips, "Run Program", "");
		toolbar.insert(item, 0);
		item = (ToolItem) this.stop.createToolItem();
		item.setToolTip(this.tips, "Stops execution", "");
		toolbar.insert(item, 1);
		item = (ToolItem) this.step.createToolItem();
		item.setToolTip(this.tips, "Step", "");
		toolbar.insert(item, 2);
		item = (ToolItem) this.next.createToolItem();
		item.setToolTip(this.tips, "Next", "");
		toolbar.insert(item, 3);
		item = (ToolItem) this.cont.createToolItem();
		item.setToolTip(this.tips, "Cotinue Execution", "");
		toolbar.insert(item, 4);
		item = (ToolItem) this.finish.createToolItem();
		item.setToolTip(this.tips, "Finish Function Call", "");
		toolbar.insert(item, 5);
		toolbar.insert((ToolItem) new SeparatorToolItem(), 6);
		item = (ToolItem) this.stepAsm.createToolItem();
		item.setToolTip(this.tips, "Next Assembly Instruction", "");
		toolbar.insert(item, 7);
		item = (ToolItem) this.nextAsm.createToolItem();
		item.setToolTip(this.tips, "Step Assembly Instruction", "");
		toolbar.insert(item, 8);
		toolbar.insert((ToolItem) new SeparatorToolItem(), 9);
		item = (ToolItem) this.stackUp.createToolItem();
		item.setToolTip(this.tips, "Up One Stack Frame", "");
		toolbar.insert(item, 10);
		item = (ToolItem) this.stackDown.createToolItem();
		item.setToolTip(this.tips, "Down One Stack Frame", "");
		toolbar.insert(item, 11);
		item = (ToolItem) this.stackBottom.createToolItem();
		item.setToolTip(this.tips, "To Bottom of Stack", "");
		toolbar.insert(item, 12);

		toolbar.showAll();
		toolbar.setToolTips(true);
	}

	/**
	 * Adds icons, text, and tooltips to the widgets in the search bar
	 */
	private void createSearchBar() {

		((Button) this.glade.getWidget(SourceWindow.NEXT_FIND))
				.setImage(new Image(GtkStockItem.GO_FORWARD, IconSize.BUTTON));
		((Button) this.glade.getWidget(SourceWindow.PREV_FIND))
				.setImage(new Image(GtkStockItem.GO_BACK, IconSize.BUTTON));
		((Button) this.glade.getWidget(SourceWindow.GOTO_BUTTON))
				.setImage(new Image(GtkStockItem.JUMP_TO, IconSize.BUTTON));
		((Button) this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND))
				.setImage(new Image(new GtkStockItem("frysk-highlight"),
						IconSize.BUTTON));

		SizeGroup group1 = new SizeGroup(SizeGroupMode.HORIZONTAL);
		group1.addWidget(this.glade.getWidget(CLOSE_FIND));
		group1.addWidget(this.glade.getWidget("gotoPadding"));
		
		SizeGroup group2 = new SizeGroup(SizeGroupMode.HORIZONTAL);
		group2.addWidget(this.glade.getWidget(FIND_LABEL));
		group2.addWidget(this.glade.getWidget(LINE_LABEL));
		
		// add Tooltips
		tips.setTip(this.glade.getWidget(SourceWindow.NEXT_FIND),
						"Find Next Match", "Locate the next occurance in the file"); //$NON-NLS-1$ //$NON-NLS-2$
		tips.setTip(this.glade.getWidget(SourceWindow.PREV_FIND),
						"Find Previous Match", "Locate the previous occurance in the file"); //$NON-NLS-1$ //$NON-NLS-2$
		tips.setTip(this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND),
						"Highlight All Matches", "Locate all occurances in the file"); //$NON-NLS-1$ //$NON-NLS-2$
		tips.setTip(this.glade.getWidget(SourceWindow.GOTO_BUTTON),
						"Go to Entered Line Number", "Jump to the line number that was entered"); //$NON-NLS-1$ //$NON-NLS-2$
		tips.setTip(this.glade.getWidget(SourceWindow.CLOSE_FIND),
						"Hide Find Window", "Close the find window"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Assigns Listeners to the widgets that we need to listen for events from
	 */
	private void attachEvents() {

		// Buttons in searchBar
		((Button) this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND))
				.addListener(listener);
		((Button) this.glade.getWidget(SourceWindow.PREV_FIND))
				.addListener(listener);
		((Button) this.glade.getWidget(SourceWindow.NEXT_FIND))
				.addListener(listener);
		((Button) this.glade.getWidget(SourceWindow.CLOSE_FIND))
				.addListener(listener);
		((Button) this.glade.getWidget(SourceWindow.GOTO_BUTTON))
				.addListener(listener);

		// Text field in search bar
		((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT))
				.addListener(listener);

		// function jump box
		((ComboBoxEntry) this.glade.getWidget(SourceWindow.FUNC_SELECTOR))
				.addListener(listener);
		
		// Mode box
		((ComboBox) this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX)).addListener(listener);

		// // Stack browser
		((TreeView) this.glade.getWidget("stackBrowser")).getSelection()
				.addListener(listener);
	}

	/**
	 * Displays the preference window, or creates it if it is the first time
	 * this method is called
	 */
	private void launchPreferencesWindow() {
		if (this.prefWin == null)
			this.prefWin = new PreferenceWindow(this.gladePath);
		else
			this.prefWin.show();

		// find out when the window closes and refresh the source
		this.prefWin.attachLifeCycleListener(new LifeCycleListener() {
			public boolean lifeCycleQuery(LifeCycleEvent event) {
				return false;
			}

			public void lifeCycleEvent(LifeCycleEvent event) {
				if (event.isOfType(LifeCycleEvent.Type.HIDE))
					SourceWindow.this.refresh();
			}
		});
	}

	private void hideFindBox(){
		this.glade.getWidget(SourceWindow.FIND_BOX).hideAll();
	}
	
	private void gotoLine(){
		String text = ((Entry) this.glade
				.getWidget(SourceWindow.LINE_ENTRY)).getText();
		try {
			int gotoLine = Integer.parseInt(text);
			this.view.scrollToLine(gotoLine);
		}
		// If it's not a number in the box (or if it's nothing), return
		catch (NumberFormatException e) {
			return;
		}
	}
	
	private void doFindNext(){
		boolean caseSensitive = ((CheckButton) this.glade
				.getWidget(SourceWindow.CASE_FIND)).getState();
		String text = ((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT))
				.getText();

		// Do nothing for if nothing to search for
		if (text.trim().equals(""))
			return;

		resetSearchBox();
		
		if (!this.view.findNext(text, caseSensitive))
			this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(
					StateType.NORMAL, Color.RED);
	}
	
	private void doFindPrev(){
		boolean caseSensitive = ((CheckButton) this.glade
				.getWidget(SourceWindow.CASE_FIND)).getState();
		String text = ((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT))
				.getText();

		// Do nothing for if nothing to search for
		if (text.trim().equals(""))
			return;

		resetSearchBox();
		
		if (!this.view.findPrevious(text, caseSensitive))
			this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(
					StateType.NORMAL, Color.RED);
	}
	
	private void doHighlightAll(){
		boolean caseSensitive = ((CheckButton) this.glade
				.getWidget(SourceWindow.CASE_FIND)).getState();
		String text = ((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT))
				.getText();

		// Do nothing for if nothing to search for
		if (text.trim().equals(""))
			return;

		resetSearchBox();
		
		if (!this.view.highlightAll(text, caseSensitive))
			this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(
					StateType.NORMAL, Color.RED);
	}

	private void doScrollTofunction(String text){
		this.view.scrollToFunction(text + "_FUNC");
	}
	
	private void switchToSourceMode(){
		/*
		 * If we're switching from Assembly or Mixed mode, we can just toggle the
		 * state.
		 */
		if(this.view instanceof SourceView){
			((SourceView) this.view).setMode(SourceBuffer.SOURCE_MODE);
		}
		/*
		 * If we're switching from Source/Assembly mode, we need to re-create the
		 * source view widget
		 */
		else{
			((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).
				remove(((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).getChild());
			this.view = new SourceView(this.view.getScope(), this);
			
			((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
				.add((Widget) this.view);
			this.view.showAll();
		}
	}
	
	private void switchToAsmMode(){
		/*
		 * If we're switching from Source or Mixed more, we can just toggle
		 * the state
		 */
		if(this.view instanceof SourceView){
			((SourceView) this.view).setMode(SourceBuffer.ASM_MODE);
		}
		/*
		 * If we're switching from Source/Assembly mode, we need to re-create the
		 * source view widget
		 */
		else{
			((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).
				remove(((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).getChild());
			this.view = new SourceView(this.view.getScope(), this, SourceBuffer.ASM_MODE);
			
			((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
				.add((Widget) this.view);
			this.view.showAll();
		}
	}
	
	private void switchToMixedMode(){
		/*
		 * If we're switching from Source or Assembly we can just toggle the
		 * state
		 */
		if(this.view instanceof SourceView){
			((SourceView) this.view).setMode(SourceBuffer.MIXED_MODE);
		}
		/*
		 * If we're switching from Source/Assembly mode, we need to re-create the
		 * source view widget
		 */
		else{
			((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).
				remove(((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).getChild());
			this.view = new SourceView(this.view.getScope(), this);
			
			((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
				.add((Widget) this.view);
			((SourceView) this.view).setMode(SourceBuffer.MIXED_MODE);
			this.view.showAll();	
		}
	}
	
	private void switchToSourceAsmMode(){
		if(!(this.view instanceof MixedView)){
			// Replace the SourceView with a Mixedview to display Source/Assembly mode
			((Container) this.view.getParent()).remove((Widget) this.view);
			this.view = new MixedView(this.view.getScope(), this);
			
			((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
				.addWithViewport((Widget) this.view);
			this.view.showAll();
		}
	}
	
	private void updateShownStackFrame(){
		TreeView view = (TreeView) this.glade.getWidget("stackBrowser");
		TreeModel model = view.getModel();

		TreePath[] paths = view.getSelection().getSelectedRows();
		if(paths.length == 0)
			return;
		
		StackLevel selected = (StackLevel) model.getValue(model.getIter(paths[0]),
				(DataColumnObject) stackColumns[1]);

		((Label) this.glade.getWidget("sourceLabel")).setText("<b>"
				+ selected.getData().getFileName() + "</b>");
		((Label) this.glade.getWidget("sourceLabel")).setUseMarkup(true);
		this.view.load(selected);
		this.view.showAll();
		this.populateFunctionBox();
	}
	
	/**
	 * Tells the debugger to run the program
	 */
	private void doRun() {
		// Set status of toolbar buttons
		this.glade.getWidget(SourceWindow.FUNC_SELECTOR).setSensitive(false);
		this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX).setSensitive(false);

		// Set status of actions
		this.run.setSensitive(false);
		this.stop.setSensitive(true);
		this.step.setSensitive(false);
		this.next.setSensitive(false);
		this.finish.setSensitive(false);
		this.cont.setSensitive(false);
		this.nextAsm.setSensitive(false);
		this.stepAsm.setSensitive(false);

		this.stackBottom.setSensitive(false);
		this.stackUp.setSensitive(false);
		this.stackDown.setSensitive(false);

		this.copy.setSensitive(false);
		this.find.setSensitive(false);
		this.prefsLaunch.setSensitive(false);
	}

	private void doStop() {
		// Set status of toolbar buttons
		this.glade.getWidget(SourceWindow.FUNC_SELECTOR).setSensitive(true);
		this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX).setSensitive(true);

		// Set status of actions
		this.run.setSensitive(true);
		this.stop.setSensitive(false);
		this.step.setSensitive(true);
		this.next.setSensitive(true);
		this.finish.setSensitive(true);
		this.cont.setSensitive(true);
		this.nextAsm.setSensitive(true);
		this.stepAsm.setSensitive(true);

		this.stackBottom.setSensitive(true);
		this.stackUp.setSensitive(true);
		this.stackDown.setSensitive(true);

		this.copy.setSensitive(true);
		this.find.setSensitive(true);
		this.prefsLaunch.setSensitive(true);
	}

	/**
	 * Tells the debugger to step the program
	 */
	private void doStep() {
		System.out.println("Step");
	}

	/**
	 * Tells the debugger to execute next
	 * 
	 */
	private void doNext() {
		System.out.println("Next");
	}

	/**
	 * Tells the debugger to continue execution
	 * 
	 */
	private void doContinue() {
		System.out.println("Continue");
	}

	/**
	 * Tells the debugger to finish executing the current function
	 * 
	 */
	private void doFinish() {
		System.out.println("Finish");
	}

	/**
	 * Tells the debugger to terminate the program being debugged
	 */
	private void doTerminate() {
		System.out.println("Terminate");
	}

	/**
	 * Tells the debugger to step an assembly instruction
	 * 
	 */
	private void doAsmStep() {
		System.out.println("Asm Step");
	}

	/**
	 * Tells the debugger to execute the next assembly instruction
	 * 
	 */
	private void doAsmNext() {
		System.out.println("Asm Next");
	}

	/**
	 * Tells the debugger to move to the previous stack frame
	 * 
	 */
	private void doStackUp() {
		System.out.println("Stack up");
		TreeView stackList = (TreeView) this.glade.getWidget("stackBrowser");

		int selected = stackList.getSelection().getSelectedRows()[0]
				.getIndices()[0];

		// Can't move above top stack
		if (selected == 0)
			return;

		stackList.getSelection().select(
				stackList.getModel().getIter("" + (selected - 1)));
	}

	/**
	 * Tells the debugger to move to the following stack frame
	 * 
	 */
	private void doStackDown() {
		System.out.println("Stack down");
		TreeView stackList = (TreeView) this.glade.getWidget("stackBrowser");

		int selected = stackList.getSelection().getSelectedRows()[0]
				.getIndices()[0];

		int max = 0;
		TreeIter iter = stackList.getModel().getIter("" + max);
		while (iter != null)
			iter = stackList.getModel().getIter("" + max++);

		// Can't move below bottom stack
		if (selected == max - 2)
			return;

		stackList.getSelection().select(
				stackList.getModel().getIter("" + (selected + 1)));
	}

	/**
	 * Tells the debugger to move to the newest stack frame
	 * 
	 */
	private void doStackBottom() {
		System.out.println("Stack bottom");
		TreeView stackList = (TreeView) this.glade.getWidget("stackBrowser");

		int max = 0;
		TreeIter iter = stackList.getModel().getIter("" + max);
		while (iter != null)
			iter = stackList.getModel().getIter("" + max++);

		stackList.getSelection().select(
				stackList.getModel().getIter("" + (max - 2)));
	}

	/*
	 * Populates the "goto function" pull-down menu with the names of all the
	 * functions in the current scope
	 */
	private void populateFunctionBox() {
		ComboBoxEntry box = (ComboBoxEntry) this.glade
				.getWidget(SourceWindow.FUNC_SELECTOR);
		DataColumnString col = new DataColumnString();
		ListStore newModel = new ListStore(new DataColumn[] { col });
		Vector funcs = this.view.getFunctions();
		TreeIter iter = newModel.appendRow();
		for (int i = 0; i < funcs.size(); i++) {
			newModel.setValue(iter, col, ((String) funcs.get(i)).split("_")[0]);
			if (i != funcs.size() - 1)
				iter = newModel.appendRow();
		}

		box.setModel(newModel);
	}
	
	private class SourceWindowListener implements ButtonListener, 
			EntryListener, ComboBoxListener, TreeSelectionListener{

		private SourceWindow target;
		
		public SourceWindowListener(SourceWindow target){
			this.target = target;
		}
		
		public void buttonEvent(ButtonEvent event) {
			if(!event.isOfType(ButtonEvent.Type.CLICK))
				return;
			
			String buttonName = ((Button) event.getSource()).getName();
			if (buttonName.equals(SourceWindow.CLOSE_FIND))
				target.hideFindBox();
			else if(buttonName.equals(SourceWindow.GOTO_BUTTON))
				target.gotoLine();
			else if(buttonName.equals(SourceWindow.NEXT_FIND))
				target.doFindNext();
			else if(buttonName.equals(SourceWindow.PREV_FIND))
				target.doFindPrev();
			else if(buttonName.equals(SourceWindow.HIGHLIGHT_FIND))
				target.doHighlightAll();
		}

		public void entryEvent(EntryEvent event) {
			if (event.isOfType(EntryEvent.Type.DELETE_TEXT))
				target.resetSearchBox();
			else if (event.isOfType(EntryEvent.Type.CHANGED))
				target.doFindNext();
		}

		public void comboBoxEvent(ComboBoxEvent event) {
			String text = ((ComboBox) event.getSource()).getActiveText();
			
			// The only ComboBoxEntry is the function goto box
			if(event.getSource() instanceof ComboBoxEntry){
				target.doScrollTofunction(text);
			}
			/*
			 * The only widget other than the function goto box that this listener is
			 * added to is the mode selector: so we know by know that it must have come
			 * from this widget
			 */
			else{
				
				// Switch to source mode
				if(text.equals("SOURCE"))
					target.switchToSourceMode();
				// Switch to Assembly mode
				else if(text.equals("ASM"))
					target.switchToAsmMode();
				// Switch to Mixed mode
				else if(text.equals("MIXED"))
					target.switchToMixedMode();
				/*
				 * Switch to Source/Assembly mode - we only need to worry about this
				 * case if we're switching from Source, Assembly, or Mixed view. If 
				 * we were previously in Source/Assembly view we don't need to
				 * do anything
				 */
				else if(text.equals("SOURCE/ASM"))
					target.switchToSourceAsmMode();
				
			}
		}

		public void selectionChangedEvent(TreeSelectionEvent arg0) {
			target.updateShownStackFrame();
		}
		
	}
}
