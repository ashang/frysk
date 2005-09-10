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

AC_PREREQ(2.59)
AC_INIT(frysk-imports, 0.0)
AM_INIT_AUTOMAKE([subdir-objects foreign no-installinfo no-exeext])
AC_CONFIG_FILES([Makefile jline/Makefile jargs/Makefile tests/cniinner/Makefile tests/anoncall/Makefile tests/werror/Makefile])

sinclude(common/acinclude.m4)

AC_MSG_CHECKING([for junit.jar])
JUNIT_JAR=/usr/share/java/junit.jar
test -r ${JUNIT_JAR} || AC_MSG_ERROR([no junit.jar file found]);
AC_SUBST([JUNIT_JAR])
AC_MSG_RESULT([$JUNIT_JAR])

AC_MSG_CHECKING([for antlr.jar])
ANTLR_JAR=/usr/share/java/antlr.jar
test -r ${ANTLR_JAR} || AC_MSG_ERROR([no antlr.jar file found]);
AC_SUBST([ANTLR_JAR])
AC_MSG_RESULT([$ANTLR_JAR])

AC_OUTPUT
