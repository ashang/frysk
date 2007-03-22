// This file is part of the program FRYSK.
//
// Copyright 2005, 2007 Red Hat Inc.
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

import frysk.rt.Line;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
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
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;

import frysk.cli.hpd.SymTab;
import frysk.dom.DOMFactory;
import frysk.dom.DOMFrysk;
import frysk.dom.DOMSource;
import frysk.gui.common.IconManager;
import frysk.gui.dialogs.WarnDialog;
import frysk.gui.disassembler.DisassemblyWindow;
import frysk.gui.disassembler.DisassemblyWindowFactory;
import frysk.gui.memory.MemoryWindow;
import frysk.gui.memory.MemoryWindowFactory;
import frysk.gui.prefs.BooleanPreference;
import frysk.gui.prefs.PreferenceManager;
import frysk.gui.prefs.PreferenceWindow;
import frysk.gui.prefs.BooleanPreference.BooleanPreferenceListener;
import frysk.gui.register.RegisterWindow;
import frysk.gui.register.RegisterWindowFactory;
import frysk.gui.srcwin.CurrentStackView.StackViewListener;
import frysk.gui.srcwin.prefs.SourceWinPreferenceGroup;
import frysk.proc.Isa;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rt.RunState;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;
import frysk.value.Variable;
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

  private org.gnu.gtk.Action stackTop;
  
  private org.gnu.gtk.Action stepInDialog;
  
  private org.gnu.gtk.Action stepOverDialog;
  
  private org.gnu.gtk.Action stepOutDialog;
  
  private org.gnu.gtk.Action stepInstructionDialog;
  
  private org.gnu.gtk.Action stepInstructionNextDialog;

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

  protected boolean SW_active = false;

  private StackFrame currentFrame;

  private Task currentTask;

  private StackFrame[] frames;

  private RunState runState;
  
  private SymTab symTab;

  // Due to java-gnome bug #319415
  private ToolTips tips;

  // private static Logger errorLog = Logger.getLogger(Gui.ERROR_LOG_ID);

  // Private inner class to take care of the event handling
  private SourceWindowListener listener;

  private SourceWindowFactory.AttachedObserver attachedObserver;

  private LockObserver lock;

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
  public SourceWindow (LibGlade glade, String gladePath, Proc proc)
  {
    super(((Window) glade.getWidget(SOURCE_WINDOW)).getHandle());

    this.setIcon(IconManager.windowIcon);

    this.glade = glade;
    this.gladePath = gladePath;
    this.swProc = proc;
    this.runState = new RunState();
    this.lock = new LockObserver();
    this.runState.addObserver(lock);
    this.runState.setProc(proc);
  }
  
  public SourceWindow (LibGlade glade, String gladePath, StackFrame trace)
  {
    super(((Window) glade.getWidget(SOURCE_WINDOW)).getHandle());

    this.setIcon(IconManager.windowIcon);

    this.glade = glade;
    this.gladePath = gladePath;
    this.swProc = trace.getTask().getProc();
    this.runState = new RunState();
    this.runState.setRunning();
    
    try
      {
        this.dom = DOMFactory.createDOM(trace, this.swProc);
      }

    catch (NoDebugInfoException e)
      {
      }
    catch (IOException e)
      {
      }
    
    finishSourceWin();
    
    StackFrame[] newTrace = new StackFrame[1];
    newTrace[0] = trace;
    populateStackBrowser(newTrace);
    
    desensitize();
    this.stop.setSensitive(false);
  }

  public SourceWindow (LibGlade glade, String gladePath, Proc proc,
                       SourceWindowFactory.AttachedObserver ao)
  {
    this(glade, gladePath, proc);
    this.attachedObserver = ao;

    this.addListener(new LifeCycleListener()
    {
      public void lifeCycleEvent (LifeCycleEvent event)
      {
      }

      public boolean lifeCycleQuery (LifeCycleEvent event)
      {
        if (event.isOfType(LifeCycleEvent.Type.DESTROY)
            || event.isOfType(LifeCycleEvent.Type.DELETE))
          {
            SourceWindow.this.hideAll();
          }
        return true;
      }
    });

  }

  /**
   * Initializes the rest of the members of the SourceWindow not handled by the
   * constructor. Most of these depend on the Tasks of the process being
   * blocked.
   */
  private void finishSourceWin ()
  { 
    StackFrame[] frames = null;
    if (this.runState.getState() == RunState.STOPPED)
      frames = generateProcStackTrace(null, null);
      
    
    this.listener = new SourceWindowListener(this);
    this.watchView = new VariableWatchView();
    this.tips = new ToolTips();

    this.glade.getWidget(SourceWindow.SOURCE_WINDOW).hideAll();

    AccelGroup ag = new AccelGroup();
    ((Window) this.glade.getWidget(SourceWindow.SOURCE_WINDOW)).addAccelGroup(ag);

    ((ComboBox) this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX)).setActive(0);

    if (this.runState.getState() == RunState.STOPPED)
      this.populateStackBrowser(frames);

    if (this.attachedObserver != null)
      {
        Iterator i = this.swProc.getTasks().iterator();
        while (i.hasNext())
          {
            Task t = (Task) i.next();
            t.requestDeleteAttachedObserver(this.attachedObserver);
          }
      }

    this.createActions(ag);
    this.createMenus();
    this.createToolBar();
    this.createSearchBar();

    this.attachEvents();

    ScrolledWindow sw = (ScrolledWindow) this.glade.getWidget("traceScrolledWindow");
    sw.add(this.watchView);

    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stopped");

    this.setTitle(this.getTitle() + this.swProc.getCommand() + " - process "
                  + this.swProc.getPid());

    this.cont.setSensitive(true);
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
   * Populates the stack browser window
   * 
   * @param frames An array of StackFrames
   */
  public void populateStackBrowser (StackFrame[] frames)
  {
    this.frames = frames;

    /* Initialization */
    if (this.view == null)
      {
        this.stackView = new CurrentStackView(frames);
        StackFrame temp = null;

        temp = CurrentStackView.getCurrentFrame();

        StackFrame curr = temp;
        this.currentFrame = temp;

	while (curr != null && curr.getLines().length == 0)
	    curr = curr.getOuter();
    
        if (curr != null)
          {
            this.currentFrame = curr;
            this.currentTask = curr.getTask();
            this.view = new SourceView(curr, this);
            
            SourceBuffer b = (SourceBuffer) ((SourceView) this.view).getBuffer();

            for (int j = 0; j < frames.length; j++)
              {
                b.highlightLine(frames[j], true);
              }
          }
        else
          {
            this.view = new SourceView(temp, this);
            this.currentTask = temp.getTask();
          }

        ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW)).add((Widget) this.view);
        ScrolledWindow sw = (ScrolledWindow) this.glade.getWidget("stackScrolledWindow");
        sw.add(stackView);
        this.watchView.setView((SourceView) this.view);
        stackView.expandAll();
        this.stackView.selectRow(this.currentFrame);
        
        if (this.currentFrame.getLines().length != 0)
          {
            this.view.scrollToLine(this.currentFrame.getLines()[0].getLine());
          }
        else
          {
            if (this.runState.getState() == RunState.STOPPED)
              {
                SourceBuffer b = (SourceBuffer) ((SourceView) this.view).getBuffer();
                b.disassembleFrame(this.currentFrame);
              }
          }
        
        stackView.showAll();
        this.view.showAll();
        return;
      }

    SourceView sv = (SourceView) this.view;
    SourceBuffer sb = (SourceBuffer) sv.getBuffer();

    StackFrame curr = null;
    StackFrame taskMatch = null;

    String currentMethodName = this.currentFrame.getSymbol ().getDemangledName ();

    this.stackView.resetView(frames);
    this.stackView.expandAll();
    StackFrame newFrame = null;
    
    /*
     * Try to find the new StackFrame representing the same frame from before
     * the reset
     */
    for (int j = 0; j < frames.length; j++)
      {
        curr = frames[j];
        if (curr.getTask().getTid() == this.currentTask.getTid())
          {
            this.currentTask = curr.getTask();
            taskMatch = curr;
          }

        sb.highlightLine(curr, true);
        
        if (newFrame == null)
          {
            while (curr != null)
              {
		if (currentMethodName.equals(curr.getSymbol ().getDemangledName ()))
                  {
                    newFrame = curr;
                    break;
                  }
                curr = curr.getOuter();
              }
          }
      }
    
    if (newFrame == null)
      {
        if (taskMatch != null)
          {
            newFrame = taskMatch;
          }
        else
          {
            newFrame = this.stackView.getFirstFrameSelection();
          }
      }

    this.stackView.selectRow(newFrame);
    updateShownStackFrame(newFrame);
    
    /* Update the variable watch as well */
    this.watchView.refreshList();
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

  public void updateThreads ()
  {
    executeTasks(this.threadDialog.getBlockTasks());
  }

  /**
   * Called from SourceWindowFactory when all Tasks have notified that they are
   * blocked and new stack traces have been generated. This is called after the
   * StackView has been re-populated, allowing the SourceWindow to be sensitive
   * again.
   */
  protected void procReblocked ()
  {
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stopped");

    this.runState.runCompleted();
    this.runState.stepCompleted();
    
    if (this.currentFrame.getLines().length== 0)
      {
        ((SourceBuffer)((SourceView) this.view).getBuffer())
                                         .disassembleFrame(this.currentFrame);
      }
    
    resensitize();
  }

  /*****************************************************************************
   * Getters and Setters
   ****************************************************************************/

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

  public RunState getRunState ()
  {
    return this.runState;
  }

  public LockObserver getLockObserver ()
  {
    return this.lock;
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
        // SourceWindow.this.glade.getWidget(SOURCE_WINDOW).destroy();
        SourceWindow.this.glade.getWidget(SOURCE_WINDOW).hide();
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

    // Thread-specific starting and stopping
    this.toggleThreadDialog = new ToggleAction(
                                               "threads",
                                               "Start/Stop Threads",
                                               "Start or Stop thread execution",
                                               "frysk-thread");
    this.toggleThreadDialog.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent arg0)
      {
        SourceWindow.this.toggleThreadDialog();
      }
    });

    this.toggleStepDialog = new ToggleAction(
                                             "step",
                                             "Instruction Stepping",
                                             "Instruction stepping multiple threads",
                                             "frysk-thread");
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
    this.stepAsm.setSensitive(true);

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

    // top of stack action
    this.stackTop = new org.gnu.gtk.Action("stackTop", "To top of Stack",
                                           "To top of Stack", "frysk-top");
    this.stackTop.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.doStackTop();
      }
    });
    this.stackTop.setAccelGroup(ag);
    this.stackTop.setAccelPath("<sourceWin>/Stack/Bottom");
    AccelMap.changeEntry("<sourceWin>/Stack/Bottom", KeyValue.Down,
                         ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK),
                         true);
    this.stackTop.connectAccelerator();

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
    
    // Thread specific line stepping
    this.stepInDialog = new org.gnu.gtk.Action("StepIn", "Step into functions...",
                                          "Step One Line", "frysk-step");
    this.stepInDialog.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.handleDialog(0);
      }
    });
    
    // Thread specific stepping over
    this.stepOverDialog = new org.gnu.gtk.Action("StepOut", "Step over functions...",
                                          "Step Over Function", "frysk-next");
    this.stepOverDialog.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.handleDialog(1);
      }
    });
    
    // Thread specific stepping out
    this.stepOutDialog = new org.gnu.gtk.Action("StepOut", "Step out of functions...",
                                          "Step out of frame", "frysk-finish");
    this.stepOutDialog.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.handleDialog(2);
      }
    });
    
    // Thread specific instruction stepping
    this.stepInstructionDialog = new org.gnu.gtk.Action("StepInstruction", "Step single instructions...",
                                          "Step single instruction", "frysk-stepAI");
    this.stepInstructionDialog.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.handleDialog(3);
      }
    });
    
    // Thread specific instruction next stepping
    this.stepInstructionNextDialog = new org.gnu.gtk.Action("StepInstructionNext", "Step next instructions...",
                                          "Step next instruction", "frysk-nextAI");
    this.stepInstructionNextDialog.addListener(new org.gnu.gtk.event.ActionListener()
    {
      public void actionEvent (ActionEvent action)
      {
        SourceWindow.this.handleDialog(4);
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
    mi = (MenuItem) this.cont.createMenuItem();
    tmp.append(mi);
    mi = (MenuItem) this.stop.createMenuItem();
    tmp.append(mi);
    // mi = (MenuItem) this.toggleThreadDialog.createMenuItem();
    // tmp.append(mi);
//    mi = (MenuItem) this.toggleStepDialog.createMenuItem();
//    tmp.append(mi);
    mi = (MenuItem) this.step.createMenuItem();
    tmp.append(mi);
    mi = (MenuItem) this.next.createMenuItem();
    tmp.append(mi);
    mi = (MenuItem) this.finish.createMenuItem();
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
    mi = (MenuItem) this.stackTop.createMenuItem();
    tmp.append(mi);

    menu.setSubmenu(tmp);
    ((MenuBar) this.glade.getWidget("menubar")).append(menu);
    
    menu = new MenuItem("Threads", false);
    tmp = new Menu();
    
    mi = (MenuItem) this.stepInDialog.createMenuItem();
    tmp.append(mi);
//    mi = (MenuItem) this.stepOverDialog.createMenuItem();
//    tmp.append(mi);
//    mi = (MenuItem) this.stepOutDialog.createMenuItem();
//    tmp.append(mi);
    mi = (MenuItem) this.stepInstructionDialog.createMenuItem();
    tmp.append(mi);
//    mi = (MenuItem) this.stepInstructionNextDialog.createMenuItem();
//    tmp.append(mi);
    
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
    item = (ToolItem) this.cont.createToolItem();
    item.setToolTip(this.tips, "Continue Execution", "");
    toolbar.insert(item, 1);
    item = (ToolItem) this.stop.createToolItem();
    item.setToolTip(this.tips, "Stops execution", "");
    toolbar.insert(item, 2);
    item = (ToolItem) this.step.createToolItem();
    item.setToolTip(this.tips, "Step", "");
    toolbar.insert(item, 3);
    item = (ToolItem) this.next.createToolItem();
    item.setToolTip(this.tips, "Next", "");
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
    item = (ToolItem) this.stackTop.createToolItem();
    item.setToolTip(this.tips, "To Bottom of Stack", "");
    toolbar.insert(item, 12);

    toolbar.showAll();
    toolbar.setToolTips(true);
  }

  private void desensitize ()
  {
    this.glade.getWidget("toolbarGotoBox").setSensitive(false);
    this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX).setSensitive(false);
    
    if (this.stepDialog != null)
      this.stepDialog.desensitize();

    // Set status of actions
    this.run.setSensitive(false);
    this.stop.setSensitive(true);
    this.step.setSensitive(false);
    this.next.setSensitive(false);
    this.finish.setSensitive(false);
    this.cont.setSensitive(false);
    this.nextAsm.setSensitive(false);
    this.stepAsm.setSensitive(false);
    
    this.stepInDialog.setSensitive(false);
    this.stepOverDialog.setSensitive(false);
    this.stepOutDialog.setSensitive(false);
    this.stepInstructionDialog.setSensitive(false);
    this.stepInstructionNextDialog.setSensitive(false);
    
    this.toggleDisassemblyWindow.setSensitive(false);
    this.toggleMemoryWindow.setSensitive(false);
    this.toggleRegisterWindow.setSensitive(false);

    this.stackTop.setSensitive(false);
    this.stackUp.setSensitive(false);
    this.stackDown.setSensitive(false);

    this.copy.setSensitive(false);
    this.find.setSensitive(false);
    this.prefsLaunch.setSensitive(false);
  }

  private void resensitize ()
  {
    // Set status of toolbar buttons
    this.glade.getWidget("toolbarGotoBox").setSensitive(true);
    this.glade.getWidget(SourceWindow.SOURCE_WINDOW).setSensitive(true);

    if (this.stepDialog != null)
      this.stepDialog.resensitize();
    
    // Set status of actions
//    this.run.setSensitive(true);
    this.stop.setSensitive(false);
    this.step.setSensitive(true);
//     this.next.setSensitive(true);
//     this.finish.setSensitive(true);
     this.cont.setSensitive(true);
//     this.nextAsm.setSensitive(true);
     this.stepAsm.setSensitive(true);
     
     this.stepInDialog.setSensitive(true);
//     this.stepOverDialog.setSensitive(true);
//     this.stepOutDialog.setSensitive(true);
     this.stepInstructionDialog.setSensitive(true);
//     this.stepInstructionNextDialog.setSensitive(true);
     
     this.toggleDisassemblyWindow.setSensitive(true);
     this.toggleMemoryWindow.setSensitive(true);
     this.toggleRegisterWindow.setSensitive(true);

    this.stackTop.setSensitive(true);
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

    List funcs = this.view.getFunctions();
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
    
    DOMSource source = null;
    Line[] lines = selected.getLines();
    
    if (lines.length > 0)
      source = selected.getLines()[0].getDOMSource();
    
    if (lines.length == 0)
      ((Label) this.glade.getWidget("sourceLabel")).setText("<b>"
                                                            + "Unknown File"
                                                            + "</b>");
    else if (source == null && lines.length > 0)
      ((Label) this.glade.getWidget("sourceLabel")).setText("<b>"
                                                            + selected.getLines()[0].getFile().getPath()
                                                            + "</b>");
    else
      ((Label) this.glade.getWidget("sourceLabel")).setText("<b>"
                                                            + source.getFileName()
                                                            + "</b>");
    ((Label) this.glade.getWidget("sourceLabel")).setUseMarkup(true);
    
    if (lines.length == 0)
    {
      SourceView v = (SourceView) SourceWindow.this.view;
      SourceBuffer b = (SourceBuffer) v.getBuffer();
      
      removeTags();
      v.load(selected);
      
      if(runState.getState() == RunState.STOPPED)
        b.disassembleFrame(selected);
      else
        b.deleteText(b.getStartIter(), b.getEndIter());
    }
    else if (source != null && lines[0].getDOMFunction() != null)
      {
        if (this.currentFrame.getLines().length == 0
            || ! source.getFileName().equals(this.currentFrame.getLines()[0].getFile().getName()))
          {
            removeTags();
            
            this.view.load(selected);

//            SourceView v = (SourceView) SourceWindow.this.view;
//            SourceBuffer b = (SourceBuffer) v.getBuffer();

            StackFrame curr = selected;

            /*
             * Find the innermost frame - want to make sure that we get
             * everything highlighted
             */
            while (curr.getInner() != null)
              curr = curr.getInner();

//            b.highlightLine(curr, true);
            createTags();

            this.view.scrollToFunction(lines[0].getDOMFunction().getFunctionCall());
          }
        else
          {
            if (selected.getLines().length == 0)
              return;
            else
	      this.view.scrollToLine(selected.getLines()[0].getLine());
          }
      }

    this.currentFrame = selected;
    this.view.showAll();
  }

  private void removeTags ()
  {
    SourceView sv = (SourceView) view;
    SourceBuffer sb = (SourceBuffer) sv.getBuffer();

    for (int i = 0; i < this.frames.length; i++)
      {
        sb.highlightLine(frames[i], false);
      }
  }
  
  private void createTags ()
  {
    SourceView sv = (SourceView) view;
    SourceBuffer sb = (SourceBuffer) sv.getBuffer();

    for (int i = 0; i < this.frames.length; i++)
      {
        sb.highlightLine(frames[i], true);
      }
  }

  /**
   * Tells the debugger to run the program
   */
  private void doRun ()
  {

  }

  private void doStop ()
  {

    // Set status of toolbar buttons
    this.glade.getWidget("toolbarGotoBox").setSensitive(false);
    this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX).setSensitive(false);

    this.glade.getWidget(SourceWindow.SOURCE_WINDOW).setSensitive(false);

    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stopped");

    // this.pbo.requestAddObservers(this.myProc.getMainTask());

    if (this.threadDialog == null)
      {
        this.runState.stop(null);
      }
    else
      {
        this.runState.stop(this.threadDialog.getBlockTasks());
      }
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
  private synchronized void doStep ()
  {
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping");

    desensitize();

    if (this.runState.setUpLineStep(this.currentTask))
      removeTags();
  }
  
  protected void doStep (LinkedList tasks)
  {
    if (tasks.size() == 0)
      return;

    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping");

    desensitize();

    if (this.runState.setUpLineStep(tasks))
      removeTags();
  }

  /**
   * "Step-over"
   */
  private void doNext ()
  {
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping Over");

    desensitize();
    LinkedList l = new LinkedList();
    l.add(this.currentTask);
    this.runState.setUpStepOver(l, this.currentFrame);
    removeTags();
  }
  
  protected void doNext (LinkedList tasks)
  {
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping Over");

    desensitize();

    this.runState.setUpStepOver(tasks, this.currentFrame);
    removeTags();
  }

  /**
   * Tells the debugger to continue execution
   */
  private void doContinue ()
  {
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Running");

    desensitize();

    this.runState.continueExecution(this.swProc.getTasks());

    removeTags();
  }

  /**
   * Tells the debugger to finish executing the current function "Step out"
   */
  private void doFinish ()
  {
    System.out.println("Step Out");

    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping Out");

    desensitize();

    LinkedList l = new LinkedList();
    l.add(this.currentTask);
    this.runState.setUpStepOut(l, this.currentFrame);
    removeTags();
  }
  
  protected void doFinish (LinkedList tasks)
  {
    System.out.println("Step Out");

    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping Out");

    desensitize();

    this.runState.setUpStepOut(tasks, this.currentFrame);
    removeTags();
  }
  
  protected void doAdvance ()
  {
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping to current frame");
    
    desensitize();
    
    this.runState.setUpStepAdvance(this.currentTask, this.currentFrame);
    removeTags();
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
  private synchronized void doAsmStep ()
  {
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping instruction");

    desensitize();

    if (this.runState.stepInstruction(this.currentTask))
      removeTags();
  }
  
  /**
   * A request for an instruction step on one or more tasks.
   * 
   * @param tasks The list of tasks to step.
   */
  protected void doStepAsm (LinkedList tasks)
  {
    if (tasks.size() == 0)
      return;

    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping");

    desensitize();

    if (this.runState.stepInstruction(tasks))
      removeTags();
  }

  /**
   * Tells the debugger to execute the next assembly instruction
   */
  private void doAsmNext ()
  {
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping next instruction");

    desensitize();

    this.runState.setUpStepNextInstruction(this.currentTask, this.currentFrame);
    removeTags();
  }
  
  protected void doAsmNext (LinkedList tasks)
  {
    if (tasks.size() == 0)
      return;
    
    StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
    sbar.push(0, "Stepping next instruction");

    desensitize();

    this.runState.setUpStepNextInstruction(tasks, this.currentFrame);
    removeTags();
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
  private void doStackTop ()
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
        this.runState.addObserver(regWin.getLockObserver());
        regWin.setClosed(false);
        regWin.showAll();
      }
  }

  private Isa getProcIsa ()
  {
    return swProc.getMainTask().getIsa();
  }

  private void toggleMemoryWindow ()
  {
    Isa isa = getProcIsa();
    if (! (isa instanceof frysk.proc.IsaIA32 || isa instanceof frysk.proc.IsaPPC))
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
        this.runState.addObserver(memWin.getLockObserver());
        memWin.setClosed(false);
        memWin.showAll();
      }
  }

  private void toggleDisassemblyWindow ()
  {
    Isa isa = getProcIsa();
    if (! (isa instanceof frysk.proc.IsaIA32 || isa instanceof frysk.proc.IsaPPC))
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
        this.runState.addObserver(disWin.getLockObserver());
        disWin.setClosed(false);
        disWin.showAll();
      }
  }

  private void toggleConsoleWindow ()
  {
    if (this.conWin == null)
      this.conWin = new ConsoleWindow();
    else
      this.conWin.showAll();
  }
  
  private void handleDialog (int type)
  {
    if (this.stepDialog == null)
      {
        this.stepDialog = new StepDialog(glade, this);
      }
      
      this.stepDialog.setType(type);
      this.stepDialog.showAll();
  }

  private synchronized void executeTasks (LinkedList tasks)
  {
    this.runState.executeTasks(tasks);
  }

  private StackFrame[] generateProcStackTrace (StackFrame[] frames, Task[] tasks)
  {

    int size = this.swProc.getTasks().size();
    int mainTid = this.swProc.getPid();
    
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
        /** Create the stack frame * */

        StackFrame curr = null;
        try
          {
            frames[j] = StackFactory.createStackFrame(tasks[j]);
            curr = frames[j];
          }
        catch (Exception e)
          {
            System.out.println("Error generating stack trace");
            e.printStackTrace();
          }

        /** Stack frame created */
        
        if (tasks[j].getTid() == mainTid)
          this.symTab = new SymTab(mainTid, this.swProc, tasks[j], frames[j]);
          

        while (curr != null && this.dom == null) 
          {
            
            if (this.dom == null)
              {
                try
                  {
                    this.dom = DOMFactory.createDOM(curr, this.swProc);
                  }

                catch (NoDebugInfoException e)
                  {
                  }
                catch (IOException e)
                  {
                  }
              }
            curr = curr.getOuter();
          }
      }

    DOMFactory.clearDOMSourceMap(this.swProc);
    
    if (this.runState.getState() == RunState.STOPPED)
      symTab.setFrames(frames);
    
    return frames;
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
      if (newFrame == null)
        return;

      if (newFrame.getTask().getTid() != SourceWindow.this.currentTask.getTid())
        SourceWindow.this.currentTask = newFrame.getTask();

      if (SourceWindow.this.currentFrame != null
          && SourceWindow.this.currentFrame.getCFA() != newFrame.getCFA())
        {
          DisassemblyWindow disWin = DisassemblyWindowFactory.disWin;
          if (disWin != null && disWin.getClosed() == false)
            disWin.resetTask(newFrame.getTask());

          MemoryWindow memWin = MemoryWindowFactory.memWin;
          if (memWin != null && memWin.getClosed() == false)
            memWin.resetTask(newFrame.getTask());

          RegisterWindow regWin = RegisterWindowFactory.regWin;
          if (regWin != null && regWin.getClosed() == false)
            regWin.resetTask(newFrame.getTask());
        }

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
   * Local Observer class used to poke this window from RunState when all the
   * Tasks belonging to this window's Proc have been blocked. These Tasks could
   * have ben running, stepping, or neither and were just blocked once to allow
   * this window to finish building. This observer is synchronized between this
   * windowand the Memory, Register, and Disassembly windows.
   */
  private class LockObserver
      implements Observer
  {

//    private Task lockTask;

    /**
     * Builtin Observer method - called whenever the Observable we're concerned
     * with - in this case the RunState - has changed.
     * 
     * @param o The Observable we're watching
     * @param arg An Object argument
     */
    public void update (Observable o, Object arg)
    {
      /* We don't need to worry about this case here */
      if (arg == null)
        return;

      /*
       * The very first time all the Tasks are blocked is when we're
       * initializing this window.
       */
      if (SW_active == false)
        {
          SW_active = true;
//          lockTask = (Task) arg;
          CustomEvents.addEvent(new Runnable()
          {
            public void run ()
            {
              finishSourceWin();
            }
          });
          return;
        }

      /*
       * Otherwise, this callback was called because all our Proc's Tasks were
       * blocked because of some state change operation. Re-generate the stack
       * trace information and refresh the window.
       */
      CustomEvents.addEvent(new Runnable()
      {
        public void run ()
        {
          StackFrame[] frames = generateProcStackTrace(null, null);
          populateStackBrowser(frames);
          SourceWindow.this.runState.notifyStopped();
          procReblocked();
        }
      });
    }
  }

}
