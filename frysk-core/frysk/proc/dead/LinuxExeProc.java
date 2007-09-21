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

package frysk.proc.dead;

import inua.eio.ByteBuffer;

import java.util.ArrayList;

import frysk.proc.Auxv;
import frysk.proc.Host;
import frysk.proc.Isa;
import frysk.proc.MemoryMap;
import frysk.proc.Proc;
import frysk.proc.ProcId;
import frysk.proc.ProcState;
import frysk.proc.TaskId;

public class LinuxExeProc extends Proc {

    ArrayList metaData = new ArrayList();
    LinuxExeHost host = null;
    ProcId id = null;
    
    protected LinuxExeProc(Host host, Proc parent, ProcId id) {
	super(host, parent, id);
	this.host = (LinuxExeHost) host;
	this.id = id;
	buildMetaData();
    }

    protected ProcState getInitialState(boolean procStarting) {
	return null;
    }


    public void sendRefresh() {
	 LinuxExeTask newTask = new LinuxExeTask(this, new TaskId(0), null);
	 newTask.getClass();
    }


    protected Auxv[] sendrecAuxv() {
	return null;
    }


    protected String[] sendrecCmdLine() {
	return null;
    }

    protected String sendrecCommand() {
	return this.host.exeFile.getName();
    }

    protected String sendrecExe() {
	return host.exeFile.getAbsolutePath();
    }

    protected int sendrecGID() {
	return 0;
    }

    protected Isa sendrecIsa() {
	return null;
    }

    protected MemoryMap[] sendrecMaps() {
	return null;
    }

    public ByteBuffer sendrecMemory() {
	ByteBuffer memory = new ExeByteBuffer(metaData);
	return memory;
    }
    protected int sendrecUID() {
	return 0;
    }
    
    private void buildMetaData()
    {
	class BuildExeMaps extends SOLibMapBuilder

	{
	    public void buildMap(long addrLow, long addrHigh, boolean permRead,
		    boolean permWrite, boolean permExecute, long offset,
		    String name, long align) 
	    {
		
		metaData.add(new MapAddressHeader(addrLow, addrHigh, permRead,
			permWrite, permExecute, 0, offset, 0, 0, name, align));
	    }
	}
	
	BuildExeMaps SOMaps = new BuildExeMaps();
	// Add in case for executables maps.
	SOMaps.construct(this.host.exeFile, 0);
    }

}