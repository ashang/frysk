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
AC_INIT(frysk, 0.0)
AM_INIT_AUTOMAKE([subdir-objects foreign no-installinfo no-exeext])

sinclude(common/acinclude.m4)

export PKG_CONFIG_PATH=$PKG_CONFIG_PATH:/opt/lib/pkgconfig

dnl For EggTrayIcon
PKG_CHECK_MODULES(GTK, gtk+-2.0 >= 2.7.0)

PKG_CHECK_MODULES(GTK_JAVA, gtk2-java >= 2.7.0)
AC_MSG_CHECKING(for gtk2-java jar file)
GTK2_JAR=`$PKG_CONFIG --variable classpath gtk2-java`
AC_MSG_RESULT($GTK2_JAR)
AC_SUBST(GTK2_JAR)

PKG_CHECK_MODULES(GLADE_JAVA, glade-java >= 2.7.0)
AC_MSG_CHECKING(for glade-java jar file)
GLADE_JAR=`$PKG_CONFIG --variable classpath glade-java`
AC_MSG_RESULT($GLADE_JAR)
AC_SUBST(GLADE_JAR)

PKG_CHECK_MODULES(JGCOMMON, jg-common >= 0.1)
AC_MSG_CHECKING(for jg-common jar file)
JGCOMMON_JAR=`$PKG_CONFIG --variable classpath jg-common`
AC_MSG_RESULT($JGCOMMON_JAR)
AC_SUBST(JGCOMMON_JAR)


RPATH=-Wl,-rpath,`$PKG_CONFIG --libs-only-L gtk+-2.0 gtk2-java glade-java cairo-java jg-common| sed -e 's/ /:/' -e 's/-L//'  -e 's/:*$//'`
AC_SUBST(RPATH)
JAVA_LIBRARY_PATH=-Djava.library.path=$PWD/EggTrayIcon:`$PKG_CONFIG --libs-only-L gtk+-2.0 gtk2-java glade-java cairo-java jg-common| sed -e 's/ /:/' -e 's/-L//'  -e 's/:*$//'`
AC_SUBST(JAVA_LIBRARY_PATH)

AC_CONFIG_FILES([Makefile])
AC_CONFIG_SUBDIRS([frysk-sys frysk-core])
AC_OUTPUT
