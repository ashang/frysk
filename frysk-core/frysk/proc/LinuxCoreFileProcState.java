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

import java.util.logging.Level;

/**
 * A CoreFile Process State
 *
 */

abstract class LinuxCoreFileProcState
  extends ProcState
{

  protected LinuxCoreFileProcState (String state)
  {
    super (state);
  }
  
  /**
   * Return the Proc's initial state.
   *
   */
  static ProcState initial (Proc proc)    
  {
    logger.log (Level.FINEST, "{0} initial\n", proc); 
    return detached;
  }
  
  /**
   * The process is running free (or at least was the last time its
   * status was checked).
   */
  private static final ProcState detached = new ProcState ("detached")
    {
      ProcState handleRefresh (Proc proc)
      {
	logger.log (Level.FINE, "{0} handleRefresh\n", proc); 
	proc.sendRefresh ();
	return detached;
      }
      ProcState handleRemoval (Proc proc)
      {
	logger.log (Level.FINEST, "{0} handleRemoval\n", proc); 
	
	// XXX: Can't remove a core file Proc, it's there forever
	// and there is only one proc. Maybe need to have a
	// destroyed state for compatability?
	
	return detached;
      }
      ProcState handleAddObservation (Proc proc,
				      Observation observation)
      {
	logger.log (Level.FINE, "{0} handleAddObserver \n", proc); 
	
	// XXX: Fake out for now. What kind of observers would you
	// put on a core file? Might need a brain dead
	// attached state in this scenario for compataibility.
	return detached;
	// return Attaching.initialState (proc, observation);
      }
      
      ProcState handleDeleteObservation (Proc proc,
					 Observation observation)
      {
	logger.log (Level.FINE, "{0} handleDeleteObservation\n", proc); 
	// Must be bogus; if there were observations then the
	// Proc wouldn't be in this state.
	observation.fail (new RuntimeException ("not attached"));
	return detached;
      }
    };
}
