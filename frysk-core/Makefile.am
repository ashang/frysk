# This file is part of the program FRYSK.
#
# Copyright 2005, 2007, 2008, Red Hat Inc.
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


include common/Makefile.rules
include Makefile.gen

GEN_CLASSPATH += ../frysk-sys/frysk-sys.jar
GEN_CLASSPATH += ../frysk-imports/jline.jar
GEN_CLASSPATH += ../frysk-imports/antlr.jar
GEN_CLASSPATH += ../frysk-imports/junit.jar
GEN_CLASSPATH += ../frysk-imports/getopt.jar
GEN_CLASSPATH += ../frysk-imports/jdom.jar
GEN_CLASSPATH += ../frysk-imports/cdtparser.jar 
GEN_GCJ_LDADD_LIST += ../frysk-sys/libfrysk-sys.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-jline.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-antlr.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-junit.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-getopt.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-jdom.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/libfrysk-cdtparser.a 
GEN_GCJ_LDADD_LIST += ../frysk-imports/elfutils/libdwfl/libdwfl.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/elfutils/libdw/libdw.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/elfutils/libebl/libebl.a
GEN_GCJ_LDADD_LIST += ../frysk-imports/elfutils/libelf/libelf.a
if USE_LIBUNWIND
GEN_GCJ_LDADD_LIST += ../frysk-imports/libunwind-i386/src/.libs/libunwind-x86.a \
                 ../frysk-imports/libunwind-x86_64/src/.libs/libunwind-x86_64.a \
                 ../frysk-imports/libunwind-ppc32/src/.libs/libunwind-ppc32.a \
                 ../frysk-imports/libunwind-ppc64/src/.libs/libunwind-ppc64.a
endif
GEN_GCJ_LDADD_LIST += -lstdc++
# Stub bfd_getb32 and bfd_getl32 for PPC64.  Unconditionally
# link -lbfd_get just for simplification.
GEN_GCJ_LDADD_LIST += -laudit

# For TestExec.java
pkglib_PROGRAMS += frysk/pkglibdir/funit-exec-alias
frysk_pkglibdir_funit_exec_alias_SOURCES =
frysk/pkglibdir/funit-exec-alias: frysk/pkglibdir/funit-exec
	cp -p frysk/pkglibdir/funit-exec $@

# Shared library for inserting a breakpoint
frysk_pkglibdir_test1_LDADD = -lm

if DO_ARCH32_TEST
pkglib32_PROGRAMS += frysk/pkglibdir/arch32/funit-exec-alias
frysk_pkglibdir_arch32_funit_exec_alias_SOURCES =
frysk/pkglibdir/arch32/funit-exec-alias: frysk/pkglibdir/arch32/funit-exec
	cp -p frysk/pkglibdir/arch32/funit-exec $@
frysk_pkglibdir_arch32_test1_LDADD = -lm
endif

# Try running funit from the install directory; ignore things like
# program transform name for the moment.
installcheck-local: installcheck-funit
.PHONY: installcheck-funit
installcheck-funit:
	$(DESTDIR)$(pkglibdir)/funit

# Create executables for type tests for TestTypeFactory.java.
frysk/debuginfo/TestTypeFactory.java: frysk/pkglibdir/funit-type-entry.c

# Executable without debug_frame and without eh_frame for Frame tests.
# Note that this depends on both being available by default because
# Makefile.rules adds both -g and -fasynchronous-unwind-tables
# WARNING. eh_frames can only be removed if it isn't used during runtime
# of the test program (in particular c++ exceptions).

# funit-stacks
pkglib_PROGRAMS += frysk/pkglibdir/funit-stacks-nodebug
pkglib_PROGRAMS += frysk/pkglibdir/funit-stacks-noeh
frysk_pkglibdir_funit_stacks_nodebug_SOURCES =
frysk_pkglibdir_funit_stacks_noeh_SOURCES =
frysk/pkglibdir/funit-stacks-nodebug: frysk/pkglibdir/funit-stacks
	objcopy --remove-section=.debug_frame \
		frysk/pkglibdir/funit-stacks $@
frysk/pkglibdir/funit-stacks-noeh: frysk/pkglibdir/funit-stacks
	objcopy --remove-section=.eh_frame_hdr --remove-section=.eh_frame \
		frysk/pkglibdir/funit-stacks $@

# funit-empty-functions
pkglib_PROGRAMS += frysk/pkglibdir/funit-empty-functions-nodebug
pkglib_PROGRAMS += frysk/pkglibdir/funit-empty-functions-noeh
frysk_pkglibdir_funit_empty_functions_nodebug_SOURCES =
frysk_pkglibdir_funit_empty_functions_noeh_SOURCES =
frysk/pkglibdir/funit-empty-functions-nodebug: frysk/pkglibdir/funit-empty-functions
	objcopy --remove-section=.debug_frame \
		frysk/pkglibdir/funit-empty-functions $@
frysk/pkglibdir/funit-empty-functions-noeh: frysk/pkglibdir/funit-empty-functions
	objcopy --remove-section=.eh_frame_hdr --remove-section=.eh_frame \
		frysk/pkglibdir/funit-empty-functions $@

# funit-stacks-exit
pkglib_PROGRAMS += frysk/pkglibdir/funit-stacks-exit-nodebug
pkglib_PROGRAMS += frysk/pkglibdir/funit-stacks-exit-noeh
frysk_pkglibdir_funit_stacks_exit_nodebug_SOURCES =
frysk_pkglibdir_funit_stacks_exit_noeh_SOURCES =
frysk/pkglibdir/funit-stacks-exit-nodebug: frysk/pkglibdir/funit-stacks-exit
	objcopy --remove-section=.debug_frame \
		frysk/pkglibdir/funit-stacks-exit $@
frysk/pkglibdir/funit-stacks-exit-noeh: frysk/pkglibdir/funit-stacks-exit
	objcopy --remove-section=.eh_frame_hdr --remove-section=.eh_frame \
		frysk/pkglibdir/funit-stacks-exit $@

# funit-scopes
pkglib_PROGRAMS += frysk/pkglibdir/funit-scopes-nodebug
pkglib_PROGRAMS += frysk/pkglibdir/funit-scopes-noeh
frysk_pkglibdir_funit_scopes_nodebug_SOURCES =
frysk_pkglibdir_funit_scopes_noeh_SOURCES =
frysk/pkglibdir/funit-scopes-nodebug: frysk/pkglibdir/funit-scopes
	objcopy --remove-section=.debug_frame \
		frysk/pkglibdir/funit-scopes $@
frysk/pkglibdir/funit-scopes-noeh: frysk/pkglibdir/funit-scopes
	objcopy --remove-section=.eh_frame_hdr --remove-section=.eh_frame \
		frysk/pkglibdir/funit-scopes $@

# funit-scopes-workaround
pkglib_PROGRAMS += frysk/pkglibdir/funit-scopes-workaround-nodebug
pkglib_PROGRAMS += frysk/pkglibdir/funit-scopes-workaround-noeh
frysk_pkglibdir_funit_scopes_workaround_nodebug_SOURCES =
frysk_pkglibdir_funit_scopes_workaround_noeh_SOURCES =
frysk/pkglibdir/funit-scopes-workaround-nodebug: frysk/pkglibdir/funit-scopes-workaround
	objcopy --remove-section=.debug_frame \
		frysk/pkglibdir/funit-scopes-workaround $@
frysk/pkglibdir/funit-scopes-workaround-noeh: frysk/pkglibdir/funit-scopes-workaround
	objcopy --remove-section=.eh_frame_hdr --remove-section=.eh_frame \
		frysk/pkglibdir/funit-scopes-workaround $@

# funit-stack-inlined
pkglib_PROGRAMS += frysk/pkglibdir/funit-stack-inlined-nodebug
pkglib_PROGRAMS += frysk/pkglibdir/funit-stack-inlined-noeh
frysk_pkglibdir_funit_stack_inlined_nodebug_SOURCES =
frysk_pkglibdir_funit_stack_inlined_noeh_SOURCES =
frysk/pkglibdir/funit-stack-inlined-nodebug: frysk/pkglibdir/funit-stack-inlined
	objcopy --remove-section=.debug_frame \
		frysk/pkglibdir/funit-stack-inlined $@
frysk/pkglibdir/funit-stack-inlined-noeh: frysk/pkglibdir/funit-stack-inlined
	objcopy --remove-section=.eh_frame_hdr --remove-section=.eh_frame \
		frysk/pkglibdir/funit-stack-inlined $@

# funit-stacks-values
pkglib_PROGRAMS += frysk/pkglibdir/funit-stacks-values-nodebug
pkglib_PROGRAMS += frysk/pkglibdir/funit-stacks-values-noeh
frysk_pkglibdir_funit_stacks_values_nodebug_SOURCES =
frysk_pkglibdir_funit_stacks_values_noeh_SOURCES =
frysk/pkglibdir/funit-stacks-values-nodebug: frysk/pkglibdir/funit-stacks-values
	objcopy --remove-section=.debug_frame \
		frysk/pkglibdir/funit-stacks-values $@
frysk/pkglibdir/funit-stacks-values-noeh: frysk/pkglibdir/funit-stacks-values
	objcopy --remove-section=.eh_frame_hdr --remove-section=.eh_frame \
		frysk/pkglibdir/funit-stacks-values $@

# funit-stacks-linenum
pkglib_PROGRAMS += frysk/pkglibdir/funit-stacks-linenum-nodebug
pkglib_PROGRAMS += frysk/pkglibdir/funit-stacks-linenum-noeh
frysk_pkglibdir_funit_stacks_linenum_nodebug_SOURCES =
frysk_pkglibdir_funit_stacks_linenum_noeh_SOURCES =
frysk/pkglibdir/funit-stacks-linenum-nodebug: frysk/pkglibdir/funit-stacks-linenum
	objcopy --remove-section=.debug_frame \
		frysk/pkglibdir/funit-stacks-linenum $@
frysk/pkglibdir/funit-stacks-linenum-noeh: frysk/pkglibdir/funit-stacks-linenum
	objcopy --remove-section=.eh_frame_hdr --remove-section=.eh_frame \
		frysk/pkglibdir/funit-stacks-linenum $@

# funit-cpp-scopes-class
pkglib_PROGRAMS += frysk/pkglibdir/funit-cpp-scopes-class-nodebug
pkglib_PROGRAMS += frysk/pkglibdir/funit-cpp-scopes-class-noeh
frysk_pkglibdir_funit_cpp_scopes_class_nodebug_SOURCES =
frysk_pkglibdir_funit_cpp_scopes_class_noeh_SOURCES =
frysk/pkglibdir/funit-cpp-scopes-class-nodebug: frysk/pkglibdir/funit-cpp-scopes-class
	objcopy --remove-section=.debug_frame \
		frysk/pkglibdir/funit-cpp-scopes-class $@
frysk/pkglibdir/funit-cpp-scopes-class-noeh: frysk/pkglibdir/funit-cpp-scopes-class
	objcopy --remove-section=.eh_frame_hdr --remove-section=.eh_frame \
		frysk/pkglibdir/funit-cpp-scopes-class $@


# Create executables for symbol tests for TestSymbol.java.

pkglib_PROGRAMS += frysk/pkglibdir/funit-symbols-nodebug
frysk_pkglibdir_funit_symbols_nodebug_SOURCES =
frysk/pkglibdir/funit-symbols-nodebug: frysk/pkglibdir/funit-symbols
	strip --strip-debug -o $@ frysk/pkglibdir/funit-symbols


pkglib_PROGRAMS += frysk/pkglibdir/funit-symbols-stripped
frysk_pkglibdir_funit_symbols_stripped_SOURCES =
frysk/pkglibdir/funit-symbols-stripped: frysk/pkglibdir/funit-symbols
	strip -o $@ frysk/pkglibdir/funit-symbols

# Create executable for list tests for TestListCommand.java

frysk_pkglibdir_funit_quicksort_LINK = $(LINK) $(srcdir)/frysk/pkglibdir/funit-bubblesort.c -DNO_MAIN

# Create sysroot for TestSysRoot.java

all-local: frysk/pkglibdir/test-sysroot frysk/pkglibdir/arch32/test-sysroot
frysk/pkglibdir/test-sysroot: $(frysk_pkglibdir_funit_quicksort_SOURCES)
	mkdir -p $@/usr/src/debug
	cp $(abspath $(srcdir)/$(frysk_pkglibdir_funit_quicksort_SOURCES)) \
	   $@/usr/src/debug
	cp $(abspath $(srcdir)/$(frysk_pkglibdir_funit_bubblesort_SOURCES)) \
	   $@/usr/src/debug
	mkdir -p $@/usr/bin
	mkdir -p $@/usr/lib/debug/usr/bin
	mkdir -p $@/usr/lib/debug/usr/lib
	(cd $@/usr/src/debug; \
	 $(COMPILE) funit-bubblesort.c -fpic -shared -o ../../lib/libbubble.so -g; \
	 $(COMPILE) funit-quicksort.c -L ../../lib -lbubble  -g -o ../../bin/funit-quicksort -DNO_MAIN)
	/usr/lib/rpm/debugedit -b $(abspath $@) -d / $@/usr/bin/funit-quicksort
	/usr/lib/rpm/debugedit -b $(abspath $@) -d / $@/usr/lib/libbubble.so
	eu-strip --remove-comment -f $@/usr/lib/debug/usr/bin/funit-quicksort.debug $@/usr/bin/funit-quicksort; \
	eu-strip --remove-comment -f $@/usr/lib/debug/usr/lib/funit-bubblesort.debug $@/usr/lib/libbubble.so; 

install-exec-local: install-sysroot-local
uninstall-local: uninstall-sysroot-local
.PHONY: install-sysroot-local uninstall-sysroot-local
install-sysroot-local:
	cp -r frysk/pkglibdir/test-sysroot $(DESTDIR)$(pkglibdir)
if DO_ARCH32_TEST
	cp -r frysk/pkglibdir/arch32/test-sysroot $(DESTDIR)$(pkglib32dir)
endif
# we don't want this installed, but noinst_SCRIPTS has no effect
	rm -f $(libdir)/frysk/gen-type-funit-tests
uninstall-sysroot-local:
	rm -rf $(pkglibdir)/test-sysroot
	rm -rf $(pkglib32dir)/test-sysroot

if DO_ARCH32_TEST
pkglib32_PROGRAMS += frysk/pkglibdir/arch32/funit-symbols-nodebug
frysk_pkglibdir_arch32_funit_symbols_nodebug_SOURCES =
frysk/pkglibdir/arch32/funit-symbols-nodebug: frysk/pkglibdir/arch32/funit-symbols
	strip --strip-debug -o $@ frysk/pkglibdir/arch32/funit-symbols


pkglib32_PROGRAMS += frysk/pkglibdir/arch32/funit-symbols-stripped
frysk_pkglibdir_arch32_funit_symbols_stripped_SOURCES =
frysk/pkglibdir/arch32/funit-symbols-stripped: frysk/pkglibdir/arch32/funit-symbols
	strip -o $@ frysk/pkglibdir/arch32/funit-symbols

frysk_pkglibdir_arch32_funit_quicksort_LINK += $(srcdir)/frysk/pkglibdir/funit-bubblesort.c -DNO_MAIN

frysk/pkglibdir/arch32/test-sysroot: $(frysk_pkglibdir_arch32_funit_quicksort_SOURCES)
	mkdir -p $@/usr/src/debug
	cp $(abspath $(srcdir)/$(frysk_pkglibdir_arch32_funit_quicksort_SOURCES)) \
	   $@/usr/src/debug
	cp $(abspath $(srcdir)/$(frysk_pkglibdir_arch32_funit_bubblesort_SOURCES)) \
	   $@/usr/src/debug
	mkdir -p $@/usr/bin
	mkdir -p $@/usr/lib/debug/usr/bin
	mkdir -p $@/usr/lib/debug/usr/lib
	(cd $@/usr/src/debug; \
	 $(ARCH32_COMPILE) funit-bubblesort.c -fpic -shared -o ../../lib/libbubble.so -g; \
	 $(ARCH32_COMPILE) funit-quicksort.c -L ../../lib -lbubble  -g -o ../../bin/funit-quicksort -DNO_MAIN)
	/usr/lib/rpm/debugedit -b $(abspath $@) -d / $@/usr/bin/funit-quicksort
	/usr/lib/rpm/debugedit -b $(abspath $@) -d / $@/usr/lib/libbubble.so
	eu-strip --remove-comment -f $@/usr/lib/debug/usr/bin/funit-quicksort.debug $@/usr/bin/funit-quicksort; \
	eu-strip --remove-comment -f $@/usr/lib/debug/usr/lib/funit-bubblesort.debug $@/usr/lib/libbubble.so; 

endif

# frysk-core code needs to be able to find the core files found in
# frysk-sys/frysk/pkgddatadir/.  Since frysk-core can only see
# frysk-core/frysk/pkgdatadir/ add links to the unpacked core files in
# frysk-sys.

CLEANFILES += frysk/pkgdatadir/test-core-x8664
CLEANFILES += frysk/pkgdatadir/test-core-x86
noinst_DATA += frysk/pkgdatadir/test-core-x8664
noinst_DATA += frysk/pkgdatadir/test-core-x86

frysk/pkgdatadir/test-core-x8664:
	mkdir -p `dirname $@`
	ln -s -f @abs_top_builddir@/../frysk-sys/$@ $@
frysk/pkgdatadir/test-core-x86:
	mkdir -p `dirname $@`
	ln -s -f @abs_top_builddir@/../frysk-sys/$@ $@

#FunitSimpleInterfaceTest
pkglib_PROGRAMS += frysk/pkglibdir/FunitSimpleInterfaceTest
frysk/pkglibdir/FunitSimpleInterfaceTest:
	gcj -o $@ -g --main=FunitSimpleInterfaceTest $(srcdir)/frysk/pkglibdir/FunitSimpleInterfaceTest.java $(srcdir)/frysk/pkglibdir/FunitSimpleInterface.java
