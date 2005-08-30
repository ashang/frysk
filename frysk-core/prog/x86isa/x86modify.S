// This file is part of FRYSK.
//
// Copyright 2005, Red Hat Inc.
//
// FRYSK is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// FRYSK is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with FRYSK; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

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
