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

package frysk.sys.proc;

/**
 * The contents of <tt>/proc/PID/stat</tt> file.
 */
public class Stat
{
    /**
     * Create a Stat object for PID.
     */
    public Stat ()
    {
    }

    /**
     * Refresh Stat from <tt>/proc/PID/stat</tt>, return true if the
     * scan was successful.  Returns false when the file doesn't
     * exist, or can't be read.  Throws an error if there is some sort
     * of scan problem.
     */
    public native boolean refresh (int pid);

    /**
     * Refresh stat from <tt>/proc/</tt>{@link #pid}<tt>/stat</tt>.
     */
    public final boolean refresh ()
    {
	return refresh (pid);
    }

    /** The process id.  */
    public int pid;
    /** The filename of the executable.  */
    public String comm;
    /** The state represented by a character from "RSDZTW".  */
    public char state;
    /** The parent process id.  */
    public int ppid;
    /** The process group ID.  */
    public int pgrp;
    /** The session ID.  */
    public int session;
    /** The tty.  */
    public int ttyNr;
    /** The process group ID of the process that owns the tty.  */
    public int tpgid;
    /** The flags of the process.  */
    public long flags;
    /** The number of minor faults.  */
    public long minflt;
    /** The number of minor faults of the children.  */
    public long cminflt;
    /** The number of major faults.  */
    public long majflt;
    /** The number of major faluts of the children.  */
    public long cmajflt;
    /** The number of user mode jiffies.  */
    public long utime;
    /** The number of kernel mode jiffies.  */
    public long stime;
    /** The number of user mode child jiffies.  */
    public long cutime;
    /** The number of kernel mode child jiffies.  */
    public long cstime;
    /** The nice value (plus fifteen).  */
    public long priority;
    /** The nice value.  */
    public int nice;
    /** A hard coded zero.  */
    /** The number of jiffies to the next SIGALRM.  */
    public long irealvalue;
    /** The number of jiffies, after system boot, that process started.  */
    public long starttime;
    /** Virtual memory size.  */
    public long vsize;
    /** Resident Set Size.  */
    public long rss;
    /** Current rss limit.  */
    public long rlim;
    /** The address above which program text can run.  */
    public long startcode;
    /** The address below which program text can run.  */
    public long endcode;
    /** The address of the start of the stack.  */
    public long startstack;
    /** The current value of the stack pointer.  */
    public long kstkesp;
    /** The current instruction pointer.  */
    public long kstkeip;
    /** The bitmap of pending signals.  */
    public long signal;
    /** The bitmap of blocked signals.  */
    public long blocked;
    /** The bitmap of ignored signals.  */
    public long sigignore;
    /** The bitmap of catched signals.  */
    public long sigcatch;
    /** The "channel" in which the process is waiting.  */
    public long wchan;
    /** Number of pages swapped.  */
    public long nswap;
    /** Number of pages swapped by children.  */
    public long cnswap;
    /** Signal to be sent to parent.  */
    public int exitSignal;
    /** CPU number last executed on.  */
    public int processor;
}
