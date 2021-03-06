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

package frysk.sys.proc;

import frysk.sys.ProcessIdentifier;
import frysk.rsl.Log;
import frysk.rsl.LogFactory;

/**
 * Scan the <tt>/proc</tt>, or <tt>/proc/</tt>pid<tt>/task</tt>
 * directory for process/task IDs building each ID as it is
 * encountered.
 */
public abstract class ProcBuilder {
    private static final Log warning = LogFactory.warning(ProcBuilder.class);

    /**
     * Iterate over the <tt>/proc</tt>pid<tt>/task</tt> directory
     * notifying ProcBuilder of each "interesting" entry.  Use
     * "finally" to ensure that the directory is always closed.
     */
    public final void construct(ProcessIdentifier pid) {
	construct(pid.intValue(), warning);
    }
    /**
     * Iterate over the <tt>/proc</tt> directory notifying TaskBuilder
     * of each "interesting" entry.
     */
    public final void construct() {
	construct(0, warning);
    }
    /**
     * Called for each process or task ID in the <tt>/proc</tt>, or
     * <tt>/proc/PID/task</tt> directory.
     */
    abstract public void build(ProcessIdentifier pid);


    /**
     * Iterate over /proc/PID/ finding all entries.
     */
    private native void construct(int pid, Log warning);
}
