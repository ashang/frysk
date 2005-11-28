#!/bin/sh -eu
# This file is part of the program FRYSK.
#
# Copyright 2005, Red Hat Inc.
#
# FRYSK is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 2 of the License.
#
# FRYSK is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with FRYSK; if not, write to the Free Software Foundation,
# Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
# 
# In addition, as a special exception, Red Hat, Inc. gives You the
# additional right to link the code of FRYSK with code not covered
# under the GNU General Public License ("Non-GPL Code") and to
# distribute linked combinations including the two, subject to the
# limitations in this paragraph. Non-GPL Code permitted under this
# exception must only link to the code of FRYSK through those well
# defined interfaces identified in the file named EXCEPTION found in
# the source code files (the "Approved Interfaces"). The files of
# Non-GPL Code may instantiate templates or use macros or inline
# functions from the Approved Interfaces without causing the
# resulting work to be covered by the GNU General Public
# License. Only Red Hat, Inc. may make changes or additions to the
# list of Approved Interfaces. You must obey the GNU General Public
# License in all respects for all of the FRYSK code and other code
# used in conjunction with FRYSK except the Non-GPL Code covered by
# this exception. If you modify this file, you may extend this
# exception to your version of the file, but you are not obligated to
# do so. If you do not wish to provide this exception without
# modification, you must delete this exception statement from your
# version and license this file solely under the GPL without
# exception.

if test $# -eq 0 ; then
    cat <<EOF 1>&2
Usage: $0 <source-dir>... <.jar-file>... <_JAR-macro>...

<source-dir>:

Search source directory for .java, .mkjava, .shjava, .javain, .c and
.cxx files.  For each, generate a corresponding automake entry.  If
the file contains a main program, also generate automake to build the
corresponding program.  Any program located under a bindir/, sbindir/,
or libexecdir/ sub-directory, will be installed in the corresponding
bin/, sbin/, or libexec/ destination directory.

<.jar-file> or <_JAR-macro>:

Generate rules to compile the corresponding .jar file into a JNI
object file.  In the case of _JAR files, initialize the Makefile
variable to @_JAR@; it is assumed that configure.ac contains logic to
perform that substitution.

EOF
    exit 1
fi

dirs=
jars=
JARS=
while test $# -gt 0
do
  case "$1" in
      *.jar ) jars="${jars} $1" ;;
      *_JAR ) JARS="${JARS} $1" ;;
      * ) dirs="${dirs} $1" ;;
  esac
  shift
done
dirs=`echo ${dirs}`
jars=`echo ${jars}`
JARS=`echo ${JARS}`

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

echo_PROGRAMS ()
{
    case "$1" in
	*/bindir/* ) echo "bin_PROGRAMS += $1" ;;
	*/sbindir/* ) echo "sbin_PROGRAMS += $1" ;;
	*/libexecdir/* ) echo "libexec_PROGRAMS += $1" ;;
        * ) echo "noinst_PROGRAMS += $1" ;;
    esac
}

echo_LDFLAGS ()
{
    case "$1" in
	*_bindir_* | *_sbindir_* | *_libexec_* )
                echo "$1_LDFLAGS = --main=$2"
                echo "$1_LDFLAGS += -Djava.library.path=@RPATH@"
                echo "$1_LDFLAGS += -Wl,-rpath,@RPATH@"
		;;
	* )
                echo "$1_LDFLAGS = --main=$2 \$(GEN_GCJ_RPATH_FLAGS)"
		;;
    esac
}

has_main ()
{
    case "$1" in
		*.java ) grep ' main[ ]*[(]' $1 > /dev/null 2>&1 ;;
        *.c|*.cxx ) grep -e '^main[( ]' -e ' main[( ]' $1 > /dev/null 2>&1 ;;
		* ) false ;; 
    esac
}


GEN_ARGS="${dirs} ${jars} ${JARS}"
print_header Makefile.gen.in arguments: ${GEN_ARGS}
echo GEN_ARGS="${GEN_ARGS}"


# Generate rules to compile any .jar and _JAR files.

echo GEN_JARS=

print_jar_rule ()
{
  cat <<EOF
# print_jar_rule $1 $2
$1.jar: \$($2_JAR)
	cp \$($2_JAR) .
BUILT_SOURCES += $1.jar
GEN_JARS += $1.jar
noinst_LIBRARIES += libfrysk-$1.a
libfrysk_$1_a_LIBADD = $1.o
$1.o: $1.jar
libfrysk_$1_a_SOURCES = 
libfrysk_$1_so_SOURCES =
frysk_$1_db_SOURCES =
CLEANFILES += $1.jar $1.o libfrysk-$1.a libfrysk-$1.so
libfrysk-$1.so: libfrysk-$1.a
noinst_PROGRAMS += frysk-$1.db
solib_PROGRAMS += libfrysk-$1.so
frysk-$1.db: libfrysk-$1.so $1.jar
EOF
}

for jar in x ${jars}
do
  test ${jar} = x && continue
  b=`basename ${jar} .jar`
  d=`dirname ${jar}`
  B=`echo $b | tr '[a-z]' '[A-Z]'`
  echo ""
  print_header "... $jar"
  echo ${B}_JAR = ${jar}
cat <<EOF
\$(${B}_JAR):
	cd ${d} && \$(MAKE) \$(AM_MAKEFLAGS)
EOF
  print_jar_rule ${b} ${B}
done

# These imports are included in the built sources to ensure that they
# have been compiled _before_ any files in this directory.  Otherwize
# JAVAC complains about these files not yet existing.

for jar in x ${JARS}
do
  test ${jar} = x && continue
  B=`basename ${jar} _JAR`
  b=`echo ${B} | tr '[A-Z]' '[a-z]'`
  echo ""
  print_header "... $jar"
  echo ${B}_JAR = @${B}_JAR@
  print_jar_rule ${b} ${B}
done


# If there are no directories, bail here.

test x"${dirs}" = x && exit 0


echo GEN_SOURCES =
echo GEN_NODIST =
 

# Generate rule to build this directory's .jar, .a, and .so file.
# Need to do this here as automake gets grumpy when things like
# $(GEN__DIR)_jar_SOURCES appear in Makefile.am

# Generate a file containing the .java files that need compiling,
# otherwize the list of files is so long that SH gets an argument
# length error.

pwd=`pwd`
dir=`basename $pwd`
_dir=`echo ${dir} | sed -e 's,[-/],_,g'`
print_header "... creating rule for ${dir}.db et.al."

cat <<EOF
solib_PROGRAMS += lib${dir}.so
GEN_GCJ_LDADD += lib${dir}.a
lib${_dir}_a_SOURCES = \$(GEN_SOURCES)
nodist_lib${_dir}_a_SOURCES = \$(GEN_NODIST)
lib${_dir}_so_SOURCES = 
noinst_LIBRARIES += lib${dir}.a
lib${dir}.so: lib${dir}.a

# Create a list of .java files that need to be compiled.  It turns out
# that it is faster to just feed all the files en-mass to the compiler
# (then compile each individually).  Put the list into a file to avoid
# having too-long an argument list.  Remember to filter out duplicate
# directories.

\$(GEN_CLASSDIR)/files: lib${dir}.a
	rm -rf \$(GEN_CLASSDIR)
	mkdir -p \$(GEN_CLASSDIR)
	dirs= ; \
	for d in \$(top_builddir) \$(top_srcdir) ; do \
	    p=\`cd \$\$d && pwd\` ; \
	    case " \$\$dirs " in \
	        *\$\$p* ) continue ;; \
	    esac ; \
	    dirs="\$\$dirs \$\$p" ; \
	    for g in ${dirs} ; do \
		find \$\$d/\$\$g \
	            -name '*#*' -prune -o \
                    -name '[A-Za-z]*.java' -print ; \
	    done ; \
	done > \$@.tmp
	echo TestRunner.java >> \$@.tmp
	mv \$@.tmp \$@

# Using that list, convert to .class files and from there to a .jar.
# Since java compilers don't abort on a warning, fake the behavior by
# checking for any output.

java_DATA += ${dir}.jar
${dir}.jar: \$(GEN_CLASSDIR)/files
	\$(JAVAC) -d \$(GEN_CLASSDIR) \$(JAVACFLAGS) \
		@\$(GEN_CLASSDIR)/files \
		2>&1 | tee \$*.log
	if test -s \$*.log ; \
	then rm \$*.log ; false ; \
	fi
	cd \$(GEN_CLASSDIR) ; \
		find * -name '*.class' -print \
		| \$(JAR) -@ -cf \$@
	mv \$(GEN_CLASSDIR)/\$@ \$@

# Finally, merge the .so and .jar files into the java .db file.

noinst_PROGRAMS += ${dir}.db
${_dir}_db_SOURCES = 
${dir}.db: lib${dir}.so ${dir}.jar
	gcj-dbtool -n \$@.tmp
	gcj-dbtool -a \$@.tmp ${dir}.jar lib${dir}.so
	mv \$@.tmp \$@
EOF





for suffix in .mkjava .shjava .javain ; do
    print_header "... ${suffix}"
    SUFFIX=`echo ${suffix} | tr '[a-z.]' '[A-Z_]'`
    find ${dirs} \
	-name "*${suffix}" -print \
	| sort -f | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	echo "GEN_SOURCES += ${file}"
	echo "BUILT_SOURCES += ${d}/${b}.java"
	echo "${d}/${b}.java: \$(MKJAVA)"
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
	test -r "${d}/${b}.javain" && continue
	echo "GEN_SOURCES += ${file}"
	if has_main ${file} ; then
	    echo "${name_}_SOURCES ="
	    echo "${name_}_LINK = \$(GCJLINK)"
	    echo_PROGRAMS ${name}
	    echo_LDFLAGS ${name_} ${class}
	    echo "${name_}_LDADD = \$(GEN_GCJ_LDADD)"
	fi
    done
done



for suffix in .cxx .c .hxx ; do
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
	    echo_PROGRAMS ${name}
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

print_header "... *.cxx=.h"
find ${dirs} -name 'cni' -print | while read d
do
    find $d -name '*.cxx' -print
done \
    | xargs grep '#include ".*.h"' \
    | sed -e 's/^.*#include "//' -e 's/.h".*$//' -e 's/$.*//' \
    | sort -u \
    | while read c
do
  if test -r $c.java ; then
      echo "BUILT_SOURCES += $c.h"
  fi
done




# Form a list of all the .glade files, these are installed in
# PREFIX/share/PACKAGE/glade/.

print_header "... glade_DATA"
echo "gladedir = \$(pkgdatadir)/glade"
echo "glade_DATA ="
find ${dirs} -type f -name '*.glade' | while read file
do
  echo glade_DATA += ${file}
done

# Form a list of all the image files, these are installed in
# PREFIX/share/PACKAGE/images/.

print_header "... image_DATA"
echo "imagedir = \$(pkgdatadir)/images"
echo "image_DATA ="
find ${dirs} -type f -name '*.png' -o -name '*.jpg' | while read file
do
  echo image_DATA += ${file}
done

# Form a list of all the .properties files, these need to be copied over
# after install

print_header "... properties_DATA"
echo "propertydir = \$(pkgdatadir)"
echo "property_DATA ="
find ${dirs} -type f -name '*.properties' | while read file
do
  echo property_DATA += ${file}
done

# Form a list of all the .fig files, they need to be compiled into
# .jpg
print_header "... sample_DATA"
echo "sampledir = \$(pkgdatadir)/samples"
echo "sample_DATA ="
find ${dirs} -type f -name 'test*.cpp' | while read file
do
  echo sample_DATA += ${file}
done

# Form a list of all the .fig files, they need to be compiled into
# .jpg.

print_header "... GEN_FIG = .fig"
echo GEN_FIG =
find ${dirs} -type f -name '*.fig' | while read f
do
  echo GEN_FIG += $f
  echo EXTRA_DIST += $f
done

# Form a list of all the antlr generated files.

print_header "... GEN_G = .g"
find ${dirs} -type f -name '*.g' | while read g
do
  echo "EXTRA_DIST += $g"
  d=`dirname $g`
  (
      awk '/class/ { print $2 }' $g
      awk '/class .* extends .*Parser/ { print $2"TokenTypes" }' $g
  ) | while read c
  do
    echo "GEN_NODIST += $d/$c.java"
    echo "BUILT_SOURCES += $d/$c.java"
    echo "EXTRA_DIST += $d/$c.sed"
    t=$d/$c.tmp
    echo "CLEANFILES += $t"
cat <<EOF
$d/$c.java: $g $d/$c.sed
	mkdir -p $t
	\$(ANTLR) -o $t \$(srcdir)/$g
	sed -f \$(srcdir)/$d/$c.sed < $t/$c.java > $d/$c.java
	rm -rf $t
EOF
  done
done


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
