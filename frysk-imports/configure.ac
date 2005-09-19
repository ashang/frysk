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

AC_PREREQ(2.59)
AC_INIT(frysk, 0.0)
AM_INIT_AUTOMAKE([subdir-objects foreign no-installinfo no-exeext])
AC_CONFIG_FILES([Makefile jline/Makefile jargs/Makefile tests/cniinner/Makefile tests/anoncall/Makefile tests/werror/Makefile])

sinclude(common/acinclude.m4)

AC_MSG_CHECKING([for junit.jar])
JUNIT_JAR=/usr/share/java/junit.jar
test -r ${JUNIT_JAR} || AC_MSG_ERROR([no junit.jar file found]);
AC_SUBST([JUNIT_JAR])
AC_MSG_RESULT([$JUNIT_JAR])

AC_MSG_CHECKING([for jdom.jar])
JDOM_JAR=/usr/share/java/jdom.jar
test -r ${JDOM_JAR} || AC_MSG_ERROR([no jdom.jar file found]);
AC_SUBST([JDOM_JAR])
AC_MSG_RESULT([$JDOM_JAR])

AC_MSG_CHECKING([for antlr.jar])
ANTLR_JAR=/usr/share/java/antlr.jar
test -r ${ANTLR_JAR} || AC_MSG_ERROR([no antlr.jar file found]);
AC_SUBST([ANTLR_JAR])
AC_MSG_RESULT([$ANTLR_JAR])

dnl For EggTrayIcon
export PKG_CONFIG_PATH=$PKG_CONFIG_PATH:/opt/lib/pkgconfig
PKG_CHECK_MODULES(GTK, gtk+-2.0 >= 2.7.0)
PKG_CHECK_MODULES(GLIB_JAVA, glib-java >= 0.2)

AC_OUTPUT
