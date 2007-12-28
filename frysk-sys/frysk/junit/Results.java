// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, Red Hat Inc.
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

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.textui.ResultPrinter;
import junit.framework.TestResult;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;

/**
 * Overide JUnit's print methods with a version that displays each
 * test as it is run.  Makes tracking down problems when the run does
 * a crash 'n' burn easier.
 */
class Results
    extends ResultPrinter
{
    private static final Logger logger = Logger.getLogger("frysk");

    private static Result result;
    private static final Set unresolved = new TreeSet();
    private static final Set resolved = new TreeSet();
    private static final Set unsupported = new TreeSet();

    public void startTest (Test test)
    {
	logger.log (Level.FINE, "{0} ---- startTest ----\n", test);
	System.out.print ("Running ");
	System.out.print (test);
	System.out.print (" ...");
	System.out.flush ();
	result = Result.PASS;
    }
    private void addProblem (Test test, String name, String what, Throwable t)
    {
	logger.log (Level.FINE, "{0} --- {1} ---- {2}: {3}\n",
		    new Object[] { test, name, what, t });
	// If a problem was previously recorded, move it to the
	// unresolved set.
	if (result != Result.PASS) {
	    // Convert to fail and add to unresolved.  At end will
	    // remove all unresolved from resolved.
	    Result.Problem problem = (Result.Problem)result;
	    unresolved.add(problem);
	    result = Result.fail(what, problem, t);
	}
	else
	    result = Result.fail(what, t);
    }
    public void addError (Test test, java.lang.Throwable t)
    {
	addProblem (test, "addError", "ERROR", t);
    }
    public void addFailure (Test test, AssertionFailedError t)
    {
	addProblem (test, "addFailure", "FAIL", t);
    }
    static void addUnresolved(String bug) {
	result = Result.unresolved(bug);
	unresolved.add (result);
    }
    static void addResolved(String bug) {
	result = Result.pass(bug);
	resolved.add(result);
    }
    static void addUnsupported (String reason)
    {
	result = Result.unsupported (reason);
	unsupported.add(result);
    }
    public void endTest (Test test)
    {
	logger.log (Level.FINE, "{0} ---- endTest ----\n", test);
	result.println();
    }

    private void printResolution (String what, Set set)
    {
	if (set.size() > 0) {
	    System.out.println ("There were "
				+ set.size()
				+ " "
				+ what
				+ ":");
	    for (Iterator i = set.iterator(); i.hasNext(); ) {
		Result.Problem r = (Result.Problem)i.next();
		System.out.print ("  ");
		System.out.println (r.getReason());
	    }
	}
    }

    protected void printHeader(long runTime)
    {
	super.printHeader (runTime);
	// Any tests that ended up in unresolved, can't have been
	// resolved.
	resolved.removeAll(unresolved);
	printResolution("unresolved", unresolved);
	printResolution("resolved", resolved);
	printResolution("unsupported", unsupported);
    }

    protected void printFooter(TestResult result)
    {
	super.printFooter (result);
    }
    // Constructor.
    Results (PrintStream stream)
    {
	super (stream);
    }
}
