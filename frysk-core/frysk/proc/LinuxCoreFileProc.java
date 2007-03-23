// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

import lib.elf.ElfData;
import lib.elf.ElfPrpsinfo;
import lib.elf.ElfPrAuxv;
import lib.elf.ElfEHeader;
import lib.elf.ElfPrstatus;
import frysk.sys.proc.AuxvBuilder;
import java.util.logging.Level;

public class LinuxCoreFileProc extends Proc 
{
  
  private ElfData elfData = null;
  private ElfPrpsinfo elfProc = null;

  public LinuxCoreFileProc(ElfData data, Host host, ProcId procId )
  {
    super(host, null, procId);
    this.elfData = data;
    this.elfProc = ElfPrpsinfo.decode(elfData);
  }
	
  public String getCommand()
  {
    return elfProc.getPrFname();
  }

  protected String sendrecCommand() 
  {
    return elfProc.getPrFname();
  }

  protected String sendrecExe() 
  {
    return elfProc.getPrFname();
  }

  protected int sendrecUID() 
  {
    return (int) elfProc.getPrUid();
  }

  protected int sendrecGID() 
  {
    return (int) elfProc.getPrGid();
  }

  protected String[] sendrecCmdLine() 
  {
    String args[] = {elfProc.getPrPsargs()};
    return args;
  }

  void sendRefresh() 
  {
    // Find tasks. Refresh is a misnomer here as 
    // Corefiles will never spawn new tasks beyond the
    // original refresh, and will lose them. 
    
    // Check to see if we did an initial refresh. If so, don't
    // refresh again as core files are static.
    ElfPrstatus elfTasks[] = null;
    elfTasks = ElfPrstatus.decode(elfData);
    for (int i=0; i<elfTasks.length; i++)
      {
	Task newTask = new LinuxCoreFileTask(LinuxCoreFileProc.this, elfTasks[i]);
	newTask.getClass();
      }
  }

  Auxv[] sendrecAuxv ()
    {
      final ElfPrAuxv prAuxv =  ElfPrAuxv.decode(elfData);

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
      auxv.construct (prAuxv.getAuxvBuffer());
      return auxv.vec;
    }


  Isa sendrecIsa() 
  {
    logger.log(Level.FINE, "{0} sendrecIsa\n", this);

    ElfEHeader header = elfData.getParent().getEHeader();
    
    IsaFactory factory = IsaFactory.getFactory();
    return factory.getIsaForCoreFile(header.machine);
  }

  protected ProcState getInitialState (boolean procStarting) 
  {
    return LinuxCoreFileProcState.initial(this);
  }
}
