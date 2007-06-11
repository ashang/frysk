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

import java.util.ArrayList;

import frysk.stack.Frame;
import lib.dw.DwarfDie;
import java.io.IOException;
import java.io.FileReader;
import java.io.File;
import java.io.LineNumberReader;
import javax.naming.NameNotFoundException;
import java.text.ParseException;

/**
 * Implement the "list" source command.
 */

class ListCommand
    implements CommandHandler
{
    private CLI cli;
    ListCommand (CLI cli)
    {
	this.cli = cli;
    }
    private File file = null;
    private int line;
    private int exec_line = 0;
    public void handle(Command cmd) throws ParseException
    {
	ArrayList params = cmd.getParameters();
	int windowSize = 20;
	if (params.size() == 1 && params.get(0).equals("-help")) {
	    cli.printUsage(cmd);
	    return;
	}
	cli.refreshSymtab();
	if (cli.proc == null) {
	    cli.addMessage("No symbol table is available.",
			   Message.TYPE_NORMAL);
	    return;
	}
	if (params.size() == 1) {
	    // list N
	    try {
		line = Integer.parseInt((String)params.get(0));
	    }
	    catch (NumberFormatException ignore) {
		if (((String)params.get(0)).compareTo("$EXEC") == 0)
		    line = cli.debugInfo.getCurrentFrame().getLines()[0].getLine() - 10;
		else {
		    DwarfDie funcDie = null;
		    try {
			funcDie = cli.debugInfo.getSymbolDie((String)params.get(0));
		    }
		    catch (NameNotFoundException none) {
			// XXX: Ignored?
		    }
		    line = (int)funcDie.getDeclLine();
		}
	    }
	}
	else if (params.size() == 2) {
	    // list -length {-}N
	    if (((String)params.get(0)).equals("-length"))		    {
		try 			    {
		    windowSize = Integer.parseInt((String)params.get(1));
		    if (windowSize < 0)				    {
			line += windowSize;
		    }
		}
		catch (NumberFormatException ignore)			    {
		    // XXX: Ignored?
		}
	    }
	}
 
	if (file== null) {
	    Frame frame = cli.debugInfo.getCurrentFrame();
	    if (frame.getLines().length > 0) {
		file = (frame.getLines()[0]).getFile();
		line = (frame.getLines()[0]).getLine() - 10;
		exec_line = line;
	    }
	    else
		cli.outWriter.println("No source for current frame");
	}
      
	if (line < 0)
	    line = 1;
	try {
	    FileReader fr = new FileReader(file);
	    LineNumberReader lr = new LineNumberReader(fr);
	    String str;
	    boolean display = false;
	    int endLine = line + StrictMath.abs(windowSize);
	    String flag = "";
	    while ((str = lr.readLine()) != null) 		    {
		if (lr.getLineNumber() == line)
		    display = true;
		else if (lr.getLineNumber() == exec_line)
		    flag = "*";
		else if (lr.getLineNumber() == endLine)
		    break;
                
		if (display)			    {
		    cli.outWriter.println(lr.getLineNumber() + flag + "\t "+ str);
		    flag = "";
		}
	    }
	    if (str != null && windowSize > 0)
		line += windowSize;
	    lr.close();
	}
	catch (IOException e) {
	    cli.addMessage("file " + file + " not found.",
			   Message.TYPE_ERROR);
	}
    }
}
