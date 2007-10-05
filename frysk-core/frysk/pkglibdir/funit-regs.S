// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
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

// Simple test program for frysk-asm.h header.  Set the registers to
// known values and then, from frysk examine them.

#include "frysk-asm.h"

	FUNCTION_BEGIN(main,0)
	MAIN_PROLOGUE(0)

#if defined __i386__
	
	mov $0x7eb03efc, %eax
	mov $0x35a322a0, %ebx
	mov $0x7f198cab, %ecx
	mov $0x35b374c3, %edx
	mov $0x1bc5daed, %esi
	mov $0x457319f3, %edi
	mov $0xcbfed73c, %ebp
	// eip
	// %eflags
	mov $0x93d4a6ed, %esp
	jmp 0 // SIGILL
	
#elif defined __x86_64__

        mov $0x837bb4e2d8209ca3, %rax
        mov $0x16d196be91fb2b92, %rdx
        mov $0x2b9f5c8f2d8cc8f9, %rcx
        mov $0x96c38e833b3f5b12, %rbx
        mov $0x79a4d3db85249938, %rsi
        mov $0x20f76067e815c3b3, %rdi
        mov $0x187298d02605a3d, %rbp
        mov $0x284732dfa61c4a30, %rsp
        mov $0xb3323ac248cb3a9, %r8
        mov $0x4bbb808dda0214ec, %r9
        mov $0xb21dd4b61174f4b2, %r10
        mov $0x8c5582abde79fd44, %r11
        mov $0xad1a8867c8bd5a68, %r12
        mov $0xe0fb8a51946f37a0, %r13
        mov $0x3f22bc816a35f02d, %r14
        mov $0x46bf65d4d966290, %r15
        // mov $0x9a602112ce465ffa, %rip

#else
	
	LOAD_IMMED_WORD(REG0, 0)
	STORE(REG0, REG0)
#endif

	MAIN_EPILOGUE(0)
	FUNCTION_RETURN(main,0)
	FUNCTION_END(main,0)
