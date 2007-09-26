// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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
import java.util.ArrayList;
import frysk.debuginfo.ValueUavailableException;
import frysk.debuginfo.VariableOptimizedOutException;
import java.util.Iterator;
import frysk.proc.Task;
import frysk.value.Value;
import javax.naming.NameNotFoundException;

class PlocationCommand
    extends CLIHandler
{
    PlocationCommand(CLI cli)
    {
	this(cli, "plocation", "Display the location of a program variable or expression.",
		"plocation expression [-name] [-index]", "The plocation command " +
		"evaluates and displays the type of an expression. The debugger\n" +
		"interprets the expression by looking up the value(s) associated with\n" +
		"each symbol and applying the operators." +
		"Output is \"LocationType LocationName - Size byte(s)\"");
    }
    
    PlocationCommand (CLI cli, String name, String description, String syntax, 
	    String full)
    {
	super (cli, name, description, syntax, full);
    }

    public void handle(Command cmd) throws ParseException {
        PTSet ptset = cli.getCommandPTSet(cmd);
	ArrayList params = cmd.getParameters();
	if (params.size() == 1 && params.get(0).equals("-help")) {
	    cli.printUsage(cmd);
	    return;
        }
	if (cmd.getParameters().size() == 0
	    || (((String)params.get(0)).equals("-help"))) {
	    cli.printUsage(cmd);
	    return;
        }
        // Skip set specification, if any
        String commandString = cmd.getFullCommand()
            .substring(cmd.getFullCommand().indexOf(cmd.getAction()));
	String sInput 
            = commandString.substring(cmd.getAction().length()).trim();

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
                result = cli.parseValue(task, sInput);	  
            }
            catch (NameNotFoundException nnfe) {
                continue;
            }
            catch (ValueUavailableException vue) {
                cli.addMessage("Symbol \"" + sInput + "\" is not available in the current context.",
                               Message.TYPE_ERROR);
                continue;
            }
            catch (VariableOptimizedOutException vooe) {
                cli.addMessage("Value of symbol \"" + sInput + "\" is optimized out.",
                               Message.TYPE_ERROR);
                continue;
            }
	    result.getLocation().toPrint(cli.outWriter);
	    cli.outWriter.println();
        }
        if (result == null) {
            cli.addMessage("Symbol \"" + sInput + "\" is not found in the current context.",
                           Message.TYPE_ERROR);
        }
    }
}