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
		
		temp = 
"The alias command associates a user-defined name with a list of one or\n" +
"more debugger commands. After definition, the user-defined command can\n" +
"be used in the same way as a debugger-defined command, including as part\n" +
"of the definition of new user-defined commands. ";
        addHelp("alias", "Create or view user-define commands.",
                "alias command-name command-body\nalias [command-name]", temp);

        temp = 
"The assign command evaluates a scalar expression and uses the result to\n" +
"replace the previous contents of a program variable. The target location\n" +
"may be a scalar variable, an element of an array or structure/record, or\n" +
"a de-referenced pointer variable.";
        addHelp("assign", "Change the value of a scalar program variable.",
		"assign scalar-target scalar-value [-force]", temp);
        
        temp = 
"The attach command causes the debugger to attach to an existing\n" +
"process(es), making it possible to continue the process' execution under\n" +
"debugger control. The command applies at the process level; all threads\n" +
"corresponding to the process will be attached by the operation. It is\n" +
"the user's responsibility to ensure that the process(es) actually is\n" +
"executing the specified executable.";
        addHelp("attach", "Attach to a running process.",
		"attach [executable] pid [-task tid] [-cli]", temp);
        
        temp = 
"Associates a logical name with a group of threads and/or processes,\n" +
"creating a user-defined set. Once a user-defined set has been\n" +
"established, it can be used (enclosed in brackets) as a p/t set prefix\n" +
"or as the argument to a focus command, providing a shorthand way of\n" +
"referring to potentially complex groupings of processes and threads. ";
	addHelp("defset", "Assign a set name to a group of processes/threads",
		"defset set-name p/t-set", temp);

	temp = 
"The detach command detaches the debugger from all processes in the\n" +
"affected set. This serves to undo the effects of attaching the debugger\n" +
"to a running process; that is, the debugger releases all control over\n" +
"the process, eliminates all debugger state information related to it,\n" +
"and allows it to continue execution in the normal run-time\n" +
"environment. ";
        addHelp("detach", "Detach from a running process.",
		"detach", temp);
        
        temp = 
"Changes the current p/t set. As a consequence, subsequent commands will\n" +
"apply to just the threads specified in the argument of this\n" +
"command. When no argument is specified, the command lists the threads in\n" +
"the current p/t set. ";
        addHelp("focus", "Change the current process/thread set.",
		"focus [p/t-set]", temp);

        temp = 
"The list command displays lines of source code. The user can control\n" +
"both the location in the source code and the number of lines\n" +
"displayed. Successive list commands without location arguments result in\n" +
"the display of consecutive sequences of source lines.";
        addHelp("list",	"Display source code lines.",
		"list source-loc [-length [-]num-lines]", temp);
        
        temp = 
"The print command evaluates and displays an expression. The debugger\n" +
"interprets the expression by looking up the value(s) associated with\n" +
"each symbol and applying the operators.  The result of an expression may\n" +
"be a scalar value or an aggregate (array, array slice, record, or\n" +
"structure.";
        addHelp("print", "Evaluate and display the value of a program variable or expression.",
		"print expression [-name] [-index] [-format d|o|x]", temp);
        
        temp = 
"The set command supports the viewing of debugger state variables and the\n" +
"assignment of new values to them.  When no arguments are specified, the\n" +
"names and current values for all debugger state variables are\n" +
"displayed. When just a single argument is included, the debugger echoes\n" +
"the variable name and displays its current value.  The second argument\n" +
"defines the value that should replace any previous value for that\n" +
"variable.  It must be enclosed in quotes if it contains multiple\n" +
"words. ";
        addHelp("set", "Change or view a debugger variable.",
		"set debugger-var = value\nset [debugger-var]", temp);

        temp = 
"The unalias command removes the alias that was previously established\n" +
"for the specified user-defined command name. Use of the argument -all\n" +
"deletes all user-defined commands at once.";
        addHelp("unalias", "Create or view user-define commands.",
		"unalias { command-name | -all }", temp);

        temp = 
"The undefset command reverses the action of defset, so that the set is\n" +
"deleted. This command is applicable only to user-defined sets.";
        addHelp("undefset", "Undefine a previously defined process/thread set.",
		"undefset {set-name | -all}", temp);

        temp = 
"The unset command reverses the effects of any previous set operations,\n" +
"restoring the debugger state variable(s) to their default settings.\n" +
"When the argument -all is specified, the command affects all debugger\n" +
"state variables, restoring them to the original settings that were in\n" +
"effect when the debugging session began.  When just a single argument is\n" +
"included, only that variable is affected. ";
        addHelp("unset", "Revert variable value to default.",
		"unset { debugger-var | -all }", temp);
        
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
        
        temp = 
"The viewset command displays the members of debugger- or user-defined\n" +
"sets. When no argument is used, the members of all currently defined\n" +
"sets are displayed. ";
        addHelp("viewset", "List members of a proc/task set.",
		"viewset [set-name]", temp);

        temp = 
"The what command queries the debugger about its current interpretation\n" +
"of a symbol name from the target program. Intuitively, the command shows\n" +
"what program symbol(s) would be displayed (modified) if the symbol name\n" +
"were used as the argument of a print command.";
        addHelp("what", "Determine what a target program name refers to",
		"what symbol-name [-all]", temp);

        temp = 
"The where command displays the current execution location(s) and the\n" +
"call stack(s) - or sequence of procedure calls - which led to that\n" +
"point.";
        addHelp("where", "Display the current execution location and call stack",
		"where [ {num-levels | -all} ] [-args]", temp);
        
        temp = 
"The whichsets command displays sets to which a particular thread(s)\n" +
"belongs. When no argument is used, the membership of each thread in the\n" +
"target p/t set is displayed. If a thread(s) is specified as the\n" +
"argument, only its membership information will be displayed. ";
        addHelp("whichsets", "List all user-defined set to which a proc/task belongs.",
		"whichsets [p/t-set]", temp);

        temp = 
"Continue running a process, returning without blocking.  The go command\n" +
"resumes execution of a collection of processes. The prompt will then be\n" +
"returned so that the user can issue further commands; execution\n" +
"continues behind the scene.";
        addHelp("go", "Continue a process.",
		"go", temp);

        temp = 
"Stop a process which is already attached.  The halt command temporarily\n" +
"suspends the execution of a collection of processes.";
        addHelp("halt", "Stop a process.",
		"halt", temp);
        
        temp = 
"The break command defines a breakpoint that will be triggered when some\n" +
"thread(s) in the trigger set arrives at the specified location during\n" +
"program execution. When that occurs, the process(es) containing the\n" +
"triggering thread(s) plus all processes in the stop set will be forcibly\n" +
"stopped so the user can examine program state information.";
        addHelp("break", "Define a breakpoint",
		"break {proc | line | #file#line} [-stop stop-set]", temp);
        
        addHelp("exit", "Terminate the debugging session.",
		"exit", "Terminate the debugging session.");
        
        addHelp("quit", "Terminate the debugging session.",
		"quit", "Terminate the debugging session.");
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
