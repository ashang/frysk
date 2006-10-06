// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

import java.util.Vector;
import java.util.LinkedList;
import java.util.Hashtable;
import java.util.Iterator;

import java.text.ParseException;
import java.lang.RuntimeException;

import javax.naming.NameNotFoundException;

import frysk.lang.BaseTypes;
import frysk.lang.InvalidOperatorException;
import frysk.lang.Variable;
import frysk.proc.Manager;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.Task;
import frysk.proc.TaskException;
import frysk.rt.StackFactory;
import frysk.rt.StackFrame;
import frysk.sys.Errno;
import frysk.sys.Ptrace;
import frysk.sys.Sig;


import lib.dw.DwarfDie;
import lib.dw.Dwfl;
import lib.dw.DwflDieBias;
import lib.dw.DwflLine;


public class CLI 
{
    private Dwfl dwfl;
    Proc proc;
    Task task;
    int pid = 0;
    SymTab symtab;
    StackFrame frame = null;
    int stackLevel = 0;

	/*
	 * Command handlers
	 */

	/*
	 * Set commands
	 */
	class DefsetHandler implements CommandHandler
	{
		public void handle(Command cmd) throws ParseException {
			Vector params = cmd.getParameters();
			String setname = null;
			String setnot = null;
			PTSet set = null;

			if (params.size() == 2)
			{
				setname = (String) params.elementAt(0);
				if (!setname.matches("\\w+"))
					throw new ParseException("Set name must be alphanumeric.", 0);

				setnot = (String) params.elementAt(1);

				if (!builtinPTSets.containsKey(setnot))
				{
					set = createSet(setnot);
					namedPTSets.put(setname, set);
				}
				else
				{
					addMessage(new Message("The set name is reserved for a predefined set.", Message.TYPE_ERROR));
				}
			}
			else
			{
				addMessage(new Message("Usage: " + userhelp.getCmdSyntax(cmd.getAction()), Message.TYPE_NORMAL));
			}
		}
	}
	class UndefsetHandler implements CommandHandler
	{
		public void handle(Command cmd) throws ParseException
		{
			if (cmd.getParameters().size() == 1)
			{
				String setname = (String)cmd.getParameters().elementAt(0);

				if (builtinPTSets.contains(setname))
				{
					addMessage(new Message("The set \"" + setname + "\" cannot be undefined.",
											Message.TYPE_ERROR));
				}
				else if (namedPTSets.contains(setname))
				{
					namedPTSets.remove(setname);
					addMessage(new Message("Set \"" + setname + "\" successfuly undefined.", Message.TYPE_VERBOSE));
				}
				else
				{
					addMessage(new Message("Set \"" + setname + "\" does not exist, no action taken.",
											Message.TYPE_NORMAL));
				}
			}
			else
			{
				addMessage(new Message("Usage: " + userhelp.getCmdSyntax(cmd.getAction()), Message.TYPE_NORMAL));
			}
		}
	}

	class ViewsetHandler implements CommandHandler 
	{
		public void handle(Command cmd) throws ParseException
		{
			PTSet tempset = null;
			TaskData temptd = null;
			String setname = "";
			String output = "";

			if (cmd.getParameters().size() <= 1)
			{
				if (cmd.getParameters().size() == 0)
					tempset = targetset;
				else if (cmd.getParameters().size() == 1)
				{
					setname = (String)cmd.getParameters().elementAt(0);
					if (namedPTSets.containsKey(setname))
						tempset = (PTSet) namedPTSets.get(setname);
					else
					{
						addMessage(new Message("Set \"" + setname + "\" does not exist.",
											Message.TYPE_NORMAL));
						return;
					}
				}

				for (Iterator iter = tempset.getTaskData(); iter.hasNext();)
				{
					//TODO this way of outputing is rather stoopid, but it's okay for now
					temptd = (TaskData)iter.next();
					output += "Set " + setname + " includes:\n";
					output += "[" + temptd.getParentID() + "." + temptd.getID() + "]\n";
					addMessage(new Message(output, Message.TYPE_NORMAL));
				}
			}
			else
			{
				addMessage(new Message("Usage: " + userhelp.getCmdSyntax(cmd.getAction()), Message.TYPE_NORMAL));
			}
		}
	}

	class WhichsetsHandler implements CommandHandler
	{
		public void handle(Command cmd) throws ParseException
		{
			PTSet searchset = null;
			PTSet tempset = null;
			TaskData temptd = null;
			String setname = null;

			//TODO check builtin sets
			if (cmd.getParameters().size() <= 1)
			{
				if (cmd.getParameters().size() == 0)
					searchset = targetset;
				else if (cmd.getParameters().size() == 1)
					searchset = createSet((String)cmd.getParameters().elementAt(0));

				//start iterating through availabe sets
				for (Iterator it = searchset.getTaskData(); it.hasNext();)
				{
					temptd = (TaskData) it.next();
					addMessage(new Message("Task " + temptd.getParentID() + "." + temptd.getID() +
											" is in sets: \n", Message.TYPE_NORMAL));
					for (Iterator iter = namedPTSets.keySet().iterator(); iter.hasNext();)
					{
						setname = (String)iter.next();
						tempset = (PTSet)namedPTSets.get(setname);

						if ( tempset.containsTask(temptd.getParentID(), temptd.getID()) )
						addMessage(new Message("\t" + setname + "\n", Message.TYPE_NORMAL));
					}
					addMessage(new Message("\n", Message.TYPE_NORMAL));
				}
			}
			else
			{
				addMessage(new Message("Usage: " + userhelp.getCmdSyntax(cmd.getAction()), Message.TYPE_NORMAL));
			}
		}
	}

	class FocusHandler implements CommandHandler 
	{
		public void handle(Command cmd) throws ParseException
		{
			if (cmd.getParameters().size() <= 1)
			{
				if (cmd.getParameters().size() == 1)
					targetset = createSet((String)cmd.getParameters().elementAt(0));
				else
					( (CommandHandler) handlers.get("viewset") ).handle( new Command("viewset") );
			}
			else
			{
				addMessage(new Message("Usage: " + userhelp.getCmdSyntax(cmd.getAction()), Message.TYPE_NORMAL));
			}
		}
	}


	class AliasHandler implements CommandHandler 
	{
		public void handle(Command cmd) throws ParseException
		{
			Vector params = cmd.getParameters();
			if (params.size() <= 2)
			{
				if (params.size() == 2)
				{
					aliases.put((String)params.elementAt(0), (String)params.elementAt(1));
				}
				else if (params.size() == 1)
				{
					String temp = (String)params.elementAt(0);
					if (aliases.containsKey(temp))
					{
						addMessage(new Message(temp + " = " + (String)aliases.get(temp), Message.TYPE_NORMAL));
					}
					else
						addMessage(new Message("Alias \"" + temp + "\" not defined.", Message.TYPE_ERROR));
				}
				else 
				{
					addMessage(new Message(aliases.toString(), Message.TYPE_NORMAL));
				}
			}
			else
			{
				addMessage(new Message("Usage: " + userhelp.getCmdSyntax(cmd.getAction()), Message.TYPE_NORMAL));
			}
		}
	}

    class AttachHandler implements CommandHandler
    {
        public void handle(Command cmd) throws ParseException
        {
          Vector params = cmd.getParameters();
          String executable = "";
          boolean cli = false;

          if (params.size() < 2)
            {
              cmd.getOut().println ("Usage " + cmd.getAction() + "Executable PID");
              return;
            }
          if (params.size() == 3 && ((String)params.elementAt(2)).equals("-cli"))
            cli = true;
          executable = ((String)params.elementAt(0));
          pid = Integer.parseInt((String)params.elementAt(1));

          if (cli)
            {
              Manager.eventLoop.runPolling (1000);
              try {
                Ptrace.attach(pid);
              }
              catch (Errno errno) {
                addMessage(new Message("No such process.", Message.TYPE_ERROR));
              }
              Manager.host.requestRefreshXXX (true);
              Manager.eventLoop.runPending ();
            }

          proc = Manager.host.getProc (new ProcId (pid));
          if (proc == null)
            {
              addMessage(new Message("The event manager is not running.", Message.TYPE_ERROR));
              return;
            }          
          task = proc.getMainTask();

 
          symtab = new SymTab(pid, proc, task, null);
        }
    }

    class DetachHandler implements CommandHandler
    {
        public void handle(Command cmd) throws ParseException
        {
          Vector params = cmd.getParameters();
          String sInput = cmd.getFullCommand().substring(cmd.getAction().length()).trim();

          if (params.size() > 0)
            {
              cmd.getOut().println ("Usage " + cmd.getAction());
              return;
            }

          Manager.eventLoop.requestStop();
        }
    }
    
	class UnaliasHandler implements CommandHandler
	{
		public void handle(Command cmd)  throws ParseException
		{
			Vector params = cmd.getParameters();
			if (params.size() == 1)
			{
				if ( ((String)params.elementAt(0)).equals("-all") )
				{
						aliases.clear();
						addMessage(new Message("Removing all aliases.", Message.TYPE_VERBOSE));
				}
				else
				{
					String temp = (String)params.elementAt(0);
					if (aliases.containsKey(temp))
					{
						aliases.remove(temp);
						addMessage(new Message("Removed alias \"" + temp + "\"", Message.TYPE_VERBOSE));
					}
					else
						addMessage(new Message("Alias \"" + temp + "\" not defined.", Message.TYPE_ERROR));
				}
			}
			else
			{
				addMessage(new Message("Usage: " + userhelp.getCmdSyntax(cmd.getAction()), Message.TYPE_NORMAL));
			}
		}
	}

	class SetHandler implements CommandHandler
	{
		public void handle(Command cmd) throws ParseException
		{
			Vector params = cmd.getParameters();
			String temp;
			if (params.size() == 3 && ((String)params.elementAt(1)).equals("=") )
			{
				temp = (String)params.elementAt(0);
				if (dbgvars.variableIsValid(temp))
				{
					if (dbgvars.valueIsValid(temp, (String)params.elementAt(2)))
					{
						dbgvars.setVariable(temp, (String)params.elementAt(2));
					}
					else
						addMessage(new Message("Illegal variable value.", Message.TYPE_ERROR));
				}
				else
					addMessage(new Message("Illegal debugger variable \"" + (String)params.elementAt(0) + "\"",
											Message.TYPE_ERROR));
			}
			else if (params.size() == 1)
			{
				temp = (String)params.elementAt(0);
				if (dbgvars.variableIsValid(temp))
				{
					addMessage(new Message(temp + " = " + dbgvars.getValue(temp).toString(), Message.TYPE_NORMAL));
				}
				else
					addMessage(new Message("Illegal debugger variable \"" + (String)params.elementAt(0) + "\"",
											Message.TYPE_ERROR));
			}
			else if (params.size() == 0)
			{
					addMessage(new Message(dbgvars.toString(), Message.TYPE_NORMAL));
			}
			else
			{
				addMessage(new Message("Usage: " + userhelp.getCmdSyntax(cmd.getAction()), Message.TYPE_NORMAL));
			}
		}
	}

	class UnsetHandler implements CommandHandler
	{
		public void handle(Command cmd) throws ParseException
		{
			Vector params = cmd.getParameters();
			String temp;
			if (params.size() == 1)
			{
				temp = (String)params.elementAt(0);
				if (temp.equals("-all"))
					dbgvars.unsetAll();
				else
				{
					if (dbgvars.variableIsValid(temp))
						dbgvars.unsetVariable(temp);
					else
						addMessage(new Message("\"" + (String)params.elementAt(0) + "\" is not a valid debugger variable",
											Message.TYPE_ERROR));
				}
			}
			else
			{
				addMessage(new Message("Usage: " + userhelp.getCmdSyntax(cmd.getAction()), Message.TYPE_NORMAL));
			}
		}
	}

	class UpDownHandler implements CommandHandler
    {
      public void handle(Command cmd) throws ParseException {
        int action;
        int level = 1;
        StackFrame tmpFrame = null;
        
        if (cmd.getParameters().size() != 0)
          level = Integer.parseInt((String)cmd.getParameters().elementAt(0));

        if (cmd.getAction().compareTo("up") == 0)
          {
            tmpFrame = symtab.setCurrentFrame(level);
            stackLevel += 1;
          }
        else if (cmd.getAction().compareTo("down") == 0)
          {
            tmpFrame = symtab.setCurrentFrame(-level);
            stackLevel -= 1;
          }
        cmd.getOut().print("#" + stackLevel);
        cmd.getOut().print(" 0x" + Integer.toString((int)tmpFrame.getAddress(), 16));
        cmd.getOut().print(" in " + tmpFrame.getMethodName());
        cmd.getOut().print(" at " + tmpFrame.getSourceFile());
        cmd.getOut().println(":" + tmpFrame.getLineNumber());        
      }
    }
  
    class WhereHandler implements CommandHandler
    {
      public void handle(Command cmd) throws ParseException {
        int action;
        int level = 0;
        StackFrame tmpFrame = null;
        
        if (cmd.getParameters().size() != 0)
          level = Integer.parseInt((String)cmd.getParameters().elementAt(0));
 
        int l = stackLevel;
	int stopLevel;
	if (level > 0)
	  stopLevel = l + level;
	else
	  stopLevel = 0;
	tmpFrame = symtab.getCurrentFrame();
	while (tmpFrame != null)
	    {
              cmd.getOut().print("# " + l);
	      cmd.getOut().print(" 0x" + Integer.toString((int)tmpFrame.getAddress(), 16));
	      cmd.getOut().print(" in " + tmpFrame.getMethodName());
	      cmd.getOut().print(" at " + tmpFrame.getSourceFile());
	      cmd.getOut().println(":" + tmpFrame.getLineNumber());
	      tmpFrame = tmpFrame.getOuter();
	      l += 1;
	      if (l == stopLevel)
                break;
            }
      }
    }
    
    class WhatHandler implements CommandHandler
    {
      public void handle(Command cmd) throws ParseException {
        if (cmd.getParameters().size() == 0)
          return;
        String sInput = ((String)cmd.getParameters().elementAt(0));
        try 
        {
          cmd.getOut().println(symtab.what(sInput));
        }
        catch (NameNotFoundException nnfe)
        {
          addMessage(new Message(nnfe.getMessage(),
                                 Message.TYPE_ERROR));
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
        Vector params = cmd.getParameters();
        boolean haveFormat = false;
        int outputFormat = DECIMAL;
        
        String sInput = cmd.getFullCommand().substring(cmd.getAction().length()).trim();

        for (int i = 0; i < params.size(); i++)
          {
            if (((String)params.elementAt(i)).equals("-format"))
              {
                haveFormat = true;
                i += 1;
                String arg = ((String)params.elementAt(i));
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

        if (sInput.length() == 0) {
          cmd.getOut().println ("Usage " + cmd.getAction() + " Expression [-format d|x|o]");
          return;
        }

        if (cmd.getAction().compareTo("assign") == 0) {
          int i = sInput.indexOf(' ');
          if (i == -1) {
            cmd.getOut().println ("Usage: assign Lhs Expression");
            return;
          }
          sInput = sInput.substring(0, i) + "=" + sInput.substring(i);
        }        

        Variable result = symtab.print(sInput);
        if (result == null)
          {
            addMessage(new Message("Variable " + sInput + " not found in scope", Message.TYPE_ERROR));
            return;
          }

        try
        {
          switch (outputFormat)
          {
          case HEX: 
            cmd.getOut().print("0x");
            break;
          case OCTAL: 
            cmd.getOut().print("0");
            break;
          }
          if (result.getType().getTypeId() == BaseTypes.baseTypeFloat)
            cmd.getOut().println(String.valueOf(result.getFloat()));
          else if (result.getType().getTypeId() == BaseTypes.baseTypeDouble)
            cmd.getOut().println(String.valueOf(result.getDouble()));
          else
            cmd.getOut().println(Integer.toString((int)result.getType().longValue(result),outputFormat));
        }
        catch (InvalidOperatorException ioe)
        {
        }
      }
    }
    
	class QuitHandler implements CommandHandler
    {
		public void handle(Command cmd) throws ParseException {
		       addMessage(new Message("Quitting...", Message.TYPE_NORMAL));
		}
	}
	class HelpHandler implements CommandHandler
	{
		public void handle(Command cmd) throws ParseException {
			String output = "";
			String temp = "";

			for (Iterator iter = userhelp.getCmdList().iterator(); iter.hasNext();)
			{
				temp = (String)iter.next();
				output += temp + " - " + userhelp.getCmdDescription(temp) + "\n";
			}
 		    output += "help\n";
		    output += "quit\n";

		    addMessage(new Message(output, Message.TYPE_NORMAL));
		}
	}
    
	/*
	 * Private variables
	 */

	private static PrintStream out = null;// = System.out;
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
	private LinkedList messages; // debugger output messages, e.g. the Message class

	// alias
	private Hashtable aliases;

	/*
	 * Public methods
	 */

	/**
	 * Constructor
	 * @param prompt String initially to be used as the prompt
	 */
	public CLI(String prompt, PrintStream out)
	{
		this.prompt = prompt;
		CLI.out = out;

		prepro = new Preprocessor();
		handlers = new Hashtable();
		userhelp = new UserHelp();
		dbgvars = new DbgVariables();
        handlers.put("alias", new AliasHandler());
        handlers.put("assign", new PrintHandler());
        handlers.put("attach", new AttachHandler());
        handlers.put("defset", new DefsetHandler());
        handlers.put("detach", new DetachHandler());
        handlers.put("down", new UpDownHandler());
		handlers.put("focus", new FocusHandler());
        handlers.put("help", new HelpHandler());
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

		// initialize PT set stuff
		setparser = new SetNotationParser();

		allset = new AllPTSet();
		targetset = allset;

		builtinPTSets = new Hashtable();
		builtinPTSets.put("all", allset);

		namedPTSets = new Hashtable();
		namedPTSets.toString(); // placeholder so compiler doesn't give unused variable warnings

		messages = new LinkedList();

		//initialize alias table
		aliases = new Hashtable();
		aliases.toString(); // placeholder so compiler doesn't give unused variable warnings
	}

	public String getPrompt()
	{
		return prompt;
	}

	public String execCommand(String cmd)
	{
		String pcmd = ""; //preprocessed command
		Command command;
		CommandHandler handler = null;

		if (cmd != null)
		{
			try
			{
				for (Iterator iter = prepro.preprocess(cmd); iter.hasNext();) //preprocess and iterate
				{
					pcmd = (String)iter.next();
					command = new Command(pcmd);

					command.setOut (CLI.out);
					if (command.getAction() != null)
					{
						handler = (CommandHandler)handlers.get(command.getAction());
						if (handler != null)
								handler.handle(command);
						else
							addMessage(new Message("Unrecognized command: " + command.getAction() + ".",
													Message.TYPE_ERROR));
					}
					else
					{
						addMessage(new Message("No action specified.", Message.TYPE_ERROR));
					}
				}

			}
			catch (ParseException e)
			{	
				String msg = "";
				if (e.getMessage() != null)
					msg = e.getMessage();

				addMessage(new Message(msg, Message.TYPE_ERROR));
			}
			catch (RuntimeException e)
			{
				e.printStackTrace();
				String msg = "";
				if (e.getMessage() != null)
					msg = e.getMessage();

				addMessage(new Message(msg, Message.TYPE_DBG_ERROR));
			}

			flushMessages();
		}

		return null;
	}

	private void addMessage(Message msg)
	{
		messages.add(msg);
	}
	
	private void flushMessages()
	{
		String prefix = "";
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

			out.println(prefix + tempmsg.getMessage());

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

			addMessage( new Message("Creating new " + parsed.getName() + " state set.", Message.TYPE_VERBOSE));
		}
		else if (parsed.getType() == ParsedSet.TYPE_HPD)
		{
			if (parsed.isStatic())
				result = new StaticPTSet(allset.getSubset(parsed.getParseTreeNodes()));
			else
				result = new DynamicPTSet(allset, parsed.getParseTreeNodes());

			addMessage( new Message("Creating new HPD notation set.", Message.TYPE_VERBOSE));
		}
		else if (parsed.getType() == ParsedSet.TYPE_NAMED)
		{
			if (parsed.isStatic())
			{
				addMessage( new Message("Cannot create a static set from a predefined set.",
											Message.TYPE_ERROR));
			}
			else
			{
				addMessage( new Message("Creating new set from named set \"" +
											parsed.getName() + "\".", Message.TYPE_VERBOSE));
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
			addMessage( new Message("Creating new set from executable \"" +
											parsed.getName() + "\".", Message.TYPE_VERBOSE));
		}

		return result;
	}

	/**
	 * Main function, renamed to not get caught by the build system
	 */
	/*
	public static void in(String[] args)
	{
		CLI dbg = new CLI("cli$ ");
		ConsoleReader reader = null; // the jline reader
		String line = "";

		try {
			reader = new ConsoleReader();
		}
		catch (IOException ioe)	{
			out.println("ERROR: Could not create a command line");
			out.print(ioe.getMessage());
		}

		try {
			while (line != null && !line.equals("quit")) {
				line = reader.readLine(dbg.getPrompt());
				dbg.execCommand(line);
			}

		}
		catch (IOException ioe) {
			out.println("ERROR: Could not read from command line");
			out.print(ioe.getMessage());
		}
	}
	*/
}
