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

import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import frysk.value.Format;
import frysk.debuginfo.ValueUavailableException;
import frysk.debuginfo.VariableOptimizedOutException;
import java.util.Iterator;

import frysk.proc.UBigInteger;
import frysk.proc.Task;
import frysk.value.Value;
import javax.naming.NameNotFoundException;
import lib.dwfl.BaseTypes;

class PrintCommand
    implements CommandHandler
{
    private final CLI cli;
    PrintCommand(CLI cli)
    {
	this.cli = cli;
    }

    private int getFormat(Format f) {
	if (f == Format.HEXADECIMAL)
	    return 16;
	if (f == Format.OCTAL)
	    return 8;
	else return 10;
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
	Format format = Format.NATURAL;
	boolean haveFormat = false;

	String sInput 
	    = cmd.getFullCommand().substring(cmd.getAction().length()).trim();

	if (sInput.indexOf('$') != -1) 
	    format = Format.HEXADECIMAL;
	else
	    format = Format.DECIMAL;

	for (int i = 0; i < params.size(); i++) {
	    if (((String)params.get(i)).equals("-format")) {
		haveFormat = true;
		i += 1;
		String arg = ((String)params.get(i));
		if (arg.compareTo("d") == 0) 
		    format = Format.DECIMAL;
		else if (arg.compareTo("o") == 0)
		    format = Format.OCTAL;
		else if (arg.compareTo("x") == 0) 
		    format = Format.HEXADECIMAL;
	    }
	}
	if (haveFormat)
	    sInput = sInput.substring(0,sInput.indexOf("-format"));

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
                cli.addMessage(new Message(nnfe.getMessage(), Message.TYPE_ERROR));
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


	    if (format == Format.HEXADECIMAL) 
                cli.outWriter.print("0x");
	    else if (format == Format.OCTAL)
                cli.outWriter.print("0");

	    int resultType = result.getType().getTypeId();
            if (resultType == BaseTypes.baseTypeFloat
                || resultType == BaseTypes.baseTypeDouble)
                cli.outWriter.println(result.toString());
            else if (resultType == BaseTypes.baseTypeShort
                     || resultType == BaseTypes.baseTypeInteger
                     || resultType == BaseTypes.baseTypeLong)
                {
                    if (format == Format.DECIMAL)
                        cli.outWriter.println(Long.toString(result.longValue(),
                                                            getFormat(format)));
                    else
                        {
                            BigInteger bigInt = new BigInteger(Long.toString(result.longValue()));
                            cli.outWriter.println(UBigInteger.toString(bigInt, bigInt.bitLength() + 1, 16));
                        }
                }
            else if (resultType == BaseTypes.baseTypeByte)
                cli.outWriter.println(Integer.toString((int)result.longValue(),
                                                       getFormat(format)) + 
                                      " '" + (char)result.intValue() + "'");
            else {
                result.toPrint(cli.outWriter, task.getMemory(), format);
                cli.outWriter.println();
            }
        }
        if (result == null) {
            cli.addMessage("Symbol \"" + sInput + "\" is not found in the current context.",
                           Message.TYPE_ERROR);
        }
    }
}
