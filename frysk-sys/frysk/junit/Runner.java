// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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

import frysk.config.Config;
import frysk.rsl.LogOption;
import frysk.expunit.Expect;
import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.PatternSyntaxException;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * <em>frysk</em> specific extension to the JUnit test framework.
 */

public class Runner extends TestRunner {
    // Repeat once by default.
    private int repeatValue = 1;
    private Collection testCases = null;
    private boolean listClassesOnly = false;
    // Put all tests through a filter; by default exclude all Stress.*
    // classes.
    private String testFilter = "^(|.*\\.)(?!Stress)[^\\.]*$";
    private ArrayList excludeTests = new ArrayList();
    private ArrayList includeTests = new ArrayList();
    
    private LinkedList otherArgs;
    
    public static void usage(String message, int exitVal) {
        System.out.println (message);                                       
        System.exit (exitVal);
    }

    public void setTestCases(Collection testCases) {
	this.testCases = testCases;
    }
    
    public Collection getTestCases() {
	return this.testCases;
    }
   
    private int runCases(Collection testClasses) {
	// Create the testsuite to be run, either as specified on the
	// command line, or from the provided list of classes.
      
	TestSuite testSuite = new TestSuite ();
      
	if (otherArgs.size() > 0) {
	    // Construct the testsuite from the list of names.
	    Iterator iter = otherArgs.listIterator(0);
	    while (iter.hasNext()) {
		String arg = (String) iter.next();
		if (arg.charAt(0) == '-') {
		    this.repeatValue = - Integer.parseInt(arg);
		} else {
		    int lidot = arg.lastIndexOf('.');
		    String testName = null;
		    String testCaseName = null;
		    if (arg.substring(lidot + 1).startsWith("test")) {
			testCaseName = arg.substring(0, lidot);
			testName = arg.substring(lidot + 1);
		    } else if (arg.matches("test.*\\(.*\\)")) {
			String[] testTuple = arg.split("[\\(\\)]");
			testName = testTuple[0];
			testCaseName = testTuple[1];
		    } else {
			testCaseName = arg;
		    }
		    
		    try {
			if (testName == null) {
			    testSuite.addTest(getTest(testCaseName));
			} else {
			    Class klass = loadSuiteClass(testCaseName);
			    TestCase test = (TestCase) klass.newInstance();
			    
			    //Check if the method exists.
			    klass.getMethod(testName, null);
			    
			    test.setName(testName);
			    testSuite.addTest(test);
			}
		    } catch (NoSuchMethodException e) {
			System.out.println("Couldn't find method with name: "
					   + testName);
		    } catch (ClassNotFoundException e) {
			System.out.println("Couldn't find class with name: "
					   + testCaseName);
		    } catch (InstantiationException e) {
			System.out.println("Couldn't instantiate class with name: "
					   + testCaseName);
		    } catch (IllegalAccessException e) {
			System.out.println("Couldn't access class with name: "
					   + testCaseName);
		    }
		}
	    }
	} else {
	    for (Iterator i = testClasses.iterator (); i.hasNext (); ) {
		Class testClass = (Class) i.next ();
		// Only include tests that gets by both filters.
		if (testClass.getName ().matches (testFilter)) {
		    boolean addit = true;
		    for (int j = 0; j < excludeTests.size(); j++) {
			try {
			    if (testClass.getName ()
				.matches ((String)excludeTests.get (j))) {
				addit = false;
				break;
			    }
			} catch (PatternSyntaxException p) {
			    System.out.println(p.getMessage());
			}
		    }
		    if (!addit) {
			for (int j = 0; j < includeTests.size(); j++) {
			    try {
				if (testClass.getName ()
				    .matches ((String)includeTests.get (j))) {
				    addit = true;
				    break;
				}
			    } catch (PatternSyntaxException p) {
				System.out.println(p.getMessage());
			    }
			}
		    }
		    if (addit) {
			testSuite.addTest (new TestSuite (testClass));
		    } else {
			System.out.println ("Omitting " + testClass.getName());
		    }
		}
	    }
	}
	
	if (listClassesOnly) {
	    for (Enumeration e = testSuite.tests (); e.hasMoreElements (); ) {
		Test test = (Test) e.nextElement ();
		System.out.println (test.toString ());
	    }
	    return SUCCESS_EXIT;
	}
	
	// Run the TestSuite <<repeat>> times.
	try {
	    for (int i = 0; i < this.repeatValue; i++) {
		TestResult testResult = doRun (testSuite);
		
		if (!testResult.wasSuccessful()) {
		    System.out.println("Failed after run #" + i);
		    return FAILURE_EXIT;
		}
	    }
	} catch(Exception e) {
	    System.err.println(e.getMessage());
	    return EXCEPTION_EXIT;
	}
	return SUCCESS_EXIT;      
    }
    
    /**
     * Create and return the command line parser used by frysk's JUnit
     * tests.
     */
    private Parser createCommandLineParser (String programName) {
	Parser parser = new Parser (programName, "1.0", true);
	
	parser.add(new LogOption("debug", 'c'));
	
	parser.add(new Option("unbreak", 'u', "Run broken tests") {
		public void parsed (String arg) throws OptionException {
		    skipUnresolvedTests = false;
		}
	    });
		
	// Determine the number of times that the testsuite should be
	// run.
	parser.add (new Option ("repeat",  'r',
				"Set the count of repeating the test.",
				"<repeat-count>") {
		public void parsed (String arg0) throws OptionException {
		    try {
			repeatValue = Integer.parseInt (arg0);
		    } catch (NumberFormatException e) {
			throw new OptionException ("Argument: " + arg0
						   + " was not a number");
		    }
		}
	    });
	
	parser.add (new Option ("arch",
				("On 64-bit systems,"
				 + " only use test programs with the"
				 + " specified word-size (32, 64, all)."
				 + " By default, both 32-bit and 64-bit"
				 + " test programs are used"),
				"<arch>") {
		public void parsed (String arg0) throws OptionException {
		    if (arg0.equals("32"))
			Config.set(config32);
		    else if (arg0.equals("64")) {
			if (Config.getWordSize() != 64)
			    throw new OptionException("-arch requires 64-bit");
			Config.set(config64);
		    } else if (arg0.equals("all"))
			Config.set(configAll);
		    else
			throw new OptionException( "Invalid arch value: "
						   + arg0);
		}
	    });
	
	parser.add (new Option ("list-classes-only", 'n',
				"Do not run any tests, instead list the"
				+ " classes that would have been tested") {
		public void parsed (String nullArgument)
		    throws OptionException
		{
		    listClassesOnly = true;
		}
	    });

	parser.add (new Option ("stress",
				"Run only stress tests "
				+ "(by default stress tests are excluded).") {
		public void parsed (String nullArgument)
		    throws OptionException
		{
		    testFilter = "^(|.*)Stress.*$";
		}
	    });
	
	parser.add (new Option ("all",
				"Run all tests "
				+ "(by default stress tests are excluded).") {
		public void parsed (String nullArgument)
		    throws OptionException
		{
		    testFilter = "^.*$";
		}
	    });
		
	// Specify tests to omit.
	parser.add (new Option ("exclude",  'e',
				"Specify a test to exclude.  Each passed"
				+ " option will be interpreted as the"
				+ " regex specification of a test to omit."
				+ "  This option may be used multiple"
				+ " times.",
				"<test-spec>") {
		public void parsed (String arg0) {
		    excludeTests.add (arg0);
		}
	    });

	// Specify tests to include, overriding omit.
	parser.add (new Option ("include",  'i',
				"Specify a test to include, ovirriding an"
				+ " omit specification.  Each passed"
				+ " option will be interpreted as the"
				+ " regex specification of a test to include."
				+ "  This option may be used multiple"
				+ " times.",
				"<test-spec>") {
		public void parsed (String arg0) {
		    includeTests.add (arg0);
		}
	    });
	
	parser.add(new Option ("timeout",
			       "Specify a timeout (in seconds) to use for " +
			       "assertRunUntilStop", "<timeout>") {
		public void parsed (String arg0) {
		    int timeout = Integer.parseInt(arg0);
		    TestCase.setTimeoutSeconds (timeout);
		    Expect.setDefaultTimeoutSeconds (timeout);
		}
            });
	
	parser.setHeader ("Usage:"
			  + " [ --console <LOG=LEVEL> ]"
			  + " [ --log <LOG=LEVEL> ]"
			  + " [ -r <repeat-count> ]"
			  + " [ --arch <arch>]"
			  + " [ -n ]"
			  + " [ --stress ]"
			  + " [ --all ]"
			  + " [-o spec...]"
			  + " [-i spec...]" 
			  + " [--timeout <timeout>]"
			  + " [--unbreak ]"
			  + " [ class ... ]");
	return parser;
    }
    
    private static String programBasename;
    /**
     * Possible configurations.
     */
    private final Config configAll;
    private final Config config32;
    private final Config config64;

    /**
     * Return the TestRunner's true basename - it could be "funit" or
     * it could be "TestRunner".
     *
     * XXX: Hack, shouldn't be using static storage for this.  Should
     * this go in frysk.Config?
     */
    public static String getProgramBasename ()
    {
	return programBasename;
    }

    /**
     * Create a JUnit TestRunner, using command-line arguments args,
     * and the supplied testClasses.
     */
    public Runner(String programBasename, String[] args,
		  Config configAll, Config config32, Config config64) {
	// Override the print methods.
	super (new Results (System.out));
	
	Config.set(configAll); // default
	this.configAll = configAll;
	this.config32 = config32;
	this.config64 = config64;

	// Tell expect the default timeout.
	Expect.setDefaultTimeoutSeconds (TestCase.getTimeoutSeconds ());

	// Create the command line parser, and use it to parse all
	// command line options.
	Runner.programBasename = programBasename;
	Parser parser = createCommandLineParser(programBasename);
	
	otherArgs = new LinkedList();
	
	parser.parse(args, new FileArgumentCallback() {
		public void notifyFile(String arg) throws OptionException {
		    otherArgs.add(arg);
		}
	    });
	
    }

    /**
     * Merge two TestRunner results returning the most fatal.
     */
    private int worstResult (int lhs, int rhs) {
	if (lhs == SUCCESS_EXIT) {
	    return rhs;
	}
	if (lhs == FAILURE_EXIT) {
	    if (rhs == SUCCESS_EXIT) {
		return FAILURE_EXIT;
	    } else {
		return rhs;
	    }
	}
	return EXCEPTION_EXIT;
    }

    /**
     * Run the testcases carried by testClasses.
     * 
     * @param testClasses
     * @return int the value of exit.
     */
    public int runTestCases (Collection tests) {
	int result = SUCCESS_EXIT;
	result = worstResult(runCases(tests), result);
	return result;
    }

    /**
     * Should the known-to-be-broken tests run?
     */
    private static boolean skipUnresolvedTests = true;

    /**
     * A method that returns true, and reports "UNRESOLVED".  Used by
     * test cases that want to be skipped (vis: if (broken()) return)
     * while trying to avoid the compiler's optimizer realizing that
     * the rest of the function is dead.
     */
    static boolean unresolved(int bug, boolean unresolved) {
	String msg = "http://sourceware.org/bugzilla/show_bug.cgi?id=" + bug;
	return unresolved(msg, unresolved);
    }

    /**
     * A method that returns true, and reports "UNRESOLVED".  Used by
     * test cases that want to be skipped (vis: if (broken()) return)
     * while trying to avoid the compiler's optimizer realizing that
     * the rest of the function is dead.
     */
    static boolean unresolved(String bug, boolean unresolved) {
	if (skipUnresolvedTests) {
	    if (unresolved) {
		Results.addUnresolved(bug);
	    }
	} else {
	    Results.addResolved(bug);
	}
	return skipUnresolvedTests && unresolved;
    }

    /**
     * An unsupported feature; can't test.
     */
    static boolean unsupported(String reason, boolean unsupported)
    {
	if (unsupported) {
	    Results.addUnsupported(reason);
	}
	// XXX: For moment do not enable unsupported tests when -u was
	// specified as it seems to cause cascading problems.
	return unsupported;
    }
}
