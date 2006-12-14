// This file is part of the program FRYSK.
//
// Copyright 2005, 2006, Red Hat Inc.
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

// XXX: Right now this file contains an inconsistent hodge-podge of
// assembler code that doesn't really test register reads.

// XXX:	 What the programs should do is:
// Initialize the ISA's registers, setting on to a new pc
// value and then jumping to 0.
// At the new PC value verify the registers and exit cleanly.
	
	.global main
	

#if defined __i386__
main:
	push %ebp
	mov $1, %eax // exit syscall number
	mov $2, %ebx // first syscall arg
	mov $3, %ecx
	mov $4, %edx
	mov $5, %ebp
	mov $6, %esi
	mov $7, %edi
	mov $8, %esp
// Perform a syscall which will end up being exit (2)
	int $0x80
	ret


#elif defined __powerpc64__
	
// 64-bit only asm code - need to store values in high 32-bits of
// 64-bit registers.

.section        .rodata
        .align 2
.LCFloat:
        .long   1072693248
        .long   0

.text                       # section declaration - begin code

	.global main
	.section        ".opd","aw"
	.align 3

main:

	.quad   ._main,.TOC.@tocbase,0
	.previous

	.global  ._main
._main:

	# shift left 36-bits for the value: 0x1
	# test whether the low 32-bits and high 32-bits can be accessed 
	# right	in 64-bit mode.
	li      5,1         # gpr5=1
	sldi    5,5,36

	fsub    0,2,2 	    # fpr0 = 0 
        lis 9,.LCFloat@ha
        lfd 1,.LCFloat@l(9) # fpr1 = 1

        fadd    2,1,1       # fpr2 = 2


	li      0,1         # syscall number (sys_exit)
	li      3,1         # first argument: exit code
	li      4,4         # gpr4=4
	sc                  # call kernel



#elif defined __powerpc__
.section        .rodata
        .align 2
.LCFloat:
        .long   1072693248
        .long   0

.text                       # section declaration - begin code

	.global main
	.align 2
main:
	# shift left 36-bits for the value: 0x1
	# test whether the low 32-bits and high 32-bits can be accessed 
	# right	in 64-bit mode.
	li      5,1         # gpr5=1
	sldi    5,5,36

	fsub    0,2,2 	    # fpr0 = 0 
        lis 9,.LCFloat@ha
        lfd 1,.LCFloat@l(9) # fpr1 = 1

        fadd    2,1,1       # fpr2 = 2


	li      0,1         # syscall number (sys_exit)
	li      3,1         # first argument: exit code
	li      4,4         # gpr4=4
	sc                  # call kernel


#elif defined __x86_64__
	.global main
main:
	mov $1, %rax // exit syscall number
	mov $2, %rdi // first syscall arg
	mov $3, %rsi
	mov $-4, %rdx
	mov $0xdeadbeef, %r10
	mov $0xfeeddeadbeef, %r8
	mov $0xdeadbeefdeadbeef, %r9
// Perform a syscall which will end up being exit (2)
	syscall
	retq


#else
#error Unknown architecture
#endif

	// Make the stack no-execute.
.section        .note.GNU-stack,"",@progbits
