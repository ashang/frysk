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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import lib.dw.Dwfl;
import lib.dw.DwflLine;
import lib.dw.NoDebugInfoException;

import org.gnu.gdk.Color;
import org.gnu.gdk.KeyValue;
import org.gnu.gdk.ModifierType;
import org.gnu.glade.GladeXMLException;
import org.gnu.glade.LibGlade;
import org.gnu.glib.CustomEvents;
import org.gnu.gtk.AccelGroup;
import org.gnu.gtk.AccelMap;
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
import org.gnu.gtk.StatusBar;
import org.gnu.gtk.ToggleAction;
import org.gnu.gtk.ToolBar;
import org.gnu.gtk.ToolItem;
import org.gnu.gtk.ToolTips;
import org.gnu.gtk.TreeIter;
import org.gnu.gtk.TreePath;
import org.gnu.gtk.Widget;
import org.gnu.gtk.Window;
import org.gnu.gtk.event.ActionEvent;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;

import frysk.dom.DOMFactory;
import frysk.dom.DOMFrysk;
import frysk.dom.DOMFunction;
import frysk.dom.DOMImage;
import frysk.dom.DOMSource;
import frysk.gui.Gui;
import frysk.gui.common.IconManager;
import frysk.gui.common.dialogs.WarnDialog;
import frysk.gui.common.prefs.BooleanPreference;
import frysk.gui.common.prefs.PreferenceManager;
import frysk.gui.common.prefs.PreferenceWindow;
import frysk.gui.common.prefs.BooleanPreference.BooleanPreferenceListener;
import frysk.gui.disassembler.DisassemblyWindowFactory;
import frysk.gui.disassembler.DisassemblyWindow;
import frysk.gui.memory.MemoryWindow;
import frysk.gui.memory.MemoryWindowFactory;
import frysk.gui.register.RegisterWindow;
import frysk.gui.register.RegisterWindowFactory;
import frysk.gui.srcwin.CurrentStackView.StackViewListener;
import frysk.gui.srcwin.prefs.SourceWinPreferenceGroup;
import frysk.lang.Variable;
import frysk.proc.Action;
import frysk.proc.MachineType;
import frysk.proc.Proc;
import frysk.proc.ProcBlockObserver;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;
import frysk.vtecli.ConsoleWindow;

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
  private org.gnu.gtk.Action close;

  private org.gnu.gtk.Action copy;

  private ToggleAction find;

  private org.gnu.gtk.Action prefsLaunch;

  private org.gnu.gtk.Action run;

  private org.gnu.gtk.Action stop;

  private org.gnu.gtk.Action step;

  private org.gnu.gtk.Action next;

  private org.gnu.gtk.Action cont;

  private org.gnu.gtk.Action finish;

  private org.gnu.gtk.Action terminate;

  private org.gnu.gtk.Action stepAsm;

  private org.gnu.gtk.Action nextAsm;

  private org.gnu.gtk.Action stackUp;

  private org.gnu.gtk.Action stackDown;

  private org.gnu.gtk.Action stackBottom;

  private ToggleAction toggleRegisterWindow;

  private ToggleAction toggleMemoryWindow;

  private ToggleAction toggleDisassemblyWindow;

  private ToggleAction toggleConsoleWindow;
  
  private ToggleAction toggleThreadDialog;
  
  private ToggleAction toggleStepDialog;
  
  private ThreadSelectionDialog threadDialog = null;
  
  private StepDialog stepDialog = null;

  private DOMFrysk dom;
  
  private Proc swProc;

  private CurrentStackView stackView;

  private VariableWatchView watchView;

  private ConsoleWindow conWin;

  private ProcBlockObserver pbo;
  
  private HashSet runningThreads;
  
  private HashMap dwflMap;
  
  private HashMap lineMap;

  //protected boolean runningState = false;
  
  //private boolean steppingState = false;
  
  protected boolean SW_active = false;
  
  private static int taskCount = 0;
  
  private static int taskStepCount = 0;
  
  private int numSteppingThreads = 0;
  
  /* The state that the SourceWindow is current in. Critical for determining
   * which operations can be performed at which time. */
  private int SW_state = 0;
  
  /* Possible states this SourceWindow can be in. */
  private static final int STOPPED = 0;
  private static final int RUNNING = 1;
  private static final int INSTRUCTION_STEP = 2;
  protected static final int STEP_IN = 3;
  protected static final int STEP_OVER = 4;
  private static final int STEP_OUT = 5;

  // Due to java-gnome bug #319415
  private ToolTips tips;
  
  private static Logger errorLog = Logger.getLogger(Gui.ERROR_LOG_ID);

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
  public SourceWindow (LibGlade glade, String gladePath, Proc proc, ProcBlockObserver pbo)
  {
    super(((Window) glade.getWidget(SOURCE_WINDOW)).getHandle());

    this.setIcon(IconManager.windowIcon);

    this.glade = glade;
    this.gladePath = gladePath;
    this.swProc = proc;
    this.pbo = pbo;
  }
  
  /**
   * Initializes the Glade file, the SourceWindow itself, adds listeners and
   * Assigns the Proc. Sets up the DOM information and the Stack information.
   * 
   * @param mw The MemoryWindow to be initialized.
   * @param proc The Proc to be examined by mw.
   */
  private void finishSourceWin (Proc proc)
  {
    
    StackFrame[] frames = generateProcStackTrace(null, null);

    this.listener = new SourceWindowListener(this);
    this.runningThreads = new HashSet();
    this.dwflMap = new HashMap();
    this.lineMap = new HashMap();
    this.watchView = new VariableWatchView(this);
    this.tips = new ToolTips();
    
    this.glade.getWidget(SourceWindow.SOURCE_WINDOW).hideAll();
    
    AccelGroup ag = new AccelGroup();
    ((Window) this.glade.getWidget(SourceWindow.SOURCE_WINDOW)).addAccelGroup(ag);
    
    ((ComboBox) this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX)).setActive(0);

    this.populateStackBrowser(frames);

    this.createActions(ag);
    this.createMenus();
    this.createToolBar();
    this.createSearchBar();
    
    this.attachEvents();

    ScrolledWindow sw = (ScrolledWindow) this.glade.getWidget("traceScrolledWindow");
    sw.add(this.watchView);

    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stopped");

    this.run.setSensitive(true);
    this.stop.setSensitive(false);
    
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

  public void removeVariableTrace (Variable var)
  {
    this.watchView.removeTrace(var);
  }
  

  /**
   * Populates the stack browser window
   * 
   * @param frames An array of StackFrames
   */
  public void populateStackBrowser (StackFrame[] frames)
  {
    stackView = new CurrentStackView(frames);

    if (this.view == null)
      {
        this.view = new SourceView(CurrentStackView.getCurrentFrame(), this);
        ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).add((Widget) this.view);
        ScrolledWindow sw = (ScrolledWindow) this.glade.getWidget("stackScrolledWindow");
        sw.add(stackView);
      }

    updateShownStackFrame(stackView.getFirstFrameSelection());

    stackView.expandAll();
    stackView.showAll();
    this.view.showAll();
  }
  
  public void updateThreads ()
  {
    executeTasks(this.threadDialog.getBlockTasks());
  }
  
  /**
   * A request for an instruction step on one or more tasks.
   * 
   * @param tasks   The list of tasks to step.
   */
  protected void step (LinkedList tasks)
  {

    if (tasks.size() == 0)
      return;
    
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping");
    
    desensitize();
    
    this.SW_state = INSTRUCTION_STEP;
    this.numSteppingThreads = tasks.size();
    
    Iterator i = tasks.iterator();
    while (i.hasNext())
      {
        Task t = (Task) i.next();
        this.pbo.requestUnblock(t);
      }
  }
  
  /**
   * Thread stepping has completed, clean up. 
   */
  protected void stepCompleted ()
  {
    //System.out.println("step completed");
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stopped");
    
    resensitize();
    
    this.SW_state = STOPPED;
  }
  
  /**
   * Called from SourceWindowFactory when all Tasks have notified that they are
   * blocked and new stack traces have been generated. This is called after
   * the StackView has been re-populated, allowing the SourceWindow to be 
   * sensitive again. 
   */
  protected void procReblocked ()
  {

    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stopped");
    
    resensitize();
    
    this.SW_state = STOPPED;
  }
  
  /***************************************
   * Getters and Setters
   ***************************************/

  public void setSwProc (Proc myProc)
  {
    this.swProc = myProc;
    this.setTitle(this.getTitle() + this.swProc.getCommand() + " - process "
                  + this.swProc.getPid());
  }

  public Proc getSwProc ()
  {
    return this.swProc;
  }

  public DOMFrysk getDOM ()
  {
    return this.dom;
  }
  
  public View getView ()
  {
    return this.view;
  }
  
  public int getState ()
  {
    return this.SW_state;
  }
  
  public int getNumSteppingThreads ()
  {
    return this.numSteppingThreads;
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
   * Creates the menus and assigns hotkeys
   */
  private void createActions (AccelGroup ag)
  {

    // Close action
    this.close = new org.gnu.gtk.Action("close", "Close", "Close Window",
                                        GtkStockItem.CLOSE.getString());
    this.close.setAccelGroup(ag);
    this.close.setAccelPath("<sourceWin>/File/Close");
    this.close.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.copy = new org.gnu.gtk.Action("copy", "Copy",
                                       "Copy Selected Text to the Clipboard",
                                       GtkStockItem.COPY.getString());
    this.copy.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.find.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.prefsLaunch = new org.gnu.gtk.Action(
                                              "prefs",
                                              "Frysk Preferences",
                                              "Edit Preferences",
                                              GtkStockItem.PREFERENCES.getString());
    this.prefsLaunch.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.launchPreferencesWindow();
      }
    });

    // Run program action
    this.run = new org.gnu.gtk.Action("run", "Run", "Run Program", "frysk-run");
    this.run.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.stop = new org.gnu.gtk.Action("stop", "Stop",
                                       "Stop Program execution", "frysk-stop");
    this.stop.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent arg0)
      {
        SourceWindow.this.doStop();
      }
    });
    this.stop.setSensitive(false);
    
    //  Thread-specific starting and stopping
    this.toggleThreadDialog = new ToggleAction("threads", "Start/Stop Threads",
                              "Start or Stop thread execution", "frysk-thread");
    this.toggleThreadDialog.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent arg0)
      {
        SourceWindow.this.toggleThreadDialog();
      }
    });
    
    this.toggleStepDialog = new ToggleAction("step", "Instruction Stepping",
                                               "Instruction stepping multiple threads", "frysk-thread");
    this.toggleStepDialog.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent arg0)
      {
        SourceWindow.this.toggleStepDialog();
      }
    });

    // Step action
    this.step = new org.gnu.gtk.Action("step", "Step", "Step", "frysk-step");
    this.step.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.step.setSensitive(true);

    // Next action
    this.next = new org.gnu.gtk.Action("next", "Next", "Next", "frysk-next");
    this.next.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.finish = new org.gnu.gtk.Action("finish", "Finish",
                                         "Finish Function Call", "frysk-finish");
    this.finish.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.cont = new org.gnu.gtk.Action("continue", "Continue",
                                       "Continue Execution", "frysk-continue");
    this.cont.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.terminate = new org.gnu.gtk.Action("terminate", "Terminate",
                                            "Kill Currently Executing Program",
                                            "");
    this.terminate.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.stepAsm = new org.gnu.gtk.Action("stepAsm",
                                          "Step Assembly Instruction",
                                          "Step Assembly Instruction",
                                          "frysk-stepAI");
    this.stepAsm.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.nextAsm = new org.gnu.gtk.Action("nextAsm",
                                          "Next Assembly Instruction",
                                          "Next Assembly Instruction",
                                          "frysk-nextAI");
    this.nextAsm.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.stackBottom = new org.gnu.gtk.Action("stackBottom",
                                              "To Bottom of Stack",
                                              "To Bottom of Stack",
                                              "frysk-bottom");
    this.stackBottom.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.stackDown = new org.gnu.gtk.Action("stackDown",
                                            "Down One Stack Frame",
                                            "Down One Stack Frame",
                                            "frysk-down");
    this.stackDown.addListener(new org.gnu.gtk.event.ActionListener()
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
    this.stackUp = new org.gnu.gtk.Action("stackUp", "Up One Stack Frame",
                                          "Up One Stack Frame", "frysk-up");
    this.stackUp.addListener(new org.gnu.gtk.event.ActionListener()
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

    toggleRegisterWindow = new ToggleAction("toggleRegWindow",
                                            "Register Window",
                                            "Toggle the Register Window", "");
    toggleRegisterWindow.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent arg0)
      {
        SourceWindow.this.toggleRegisterWindow();
      }
    });

    this.toggleMemoryWindow = new ToggleAction("toggleMemWindow",
                                               "Memory Window",
                                               "Toggle the Memory Window", "");
    this.toggleMemoryWindow.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent arg0)
      {
        SourceWindow.this.toggleMemoryWindow();
      }
    });

    this.toggleDisassemblyWindow = new ToggleAction(
                                                    "toggleDisWindow",
                                                    "Disassembly Window",
                                                    "Toggle the Disassembly Window",
                                                    "");
    this.toggleDisassemblyWindow.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent arg0)
      {
        SourceWindow.this.toggleDisassemblyWindow();
      }
    });

    this.toggleConsoleWindow = new ToggleAction("toggleConWindow",
                                                "Console Window",
                                                "Toggle the Console Window", "");
    this.toggleConsoleWindow.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent arg0)
      {
        SourceWindow.this.toggleConsoleWindow();
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

    // mi = (MenuItem) this.toggleConsoleWindow.createMenuItem();
    // tmp.append(mi);

    mi = (MenuItem) this.toggleRegisterWindow.createMenuItem();
    tmp.append(mi);

    mi = (MenuItem) this.toggleMemoryWindow.createMenuItem();
    tmp.append(mi);

    mi = (MenuItem) this.toggleDisassemblyWindow.createMenuItem();
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
//    mi = (MenuItem) this.toggleMainThread.createMenuItem();
//    tmp.append(mi);
//    mi = (MenuItem) this.toggleThreadDialog.createMenuItem();
//    tmp.append(mi);
    mi = (MenuItem) this.toggleStepDialog.createMenuItem();
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
  
  private void desensitize ()
  {
    this.glade.getWidget("toolbarGotoBox").setSensitive(false);
    this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX).setSensitive(false);
    
    //  Set status of actions
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
  
  private void resensitize ()
  {
    //  Set status of toolbar buttons
    this.glade.getWidget("toolbarGotoBox").setSensitive(true);
    this.glade.getWidget(SourceWindow.SOURCE_WINDOW).setSensitive(true);
    
    //  Set status of actions
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
                "Find Next Match", "Locate the next occurrence in the file"); //$NON-NLS-1$ //$NON-NLS-2$
    tips.setTip(
                this.glade.getWidget(SourceWindow.PREV_FIND),
                "Find Previous Match", "Locate the previous occurrence in the file"); //$NON-NLS-1$ //$NON-NLS-2$
    tips.setTip(this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND),
                "Highlight All Matches", "Locate all occurrences in the file"); //$NON-NLS-1$ //$NON-NLS-2$
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

  private void updateShownStackFrame (StackFrame selected)
  {
    if (selected == null)
      return;
    DOMSource source = selected.getData();
    ((Label) this.glade.getWidget("sourceLabel")).setText("<b>"
                                                          + (source == null ? "Unknown File"
                                                                           : source.getFileName())
                                                          + "</b>");
    ((Label) this.glade.getWidget("sourceLabel")).setUseMarkup(true);
    this.view.load(selected);

    if (source != null && selected.getDOMFunction() != null)
      {
        int line = (selected.getDOMFunction().getStartingLine());
        String declaration = source.getLine(line).getText();
        String ret = source.getLine(line - 1).getText();
        if (ret != "")
          declaration = ret.split("\n")[0] + " " + declaration;

        this.view.scrollToFunction(declaration);
      }

    this.view.showAll();
  }

  /**
   * Tells the debugger to run the program
   */
  private void doRun ()
  {

    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Running");
    
    desensitize();

    this.SW_state = RUNNING;

    unblockProc(this.swProc);
  }

  private void unblockProc (Proc proc)
  {
    Iterator i = this.swProc.getTasks().iterator();
    while (i.hasNext())
      {
        Task t = (Task) i.next();
        if (!this.runningThreads.contains(t))
          {
            this.runningThreads.add(t);
            this.pbo.requestDeleteInstructionObserver(t);
          }
      }
  }

  private void doStop ()
  {
    // Set status of toolbar buttons
    this.glade.getWidget("toolbarGotoBox").setSensitive(false);
    this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX).setSensitive(false);

    this.glade.getWidget(SourceWindow.SOURCE_WINDOW).setSensitive(false);

    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stopped");

    //this.pbo.requestAddObservers(this.myProc.getMainTask());
    
    if (this.threadDialog == null)
      {
        this.runningThreads.clear();
        this.pbo.requestAdd();
        return;
      }
    
    LinkedList l = this.threadDialog.getBlockTasks();
    if (l.size() == 0)
        this.pbo.requestAdd();
    else
      {
        LinkedList tasks = swProc.getTasks();
        Iterator i = this.runningThreads.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            if (tasks.contains(t))
              tasks.remove(t);
          }
        this.pbo.blockTask(tasks);
      }
    this.runningThreads.clear();
  }
  
  private void toggleThreadDialog ()
  {
    if (this.threadDialog == null)
      {
        this.threadDialog = new ThreadSelectionDialog(glade, this);
        this.threadDialog.showAll();
      }
    else
      this.threadDialog.showAll();
  }
  
  private void toggleStepDialog ()
  {
    if (this.stepDialog == null)
      {
        this.stepDialog = new StepDialog(glade, this);
        this.stepDialog.showAll();
      }
    else
      this.stepDialog.showAll();
  }
 

  /**
   * Tells the debugger to step the program
   */
  private void doStep ()
  {
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping");
    
    desensitize();
    
    this.SW_state = STEP_IN;
    this.numSteppingThreads = swProc.getTasks().size();
    
    Iterator i = this.swProc.getTasks().iterator();
    while (i.hasNext())
      {
        Task t = (Task) i.next();
        if (this.dwflMap.get(t) == null)
          {
            Dwfl d = new Dwfl(t.getTid());
            DwflLine line = null;
            try
              {
                line = d.getSourceLine(t.getIsa().pc(t));
              }
            catch (TaskException te)
              {
                continue;
              }

            this.dwflMap.put(t, d);
            this.lineMap.put(t, new Integer(line.getLineNum()));
          }
        this.pbo.requestUnblock(t);
      }
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
    TreePath path = null;
    try
      {
        path = this.stackView.getSelection().getSelectedRows()[0];
      }
    catch (ArrayIndexOutOfBoundsException ae)
      {
        return;
      }

    int selected;

    if (path.getDepth() == 1)
      {
        selected = path.getIndices()[0];

        // Can't move above top stack
        if (selected == 0)
          {
            this.stackUp.setSensitive(false);
            return;
          }

        this.stackView.getSelection().select(
                                             this.stackView.getModel().getIter(
                                                                               ""
                                                                                   + (selected - 1)));

        if (this.stackView.getModel().getIter("" + (selected - 1)) == null)
          this.stackUp.setSensitive(false);
      }

    else
      {

        selected = path.getIndices()[1];

        // Can't move above top stack
        if (selected == 0)
          return;

        this.stackView.getSelection().select(
                                             this.stackView.getModel().getIter(
                                                                               ""
                                                                                   + path.getIndices()[0]
                                                                                   + ":"
                                                                                   + (selected - 1)));

        if (this.stackView.getModel().getIter(
                                              "" + path.getIndices()[0] + ":"
                                                  + (selected - 1)) == null)
          this.stackUp.setSensitive(false);
      }

    this.stackDown.setSensitive(true);
  }

  /**
   * Tells the debugger to move to the following stack frame
   */
  private void doStackDown ()
  {
    TreePath path = null;
    try
      {
        path = this.stackView.getSelection().getSelectedRows()[0];
      }
    catch (ArrayIndexOutOfBoundsException ae)
      {
        return;
      }

    int selected;

    if (path.getDepth() == 1)
      {
        selected = path.getIndices()[0];

        try
          {
            this.stackView.getSelection().select(
                                                 this.stackView.getModel().getIter(
                                                                                   ""
                                                                                       + (selected + 1)));

            if (this.stackView.getModel().getIter("" + (selected + 1)) == null)
              this.stackDown.setSensitive(false);
          }
        catch (NullPointerException npe)
          {
            this.stackDown.setSensitive(false);
            return;
          }
      }
    else
      {
        selected = path.getIndices()[1];

        try
          {
            this.stackView.getSelection().select(
                                                 this.stackView.getModel().getIter(
                                                                                   ""
                                                                                       + path.getIndices()[0]
                                                                                       + ":"
                                                                                       + (selected + 1)));

            if (this.stackView.getModel().getIter(
                                                  "" + path.getIndices()[0]
                                                      + ":" + (selected + 1)) == null)
              this.stackDown.setSensitive(false);
          }
        catch (NullPointerException npe)
          {
            this.stackDown.setSensitive(false);
            return;
          }
      }

    this.stackUp.setSensitive(true);
  }

  /**
   * Tells the debugger to move to the newest stack frame
   */
  private void doStackBottom ()
  {
    TreePath path = null;
    try
      {
        path = this.stackView.getSelection().getSelectedRows()[0];
      }
    catch (ArrayIndexOutOfBoundsException ae)
      {
        return;
      }

    if (path.getDepth() != 1)
      path.up();

    TreeIter iter = this.stackView.getModel().getIter(path);
    this.stackView.getSelection().select(iter.getFirstChild());
    this.stackUp.setSensitive(false);
  }

  private void doJumpToFunction (String name)
  {
    this.view.scrollToFunction(name);
  }

  private void toggleRegisterWindow ()
  {
    RegisterWindow regWin = RegisterWindowFactory.regWin;
    if (regWin == null)
      {
        RegisterWindowFactory.createRegisterWindow(swProc);
        RegisterWindowFactory.setRegWin(swProc);
      }
    else
      {
        RegisterWindowFactory.regWin.showAll();
      }
  }

  private void toggleMemoryWindow ()
  {
    if (MachineType.getMachineType() == MachineType.X8664
        || MachineType.getMachineType() == MachineType.PPC64)
      {
        WarnDialog dialog = new WarnDialog(
                                           " The Memory Window is yet not supported\n"
                                               + " on 64-bit architectures! ");
        dialog.showAll();
        dialog.run();
        return;
      }

    MemoryWindow memWin = MemoryWindowFactory.memWin;
    if (memWin == null)
      {
        MemoryWindowFactory.createMemoryWindow(swProc);
        MemoryWindowFactory.setMemWin(swProc);
      }
    else
      {
        MemoryWindowFactory.memWin.showAll();
      }
  }

  private void toggleDisassemblyWindow ()
  {
    if (MachineType.getMachineType() == MachineType.X8664
        || MachineType.getMachineType() == MachineType.PPC64)
      {
        WarnDialog dialog = new WarnDialog(
                                           " The Disassembly Window is yet not supported\n"
                                               + " on 64-bit architectures! ");
        dialog.showAll();
        dialog.run();
        return;
      }

    DisassemblyWindow disWin = DisassemblyWindowFactory.disWin;
    if (disWin == null)
      {
        DisassemblyWindowFactory.createDisassemblyWindow(swProc);
        DisassemblyWindowFactory.setDisWin(swProc);
      }
    else
      {
        DisassemblyWindowFactory.disWin.showAll();
      }
  }

  private void toggleConsoleWindow ()
  {
    if (this.conWin == null)
      this.conWin = new ConsoleWindow();
    else
      this.conWin.showAll();
  }
  
  private synchronized void executeTasks (LinkedList threads)
  {

//    System.out.println("In executeThreads with thread size " + threads.size()
//                       + " and runningthreads size "
//                       + this.runningThreads.size());

    if (threads.size() == 0 && this.runningThreads.size() == 0)
      return;   /* runningState should already be false */
    
    else if (threads.size() == 0 && this.runningThreads.size() != 0)
      {
        LinkedList l = new LinkedList();
        Iterator i = this.runningThreads.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            l.add(t);
            i.remove();
            //System.out.println("Blocking " + t);
          }
        this.pbo.blockTask(l);
        this.SW_state = STOPPED;
        return;
      }

    if (this.runningThreads.size() == 0)
      {
        Iterator i = threads.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            //System.out.println("(0) Running " + t);
            this.runningThreads.add(t);

            this.pbo.requestDeleteInstructionObserver(t);
          }
        this.SW_state = RUNNING;
        return;
      }
    else
      {
       
        this.SW_state = RUNNING;
        HashSet temp = new HashSet();
        // this.runningThreads.clear();
        Iterator i = threads.iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            //System.out.println("Iterating running thread" + t);
            /* If this thread has not already been unblocked, do it */
            if (!this.runningThreads.remove(t))
              {
                //System.out.println("unBlocking " + t);
                this.pbo.requestDeleteInstructionObserver(t);
              }
            else
              //System.out.println("Already Running");
            /* Put all threads back into a master list */
            temp.add(t);
          }

        /* Now catch the threads which have a block request */
        if (this.runningThreads.size() != 0)
          {
            //System.out.println("temp size not zero");
            LinkedList l = new LinkedList();
            i = this.runningThreads.iterator();
            while (i.hasNext())
              {
                Task t = (Task) i.next();
                l.add(t);
                //System.out.println("Blocking from runningThreads " + t);
              }
            this.pbo.blockTask(l);
          }

        this.runningThreads = temp;
        //System.out.println("rt temp" + this.runningThreads.size() + " "
 //                         + temp.size());
      }
  }
  
  private void stepIn (Task task)
  {
    //System.out.println("stepin " + task);
    DwflLine line = null;
    try
      {
        line = ((Dwfl) this.dwflMap.get(task)).getSourceLine(task.getIsa().pc(task));
      }
    catch (TaskException te)
      {
        return;
      }
    catch (NullPointerException npe)
      {
        return;
      }

    if (line == null)
      return;

    int lineNum = line.getLineNum();
    int prev = ((Integer) this.lineMap.get(task)).intValue();

    if (lineNum != prev)
      {
        this.lineMap.put(task, new Integer(lineNum));
        --taskStepCount;
      }
    else
      {
        this.pbo.requestUnblock(task);
      }
  }
  
  private void stepOver (Task task)
  {
    
  }
  
  private void stepOut (Task task)
  {
    
  }
  
  private synchronized void handleTask (Task task)
  {

    if (SW_active == false)
      {
        --taskCount;
        if (taskCount == 0)
          {
            SW_active = true;
            finishSourceWin(task.getProc());
          }
      }
    else
      {
        // System.out.println("SW false " + taskCount);
        --taskCount;
        if (taskCount == 0)
          {
            StackFrame[] frames = generateProcStackTrace(null, null);
            populateStackBrowser(frames);
            procReblocked();
          }
      }
  }

  private StackFrame[] generateProcStackTrace (StackFrame[] frames, Task[] tasks)
  {
    
    int size = this.swProc.getTasks().size();
    if (frames == null || tasks == null)
      {
        if (tasks == null)
          {
            tasks = new Task[size];
            Iterator iter = this.swProc.getTasks().iterator();
            for (int k = 0; k < size; k++)
              tasks[k] = (Task) iter.next();
          }

        frames = new StackFrame[size];
      }

    for (int j = 0; j < size; j++)
      {
        DwflLine line;
        DOMFunction f = null;

        /** Create the stack frame * */

        StackFrame curr = null;
        try
          {
            frames[j] = StackFactory.createStackFrame(tasks[j]);
            curr = frames[j];
          }
        catch (Exception e)
          {
            System.out.println(e.getMessage());
          }

        /** Stack frame created * */

        while (curr != null) /*
                               * Iterate and initialize information for all
                               * frames, not just the top one
                               */
          {

            if (this.dom == null && curr.getDwflLine() != null)
              {
                try
                  {
                    this.dom = DOMFactory.createDOM(curr, this.swProc);
                  }

                // If we don't have a dom, tell the task to continue
                catch (NoDebugInfoException e)
                  {
                  }
                catch (IOException e)
                  {
                    unblockProc(this.swProc);
                    WarnDialog dialog = new WarnDialog("File not found",
                                                       "Error loading source code: "
                                                           + e.getMessage());
                    dialog.showAll();
                    dialog.run();
                    return null;
                  }
              }

            line = curr.getDwflLine();

            if (line != null)
              {
                // System.out.println("Line not null");
                String filename = line.getSourceFile();
                // System.out.println("got filename");
                filename = filename.substring(filename.lastIndexOf("/") + 1);

                try
                  {
                    f = getFunctionXXX(
                                       this.dom.getImage(tasks[j].getProc().getMainTask().getName()),
                                       filename, line.getLineNum());
                  }
                catch (NullPointerException npe)
                  {
                    f = null;
                  }
              }

            curr.setDOMFunction(f);
            curr = curr.getOuter();
          }
      }
    return frames;
  }

  /**
   * Returns a DOMFunction matching the incoming function information from the
   * DOMImage.
   * 
   * @param image The DOMImage containing the source information.
   * @param filename The name of the source file.
   * @param linenum The line number of the function.
   * @return The found DOMFunction.
   */
  private static DOMFunction getFunctionXXX (DOMImage image, String filename,
                                             int linenum)
  {
    
    Iterator functions = image.getFunctions();

    // System.out.println("Looking for " + filename + ": " + linenum);

    DOMFunction found = null;

    while (functions.hasNext())
      {
        DOMFunction function = (DOMFunction) functions.next();
        if (function.getSource().getFileName().equals(filename)
            && function.getStartingLine() <= linenum)
          {
        
            if (found == null
                || function.getStartingLine() > found.getStartingLine())
              found = function;
          }
      }

    return found;
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

    public void currentStackChanged (StackFrame newFrame)
    {
      // TreePath path = stackView.getSelection().getSelectedRows()[0];
      // int selected = path.getIndices()[0];

      // if (stackView.getModel().getIter("" + path.getIndices()[0] +
      // ":" + (selected + 1)) != null)
      stackDown.setSensitive(true);

      // else if (stackView.getModel().getIter("" + path.getIndices()[0] +
      // ":" + (selected - 1)) != null)
      stackUp.setSensitive(true);

      target.updateShownStackFrame(newFrame);
    }

  }
  
  /**
   * A wrapper for TaskObserver.Attached which initializes the MemoryWindow 
   * upon call, and blocks the task it is to examine.
   */
  protected class SourceWinBlocker
      extends ProcBlockObserver
  {
    Task myTask;

    public SourceWinBlocker (Proc theProc)
    {
      super(theProc);
      pbo = this;
    }

    public Action updateAttached (Task task)
    {
      myTask = task;
      return Action.BLOCK;
    }
    
    public void existingTask (Task task)
    {
      //System.out.println("existing task");
       myTask = task;

       /* The source window has been properly initialized and there is a step
        * request in progress. */
      if (SW_active && (SW_state >= INSTRUCTION_STEP 
          && SW_state <= STEP_OUT))
        {
          
          if (taskStepCount == 0)
            {
              //System.out.println("resetting taskstepcount");    
              taskStepCount = numSteppingThreads;
            }
          
          switch (SW_state)
          {
            case INSTRUCTION_STEP:  --taskStepCount; break;
            case STEP_IN: stepIn(task); break;
            case STEP_OVER: stepOver(task); break;
            case STEP_OUT: stepOut(task); break;
          }
          //System.out.println("taskstepcount " + taskStepCount);
          if (taskStepCount == 0)
            {
              CustomEvents.addEvent(new Runnable()
              {
                public void run ()
                {
                  StackFrame[] frames = generateProcStackTrace(null, null);

                  populateStackBrowser(frames);
                  stepCompleted();
                }
              });
            }

          return;
        }
      
      if (taskCount == 0)
        taskCount = swProc.getTasks().size();

      CustomEvents.addEvent(new Runnable()
      {
        public void run ()
        {
          handleTask(myTask);
        }

      });
    }

    public void addFailed (Object observable, Throwable w)
    {
      errorLog.log(Level.WARNING, "addFailed (Object observable, Throwable w)",
                   w);
      throw new RuntimeException(w);
    }

    public void deletedFrom (Object observable)
    {
      // TODO Auto-generated method stub
    }
  }
  
}
