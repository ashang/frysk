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

class UserHelp
{
	class CommandHelp
	{
		String cmd;
		String descr;
		String syntax;
		String full;

		CommandHelp(String cmd, String descr, String syntax, String full)
		{
			this.cmd = cmd;
			this.descr = descr;
			this.syntax = syntax;
			this.full = full;
		}

		String getCommand()
		{
			return cmd;
		}
		
		String getDescription()
		{
			return descr;
		}

		String getSyntax()
		{
			return syntax;
		}

		String getFull()
		{
			return full;
		}
	}
	
	TreeMap commandHelp;

	public UserHelp()
	{
		String temp = null;
		
		temp = "Creates a new set \"set-name\" with members specified in \"p/t-set\" set notation.";
		commandHelp.put("defset", new CommandHelp("defset",
													"Define a named proc/task set.",
													"defset set-name p/t-set",
													temp));

		temp = "Undefine an earlier defined set \"set-name\", or ";
		temp += "all user-defined sets if option \"-all\" is present.";
		commandHelp.put("undefset", new CommandHelp("undefset",
													"Undefine a named proc/task set.",
													"undefset {set-name | -all}",
													temp));

		temp = "List the members of a proc/task set \"set-name\",";
		temp += "or of all user-defined sets if no set is specified.";
		commandHelp.put("viewset", new CommandHelp("viewset",
													"List members of a proc/task set.",
													"viewset [set-name]",
													temp));

		temp = "For every proc/task in \"p/t-set\" list user-defined set to which it belongs. ";
		temp += "If no \"p/t-set\" is specified default to the current target p/t-set.";
		commandHelp.put("whichsets", new CommandHelp("whichsets",
													"List all user-defined set to which a proc/task belongs.",
													"whichsets [p/t-set]",
													temp));
	}
}
