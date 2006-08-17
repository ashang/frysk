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

include common/Makefile.rules
include Makefile.gen

CHECK_SUBDIRS = \
	jline \
	jargs \
	tests \
	cdtparser \
	junit \
	elfutils

SUBDIRS = $(CHECK_SUBDIRS)
	
# Add in libunwind only for certain arches
if USE_LIBUNWIND
SUBDIRS += libunwind
endif

# On certain platforms, "make check" in libunwind is disabled.
# (This is probably not portable, but we do not mind.)
if !CHECK_LIBUNWIND
check-recursive: SUBDIRS = $(CHECK_SUBDIRS)
endif

EXTRA_DIST += \
	frysk/ChangeLog \
	frysk/junit/ChangeLog \
	inua/ChangeLog \
	lib/dw/ChangeLog \
	lib/elf/ChangeLog \
	lib/opcodes/ChangeLog \
	lib/elf/tests/test-core.bz2.uu \
	lib/elf/tests/helloworld.o.bz2.uu

GEN_CLASSPATH += jargs.jar
GEN_CLASSPATH += junit.jar
GEN_GCJ_LDADD += libfrysk-jargs.a
GEN_GCJ_LDADD += libfrysk-junit.a
GEN_GCJ_LDADD += ./elfutils/libdwfl/libdwfl.a
GEN_GCJ_LDADD += ./elfutils/libdw/libdw.a
GEN_GCJ_LDADD += ./elfutils/libebl/libebl.a
GEN_GCJ_LDADD += ./elfutils/libelf/libelf.a

# Add in libunwind only for certain arches
if USE_LIBUNWIND
GEN_GCJ_LDADD += ./libunwind/src/libunwind-$(libunwind_cpu).a 
endif

# Stub bfd_getb32 and bfd_getl32 for PPC64.  Unconditionally 
# compile this code just for simplification.
noinst_LIBRARIES += lib/opcodes/libbfd_get.a
GEN_GCJ_LDADD += -lopcodes -L./lib/opcodes -lbfd_get

all-local: lib/elf/tests/test-core lib/elf/tests/helloworld.o

lib/elf/tests/test-core: lib/elf/tests/test-core.bz2.uu
	mkdir -p lib/elf/tests
	uudecode -o lib/elf/tests/test-core.bz2 $(srcdir)/lib/elf/tests/test-core.bz2.uu
	bzip2 -d < lib/elf/tests/test-core.bz2 > lib/elf/tests/test-core

lib/elf/tests/helloworld.o: lib/elf/tests/helloworld.o.bz2.uu
	mkdir -p lib/elf/tests
	uudecode -o lib/elf/tests/helloworld.o.bz2 $(srcdir)/lib/elf/tests/helloworld.o.bz2.uu
	bzip2 -d < lib/elf/tests/helloworld.o.bz2 > lib/elf/tests/helloworld.o

CLEANFILES += lib/elf/tests/test-core.bz2 lib/elf/tests/test-core
CLEANFILES += lib/elf/tests/helloworld.o.bz2 lib/elf/tests/helloworld.o


# we need to get this into the distribution
noinstdir = $(srcdir)
noinst_HEADERS = include/linux.ptrace.h include/linux.syscall.h
