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

package frysk.proc.live;

import frysk.sys.proc.Exe;
import frysk.proc.ProcId;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.Host;
import frysk.sys.proc.Stat;
import frysk.proc.Auxv;
import frysk.sys.proc.AuxvBuilder;
import frysk.proc.MemoryMap;
import java.util.ArrayList;
import frysk.proc.Isa;
import frysk.sys.proc.CmdLineBuilder;
import frysk.sys.proc.MapsBuilder;
import frysk.sys.proc.Status;
import frysk.proc.IsaFactory;
import java.util.logging.Level;
import frysk.proc.ProcState;
import frysk.sys.proc.ProcBuilder;
import java.util.Map;
import java.util.HashMap;
import frysk.proc.TaskId;
import java.util.Iterator;
import java.io.File;

/**
 * A Linux Proc tracked using PTRACE.
 */

public class LinuxProc extends LiveProc {
    /**
     * Create a new detached process.  RUNNING makes no sense here.
     * Since PARENT could be NULL, also explicitly pass in the host.
     */
    public LinuxProc (Host host, Proc parent, ProcId pid, Stat stat)
    {
	super (host, parent, pid);
	this.stat = stat;
    }
    /**
     * Create a new, definitely attached, definitely running fork of
     * Task.
     */
    public LinuxProc (Task task, ProcId forkId)
    {
	super (task, forkId);
    }

    /**
     * Get the AUXV.
     */
    protected Auxv[] sendrecAuxv ()
    {
	class BuildAuxv
	    extends AuxvBuilder
	{
	    Auxv[] vec;
	    public void buildBuffer (byte[] auxv)
	    {
	    }
	    public void buildDimensions (int wordSize, boolean bigEndian,
					 int length)
	    {
		vec = new Auxv[length];
	    }
	    public void buildAuxiliary (int index, int type, long val)
	    {
		vec[index] = new Auxv (type, val);
	    }
	}
	BuildAuxv auxv = new BuildAuxv ();
	auxv.construct (getPid ());
	return auxv.vec;
    }
    /**
     * Get the address-maps.
     */
    protected MemoryMap[] sendrecMaps () 
    {
        class BuildMaps
	    extends MapsBuilder
	{
	 
	    ArrayList  maps = new ArrayList();
            byte[] mapsLocal;

     	    public void buildBuffer (byte[] maps)
	    {
	        mapsLocal = maps;
	        maps[maps.length - 1] = 0;
	    }
	  
	    public void buildMap (long addressLow, long addressHigh,
				  boolean permRead, boolean permWrite,
				  boolean permExecute, boolean shared, long offset,
				  int devMajor, int devMinor, int inode,
				  int pathnameOffset, int pathnameLength)
	    {

	        byte[] filename = new byte[pathnameLength];

		System.arraycopy(mapsLocal, pathnameOffset, filename, 0,
				 pathnameLength);

		MemoryMap map = new MemoryMap(addressLow, addressHigh,
					      permRead, permWrite,
					      permExecute, shared, offset,
					      devMajor, devMinor, inode,
					      pathnameOffset,
					      pathnameLength, new
					      String(filename));
		maps.add(map);
		
	    }
	}

	BuildMaps constructedMaps = new BuildMaps ();
	constructedMaps.construct(getPid ());
	MemoryMap arrayMaps[] = new MemoryMap[constructedMaps.maps.size()];
	constructedMaps.maps.toArray(arrayMaps);
	return arrayMaps;
    }
    /**
     * Get the Command line.
     */
    protected String[] sendrecCmdLine ()
    {
	class BuildCmdLine
	    extends CmdLineBuilder
	{
	    String[] argv;
	    public void buildBuffer (byte[] buf)
	    {
	    }
	    public void buildArgv (String[] argv)
	    {
		this.argv = argv;
	    }
	}
	BuildCmdLine cmdLine = new BuildCmdLine ();
	cmdLine.construct (getPid ());
	return cmdLine.argv;
    }
    /**
     * Get the process group-ID.
     */
    protected int sendrecGID()
    {
	return Status.getGID (getPid ());
    }
    /**
     * Get the process user-ID.
     */
    protected int sendrecUID ()
    {
	return Status.getUID (getPid ());
    }
    /**
     * Get the Executable.
     *
     * XXX: This is racy - it can miss file renames.  The alternative
     * would be to have two methods; one returning a file descriptor
     * and a second returning the exe as it should be (but possibly
     * isn't :-).  Better yet have utrace handle it :-)
     */
    protected String sendrecExe () {
	String exe = Exe.get(getPid());
	// Linux's /proc/$$/exe can get screwed up in several ways.
	// Detect each here and return null.
	if (exe.endsWith(" (deleted)"))
	    // Assume (possibly incorrectly) that a trailing
	    // "(deleted)" always indicates a deleted file.
	    return null;
	if (exe.indexOf((char)0) >= 0)
	    // Assume that an EXE that has somehow ended up with an
	    // embedded NUL character is invalid.  This happens when
	    // the kernel screws up "mv a-really-long-file $exe"
	    // leaving the updated EXE string with something like
	    // "$exe<NUL>ally-long-file (deleted)".
	    return null;
	if (new File(exe).exists())
	    // Final sanity check; the above two should have covered
	    // all possible cases.  But one never knows.
	    return exe;
	return null;
    }
    /**
     * Get the Process-wide ISA.
     *
     * XXX: IsaFactory should not be given a PID to extract the ISA
     * from, instead IsaFactory should receive some sort of
     * higher-level object, such as the ELF MACHINE.
     */
    protected Isa sendrecIsa ()
    {
	logger.log(Level.FINE, "{0} sendrecIsa\n", this);
	IsaFactory factory = IsaFactory.getSingleton();
	return factory.getIsa(getId().intValue());
    }

    /**
     * If it hasn't already been read, read the stat structure.
     */
    public Stat getStat ()
    {
	if (stat == null) {
	    stat = new Stat ();
	    stat.refresh (getPid());
	}
	return stat;
    }
    private Stat stat;

    public String sendrecCommand ()
    {
	return getStat ().comm;
    }

    /**
     * Some constructors in Proc.java need a starting state.  As Proc
     * is abstract and cannot return a state specific to its subclass,
     * return here in the subclass
     */
    protected ProcState getInitialState (boolean procStarting)
    {
	return LinuxProcState.initial(this, procStarting);
    }

    /**
     * Refresh the Proc.
     */
    public void sendRefresh ()
    {
	// Compare this against the existing taskPool.  ADDED
	// accumulates any tasks added to the taskPool.  REMOVED,
	// starting with all known tasks has any existing tasks
	// removed, so that by the end it contains a set of removed
	// tasks.
	class TidBuilder
	    extends ProcBuilder
	{
	    Map added = new HashMap ();
	    HashMap removed = (HashMap) ((HashMap)taskPool).clone ();
	    TaskId searchId = new TaskId ();
	    public void buildId (int tid)
	    {
		searchId.id = tid;
		if (removed.containsKey (searchId)) {
		    removed.remove (searchId);
		}
		else {
		    // Add the process (it currently isn't attached).
		    Task newTask = new LinuxTask (LinuxProc.this,
						  new TaskId (tid));
		    added.put (newTask.getTaskId(), newTask);
		}
	    }
	}
	TidBuilder tasks = new TidBuilder ();
	tasks.construct (getPid());
	// Tell each task that no longer exists that it has been
	// removed.
	for (Iterator i = tasks.removed.values().iterator(); i.hasNext();) {
	    Task task = (Task) i.next ();
	    // XXX: Should there be a TaskEvent.schedule(), instead of
	    // Manager .eventLoop .appendEvent for injecting the event
	    // into the event loop?
	    task.performRemoval ();
	    remove (task);
	}
    }
}
