#!/bin/sh -eu
# This file is part of FRYSK.
#
# Copyright 2005, Red Hat Inc.
#
# FRYSK is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# FRYSK is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with FRYSK; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

if test $# -eq 0 ; then
    echo "Usage: $0 directory ..." 1>&2
    exit 1
fi
dirs="$*"

# Generate the list of source files

echo Creating Makefile.gen from directories ${dirs} ...

rm -f Makefile.gen

# Accumulate all the sources, each file type with its own category.

print_header ()
{
    cat <<EOF >> Makefile.gen

# Re-generate using "make autogen"
# $@
EOF
    echo "$@"
}

print ()
{
    echo "$@" >> Makefile.gen
}


print_header List of sub-directories.
print 'GEN_DIRS =' ${dirs}



for suffix in .mkjava .shjava ; do
    print_header "... ${suffix}"
    SUFFIX=`echo ${suffix} | tr '[a-z.]' '[A-Z_]'`
    print "GEN_BUILT${SUFFIX} ="
    find ${dirs} \
	-name "*${suffix}" -print \
	| sort -f | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	print "GEN_SOURCES += ${file}"
	print "GEN_BUILT_CLASSES += ${d}/${b}.classes"
	print "${d}/${b}.classes: ${d}/${b}.o"
	print "GEN_BUILT${SUFFIX} += ${d}/${b}.java"
    done
done



for suffix in .java ; do
    print_header "... ${suffix}"
    find ${dirs} \
	-name 'TestLib.java' -print -o \
	-name '*Test*' -prune -o \
	-name "*${suffix}" -print \
	| sort -f | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	test -r "${d}/${b}.mkjava" && continue
	test -r "${d}/${b}.shjava" && continue
	print "GEN_BUILT_CLASSES += ${d}/${b}.classes"
	print "GEN_SOURCES += ${file}"
    done
done



for suffix in .cxx ; do
    print_header "... ${suffix}"
    find ${dirs} \
	-name "*${suffix}" -print \
	| sort -f | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	print "GEN_SOURCES += ${file}"
    done
done


# Grep the cni/*.cxx files forming a list of any includes.  Assume
# these are all generated from .class files.  The list can be pruned a
# little since, given Class$Nested and Class, generating Class.h will
# automatically generate the inner Class$Nested class.

print_header "... GEN_BUILT_H  += *.cxx=.h"
print "GEN_BUILT_H = \\"
find ${dirs} -name 'cni' -print | while read d
do
    find $d -name '*.cxx' -print
done \
    | xargs grep '#include ".*.h"' \
    | sed -e 's/^.*#include "//' -e 's/.h".*$//' -e 's/$.*//' \
    | sort -u \
    | while read c
do
    test -r $c.java && print "	$c.h \\"
done
print '	$(ZZZ)'
print 'BUILT_SOURCES += $(GEN_BUILT_H)'
print 'CLEANFILES += $(GEN_BUILT_H)'



# Form a list of all the directories that contain JUnit tests (named
# *Test.java).  For each of those directories generate a
# TestJUnit.java file which will then run all of those tests using the
# standard TESTS+= mechanism.

rm -f TestJUnits.java
cat <<EOF >> TestJUnits.java
package TestJUnits;
import junit.framework.TestSuite;
import junit.framework.TestResult;
import junit.textui.TestRunner;
public class TestJUnits
{
    public static void main (String[] args)
    {
	try {
	    TestSuite testSuite = new TestSuite ();
EOF
find ${dirs} \
    -name 'TestLib.*' -prune -o \
    -name '*Test*.java' -print \
    | sort -f | while read test ; do
	grep ' main ' $test > /dev/null 2>&1 && continue
	print "GEN_SOURCES += ${test}"
	d=`dirname ${test}`
	b=`basename ${test} .java`
	class=`echo ${d}/${b} | tr '[/]' '[.]'`
cat <<EOF  >> TestJUnits.java
	    testSuite.addTest (new TestSuite (${class}.class));
EOF
done
cat <<EOF >> TestJUnits.java
	    TestResult testResult = TestRunner.run (testSuite);
	    if (!testResult.wasSuccessful()) 
		System.exit (TestRunner.FAILURE_EXIT);
	    System.exit(TestRunner.SUCCESS_EXIT);
	} catch(Exception e) {
	    System.err.println(e.getMessage());
	    System.exit (TestRunner.EXCEPTION_EXIT);
	}
    }
}
EOF
print "TestJUnits_SOURCES = TestJUnits.java"
print "TestJUnits_LINK = \${GCJLINK}"
print "TESTS += TestJUnits"
print "noinst_PROGRAMS += TestJUnits"



# Form a list of all the test cases that need to be built.  For any
# java file.  If there's a corresponding cni/*.cxx file add that in,
# ditto for a TestLib.* files.

print_header "... TESTS += Test*.java (standalone)"
find ${dirs} \
    -name 'TestLib.*' -prune -o \
    -name '*Test*.java' -print \
    | sort -f | while read file ; do
    grep ' main ' ${file} > /dev/null 2>&1 || continue
    d=`dirname ${file}`
    b=`basename ${file} .java`
    test=${d}/${b}
    test_=`echo ${test} | tr '[/]' '[_]'`
    print ""
    files=${file}
    dir=`dirname ${file}`
    cxx=${dir}/cni/`basename ${file} .java`.cxx
    test -r ${cxx} && files="${files} ${cxx}"
    print "${test_}_SOURCES = ${files}"
    print "${test_}_LINK = \${GCJLINK}"
    print "TESTS += ${test}"
    print "noinst_PROGRAMS += ${test}"
done
