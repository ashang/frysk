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
import java.util.LinkedList;

class DynamicPTSet implements PTSet {
    ParseTreeNode[] set;

    AllPTSet pool;

    public DynamicPTSet(AllPTSet ptpool, ParseTreeNode[] set) {
	this.set = set;
	pool = ptpool;
    }

    // not efficient, but okay for now
    public Iterator getProcData() {
	ProcTasks[] proctasks = pool.getSubset(set);
	LinkedList result = new LinkedList();

	for (int i = 0; i < proctasks.length; i++)
	    result.add(proctasks[i].getProcData().getProc());

	return result.iterator();
    }

    public boolean containsTask(int procid, int taskid) {
	boolean result = false;

	ProcTasks[] proctasks = pool.getSubset(set);
	for (int i = 0; i < proctasks.length; i++) {
	    if (proctasks[i].getProcData().getID() == procid
		    && proctasks[i].containsTask(taskid)) {
		result = true;
		break;
	    }
	}

	return result;
    }

    // not efficient, but okay for now
    public Iterator getTasks() {
	ProcTasks[] proctasks = pool.getSubset(set);
	ArrayList temp = new ArrayList();
	LinkedList result = new LinkedList();

	for (int i = 0; i < proctasks.length; i++) {
	    temp = proctasks[i].getTaskData();
	    for (int j = 0; j < temp.size(); j++)
		result.add(((TaskData) temp.get(i)).getTask());
	}

	return result.iterator();
    }

    public Iterator getTaskData() {
	ProcTasks[] proctasks = pool.getSubset(set);
	LinkedList result = new LinkedList();

	for (int i = 0; i < proctasks.length; i++) {
	    result.addAll(proctasks[i].getTaskData());
	}

	return result.iterator();
    }
    
    /**
     * Remove a proc from the list
     */
    
    public void removeProc(int procId) {
    }
}
