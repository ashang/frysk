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

package frysk.proc.dead;

import inua.eio.ByteBuffer;

import java.util.ArrayList;
import java.util.logging.Level;

import lib.dwfl.ElfEHeader;
import lib.dwfl.ElfData;

import frysk.isa.ISA;
import frysk.isa.ElfMap;
import frysk.proc.IsaFactory;
import frysk.proc.Auxv;
import frysk.proc.Isa;
import frysk.proc.MemoryMap;
import frysk.proc.ProcId;
import frysk.proc.TaskId;

public class LinuxExeProc extends DeadProc {

    private ElfData elfData = null;
    ArrayList metaData = new ArrayList();
    LinuxExeHost host = null;
    ProcId id = null;	

    public LinuxExeProc(ElfData data, LinuxExeHost host, ProcId id) {
	super(host, null, id);
	this.host = host;
	this.elfData = data;
	this.id = id;
	buildMetaData();
    }

    public void sendRefresh() {
	LinuxExeTask newTask = new LinuxExeTask(this, new TaskId(0));
	newTask.getClass();
    }

    public Auxv[] getAuxv() {
	return null;
    }
    
    public int getUID() {
	return 0;
    }

    public String[] getCmdLine() {
	return null;
    }

    public String getCommand() {
	return this.host.exeFile.getName();
    }

    public String getExe() {
	return host.exeFile.getAbsolutePath();
    }

    public int getGID() {
	return 0;
    }
    
    ISA sendrecISA() {
	ElfEHeader header = elfData.getParent().getEHeader();
	return ElfMap.getISA(header);
    }

    Isa getIsa() {
	logger.log(Level.FINE, "{0} sendrecIsa\n", this);
	ElfEHeader header = elfData.getParent().getEHeader();
	IsaFactory factory = IsaFactory.getSingleton();
	return factory.getIsaForCoreFile(header.machine);
    }

    public MemoryMap[] getMaps() {
	return (MemoryMap[]) metaData.toArray(new MemoryMap[metaData.size()]);
    }

    public ByteBuffer sendrecMemory() {
	ByteBuffer memory = new ExeByteBuffer(metaData);
	return memory;
    }
    
    private void buildMetaData()
    {
	class BuildExeMaps extends SOLibMapBuilder

	{
	    public void buildMap(long addrLow, long addrHigh, boolean permRead,
		    boolean permWrite, boolean permExecute, long offset,
		    String name, long align) 
	    {
		metaData.add(new MemoryMap(addrLow, addrHigh, permRead,
			permWrite, permExecute, false, offset, -1, -1, -1, -1, -1,
			host.exeFile.getAbsolutePath()));
	    }
	}
	
	BuildExeMaps SOMaps = new BuildExeMaps();
	// Add in case for executables maps.
	SOMaps.construct(this.host.exeFile, 0);
    }

}
