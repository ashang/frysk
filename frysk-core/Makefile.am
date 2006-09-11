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
	frysk/pkglibexecdir/ChangeLog \
	frysk/model/ChangeLog \
	frysk/lang/ChangeLog \
	frysk/dom/ChangeLog \
	frysk/expr/ChangeLog


ANTLR = $(JAVA) -classpath ../frysk-imports/antlr.jar antlr.Tool
# Prune files.list so these antlr generated files are not run through ecj
prune_ecj = -e CppParser.java -e CppTreeParser.java -e CppParserTokenTypes.java -e CppTreeParserTokenTypes.java -e CppLexer.java -e CLI.java -e PrintHandler.java -e SymTab.java 

# Add dejagnu, remember to clean up.
AUTOMAKE_OPTIONS = dejagnu
RUNTESTDEFAULTFLAGS = --srcdir $$srcdir/testsuite --tool expr
CLEANFILES += *.sum *.log

GEN_SOURCEPATH += ../frysk-sys/
GEN_SOURCEPATH += $(top_srcdir)/../frysk-sys/
GEN_SOURCEPATH += ../frysk-imports/
GEN_SOURCEPATH += $(top_srcdir)/../frysk-imports/
GEN_CLASSPATH += ../frysk-imports/jline.jar
GEN_CLASSPATH += ../frysk-imports/antlr.jar
GEN_CLASSPATH += ../frysk-imports/junit.jar
GEN_CLASSPATH += ../frysk-imports/jargs.jar
GEN_CLASSPATH += ../frysk-imports/jdom.jar
GEN_CLASSPATH += ../frysk-imports/cdtparser.jar 
GEN_GCJ_LDADD += ../frysk-sys/libfrysk-sys.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-imports.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jline.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-antlr.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-junit.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jargs.a
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
# Stub bfd_getb32 and bfd_getl32 for PPC64.  Unconditionally
# link -lbfd_get just for simplification.
GEN_GCJ_LDADD += -lopcodes -L../frysk-imports/lib/opcodes -lbfd_get

# XXX: This needs to be make less-architecture independant.

frysk_pkglibexecdir_funit_ia32_regs_SOURCES = frysk/pkglibexecdir/funit-ia32-regs.S
pkglibexec_PROGRAMS += frysk/pkglibexecdir/funit-ia32-regs
frysk_pkglibexecdir_funit_ia32_modify_SOURCES = frysk/pkglibexecdir/funit-ia32-modify.S
pkglibexec_PROGRAMS += frysk/pkglibexecdir/funit-ia32-modify
frysk_pkglibexecdir_funit_x8664_regs_SOURCES = frysk/pkglibexecdir/funit-x8664-regs.S
pkglibexec_PROGRAMS += frysk/pkglibexecdir/funit-x8664-regs
frysk_pkglibexecdir_funit_x8664_modify_SOURCES = frysk/pkglibexecdir/funit-x8664-modify.S
pkglibexec_PROGRAMS += frysk/pkglibexecdir/funit-x8664-modify

frysk_pkglibexecdir_funit_ppc64_regs_SOURCES = frysk/pkglibexecdir/funit-ppc64-regs.S
pkglibexec_PROGRAMS += frysk/pkglibexecdir/funit-ppc64-regs

# For TestExec.java
pkglibexec_PROGRAMS += frysk/pkglibexecdir/funit-child-alias
frysk_pkglibexecdir_funit_child_alias_SOURCES =
frysk_pkglibexecdir_funit_child_alias_LDADD = frysk/pkglibexecdir/funit-child-alias.o
frysk_pkglibexecdir_funit_child_alias_LDFLAGS = -pthread
frysk/pkglibexecdir/funit-child-alias.$(OBJEXT): $(frysk_pkglibexecdir_funit_child_SOURCES)
	$(COMPILE) -o $@ -c $^

MOSTLYCLEANFILES += frysk/pkglibexecdir/funit-child-alias.$(OBJEXT)
EXTRA_DIST += $(srcdir)/frysk/cli/hpd/doc-files/parsetree.png
EXTRA_DIST += $(srcdir)/frysk/pkglibexecdir/util.h
EXTRA_DIST += $(srcdir)/frysk/pkglibexecdir/funit-rt-looper.h

# Try running funit from the install directory; ignore things like
# program transform name for the moment.
installcheck-local: installcheck-funit
.PHONY: installcheck-funit
installcheck-funit:
	$(DESTDIR)$(pkglibexecdir)/funit
