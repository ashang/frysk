define(pkg_major, 0)
define(pkg_minor, 99)
define(pkg_extra, -alpha)
define(pkg_maintainer, libunwind@linux.hpl.hp.com)
define(mkvers, $1.$2$3)
dnl Process this file with autoconf to produce a configure script.
AC_INIT(libunwind, mkvers(pkg_major, pkg_minor, pkg_extra), pkg_maintainer)
AC_CONFIG_SRCDIR(src/mi/backtrace.c)
AC_CONFIG_AUX_DIR(config)
AC_CANONICAL_SYSTEM

dnl Checks for programs.
AC_PROG_CC
AC_PROG_CXX
AC_PROG_INSTALL
AC_CHECK_TOOL(AR, ar)
AC_PROG_RANLIB
AC_PROG_LN_S
AM_PROG_MKDIR_P

dnl Checks for libraries.
AC_CHECK_LIB(uca, __uc_get_grs)
CHECK_ATOMIC_OPS

dnl Checks for header files.
AC_HEADER_STDC
AC_CHECK_HEADERS(asm/ptrace_offsets.h endian.h execinfo.h ia64intrin.h \
		 sys/uc_access.h unistd.h signal.h)

dnl Checks for typedefs, structures, and compiler characteristics.
AC_C_CONST
AC_C_INLINE
AC_TYPE_SIGNAL
AC_TYPE_SIZE_T

CPPFLAGS="${CPPFLAGS} -D_GNU_SOURCE"
CFLAGS="${CFLAGS} -DUNW_REMOTE_ONLY"

AC_ARG_ENABLE(debug,
[  --enable-debug          turn on debug support (slows down execution)],
[enable_debug=yes], [])

if test x$enable_debug = xyes; then
  CPPFLAGS="${CPPFLAGS} -DDEBUG"
else
  CPPFLAGS="${CPPFLAGS} -DNDEBUG"
fi

AC_CHECK_MEMBERS([struct dl_phdr_info.dlpi_subs],,,[#include <link.h>])
AC_CHECK_TYPES([sighandler_t], [], [],
[$ac_includes_default
#if HAVE_SIGNAL_H
# include <signal.h>
#endif
])

dnl Checks for library functions.
AC_FUNC_MEMCMP
AC_TYPE_SIGNAL
AC_CHECK_FUNCS(dl_iterate_phdr dl_phdr_removals_counter dlmodinfo getunwind \
		ttrace)

get_arch() {
 case "$1" in
  i?86) echo x86;;
  hppa*) echo hppa;;
  powerpc64*) echo ppc64;;
  powerpc*) echo ppc;;
  *) echo $1;;
 esac
}

get_os() {
  case "$1" in
   linux*) echo linux;;
   *) echo $1;;
  esac
}

build_arch=`get_arch $build_cpu`
target_arch=`get_arch $target_cpu`
target_os=`get_os $target_os`

if test x$target_arch != x$build_arch; then
  CPPFLAGS="${CPPFLAGS}"
fi
AC_CONFIG_LINKS(include/libunwind.h:include/libunwind-$target_arch.h
		include/tdep:include/tdep-$target_arch)

LIBUNWIND___THREAD

save_LDFLAGS="$LDFLAGS"
LDFLAGS="$LDFLAGS -static-libcxa"
AC_TRY_LINK([], [], [have_static_libcxa=yes])
LDFLAGS="$save_LDFLAGS"
if test "x$have_static_libcxa" = xyes; then
  LDFLAGS_STATIC_LIBCXA="-XCClinker -static-libcxa"
fi

AC_TRY_COMPILE([], [#ifndef __INTEL_COMPILER
#error choke me
#endif], [intel_compiler=yes])

if test x$GCC = xyes -a x$intel_compiler != xyes; then
  CFLAGS="${CFLAGS} -fexceptions -Wall -Wsign-compare"
  LIBCRTS="-lgcc"
fi

CCASFLAGS="${CCASFLAGS} ${CPPFLAGS}"

arch="$target_arch"
ARCH=`echo $target_arch | tr [a-z] [A-Z]`

dnl create shell variables from the M4 macros:
PACKAGE=libunwind
PKG_MAJOR=pkg_major
PKG_MINOR=pkg_minor
PKG_EXTRA=pkg_extra
PKG_MAINTAINER=pkg_maintainer

AC_SUBST(build_arch)
AC_SUBST(target_os)
AC_SUBST(arch)
AC_SUBST(ARCH)
AC_SUBST(LDFLAGS_STATIC_LIBCXA)
AC_SUBST(LIBCRTS)
AC_SUBST(PACKAGE)
AC_SUBST(PKG_MAJOR)
AC_SUBST(PKG_MINOR)
AC_SUBST(PKG_EXTRA)
AC_SUBST(PKG_MAINTAINER)

AC_CONFIG_HEADERS(include/config.h)
AC_CONFIG_FILES(Makefile src/Makefile tests/Makefile tests/check-namespace.sh
		doc/Makefile doc/common.tex include/libunwind-common.h)
AC_CONFIG_COMMANDS([src-subdirs],
  [for i in $arch dwarf mi unwind setjmp ptrace; do
     test -d src/$i || mkdir src/$i
   done], [arch=$arch])
AC_OUTPUT
