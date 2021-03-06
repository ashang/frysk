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
import java.util.List;
import frysk.debuginfo.DebugInfoFrame;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.symtab.Symbol;
import frysk.symtab.SymbolFactory;
import lib.dwfl.Disassembler;
import lib.dwfl.Instruction;

public class DisassembleCommand extends ParameterizedCommand {

    static private class Options {
        boolean allInstructions = true;
        boolean full = false;
        boolean symbol = true;
    }

    Object options() {
        return new Options();
    }
    
    public DisassembleCommand() {
	super("disassemble a section of memory",
	      "disassemble  [startAddress] [--] [OPTIONS]||\n"
	      + "disassemble  "
	      + "<startAddress> <endAddress> [--] [-OPTIONS]",
	      "disassemble the function surrounding the current pc, "
	      + "the function surrounding a given address, "
	      + "or a range of functions.");
	add(new CommandOption("all-instructions", 'a',
		"only print the instruction portion not the parameters",
		"<Yes|no>") {
		void parse(String argument, Object options) {
		    ((Options)options).allInstructions = parseBoolean(argument);
		}
	    });
	add(new CommandOption("full-function", 'f',
			      "disassemble the entire function",
			      "<yes/No>") {
		void parse(String argument, Object options) {
		    ((Options)options).full = parseBoolean(argument);
		}
	    });
	add(new CommandOption("symbol", 's', "print the symbol name",
			      "<Yes|no>") {
		void parse(String argument, Object options) {
		    ((Options)options).symbol = parseBoolean(argument);
		}
	    });
    }

    void interpret(CLI cli, Input cmd, Object opts) {
        Options options = (Options)opts;
	PTSet ptset = cli.getCommandPTSet(cmd);
	Iterator taskDataIter = ptset.getTaskData();
	if (cmd.size() > 2)
	    throw new InvalidCommandException
		("too many arguments to disassemble");

	while (taskDataIter.hasNext()) {
	    TaskData data = (TaskData) taskDataIter.next();
	    Task task = data.getTask();

	    if (cli.getSteppingEngine().isTaskRunning(task))
		continue;
	    DebugInfoFrame frame = cli.getTaskFrame(task);
	    long currentInstruction = frame.getAddress();
	    Symbol symbol = frame.getSymbol();

	    Disassembler disassembler
                = new Disassembler(DwflCache.getDwfl(task), task.getMemory());
	    cli.outWriter.println("[" + data.getParentID() + "." + data.getID()
		    + "]");
	    if (cmd.size() == 1) {
		try {
		    currentInstruction
			= cli.parseExpression(task, cmd.parameter(0))
			.getValue().asLong();
		    symbol = SymbolFactory.getSymbol(task, currentInstruction);
		} catch (RuntimeException nnfe) {
		    cli.addMessage(new Message(nnfe.getMessage(),
					       Message.TYPE_ERROR));
		    continue;
		}
	    } else if (cmd.size() == 2) {
		long startInstruction, endInstruction;
		try {
		    startInstruction
			= cli.parseExpression(task, cmd.parameter(0))
			.getValue().asLong();
		    endInstruction
			= cli.parseExpression(task, cmd.parameter(1))
			.getValue().asLong();
		} catch (RuntimeException nnfe) {
		    cli.addMessage(new Message(nnfe.getMessage(),
					       Message.TYPE_ERROR));
		    continue;
		}
		cli.outWriter.println("Dump of assembler code from 0x"
			+ Long.toHexString(startInstruction) + " to 0x"
			+ Long.toHexString(endInstruction) + ":");
		List instructions = disassembler
			.disassembleInstructionsStartEnd(startInstruction,
                                                         endInstruction);
                options.full = true;
		printInstructions(cli, task, -1, instructions, options);
		continue;
	    }
	    cli.outWriter.println("Dump of assembler code for function: "
		    + symbol.getName());
	    List instructions;
	    // XXX: Need a better way of handling symbol size = 0
	    long padding = 100;
	    if (symbol.getSize() == 0) {
		instructions = disassembler
                    .disassembleInstructionsStartEnd(symbol.getAddress(),
                                                     (currentInstruction
                                                      + padding));
	    } else {
		instructions = disassembler
                    .disassembleInstructionsStartEnd(symbol.getAddress(),
                                                     (symbol.getAddress()
                                                      + symbol.getSize()));
	    }
	    printInstructions(cli, task, currentInstruction, instructions,
                              options);
	}
    }

    /**
         * Print a list of instructions, highlighting the current instruction.
         * 
         * @param currentAddress
         * @param instructions
         */
    private void printInstructions(CLI cli, Task task, long currentAddress,
				   List instructions, Options options) {
	InstructionPrinter printer;
	printer = new AddressPrinter();

	if (options.symbol)
	    printer = new SymbolPrinter(task, printer);

	if (options.allInstructions)
	    printer = new InstructionParamsPrinter(printer);
	else
	    printer = new InstructionOnlyPrinter(printer);
	int wrapLines = 10;

	HardList cache = null;
	if (!options.full)
	    cache = new HardList(wrapLines * 2);

	Iterator iter = instructions.iterator();

	while (iter.hasNext()) {
	    Instruction instruction = (Instruction) iter.next();
	    if (cache != null)
		cache.add(instruction);
	    else
		printInstruction(cli, currentAddress, instruction, printer);

	    if (instruction.address == currentAddress && !options.full) {
		break;
	    }
	}

	if (options.full) {
	    cli.outWriter.println("End of assembly dump");
	    return;
	}

	while (iter.hasNext() && wrapLines > 0) {
	    Instruction instruction = (Instruction) iter.next();
	    cache.add(instruction);
	    wrapLines--;
	}

	// Note the space in front of address, placeholder for * character for
	// current address.

	iter = cache.iterator();

	while (iter.hasNext()) {
	    Instruction instruction = (Instruction) iter.next();
	    printInstruction(cli, currentAddress, instruction, printer);
	}

	cli.outWriter.println("End of assembly dump");
    }

    private void printInstruction(CLI cli, long currentAddress,
				  Instruction instruction,
				  InstructionPrinter printer) {
	if (instruction.address == currentAddress)
	    cli.outWriter.print("*");
	else
	    cli.outWriter.print(" ");

	cli.outWriter.println(printer.toPrint(instruction));
    }

    class AddressPrinter implements InstructionPrinter {

	public String toPrint(Instruction instruction) {
	    return "0x" + Long.toHexString(instruction.address) + "\t";
	}
    }

    class SymbolPrinter implements InstructionPrinter {

	InstructionPrinter printer;

	private Task task;

	SymbolPrinter(Task task, InstructionPrinter decorator) {
	    this.task = task;
	    this.printer = decorator;
	}

	public String toPrint(Instruction instruction) {
	    Symbol symbol = SymbolFactory.getSymbol(task, instruction.address);
	    return printer.toPrint(instruction)
		    + "<"
		    + symbol.getName()
		    + "+"
		    + Long.toHexString(instruction.address
			    - symbol.getAddress()) + ">:\t";
	}
    }

    class InstructionParamsPrinter implements InstructionPrinter {
	InstructionPrinter printer;

	InstructionParamsPrinter(InstructionPrinter decorator) {
	    this.printer = decorator;
	}

	public String toPrint(Instruction instruction) {
	    return printer.toPrint(instruction) + instruction.instruction;
	}
    }

    class InstructionOnlyPrinter implements InstructionPrinter {
	InstructionPrinter printer;

	InstructionOnlyPrinter(InstructionPrinter decorator) {
	    this.printer = decorator;
	}

	public String toPrint(Instruction instruction) {
	    return printer.toPrint(instruction)
		    + instruction.instruction.split("\\s")[0];
	}
    }

    interface InstructionPrinter {
	String toPrint(Instruction instruction);
    }


    int completer(CLI cli, Input input, int cursor, List completions) {
	return CompletionFactory.completeExpression(cli, input, cursor,
						    completions);
    }

}
