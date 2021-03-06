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

package frysk.proc.dummy;

import frysk.proc.TaskAttachedObserverXXX;
import inua.eio.ByteBuffer;
import frysk.proc.TaskObserver;
import frysk.isa.ISA;
import frysk.isa.banks.RegisterBanks;
import frysk.proc.Task;
import frysk.proc.Proc;

public class DummyTask extends Task {

    public DummyTask (Proc parent) {
	super(parent, parent.getPid());
    }
    public DummyTask (Proc parent, int pid) {
	super(parent, pid);
    }
    public String getStateString() {
	return "Attached";
    }
    public ISA getISA() {
	return null;
    }
    public ByteBuffer getMemory() {
	return null;
    }
    protected RegisterBanks getRegisterBanks() {
	return null;
    }

    protected String getStateFIXME() {
	return "dummy";
    }

    public void requestUnblock(final TaskObserver observerArg) {
	throw new RuntimeException("oops!");
    }
    public void requestAddClonedObserver(TaskObserver.Cloned o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteClonedObserver(TaskObserver.Cloned o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddAttachedObserver(TaskAttachedObserverXXX o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteAttachedObserver(TaskAttachedObserverXXX o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddForkedObserver(TaskObserver.Forked o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteForkedObserver(TaskObserver.Forked o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddTerminatedObserver(TaskObserver.Terminated o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteTerminatedObserver(TaskObserver.Terminated o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddTerminatingObserver(TaskObserver.Terminating o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteTerminatingObserver(TaskObserver.Terminating o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddExecedObserver(TaskObserver.Execed o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteExecedObserver(TaskObserver.Execed o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddSyscallsObserver(TaskObserver.Syscalls o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteSyscallsObserver(TaskObserver.Syscalls o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddSignaledObserver(TaskObserver.Signaled o) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteSignaledObserver(TaskObserver.Signaled o) {
	throw new RuntimeException("oops!");
    }
    public void requestAddCodeObserver(TaskObserver.Code o, long a) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteCodeObserver(TaskObserver.Code o, long a) {
	throw new RuntimeException("oops!");
    }
    
    public void requestAddWatchObserver(TaskObserver.Watch o, long address, 
	    				int length, boolean writeOnly) {
	throw new RuntimeException("requestAddWatchObserver");
    }

    public  void requestDeleteWatchObserver(TaskObserver.Watch o, long address,
	    				int length, boolean writeOnly) {

	throw new RuntimeException("requestDeleteWatchObserver");
    }

    public void requestAddInstructionObserver(TaskObserver.Instruction o) {
	throw new RuntimeException("oops!");
    }
    
    
    public void requestDeleteInstructionObserver(TaskObserver.Instruction o) {
	throw new RuntimeException("oops!");
    }

    public boolean isInstructionObserverAdded (TaskObserver.Instruction o) {
	throw new RuntimeException("isInstructionObserverAddded");
    }
    
    public int getMod() {
	return 1;
    }

    public long getPC() {
	throw new RuntimeException("oops!");
    }
    public void setPC(long addr) {
	throw new RuntimeException("oops!");
    }
}
