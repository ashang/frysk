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
package frysk.hpd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.LinkedList;
import java.lang.IllegalStateException;

import frysk.proc.Proc;
import frysk.proc.Task;

import frysk.rt.ProcTaskIDManager;

 // There are two different range notations that are handled one creation of subsets
 // referred to as "reg", which is in the form a:b.c:d, and the "range" notation,
 // which looks like a.b:c.d. Any wildcards in that notation are presented by -1.
 // So an array of length 4, to which parts of the set notation tree are converted,
 // for 3.*:4.5 would be {3, -1, 4, 5}

/**
 * AllPTSet holds all processes and their tasks and generates subsets for different types of sets. 
 */
class AllPTSet implements PTSet
{

	public static final int TASK_STATE_RUNNING = 0;
	public static final int TASK_STATE_STOPPED = 1;
	public static final int TASK_STATE_RUNNABLE = 2;
	public static final int TASK_STATE_HELD = 3;

	// parallel arrays are not cute, but in this case they are handy
	// Proc at proSet[i] has it's tasks in taskSets[i] in the form of a ArrayList

	/*
	 * Public methods
	 */

    private final ProcTaskIDManager manager;
    private final CLI cli;

    public AllPTSet(CLI cli) {
        this.cli = cli;
        manager = ProcTaskIDManager.getSingleton();
    }


	public Proc getProc(int procID)	{
            return manager.getProc(procID);
	}

	public boolean containsTask(int procid, int taskid) {
            ProcTaskIDManager manager = ProcTaskIDManager.getSingleton();
            return (manager.getTask(procid, taskid) != null);
	}

    /**
     * Return an iterator to a collection of all the tasks.
     */
    public Iterator getTasks() {
        ArrayList result = new ArrayList();
        synchronized (manager) {
            int numProcs = manager.getNumberOfProcs();
            for (int p = 0; p < numProcs; p++) {
                int numTasks = manager.getNumberOfTasks(p);
                for (int t = 0; t < numTasks; t++) {
                    Task task = manager.getTask(p, t);
                    if (task != null)
                        result.add(task);
                }
            }
        }
        return result.iterator();
    }
    

    public Iterator getTaskData() {
        LinkedList result = new LinkedList();
        synchronized (manager) {
            int numProcs = manager.getNumberOfProcs();
            for (int p = 0; p < numProcs; p++) {
                int numTasks = manager.getNumberOfTasks(p);
                for (int t = 0; t < numTasks; t++) {
                    Task task = manager.getTask(p, t);
                    if (task != null)
                        result.add(new TaskData(task, t, p));
                }
            }
        }
        return result.iterator();
    }
    
    /**
     * Remove a proc from the list
     */
    
    public void removeProc(int procId) {
	synchronized (manager) {
           manager.removeProc(procId);
        }
    }
    
	/**
	 * Generate a subset of this set, based on the tree generated by set
	 * notation parser.
	 * @param parseTree tree generated by {@link SetNotationParser}, which is
	 * an array of ParseTreeNodes
	 */
	public ProcTasks[] getSubset(ParseTreeNode[] parseTree)
	{
            ArrayList snapshot = ProcTaskIDManager.getSingleton().snapshot();
		TreeMap proctasks = new TreeMap();
		ProcTasks[] result = null;
		ParseTreeNode tempNode = null;
		int[] walkResult;

		for (int i = 0; i < parseTree.length; i++)
		{
			tempNode = parseTree[i];

			if (tempNode.getType() == ParseTreeNode.TYPE_REG)
			{
				walkResult = walkRegTree(tempNode);
				addTasksFromReg(snapshot, proctasks, walkResult);
			}
			else if (tempNode.getType() == ParseTreeNode.TYPE_RANGE)
			{
				walkResult = walkRangeTree(tempNode);
				addTasksFromRange(snapshot, proctasks, walkResult);
			}
			else
			{
				throw new IllegalStateException("Illegal ParseTreeNode type");
			}
		}

		//convert values to array
		result = (ProcTasks[]) (new ArrayList(proctasks.values()).toArray(new ProcTasks[0]));

		return result;
	}

    public ProcTasks[] getSubsetByState(int state) {
        ArrayList alist = new ArrayList();
        synchronized (manager) {
            int numProcs = manager.getNumberOfProcs();
            for (int p = 0; p < numProcs; p++) {
                ProcTasks procTasks
                    = new ProcTasks(new ProcData(manager.getProc(p), p));
                int numTasks = manager.getNumberOfTasks(p);
                for (int t = 0; t < numTasks; t++) {
                    Task task = manager.getTask(p, t);
                    if (task != null) {
                        boolean addTask = false;
                        boolean taskIsRunning
                            = cli.getSteppingEngine().isTaskRunning(task);
                        switch (state) {
                        case TASK_STATE_RUNNING:
                            if (taskIsRunning)
                                addTask = true;
                            break;
                        case TASK_STATE_STOPPED:
                            if (!taskIsRunning)
                                addTask = true;
                            break;
                        // Other states will come later.
                        default:
                            addTask = false;
                            break;
                        }
                        if (addTask)
                            procTasks.addTaskData(new TaskData(task, t, p));
                    }
                        

                }
                if (procTasks.getTaskData().size() > 0)
                    alist.add(procTasks);
            }
        }
        return (ProcTasks[])alist.toArray(new ProcTasks[0]);
    }

	public ProcTasks[] getSubsetByExec(String execname)
	{
		return null;
	}
	
	public String toString() {
            String result = "";

            for (int i = 0; i < manager.getNumberOfProcs(); i++) {
                result += i + ".0:";
                result += manager.getNumberOfTasks(i) - 1;
                result += "\n";
            }
            return result;
	}

	/**
	 * Walks a tree that corresponds to a.b:c.d notation.
	 * @param node on a non-recursive call this should be a root of type
	 * ParseTreeNode.TYPE_RANGE
	 * @return array of {a,b,c,d} from notation a.b:c.d, -1 stands for a
	 * wildcard (*)
	 */ 
	private int[] walkRangeTree(ParseTreeNode node)
	{
		int[] result = new int[4];
		int[] leftResult = null;
		int[] rightResult = null;
		
		if (!node.isLeaf())
		{
			leftResult = walkRangeTree(node.getLeft());
			rightResult = walkRangeTree(node.getRight());
		}

		if (node.getType() == ParseTreeNode.TYPE_REG)
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
		else if (node.getType() == ParseTreeNode.TYPE_RANGE)
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
	 * ParseTreeNode.TYPE_REG
	 * @return array of {a,b,c,d} from notation a:b.c:d, -1 stands for a
	 * wildcard (*)
	 */
	private int[] walkRegTree(ParseTreeNode node)
	{
		int[] result = new int[4];
		int[] leftResult = null;
		int[] rightResult = null;
		
		if (!node.isLeaf())
		{
			leftResult = walkRegTree(node.getLeft());
			rightResult = walkRegTree(node.getRight());
		}

		if (node.getType() == ParseTreeNode.TYPE_REG)
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
		else if (node.getType() == ParseTreeNode.TYPE_RANGE)
		{
				result[0] = leftResult[0];
				result[1] = rightResult[0];
		}

		return result;
	}

	/**
	 * Add tasks to the "tasks" ArrayList, as specified in "range", which
	 * corresponds to a.b:c.d notation
         * @param taskSets array of arrays of tasks
	 * @param proctasks A treemap of Integer(procID) to ProcTasks
	 * @param reg an array of length 4 returned by walkRangeTree
	 */
    private void addTasksFromRange(ArrayList taskSets, TreeMap proctasks, int[] range)
	{
		ArrayList tempSet = null; // a temporary ArrayList of a process tasks
		ProcTasks tempPT = null; // 
		int procStart = range[0];
		int taskStart = range[1];
		int procEnd = range[2];
		int taskEnd = range[3];

		// pointers
		int procP = 0;
		int taskP = 0;

		if (procEnd == -1 || procEnd >= taskSets.size())
			procEnd = taskSets.size() - 1;
                
		tempSet = (ArrayList)taskSets.get(procEnd);

		if (taskEnd == -1 || taskEnd >= tempSet.size()) //if wildcard or more than actual
		{
			taskEnd = tempSet.size() - 1;
		}

		if (procStart == -1)
			procStart = 0; 

		if (taskStart == -1)
			taskStart = 0;

		procP = procStart;
		taskP = taskStart;

		if (procP < taskSets.size())
			tempSet = (ArrayList)taskSets.get(procP);
		else
			procP = -1;

		while ((procP < procEnd || taskP <= taskEnd) && procP != -1) 
		{
			if (taskP > tempSet.size() - 1)
			{
				procP++;
				taskP = 0;
				tempSet = (ArrayList)taskSets.get(procP);
			}

			if (!proctasks.containsKey(new Integer(procP))) // if this process hasn't been added yet
			{
				//create a new ProcTasks for this process
				tempPT = new ProcTasks(new ProcData( getProc(procP), procP ));
				proctasks.put( new Integer(procP), tempPT);
			}
			else
			{
				tempPT = (ProcTasks) proctasks.get(new Integer(procP));
			}

			tempPT.addTaskData(new TaskData( (Task)tempSet.get(taskP), taskP, procP ));

			taskP++;
		}
	}

	/**
	 * Add tasks to the "tasks" ArrayList, as specified in "reg", which
	 * corresponds to a:b.c:d notation
         * @param taskSets array of arrays of tasks
	 * @param proctasks TreeMap of ProcTasks to put Procs and Tasks into
	 * @param reg an array of length 4 returned by walkRegTree
	 */
    private void addTasksFromReg(ArrayList taskSets, TreeMap proctasks, int[] reg)
	{
		ArrayList tempSet = null; // a temporary ArrayList of a process tasks
		ProcTasks tempPT = null;  
		int procStart = reg[0];
		int procEnd = reg[1];
		int taskStart = reg[2];
		int taskEnd = reg[3];

		//pointers
		int procP = 0;
		int taskP = 0;

		if (procEnd >= taskSets.size() || procEnd == -1)
			procEnd = taskSets.size() - 1;

		if (procStart == -1) // if this is a wildcard
			procStart = 0;

		if (taskStart == -1)
			taskStart = 0;

		procP = procStart;
		taskP = taskStart;

		
		if (procP < taskSets.size())
			tempSet = (ArrayList)taskSets.get(procP);
		else
			procP = -1;

		while ((procP < procEnd || taskP <= taskEnd) && procP != -1)
		{
			if (taskP >= tempSet.size() || taskP > taskEnd)
			{
				procP++;
				taskP = taskStart;
				tempSet = (ArrayList)taskSets.get(procP);
			}

			if (!proctasks.containsKey(new Integer(procP))) // if this process hasn't been added yet
			{
				//create a new ProcTasks for this process
				tempPT = new ProcTasks(new ProcData( getProc(procP), procP ));
				proctasks.put( new Integer(procP), tempPT);
			}
			else
			{
				tempPT = (ProcTasks) proctasks.get(new Integer(procP));
			}

			tempPT.addTaskData(new TaskData( (Task)tempSet.get(taskP), taskP, procP ));

			taskP++;
		}
	}
}
