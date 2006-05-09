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

ANTLR = $(JAVA) -classpath ../frysk-imports/antlr.jar antlr.Tool

# Add dejagnu, remember to clean up.
AUTOMAKE_OPTIONS = dejagnu
RUNTESTDEFAULTFLAGS = --srcdir $$srcdir/testsuite --tool expr
CLEANFILES += *.sum *.log

GEN_SOURCEPATH += $(top_builddir)/../frysk-sys/
GEN_SOURCEPATH += $(top_srcdir)/../frysk-sys/
GEN_SOURCEPATH += $(top_builddir)/../frysk-imports/
GEN_SOURCEPATH += $(top_srcdir)/../frysk-imports/
GEN_CLASSPATH += ../frysk-imports/jline.jar
GEN_CLASSPATH += ../frysk-imports/antlr.jar
GEN_CLASSPATH += ../frysk-imports/junit.jar
GEN_CLASSPATH += ../frysk-imports/jargs.jar
GEN_CLASSPATH += ../frysk-imports/jdom.jar
GEN_GCJ_LDADD += ../frysk-sys/libfrysk-sys.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-imports.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jline.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-antlr.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-junit.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jargs.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jdom.a

# XXX: This needs to be make less-architecture independant.

frysk_pkglibexecdir_funit_ia32_regs_SOURCES = frysk/pkglibexecdir/funit-ia32-regs.S
pkglibexec_PROGRAMS += frysk/pkglibexecdir/funit-ia32-regs
frysk_pkglibexecdir_funit_ia32_modify_SOURCES = frysk/pkglibexecdir/funit-ia32-modify.S
pkglibexec_PROGRAMS += frysk/pkglibexecdir/funit-ia32-modify
# For TestExec.java
pkglibexec_PROGRAMS += frysk/pkglibexecdir/funit-child-alias
frysk_pkglibexecdir_funit_child_alias_SOURCES =
frysk_pkglibexecdir_funit_child_alias_LDADD = frysk/pkglibexecdir/funit-child-alias.o
frysk_pkglibexecdir_funit_child_alias_LDFLAGS = -pthread
frysk/pkglibexecdir/funit-child-alias.$(OBJEXT): $(frysk_pkglibexecdir_funit_child_SOURCES)
	$(COMPILE) -o $@ -c $^

MOSTLYCLEANFILES = frysk/pkglibexecdir/funit-child-alias.$(OBJEXT)
EXTRA_DIST +=  	$(srcdir)/frysk/cli/hpd/doc-files/parsetree.png
EXTRA_DIST += $(srcdir)/frysk/pkglibexecdir/util.h

# Try running funit from the install directory; ignore things like
# program transform name for the moment.
installcheck-local: installcheck-funit
.PHONY: installcheck-funit
installcheck-funit:
	$(DESTDIR)$(pkglibexecdir)/funit
