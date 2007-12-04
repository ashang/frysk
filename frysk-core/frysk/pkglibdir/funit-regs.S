// This file is part of the program FRYSK.
//
// Copyright 2007 Red Hat Inc.
// Copyright 2007 (C) IBM
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
	
	// floating-point and vector registers
	mov $fxregs, %eax
	fxrstor (%eax)
	fxsave (%eax)

	mov $0x7eb03efc, %eax
	mov $0x35a322a0, %ebx
	mov $0x7f198cab, %ecx
	mov $0x35b374c3, %edx
	mov $0x1bc5daed, %esi
	mov $0x457319f3, %edi
	mov $0xcbfed73c, %ebp
	// %eflags
	mov $0x93d4a6ed, %esp
	
#elif defined __x86_64__

	// floating-point and vector registers
	mov $fxregs, %rax
	rex64/fxrstor (%rax)

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

#elif defined __powerpc64__

        LOAD_IMMED_WORD( 0, 0x0000000000000000) //This Reg is always Zero
        LOAD_IMMED_WORD( 1, 0x514c159c25c27735)
        LOAD_IMMED_WORD( 2, 0x674b6064cdf97685)
        LOAD_IMMED_WORD( 3, 0x808ac01e8911f56c)
        LOAD_IMMED_WORD( 4, 0xcf4362db3356a25a)
        LOAD_IMMED_WORD( 5, 0xe356818815d30ae3)
        LOAD_IMMED_WORD( 6, 0x34a847d84ac039eb)
        LOAD_IMMED_WORD( 7, 0xa6c244ccfc672fd1)
        LOAD_IMMED_WORD( 8, 0x4e857fa76fae4610)
        LOAD_IMMED_WORD( 9, 0xfa6ecb942e56bdb1)
        LOAD_IMMED_WORD(10, 0xce40dcae99e51340)
        LOAD_IMMED_WORD(11, 0xd5e20897737372fa)
        LOAD_IMMED_WORD(12, 0x44da7341c8169fcb)
        LOAD_IMMED_WORD(13, 0xef908c0f5ceb230f)
        LOAD_IMMED_WORD(14, 0xd6f64efaf644ba20)
        LOAD_IMMED_WORD(15, 0xd5b5dd6910287bb3)
        LOAD_IMMED_WORD(16, 0xce9381ebf6d51d50)
        LOAD_IMMED_WORD(17, 0xb3d21c30af96757c)
        LOAD_IMMED_WORD(18, 0x48ce58b2c1242f3)
        LOAD_IMMED_WORD(19, 0x5031020316f4a712)
        LOAD_IMMED_WORD(20, 0xe5a6446b480c1bcb)
        LOAD_IMMED_WORD(21, 0x39d366cce0933e0c)
        LOAD_IMMED_WORD(22, 0xf86103d6cd223af7)
        LOAD_IMMED_WORD(23, 0xcab98ee45bb9a68f)
        LOAD_IMMED_WORD(24, 0x34195aae274630f9)
        LOAD_IMMED_WORD(25, 0xd1df3bfceb05da1a)
        LOAD_IMMED_WORD(26, 0xe408d863626b040a)
        LOAD_IMMED_WORD(27, 0xb847f6c562a62676)
        LOAD_IMMED_WORD(28, 0x44508793aa174c36)
        LOAD_IMMED_WORD(29, 0x3c21d5f786149c80)
        LOAD_IMMED_WORD(30, 0x9871b47e31368590)
        LOAD_IMMED_WORD(31, 0xd5a767e17d453bef)

#elif defined __powerpc__

	LOAD_IMMED_WORD( 0, 0x00000000)
	LOAD_IMMED_WORD( 1, 0x25c27735)
	LOAD_IMMED_WORD( 2, 0xcdf97685)
        LOAD_IMMED_WORD( 3, 0x8911f56c)
        LOAD_IMMED_WORD( 4, 0x3356a25a)
        LOAD_IMMED_WORD( 5, 0x15d30ae3)
        LOAD_IMMED_WORD( 6, 0x4ac039eb)
        LOAD_IMMED_WORD( 7, 0xfc672fd1)
        LOAD_IMMED_WORD( 8, 0x6fae4610)
        LOAD_IMMED_WORD( 9, 0x2e56bdb1)
        LOAD_IMMED_WORD(10, 0x99e51340)
        LOAD_IMMED_WORD(11, 0x737372fa)
        LOAD_IMMED_WORD(12, 0xc8169fcb)
        LOAD_IMMED_WORD(13, 0x5ceb230f)
        LOAD_IMMED_WORD(14, 0xf644ba20)
        LOAD_IMMED_WORD(15, 0x10287bb3)
        LOAD_IMMED_WORD(16, 0xf6d51d50)
        LOAD_IMMED_WORD(17, 0xaf96757c)
        LOAD_IMMED_WORD(18, 0x2c1242f3)
        LOAD_IMMED_WORD(19, 0x16f4a712)
        LOAD_IMMED_WORD(20, 0x480c1bcb)
        LOAD_IMMED_WORD(21, 0xe0933e0c)
        LOAD_IMMED_WORD(22, 0xcd223af7)
        LOAD_IMMED_WORD(23, 0x5bb9a68f)
        LOAD_IMMED_WORD(24, 0x274630f9)
        LOAD_IMMED_WORD(25, 0xeb05da1a)
        LOAD_IMMED_WORD(26, 0x626b040a)
        LOAD_IMMED_WORD(27, 0x62a62676)
        LOAD_IMMED_WORD(28, 0xaa174c36)
        LOAD_IMMED_WORD(29, 0x86149c80)
        LOAD_IMMED_WORD(30, 0x31368590)
        LOAD_IMMED_WORD(31, 0x7d453bef)

#else
#error Need to set values for the Register of this Arch
#endif

	// Jumping to crash sets the instruction-pointer
	JUMP(crash)

	MAIN_EPILOGUE(0)
	FUNCTION_RETURN(main,0)
	FUNCTION_END(main,0)

// For the PC, get it to crash at a known symbol.

.global crash
crash:
#if defined __i386__
	mov 0, %eax
#elif defined __x86_64__
	mov 0, %rax
#elif defined __powerpc64__
        lis 0, 0@highest
#elif defined __powerpc__
        lis 0, 0@ha
#else
	LOAD_IMMED_WORD(REG0, 0)
	STORE(REG0, REG0)
#endif

	// Buffer containing values for all the X86 (fpu and sse)
	// registers that can be loaded using a single fxrstor
	// instruction.  Buffer must be 16-byte aligned.
#if defined __i386__ || defined __x86_64__
.data
	.align 16
fxregs:
	// FCW 0x1e71
	.byte 0x71, 0x1e
        // FSW 0xc7e4
        .byte 0xe4, 0xc7
        // FTW 0xc9
        .byte 0xc9
	.fill 1, 1, 0
        // FOP 0x068f
        .byte 0x8f, 0x06
#if defined __i386__
	// EIP
        .byte 0xff, 0xee, 0xd5, 0x79
	// can't reliably set CS on i386
        .byte 0, 0, 0, 0
	// DP
        .byte 0xb0, 0xd7, 0xac, 0xd0
	// can't reliably set DS on i386
        .byte 0, 0, 0, 0
#else
        // RIP 0x0000236679d5eeff
        .byte 0xff, 0xee, 0xd5, 0x79
        .byte 0x66, 0x23, 0x00, 0x00
        // RDP 0x00007565d0acd7b0
        .byte 0xb0, 0xd7, 0xac, 0xd0
        .byte 0x65, 0x75, 0x00, 0x00
#endif
	// MXCSR
	.fill 4, 1, 0
	// MXCSR_MASK
	.fill 4, 1, 0
	// ST0 0xa7367289dc779dba0bd9
        .byte 0xd9, 0x0b, 0xba, 0x9d
        .byte 0x77, 0xdc, 0x89, 0x72
	.byte 0x36, 0xa7
	.fill 6, 1, 0
        // ST1 0x64abfe452c2a5b8d0eb1
        .byte 0xb1, 0x0e, 0x8d, 0x5b
        .byte 0x2a, 0x2c, 0x45, 0xfe
        .byte 0xab, 0x64
	.fill 6, 1, 0
        // ST2 0xb829e094740ce9d53a04
        .byte 0x4, 0x3a, 0xd5, 0xe9
        .byte 0xc, 0x74, 0x94, 0xe0
        .byte 0x29, 0xb8
	.fill 6, 1, 0
        // ST3 0x4bd27ebf86294a4a48f8
        .byte 0xf8, 0x48, 0x4a, 0x4a
        .byte 0x29, 0x86, 0xbf, 0x7e
        .byte 0xd2, 0x4b
	.fill 6, 1, 0
        // ST4 0xb96a6b1dabba9af1fa66
        .byte 0x66, 0xfa, 0xf1, 0x9a
        .byte 0xba, 0xab, 0x1d, 0x6b
        .byte 0x6a, 0xb9
	.fill 6, 1, 0
        // ST5 0x87d05c0a1e9c9bb98ebc
        .byte 0xbc, 0x8e, 0xb9, 0x9b
        .byte 0x9c, 0x1e, 0xa, 0x5c
	.byte 0xd0, 0x87
	.fill 6, 1, 0
        // ST6 0x5a0f14dcf87c56690b5f
        .byte 0x5f, 0xb, 0x69, 0x56
	.byte 0x7c, 0xf8, 0xdc, 0x14
	.byte 0xf, 0x5a
	.fill 6, 1, 0
        // ST7 0x1f12ae5ec49479a8cb19
        .byte 0x19, 0xcb, 0xa8, 0x79
	.byte 0x94, 0xc4, 0x5e, 0xae
	.byte 0x12, 0x1f
	.fill 6, 1, 0
    // vector registers
        // XMM0 0x47beb912e3bfa457d6af5267b3fec23b
        .byte 0x3b, 0xc2, 0xfe, 0xb3
	.byte 0x67, 0x52, 0xaf, 0xd6
	.byte 0x57, 0xa4, 0xbf, 0xe3
	.byte 0x12, 0xb9, 0xbe, 0x47
        // XMM1 0x7ce95f1c2fe254e2cac9b22bf43f73c5
        .byte 0xc5, 0x73, 0x3f, 0xf4
	.byte 0x2b, 0xb2, 0xc9, 0xca
	.byte 0xe2, 0x54, 0xe2, 0x2f
	.byte 0x1c, 0x5f, 0xe9, 0x7c
        // XMM2 0x566b1b326d658a3365678d130362a6b5
        .byte 0xb5, 0xa6, 0x62, 0x3
	.byte 0x13, 0x8d, 0x67, 0x65
	.byte 0x33, 0x8a, 0x65, 0x6d
	.byte 0x32, 0x1b, 0x6b, 0x56
        // XMM3 0x20801ada9126df05d6927e0847fa8f07
        .byte 0x7, 0x8f, 0xfa, 0x47
	.byte 0x8, 0x7e, 0x92, 0xd6
	.byte 0x5, 0xdf, 0x26, 0x91
	.byte 0xda, 0x1a, 0x80, 0x20
        // XMM4 0x2304fff624579bbddc74a7df4d34cfd9
        .byte 0xd9, 0xcf, 0x34, 0x4d
	.byte 0xdf, 0xa7, 0x74, 0xdc
	.byte 0xbd, 0x9b, 0x57, 0x24
	.byte 0xf6, 0xff, 0x4, 0x23
        // XMM5 0x58afb31bf2d2b4a33512eefc0d1f5fc4
        .byte 0xc4, 0x5f, 0x1f, 0xd
	.byte 0xfc, 0xee, 0x12, 0x35
	.byte 0xa3, 0xb4, 0xd2, 0xf2
	.byte 0x1b, 0xb3, 0xaf, 0x58
        // XMM6 0x5caa454b2a0c2975df1df97f8d180e93
        .byte 0x93, 0xe, 0x18, 0x8d
	.byte 0x7f, 0xf9, 0x1d, 0xdf
	.byte 0x75, 0x29, 0xc, 0x2a
	.byte 0x4b, 0x45, 0xaa, 0x5c
        // XMM7 0x2147b54a26ac605b98ef6a2f2da97f57
        .byte 0x57, 0x7f, 0xa9, 0x2d
	.byte 0x2f, 0x6a, 0xef, 0x98
	.byte 0x5b, 0x60, 0xac, 0x26
	.byte 0x4a, 0xb5, 0x47, 0x21
        // XMM8 0x59f29cf0c8c06a32cfbcf982d29b2622
        .byte 0x22, 0x26, 0x9b, 0xd2
	.byte 0x82, 0xf9, 0xbc, 0xcf
	.byte 0x32, 0x6a, 0xc0, 0xc8
	.byte 0xf0, 0x9c, 0xf2, 0x59
        // XMM9 0x20105410d22c027c7ff7fd949673caad
        .byte 0xad, 0xca, 0x73, 0x96
	.byte 0x94, 0xfd, 0xf7, 0x7f
	.byte 0x7c, 0x2, 0x2c, 0xd2
	.byte 0x10, 0x54, 0x10, 0x20
        // XMM10 0xf618933912ad69c1b184ef8159ce5708
        .byte 0x8, 0x57, 0xce, 0x59
	.byte 0x81, 0xef, 0x84, 0xb1
	.byte 0xc1, 0x69, 0xad, 0x12
	.byte 0x39, 0x93, 0x18, 0xf6
        // XMM11 0x856c8d779d6ed06a4bdadc488a3e1989
        .byte 0x89, 0x19, 0x3e, 0x8a
	.byte 0x48, 0xdc, 0xda, 0x4b
	.byte 0x6a, 0xd0, 0x6e, 0x9d
	.byte 0x77, 0x8d, 0x6c, 0x85
        // XMM12 0xf232c4e489f81468c534d0627b8f373
        .byte 0x73, 0xf3, 0xb8, 0x27
	.byte 0x6, 0x4d, 0x53, 0x8c
	.byte 0x46, 0x81, 0x9f, 0x48
	.byte 0x4e, 0x2c, 0x23, 0xf
        // XMM13 0x117c5a8df5f87761027e19798ad84b0
        .byte 0xb0, 0x84, 0xad, 0x98
	.byte 0x97, 0xe1, 0x27, 0x10
	.byte 0x76, 0x87, 0x5f, 0xdf
	.byte 0xa8, 0xc5, 0x17, 0x1
        // XMM15 0xea03fbb7498f45fc918621ffe21d2f53
        .byte 0x53, 0x2f, 0x1d, 0xe2
	.byte 0xff, 0x21, 0x86, 0x91
	.byte 0xfc, 0x45, 0x8f, 0x49
	.byte 0xb7, 0xfb, 0x3, 0xea
        // XMM15 0xfc6683d958a95180c6226f27a78b2c5a
        .byte 0x5a, 0x2c, 0x8b, 0xa7
	.byte 0x27, 0x6f, 0x22, 0xc6
	.byte 0x80, 0x51, 0xa9, 0x58
	.byte 0xd9, 0x83, 0x66, 0xfc
	// some padding
	.fill 512, 1, 0
#endif
