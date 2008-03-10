#! /bin/sh
# Copyright (C) 2007 Red Hat, Inc.
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

testfiles testfile34 testfile38 testfile41

testrun_compare ../src/addr2line -f -e testfile34 \
				 0x08048074 0x08048075 0x08048076 \
				 0x08049078 0x08048080 0x08049080 <<\EOF
foo
??:0
bar
??:0
_etext
??:0
data1
??:0
??
??:0
_end
??:0
EOF

testrun_compare ../src/addr2line -S -e testfile38 0x02 0x10a 0x211 0x31a <<\EOF
t1_global_outer+0x2
??:0
t2_global_symbol+0x2
??:0
t3_global_after_0+0x1
??:0
(.text)+0x31a
??:0
EOF

testrun_compare ../src/addr2line -S -e testfile41 0x1 0x104 <<\EOF
small_global_at_large_global+0x1
??:0
small_global_first_at_large_global+0x1
??:0
EOF

exit 0
