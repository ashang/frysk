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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Observer;
import java.util.Observable;

import java.text.ParseException;
import java.lang.RuntimeException;

import javax.naming.NameNotFoundException;

import frysk.value.Value;
import frysk.proc.Host;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.Task;
import frysk.rt.Frame;
import frysk.rt.SteppingEngine;
import frysk.sys.Signal;
import frysk.sys.Sig;

import lib.dw.BaseTypes;
import lib.dw.DwarfDie;

public class CLI 
{
  Proc proc;
  Task task;
  int pid = 0;
  int tid = 0;
  SymTab symtab = null;
  boolean symtabNeedsRefresh = false;
  int stackLevel = 0;
  private SteppingObserver steppingObserver;
  boolean procSearchFinished = false;
  boolean attached;
  
  private HashSet runningProcs = new HashSet(); //Processes started with run command
  
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
      }
    // Otherwise assume a symbol is being completed
    else if (symtab != null)
      {
	cursor = symtab.complete(buffer.substring(first_ws),
				 cursor - first_ws, candidates);
	return cursor + first_ws;
      }
    return 1 + offset;
  }
  // Superclass refreshes symbol table if necessary.
  public void refreshSymtab()
  {
    if (symtabNeedsRefresh && symtab != null)
      {
	symtab.refresh();
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

	  if (builtinPTSets.contains(setname))
	    {
	      addMessage(new Message("The set \"" + setname
				     + "\" cannot be undefined.",
				     Message.TYPE_ERROR));
	    }
	  else if (namedPTSets.contains(setname))
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
    symtab = new SymTab(pid, proc, task, null);
  }
  
  class DetachHandler implements CommandHandler
  {
    public void handle (Command cmd) throws ParseException
    {
      ArrayList params = cmd.getParameters();
      if (params.size() == 1 && params.get(0).equals("-help"))
	{
	  printUsage(cmd);
	  return;
	}
      refreshSymtab();

      boolean startedByRun;
      synchronized (CLI.this)
      {
	startedByRun = runningProcs.contains(proc);
      }
      if (startedByRun)
	{
	  addMessage("Can't detach a process started by the run command.", 
	             Message.TYPE_ERROR);
	  return;
	}
      // Delete all breakpoints.
      if (steppingObserver != null)
		SteppingEngine.removeObserver(steppingObserver, proc);

      proc = null;
      task = null;

      attached = false;
      if (params.size() > 0)
	{
	  printUsage(cmd);
	}
    }
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
    
  class ListHandler implements CommandHandler
  {
    File file = null;
    int line;
    int exec_line = 0;
    public void handle(Command cmd) throws ParseException
    {
      ArrayList params = cmd.getParameters();
      int windowSize = 20;
      if (params.size() == 1 && params.get(0).equals("-help"))
        {
          printUsage(cmd);
          return;
        }
      refreshSymtab();
      if (proc == null)
	{
	  addMessage("No symbol table is available.", Message.TYPE_NORMAL);
	  return;
	}
      if (params.size() == 1)			// list N
	{
	  try 
	  {
	    line = Integer.parseInt((String)params.get(0));
	  }
	  catch (NumberFormatException ignore)
	  {
	    if (((String)params.get(0)).compareTo("$EXEC") == 0)
		line = symtab.getCurrentFrame().getLines()[0].getLine() - 10;
	    else 
	      {
		DwarfDie funcDie = null;
		try
		{
		  funcDie = symtab.getSymbolDie((String)params.get(0));
		}
		catch (NameNotFoundException none) {}
		line = (int)funcDie.getDeclLine();
	      }
	  }
	}
      else if (params.size() == 2)		// list -length {-}N
	{
	  if (((String)params.get(0)).equals("-length"))
	    {
	      try 
	      {
		windowSize = Integer.parseInt((String)params.get(1));
		if (windowSize < 0)
		  {
		    line += windowSize;
		  }
	      }
	      catch (NumberFormatException ignore)
	      {}
	    }
	}
 
      if (file== null)
	{
	  Frame frame = symtab.getCurrentFrame();
	  if (frame.getLines().length > 0)
	    {
	      file = (frame.getLines()[0]).getFile();
	      line = (frame.getLines()[0]).getLine() - 10;
	      exec_line = line;
	    }
	  else
	    outWriter.println("No source for current frame");
	}
      
      if (line < 0)
	line = 1;
      try 
	{
	  FileReader fr = new FileReader(file);
	  LineNumberReader lr = new LineNumberReader(fr);
	  String str;
	  boolean display = false;
	  int endLine = line + StrictMath.abs(windowSize);
	  String flag = "";
	  while ((str = lr.readLine()) != null) 
	    {
	      if (lr.getLineNumber() == line)
		display = true;
	      else if (lr.getLineNumber() == exec_line)
		flag = "*";
	      else if (lr.getLineNumber() == endLine)
		break;
                
	      if (display)
		{
		  outWriter.println(lr.getLineNumber() + flag + "\t "+ str);
		  flag = "";
		}
	    }
	  if (str != null && windowSize > 0)
	    line += windowSize;
	  lr.close();
	}
      catch (IOException e) 
        {
          addMessage("file " + file + " not found.",
		     Message.TYPE_ERROR);
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
      Frame currentFrame = symtab.getCurrentFrame();

      if (params.size() != 0)
	level = Integer.parseInt((String)params.get(0));

      if (cmd.getAction().compareTo("up") == 0)
	{
	  tmpFrame = symtab.setCurrentFrame(level);
	  if (tmpFrame != currentFrame)
	    stackLevel += level;
	}
      else if (cmd.getAction().compareTo("down") == 0)
	{
	  tmpFrame = symtab.setCurrentFrame(-level);
	  if (tmpFrame != currentFrame)
	    stackLevel -= level;
	}
        
      if (tmpFrame == null)
	tmpFrame = currentFrame;
      outWriter.print("#" + stackLevel + " ");
      outWriter.println(tmpFrame.toPrint(false));        
    }
  }
  
  class WhereHandler implements CommandHandler
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
      int level = 0;
      Frame tmpFrame = null;
        
      if (proc == null)
	{
	  addMessage("No symbol table is available.", Message.TYPE_NORMAL);
	  return;
	}

      if (params.size() != 0)
	level = Integer.parseInt((String)params.get(0));
 
      int l = stackLevel;
      int stopLevel;
      if (level > 0)
	stopLevel = l + level;
      else
	stopLevel = 0;
      tmpFrame = symtab.getCurrentFrame();
      while (tmpFrame != null)
	{
	  outWriter.print("#" + l + " ");
	  outWriter.println(tmpFrame.toPrint(false));
	  tmpFrame = tmpFrame.getOuter();
	  l += 1;
	  if (l == stopLevel)
	    break;
	}
    }
  }
    
  class WhatHandler implements CommandHandler
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
      if (params.size() == 0)
	return;
        
      if (proc == null)
	{
	  addMessage("No symbol table is available.", Message.TYPE_NORMAL);
	  return;
	}

      if (params.size() == 0
          || (((String)params.get(0)).equals("-help")))
        {
          printUsage(cmd);
          return;
        }
      String sInput = ((String)params.get(0));
      try 
        {
          outWriter.println(symtab.what(sInput));
        }
      catch (NameNotFoundException nnfe)
        {
          addMessage(nnfe.getMessage(), Message.TYPE_ERROR);
        }
    }
  }
    
  private static final int DECIMAL = 10;
  private static final int HEX = 16;
  private static final int OCTAL = 8;    
    
  class PrintHandler implements CommandHandler
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
      if (cmd.getParameters().size() == 0
          || (((String)params.get(0)).equals("-help")))
        {
          printUsage(cmd);
          return;
        }
      boolean haveFormat = false;
      int outputFormat = DECIMAL;

      String sInput 
	= cmd.getFullCommand().substring(cmd.getAction().length()).trim();

      for (int i = 0; i < params.size(); i++)
	{
	  if (((String)params.get(i)).equals("-format"))
	    {
	      haveFormat = true;
	      i += 1;
	      String arg = ((String)params.get(i));
	      if (arg.compareTo("d") == 0)
		outputFormat = DECIMAL;
	      else if (arg.compareTo("o") == 0)
		outputFormat = OCTAL;
	      else if (arg.compareTo("x") == 0)
		outputFormat = HEX;
	    }
	}
      if (haveFormat)
	sInput = sInput.substring(0,sInput.indexOf("-format"));

      if (sInput.length() == 0) 
	{
	  printUsage(cmd);
	  return;
	}

      if (cmd.getAction().compareTo("assign") == 0) 
	{
	  int i = sInput.indexOf(' ');
	  if (i == -1) 
	    {
	      printUsage(cmd);          
	      return;
	    }
	  sInput = sInput.substring(0, i) + "=" + sInput.substring(i);
	}        

      Value result = null;
      try 
        {
          result = SymTab.print(sInput);
        }
      catch (NameNotFoundException nnfe)
        {
          addMessage(new Message(nnfe.getMessage(),
                                 Message.TYPE_ERROR));
          return;
        }
      if (result == null)
	{
	  addMessage("Variable " + sInput + " not found in scope",
		     Message.TYPE_ERROR);
	  return;
	}

      switch (outputFormat)
      {
      case HEX: 
	outWriter.print("0x");
	break;
      case OCTAL: 
	outWriter.print("0");
	break;
      }
      int resultType = result.getType().getTypeId();
      if (resultType == BaseTypes.baseTypeFloat
	  || resultType == BaseTypes.baseTypeDouble)
	outWriter.println(result.toString());
      else if (resultType == BaseTypes.baseTypeShort
	  || resultType == BaseTypes.baseTypeInteger
	  || resultType == BaseTypes.baseTypeLong)
	outWriter.println(Long.toString(result.longValue(),
	                                outputFormat));
      else if (resultType == BaseTypes.baseTypeByte)
	outWriter.println(Integer.toString((int)result.longValue(),
	                                   outputFormat) + 
	                                   " '" + result.toString() + "'");
      else
	outWriter.println(result.toString());
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
	SteppingEngine.continueExecution(proc.getTasks());
      else
	addMessage("Not attached to any process", Message.TYPE_ERROR);
    }
  }

  class HaltHandler
    implements CommandHandler
  {
    public void handle(Command cmd)
      throws ParseException
    {
      ArrayList params = cmd.getParameters();
      if (params.size() == 1 && params.get(0).equals("-help"))
        {
          printUsage(cmd);
          return;
        }
      refreshSymtab();
      
      if (steppingObserver != null)
	SteppingEngine.stop(null, proc.getTasks());
      else
	addMessage("Not attached to any process", Message.TYPE_ERROR);
   	   
    }
  }

  class QuitHandler implements CommandHandler
  {
    public void handle(Command cmd) throws ParseException 
    {
      refreshSymtab();
      Iterator iterator = runningProcs.iterator();
      while (iterator.hasNext())
	{
	  Proc p = (Proc) iterator.next();
	  Signal.kill(p.getPid(),Sig.KILL);
	}
      addMessage("Quitting...", Message.TYPE_NORMAL);
      DetachHandler detachHandler = new DetachHandler();
      Command command = new Command ("detach");
      detachHandler.handle(command);
    }
  }
  class HelpHandler implements CommandHandler
  {
    public void handle(Command cmd) throws ParseException 
    {
      ArrayList params = cmd.getParameters();
      String output = "";
      String temp = "";
      if (params.size() == 0)
        for (Iterator iter = userhelp.getCmdList().iterator(); iter.hasNext();)
          {  
            temp = (String)iter.next();
            output += temp + " - " + userhelp.getCmdDescription(temp) + "\n";
          }
      else
        for (Iterator iter = userhelp.getCmdList().iterator(); iter.hasNext();)
          {
            temp = (String)iter.next();
            if (temp.compareTo(params.get(0)) == 0)
              {              
                output += userhelp.getCmdSyntax(temp) + "\n";
                output += userhelp.getCmdFullDescr(temp);
              }
          }
      addMessage(output, Message.TYPE_NORMAL);
    }
  }
    
  /*
   * Private variables
   */

  //private static PrintStream out = null;// = System.out;
  private PrintWriter outWriter = null;
  private Preprocessor prepro;
  private String prompt; // string to represent prompt, will be moved
  private Hashtable handlers;
  private UserHelp userhelp;
  private DbgVariables dbgvars;

  // PT set related stuff
  private SetNotationParser setparser;
  private AllPTSet allset; // the "all" set
  private Hashtable namedPTSets; // user-created named sets
  private Hashtable builtinPTSets; // predefined named sets
  private PTSet targetset;

  // other
  // debugger output messages, e.g. the Message class
  private LinkedList messages; 

  // alias
  private Hashtable aliases;

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
    handlers = new Hashtable();
    userhelp = new UserHelp();
    dbgvars = new DbgVariables();
    addHandler(new ActionsHandler(this));
    handlers.put("alias", new AliasHandler());
    handlers.put("assign", new PrintHandler());
    handlers.put("attach", new AttachHandler());
    addHandler(new BreakpointHandler(this));
    handlers.put("defset", new DefsetHandler());
    addHandler(new DeleteHandler(this));
    handlers.put("detach", new DetachHandler());
    addHandler(new DisableHandler(this));
    handlers.put("down", new UpDownHandler());
    addHandler(new EnableHandler(this));
    handlers.put("focus", new FocusHandler());
    handlers.put("go", new GoHandler());
    handlers.put("halt", new HaltHandler());
    handlers.put("help", new HelpHandler());
    handlers.put("list", new ListHandler());
    handlers.put("print", new PrintHandler());
    handlers.put("quit", new QuitHandler());
    handlers.put("set", new SetHandler());
    handlers.put("unalias", new UnaliasHandler());
    handlers.put("undefset", new UndefsetHandler());
    handlers.put("unset", new UnsetHandler());
    handlers.put("up", new UpDownHandler());
    handlers.put("viewset", new ViewsetHandler());
    handlers.put("what", new WhatHandler());
    handlers.put("where", new WhereHandler());
    handlers.put("whichsets", new WhichsetsHandler());
    // New interface
    addHandler(new RunHandler(this));

    // initialize PT set stuff
    setparser = new SetNotationParser();

    allset = new AllPTSet();
    targetset = allset;

    builtinPTSets = new Hashtable();
    builtinPTSets.put("all", allset);

    namedPTSets = new Hashtable();
    namedPTSets.toString(); // avoid unused variable warnings

    messages = new LinkedList();

    //initialize alias table
    aliases = new Hashtable();
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

  private void addMessage(Message msg)
  {
    messages.add(msg);
  }

  private void addMessage(String msg, int type)
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
    
  private class SteppingObserver implements Observer 
  {
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

  public SymTab getSymTab()
  {
    return symtab;
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
  
}
