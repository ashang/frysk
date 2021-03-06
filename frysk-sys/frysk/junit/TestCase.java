// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008, Red Hat Inc.
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

import frysk.config.Prefix;
import frysk.config.Host;
import frysk.sys.Uname;
import java.math.BigInteger;

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
    static void setTimeoutSeconds (int timeoutSeconds) {
	TestCase.timeoutSeconds = timeoutSeconds;
    }
    static private int timeoutSeconds = 5;

    /**
     * A second timeout.
     */
    public static int getTimeoutSeconds () {
	return timeoutSeconds;
    }
    /**
     * A milli-second timeout.
     */
    public static long getTimeoutMilliseconds () {
	return timeoutSeconds * 1000;
    }

    /**
     * The test requires both 32-bit and 64-bit executables.
     *
     * This method returns true, and reports either UNSUPPORTED
     * (32-bit) or UNRESOLVED (64-bit), when a full 32-bit and 64-bit
     * testing environment is not available.
     */
    protected static boolean missing32or64() {
	String msg = "requires both 32-bit and 64-bit";
	switch (Host.wordSize()) {
	case 32:
	    return Runner.unsupported(msg, true);
	case 64:
	    return Runner.unresolved(msg,
				     Prefix.pkgLib32File(null) == null
				     || Prefix.pkgLib64File(null) == null);
	default:
	    throw new RuntimeException("unknown word-size: "
				       + Host.wordSize());
	}
    }

    /**
     * A method that returns true, and reports UNSUPPORTED.
     */
    protected static boolean unsupported(String reason, boolean notSupported) {
	return Runner.unsupported(reason, notSupported);
    }

    /**
     * The test has problems that have not been resolved on all
     * systems; see BUG for more details.  Return true and report
     * UNRESOLVED when called.
     */
    protected static boolean unresolved(int bug) {
	return Runner.unresolved(bug, true);
    }

    /**
     * The test has problems that have not been resolved on a PPC
     * system; see BUG for more details.  Return true and report
     * UNRESOLVED when running on a PowerPC.
     */
    protected static boolean unresolvedOnPPC (int bug) {
	return Runner.unresolved(bug, Host.cpuXXX().indexOf("powerpc") != - 1);
    }

    /**
     * The test has problems that have not been resolved on a X8664
     * system; see BUG for more details.  Return true and report
     * UNRESOLVED when running on a X8664.
     */
    protected static boolean unresolvedOnIA32 (int bug) {
	String cpu = Host.cpuXXX();
        return Runner.unresolved(bug, (cpu.indexOf ("i386") >= 0
				       || cpu.indexOf ("i686") >= 0));
    }

    /**
     * The test has problems that have not been resolved on a X8664
     * system; see BUG for more details.  Return true and report
     * UNRESOLVED when running on a X8664.
     */
    protected static boolean unresolvedOnx8664 (int bug) {
        return Runner.unresolved(bug, Host.cpuXXX().indexOf("x86_64") != - 1);
    }

    /**
     * The test has problems that have not been resolved on 64-bit
     * systems trying to control a 32-bit process; see BUG for more
     * details.  Returns true and reports UNRESOLVED when testing
     * 32-bit programs on a 64-bit system.
     */
    protected static boolean unresolvedOn32On64(int bug) {
	// 32-on-64 sets lib32 to NULL.
	return Runner.unresolved(bug,
				 Prefix.pkgLib32File(null) == null
				 && (Prefix.pkgLibFile(null)
				     != Prefix.pkgLib64File(null)));
    }

    /**
     * Results from uname(2) call.
     */
    private static Uname uname;
    private static KernelVersion version;
    private static boolean unresolvedOn(int bug, KernelMatch matcher) {
	if (uname == null) {
	    uname = Uname.get ();
	}
	if (version == null) {
	    version = new KernelVersion(uname.getRelease());
	}
	return Runner.unresolved(bug, matcher.matches(version));
    }
    
    /**
     * A method that returns true, and prints UNRESOLVED, when the
     * build kernel includes UTRACE.
     */
    protected static boolean unresolvedOnUtrace(int bug) {
	return unresolvedOn(bug, new KernelMatch() {
		public boolean matches(KernelVersion version) {
		    if (version.isFedora() && version.getFedoraRelease() > 5) {
			return true;
		    }
		    return false;
		}
	    });
    }
    
    /**
     * A method that returns true, and prints UNRESOLVED, when the
     * build kernel excludes utrace.
     */
    protected static boolean unresolvedOffUtrace(int bug) {
	return unresolvedOn(bug, new KernelMatch() {
		public boolean matches(KernelVersion version) {
		    if (!version.isFedora() 
			|| version.getFedoraRelease() <= 5) {
			return true;
		    }
		    return false;
		}
	    });
    }

    /**
     * The two byte arrays have identical contents.
     */
    public static void assertEquals(String what, byte[] correct,
				    byte[] test) {
	if (correct == null || test == null) {
	    assertEquals(what, (Object)correct, (Object)test);
	    return;
	}
	if (correct.length != test.length)
	    fail(what + ":"
		 + "expected byte[].length <" + correct.length + ">"
		 + " but was <" + test.length + ">");
	for (int i = 0; i < correct.length; i++) {
	    if (correct[i] != test[i])
		fail(what + ":"
		     + " expected byte[" + i + "] <"
		     + Integer.toHexString(correct[i] & 0xff)
		     + ">"
		     + " but was <"
		     + Integer.toHexString(test[i] & 0xff)
		     + ">");
	}
    }

    /**
     * The BigIntegers have identical values.
     */
    public static void assertEquals(String what, BigInteger correct,
				    BigInteger test) {
	if (correct == null || test == null) {
	    assertEquals(what, (Object)correct, (Object)test);
	    return;
	}
	if (!correct.equals(test)) {
	    if (correct.signum() < 0 || test.signum() < 0) {
		fail(what + ":"
		     + " expected <" + correct.toString() + ">"
		     + " but was <" + test.toString() + ">");
	    } else {
		fail(what + ":"
		     + " expected <0x" + correct.toString(16) + ">"
		     + " but was <0x" + test.toString(16) + ">");
	    }
	}
    }
    /**
     * The BigInteger is equal to the long value (when compared as
     * BigIntegers).
     */
    public static void assertEquals(String what, long correct,
				    BigInteger test) {
	assertEquals(what, BigInteger.valueOf(correct), test);
    }

    /**
     * The two String arrays are equal.
     */
    public static void assertEquals(String what, String[] correct,
				    String[] test) {
	if (correct == null || test == null) {
	    assertEquals(what, (Object)correct, (Object)test);
	    return;
	}
	assertEquals(what + " (String[].length)",
		     correct.length, test.length);
	for (int i = 0; i < correct.length; i++) {
	    if (correct[i] == null || test[i] == null) {
		assertEquals(what + " (String[" + i + "])",
			     (Object)(correct[i]), (Object)(test[i]));
	    } else {
		assertEquals(what + " (String[" + i + "])",
			     correct[i], test[i]);
	    }
	}
    }
    
    /**
     * Override JUnit's long == with one that prints failing values in hex.
     */
    public static void assertEquals(String what, long correct, long test) {
	if (correct != test) {
	    fail(what + " expected:<0x" + Long.toHexString(correct)
		 + "> but was:<0x" + Long.toHexString(test) + ">");
	}
    }
}
