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


AC_PROG_RANLIB

AC_PROG_CXX
test x"$CXXFLAGS" = "x-g -O2" && CXXFLAGS="-g -O"

AM_PROG_GCJ
test x"$GCJFLAGS" = "x-g -O2" && GCJFLAGS="-g -O"

AC_CHECK_PROGS(JAVAC, javac)
test "x$JAVAC" = x && AC_MSG_ERROR([no acceptable Java compiler found in \$(PATH)])

AC_PROG_CC
AM_PROG_CC_C_O
test x"$CFLAGS" = "x-g -O2" && CFLAGS="-g -O"

AM_PROG_AS
