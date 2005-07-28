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

	.global main
main:
#ifdef __i386__
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
#endif
