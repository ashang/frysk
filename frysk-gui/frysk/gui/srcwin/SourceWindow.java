// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008 Red Hat Inc.
// Copyright 2007 Oracle Corporation.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gnu.gdk.Color;
import org.gnu.gdk.KeyValue;
import org.gnu.gdk.ModifierType;
import org.gnu.glade.GladeXMLException;
import org.gnu.glade.LibGlade;
import org.gnu.glib.CustomEvents;
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
import org.gnu.gtk.FileChooserAction;
import org.gnu.gtk.FileChooserDialog;
import org.gnu.gtk.GtkStockItem;
import org.gnu.gtk.IconSize;
import org.gnu.gtk.Image;
import org.gnu.gtk.Label;
import org.gnu.gtk.ListStore;
import org.gnu.gtk.Menu;
import org.gnu.gtk.MenuBar;
import org.gnu.gtk.MenuItem;
import org.gnu.gtk.ResponseType;
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
import org.gnu.gtk.event.ActionListener;
import org.gnu.gtk.event.ButtonEvent;
import org.gnu.gtk.event.ButtonListener;
import org.gnu.gtk.event.ComboBoxEvent;
import org.gnu.gtk.event.ComboBoxListener;
import org.gnu.gtk.event.EntryEvent;
import org.gnu.gtk.event.EntryListener;
import org.gnu.gtk.event.FileChooserEvent;
import org.gnu.gtk.event.FileChooserListener;
import org.gnu.gtk.event.LifeCycleEvent;
import org.gnu.gtk.event.LifeCycleListener;
import org.gnu.gtk.event.MenuItemEvent;
import org.gnu.gtk.event.MenuItemListener;
import org.gnu.gtk.event.MouseEvent;
import org.gnu.gtk.event.MouseListener;
import frysk.config.Prefix;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.dom.DOMFactory;
import frysk.dom.DOMFrysk;
import frysk.dom.DOMSource;
import frysk.gui.common.IconManager;
import frysk.gui.console.ConsoleWindow;
import frysk.gui.disassembler.DisassemblyWindow;
import frysk.gui.disassembler.DisassemblyWindowFactory;
import frysk.gui.memory.MemoryWindow;
import frysk.gui.memory.MemoryWindowFactory;
import frysk.gui.monitor.WindowManager;
import frysk.gui.prefs.BooleanPreference;
import frysk.gui.prefs.PreferenceManager;
import frysk.gui.prefs.PreferenceWindow;
import frysk.gui.prefs.BooleanPreference.BooleanPreferenceListener;
import frysk.gui.register.RegisterWindow;
import frysk.gui.register.RegisterWindowFactory;
import frysk.gui.sessions.DebugProcess;
import frysk.gui.sessions.SessionManager;
import frysk.gui.srcwin.CurrentStackView.StackViewListener;
import frysk.gui.srcwin.prefs.SourceWinPreferenceGroup;
import frysk.gui.terminal.TermWindow;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rt.DisplayManager;
import frysk.rt.LineXXX;
import frysk.rt.UpdatingDisplayValue;
import frysk.scopes.SourceLocation;
import frysk.stack.FrameIdentifier;
import frysk.stepping.SteppingEngine;
import frysk.stepping.TaskStepEngine;
import frysk.proc.dead.LinuxCoreProc;

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

    private LibGlade glade;

    private LibGlade glade_fc;

    private View view;

    // private PreferenceWindow prefWin;

    // ACTIONS
    private Action close;

    private Action open_core;
    
    private Action open_load;

    private Action open_executable;

    private Action attach_proc;

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

    private Action stackTop;

    private Action stepInDialog;

    private Action stepOverDialog;

    private Action stepOutDialog;

    private Action stepInstructionDialog;

    private Action stepInstructionNextDialog;

    private ToggleAction toggleRegisterWindow;

    private ToggleAction toggleMemoryWindow;

    private ToggleAction toggleDisassemblyWindow;

    private ToggleAction toggleConsoleWindow;

    private ToggleAction toggleThreadDialog;

    private ToggleAction toggleStepDialog;

    private ComboBox viewPicker;

    private ThreadSelectionDialog threadDialog = null;

    private StepDialog stepDialog = null;

    private DOMFrysk dom[];

    private Proc[] swProc;

    private int current = 0;

    private int numProcs = 1;

    private CurrentStackView stackView;

    private VariableWatchView watchView;

    private ConsoleWindow conWin;

    private TermWindow termWin;

    // HashMap to keep up with Terminal Windows
    private HashMap termHash;

    protected boolean SW_active = false;

    private DebugInfoFrame currentFrame;

    private Task currentTask;

    private FrameIdentifier fi;

    private DebugInfoFrame[][] frames;

    // Due to java-gnome bug #319415
    private ToolTips tips;

    // private static Logger errorLog = Logger.getLogger(Gui.ERROR_LOG_ID);

    private SteppingEngine steppingEngine;

    // Private inner class to take care of the event handling
    private SourceWindowListener listener;

    private StackMouseListener mouseListener;

    private SourceWindowFactory.AttachedObserver attachedObserver;

    private SourceWindowFactory.AttachedObserver addedAttachedObserver;

    private LockObserver lock;

    private org.gnu.gtk.FileChooserDialog chooser;

    private FileChooserDialog fc;

    private Logger logger = Logger.getLogger("frysk");

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

    /*
         * GLADE CONSTANTS
         */
    // Glade file to use
    public static final String GLADE_FILE = "frysk_debug.glade";

    // Modified FileChooser widget used for executable activation
    public static final String FILECHOOSER_GLADE = "frysk_filechooser.glade";

    // Glade file that contains the process list to attach frysk to
    public static final String PROC_LIST_GLADE = "frysk_create_session_druid.glade";

    /*
         * END GLADE CONSTANTS
         */

    /**
     * Creates a new source window with the given properties. This
     * constructor should not be called explicitly, SourceWindow
     * objects should be created through the {@link
     * SourceWindowFactory} class.
     * 
     * @param glade
     *                The LibGlade object that contains the window for this
     *                instance
     * @param proc
     *                The Proc to have this SourceWindow observe
     */
    public SourceWindow(LibGlade glade, Proc proc) {
	super(((Window) glade.getWidget(SOURCE_WINDOW)).getHandle());
	
	this.setIcon(IconManager.windowIcon);

	this.glade = glade;
	this.swProc = new Proc[this.numProcs];
	this.swProc[this.current] = proc;
	this.frames = new DebugInfoFrame[1][];
	this.lock = new LockObserver();
	Proc[] temp = new Proc[1];
	temp[0] = proc;
	this.steppingEngine = new SteppingEngine(temp, this.lock);
	this.termHash = new HashMap();
    }

    /**
         * Creates a new source window with the given properties. This
         * constructor should not be called explicitly, SourceWindow objects
         * should be created through the {@link SourceWindowFactory} class.
         * 
         * @param glade
         *                The LibGlade object that contains the window for this
         *                instance
         * @param procs
         *                The array of Procs to have this new SourceWindow
         *                observes
         */
    public SourceWindow(LibGlade glade, Proc[] procs) {
	super(((Window) glade.getWidget(SOURCE_WINDOW)).getHandle());
	
	this.setIcon(IconManager.windowIcon);

	this.glade = glade;
	this.numProcs = procs.length;
	this.swProc = procs;
	this.frames = new DebugInfoFrame[this.numProcs][];
	this.lock = new LockObserver();
	this.dom = new DOMFrysk[this.numProcs];
	this.steppingEngine = new SteppingEngine(procs, this.lock);
	this.termHash = new HashMap();
    }

    /**
         * Creates a new source window with the given properties. This
         * constructor builds a SourceWindow based off of a stack trace, and
         * thus has no usable or running process. This constructor should not be
         * called explicitly, SourceWindow objects should be created through the
         * {@link SourceWindowFactory} class.
         * 
         * @param glade
         *                The LibGlade object that contains the window for this
         *                instance
         * @param trace
         *                The stack frame that represents the current state of
         *                execution
         */
    public SourceWindow(LibGlade glade, DebugInfoFrame trace) {
	super(((Window) glade.getWidget(SOURCE_WINDOW)).getHandle());
	
	this.setIcon(IconManager.windowIcon);

	this.glade = glade;
	this.swProc = new Proc[1];
	this.swProc[this.current] = trace.getTask().getProc();
	this.steppingEngine = new SteppingEngine();
	this.steppingEngine.setRunning(this.swProc[this.current].getTasks());
	this.frames = new DebugInfoFrame[1][];
	this.dom = new DOMFrysk[1];

	try {
	    this.dom[0] = DOMFactory.createDOM(trace, this.swProc[0]);
	} catch (IOException e) {
	}

	DebugInfoFrame[] newTrace = new DebugInfoFrame[1];
	newTrace[0] = trace;
	this.frames[0] = newTrace;

	finishSourceWin();

	desensitize();
	this.stop.setSensitive(false);
    }

    /**
         * Creates a new source window with the given properties. This
         * constructor builds a SourceWindow based off of a stack trace, and
         * thus has no usable or running process. This constructor should not be
         * called explicitly, SourceWindow objects should be created through the
         * {@link SourceWindowFactory} class.
         * 
         * @param glade
         *                The LibGlade object that contains the window for this
         *                instance
         * @param traces
         *                The stack frames that represents the current state of
         *                execution
         */
    public SourceWindow(LibGlade glade, DebugInfoFrame[] traces) {
	super(((Window) glade.getWidget(SOURCE_WINDOW)).getHandle());
	
	this.setIcon(IconManager.windowIcon);

	this.glade = glade;
	this.swProc = new Proc[1];
	this.swProc[this.current] = traces[0].getTask().getProc();
	this.steppingEngine = new SteppingEngine();
	this.frames = new DebugInfoFrame[traces.length][];
	this.dom = new DOMFrysk[traces.length];

	try {
	    for (int i = 0; i < traces.length; i++) {
		this.dom[i] = DOMFactory.createDOM(traces[i], this.swProc[0]);
	    }

	} catch (IOException e) {
	}

	for (int i = 0; i < traces.length; i++)
	    this.frames[i] = new DebugInfoFrame[] { traces[i] };

	finishSourceWin();

	desensitize();
	this.stop.setSensitive(false);
	this.toggleDisassemblyWindow.setSensitive(true);
	this.toggleMemoryWindow.setSensitive(true);
	// this.toggleRegisterWindow.setSensitive(true);
    }

    /**
         * Creates a new source window with the given properties. This
         * constructor should not be called explicitly, SourceWindow objects
         * should be created through the {@link SourceWindowFactory} class.
         * 
         * @param glade
         *                The LibGlade object that contains the window for this
         *                instance
         * @param proc
         *                The Proc to have this SourceWindow observe
         * @param ao
         *                The AttachedObserver currently blocking the given Proc
         */
    public SourceWindow(LibGlade glade, Proc proc, SourceWindowFactory.AttachedObserver ao) {
	
	
	this(glade, proc);
	this.attachedObserver = ao;

	this.addListener(new LifeCycleListener() {
	    public void lifeCycleEvent(LifeCycleEvent event) {
	    }

	    public boolean lifeCycleQuery(LifeCycleEvent event) {
		if (event.isOfType(LifeCycleEvent.Type.DESTROY)
			|| event.isOfType(LifeCycleEvent.Type.DELETE)) {
		    SourceWindow.this.hideAll();
		}
		return true;
	    }
	});

    }

    /**
         * Initializes the rest of the members of the SourceWindow not handled
         * by the constructor. Most of these depend on the Tasks of the process
         * being blocked.
         */
    private void finishSourceWin() {
	/* Only because this wouldn't be the case during a Monitor stack trace */
	if (!this.steppingEngine.isTaskRunning(this.swProc[this.current]
		.getMainTask())) {
	    for (int j = 0; j < numProcs; j++)
		this.frames[j] = generateProcStackTrace(this.swProc[j], j);
	}

	this.listener = new SourceWindowListener(this);
	this.mouseListener = new StackMouseListener();
	this.watchView = new VariableWatchView();
	this.tips = new ToolTips();

	/* Attach the variableWatchView to the WatchList for this process */
	DebugProcess dp = getCurrentDebugProcess();

	if (dp != null)
	    dp.getWatchList().addListener(watchView);

	this.glade.getWidget(SourceWindow.SOURCE_WINDOW).hideAll();

	AccelGroup ag = new AccelGroup();
	((Window) this.glade.getWidget(SourceWindow.SOURCE_WINDOW))
		.addAccelGroup(ag);

	this.viewPicker = (ComboBox) this.glade
		.getWidget(SourceWindow.VIEW_COMBO_BOX);
	this.viewPicker.setActive(0);

	/* Populate the stack view */
	this.populateStackBrowser(this.frames);

	/* This would be the case during a CLI attach to a single executable */
	if (this.attachedObserver != null) {
	    Iterator i = this.swProc[0].getTasks().iterator();
	    while (i.hasNext()) {
		Task t = (Task) i.next();
		t.requestDeleteAttachedObserver(this.attachedObserver);
	    }
	}

	this.createActions(ag);
	this.createMenus();
	this.createToolBar();
	this.createSearchBar();

	this.attachEvents();

	ScrolledWindow sw = (ScrolledWindow) this.glade
		.getWidget("traceScrolledWindow");
	sw.add(this.watchView);

	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stopped");

	this.cont.setSensitive(true);
	this.stop.setSensitive(false);

	this.showAll();
	this.glade.getWidget(FIND_BOX).hideAll();
    }

    /**
         * Populates the stack browser window
         * 
         * @param frames An array of DebugInfoFrames used to populate information
         * inside the stack frame window.
         */
    public void populateStackBrowser(DebugInfoFrame[][] frames) {
	this.frames = frames;

	/* Initialization */
	if (this.view == null) {
	    this.stackView = new CurrentStackView(frames);
	    DebugInfoFrame temp = null;

	    temp = CurrentStackView.getCurrentFrame();

	    if (temp == null)
		temp = frames[0][0];

	    DebugInfoFrame curr = temp;
	    this.currentFrame = temp;

	    while (curr != null && curr.getLine() == SourceLocation.UNKNOWN)
		curr = curr.getOuterDebugInfoFrame();

	    if (curr != null) {
		this.currentFrame = curr;
		this.currentTask = curr.getTask();
		this.view = new SourceView(curr, this);

		SourceBuffer b = (SourceBuffer) ((SourceView) this.view)
			.getBuffer();

		for (int j = 0; j < frames[this.current].length; j++) {
		    b.highlightLine(frames[this.current][0], true);
		}
	    } else {
		this.view = new SourceView(temp, this);
		this.currentTask = temp.getTask();
	    }

	    ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
		    .add((Widget) this.view);
	    ScrolledWindow sw = (ScrolledWindow) this.glade
		    .getWidget("stackScrolledWindow");
	    sw.add(stackView);
	    this.watchView.setView((SourceView) this.view);
	    updateSourceLabel(this.currentFrame);
	    stackView.expandAll();
	    this.stackView.selectRow(this.currentFrame);
	    TreePath path = null;
	    try {
		path = this.stackView.getSelection().getSelectedRows()[0];
		this.stackView.expandRow(path, true);
		this.stackView.scrollToCell(path);
	    } catch (ArrayIndexOutOfBoundsException ae) {
		// stackView.expandAll();
	    }

	    if (this.currentFrame.getLine() != SourceLocation.UNKNOWN) { 
		    if (this.currentFrame.getLineXXX().getDOMFunction() != null)
			this.view.scrollToFunction(this.currentFrame.getLineXXX().getDOMFunction().getFunctionCall());
		    else
			this.view.scrollToLine(this.currentFrame.getLine().getLine());
	    } else {
		/* Only the case during a monitor stack trace */
		if (!this.steppingEngine
			.isProcRunning(this.swProc[this.current].getTasks())) {
		    SourceBuffer b = (SourceBuffer) ((SourceView) this.view)
			    .getBuffer();
		    b.disassembleFrame(this.currentFrame);
		}
	    }
	    updateSourceLabel(curr);
	    this.fi = this.currentFrame.getFrameIdentifier();
	    stackView.showAll();
	    this.view.showAll();
	    return;
	}

	SourceBuffer sb = null;
	if (this.view instanceof SourceView)
	    sb = (SourceBuffer) ((SourceView) this.view).getBuffer();
	else
	    sb = (SourceBuffer) ((MixedView) this.view).getSourceWidget()
		    .getBuffer();

	DebugInfoFrame curr = null;
	DebugInfoFrame taskMatch = null;

	/*
         * Refresh the information displayed in the stack view with the info
         * from the new stack trace
         */
	this.stackView.refreshProc(frames[this.current], this.current);
	this.stackView.expandAll();
	DebugInfoFrame newFrame = null;

	/*
         * Try to find the new StackFrame representing the same frame from
         * before the reset
         */
	for (int j = 0; j < frames[this.current].length; j++) {
	    curr = frames[this.current][j];
	    if (curr.getTask().getTid() == this.currentTask.getTid()) {
		this.currentTask = curr.getTask();
		taskMatch = curr;
	    }

	    sb.highlightLine(curr, true);

	    if (newFrame == null) {
		while (curr != null) {
		    if (fi.equals(curr.getFrameIdentifier())) {
			newFrame = curr;
			break;
		    }
		    curr = curr.getOuterDebugInfoFrame();
		}
	    }
	}

	if (newFrame == null) {
	    if (taskMatch != null) {
		newFrame = taskMatch;
	    } else {
		newFrame = this.stackView.getFirstFrameSelection();
	    }
	}

	this.stackView.selectRow(newFrame);
	TreePath path = null;
	try {
	    path = this.stackView.getSelection().getSelectedRows()[0];
	    this.stackView.expandRow(path, true);
	    this.stackView.scrollToCell(path);
	} catch (ArrayIndexOutOfBoundsException ae) {
	    // not sure what to do here yet, although expandAll() isn't it
	    // stackView.expandAll();
	}

	this.fi = newFrame.getFrameIdentifier();
	updateShownStackFrame(newFrame, this.current);
	updateSourceLabel(newFrame);
    }
    
    /**
     * Cleans up the SourceWindow after all observed processes have exited.
     */
    private void allProcsExited() {
	((Label) SourceWindow.this.glade.getWidget("sourceLabel"))
		.setUseMarkup(true);
	SourceWindow.this.stackView.clear();
	SourceBuffer b = (SourceBuffer) ((SourceView) view).getBuffer();
	b.clear();
	SourceWindow.this.desensitize();
	SourceWindow.this.stop.setSensitive(false);

	return;
    }

    /**
         * Adds the selected variable to the variable trace window
         * 
         * @param var
         *                The variable to trace
         */
    public void addVariableTrace(String var) {
	UpdatingDisplayValue disp = DisplayManager.createDisplay(currentTask,
		currentFrame.getFrameIdentifier(), steppingEngine, var);
	getCurrentDebugProcess().getWatchList().addVariable(disp);
    }

    /**
         * Removes the given variable from the list of watched variables. The
         * variable is assumed to come from the current context.
         * 
         * @param var
         *                The value to remove
         */
    public void removeVariableTrace(String var) {
	UpdatingDisplayValue disp = DisplayManager.createDisplay(currentTask,
		currentFrame.getFrameIdentifier(), steppingEngine, var);
	getCurrentDebugProcess().getWatchList().removeVariable(disp);
    }

    /**
         * Removes the given display from the list of watched expressions
         * 
         * @param disp
         *                The display to remove
         */
    public void removeDisplay(UpdatingDisplayValue disp) {
	getCurrentDebugProcess().getWatchList().removeVariable(disp);
    }

    public void updateThreads() {
	executeTasks(this.threadDialog.getBlockTasks());
    }

    /**
         * Called from SourceWindowFactory when all Tasks have notified that
         * they are blocked and new stack traces have been generated. This is
         * called after the StackView has been re-populated, allowing the
         * SourceWindow to be sensitive again.
         */
    protected void procReblocked() {
	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stopped");

	if (this.currentFrame.getLine() == SourceLocation.UNKNOWN) {
	    ((SourceBuffer) ((SourceView) this.view).getBuffer())
		    .disassembleFrame(this.currentFrame);
	}

	resensitize();
    }

    /***********************************************************************
         * Adding/Removing Processes
         **********************************************************************/

    /**
         * Called when a new executable has been selected to run and add to the
         * window.
         * 
         * @param exe -
         *                the executable's path to start
         * @param env_variables -
         *                reserved for future use, will be used to pass the
         *                environment arguments to the executable
         * @param options -
         *                options to pass to tDEBUG_WINDOW_MODEhe task on the
         *                command line
         * @param stdin -
         *                device to point the executed task's stdin to
         * @param stdout -
         *                device to point the executed task's stdout to
         * @param stserr -
         *                device to point the executed task's stderr to
         */
    protected void addProc(String exe, String env_variables, String options,
	    String stdin, String stdout, String stderr) {
	this.SW_add = true;
	this.addedAttachedObserver = SourceWindowFactory.startNewProc(exe,
		env_variables, options, stdin, stdout, stderr);
    }

    public void appendTask(Proc proc) {
	getSteppingEngine().addProc(proc);
	getSteppingEngine().addObserver(this.lock);
	// this.threadObserver = new ThreadLifeObserver();
	// this.steppingEngine.setThreadObserver(this.threadObserver);
	appendProc(proc.getMainTask());
    }

    /**
         * Appends a new Proc object to this window's data structures and
         * interface
         * 
         * @param task
         *                The Task whose Proc is to be appended to this window
         */
    protected void appendProc(Task task) {
	this.SW_add = false;
	Proc proc = task.getProc();
	int oldSize = this.numProcs;
	++this.numProcs;

	DebugInfoFrame[][] newFrames = new DebugInfoFrame[numProcs][];
	DOMFrysk[] newDom = new DOMFrysk[numProcs];
	Proc[] newSwProc = new Proc[numProcs];

	for (int i = 0; i < oldSize; i++) {
	    newFrames[i] = new DebugInfoFrame[this.frames[i].length];
	    System.arraycopy(this.frames, 0, newFrames, 0, oldSize);
	}
	System.arraycopy(this.dom, 0, newDom, 0, oldSize);
	System.arraycopy(this.swProc, 0, newSwProc, 0, oldSize);

	this.frames = newFrames;
	this.dom = newDom;
	this.swProc = newSwProc;

	this.swProc[oldSize] = proc;
	this.frames[oldSize] = generateProcStackTrace(task.getProc(), oldSize);
	this.stackView.addProc(this.frames[oldSize], oldSize);
	SourceWindowFactory.removeAttachedObserver(task,
		this.addedAttachedObserver);
	resensitize();
    }

    /**
         * Removes the currently selected process from SourceWindow data
         * structures and the interface.
         * 
         * @param kill
         *                Whether or not the process should be killed after a
         *                detach is performed.
         */
    protected void removeProc(boolean kill) {
	int oldSize = this.numProcs;
	--this.numProcs;

	DebugInfoFrame[][] newFrames = new DebugInfoFrame[numProcs][];
	DOMFrysk[] newDom = new DOMFrysk[numProcs];
	Proc[] newSwProc = new Proc[numProcs];

	DOMFactory.clearDOMSourceMap(this.swProc[this.current]);
	if (this.swProc[this.current].getPid() != 0)
	    this.steppingEngine.detachProc(this.swProc[this.current], kill);

	int j = 0;
	for (int i = 0; i < oldSize; i++) {
	    if (i != this.current) {
		newFrames[j] = new DebugInfoFrame[this.frames[i].length];
		System.arraycopy(this.frames[i], 0, newFrames[j], 0,
			this.frames[i].length);
		newDom[j] = this.dom[i];
		newSwProc[j] = this.swProc[i];
		++j;
	    }
	}

	this.frames = newFrames;
	this.dom = newDom;
	this.swProc = newSwProc;
	this.stackView.removeProc(this.current);

	this.current = 0;

	if (this.swProc.length > 0)
	    this.currentTask = this.swProc[this.current].getMainTask();
	else
	    this.currentTask = null;
    }

    /***********************************************************************
         * Getters and Setters
         **********************************************************************/
    
    public Proc getSwProc() {
	if (this.swProc.length > 0)
	    return this.swProc[this.current];
	else
	    return null;
    }

    public DOMFrysk getDOM() {
	return this.dom[this.current];
    }

    public View getView() {
	return this.view;
    }

    public CurrentStackView getStackView() {
	return this.stackView;
    }

    public boolean isRunning() {
	return this.steppingEngine.isTaskRunning(this.currentTask);
    }

    public LockObserver getLockObserver() {
	return this.lock;
    }

    public SteppingEngine getSteppingEngine() {
	return this.steppingEngine;
    }

    /***********************************************************************
         * PRIVATE METHODS
         **********************************************************************/

    /**
         * Returns the DebugProcess from the SessionManager corresponding to the
         * current Proc
         */
    private DebugProcess getCurrentDebugProcess() {
	logger.log(Level.FINE, "{0}, entering getCurrentDebugProcess", this);
	Iterator iter = SessionManager.theManager.getCurrentSession()
		.getProcesses().iterator();
	while (iter.hasNext()) {
	    DebugProcess dProc = (DebugProcess) iter.next();
	    if (dProc.getExecutablePath().equals(swProc[current].getExeFile().getSysRootedPath()))
		return dProc;
	}
	/* should not get here */
	// TODO: throw an exception if we do
	return null;
    }

    /**
         * Resets the search box to its original state.
         */
    private void resetSearchBox() {
	this.glade.getWidget(SourceWindow.FIND_TEXT).setBaseColor(
		StateType.NORMAL, Color.WHITE);
    }

    /**
         * Creates the menus and assigns hotkeys
         */
    private void createActions(AccelGroup ag) {
	// Examine core file action
	this.open_core = new Action("open", "Examine core file...",
		"Examine core file", GtkStockItem.OPEN.getString());
	this.open_core.setAccelGroup(ag);
	this.open_core.setAccelPath("<sourceWin>/Processes/Examine core file...");
	this.open_core.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		// SourceWindow.this.glade.getWidget(SOURCE_WINDOW).destroy();
		chooser = new FileChooserDialog(
			"Frysk: Choose a core file to examine",
			(Window) SourceWindow.this.glade
				.getWidget(SOURCE_WINDOW),
			FileChooserAction.ACTION_OPEN);
		// Set the selection insensitive until code is written to handle
                // Core Files
		chooser.setSensitive(true);
		chooser.addListener(new LifeCycleListener() {
		    public void lifeCycleEvent(LifeCycleEvent event) {
		    }

		    public boolean lifeCycleQuery(LifeCycleEvent event) {
			if (event.isOfType(LifeCycleEvent.Type.DELETE)
				|| event.isOfType(LifeCycleEvent.Type.DESTROY))
			    chooser.destroy();
			return false;
		    }
		});
		chooser.addListener(new FileChooserListener() {
		    public void currentFolderChanged(FileChooserEvent event) {
		    }

		    public void selectionChanged(FileChooserEvent event) {
		    }

		    public void updatePreview(FileChooserEvent event) {
		    }

		    // This method is called when the "Enter" key is pressed
                        // to
		    // select a file name
		    public void fileActivated(FileChooserEvent event) {
			examineCoreFile();
		    }
		});
		setDefaultIcon(IconManager.windowIcon);
		chooser.setDefaultResponse(FileChooserEvent.Type.FILE_ACTIVATED
			.getID());
		chooser.setCurrentFolder(System.getProperty("user.home"));
		int response = chooser.open();
		if (response == ResponseType.CANCEL.getValue())
		    chooser.destroy();
		// The OK button was clicked, go open a source window for this
                // core file
		else if (response == ResponseType.OK.getValue()) {
		    examineCoreFile();
		    chooser.destroy();
		}
	    }
	});
	AccelMap.changeEntry("<sourceWin>/Processes/Examine core file...",
		KeyValue.o, ModifierType.CONTROL_MASK, true);
	this.open_core.connectAccelerator();
	
	// Load executable action
	this.open_load = new Action("Load an executable",
		"Load a process...", "Load a process from a file",
		GtkStockItem.OPEN.getString());
	this.open_load.setAccelGroup(ag);
	this.open_load
		.setAccelPath("<sourceWin>/Processes/Load a process...");
	this.open_load.addListener(new ActionListener() {
	    public void actionEvent(ActionEvent action) {
		try {
		    glade_fc = new LibGlade(Prefix.gladeFile(FILECHOOSER_GLADE).getAbsolutePath(), null);
		    fc = (FileChooserDialog) glade_fc
			    .getWidget("frysk_filechooserdialog");
		    fc.addListener(new LifeCycleListener() {
			public void lifeCycleEvent(LifeCycleEvent event) {
			}

			public boolean lifeCycleQuery(LifeCycleEvent event) {
			    if (event.isOfType(LifeCycleEvent.Type.DELETE)
				    || event
					    .isOfType(LifeCycleEvent.Type.DESTROY))
				fc.destroy();
			    return false;
			}
		    });

		    fc.addListener(new FileChooserListener() {
			public void currentFolderChanged(FileChooserEvent event) {
			}

			public void selectionChanged(FileChooserEvent event) {
			}

			public void updatePreview(FileChooserEvent event) {
			}

			// This method is called when the "Enter" key is pressed
                        // to
			// select a file name in the chooser
			public void fileActivated(FileChooserEvent event) {
			    loadExecutableFile();
			}
		    });
		    fc.setIcon(IconManager.windowIcon);
		    fc.setDefaultResponse(FileChooserEvent.Type.FILE_ACTIVATED
			    .getID());
		    fc.setCurrentFolder(System.getProperty("user.home"));
		    CheckButton term_activate = (CheckButton) glade_fc
			.getWidget("term_activate");
		    term_activate.setSensitive(false);
		    gtk_widget_set_size_request(fc.getHandle(), 300, 600);
		    int response = fc.open();
		    // "OK" key has been clicked
		    if (response == ResponseType.OK.getValue())
			loadExecutableFile();
		    // "Cancel" key has been clicked
		    if (response == ResponseType.CANCEL.getValue())
			fc.destroy();
		} catch (Exception e) {
		    throw new RuntimeException(e);
		}
	    }
	});
	
	AccelMap.changeEntry("<sourceWin>/Processes/Load executable file...",
		KeyValue.l, ModifierType.CONTROL_MASK, true);
	this.open_load.connectAccelerator();

	
	// Close action
	this.close = new Action("close", "Close", "Close Window",
		GtkStockItem.CLOSE.getString());
	this.close.setAccelGroup(ag);
	this.close.setAccelPath("<sourceWin>/File/Close");
	this.close.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		// SourceWindow.this.glade.getWidget(SOURCE_WINDOW).destroy();
		if (!SourceWindow.this.swProc[current].getClass().equals(
			LinuxCoreProc.class))
		    SourceWindow.this.steppingEngine.removeObserver(
			    SourceWindow.this.lock,
			    SourceWindow.this.swProc[current], true);
		SourceWindow.this.glade.getWidget(SOURCE_WINDOW).hide();
		WindowManager.theManager.sessionManagerDialog.showAll();
	    }
	});
	AccelMap.changeEntry("<sourceWin>/File/Close", KeyValue.x,
		ModifierType.CONTROL_MASK, true);
	this.close.connectAccelerator();

	// Copy action
	this.copy = new Action("copy", "Copy",
		"Copy Selected Text to the Clipboard", GtkStockItem.COPY
			.getString());
	this.copy.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
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
		"Find Text in the Current Buffer", GtkStockItem.FIND
			.getString());
	this.find.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
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
	this.prefsLaunch = new Action("prefs", "Frysk Preferences",
		"Edit Preferences", GtkStockItem.PREFERENCES.getString());
	this.prefsLaunch.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		SourceWindow.this.launchPreferencesWindow();
	    }
	});

	// Run program action
	this.run = new Action("run", "Run", "Run Program", "frysk-run");
	this.run.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
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
	this.stop.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent arg0) {
		SourceWindow.this.doStop();
	    }
	});
	this.stop.setSensitive(false);

	// Thread-specific starting and stopping
	this.toggleThreadDialog = new ToggleAction("threads",
		"Start/Stop Threads", "Start or Stop thread execution",
		"frysk-thread");
	this.toggleThreadDialog
		.addListener(new org.gnu.gtk.event.ActionListener() {
		    public void actionEvent(ActionEvent arg0) {
			SourceWindow.this.toggleThreadDialog();
		    }
		});

	this.toggleStepDialog = new ToggleAction("step",
		"Instruction Stepping",
		"Instruction stepping multiple threads", "frysk-thread");
	this.toggleStepDialog
		.addListener(new org.gnu.gtk.event.ActionListener() {
		    public void actionEvent(ActionEvent arg0) {
			SourceWindow.this.toggleStepDialog();
		    }
		});

	// Step action
	this.step = new Action("step", "Step", "Step", "frysk-step");
	this.step.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
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
	this.next = new Action("next", "Next", "Next", "frysk-next");
	this.next.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		SourceWindow.this.doNext();
	    }
	});
	this.next.setAccelGroup(ag);
	this.next.setAccelPath("<sourceWin>/Program/Next");
	AccelMap.changeEntry("<sourceWin>/Program/Next", KeyValue.n,
		ModifierType.MOD1_MASK, true);
	this.next.connectAccelerator();
	this.next.setSensitive(true);

	// Finish action
	this.finish = new Action("finish", "Finish", "Finish Function Call",
		"frysk-finish");
	this.finish.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		SourceWindow.this.doFinish();
	    }
	});
	this.finish.setAccelGroup(ag);
	this.finish.setAccelPath("<sourceWin>/Program/Finish");
	AccelMap.changeEntry("<sourceWin>/Program/Finish", KeyValue.f,
		ModifierType.MOD1_MASK, true);
	this.finish.connectAccelerator();
	this.finish.setSensitive(true);

	// Continue action
	this.cont = new Action("continue", "Continue", "Continue Execution",
		"frysk-continue");
	this.cont.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
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
	this.terminate.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
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
	this.stepAsm.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		SourceWindow.this.doAsmStep();
	    }
	});
	this.stepAsm.setAccelGroup(ag);
	this.stepAsm.setAccelPath("<sourceWin>/Program/Step Assembly");
	AccelMap.changeEntry("<sourceWin>/Program/Step Assembly", KeyValue.s,
		ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK), true);
	this.stepAsm.connectAccelerator();
	this.stepAsm.setSensitive(true);

	// Next assembly instruction action
	this.nextAsm = new Action("nextAsm", "Next Assembly Instruction",
		"Next Assembly Instruction", "frysk-nextAI");
	this.nextAsm.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		SourceWindow.this.doAsmNext();
	    }
	});
	this.nextAsm.setAccelGroup(ag);
	this.nextAsm.setAccelPath("<sourceWin>/Program/Next Assembly");
	AccelMap.changeEntry("<sourceWin>/Program/Next Assembly", KeyValue.n,
		ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK), true);
	this.nextAsm.connectAccelerator();
	this.nextAsm.setSensitive(true);

	// top of stack action
	this.stackTop = new Action("stackTop", "To top of Stack",
		"To top of Stack", "frysk-top");
	this.stackTop.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		SourceWindow.this.doStackTop();
	    }
	});
	this.stackTop.setAccelGroup(ag);
	this.stackTop.setAccelPath("<sourceWin>/Stack/Bottom");
	AccelMap.changeEntry("<sourceWin>/Stack/Bottom", KeyValue.Down,
		ModifierType.MOD1_MASK.or(ModifierType.SHIFT_MASK), true);
	this.stackTop.connectAccelerator();

	// Stack down action
	this.stackDown = new Action("stackDown", "Down One Stack Frame",
		"Down One Stack Frame", "frysk-down");
	this.stackDown.addListener(new org.gnu.gtk.event.ActionListener() {
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
	this.stackUp = new Action("stackUp", "Up One Stack Frame",
		"Up One Stack Frame", "frysk-up");
	this.stackUp.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		SourceWindow.this.doStackUp();
	    }
	});
	this.stackUp.setAccelGroup(ag);
	this.stackUp.setAccelPath("<sourceWin>/Stack/Up");
	AccelMap.changeEntry("<sourceWin>/Stack/Up", KeyValue.Up,
		ModifierType.MOD1_MASK, true);
	this.stackUp.connectAccelerator();

	// Run executable action
	this.open_executable = new Action("start executable",
		"Run a process...", "Run a process from a file",
		GtkStockItem.OPEN.getString());
	this.open_executable.setAccelGroup(ag);
	this.open_executable
		.setAccelPath("<sourceWin>/Processes/Run a process...");
	this.open_executable.addListener(new ActionListener() {
	    public void actionEvent(ActionEvent action) {
		try {
		    glade_fc = new LibGlade(Prefix.gladeFile(FILECHOOSER_GLADE).getAbsolutePath(), null);
		    fc = (FileChooserDialog) glade_fc
			    .getWidget("frysk_filechooserdialog");
		    fc.addListener(new LifeCycleListener() {
			public void lifeCycleEvent(LifeCycleEvent event) {
			}

			public boolean lifeCycleQuery(LifeCycleEvent event) {
			    if (event.isOfType(LifeCycleEvent.Type.DELETE)
				    || event
					    .isOfType(LifeCycleEvent.Type.DESTROY))
				fc.destroy();
			    return false;
			}
		    });

		    fc.addListener(new FileChooserListener() {
			public void currentFolderChanged(FileChooserEvent event) {
			}

			public void selectionChanged(FileChooserEvent event) {
			}

			public void updatePreview(FileChooserEvent event) {
			}

			// This method is called when the "Enter" key is pressed
                        // to
			// select a file name in the chooser
			public void fileActivated(FileChooserEvent event) {
			    activateProc();
			}
		    });
		    fc.setIcon(IconManager.windowIcon);
		    fc.setDefaultResponse(FileChooserEvent.Type.FILE_ACTIVATED
			    .getID());
		    fc.setCurrentFolder(System.getProperty("user.home"));
		    CheckButton term_activate = (CheckButton) glade_fc
			.getWidget("term_activate");
		    term_activate.setSensitive(false);
		    gtk_widget_set_size_request(fc.getHandle(), 300, 1000);
		    int response = fc.open();
		    // "OK" key has been clicked
		    if (response == ResponseType.OK.getValue())
			activateProc();
		    // "Cancel" key has been clicked
		    if (response == ResponseType.CANCEL.getValue())
			fc.destroy();
		} catch (Exception e) {
		    throw new RuntimeException(e);
		}
	    }
	});

	// Attach to a running process
	this.attach_proc = new Action("attach to process",
		"Attach to running process...",
		"Attach to process in cpu queue", GtkStockItem.FIND.getString());
	this.attach_proc.setAccelGroup(ag);
	this.attach_proc
		.setAccelPath("<sourceWin>/Processes/Attach to running process...");
	this.attach_proc.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		WindowManager.theManager.createFryskSessionDruid
			.presentProcLister();
	    }
	});
	this.attach_proc.setAccelGroup(ag);
	this.attach_proc
		.setAccelPath("<sourceWin>/Processes/Attach to running process...");
	AccelMap.changeEntry(
		"<sourceWin>/Processes/Attach to running process...",
		KeyValue.a, ModifierType.CONTROL_MASK, true);
	this.attach_proc.connectAccelerator();

	// Run executable action
	this.open_executable = new Action("start executable",
		"Run a process...", "Run a process from a file",
		GtkStockItem.OPEN.getString());
	this.open_executable.setAccelGroup(ag);
	this.open_executable
		.setAccelPath("<sourceWin>/Processes/Run a process...");
	this.open_executable.addListener(new ActionListener() {
	    public void actionEvent(ActionEvent action) {
		try {
		    glade_fc = new LibGlade(Prefix.gladeFile(FILECHOOSER_GLADE).getAbsolutePath(), null);
		    fc = (FileChooserDialog) glade_fc
			    .getWidget("frysk_filechooserdialog");
		    fc.addListener(new LifeCycleListener() {
			public void lifeCycleEvent(LifeCycleEvent event) {
			}

			public boolean lifeCycleQuery(LifeCycleEvent event) {
			    if (event.isOfType(LifeCycleEvent.Type.DELETE)
				    || event
					    .isOfType(LifeCycleEvent.Type.DESTROY))
				fc.destroy();
			    return false;
			}
		    });

		    fc.addListener(new FileChooserListener() {
			public void currentFolderChanged(FileChooserEvent event) {
			}

			public void selectionChanged(FileChooserEvent event) {
			}

			public void updatePreview(FileChooserEvent event) {
			}

			// This method is called when the "Enter" key is pressed
                        // to
			// select a file name in the chooser
			public void fileActivated(FileChooserEvent event) {
			    activateProc();
			}
		    });
		    fc.setIcon(IconManager.windowIcon);
		    fc.setDefaultResponse(FileChooserEvent.Type.FILE_ACTIVATED
			    .getID());
		    fc.setCurrentFolder(System.getProperty("user.home"));
		    int response = fc.open();
		    // "OK" key has been clicked
		    if (response == ResponseType.OK.getValue())
			activateProc();
		    // "Cancel" key has been clicked
		    if (response == ResponseType.CANCEL.getValue())
			fc.destroy();
		} catch (Exception e) {
		    throw new RuntimeException(e);
		}
	    }
	});

	// Attach to a running process
	this.attach_proc = new Action("attach to process",
		"Attach to running process...",
		"Attach to process in cpu queue", GtkStockItem.FIND.getString());
	this.attach_proc.setAccelGroup(ag);
	this.attach_proc
		.setAccelPath("<sourceWin>/Processes/Attach to running process...");
	this.attach_proc.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		WindowManager.theManager.createFryskSessionDruid
			.presentProcLister();
	    }
	});
	this.attach_proc.setAccelGroup(ag);
	this.attach_proc
		.setAccelPath("<sourceWin>/Processes/Attach to running process...");
	AccelMap.changeEntry(
		"<sourceWin>/Processes/Attach to running process...",
		KeyValue.a, ModifierType.CONTROL_MASK, true);
	this.attach_proc.connectAccelerator();

	toggleRegisterWindow = new ToggleAction("toggleRegWindow",
		"Register Window", "Toggle the Register Window", "");
	toggleRegisterWindow
		.addListener(new org.gnu.gtk.event.ActionListener() {
		    public void actionEvent(ActionEvent arg0) {
			SourceWindow.this.toggleRegisterWindow();
		    }
		});

	this.toggleMemoryWindow = new ToggleAction("toggleMemWindow",
		"Memory Window", "Toggle the Memory Window", "");
	this.toggleMemoryWindow
		.addListener(new org.gnu.gtk.event.ActionListener() {
		    public void actionEvent(ActionEvent arg0) {
			SourceWindow.this.toggleMemoryWindow();
		    }
		});

	this.toggleDisassemblyWindow = new ToggleAction("toggleDisWindow",
		"Disassembly Window", "Toggle the Disassembly Window", "");
	this.toggleDisassemblyWindow
		.addListener(new org.gnu.gtk.event.ActionListener() {
		    public void actionEvent(ActionEvent arg0) {
			SourceWindow.this.toggleDisassemblyWindow();
		    }
		});

	this.toggleConsoleWindow = new ToggleAction("toggleConWindow",
		"Console Window", "Toggle the Console Window", "");
	this.toggleConsoleWindow
		.addListener(new org.gnu.gtk.event.ActionListener() {
		    public void actionEvent(ActionEvent arg0) {
			SourceWindow.this.toggleConsoleWindow();
		    }
		});

	// Thread specific line stepping
	this.stepInDialog = new Action("StepIn", "Step into functions...",
		"Step One Line", "frysk-step");
	this.stepInDialog.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		SourceWindow.this.handleDialog(0);
	    }
	});

	// Thread specific stepping over
	this.stepOverDialog = new Action("StepOut", "Step over functions...",
		"Step Over Function", "frysk-next");
	this.stepOverDialog.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		SourceWindow.this.handleDialog(1);
	    }
	});

	// Thread specific stepping out
	this.stepOutDialog = new Action("StepOut", "Step out of functions...",
		"Step out of frame", "frysk-finish");
	this.stepOutDialog.addListener(new org.gnu.gtk.event.ActionListener() {
	    public void actionEvent(ActionEvent action) {
		SourceWindow.this.handleDialog(2);
	    }
	});

	// Thread specific instruction stepping
	this.stepInstructionDialog = new Action("StepInstruction",
		"Step single instructions...", "Step single instruction",
		"frysk-stepAI");
	this.stepInstructionDialog
		.addListener(new org.gnu.gtk.event.ActionListener() {
		    public void actionEvent(ActionEvent action) {
			SourceWindow.this.handleDialog(3);
		    }
		});

	// Thread specific instruction next stepping
	this.stepInstructionNextDialog = new Action("StepInstructionNext",
		"Step next instructions...", "Step next instruction",
		"frysk-nextAI");
	this.stepInstructionNextDialog
		.addListener(new org.gnu.gtk.event.ActionListener() {
		    public void actionEvent(ActionEvent action) {
			SourceWindow.this.handleDialog(4);
		    }
		});
    }

    /**
         * activateProc is called when the user has selected an executable from
         * the FileChooserDialog. It checks to see if a gnome terminal is to be
         * activated if so the selected process' STDIN/STDOUT/STDERR will be
         * assigned to it.
         * 
         */
    public void activateProc() {
	CheckButton term_activate = (CheckButton) glade_fc
		.getWidget("term_activate");
	Entry task_options = (Entry) glade_fc.getWidget("task_options");
	boolean term_active = term_activate.getState();
	String task_opt = task_options.getText();
	String filename = fc.getFilename();
	fc.destroy();
	String[] stds = { "/dev/null", "/dev/null", "/dev/null" };
	if (term_active)
	    stds = createTermWindow(filename);
	addProc(filename, "", task_opt, stds[0], stds[1], stds[2]);
    }

    /**
         * This method will activate a window to allow the user to examine a
         * core file
         * 
         * @param filename -
         *                String containing the path to the core file
         * 
         */
    private void examineCoreFile() {
	Entry task_options = (Entry) glade_fc.getWidget("task_options");
	String task_opt = task_options.getText();
	String filename = fc.getFilename();
	fc.destroy();
	String[] stds = { "/dev/null", "/dev/null", "/dev/null" };
	addProc(filename, "", task_opt, stds[0], stds[1], stds[2]);
	SourceWindowFactory.attachToCore(new File(filename));
    }
    
    /**
     * This method will activate a window to allow the user to load an
     * executable file
     * 
     * @param filename -
     *                String containing the path to the executable file
     * 
     */
    private void loadExecutableFile() {
	Entry task_options = (Entry) glade_fc.getWidget("task_options");
	String task_opt = task_options.getText();
	String filename = fc.getFilename();
	fc.destroy();
	String[] stds = { "/dev/null", "/dev/null", "/dev/null" };
	addProc(filename, "", task_opt, stds[0], stds[1], stds[2]);
	SourceWindowFactory.loadExecutable(new File(filename), null);
	//this.destroy();
}

    /**
         * Creates the toolbar menus with initialized Actions.
         */
    private void createMenus() {
	// File menu
	MenuItem menu = new MenuItem("File", true);

	/* MenuItem mi = (MenuItem) this.open_core.createMenuItem();
	Menu tmp = new Menu();
	tmp.append(mi);
	mi = (MenuItem) this.open_load.createMenuItem();
	tmp.append(mi);
	mi = new MenuItem(); // Separator
	tmp.append(mi); */
	Menu tmp = new Menu();
	MenuItem mi = (MenuItem) this.close.createMenuItem();
	tmp.append(mi);

	menu.setSubmenu(tmp);

	((MenuBar) this.glade.getWidget("menubar")).append(menu);

	// Edit Menu
	menu = new MenuItem("Edit", true);
	tmp = new Menu();

	mi = (MenuItem) this.copy.createMenuItem();
	tmp.append(mi);
	mi = new MenuItem(); // Separator
	tmp.append(mi);
	mi = (MenuItem) this.find.createMenuItem();
	tmp.append(mi);
	mi = new MenuItem(); // Separator
	tmp.append(mi);
	mi = (MenuItem) this.prefsLaunch.createMenuItem();
	tmp.append(mi);

	menu.setSubmenu(tmp);
	((MenuBar) this.glade.getWidget("menubar")).append(menu);

	// View Menu
	menu = new MenuItem("View", false);
	tmp = new Menu();

	mi = (MenuItem) this.toggleConsoleWindow.createMenuItem();
	tmp.append(mi);

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
	// mi = (MenuItem) this.toggleStepDialog.createMenuItem();
	// tmp.append(mi);
	mi = (MenuItem) this.step.createMenuItem();
	tmp.append(mi);
	mi = (MenuItem) this.next.createMenuItem();
	tmp.append(mi);
	mi = (MenuItem) this.finish.createMenuItem();
	tmp.append(mi);
	mi = (MenuItem) this.terminate.createMenuItem();
	tmp.append(mi);
	mi = new MenuItem(); // Separator
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

	menu = new MenuItem("Processes", false);
	tmp = new Menu();
	mi = (MenuItem) this.attach_proc.createMenuItem();
	tmp.append(mi);
	mi = new MenuItem(); // Separator
	tmp.append(mi);
	mi = (MenuItem) this.open_executable.createMenuItem();
	tmp.append(mi);
	mi = (MenuItem) this.open_core.createMenuItem();
	tmp.append(mi);
	mi = (MenuItem) this.open_load.createMenuItem();
	tmp.append(mi);

	menu.setSubmenu(tmp);
	((MenuBar) this.glade.getWidget("menubar")).append(menu);

	menu = new MenuItem("Threads", false);
	tmp = new Menu();

	mi = (MenuItem) this.stepInDialog.createMenuItem();
	tmp.append(mi);
	// mi = (MenuItem) this.stepOverDialog.createMenuItem();
	// tmp.append(mi);
	// mi = (MenuItem) this.stepOutDialog.createMenuItem();
	// tmp.append(mi);
	mi = (MenuItem) this.stepInstructionDialog.createMenuItem();
	tmp.append(mi);
	// mi = (MenuItem) this.stepInstructionNextDialog.createMenuItem();
	// tmp.append(mi);

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

    /**
         * Desensitizes all Action-related widgets on the window, except for the
         * 'stop' Action.
         */
    private void desensitize() {
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
	this.viewPicker.setSensitive(false);
    }

    /**
         * Resensitizes all Action-related widgets on the window, except for the
         * 'stop' Action.
         */
    private void resensitize() {
	// Set status of toolbar buttons
	this.glade.getWidget("toolbarGotoBox").setSensitive(true);
	this.glade.getWidget(SourceWindow.SOURCE_WINDOW).setSensitive(true);

	if (this.stepDialog != null)
	    this.stepDialog.resensitize();

	// Set status of actions
//	 this.run.setSensitive(true);
	this.stop.setSensitive(false);
	this.step.setSensitive(true);
	this.next.setSensitive(true);
	this.finish.setSensitive(true);
	this.cont.setSensitive(true);
	this.nextAsm.setSensitive(true);
	this.stepAsm.setSensitive(true);

	this.stepInDialog.setSensitive(true);
	// this.stepOverDialog.setSensitive(true);
	// this.stepOutDialog.setSensitive(true);
	this.stepInstructionDialog.setSensitive(true);
	// this.stepInstructionNextDialog.setSensitive(true);

	this.toggleDisassemblyWindow.setSensitive(true);
	this.toggleMemoryWindow.setSensitive(true);
	this.toggleRegisterWindow.setSensitive(true);

	this.stackTop.setSensitive(true);
	this.stackUp.setSensitive(true);
	this.stackDown.setSensitive(true);

	this.copy.setSensitive(true);
	this.find.setSensitive(true);
	this.prefsLaunch.setSensitive(true);
	this.viewPicker.setSensitive(true);
    }

    /**
         * Toggles whether the toolbar is visible
         * 
         * @param value
         *                Whether or not to show the toolbar
         */
    public void setShowToolbar(boolean value) {
	if (value)
	    this.glade.getWidget(SourceWindow.GLADE_TOOLBAR_NAME).showAll();
	else
	    this.glade.getWidget(SourceWindow.GLADE_TOOLBAR_NAME).hideAll();
    }

    /**
         * Adds icons, text, and tooltips to the widgets in the search bar
         */
    private void createSearchBar() {
	// we do this to ovewrite a bug (?) where when we set the label of the
	// button, the text disappears too
	((Button) this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND))
		.setImage(new Image(new GtkStockItem("frysk-highlight"),
			IconSize.BUTTON));
	((Button) this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND))
		.setLabel("Highlight All");

	// add Tooltips
	tips.setTip(this.glade.getWidget(SourceWindow.NEXT_FIND),
		"Find Next Match", "Locate the next occurrence in the file"); //$NON-NLS-1$ //$NON-NLS-2$
	tips
		.setTip(
			this.glade.getWidget(SourceWindow.PREV_FIND),
			"Find Previous Match", "Locate the previous occurrence in the file"); //$NON-NLS-1$ //$NON-NLS-2$
	tips.setTip(this.glade.getWidget(SourceWindow.HIGHLIGHT_FIND),
		"Highlight All Matches", "Locate all occurrences in the file"); //$NON-NLS-1$ //$NON-NLS-2$
	tips.setTip(this.glade.getWidget(SourceWindow.CLOSE_FIND),
		"Hide Find Window", "Close the find window"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
         * Assigns Listeners to the widgets that we need to listen for events
         * from
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

	// Text field in search bar
	((Entry) this.glade.getWidget(SourceWindow.FIND_TEXT))
		.addListener(listener);

	// function jump box
	((Entry) this.glade.getWidget("toolbarGotoBox")).addListener(listener);
	EntryCompletion completion = new EntryCompletion();
	completion.setInlineCompletion(false);
	completion.setPopupCompletion(true);
	DataColumn[] cols = { new DataColumnString() };
	ListStore store = new ListStore(cols);

	List funcs = this.view.getFunctions();
	for (int i = 0; i < funcs.size(); i++) {
	    TreeIter iter = store.appendRow();
	    store.setValue(iter, (DataColumnString) cols[0], (String) funcs
		    .get(i));
	}

	completion.setModel(store);
	completion.setTextColumn(cols[0].getColumn());
	((Entry) this.glade.getWidget("toolbarGotoBox"))
		.setCompletion(completion);

	((Entry) this.glade.getWidget("toolbarGotoBox"))
		.addListener(new MouseListener() {

		    public boolean mouseEvent(MouseEvent arg0) {
			if (arg0.isOfType(MouseEvent.Type.BUTTON_PRESS)
				|| arg0.getButtonPressed() == MouseEvent.BUTTON1) {

			    Entry source = (Entry) arg0.getSource();
			    source.selectRegion(0, source.getText().length());

			    return false;
			}
			return false;
		    }

		});

	// Mode box
	this.viewPicker.addListener(listener);
	this.viewPicker.setSensitive(true);

	// Stack browser
	this.stackView.addListener(listener);
	this.stackView.addListener(mouseListener);

	// Preferences
	((BooleanPreference) PreferenceManager.sourceWinGroup
		.getPreference(SourceWinPreferenceGroup.TOOLBAR))
		.addListener(new BooleanPreferenceListener() {
		    public void preferenceChanged(String prefName,
			    boolean newValue) {
			SourceWindow.this.setShowToolbar(newValue);
		    }
		});

    }

    /**
         * Displays the preference window, or creates it if it is the first time
         * this method is called
         */
    private void launchPreferencesWindow() {
	PreferenceWindow prefWin = null;
	try {
	    prefWin = new PreferenceWindow(new LibGlade(Prefix.gladeFile("frysk_source_prefs.glade").getAbsolutePath(), prefWin));
	} catch (GladeXMLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	prefWin.showAll();
    }

    private void showFindBox() {
	this.glade.getWidget(SourceWindow.FIND_BOX).showAll();
	this.glade.getWidget(SourceWindow.FIND_TEXT).grabFocus();
    }

    private void hideFindBox() {
	this.glade.getWidget(SourceWindow.FIND_BOX).hideAll();
    }

    private void gotoLine(int line) {
	this.view.scrollToLine(line);
    }

    private void doFindNext() {
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

    private void doFindPrev() {
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

    private void doHighlightAll() {
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

    /***********************************************************************
         * Display Mode Switching
         **********************************************************************/

    /**
         * Switches the SourceWindow to dislaying source-only information
         */
    private void switchToSourceMode() {
	/*
         * If we're switching from Assembly or Mixed mode, we can just toggle
         * the state.
         */
	if (this.view instanceof SourceView) {
	    ((SourceView) this.view).setLineNums(true);
	    ((SourceView) this.view).setMode(SourceBuffer.SOURCE_MODE);

	    if (this.currentFrame.getLine() != SourceLocation.UNKNOWN) {
		((SourceView) this.view).scrollToFunction(this.currentFrame
			.getSymbol().getDemangledName());
	    }
	}
	/*
         * If we're switching from Source/Assembly mode, we need to re-create
         * the source view widget
         */
	else {
	    ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
		    .remove(((ScrolledWindow) this.glade
			    .getWidget(SourceWindow.TEXT_WINDOW)).getChild());
	    this.view = new SourceView(this.view.getScope(), this);

	    ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
		    .add((Widget) this.view);
	    this.view.showAll();
	}

	createTags();
    }

    /**
         * Switches the SourceWindow to displaying assembly-only information.
         */
    private void switchToAsmMode() {
	removeTags();
	/*
         * If we're switching from Source or Mixed more, we can just toggle the
         * state
         */
	if (this.view instanceof SourceView) {
	    ((SourceView) this.view).setLineNums(false);
	    ((SourceView) this.view).setMode(SourceBuffer.ASM_MODE);
	}
	/*
         * If we're switching from Source/Assembly mode, we need to re-create
         * the source view widget
         */
	else {
	    ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
		    .remove(((ScrolledWindow) this.glade
			    .getWidget(SourceWindow.TEXT_WINDOW)).getChild());
	    this.view = new SourceView(this.view.getScope(), this,
		    SourceBuffer.ASM_MODE);

	    ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
		    .add((Widget) this.view);
	    this.view.showAll();
	}
    }

    /**
         * Switches the SourceWindow to displaying combined assembly and source
         * information.
         */
    private void switchToMixedMode() {
	/*
         * If we're switching from Source or Assembly we can just toggle the
         * state
         */
	if (this.view instanceof SourceView) {
	    ((SourceView) this.view).setMode(SourceBuffer.MIXED_MODE);
	}
	/*
         * If we're switching from Source/Assembly mode, we need to re-create
         * the source view widget
         */
	else {
	    ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
		    .remove(((ScrolledWindow) this.glade
			    .getWidget(SourceWindow.TEXT_WINDOW)).getChild());
	    this.view = new SourceView(this.view.getScope(), this);

	    ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
		    .add((Widget) this.view);
	    ((SourceView) this.view).setMode(SourceBuffer.MIXED_MODE);
	    this.view.showAll();
	}
    }

    /**
         * Has the SourceWindow split into two panes, one displaying source
         * code, the other displaying assembly information.
         */
    private void switchToSourceAsmMode() {
	if (this.currentFrame.getLine() == SourceLocation.UNKNOWN)
	    return;

	if (!(this.view instanceof MixedView)) {
	    // Replace the SourceView with a Mixedview to display
	    // Source/Assembly
	    // mode
	    ((Container) this.view.getParent()).remove((Widget) this.view);
	    this.view = new MixedView(this.view.getScope(), this);

	    ((ScrolledWindow) this.glade.getWidget(SourceWindow.TEXT_WINDOW))
		    .addWithViewport((Widget) this.view);
	    this.view.showAll();
	}
    }

    /***********************************************************************
         * Source Label
         **********************************************************************/

    /**
         * This method actually sets the sourceLabel widget with the info
         * provided.
         * 
         * @param task_name
         *                is a string containing the command used to start the
         *                task.
         * @param proc_id
         *                is an integer containing the PID of the task being
         *                displayed.
         * @param tid 
         * 		  is an integer with the thread id being displayed
         * @param noDOMFunction
         * 		  is a boolean indicating whether or not the parser
         *         	  could find the function the process is currently executing in
         * @param source  
         * 		  is the DOMSource of the source code being displayed
         * 
         * 
         */
    private void setSourceLabel(String header, String task, int pid, int tid,
	    boolean noDOMFunction, DOMSource source) {
	if (noDOMFunction && !(source == null))
	    ((Label) this.glade.getWidget("sourceLabel")).setText("<b>"
		    + header + task + " -- PID: " + pid + " -- TID: " + tid + "</b>");
		//    + " **** Parser could not find function ****" + "</b>");
	else
	    ((Label) this.glade.getWidget("sourceLabel")).setText("<b>"
		    + header + task + " -- PID: " + pid + " -- TID: " + tid
		    + "</b>");

	((Label) this.glade.getWidget("sourceLabel")).setUseMarkup(true);
    }

    /**
         * This method updates the label at the top of the debug window frame
         * whenever a stack frame is selected.
         * 
         * @param sf
         *                is the StackFrame that has been selected for viewing
         *                in the source frame.
         */
    private void updateSourceLabel(DebugInfoFrame sf) {
	boolean noDOMFunction = false;
	if (sf == null) {
	    String task_name = this.swProc[0].getExeFile().getSysRootedPath();
	    int proc_id = this.swProc[0].getPid();
	    setSourceLabel("Unknown File for: ", task_name, proc_id, 0,
		    false, null);
	    return;
	}

	((Label) this.glade.getWidget("sourceLabel")).setUseMarkup(true);
	String task_name = sf.getTask().getProc().getExeFile().getSysRootedPath();
	int proc_id = sf.getTask().getProc().getPid();
	int task_id = sf.getTask().getTid();

	DOMSource source = null;
	LineXXX line = sf.getLineXXX();

	if (sf.getLine() != SourceLocation.UNKNOWN) {
	    if (line.getDOMFunction() == null)
	        noDOMFunction = true;
	    source = line.getDOMSource();
	    if (source == null)
		try {
		    DOMFactory.createDOM(sf, sf.getTask().getProc());
		    source = line.getDOMSource();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}

	if (sf.getLine() == SourceLocation.UNKNOWN)
	    setSourceLabel("Unknown File for: ", task_name, proc_id, task_id, noDOMFunction, source);
	else if (source == null && sf.getLine() != SourceLocation.UNKNOWN)
	    setSourceLabel(sf.getLine().getFile().getPath() + " for: ",
		    task_name, proc_id, task_id, noDOMFunction, source);
	else
	    setSourceLabel(source.getFileName() + " for: ", task_name, proc_id,
		    task_id, noDOMFunction, source);
    }

    /***********************************************************************
         * Stack View Event Handling
         **********************************************************************/

    /**
         * Main logic for determining what to display after a new StackFrame is
         * selected from the stack view.
         * 
         * Depending on what is selected, will either load source from a new
         * file, scroll to a new line in the current file, display assembly
         * information, simply do nothing, or other actions dependant on
         * available frame information.
         * 
         * Also updates SymTab information, information displayed in the source
         * label, and current highlighting.
         * 
         * @param selected
         *                The selected StackFrame
         * @param current
         *                The index of the currently selected Proc
         */
    private void updateShownStackFrame(DebugInfoFrame selected, int current) {
	int mode = this.viewPicker.getActive();

	DOMSource source = null;
	LineXXX line = selected.getLineXXX();

	updateSourceLabel(selected);

	if (selected.getLine() != SourceLocation.UNKNOWN) {
	    source = line.getDOMSource();
	    if (source == null)
		try {
		    DOMFactory
			    .createDOM(selected, selected.getTask().getProc());
		    source = line.getDOMSource();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}

	if (selected.getLine() == SourceLocation.UNKNOWN) {
	    SourceBuffer b = null;

	    if (mode == 2)
		switchToAsmMode();

	    if (this.view instanceof SourceView)
		b = (SourceBuffer) ((SourceView) this.view).getBuffer();
	    else
		b = (SourceBuffer) ((MixedView) this.view).getSourceWidget()
			.getBuffer();

	    removeTags();
	    this.view.load(selected, this.viewPicker.getActive());

	    if (!this.steppingEngine.isTaskRunning(selected.getTask())) {
		if (this.stop.isSensitive())
		    resensitize();
		b.disassembleFrame(selected);
	    } else {
		if (!this.stop.isSensitive())
		    desensitize();
		b.deleteText(b.getStartIter(), b.getEndIter());
	    }
	} else if (source != null) {
	    if (this.currentFrame.getLine() == SourceLocation.UNKNOWN
		|| !source.getFileName()
			    .equals(
				    this.currentFrame.getLine().getFile()
					    .getName()) || mode != 0
		    || current != this.current) {

		this.view.load(selected, mode);

		boolean running = this.steppingEngine
			.isProcRunning(this.swProc[current].getTasks());

		if (current != this.current && !running)
		{
		    if (this.stop.isSensitive())
			resensitize();
		} else if (current != this.current && running) {
		    updateSourceLabel(this.currentFrame);

		    if (!this.stop.isSensitive())
			desensitize();
		}

		this.current = current;
		this.currentTask = selected.getTask();

		removeTags();

		createTags();

		if (this.currentFrame.getLine() == SourceLocation.UNKNOWN) {
		    if (mode == 2) {
			this.currentFrame = selected;
			switchToSourceAsmMode();
			if (line.getDOMFunction() != null)
			    ((MixedView) this.view).getSourceWidget()
				.scrollToFunction(
					line.getDOMFunction()
					.getFunctionCall());
		    } else if (mode == 0 && line.getDOMFunction() != null)
			this.view.scrollToFunction(line.getDOMFunction()
				.getFunctionCall());
		} else {
		    if (mode == 0 && line.getDOMFunction() != null) {
			this.view.scrollToFunction(line.getDOMFunction()
				.getFunctionCall());
		    }
		    else if (mode == 2)
			((MixedView) this.view).getSourceWidget().scrollToLine(
				line.getLine());
		}
	    } else {
		if (mode == 0)
		    this.view.scrollToLine(line.getLine());
		else if (mode == 2)
		    ((MixedView) this.view).getSourceWidget().scrollToLine(
			    line.getLine());
	    }
	}

	this.current = current;
	this.currentFrame = selected;
	this.view.showAll();
    }

    /**
         * Handles right-clicks on the stack view, signifying the user wishes to
         * perform some sort of Proc-removal operation.
         * 
         * @param event
         *                The generated MouseEvent
         */
    private void menuEvent(MouseEvent event) {
	Menu m = new Menu();
	MenuItem detachItem = new MenuItem("Detach process "
		+ this.swProc[this.current].getPid() + " from Frysk", false);
	m.append(detachItem);
	detachItem.setSensitive(true);
	detachItem.addListener(new MenuItemListener() {
	    public void menuItemEvent(MenuItemEvent arg0) {
		detachProc(false);
	    }
	});

	// MenuItem killItem = new MenuItem("Kill process " +
        // this.swProc[this.current].getPid(), false);
	// m.append(killItem);
	// killItem.setSensitive(true);
	// killItem.addListener(new MenuItemListener()
	// {
	// public void menuItemEvent (MenuItemEvent arg0)
	// {
	// detachProc(true);
	// }
	// });

	m.showAll();
	m.popup();
    }

    private void detachProc(boolean kill) {
	this.removeProc(kill);

	if (this.swProc.length == 0) {
	    ((Label) SourceWindow.this.glade.getWidget("sourceLabel"))
		    .setText("<b>" + "All processes have exited." + "</b>");
	    ((Label) SourceWindow.this.glade.getWidget("sourceLabel"))
		    .setUseMarkup(true);
	    SourceWindow.this.stackView.clear();
	    SourceBuffer b = (SourceBuffer) ((SourceView) view).getBuffer();
	    b.clear();
	    SourceWindow.this.desensitize();
	    SourceWindow.this.stop.setSensitive(false);
	} else
	    lock.update(null, new Object());
    }

    /***********************************************************************
         * Line Highlighting
         **********************************************************************/

    private void removeTags() {
	SourceBuffer sb = null;

	if (this.view instanceof SourceView)
	    sb = (SourceBuffer) ((SourceView) this.view).getBuffer();
	else
	    sb = (SourceBuffer) ((MixedView) this.view).getSourceWidget()
		    .getBuffer();
	
	if (this.frames.length > 0) {
	    for (int i = 0; i < this.frames[this.current].length; i++) {
		sb.highlightLine(this.frames[this.current][i], false);
	    }
	}
    }

    private void createTags() {
	SourceBuffer sb = null;

	if (this.view instanceof SourceView)
	    sb = (SourceBuffer) ((SourceView) this.view).getBuffer();
	else
	    sb = (SourceBuffer) ((MixedView) this.view).getSourceWidget()
		    .getBuffer();

	if (this.frames.length > 0) {
	    for (int i = 0; i < this.frames[this.current].length; i++) {
		sb.highlightLine(frames[this.current][i], true);
	    }
	}
    }

    /***********************************************************************
         * Action Handling Methods
         **********************************************************************/

    /**
         * Tells the debugger to run the program
         */
    private void doRun() {

    }

    private void doStop() {
	// Set status of toolbar buttons
	this.glade.getWidget("toolbarGotoBox").setSensitive(false);
	this.glade.getWidget(SourceWindow.VIEW_COMBO_BOX).setSensitive(false);

	this.glade.getWidget(SourceWindow.SOURCE_WINDOW).setSensitive(false);

	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stopped");

	if (this.threadDialog == null) {
	    this.steppingEngine
		    .stop(null, this.swProc[this.current].getTasks());
	} else {
	    this.steppingEngine.stop(this.threadDialog.getBlockTasks(),
		    this.threadDialog.getStopTasks());
	}
    }

    private void toggleThreadDialog() {
	if (this.threadDialog == null) {
	    this.threadDialog = new ThreadSelectionDialog(glade, this);
	    this.threadDialog.showAll();
	} else
	    this.threadDialog.showAll();
    }

    private void toggleStepDialog() {
	if (this.stepDialog == null) {
	    this.stepDialog = new StepDialog(glade, this);
	    this.stepDialog.showAll();
	} else
	    this.stepDialog.showAll();
    }

    /**
         * Tells the debugger to step the program
         */
    private synchronized void doStep() {
	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stepping");

	desensitize();

	if (this.steppingEngine.stepLine(this.currentTask))
	    removeTags();
    }

    protected void doStep(LinkedList tasks) {
	if (tasks.size() == 0)
	    return;

	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stepping");

	desensitize();

	if (this.steppingEngine.stepLine(tasks))
	    removeTags();
    }

    /**
         * "Step-over"
         */
    private void doNext() {
	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stepping Over");

	desensitize();
	LinkedList l = new LinkedList();
	l.add(this.currentTask);
	this.steppingEngine.stepOver(l);
	removeTags();
    }

    protected void doNext(LinkedList tasks) {
	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stepping Over");

	desensitize();

	this.steppingEngine.stepOver(tasks);
	removeTags();
    }

    /**
         * Tells the debugger to continue execution
         */
    private void doContinue() {
	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Running");

	desensitize();

	this.steppingEngine.continueExecution(this.swProc[this.current]
		.getTasks());

	removeTags();
    }

    /**
         * Tells the debugger to finish executing the current function "Step
         * out"
         */
    private void doFinish() {

	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stepping Out");

	desensitize();

	this.steppingEngine.stepOut(this.currentTask, DebugInfoStackFactory.createDebugInfoStackTrace(this.currentTask));
	removeTags();
    }

    protected void doFinish(LinkedList tasks) {

	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stepping Out");

	desensitize();

	this.steppingEngine.stepOut(tasks);
	removeTags();
    }

    protected void doAdvance() {
	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stepping to current frame");

	desensitize();

	this.steppingEngine.stepAdvance(this.currentTask, this.currentFrame);
	removeTags();
    }

    /**
         * Tells the debugger to terminate the program being debugged
         */
    private void doTerminate() {
	System.out.println("Terminate");
    }

    /**
         * Tells the debugger to step an assembly instruction
         */
    private synchronized void doAsmStep() {
	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stepping instruction");

	desensitize();

	if (this.steppingEngine.stepInstruction(this.currentTask))
	    removeTags();
    }

    /**
         * A request for an instruction step on one or more tasks.
         * 
         * @param tasks
         *                The list of tasks to step.
         */
    protected void doStepAsm(LinkedList tasks) {
	if (tasks.size() == 0)
	    return;

	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stepping");

	desensitize();

	if (this.steppingEngine.stepInstruction(tasks))
	    removeTags();
    }

    /**
         * Tells the debugger to execute the next assembly instruction
         */
    private void doAsmNext() {
	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stepping next instruction");

	desensitize();

	this.steppingEngine.stepNextInstruction(this.currentTask,
		this.currentFrame);
	removeTags();
    }

    protected void doAsmNext(LinkedList tasks) {
	if (tasks.size() == 0)
	    return;

	StatusBar sbar = (StatusBar) this.glade.getWidget("statusBar");
	sbar.push(0, "Stepping next instruction");

	desensitize();

	this.steppingEngine.stepNextInstruction(this.currentTask,
		this.currentFrame);
	removeTags();
    }

    /**
         * Tells the debugger to move to the previous stack frame
         */
    private void doStackUp() {
	TreePath path = null;
	try {
	    path = this.stackView.getSelection().getSelectedRows()[0];
	} catch (ArrayIndexOutOfBoundsException ae) {
	    return;
	}

	if (path.getDepth() == 3) {
	    if (!path.previous()) {
		return;
	    }

	    TreeIter iter = this.stackView.getModel().getIter(path);

	    if (iter == null) {
		this.stackUp.setSensitive(false);
		return;
	    }

	    this.stackView.getSelection().select(iter);
	    this.stackDown.setSensitive(true);
	}
    }

    /**
         * Tells the debugger to move to the following stack frame
         */
    private void doStackDown() {
	TreePath path = null;
	try {
	    path = this.stackView.getSelection().getSelectedRows()[0];
	} catch (ArrayIndexOutOfBoundsException ae) {
	    return;
	}

	if (path.getDepth() == 3) {
	    path.next();
	    TreeIter iter = this.stackView.getModel().getIter(path);
	    if (iter == null) {
		return;
	    }

	    this.stackView.getSelection().select(iter);
	}
    }

    /**
         * Tells the debugger to move to the newest stack frame
         */
    private void doStackTop() {
	TreePath path = null;
	try {
	    path = this.stackView.getSelection().getSelectedRows()[0];
	} catch (ArrayIndexOutOfBoundsException ae) {
	    return;
	}

	if (path.getDepth() == 3)
	    path.up();
	else
	    return;

	TreeIter iter = this.stackView.getModel().getIter(path);
	this.stackView.getSelection().select(iter.getFirstChild());
    }

    private void doJumpToFunction(String name) {
	this.view.scrollToFunction(name);
    }

    /**
         * Creates and toggles the display of the RegisterWindow.
         */
    private void toggleRegisterWindow() {
	RegisterWindow regWin = RegisterWindowFactory.regWin;
	if (regWin == null) {
	    RegisterWindowFactory.createRegisterWindow(swProc[this.current],
		    this.steppingEngine);
	    RegisterWindowFactory.setRegWin(swProc[this.current]);
	} else {
	    this.steppingEngine.addObserver(regWin.getLockObserver());
	    regWin.setClosed(false);
	    regWin.showAll();
	}
    }

    /**
         * Creates and toggles the display of the MemoryWindow.
         */
    private void toggleMemoryWindow() {
	MemoryWindow memWin = MemoryWindowFactory.memWin;
	if (memWin == null) {
	    MemoryWindowFactory.createMemoryWindow(swProc[this.current],
		    this.steppingEngine);
	    MemoryWindowFactory.setMemWin(swProc[this.current]);
	} else {
	    this.steppingEngine.addObserver(memWin.getLockObserver());
	    memWin.setClosed(false);
	    memWin.showAll();
	}
    }

    /**
         * Creates and toggles the display of the DisassemblyWindow.
         */
    private void toggleDisassemblyWindow() {
	DisassemblyWindow disWin = DisassemblyWindowFactory.disWin;
	if (disWin == null) {
	    DisassemblyWindowFactory.createDisassemblyWindow(
		    swProc[this.current], this.steppingEngine);
	    DisassemblyWindowFactory.setDisWin(swProc[this.current]);
	} else {
	    this.steppingEngine.addObserver(disWin.getLockObserver());
	    disWin.setClosed(false);
	    disWin.showAll();
	}
    }

    /**
         * Creates and toggles the display of the ConsoleWindow.
         */
    private void toggleConsoleWindow() {
	if (this.conWin == null)
	    this.conWin = new ConsoleWindow();
	else
	    this.conWin.showAll();
    }

    /**
         * Creates the TermWindow that a process will have STDIN/STDOUT/STDERR
         * assigned to
         */
    private String[] createTermWindow(String filepath) {
	this.termWin = new TermWindow();
	termHash.put(filepath, this.termWin);
	this.termWin.setWindowTitle(filepath);
	this.termWin.showAll();
	// Get the /dev/pts/?? pseudoterminal to point the new process to
	String std = this.termWin.getPts();
	String stds[] = { std, std, std };
	return stds;
    }

    /**
         * Creates and toggles the display of the thread dialog.
         */
    private void handleDialog(int type) {
	if (this.stepDialog == null) {
	    this.stepDialog = new StepDialog(glade, this);
	}

	this.stepDialog.setType(type);
	this.stepDialog.showAll();
    }

    private synchronized void executeTasks(LinkedList tasks) {
	this.steppingEngine.executeTasks(tasks);
    }

    /**
         * Generates a new stack trace for each of the Tasks belonging to the
         * given Proc. Updates their DOM and SymTab information.
         * 
         * @param proc
         *                The Proc to be updated
         * @param current
         *                The new Proc array index
         * @return frames The new StackFrame[] stack trace
         */
    private DebugInfoFrame[] generateProcStackTrace(Proc proc, int current) {
	int size = proc.getTasks().size();
	Task[] tasks = new Task[size];
	DebugInfoFrame[] frames = new DebugInfoFrame[size];

	Iterator iter = proc.getTasks().iterator();
	int k = 0;

	/* Copy the list of Tasks into an array */
	while (iter.hasNext()) {
	    tasks[k] = (Task) iter.next();
	    ++k;
	}

	frames = new DebugInfoFrame[size];

	for (int j = 0; j < size; j++) {
	    /** Create the stack frame * */
	    DebugInfoFrame curr = null;
	    try {
		frames[j] = DebugInfoStackFactory.createDebugInfoStackTrace(tasks[j]);
		curr = frames[j];
	    } catch (Exception e) {
		System.out.println("Error generating stack trace");
		e.printStackTrace();
	    }

	    /* Update SymTab information for the main Task. */

	    /* Create a DOM for the Proc */
	    if (this.dom != null) {
		while (curr != null && this.dom[this.current] == null) {
		    if (this.dom[this.current] == null) {
			try {
			    this.dom[this.current] = DOMFactory.createDOM(curr,
				    this.swProc[this.current]);
			}

			catch (IOException e) {
			}
		    } else
			break;

		    curr = curr.getOuterDebugInfoFrame();
		}
	    }
	}

	/* Clear out any irrelevant DOM information from the last stack trace */
	DOMFactory.clearDOMSourceMap(this.swProc[this.current]);

	return frames;
    }

    /***********************************************************************
         * SourceWindow Listener And Observer Implementations
         **********************************************************************/

    private class SourceWindowListener implements ButtonListener,
	    EntryListener, ComboBoxListener, StackViewListener {

	private SourceWindow target;

	public SourceWindowListener(SourceWindow target) {
	    this.target = target;
	}

	public void buttonEvent(ButtonEvent event) {
	    if (!event.isOfType(ButtonEvent.Type.CLICK))
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

	public void entryEvent(EntryEvent event) {
	    // Search box in the find bar
	    if (((Widget) event.getSource()).getName().equals("findText")) {
		if (event.isOfType(EntryEvent.Type.DELETE_TEXT))
		    target.resetSearchBox();
		else if (event.isOfType(EntryEvent.Type.CHANGED))
		    target.doFindNext();
	    }
	    // Magic goto box in the toolbar
	    else {
		// user had to hit enter to do anything
		if (!event.isOfType(EntryEvent.Type.ACTIVATE))
		    return;

		Entry source = (Entry) event.getSource();

		String text = source.getText();
		boolean isNum = true;
		int value = -1;

		try {
		    if (text.indexOf("line ") == 0) {
			text = text.split("line ")[1];
		    }
		    value = Integer.parseInt(text);
		}
		// didn't work, we have to try to parse the text
		catch (NumberFormatException ex) {
		    isNum = false;
		    // Since we might have screwed around with this, reset
                        // it
		    text = source.getText();
		}

		// goto line
		if (isNum) {
		    target.gotoLine(value);
		} else {
		    target.doJumpToFunction(text);
		}
	    }
	}

	public void comboBoxEvent(ComboBoxEvent event) {
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
                 * Switch to Source/Assembly mode - we only need to worry about
                 * this case if we're switching from Source, Assembly, or Mixed
                 * view. If we were previously in Source/Assembly view we don't
                 * need to do anything
                 */
	    else if (text.equals("Source/Assembly"))
		target.switchToSourceAsmMode();

	}

	/**
	 * Notified from the CurrentStackView's listeners in the event of a new
	 * stack frame selection from the tree in that window.
	 * 
	 * @param newFrame  The new frame that was selected
	 * @param current   The process number that the frame was selected from
	 */
	public void currentStackChanged(DebugInfoFrame newFrame, int current) {
	    if (newFrame == null)
		return;

	    SourceWindow.this.fi = newFrame.getFrameIdentifier();

	    if (SourceWindow.this.currentTask == null
		    || newFrame.getTask().getTid() != SourceWindow.this.currentTask
			    .getTid())
		SourceWindow.this.currentTask = newFrame.getTask();

	    /* Make sure that a frame from a running task wasn't selected. */
	    if (!SourceWindow.this.steppingEngine.isTaskRunning(newFrame
		    .getTask())) {
		if (SourceWindow.this.currentFrame != null
		    && !(SourceWindow.this.currentFrame.getFrameIdentifier()
			 .equals(newFrame.getFrameIdentifier()))) {
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
	    }

	    stackDown.setSensitive(true);
	    stackUp.setSensitive(true);

	    target.updateShownStackFrame(newFrame, current);
	}
    }

    /**
         * Listens for mouse right-clicks on the stack view, and eventually
         * generates a menu for the user.
         */
    private class StackMouseListener implements MouseListener {
	public boolean mouseEvent(MouseEvent event) {
	    if (!event.isOfType(MouseEvent.Type.BUTTON_PRESS))
		return false;

	    if (event.getButtonPressed() == MouseEvent.BUTTON3) {
		menuEvent(event);
	    }

	    return false;
	}
    }

    private boolean SW_add = false;

    /**
         * Local Observer class used to poke this window from RunState when all
         * the Tasks belonging to this window's Proc have been blocked. These
         * Tasks could have ben running, stepping, or neither and were just
         * blocked once to allow this window to finish building. This observer
         * is synchronized between this windowand the Memory, Register, and
         * Disassembly windows.
         */
    private class LockObserver implements Observer {

	/**
	 * Builtin Observer method - called whenever the Observable we're
	 * concerned with - in this case the SteppingEngine - has changed.
	 * 
	 * @param o
	 *                The Observable we're watching
	 * @param arg
	 *                A TaskStepEngine with information about the current
	 *                Task stepping state.
	 */
	public void update(Observable o, Object arg) {

	    TaskStepEngine tse = (TaskStepEngine) arg;
	    
	    /* For some reason a task the window was dealing with has
	     * mysteriously died. Here, push a message out to the sourceLabel
	     * widget with information on the event. If it was the one and only
	     * Task and Proc being watched, clean up everything. */ 
	    if (!tse.isAlive()) {
		LinkedList tasks = SourceWindow.this.swProc[SourceWindow.this.current]
			.getTasks();
		
		((Label) SourceWindow.this.glade.getWidget("sourceLabel"))
		.setText("<b>" + tse.getMessage() + "</b>");

		if (tasks.contains(tse.getTask()) && tasks.size() == 1) {

		    removeProc(false);
		    SW_add = false;

		    if (swProc.length == 0)
			allProcsExited();

		    return;
		}
	    }

	    /* The updated task isn't dead, but it also isn't stopped, meaning
	     * it is useless to bother doing anything with it. */
	    if (!tse.getState().isStopped())
		return;

	    if (SW_active) {
		if (SW_add == false) {
		    /*
		     * This callback was called because all our Proc's Tasks
		     * were blocked because of some state change operation.
		     * Re-generate the stack trace information and refresh
		     * the window.
		     */
		    CustomEvents.addEvent(new Runnable() {
			public void run() {
			    SourceWindow.this.frames[SourceWindow.this.current] = generateProcStackTrace(
				    SourceWindow.this.swProc[SourceWindow.this.current],
				    SourceWindow.this.current);
			    populateStackBrowser(SourceWindow.this.frames);
			    procReblocked();
			}
		    });
		} else {
		    appendProc(tse.getTask());
		}
	    } else {
		/*
		 * The very first time all the Tasks are blocked is when we're
		 * initializing this window.
		 */
		SW_active = true;

		CustomEvents.addEvent(new Runnable() {
		    public void run() {
			finishSourceWin();
		    }
		});
		return;
	    }
	}
    }

}
