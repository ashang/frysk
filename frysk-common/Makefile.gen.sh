#!/bin/sh -eu
# This file is part of the program FRYSK.
#
# Copyright 2005, 2006, 2007, Red Hat Inc.
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
Usage: $0 [ --cni ] <source-dir>... <.jar-file>... <_JAR-macro>...

--cni:

Include CNI directories in build.

<source-dir>:

Search source directory for .java, .mkjava, .shjava, shenum, mkenum,
.javain, cxxin, .c and .cxx files.  For each, generate a corresponding
automake entry.  If the file contains a main program, also generate
automake to build the corresponding program.  Any program located
under a bindir/, sbindir/, pkgdatadir, or pkglibdir/ sub-directory,
will be installed in the corresponding bin/, sbin/, share/, or
lib{,64}/frysk/ destination directory.

<.jar-file> or <_JAR-macro>:

Generate rules to compile the corresponding .jar file into a JNI
object file.  In the case of _JAR files, initialize the Makefile
variable to @_JAR@; it is assumed that configure.ac contains logic to
perform that substitution.

EOF
    exit 1
fi

cni=false
dirs=
jars=
JARS=
GEN_ARGS="$@"
while test $# -gt 0
do
  case "$1" in
      --cni ) cni=true ;;
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
# this list, and not run a local find.

(
    find ${dirs} -name 'CVS' -prune \
    -o -name "[A-Za-z]*\.s" -print \
    -o -name "[A-Za-z]*\.S" -print \
    -o -name "[A-Za-z]*\.h" -print \
    -o -name "[A-Za-z]*\.c" -print \
    -o -name "[A-Za-z]*\.cpp" -print \
    -o -name "[A-Za-z]*\.java" -print \
    -o -name "[A-Za-z]*\.shjava" -print \
    -o -name "[A-Za-z]*\.javain" -print \
    -o -name "[A-Za-z]*\.mkjava" -print \
    -o -name "[A-Za-z]*\.mkenum" -print \
    -o -name "[A-Za-z]*\.shenum" -print \
    -o -name "[A-Za-z]*\.fig" -print \
    -o -name "[A-Za-z]*\.g" -print \
    -o -name "[A-Za-z0-9_]*\.glade" -print \
    -o -name "[A-Za-z0-9_]*\.png" -print \
    -o -name "[A-Za-z0-9_]*\.gif" -print \
    -o -name "[A-Za-z0-9_]*\.xml" -print \
    -o -path "*dir/[A-Za-z_]*\.in" -print \
    -o -path "*dir/[A-Za-z_]*\.uu" -print \
    -o -path "*dir/[A-Za-z]*\.sh" -print \
    -o -path "*dir/[A-Za-z]*\.py" -print \
    -o -type f -name 'test*' -print
    if $cni ; then
	find ${dirs} \
	    -path '*/cni/[A-Za-z]*\.hxx' -print \
	    -o -path '*/cni/[A-Za-z]*\.cxxin' -print \
	    -o -path '*/cni/[A-Za-z]*\.cxx' -print
    fi
) | sort -f > files.tmp

if cmp files.tmp files.list > /dev/null 2>&1
then
    rm files.tmp
else
    echo 1>&2 "Updating files.list"
    mv files.tmp files.list
fi

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

check_MANS ()
{
    # bin/ directories require a man page.
    case "$1" in
	*bindir/* )
          if test ! -r $1.xml ; then
	      echo "error: no $(basename $(dirname $1))/$(basename $1).xml man page" 1>&2
	      exit 1
	  fi
	  ;;
    esac
}

echo_PROGRAMS ()
{
    case "$1" in
	*.javain )
	    # .javain programs are never installed.
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

echo_arch32_COMPILER()
{
	# FIXME: This variable should outputted only when a rule uses it.
	# As a workaround, we omit the "if DO_ARCH32_TEST", as not all
	# configure scripts call FRYSK_DO_ARCH32_TEST.
	#echo "if DO_ARCH32_TEST"
	echo "ARCH32_COMPILE=\$(CC) \$(DEFAULT_INCLUDES) \$(INCLUDES) \
	      \$(AM_CPPFLAGS) \$(CPPFLAGS) \$(AM_CFLAGS)"
	#echo "endif"
}

# usage:
#	echo_arch32_PROGRAMS ${name} ${file} $(COMPILE_CMD) ${arch32_cflag_name}
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
	    
	    local compiler="$3"
	    local cflag="$4"
	    
	    echo 
	    echo "if DO_ARCH32_TEST"
	    echo "${name_}_SOURCES = ${file}"
	    echo "am_${name_}_OBJECTS = ${dir_name}/arch32/${base_name}.\$(OBJEXT)"
	    echo "${ldflags}"
	    
	    test ${suffix} = .cxx && echo "${name_}_LINK = \$(CXXLINK)"

cat <<EOF
${dir_name}/arch32/${base_name}.\$(OBJEXT): \$(${name_}_SOURCES) frysk/pkglibdir/arch32/\$(am__dirstamp)
	@ARCH32_COMPILE=\`echo "\$(${compiler}) " | sed -e 's, -m64 , ,g'\`; \\
	\$\$ARCH32_COMPILE \$(${cflag}) -c -o \$@ $<

${dir_name}/arch32/${base_name}\$(EXEEXT): \$(${name_}_OBJECTS) \$(${name_}_DEPENDENCIES) ${dir_name}/arch32/\$(am__dirstamp)
	@rm -f \$@
	@ARCH32_LINK=\`echo "\$(LINK) " | sed -e 's, -m64 , ,g'\`; \\
	\$\$ARCH32_LINK \$(${name_}_LDFLAGS) \$(${name_}_OBJECTS) \$(${name_}_LDADD) \$(LIBS)
${dir}32_PROGRAMS += ${dir_name}/arch32/${base_name}
MOSTLYCLEANFILES += ${dir_name}/arch32/${base_name}.\$(OBJEXT)
EOF

	    if grep pthread.h ${file} > /dev/null 2>&1 ; then
		echo "${name_}_LDADD = -lpthread"
	    fi
	    echo "endif"
	    echo
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
    case "${name}" in
	*dir/* )
                local base=`echo "${class}" | sed -e 's,.*\.,,'`
                echo "${name_}_LDFLAGS = --main=${base}"
                echo "${name_}_LDFLAGS += \${GEN_${GEN_UBASENAME}_RPATH_FLAGS}"
		;;
	* )
	        echo "${name_}_LDFLAGS = --main=${class}"
                echo "${name_}_LDFLAGS += \$(GEN_GCJ_RPATH_FLAGS)"
		;;
    esac
    echo "${name_}_LDFLAGS += \${GEN_GCJ_NO_SIGCHLD_FLAGS}"
}

has_main ()
{
    case "$1" in
	*.javain )
            # .javain files must always have main
            if jv-scan --print-main $1 | grep .  > /dev/null 2>&1 ; then
		:
	    else
		echo "$1 must have a main" 1>&2
		exit 1
	    fi
            true
	    ;;
	*.java )
	    jv-scan --print-main $1 | grep .  > /dev/null 2>&1
	    ;;
        *.c | *.cxx )
	    grep -e '^main[( ]' -e ' main[( ]' $1 > /dev/null 2>&1
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
echo GEN_${GEN_UBASENAME}_RPATH_FLAGS = -Djava.library.path=@RPATH@ -Wl,-rpath,@RPATH@


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


# If there are no directories, bail here.  Need to do this here as
# automake gets grumpy when things like $(GEN__DIR)_jar_SOURCES appear
# in Makefile.am

test x"${dirs}" = x && exit 0


print_header "... the lib${GEN_DIRNAME}.a skeleton"

nodist_lib_sources=nodist_lib${GEN_MAKENAME}_a_SOURCES
sources=lib${GEN_MAKENAME}_a_SOURCES

cat <<EOF

# Most of the directory's sources will be built into a single archive
# (.a).  Start with a skeleton for that archive and then accumulate
# the relevant files.

noinst_LIBRARIES += lib${GEN_DIRNAME}.a
${sources} =
${nodist_lib_sources} =
GEN_GCJ_LDADD += lib${GEN_DIRNAME}.a

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
EXTRA_DIST += common/TestRunner.javain
nodist_TestRunner_SOURCES = TestRunner.java
CLEANFILES += TestRunner.java
${nodist_lib_sources} += ${GEN_SOURCENAME}/JUnitTests.java
BUILT_SOURCES += ${GEN_SOURCENAME}/JUnitTests.java
TestRunner_LDADD = \${LIBJUNIT} \${GEN_GCJ_LDADD}
TESTS += TestRunner
noinst_PROGRAMS += TestRunner
EOF
echo_LDFLAGS TestRunner


# Generate SOURCES list for all files.

for suffix in .java .mkjava .shjava .mkenum .shenum .javain ; do
    print_header "... ${suffix}"
    grep -e  "\\${suffix}\$" files.list | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	name=${d}/${b}
	# Skip when a generated file, happens when configured in
	# source tree - handled earlier.
	case ${suffix} in
	    .java)
	        test -r "${d}/${b}.mkjava" && continue
		test -r "${d}/${b}.shjava" && continue
		test -r "${d}/${b}.mkenum" && continue
		test -r "${d}/${b}.shenum" && continue
		test -r "${d}/${b}.javain" && continue
		test -r "common/${b}.javain" && continue # too strong?
		test "${b}" = JUnitTests && continue # hack
		test -r "${d}/${b}.g" && continue
		test -r "${d}/${b}.sed" && continue
		echo "${sources} += ${file}"
		;;
	    *)
	        echo "EXTRA_DIST += ${file}"
		echo "BUILT_SOURCES += ${name}.java"
		case "${suffix}" in
		    *java ) echo "${name}.java: \$(MKJAVA)" ;;
		    *enum ) echo "${name}.java: \$(MKENUM)" ;;
		esac
		echo "${nodist_lib_sources} += ${d}/${b}.java"
		;;
	esac
	echo "${GEN_DIRNAME}.jar: ${name}.java"
	if has_main ${file} ; then
	    name_=`echo_name_ ${name}`
	    echo_PROGRAMS ${name}
	    check_MANS ${name}
	    echo "${name_}_SOURCES ="
	    echo "${name_}_LINK = \$(GCJLINK)"
	    echo_LDFLAGS ${name}
	    echo "${name_}_LDADD = \$(GEN_GCJ_LDADD)"
	fi
    done || exit 1
done

# output the compile for arch32
echo_arch32_COMPILER

for suffix in .cxxin ; do
    print_header "... ${suffix}"
    grep -e "\\${suffix}\$" files.list | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	echo "EXTRA_DIST += ${file}"
	echo "${nodist_lib_sources} += ${d}/${b}.cxx"
	echo "BUILT_SOURCES += ${d}/${b}.cxx"
    done
done

for suffix in .cxx .c .hxx ; do
    print_header "... ${suffix}"
    grep -e "\\${suffix}\$" files.list | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	name=${d}/${b}
	name_=`echo ${name} | sed -e 'y,/-,__,'`
	if has_main ${file} ; then
	    echo "${name_}_SOURCES = ${file}"
	    test ${suffix} = .cxx && echo "${name_}_LINK = \$(CXXLINK)"
	    echo_PROGRAMS ${name}
	    check_MANS ${name}
	    if grep pthread.h ${file} > /dev/null 2>&1 ; then
		echo "${name_}_LDADD = -lpthread"
	    fi

           # Generate the rules for arch32 test
           if [ -z "${arch32_cflag_name+set}" ]; then
               arch32_cflag_name=`echo ${d}/arch32 | sed -e 'y,/-,__,'`_CFLAGS
               # Set unconditionally, see the comment in echo_arch32_COMPILER.
               echo "${arch32_cflag_name}=-m32 -g"
           fi

           echo_arch32_PROGRAMS ${name} ${file} "ARCH32_COMPILE" ${arch32_cflag_name}

	else
	    echo "${sources} += ${file}"
	fi
    done
    # arch32_cflag_name is empty again, the pipe created a subshell.
done

#
# Generate rules for .S/.s assembly files
#
for suffix in .s .S ; do
    print_header "... ${suffix}"
    grep -e "\\${suffix}\$" files.list | while read file ; do
        d=`dirname ${file}`
        b=`basename ${file} ${suffix}`
        name=${d}/${b}
        name_=`echo ${name} | sed -e 'y,/-,__,'`
	
	echo "${name_}_SOURCES = ${file}"
	echo_PROGRAMS ${name}

	# Generate the rules for arch32 test
	if [ -z "${arch32_as_cflag_name+set}" ]; then
	    arch32_as_cflag_name=`echo ${d}/arch32 | sed -e 'y,/-,__,'`_AS_CFLAGS
            # Set unconditionally, see the comment in echo_arch32_COMPILER.
	    echo "${arch32_as_cflag_name}=-m32 -g"
	fi

	echo_arch32_PROGRAMS ${name} ${file} "CCASCOMPILE" ${arch32_as_cflag_name}
    done
    # arch32_as_cflag_name is empty again, the pipe created a subshell.
done

# Grep the cni/*.cxx files forming a list of included files.  Assume
# these are all generated from .class files.  The list can be pruned a
# little since, given Class$Nested and Class, generating Class.h will
# automatically generate the inner Class$Nested class.

print_header "... *.cxx=.h"
grep -e '/cni/' files.list \
    | xargs grep -H '#include ".*.h"' \
    | sed -e 's/\..*:#include "/.o /' -e 's/\.h".*$//' -e 's/$.*//' \
    | while read o h
do
  if test \
      -r ${h}.java -o \
      -r ${h}.shenum -o \
      -r ${h}.mkenum -o \
      -r ${h}.shjava -o \
      -r ${h}.mkjava -o \
      -r ${h}.javain \
      ; then
      echo ${o}: ${h}.h
      echo "CLEANFILES += ${h}.h"
      echo "CLEANFILES += ${h}\\\$\$*.h"
  fi
done | sort -u


# Generate rules for all .xml files, assume that they are converted to
# man pages.

print_header "... .xml"
grep -e '\.xml$' files.list | while read xml
do
  case "$xml" in
      *dir/* )
          # Only programs in bindir, pkglibdir et.al. get man pages.
          echo "EXTRA_DIST += $xml"
          # extract the section number
	  n=`sed -n -e 's,.*<manvolnum>\([0-9]\)</manvolnum>.*,\1,p' < $xml`
	  d=`dirname $xml`
          # And the possible list of names.
	  sed -n -e 's,^.*<refname>\(.*\)</refname>.*$,\1,p' < $xml \
	          | while read title ; do
                  # Need to generate explicit rules
                  cat <<EOF
man_MANS += ${d}/${title}.${n}
CLEANFILES += ${d}/${title}.${n}
${d}/${title}.${n}: $xml
	mkdir -p ${d}
	\$(SUBST_SED) < \$< > \$@.tmp
	\$(XMLTO) -o ${d} man \$@.tmp
	rm -f \$@.tmp
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
    echo EXTRA_DIST += ${file}
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
  echo EXTRA_DIST += $f
  echo CLEANFILES += $jpg
  echo noinst_DATA += $jpg
done

# Form a list of all the antlr generated files.

print_header "... GEN_G = .g"
grep -e '\.g$' files.list | while read g
do
  echo "EXTRA_DIST += $g"
  d=`dirname $g`
  (
      awk '/class/ { print $2 }' $g
      awk '/class .* extends .*Parser/ { print $2"TokenTypes" }' $g
  ) | while read c
  do
    echo "${nodist_lib_sources} += $d/$c.java"
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
            echo "EXTRA_DIST += $f"
	    ;;
	*/bindir/* | */pkglibdir/* )
	    # skip, not a DATA dir.
	    continue
	    ;;
	*)
	    data="$f"
	    ;;
    esac
    if eval test -z "\${${dir}_DATA:-}"; then
	eval ${dir}_DATA=true
	echo "dist_${dir}_DATA = "
	if test -n "${d2}"; then
	    echo "${dir}dir = \$(${d1}dir)/${d2}"
	fi
    fi
    echo "dist_${dir}_DATA += $data"
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
