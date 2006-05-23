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
CLEANFILES = \
	$(TESTS) \
	$(noinst_SCRIPTS) \
	*.gcno
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
frysk2493_Npe_SOURCES = frysk2493/Npe.java
frysk2493_Npe_LDFLAGS = --main=Npe
noinst_PROGRAMS += frysk2493/Npe
TESTS += frysk2493/Npe

# This failed in GCJ 4, but passes in GCJ 4.1.
frysk2491_Anon_SOURCES = frysk2491/Anon.java
frysk2491_Anon_LDFLAGS = --main=Anon
TESTS += frysk2491/Anon
noinst_PROGRAMS += frysk2491/Anon

frysk2491_Nest_SOURCES = frysk2491/Nest.java
frysk2491_Nest_LDFLAGS = --main=Nest
XFAIL_TESTS += frysk2491/Nest
noinst_PROGRAMS += frysk2491/Nest

TESTS += frysk2490/a.out
frysk2490_FILES = \
	frysk2490/native.cxx \
	frysk2490/Parent.java \
	frysk2490/Child.java
EXTRA_DIST += $(frysk2490_FILES)
CLEANFILES += frysk2490/Child.h frysk2490/Parent.h
frysk2490/a.out: $(frysk2490_FILES)
	mkdir -p frysk2490
	$(GCJ) -I$(srcdir) -d . -C $(srcdir)/frysk2490/Parent.java
	$(GCJ) -I$(srcdir) -d . -C $(srcdir)/frysk2490/Child.java
	$(GCJH) -I. frysk2490.Parent
	$(GCJH) -I. frysk2490.Child
	$(GCJH) -I. 'frysk2490.Child$$Nested'
	$(GCJ) -g -I. -c -o frysk2490/native.o $(srcdir)/frysk2490/native.cxx
	$(GCJ) -g -I. --main=frysk2490.Child -o frysk2490/a.out \
		$(srcdir)/frysk2490/Parent.java \
		$(srcdir)/frysk2490/Child.java \
		frysk2490/native.o

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

TESTS += frysk2105/O.out
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
frysk2492_FILES = frysk2492/Werror.java frysk2492/Werror.cxx
EXTRA_DIST += $(frysk2492_FILES)
XFAIL_TESTS += frysk2492/java.sh
frysk2492/java.sh:
	mkdir -p frysk2492
	echo \! $(GCJ) -Werror -Wall -c -o /dev/null \
		$(srcdir)/frysk2492/Werror.java \
		> $@
	chmod a+x $@
TESTS += frysk2492/cxx.sh
frysk2492/cxx.sh:
	mkdir -p frysk2492
	echo \! g++ -Werror -Wall -c -o /dev/null \
		$(srcdir)/frysk2492/Werror.cxx \
		> $@
	chmod a+x $@

frysk1986_FILES = \
	frysk1986/rh174912.java \
	frysk1986/U.java frysk1986/T.java
EXTRA_DIST += $(frysk1986_FILES)
TESTS += frysk1986/gcj.sh
frysk1986/gcj.sh:
	mkdir -p frysk1986
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -C -d . $(srcdir)/frysk1986/rh174912.java >> $@.tmp
	echo $(GCJ) -c -I . -o frysk1986/rh174912.o \
		frysk1986/rh174912.class >> $@.tmp
	echo $(GCJ) -c -I . -o \'frysk1986/rh174912\$$1.o\' \
		\'frysk1986/rh174912\$$1.class\' >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@
TESTS += frysk1986/ice.sh
frysk1986/ice.sh:
	mkdir -p `dirname $@`
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -C -I $(srcdir) -d . $(srcdir)/frysk1986/T.java >> $@.tmp
	echo $(GCJ) -c -I $(srcdir) -I . frysk1986/T.class >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

frysk2041_FILES = frysk2041/C.java
EXTRA_DIST += $(frysk2041_FILES)
TESTS += frysk2041/gcj.sh
frysk2041/gcj.sh:
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

# test for kernel ptrace - from a thread other than the attacher
noinst_PROGRAMS += frysk2595/ptrace_peek_wrong_thread
frysk2595_ptrace_peek_wrong_thread_LDFLAGS = -lpthread
TESTS += frysk2595/ptrace_peek_wrong_thread
frysk2595_ptrace_peek_wrong_thread_SOURCES = frysk2595/ptrace_peek_wrong_thread.c
# test for kernel ptrace - fork from thread, and ptrace after thread exit
noinst_PROGRAMS += frysk2595/ptrace_after_forked_thread_exits
frysk2595_ptrace_after_forked_thread_exits_LDFLAGS = -lpthread
TESTS += frysk2595/ptrace_after_forked_thread_exits
frysk2595_ptrace_after_forked_thread_exits_SOURCES = frysk2595/ptrace_after_forked_thread_exits.c
# test for kernel ptrace - fork from thread, and ptrace before thread exit
noinst_PROGRAMS += frysk2595/ptrace_before_forked_thread_exits
frysk2595_ptrace_before_forked_thread_exits_LDFLAGS = -lpthread
TESTS += frysk2595/ptrace_before_forked_thread_exits
frysk2595_ptrace_before_forked_thread_exits_SOURCES = frysk2595/ptrace_before_forked_thread_exits.c
# test for kernel ptrace - ptrace after main thread execs - YES
# THIS REALLY DOES WORK.
noinst_PROGRAMS += frysk2595/ptrace_after_exec
frysk2595_ptrace_after_exec_LDFLAGS = -lpthread
TESTS += frysk2595/ptrace_after_exec
frysk2595_ptrace_after_exec_SOURCES = frysk2595/ptrace_after_exec.c
# test for kernel ptrace - ptrace after exec, child of other thread
noinst_PROGRAMS += frysk2595/ptrace_after_exec_of_thread
frysk2595_ptrace_after_exec_of_thread_LDFLAGS = -lpthread
TESTS += frysk2595/ptrace_after_exec_of_thread
frysk2595_ptrace_after_exec_of_thread_SOURCES = frysk2595/ptrace_after_exec_of_thread.c

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
frysk1929_FILES = \
	frysk1929/frysk/model/Signal.java \
	frysk1929/frysk/event/Signal.java \
	frysk1929/frysk/event/SignalEvent.java \
	frysk1929/frysk/sys/Wait.java \
	frysk1929/frysk/proc/LinuxHost.java
EXTRA_DIST += $(frysk1929_FILES)
TESTS += frysk1929/gcj.sh
frysk1929/gcj.sh: $(frysk1929_FILES)
	mkdir -p frysk1929
	echo $(GCJ) -C \
		-I $(srcdir)/frysk1929 \
		-d `pwd`/frysk1929 \
		$(srcdir)/frysk1929/frysk/sys/Wait.java \
		$(srcdir)/frysk1929/frysk/model/Signal.java \
		$(srcdir)/frysk1929/frysk/event/Signal.java \
		$(srcdir)/frysk1929/frysk/event/SignalEvent.java \
		$(srcdir)/frysk1929/frysk/proc/LinuxHost.java \
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

frysk2670_TESTS = \
	frysk2670/AddTaskObserverAction.java \
	frysk2670/ExitNotificationObserver.java \
	frysk2670/TaskAction.java
EXTRA_DIST += $(frysk2670_FILES)
TESTS += frysk2670/gcj.sh
frysk2670/gcj.sh: $(frysk2670_FILES)
	mkdir -p frysk2670
	cp $(srcdir)/frysk2670/* frysk2670
	echo cd frysk2670 > $@
	echo gcj -C -classpath frysk.jar @./files0.list >> $@
	echo gcj -C -classpath frysk.jar @./files.list >> $@
	chmod a+x $@

# Frysk, when creating test processes uses the sequence vfork -> fork
# -> exec to create a process of known pid that has process 1 as its
# parent.
TESTS += vfork-exec/vfork-exec
vfork_exec_vfork_exec_SOURCES = vfork-exec/vfork-exec.c
noinst_PROGRAMS += vfork-exec/vfork-exec

# Indexing an array with a class pointer makes no sense but it compiles.
EXTRA_DIST += frysk2630/ClassIndex.java
XFAIL_TESTS += frysk2630/noerror.sh
frysk2630/noerror.sh:
	mkdir -p `dirname $@`
	echo '#!/bin/sh -xe' > $@.tmp
	echo '!'$(GCJ) -C -I $(srcdir) -d . $(srcdir)/frysk2630/ClassIndex.java >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

EXTRA_DIST += frysk2631/EnumClass.java frysk2631/ClassSwitch.java
XFAIL_TESTS += frysk2631/ice.sh
frysk2631/ice.sh:
	mkdir -p `dirname $@`
	echo '#!/bin/sh -xe' > $@.tmp
	echo $(GCJ) -I $(srcdir)/frysk2631 -d . $(srcdir)/frysk2631/ClassSwitch.java >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@
