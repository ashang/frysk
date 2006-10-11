// This file is part of the program FRYSK.
//
// Copyright 2006 IBM Corp.
//
// FRYSK is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// FRYSK is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
//
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of FRYSK with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of FRYSK through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

# Asm code both for 32-bit and 64-bit.  Make sure this test case could
# be built in both 32-bit and 64-bit on ppc64.

.text                       # section declaration - begin code

	.global main
#ifdef	__powerpc64__
	.section        ".opd","aw"
	.align 3
#else
	.align 2
#endif

main:

#ifdef __powerpc64__
	.quad   ._main,.TOC.@tocbase,0
	.previous

	.global  ._main
._main:
#endif

#if defined(__powerpc64__) || defined(__powerpc__)
	li      0,1         # syscall number (sys_exit)
	li      3,1         # first argument: exit code
	li      4,4         # gpr4=4

	# shift left 36-bits for the value: 0x1
	# test whether the low 32-bits and high 32-bits can be accessed 
	# right	in 64-bit mode.
	li      5,1         # gpr5=1
	sldi    5,5,36
	sc                  # call kernel
#endif

.section        .note.GNU-stack,"",@progbits
