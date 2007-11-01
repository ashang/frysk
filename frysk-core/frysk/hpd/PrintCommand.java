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

class PrintCommand
    extends Command
{
    PrintCommand() {
	this("print", "Evaluate and display the value of a program variable or expression.",
	     "print expression [-name] [-index] [-format d|o|x|t]", "The print command evaluates and displays an expression. The debugger\n" +
	     "interprets the expression by looking up the value(s) associated with\n" +
	     "each symbol and applying the operators.  The result of an expression may\n" +
	     "be a scalar value or an aggregate (array, array slice, record, or\n" +
	     "structure.");
    }
    
    PrintCommand (String name, String description, String syntax, 
		  String full) {
	super (name, description, syntax, full);
    }

    public void interpret(CLI cli, Input cmd) {
        PTSet ptset = cli.getCommandPTSet(cmd);
	boolean dumpTree = false;
	if (cmd.size() == 1 && cmd.parameter(0).equals("-help")) {
	    cli.printUsage(cmd);
	    return;
        }
	if (cmd.size() == 0
	    || ((cmd.parameter(0)).equals("-help"))) {
	    cli.printUsage(cmd);
	    return;
        }
        // Skip set specification, if any.  XXX: Should do this after
        // parameter parsing.
	String sInput = cmd.stringValue();

	Format format = null;
	for (int i = 0; i < cmd.size(); i++) {
	    if ((cmd.parameter(i)).equals("-format")) {
		i += 1;
		String arg = cmd.parameter(i);
		if (arg.compareTo("d") == 0) 
		    format = Format.DECIMAL;
		else if (arg.compareTo("o") == 0)
		    format = Format.OCTAL;
		else if (arg.compareTo("x") == 0) 
		    format = Format.HEXADECIMAL;
		else if (arg.compareTo("t") == 0)
		    format = Format.BINARY;
		else
		    throw new InvalidCommandException
			("unrecognized format: " + arg);
	    }
	    else if ((cmd.parameter(i)).equals("-dump-tree")) 
		dumpTree = true;
	}
	if (format != null)
	    sInput = sInput.substring(0,sInput.indexOf("-format"));
	else
	    format = Format.NATURAL;
	if (dumpTree == true)
	    sInput = sInput.substring(0,sInput.indexOf("-dump-tree"));

	if (sInput.length() == 0) {
	    cli.printUsage(cmd);
	    return;
	}

	if (cmd.getAction().compareTo("assign") == 0) {
	    int i = sInput.indexOf(' ');
	    if (i == -1) {
		cli.printUsage(cmd);          
		return;
	    }
	    sInput = sInput.substring(0, i) + "=" + sInput.substring(i);
	}        

	Value result = null;
        Iterator taskDataIter = ptset.getTaskData();
        boolean doWithoutTask = !taskDataIter.hasNext();
        while (doWithoutTask || taskDataIter.hasNext()) {
            TaskData td = null;
            Task task = null;
            if (!doWithoutTask) {
                td = (TaskData)taskDataIter.next();
                task = td.getTask();
                cli.outWriter.println("[" + td.getParentID() + "." + td.getID()
                                      + "]\n");
            }
            doWithoutTask = false;
            try {
                result = cli.parseValue(task, sInput, dumpTree);	  
            } catch (RuntimeException nnfe) {
		cli.addMessage(nnfe.getMessage(), Message.TYPE_ERROR);
                continue;
            }

	    // XXX: Would it be better to just always have some sort
	    // of fake task?
	    if (task == null)
		result.toPrint(cli.outWriter, null, format);
	    else {
		Type t = result.getType();
		if (t instanceof PointerType) {
		    cli.outWriter.print("(");
		    t.toPrint(cli.outWriter);
		    cli.outWriter.print(") ");
		}
		result.toPrint(cli.outWriter, task.getMemory(), format);
	    }	
	    cli.outWriter.println();
        }
        if (result == null) {
            cli.addMessage("Symbol \"" + sInput + "\" is not found in the current context.",
                           Message.TYPE_ERROR);
        }
    }

    int complete(CLI cli, Input input, int cursor, List candidates) {
	return CompletionFactory.completeFocusedExpression(cli, input, cursor,
							   candidates);
    }
}
