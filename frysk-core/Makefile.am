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


include common/Makefile.rules
include Makefile.gen
