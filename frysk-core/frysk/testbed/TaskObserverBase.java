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

import frysk.proc.TaskObserver;
import java.util.logging.Logger;
import java.util.logging.Level;
import frysk.junit.TestCase;
import java.util.LinkedList;
import java.util.List;

/**
 * A base class for implementing TaskObservers. This provides a
 * framework for both automatically adding and implementing
 * TaskObserver's. The client supplied .updateClass method is called
 * as each new task is found. It should register itself with the
 * applicable observer.
 */

public abstract class TaskObserverBase
    implements TaskObserver
{
    protected static final Logger logger = Logger.getLogger("frysk");
    /**
     * Count of number of times that this observer was added to a Task's
     * observer set.
     */
    private List added = new LinkedList();
    public void addedTo (Object o) {
	logger.log(Level.FINE, "{0} addedTo {1}\n",
		   new Object[] {this, o});
	added.add(o);
    }
    public int addedCount() {
	return added.size();
    }
    
    /**
     * Count of number of times this observer was deleted from a
     * Task's observer set.
     */
    private List deleted = new LinkedList();
    public void deletedFrom (Object o) {
	logger.log(Level.FINE, "{0} deletedFrom {1}\n",
		   new Object[] {this, o});
	deleted.add(o);
    }
    public int deletedCount() {
	return deleted.size();
    }

    /**
     * The add operation failed, should never happen.
     */
    public void addFailed (Object o, Throwable w) {
	TestCase.fail("add to " + o + " failed due to " + w);
    }

    public String toString() {
	return super.toString() + added + deleted;
    }
}
