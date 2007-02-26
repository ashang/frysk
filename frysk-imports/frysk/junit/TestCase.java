// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, Red Hat Inc.
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

package frysk.junit;

import frysk.Config;
import frysk.sys.Uname;

/**
 * <em>frysk</em> specific extension to the JUnit test framework's
 * TestCase.
 */

public class TestCase
    extends junit.framework.TestCase
{
    /**
     * Set the second-timeout.
     */
    static void setTimeoutSeconds (long timeoutSeconds)
    {
	TestCase.timeoutSeconds = timeoutSeconds;
    }
    static private long timeoutSeconds = 5;

    /**
     * A second timeout.
     */
    protected static long getTimeoutSeconds ()
    {
	return timeoutSeconds;
    }
    /**
     * A milli-second timeout.
     */
    protected static long getTimeoutMilliseconds ()
    {
	return timeoutSeconds * 1000;
    }

    /**
     * A variable that has the value true.  Used by code trying to
     * stop the optimizer realise that there's dead code around.
     */
    static boolean trueXXX = true;
    /**
     * A method that returns true, and prints skip.  Used by test
     * cases that want to be skipped (vis: if (broken()) return) while
     * trying to avoid the compiler's optimizer realizing that the
     * rest of the function is dead.
     */
    protected static boolean brokenXXX (int bug)
    {
	System.out.print ("<<BROKEN http://sourceware.org/bugzilla/show_bug.cgi?id=" + bug + " >>");
	return trueXXX;
    }

  /**
   * A method that returns true, and prints skip, when the build
   * architecture is PowerPC.
   */
  protected static boolean brokenPpcXXX (int bug)
  {
      if (Config.getTargetCpuXXX ().indexOf ("powerpc") != - 1) {
	return brokenXXX (bug);
    }
    return false;
  }

  /**
   * A method that returns true, and prints skip, when the build
   * architecture is X86_64.
   */
  protected static boolean brokenX8664XXX (int bug)
  {
      if (Config.getTargetCpuXXX ().indexOf ("_64") != - 1) {
	  return brokenXXX (bug);
      }
      return false;
  }

    /**
     * Results from uname(2) call.
     */
  private static Uname uname;
  private static KernelVersion version;
  /**
   * A method that returns true, and prints broken, when the running
   * kernel matches the supplied list.
   */
  protected static boolean brokenIfKernelXXX (int bug, String[] kernels)
  {
      if (uname == null)
	  uname = Uname.get ();
      for (int i = 0; i < kernels.length; i++) {
	  String kernel = kernels[i];
	  if (uname.getRelease ().startsWith (kernel))
	      return brokenXXX (bug);
      }
      return false;
  }

  protected static boolean brokenIfKernelXXX(int bug, KernelMatch matcher)
    {
	if (uname == null)
	    uname = Uname.get ();
	if (version == null)
	    version = new KernelVersion(uname.getRelease());
	if (matcher.matches(version))
	    return brokenXXX(bug);
	return false;
    }
    
    /**
     * A method that returns true, and prints broken, when the build
     * kernel includes UTRACE.
     */
    private static KernelVersion goodFC5217
	= new KernelVersion("2.6.17-1.2187.fc5");
    private static KernelVersion brokenFC5
	= new KernelVersion("2.6.18-1.2257.fc5");
    
    protected static boolean brokenIfUtraceXXX (int bug)
    {
	return brokenIfKernelXXX(bug, new KernelMatch()
	    {
		public boolean matches(KernelVersion version)
		{
		    if (version.isFedora()) {
			if (version.getFedoraRelease() == 5) {
			    if (version.newer(goodFC5217)
				&& !version.newer(brokenFC5))
				return true;
			    else
				return false;
			} else if (version.getFedoraRelease() == 6)
			    return true;
			return false;
		    }
		    else {
			return false;
		    }
		}
	    });
    }
}
