# This file is part of FRYSK.
#
# Copyright 2005, Red Hat Inc.
#
# FRYSK is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# FRYSK is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with FRYSK; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

# Initialize everything to empty and then use += to add things in.

SUFFIXES =
BUILT_SOURCES =
CLEANFILES = $(BUILT_SOURCES)
noinst_PROGRAMS =
bin_PROGRAMS =
noinst_LIBRARIES =
LDADD = 
check_SCRIPTS =
TESTS =

# Modify the link rules so the program <dir>/<prog> is built with
# <dir>.<prog>.main() as the Java entry point.

AM_LDFLAGS = \
	--main=$$( \
		b=$$(basename $@ $(EXEEXT)) ; \
		d=$$(dirname $@ | tr '[/]' '[.]') ; \
		if test x"$$d" = x. ; then \
			main=$$b.$$b ; \
		else \
			main=$$d.$$b ; \
		fi ; \
		echo main=$$main 1>&2 ; \
		echo $$main )


# XXX: GCJ: I think there should be a -Wextraneous-throws; I think
# -Wno-deprecated is backward; I think -Wredundant-modifiers is broken
# with gcc4.  It appears to complain about abstract methods in a
# non-abstract class when the class actually is abstract.

AM_GCJFLAGS = \
	-I$(srcdir)/../frysk-sys \
	-I$(srcdir)/../frysk-sys/include \
	-I$(top_builddir)/../frysk-sys \
	-I$(srcdir) \
	-I$(top_builddir) \
	-Werror \
	-Wall \
	-fPIC \
	$(ZZZ)


AM_CXXFLAGS = \
	-I$(srcdir) \
	-I$(srcdir)/include \
	-I$(top_buildir) \
	-Werror \
	-Wall \
	-fpic \
	$(ZZZ)

frysk.core.jar: $(GEN_BUILT_CLASSES)
	cat $^ /dev/null \
		| sed -e '/^#/d' -e 's,\.,/,g' -e 's,$$,.class,' \
		| fastjar -@ -cf $@
noinst_PROGRAMS += frysk.core.jar



LDADD += libfrysk-core.a ../frysk-sys/libfrysk-sys.a
libfrysk_core_a_SOURCES = $(GEN_SOURCES)
noinst_LIBRARIES += libfrysk-core.a

libfrysk-core.so: libfrysk-core.a
noinst_PROGRAMS += libfrysk-core.so


prog_perf_clone_SOURCES = prog/perf/clone.c
prog_perf_clone_LDADD = -lpthread
noinst_PROGRAMS += prog/perf/clone
prog_perf_fork_SOURCES = prog/perf/fork.c
prog_perf_fork_LDADD = -lpthread
noinst_PROGRAMS += prog/perf/fork

prog_fib_clone_SOURCES = prog/fib/clone.c
prog_fib_clone_LDADD = -lpthread
prog_fib_fork_SOURCES = prog/fib/fork.c
noinst_PROGRAMS += \
	prog/fib/clone \
	prog/fib/fork \
	$(ZZZ)

prog_print_argv_SOURCES = prog/print/argv.c
prog_print_auxv_SOURCES = prog/print/auxv.c
prog_print_argc_SOURCES = prog/print/argc.c
prog_print_envp_SOURCES = prog/print/envp.c
noinst_PROGRAMS += \
	prog/print/argv \
	prog/print/auxv \
	prog/print/argc \
	prog/print/envp \
	$(ZZZ)

prog_step_infThreadLoop_SOURCES = prog/step/infThreadLoop.c
prog_step_infThreadLoop_LDADD = -lpthread
noinst_PROGRAMS += prog/step/infThreadLoop

prog_syscall_syscalls_SOURCES = prog/syscall/syscalls.c
prog_syscall_exec_SOURCES = prog/syscall/exec.c
prog_syscall_threadexec_SOURCES = prog/syscall/threadexec.c
prog_syscall_threadexec_LDADD = -lpthread
prog_syscall_syscallloop_SOURCES = prog/syscall/syscallloop.c
noinst_PROGRAMS += \
	prog/syscall/threadexec \
	prog/syscall/exec \
	prog/syscall/syscalls \
	prog/syscall/syscalls \
	prog/syscall/syscallloop

prog_terminated_infloop_SOURCES = prog/terminated/infloop.c
noinst_PROGRAMS += \
	prog/terminated/infloop

prog_x86isa_x86regs_SOURCES = prog/x86isa/x86regs.S
noinst_PROGRAMS += prog/x86isa/x86regs

prog_util_auxv_SOURCES = prog/util/auxv.java
prog_util_run_SOURCES = prog/util/run.java
noinst_PROGRAMS += \
	prog/util/auxv \
	prog/util/run

include common/Makefile.rules
include Makefile.gen
