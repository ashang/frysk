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


GEN_CLASSPATH += $(top_builddir)/../frysk-core/frysk.core.jar
GEN_CLASSPATH += $(top_builddir)/../frysk-sys/frysk-sys.jar

GEN_CLASSPATH += $(JAVAGNOME_JAR) 
GEN_CLASSPATH += $(GTK2_JAR) 
GEN_CLASSPATH += $(GLADE_JAR) 
GEN_CLASSPATH += $(JGCOMMON_JAR) 
GEN_CLASSPATH += /usr/lib



noinst_PROGRAMS += EggTrayIcon/libEggTrayIcon.so
EggTrayIcon/libEggTrayIcon.so: EggTrayIcon/libEggTrayIcon.a

noinst_LIBRARIES += EggTrayIcon/libEggTrayIcon.a 
EggTrayIcon_libEggTrayIcon_a_SOURCES = EggTrayIcon/eggtrayicon.c \
                                       EggTrayIcon/libEggTrayIcon.c 
EggTrayIcon_libEggTrayIcon_a_LDFLAGS  = $(GTK_JAVA_LIBS) $(GTK_LIBS) $(JGCOMMON_LIBS) -ljgjni-0.1
EggTrayIcon_libEggTrayIcon_a_LDFLAGS += $(CAIRO_JAVA_LIBS)
EggTrayIcon_libEggTrayIcon_a_CFLAGS = -fPIC $(GTK_CFLAGS) $(JGCOMMON_CFLAGS)






LDADD += ../frysk-core/libfrysk-core.a ../frysk-sys/libfrysk-sys.a 
noinst_PROGRAMS += com/redhat/fedora/frysk/gui/Procpop

com_redhat_fedora_frysk_gui_Procpop_SOURCES = $(GEN_SOURCES)
com_redhat_fedora_frysk_gui_Procpop_CLASSPATH = $(GEN_CLASSPATH)
com_redhat_fedora_frysk_gui_Procpop_LDADD =  -L$(top_builddir)/EggTrayIcon -lEggTrayIcon 
com_redhat_fedora_frysk_gui_Procpop_LDADD += ../frysk-core/libfrysk-core.a ../frysk-sys/libfrysk-sys.a 
com_redhat_fedora_frysk_gui_Procpop_LDADD += $(GTK_JAVA_LIBS) -lgtkjni -ljgjni-0.1
com_redhat_fedora_frysk_gui_Procpop_LDADD += $(GLADE_JAVA_LIBS)
com_redhat_fedora_frysk_gui_Procpop_LDADD += $(GNOME_JAVA_LIBS) 
com_redhat_fedora_frysk_gui_Procpop_LDADD += $(CAIRO_JAVA_LIBS) 

GCJFLAGS += -fjni 
