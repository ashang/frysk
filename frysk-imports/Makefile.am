# This file is part of the program FRYSK.
#
# Copyright 2005, 2006, 2007, 2008, Red Hat Inc.
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


# See the comment above CHECK_SUBDIRS, only add libunwind to the tests
# if explicitly configured to do so.
if CHECK_LIBUNWIND
  LIBUNWIND_DIR = libunwind
else
  LIBUNWIND_DIR = 
endif # CHECK_LIBUNWIND
LIBUNWIND_ARCH_DIRS = libunwind-i386 libunwind-x86_64 libunwind-ppc32 libunwind-ppc64

# List of directories that should both be included in the distro and
# included in a recursive "make check".  Only the native architecture
# is included because for all cross architectures the library gets
# compiled with UNW_REMOTE_ONLY and so wouldn't be running the normal
# tests.
CHECK_SUBDIRS = \
	jline \
	junit \
	elfutils \
	getopt \
	tests \
	$(LIBUNWIND_DIR)

check-recursive: SUBDIRS = $(CHECK_SUBDIRS)

# List of directories to build.  This does not include "libunwind" but
# does include the unwind cross libraries.
SUBDIRS = $(CHECK_SUBDIRS) $(LIBUNWIND_ARCH_DIRS)

# List of directories to install, put simply, only install frysk
# stuff.  Will eventually need to change this so that it installs
# .so-s for any libraries that frysk uses; of course that assumes that
# frysk is dynamically linked.
install-data-recursive \
install-exec-recursive \
install-info-recursive \
install-recursive \
installcheck-recursive \
installdirs-recursive \
uninstall-info-recursive \
uninstall-recursive: SUBDIRS = tests

GEN_CLASSPATH += getopt.jar
GEN_CLASSPATH += junit.jar
GEN_GCJ_LDADD_LIST += libfrysk-getopt.a
GEN_GCJ_LDADD_LIST += libfrysk-junit.a
GEN_GCJ_LDADD_LIST += ./elfutils/libdwfl/libdwfl.a
GEN_GCJ_LDADD_LIST += ./elfutils/libdw/libdw.a
GEN_GCJ_LDADD_LIST += ./elfutils/libebl/libebl.a
GEN_GCJ_LDADD_LIST += ./elfutils/libelf/libelf.a
GEN_GCJ_LDADD_LIST += -lstdc++
GEN_GCJ_LDADD_LIST += ./libunwind-i386/src/.libs/libunwind-x86.a
GEN_GCJ_LDADD_LIST += ./libunwind-x86_64/src/.libs/libunwind-x86_64.a
GEN_GCJ_LDADD_LIST += ./libunwind-ppc32/src/.libs/libunwind-ppc32.a
GEN_GCJ_LDADD_LIST += ./libunwind-ppc64/src/.libs/libunwind-ppc64.a
# For auding of system calls.
GEN_GCJ_LDADD_LIST += -laudit

# Need to get this into the distribution
noinst_HEADERS = \
	include/ptrace_wait.h \
	include/frysk-asm.h \
	include/linux.ptrace.h \
	include/linux.syscall.h
