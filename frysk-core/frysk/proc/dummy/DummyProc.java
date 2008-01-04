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

import frysk.proc.Auxv;
import frysk.proc.ProcId;
import frysk.proc.Isa;
import frysk.proc.MemoryMap;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;
import frysk.proc.TaskObservable;
import frysk.proc.TaskObservation;

public class DummyProc extends Proc {
    public DummyProc() {
	super(new DummyHost(), null, new ProcId(42));
    }
  
    public String getCommand() {
	return "Foo";
    }
    protected String sendrecCommand() {
	return null;
    }
    protected String sendrecExe() {
	return null;
    }
    protected int sendrecUID() {
	return 0;
    }
    protected int sendrecGID() {
	return 0;
    }
    protected String[] sendrecCmdLine() {
	return null;
    }
    public void sendRefresh() {
    }
    public MemoryMap[] sendrecMaps () {
        return null;
    }
    protected Auxv[] sendrecAuxv() {
	return null;
    }
    protected Isa sendrecIsa() {
	return null;
    }
    protected String getStateFIXME() {
	return "<dummy>";
    }
    public void requestRefresh() {
	throw new RuntimeException("oops!");
    }
    public void performRemoval() {
	throw new RuntimeException("oops!");
    }
    public void performTaskAttachCompleted (final Task theTask) {
	throw new RuntimeException("oops!");
    }
    public void performTaskDetachCompleted(final Task theTask) {
	throw new RuntimeException("oops!");
    }
    protected void performTaskDetachCompleted(final Task theTask, final Task theClone) {
	throw new RuntimeException("oops!");
    }
    protected void performDetach() {
	throw new RuntimeException("oops!");
    }
    protected void handleAddObservation(TaskObservation observation) {
	throw new RuntimeException("oops!");
    }
    public void requestAddObserver(Task task, TaskObservable observable,
				   TaskObserver observer) {
	throw new RuntimeException("oops!");
    }
    public void requestAddSyscallObserver(Task task, TaskObservable observable,
					  TaskObserver observer) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteObserver(Task task, TaskObservable observable,
				      TaskObserver observer) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteSyscallObserver(final Task task,
				      TaskObservable observable,
				      TaskObserver observer) {
	throw new RuntimeException("oops!");
    }
    public void requestAddCodeObserver(Task task, TaskObservable observable,
				       TaskObserver.Code observer,
				       long address) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteCodeObserver(Task task, TaskObservable observable,
					  TaskObserver.Code observer,
					  long address)    {
	throw new RuntimeException("oops!");
    }
    public void requestAddInstructionObserver(Task task,
					      TaskObservable observable,
					      TaskObserver.Instruction observer) {
	throw new RuntimeException("oops!");
    }
    public void requestDeleteInstructionObserver(Task task,
						 TaskObservable observable,
						 TaskObserver.Instruction observer) {
	throw new RuntimeException("oops!");
    }
}