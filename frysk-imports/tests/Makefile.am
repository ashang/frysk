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
# Gag all the output from the failing tests
TESTS_ENVIRONMENT = eval > log 2>&1
XFAIL_TESTS =
TESTS = $(XFAIL_TESTS)
all-local: $(TESTS)
$(TESTS) $(XFAIL_TESTS): Makefile
CLEANFILES = $(TESTS)
clean-local:
	rm -f */.class
	rm -f */*.o
	rm -f */a.out
	rm -f a.out

# This failed in 4.0 betas
logger_FILES = logger/Npe.java
TESTS += logger/a.out
EXTRA_DIST += $(logger_FILES)
logger/a.out: $(logger_FILES)
	mkdir -p logger
	$(GCJ) -o logger/a.out --main=Npe $(srcdir)/logger/Npe.java

# This fails on gcj 4, but passes on gcj 4.1.
anoncall_FILES = anoncall/Anon.java
TESTS += anoncall/a.out
EXTRA_DIST += $(anoncall_FILES)
anoncall/a.out: $(anoncall_FILES)
	mkdir -p anoncall
	$(GCJ) -o anoncall/a.out --main=Anon $(srcdir)/anoncall/Anon.java
# This fails on gcj 4, but passes on gcj 4.1.
nestcall_FILES = nestcall/Nest.java
XFAIL_TESTS += nestcall/a.out
EXTRA_DIST += $(nestcall_FILES)
nestcall/a.out: $(nestcall_FILES)
	mkdir -p nestcall
	$(GCJ) -o nestcall/a.out --main=Nest $(srcdir)/nestcall/Nest.java

TESTS += cniinner/a.out
cniinner_FILES = \
	cniinner/native.cxx \
	cniinner/Parent.java \
	cniinner/Child.java
EXTRA_DIST += $(cniinner_FILES)
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

XFAIL_TESTS += samename/a.out
samename_FILES = samename/SameName.java
EXTRA_DIST += $(samename_FILES)
samename/a.out: $(samename_FILES)
	mkdir -p samename
	$(GCJ) -g --main=SameName \
		$(srcdir)/samename/SameName.java \
		-o samename/a.out

XFAIL_TESTS += packagename/O.out
packagename_O_FILES = packagename/O.java
EXTRA_DIST += $(packagename_O_FILES)
packagename/O.out: $(packagename_O_FILES)
	mkdir -p packagename
	$(GCJ) -g --main=packagename.O \
		$(srcdir)/packagename/O.java \
		-o packagename/O.out
TESTS += packagename/C.out
packagename_C_FILES = packagename/C.java
EXTRA_DIST += $(packagename_C_FILES)
packagename/C.out: $(packagename_C_FILES)
	mkdir -p packagename
	$(GCJ) -g --main=packagename.C \
		$(srcdir)/packagename/C.java \
		-o packagename/C.out

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
XFAIL_TESTS += rh175569/gcj.sh
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


TESTS += process/single_exec
TESTS += process/multi_child_exec
TESTS += process/multi_parent_exec
TESTS += process/multi_child_parent_exec
process_FILES = process/single_exec.c \
	process/multi_child_exec.c \
	process/multi_parent_exec.c \
	process/multi_child_parent_exec.c 
EXTRA_DIST += $(process_FILES)
process/single_exec: $(process_FILES)
	mkdir -p process
	$(GCJ) $(srcdir)/process/single_exec.c -o process/single_exec
	$(GCJ) $(srcdir)/process/multi_child_exec.c -o process/multi_child_exec
	$(GCJ) $(srcdir)/process/multi_parent_exec.c -o process/multi_parent_exec
	$(GCJ) $(srcdir)/process/multi_child_parent_exec.c -o process/multi_child_parent_exec

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

#cdtparserversion_FILES = cdtparserversion/CDTParserTest.java
#cdtparserversion_FILES += cdtparserversion/main.cpp
#EXTRA_DIST += $(cdtparserversion_FILES)
#TESTS += cdtparserversion/a.out
#cdtparserversion/a.out: $(cdtparserversion_FILES)
#	mkdir -p cdtparserversion
#	$(GCJ) -g \
#		-I$(CDTPARSER_JAR) \
#		$(CDTPARSER_LIBS) \
#		--main=cdtparserversion.CDTParserTest \
#		$(srcdir)/cdtparserversion/CDTParserTest.java \
#		-o cdtparserversion/a.out
#TESTS += cdtparserversion/cdttest.sh
#cdtparserversion/cdttest.sh: cdtparserversion/a.out
#	mkdir -p cdtparserversion
#	echo "#!/bin/sh" > cdtparserversion/cdttest.sh
#	echo "cdtparserversion/a.out cdtparserversion/main.cpp > cdtparserversion/RESULT" \
#	>> cdtparserversion/cdttest.sh
#	echo "if [ ! \`cat cdtparserversion/RESULT\` == \"4,5\" ]; then" \
#	>> cdtparserversion/cdttest.sh
#	echo "FAIL" >> cdtparserversion/cdttest.sh
#	echo "fi" >> cdtparserversion/cdttest.sh
#	chmod a+x cdtparserversion/cdttest.sh
