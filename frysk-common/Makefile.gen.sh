#!/bin/sh -eu
# This file is part of the program FRYSK.
#
# Copyright 2005, 2006, Red Hat Inc.
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

Search source directory for .java, .mkjava, .shjava, shenum, mkenum,
.javain, .c and .cxx files.  For each, generate a corresponding
automake entry.  If the file contains a main program, also generate
automake to build the corresponding program.  Any program located
under a bindir/, sbindir/, or libexecdir/ sub-directory, will be
installed in the corresponding bin/, sbin/, or libexec/ destination
directory.

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

echo_MANS ()
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
    case "$1" in
      *dir/* )
          # Only programs in bindir, pkglibexecdir et.al. get man pages.
          if test -r $1.xml ; then
	      echo "EXTRA_DIST += $1.xml"
              # extract the section number
              local n=`sed -n -e 's,.*<manvolnum>\([0-9]\)</manvolnum>.*,\1,p' < $1.xml`
              local d=`dirname $1`
              # And the possible list of names.
	      sed -n -e 's,^.*<refname>\(.*\)</refname>.*$,\1,p' < $1.xml \
	          | while read title ; do
                  # Need to generate explicit rules
                  cat <<EOF
man_MANS += ${d}/${title}.${n}
CLEANFILES += ${d}/${title}.${n}
${d}/${title}.${n}: $1.xml
	\$(SUBST_SED) < \$< > \$@.tmp
	\$(XMLTO) -o ${d} man \$@.tmp
	rm -f \$@.tmp
EOF
	      done
	  fi
	  ;;
  esac
}

echo_PROGRAMS ()
{
    case "$1" in
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
	echo "if DO_ARCH32_TEST"
	echo "ARCH32_COMPILE=\$(CC) \$(DEFAULT_INCLUDES) \$(INCLUDES) \
	      \$(AM_CPPFLAGS) \$(CPPFLAGS) \$(AM_CFLAGS)"
	echo "endif"
}

# usage:
#	echo_arch32_PROGRAMS ${name} ${file} $(COMPILE_CMD) ${arch32_cflag_name}
echo_arch32_PROGRAMS()
{
    case "$1" in
        *dir/* )
            # extract the directory prefix
            local dir=`echo /"$1" | sed -e 's,.*/\([a-z]*\)dir/.*,\1,'`

	    local dname=`dirname $1`
		
	    dname=`dirname ${dname}`
	    if [ "${dir}" = "pkglibexec" ] && [ "${dname}" = "frysk" ]; then
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
${dir_name}/arch32/${base_name}.\$(OBJEXT): \$(${name_}_SOURCES) frysk/pkglibexecdir/arch32/\$(am__dirstamp)
	@ARCH32_COMPILE=\`echo "\$(${compiler}) " | sed -e 's, -m64 , ,g'\`; \\
	\$\$ARCH32_COMPILE \$(${cflag}) -c -o \$@ $<

${dir_name}/arch32/${base_name}\$(EXEEXT): \$(${name_}_OBJECTS) \$(${name_}_DEPENDENCIES) ${dir_name}/arch32/\$(am__dirstamp)
	@rm -f \$@
	@ARCH32_LINK=\`echo "\$(LINK) " | sed -e 's, -m64 , ,g'\`; \\
	\$\$ARCH32_LINK \$(${name_}_LDFLAGS) \$(${name_}_OBJECTS) \$(${name_}_LDADD) \$(LIBS)
EOF
	
            	echo "${dir}_arch32_PROGRAMS += ${dir_name}/arch32/${base_name}"
		echo "MOSTLYCLEANFILES += ${dir_name}/arch32/${base_name}.\$(OBJEXT)"

		if grep pthread.h ${file} > /dev/null 2>&1 ; then
                    echo "${name_}_LDADD = -lpthread"
           	fi
		echo "endif"
		echo
	    fi
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
		*.java ) jv-scan --print-main $1 | grep .  > /dev/null 2>&1 ;;
        *.c|*.cxx ) grep -e '^main[( ]' -e ' main[( ]' $1 > /dev/null 2>&1 ;;
		* ) false ;; 
    esac
}


GEN_ARGS="${dirs} ${jars} ${JARS}"
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



cat <<EOF
EXTRA_DIST += common/Build.javain
${nodist_lib_sources} += ${GEN_SOURCENAME}/Build.java
${GEN_DIRNAME}.jar: ${GEN_SOURCENAME}/Build.java
BUILT_SOURCES += ${GEN_SOURCENAME}/Build.java
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

for suffix in .mkjava .shjava .mkenum .shenum .javain ; do
    print_header "... ${suffix}"
    SUFFIX=`echo ${suffix} | tr '[a-z.]' '[A-Z_]'`
    find ${dirs} -name "[A-Za-z]*${suffix}" -print \
	| sort -f | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	echo "EXTRA_DIST += ${file}"
	echo "${nodist_lib_sources} += ${d}/${b}.java"
	echo "BUILT_SOURCES += ${d}/${b}.java"
	case "${suffix}" in
	    *java ) echo "${d}/${b}.java: \$(MKJAVA)" ;;
	    *enum ) echo "${d}/${b}.java: \$(MKENUM)" ;;
	esac
	echo "${GEN_DIRNAME}.jar: ${d}/${b}.java"
    done
done

for suffix in .java ; do
    print_header "... ${suffix}"
    find ${dirs} -name "[A-Za-z]*${suffix}" -print \
	| sort -f | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	name=${d}/${b}
	# Skip when a generated file, happens when configured in
	# source tree - handled earlier.
	test -r "${d}/${b}.mkjava" && continue
	test -r "${d}/${b}.shjava" && continue
	test -r "${d}/${b}.mkenum" && continue
	test -r "${d}/${b}.shenum" && continue
	test -r "${d}/${b}.javain" && continue
	test -r "common/${b}.javain" && continue # too strong?
	test "${b}" = JUnitTests && continue # hack
	test -r "${d}/${b}.g" && continue
	test -r "${d}/${b}.sed" && continue
	echo "${GEN_DIRNAME}.jar: ${d}/${b}.java"
	if has_main ${file} ; then
	    name_=`echo_name_ ${name}`
	    echo_PROGRAMS ${name}
	    echo_MANS ${name}
	    echo "${name_}_SOURCES = ${file}"
	    echo "${name_}_LINK = \$(GCJLINK)"
	    echo_LDFLAGS ${name}
	    echo "${name_}_LDADD = \$(GEN_GCJ_LDADD)"
	else
	    echo "${sources} += ${file}"
	fi
    done || exit 1
done

# output the compile for arch32
echo_arch32_COMPILER

# the flag for output of arch32 test's CFLAGS
arch32_cflags_output=0

for suffix in .cxx .c .hxx ; do
    print_header "... ${suffix}"
    find ${dirs} \
	-name "[A-Za-z]*${suffix}" -print \
	| sort -f | while read file ; do
	d=`dirname ${file}`
	b=`basename ${file} ${suffix}`
	name=${d}/${b}
	name_=`echo ${name} | sed -e 'y,/-,__,'`
	if has_main ${file} ; then
	    echo "${name_}_SOURCES = ${file}"
	    test ${suffix} = .cxx && echo "${name_}_LINK = \$(CXXLINK)"
	    echo_PROGRAMS ${name}
	    echo_MANS ${name}
	    if grep pthread.h ${file} > /dev/null 2>&1 ; then
		echo "${name_}_LDADD = -lpthread"
	    fi

           # Generate the rules for arch32 test
           if [ $arch32_cflags_output -eq 0 ]; then
               echo "if DO_ARCH32_TEST"
               arch32_path="${d}/arch32"
               arch32_cflag_name=`echo ${arch32_path}_CFLAGS | sed -e 'y,/-,__,'`
               echo "${arch32_cflag_name}=-m32 -g"
               echo "endif"
           fi

           echo_arch32_PROGRAMS ${name} ${file} "ARCH32_COMPILE" ${arch32_cflag_name}
           arch32_cflags_output=1

	else
	    echo "${sources} += ${file}"
	fi
    done
done

#
# Generate rules for .S/.s assembly files
#
for suffix in .s .S ; do
    print_header "... ${suffix}"
    find ${dirs} \
        -name "[A-Za-z]*${suffix}" -print \
        | sort -f | while read file ; do
        d=`dirname ${file}`
        b=`basename ${file} ${suffix}`
        name=${d}/${b}
        name_=`echo ${name} | sed -e 'y,/-,__,'`
	
	echo "${name_}_SOURCES = ${file}"
	echo_PROGRAMS ${name}

	# Generate the rules for arch32 test
	if [ $arch32_cflags_output -eq 0 ]; then
	    echo "if DO_ARCH32_TEST"
	    arch32_path="${d}/arch32"
	    arch32_as_cflag_name=`echo ${arch32_path}_AS_CFLAGS | sed -e 'y,/-,__,'`
	    echo "${arch32_as_cflag_name}=-m32 -g"
	    echo "endif"
	fi

	echo_arch32_PROGRAMS ${name} ${file} "CCASCOMPILE" ${arch32_as_cflag_name}
	arch32_cflags_output=1
    done
done

# Grep the cni/*.cxx files forming a list of included files.  Assume
# these are all generated from .class files.  The list can be pruned a
# little since, given Class$Nested and Class, generating Class.h will
# automatically generate the inner Class$Nested class.

print_header "... *.cxx=.h"
find ${dirs} -name 'cni' -print | while read d
do
    find $d -name "[A-Za-z]*.cxx" -print
done \
    | xargs grep -H '#include ".*.h"' \
    | sed -e 's/\.cxx:#include "/.o /' -e 's/\.h".*$//' -e 's/$.*//' \
    | while read o h
do
  if test \
      -r ${h}.java -o \
      -r ${h}.shenum -o \
      -r ${h}.mkenum -o \
      -r ${h}.shjava -o \
      -r ${h}.mkjava \
      ; then
      echo ${o}: ${h}.h
      echo "CLEANFILES += ${h}.h"
      echo "CLEANFILES += ${h}\\\$\$*.h"
  fi
done | sort -u


# Form a list of all the .glade files, these are installed in
# PREFIX/share/PACKAGE/glade/.

print_header "... glade_DATA"
echo "gladedir = \$(pkgdatadir)/glade"
echo "glade_DATA ="
find ${dirs} -type f -name "[A-Za-z]*.glade" | while read file
do
  echo glade_DATA += ${file}
  echo EXTRA_DIST += ${file}
done

# Form a list of all the image files, these are installed in
# PREFIX/share/PACKAGE/images/.

# $1 - name of the images we're loading (i.e. image16, imageMACOSXicon, etc)
# $2 - The path to look in
find_images ()
{
   print_header "... ${1}_DATA"

   echo ${1}"dir = \$(pkgdatadir)/"${2}
   echo ${1}"_DATA ="

   find ${dirs} \
       -path "*/${2}/*" -prune \
       | while read file
   do
     if test -f ${file} ; then
	 echo ${1}"_DATA += "${file} 
	 echo EXTRA_DIST += ${file} 
     fi
   done
}

find_images "image" "images"
find_images "imageicon" "images/icon"
find_images "image16" "images/16"
find_images "image24" "images/24"
find_images "image32" "images/32"
find_images "image48" "images/48"
find_images "imageMACOSX" "images/__MACOSX"
find_images "imageMACOSXicon" "images/__MACOSX/icon"
find_images "imageMACOSX16" "images/__MACOSX/16"
find_images "imageMACOSX24" "images/__MACOSX/24"
find_images "imageMACOSX32" "images/__MACOSX/32"

# Form a list of all the .desktop files, these are installed in
# PREFIX/usr/share/applications

print_header "... desktop_DATA"
echo "desktopdir = \${prefix}/share/applications"
echo "desktop_DATA ="
find ${dirs} -type f -name "[A-Za-z]*.desktop" | while read file
do
  echo desktop_DATA += ${file}
  echo EXTRA_DIST += ${file}
done

print_header "... icon_DATA"
echo "icondir = \${prefix}/share/pixmaps"
echo "icon_DATA ="
find ${dirs} -type f -name 'fryskTrayIcon48.png' | while read file
do
  echo icon_DATA += ${file}
  echo EXTRA_DIST += ${file}
done

# Form a list of all the .properties files, these need to be copied over
# after install

print_header "... properties_DATA"
echo "propertydir = \$(pkgdatadir)"
echo "property_DATA ="
find ${dirs} -type f -name "[A-Za-z]*.properties" | while read file
do
  echo property_DATA += ${file}
  echo EXTRA_DIST += ${file}
done

# Form a list of all the .fig files, they need to be compiled into
# .jpg
print_header "... sample_DATA"
echo "sampledir = \$(pkgdatadir)/samples"
echo "sample_DATA ="
find ${dirs} -type f -name 'test*.cpp' | while read file
do
  echo sample_DATA += ${file}
  echo EXTRA_DIST += ${file}
done

# For all .fig files, add the corresponding .jpg file to what needs to
# be built as DATA.

print_header "... .fig.jpg:"
find ${dirs} -type f -name "[A-Za-z]*.fig" | while read f
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
find ${dirs} -type f -name "[A-Za-z]*.g" | while read g
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
find ${dirs} \
    -name "[^A-Za-z]*" -prune -o \
    -name 'TestCase.java' -prune -o \
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
