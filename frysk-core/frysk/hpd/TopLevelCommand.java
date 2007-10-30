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

/**
 * A handler class for the CLI that supplies its own help messages.
 */

public class TopLevelCommand extends MultiLevelCommand {
    TopLevelCommand(DbgVariables dbgvars) {
	super("<top-level", "top level command",
	      "<command> <parameter> ...",
	      "a top level command");
        add(new ActionsCommand());
        add(new AliasCommand());
        add(new AssignCommand());
        add(new AttachCommand());
        add(new BreakpointCommand());
        add(new DebuginfoCommand());
        add(new DefsetCommand());
        add(new DeleteCommand());
        add(new DetachCommand());
        add(new DisableCommand());
        add(new FrameCommands("down"));
        add(new EnableCommand());
        add(new StepFinishCommand());
        add(new FocusCommand());
        add(new GoCommand());
        add(new HaltCommand());
        add(new HelpCommand());
        add(new ListCommand());
        add(new StepNextCommand());
        add(new StepNextiCommand());
        add(new PrintCommand());
        add(new PlocationCommand());
        add(new PtypeCommand());
        add(new QuitCommand("quit"));
        add(new QuitCommand("exit"));
        add(new SetCommand(dbgvars));
        add(new StepCommand());
        add(new StepInstructionCommand());
        add(new UnaliasCommand());
        add(new UndefsetCommand());
        add(new UnsetCommand(dbgvars));
        add(new FrameCommands("up"));
        add(new ViewsetCommand());
        add(new WhatCommand());
        add(new WhereCommand());
        add(new WhichsetsCommand());
        add(new DisplayCommand());
        add(new RunCommand());
        add(new CoreCommand());
        add(new DisassembleCommand());
        add(new RegsCommand());
        add(new ExamineCommand());
        add(new LoadCommand());
        add(new PeekCommand());
    }
}