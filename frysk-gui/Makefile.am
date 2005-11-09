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

GEN_CLASSPATH += $(top_builddir)/../frysk-core/frysk-core.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-sys/frysk-sys.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-imports/antlr.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-imports/frysk-imports.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-imports/jdom.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-imports/cdtparser.jar 
GEN_CLASSPATH += $(JAVAGNOME_JAR) 
GEN_CLASSPATH += $(GTK2_JAR) 
GEN_CLASSPATH += $(GLADE_JAR) 
GEN_CLASSPATH += $(GLIB_JAVA_JAR) 
GEN_CLASSPATH += $(VTE_JAVA_JAR)
GEN_CLASSPATH += /usr/lib

# Hack, need to compile frysk.gui.monitor.EggTrayIcon.java with JNI.
AM_GCJFLAGS += -fjni 

GEN_GCJ_LDADD += ../frysk-core/libfrysk-core.a
GEN_GCJ_LDADD += ../frysk-sys/libfrysk-sys.a
GEN_GCJ_LDADD += ../frysk-imports/libfrysk-imports.a
GEN_GCJ_LDADD += ../frysk-imports/libantlr.a
GEN_GCJ_LDADD += ../frysk-imports/libjdom.a
GEN_GCJ_LDADD += ../frysk-imports/libcdtparser.a 
GEN_GCJ_LDADD += $(GLADE_JAVA_LIBS)
GEN_GCJ_LDADD += $(GTK_JAVA_LIBS)
GEN_GCJ_LDADD += -lgtkjni -lglibjni 
GEN_GCJ_LDADD += $(GLADE_JAVA_LIBS)
GEN_GCJ_LDADD += $(GNOME_JAVA_LIBS) 
GEN_GCJ_LDADD += $(CAIRO_JAVA_LIBS)
GEN_GCJ_LDADD += $(GLIB_JAVA_LIBS)
GEN_GCJ_LDADD += $(VTE_JAVA_LIBS)

LDFLAGS = -Djava.library.path=@RPATH@ -Wl,-rpath,@RPATH@

EXTRA_DIST += 	$(srcdir)/frysk/gui/glade/*.glade \
				$(srcdir)/frysk/gui/images/*.png \
				$(srcdir)/frysk/gui/images/*.jpg \
			 	$(srcdir)/frysk/gui/common/messages.properties \
				$(srcdir)/frysk/gui/srcwin/testfiles/*.cpp

