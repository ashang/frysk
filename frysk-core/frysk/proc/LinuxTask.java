// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

import frysk.sys.Errno;
import frysk.sys.Ptrace;
import frysk.sys.PtraceByteBuffer;
import frysk.sys.Sig;
import frysk.sys.Signal;

import inua.eio.ByteOrder;
import java.util.logging.Level;

/**
 * Linux implementation of Task.
 */

public class LinuxTask extends Task
{
  private long ptraceOptions = 0;	
  
  // XXX: For moment wire in standard 32-bit memory
  // map.  This will be replaced by a memory map created using
  // information from /proc/PID/maps.
  private void setupMapsXXX() throws TaskException
  {
    ByteOrder byteOrder = getIsa().getByteOrder();
    // XXX: For writing at least, PTRACE must be used as /proc/mem
    // cannot be written to.
    memory = new PtraceByteBuffer(id.id, PtraceByteBuffer.Area.DATA,
				  0xffffffffl);
    memory.order (byteOrder);
    registerBank = getIsa().getRegisterBankBuffers(id.id);
  }
  
  /**
   * Create a new unattached Task.
   */
  LinuxTask (Proc proc, TaskId id) throws TaskException
  {
    super (proc, id);
    setupMapsXXX ();
  }
  
  /**
   * Create a new attached clone of Task.
   */
  LinuxTask (Task task, TaskId clone)
    throws TaskException
  {
    super (task, clone);
    setupMapsXXX ();
  }

  /**
   * Create a new attached main Task of Proc.
   */
  LinuxTask (Proc proc, TaskObserver.Attached attached)
    throws TaskException
  {
    super (proc, attached);
    setupMapsXXX ();
  }
  
  /**
   * Must inject disappeared events back into the event loop so that
   * they can be processed in sequence.  Calling
   * receiveDisappearedEvent directly would cause a recursive state
   * transition.
   */
  private void postDisappearedEvent (final Throwable arg)
  {
    logger.log (Level.FINE, "{0} postDisappearedEvent\n", this);
    Manager.eventLoop.add (new TaskEvent () 
      {
        Throwable w = arg;
        public void execute ()
        {
          processDisappearedEvent (w);
        }
      });
  }
  
  protected void sendContinue (int sig)
  {
    logger.log (Level.FINE, "{0} sendContinue\n", this); 
    try 
      {
	Ptrace.cont (getTid (), sig);
      }
    catch (Errno.Esrch e) 
      {
	postDisappearedEvent (e);
      }
  }

  protected void sendSyscallContinue(int sig)
  {
    logger.log(Level.FINE, "{0} sendSyscallContinue\n", this);
    try 
      {
        Ptrace.sysCall (getTid (), sig);
      }
    catch (Errno.Esrch e) 
      {
	postDisappearedEvent (e);
      }
  }
  protected void sendStepInstruction (int sig)
  {
    logger.log (Level.FINE, "{0} sendStepInstruction\n", this);
    try 
      {
        Ptrace.singleStep (getTid (), sig);
      }
    catch (Errno.Esrch e) 
      {
        postDisappearedEvent (e);
      }
  }
  protected void sendStop ()
  {
    logger.log (Level.FINE, "{0} sendStop\n", this); 
    Signal.tkill (id.hashCode (), Sig.STOP);
  }
  protected void sendSetOptions ()
  {
    logger.log (Level.FINE, "{0} sendSetOptions\n", this); 
    try 
      {
	// XXX: Should be selecting the trace flags based on the
	// contents of .observers.
	ptraceOptions |= Ptrace.optionTraceClone ();
	ptraceOptions |= Ptrace.optionTraceFork ();
	ptraceOptions |= Ptrace.optionTraceExit ();
	// ptraceOptions |= Ptrace.optionTraceSysgood (); not set by default
	ptraceOptions |= Ptrace.optionTraceExec ();
	Ptrace.setOptions (getTid (), ptraceOptions);
      }
    catch (Errno.Esrch e) 
      {
	postDisappearedEvent (e);
      }
  }
  protected void sendAttach ()
  {
    logger.log (Level.FINE, "{0} sendAttach\n", this);
    try 
      {
	Ptrace.attach (getTid ());
      }
    catch (Errno.Eperm e) 
      {
	logger.log (Level.FINE, "{" + e.toString ()
		    + "} Cannot attach to process\n");
      }
    catch (Errno.Esrch e) 
      {
	postDisappearedEvent (e);
      }
  }
  protected void sendDetach (int sig)
  {
    logger.log (Level.FINE, "{0} sendDetach\n", this);
    Ptrace.detach (getTid (), sig);
  }
    
  protected Isa sendrecIsa () throws TaskException
  {
    logger.log (Level.FINE, "{0} sendrecIsa\n", this);
    IsaFactory factory = IsaFactory.getFactory();
    
    return factory.getIsa(id.id);
  }

  protected void startTracingSyscalls()
  {
    logger.log(Level.FINE, "{0} startTracingSyscalls\n", this);
    ptraceOptions |= Ptrace.optionTraceSysgood ();
    this.sendSetOptions();
  }

  protected void stopTracingSyscalls()
  {
    logger.log(Level.FINE, "{0} stopTracingSyscalls\n", this);
    ptraceOptions &= ~(Ptrace.optionTraceSysgood ());
    this.sendSetOptions();
  }
}
