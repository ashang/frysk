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

package frysk.util;

import java.io.File;
import java.util.Iterator;

import frysk.proc.Host;
import frysk.proc.Manager;
import frysk.proc.ProcId;
import frysk.proc.dead.LinuxHost;
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
  public static Proc getProcFromCoreFile(File coreFile)
  {
    LinuxHost core = new LinuxHost(Manager.eventLoop, coreFile);

    Iterator iterator = core.getProcIterator();

    Proc proc;
    if (iterator.hasNext())
      proc = (Proc) iterator.next();
    else
      {
        proc = null;
        throw new RuntimeException("No proc in this corefile");
      }
    if (iterator.hasNext())
      throw new RuntimeException("Too many procs on this corefile");
    
    return proc;
  }
  
  public static Proc getProcFromCoreFile(File coreFile, File exeFile)
  {
    LinuxHost core = new LinuxHost(Manager.eventLoop, coreFile, exeFile);

    Iterator iterator = core.getProcIterator();

    Proc proc;
    if (iterator.hasNext())
      proc = (Proc) iterator.next();
    else
      {
        proc = null;
        throw new RuntimeException("No proc in this corefile");
      }
    if (iterator.hasNext())
      throw new RuntimeException("Too many procs on this corefile");
    
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
  public static Proc getProcFromPid(ProcId procId)
  {
      class ProcFinder implements Host.FindProc {
	  Proc proc;
	  public void procFound (ProcId procId)
	  {
	      proc = Manager.host.getProc(procId);
	      Manager.eventLoop.requestStop();
	  }
	  
	  public void procNotFound (ProcId procId, Exception e)
	  { 
	      System.err.println("Couldn't find the process: "
				 + procId.toString());
	      Manager.eventLoop.requestStop();  
	  } 
      }
      ProcFinder finder = new ProcFinder();
      Manager.host.requestFindProc(procId, finder);
      Manager.eventLoop.run();
      return finder.proc;
  }
  
  /**
   * Return the Proc associated with an executable File.
   * @param exeHost the Host associated with the desired Proc.
   * @return The Proc for the given executable File.
   */
  public static Proc getProcFromExeFile(Host exeHost)
  {
      Iterator iterator = exeHost.getProcIterator();

      Proc proc;
      if (iterator.hasNext())
        proc = (Proc) iterator.next();
      else
        {
          proc = null;
          throw new RuntimeException("No proc in this exefile");
        }
      if (iterator.hasNext())
        throw new RuntimeException("Too many procs in this exefile");
      
      return proc;
    }
}
