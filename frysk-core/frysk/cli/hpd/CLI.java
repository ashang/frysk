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

package frysk.cli.hpd;

import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observer;
import java.util.Observable;

import java.text.ParseException;
import java.lang.RuntimeException;

import frysk.debuginfo.DebugInfo;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.rt.Frame;
import frysk.rt.StackFactory;
import frysk.rt.SteppingEngine;

public class CLI 
{
  Proc proc;
  Task task;
  int pid = 0;
  int tid = 0;
  DebugInfo debugInfo = null;
  boolean symtabNeedsRefresh = false;
  boolean running = false;
  int stackLevel = 0;
  SteppingObserver steppingObserver;
  boolean procSearchFinished = false;
  boolean attached;
  
  final HashSet runningProcs = new HashSet(); //Processes started with run command
  
  /**
   * Handle ConsoleReader Completor
   * @param buffer Input buffer.
   * @param cursor Position of TAB in buffer.
   * @param candidates List that may complete token.
   * @return cursor position in buffer
   */
  public int complete (String buffer, int cursor, List candidates)
  {
    int first_ws = buffer.indexOf(' ');
    int offset = 0;
    // Complete the request name for help
    if (buffer.startsWith("help "))
      offset = 5;
    // Complete the request name or help request
    if (first_ws == -1 || offset > 0)
      {
	Set commands = handlers.keySet();
	Iterator it = commands.iterator();
	while(it.hasNext())
	  {
	    String command = (String)it.next();
	    if (command.startsWith(buffer.substring(offset)))
	      candidates.add(command + " ");
	  }
        java.util.Collections.sort(candidates);
      }
    // Otherwise assume a symbol is being completed
    else if (debugInfo != null)
      {
	cursor = debugInfo.complete(buffer.substring(first_ws),
				 cursor - first_ws, candidates);
	return cursor + first_ws;
      }
    return 1 + offset;
  }
  // Superclass refreshes symbol table if necessary.
  public void refreshSymtab()
  {
    if (symtabNeedsRefresh && debugInfo != null)
      {
	debugInfo.refresh();
	symtabNeedsRefresh = false;
      }
  }
  
  /*
   * Command handlers
   */

  public void startAttach(int pid, Proc proc, Task task)
  {
    // At some point we will be able to use a RunState object
    // created elsewhere e.g., by the SourceWindowFactory.
    if (steppingObserver == null) 
      {
	steppingObserver = new SteppingObserver();
	SteppingEngine.addObserver(steppingObserver);
      }
    this.pid = pid;
    this.proc = proc;
    this.task = task;
    SteppingEngine.setProc(proc);
  }
  
  public void startAttach(Task task)
  {
    Proc proc = task.getProc();
    startAttach(proc.getPid(), proc, task);
  }

  public synchronized void finishAttach()
  {
    // Wait till we are attached.
    while (!attached)
      {
	try
	{
	  wait();
	}	
	catch (InterruptedException ie)
	{
	  addMessage("Attach interrupted.", Message.TYPE_ERROR);
	  return;
	}
      }
    addMessage("Attached to process " + pid, Message.TYPE_NORMAL);
    debugInfo = new DebugInfo(StackFactory.createFrame(this.task));
  }
  
  class UpDownHandler implements CommandHandler
  {
    public void handle(Command cmd) throws ParseException 
    {
      ArrayList params = cmd.getParameters();
      if (params.size() == 1 && params.get(0).equals("-help"))
        {
          printUsage(cmd);
          return;
        }
      refreshSymtab();
      int level = 1;
      Frame tmpFrame = null;
      Frame currentFrame = debugInfo.getCurrentFrame();

      if (params.size() != 0)
	level = Integer.parseInt((String)params.get(0));

      if (cmd.getAction().compareTo("up") == 0)
	{
	  tmpFrame = debugInfo.setCurrentFrame(level);
	  if (tmpFrame != currentFrame)
	    stackLevel += level;
	}
      else if (cmd.getAction().compareTo("down") == 0)
	{
	  tmpFrame = debugInfo.setCurrentFrame(-level);
	  if (tmpFrame != currentFrame)
	    stackLevel -= level;
	}
        
      if (tmpFrame == null)
	tmpFrame = currentFrame;
      outWriter.print("#" + stackLevel + " ");
      outWriter.println(tmpFrame.toPrint(false));        
    }
  }

  /*
   * Private variables
   */

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
  public void addHandler(CLIHandler handler)
  {
    String name = handler.getName(); 
    handlers.put(name, handler);
    userhelp.addHelp(name, handler.getHelp());
  }
  
  /**
   * Constructor
   * @param prompt String initially to be used as the prompt
   * @param out Stream for output. This really should be a PrintWriter
   */
  public CLI(String prompt, PrintStream out)
  {
    this.prompt = prompt;
    outWriter = new PrintWriter(out, true);

    prepro = new Preprocessor();
    handlers = new HashMap();
    userhelp = new UserHelp();
    dbgvars = new DbgVariables();
    addHandler(new ActionsCommand(this));
    handlers.put("alias", new AliasCommand(this));
    handlers.put("assign", new PrintCommand(this));
    handlers.put("attach", new AttachCommand(this));
    addHandler(new BreakpointCommand(this));
    handlers.put("defset", new DefsetCommand(this));
    addHandler(new DeleteCommand(this));
    handlers.put("detach", new DetachCommand(this));
    addHandler(new DisableCommand(this));
    handlers.put("down", new UpDownHandler());
    addHandler(new EnableCommand(this));
    handlers.put("focus", new FocusCommand(this));
    handlers.put("go", new GoCommand(this));
    handlers.put("halt", new HaltCommand(this));
    handlers.put("help", new HelpCommand(this));
    handlers.put("list", new ListCommand(this));
    handlers.put("print", new PrintCommand(this));
    handlers.put("quit", new QuitCommand(this));
    handlers.put("set", new SetCommand(this, dbgvars));
    handlers.put("step", new StepCommand(this));
    handlers.put("stepi", new StepInstructionCommand(this));
    handlers.put("unalias", new UnaliasCommand(this));
    handlers.put("undefset", new UndefsetCommand(this));
    handlers.put("unset", new UnsetCommand(this, dbgvars));
    handlers.put("up", new UpDownHandler());
    handlers.put("viewset", new ViewsetCommand(this));
    handlers.put("what", new WhatCommand(this));
    handlers.put("where", new WhereCommand(this));
    handlers.put("whichsets", new WhichsetsCommand(this));
    // New interface
    addHandler(new RunCommand(this));

    // initialize PT set stuff
    setparser = new SetNotationParser();

    allset = new AllPTSet();
    targetset = allset;

    builtinPTSets = new HashMap();
    builtinPTSets.put("all", allset);

    namedPTSets = new HashMap();
    namedPTSets.toString(); // avoid unused variable warnings

    messages = new LinkedList();

    //initialize alias table
    aliases = new HashMap();
    aliases.toString(); // avoid unused variable warnings
  }

  public String getPrompt()
  {
    return prompt;
  }

  public String execCommand(String cmd)
  {
    String pcmd = ""; // preprocessed command
    Command command;
    CommandHandler handler = null;

    if (cmd != null)
      {
	try
	  {
	    // preprocess and iterate
	    for (Iterator iter = prepro.preprocess(cmd); iter.hasNext();) 
	      {
		pcmd = (String)iter.next();
		command = new Command(pcmd);

		if (command.getAction() != null)
		  {
		    handler = (CommandHandler)handlers.get(command.getAction());
		    if (handler != null)
		      handler.handle(command);
		    else
		      addMessage("Unrecognized command: " 
				 + command.getAction() + ".",
				 Message.TYPE_ERROR);
		  }
		else
		  {
		    addMessage("No action specified.", Message.TYPE_ERROR);
		  }
	      }
	  }
	catch (ParseException e)
	  {	
	    String msg = "";
	    if (e.getMessage() != null)
	      msg = e.getMessage();
	    addMessage(msg, Message.TYPE_ERROR);
	  }
	catch (RuntimeException e)
	  {
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

  void addMessage(Message msg)
  {
    messages.add(msg);
  }

  void addMessage(String msg, int type)
  {
    addMessage(new Message(msg, type));
  }
	
  private void flushMessages()
  {
    String prefix = null;
    Message tempmsg;

    for (Iterator iter = messages.iterator(); iter.hasNext();)
      {
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

  PTSet createSet(String set) throws ParseException
  {
    ParsedSet parsed = setparser.parse(set);
    PTSet result = null;

    if (parsed.getType() == ParsedSet.TYPE_STATE)
      {
	int state = 0;
	if (parsed.getName().equals("running"))
	  state = AllPTSet.TASK_STATE_RUNNING;
	else if (parsed.getName().equals("stopped"))
	  state = AllPTSet.TASK_STATE_STOPPED;
	else if (parsed.getName().equals("runnable"))
	  state = AllPTSet.TASK_STATE_RUNNABLE;
	else if (parsed.getName().equals("held"))
	  state = AllPTSet.TASK_STATE_HELD;
	else
	  {
	    throw new RuntimeException("Illegal state name when creating set.");
	  }
	if (parsed.isStatic())
	  result = new StaticPTSet(allset.getSubsetByState(state));
	else
	  result = new StatePTSet(allset, state);
	addMessage("Creating new " + parsed.getName() + " state set.",
		   Message.TYPE_VERBOSE);
      }
    else if (parsed.getType() == ParsedSet.TYPE_HPD)
      {
	if (parsed.isStatic())
	  result 
	    = new StaticPTSet(allset.getSubset(parsed.getParseTreeNodes()));
	else
	  result = new DynamicPTSet(allset, parsed.getParseTreeNodes());

	addMessage("Creating new HPD notation set.", Message.TYPE_VERBOSE);
      }
    else if (parsed.getType() == ParsedSet.TYPE_NAMED)
      {
	if (parsed.isStatic())
	  {
	    addMessage("Cannot create a static set from a predefined set.",
		       Message.TYPE_ERROR);
	  }
	else
	  {
	    addMessage("Creating new set from named set \""
		       + parsed.getName() + "\".", Message.TYPE_VERBOSE);
	    result = (PTSet) namedPTSets.get(parsed.getName());
	  }

      }
    else if (parsed.getType() == ParsedSet.TYPE_EXEC)
      {
	if (parsed.isStatic())
	  {
	    result = new StaticPTSet(allset.getSubsetByExec(parsed.getName()));
	  }
	else
	  {
	    result = new ExecPTSet(allset, parsed.getName());
	  }
	addMessage("Creating new set from executable \"" + parsed.getName()
		   + "\".", Message.TYPE_VERBOSE);
      }
    return result;
  }
  
  class SteppingObserver implements Observer 
  {
    private Object monitor = new Object();
    
    public Object getMonitor()
    {
      return this.monitor;
    }
    
    public void update(Observable observable, Object arg)
    {
      if (arg == null)
	return;
      
      Task task = (Task)arg;
      //Breakpoint.PersistentBreakpoint bpt = null;

      synchronized (CLI.this) 
	{
	    if (!SteppingEngine.isTaskRunning(task))
	    {
	      attached = true;
	      symtabNeedsRefresh = true;
	    }
	  else
	    attached = false;
	    
	    //bpt = (Breakpoint.PersistentBreakpoint) SteppingEngine.getTaskBreakpoint(task);
            synchronized (this.monitor)
              {
                this.monitor.notifyAll();
              }

	  CLI.this.notifyAll();
	}
      //      if (bpt != null) 
      //	{
      //	  int size = apTable.size();
      //	  int i;
      //	  for (i = 0; i < size; i++)
      //	    {
      //	      Actionpoint ap = apTable.getActionpoint(i);
      //	      if (ap.getRTBreakpoint().containsPersistantBreakpoint(task.getProc(), bpt))
      //	{
      //	  outWriter.print("breakpoint " + i + " hit: ");
      //	  ap.output(outWriter);
      //	  outWriter.println("");
      //	  break;
      //	}
      //    }
      //  if (i >= size)
      //    {
      //      outWriter.println("unknown breakpoint hit: " + bpt.toString());
      //    }
      //}
    }
  }

  /**
   * Prints a usage message for a command.
   * @param cmd the command
   */
  public void printUsage(Command cmd)
  {
    addMessage("Usage: " + userhelp.getCmdSyntax(cmd.getAction()), 
	       Message.TYPE_NORMAL);
  }
  
  /**
   * Return output writer.
   */
  public PrintWriter getPrintWriter()
  {
    return outWriter;
  }

  /**
   * Return CLI's current task.
   */

  public Task getTask()
  {
    return task;
  }

  public DebugInfo getDebugInfo()
  {
    return debugInfo;
  }
  
  /**
   * Get the set of processes (Proc) started by the run command. Access to the
   * CLI object should be synchronized when using the set.
   * @return the set
   */
  public HashSet getRunningProcs()
  {
    return runningProcs;
  }
  
  boolean isRunning ()
  {
    return this.running;
  }
}
