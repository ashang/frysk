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

import frysk.proc.*;
import java.util.Vector;
import java.lang.IllegalStateException;
import java.lang.RuntimeException;


 // There are two different range notations that are handled one creation of subsets
 // referred to as "reg", which is in the form a:b.c:d, and the "range" notation,
 // which looks like a.b:c.d. Any wildcards in that notation are presented by -1.
 // So an array of length 4, to which parts of the set notation tree are converted,
 // for 3.*:4.5 would be {3, -1, 4, 5}

/**
 * TaskSet represents a set of processes and their tasks. 
 * Tasks are stored in "two-dimensional" Vector [process][task].
 * The class makes some assumptions about when and how it's useful, and about
 * the engine, so review it for possible changes.
 */
public class AllPTSet
{
	// parallel arrays are not cute, but in this case they are handy
	// Proc at proSet[i] has it's tasks in taskSets[i] in the form of a vector
	Vector taskSets; // a Vector of Vectors containing Tasks
	Vector procSet; // a Vector of Proc's

	/*
	 * Public methods
	 */

	/**
	 * Constructs an empty TaskSet
	 */
	public AllPTSet()
	{
		taskSets = new Vector();
	}

	/**
	 * Adds a task to a specific process, if a process was not created, throw
	 * an exception.
	 * @param task Task to add
	 * @param procID id of the the task's parent process
	 * @return id of the Task
	 */
	public int addTask(Task task, int procID)
	{
		int result = -1;

		if (procID < taskSets.size())
		{
			Vector set = (Vector) taskSets.elementAt(procID);

			result = set.size();
			set.add(task);
		}
		else
		{
			throw new RuntimeException("Task added to a nonexistent Proc");
		}

		return result;
	}

	/**
	 * Tells the set to alocate a structure for a set of tasks.
	 */
	public int addProc(Proc proc)
	{
		int result = procSet.size(); // new Proc will be appended

		procSet.add(proc);
		taskSets.add(new Vector());

		return result;
	}


	/**
	 * Find the ID of a task.
	 * @return a tuple (procID,taskID) in the form of a two-element array, null if task not found
	 */
	public int[] getTaskID(Task task)
	{
		int[] result = new int[2];
		result[0] = -1;
		result [1] = -1;

		Vector temp = null;

		for (int i = 0; i < taskSets.size(); i++)
		{
			temp = (Vector) taskSets.elementAt(i);

			if (temp.contains(task))
			{
				result[0] = i;
				result[1] = temp.indexOf(task);
				break;
			}
		}
		return result;
	}

	/**
	 * Find the ID of a task in a specific process.
	 * @return id of the task in that process, -1 if task not found
	 */
	public int getTaskID(Task task, int procID)
	{
		int result = -1;
		Vector temp = null;

		if (taskSets.size() < procID)
		{
			temp = (Vector) taskSets.elementAt(procID);
			result = temp.indexOf(task);
		}
		else
		{
			throw new RuntimeException("Looking up Task in a nonexistent Proc");
		}

		return result;
	}

	/**
	 * Find the ID of a process.
	 * @return id of the process, -1 if not found
	 */
	public int getProcID(Proc proc)
	{
		return procSet.indexOf(proc);
	}

	/**
	 * Generate a subset of this set, based on the tree generated by set
	 * notation parser.
	 * @param parseTree tree generated by {@link SetNotationParser}, which is
	 * a Vector of PTNodes
	 */
	public Vector getSubset(Vector parseTree)
	{
		Vector result = new Vector();
		PTNode tempNode = null;
		int[] walkResult;

		for (int i = 0; i < parseTree.size(); i++)
		{
			tempNode = (PTNode)parseTree.elementAt(i);

			if (tempNode.getType() == PTNode.TYPE_REG)
			{
				walkResult = walkRegTree(tempNode);
				addTasksFromRange(result, walkResult);
			}
			else if (tempNode.getType() == PTNode.TYPE_RANGE)
			{
				walkResult = walkRangeTree(tempNode);
				addTasksFromReg(result, walkResult);
			}
			else
			{
				throw new IllegalStateException("Illegal PTNode type");
			}
		}

		return result;
	}

	public Vector getAllTasks()
	{
		Vector result = new Vector();

		for (int i = 0; i < result.size(); i++)
			result.addAll((Vector)taskSets.elementAt(i));

		return result;
	}

	/**
	 * Walks a tree that corresponds to a.b:c.d notation.
	 * @param node on a non-recursive call this should be a root of type
	 * PTNode.TYPE_RANGE
	 * @return array of {a,b,c,d} from notation a.b:c.d, -1 stands for a
	 * wildcard (*)
	 */ 
	private int[] walkRangeTree(PTNode node)
	{
		int[] result = new int[4];
		int[] leftResult = null;
		int[] rightResult = null;
		
		if (!node.isLeaf())
		{
			leftResult = walkRangeTree(node.getLeft());
			rightResult = walkRangeTree(node.getRight());
		}

		if (node.getType() == PTNode.TYPE_REG)
		{
			if (node.isLeaf())
			{
				result[0] = node.getID();
			}
			else
			{
				result[0] = leftResult[0];
				result[1] = rightResult[0];
			}
		}
		else if (node.getType() == PTNode.TYPE_RANGE)
		{
			result[0] = leftResult[0];
			result[1] = leftResult[1];
			result[2] = rightResult[0];
			result[3] = rightResult[1];
		}

		return result;
	}

	/**
	 * Walks a tree that corresponds to a:b.c:d notation, where b and d are
	 * optional ranges.
	 * @param node on a non-recursive call this should be a root of type
	 * PTNode.TYPE_REG
	 * @return array of {a,b,c,d} from notation a:b.c:d, -1 stands for a
	 * wildcard (*)
	 */
	private int[] walkRegTree(PTNode node)
	{
		int[] result = new int[4];
		int[] leftResult = null;
		int[] rightResult = null;
		
		if (!node.isLeaf())
		{
			leftResult = walkRangeTree(node.getLeft());
			rightResult = walkRangeTree(node.getRight());
		}

		if (node.getType() == PTNode.TYPE_REG)
		{
			if (node.isLeaf())
			{
				result[0] = node.getID();
			}
			else
			{
				result[0] = leftResult[0];
				result[1] = leftResult[1];
				result[2] = rightResult[0];
				result[3] = rightResult[1];
			}
		}
		else if (node.getType() == PTNode.TYPE_RANGE)
		{
				result[0] = leftResult[0];
				result[1] = rightResult[0];
		}

		return result;
	}

	/**
	 * Add tasks to the "tasks" Vector, as specified in "range", which
	 * corresponds to a.b:c.d notation
	 * @param tasks Vector to add tasks to
	 * @param reg an array of length 4 returned by walkRangeTree
	 */
	private void addTasksFromRange(Vector tasks, int[] range)
	{
		Vector tempSet = null;
		int procStart = range[0];
		int taskStart = range[2];
		int procEnd = range[1];
		int taskEnd = range[3];

		// pointers
		int procP = 0;
		int taskP = 0;

		// TODO. THIS IS A BUG
		// It does not check for existence of the element

		if (procStart == -1)
		{
			procStart = 0;
			procEnd = taskSets.size() - 1;
		}
		else if (taskStart == -1)
		{
			taskStart = 0;
		}
		else if (taskEnd == -1)
		{
			tempSet = (Vector) taskSets.elementAt(procEnd-1);
			taskEnd = tempSet.size() - 1;
		}

		procP = procStart;
		taskP = taskStart;

		tempSet = (Vector) taskSets.elementAt(procStart);
		while (procP <= procEnd && taskP <= taskEnd)
		{
			tasks.add(tempSet.elementAt(taskP));	

			taskP++;
			if (taskP == tempSet.size())
			{
				tempSet = (Vector) taskSets.elementAt(procStart);
				procP++;
			}
		}
	}

	/**
	 * Add tasks to the "tasks" Vector, as specified in "reg", which
	 * corresponds to a:b.c:d notation
	 * @param tasks Vector to add tasks to
	 * @param reg an array of length 4 returned by walkRegTree
	 */
	private void addTasksFromReg(Vector tasks, int[] reg)
	{
		Vector tempSet = null;
		int from1 = reg[0];
		int to1 = reg[1];
		int from2 = reg[2];
		int to2 = reg[3];

		if (reg[0] == -1) // if this is a wildcard
		{
			from1 = 0;
			to1 = taskSets.size() - 1;
		}

		for (int i = from1; i <= to1 && i < taskSets.size(); i++)
		{
			tempSet = (Vector) taskSets.elementAt(i);
			if (reg[2] == -1) //r
			{
				from2 = 0;
				to2 = tempSet.size();
			}

			for (int j = from2; j <= to2 && i < tempSet.size(); j++)
			{
				if (!tasks.contains(tempSet.elementAt(j))) // if we have not already added the task
					tasks.add(tempSet.elementAt(j));
			}
		}
	}
}
