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

tlwidgetssrc=$(srcdir)/src

solibdir = $(libdir)
solib_PROGRAMS = libftk.so libftkjava.so libftkjni.so
libftk_so_SOURCES =
libftkjava_so_SOURCES =
libftkjni_so_SOURCES =

javadir = $(datadir)/java
java_DATA = ftk.jar

EXTRA_DIST= $(tlwidgetssrc)/java/com/redhat/ftk/*.java \
            $(tlwidgetssrc)/jni/*.c \
            $(tlwidgetssrc)/widgets/*.c \
            $(tlwidgetssrc)/widgets/*.h \
            $(tlwidgetssrc)/widgets/ftk.pc

CLEANFILES = *.so *.jar *.o *.class
SRC= $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.java
CFLAGS += -fPIC -DPIC -std=gnu99

com/redhat/ftk/Stripchart.class: $(SRC) 
	$(GCJ) -C -d . -classpath $(FRYSK_GTK_CLASSPATH) $(SRC)

ftk.jar: com/redhat/ftk/Stripchart.class
	$(JAR) cf ftk.jar -C . com/redhat/ftk/Stripchart.class

Stripchart.o: $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.java
	$(GCJ) -fjni -classpath $(FRYSK_GTK_CLASSPATH) -I ./src/java \
            -g -O2 -fPIC -c $< -o $@

libftkjava.so: Stripchart.o
	$(GCJ) -shared $^ $(FRYSK_GTK_LIBS) \
		-Wl,-soname -Wl,libftkjava.so -o $@

com_redhat_ftk_Stripchart.o: $(tlwidgetssrc)/jni/com_redhat_ftk_Stripchart.c
	$(CC) -c $< -I$(tlwidgetssrc)/widgets/ $(FRYSK_GTK_CFLAGS) -fPIC -DPIC -o $@

libftkjni.so: com_redhat_ftk_Stripchart.o
	$(CC) -shared $^ $(FRYSK_GTK_LIBS) -o $@

ftkstripchart.o: $(tlwidgetssrc)/widgets/ftkstripchart.c  $(tlwidgetssrc)/widgets/ftkstripchart.h
	$(CC) $(CFLAGS) $(FRYSK_GTK_CFLAGS) -I$(tlwidgetssrc)/widgets -c -o $@ $<

ftkconsole.o: $(tlwidgetssrc)/widgets/ftkconsole.c  $(tlwidgetssrc)/widgets/ftkconsole.h
	$(CC) $(CFLAGS) $(FRYSK_GTK_CFLAGS) -I$(tlwidgetssrc)/widgets -c -o $@ $<

libftk.so: ftkstripchart.o ftkconsole.o
	$(CC) -shared -o $@ $^ -lrt
