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

package frysk.cli.hpd;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NameNotFoundException;

import frysk.debuginfo.DebugInfo;
import frysk.dwfl.DwflCache;
import frysk.stack.Frame;

import lib.dwfl.Dwfl;
import lib.dwfl.SymbolBuilder;
import lib.opcodes.Disassembler;
import lib.opcodes.Instruction;

public class DisassembleCommand extends CLIHandler {

    private static String name = "disassemble";

    private static String description = "disassemble a section of memory";

    public DisassembleCommand(CLI cli) {
	super(name, cli, new CommandHelp(name, description,
		"disassemble [startAddress] [endAddress]", description));
    }

    public void handle(Command cmd) throws ParseException {
	Frame currentFrame = getCLI().frame;

	long currentInstruction = currentFrame.getAddress();

	Disassembler disassembler = new Disassembler(getCLI().getTask()
		.getMemory());

	ArrayList params = cmd.getParameters();

	if (params.size() == 1) {
	    // currentInstruction = Long.parseLong((String) params.get(0));
	    try {
		String sInput = (String) params.get(0);
		if (cli.debugInfo != null)
		    currentInstruction = cli.debugInfo.print(sInput, cli.frame)
			    .getLong();
		else
		    currentInstruction = DebugInfo.printNoSymbolTable(sInput)
			    .getLong();
	    } catch (NameNotFoundException nnfe) {
		cli.addMessage(new Message(nnfe.getMessage(),
			Message.TYPE_ERROR));
		return;
	    }
	} else if (params.size() == 2) {
	    long startInstruction = Long.parseLong((String) params.get(0));
	    long endInstruction = Long.parseLong((String) params.get(1));
	    List instructions = disassembler.disassembleInstructionsStartEnd(
		    startInstruction, endInstruction);
	    printInstructions(-1, instructions);
	    return;
	} else if (params.size() > 2) {
	    throw new RuntimeException("too many arguments to disassemble");
	}

	DisassembleSymbol symbol = new DisassembleSymbol(disassembler);
	Dwfl dwfl = DwflCache.getDwfl(getCLI().getTask());
	dwfl.getModule(currentInstruction)
		.getSymbol(currentInstruction, symbol);
	printInstructions(currentInstruction, symbol.instructions);
    }

    /**
         * Print a list of instructions, highlighting the current instruction.
         * 
         * @param currentAddress
         * @param instructions
         */
    private void printInstructions(long currentAddress, List instructions) {

	Iterator iter = instructions.iterator();

	while (iter.hasNext()) {
	    Instruction instruction = (Instruction) iter.next();
	    if (instruction.address == currentAddress)
		DisassembleCommand.this.cli.outWriter
			.println("*" + instruction);
	    else
		DisassembleCommand.this.cli.outWriter
			.println(" " + instruction);
	}
    }

    private class DisassembleSymbol implements SymbolBuilder {
	private Disassembler disassembler;

	private List instructions;

	private DisassembleSymbol(Disassembler disassembler) {
	    this.disassembler = disassembler;
	}

	public void symbol(String name, long value, long size, int type,
		int bind, int visibility) {
	    instructions = disassembler.disassembleInstructionsStartEnd(value,
		    value + size);
	}

    }
}
