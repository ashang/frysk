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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import org.gnu.gdk.Color;
import org.gnu.gdk.KeyValue;
import org.gnu.gdk.ModifierType;
import org.gnu.glade.GladeXMLException;
import org.gnu.glade.LibGlade;
import org.gnu.gtk.AccelGroup;
import org.gnu.gtk.AccelMap;
import org.gnu.gtk.Action;
import org.gnu.gtk.Button;
import org.gnu.gtk.CheckButton;
import org.gnu.gtk.ComboBox;
import org.gnu.gtk.Container;
import org.gnu.gtk.DataColumn;
import org.gnu.gtk.DataColumnString;
import org.gnu.gtk.Entry;
import org.gnu.gtk.EntryCompletion;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.Image;
import org.gnu.gtk.Label;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuBar;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.ScrolledWindow;
import org.gnu.gtk.SeparatorToolItem;
import org.gnu.gtk.StateType;
import org.gnu.gtk.ToggleAction;
import org.gnu.gtk.ToolBar;
import org.gnu.gtk.ToolItem;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.TreeIter;
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
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;

import frysk.dom.DOMFrysk;
import frysk.dom.DOMSource;
import frysk.gui.common.IconManager;
import frysk.gui.common.prefs.BooleanPreference;
import frysk.gui.common.prefs.PreferenceManager;
import frysk.gui.common.prefs.PreferenceWindow;
import frysk.gui.common.prefs.BooleanPreference.BooleanPreferenceListener;
import frysk.gui.monitor.WindowManager;
import frysk.gui.srcwin.CurrentStackView.StackViewListener;
import frysk.gui.srcwin.prefs.SourceWinPreferenceGroup;
import frysk.proc.Task;

/**
 * The SourceWindow displays the source or assembly level view of a Task's
 * current state of execution. It has the ability to display code that has been
 * inlined as well as optimized out by the compiler. It also provides an
 * interface to allow to user to query for variable values, set traces on
 * variables, and perform other such traditional debugging tasks.
 */
public class SourceWindow
    extends Window
{
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

  public static final String CLOSE_FIND = "closeFind"; //$NON-NLS-1$

  // Widget names - toolbar
  public static final String GLADE_TOOLBAR_NAME = "toolbar"; //$NON-NLS-1$

  public static final String FILE_SELECTOR = "fileSelector";

  public static final String VIEW_COMBO_BOX = "viewComboBox";

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

  // private PreferenceWindow prefWin;

  // ACTIONS
  private Action close;

  private Action copy;

  private ToggleAction find;

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

  private ToggleAction toggleRegisterWindow;

  private ToggleAction toggleMemoryWindow;

//  private DOMFrysk dom;

  private Task myTask;

  private StackLevel stack;

  private CurrentStackView stackView;

  private VariableWatchView watchView;

  // Due to java-gnome bug #319415
  private ToolTips tips;

  // Private inner class to take care of the event handling
  private SourceWindowListener listener;

  /**
   * Creates a new source window with the given properties. This constructor
   * should not be called explicitly, SourceWindow objects should be created
   * through the {@link SourceWindowFactory} class.
   * 
   * @param glade The LibGlade object that contains the window for this instance
   * @param gladePath The path that the .glade file for the LibGlade was on
   * @param dom The DOM that describes the executable being debugged
   * @param stack The stack frame that represents the current state of execution
   */
  public SourceWindow (LibGlade glade, String gladePath, DOMFrysk dom,
                       StackLevel stack)
  {
    super(((Window) glade.getWidget(SOURCE_WINDOW)).getHandle());

    this.setIcon(IconManager.windowIcon);

    this.listener = new SourceWindowListener(this);
    this.glade = glade;
    this.gladePath = gladePath;
//    this.dom = dom;
    this.stack = stack;

    this.glade.getWidget(SourceWindow.SOURCE_WINDOW).hideAll();

    AccelGroup ag = new AccelGroup();
    ((Window) this.glade.getWidget(SourceWindow.SOURCE_WINDOW)).addAccelGroup(ag);

    this.tips = new ToolTips();

    this.populateStackBrowser(this.stack);

    this.createActions(ag);
    this.createMenus();
    this.createToolBar();
    this.createSearchBar();

    ((ComboBox) this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX)).setActive(0);

    this.attachEvents();

    this.watchView = new VariableWatchView();
    ScrolledWindow sw = (ScrolledWindow) this.glade.getWidget("traceScrolledWindow");
    sw.add(this.watchView);

    this.hideAll();
    this.showAll();
    this.glade.getWidget(FIND_BOX).hideAll();
  }

  /**
   * Toggles whether the toolbar is visible
   * 
   * @param value Whether or not to show the toolbar
   */
  public void setShowToolbar (boolean value)
  {
    if (value)
      this.glade.getWidget(SourceWindow.GLADE_TOOLBAR_NAME).showAll();
    else
      this.glade.getWidget(SourceWindow.GLADE_TOOLBAR_NAME).hideAll();
  }

  /**
   * Adds the selected variable to the variable trace window
   * 
   * @param var The variable to trace
   */
  public void addVariableTrace (Variable var)
  {
    this.watchView.addTrace(var);
  }

  /**
   * @return The Task being shown by this SourceWindow
   */
  public Task getMyTask ()
  {
    return myTask;
  }

  /**
   * Sets the task that is being displayed by the SourceWindow
   * 
   * @param myTask The new task TODO: This doesn't actually update the display,
   *          all it will do (if called more than once) is screw up the removal
   *          of this SourceWindow from SourceWindowFactory's HashMap. Maybe
   *          integrate into constructor?
   */
  public void setMyTask (Task myTask)
  {
    this.myTask = myTask;
    this.setTitle(this.getTitle() + " - " + this.myTask.getProc().getCommand()
                  + " " + this.myTask.getName());
  }

  /*****************************************************************************
   * PRIVATE METHODS
   ****************************************************************************/
  /**
   * 
   */
  private void resetSearchBox ()
  {
    this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(StateType.NORMAL,
                                                              Color.WHITE);
  }

  /**
   * Populates the stack browser window
   * 
   * @param top
   */
  private void populateStackBrowser (StackLevel top)
  {
    stackView = new CurrentStackView(top);

    StackLevel lastStack = stackView.getCurrentLevel();

    if (this.view != null)
      ((Container) ((Widget) this.view).getParent()).remove((Widget) this.view);
    
    this.view = new SourceView(lastStack, this);
    ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).add((Widget) this.view);
    this.view.showAll();

    ScrolledWindow sw = (ScrolledWindow) this.glade.getWidget("stackScrolledWindow");
    sw.add(stackView);

    stackView.showAll();
  }

  /**
   * Creates the menus and assigns hotkeys
   */
  private void createActions (AccelGroup ag)
  {

    // Close action
    this.close = new Action("close", "Close", "Close Window",
                            GtkStockItem.CLOSE.getString());
    this.close.setAccelGroup(ag);
    this.close.setAccelPath("<sourceWin>/File/Close");
    this.close.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.glade.getWidget(SOURCE_WINDOW).destroy();
      }
    });
    AccelMap.changeEntry("<sourceWin>/File/Close", KeyValue.x,
                         ModifierType.CONTROL_MASK, true);
    this.close.connectAccelerator();

    // Copy action
    this.copy = new Action("copy", "Copy",
                           "Copy Selected Text to the Clipboard",
                           GtkStockItem.COPY.getString());
    this.copy.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        System.out.println("<copy />");
      }
    });
    this.copy.setAccelGroup(ag);
    this.copy.setAccelPath("<sourceWin>/Edit/Copy");
    AccelMap.changeEntry("<sourceWin>/Edit/Copy", KeyValue.c,
                         ModifierType.CONTROL_MASK, true);
    this.copy.connectAccelerator();
    this.copy.setSensitive(false);

    // Find action
    this.find = new ToggleAction("find", "Find",
                                 "Find Text in the Current Buffer",
                                 GtkStockItem.FIND.getString());
    this.find.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        if (SourceWindow.this.find.getActive())
          SourceWindow.this.showFindBox();
        else
          SourceWindow.this.hideFindBox();
      }
    });
    this.find.setAccelGroup(ag);
    this.find.setAccelPath("<sourceWin>/Edit/Find");
    AccelMap.changeEntry("<sourceWin>/Edit/Find", KeyValue.f,
                         ModifierType.CONTROL_MASK, true);
    this.find.connectAccelerator();

    // Launch preference window action
    this.prefsLaunch = new Action("prefs", "Preferences", "Edit Preferences",
                                  GtkStockItem.PREFERENCES.getString());
    this.prefsLaunch.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.launchPreferencesWindow();
      }
    });

    // Run program action
    this.run = new Action("run", "Run", "Run Program", "frysk-run");
    this.run.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doRun();
      }
    });
    this.run.setAccelGroup(ag);
    this.run.setAccelPath("<sourceWin>/Program/Run");
    AccelMap.changeEntry("<sourceWin>/Program/Run", KeyValue.r,
                         ModifierType.MOD1_MASK, true);
    this.run.connectAccelerator();
    this.run.setSensitive(false);

    // Stop program action
    this.stop = new Action("stop", "Stop", "Stop Program execution",
                           "frysk-stop");
    this.stop.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent arg0)
      {
        SourceWindow.this.doStop();
      }
    });
    this.stop.setSensitive(false);

    // Step action
    this.step = new Action("step", "Step", "Step", "frysk-step");
    this.step.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doStep();
      }
    });
    this.step.setAccelGroup(ag);
    this.step.setAccelPath("<sourceWin>/Program/Step");
    AccelMap.changeEntry("<sourceWin>/Program/Step", KeyValue.s,
                         ModifierType.MOD1_MASK, true);
    this.step.connectAccelerator();
    this.step.setSensitive(false);

    // Next action
    this.next = new Action("next", "Next", "Next", "frysk-next");
    this.next.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doNext();
      }
    });
    this.next.setAccelGroup(ag);
    this.next.setAccelPath("<sourceWin>/Program/Next");
    AccelMap.changeEntry("<sourceWin>/Program/Next", KeyValue.n,
                         ModifierType.MOD1_MASK, true);
    this.next.connectAccelerator();
    this.next.setSensitive(false);

    // Finish action
    this.finish = new Action("finish", "Finish", "Finish Function Call",
                             "frysk-finish");
    this.finish.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doFinish();
      }
    });
    this.finish.setAccelGroup(ag);
    this.finish.setAccelPath("<sourceWin>/Program/Finish");
    AccelMap.changeEntry("<sourceWin>/Program/Finish", KeyValue.f,
                         ModifierType.MOD1_MASK, true);
    this.finish.connectAccelerator();
    this.finish.setSensitive(false);

    // Continue action
    this.cont = new Action("continue", "Continue", "Continue Execution",
                           "frysk-continue");
    this.cont.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doContinue();
      }
    });
    this.cont.setAccelGroup(ag);
    this.cont.setAccelPath("<sourceWin>/Program/Continue");
    AccelMap.changeEntry("<sourceWin>/Program/Continue", KeyValue.c,
                         ModifierType.MOD1_MASK, true);
    this.cont.connectAccelerator();
    this.cont.setSensitive(false);

    // Terminate action
    this.terminate = new Action("terminate", "Terminate",
                                "Kill Currently Executing Program", "");
    this.terminate.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doTerminate();
      }
    });
    this.terminate.setAccelGroup(ag);
    this.terminate.setAccelPath("<sourceWin>/Program/Terminate");
    AccelMap.changeEntry("<sourceWin>/Program/Terminate", KeyValue.t,
                         ModifierType.MOD1_MASK, true);
    this.terminate.setSensitive(false);

    // Step assembly instruction action
    this.stepAsm = new Action("stepAsm", "Step Assembly Instruction",
                              "Step Assembly Instruction", "frysk-stepAI");
    this.stepAsm.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doAsmStep();
      }
    });
    this.stepAsm.setAccelGroup(ag);
    this.stepAsm.setAccelPath("<sourceWin>/Program/Step Assembly");
    AccelMap.changeEntry("<sourceWin>/Program/Step Assembly", KeyValue.s,
                         ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK),
                         true);
    this.stepAsm.connectAccelerator();
    this.stepAsm.setSensitive(false);

    // Next assembly instruction action
    this.nextAsm = new Action("nextAsm", "Next Assembly Instruction",
                              "Next Assembly Instruction", "frysk-nextAI");
    this.nextAsm.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doAsmNext();
      }
    });
    this.nextAsm.setAccelGroup(ag);
    this.nextAsm.setAccelPath("<sourceWin>/Program/Next Assembly");
    AccelMap.changeEntry("<sourceWin>/Program/Next Assembly", KeyValue.n,
                         ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK),
                         true);
    this.nextAsm.connectAccelerator();
    this.nextAsm.setSensitive(false);

    // Bottom of stack action
    this.stackBottom = new Action("stackBottom", "To Bottom of Stack",
                                  "To Bottom of Stack", "frysk-bottom");
    this.stackBottom.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doStackBottom();
      }
    });
    this.stackBottom.setAccelGroup(ag);
    this.stackBottom.setAccelPath("<sourceWin>/Stack/Bottom");
    AccelMap.changeEntry("<sourceWin>/Stack/Bottom", KeyValue.Down,
                         ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK),
                         true);
    this.stackBottom.connectAccelerator();

    // Stack down action
    this.stackDown = new Action("stackDown", "Down One Stack Frame",
                                "Down One Stack Frame", "frysk-down");
    this.stackDown.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doStackDown();
      }
    });
    this.stackDown.setAccelGroup(ag);
    this.stackDown.setAccelPath("<sourceWin>/Stack/Down");
    AccelMap.changeEntry("<sourceWin>/Stack/Down", KeyValue.Down,
                         ModifierType.MOD1_MASK, true);
    this.stackDown.connectAccelerator();

    // Stack up action
    this.stackUp = new Action("stackUp", "Up One Stack Frame",
                              "Up One Stack Frame", "frysk-up");
    this.stackUp.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doStackUp();
      }
    });
    this.stackUp.setAccelGroup(ag);
    this.stackUp.setAccelPath("<sourceWin>/Stack/Up");
    AccelMap.changeEntry("<sourceWin>/Stack/Up", KeyValue.Up,
                         ModifierType.MOD1_MASK, true);
    this.stackUp.connectAccelerator();

    // Toggle view source window
    this.toggleRegisterWindow = new ToggleAction("toggleRegWindow",
                                                 "Register Window",
                                                 "Toggle the Register Window",
                                                 "");
    this.toggleRegisterWindow.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent arg0)
      {
        SourceWindow.this.toggleRegisterWindow();
      }
    });

    this.toggleMemoryWindow = new ToggleAction("toggleMemWindow",
                                               "Memory Window",
                                               "Toggle the Memory Window", "");
    this.toggleMemoryWindow.addListener(new ActionListener()
    {
      public void actionEvent (ActionEvent arg0)
      {
        SourceWindow.this.toggleMemoryWindow();
      }
    });
  }

  /*
   * Populates the menus from the actions created earlier.
   */
  private void createMenus ()
  {
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

    // View Menu
    menu = new MenuItem("View", false);
    tmp = new Menu();

    mi = (MenuItem) this.toggleRegisterWindow.createMenuItem();
    tmp.append(mi);

    mi = (MenuItem) this.toggleMemoryWindow.createMenuItem();
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
  private void createToolBar ()
  {
    ToolBar toolbar = (ToolBar) this.glade.getWidget(SourceWindow.GLADE_TOOLBAR_NAME);

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
    item.setToolTip(this.tips, "Continue Execution", "");
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
  private void createSearchBar ()
  {
    // we do this to ovewrite a bug (?) where when we set the label of the
    // button, the text disappears too
    ((Button) this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND)).setImage(new Image(
                                                                                    new GtkStockItem(
                                                                                                     "frysk-highlight"),
                                                                                    IconSize.BUTTON));
    ((Button) this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND)).setLabel("Highlight All");

    // add Tooltips
    tips.setTip(this.glade.getWidget(SourceWindow.NEXT_FIND),
                "Find Next Match", "Locate the next occurance in the file"); //$NON-NLS-1$ //$NON-NLS-2$
    tips.setTip(
                this.glade.getWidget(SourceWindow.PREV_FIND),
                "Find Previous Match", "Locate the previous occurance in the file"); //$NON-NLS-1$ //$NON-NLS-2$
    tips.setTip(this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND),
                "Highlight All Matches", "Locate all occurances in the file"); //$NON-NLS-1$ //$NON-NLS-2$
    tips.setTip(this.glade.getWidget(SourceWindow.CLOSE_FIND),
                "Hide Find Window", "Close the find window"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Assigns Listeners to the widgets that we need to listen for events from
   */
  private void attachEvents ()
  {

    // Buttons in searchBar
    ((Button) this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND)).addListener(listener);
    ((Button) this.glade.getWidget(SourceWindow.PREV_FIND)).addListener(listener);
    ((Button) this.glade.getWidget(SourceWindow.NEXT_FIND)).addListener(listener);
    ((Button) this.glade.getWidget(SourceWindow.CLOSE_FIND)).addListener(listener);

    // Text field in search bar
    ((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT)).addListener(listener);

    // function jump box
    ((Entry) this.glade.getWidget("toolbarGotoBox")).addListener(listener);
    EntryCompletion completion = new EntryCompletion();
    completion.setInlineCompletion(false);
    completion.setPopupCompletion(true);
    DataColumn[] cols = { new DataColumnString() };
    ListStore store = new ListStore(cols);

    Vector funcs = this.view.getFunctions();
    for (int i = 0; i < funcs.size(); i++)
      {
        TreeIter iter = store.appendRow();
        store.setValue(iter, (DataColumnString) cols[0], (String) funcs.get(i));
      }

    completion.setModel(store);
    completion.setTextColumn(cols[0].getColumn());
    ((Entry) this.glade.getWidget("toolbarGotoBox")).setCompletion(completion);

    ((Entry) this.glade.getWidget("toolbarGotoBox")).addListener(new MouseListener()
    {

      public boolean mouseEvent (MouseEvent arg0)
      {
        if (arg0.isOfType(MouseEvent.Type.BUTTON_PRESS)
            || arg0.getButtonPressed() == MouseEvent.BUTTON1)
          {

            Entry source = (Entry) arg0.getSource();
            source.selectRegion(0, source.getText().length());

            return false;
          }
        return false;
      }

    });

    // Register Window
    WindowManager.theManager.registerWindow.addListener(new LifeCycleListener()
    {
      public boolean lifeCycleQuery (LifeCycleEvent arg0)
      {
        // clicked on the 'x' - hide it and toggle the event
        if (arg0.isOfType(LifeCycleEvent.Type.DELETE))
          {
            ((Window) arg0.getSource()).hideAll();
            SourceWindow.this.toggleRegisterWindow.setActive(false);
            return true;
          }

        return false;
      }

      public void lifeCycleEvent (LifeCycleEvent arg0)
      {
        if (arg0.isOfType(LifeCycleEvent.Type.HIDE))
          SourceWindow.this.toggleRegisterWindow.setActive(false);
      }
    });

    // Memory Window
    WindowManager.theManager.memoryWindow.addListener(new LifeCycleListener()
    {
      public boolean lifeCycleQuery (LifeCycleEvent arg0)
      {
        // clicked on the 'x' - hide it and toggle the event
        if (arg0.isOfType(LifeCycleEvent.Type.DELETE))
          {
            ((Window) arg0.getSource()).hideAll();
            SourceWindow.this.toggleMemoryWindow.setActive(false);
            return true;
          }

        return false;
      }

      public void lifeCycleEvent (LifeCycleEvent arg0)
      {
        if (arg0.isOfType(LifeCycleEvent.Type.HIDE))
          SourceWindow.this.toggleMemoryWindow.setActive(false);
      }
    });

    // Mode box
    ((ComboBox) this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX)).addListener(listener);
    this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX).setSensitive(false);

    // Stack browser
    this.stackView.addListener(listener);

    // Preferences
    ((BooleanPreference) PreferenceManager.sourceWinGroup.getPreference(SourceWinPreferenceGroup.TOOLBAR)).addListener(new BooleanPreferenceListener()
    {
      public void preferenceChanged (String prefName, boolean newValue)
      {
        SourceWindow.this.setShowToolbar(newValue);
      }
    });

  }

  /**
   * Displays the preference window, or creates it if it is the first time this
   * method is called
   */
  private void launchPreferencesWindow ()
  {
    PreferenceWindow prefWin = null;
    try
      {
        prefWin = new PreferenceWindow(
                                       new LibGlade(
                                                    this.gladePath
                                                        + "/frysk_source_prefs.glade",
                                                    prefWin));
      }
    catch (GladeXMLException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    catch (FileNotFoundException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    catch (IOException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    prefWin.showAll();
  }

  private void showFindBox ()
  {
    this.glade.getWidget(SourceWindow.FIND_BOX).showAll();
    this.glade.getWidget(SourceWindow.FIND_TEXT).grabFocus();
  }

  private void hideFindBox ()
  {
    this.glade.getWidget(SourceWindow.FIND_BOX).hideAll();
  }

  private void gotoLine (int line)
  {
    this.view.scrollToLine(line);
  }

  private void doFindNext ()
  {
    boolean caseSensitive = ((CheckButton) this.glade.getWidget(SourceWindow.CASE_FIND)).getState();
    String text = ((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT)).getText();

    // Do nothing for if nothing to search for
    if (text.trim().equals(""))
      return;

    resetSearchBox();

    if (! this.view.findNext(text, caseSensitive))
      this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(
                                                                StateType.NORMAL,
                                                                Color.RED);
  }

  private void doFindPrev ()
  {
    boolean caseSensitive = ((CheckButton) this.glade.getWidget(SourceWindow.CASE_FIND)).getState();
    String text = ((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT)).getText();

    // Do nothing for if nothing to search for
    if (text.trim().equals(""))
      return;

    resetSearchBox();

    if (! this.view.findPrevious(text, caseSensitive))
      this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(
                                                                StateType.NORMAL,
                                                                Color.RED);
  }

  private void doHighlightAll ()
  {
    boolean caseSensitive = ((CheckButton) this.glade.getWidget(SourceWindow.CASE_FIND)).getState();
    String text = ((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT)).getText();

    // Do nothing for if nothing to search for
    if (text.trim().equals(""))
      return;

    resetSearchBox();

    if (! this.view.highlightAll(text, caseSensitive))
      this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(
                                                                StateType.NORMAL,
                                                                Color.RED);
  }

  private void switchToSourceMode ()
  {
    /*
     * If we're switching from Assembly or Mixed mode, we can just toggle the
     * state.
     */
    if (this.view instanceof SourceView)
      {
        ((SourceView) this.view).setMode(SourceBuffer.SOURCE_MODE);
      }
    /*
     * If we're switching from Source/Assembly mode, we need to re-create the
     * source view widget
     */
    else
      {
        ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).remove(((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).getChild());
        this.view = new SourceView(this.view.getScope(), this);

        ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).add((Widget) this.view);
        this.view.showAll();
      }
  }

  private void switchToAsmMode ()
  {
    /*
     * If we're switching from Source or Mixed more, we can just toggle the
     * state
     */
    if (this.view instanceof SourceView)
      {
        ((SourceView) this.view).setMode(SourceBuffer.ASM_MODE);
      }
    /*
     * If we're switching from Source/Assembly mode, we need to re-create the
     * source view widget
     */
    else
      {
        ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).remove(((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).getChild());
        this.view = new SourceView(this.view.getScope(), this,
                                   SourceBuffer.ASM_MODE);

        ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).add((Widget) this.view);
        this.view.showAll();
      }
  }

  private void switchToMixedMode ()
  {
    /*
     * If we're switching from Source or Assembly we can just toggle the state
     */
    if (this.view instanceof SourceView)
      {
        ((SourceView) this.view).setMode(SourceBuffer.MIXED_MODE);
      }
    /*
     * If we're switching from Source/Assembly mode, we need to re-create the
     * source view widget
     */
    else
      {
        ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).remove(((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).getChild());
        this.view = new SourceView(this.view.getScope(), this);

        ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).add((Widget) this.view);
        ((SourceView) this.view).setMode(SourceBuffer.MIXED_MODE);
        this.view.showAll();
      }
  }

  private void switchToSourceAsmMode ()
  {
    if (! (this.view instanceof MixedView))
      {
        // Replace the SourceView with a Mixedview to display
        // Source/Assembly
        // mode
        ((Container) this.view.getParent()).remove((Widget) this.view);
        this.view = new MixedView(this.view.getScope(), this);

        ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).addWithViewport((Widget) this.view);
        this.view.showAll();
      }
  }

  private void updateShownStackFrame (StackLevel selected)
  {
    DOMSource source = selected.getData();
    ((Label) this.glade.getWidget("sourceLabel")).setText("<b>"
                                                          + (source == null ? "Unknown File"
                                                                           : source.getFileName())
                                                          + "</b>");
    ((Label) this.glade.getWidget("sourceLabel")).setUseMarkup(true);
    this.view.load(selected);
    this.view.showAll();
  }

  /**
   * Tells the debugger to run the program
   */
  private void doRun ()
  {
    // Set status of toolbar buttons
    this.glade.getWidget("toolbarGotoBox").setSensitive(false);
    this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX).setSensitive(false);

    WindowManager.theManager.registerWindow.setIsRunning(true);
    WindowManager.theManager.memoryWindow.setIsRunning(true);

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

  private void doStop ()
  {
    // Set status of toolbar buttons
    this.glade.getWidget("toolbarGotoBox").setSensitive(true);
    this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX).setSensitive(true);

    WindowManager.theManager.registerWindow.setIsRunning(false);
    WindowManager.theManager.memoryWindow.setIsRunning(true);

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
  private void doStep ()
  {
    System.out.println("Step");
  }

  /**
   * Tells the debugger to execute next
   */
  private void doNext ()
  {
    System.out.println("Next");
  }

  /**
   * Tells the debugger to continue execution
   */
  private void doContinue ()
  {
    System.out.println("Continue");
  }

  /**
   * Tells the debugger to finish executing the current function
   */
  private void doFinish ()
  {
    System.out.println("Finish");
  }

  /**
   * Tells the debugger to terminate the program being debugged
   */
  private void doTerminate ()
  {
    System.out.println("Terminate");
  }

  /**
   * Tells the debugger to step an assembly instruction
   */
  private void doAsmStep ()
  {
    System.out.println("Asm Step");
  }

  /**
   * Tells the debugger to execute the next assembly instruction
   */
  private void doAsmNext ()
  {
    System.out.println("Asm Next");
  }

  /**
   * Tells the debugger to move to the previous stack frame
   */
  private void doStackUp ()
  {
    int selected = this.stackView.getSelection().getSelectedRows()[0].getIndices()[0];

    // Can't move above top stack
    if (selected == 0)
      return;

    this.stackView.getSelection().select(
                                         this.stackView.getModel().getIter(
                                                                           ""
                                                                               + (selected - 1)));
  }

  /**
   * Tells the debugger to move to the following stack frame
   */
  private void doStackDown ()
  {
    int selected = this.stackView.getSelection().getSelectedRows()[0].getIndices()[0];

    int max = 0;
    TreeIter iter = this.stackView.getModel().getIter("" + max);
    while (iter != null)
      iter = this.stackView.getModel().getIter("" + max++);

    // Can't move below bottom stack
    if (selected == max - 2)
      return;

    this.stackView.getSelection().select(
                                         this.stackView.getModel().getIter(
                                                                           ""
                                                                               + (selected + 1)));
  }

  /**
   * Tells the debugger to move to the newest stack frame
   */
  private void doStackBottom ()
  {
    int max = 0;
    TreeIter iter = this.stackView.getModel().getIter("" + max);
    while (iter != null)
      iter = this.stackView.getModel().getIter("" + max++);

    this.stackView.getSelection().select(
                                         this.stackView.getModel().getIter(
                                                                           ""
                                                                               + (max - 2)));
  }

  private void doJumpToFunction (String name)
  {
    this.view.scrollToFunction(name);
  }

  private void toggleRegisterWindow ()
  {
    if (this.toggleRegisterWindow.getActive())
      {
        if (! WindowManager.theManager.registerWindow.hasTaskSet())
          {
            WindowManager.theManager.registerWindow.setTask(this.myTask);
          }
        else
          WindowManager.theManager.registerWindow.showAll();
      }
    else
      WindowManager.theManager.registerWindow.hideAll();
  }

  private void toggleMemoryWindow ()
  {
    if (this.toggleMemoryWindow.getActive())
      {
        if (! WindowManager.theManager.memoryWindow.hasTaskSet())
          WindowManager.theManager.memoryWindow.setTask(this.myTask);
        else
          WindowManager.theManager.memoryWindow.showAll();
      }
  }

  private class SourceWindowListener
      implements ButtonListener, EntryListener, ComboBoxListener,
      StackViewListener
  {

    private SourceWindow target;

    public SourceWindowListener (SourceWindow target)
    {
      this.target = target;
    }

    public void buttonEvent (ButtonEvent event)
    {
      if (! event.isOfType(ButtonEvent.Type.CLICK))
        return;

      String buttonName = ((Button) event.getSource()).getName();
      if (buttonName.equals(SourceWindow.CLOSE_FIND))
        target.hideFindBox();
      else if (buttonName.equals(SourceWindow.NEXT_FIND))
        target.doFindNext();
      else if (buttonName.equals(SourceWindow.PREV_FIND))
        target.doFindPrev();
      else if (buttonName.equals(SourceWindow.HIGHLIGHT_FIND))
        target.doHighlightAll();
    }

    public void entryEvent (EntryEvent event)
    {
      // Search box in the find bar
      if (((Widget) event.getSource()).getName().equals("findText"))
        {
          if (event.isOfType(EntryEvent.Type.DELETE_TEXT))
            target.resetSearchBox();
          else if (event.isOfType(EntryEvent.Type.CHANGED))
            target.doFindNext();
        }
      // Magic goto box in the toolbar
      else
        {
          // user had to hit enter to do anything
          if (! event.isOfType(EntryEvent.Type.ACTIVATE))
            return;

          Entry source = (Entry) event.getSource();

          String text = source.getText();
          boolean isNum = true;
          int value = - 1;

          try
            {
              if (text.indexOf("line ") == 0)
                {
                  text = text.split("line ")[1];
                }
              value = Integer.parseInt(text);
            }
          // didn't work, we have to try to parse the text
          catch (NumberFormatException ex)
            {
              isNum = false;
              // Since we might have screwed around with this, reset it
              text = source.getText();
            }

          // goto line
          if (isNum)
            {
              target.gotoLine(value);
            }
          else
            {
              target.doJumpToFunction(text);
            }
        }
    }

    public void comboBoxEvent (ComboBoxEvent event)
    {
      String text = ((ComboBox) event.getSource()).getActiveText();

      // Switch to source mode
      if (text.equals("Source"))
        target.switchToSourceMode();
      // Switch to Assembly mode
      else if (text.equals("Assembly"))
        target.switchToAsmMode();
      // Switch to Mixed mode
      else if (text.equals("Mixed"))
        target.switchToMixedMode();
      /*
       * Switch to Source/Assembly mode - we only need to worry about this case
       * if we're switching from Source, Assembly, or Mixed view. If we were
       * previously in Source/Assembly view we don't need to do anything
       */
      else if (text.equals("Source/Assembly"))
        target.switchToSourceAsmMode();

    }

    public void currentStackChanged (StackLevel newLevel)
    {
      target.updateShownStackFrame(newLevel);
    }

  }
}
