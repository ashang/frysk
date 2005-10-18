all-local: FAIL

# Compiling Werror with -Werror -Wall should get a warning and hence
# abort the compilation.  At least GCC 4.0.1 20050727 (Red Hat
# 4.0.1-5) does not abort, and consequently this test is marked as a
# FAIL.  The build system contains hacks to get around this.

EXTRA_DIST = $(srcdir)/Werror.java

FAIL: Werror.java Makefile
	if gcj -Werror -Wall -c -o /dev/null $(srcdir)/Werror.java ; \
	then touch FAIL ; \
	else touch PASS ; \
	fi
	test -r FAIL # expected to fail

CLEANFILES = FAIL PASS
