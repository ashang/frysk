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

import java.util.Iterator;
import frysk.value.Value;
import frysk.value.Format;
import frysk.isa.ISA;
import frysk.isa.registers.RegisterGroup;
import frysk.isa.registers.Registers;
import frysk.isa.registers.RegistersFactory;
import java.util.List;

public class RegsCommand extends ParameterizedCommand {

    
    String group = "";
    
    public RegsCommand() {
	super("print registers", "regs group", "print out "
	      + "registers in the given group, general registers "
	      + "printed by default.");
    }

    public RegsCommand(String groupname) {
	super("print registers", "regs group", "print out "
	      + "registers in the given group, general registers "
	      + "printed by default.");
	this.group = groupname;
    }

    void interpret(CLI cli, Input cmd, Object options) {
	String groupName = "";
	PTSet ptset = cli.getCommandPTSet(cmd);
	Iterator taskDataIter = ptset.getTaskData();
	while (taskDataIter.hasNext()) {
	    TaskData td = (TaskData) taskDataIter.next();
	    ISA isa = td.getTask().getISA();
	    Registers regs = RegistersFactory.getRegisters(isa);
	    RegisterGroup selectedGroup = regs.getGeneralRegisterGroup();
	    
	    if (!this.group.equals(""))
		groupName = this.group;
	    
	    if (cmd.size() > 0) 
		groupName = cmd.parameter(0);
		
	    if (!groupName.equals("")) {
		selectedGroup = regs.getGroup(groupName);
		if (selectedGroup == null) {
		    StringBuffer b = new StringBuffer();
		    b.append("Register group <");
		    b.append(groupName);
		    b.append("> not recognized; possible groups are:");
		    String[] groupNames = regs.getGroupNames();
		    for (int r = 0; r < groupNames.length; r++) {
			b.append(" ");
			b.append(groupNames[r]);
		    }
		    cli.addMessage(b.toString(), Message.TYPE_ERROR);
		    return;
		}
	    }
	    cli.outWriter.println("[" + td.getParentID() + "." + td.getID()
		    + "]");
	    for (int i = 0; i < selectedGroup.getRegisters().length; i++) {
		cli.outWriter.print(selectedGroup.getRegisters()[i].getName());
		cli.outWriter.print(":\t");
		try {
		    Value r = (cli.getTaskFrame(td.getTask())
			       .getRegisterValue(selectedGroup.getRegisters()[i]));
		    r.toPrint(cli.outWriter, Format.NATURAL);
		    cli.outWriter.print("\t");
		    r.toPrint(cli.outWriter, Format.HEXADECIMAL);
		} catch (RuntimeException e) {
		    cli.outWriter.print(e.getMessage());
		}
		cli.outWriter.println();
	    }
	}
    }

    int completer(CLI cli, Input input, int cursor, List completions) {
	return -1;
    }
}
