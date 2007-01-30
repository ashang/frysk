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

tlwidgetssrc=$(srcdir)/src

solibdir = $(libdir)
solib_PROGRAMS = libftkjava.so
noinst_LIBRARIES =
libftkjava_so_SOURCES =

javadir = $(datadir)/java
java_DATA = ftk.jar

#EXTRA_DIST = $(SRC) \
#	src/widgets/ll.xpm \
#	src/widgets/rr.xpm \
#	src/widgets/ftkstripchart.h \
#	src/widgets/ftkstripchartx.h \
#	src/widgets/ftkeventviewer.h

EXTRA_DIST = $(SRC) \
	src/widgets/ftkeventviewer.h

CLEANFILES = *.so *.jar
#SRC= $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.java \
#     $(tlwidgetssrc)/java/com/redhat/ftk/StripchartX.java \
#     $(tlwidgetssrc)/java/com/redhat/ftk/EventViewer.java
SRC=  $(tlwidgetssrc)/java/com/redhat/ftk/SimultaneousEvent.java \
      $(tlwidgetssrc)/java/com/redhat/ftk/EventPair.java \
      $(tlwidgetssrc)/java/com/redhat/ftk/EventViewer.java \
      $(tlwidgetssrc)/java/com/redhat/ftk/CustomDrawingArea.java \
      $(tlwidgetssrc)/java/com/redhat/ftk/CustomAtkObject.java 

AM_CFLAGS = -fPIC -DPIC -std=gnu99 \
	-Wall -Werror \
	-I$(srcdir)/src/widgets \
	$(FRYSK_GTK_CFLAGS)

AM_CPPFLAGS = -I src/widgets -I $(srcdir)/src/widgets	

.a.so:
	soname=`basename $@` ; \
	$(CC) -shared -o $@.tmp \
		-Wl,--whole-archive,$<,--no-whole-archive \
		-Wl,--soname,$$soname,-z,-defs
	if readelf -d $@.tmp | fgrep -q TEXTREL; then exit 1; fi
	mv $@.tmp $@

#CLEANFILES += com/redhat/ftk/Stripchart.class \
#              com/redhat/ftk/StripchartX.class \
#              com/redhat/ftk/EventViewer.class
CLEANFILES +=  com/redhat/ftk/SimultaneousEvent.class \
	      com/redhat/ftk/EventPair.class \
	      com/redhat/ftk/EventViewer.class

#com/redhat/ftk/Stripchart.class: $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.java
#	$(GCJ) -C -d . -classpath $(FRYSK_GTK_CLASSPATH) $(SRC)
#com/redhat/ftk/StripchartX.class: $(tlwidgetssrc)/java/com/redhat/ftk/StripchartX.java
#	$(GCJ) -C -d . -classpath $(FRYSK_GTK_CLASSPATH) $(SRC)
#com/redhat/ftk/EventPair.class: $(tlwidgetssrc)/java/com/redhat/ftk/EventPair.java
#	$(GCJ) -C -d . -classpath $(FRYSK_GTK_CLASSPATH) $(SRC)
#com/redhat/ftk/SimultaneousEvent.class: $(tlwidgetssrc)/java/com/redhat/ftk/SimultaneousEvent.java
#	$(GCJ) -C -d . -classpath $(FRYSK_GTK_CLASSPATH) $(SRC)
#com/redhat/ftk/EventViewer.class: $(tlwidgetssrc)/java/com/redhat/ftk/EventViewer.java com/redhat/ftk/SimultaneousEvent.class com/redhat/ftk/EventPair.class
#	$(GCJ) -C -d . -classpath $(FRYSK_GTK_CLASSPATH) $(SRC)



#ftk.jar: com/redhat/ftk/Stripchart.class \
#         com/redhat/ftk/StripchartX.class \
#         com/redhat/ftk/EventViewer.class
#	$(JAR) cf ftk.jar -C . com/redhat/ftk/Stripchart.class \
#	                  -C . com/redhat/ftk/StripchartX.class \
#	                  -C . com/redhat/ftk/EventViewer.class

ftk.jar: $(tlwidgetssrc)/java/com/redhat/ftk/EventPair.java \
         $(tlwidgetssrc)/java/com/redhat/ftk/SimultaneousEvent.java \
         $(tlwidgetssrc)/java/com/redhat/ftk/EventViewer.java \
         $(tlwidgetssrc)/java/com/redhat/ftk/CustomDrawingArea.java \
         $(tlwidgetssrc)/java/com/redhat/ftk/CustomAtkObject.java 
	$(GCJ) -C -d . -classpath $(FRYSK_GTK_CLASSPATH) $(SRC)    
	$(JAR) cf ftk.jar -C . com/redhat/ftk/EventViewer.class \
			  -C . com/redhat/ftk/SimultaneousEvent.class \
			  -C . com/redhat/ftk/EventPair.class \
			  -C . com/redhat/ftk/CustomDrawingArea.class \
		          -C . com/redhat/ftk/CustomAtkObject.class

#Stripchart.o: $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.java
#	$(GCJ) -fjni -classpath $(FRYSK_GTK_CLASSPATH) -I ./src/java \
#            -g -O2 -fPIC -c $< -o $@

#StripchartX.o: $(tlwidgetssrc)/java/com/redhat/ftk/StripchartX.java
#	$(GCJ) -fjni -classpath $(FRYSK_GTK_CLASSPATH) -I ./src/java \
#            -g -O2 -fPIC -c $< -o $@

SimultaneousEvent.o: $(tlwidgetssrc)/java/com/redhat/ftk/SimultaneousEvent.java
	$(GCJ) -fjni -classpath $(FRYSK_GTK_CLASSPATH) -I $(srcdir)/src/java \
            -g -O2 -fPIC -c $< -o $@

EventPair.o: $(tlwidgetssrc)/java/com/redhat/ftk/EventPair.java
	$(GCJ) -fjni -classpath $(FRYSK_GTK_CLASSPATH) -I ./src/java \
            -g -O2 -fPIC -c $< -o $@

EventViewer.o: $(tlwidgetssrc)/java/com/redhat/ftk/EventViewer.java SimultaneousEvent.o EventPair.o
	$(GCJ) -fjni -classpath $(FRYSK_GTK_CLASSPATH) -I $(srcdir)/src/java -I ./ \
            -g -O2 -fPIC -c $< -o $@

#libftkjava.so: Stripchart.o StripchartX.o EventViewer.o SimultaneousEvent.o
#	$(GCJ) -shared $^ $(FRYSK_GTK_LIBS) \
#		-Wl,-soname -Wl,libftkjava.so -o $@

libftkjava.so:  SimultaneousEvent.o EventPair.o EventViewer.o
	$(GCJ) -shared $^ $(FRYSK_GTK_LIBS) \
		-Wl,-soname -Wl,libftkjava.so -o $@

#libftkjni_a_SOURCES = src/jni/com_redhat_ftk_Stripchart.c \
#                      src/jni/com_redhat_ftk_StripchartX.c \
#                      src/jni/com_redhat_ftk_EventViewer.c
libftkjni_a_SOURCES = src/jni/com_redhat_ftk_EventViewer.c \
                      src/jni/com_redhat_ftk_CustomDrawingArea.c \
                      src/jni/com_redhat_ftk_CustomAtkObject.c

noinst_LIBRARIES += libftkjni.a
libftkjni_so_SOURCES =
solib_PROGRAMS += libftkjni.so
libftkjni.so: libftkjni.a

#libftk_a_SOURCES = src/widgets/ftkstripchart.c \
#                   src/widgets/ftkstripchartx.c \
#                   src/widgets/ftkeventviewer.c
libftk_a_SOURCES = src/widgets/ftkeventviewer.c \
                   src/widgets/ftkcustomdrawingarea.c \
                   src/widgets/ftkcustomatkobject.c \
                   src/jni/com_redhat_ftk_CustomAtkObject.c \
                   src/widgets/ftk_marshaller.list
noinst_LIBRARIES += libftk.a
solib_PROGRAMS += libftk.so
libftk_so_SOURCES =
libftk.so: libftk.a
BUILT_SOURCES = src/widgets/ftk_marshaller.h

# Rules for the marshalers code:
GENMARSHAL_COMMAND = glib-genmarshal --prefix=_ftk_marshal

.list.h:
	$(GENMARSHAL_COMMAND) --header $< >$@

.list.c:
	(echo '/* This file has been automatically generated.  Do not edit. */' && \
	echo '#include "$*.h"' && \
	$(GENMARSHAL_COMMAND) --body $< ) >$@

# Fake rules for CONFIG_STATUS_DEPENDENCIES:
$(FRYSK_GTK_JARS):
	: no-op
