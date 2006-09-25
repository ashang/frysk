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

.text                       # section declaration - begin code

	.global main
#ifdef	__powerpc64__
	.section        ".opd","aw"
	.align 3
#endif

main:

#ifdef __powerpc64__
	.quad   ._main,.TOC.@tocbase,0
	.previous

	.global  ._main
._main:

        mflr    0               # move the value of LR to gpr0
        std     31,-8(1)
        std     0,16(1)         # store the LR to 16(1)
        stdu    1,-144(1)       # store the stack frame pointer value to -144(1)
        mr      31,1            # copy the value gpr1 to gpr31

        li      0,20            # syscall number(sys_write)
        li      3,21            # values for TestPPC64Modify.java to inspect. gpr3=21
        li      4,22            # gpr4=22
        li      5,23            # gpr5=23

        # load the data's address
        lis     6,data_value@highest
        ori     6,6,data_value@higher # load data bits 32-47 into gpr6 bits 0-15

        rldicr  6,6,32,31       # rotate gpr6's low word(0-31)into 32-63

        oris    6,6,data_value@h
        ori     6,6,data_value@l

        #load the .L0's address
        lis     7,.L0@highest
        ori     7,7,.L0@higher # load data bits 32-47 into gpr6 bits 0-15

        rldicr  7,7,32,31       # rotate gpr6's low word(0-31)into 32-63

        oris    7,7,.L0@h
        ori     7,7,.L0@l

        # load the .L1's address
        lis     8,.L1@highest
        ori     8,8,.L1@higher # load data bits 32-47 into gpr6 bits 0-15

        rldicr  8,8,32,31       # rotate gpr6's low word(0-31)into 32-63

        oris    8,8,.L1@h
        ori     8,8,.L1@l

        sc
        #mtctr  8
        #bctr
.L0:
        # Check the value of variables is right or not
        cmpdi   cr7,4,4
        bne     cr7,.L2
        cmpdi   cr7,5,5
        bne     cr7,.L2

        # check the value of gpr6
        # load the variable's value
        lis     6,data_value@highest  # load data bits 48-63 into gpr6 bits 16-31
        ori     6,6,data_value@higher # load data bits 32-47 into gpr6 bits 0-15

        rldicr  6,6,32,31       # rotate gpr6's low word(0-31)into 32-63

        oris    6,6,data_value@h
        ori     6,6,data_value@l

        lwa     10,0(6)
        cmpdi   cr7,10,8
        bne     cr7,.L2
        cmpwi   cr7,7,7
        bne     cr7,.L2

        li      0,1
        li      3,3
        sc
        b .L1

.L2:    b .L1                   # failure
.L1:
        ld      1,0(1)          # restore the stack frame pointer value
        # restore the LR register
        ld      0,16(1)
        mtlr    0

        ld      31,-8(1)
        blr                   # Not really supposed to get here

        .data
data_value:     .long  3

#endif

.section        .note.GNU-stack,"",@progbits

