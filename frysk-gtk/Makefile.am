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

SUBDIRS = tlwidgets tests

EXTRA_DIST += EggTrayIcon/ChangeLog org/gnu/gtk/frysk/ChangeLog


GEN_CLASSPATH += ../frysk-imports/junit.jar 
GEN_CLASSPATH += ../frysk-imports/jargs.jar
GEN_CLASSPATH += ../frysk-imports/frysk-imports.jar
GEN_CLASSPATH += $(FRYSK_GTK_JARS)

GEN_GCJ_LDADD += ../frysk-imports/libfrysk-imports.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-junit.a 
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jargs.a 
GEN_GCJ_LDADD += $(FRYSK_GTK_LIBS)
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libdwfl/libdwfl.a
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libdw/libdw.a
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libebl/libebl.a
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libelf/libelf.a
if USE_LIBUNWIND
GEN_GCJ_LDADD += ../frysk-imports/libunwind/src/libunwind-ptrace.a \
		 ../frysk-imports/libunwind/src/libunwind-$(libunwind_cpu).a
endif
GEN_GCJ_LDADD += -lopcodes

# Hack, need to compile this entire sub-tree with JNI.
AM_GCJFLAGS += -fjni 
AM_CFLAGS += $(GTK_CFLAGS) $(FRYSK_GTK_CFLAGS)

solib_PROGRAMS += EggTrayIcon/libEggTrayIcon.so
EggTrayIcon/libEggTrayIcon.so: EggTrayIcon/libEggTrayIcon.a
EggTrayIcon_libEggTrayIcon_so_LDADD = $(FRYSK_GTK_LIBS)

noinst_LIBRARIES += EggTrayIcon/libEggTrayIcon.a 
EggTrayIcon_libEggTrayIcon_a_SOURCES = \
	EggTrayIcon/eggtrayicon.c \
	EggTrayIcon/libEggTrayIcon.c \
	EggTrayIcon/eggtrayicon.h
