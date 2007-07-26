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

import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

import javax.naming.NameNotFoundException;

import frysk.Config;

import frysk.symtab.Symbol;
import frysk.symtab.SymbolFactory;

import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

import lib.opcodes.Disassembler;
import lib.opcodes.Instruction;

public class DisassembleCommand extends CLIHandler {

    public DisassembleCommand(CLI cli) {
	super(cli, "disassemble", "disassemble a section of memory",
		"disassemble [-i|--instructions-only] [startAddress] ||\n"
			+ "disassemble [-i|--instructions-only] "
			+ "<startAddress> <endAddress>",
		"disassemble the function surrounding the current pc, "
			+ "the function surrounding a given address, "
			+ "or a range of functions.");

	addOptions(parser);
    }

    void addOptions(Parser parser) {
	parser.add(new Option("instructions-only", 'i',
		"only print the instruction portion not the parameters") {

	    public void parsed(String argument) throws OptionException {
		instructionOnly = true;
	    }

	});

	parser.add(new Option("no-truncate", 't',
		"don't truncate the number of instructions printed") {

	    public void parsed(String argument) throws OptionException {
		truncate = false;
	    }

	});

	parser.add(new Option("no-symbol", 's', "don't print the symbol name") {

	    public void parsed(String argument) throws OptionException {
		symbol = false;
	    }
	});

	// XXX: Need these two to prevent help and version from exiting fhpd
	// when finished.
	parser.add(new Option("version", "display version number") {

	    public void parsed(String argument) throws OptionException {
		cli.outWriter.println(Config.getVersion());
		helpStop = true;
	    }
	});

	parser.add(new Option("help", "display help") {
	    public void parsed(String argument) throws OptionException {
		cli.outWriter.println(getHelp().toPrint());
		helpStop = true;
	    }
	});
    }

    private boolean instructionOnly = false;

    private boolean truncate = true;

    private boolean helpStop = false;

    private boolean symbol = true;

    Parser parser = new Parser("disassemble", Config.getVersion(), true);

    private void reset() {
	instructionOnly = false;
	helpStop = false;
	truncate = true;
	symbol = true;
    }

    public void handle(Command cmd) throws ParseException {
	reset();

	long currentInstruction = getCLI().frame.getAddress();
	Symbol symbol = getCLI().frame.getSymbol();

	Disassembler disassembler = new Disassembler(getCLI().getTask()
		.getMemory());

	Object[] objects = cmd.getParameters().toArray();

	String[] params = new String[objects.length];
	for (int i = 0; i < objects.length; i++)
	    params[i] = (String) objects[i];

	params = parser.parse(params);

	if (helpStop)
	    return;

	if (params.length == 1) {
	    try {
		currentInstruction = cli.parseValue((String) params[0])
			.getLong();
		symbol = SymbolFactory.getSymbol(cli.task, currentInstruction);
	    } catch (NameNotFoundException nnfe) {
		cli.addMessage(new Message(nnfe.getMessage(),
			Message.TYPE_ERROR));
		return;
	    }
	} else if (params.length == 2) {
	    long startInstruction, endInstruction;
	    try {
		startInstruction = cli.parseValue((String) params[0]).getLong();
		endInstruction = cli.parseValue((String) params[1]).getLong();
	    } catch (NameNotFoundException nnfe) {
		cli.addMessage(new Message(nnfe.getMessage(),
			Message.TYPE_ERROR));
		return;
	    }
	    cli.outWriter.println("Dump of assembler code from 0x"
		    + Long.toHexString(startInstruction) + " to 0x"
		    + Long.toHexString(endInstruction) + ":");
	    List instructions = disassembler.disassembleInstructionsStartEnd(
		    startInstruction, endInstruction);
	    printInstructions(-1, instructions, false);
	    return;
	} else if (params.length > 2) {
	    throw new RuntimeException("too many arguments to disassemble");
	}

	cli.outWriter.println("Dump of assembler code for function: "
		+ symbol.getName());
	List instructions;
	// XXX: Need a better way of handling symbol size = 0
	long padding = 100;
	if (symbol.getSize() == 0)
	    instructions = disassembler.disassembleInstructionsStartEnd(symbol
		    .getAddress(), currentInstruction + padding);
	else
	    instructions = disassembler.disassembleInstructionsStartEnd(symbol
		    .getAddress(), symbol.getAddress() + symbol.getSize());
	printInstructions(currentInstruction, instructions, truncate);
    }

    /**
         * Print a list of instructions, highlighting the current instruction.
         * 
         * @param currentAddress
         * @param instructions
         */
    private void printInstructions(long currentAddress, List instructions,
	    boolean truncate) {

	InstructionPrinter printer;
	printer = new AddressPrinter();

	if (symbol)
	    printer = new SymbolPrinter(printer);

	if (instructionOnly)
	    printer = new InstructionOnlyPrinter(printer);
	else
	    printer = new InstructionParamsPrinter(printer);
	int wrapLines = 10;

	HardList cache = null;
	if (truncate)
	    cache = new HardList(wrapLines * 2);

	Iterator iter = instructions.iterator();

	while (iter.hasNext()) {
	    Instruction instruction = (Instruction) iter.next();
	    if (cache != null)
		cache.add(instruction);
	    else
		printInstruction(currentAddress, instruction, printer);

	    if (instruction.address == currentAddress && truncate) {
		break;
	    }
	}

	if (!truncate) {
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
	    printInstruction(currentAddress, instruction, printer);
	}

	cli.outWriter.println("End of assembly dump");
    }

    private void printInstruction(long currentAddress, Instruction instruction,
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

	SymbolPrinter(InstructionPrinter decorator) {
	    this.printer = decorator;
	}

	public String toPrint(Instruction instruction) {
	    Symbol symbol = SymbolFactory.getSymbol(cli.task,
		    instruction.address);
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
	    return printer.toPrint(instruction) + instruction.toString();
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
}
