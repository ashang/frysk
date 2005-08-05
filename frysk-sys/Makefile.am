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

include common/Makefile.rules
include Makefile.gen

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
