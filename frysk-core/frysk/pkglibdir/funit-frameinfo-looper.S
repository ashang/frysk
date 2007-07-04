// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
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


#include "frysk-asm.h"

#ifdef __x86_64__

.globl second
	.type	second, @function
 second:
.LFB5:
	PUSH_BASEP
.LCFI0:
	MOV_STACKP
.LCFI1:
	NO_OP
	NO_OP
	NO_OP	;  NO_OP ; NO_OP
	LEAVE
.LFE5:
	.size	second, .-second
.globl first
	.type	first, @function
first:
.LFB6:
	PUSH_BASEP
.LCFI2:
	MOV_STACKP
.LCFI3:
	NO_OP
	NO_OP
	NO_OP	; NO_OP ; NO_OP
	NO_OP
	JUMP	(.L4)
.L5:	
	NO_OP
	CALL	(second@PLT) #x86_64_func_entry_test
.L4:	
	NO_OP
	NO_OP
	JUMP	(.L5)
	LEAVE
.LFE6:
	.size	first, .-first
.globl main
	.type	main, @function
main:
.LFB7:
	PUSH_BASEP
.LCFI4:
	MOV_STACKP
.LCFI5:
	subq	$16, STACKP
.LCFI6:
	movl	%edi, -4(BASEP)
	movq	REG3, -16(BASEP)
	movl	$0, %eax
	CALL	(first@PLT)
	LEAVE
.LFE7:
	.size	main, .-main
	.section	.eh_frame,"a",@progbits
.Lframe1:
	.long	.LECIE1-.LSCIE1		# CIE length
.LSCIE1:
	.long	0x0	# CIE ID tag
	.byte	0x1	# Version
	.string	"zR"	# Augmentation
	.uleb128 0x1	# Code alignment factor
	.sleb128 -8	# Data alignment factor 
	.byte	0x10	# Return address register
	.uleb128 0x1	# Augmentation size
	.byte	0x1b	# FDE encoding
	.byte	0xc	# DW_CFA_def_cfa
	.uleb128 0x7
	.uleb128 0x8
	.byte	0x90	# DW_CFA_offset, column 0x10
	.uleb128 0x1
	.align 8
.LECIE1:
.LSFDE1:
	.long	.LEFDE1-.LASFDE1	# FDE length
.LASFDE1:
	.long	.LASFDE1-.Lframe1	# FDE CIE offset
	.long	.LFB5-.		# Initial location
	.long	.LFE5-.LFB5	# Address range
	.uleb128 0x0		# Augmentation size
	.byte	0x4		# DW_CFA_advance_loc4
	.long	.LCFI0-.LFB5
	.byte	0xe		# DW_CFA_def_cfa_offset
	.uleb128 0x10
	.byte	0x86		# DW_CFA_offset, column 0x6
	.uleb128 0x2
	.byte	0x4		# DW_CFA_advance_loc4
	.long	.LCFI1-.LCFI0
	.byte	0xd		# DW_CFA_def_cfa_register
	.uleb128 0x6
	.align 8
.LEFDE1:
.LSFDE3:
	.long	.LEFDE3-.LASFDE3
.LASFDE3:
	.long	.LASFDE3-.Lframe1
	.long	.LFB6-.
	.long	.LFE6-.LFB6
	.uleb128 0x0
	.byte	0x4
	.long	.LCFI2-.LFB6
	.byte	0xe
	.uleb128 0x10
	.byte	0x86
	.uleb128 0x2
	.byte	0x4
	.long	.LCFI3-.LCFI2
	.byte	0xd
	.uleb128 0x6
	.align 8
.LEFDE3:
.LSFDE5:
	.long	.LEFDE5-.LASFDE5
.LASFDE5:
	.long	.LASFDE5-.Lframe1
	.long	.LFB7-.
	.long	.LFE7-.LFB7
	.uleb128 0x0
	.byte	0x4
	.long	.LCFI4-.LFB7
	.byte	0xe
	.uleb128 0x10
	.byte	0x86
	.uleb128 0x2
	.byte	0x4
	.long	.LCFI5-.LCFI4
	.byte	0xd
	.uleb128 0x6
	.align 8
.LEFDE5:
	.ident	"GCC: (GNU) 4.1.1 20070105 (Red Hat 4.1.1-51)"
	.section	.note.GNU-stack,"",@progbits
	
#elif defined __i386__

.globl second
	.type	second, @function
second:
.LFB5:
	PUSH_BASEP
.LCFI0:
	MOV_STACKP
.LCFI1:
	NO_OP
	NO_OP
	NO_OP
.L2:
	NO_OP
	NO_OP
	JUMP	(.L2)
	EXIT
.LFE5:
	.size	second, .-second
.globl first
	.type	first, @function
first:
.LFB6:
	PUSH_BASEP
.LCFI2:
	MOV_STACKP
.LCFI3:
	NO_OP
	NO_OP
.L3:	
	NO_OP
	CALL	(second)
	NO_OP
	JUMP	(.L3)
	EXIT
.LFE6:
	.size	first, .-first
.globl main
	.type	main, @function
main:
.LFB7:
	leal	4(%esp), %ecx
.LCFI4:
	andl	$-16, %esp
	pushl	-4(%ecx)
.LCFI5:
	PUSH_BASEP
.LCFI6:
	MOV_STACKP
.LCFI7:
	pushl	%ecx
.LCFI8:
	CALL	(first)
	POP	(%ecx)
	POP	(%ebp)
	leal	-4(%ecx), %esp
	ret
.LFE7:
	.size	main, .-main
	.section	.eh_frame,"a",@progbits
.Lframe1:
	.long	.LECIE1-.LSCIE1
.LSCIE1:
	.long	0x0
	.byte	0x1
	.string	""
	.uleb128 0x1
	.sleb128 -4
	.byte	0x8
	.byte	0xc
	.uleb128 0x4
	.uleb128 0x4
	.byte	0x88
	.uleb128 0x1
	.align 4
.LECIE1:
.LSFDE1:
	.long	.LEFDE1-.LASFDE1
.LASFDE1:
	.long	.LASFDE1-.Lframe1
	.long	.LFB5
	.long	.LFE5-.LFB5
	.byte	0x4
	.long	.LCFI0-.LFB5
	.byte	0xe
	.uleb128 0x8
	.byte	0x85
	.uleb128 0x2
	.byte	0x4
	.long	.LCFI1-.LCFI0
	.byte	0xd
	.uleb128 0x5
	.align 4
.LEFDE1:
.LSFDE3:
	.long	.LEFDE3-.LASFDE3
.LASFDE3:
	.long	.LASFDE3-.Lframe1
	.long	.LFB6
	.long	.LFE6-.LFB6
	.byte	0x4
	.long	.LCFI2-.LFB6
	.byte	0xe
	.uleb128 0x8
	.byte	0x85
	.uleb128 0x2
	.byte	0x4
	.long	.LCFI3-.LCFI2
	.byte	0xd
	.uleb128 0x5
	.align 4
.LEFDE3:
.LSFDE5:
	.long	.LEFDE5-.LASFDE5
.LASFDE5:
	.long	.LASFDE5-.Lframe1
	.long	.LFB7
	.long	.LFE7-.LFB7
	.byte	0x4
	.long	.LCFI4-.LFB7
	.byte	0xc
	.uleb128 0x1
	.uleb128 0x0
	.byte	0x9
	.uleb128 0x4
	.uleb128 0x1
	.byte	0x4
	.long	.LCFI5-.LCFI4
	.byte	0xc
	.uleb128 0x4
	.uleb128 0x4
	.byte	0x4
	.long	.LCFI6-.LCFI5
	.byte	0xe
	.uleb128 0x8
	.byte	0x85
	.uleb128 0x2
	.byte	0x4
	.long	.LCFI7-.LCFI6
	.byte	0xd
	.uleb128 0x5
	.byte	0x4
	.long	.LCFI8-.LCFI7
	.byte	0x84
	.uleb128 0x3
	.align 4
.LEFDE5:
	.ident	"GCC: (GNU) 4.1.2 20070502 (Red Hat 4.1.2-12)"
	.section	.note.GNU-stack,"",@progbits	


#else

	main:
	
	ENTER
	
	LOAD_IMMED (REG1, 0x14)
	LOAD_IMMED (REG2, 1)
	LOAD_IMMED (REG3, 2)
	LOAD_IMMED (REG4, 3)
//	INTERRUPT $0x80
loop:
	NO_OP
	NO_OP
	NO_OP
	NO_OP ; NO_OP ; NO_OP
	NO_OP
	NO_OP ; NO_OP ; NO_OP
	NO_OP;
	COMPARE_IMMED(REG2, 1)
	JUMP_NE (fail)
	COMPARE_IMMED(REG3, 2)
	JUMP_NE (fail)
	COMPARE_IMMED(REG4, 3)
	JUMP_NE (fail)
	JUMP (loop)
	EXIT
fail: JUMP (0)
	
#endif
