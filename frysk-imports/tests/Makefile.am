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

TESTS += cniinner/a.out
cniinner_FILES = \
	cniinner/native.cxx \
	cniinner/Parent.java \
	cniinner/Child.java
EXTRA_DIST += $(cniinner_FILES)
CLEANFILES += cniinner/Child.h cniinner/Parent.h
cniinner/a.out: $(cniinner_FILES)
	mkdir -p cniinner
	$(GCJ) -I$(srcdir) -d . -C $(srcdir)/cniinner/Parent.java
	$(GCJ) -I$(srcdir) -d . -C $(srcdir)/cniinner/Child.java
	$(GCJH) -I. cniinner.Parent
	$(GCJH) -I. cniinner.Child
	$(GCJH) -I. 'cniinner.Child$$Nested'
	$(GCJ) -g -I. -c -o cniinner/native.o $(srcdir)/cniinner/native.cxx
	$(GCJ) -g -I. --main=cniinner.Child -o cniinner/a.out \
		$(srcdir)/cniinner/Parent.java \
		$(srcdir)/cniinner/Child.java \
		cniinner/native.o

XFAIL_TESTS += samename/SameName
samename_SameName_SOURCES = samename/SameName.java
samename_SameName_LDFLAGS = --main=SameName
noinst_PROGRAMS += samename/SameName

XFAIL_TESTS += samename/WrongVariable
samename_WrongVariable_SOURCES = samename/WrongVariable.java
samename_WrongVariable_LDFLAGS = --main=samename.WrongVariable
noinst_PROGRAMS += samename/WrongVariable

XFAIL_TESTS += samename/WrongScope.gcj
EXTRA_DIST += samename/WrongScope.java
noinst_SCRIPTS += samename/WrongScope.gcj
samename/WrongScope.gcj:
	echo '#!/bin/sh' > $@
	echo \! $(GCJ) -C -d . $(srcdir)/samename/WrongScope.java >> $@
	chmod a+x $@

XFAIL_TESTS += packagename/O.out
packagename_O_SOURCES = packagename/O.java
packagename_O_LDFLAGS = --main=packagename.O
noinst_PROGRAMS += packagename/O
TESTS += packagename/C.out
packagename_C_SOURCES = packagename/C.java
packagename_C_LDFLAGS = --main=packagename.C
noinst_PROGRAMS += packagename/C

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

rh174912_FILES = rh174912/rh174912.java
EXTRA_DIST += $(rh174912_FILES)
TESTS += rh174912/gcj.sh
rh174912/gcj.sh: $(werror_FILES)
	mkdir -p rh174912
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -C -d . $(srcdir)/rh174912/rh174912.java >> $@.tmp
	echo $(GCJ) -c -I . -o rh174912/rh174912.o rh174912/rh174912.class >> $@.tmp
	echo $(GCJ) -c -I . -o \'rh174912/rh174912\$$1.o\' \'rh174912/rh174912\$$1.class\' >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

rh174912ice_FILES = rh174912ice/U.java rh174912ice/T.java
EXTRA_DIST += $(rh174912ice_FILES)
TESTS += rh174912ice/gcj.sh
rh174912ice/gcj.sh: $(werror_FILES)
	mkdir -p rh174912ice
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -C -I $(srcdir) -d . $(srcdir)/rh174912ice/T.java >> $@.tmp
	echo $(GCJ) -c -I $(srcdir) -I . rh174912ice/T.class >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

rh175569_FILES = rh175569/C.java
EXTRA_DIST += $(rh175569_FILES)
TESTS += rh175569/gcj.sh
rh175569/gcj.sh: $(werror_FILES)
	mkdir -p rh175569
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -C -d rh175569 $(srcdir)/rh175569/C.java >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

gcc8544_FILES = gcc8544/DisplayRenderer.java gcc8544/StyledText.java
EXTRA_DIST += $(gcc8544_FILES)
XFAIL_TESTS += gcc8544/gcj.sh
gcc8544/gcj.sh: $(gcc8544_FILES)
	mkdir -p gcc8544
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -C -d . $(srcdir)/gcc8544/DisplayRenderer.java $(srcdir)/gcc8544/StyledText.java >> $@.tmp
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
EXTRA_DIST += rh177240/strace-clone-exec.shin
noinst_PROGRAMS += rh177240/threadexec
noinst_SCRIPTS += rh177240/strace-clone-exec.sh
rh177240_threadexec_LDFLAGS = -lpthread
TESTS += rh177240/strace-clone-exec.sh

# A garbage collector stress test.
noinst_PROGRAMS += gctest/GCTest
gctest_GCTest_SOURCES = gctest/GCTest.java
gctest_GCTest_LDFLAGS = --main=GCTest
TESTS += gctest/GCTest

# Frysk, when creating test processes uses the sequence vfork -> fork
# -> exec to create a process of known pid that has process 1 as its
# parent.
TESTS += vfork-exec/vfork-exec
vfork_exec_vfork_exec_SOURCES = vfork-exec/vfork-exec.c
noinst_PROGRAMS += vfork-exec/vfork-exec
