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

package frysk.proc;

import java.io.File;
import java.util.Collection;
import frysk.rsl.Log;
import frysk.sysroot.SysRoot;
import frysk.sysroot.SysRootCache;
import frysk.sysroot.SysRootFile;

/**
 * A host machine.
 *
 * A HOST has processes which contain threads.  A HOST also has a
 * process that is running this code - frysk is self aware.
 */

public abstract class Host implements Comparable {
    private static final Log fine = Log.fine(Host.class);

    /**
     * The host corresponds to a specific system.
     */
    protected Host() {
	fine.log(this, "Host");
    }
  
    /**
     * Find a specifc process from its Id.
     */
    public abstract void requestProc(int pid, FindProc finder);
    
    /**
     * Given a set of knownProcesses and knownDaemons (children of
     * init) scan the system passing back the set of newProcesses (not
     * in knownProcesses), deadProcesses (were in knownProcesses but
     * exited), newDaemons (processes that re-parented to init).
     */
    public abstract void requestRefresh(Collection knownProcesses,
					HostRefreshBuilder update);

    /**
     * Tell the host to create a running child process.
     *
     * Unlike other requests, this operation is bound to an explicit
     * call-back.  Doing this means that the requester has a robust
     * way of receiving an acknowledge of the operation.  Without this
     * there would be no reliable way to bind to the newly created
     * process - frysk's state machine could easily detach before the
     * requester had an opportunity to add an attached observer.
     */
    public abstract void requestCreateAttachedProc(File exe,
						   String stdin,
						   String stdout,
						   String stderr,
						   String[] args,
						   String libs,
						   TaskAttachedObserverXXX attachedObserver);
    /**
     * Request that a new attached and running process(with stdin,
     * stdout, and stderr are shared with this process) be created.
     */
    public void requestCreateAttachedProc(String stdin, String stdout,
					  String stderr, String[] args, 
					  TaskAttachedObserverXXX attachedObserver) {
	fine.log(this, "requestCreateAttachedProc", args, "observer",
		 attachedObserver);
	SysRoot sysRoot = new SysRoot(SysRootCache.getSysRoot(args[0]));
	requestCreateAttachedProc(new File(args[0]), stdin, stdout, stderr,
				  args, sysRoot.getLibPathViaSysRoot(),
				  attachedObserver);
    }
    /**
     * Request that a new attached and running process(with stdin,
     * stdout, and stderr are shared with this process) be created.
     */
    public void requestCreateAttachedProc(String[] args,
					  TaskAttachedObserverXXX attachedObserver) {
	fine.log(this, "requestCreateAttachedProc", args, "observer",
		 attachedObserver);
	SysRoot sysRoot = new SysRoot(SysRootCache.getSysRoot(args[0]));
	requestCreateAttachedProc(new File(args[0]), null, null, null,
				  args, sysRoot.getLibPathViaSysRoot(),
				  attachedObserver);
    }
    /**
     * Request that a new attached and running process based on
     * TEMPLATE be created.
     */
    public void requestCreateAttachedProc(Proc template,
					  TaskAttachedObserverXXX attachedObserver) {
	fine.log(this, "requestCreateAttachedProc template", template,
		 "observer", attachedObserver);
	SysRootFile sysRootFile = template.getExeFile();
	requestCreateAttachedProc(new File(sysRootFile.getSysRootedPath()),
				  null, null, null,
				  template.getCmdLine(),
				  new SysRoot(sysRootFile.getSysRoot()).getLibPathViaSysRoot(),
				  attachedObserver);
    }					  

    /**
     * Return the process corresponding to this running frysk instance
     * found on this host.
     */
    public abstract Proc getSelf();

    /**
     * Print this.
     */
    public String toString() {
	return ("{" + super.toString()
		+ "}");
    }
    
    /**
     * Returns the name of the host
     */
    public abstract String getName();

    public int compareTo(Object o) {
	return getName().compareTo(((Host)o).getName());
    }
}
