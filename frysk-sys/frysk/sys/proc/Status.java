// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2008, Red Hat Inc.
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

import frysk.rsl.Log;
import frysk.sys.ProcessIdentifier;

/**
 * Partial wrapper for /proc/$$/status.
 *
 * This class supplements frysk.sys.proc.Stat as that class does not
 * contain GID and UID.
 *
 * This class extracts that information from either /proc/$$/status or
 * from a buffer that has been been passed to it for parsing.
 */

public class Status {
    private static final Log fine = Log.fine(Status.class);

    public Status() {
    }

    public String toString() {
	if (pid != null)
	    return "/proc/" + pid + "/status";
	else
	    return super.toString();
    }

    public Status scan(ProcessIdentifier pid) {
	fine.log(this, "stan", pid);
	this.pid = pid;
	return scan(pid.intValue());
    }
    private native Status scan(int pid);
    /**
     * For testing only; package private.
     */
    native Status scan(byte[] buffer);

    /**
     * Re-scan /proc/$$/status using the previous pid.
     */
    public Status rescan() {
	return scan(pid.intValue());
    }

    public ProcessIdentifier pid;

    /**
     * The UID from the /proc/$$/status file.
     */
    public int uid;

    /**
     * Return the GID from the /proc/$$/status file according to the
     * PID passed.
     */
    public int gid;

    /**
     * The state from /proc/$$/status file.
     */
    public char state;

    /**
     * True if the PID is in a stopped state.
     */
    public boolean stoppedState;
}
