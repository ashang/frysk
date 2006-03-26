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

EXTRA_DIST =
AM_CFLAGS = -Werror -Wall
# Gag all the output from the failing tests
noinst_PROGRAMS =
noinst_SCRIPTS =
TESTS_ENVIRONMENT = eval > /dev/null 2>&1
XFAIL_TESTS =
TESTS = $(XFAIL_TESTS)
all-local: $(TESTS)
$(TESTS) $(XFAIL_TESTS): Makefile
CLEANFILES = $(TESTS) $(noinst_SCRIPTS)
.shin.sh:
	cp $< $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@
clean-local:
	find * -name '*.class' -print | xargs rm -f
	find * -name '*.o' -print | xargs rm -f
	find * -name '*$$*.h' -print | xargs rm -f
	find * -name 'a.out' -print | xargs rm -f
check-local:
	@echo ''
	@echo 'vvvvvvvvvvvvvvvv PLEASE NOTE THE FOLLOWING vvvvvvvvvvvvvvvvv'
	@$(GCJ) --version
	@uname -a
	@echo '^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^'
	@echo ''

# This failed in GCC 4.0 beta
logger_Npe_SOURCES = logger/Npe.java
logger_Npe_LDFLAGS = --main=Npe
noinst_PROGRAMS += logger/Npe
TESTS += logger/Npe

# This failed in GCJ 4, but passes in GCJ 4.1.
anoncall_Anon_SOURCES = anoncall/Anon.java
anoncall_Anon_LDFLAGS = --main=Anon
TESTS += anoncall/Anon
noinst_PROGRAMS += anoncall/Anon

# This fails on gcj 4, but passes on gcj 4.1.
nestcall_Nest_SOURCES = nestcall/Nest.java
nestcall_Nest_LDFLAGS = --main=Nest
XFAIL_TESTS += nestcall/Nest
noinst_PROGRAMS += nestcall/Nest

TESTS += frysk1929/a.out
frysk1929_FILES = \
	frysk1929/native.cxx \
	frysk1929/Parent.java \
	frysk1929/Child.java
EXTRA_DIST += $(frysk1929_FILES)
CLEANFILES += frysk1929/Child.h frysk1929/Parent.h
frysk1929/a.out: $(frysk1929_FILES)
	mkdir -p frysk1929
	$(GCJ) -I$(srcdir) -d . -C $(srcdir)/frysk1929/Parent.java
	$(GCJ) -I$(srcdir) -d . -C $(srcdir)/frysk1929/Child.java
	$(GCJH) -I. frysk1929.Parent
	$(GCJH) -I. frysk1929.Child
	$(GCJH) -I. 'frysk1929.Child$$Nested'
	$(GCJ) -g -I. -c -o frysk1929/native.o $(srcdir)/frysk1929/native.cxx
	$(GCJ) -g -I. --main=frysk1929.Child -o frysk1929/a.out \
		$(srcdir)/frysk1929/Parent.java \
		$(srcdir)/frysk1929/Child.java \
		frysk1929/native.o

XFAIL_TESTS += frysk2270/SameName
frysk2270_SameName_SOURCES = frysk2270/SameName.java
frysk2270_SameName_LDFLAGS = --main=SameName
noinst_PROGRAMS += frysk2270/SameName

XFAIL_TESTS += frysk2270/WrongVariable
frysk2270_WrongVariable_SOURCES = frysk2270/WrongVariable.java
frysk2270_WrongVariable_LDFLAGS = --main=frysk2270.WrongVariable
noinst_PROGRAMS += frysk2270/WrongVariable

XFAIL_TESTS += frysk2270/WrongScope.gcj
EXTRA_DIST += frysk2270/WrongScope.java
noinst_SCRIPTS += frysk2270/WrongScope.gcj
frysk2270/WrongScope.gcj:
	echo '#!/bin/sh' > $@
	echo \! $(GCJ) -C -d . $(srcdir)/frysk2270/WrongScope.java >> $@
	chmod a+x $@

XFAIL_TESTS += frysk2105/O.out
frysk2105_O_SOURCES = frysk2105/O.java
frysk2105_O_LDFLAGS = --main=frysk2105.O
noinst_PROGRAMS += frysk2105/O
TESTS += frysk2105/C.out
frysk2105_C_SOURCES = frysk2105/C.java
frysk2105_C_LDFLAGS = --main=frysk2105.C
noinst_PROGRAMS += frysk2105/C

# Compiling Werror with -Werror -Wall should get a warning and hence
# abort the compilation.  At least GCC 4.0.1 20050727 (Red Hat
# 4.0.1-5) does not abort, and consequently this test is marked as a
# FAIL.  The build system contains hacks to get around this.
werror_FILES = werror/Werror.java werror/Werror.cxx
EXTRA_DIST += $(werror_FILES)
XFAIL_TESTS += werror/java.sh
werror/java.sh: $(werror_FILES)
	mkdir -p werror
	echo \! $(GCJ) -Werror -Wall -c -o /dev/null \
		$(srcdir)/werror/Werror.java \
		> $@
	chmod a+x $@
TESTS += werror/cxx.sh
werror/cxx.sh: $(werror_FILES)
	mkdir -p werror
	echo \! g++ -Werror -Wall -c -o /dev/null \
		$(srcdir)/werror/Werror.cxx \
		> $@
	chmod a+x $@

frysk1986_FILES = frysk1986/frysk1986.java
EXTRA_DIST += $(frysk1986_FILES)
TESTS += frysk1986/gcj.sh
frysk1986/gcj.sh: $(werror_FILES)
	mkdir -p frysk1986
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -C -d . $(srcdir)/frysk1986/rh174912.java >> $@.tmp
	echo $(GCJ) -c -I . -o frysk1986/rh174912.o \
		frysk1986/rh174912.class >> $@.tmp
	echo $(GCJ) -c -I . -o \'frysk1986/rh174912\$$1.o\' \
		\'frysk1986/rh174912\$$1.class\' >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

frysk1986ice_FILES = frysk1986ice/U.java frysk1986ice/T.java
EXTRA_DIST += $(frysk1986ice_FILES)
TESTS += frysk1986/ice.sh
frysk1986/ice.sh: $(werror_FILES)
	mkdir -p frysk1986
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -C -I $(srcdir) -d . $(srcdir)/frysk1986/T.java >> $@.tmp
	echo $(GCJ) -c -I $(srcdir) -I . frysk1986/T.class >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

frysk2041_FILES = frysk2041/C.java
EXTRA_DIST += $(frysk2041_FILES)
TESTS += frysk2041/gcj.sh
frysk2041/gcj.sh: $(werror_FILES)
	mkdir -p frysk2041
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -C -d frysk2041 $(srcdir)/frysk2041/C.java >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

frysk1840_FILES = frysk1840/DisplayRenderer.java frysk1840/StyledText.java
EXTRA_DIST += $(frysk1840_FILES)
XFAIL_TESTS += frysk1840/gcj.sh
frysk1840/gcj.sh: $(frysk1840_FILES)
	mkdir -p frysk1840
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -C -d . $(srcdir)/frysk1840/DisplayRenderer.java $(srcdir)/frysk1840/StyledText.java >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

frysk2488_FILES = frysk2488/Two.java
EXTRA_DIST += $(frysk2488_FILES)
TESTS += frysk2488/gcj.sh
frysk2488/gcj.sh: $(frysk2488_FILES)
	mkdir -p frysk2488
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -C -d . $(srcdir)/frysk2488/Two.java >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

frysk2137_FILES = frysk2137/Hello.java
EXTRA_DIST += $(frysk2137_FILES)
XFAIL_TESTS += frysk2137/gcj.sh
frysk2137/gcj.sh: $(frysk2137_FILES)
	mkdir -p frysk2137
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -fprofile-arcs -ftest-coverage -c -d . \
		$(srcdir)/frysk2137/Hello.java >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

process_TESTS = \
	process/single_exec \
	process/multi_child_exec \
	process/multi_parent_exec \
	process/multi_child_parent_exec
process_single_exec_LDFLAGS = -lpthread
process_multi_child_exec_LDFLAGS = -lpthread
process_multi_parent_exec_LDFLAGS = -lpthread
process_multi_child_parent_exec_LDFLAGS = -lpthread
TESTS += $(process_TESTS)
noinst_PROGRAMS += $(process_TESTS)

# GCJ complains that frysk.model.Signal isn't visible from
# frysk.event.SignalEvent, it should have imported frysk.event.Signal.
wrongclass_FILES = \
	wrongclass/frysk/model/Signal.java \
	wrongclass/frysk/event/Signal.java \
	wrongclass/frysk/event/SignalEvent.java \
	wrongclass/frysk/sys/Wait.java \
	wrongclass/frysk/proc/LinuxHost.java
EXTRA_DIST += $(wrongclass_FILES)
TESTS += wrongclass/gcj.sh
wrongclass/gcj.sh: $(wrongclass_FILES)
	mkdir -p wrongclass
	echo $(GCJ) -C \
		-I $(srcdir)/wrongclass \
		-d `pwd`/wrongclass \
		$(srcdir)/wrongclass/frysk/sys/Wait.java \
		$(srcdir)/wrongclass/frysk/model/Signal.java \
		$(srcdir)/wrongclass/frysk/event/Signal.java \
		$(srcdir)/wrongclass/frysk/event/SignalEvent.java \
		$(srcdir)/wrongclass/frysk/proc/LinuxHost.java \
		> $@
	chmod a+x $@

CDTPARSER_JAR = ../cdtparser/cdtparser.jar
cdtparserversion_FILES = cdtparserversion/CDTParserTest.java
cdtparserversion_FILES += cdtparserversion/main.cpp
EXTRA_DIST += $(cdtparserversion_FILES)
cdtparserversion/CDTParserTest.class: $(cdtparserversion_FILES) $(CDTPARSER_JAR)
	mkdir -p cdtparserversion
	$(JAVAC) -d . \
		-classpath $(CDTPARSER_JAR) \
		$(srcdir)/cdtparserversion/CDTParserTest.java
CLEANFILES += cdtparserversion/RESULT
TESTS += cdtparserversion/cdttest.sh
cdtparserversion/cdttest.sh: cdtparserversion/CDTParserTest.class $(CDTPARSER_JAR)
	mkdir -p cdtparserversion
	rm -f $@.tmp
	echo "#!/bin/sh" >> $@.tmp
	echo '$(JAVA) \' >> $@.tmp
	echo '  -classpath .:$(CDTPARSER_JAR) \' >> $@.tmp
	echo '  cdtparserversion.CDTParserTest \' >> $@.tmp
	echo '  $(srcdir)/cdtparserversion/main.cpp \' >> $@.tmp
	echo '  > cdtparserversion/RESULT' >> $@.tmp
	echo 'test `cat cdtparserversion/RESULT` == 4,5' >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@
$(CDTPARSER_JAR):
	cd `dirname $(CDTPARSER_JAR)` && $(MAKE)

# Test effect of argv on /proc/PID/cmdline (should not abort)
noinst_PROGRAMS += cmdline/dump cmdline/print
TESTS += cmdline/print

# Test an strace of a cloned exec.
EXTRA_DIST += frysk2130/strace-clone-exec.shin
noinst_PROGRAMS += frysk2130/threadexec
noinst_SCRIPTS += frysk2130/strace-clone-exec.sh
frysk2130_threadexec_LDFLAGS = -lpthread
TESTS += frysk2130/strace-clone-exec.sh

# A garbage collector stress test.
noinst_PROGRAMS += frysk2299/GCTest
frysk2299_GCTest_SOURCES = frysk2299/GCTest.java
frysk2299_GCTest_LDFLAGS = --main=GCTest
TESTS += frysk2299/GCTest

# Frysk, when creating test processes uses the sequence vfork -> fork
# -> exec to create a process of known pid that has process 1 as its
# parent.
TESTS += vfork-exec/vfork-exec
vfork_exec_vfork_exec_SOURCES = vfork-exec/vfork-exec.c
noinst_PROGRAMS += vfork-exec/vfork-exec
