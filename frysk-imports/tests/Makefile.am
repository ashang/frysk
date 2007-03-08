# This file is part of the program FRYSK.
#
# Copyright 2005, 2006, 2007 Red Hat Inc.
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
AM_CFLAGS = -Werror -Wall -I$(top_srcdir)/include
# Gag all the output from the failing tests
noinst_PROGRAMS =
noinst_SCRIPTS =
pkglib_SCRIPTS =
TESTS_ENVIRONMENT = eval > /dev/null 2>&1
XFAIL_TESTS =
PASS_TESTS =
TESTS = $(XFAIL_TESTS) $(PASS_TESTS)
all-local: $(TESTS)
$(TESTS): Makefile
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


# Install all passing systests tests so that they can be run on the
# install system.  This lets the frysk user test for system problems
# in a deployed installation.

# Tests are either PROGRAMS or SCRIPTS, accumulate each separatly.
# Can't assume that all PROGRAMS are TESTS and/or all TESTS are
# PROGRAMS, as some scripts come with programs that are not directly
# executed as tests.

systests_PASS_SCRIPT =
systests_PASS_PROGRAM =
systests_XFAIL_PROGRAM =
systests_XFAIL_SCRIPT =
PASS_TESTS += $(systests_PASS_SCRIPT) $(systests_PASS_PROGRAM)
XFAIL_TESTS += $(systests_XFAIL_PROGRAM) $(systests_XFAIL_SCRIPT)
pkglibsystests_PROGRAMS = $(systests_PASS_PROGRAM) $(systests_XFAIL_PROGRAM)
pkglibsystests_SCRIPTS = $(systests_PASS_SCRIPT) $(systests_XFAIL_SCRIPT)
pkglibsystestsdir = $(pkglibdir)/sys-tests

pkglibsystests_SCRIPTS += run-sys-tests.sh
CLEANFILES += run-sys-tests.sh
run-sys-tests.sh: Makefile
	rm -f $@.tmp
	touch $@.tmp
	echo >> $@.tmp '#!/bin/sh'
	echo >> $@.tmp 'ulimit -c 0'
	echo >> $@.tmp 'status=true'
	echo >> $@.tmp 'p="$(systests_PASS_PROGRAM) $(systests_PASS_SCRIPT)"'
	echo >> $@.tmp 'x="$(systests_XFAIL_PROGRAM) $(systests_XFAIL_SCRIPT)"'
	echo >> $@.tmp 'for s in p x'
	echo >> $@.tmp 'do'
	echo >> $@.tmp '  tests=$$(eval echo \$$$$s)'
	echo >> $@.tmp '  for test in $$tests'
	echo >> $@.tmp '  do'
	echo >> $@.tmp '    t=`basename $$test`'
	echo >> $@.tmp '    d=`dirname $$test`'
	echo >> $@.tmp '    ( test -d $$d && cd $$d ; ./$$t ) > /dev/null 2>&1'
	echo >> $@.tmp '    case $${s}:$$? in'
	echo >> $@.tmp '      p:0 ) echo PASS: $$test ;;'
	echo >> $@.tmp '      x:0 ) echo XPASS: $$test ;;'
	echo >> $@.tmp '      *:77 ) echo SKIP: $$test ;;'
	echo >> $@.tmp '      p:* ) echo FAIL: $$test ; status=false ;;'
	echo >> $@.tmp '      x:* ) echo XFAIL: $$test ;;'
	echo >> $@.tmp '    esac'
	echo >> $@.tmp '  done'
	echo >> $@.tmp 'done'
	echo >> $@.tmp '$$status'
	chmod a+x $@.tmp
	mv $@.tmp $@
# TESTS += run-sys-tests.sh # Disable - not informative enough

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
	$(GCJH) $(GCJHFLAGS) -I. frysk2490.Parent
	$(GCJH) $(GCJHFLAGS) -I. frysk2490.Child
	$(GCJH) $(GCJHFLAGS) -I. 'frysk2490.Child$$Nested'
	$(CXX) -g -I. -c -o frysk2490/native.o $(srcdir)/frysk2490/native.cxx
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

noinst_PROGRAMS += frysk2137/Gcov
frysk2137_Gcov_SOURCES = frysk2137/Gcov.java
frysk2137_Gcov_GCJFLAGS = --coverage
frysk2137_Gcov_LDFLAGS = --main=frysk2137.Gcov --coverage
TESTS += frysk2137/Gcov.sh
CLEANFILES += frysk2137/*.gcno frysk2137/*.gcda Gcov.java.gcov
frysk2137/Gcov.sh:
	rm -f $@.tmp Gcov.java.gcov frysk2137/*.gcda
	touch $@.tmp
	echo '#!/bin/sh -ex' >> $@.tmp
	echo './frysk2137/Gcov' >> $@.tmp
	echo 'gcov -o frysk2137/frysk2137_Gcov-Gcov.o frysk2137/Gcov.java' >> $@.tmp
	echo 'test -r Gcov.java.gcov' >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

# test for kernel ptrace - from a thread other than the attacher
frysk2595_ptrace_peek_wrong_thread_LDFLAGS = -lpthread
systests_XFAIL_PROGRAM += frysk2595/ptrace_peek_wrong_thread

# test for kernel ptrace - fork from thread, and ptrace after thread exit
frysk2595_ptrace_after_forked_thread_exits_LDFLAGS = -lpthread
systests_PASS_PROGRAM += frysk2595/ptrace_after_forked_thread_exits

# test for kernel ptrace - fork from thread, and ptrace before thread exit
frysk2595_ptrace_before_forked_thread_exits_LDFLAGS = -lpthread
systests_XFAIL_PROGRAM += frysk2595/ptrace_before_forked_thread_exits

# test for kernel ptrace - ptrace after main thread execs - YES
# THIS REALLY DOES WORK.
frysk2595_ptrace_after_exec_LDFLAGS = -lpthread
systests_PASS_PROGRAM += frysk2595/ptrace_after_exec

# test for kernel ptrace - ptrace after exec, child of other thread
frysk2595_ptrace_after_exec_of_thread_LDFLAGS = -lpthread
systests_PASS_PROGRAM += frysk2595/ptrace_after_exec_of_thread

process_TESTS = \
	process/single_exec \
	process/multi_child_exec \
	process/multi_parent_exec \
	process/multi_child_parent_exec
process_single_exec_LDFLAGS = -lpthread
process_multi_child_exec_LDFLAGS = -lpthread
process_multi_parent_exec_LDFLAGS = -lpthread
process_multi_child_parent_exec_LDFLAGS = -lpthread
systests_PASS_PROGRAM += $(process_TESTS)

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
	$(JAVAC) $(JAVAC_FLAGS) -d . \
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
pkglibsystests_PROGRAMS += frysk2130/threadexec
frysk2130_threadexec_LDFLAGS = -lpthread
systests_PASS_SCRIPT += frysk2130/strace-clone-exec.sh

# A garbage collector stress test.
noinst_PROGRAMS += frysk2299/GCTest
frysk2299_GCTest_SOURCES = frysk2299/GCTest.java
frysk2299_GCTest_LDFLAGS = --main=GCTest
TESTS += frysk2299/GCTest

# The order is important!!
frysk2670_FILES = \
	$(srcdir)/frysk2670/other/Action.java \
	$(srcdir)/frysk2670/one/Action.java \
	$(srcdir)/frysk2670/one/ExitNotificationObserver.java \
	$(srcdir)/frysk2670/one/AddTaskObserverAction.java \
	$(srcdir)/frysk2670/one/TaskAction.java
EXTRA_DIST += $(frysk2670_FILES)
XFAIL_TESTS += frysk2670/gcj.sh
frysk2670/gcj.sh:
	mkdir -p frysk2670
	rm -f $@
	echo '#!/bin/sh' >> $@
	echo mkdir -p frysk2670 >> $@
	echo 'rm -f frysk2670/*.class' >> $@
	echo 'echo ""' >> $@
	echo 'echo "====> Sorted list - compiles"' >> $@
	echo 'echo ""' >> $@
	echo '( set -xe ; $(GCJ) -d . -I . -I $(srcdir)/frysk2670 -C \
		$(sort $(frysk2670_FILES)) \
		) || exit 1' >> $@
	echo 'rm -f frysk2670/*.class' >> $@
	echo 'echo ""' >> $@
	echo 'echo "====> Unsorted list - dies"' >> $@
	echo 'echo ""' >> $@
	echo '( set -xe ; $(GCJ) -d . -I . -I $(srcdir)/frysk2670 -C \
		$(frysk2670_FILES) \
		) || exit 1' >> $@
	chmod a+x $@

# Frysk, when creating test processes uses the sequence vfork -> fork
# -> exec to create a process of known pid that has process 1 as its
# parent.
systests_PASS_PROGRAM += vfork-exec/vfork-exec

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
	echo "! $(GCJ) -I $(srcdir)/frysk2631 -d . $(srcdir)/frysk2631/ClassSwitch.java 2>&1 | grep 'internal compiler error' > /dev/null " >> $@.tmp
	chmod a+x $@.tmp
	mv $@.tmp $@

XFAIL_TESTS += frysk2760/barriertest
frysk2760_FILES = \
	frysk2760/TestBarrier.cxx \
	frysk2760/TestBarrier.java \
	frysk2760/linux.ptrace.h
EXTRA_DIST += $(frysk2760_FILES)
CLEANFILES += frysk2760/TestBarrier.h
frysk2760/barriertest: $(frysk2760_FILES)
	mkdir -p frysk2760
	$(GCJ) -I$(srcdir) -d . -C $(srcdir)/frysk2760/TestBarrier.java
	$(GCJH) $(GCJHFLAGS) -I. -o frysk2760/TestBarrier.h frysk2760/TestBarrier
	$(CXX) -g -I. -c -o frysk2760/barriertest.o $(srcdir)/frysk2760/TestBarrier.cxx
	$(GCJ) -g -I. --main=frysk2760.TestBarrier -o frysk2760/barriertest \
		$(srcdir)/frysk2760/TestBarrier.java \
		frysk2760/barriertest.o

TESTS += audit/test-audit-libs
audit_test_audit_libs_LDADD = -laudit
noinst_PROGRAMS += audit/test-audit-libs

# Failed with gcj 4.1 and earlier versions of 4.2
systests_XFAIL_PROGRAM += frysk3228/LoggerParent
frysk3228_LoggerParent_SOURCES = frysk3228/LoggerParent.java
frysk3228_LoggerParent_LDFLAGS = --main=LoggerParent

# The FC-6 kernel screws up signal delivery during ptrace attach,
# check this is fixed.
systests_PASS_PROGRAM += frysk3231/ptrace-detach-looses-signal

# Legacy ptrace inhibits some ptrace requests (PTRACE_DETACH, possibly
# PTRACE_CONT, and maybe others) if the accompanying signal is invalid.
# This test makes sure that behaviour hasn't been "fixed."
systests_PASS_PROGRAM += frysk3205/ptrace-detach-bad-signal

# A daemon can produce double or plain wrong exit events.
systests_PASS_PROGRAM += frysk3525/exit47

# A non-joined but exited thread should end up in state 'X'
frysk3491_x_state_LDFLAGS = -lpthread
systests_PASS_PROGRAM += frysk3491/x-state

# A hang during a brutal detach from a multi-threaded program.
frysk3595_detach_multi_thread_LDFLAGS = -lpthread
systests_PASS_PROGRAM += frysk3595/detach-multi-thread

# ptrace singlestep through a sigtrap handler should NOT reset it.
# On some kernels the handler is sometimes mysteriously cleared.
systests_PASS_PROGRAM += frysk3997/ptrace_step_sig

# Confirm that SIGCHLD remains around after waitpid events have been
# drained.
systests_PASS_PROGRAM += frysk3486/waitpid-masked-sigchld
