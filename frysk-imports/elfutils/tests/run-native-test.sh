#! /bin/sh
# Copyright (C) 2005, 2006 Red Hat, Inc.
# This file is part of Red Hat elfutils.
#
# Red Hat elfutils is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by the
# Free Software Foundation; version 2 of the License.
#
# Red Hat elfutils is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License along
# with Red Hat elfutils; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA.
#
# Red Hat elfutils is an included package of the Open Invention Network.
# An included package of the Open Invention Network is a package for which
# Open Invention Network licensees cross-license their patents.  No patent
# license is granted, either expressly or impliedly, by designation as an
# included package.  Should you wish to participate in the Open Invention
# Network licensing program, please visit www.openinventionnetwork.com
# <http://www.openinventionnetwork.com>.


. $srcdir/test-subr.sh

# This tests all the miscellaneous components of backend support
# against whatever this build is running on.  A platform will fail
# this test if it is missing parts of the backend implementation.
#
# As new backend code is added to satisfy the test, be sure to update
# the fixed test cases (run-allregs.sh et al) to test that backend
# in all builds.

tempfiles native.c native
echo 'main () { while (1) pause (); }' > native.c

native=0
kill_native()
{
  test $native -eq 0 || {
    kill -9 $native 2> /dev/null || :
    wait $native 2> /dev/null || :
  }
  native=0
}

native_cleanup()
{
  kill_native
  test_cleanup
}

trap native_cleanup 0 1 2 15

for cc in "$HOSTCC" "$HOST_CC" cc gcc "$CC"; do
  test "x$cc" != x || continue
  $cc -o native -g native.c > /dev/null 2>&1 &&
  ./native > /dev/null 2>&1 & native=$! &&
  sleep 1 && kill -0 $native 2> /dev/null &&
  break ||
  native=0
done

native_test()
{
  # Try the build against itself, i.e. $config_host.
  testrun "$@" -e $1 > /dev/null

  # Try the build against a presumed native process, running this sh.
  # For tests requiring debug information, this may not test anything.
  testrun "$@" -p $$ > /dev/null

  # Try the build against the trivial native program we just built with -g.
  test $native -eq 0 || testrun "$@" -p $native > /dev/null
}

native_test ./allregs
native_test ./funcretval

# We do this explicitly rather than letting the trap 0 cover it,
# because as of version 3.1 bash prints the "Killed" report for
# $native when we do the kill inside the exit handler.
native_cleanup

exit 0
