// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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

package frysk.ftrace;

import frysk.proc.Task;
import java.io.File;
import frysk.proc.Syscall;

/**
 * Ltrace observers implement this interface to get notified about
 * interesting events in traced program.
 *
 * XXX: Convert all Object[] arguments to Value[] or something.
 */
public interface LtraceObserver
{
  /** The task has hit PLT entry for given function. */
  void pltEntryEnter(Task task, Symbol symbol, Object[] args);

  /** The task has returned from given function traced via PLT
      entry. */
  void pltEntryLeave(Task task, Symbol symbol, Object retVal);

  /** The task has entered given function traced via dynamic symbol
      table. */
  void dynamicEnter(Task task, Symbol symbol, Object[] args);

  /** The task has left given function traced via dynamic symbol
      table. */
  void dynamicLeave(Task task, Symbol symbol, Object retVal);

  /** The task has entered given function traced via static symbol
      table. */
  void staticEnter(Task task, Symbol symbol, Object[] args);

  /** The task has left given function traced via static symbol
      table. */
  void staticLeave(Task task, Symbol symbol, Object retVal);

  /** The task has entered a syscall. */
  void syscallEnter(Task task, Syscall syscall, Object[] args);

  /** The task has returned from a syscall. */
  void syscallLeave(Task task, Syscall syscall, Object retVal);

  /** New file was mapped. */
  void fileMapped(Task task, File file);

  /** Mapped file was unmapped. */
  void fileUnmapped(Task task, File file);

  /** New task was attached. */
  void taskAttached(Task task);

  /** Task was removed, died or detached. */
  void taskRemoved(Task task);
}
