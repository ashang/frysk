// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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

package frysk.proc.live;

import inua.eio.ByteBuffer;
import frysk.proc.Task;
import frysk.proc.Proc;
import frysk.proc.TaskObserver.Attached;
import frysk.sys.ProcessIdentifier;
import frysk.sys.ProcessIdentifierFactory;

/**
 * A live Host/Proc/Task is characterised by its stateful nature;
 * i.e., an ability to respond to stateful requests such as add/remove
 * observers.
 */

abstract class LiveTask extends Task {
    final ProcessIdentifier tid;
    /**
     * New un-attached task of Proc.
     */
    LiveTask(Proc proc, ProcessIdentifier pid) {
	super(proc, pid.intValue());
	tid = pid;
    }
    /**
     * New attached clone.
     */
    LiveTask(Task task, ProcessIdentifier clone) {
	super(task, clone.intValue());
	tid = clone;
    }
    /**
     * New attached child.
     */
    LiveTask(LiveProc proc, Attached attached) {
	super(proc, attached);
	tid = ProcessIdentifierFactory.create(proc.getPid());
    }

    /**
     * Process the add observation event.
     */
    abstract void handleAddObservation(TaskObservation observation);
    /**
     * Process the add observation event.
     */
    abstract void handleDeleteObservation(TaskObservation observation);

    /**
     * Returns the memory as seen by frysk-core. That includes things
     * like inserted breakpoint instructions bytes which are filtered
     * out by <code>getMemory()</code> (which is what you normally
     * want unless you are interested in frysk-core specifics).  <p>
     * Default implementation calls <code>getMemory()</code>, need to
     * be overriden by subclasses for which the raw memory view and
     * the logical memory view are different.
     */
    abstract ByteBuffer getRawMemory();
}
