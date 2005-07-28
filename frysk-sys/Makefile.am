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

# Initialize everything to empty and then use += to add things in.

SUFFIXES =
BUILT_SOURCES =
CLEANFILES = $(BUILT_SOURCES)
noinst_PROGRAMS =
bin_PROGRAMS =
noinst_LIBRARIES =
LDADD = 
check_SCRIPTS =
TESTS =

# Modify the link rules so the program <dir>/<prog> is built with
# <dir>.<prog>.main() as the Java entry point.

AM_LDFLAGS = \
	--main=$$( \
		b=$$(basename $@ $(EXEEXT)) ; \
		d=$$(dirname $@ | tr '[/]' '[.]') ; \
		if test x"$$d" = x. ; then \
			main=$$b.$$b ; \
		else \
			main=$$d.$$b ; \
		fi ; \
		echo main=$$main 1>&2 ; \
		echo $$main )


# XXX: GCJ: I think there should be a -Wextraneous-throws; I think
# -Wno-deprecated is backward; I think -Wredundant-modifiers is broken
# with gcc4.  It appears to complain about abstract methods in a
# non-abstract class when the class actually is abstract.

AM_GCJFLAGS = \
	-I$(srcdir) \
	-I$(top_builddir) \
	-Werror \
	-Wall \
	-fPIC \
	$(ZZZ)


AM_CXXFLAGS = \
	-I$(srcdir) \
	-I$(srcdir)/include \
	-I$(top_buildir) \
	-Werror \
	-Wall \
	-fpic \
	$(ZZZ)


frysk-sys.jar: $(GEN_BUILT_CLASSES)
	cat $^ /dev/null \
		| sed -e '/^#/d' -e 's,\.,/,g' -e 's,$$,.class,' \
		| fastjar -@ -cf $@
noinst_PROGRAMS += frysk-sys.jar

LDADD += libfrysk-sys.a
libfrysk_sys_a_SOURCES = $(GEN_SOURCES)
noinst_LIBRARIES += libfrysk-sys.a

libfrysk-sys.so: libfrysk-sys.a
noinst_PROGRAMS += libfrysk-sys.so

frysk-sys.db: libfrysk-sys.so frysk-sys.jar
	gcj-dbtool -n $@.tmp
	gcj-dbtool -a $@.tmp frysk-sys.jar libfrysk-sys.so
	mv $@.tmp $@
noinst_PROGRAMS += frysk-sys.db

include common/Makefile.rules
include Makefile.gen
