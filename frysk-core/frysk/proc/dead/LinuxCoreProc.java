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

import frysk.proc.Auxv;
import frysk.proc.MemoryMap;
import frysk.rsl.Log;

public class LinuxCoreProc extends DeadProc {
    private static final Log fine = Log.fine(LinuxCoreProc.class);
  
    private final LinuxCoreInfo info;

    LinuxCoreProc(LinuxCoreHost host, LinuxCoreInfo info) {
	super(host, null, info.prpsInfo.getPrPid());
	fine.log(this, "LinuxCoreProc host", host, "info", info);
	this.info = info;
	info.constructTasks(this);
    }	

    public String getCommand() {
	String command = info.prpsInfo.getPrFname();
	fine.log(this, "getCommand()", command);
        return command;
    }

    public String getExe() {
	String exe = info.exeFile.getPath();
	fine.log(this, "getExe()", exe);
	return exe;
    }

    public int getUID() {
	fine.log(this,"getUID()", info.prpsInfo.getPrUid());
	return (int) info.prpsInfo.getPrUid();
    }

    public int getGID() {
	fine.log(this,"getGID()", info.prpsInfo.getPrGid());
	return (int) info.prpsInfo.getPrGid();
    }

    public String[] getCmdLine() {
	fine.log(this, "getCmdLine()", info.args);
	return info.args;
    }

    protected CorefileByteBuffer getMemory() {
	return info.memory;
    }

    public MemoryMap[] getMaps () {
	return info.memoryMaps;
    }

    public Auxv[] getAuxv() {
	return info.auxv;
    }
}
