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

GEN_CLASSPATH += $(top_builddir)/../frysk-gtk/frysk-gtk.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-core/frysk-core.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-sys/frysk-sys.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-imports/antlr.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-imports/frysk-imports.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-imports/jdom.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-imports/junit.jar 
GEN_CLASSPATH += $(top_builddir)/../frysk-imports/jargs.jar 
GEN_CLASSPATH += $(top_builddir)/../frysk-gtk/tlwidgets/ftk.jar
GEN_CLASSPATH += $(FRYSK_GUI_JARS) 
GEN_CLASSPATH += /usr/lib

GEN_GCJ_LDADD += ../frysk-gtk/libfrysk-gtk.a
GEN_GCJ_LDADD += ../frysk-core/libfrysk-core.a
GEN_GCJ_LDADD += ../frysk-sys/libfrysk-sys.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-imports.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-antlr.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jdom.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-cdtparser.a 
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-junit.a 
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jargs.a 
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-jline.a 
GEN_GCJ_LDADD += -L../frysk-imports/elfutils/libelf -lelf
GEN_GCJ_LDADD += -L../frysk-imports/elfutils/libdw -ldw
GEN_GCJ_LDADD += ../frysk-imports/elfutils/libdwfl/libdwfl.a
GEN_GCJ_LDADD += ../frysk-gtk/libfrysk-ftk.a
GEN_GCJ_LDADD += -L../frysk-gtk/EggTrayIcon
GEN_GCJ_LDADD += -L../frysk-gtk/tlwidgets
GEN_GCJ_LDADD += $(FRYSK_GUI_LIBS)
GEN_GCJ_LDADD += -lopcodes

# Skip tests (exit with status 77) when there is no display.
TESTS_ENVIRONMENT = ( test x"$$DISPLAY" != x || sh -c "exit 77" ) && 
