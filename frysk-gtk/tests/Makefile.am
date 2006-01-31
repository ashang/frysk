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

TESTS += textiter/a.out
textiter_FILES = textiter/JNICrash.java
EXTRA_DIST += $(textiter_FILES)
textiter/a.out: $(textiter_FILES)
	mkdir -p textiter
	$(GCJ) -g \
		-classpath $(FRYSK_GTK_CLASSPATH) \
		$(FRYSK_GTK_LIBS) \
		-Wl,-rpath,/usr/lib/frysk \
		-Djava.library.path=/usr/lib/frysk \
		--main=textiter.JNICrash \
		-o textiter/a.out \
		$(srcdir)/textiter/JNICrash.java

gcjtreeiter_FILES = gcjtreeiter/IterTest.java
EXTRA_DIST += $(gcjtreeiter_FILES)
XFAIL_TESTS += gcjtreeiter/a.out
gcjtreeiter/a.out: $(gcjtreeiter_FILES)
	mkdir -p gcjtreeiter
	$(GCJ) -g \
		-classpath $(FRYSK_GTK_CLASSPATH) \
		$(FRYSK_GTK_LIBS) \
		-Wl,-rpath,/usr/lib/frysk \
		-Djava.library.path=/usr/lib/frysk \
		--main=gcjtreeiter.IterTest \
		$(srcdir)/gcjtreeiter/IterTest.java \
		-o gcjtreeiter/a.out
XFAIL_TESTS += gcjtreeiter/gij.sh
gcjtreeiter/gij.sh: $(gcjtreeiter_FILES)
	mkdir -p gcjtreeiter
	$(GCJ) -g -C -d . -I$(srcdir) \
		-classpath $(FRYSK_GTK_CLASSPATH) \
		$(srcdir)/gcjtreeiter/IterTest.java
	echo $(GIJ) -classpath .:$(FRYSK_GTK_CLASSPATH) \
		-Djava.library.path=/usr/lib/frysk \
		gcjtreeiter.IterTest \
		> gcjtreeiter/gij.sh
	chmod a+x gcjtreeiter/gij.sh
