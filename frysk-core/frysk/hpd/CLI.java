// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.WeakHashMap;
import frysk.debuginfo.DebugInfo;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfoStackFactory;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.Host;
import frysk.rt.ProcTaskIDManager;
import frysk.stepping.SteppingEngine;
import frysk.stepping.TaskStepEngine;
import frysk.util.CountDownLatch;
import frysk.value.Value;

public class CLI {
    ProcTaskIDManager idManager;
    SteppingObserver steppingObserver;
    SteppingEngine steppingEngine;
    int attached = -1;
    CountDownLatch attachedLatch;
    //Processes started with run command
    final HashSet runningProcs = new HashSet();

    private class TaskInfo {
        DebugInfoFrame frame;
        DebugInfo debugInfo;
        int stackLevel;
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

    int getTaskStackLevel(Task task) {
        TaskInfo taskInfo = (TaskInfo)taskInfoMap.get(task);
        if (taskInfo == null)
            return 0;
        else
            return taskInfo.stackLevel;
    }

    void setTaskStackLevel(Task task, int stackLevel) {
        TaskInfo taskInfo = (TaskInfo)taskInfoMap.get(task);
        if (taskInfo == null) {
            taskInfo = new TaskInfo();
            taskInfoMap.put(task, taskInfo);
        }
        taskInfo.stackLevel = stackLevel;
    }
   
    /**
     * Handle ConsoleReader Completor
     * @param buffer Input buffer.
     * @param cursor Position of TAB in buffer.
     * @param candidates List that may complete token.
     * @return cursor position in buffer
     */
    public int complete (String buffer, int cursor, List candidates) {
        int first_ws = buffer.indexOf(' ');
        int offset = 0;
        // Complete the request name for help
        if (buffer.startsWith("help "))
            offset = 5;
        // Complete the request name or help request
        if (first_ws == -1 || offset > 0) {
            Set commands = handlers.keySet();
            Iterator it = commands.iterator();
            while(it.hasNext()) {
                String command = (String)it.next();
                if (command.startsWith(buffer.substring(offset)))
                    candidates.add(command + " ");
            }
            java.util.Collections.sort(candidates);
        }
        // Otherwise assume a symbol is being completed
        else {
            // XXX We should support the p/t set specified in the current
            // command, if any.
            Iterator taskIterator = targetset.getTasks();
            int newCursor = -1;

            while (taskIterator.hasNext()) {
                Task task = (Task)taskIterator.next();
                DebugInfoFrame frame = getTaskFrame(task);
                DebugInfo debugInfo = getTaskDebugInfo(task);
                if (debugInfo != null)
                    newCursor = debugInfo.complete(frame, buffer.substring(first_ws),
                                                   cursor - first_ws, candidates);
            }
            if (newCursor >= 0)
                return newCursor + first_ws;
        }
        return 1 + offset;
    }

    /*
     * Command handlers
     */

    public void doAttach(int pid, Proc proc, Task task) {
        Proc[] temp = new Proc[1];
        temp[0] = proc;
        synchronized (this) {
            attached = -1;
            attachedLatch = new CountDownLatch(1);
        }
        steppingEngine.addProc(proc);
                // Wait till we are attached.
        try {
            attachedLatch.await();
            addMessage("Attached to process " + attached, Message.TYPE_NORMAL);
        }
        catch (InterruptedException ie) {
            addMessage("Attach interrupted.", Message.TYPE_ERROR);
            return;
        }
        finally {
            synchronized (this) {
                attached = -1;
                attachedLatch = null;
            }
        }
    }

    public void doAttach(Task task) {
        Proc proc = task.getProc();
        doAttach(proc.getPid(), proc, task);
    }
    
    //private static PrintStream out = null;// = System.out;
    final PrintWriter outWriter;
    private Preprocessor prepro;
    private String prompt; // string to represent prompt, will be moved
    final HashMap handlers;
    final UserHelp userhelp;
    private DbgVariables dbgvars;

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

    /**
     * Add a CLIHandler, along with its help messages.
     * @param handler the handler
     */
    public void addHandler(CLIHandler handler) {
        String name = handler.getName();
        handlers.put(name, handler);
        userhelp.addHelp(name, handler.getHelp());
    }
 
    Value parseValue(Task task, String value) throws ParseException {
	return parseValue(task, value, false);
    }

    Value parseValue(Task task, String value, boolean dumpTree) throws ParseException {
        DebugInfoFrame frame = getTaskFrame(task);
        DebugInfo debugInfo = getTaskDebugInfo(task);
        if (debugInfo != null)
            return debugInfo.print(value, frame, dumpTree);
        else
            return DebugInfo.printNoSymbolTable(value, dumpTree);       
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
        handlers = new HashMap();
        userhelp = new UserHelp();
        dbgvars = new DbgVariables();
   
        //XXX: Must make a reference to every command that is used otherwise build
        //system will discard those classes. Therefore CLI cannot be made to be a
        //singleton.
        new ActionsCommand(this);
        new AliasCommand(this);
        new AssignCommand(this);
        new AttachCommand(this);
        new BreakpointCommand(this);
        new DebuginfoCommand(this);
        new DefsetCommand(this);
        new DeleteCommand(this);
        new DetachCommand(this);
        new DisableCommand(this);
        new FrameCommands(this, "down");
        new EnableCommand(this);
        new StepFinishCommand(this);
        new FocusCommand(this);
        new GoCommand(this);
        new HaltCommand(this);
        new HelpCommand(this);
        new ListCommand(this);
        new StepNextCommand(this);
        new StepNextiCommand(this);
        new PrintCommand(this);
        new PlocationCommand(this);
        new PtypeCommand(this);
        new QuitCommand(this, "quit");
        new QuitCommand(this, "exit");
        new SetCommand(this, dbgvars);
        new StepCommand(this);
        new StepInstructionCommand(this);
        new UnaliasCommand(this);
        new UndefsetCommand(this);
        new UnsetCommand(this, dbgvars);
        new FrameCommands(this, "up");
        new ViewsetCommand(this);
        new WhatCommand(this);
        new WhereCommand(this);
        new WhichsetsCommand(this);
        new DisplayCommand(this);
        new RunCommand(this);
        new CoreCommand(this);
        new DisassembleCommand(this);
        new RegsCommand(this);
        new ExamineCommand(this);
        new LoadCommand(this);
        new PeekCommand(this);

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
        String pcmd = ""; // preprocessed command
        Command command;
        CommandHandler handler = null;

        if (cmd != null) {
            try {
                // preprocess and iterate
                for (Iterator iter = prepro.preprocess(cmd); iter.hasNext();) {
                    pcmd = (String)iter.next();
                    command = new Command(pcmd);

                    if (command.getAction() != null) {
                        handler = (CommandHandler)handlers.get(command.getAction());
                        if (handler != null)
                            handler.handle(command);
                        else
                            addMessage("Unrecognized command: "
                                       + command.getAction() + ".",
                                       Message.TYPE_ERROR);
                    }
                    else {
                        addMessage("No action specified.", Message.TYPE_ERROR);
                    }
                }
            }
            catch (ParseException e) {
                String msg = "";
                if (e.getMessage() != null)
                    msg = e.getMessage();
                addMessage(msg, Message.TYPE_ERROR);
            }
            catch (RuntimeException e) {
                e.printStackTrace();
                String msg = "";
                if (e.getMessage() != null)
                    msg = e.getMessage();

                addMessage(msg, Message.TYPE_DBG_ERROR);
            }
            flushMessages();
        }
        return null;
    }

    void addMessage(Message msg) {
        messages.add(msg);
    }

    void addMessage(String msg, int type) {
        addMessage(new Message(msg, type));
    }

    private void flushMessages() {
        String prefix = null;
        Message tempmsg;

        for (Iterator iter = messages.iterator(); iter.hasNext();) {
            tempmsg = (Message) iter.next();
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

    PTSet createSet(String set) throws ParseException {
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
     * Prints a usage message for a command.
     *
     * @param cmd the command
     */
    public void printUsage(Command cmd) {
        addMessage("Usage: " + userhelp.getCmdSyntax(cmd.getAction()),
                   Message.TYPE_NORMAL);
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
 
    SteppingEngine getSteppingEngine () {
        return this.steppingEngine;
    }

    public PTSet getCommandPTSet(Command cmd) throws ParseException {
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
     * Sets the LinuxExeHost for an executable file so we can access its memory
     * via the "peek" command.
     * 
     */
    Host exeHost;
    
    public void setExeHost (Host host) {
	this.exeHost = host;
    }
    
    /**
     * Sets the LinuxExeProc for an executable file so we can access its memory
     * via the "peek" command.
     * 
     */
    Proc exeProc;
    
    public void setExeProc (Proc proc) {
	this.exeProc = proc;
    }
}
