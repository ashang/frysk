#!/bin/sh

cat <<EOF
import junit.framework.TestSuite;
import junit.framework.TestResult;
import junit.framework.AssertionFailedError;
import junit.textui.ResultPrinter;
import junit.framework.Test;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import frysk.Config;

public class TestRunner
    extends junit.textui.TestRunner
{
    TestRunner (ResultPrinter resultPrinter)
    {
	super (resultPrinter);
    }

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

    static Class[] testClasses = new Class[] {
EOF

for test in x "$@" ; do
    test ${test} = x && continue
    d=`dirname ${test}`
    b=`basename ${test} .java`
    class=`echo ${d}/${b} | tr '[/]' '[.]'`
    cat <<EOF
            ${class}.class,
EOF
done

cat <<EOF
	};
    public static void main (String[] args)
    {
	Logger logger = Config.EventLogger.get ("logs/", "frysk_core_event.log");
	Level level = logger.getLevel ();
	String level_name = "";
	String level_names [] = {"INFO", "CONFIG", "FINE", "FINER", "FINEST"};
	Level levels [] = {Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST};
	int first_test_arg = 0;
	TestRunner runner = new TestRunner (new Results (System.out));
        TestSuite testSuite = new TestSuite ();
        int repeats = 1;

	if (args.length > 0 && args[0].compareTo ("-level") == 0) {
	    int j;
	    if (args.length >= 2) 
		level_name = args[1];
	    first_test_arg = 2;
	    for (j = 0; j < level_names.length; j++)
		if (level_name.compareTo (level_names[j]) == 0) {
		    logger.setLevel (levels [j]);
		    break;
		}
	    if (j == level_names.length) {
		System.err.println ("Usage -level L, where L is one of:");
		for (int k = 0; k < level_names.length; k++)
		    System.err.println (level_names[k]);
		System.exit (FAILURE_EXIT);
	    }
	}

        if (args.length > first_test_arg) {
	    // Construct the testsuite from the list of names.
            for (int i = first_test_arg; i < args.length; i++) {
                if (args[i].charAt (0) == '-')
		    repeats = -Integer.parseInt (args[i]);
                else
                    testSuite.addTest (runner.getTest (args[i]));
            }
        }
        else {
	    for (int i = 0; i < testClasses.length; i++) {
                testSuite.addTest (new TestSuite (testClasses[i]));
            }
        }
        try {
	    for (int i = 0; i < repeats; i++) {
	        TestResult testResult = runner.doRun (testSuite);
	        if (!testResult.wasSuccessful()) 
		    System.exit (FAILURE_EXIT);
            }
	    System.exit(SUCCESS_EXIT);
	} catch(Exception e) {
	    System.err.println(e.getMessage());
	    System.exit (EXCEPTION_EXIT);
	}
	logger.setLevel (level);
    }
}
EOF
