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

import frysk.value.Format;
import java.util.Iterator;
import frysk.proc.Task;
import frysk.value.Value;
import frysk.value.PointerType;
import frysk.value.Type;
import java.util.List;

/**
 * Evaluate an expression; in various forms.
 */
abstract class EvalCommands extends ParameterizedCommand {
    private class Options {
	Format format = Format.NATURAL;
	boolean dumpTree = false;
    }
    Object options() {
	return new Options();
    }

    EvalCommands(String name, String description, String syntax, String full) {
	super(name, description, syntax, full);
	add(new CommandOption.FormatOption() {
		void set(Object options, Format format) {
		    ((Options)options).format = format;
		}
	    });
	add(new CommandOption("dump-tree", "dump the expression AST") {
		void parse(String arg, Object options) {
		    ((Options)options).dumpTree = true;
		}
	    });
    }

    int complete(CLI cli, PTSet ptset, String incomplete, int base,
		 List candidates) {
	return CompletionFactory.completeExpression(cli, ptset, incomplete,
						    base, candidates);
    }

    static private void eval(CLI cli, PTSet ptset, String expression,
			     Options options) {
	if (expression.equals(""))
	    throw new InvalidCommandException("missing expression");
	Value result = null;
	Iterator taskDataIter = ptset.getTaskData();
	do {
	    Task task = null;
	    if (taskDataIter.hasNext()) {
		TaskData td = (TaskData)taskDataIter.next();
		task = td.getTask();
		td.toPrint(cli.outWriter, true);
		cli.outWriter.println();
	    }
	    try {
		result = cli.parseValue(task, expression, options.dumpTree);
	    } catch (RuntimeException nnfe) {
		cli.addMessage(nnfe.getMessage(), Message.TYPE_ERROR);
		continue;
	    }

	    Type t = result.getType();
	    if (t instanceof PointerType) {
		cli.outWriter.print("(");
		t.toPrint(cli.outWriter);
		cli.outWriter.print(") ");
	    }	
	    result.toPrint(cli.outWriter,
			   task == null ? null : task.getMemory(),
			   options.format);
	    cli.outWriter.println();
	} while (taskDataIter.hasNext());
    }

    static class Print extends EvalCommands {
	Print() {
	    super("print",
		 "Evaluate and display the value of an expression.",
		 "print expression [-format d|o|x|t]",
		 ("The print command evaluates and displays an expression. The"
		  + " debugger interprets the expression by looking up the"
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
	    super("assign", "Change the value of a scalar program variable.",
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
