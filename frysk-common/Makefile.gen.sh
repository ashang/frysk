#!/bin/sh -eu
# This file is part of the program FRYSK.
#
# Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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
Usage: $0 [ --cni | --jni ] <source-dir>... <.jar-file>... <_JAR-macro>...

--cni: Include CNI directories in build.
--jni: Include JNI directories in build and build with JNI abi.

<source-dir>:

Search source directory for .java, .shenum, .mkenum, -in,
-sh, .c and .cxx files.  For each, generate a corresponding automake
entry.  If the file contains a main program, also generate automake to
build the corresponding program.  Any program located under a bindir/,
sbindir/, pkgdatadir, or pkglibdir/ sub-directory, will be installed
in the corresponding bin/, sbin/, share/, or lib{,64}/frysk/
destination directory.

<.jar-file> or <_JAR-macro>:

Generate rules to compile the corresponding .jar file into a JNI
object file.  In the case of _JAR files, initialize the Makefile
variable to @_JAR@; it is assumed that configure.ac contains logic to
perform that substitution.

EOF
    exit 1
fi

cni=false
jni=false
dirs=
jars=
JARS=
GEN_ARGS="$@"
while test $# -gt 0
do
  case "$1" in
      --cni ) cni=true ;;
      --jni ) jni=true ;;
      *.jar ) jars="${jars} $1" ;;
      *_JAR ) JARS="${JARS} $1" ;;
      * ) dirs="${dirs} $1" ;;
  esac
  shift
done
dirs=`echo ${dirs}`
jars=`echo ${jars}`
JARS=`echo ${JARS}`

# Generate a list of source files; all the code below should refer to
# this list, and not run a local find.  If ${dirs} is empty, the
# search is started from "." so prune that.

(
    find ${dirs} -name '\.' -prune \
    -o -name 'CVS' -prune \
    -o -name "ChangeLog" -print \
    -o -name "[A-Za-z]*\.h" -print \
    -o -name "[A-Za-z]*\.c" -print \
    -o -name "[A-Za-z]*\.java" -print \
    -o -name "[A-Za-z]*\.mkenum" -print \
    -o -name "[A-Za-z]*\.shenum" -print \
    -o -name "[A-Za-z]*\.desktop" -print \
    -o -name "[A-Za-z]*\.properties" -print \
    -o -name "[A-Za-z]*\.fig" -print \
    -o -name "[A-Za-z]*\.g" -print \
    -o -name "[A-Za-z0-9_]*\.glade" -print \
    -o -name "[A-Za-z0-9_]*\.png" -print \
    -o -name "[A-Za-z0-9_]*\.gif" -print \
    -o -name "[A-Za-z0-9_]*\.xml-in" -print \
    -o -path "*dir/[A-Za-z_]*\.[sS]" -print \
    -o -path "*dir/[A-Za-z_]*\.in" -print \
    -o -path "*dir/[A-Za-z_]*\.uu" -print \
    -o -path "*dir/[A-Za-z]*\.sh" -print \
    -o -path "*dir/[A-Za-z]*\.py" -print \
    -o -path '[A-Za-z]*\.hxx' -print \
    -o -path '[A-Za-z]*\.java-sh' -print \
    -o -path '[A-Za-z]*\.c-sh' -print \
    -o -path '[A-Za-z]*\.cxx-sh' -print \
    -o -path '[A-Za-z]*\.java-in' -print \
    -o -path '[A-Za-z]*\.cxx-in' -print \
    -o -path '[A-Za-z]*\.c-in' -print \
    -o -path '[A-Za-z]*\.cxx' -print \
    -o -path '*/cni/[A-Za-z]*\.[sS]' -print \
    -o -path '*/jni/[A-Za-z]*\.[sS]' -print \
    -o -type f -name 'test*' -print
    ) \
| if $cni ; then cat ; else grep -v '/cni/' ; fi \
| if $jni ; then cat ; else grep -v '/jni/' ; fi \
| sort -f > files.tmp

if cmp files.tmp files.list > /dev/null 2>&1
then
    rm files.tmp
else
    echo 1>&2 "Updating files.list"
    mv files.tmp files.list
fi

# It is assumed that each file is of the form DIRNAME/BASENAME.SUFFIX,
# pre-process each into: FILE DIRNAME BASENAME SUFFIX

sed -e 's,^\(\(.*\)/\([^/]*\)\.\([a-zA-Z-]*\)\),\1 \2 \3 \4,' \
    -e 's,-in$,,' \
    -e 's,-sh$,,' \
    < files.list \
    > files.base

#

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

# Does the automake variable exist (as in did this file define it).

automake_variable_defined ()
{
    local name=$1
    local variable=variable_${name}_set
    eval test -n \"\${${variable}:-}\"
}

# Called as: variable NAME = [VALUE] or variable NAME += VALUE sets or
# appends variable $1 to value $3, if this is the fist time called.
# The only GOT-YA is that all invocations of this function must occure
# in the same process; which means ``a | while read ... '' can't be
# used.

automake_variable ()
{
    local name=$1 ; shift
    local op=$1 ; shift
    if automake_variable_defined $name ; then
	# Append only; ignore duplicate assigns
	if test "x$op" = "x+=" ; then
	    echo "$name" += "$@"
	fi
    else
	local variable=variable_${name}_set
	eval $variable=\'$name $op "$*"\'
	echo "$name" = "$@"
    fi
}

check_MANS ()
{
    # bin/ directories require a man page.
    case "$1" in
	*bindir/* )
          if test ! -r $1.xml-in ; then
	      echo "error: no $(basename $(dirname $1))/$(basename $1).xml-in man page" 1>&2
	      exit 1
	  fi
	  ;;
    esac
}

echo_PROGRAMS ()
{
    case "$1" in
	*.java-in )
	    # .java-in programs are never installed.
	    echo "noinst_PROGRAMS += $1"
            ;;
	*dir/* )
            # extract the directory prefix
            local dir=`echo /"$1" | sed -e 's,.*/\([a-z]*\)dir/.*,\1,'`
            echo "${dir}_PROGRAMS += $1"
	    ;;
        * )
	    echo "noinst_PROGRAMS += $1"
	    ;;
    esac
}

# usage:
#	echo_arch32_PROGRAMS ${name} ${file}
echo_arch32_PROGRAMS()
{
    case "$1" in
        frysk/pkglibdir/* )
            # extract the directory prefix
            local dir=`echo /"$1" | sed -e 's,.*/\([a-z]*\)dir/.*,\1,'`

	    local file="$2"
	    local dir_name=`dirname $1`
	    local base_name=`basename $1`
	    local name="${dir_name}/arch32/${base_name}"
	    
	    local name_=`echo ${name} | sed -e 'y,/-,__,'`
	    local ldflags="${name_}_LDFLAGS = -m32 -g"
	    
	    local compiler=
	    local cflag=
	    
	    case "${file}" in
		*.S | *.s )
		  compiler=FRYSK_ASCOMPILE
		  linker=LINK
		  ;;
		*.cxx )
		  compiler=CXXCOMPILE
		  linker=CXXLINK
		  ;;
		* )
		  compiler=COMPILE
		  linker=LINK
		  ;;
	    esac

cat <<EOF

if DO_ARCH32_TEST
${name_}_SOURCES = ${file}
# why am_?
am_${name_}_OBJECTS = ${dir_name}/arch32/${base_name}.\$(OBJEXT)
${name_}_LINK = \$(ARCH32_${linker})
${dir}32_PROGRAMS += ${dir_name}/arch32/${base_name}
MOSTLYCLEANFILES += ${dir_name}/arch32/${base_name}.\$(OBJEXT)
# XXX: Re-compile whenever the base .o file chages; avoids getting
# .deps working.
${dir_name}/arch32/${base_name}.\$(OBJEXT): ${file}
	\$(ARCH32_${compiler}) -c -o \$@ $<
${dir_name}/arch32/${base_name}.\$(OBJEXT): \\
${dir_name}/${base_name}.\$(OBJEXT) \\
frysk/pkglibdir/arch32/\$(am__dirstamp)
EOF

	    if grep pthread.h ${file} > /dev/null 2>&1 ; then
		echo "${name_}_LDADD = -lpthread"
	    fi
	    echo "endif"
            ;;
        * )
            ;;
    esac
}

# Convert path to the automake equivalent (/ replaced with _).
echo_name_ ()
{
    echo "$1" | sed -e 'y,/-,__,'
}

# Print the LD flags for program.
echo_LDFLAGS ()
{
    local name=$1
    local name_=`echo_name_ $1`
    local class=`echo $1 | tr '[/]' '[.]'`
    echo "${name_}_LDFLAGS = --main=${class}"
    echo "${name_}_LDADD = \${GEN_GCJ_LDADD_LIST}"
    case "${name}" in
	*dir/* )
            # set during non-standard builds such as RHEL 4.
            echo "${name_}_LDFLAGS += \${GEN_${GEN_UBASENAME}_RPATH_FLAGS}"
	    ;;
	* )
            echo "${name_}_LDFLAGS += \$(GEN_GCJ_BUILDTREE_RPATH_FLAGS)"
	    ;;
    esac
    echo "${name_}_LDFLAGS += \${GEN_GCJ_NO_SIGCHLD_FLAGS}"
}

# Returns true if the path leads to a java source generator.

has_generated_java_source ()
{
    test \
	-r $1.shenum \
	-o -r $1.mkenum \
	-o -r $1.java-sh \
	-o -r $1.java-in
}

# Return true if the path leads to either java source, or a java
# source generator.

has_java_source ()
{
    test -r $1.java || has_generated_java_source $1
}
	   
has_java_main ()
{
    grep ' void main[ ]*[(][ ]*String' $1 > /dev/null 2>&1
}

has_main ()
{
    case "$1" in
	*.java-in )
            # .java-in files must always have main
            if has_java_main $1 ; then
		:
	    else
		echo "$1 must have a main" 1>&2
		exit 1
	    fi
            true
	    ;;
	*.java )
	    has_java_main $1
	    ;;
        *.c | *.cxx | *.c-in | *.cxx-in | *.c-sh | *.cxx-sh )
	    grep -e '^main[( ]' -e ' main[( ]' $1 > /dev/null 2>&1
	    ;;
        *.S | *.s )
	    grep -e 'main:' -e 'FUNCTION_BEGIN *(main' $1 > /dev/null 2>&1
	    ;;
	* )
	    false
	    ;; 
    esac
}


GEN_DIRNAME=`basename $PWD`
GEN_MAKENAME=`echo ${GEN_DIRNAME} | sed -e 's,-,_,g'`
GEN_PACKAGENAME=`echo ${GEN_DIRNAME} | sed -e 's,-,.,g'`
GEN_SOURCENAME=`echo ${GEN_DIRNAME} | sed -e 's,-,/,g'`
GEN_BASENAME=`echo ${GEN_DIRNAME} | sed -e 's,.*-,,'`
GEN_UBASENAME=`echo ${GEN_BASENAME} | tr 'a-z' 'A-Z'`
print_header Makefile.gen.in arguments: ${GEN_ARGS}
echo GEN_ARGS="${GEN_ARGS}"
echo GEN_DIRS = ${dirs}
echo GEN_DIRNAME=${GEN_DIRNAME}
echo GEN_PACKAGENAME=${GEN_PACKAGENAME}
echo GEN_SOURCENAME=${GEN_SOURCENAME}
echo GEN_BASENAME=${GEN_BASENAME}
echo GEN_UBASENAME=${GEN_UBASENAME}


# Generate rules to compile any .jar and _JAR files.

echo GEN_JARS=

print_jar_rule ()
{
  cat <<EOF
# print_jar_rule $1 $2
$1.jar: \$($2_JAR)
	cp \$($2_JAR) $1.jar
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


# If there are no directories, bail here.  Need to do this here as
# automake gets grumpy when things like $(GEN__DIR)_jar_SOURCES appear
# in Makefile.am

test x"${dirs}" = x && exit 0


print_header "... the lib${GEN_DIRNAME}.a skeleton"

sources=lib${GEN_MAKENAME}_a_SOURCES

# Most of the directory's sources will be built into a single archive
# (.a).  Start with a skeleton for that archive and then accumulate
# the relevant files.
automake_variable ${sources} =

cat <<EOF

noinst_LIBRARIES += lib${GEN_DIRNAME}.a
if JAR_COMPILE
${sources} += ${GEN_DIRNAME}.jar
endif
GEN_GCJ_LDADD_LIST += lib${GEN_DIRNAME}.a

# Compile the .a into a .so; Makefile.rules contains the rule and does
# not use libtool.

solib_PROGRAMS += lib${GEN_DIRNAME}.so
lib${GEN_MAKENAME}_so_SOURCES = 
lib${GEN_DIRNAME}.so: lib${GEN_DIRNAME}.a


# Using that list, convert to .class files and from there to a .jar.
# Since java compilers don't abort on a warning, fake the behavior by
# checking for any output.

java_DATA += ${GEN_DIRNAME}.jar
CLEANFILES += ${GEN_DIRNAME}.jar

# Finally, merge the .so and .jar files into the java .db file.

noinst_PROGRAMS += ${GEN_DIRNAME}.db
${GEN_MAKENAME}_db_SOURCES = 
${GEN_DIRNAME}.db: lib${GEN_DIRNAME}.so ${GEN_DIRNAME}.jar
	\$(GCJ_DBTOOL) -n \$@.tmp
	\$(GCJ_DBTOOL) -a \$@.tmp ${GEN_DIRNAME}.jar lib${GEN_DIRNAME}.so
	mv \$@.tmp \$@
EOF



# Test runner program.

cat <<EOF
TestRunner_SOURCES = TestRunner.java
CLEANFILES += TestRunner.java
if !JAR_COMPILE
${sources} += ${GEN_SOURCENAME}/JUnitTests.java
endif
BUILT_SOURCES += ${GEN_SOURCENAME}/JUnitTests.java
SCRIPT_BUILT += ${GEN_SOURCENAME}/JUnitTests.java
TESTS += TestRunner
noinst_PROGRAMS += TestRunner
EOF
echo_LDFLAGS TestRunner


# Generate SOURCES list for all files.

for suffix in .java .java-sh .mkenum .shenum .java-in ; do
    print_header "... ${suffix}"
    grep -e  "\\${suffix}\$" files.list | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	name=${d}/${b}
	# Skip when a generated file, happens when configured in
	# source tree - handled earlier.
	case ${suffix} in
	    .java)
		has_generated_java_source ${d}/${b} && continue
		test -r "common/${b}.java-in" && continue # too strong?
		test "${b}" = JUnitTests && continue # hack
		test -r "${d}/${b}.g" && continue
		test -r "${d}/${b}.sed" && continue
		echo "if !JAR_COMPILE"
		echo "${sources} += ${file}"
		echo "endif"
		;;
	esac
	echo "${GEN_DIRNAME}.jar: ${name}.java"
	if has_main ${file} ; then
	    name_=`echo_name_ ${name}`
	    echo_PROGRAMS ${name}
	    check_MANS ${name}
	    echo "${name_}_SOURCES ="
	    echo "${name_}_LINK = \$(GCJLINK) \$(${name_}_LDFLAGS)"
	    echo_LDFLAGS ${name}
	fi
    done || exit 1
done

for suffix in .java-in .java-sh .mkenum .shenum ; do
    print_header "... ${suffix}"
    s=`echo ${suffix} | sed \
	-e 's/-sh$//' \
	-e 's/-in$//' \
	-e 's/.mkenum$/.java/' \
	-e 's/.shenum$/.java/' \
	`
    grep -e "\\${suffix}\$" files.list | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	name="${d}/${b}${s}"
	echo "if !JAR_COMPILE"
	echo "${sources} += ${file}"
	echo "endif"
	echo "BUILT_SOURCES += ${name}"
	echo "SCRIPT_BUILT += ${name}"
        case "${suffix}" in
	    .mkenum ) echo "${name}: \$(MKENUM)" ;;
	esac
    done
done

generate_compile ()
{
    local file=$1
    local d=$2
    local b=$3
    local suffix=$4
    local sources=$5
    local name=${d}/${b}
    local name_=`echo ${name} | sed -e 'y,-/,__,'`
    if has_main ${file} ; then
	echo "${name_}_SOURCES = ${name}.${suffix}"
	case "${suffix}" in
	    cxx ) echo "${name_}_LINK = \$(CXXLINK)" ;;
	esac
	echo_PROGRAMS ${name}
	check_MANS ${name}
	if grep 'pthread\.h' ${file} > /dev/null 2>&1 ; then
	    echo "${name_}_LDADD = -lpthread"
	fi
        # Generate the rules for 32-bit compile
	echo_arch32_PROGRAMS ${name} ${name}.${suffix}
    else
	automake_variable ${sources} += ${file}
    fi
    case "${file}" in
	*-in | *-sh)
	    echo "BUILT_SOURCES += ${name}.${suffix}"
	    echo "SCRIPT_BUILT += ${name}.${suffix}"
	    ;;
    esac
    case "${file}" in
	*.S | *.s)
	    # Hardwire assembler dependency on include/frysk-asm.h;
	    # automake doesn't generate this :-(
	    echo "${name}.\$(OBJEXT): \$(top_srcdir)/../frysk-imports/include/frysk-asm.h"
	    echo "if DO_ARCH32_TEST"
	    echo "${d}/arch32/${b}.\$(OBJEXT): \$(top_srcdir)/../frysk-imports/include/frysk-asm.h"
	    echo "endif"
    esac
}

# What type of build?
if $cni ; then
    : default
elif $jni ; then
    echo "AM_GCJFLAGS += -fjni"
else
    : default
fi

# Grep the *.cxx and *.hxx files forming a list of included files.
# Assume these are all generated from .class files found in the master
# .jar.

generate_cni_header () {
    local file=$1
    local d=$2
    local b=$3
    local suffix=$4
    local _file=`echo $file | tr '[/.]' '[__]'`
    sed -n \
	-e 's,#include "\(.*\)\.h".*,include - \1,p' \
	-e 's,#include \([A-Z][A-Z0-9_]*\).*,minclude \1 -,p' \
	-e 's,#define \([A-Z0-9_]*\) "\(.*\)\.h".*,define \1 \2,p' \
	< $file > $$.tmp
    while read action m h j; do
	echo "# file=$file action=$action m=$m h=$h"
	if test "$action" = "minclude" ; then
            # Assume file defining macro depends on this file
	    automake_variable $m = \$\($_file\)
	elif has_java_source ${h} ; then
	    echo "JAVAH_CNI_BUILT += ${h}.h"
	    echo "CLEANFILES += ${h}.h"
	    echo "CLEANFILES += ${h}\\\$\$*.h"
	    j=`echo ${h} | tr '[_]' '[/]'`
	    echo "${h}.h: $j.java | ${GEN_DIRNAME}.jar"
	    case $action in
		include)
		    case "$suffix" in
			cxx) echo "$d/$b.o: ${h}.h" ;;
			hxx) # remember what this file includes
			    automake_variable $_file += ${h}.h ;;
		    esac
		    ;;
		define)
		    echo "$d/$b.o: ${h}.h"
		    # Assume file using this macro is a dependency.
		    echo "$d/$b.o: \$($m)"
		    ;;
	    esac
	fi
    done < $$.tmp
    rm -f $$.tmp
}

# Grep the *.cxx and *.hxx files forming a list of classes that are
# native.

generate_jnixx_class() {
    local file=$1
    local d=$2
    local b=$3
    local suffix=$4
    local j=$(echo $(dirname $dir)/$base)
    if has_java_source $j ; then
	echo "jni.hxx jni.cxx: $j.java"
	echo "${dir}/${base}.o: $j.java | jni.hxx.gch"
    fi
}

# Generate rules for all .xml-in files, assume that they are converted
# to man pages.

print_header "... .xml-in"
grep -e '\.xml-in$' files.list | while read xml
do
  case "$xml" in
      *dir/* )
          # Only programs in bindir, pkglibdir et.al. get man pages.
          # extract the section number
	  n=`sed -n <  $xml \
	      -e 's,.*<manvolnum>\([0-9]\)</manvolnum>.*,\1,p' \
	      -e 's,.*ENTITY volume "\([0-9]\)".*,\1,p'`
	  d=`dirname $xml`
          # And the possible list of names.
	  sed -n < $xml \
	      -e 's,^.*<refname>\(.*\)</refname>.*$,\1,p' \
	      -e 's,.*ENTITY command "\([^"]*\)".*,\1,p' \
	          | sort -u | while read title ; do
                  # Need to generate explicit rules
                  cat <<EOF
man_MANS += ${d}/${title}.${n}
CLEANFILES += ${d}/${title}.${n}
CLEANFILES += ${d}/${title}.xml
${d}/${title}.xml: $xml
${d}/${title}.${n}: ${d}/${title}.xml
EOF
	  done
	  ;;
  esac
done


# Generate rules for .in files, convert to basename using SUBST_SED.

for suffix in .sh .py
do
  print_header "... ${suffix}"
  grep -e "dir/.*\\${suffix}$" files.list | while read file
  do
    d=`dirname $file`
    b=`basename $file ${suffix}`
    echo `expr $d : '.*/\([a-z]*\)dir'`_SCRIPTS += $d/$b
    check_MANS $d/$b
    cat <<EOF
${d}/${b}: ${file}
	\$(SUBST)
EOF
  done
done


# For all .fig files, add the corresponding .jpg file to what needs to
# be built as DATA.

print_header "... .fig.jpg:"
grep -e '\.fig' files.list | while read f
do
  d=`dirname ${f}`
  b=`basename ${f} .fig`
  jpg=$d/$b.jpg
  echo CLEANFILES += $jpg
  echo noinst_DATA += $jpg
done

# Form a list of all the antlr generated files.

print_header "... GEN_G = .g"
grep -e '\.g$' files.list | while read g
do
  d=`dirname $g`
  b=`basename $g .g`
  echo "CLEANFILES += $d/$b.antlred"
  echo "CLEANDIRS += $d/$b.tmp"
  awk '
BEGIN { FS = "=" }
/importVocab/ {
    gsub(";","",$2)
    print "'$d/$b'.antlred: '$d'/" $2 "TokenTypes.txt"
}' $g
  awk '
/class/ { print $2 ".java" }
/class .* extends .*Parser/ { print $2 "TokenTypes.java" }
/class .* extends .*Parser/ { print $2 "TokenTypes.txt" }
' $g | while read c
  do
    echo "# Dummy dependency, see implicit .g.antlred for generation"
    echo "$d/$c: $d/$b.antlred"
    echo "BUILT_SOURCES += $d/$c"
    echo "ANTLR_BUILT += $d/$c"
    echo "if !JAR_COMPILE"
    echo "${sources} += $d/$c"
    echo "endif"
  done
done


# Form a list of all the stand-alone test cases that need to be run.

print_header "... TESTS += Test*.java"
grep -e 'Test.*\.java$' files.list | \
    grep -v -e 'TestCase.java$' | \
    while read file ; do
    if has_main ${file} ; then
	d=`dirname ${file}`
	b=`basename ${file} .java`
	main=${d}/${b}
	main_=`echo ${main} | tr '[/]' '[_]'`
	echo "TESTS += ${main}"
    fi
done

# Generate rules for _DATA directories; these are copied over as
# hierarchies.

print_header "... *_DATA"
sed -n -e '/dir\// {
  h
  x
  s,.*/\([^/]*\)dir/\(.*\),\1/\2,
  s,/[^/]*$,/,
  s,/\([^/]*\)/$, \1,
  s,/,,g
  x
  G
  s,\n, ,
  p
}' files.list | while read f d1 d2 ; do
    # Given a/bdir/c/d/e; read a/bdir/c/d/e bcd e
    # Given a/bdir/c; read a/bdir/c b
    dir="${d1}${d2}"
    case "$f" in
	*.bz2.uu )
	    data=`expr "$f" : '\(.*\).bz2.uu'`
	    ;;
	*/bindir/* | */pkglibdir/* )
	    # skip, not a DATA dir.
	    continue
	    ;;
	*)
	    data="$f"
	    ;;
    esac
    if test -n "${d2}"; then
	automake_variable "${dir}dir" = "\$(${d1}dir)/${d2}"
    fi
    automake_variable "${dir}_DATA" += $data
done


# Generate rules for unpacking data files.

print_header "... packed files"
for suffix in .uu .bz2 ; do
    sed -n -e "s,\\${suffix}.*,, p" files.list | while read f ; do
        d=`dirname $f`
	cat <<EOF
CLEANFILES += ${f}
${f}: ${f}${suffix}
	mkdir -p ${d}
	rm -f \$@.tmp
EOF
	case "${suffix}" in
	    .uu ) printf "\tuudecode -o \$@.tmp \$<\n" ;;
	    .bz2 ) printf "\tbunzip2 < \$< > \$@.tmp\n" ;;
	esac
	printf "\tmv \$@.tmp \$@\n"
    done
done


# Iterate through all the files in file.list; applying all applicable
# generation rules to each file type.  There are no smarts, each file
# type gets all operations listed explicitly.

print_header "bulk processing"

while read file dir base suffix ; do

    echo -n "." 1>&2

    echo ""
    echo "# file=$file"
    echo "# dir=$dir"
    echo "# base=$base"
    echo "# suffix=$suffix"
    echo ""

    case $file in
	*/cni/*.cxx | */cni/*.cxx-in | */cni/*.cxx-sh | */cni/*.hxx)
	    generate_cni_header $file $dir $base $suffix
	    generate_compile $file $dir $base $suffix ${sources}
	    ;;
	*/jni/*.cxx | */jni/*.cxx-in | */jni/*.cxx-sh | */jnixx/*.cxx )
	    generate_jnixx_class $file $dir $base $suffix
	    generate_compile $file $dir $base $suffix \
		lib${GEN_MAKENAME}_jni_a_SOURCES
	    ;;
	*.java | *.java-in | *.java-sh )
	    ;;
	*.cxx | *.c | *.S | *.cxx-in | *.c-in | *.S-in \
	    | *.cxx-sh | *.c-sh | *.S-sh )
	    # Non-cni/jni source.
	    generate_compile $file $dir $base $suffix ${sources}
	    ;;
    esac

done < files.base
echo "" 1>&2

if automake_variable_defined lib${GEN_MAKENAME}_jni_a_SOURCES ; then
    cat <<EOF
JNIXX_CLASSES = 
noinst_LIBRARIES += lib${GEN_DIRNAME}-jni.a
lib${GEN_MAKENAME}_jni_so_SOURCES =
solib_PROGRAMS += lib${GEN_DIRNAME}-jni.so
lib${GEN_DIRNAME}-jni.so: lib${GEN_DIRNAME}-jni.a
.PHONY: jni
jni: lib${GEN_DIRNAME}-jni.so ${GEN_DIRNAME}.jar
lib${GEN_MAKENAME}_jni_a_SOURCES += jni.cxx
jnixx_sources = \$(wildcard \$(root_srcdir)/frysk-sys/frysk/jnixx/*.java)
CLEANFILES += jni.hxx jni.cxx jni.hxx.gch
\$(lib${GEN_MAKENAME}_jni_a_SOURCES): | jni.hxx jni.hxx.gch
jni.hxx: \$(jnixx_sources) | ${GEN_DIRNAME}.jar
	CLASSPATH=\$(GEN_DIRNAME).jar:\$(CLASSPATH) \\
	    \$(JAVA) frysk.jnixx.Main \\
		hxx \\
		jni.hxx ${GEN_DIRNAME}.jar \$(JNIXX_CLASSES) \\
	        > \$@.tmp
	mv \$@.tmp \$@
jni.hxx.gch: jni.hxx
	\$(CXXCOMPILE) -c -x c++-header jni.hxx
jni.cxx: \$(jnixx_sources) | ${GEN_DIRNAME}.jar
	CLASSPATH=\$(GEN_DIRNAME).jar:\$(CLASSPATH) \\
	    \$(JAVA) frysk.jnixx.Main \\
		cxx \\
		jni.hxx ${GEN_DIRNAME}.jar \$(JNIXX_CLASSES) \\
	        > \$@.tmp
	mv \$@.tmp \$@
jni.o: jni.hxx | jni.hxx.gch
EOF
fi
