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
     
package frysk.cli.hpd;

import java.text.ParseException;
import java.util.ArrayList;
import frysk.value.Value;
import frysk.debuginfo.DebugInfo;
import javax.naming.NameNotFoundException;
import lib.dw.BaseTypes;

class PrintCommand
    implements CommandHandler
{
    private final CLI cli;
    PrintCommand(CLI cli)
    {
	this.cli = cli;
    }

    private static final int DECIMAL = 10;
    private static final int HEX = 16;
    private static final int OCTAL = 8;    
    
    public void handle(Command cmd) throws ParseException
    {
	ArrayList params = cmd.getParameters();
	if (params.size() == 1 && params.get(0).equals("-help")) {
	    cli.printUsage(cmd);
	    return;
        }
	cli.refreshSymtab();
	if (cmd.getParameters().size() == 0
	    || (((String)params.get(0)).equals("-help"))) {
	    cli.printUsage(cmd);
	    return;
        }
	boolean haveFormat = false;
	int outputFormat = DECIMAL;

	String sInput 
	    = cmd.getFullCommand().substring(cmd.getAction().length()).trim();

	for (int i = 0; i < params.size(); i++) {
	    if (((String)params.get(i)).equals("-format")) {
		haveFormat = true;
		i += 1;
		String arg = ((String)params.get(i));
		if (arg.compareTo("d") == 0)
		    outputFormat = DECIMAL;
		else if (arg.compareTo("o") == 0)
		    outputFormat = OCTAL;
		else if (arg.compareTo("x") == 0)
		    outputFormat = HEX;
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
	try {
	  if (cli.debugInfo != null)
	    result = cli.debugInfo.print(sInput);
	  else
	    result = DebugInfo.printNoSymbolTable(sInput);
        }
	catch (NameNotFoundException nnfe)
	    {
		cli.addMessage(new Message(nnfe.getMessage(),
					   Message.TYPE_ERROR));
		return;
	    }
	if (result == null) {
	    cli.addMessage("Variable " + sInput + " not found in scope",
			   Message.TYPE_ERROR);
	    return;
	}

	switch (outputFormat) {
	case HEX: 
	    cli.outWriter.print("0x");
	    break;
	case OCTAL: 
	    cli.outWriter.print("0");
	    break;
	}
	int resultType = result.getType().getTypeId();
	if (resultType == BaseTypes.baseTypeFloat
	    || resultType == BaseTypes.baseTypeDouble)
	    cli.outWriter.println(result.toString());
	else if (resultType == BaseTypes.baseTypeShort
		 || resultType == BaseTypes.baseTypeInteger
		 || resultType == BaseTypes.baseTypeLong)
	    cli.outWriter.println(Long.toString(result.longValue(),
						outputFormat));
	else if (resultType == BaseTypes.baseTypeByte)
	    cli.outWriter.println(Integer.toString((int)result.longValue(),
						   outputFormat) + 
				  " '" + result.toString() + "'");
	else
	    cli.outWriter.println(result.toString());
    }
}
