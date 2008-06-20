// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

package frysk.ftrace;

import java.util.HashMap;
import java.util.ArrayList;

import inua.util.PrintWriter;

import frysk.debuginfo.PrintDebugInfoStackOptions;
import frysk.proc.Task;
import frysk.util.ArchFormatter;
import frysk.util.StackPrintUtil;

class Reporter
{
    private PrintWriter writer;
    private Object lastItem = null;
    private Task lastTask = null;

    // HashMap<Task, ArrayList<Object>> -- array of entry tokens for each taks
    private final HashMap tokenMap = new HashMap();

    private final PrintDebugInfoStackOptions stackPrintOptions;
    private final boolean showPC;

    public Reporter(PrintWriter writer,
		    PrintDebugInfoStackOptions stackPrintOptions,
		    boolean show) {
	this.writer = writer;
	this.stackPrintOptions = stackPrintOptions;
	this.showPC = show;
    }

    private ArrayList getTokens(Task task)
    {
	ArrayList l = (ArrayList)tokenMap.get(task);
	if (l == null) {
	    l = new ArrayList();
	    tokenMap.put(task, l);
	}
	return l;
    }

    private boolean lineOpened()
    {
	return lastItem != null;
    }

    private boolean myLineOpened(Task task, Object item)
    {
	return lastItem == item && lastTask == task;
    }

    private void updateOpenLine(Task task, Object item)
    {
	lastItem = item;
	lastTask = task;
    }

    private String pidInfo(Task task)
    {
	return "" + task.getProc().getPid() + "." + task.getTid();
    }

    private void printArgs(Object[] args)
    {
	if (args == null)
	    return;

	writer.print("(");
	for (int i = 0; i < args.length; ++i) {
	    writer.print(i > 0 ? ", " : "");
	    // Temporary hack to get proper formatting before
	    // something more sane lands.
	    if (args[i] instanceof Long)
		writer.print("0x" + Long.toHexString(((Long)args[i]).longValue()));
	    else if (args[i] instanceof Integer)
		writer.print("0x" + Integer.toHexString(((Integer)args[i]).intValue()));
	    else
		writer.print(args[i]);
	}
	writer.print(")");
    }

    private String formatTaskPC(Task task) {
	if (!showPC)
		return "";

	long pc;
	try {
	    pc = task.getPC();
	}
	catch (RuntimeException exc) {
	    pc = -1;
	}

	return ArchFormatter.toHexString(task, pc) + " ";
    }

    public void eventEntry(Task task, Object item, String eventType,
			    String eventName, Object[] args)
    {
	ArrayList tokens = getTokens(task);
	String spaces = ArchFormatter.repeat(' ', tokens.size());
	tokens.add(item);

	if (lineOpened())
	    writer.println('\\');

	writer.print(pidInfo(task)
		     + " " + formatTaskPC(task)
		     + spaces + eventType
		     + " " + eventName);
	printArgs(args);
	writer.flush();

	updateOpenLine(task, item);
    }

    public void eventLeave(Task task, Object item, String eventType,
			    String eventName, Object retVal)
    {
	String stray = "";

	ArrayList tokens = getTokens(task);
	int i = tokens.size() - 1;
	while (i >= 0 && tokens.get(i) != item)
	    --i;
	if (i < 0)
	    stray = "stray ";
	else
	    tokens.subList(i, tokens.size()).clear();

	if (!myLineOpened(task, item)) {
	    if (lineOpened())
		writer.println();
	    String spaces = ArchFormatter.repeat(' ', tokens.size());
	    writer.print(pidInfo(task)
			 + " " + formatTaskPC(task)
			 + spaces + stray + eventType
			 + " " + eventName);
	}

	writer.println(" = " + retVal);
	writer.flush();

	updateOpenLine(null, null);
    }

    public void eventSingle(Task task, String eventName)
    {
	eventSingle(task, eventName, null);
    }

    public void eventSingle(Task task, String eventName, Object[] args)
    {
	ArrayList tokens = getTokens(task);
	if (lineOpened())
	    writer.println("\\");
	writer.print(pidInfo(task)
		     + " " + formatTaskPC(task)
		     + ArchFormatter.repeat(' ', tokens.size())
		     + eventName);

	if (args != null)
	    printArgs(args);
	writer.println();
	writer.flush();

	updateOpenLine(null, null);
    }

    public void generateStackTrace(Task task) {
	eventSingle(task, "dumping stack trace:");
	StackPrintUtil.print(task, stackPrintOptions, writer);
	writer.flush();
	updateOpenLine(null, null);
    }
}
