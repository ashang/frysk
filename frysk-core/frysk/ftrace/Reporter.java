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

import frysk.debuginfo.PrintStackOptions;
import frysk.util.StackPrintUtil;
import frysk.proc.Task;
import inua.util.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;

class Reporter
{
    private PrintWriter writer;
    private Object lastItem = null;
    private Task lastTask = null;
    private HashMap levelMap = new HashMap();
    private final PrintStackOptions stackPrintOptions;

    public Reporter(PrintWriter writer, PrintStackOptions stackPrintOptions) {
	this.writer = writer;
	this.stackPrintOptions = stackPrintOptions;
    }

    private int getLevel(Task task)
    {
	int level = 0;
	Integer l = (Integer)levelMap.get(task);
	if (l != null)
	    level = l.intValue();
	return level;
    }

    private void setLevel(Task task, int level)
    {
	levelMap.put(task, new Integer(level));
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

    private String repeat(char c, int count)
    {
	// Taken from code by Stephen Friedrich
	// http://weblogs.java.net/blog/skelvin/archive/2004/08/big_severe_logg.html
	char[] fill = new char[count];
	Arrays.fill(fill, c);
	return new String(fill);
    }

    private String pidInfo(Task task)
    {
	return "" + task.getProc().getPid() + "." + task.getTid();
    }

    private void printArgs(Object[] args)
    {
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

    public void eventEntry(Task task, Object item, String eventType,
			    String eventName, Object[] args)
    {
	int level = this.getLevel(task);
	String spaces = repeat(' ', level);
	this.setLevel(task, ++level);

	if (lineOpened())
	    writer.println('\\');

	writer.print(pidInfo(task) + " "
			 + spaces + eventType
			 + " " + eventName);
	printArgs(args);
	writer.flush();

	updateOpenLine(task, item);
    }

    public void eventLeave(Task task, Object item, String eventType,
			    String eventName, Object retVal)
    {
	int level = this.getLevel(task);
	this.setLevel(task, --level);

	if (!myLineOpened(task, item)) {
	    if (lineOpened())
		writer.println();
	    String spaces = repeat(' ', level);
	    writer.print(pidInfo(task) + " " + spaces + eventType + " " + eventName);
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
	int level = this.getLevel(task);
	if (lineOpened())
	    writer.println("\\");
	writer.println(pidInfo(task) + " " + repeat(' ', level) + eventName);

	if (args != null)
	    printArgs(args);
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
