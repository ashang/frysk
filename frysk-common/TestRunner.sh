#!/bin/sh

cat <<EOF
import junit.framework.TestSuite;
import junit.framework.TestResult;
import junit.framework.AssertionFailedError;
import junit.textui.ResultPrinter;
import junit.framework.Test;
import java.io.PrintStream;

public class TestRunner
    extends junit.textui.TestRunner
{
    TestRunner (ResultPrinter resultPrinter)
    {
	super (resultPrinter);
    }


    static class Results
	extends ResultPrinter
    {
	public void startTest (Test test)
	{
	    System.out.print ("Running ");
	    System.out.print (test);
	    System.out.print (" ...");
	    System.out.flush ();
	}
	public void endTest (Test test)
	{
	    System.out.println ();
	}
	public void addError (Test test, java.lang.Throwable t)
	{
	    System.out.print (" ERROR: ");
	    t.printStackTrace ();
	}
	public void addFailure (Test test, AssertionFailedError t)
	{
	    System.out.print (" FAIL: ");
	    t.printStackTrace ();
	}
	protected void printDefects(java.util.Enumeration booBoos,
				    int count, java.lang.String type)
	{
	    // Do nothing.
	}
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
	TestRunner runner = new TestRunner (new Results (System.out));
        TestSuite testSuite = new TestSuite ();
        if (args.length > 0) {
	    // Construct the testsuite from the list of names.
            for (int i = 0; i < args.length; i++) {
                testSuite.addTest (runner.getTest (args[i]));
            }
        }
        else {
	    for (int i = 0; i < testClasses.length; i++) {
                testSuite.addTest (new TestSuite (testClasses[i]));
            }
        }
EOF

cat <<EOF
        try {
	    TestResult testResult = runner.doRun (testSuite);
	    if (!testResult.wasSuccessful()) 
		System.exit (FAILURE_EXIT);
	    System.exit(SUCCESS_EXIT);
	} catch(Exception e) {
	    System.err.println(e.getMessage());
	    System.exit (EXCEPTION_EXIT);
	}
    }
}
EOF
