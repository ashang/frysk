// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.rt;

import java.util.ArrayList;
import java.util.HashMap;

import frysk.proc.Action;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

public class ProcTaskIDManager
    implements TaskObserver.Cloned, TaskObserver.Forked,
               TaskObserver.Terminated {

    private void addTaskObservers(Task task) {
        task.requestAddForkedObserver(this);
        task.requestAddClonedObserver(this);
        task.requestAddTerminatedObserver(this);
    }
    
    private class ProcEntry {
        int id;
        Proc proc;
        ArrayList tasks;
        HashMap taskMap;

        ProcEntry(Proc proc, int id) {
            this.id = id;
            this.proc = proc;
            tasks = new ArrayList(proc.getTasks());
            taskMap = new HashMap();
            for (int i = 0; i < tasks.size(); i++) {
                Task task = (Task)tasks.get(i);
                taskMap.put(task, new Integer(i));
                addTaskObservers(task);
            }
        }
    }
    
    private ArrayList procList = new ArrayList();
    private HashMap procMap = new HashMap();

    // Private for singleton
    private ProcTaskIDManager() {
    }
    public synchronized int reserveProcID() {
        int result = procList.size();
        procList.add(null);
        return result;
    }

    public void manageProc(Proc proc, int reservedID) {
        ProcEntry entry;
        synchronized (this) {
            entry = new ProcEntry(proc, reservedID);
            procList.set(reservedID, entry);
            procMap.put(proc, new Integer(reservedID));
        }
    }

    public synchronized Proc getProc(int id) {
        if (id < procList.size())
            return ((ProcEntry)procList.get(id)).proc;
        else
            return null;
    }

    public synchronized int getProcID(Proc proc) {
        Integer result = (Integer)procMap.get(proc);
        if (result != null) {
            return ((Integer)result).intValue();
        }
        return -1;
    }

    public synchronized int getNumberOfProcs() {
        return procList.size();
    }

    public synchronized int getNumberOfTasks(int procID) {
        if (procID >= procList.size())
            return 0;
        ProcEntry entry = (ProcEntry)procList.get(procID);
        if (entry == null)
            return 0;
        else
            return entry.tasks.size();
    }

    public synchronized Task getTask(int procID, int taskID) {
        if (procID >= procList.size())
            return null;
        ProcEntry entry = (ProcEntry)procList.get(procID);
        if (entry == null || taskID >= entry.tasks.size())
            return null;
        return (Task)entry.tasks.get(taskID);
    }

    // Observer interface
    public void addedTo(Object observable) {
    }

    public void addFailed(Object observable, Throwable w) {
    }

    public void deletedFrom(Object observable) {
    }

    public Action updateForkedParent(Task parent, Task offspring) {
        Proc newProc = offspring.getProc();
        int id = reserveProcID();
        manageProc(newProc, id);
        parent.requestUnblock(this);
        return Action.BLOCK;
    }

    public Action updateForkedOffspring(Task parent, Task offspring) {
        return Action.CONTINUE;
    }

    public Action updateClonedParent(Task parent, Task offspring) {
        Proc proc = offspring.getProc();
        int id = getProcID(proc);
        if (id < 0)
            return Action.CONTINUE;
        synchronized (this) {
            ProcEntry entry = (ProcEntry)procList.get(id);
            if (entry == null)
                return Action.CONTINUE;
            int taskID = entry.tasks.size();
            entry.tasks.add(offspring);
            entry.taskMap.put(offspring, new Integer(taskID));
        }
        addTaskObservers(offspring);
        parent.requestUnblock(this);
        return Action.BLOCK;
    }

    public Action updateClonedOffspring(Task parent, Task offspring) {
        return Action.CONTINUE;
    }
    
    public Action updateTerminated(Task task, boolean signal, int value) {
        Proc proc = task.getProc();
        int id = getProcID(proc);
        if (id < 0)
            return Action.CONTINUE;
        synchronized (this) {
            ProcEntry entry = (ProcEntry)procList.get(id);
            if (entry == null)
                return Action.CONTINUE;
            Integer taskIDInt = (Integer)entry.taskMap.get(task);
            if (taskIDInt != null) {
                entry.taskMap.remove(task);
                entry.tasks.set(taskIDInt.intValue(), null);
            }
        }
        return Action.CONTINUE;
    }

    private static ProcTaskIDManager idManager = null;

    public static synchronized ProcTaskIDManager getSingleton() {
        if (idManager == null)
            idManager = new ProcTaskIDManager();
        return idManager;
    }

    public synchronized ArrayList snapshot() {
        ArrayList result = new ArrayList();
        int numProcs = getNumberOfProcs();
        for (int i = 0; i < numProcs; i++) {
            ProcEntry entry = (ProcEntry)procList.get(i);
            if (entry != null) {
                result.add(entry.tasks.clone());
            } else {
                result.add(null);
            }
        }
        return result;
    }
}
