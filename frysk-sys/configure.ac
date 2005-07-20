AC_PREREQ(2.59)
AC_INIT(accudog, 0.0)
AM_INIT_AUTOMAKE([subdir-objects foreign no-installinfo no-exeext])

AC_PROG_RANLIB

AC_PROG_CXX
test x"$CXXFLAGS" = "x-g -O2" && CXXFLAGS="-g -O"

AC_PROG_CC
AM_PROG_CC_C_O
test x"$CFLAGS" = "x-g -O2" && CFLAGS="-g -O"

AM_PROG_GCJ
test x"$GCJFLAGS" = "x-g -O2" && GCJFLAGS="-g -O"

AM_PROG_AS
AC_PROG_YACC()

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
