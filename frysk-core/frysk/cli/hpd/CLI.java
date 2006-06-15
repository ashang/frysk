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
package frysk.cli.hpd;

import jline.*;
import java.io.*;
import java.util.*;
import java.text.ParseException;

public class CLI 
{
	/*
	 * Command handlers
	 */

	class LoadHandler implements CommandHandler
	{
		public void handle(Command cmd) throws ParseException {
			out.println("Executing load: " + cmd);
		}
	}
	class RunHandler implements CommandHandler
	{
		public void handle(Command cmd) throws ParseException {
			out.println("Executing run: " + cmd);
		}
	}
	class AliasHandler implements CommandHandler 
	{
		public void handle(Command cmd) throws ParseException {
			Vector param = cmd.getParameters();
			out.println(param);
		}
	}
	class UnaliasHandler implements CommandHandler
	{
		public void handle(Command cmd)  throws ParseException {
			out.println("Executing unalias: " + cmd);
		}
	}
	class SetHandler implements CommandHandler
	{
		public void handle(Command cmd) throws ParseException {
			out.println("Executing set: " + cmd);
		}
	}
	class UnsetHandler implements CommandHandler
	{
		public void handle(Command cmd) throws ParseException {
			out.println("Executing unset: " + cmd);
		}
	}
	class WhatHandler implements CommandHandler
	{
	        public void handle(Command cmd) throws ParseException {
		       SymTab.what(cmd);
		}
	}
        class QuitHandler implements CommandHandler
        {
	        public void handle(Command cmd) throws ParseException {
		       System.exit(1);
	        }
	}
	class HelpHandler implements CommandHandler
	{
		public void handle(Command cmd) throws ParseException {
		    // TODO Use a better commands data structure 
		    out.println("List of commands:");
		    out.println();
		    out.println("load");
		    out.println("run");
		    out.println("alias");
		    out.println("unalias");
		    out.println("set");
		    out.println("unset");
		    out.println("assign Lhs Expression");
		    out.println("print Expression");
		    out.println("what Lhs");
		    out.println("help");
		    out.println("quit");
		    out.println("exit");
		}
	}
    
	/*
	 * Private variables
	 */

	private static final PrintStream out = System.out;
	private Preprocessor prepro;
	private String prompt;
	private Hashtable handlers;

	/*
	 * Public methods
	 */

	/**
	 * Constructor
	 * @param prompt String initially to be used as the prompt
	 */
	public CLI(String prompt)
	{
		this.prompt = prompt;
		prepro = new Preprocessor();
		handlers = new Hashtable();

		handlers.put("load", new LoadHandler());
		handlers.put("run", new RunHandler());
		handlers.put("alias", new AliasHandler());
		handlers.put("unalias", new UnaliasHandler());
		handlers.put("set", new SetHandler());
		handlers.put("unset", new UnsetHandler());
		handlers.put("assign", new PrintHandler());
		handlers.put("print", new PrintHandler());
		handlers.put("what", new WhatHandler());
		handlers.put("help", new HelpHandler());
		handlers.put("exit", new QuitHandler());
		handlers.put("quit", new QuitHandler());
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
			for (Iterator iter = prepro.preprocess(cmd); iter.hasNext();) //preprocess and iterate
			{
 				pcmd = (String)iter.next();

				try {
					command = new Command(pcmd);

					if (command.getAction() != null)
					{
						handler = (CommandHandler)handlers.get(command.getAction());
						if (handler != null)
								handler.handle(command);
						else
							out.println("ERROR: Unrecognized command \"" +
									command.getAction() + "\"");
					}
					else
					{
						out.println("ERROR: No action specified");
					}
				}
				catch (ParseException e)
				{
					out.println(e.getMessage());
				}
			}
		}

		return null;
	}

	/**
	 * Main function
	 */
	public static void main(String[] args)
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
}
