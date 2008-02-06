// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008 Red Hat Inc.
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

package frysk.hpd;

import frysk.debuginfo.ObjectDeclarationSearchEngine;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.WeakHashMap;
import frysk.debuginfo.DebugInfo;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rt.ProcTaskIDManager;
import frysk.stepping.SteppingEngine;
import frysk.stepping.TaskStepEngine;
import frysk.util.CountDownLatch;
import frysk.expr.Expression;
import frysk.expr.ScratchSymTab;
import frysk.expr.ExprSymTab;
import frysk.expr.ExpressionFactory;

public class CLI {
    ProcTaskIDManager idManager;
    SteppingObserver steppingObserver;
    SteppingEngine steppingEngine;
    int attached = -1;
    CountDownLatch attachedLatch;
    //Processes started with run command
    final HashSet runningProcs = new HashSet();
    //Processes loaded with load command
    final HashMap loadedProcs = new HashMap();
    //Processes loaded with core command
    final HashMap coreProcs = new HashMap(); 
    //Task ID to use
    int taskID = -1;

    private class TaskInfo {
        DebugInfoFrame frame;
        DebugInfo debugInfo;
    }

    WeakHashMap taskInfoMap = new WeakHashMap();
   
    DebugInfoFrame getTaskFrame(Task task) {
        TaskInfo taskInfo = (TaskInfo)taskInfoMap.get(task);
        if (taskInfo == null)
            return null;
        else
            return taskInfo.frame;
    }

    void setTaskFrame(Task task, DebugInfoFrame frame) {
        TaskInfo taskInfo = (TaskInfo)taskInfoMap.get(task);
        if (taskInfo == null) {
            taskInfo = new TaskInfo();
            taskInfoMap.put(task, taskInfo);
        }
        taskInfo.frame = frame;
    }

    DebugInfo getTaskDebugInfo(Task task) {
        TaskInfo taskInfo = (TaskInfo)taskInfoMap.get(task);
        if (taskInfo == null)
            return null;
        else
            return taskInfo.debugInfo;
    }

    void setTaskDebugInfo(Task task, DebugInfo debugInfo) {
        TaskInfo taskInfo = (TaskInfo)taskInfoMap.get(task);
        if (taskInfo == null) {
            taskInfo = new TaskInfo();
            taskInfoMap.put(task, taskInfo);
        }
        taskInfo.debugInfo = debugInfo;
    }

    /**
     * Handle ConsoleReader Completor
     * @param buffer Input buffer.
     * @param cursor Position of TAB in buffer.
     * @param candidates List that may complete token.
     * @return cursor position in buffer
     */
    public int complete (String buffer, int cursor, List candidates) {
	try {
	    return topLevelCommand.complete(this, new Input(buffer), cursor,
					    candidates);
	} catch (RuntimeException e) {
	    if (nasty(e))
		e.printStackTrace(outWriter);
	    return -1;
	}
    }

    /*
     * Command handlers
     */
    public void doAttach(Proc proc, boolean running) {
        synchronized (this) {
            attached = -1;
            attachedLatch = new CountDownLatch(1);
        }
        steppingEngine.addProc(proc);
	// Wait till we are attached.
        while (true) {
            try {
                attachedLatch.await();
                outWriter.print("Attached to process ");
                outWriter.println(attached);
                synchronized (this) {
                    attached = -1;
                    attachedLatch = null;
                }
                break;
            } catch (InterruptedException ie) {
            }            
        }

        steppingEngine.getBreakpointManager().manageProcess(proc);
        
        // If doAttach was called for a "run" command, continue running
        if (running) {
            steppingEngine.continueExecution(proc.getTasks());
            steppingEngine.setRunning(proc.getTasks());
            addMessage("Running process " + proc.getPid(),
			Message.TYPE_NORMAL);
        }
        // If passed a taskID < 0, request a reserved ProcID
        if (this.taskID < 0)
            idManager.manageProc(proc, idManager.reserveProcID());
        // Assign this proc to the passed in procID 
        else
            idManager.manageProcSelect(proc, this.taskID);
    }

    final PrintWriter outWriter;
    private Preprocessor prepro;
    private String prompt; // string to represent prompt, will be moved
    private final Command topLevelCommand = new TopLevelCommand();
    final DbgVariables dbgvars = new DbgVariables();

    // PT set related stuff
    private SetNotationParser setparser;
    private AllPTSet allset; // the "all" set
    final HashMap namedPTSets; // user-created named sets
    final HashMap builtinPTSets; // predefined named sets
    PTSet targetset;

    // other
    // debugger output messages, e.g. the Message class
    private LinkedList messages;

    // alias
    final HashMap aliases;

    /*
     * Public methods
     */
    Expression parseExpression(Task task, String expression) {
	ExprSymTab symTab;
	if (task == null) {
	    symTab = new ScratchSymTab();
	} else {
	    DebugInfoFrame frame = getTaskFrame(task);
	    symTab = new ObjectDeclarationSearchEngine(frame);
	}
	return ExpressionFactory.parse(symTab, expression);
    }
 
    /**
     * Constructor
     * @param prompt String initially to be used as the prompt
     * @param out Stream for output. This really should be a PrintWriter
     * @param steppingEngine existing SteppingEngine
     */
    public CLI(String prompt, PrintStream out, SteppingEngine steppingEngine) {
        this.prompt = prompt;
        outWriter = new PrintWriter(out, true);
        this.steppingEngine = steppingEngine;
        idManager = ProcTaskIDManager.getSingleton();

        prepro = new Preprocessor();

        // initialize PT set stuff
        setparser = new SetNotationParser();

        allset = new AllPTSet(this);
        targetset = allset;

        builtinPTSets = new HashMap();
        builtinPTSets.put("all", allset);

        namedPTSets = new HashMap();
        namedPTSets.toString(); // avoid unused variable warnings

        messages = new LinkedList();

        //initialize alias table
        aliases = new HashMap();
        aliases.toString(); // avoid unused variable warnings

        steppingObserver = new SteppingObserver();
        this.steppingEngine.addObserver(steppingObserver);
    }

    /**
     * Constructor that creates a new steppingEngine
     * @param prompt String initially to be used as the prompt
     * @param out Stream for output. This really should be a PrintWriter
     */
    public CLI(String prompt, PrintStream out) {
        this(prompt, out, new SteppingEngine());
    }
   
    public String getPrompt() {
        return prompt;
    }

    public String execCommand(String cmd) {
	if (cmd != null) {
	    // NULL when EOF.
	    try {
		// preprocess and iterate
		for (Iterator iter = prepro.preprocess(cmd); iter.hasNext();) {
		    String pcmd = (String)iter.next();
		    Input command = new Input(pcmd);
		    // Ignore empty commands
		    if (command.size() > 0)
			topLevelCommand.interpret(this, command);
		}
	    } catch (RuntimeException e) {
		printError(e);
	    }
	}
	flushMessages();
	return null;
    }
    
    /**
     * Identify "nasty", or internal exceptions; these are the
     * RuntimeExceptions thrown by the Java system.
     */
    private boolean nasty(Exception e) {
	Throwable cause = e;
	while (true) {
	    Throwable c = cause.getCause();
	    if (c == null)
		break;
	    cause = c;
	}
	return (cause instanceof NullPointerException
		|| cause instanceof ArrayIndexOutOfBoundsException
		|| e.getMessage() == null);
    }

    void printError(Exception e) {
	if (nasty(e)) {
	    outWriter.print("Internal Error: ");
	    e.printStackTrace(outWriter);
	    outWriter.println();
	} else {
	    outWriter.print("Error: ");
	    outWriter.println(e.getMessage());
	}
    }

    void addMessage(Message msg) {
	synchronized (messages) {
	    messages.add(msg);
	}
    }

    void addMessage(String msg, int type) {
        addMessage(new Message(msg, type));
    }

    private void flushMessages() {
	synchronized (messages) {
	    for (Iterator iter = messages.iterator(); iter.hasNext();) {
		Message tempmsg = (Message) iter.next();
		String prefix = null;
		if (tempmsg.getType() == Message.TYPE_DBG_ERROR)
		    prefix = "Internal debugger error:  ";
		else if (tempmsg.getType() == Message.TYPE_ERROR)
		    prefix = "Error: ";
		else if (tempmsg.getType() == Message.TYPE_WARNING)
		    prefix = "Warning: ";
		if (prefix != null)
		    outWriter.print(prefix);
		outWriter.println(tempmsg.getMessage());
		iter.remove();
	    }
	}
    }

    PTSet createSet(String set) {
        ParsedSet parsed = setparser.parse(set);
        PTSet result = null;

        if (parsed.getType() == ParsedSet.TYPE_STATE) {
            int state = 0;
            if (parsed.getName().equals("running"))
                state = AllPTSet.TASK_STATE_RUNNING;
            else if (parsed.getName().equals("stopped"))
                state = AllPTSet.TASK_STATE_STOPPED;
            else if (parsed.getName().equals("runnable"))
                state = AllPTSet.TASK_STATE_RUNNABLE;
            else if (parsed.getName().equals("held"))
                state = AllPTSet.TASK_STATE_HELD;
            else {
                throw new RuntimeException("Illegal state name when creating set.");
            }
            if (parsed.isStatic())
                result = new StaticPTSet(allset.getSubsetByState(state));
            else
                result = new StatePTSet(allset, state);
            addMessage("Creating new " + parsed.getName() + " state set.",
                       Message.TYPE_VERBOSE);
        }
        else if (parsed.getType() == ParsedSet.TYPE_HPD) {
            if (parsed.isStatic())
                result
                    = new StaticPTSet(allset.getSubset(parsed.getParseTreeNodes()));
            else
                result = new DynamicPTSet(allset, parsed.getParseTreeNodes());

            addMessage("Creating new HPD notation set.", Message.TYPE_VERBOSE);
        }
        else if (parsed.getType() == ParsedSet.TYPE_NAMED) {
            if (parsed.isStatic()) {
                addMessage("Cannot create a static set from a predefined set.",
                           Message.TYPE_ERROR);
            }
            else {
                addMessage("Creating new set from named set \""
                           + parsed.getName() + "\".", Message.TYPE_VERBOSE);
                result = (PTSet) namedPTSets.get(parsed.getName());
            }

        }
        else if (parsed.getType() == ParsedSet.TYPE_EXEC) {
            if (parsed.isStatic()) {
                result = new StaticPTSet(allset.getSubsetByExec(parsed.getName()));
            }
            else {
                result = new ExecPTSet(allset, parsed.getName());
            }
            addMessage("Creating new set from executable \"" + parsed.getName()
                       + "\".", Message.TYPE_VERBOSE);
        }
        return result;
    }
 
    class SteppingObserver
        implements Observer {
        private Object monitor = new Object();

        public Object getMonitor () {
            return this.monitor;
        }

        public void update (Observable observable, Object arg) {
            TaskStepEngine tse = (TaskStepEngine) arg;
            if (!tse.isAlive()) {
		addMessage(tse.getMessage(), Message.TYPE_VERBOSE);
		tse.setMessage("");
		flushMessages();

		synchronized (CLI.this) {
		    synchronized (this.monitor) {
			this.monitor.notifyAll();
		    }
		    CLI.this.notifyAll();
		}
		return;
	    }
     
            if (! tse.getState().isStopped()) {
                attached = -1;
                return;
            }
            Task task = tse.getTask();
            synchronized (CLI.this) {
                DebugInfoFrame frame
                    = DebugInfoStackFactory.createVirtualStackTrace(task);
                setTaskFrame(task, frame);
                setTaskDebugInfo(task, new DebugInfo(frame));
                // XXX Who's waiting on that monitor at this point?
                synchronized (this.monitor) {
                    this.monitor.notifyAll();
                }
                if (attachedLatch != null) {
                    // Notify tasks waiting on attach
                    attached = task.getProc().getPid();
                    attachedLatch.countDown();
                }
            }
        }
    }

    /**
     * Return output writer.
     */
    public PrintWriter getPrintWriter() {
        return outWriter;
    }

    /**
     * Get the set of processes (Proc) started by the run command. Access to the
     * CLI object should be synchronized when using the set.
     * @return the set
     */
    public HashSet getRunningProcs() {
        return runningProcs;
    }
 
    /**
     * Get the set of processes (Proc) started by the load command. Access to the
     * CLI object should be synchronized when using the set.
     * @return the set
     */
    public HashMap getLoadedProcs() {
        return loadedProcs;
    }
    
    /**
     * Get the set of processes (Proc) started by the core command. Access to the
     * CLI object should be synchronized when using the set.
     * @return the set
     */
    public HashMap getCoreProcs() {
        return coreProcs;
    }
    
    SteppingEngine getSteppingEngine () {
        return this.steppingEngine;
    }

    public PTSet getCommandPTSet(Input cmd) {
        String setString = cmd.getSet();
        PTSet ptset = null;
        if (setString == null) {
            ptset = targetset;
        } else {
            ptset = createSet(setString);
        }
        return ptset;
    }
    
    /**
     * notRunningFile make sure we are not running the "go" command on 
     * procs that are loaded or on core files.
     * 
     * @param checkFiles is a HashMap containing the procs we want to check
     * @param task is what we need to check against
     * @return true if it is a loaded or core file, false if not
     */
    public static boolean notRunningProc(int pid, HashMap checkFiles) {
	if (checkFiles.isEmpty())
	    return false;
	Set procSet = checkFiles.entrySet();
	Iterator foo = procSet.iterator();
	while (foo.hasNext()) {
	    Map.Entry me = (Map.Entry) foo.next();
	    Proc proc = (Proc) me.getKey();
	    if (proc.getPid() == pid)
		return true;
	}
	return false;
    }
}
