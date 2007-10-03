// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat Inc.
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

import java.util.TreeMap;
import java.util.ArrayList;

class UserHelp
{
  TreeMap commandHelp;
  void addHelp(String commandName, String description, String syntax,
	       String full)
  {
    addHelp(commandName,
	    new CommandHelp(commandName, description, syntax, full));
  }

  public void addHelp(String commandName, CommandHelp help)
  {
    commandHelp.put(commandName, help);
  }
  
	public UserHelp()
	{
		String temp = null;
		commandHelp = new TreeMap();
		
        temp = "Create or view user-define commands.";
        addHelp("alias",
		"Create or view user-define commands.",
		("\nCreate alias: alias command-name command-body\n"+
		 "View alias: alias [command-name]"),
		temp);

        temp = "Evaluate expression and assign value to variable.";
        addHelp("assign",
		"Assign value of expression.",
		"assign variable expression",
		temp);
        
        temp = "Attach to a running process.";
        addHelp("attach",
		"Attach to a running process.",
		"attach executable pid {-task tid} {-cli}",
		temp);
        
		temp = "Creates a new set \"set-name\" with members specified in \"p/t-set\" set notation.";
		addHelp("defset",
			"Define a named proc/task set.",
			"defset set-name p/t-set",
			temp);

        temp = "Detach from a running process.";
        addHelp("detach",
		"Detach from a running process.",
		"detach",
		temp);
        
        temp = "Change the current p/t set to \"p/t-set\". If no set notation is given list members of the current set";
        addHelp("focus",
		"Change current set.",
		"focus [p/t-set]",
		temp);

        temp = "Display source code lines.";
        addHelp("list",
		"Display source code lines.",
		"\nList source: list\nlist",
		temp);
        
        temp = "Print value of expression.";
        addHelp("print",
		"Print value of expression.",
		"print expression { -format x|d|o }",
		temp);
        
        temp = "Change value or \"debugger-var\" to \"value\". View value of \"debugger-var\" variable or " + 
        "of all variables if none is specified.";
        addHelp("set",
		"Change or view a debugger variable.",
		("\nChange variable: set debugger-var = value\n" +
		 "View variable: set [debugger-var]"),
		temp);

        temp = "Remove previously defined command.";
        addHelp("unalias",
		"Create or view user-define commands.",
		"unalias { command-name | -all }",
		temp);

        temp = "Undefine an earlier defined set \"set-name\", or ";
        temp += "all user-defined sets if option \"-all\" is present.";
        addHelp("undefset",
		"Undefine a named proc/task set.",
		"undefset {set-name | -all}",
		temp);

        temp = "Revert value of \"debugger-var\" variable to default, or of all variables if \"-all\" is present instead.";
        addHelp("unset",
		"Revert variable value to default.",
		"unset { debugger-var | -all }",
		temp);
        
        temp = "List the members of a proc/task set \"set-name\",";
        temp += "or of all user-defined sets if no set is specified.";
        addHelp("viewset",
		"List members of a proc/task set.",
		"viewset [set-name]",
		temp);

        temp = "Print data type of expression.";
        addHelp("what",
		"Print data type of expression.",
		"what expression",
		temp);

        temp = "For every proc/task in \"p/t-set\" list user-defined set to which it belongs. ";
        temp += "If no \"p/t-set\" is specified default to the current target p/t-set.";
        addHelp("whichsets",
		"List all user-defined set to which a proc/task belongs.",
		"whichsets [p/t-set]",
		temp);

        temp = "Continue running a process, returning without blocking.";
        addHelp("go",
		"Continue a process.",
		"go",
		temp);

        temp = "Stop a process.";
        addHelp("halt",
		"Stop a process which is already attached.",
		"halt",
		temp);
	}

	public boolean isValidCommand(String cmd)
	{
		return commandHelp.containsKey(cmd);
	}

	public ArrayList getCmdList()
	{
		return new ArrayList(commandHelp.keySet());
	}

	public String getCmdDescription(String cmd)
	{
		return ((CommandHelp)commandHelp.get(cmd)).getDescription();
	}

	public String getCmdSyntax(String cmd)
	{
		return ((CommandHelp)commandHelp.get(cmd)).getSyntax();
	}

	public String getCmdFullDescr(String cmd)
	{
		return ((CommandHelp)commandHelp.get(cmd)).getFull();
	}
}
