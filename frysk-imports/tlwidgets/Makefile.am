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
tlwidgetsdst=$(top_builddir)/$(subdir)

all-local: $(tlwidgetsdst)/libftk.so $(tlwidgetsdst)/libftkjava.so $(tlwidgetsdst)/libftkjni.so $(tlwidgetsdst)/ftk.jar

EXTRA_DIST= $(tlwidgetssrc)/java/com/redhat/ftk/*.java \
            $(tlwidgetssrc)/jni/*.c \
            $(tlwidgetssrc)/widgets/*.c \
            $(tlwidgetssrc)/widgets/*.h \
            $(tlwidgetssrc)/widgets/ftk.pc

CLEANFILES = $(tlwidgetsdst)/*.so \
             $(tlwidgetsdst)/*.jar \
             $(tlwidgetsdst)/*.o \
             $(tlwidgetsdst)/*.class
SRC= $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.java

$(tlwidgetsdst)/com/redhat/ftk/Stripchart.class: $(SRC) 
	$(GCJ) -C -d $(tlwidgetsdst) \
	    -classpath  $(GTK2_JAR):$(GLIB_JAVA_JAR) $(SRC)

$(tlwidgetsdst)/ftk.jar: $(tlwidgetsdst)/com/redhat/ftk/Stripchart.class
	$(JAR) cf ftk.jar -C $(tlwidgetsdst) com/redhat/ftk/Stripchart.class

$(tlwidgetsdst)/Stripchart.o: $(tlwidgetssrc)/java/com/redhat/ftk/Stripchart.java
	$(GCJ) -fjni -I$(GTK2_JAR) -I $(GLIB_JAVA_JAR) -I ./src/java \
            -g -O2 -c $< -o $@

$(tlwidgetsdst)/libftkjava.so: $(tlwidgetsdst)/Stripchart.o
	$(GCJ) -shared $^ $(GLADE_JAVA_LIBS) \
		-Wl,-soname -Wl,libftkjava.so -o $@

$(tlwidgetsdst)/com_redhat_ftk_Stripchart.o: $(tlwidgetssrc)/jni/com_redhat_ftk_Stripchart.c
	$(CC) -c $< -I$(tlwidgetssrc)/widgets/ \
            $(GTK_CFLAGS) \
            $(GTK_JAVA_CFLAGS) \
            $(GLIB_JAVA_CFLAGS)\
	    `pkg-config --cflags atk` \
	    -fPIC -DPIC -o $@

$(tlwidgetsdst)/libftkjni.so: $(tlwidgetsdst)/com_redhat_ftk_Stripchart.o
	$(CC) -shared $^ $(GTK_JAVA_LIBS) -o $@

$(tlwidgetsdst)/ftkstripchart.o: $(tlwidgetssrc)/widgets/ftkstripchart.c  $(tlwidgetssrc)/widgets/ftkstripchart.h
	$(CC) $(CFLAGS) -std=gnu99 `pkg-config --cflags gtk+-2.0` -I$(tlwidgetssrc)/widgets -c -o $@ $<

$(tlwidgetsdst)/ftkconsole.o: $(tlwidgetssrc)/widgets/ftkconsole.c  $(tlwidgetssrc)/widgets/ftkconsole.h
	$(CC) $(CFLAGS) -std=gnu99 `pkg-config --cflags gtk+-2.0` -I$(tlwidgetssrc)/widgets -c -o $@ $<

$(tlwidgetsdst)/libftk.so: $(tlwidgetsdst)/ftkstripchart.o $(tlwidgetsdst)/ftkconsole.o
	$(CC) -shared -o $@ $^ -lrt

