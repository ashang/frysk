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
package frysk.hpd;

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

        
        temp = 
"The up (down) command modifies the current frame location(s) by adding\n" +
"(subtracting) num-levels. Call stack movements are all relative, so up\n" +
"effectively \"moves up\" (or back) in the call stack, to a frame that\n" +
"has existed longer, while down \"moves down\" in the call stack,\n" +
"following the progress of program execution.";
        addHelp("up", "Move up one or more levels in the call stack",
		"up [num-levels]", temp);
        addHelp("down", "Move down one or more levels in the call stack",
		"down [num-levels]", temp);
   
        addHelp("exit", "Terminate the debugging session.",
		"exit", "Terminate the debugging session.");
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
