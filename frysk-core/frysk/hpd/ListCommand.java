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

import java.io.File;
import java.util.List;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import lib.dwfl.DwarfDie;
import lib.dwfl.DwTag; 
import frysk.debuginfo.DebugInfoFrame;
import frysk.debuginfo.DebugInfo;
import frysk.proc.Task;

/**
 * Implement the "list" source command.
 */

class ListCommand extends ParameterizedCommand {
    ListCommand () {
	super("list", "Display source code lines.",
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

    private File file = null;
    private int line;
    private int exec_line = 0;

    void interpret(CLI cli, Input cmd, Object o) {
	Options options = (Options)o;
        PTSet ptset = cli.getCommandPTSet(cmd);
	int windowSize = 20;
        Iterator taskIter = ptset.getTaskData();
        while (taskIter.hasNext()) {
            TaskData taskData = (TaskData)taskIter.next();
            Task task = taskData.getTask();
            cli.outWriter.print("[");
	    cli.outWriter.print(taskData.getParentID());
	    cli.outWriter.print(".");
	    cli.outWriter.print(taskData.getID());
	    cli.outWriter.println("]");
            DebugInfoFrame frame = cli.getTaskFrame(task);
            if (frame.getLines().length == 0) {
		cli.outWriter.println("No symbol table is available.");
		continue;
            }
            if (cmd.size() == 1) {
                // list N
                try {
                    line = Integer.parseInt(cmd.parameter(0));
                }
                catch (NumberFormatException ignore) {
                    if ((cmd.parameter(0)).compareTo("$EXEC") == 0)
                        line = frame.getLines()[0].getLine() - 10;
                    else {
                        DwarfDie funcDie = null;
			DebugInfo debugInfo = cli.getTaskDebugInfo(task);
			if (debugInfo != null) {
			    funcDie = debugInfo
				.getSymbolDie(cmd.parameter(0));
                        }
			if (funcDie.getTag().hashCode() == DwTag.SUBPROGRAM_) {
			    line = (int)funcDie.getDeclLine();
			    file = new File(funcDie.getDeclFile());
			}
			else {
			    cli.addMessage("function " + cmd.parameter(0) + " not found.",
					   Message.TYPE_ERROR);
			    return;
			}
                    }
                }
            }
	    else if (options.length != null) {
		windowSize = options.length.magnitude;
		if (options.length.sign < 0) {
		    windowSize *= -1;
		    line += windowSize;
		}
	    }
            else if (frame.getLines()[0].getLine() != exec_line) {
                // list around pc.
                exec_line = frame.getLines()[0].getLine();
                line = exec_line - 10;
            }
 
            if (file == null) {
                if (frame.getLines().length > 0) {
                    file = (frame.getLines()[0]).getFile();
                    if (file == null) {
                        cli.addMessage("No symbol table is available.",
                                       Message.TYPE_NORMAL);
                        return;
                    }
                    line = (frame.getLines()[0]).getLine() - 10;
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
                        flag = "*";
		    else
			flag = " ";

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

    int complete(CLI cli, PTSet ptset, String incomplete, int base,
		 List candidates) {
	return -1;
    }
}
