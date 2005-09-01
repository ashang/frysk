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
    echo "Usage: $0 <directory> ... <path-to-jni-jar> ..." 1>&2
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
exec > Makefile.gen



# Accumulate all the sources, each file type with its own category.

print_header ()
{
    cat <<EOF

# Re-generate using "make autogen"
# $@
EOF
    echo "$@" 1>&2
}

has_main ()
{
    case "$1" in
	*.java ) grep ' main[ ]*[(]' $1 > /dev/null 2>&1 ;;
        *.c|*.cxx ) grep -e '^main[( ]' -e ' main[( ]' $1 > /dev/null 2>&1 ;;
    esac
}


print_header Makefile.gen.in arguments: ${dirs} ${jars}
echo GEN_DIRS = ${dirs}
echo GEN_JARS = ${jars}


# Generate rules to compile any .jar files

for jar in x ${jars}
do
  test ${jar} = x && continue
  b=`basename ${jar} .jar`
  B=`echo $b | tr '[a-z]' '[A-Z]'`
  echo ""
  print_header "... $jar"
  cat <<EOF
${B}_JAR = ${jar}
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


echo GEN_SOURCES =
 

# Generate rule to build this directory's .jar, .a, and .so file.
pwd=`pwd`
dir=`basename $pwd`
_dir=`echo ${dir} | sed -e 's,[-/],_,g'`
print_header "... creating rule for ${dir}.db et.al."
cat <<EOF
${dir}.jar: \$(GEN_BUILT_CLASSES)
	cat \$^ /dev/null \
		| sed -e '/^#/d' -e 's,\.,/,g' -e 's,\$\$,.class,' \
		| ( cd \$(GEN_CLASSDIR) && fastjar -@ -cf \$@ )
	mv \$(GEN_CLASSDIR)/\$@ \$@
noinst_PROGRAMS += ${dir}.jar
LDADD += lib${dir}.a
lib${_dir}_a_SOURCES = \$(GEN_SOURCES)
noinst_LIBRARIES += lib${dir}.a
lib${dir}.so: lib${dir}.a
noinst_PROGRAMS += lib${dir}.so
${dir}.db: lib${dir}.so ${dir}.jar
	gcj-dbtool -n \$@.tmp
	gcj-dbtool -a \$@.tmp ${dir}.jar lib${dir}.so
	mv \$@.tmp \$@
noinst_PROGRAMS += ${dir}.db
EOF






for suffix in .mkjava .shjava ; do
    print_header "... ${suffix}"
    SUFFIX=`echo ${suffix} | tr '[a-z.]' '[A-Z_]'`
    echo "GEN_BUILT${SUFFIX} ="
    find ${dirs} \
	-name "*${suffix}" -print \
	| sort -f | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	echo "GEN_SOURCES += ${file}"
	echo "GEN_BUILT_CLASSES += ${d}/${b}.classes"
	echo "${d}/${b}.classes: ${d}/${b}.o"
	echo "GEN_BUILT${SUFFIX} += ${d}/${b}.java"
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
	echo "GEN_BUILT_CLASSES += ${d}/${b}.classes"
	echo "GEN_SOURCES += ${file}"
	if has_main ${file} ; then
	    echo "${name_}_SOURCES ="
	    echo "${name_}_LINK = \$(GCJLINK)"
	    echo "noinst_PROGRAMS += ${name}"
	    echo "${name_}_LDFLAGS = --main=${class}"
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
	    echo "${name_}_SOURCES = ${file}"
	    test ${suffix} = .cxx && echo "${name_}_LINK = \$(CXXLINK)"
	    echo "noinst_PROGRAMS += ${name}"
	    if grep pthread.h ${file} > /dev/null 2>&1 ; then
		echo "${name_}_LDADD = -lpthread"
	    fi
	else
	    echo "GEN_SOURCES += ${file}"
	fi
    done
done


# Grep the cni/*.cxx files forming a list of any includes.  Assume
# these are all generated from .class files.  The list can be pruned a
# little since, given Class$Nested and Class, generating Class.h will
# automatically generate the inner Class$Nested class.

print_header "... GEN_BUILT_H  += *.cxx=.h"
echo "GEN_BUILT_H = \\"
find ${dirs} -name 'cni' -print | while read d
do
    find $d -name '*.cxx' -print
done \
    | xargs grep '#include ".*.h"' \
    | sed -e 's/^.*#include "//' -e 's/.h".*$//' -e 's/$.*//' \
    | sort -u \
    | while read c
do
    test -r $c.java && echo "	$c.h \\"
done
echo '	$(ZZZ)'
echo 'BUILT_SOURCES += $(GEN_BUILT_H)'



# Form a list of all the JUnit tests.  Anything named *Test*, that
# does not contain a main method is considered a candidate for the
# list.

print_header "... GEN_JUNIT_TESTS += *.java"
echo GEN_JUNIT_TESTS =
find ${dirs} \
    -name 'TestLib.*' -prune -o \
    -name '*Test*.java' -print \
    | sort -f | while read test ; do
    has_main ${test} && continue
    echo GEN_JUNIT_TESTS += ${test}
done


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
	echo "TESTS += ${main}"
    fi
done
