// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

package frysk.testbed;

import java.util.List;
import java.util.LinkedList;
import java.util.Observer;
import java.util.Observable;
import frysk.proc.Task;
import frysk.proc.Manager;

/**
 * Observer that counts the number of tasks <em>frysk</em> reports
 * as added and removed to the system.. This automatically wires
 * itself in using the Proc's procAdded observer.
 */

public class TaskCounter {
    /**
     * List of tasks added.
     */
    public final List added = new LinkedList();

    /**
     * List of tasks removed.
     */
    public final List removed = new LinkedList();

    /**
     * Only count descendants of this process?
     */
    private boolean descendantsOnly;

    /**
     * Create a task counter that monitors task added and removed
     * events. If descendantsOnly, limit the count to tasks
     * belonging to descendant processes.
     */
    public TaskCounter (boolean descendantsOnly) {
	this.descendantsOnly = descendantsOnly;
	Manager.host.observableTaskAddedXXX.addObserver(new Observer() {
		public void update (Observable o, Object obj) {
		    Task task = (Task) obj;
		    if (TaskCounter.this.descendantsOnly
			&& ! TestLib.isDescendantOfMine(task.getProc()))
			return;
		    added.add(task);
		}
	    });
	Manager.host.observableTaskRemovedXXX.addObserver(new Observer() {
		public void update (Observable o, Object obj) {
		    Task task = (Task) obj;
		    if (TaskCounter.this.descendantsOnly
			&& ! TestLib.isDescendantOfMine(task.getProc()))
			return;
		    removed.add(task);
		}
	    });
    }
	
    /**
     * Create a task counter that counts all task add and removed
     * events.
     */
    public TaskCounter () {
	this(false);
    }
}
