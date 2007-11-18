// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

import java.util.List;

/**
 * A handler class for the CLI that supplies its own help messages.
 */

public class TopLevelCommand extends MultiLevelCommand {
    /**
     * Implement top-level help.
     */
    private class Help extends Command {
	Help() {
	    super("help", "Display this help message.", "help [command]",
		  "Display help (possibly for a command.)");
	}
	
	public void interpret(CLI cli, Input cmd) {
	    TopLevelCommand.this.help(cli, cmd);
	}
	
	/**
	 * Complete the line, throw problem back at the top level
	 * command.
	 */
	int complete(CLI cli, Input buffer, int cursor, List candidates) {
	    return TopLevelCommand.this.complete(cli, buffer, cursor,
						 candidates);
	}
    }

    TopLevelCommand() {
	super("<top-level", "top level command",
	      "<command> <parameter> ...",
	      "a top level command");
        add(new ActionPointCommands.Actions(), "actions");
        add(new ActionPointCommands.Delete(), "delete");
        add(new ActionPointCommands.Disable(), "disable");
        add(new ActionPointCommands.Enable(), "enable");
        add(new AliasCommands.Alias(), "alias");
        add(new AliasCommands.Unalias(), "unalias");
        add(new AttachCommand(), "attach");
        add(new BreakpointCommand(), "b|reak");
        add(new CoreCommand(), "core");
        add(new DbgVariableCommands.Set(), "set");
        add(new DbgVariableCommands.Unset(), "unset");
        add(new DebuginfoCommand(), "debuginfo");
        add(new DetachCommand(), "detach");
        add(new DisassembleCommand(), "disassemble");
        add(new DisplayCommand(), "display");
        add(new EvalCommands.Assign(), "assign");
        add(new EvalCommands.Print(), "p|rint");
        add(new ExamineCommand(), "examine");
        add(new FocusCommand(), "focus");
        add(new GoCommand(), "g|o");
        add(new HaltCommand(), "h|alt");
        add(new Help(), "help");
        add(new ListCommand(), "l|ist");
        add(new LoadCommand(), "load");
        add(new PeekCommand(), "peek");
        add(new PlocationCommand(), "plocation");
        add(new PtypeCommand(), "ptype");
        add(new QuitCommand("exit"), "exit");
        add(new QuitCommand("quit"), "quit");
        add(new RegsCommand(), "regs");
        add(new RunCommand(), "r|un");
        add(new StackCommands.Down(), "d|own");
        add(new StackCommands.Frame(), "frame");
        add(new StackCommands.Up(), "u|p");
        add(new StackCommands.Where(), "w|here");
        add(new StepCommand(), "s|tep");
        add(new StepFinishCommand(), "finish");
        add(new StepInstructionCommand(), "stepi");
        add(new StepNextCommand(), "n|ext");
        add(new StepNextiCommand(), "nexti");
        add(new WhatCommand(), "what");

        add(new WhichsetsCommand(), "whichsets");
        add(new ViewsetCommand(), "viewset");
        add(new DefsetCommand(), "defset");
        add(new UndefsetCommand(), "undefset");
    }
}
