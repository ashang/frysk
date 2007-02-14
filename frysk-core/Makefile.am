# This file is part of the program FRYSK.
#
# Copyright 2005, Red Hat Inc.
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

EXTRA_DIST += \
	prog/ChangeLog \
	frysk/cli/ChangeLog \
	frysk/event/ChangeLog \
	frysk/proc/ChangeLog \
	frysk/util/ChangeLog \
	frysk/pkglibdir/ChangeLog \
	frysk/model/ChangeLog \
	frysk/value/ChangeLog \
	frysk/dom/ChangeLog \
	frysk/expr/ChangeLog


ANTLR = $(JAVA) -classpath ../frysk-imports/antlr.jar antlr.Tool

GEN_SOURCEPATH += ../frysk-imports/
GEN_SOURCEPATH += $(top_srcdir)/../frysk-imports/
GEN_CLASSPATH += ../frysk-imports/jline.jar
GEN_CLASSPATH += ../frysk-imports/antlr.jar
GEN_CLASSPATH += ../frysk-imports/junit.jar
GEN_CLASSPATH += ../frysk-imports/getopt.jar
GEN_CLASSPATH += ../frysk-imports/jdom.jar
GEN_CLASSPATH += ../frysk-imports/cdtparser.jar 
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-imports.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jline.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-antlr.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-junit.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-getopt.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jdom.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-cdtparser.a 
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libdwfl/libdwfl.a
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libdw/libdw.a
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libebl/libebl.a
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libelf/libelf.a
if USE_LIBUNWIND
GEN_GCJ_LDADD += ../frysk-imports/libunwind/src/libunwind-ptrace.a \
                ../frysk-imports/libunwind/src/libunwind-$(libunwind_cpu).a
endif
GEN_GCJ_LDADD += -lstdc++
# Stub bfd_getb32 and bfd_getl32 for PPC64.  Unconditionally
# link -lbfd_get just for simplification.
GEN_GCJ_LDADD += -lopcodes -laudit -L../frysk-imports/lib/opcodes -lbfd_get

# For TestExec.java
pkglib_PROGRAMS += frysk/pkglibdir/funit-child-alias
frysk_pkglibdir_funit_child_alias_SOURCES =
frysk/pkglibdir/funit-child-alias: frysk/pkglibdir/funit-child
	cp -p frysk/pkglibdir/funit-child $@

if DO_ARCH32_TEST
pkglib32_PROGRAMS += frysk/pkglibdir/arch32/funit-child-alias
frysk_pkglibdir_arch32_funit_child_alias_SOURCES =
frysk/pkglibdir/arch32/funit-child-alias: frysk/pkglibdir/arch32/funit-child
	cp -p frysk/pkglibdir/arch32/funit-child $@
endif

EXTRA_DIST += frysk/cli/hpd/doc-files/parsetree.png
EXTRA_DIST += frysk/pkglibdir/util.h

# Try running funit from the install directory; ignore things like
# program transform name for the moment.
installcheck-local: installcheck-funit
.PHONY: installcheck-funit
installcheck-funit:
	$(DESTDIR)$(pkglibdir)/funit

# Create executables for stackframe tests for TestStackFrame.java.
frysk/pkglibdir/funit-stackframe-nodebug: frysk/pkglibdir/funit-stackframe
	strip --strip-debug -o frysk/pkglibdir/funit-stackframe-nodebug frysk/pkglibdir/funit-stackframe

pkglib_PROGRAMS += frysk/pkglibdir/funit-stackframe-nodebug
frysk_pkglibdir_funit_stackframe_nodebug_SOURCES =

frysk/pkglibdir/funit-stackframe-stripped: frysk/pkglibdir/funit-stackframe
	strip -o frysk/pkglibdir/funit-stackframe-stripped frysk/pkglibdir/funit-stackframe

pkglib_PROGRAMS += frysk/pkglibdir/funit-stackframe-stripped
frysk_pkglibdir_funit_stackframe_stripped_SOURCES =

if DO_ARCH32_TEST
frysk/pkglibdir/arch32/funit-stackframe-nodebug: frysk/pkglibdir/arch32/funit-stackframe
	strip --strip-debug -o frysk/pkglibdir/arch32/funit-stackframe-nodebug frysk/pkglibdir/arch32/funit-stackframe

pkglib32_PROGRAMS += frysk/pkglibdir/arch32/funit-stackframe-nodebug
frysk_pkglibdir_arch32_funit_stackframe_nodebug_SOURCES =

frysk/pkglibdir/arch32/funit-stackframe-stripped: frysk/pkglibdir/arch32/funit-stackframe
	strip -o frysk/pkglibdir/arch32/funit-stackframe-stripped frysk/pkglibdir/arch32/funit-stackframe

pkglib32_PROGRAMS += frysk/pkglibdir/arch32/funit-stackframe-stripped
frysk_pkglibdir_arch32_funit_stackframe_stripped_SOURCES =
endif
