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


AC_PROG_RANLIB

# Use AC_CHECK_PROGS, and not AC_PATH_PROGS and AC_PATH_PROG:
# AC_PATH_PROGS, when given an environment containing a variable like
# <<JAVAC=/path/to/gcj -C>> looses the <<-C>>: AC_PATH_PROG, when
# given an environment containing a variable like <<JAVAC=gcj>> (i.e.,
# path not absolute) ignores it (which is contrary behavior to
# AC_PATH_PROGS).

AC_PROG_CXX([g++4 g++ c++])
test x"$CXXFLAGS" = "x-g -O2" && CXXFLAGS="-g -O"
AC_PROG_CC([gcc4 gcc cc])
AM_PROG_CC_C_O
test x"$CFLAGS" = "x-g -O2" && CFLAGS="-g -O"
AM_PROG_AS

# XXX: AM_PROG_GCJ doesn't take arguments, hack around it by first
# explictly searching for the GCJ program pushing it into the
# environment where AM_PROG_GCJ will find it.

AC_CHECK_PROGS([GCJ], [gcj4 gcj], [gcj])
AM_PROG_GCJ
test x"$GCJFLAGS" = "x-g -O2" && GCJFLAGS="-g -O"

# Find all the GCJ utilities.  Prefer gcc4.

AC_CHECK_PROGS([GCJH], [gcjh4 gcjh])
AC_CHECK_PROGS([JAR], [fastjar4 fastjar jar], [fastjar])
AC_CHECK_PROGS([GCJ_DBTOOL], [gcj-dbtool4 gcj-dbtool], [gcj-dbtool])
AC_CHECK_PROGS([JV_SCAN], [jv-scan4 jv-scan], [jv-scan])
AC_CHECK_PROGS([GIJ], [gij4 gij], [gij])

# Always use GCJ as the Java compiler.  While other, possibly better,
# alternatives might be available, using those alternatives can (and
# did) lead to problems such as: developers missing problems in the
# base-line build compiler; and developers testing code different to
# what will be shipped.  Having said that, do allow people to override
# this.

JAVAC="${JAVAC:-$GCJ -C}"
AC_SUBST([JAVAC])

# Only add -warn flags when the compiler is known to be ECJ.
AC_MSG_CHECKING([java flags])
case "${JAVAC}" in
gcj* | */gcj* ) JAVACFLAGS='-g -classpath $(SOURCEPATH):$(CLASSPATH)' ;;
ecj | */ecj ) JAVACFLAGS='-warn:+semicolon -sourcepath $(SOURCEPATH) -classpath $(CLASSPATH)' ;;
* ) JAVACFLAGS='-g -sourcepath $(SOURCEPATH) -classpath $(CLASSPATH)' ;;
esac
AC_SUBST([JAVACFLAGS])
AC_MSG_RESULT(${JAVACFLAGS})

# Always use GIJ as the byte code interpreter.  java programs, run
# during the build, need to see the GNU Java environment.  That
# environment classes such as gnu.gcj.RawData which can be found using
# reflection.

JAVA="${JAVA:-$GIJ}"
AC_SUBST([JAVA])

# Check for the ECJ compiler.  If available, enable rules that push
# all the java code through that compiler.  Doing this adds an
# additional (but optional) code check -- ECJ tends to find more
# errors than GCJ.

ECJ_JAR=${ECJ_JAR:-/usr/share/java/eclipse-ecj.jar}
AC_SUBST([ECJ_JAR])
AC_CHECK_PROGS([ECJ], [ecj], [no])
AM_CONDITIONAL(HAVE_ECJ, test x"${ECJ}" != xno)

# Check for the availablity of fig2dev

AC_PATH_PROG(FIG2DEV, fig2dev)
test "x$FIG2DEV" = x && AC_MSG_ERROR([no fig2dev binary is found in \$(PATH)])

AC_CHECK_PROGS(XMLTO, [xmlto], [no])
test "x${XMLTO}" = xno && AC_MSG_ERROR([no xmlto binary found in \${PATH)])

# Padd PKG_CONFIG_PATH with frysk's local directories.  Be careful to
# use lib|lib64.

lib=`pkg-config --debug 2>&1 |awk -F '/' '/^Scanning.*pkgconfig.$/ { print $(NF - 1); exit; }'`
export PKG_CONFIG_PATH=$PKG_CONFIG_PATH:/usr/$lib/frysk/pkgconfig

# Check for XMLLINT
AC_CHECK_PROGS([XMLLINT], [xmllint], [no])
test $XMLLINT = no && AC_ERROR([no xmllint program found])
AC_CHECK_PROGS([XMLCATALOG], [xmlcatalog], [no])
test $XMLCATALOG = no && AC_ERROR([no xmlcatalog program found])

# Check for uudecode
AC_CHECK_PROGS([UUDECODE], [uudecode], [no])
test $UUDECODE = no && AC_ERROR([no uudecode program found, please install sharutils package])
