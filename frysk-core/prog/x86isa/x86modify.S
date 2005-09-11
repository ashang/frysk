// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

	.text
	.global main
main:
#ifdef __i386__
	push %ebp
	mov $0x14, %eax // getpid syscall number
	mov $21, %ebp 
	mov $22, %ebx
	mov $23, %ecx
	lea label1, %edi
	lea d0, %edx
	lea label0, %esi
	int $0x80
	jmp *%edi
label0:
// Perform a syscall which will end up being exit (2)
	cmp $2, %ebx
	jne label2
	cmp $3, %ecx
	jne label2
	lea d0, %eax
	mov 0(%eax), %ecx
	cmp $8, %ecx
	jne label2
	cmp $4, %edx
	jne label2
	cmp $5, %ebp
	jne label2
	cmp $6, %esi
	jne label2
	mov $1, %eax
	int $0x80
	jmp label1
label2: jmp 0  // if we get here, fail
label1:
	pop %ebp
	ret
	.data
d0:	.word 3
#endif
