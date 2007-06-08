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
import frysk.proc.Host;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.Task;
import frysk.rt.Frame;
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
  
  /*
   * Set commands
   */
  class DefsetHandler implements CommandHandler
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
      String setname = null;
      String setnot = null;
      PTSet set = null;

      if (params.size() == 2)
	{
	  setname = (String) params.get(0);
	  if (!setname.matches("\\w+"))
	    throw new ParseException("Set name must be alphanumeric.", 0);
	  setnot = (String) params.get(1);
	  if (!builtinPTSets.containsKey(setnot))
	    {
	      set = createSet(setnot);
	      namedPTSets.put(setname, set);
	    }
	  else
	    {
	      addMessage("The set name is reserved for a predefined set.",
			 Message.TYPE_ERROR);
	    }
	}
      else
	{
	  printUsage(cmd);
	}
    }
  }
  class UndefsetHandler implements CommandHandler
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
      if (params.size() == 1)
	{
	  String setname = (String)params.get(0);

	  if (builtinPTSets.containsKey(setname))
	    {
	      addMessage(new Message("The set \"" + setname
				     + "\" cannot be undefined.",
				     Message.TYPE_ERROR));
	    }
	  else if (namedPTSets.containsKey(setname))
	    {
	      namedPTSets.remove(setname);
	      addMessage("Set \"" + setname + "\" successfuly undefined.",
			 Message.TYPE_VERBOSE);
	    }
	  else
	    {
	      addMessage("Set \"" + setname
			 + "\" does not exist, no action taken.",
				     Message.TYPE_NORMAL);
	    }
	}
      else
	{
	  printUsage(cmd);
	}
    }
  }

  class ViewsetHandler implements CommandHandler 
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
      PTSet tempset = null;
      TaskData temptd = null;
      String setname = "";
      String output = "";

      if (params.size() <= 1)
	{
	  if (params.size() == 0)
	    tempset = targetset;
	  else if (params.size() == 1)
	    {
	      setname = (String)params.get(0);
	      if (namedPTSets.containsKey(setname))
		tempset = (PTSet) namedPTSets.get(setname);
	      else
		{
		  addMessage(new Message("Set \"" + setname
					 + "\" does not exist.",
					 Message.TYPE_NORMAL));
		  return;
		}
	    }

	  for (Iterator iter = tempset.getTaskData(); iter.hasNext();)
	    {
	      // ??? this way of outputting is simple, but it's okay for now
	      temptd = (TaskData)iter.next();
	      output += "Set " + setname + " includes:\n";
	      output += "[" + temptd.getParentID() + "." + temptd.getID() + "]\n";
	      addMessage(output, Message.TYPE_NORMAL);
	    }
	}
      else
	{
	  printUsage(cmd);
	}
    }
  }

  class WhichsetsHandler implements CommandHandler
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
      PTSet searchset = null;
      PTSet tempset = null;
      TaskData temptd = null;
      String setname = null;

      // ??? check builtin sets
      if (params.size() <= 1)
	{
	  if (params.size() == 0)
	    searchset = targetset;
	  else if (params.size() == 1)
	    searchset = createSet((String)params.get(0));

	  // start iterating through available sets
	  for (Iterator it = searchset.getTaskData(); it.hasNext();)
	    {
	      temptd = (TaskData) it.next();
	      addMessage("Task " + temptd.getParentID() + "." + temptd.getID()
			 + " is in sets: \n", Message.TYPE_NORMAL);
	      for (Iterator iter = namedPTSets.keySet().iterator(); 
		   iter.hasNext();)
		{
		  setname = (String)iter.next();
		  tempset = (PTSet)namedPTSets.get(setname);

		  if (tempset.containsTask(temptd.getParentID(),
					   temptd.getID()))
		    addMessage("\t" + setname + "\n", Message.TYPE_NORMAL);
		}
	      addMessage("\n", Message.TYPE_NORMAL);
	    }
	}
      else
	{
	  printUsage(cmd);
	}
    }
  }

  class FocusHandler implements CommandHandler 
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
      if (params.size() <= 1)
	{
	  if (params.size() == 1)
	    targetset = createSet((String)params.get(0));
	  else
	    ((CommandHandler)handlers.get("viewset"))
	      .handle(new Command("viewset"));
	}
      else
	{
	  printUsage(cmd);
	}
    }
  }


  class AliasHandler implements CommandHandler 
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
      if (params.size() <= 2)
	{
	  if (params.size() == 2)
	    {
	      aliases.put((String)params.get(0), (String)params.get(1));
	    }
	  else if (params.size() == 1)
	    {
	      String temp = (String)params.get(0);
	      if (aliases.containsKey(temp))
		{
		  addMessage(temp + " = " + (String)aliases.get(temp),
			     Message.TYPE_NORMAL);
		}
	      else
		addMessage("Alias \"" + temp + "\" not defined.", 
			   Message.TYPE_ERROR);
	    }
	  else 
	    {
	      addMessage(aliases.toString(), Message.TYPE_NORMAL);
	    }
	}
      else
	{
	  printUsage(cmd);
	}
    }
  }

  class AttachHandler implements CommandHandler
  {
    public void handle(Command cmd) throws ParseException
    {
      ArrayList params = cmd.getParameters();
      if (params.size() == 1 && params.get(0).equals("-help"))
        {
          printUsage(cmd);
          return;
        }
      refreshSymtab();	// XXX ?
      boolean cli = true;

      if (params.size() < 1)
	{
	  printUsage(cmd);
	  return;
	}
 
      for (int idx = 0; idx < params.size(); idx++)
	{
	  if (((String)params.get(idx)).equals("-cli"))
	    cli = true;
	  else if (((String)params.get(idx)).equals("-no-cli"))
	    cli = false;
	  else if (((String)params.get(idx)).equals("-task"))
	    {
	      idx += 1;
	      tid = Integer.parseInt(((String)params.get(idx)));
	    }
	  else if (((String)params.get(idx)).indexOf('-') == 0)
	    {
	      printUsage(cmd);
	      return;
	    }
	  else if (((String)params.get(idx)).matches("[0-9]+"))
	    pid = Integer.parseInt((String)params.get(idx)); 
	}

      if (cli)
	{
	  procSearchFinished = false;
	  Manager.host.requestFindProc(new ProcId(pid), new Host.FindProc() {
	      public void procFound (ProcId procId)
	      {
		synchronized (CLI.this)
		  {
		    proc = Manager.host.getProc(procId);
		    procSearchFinished = true;
		    CLI.this.notifyAll();
		  }
	      }

	      public void procNotFound (ProcId procId, Exception e)
	      {
		synchronized (CLI.this)
		  {
		    proc = null;
		    procSearchFinished = true;
		    CLI.this.notifyAll();
		  }
	      }});
	  synchronized (CLI.this)
	    {
	      while (!procSearchFinished)
		{
		  try
		    {
		      CLI.this.wait();
		    }
		  catch (InterruptedException ie)
		    {
		      proc = null;
		    }
		}
	    }
	}
      if (proc == null)
	{
	  addMessage("Couldn't find process " + pid, Message.TYPE_ERROR);
	  return;
	}

      if (pid == tid || tid == 0)
	task = proc.getMainTask();
      else
	for (Iterator i = proc.getTasks ().iterator (); i.hasNext (); )
	  {
	    task = (Task) i.next ();
	    if (task.getTid () == tid)
	      break;
	  }
      if (cli)
	{
	  startAttach(pid, proc, task);
	  finishAttach();
	}
      else
	{
	  // This can't work because the event loop isn't started in
	  // the non-cli case, so we can't find the proc.
	  // symtab = new SymTab(pid, proc, task, null); 
	}
    }
  }

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
    debugInfo = new DebugInfo(pid, proc, task, null);
  }
  
  class UnaliasHandler implements CommandHandler
  {
    public void handle(Command cmd)  throws ParseException
    {
      refreshSymtab();
      ArrayList params = cmd.getParameters();
      if (params.size() == 1)
	{
	  if (((String)params.get(0)).equals("-all"))
	    {
	      aliases.clear();
	      addMessage("Removing all aliases.", Message.TYPE_VERBOSE);
	    }
	  else
	    {
	      String temp = (String)params.get(0);
	      if (aliases.containsKey(temp))
		{
		  aliases.remove(temp);
		  addMessage("Removed alias \"" + temp + "\"",
			     Message.TYPE_VERBOSE);
		}
	      else
		addMessage("Alias \"" + temp + "\" not defined.", 
			   Message.TYPE_ERROR);
	    }
	}
      else
	{
	  printUsage(cmd);
	}
    }
  }
    

  class SetHandler implements CommandHandler
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
      String temp;
      if (params.size() == 3 && ((String)params.get(1)).equals("=") )
	{
	  temp = (String)params.get(0);
	  if (dbgvars.variableIsValid(temp))
	    {
	      if (dbgvars.valueIsValid(temp, (String)params.get(2)))
		{
		  dbgvars.setVariable(temp, (String)params.get(2));
		}
	      else
		addMessage("Illegal variable value.", Message.TYPE_ERROR);
	    }
	  else
	    addMessage(new Message("Illegal debugger variable \""
				   + (String)params.get(0) + "\"",
				   Message.TYPE_ERROR));
	}
      else if (params.size() == 1)
	{
	  temp = (String)params.get(0);
	  if (dbgvars.variableIsValid(temp))
	    {
	      addMessage(temp + " = " + dbgvars.getValue(temp).toString(),
			 Message.TYPE_NORMAL);
	    }
	  else
	    addMessage(new Message("Illegal debugger variable \"" 
				   + (String)params.get(0) + "\"",
				   Message.TYPE_ERROR));
	}
      else if (params.size() == 0)
	{
	  addMessage(dbgvars.toString(), Message.TYPE_NORMAL);
	}
      else
	{
	  printUsage(cmd);
	}
    }
  }

  class UnsetHandler implements CommandHandler
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
      String temp;
      if (params.size() == 1)
	{
	  temp = (String)params.get(0);
	  if (temp.equals("-all"))
	    dbgvars.unsetAll();
	  else
	    {
	      if (dbgvars.variableIsValid(temp))
		dbgvars.unsetVariable(temp);
	      else
		addMessage(new Message("\"" + (String)params.get(0)
				       + "\" is not a valid debugger variable",
				       Message.TYPE_ERROR));
	    }
	}
      else
	{
	  printUsage(cmd);
	}
    }
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

  class GoHandler implements CommandHandler
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
      
      if (steppingObserver != null)
        {
          SteppingEngine.continueExecution(proc.getTasks());
          running = true;
        }
      else
	addMessage("Not attached to any process", Message.TYPE_ERROR);
    }
  }

  /*
   * Private variables
   */

  //private static PrintStream out = null;// = System.out;
  final PrintWriter outWriter;
  private Preprocessor prepro;
  private String prompt; // string to represent prompt, will be moved
  private HashMap handlers;
  final UserHelp userhelp;
  private DbgVariables dbgvars;

  // PT set related stuff
  private SetNotationParser setparser;
  private AllPTSet allset; // the "all" set
  private HashMap namedPTSets; // user-created named sets
  private HashMap builtinPTSets; // predefined named sets
  private PTSet targetset;

  // other
  // debugger output messages, e.g. the Message class
  private LinkedList messages; 

  // alias
  private HashMap aliases;

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
    addHandler(new ActionsHandler(this));
    handlers.put("alias", new AliasHandler());
    handlers.put("assign", new PrintCommand(this));
    handlers.put("attach", new AttachHandler());
    addHandler(new BreakpointHandler(this));
    handlers.put("defset", new DefsetHandler());
    addHandler(new DeleteHandler(this));
    handlers.put("detach", new DetachCommand(this));
    addHandler(new DisableHandler(this));
    handlers.put("down", new UpDownHandler());
    addHandler(new EnableHandler(this));
    handlers.put("focus", new FocusHandler());
    handlers.put("go", new GoHandler());
    handlers.put("halt", new HaltCommand(this));
    handlers.put("help", new HelpCommand(this));
    handlers.put("list", new ListCommand(this));
    handlers.put("print", new PrintCommand(this));
    handlers.put("quit", new QuitCommand(this));
    handlers.put("set", new SetHandler());
    handlers.put("step", new StepCommand(this));
    handlers.put("stepi", new StepInstructionCommand(this));
    handlers.put("unalias", new UnaliasHandler());
    handlers.put("undefset", new UndefsetHandler());
    handlers.put("unset", new UnsetHandler());
    handlers.put("up", new UpDownHandler());
    handlers.put("viewset", new ViewsetHandler());
    handlers.put("what", new WhatCommand(this));
    handlers.put("where", new WhereCommand(this));
    handlers.put("whichsets", new WhichsetsHandler());
    // New interface
    addHandler(new RunHandler(this));

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

  private PTSet createSet(String set) throws ParseException
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
