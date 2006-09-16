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


package lib.unwind;

import gnu.gcj.RawDataManaged;
import java.util.Hashtable;

public class StackTraceCreator
{
  /**
   * Using the provided set of callbacks, initialize libunwind to provide a
   * full stack backtrace
   * @param callbacks The necessary callback functions to provide the data
   * that libunwind requires
   * @return A linked list of the stack frame
   */
  public static FrameCursor createStackTrace (UnwindCallbacks callbacks)
  {
    return unwind_setup(new UnwindArgs(callbacks));
  }

  private static native FrameCursor unwind_setup (UnwindArgs args);

  private static native void unwind_finish (UnwindArgs args);

  private static class UnwindArgs
  {
    public UnwindCallbacks CBarg;

    public RawDataManaged UPTarg;

    public RawDataManaged unwas;

    public UnwindArgs (UnwindCallbacks CBarg)
    {
      this.CBarg = CBarg;
      this.UPTarg = null;
      this.unwas = null;

      /* register_hashes must be called later, from within
	 unwind_setup().  Calling it within an if (false) avoids a
	 warning about the unused method.  */
      if (false)
	try {
	  register_hashes (this);
	} catch (UnwindException _) {
	}
    }

    public void finalize ()
    {
      /* unregister_hashes could be called here, but we call it in
	 unwind_finish(), to mirror the constructor above.  */
      if (false)
	unregister_hashes (this);
      unwind_finish(this);
    }
  }

    /* arg_hash maps long values corresponding to the addresses of an
     * UnwindArgs object and its CBarg and UPTarg fields, to the
     * UnwindArgs object itself.  This enables us to choose the right
     * argument to pass to callbacks even when one callback calls
     * another.  */
  private static Hashtable arg_hash = new Hashtable();
  private static native long pointer_to_long (Object obj);

  private static void register_hashes (UnwindArgs args)
    throws UnwindException {
    if (args.CBarg == null || args.UPTarg == null)
      throw new UnwindException ("Internal error in unwinder set up");
    arg_hash.put (new Long (pointer_to_long (args)), args);
    arg_hash.put (new Long (pointer_to_long (args.CBarg)), args);
    arg_hash.put (new Long (pointer_to_long (args.UPTarg)), args);
  }

  private static void unregister_hashes (UnwindArgs args) {
    arg_hash.remove (new Long (pointer_to_long (args.UPTarg)));
    arg_hash.remove (new Long (pointer_to_long (args.CBarg)));
    arg_hash.remove (new Long (pointer_to_long (args)));
  }

  public static UnwindArgs find_arg_from_long (long val,
					       RawDataManaged unwas)
  throws UnwindException {
    UnwindArgs arg = (UnwindArgs) arg_hash.get (new Long (val));
    if (arg.unwas != unwas)
      throw new UnwindException ("Internal error in unwinder use");
    return arg;
  }

  /**
   * This method is used in combination with dispatch_todo in order to
   * turn any exception into a -1 return value.  It's used in the
   * access_mem callback, that would otherwise throw an exception if
   * given an address not available in the target address space.

   * @param todo The argument to be passed to dispatch_todo.
   * @return The return value of dispatch_todo, unless it throws a
   * RuntimeException, in which case -1 is returned.  */
  public static int catch_errors (RawDataManaged todo) {
    try {
      return dispatch_todo (todo);
    } catch (RuntimeException whatever) {
      return -1;
    }
  }

  private static native int dispatch_todo (RawDataManaged todo);
}
