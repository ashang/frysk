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
    echo "Usage: $0 <directory> ... <path-to-jar> ..." 1>&2
    exit 1
fi
dirs=
jars=
while test $# -gt 0
do
  case "$1" in
      *.jar ) jars="${jars} $1" ;;
      * ) dirs="${dirs} $1" ;;
  esac
  shift
done
dirs=`echo ${dirs}`
jars=`echo ${jars}`

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

has_main ()
{
    case "$1" in
	*.java ) grep ' main[ ]*[(]' $1 > /dev/null 2>&1 ;;
        *.c|*.cxx ) grep -e '^main[( ]' -e ' main[( ]' $1 > /dev/null 2>&1 ;;
    esac
}


print_header Makefile.gen.in arguments
print GEN_DIRS = ${dirs}
print GEN_JARS = ${jars}


# Generate rules to compile any .jar files

for jar in x ${jars}
do
  test ${jar} = x && continue
  b=`basename ${jar} .jar`
  B=`echo $b | tr '[a-z]' '[A-Z]'`
  print ""
  print_header "... $jar"
  cat <<EOF >> Makefile.gen
${B}_JAR = /usr/share/java/${b}.jar
${b}.jar: \$(${B}_JAR)
	cp \$(${B}_JAR) .
noinst_LIBRARIES += lib${b}.a
lib${b}_a_LIBADD = ${b}.o
${b}.o: ${b}.jar
lib${b}_a_SOURCES = 
CLEANFILES += ${b}.jar ${b}.o lib${b}.a lib${b}.so
lib${b}.so: lib${b}.a
noinst_PROGRAMS += lib${b}.so ${b}.db
${b}.db: lib${b}.so ${b}.jar
EOF
done


# If there are no directories, bail here.
test x"${dirs}" = x && exit 0

print GEN_SOURCES =

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
	-name "*${suffix}" -print \
	| sort -f | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	name=${d}/${b}
	name_=`echo ${name} | tr '[/]' '[_]'`
	class=`echo ${name} | tr '[/]' '[.]'`
	test -r "${d}/${b}.mkjava" && continue
	test -r "${d}/${b}.shjava" && continue
	print "GEN_BUILT_CLASSES += ${d}/${b}.classes"
	print "GEN_SOURCES += ${file}"
	if has_main ${file} ; then
	    print "${name_}_SOURCES ="
	    print "${name_}_LINK = \$(GCJLINK)"
	    print "noinst_PROGRAMS += ${name}"
	    print "${name_}_LDFLAGS = --main=${class}"
	fi
    done
done



for suffix in .cxx .c ; do
    print_header "... ${suffix}"
    find ${dirs} \
	-name "*${suffix}" -print \
	| sort -f | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	name=${d}/${b}
	name_=`echo ${name} | tr '[/]' '[_]'`
	if has_main ${file} ; then
	    print "${name_}_SOURCES = ${file}"
	    test ${suffix} = .cxx && print "${name_}_LINK = \$(CXXLINK)"
	    print "noinst_PROGRAMS += ${name}"
	    if grep pthread.h ${file} > /dev/null 2>&1 ; then
		print "${name_}_LDADD = -lpthread"
	    fi
	else
	    print "GEN_SOURCES += ${file}"
	fi
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
# *Test*.java).  For each of those directories generate a
# TestJUnit.java file which will then run all of those tests using the
# standard TESTS+= mechanism.

rm -f TestJUnits.java
cat <<EOF >> TestJUnits.java
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
    has_main ${test} && continue
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
print "TestJUnits_LDFLAGS = --main=TestJUnits"
print "TESTS += TestJUnits"
print "noinst_PROGRAMS += TestJUnits"
print GEN_CLASSPATH += ../frysk-imports/junit.jar
print GEN_BUILT_CLASSES += TestJUnits.classes
print LDADD += ../frysk-imports/libjunit.a


# Form a list of all the stand-alone test cases that need to be run.

print_header "... TESTS += Test*.java"
find ${dirs} \
    -name '*Test*.java' -print \
    | sort -f | while read file ; do
    if has_main ${file} ; then
	d=`dirname ${file}`
	b=`basename ${file} .java`
	main=${d}/${b}
	main_=`echo ${main} | tr '[/]' '[_]'`
	print "TESTS += ${main}"
    fi
done
