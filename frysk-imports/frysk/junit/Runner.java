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

package frysk.junit;

import frysk.EventLogger;
import frysk.imports.Build;
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
    static Logger logger;
    
    // Reapeat onece by default.
    private int repeatValue = 1;
    private String archTarget = null;
    private String archBuild = null; 
    private Collection testCases = null;
    
    private Parser parser = null;
	    
    private String levelValue = null;
	
    private  Level level = null;
	    

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
   
    public void setBuildArch(String buildArch)
    {
      this.archBuild = buildArch;
    }
    public String getBuildArch()
    {
      return this.archBuild;
    }

    private int runCases(Collection testClasses)
    {
      // Create the testsuite to be run, either as specified on the
      // command line, or from the provided list of classes.  XXX:
      // It would be good if individual tests from within a testcase
      // could be identified and run.
     //String[] otherArgs = parser.getRemainingArgs ();
      
      TestSuite testSuite = new TestSuite ();
      
      if (otherArgs.size() > 0)
      {
	// Construct the testsuite from the list of names.
	Iterator iter = otherArgs.listIterator(0);
	      while (iter.hasNext())
          {
		  String arg = (String) iter.next();
            if (arg.charAt (0) == '-')
              this.repeatValue = -Integer.parseInt (arg);
            else
              testSuite.addTest (getTest (arg));
            }
          }
      else
      {
        for (Iterator i = testClasses.iterator (); i.hasNext (); )
          {
            Class testClass = (Class) i.next ();
            testSuite.addTest (new TestSuite (testClass));
          }
      }
  
      // Run the TestSuite <<repeat>> times.
      try
        {
          for (int i = 0; i < this.repeatValue; i++)
            {
              TestResult testResult = doRun (testSuite);
              
              if (!testResult.wasSuccessful()) 
                return FAILURE_EXIT;
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
      if ((null != this.archTarget) &&
          (false == this.archTarget.equals(Runner.ARCH64)))
        return SUCCESS_EXIT;
     
      boolean testArch64 = false;
      
      if (null == this.archBuild)
        this.archBuild = Build.BUILD_ARCH;

      // Check whether --arch=64 is given on 32-bit machine.
      if (archBuild.equalsIgnoreCase("x86_64") ||
          archBuild.equalsIgnoreCase("ppc64") ||
          archBuild.equalsIgnoreCase("powerpc64"))
      {
        testArch64 = true;
      }

      if ((null != this.archTarget) && 
          (false == testArch64))
      {
        System.out.println ("It's unsupported "+
                            "to do arch test in " + archBuild +". ");
        System.out.println ("Please try without --arch option! Exit...");
        System.exit (SUCCESS_EXIT);
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
      if (null == this.archTarget)
        return SUCCESS_EXIT;
      
      boolean testArch32 = false;

      if (null == this.archBuild)
        this.archBuild = Build.BUILD_ARCH;

      if (archBuild.equalsIgnoreCase("x86_64") || 
	  archBuild.equalsIgnoreCase("ppc64") || 
          archBuild.equalsIgnoreCase("powerpc64"))
      {
        testArch32 = true;
      }
	  
      if (false == testArch32)
      {
        System.out.println("It's unnecessary or unsupported "+ 
                           "to do arch test in " + archBuild +". Exit...");
        System.exit (SUCCESS_EXIT);
      }
      else if (false == this.archTarget.equals(Runner.ARCH32))
        return SUCCESS_EXIT;
      
      /**
       * Output some prompt message when we run both 64-bit and 32-bit cases.
       */
      if (null == this.archTarget)
      {
        System.out.println("+====================================================+");
        System.out.println("|                                                    |");
        System.out.println("|            The following is Biarch Test            |");
        System.out.println("|                                                    |");
        System.out.println("+====================================================+");
      }
      
      return this.runCases(testClasses);
    }
    
    private void addOptions(Parser parser) 
    {
	    parser.add(new Option("console", 'c', "Set the console level. The console-level can be "
		+ "[ OFF | SEVERE | WARNING | INFO | CONFIG | FINE | FINER | FINEST | ALL]", "<console-level>") {
			public void parsed (String consoleValue) throws OptionException
			{			
				 try
            {		
	      Level consoleLevel = Level.parse (consoleValue);	
		// Need to set both the console and the main logger as
		// otherwize the console won't see the log messages.		

	  
		System.out.println ("console " + consoleLevel);	   
		Handler consoleHandler = new ConsoleHandler ();
		consoleHandler.setLevel (consoleLevel);
		logger.addHandler (consoleHandler);
		logger.setLevel (consoleLevel);
		System.out.println (consoleHandler);		
	  		    
            }
	  catch (IllegalArgumentException e)
            {
              throw new OptionException("Invalid log console: " + consoleValue);
            }
	   
			}
		});
	    parser.add(new Option("level", 'l', "Set the log level. The log-level can be " +
			    "[ OFF | SEVERE | WARNING | INFO | CONFIG | FINE | FINER | FINEST | ALL]", "<log-level>") {
			public void parsed(String arg0) throws OptionException
			{
				levelValue = arg0;				
				try
				{
					level = Level.parse (levelValue);
				}
				catch (IllegalArgumentException e)
				{
					throw new OptionException ("Invalid log level: " + levelValue);
				}
			}
		});
	
		
		
		// Determine the number of times that the testsuite should be
	// run.		
		parser.add(new Option("repeat",  'r', "Set the count of repeating the test.", "<repeat-count>") {
			public void parsed (String arg0) throws OptionException
			{
				try
				{
				repeatValue = Integer.parseInt(arg0);
				}
				catch (NumberFormatException _) 
					{
						throw new OptionException("Argument: " + arg0 + " was not a number");
					}
					
				
			}
		});
			
		parser.add(new Option("arch",  "Set the target arch whose test " +
		"cases will be running. <ARCH> can be 64 or 32. If no any arch " + 
		"is set, the arch-64 cases will be run. All arch-64 and arch-32 " + 
		"cases will be run when arch-32 is ready. The --arch option will " + 
		"take no effect on 32-bit machines.", "<ARCH>") {
			public void parsed (String arg0) throws OptionException
			{
				if (arg0.equals(Runner.ARCH32) || arg0.equals(Runner.ARCH64))
					archTarget = arg0;
				else {
					throw new OptionException( "Invalid arch value: <" + arg0 + ">");
				}
			}
		});
    }
    
    /**
     * Create a JUnit TestRunner, using command-line arguments args,
     * and the supplied testClasses.
     */
    public Runner (String[] args)
    {
	// Override the print methods.
	super (new Results (System.out));

	// Create the command line parser, and use it to parse all
	// command line options.
	parser = new Parser ("Runner", "1.0", true)
	    {
		    protected void validate() throws OptionException {
			}
	    };
    
	    addOptions(parser);
	    
	    parser.setHeader("Usage: [ -c <console-level> ] [ -l <log-level> ]" +
			    " [ -r <repeat-count> ] [--arch <arch>] [ class ... ]");
	
	    otherArgs = new LinkedList();
	    
	logger = EventLogger.get ("logs/", "frysk_core_event.log");
	    parser.parse(args, new FileArgumentCallback() {
			public void notifyFile(String arg) throws OptionException
			{			
				otherArgs.add(arg);
				System.out.println(arg);
			}
		});
	

	
	  
	// Create the file logger, and then set it's level to that
	// specified on the command line.
	logger = EventLogger.get ("logs/", "frysk_core_event.log");
	if (levelValue != null)
	  logger.setLevel (level);

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

    public int runArchCases (String[] args,
			     Collection archTests,
			     Collection arch32Tests,
			     String execPrefix,
			     String exec32Prefix,
			     String dataPrefix)
    {
	Runner testRunner = new Runner (args);
	int result = SUCCESS_EXIT;

        testRunner.setBuildArch(Build.BUILD_ARCH);
	
	// Set the path prefixes and then do the common test.
	Paths.setPrefixes (execPrefix, dataPrefix);
	result = worstResult (testRunner.runArchCases (archTests), result);
	
	// Set the execPrefix of arch32 and then do the arch32 test.
	Paths.setPrefixes (exec32Prefix, dataPrefix);
	result = worstResult (testRunner.runArch32Cases (arch32Tests), result);

	return result;
    }

}
