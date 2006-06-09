// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

import java.util.TreeMap;
import java.util.Vector;

/**
 * A container for ProcData and a subset of it's TaskDatas. If you try to add tasks from other
 * Procs you'll get an exception;
 */
class ProcTasks
{
	ProcData proc;
	TreeMap tasks; //maps taskId -> TaskData. A map quarantees sortedness and log(n) basic operations 

	/**
	 * Constructor
	 */
	public ProcTasks(ProcData proc)
	{
		this.proc = proc;
		this.tasks = new TreeMap();
	}


	/**
	 * Constructor
	 */
	public ProcTasks(ProcData proc, TaskData[] taskarray)
	{
		this.proc = proc;
		this.tasks = new TreeMap();

		TaskData temp = null;

		for (int i = 0; i < taskarray.length; i++)
		{
			temp = taskarray[i];
			if (temp.getParentID() == proc.getID())
			{
				//task id -> task
				tasks.put(new Integer(temp.getID()), temp);
			}
			else
				throw new IllegalArgumentException("ProcTasks was passed" +
								" Tasks from a process that is not \"proc\"");
		}
	}

	public boolean hasTask(int taskID)
	{
		return tasks.containsKey(new Integer(taskID));
	}

	public ProcData getProcData()
	{
		return proc;
	}

	/**
	 * Returns a Vecotr of all TaskData objects order by ID
	 * @return a Vector of TaskData objects
	 */
	public Vector getTaskData()
	{
		return new Vector( ((TreeMap) tasks.clone()).values() );
	}

	public void addTaskData(TaskData task)
	{
		if (task.getParentID() == proc.getID())
		{
			//task id -> task
			tasks.put(new Integer(task.getID()), task);
		}
		else
			throw new IllegalArgumentException("ProcTasks was passed Task not from this instance's process.");
	}

	public String toString()
	{
		String result = ""; 
		Vector values = new Vector(tasks.values());
		for (int i = 0; i < values.size(); i++)
			result += (TaskData)values.elementAt(i) + "\n";

		return result;
	}
}
