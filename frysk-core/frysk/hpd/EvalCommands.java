// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007 Red Hat Inc.
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

import inua.eio.ByteBuffer;
import frysk.value.Format;
import java.util.Iterator;
import frysk.proc.Task;
import java.util.List;
import frysk.expr.Expression;
import java.io.PrintWriter;

/**
 * Evaluate an expression; in various forms.
 */
abstract class EvalCommands extends ParameterizedCommand {
    private static abstract class Printer {
	abstract void print(Expression e, PrintWriter writer, Format format,
			    ByteBuffer memory);
	static final Printer VALUE = new Printer() {
		void print(Expression e, PrintWriter writer, Format format,
			   ByteBuffer memory) {
		    e.getValue().toPrint(writer, memory, format, 0);
		    writer.println();
		}
	    };
	static final Printer LOCATION = new Printer() {
		void print(Expression e, PrintWriter writer, Format format,
			   ByteBuffer memory) {
		    e.getLocation().toPrint(writer);
		    writer.println();
		}
	    };
	static final Printer TYPE = new Printer() {
		void print(Expression e, PrintWriter writer, Format format,
			   ByteBuffer memory) {
		    writer.println(e.getType().toPrint());
		}
	    };
	static final Printer TREE = new Printer() {
		void print(Expression e, PrintWriter writer, Format format,
			   ByteBuffer memory) {
		    e.toPrint(writer);
		    writer.println();
		}
	    };
	static final Printer RAW = new Printer() {
		void print(Expression e, PrintWriter writer, Format format,
			   ByteBuffer memory) {
		    byte[] bytes = e.getLocation().toByteArray();
		    for (int i = 0; i < bytes.length; i++) {
			writer.print(i);
			writer.print(": ");
			writer.print(bytes[i]);
			writer.println();
		    }
		}
	    };
    }

    private class Options {
	Format format = Format.NATURAL;
	Printer printer = Printer.VALUE;
    }
    Object options() {
	return new Options();
    }

    EvalCommands(String description, String syntax, String full) {
	super(description, syntax, full);
	add(new CommandOption.FormatOption() {
		void set(Object options, Format format) {
		    ((Options)options).format = format;
		}
	    });
	add(new CommandOption("tree", "print the expression's AST") {
		void parse(String arg, Object options) {
		    ((Options)options).printer = Printer.TREE;
		}
	    });
	add(new CommandOption("location", "print the expression's location") {
		void parse(String arg, Object options) {
		    ((Options)options).printer = Printer.LOCATION;
		}
	    });
	add(new CommandOption("type", "print the expression's type") {
		void parse(String arg, Object options) {
		    ((Options)options).printer = Printer.TYPE;
		}
	    });
	add(new CommandOption("value",
			      "print the expression's value (default)") {
		void parse(String arg, Object options) {
		    ((Options)options).printer = Printer.VALUE;
		}
	    });
	add(new CommandOption("raw",
			      "print the expression's raw value") {
		void parse(String arg, Object options) {
		    ((Options)options).printer = Printer.RAW;
		}
	    });
    }

    int completer(CLI cli, Input input, int cursor, List candidates) {
	return CompletionFactory.completeExpression(cli, input, cursor,
						    candidates);
    }

    static private void eval(CLI cli, PTSet ptset, String expression,
			     Options options) {
	if (expression.equals(""))
	    throw new InvalidCommandException("missing expression");
	Iterator taskDataIter = ptset.getTaskData();
	do {
	    Task task = null;
	    if (taskDataIter.hasNext()) {
		TaskData td = (TaskData)taskDataIter.next();
		task = td.getTask();
		td.printHeader(cli.outWriter);
	    }
	    Expression result;
	    try {
		result = cli.parseExpression(task, expression);
	    } catch (RuntimeException e) {
		cli.outWriter.println();
		cli.printError(e);
		continue;
	    }
	    options.printer.print(result, cli.outWriter, options.format,
				  task == null ? null : task.getMemory());
	} while (taskDataIter.hasNext());
    }

    static class Print extends EvalCommands {
	Print() {
	    super("Evaluate and display the value of an expression.",
		  "print expression [-format d|o|x|t]",
		  ("The print command evaluates and displays an"
		   + " expression.  The debugger interprets the"
		   + " expression by looking up the"
		   + " value(s) associated with each symbol and applying the"
		   + " operators.  The result of an expression may be a scalar"
		   + " value or an aggregate (array, array slice, record, or"
		   + " structure."));
	}
	void interpret(CLI cli, Input input, Object options) {
	    eval(cli, cli.getCommandPTSet(input), input.stringValue(),
		 (Options)options);
	}
    }

    static class Assign extends EvalCommands {
	Assign() {
	    super("Change the value of a scalar program variable.",
		  "assign scalar-target scalar-value [-force]",
		  ("The assign command evaluates a scalar expression and"
		   + " uses the result to replace the previous contents"
		   + " of a program variable. The target location may be a"
		   + " scalar variable, an element of an array or"
		   + " structure/record, or a de-referenced pointer"
		   + " variable."));
	}
	void interpret(CLI cli, Input input, Object options) {
	    if (input.size() < 2)
		throw new InvalidCommandException("missing expression");
	    String lhs = input.parameter(0);
	    input.accept();
	    eval(cli, cli.getCommandPTSet(input),
		 "(" + lhs + ") = (" + input.stringValue() + ")",
		 (Options)options);
	}
    }
}
