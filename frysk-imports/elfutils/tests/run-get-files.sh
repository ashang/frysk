#! /bin/sh
# Copyright (C) 1999, 2000, 2002, 2004, 2005 Red Hat, Inc.
# This file is part of Red Hat elfutils.
# Written by Ulrich Drepper <drepper@redhat.com>, 1999.
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

testfiles testfile testfile2

testrun_compare ./get-files testfile testfile2 <<\EOF
cuhl = 11, o = 0, asz = 4, osz = 4, ncu = 191
 file[0] = "???"
 file[1] = "/home/drepper/gnu/new-bu/build/ttt/m.c"
cuhl = 11, o = 114, asz = 4, osz = 4, ncu = 5617
 file[0] = "???"
 file[1] = "/home/drepper/gnu/new-bu/build/ttt/b.c"
 file[2] = "/usr/lib/gcc-lib/i386-redhat-linux/2.96/include/stddef.h"
 file[3] = "/usr/lib/gcc-lib/i386-redhat-linux/2.96/include/stdarg.h"
 file[4] = "/usr/include/bits/types.h"
 file[5] = "/usr/include/bits/sched.h"
 file[6] = "/usr/include/bits/pthreadtypes.h"
 file[7] = "/usr/include/stdio.h"
 file[8] = "/usr/include/libio.h"
 file[9] = "/usr/include/wchar.h"
 file[10] = "/usr/include/_G_config.h"
 file[11] = "/usr/include/gconv.h"
cuhl = 11, o = 412, asz = 4, osz = 4, ncu = 5752
 file[0] = "???"
 file[1] = "/home/drepper/gnu/new-bu/build/ttt/f.c"
cuhl = 11, o = 0, asz = 4, osz = 4, ncu = 2418
 file[0] = "???"
 file[1] = "/shoggoth/drepper/b.c"
 file[2] = "/home/geoffk/objs/laurel-000912-branch/lib/gcc-lib/powerpc-unknown-linux-gnu/2.96-laurel-000912/include/stddef.h"
 file[3] = "/home/geoffk/objs/laurel-000912-branch/lib/gcc-lib/powerpc-unknown-linux-gnu/2.96-laurel-000912/include/stdarg.h"
 file[4] = "/shoggoth/drepper/<built-in>"
 file[5] = "/usr/include/bits/types.h"
 file[6] = "/usr/include/stdio.h"
 file[7] = "/usr/include/libio.h"
 file[8] = "/usr/include/_G_config.h"
cuhl = 11, o = 213, asz = 4, osz = 4, ncu = 2521
 file[0] = "???"
 file[1] = "/shoggoth/drepper/f.c"
cuhl = 11, o = 267, asz = 4, osz = 4, ncu = 2680
 file[0] = "???"
 file[1] = "/shoggoth/drepper/m.c"
EOF

exit 0
