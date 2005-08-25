#!/bin/sh

cat <<EOF
import junit.framework.TestSuite;
import junit.framework.TestResult;

public class TestRunner
    extends junit.textui.TestRunner
{
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
        TestSuite testSuite = new TestSuite ();
        if (args.length > 0) {
	    // Construct the testsuite from the list of names.
            TestRunner runner = new TestRunner ();
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
	    TestResult testResult = run (testSuite);
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
