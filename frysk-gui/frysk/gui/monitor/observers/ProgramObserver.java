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
// type filter text
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

package frysk.gui.monitor.observers;

import frysk.gui.monitor.GuiProc;
import frysk.gui.monitor.ObservableLinkedList;
import frysk.proc.Action;
import frysk.proc.Proc;
import frysk.proc.Task;
import frysk.proc.TaskObserver;

/**
 * A ProgramObserver watches a process for fork events. If the process
 * forks it watches the newly forked child for exec events when the
 * new child has called exec it is added to the procs list. The procs
 * lis is to be watched by clients interested in new procs spawning.
 */
public class ProgramObserver extends TaskObserverRoot implements TaskObserver.Forked, TaskObserver.Execed
{

  /**
   * A list of procs that have been discovered.
   */
  ObservableLinkedList procs;
  
  public ProgramObserver(){
    super("Program Observer", "Observers for launching of new processes");
    this.procs = new ObservableLinkedList();
  }
  
  public Action updateForkedParent (Task parent, Task offspring)
  {
    // ignore the parent forking event.
    return Action.CONTINUE;
  }

  public Action updateForkedOffspring (Task parent, Task offspring)
  {
    System.out.println(this + ": ProgramObserver.updateForkedOffspring()");
    // child created event
    // Add exec observer so the name of the child proc can
    // be checked
    offspring.requestAddExecedObserver(this);
    offspring.requestUnblock(this);
    return Action.BLOCK;
  }

  public void addedTo (Object observable)
  {
    //XXX: nothing to do.
  }

  public void addFailed (Object observable, Throwable w)
  {
     throw new RuntimeException("Failed to add ProgramObserver to " + observable, w);
  }

  public void deletedFrom (Object observable)
  {
    //XXX: nothing to do.
  }

  public Action updateExeced (Task task)
  {
    // a watched child has called exec.
    GuiProc guiProc = GuiProc.GuiProcFactory.getGuiProc(task.getProc());
    System.out.println(this + ": ProgramObserver.updateExeced() " + guiProc.getNiceExecutablePath());
    this.procs.add(guiProc);
    return Action.CONTINUE;
  }

  public void watchProc(Proc proc){
    this.apply(proc);
  }
  
  public void unwatchProc(Proc proc){
    this.unapply(proc);
  }

  public void apply (Task task)
  {
    task.requestAddForkedObserver(this);
  }

  public void unapply (Task task)
  {
    task.requestDeleteForkedObserver(this);
  }
  
  /**
   * Get the ObservableLinkedList for procs.
   * this can be watched in order to catch newly created
   * procs
   * @return
   */
  public ObservableLinkedList getProcsList(){
    return this.procs;
  }
}
