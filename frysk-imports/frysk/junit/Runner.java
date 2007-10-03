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

import frysk.EventLogger;
import frysk.Config;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.PatternSyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.Enumeration;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.ResultPrinter;
import junit.textui.TestRunner;
import frysk.expunit.Expect;

import gnu.classpath.tools.getopt.FileArgumentCallback;
import gnu.classpath.tools.getopt.Option;
import gnu.classpath.tools.getopt.OptionException;
import gnu.classpath.tools.getopt.Parser;

import java.util.LinkedList;

/**
 * <em>frysk</em> specific extension to the JUnit test framework.
 */

public class Runner
    extends TestRunner
{
    static Logger logger = Logger.getLogger("frysk");
    
    // Repeat once by default.
    private int repeatValue = 1;
    private String archTarget = null;
    private Collection testCases = null;
    private boolean listClassesOnly = false;
    // Put all tests through a filter; by default exclude all Stress.*
    // classes.
    private String testFilter = "^(|.*\\.)(?!Stress)[^\\.]*$";
    private ArrayList excludeTests = new ArrayList();
    private ArrayList includeTests = new ArrayList();
    
    private LinkedList otherArgs;
    
    public final static String ARCH64 = "64";
    public final static String ARCH32 = "32";
    
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
	    logger.log (Level.FINE, "{0} ---- startTest ----\n", test);
	    System.out.print ("Running ");
	    System.out.print (test);
	    System.out.print (" ...");
	    System.out.flush ();
	    pass = true;
	}
	public void endTest (Test test)
	{
	    logger.log (Level.FINE, "{0} ---- endTest ----\n", test);
	    if (pass)
		System.out.println ("PASS");
	    else
		System.out.println ();
	}
	private void printProblem (Test test, String name, String what,
				   Throwable t)
        {
	    logger.log (Level.FINE, "{0} --- {1} ---- {2}: {3}\n",
			new Object[] { test, name, what, t });
	    System.out.println (what);
	    System.out.print ("  ");
	    System.out.print (t);
	    pass = false;
        }

	public void addError (Test test, java.lang.Throwable t)
	{
	    printProblem (test, "addError", "ERROR", t);
	}
	public void addFailure (Test test, AssertionFailedError t)
	{
	    printProblem (test, "addFailure", "FAIL", t);
	}
	// Constructor.
	Results (PrintStream stream)
	{
	    super (stream);
	}
    }
    
    public static void usage(String message, int exitVal)
    {
        System.out.println (message);                                       
        System.exit (exitVal);
    }

    public void setTestCases(Collection testCases)
    {
	this.testCases = testCases;
    }
    
    public Collection getTestCases()
    {
	return this.testCases;
    }
   
    private int runCases (Collection testClasses)
    {
	// Create the testsuite to be run, either as specified on the
	// command line, or from the provided list of classes.  
      
	TestSuite testSuite = new TestSuite ();
      
	if (otherArgs.size() > 0)
      {
        // Construct the testsuite from the list of names.
        Iterator iter = otherArgs.listIterator(0);
        while (iter.hasNext())
          {
            String arg = (String) iter.next();
            if (arg.charAt(0) == '-')
              this.repeatValue = - Integer.parseInt(arg);
            else
              {
                int lidot = arg.lastIndexOf('.');
                String testName = null;
                String testCaseName = null;
                if (arg.substring(lidot + 1).startsWith("test"))
                  {
                    testCaseName = arg.substring(0, lidot);
                    testName = arg.substring(lidot + 1);
                  }
                else if (arg.matches("test.*\\(.*\\)"))
                  {
                    String[] testTuple = arg.split("[\\(\\)]");
                    testName = testTuple[0];
                    testCaseName = testTuple[1];
                  }
                else
                  {
                    testCaseName = arg;
                  }

                try
                  {
                    
                    if (testName == null)
                      testSuite.addTest(getTest(testCaseName));
                    else
                      {
                        Class klass = loadSuiteClass(testCaseName);
                        TestCase test = (TestCase) klass.newInstance();
                        
                        //Check if the method exists.
                        klass.getMethod(testName, null);
                        
                        test.setName(testName);
                        testSuite.addTest(test);
                      }
                  }
                catch (NoSuchMethodException e)
                {
                  System.out.println("Couldn't find method with name: "
                                     + testName);
                }
                catch (ClassNotFoundException e)
                  {
                    System.out.println("Couldn't find class with name: "
                                       + testCaseName);
                  }
                catch (InstantiationException e)
                  {
                    System.out.println("Couldn't instantiate class with name: "
                                       + testCaseName);
                  }
                catch (IllegalAccessException e)
                  {
                    System.out.println("Couldn't access class with name: "
                                       + testCaseName);
                  }

              }
          }
      }
	else {
	    for (Iterator i = testClasses.iterator (); i.hasNext (); ) {
		Class testClass = (Class) i.next ();
		// Only include tests that gets by both filters.
		if (testClass.getName ().matches (testFilter)) {
		    boolean addit = true;
		    for (int j = 0; j < excludeTests.size(); j++) {
			try {
			    if (testClass.getName ().matches ((String)excludeTests.get (j))) {
				addit = false;
				break;
			    }
			}
			catch (PatternSyntaxException p) {
			    System.out.println(p.getMessage());
			}
		    }
		    if (!addit) {
			for (int j = 0; j < includeTests.size(); j++) {
			    try {
				if (testClass.getName ().matches ((String)includeTests.get (j))) {
				    addit = true;
				    break;
				}
			    }
			    catch (PatternSyntaxException p) {
				System.out.println(p.getMessage());
			    }
			}
		    }
		    if (addit) testSuite.addTest (new TestSuite (testClass));
		    else System.out.println ("Omitting " + testClass.getName());
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
	try
	    {
		for (int i = 0; i < this.repeatValue; i++)
		    {
			TestResult testResult = doRun (testSuite);
              
			if (!testResult.wasSuccessful()) 
              {
                System.out.println("Failed after run #" + i);
			    return FAILURE_EXIT;
              }
		    }
	    }
	catch(Exception e)
	    {
		System.err.println(e.getMessage());
          
		return EXCEPTION_EXIT;
	    }
      
	return SUCCESS_EXIT;      
    }
    
    /**
     * Run the testcases carried by testClasses.
     * 
     * @param testClasses
     * @return int the value of exit.
     */
    public int runArchCases (Collection testClasses)
    {
	// Check whether we should continue.
	if (this.archTarget != null && !this.archTarget.equals(Runner.ARCH64))
	    return SUCCESS_EXIT;
     
	boolean testArch64 = Config.getWordSize () == 64;
      
	if (this.archTarget != null && !testArch64)
	    {
		System.out.println ("Arch test is only supported on 64-bit"
				    + " systems.");
		System.out.println ("Please try without --arch option! Exit...");
		System.exit (FAILURE_EXIT);
	    }
 
	return this.runCases(testClasses);
    }
    
    /**
     * Run bi-arch test when the "-i" or "-b" option is given.
     * When doing bi-arch test, all cases are the same as the 
     * common test except for the execPrefix path. So after the 
     * correct execPrefix is set before calling this function
     * in frysk.junit.Paths, we do the same procedures as the doRunner().
     * 
     * @param testClasses
     * @return
     * @see frysk.junit.Paths
     */
    public int runArch32Cases(Collection testClasses)
    {
	//XXX: if all 32-bit cases pass, we should comment 
	//the following instruction.
	if (this.archTarget == null)
	    return SUCCESS_EXIT;
      
	boolean testArch32 = Config.getWordSize () == 64;
	  
	if (!testArch32)
	    {
		System.out.println("It's unnecessary or unsupported"
				   + " to do arch test on "
				   + Config.getWordSize ()
				   + " system.");
		System.exit (FAILURE_EXIT);
	    }
	else if (!this.archTarget.equals(Runner.ARCH32))
	    return SUCCESS_EXIT;
      
	/**
	 * Output some prompt message when we run both 64-bit and 32-bit cases.
	 */
	if (this.archTarget == null)
	    {
		System.out.println("+====================================================+");
		System.out.println("|                                                    |");
		System.out.println("|            The following is Biarch Test            |");
		System.out.println("|                                                    |");
		System.out.println("+====================================================+");
	    }
      
	return this.runCases(testClasses);
    }
    
    /**
     * Create and return the command line parser used by frysk's JUnit
     * tests.
     */
    private Parser createCommandLineParser (String programName)
    {
	Parser parser = new Parser (programName, "1.0", true);

    EventLogger.addConsoleOptions(parser);
    
    parser.add(new Option('c', "Shortcut for --console frysk=LEVEL.", "<LEVEL>")
    {
      public void parsed (String arg0) throws OptionException
      {
        Logger logger = LogManager.getLogManager().getLogger("frysk");

        try
          {
            Level consoleLevel = Level.parse(arg0);
            EventLogger.setConsoleLog(logger, consoleLevel);

          }
        catch (IllegalArgumentException e)
          {
            throw new OptionException("Invalid log console: " + arg0);
          }
      }

    });
		
    parser.add(new Option('l', "Shortcut for --log frysk=LEVEL", "<LEVEL>")
    {
      public void parsed (String arg0) throws OptionException
      {
        Logger logger = LogManager.getLogManager().getLogger("frysk");

         try
         {
           Level level = Level.parse(arg0);
           logger.setLevel(level);
         }
         catch (IllegalArgumentException e)
         {
           throw new OptionException ("Invalid log console: " + arg0);
         }
      }
    });
	// Determine the number of times that the testsuite should be
	// run.
	parser.add (new Option ("repeat",  'r',
				"Set the count of repeating the test.",
				"<repeat-count>")
	    {
		public void parsed (String arg0)
		    throws OptionException
		{
		    try {
			repeatValue = Integer.parseInt (arg0);
		    }
		    catch (NumberFormatException e) {
			throw new OptionException ("Argument: " + arg0
						   + " was not a number");
		    }
		}
	    });
	
	parser.add (new Option ("arch",
				"Set the target arch whose test cases"
				+ " will be running. <ARCH> can be 64 or 32. "
				+ " If no any arch is set, the arch-64"
				+ " cases will be run. All arch-64 and"
				+ " arch-32 cases will be run when arch-32"
				+ " is ready.  The --arch option will take"
				+ " no effect on 32-bit machines.",
				"<arch>")
	    {
		public void parsed (String arg0)
		    throws OptionException
		{
		    if (arg0.equals(Runner.ARCH32)
			|| arg0.equals(Runner.ARCH64))
			archTarget = arg0;
		    else {
			throw new OptionException( "Invalid arch value: "
						   + arg0);
		    }
		}
	    });

	parser.add (new Option ("list-classes-only", 'n',
				"Do not run any tests, instead list the"
				+ " classes that would have been tested")
	    {
		public void parsed (String nullArgument)
		    throws OptionException
		{
		    listClassesOnly = true;
		}
	    });

	parser.add (new Option ("stress",
				"Run only stress tests "
				+ "(by default stress tests are excluded).")
	    {
		public void parsed (String nullArgument)
		    throws OptionException
		{
		    testFilter = "^(|.*)Stress.*$";
		}
	    });

	parser.add (new Option ("all",
				"Run all tests "
				+ "(by default stress tests are excluded).")
	    {
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
				"<test-spec>")
	    {
		public void parsed (String arg0)
		{
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
				"<test-spec>")
	    {
		public void parsed (String arg0)
		{
		    includeTests.add (arg0);
		}
	    });
    
    parser.add(new Option ("timeout",
			   "Specify a timeout (in seconds) to use for " +
            "assertRunUntilStop", "<timeout>")
            {
              public void parsed (String arg0)
              {
                timeout = Integer.parseInt(arg0);
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
			  + " [ class ... ]");
	return parser;
    }
    
    private static int timeout = 5; 
    public static int getTimeout()
    {
      return timeout;
    }

    private static String programBasename;
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
    public Runner (String programName, String[] args)
    {
	// Override the print methods.
	super (new Results (System.out));

	// Create the command line parser, and use it to parse all
	// command line options.
	Parser parser = createCommandLineParser (programName);
	programBasename = programName;
	
	otherArgs = new LinkedList();
	    
	parser.parse(args, new FileArgumentCallback()
	    {
		public void notifyFile(String arg) throws OptionException
		{			
		    otherArgs.add(arg);
		}
	    });	  

    }

    /**
     * Merge two TestRunner results returning the most fatal.
     */
    private int worstResult (int lhs, int rhs)
    {
	if (lhs == SUCCESS_EXIT)
	    return rhs;
	else if (lhs == FAILURE_EXIT) {
	    if (rhs == SUCCESS_EXIT)
		return FAILURE_EXIT;
	    else
		return rhs;
	}
	else
	    return EXCEPTION_EXIT;
    }

    public int runTestCases (Collection tests, Config config,
			     Collection tests32, Config config32)
    {
	int result = SUCCESS_EXIT;

	// Set the path prefixes and then do the common test.
	Config.set (config);
	result = worstResult (runArchCases (tests), result);
	
	// Set the Config to 32-on-64 and then re-run the tests.
	Config.set (config32);
	result = worstResult (runArch32Cases (tests32), result);

	return result;
    }

}
