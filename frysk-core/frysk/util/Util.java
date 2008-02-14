// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, 2008, Red Hat Inc.
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

package frysk.util;

import gnu.classpath.tools.getopt.OptionException;
import java.io.File;
import frysk.proc.Host;
import frysk.proc.FindProc;
import frysk.proc.Manager;
import frysk.proc.dead.LinuxCoreHost;
import frysk.proc.Proc;

public class Util
{

  private Util ()
  {
  }
  
    /**
     * Return the Proc associated with a coreFile.
     * @param coreFile the given coreFile.
     * @return The Proc for the given coreFile.
     */
    public static Proc getProcFromCoreFile(File coreFile) {
	LinuxCoreHost core = new LinuxCoreHost(Manager.eventLoop, coreFile);
	Proc proc = core.getSoleProcFIXME();
	if (proc == null)
	    throw new RuntimeException("Core file contains no proc.");
	return proc;
    }
  
    public static Proc getProcFromCoreFile(File coreFile, File exeFile) {
	LinuxCoreHost core = new LinuxCoreHost(Manager.eventLoop, coreFile,
					       exeFile);
	Proc proc = core.getSoleProcFIXME();
	if (proc == null)
	    throw new RuntimeException("Cannot find a process in this corefile.");
	return proc;
    }
  
  public static Proc getProcFromCoreExePair(CoreExePair coreExePair) {
      if (coreExePair.exeFile == null)
	  return getProcFromCoreFile(coreExePair.coreFile);
      else
	  return getProcFromCoreFile(coreExePair.coreFile, coreExePair.exeFile);
  }
  
    /**
     * Return a Proc associated with the given pid.
     * @param procId The given pid.
     * @return A Proc for the given pid.
     */
    public static Proc getProcFromPid(int pid) throws OptionException {
	class ProcFinder implements FindProc {
	    Proc proc;
	    public void procFound(Proc p) {
		proc = p;
		Manager.eventLoop.requestStop();
	    }
	    public void procNotFound(int pid) { 
		proc = null;
		Manager.eventLoop.requestStop();
	    } 
	}
	ProcFinder finder = new ProcFinder();
	Manager.host.requestProc(pid, finder);
	Manager.eventLoop.run();
	if (finder.proc == null)
	    throw new OptionException("Process " + pid + " not found");
	return finder.proc;
    }
  
    /**
     * Return the Proc associated with an executable File.
     * @param exeHost the Host associated with the desired Proc.
     * @return The Proc for the given executable File.
     */
    public static Proc getProcFromExeFile(Host exeHost) {
	Proc proc = exeHost.getSoleProcFIXME();
	if (proc == null)
	    throw new RuntimeException("Cannot find a process in this executable.");
	return proc;
    }
}
