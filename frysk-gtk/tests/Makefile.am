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

noinst_PROGRAMS =
EXTRA_DIST =
# If there's no display, exit with code 77 (SKIP); otherwize gag all
# the output from the failing tests
TESTS_ENVIRONMENT = \
	( test x"$$DISPLAY" != x || sh -c "exit 77" ) \
	&& eval > /dev/null 2>&1
XFAIL_TESTS =
TESTS = $(XFAIL_TESTS)
all-local: $(TESTS)
$(TESTS): Makefile
CLEANFILES = $(TESTS)
AM_GCJFLAGS = -classpath $(FRYSK_GTK_CLASSPATH)
AM_LDFLAGS = $(FRYSK_GTK_LIBS) \
	-Wl,-rpath,/usr/lib/frysk \
	-Djava.library.path=/usr/lib/frysk

TESTS += frysk1497/JNICrash
noinst_PROGRAMS += frysk1497/JNICrash
frysk1497_JNICrash_SOURCES = frysk1497/JNICrash.java
frysk1497_JNICrash_LDFLAGS = --main=frysk1497.JNICrash

TESTS += frysk1497/IterTest
noinst_PROGRAMS += frysk1497/IterTest
frysk1497_IterTest_SOURCES = frysk1497/IterTest.java
frysk1497_IterTest_LDFLAGS = --main=frysk1497.IterTest

TESTS += frysk1497/IterTest.gij
CLEANFILES += frysk1497/IterTest.class frysk1497/IterTest.gij
all-local: frysk1497/IterTest.class
frysk1497/IterTest.class: $(frysk1497_FILES)
	mkdir -p frysk1497
	$(GCJ) -g -C -d . -I$(srcdir) \
		-classpath $(FRYSK_GTK_CLASSPATH) \
		$(srcdir)/frysk1497/IterTest.java
frysk1497/IterTest.gij:
	mkdir -p frysk1497
	echo $(GIJ) -classpath .:$(FRYSK_GTK_CLASSPATH) \
		-Djava.library.path=/usr/lib/frysk \
		frysk1497.IterTest \
		> frysk1497/IterTest.gij
	chmod a+x frysk1497/IterTest.gij

TESTS += frysk2333/TestGlibTimerGC
noinst_PROGRAMS += frysk2333/TestGlibTimerGC
frysk2333_TestGlibTimerGC_SOURCES = frysk2333/TestGlibTimerGC.java
frysk2333_TestGlibTimerGC_LDFLAGS = --main=frysk2333.TestGlibTimerGC
