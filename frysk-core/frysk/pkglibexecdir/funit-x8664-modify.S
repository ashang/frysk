// This file is part of the program FRYSK.
//
// Copyright 2006 Red Hat Inc.
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
// License in all respects for all o;f the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

	.global main
main:
#ifdef __x86_64__
	mov $0x14, %rax		// getpid syscall number
	mov $21, %rdi		// values for test program to inspect
	mov $22, %rsi
	mov $23, %rdx
	movq $d0, %r10		// memory address test prog to modify
	movq $.L0, %r8		// address for test program to write into %r9
	movq $.L1, %r9
	syscall
	jmp *%r9
.L0:
	// Verify values written into registers and memory by the test program
	cmp $2, %rdi
	jne .L2
	cmp $3, %rsi
	jne .L2
	movq $d0, %r10
	mov 0(%r10), %r10
	cmp $8, %r10
	jne .L2
	mov $0xdeadbeef,%r10
	cmp %r10, %rdx
	jne .L2
	mov $0xfeeddeadbeef, %r10
	cmp %r10, %rcx
	jne .L2
	mov $0xdeadbeefdeadbeef, %r10
	cmp %r10, %r8
	jne .L2
	mov $1, %rax
	syscall
	jmp .L1
.L2:	jmp 0			// failure
.L1:
	retq			// Not really supposed to get here
	.data
d0:	.word 3
#endif
	.section	.note.GNU-stack,"",@progbits
