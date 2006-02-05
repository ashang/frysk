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

package frysk;

import frysk.EventLogger;
import jargs.gnu.CmdLineParser;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.ResultPrinter;
import junit.textui.TestRunner;

/**
 * <em>frysk</em> specific extension to the JUnit test framework.
 */

public class JUnitRunner
    extends TestRunner
{
    /**
     * Overide JUnit's print methods with a version that displays each
     * test as it is run.  Makes tracking down problems when the run
     * does a crash 'n' burn.
     */
    public static class Results
	extends ResultPrinter
    {
	// Does this have a race condition?
	boolean pass = false;
	public void startTest (Test test)
	{
	    System.out.print ("Running ");
	    System.out.print (test);
	    System.out.print (" ...");
	    System.out.flush ();
	    pass = true;
	}
	public void endTest (Test test)
	{
	    if (pass)
		System.out.println ("PASS");
	    else
		System.out.println ();
	}
	private void printProblem (String what, Throwable t)
        {
	    System.out.println (what);
	    System.out.print ("  ");
	    System.out.print (t);
        }

	public void addError (Test test, java.lang.Throwable t)
	{
	    printProblem ("ERROR", t);
	    pass = false;
	}
	public void addFailure (Test test, AssertionFailedError t)
	{
	    printProblem ("FAIL", t);
	    pass = false;
	}
	// Constructor.
	Results (PrintStream stream)
	{
	    super (stream);
	}
    }

    /**
     * Create a JUnit TestRunner, using command-line arguments args,
     * and the supplied testClasses.
     */
    public JUnitRunner (String[] args, Collection testClasses)
    {
	// Override the print methods.
	super (new Results (System.out));

	// Create the command line parser, and use it to parse all
	// command line options.
	CmdLineParser parser = new CmdLineParser ();
	CmdLineParser.Option levelOption
	    = parser.addStringOption ('l', "level");
	CmdLineParser.Option consoleOption
	    = parser.addStringOption ('c', "console");
	CmdLineParser.Option repeatOption
	    = parser.addIntegerOption ('r', "repeat");
	try {
	    parser.parse(args);
	}
	catch (CmdLineParser.OptionException e) {
	    System.out.println (e.getMessage());
	    System.out.println ("Usage: [ -c <console-level> ] [ -l <log-level> ] [ -r <repeat-count> ] [ class ... ]");
	    System.exit (FAILURE_EXIT);
	}

	// Create the file logger, and then set it's level to that
	// specified on the command line.
	Logger logger = EventLogger.get ("logs/", "frysk_core_event.log");
	String levelValue = (String) parser.getOptionValue (levelOption);
	if (levelValue != null) {
	    Level level = null;
	    try {
		level = Level.parse (levelValue);
	    }
	    catch (IllegalArgumentException e) {
		System.out.println ("Invalid log level: " + levelValue);
		System.exit (FAILURE_EXIT);
	    }
	    logger.setLevel (level);
	}

	// Need to set both the console and the main logger as
	// otherwize the console won't see the log messages.
	String consoleValue = (String) parser.getOptionValue (consoleOption);
	if (consoleValue != null) {
	    Level consoleLevel = null;
	    try {
		consoleLevel = Level.parse (consoleValue);
	    }
	    catch (IllegalArgumentException e) {
		System.out.println ("Invalid log console: " + consoleValue);
		System.exit (FAILURE_EXIT);
	    }
	    System.out.println ("console " + consoleLevel);
	    Handler consoleHandler = new ConsoleHandler ();
	    consoleHandler.setLevel (consoleLevel);
	    logger.addHandler (consoleHandler);
	    logger.setLevel (consoleLevel);
	    System.out.println (consoleHandler);
	}

	// Determine the number of times that the testsuite should be
	// run.
	Integer repeatValue = (Integer) parser.getOptionValue (repeatOption);
        int repeats = 1;
	if (repeatValue != null) {
	    repeats = repeatValue.intValue ();
	}

	// Create the testsuite to be run, either as specified on the
	// command line, or from the provided list of classes.  XXX:
	// It would be good if individual tests from within a testcase
	// could be identified and run.
	String[] otherArgs = parser.getRemainingArgs ();
        TestSuite testSuite = new TestSuite ();
        if (otherArgs.length > 0) {
	    // Construct the testsuite from the list of names.
            for (int i = 0; i < otherArgs.length; i++) {
                if (otherArgs[i].charAt (0) == '-')
		    repeats = -Integer.parseInt (otherArgs[i]);
                else
                    testSuite.addTest (getTest (otherArgs[i]));
            }
        }
        else {
	    for (Iterator i = testClasses.iterator (); i.hasNext (); ) {
		Class testClass = (Class) i.next ();
                testSuite.addTest (new TestSuite (testClass));
	    }

        }

	// Run the TestSuite <<repeat>> times.
        try {
	    for (int i = 0; i < repeats; i++) {
	        TestResult testResult = doRun (testSuite);
	        if (!testResult.wasSuccessful()) 
		    System.exit (FAILURE_EXIT);
            }
	} catch(Exception e) {
	    System.err.println(e.getMessage());
	    System.exit (EXCEPTION_EXIT);
	}

	System.exit (SUCCESS_EXIT);
    }
}
