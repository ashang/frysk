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
//XXX: Should be macros with parameters.
#define NO_OP nop
#define MOVE mov
#define INTERRUPT int
#define COMPARE cmp
#define RETURN ret
#define PUSH push
#define POP pop
#define JUMP jmp
#define JUMP_IF_NOT_EQUAL jne
#define CALL(FUNC) call FUNC
#define STORE_WORD(WORD, REG) movl $WORD, (REG)

#ifdef __i386__

	.text
	.global main

#define STACK_BASE_POINTER %ebp // stack base pointer
#define GEN_REG_1 %eax // accumulator register
#define GEN_REG_2 %ebx // base register
#define GEN_REG_3 %ecx // counter register
#define GEN_REG_4 %edx // data register
#define PROLOGUE \
		pushl %ebp ;\		
		movl %esp, %ebp 
		
#define EPILOGUE \
		popl	%ebp ;\
		ret		

#elif defined __x86_64__

	.text
	.global main

#define GEN_REG_1 %rax
#define GEN_REG_2 %rdi
#define GEN_REG_3 %rsi
#define GEN_REG_4 %rdx

#elif defined __powerpc__

.text                       # section declaration - begin code
	.global  main
main:

#elif defined __powerpc64__

	.section        ".opd","aw"
	.global main
	.align 3
main:
	.quad   ._main,.TOC.@tocbase,0
	.text                       # section declaration - begin code
	.global  ._main
._main:

#define GEN_REG_1 %gpr0
#define GEN_REG_2 %gpr3
#define GEN_REG_3 %gpr4
#define GEN_REG_4 %gpr5

#endif
