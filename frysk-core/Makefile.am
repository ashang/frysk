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

AUTOMAKE_OPTIONS = dejagnu

include frysk/expr/Makefile.inc

GEN_CLASSPATH += ../frysk-imports/$(GEN_CLASSDIR)
GEN_CLASSPATH += ../frysk-sys/$(GEN_CLASSDIR)
GEN_CLASSPATH += ../frysk-imports/jline.jar
GEN_CLASSPATH += ../frysk-imports/antlr.jar
GEN_CLASSPATH += ../frysk-imports/junit.jar
LDADD += ../frysk-imports/libfrysk-imports.a
LDADD += ../frysk-sys/libfrysk-sys.a
LDADD += ../frysk-imports/libjline.a
LDADD += ../frysk-imports/libantlr.a
LDADD += ../frysk-imports/libjunit.a

# XXX: This needs to be make less-architecture independant.

prog_x86isa_x86regs_SOURCES = prog/x86isa/x86regs.S
noinst_PROGRAMS += prog/x86isa/x86regs
prog_x86isa_x86modify_SOURCES = prog/x86isa/x86modify.S
noinst_PROGRAMS += prog/x86isa/x86modify

RUNTESTDEFAULTFLAGS = --srcdir $$srcdir/testsuite
