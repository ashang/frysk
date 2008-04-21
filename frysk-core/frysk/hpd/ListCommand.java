// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008 Red Hat Inc.
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.List;

import lib.dwfl.DwflLine;
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.ObjectDeclarationNotFoundException;
import frysk.debuginfo.ObjectDeclarationSearchEngine;
import frysk.dwfl.DwflCache;
import frysk.proc.Task;
import frysk.scopes.Function;
import frysk.scopes.SourceLocation;
import frysk.sysroot.SysRoot;
import frysk.sysroot.SysRootCache;

/**
 * Implement the "list" source command.
 */

class ListCommand extends ParameterizedCommand {
    ListCommand () {
	super("Display source code lines.",
	      "list source-loc [-length [-]num-lines]",
	      ("The list command displays lines of source code.  The"
	       + " user can control both the location in the source"
	       + " code and the number of lines displayed.  Successive"
	       + " list commands without location arguments result in"
	       + " the display of consecutive sequences of source lines."));
	add(new CommandOption("length", "number of lines to display",
			      "[+-]num-lines") {
		void parse(String args, Object options) {
		    ((Options)options).length = new Magnitude(args);
		}
	    });
    }
    private static class Options {
	Magnitude length;
    }
    Object options() {
	return new Options();
    }

    private DebugInfoFrame currentFrame = null;
    private File file = null;
    private int line;
    private int exec_line = 0;

    void interpret(CLI cli, Input cmd, Object o) {
	Options options = (Options)o;
        PTSet ptset = cli.getCommandPTSet(cmd);
	int windowSize = 20;
	boolean adjust_line = true;

        Iterator taskIter = ptset.getTaskData();
        while (taskIter.hasNext()) {
            TaskData taskData = (TaskData)taskIter.next();
            exec_line = cli.getTaskFrame(taskData.getTask()).getLine().getLine();
            if (cmd.size() == 0)
        	// default to list around PC
        	line = exec_line;
            else if (cmd.size() == 1) {
        	try {
        	    line = Integer.parseInt(cmd.parameter(0));
        	}
        	catch (NumberFormatException ignore) {
        	    line = parseFunctionName(cmd.parameter(0), file, cli, taskData);
        	    adjust_line = false;
        	}
            }
            if (options.length != null) {
        	windowSize = options.length.magnitude;
            }

            if (adjust_line)
        	line = line - (windowSize / 2);
            listOneTask(cli, cmd, taskData, windowSize);
        }
    }

    int completer(CLI cli, Input input, int cursor, List candidates) {
	return -1;
    }
    
    private void listOneTask (CLI cli, Input cmd, TaskData taskData, int windowSize) {
        Task task = taskData.getTask();
	cli.outWriter.print("[");
	cli.outWriter.print(taskData.getParentID());
	cli.outWriter.print(".");
	cli.outWriter.print(taskData.getID());
	cli.outWriter.println("]");
	DebugInfoFrame frame = cli.getTaskFrame(task);
	if (frame.getLine() == SourceLocation.UNKNOWN) {
	    cli.outWriter.println("No symbol table is available.");
	    return;
	}
	if (cmd.size() == 1) {
	}

	if (file == null || frame != currentFrame) {
	    if (frame.getLine() != SourceLocation.UNKNOWN) {
		file = (frame.getLine()).getFile();
		if (file == null) {
		    cli.addMessage("No symbol table is available.",
			    Message.TYPE_NORMAL);
		    return;
		}
		line = (frame.getLine()).getLine() - (windowSize / 2);
		currentFrame = frame;
		if (exec_line == 0)
		    exec_line = line;
	    }
	    else { 
		cli.outWriter.println("No source for current frame");
		return;
	    }
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
	    while ((str = lr.readLine()) != null) {
		if (lr.getLineNumber() == line)
		    display = true;
		else if (lr.getLineNumber() == endLine)
		    break;
		if (display && lr.getLineNumber() == exec_line)
		    flag = "->";
		    else
			flag = "  ";

		if (display) {
		    int lineNumber = lr.getLineNumber();
		    String rightAdjust;
		    if (lineNumber < 10)
			rightAdjust = "   ";
		    else if (lineNumber < 100)
			rightAdjust = "  ";
		    else if (lineNumber < 1000)
			rightAdjust = " ";
		    else
			rightAdjust = "";
		    cli.outWriter.println(flag + rightAdjust + lineNumber + "\t "+ str);
		    flag = "";
		}
	    }
	    lr.close();
	}
	catch (IOException e) {
	    cli.addMessage("file " + file + " not found.",
		    Message.TYPE_ERROR);
	}
    }
    
    private int parseFunctionName (String cmdParm, File file, CLI cli, TaskData taskData) {
        Task task = taskData.getTask();
	DebugInfoFrame frame = cli.getTaskFrame(task);

	if ((cmdParm).compareTo("$EXEC") == 0)
	    return frame.getLine().getLine();

	Function function = null;

	ObjectDeclarationSearchEngine declarationSearchEngine = new ObjectDeclarationSearchEngine(frame);

	try {
	    function = (Function) declarationSearchEngine.getSymbolDie(cmdParm);
	} catch (ObjectDeclarationNotFoundException e) {
	    function  = null;
	}catch (ClassCastException e) {
	    function  = null;
	}
	
	if (function != null ) {
	    DwflLine dwflLine = DwflCache.getDwfl(frame.getTask())
		    .getSourceLine(frame.getAdjustedAddress());
	    if (dwflLine != null) {
		SysRoot sysRoot = new SysRoot(SysRootCache.getSysRoot(frame
			.getTask()));
		file = sysRoot.getSourcePathViaSysRoot(
			new File(dwflLine.getCompilationDir()),
			function.getSourceLocation().getFile())
			.getSysRootedFile();
	    } else {
		file = function.getSourceLocation().getFile();
	    }
	    return (int) function.getSourceLocation().getLine();

	} else {
	    cli.addMessage("function " + cmdParm + " not found.",
		    Message.TYPE_ERROR);
	    return line;

	}
    }
}
