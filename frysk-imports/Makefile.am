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

# Force use of libjunit.a in this directory.
LIBJUNIT = libjunit.a

include common/Makefile.rules
include Makefile.gen

SUBDIRS = \
	jline \
	jargs \
	tests/cniinner \
	tests/anoncall \
	tests/werror

noinst_PROGRAMS += EggTrayIcon/libEggTrayIcon.so
EggTrayIcon/libEggTrayIcon.so: EggTrayIcon/libEggTrayIcon.a
EggTrayIcon_libEggTrayIcon_so_LDFLAGS  = $(GTK_JAVA_LIBS) $(GTK_LIBS) $(JGCOMMON_LIBS) -lgtkjni -ljgjni-0.1
EggTrayIcon_libEggTrayIcon_so_LDFLAGS += $(CAIRO_JAVA_LIBS)

noinst_LIBRARIES += EggTrayIcon/libEggTrayIcon.a 
EggTrayIcon_libEggTrayIcon_a_SOURCES = EggTrayIcon/eggtrayicon.c \
                                       EggTrayIcon/libEggTrayIcon.c 
EggTrayIcon_libEggTrayIcon_a_CFLAGS = -fPIC $(GTK_CFLAGS) $(JGCOMMON_CFLAGS)
