// This file is part of the program FRYSK.
//
// Copyright 2006, 2007 Red Hat Inc.
// Copyright 2007, IBM Corp.
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

#ifndef FRYSK_ASM_H
#define FRYSK_ASM_H

// This file defines a both an Instruction Set Architecure (ISA) and
// calling convention (aka Application Binary Interface or ABI) for an
// abstract machine.  The frysk-isa is emulated using the host's
// instruction set.



// The FRYSK Load-Store Instruction Set Architecture:

// In a load-store architecture more complex operations are built from
// simpler instrutions.  For instance, a memory increment uses the
// sequence:

//   LOAD_IMMED_WORD(REG1, memory_addres)
//   LOAD(REG2, REG1)
//   LOAD_IMMED_BYTE(REG3, 1)
//   ADD(REG2, REG3)
//   STORE(REG2, REG1)

// Note that while the i386 has an instruction for directly
// incrementing memory, load-store ISAs such as the PowerPC do not.

// Further reading: RISC (Reduced Instruction Set Computer) and CISC
// (Complex Instruction Set Computer).



// The frysk ISA's word size:

// For this architecture a WORD is the same size as both the general
// purpose register and the program counter; you can assume that a
// word is at least 32-bits in size.  On a 32-bit system (such as
// i386) the word is a 4-byte value, while on a 64-bit system (such as
// x86-64) a word is 8-bytes.

// For instance, to declare "variable" as a word sized memory location
// with initial value "10" use the sequence:

//        WORD(variable, 10)

// Further reading: The ALPHA Instruction Set Architecture. The
// original ALPHA ISA only supported (64-bit) word sized memory
// accesses.

#define WORD(NAME, VALUE) .data ; NAME: .word VALUE ; .text



// General purpose registers:

// The FRYSK-ISA supports a total of four (yes only four, blame the
// i386 :-) general purpose or scratch registers.  These are the only
// operands that can be used in arrithmetic operations; memory
// transfers always use a register as the source/dest address.

// For instance, a conditional jump based on "variable" being 1 can be
// implemented using:

//        WORD(var, 10)
//        ....
//        LOAD_IMMED_WORD(REG0, var)
//        LOAD_IMMED_BYTE(REG1, 1)
//        COMPARE(REG0, REG1)
//        JUMP_EQ(dest)
//        ....
//   dest:
//      ...

// By convention, parameters are also passed between functions using
// these registers (and hence they must not be preserved across across
// function calls).  REG1-REG3 contain param1-param-3 et.al.  REG0
// is reserved for the return value.

// By convention, the system call instruction assumes these registers
// contain the system call information.  REG0 contains the SYSCALL
// number, REG1-REG3 contain the system-call parameters.

#if defined __i386__
#  define REG0 %eax
#  define REG1 %ebx
#  define REG2 %ecx
#  define REG3 %edx
#elif defined __x86_64__
#  define REG0 %rax
#  define REG1 %rdi
#  define REG2 %rsi
#  define REG3 %rdx
#elif defined __powerpc__
#  define REG0 3
#  define REG1 4
#  define REG2 5
#  define REG3 6
#elif defined __powerpc64__
#  define REG0 %gpr3
#  define REG1 %gpr4
#  define REG2 %gpr5
#  define REG3 %gpr6
#else
#  warning "no general purpose registers"
#endif



// Non-operation instruction

// For instance, a source code line that contains exactly three
// instructions can be specified with the sequence:

//    NO_OP ; NO_OP ; NO_OP

#if defined __i386__
#  define NO_OP nop
#elif defined __x86_64__
#  define NO_OP nop
#elif defined __powerpc__
#  define NO_OP nop
#elif defined __powerpc64__
#  define NO_OP nop
#else
#  warning "No no-operation instruction defined"
#endif




// Load-store instructions:

// Only two instructions are available for performing WORD sized
// memory-register transfers.

// For instance, to move a WORD sized value from the address
// designated by REG1 to that designated by REG2 use the sequence:

//   LOAD(REG3,REG1)
//   STORE(REG3, REG2)

//#if defined __i386__
//#  define LOAD(DEST_REG,BASE_REG) //???
//#elif defined __x86_64__
//#  define LOAD(DEST_REG,BASE_REG) //???
//#elif defined __powerpc__
//#  define LOAD(DEST_REG,BASE_REG) //???
//#elif defined __powerpc64__
//#  define LOAD(DEST_REG,BASE_REG) //???
//#else
// #  warning "No load instruction defined"
//#endif

#if defined __i386__
#  define STORE(SOURCE_REG,BASE_REG) mov SOURCE_REG, (BASE_REG)
#elif defined __x86_64__
#  define STORE(SOURCE_REG,BASE_REG) mov SOURCE_REG, (BASE_REG)
#elif defined __powerpc__
#  define STORE(SOURCE_REG,BASE_REG) stw SOURCE_REG, 0(BASE_REG)
#elif defined __powerpc64__
#  define STORE(SOURCE_REG,BASE_REG) stw SOURCE_REG, 0(BASE_REG)
#else
#  warning "No store instruction defined"
#endif



// Load-immediate instruction sequences:

// A very small constant can be loaded directly into a register using
// the LOAD_IMMED_BYTE instruction.

// A WORD sized constant can be loaded directly into a register using
// the LOAD_IMMED_WORD compound instruction.

// For instance, to load the WORD at VARIABLE, use the sequence:

//        LOAD_IMMED_WORD(REG1, variable)
//        LOAD(REG1, REG1)

// And then to increment REG1 by 1 use:

//        LOAD_IMMED_BYTE(REG0, 1)
//        ADD(REG1, REG0)

// Implementation note: The LOAD_IMMED_BYTE macro must be a single
// instruction; while the LOAD_IMMED_WORD instruction can be a
// compound sequence.  For instance, the PowerPC, which has 32-bit
// instructions will implement LOAD_IMMED_WORD as two 16-bit immediate
// instructions.

#if defined __i386__
#  define LOAD_IMMED_BYTE(DEST_REG,CONST) mov $CONST, DEST_REG
#elif defined __x86_64__
#  define LOAD_IMMED_BYTE(DEST_REG,CONST) mov $CONST, DEST_REG
#elif defined __powerpc__
#  define LOAD_IMMED_BYTE(DEST_REG,CONST) li DEST_REG, CONST
#elif defined __powerpc64__
#  define LOAD_IMMED_BYTE(DEST_REG,CONST) li DEST_REG, CONST
#else
#  warning "No load immediate instruction sequence defined"
#endif

#if defined __i386__
#  define LOAD_IMMED_WORD(DEST_REG,CONST) mov $CONST, DEST_REG
#elif defined __x86_64__
#  define LOAD_IMMED_WORD(DEST_REG,CONST) mov $CONST, DEST_REG
#elif defined __powerpc__
#  define LOAD_IMMED_WORD(DEST_REG,CONST) li DEST_REG, CONST
#elif defined __powerpc64__
#  define LOAD_IMMED_WORD(DEST_REG,CONST) li DEST_REG, CONST
#else
#  warning "No load immediate instruction sequence defined"
#endif



// Arithmetic operations.

// These instructions perform register-register arithmetic.
// Register-immediate, register-memory, and three-register arithmetic
// operations are not supported.

// The frysk isa only supports a single register-compare instruction.
// This instruction sets flags that are implicitly used by the
// conditional-jump instructions.  The JUMP instruction must
// immediately follow the COMPARE.  The ADD and SUB instructions make
// the flags undefined.

// For instance, to add one to the register REG1, use the sequence:

//   LOAD_IMMED_BYTE(REG2, 1)
//   ADD(REG1, REG2)

// For instance, to jump to foo when REG1 is not equal to 5, the
// sequence:

//   LOAD_IMMED_BYTE(REG2, 5)
//   COMPARE(REG1, REG2)
//   JUMP_NE(foo)
//   ...
//   foo:

#if defined __i386__
#  define ADD(DEST_REG, SOURCE_REG) addl $DEST_REG, SOURCE_REG
#elif defined __x86_64__
#  define ADD(DEST_REG, SOURCE_REG) addq $DEST_REG, SOURCE_REG
//#elif defined __powerpc__
//#  define ADD(DEST_REG, SOURCE_REG)
//#elif defined __powerpc64__
//#  define ADD(DEST_REG, SOURCE_REG)
#else
#  warning "No register-add instruction defined"
#endif

#if defined __i386__
#  define SUB(DEST_REG, SOURCE_REG) subl $DEST_REG, SOURCE_REG
#elif defined __x86_64__
#  define SUB(DEST_REG, SOURCE_REG) subq $DEST_REG, SOURCE_REG
//#elif defined __powerpc__
//#  define SUB(DEST_REG, SOURCE_REG)
//#elif defined __powerpc64__
//#  define SUB(DEST_REG, SOURCE_REG)
#else
#  warning "No register-subtract instruction defined"
#endif

#if defined __i386__
#  define MOV(DEST_REG, SOURCE_REG) movl DEST_REG, SOURCE_REG
#elif defined __x86_64__
#  define MOV(DEST_REG, SOURCE_REG) movq DEST_REG, SOURCE_REG
//#elif defined __powerpc__
//#  define MOV(DEST_REG, SOURCE_REG)
//#elif defined __powerpc64__
//#  define MOV(DEST_REG, SOURCE_REG)
#else
#  warning "No register-move instruction defined"
#endif

#if defined __i386__
#  define COMPARE(LHS_REG,RHS_REG) cmpl LHS_REG, RHS_REG
#elif defined __x86_64__
#  define COMPARE(LHS_REG,RHS_REG) cmpq LHS_REG, RHS_REG
//#elif defined __powerpc__
//#  define COMPARE(LHS_REG,RHS_REG)
//#elif defined __powerpc64__
//#  define COMPARE(LHS_REG,RHS_REG)
#else
#  warning "No register-compare instruction defined"
#endif



// Jump instructions.

// Conditional, and uncondition jumps, and a register indirect jump
// are supported.

#if defined __i386__
#  define JUMP_EQ(LABEL) je LABEL
#elif defined __x86_64__
#  define JUMP_EQ(LABEL) je LABEL
#elif defined __powerpc__
#  define JUMP_EQ(LABEL) bf eq, LABEL
#elif defined __powerpc64__
#  define JUMP_EQ(LABEL) bf eq, LABEL
#else
#  warning "No jump equal instruction defined"
#endif

#if defined __i386__
#  define JUMP_NE(LABEL) jne LABEL
#elif defined __x86_64__
#  define JUMP_NE(LABEL) jne LABEL
#elif defined __powerpc__
#  define JUMP_NE(LABEL) bf ne, LABEL
#elif defined __powerpc64__
#  define JUMP_NE(LABEL) bf ne, LABEL
#else
#  warning "No jump not-equal instruction defined"
#endif

#if defined __i386__
#  define JUMP(LABEL) jmp LABEL
#elif defined __x86_64__
#  define JUMP(LABEL) jmp LABEL
#elif defined __powerpc__
#  define JUMP(LABEL) b LABEL
#elif defined __powerpc64__
#  define JUMP(LABEL) b LABEL
#else
#  warning "No unoconditional jump instruction defined"
#endif

#if defined __i386__
#  define JUMP_REG(REG) jmp (REG)
#elif defined __x86_64__
#  define JUMP_REG(REG) jmp (REG)
#elif defined __powerpc__
#  define JUMP_REG(REG) br (REG)
#elif defined __powerpc64__
#  define JUMP_REG(REG) br (REG)
#else
#  warning "No indirect or register jump instruction defined"
#endif



// Host ABI calling convetion instructions.

// This collection of compound instructions let assembler code work
// with the host's ABI's "traditional" function calling convention.
// The function is assembled from the following building blocks:

// FUNCTION_BEGIN(FUNC,SLOTS): This declares the function, defining
// the global symbol FUNC.

// FUNCTION_PROLOGUE(FUNC,SLOTS): This creates the new stack frame,
// initializing any frame-base register and pre-allocating SLOTS word
// sized locations on the stack.

// FUNCTION_EPILOGUE(FUNC,SLOTS): This tears down the created stack
// frame, seting everything up ready for the return instruction.

// FUNCTION_RETURN(FUNC,SLOTS): This executs the single final return
// instruction.

// FUNCTION_END(FUNC,SLOTS): This is closes out the function, in
// particular determining information such as the function's size.

// For instance, a host ABI conformant function, that includes unwind
// information is written as:

//        FUNCTION_BEGIN(func, 0)
//        FUNCTION_PROLOGUE(func, 0)
//          ... body ...
//        FUNCTION_EPILOGUE(func, 0)
//        FUNCTION_RETURN(func, 0)
//        FUNCTION_END(func, 0)

// And the function is called with:

//        FUNCTION_CALL(func)

// The "main" function is special.  Instead of FUNCTION_PROLOGUE and
// FUNCTION_EPILOGUE, MAIN_PROLOGUE and MAIN_EPILOGUE are used:

// MAIN_PROLOGUE, in addition to creating a frame, copies ARGC to
// REG1, ARGV is in REG2, and ENVP in REG3; REG0 is undefined.

// MAIN_EPILOGUE, in addition to tearing down the frame, copies REG0
// to the return register so that it is used as the program's exit
// status.

// For instance, this program, takes ARGC and exits with that -1:

//        FUNCTION_BEGIN(main,0)
//        MAIN_PROLOGUE(0)
//          LOAD_IMMED_BYTE(REG2,1) ;; Decrement ARGC
//          SUB(REG1,REG2)
//          MOVE(REG0,REG1) ;; Move ARGC(REG1) to return(REG0)
//        MAIN_EPILOGUE(0)
//        FUNCTION_RETURN(main,0)
//        FUNCTION_END(main,0)

// Should scratch space be required, this can be allocated on the
// stack by specifying a non-zero number of word-sized stack slots.
// These slots can then be accessed using STACK_LOAD and STACK_STORE
// instructions.

// For instance, to save REG1 on the stack before making a further
// function call, use:

//        FUNCTION_PROLOGUE(foo, 1)
//        STACK_STORE(REG1, 0)
//        FUNCTION_CALL(bar)

// Implementation note: The stack SLOT is not implemented on all
// architectures.

// Implementation note: Parameter passing following the host's
// calling-conventions is _not_ supported!  By convention, parameters
// are passed in REG's 1-3, and the return value is in REG0.

#if defined __i386__
#  define FUNCTION_CALL(LABEL) call LABEL
#elif defined __x86_64__
#  define FUNCTION_CALL(LABEL) call LABEL
#elif defined __powerpc__
#  define FUNCTION_CALL(LABEL) bl LABEL
#elif defined __powerpc64__
#  define FUNCTION_CALL(LABEL) bl LABEL
#else
#  warning "No function-call instruction defined"
#endif

#define SANE_FUNCTION_BEGIN(FUNC) \
	.text ; \
	.global FUNC ; \
	.type FUNC, @function ; \
    FUNC:
#if defined __i386__
#  define FUNCTION_BEGIN(FUNC,SLOTS) SANE_FUNCTION_BEGIN(FUNC)
#elif defined __x86_64__
#  define FUNCTION_BEGIN(FUNC,SLOTS) SANE_FUNCTION_BEGIN(FUNC)
#elif defined __powerpc__
#  define FUNCTION_BEGIN(FUNC,SLOTS) SANE_FUNCTION_BEGIN(FUNC)
#elif defined __powerpc64__
#  define FUNCTION_BEGIN(FUNC) \
	.section ".opd","aw" ; \
	.global FUNC ; \
	.align 3 ; \
    FUNC: ; \
	.quad _##FUNC, .TOC.@tocbase, 0 ; \
	.text ; \
    _##FUNC:
#else
#  warning "no function-begin defined"
#endif

#if defined __i386__
#  define FUNCTION_PROLOGUE(FUNC,SLOTS) pushl %ebp ; movl %esp, %ebp
#elif defined __x86_64__
#  define FUNCTION_PROLOGUE(FUNC,SLOTS) pushq %rbp; movq %rsp, %rbp
#elif defined __powerpc__
#  define FUNCTION_PROLOGUE(FUNC,SLOTS) \
	stwu    1, -32(1)  ; \
	mflr    0          ; \
	stw     0,  36(1)  ; \
	stw    31,  28(1)  ; \
        mr     31,   1     ; \
        stw  REG1,  12(31) ; \
	stw  REG2,  16(31) ; \
	stw  REG3,  20(31) ; \
	stw  REG4,  24(31)
#elif defined __powerpc64__
#  define FUNCTION_PROLOGUE(FUNC,SLOTS) \
	push %gpr0; \
	push %gpr3
#else
#  warning "No function-prologue compound instruction defined"
#endif

#if defined __i386__
#  define FUNCTION_EPILOGUE(FUNC,SLOTS) popl %ebp
#elif defined __x86_64__
#  define FUNCTION_EPILOGUE(FUNC,SLOTS) leave
#elif defined __powerpc__
#  define FUNCTION_EPILOGUE(FUNC,SLOTS) \
	lwz  REG4, 24(31) ; \
        lwz  REG3, 20(31) ; \
	lwz  REG2, 16(31) ; \
	lwz  REG1, 12(31) ; \
	lwz    11,  0(1)  ; \
	lwz     0,  4(11) ; \
	mtlr    0         ; \
	lwz    31, -4(11) ; \
	mr      1, 11
#elif defined __powerpc64__
#  define FUNCTION_EPILOGUE(FUNC,SLOTS) pop %gpr0
#else
#  warning "No function-epilogue instruction sequence defined"
#endif

#if defined __i386__
#  define FUNCTION_RETURN(FUNC,SLOTS) ret
#elif defined __x86_64__
#  define FUNCTION_RETURN(FUNC,SLOTS) ret
#elif defined __powerpc__
#  define FUNCTION_RETURN(FUNC,SLOTS) blr
#elif defined __powerpc64__
#  define FUNCTION_RETURN(FUNC,SLOTS) blr
#else
#  warning "No function-epilogue instruction sequence defined"
#endif

#define SANE_FUNCTION_END(FUNC) .size FUNC, . - FUNC
#if defined __i386__
#  define FUNCTION_END(FUNC,SLOTS) SANE_FUNCTION_END(FUNC)
#elif defined __x86_64__
#  define FUNCTION_END(FUNC,SLOTS) SANE_FUNCTION_END(FUNC)
#elif defined __powerpc__
#  define FUNCTION_END(FUNC,SLOTS) SANE_FUNCTION_END(FUNC)
#elif defined __powerpc64__
#  define FUNCTION_END(FUNC,SLOTS) .size FUNC, .-_##FUNC
#else
#  warning "No function-epilogue instruction sequence defined"
#endif

#if defined __i386__
#  define MAIN_PROLOGUE(SLOTS) \
	pushl %ebp ; \
	movl %esp, %ebp ; \
	movl 8(%ebp), REG1 ; \
	movl 12(%ebp), REG2 ; \
        movl 16(%ebp), REG3
#elif defined __x86_64__
#  define MAIN_PROLOGUE(SLOTS) \
	pushq %rbp ; \
	movq %rsp, %rbp // ARGC, ARGV, and ENVP already in correct registers.
#elif defined __powerpc__
//In PowerPC ABI argc comes by default in reg 3 (Frysk REG1) and argv in reg 4 (Frysk REG2) 
#define MAIN_PROLOGUE(SLOTS) FUNCTION_PROLOGUE(main,SLOTS)
#elif defined __powerpc64__
#define MAIN_PROLOGUE(SLOTS) FUNCTION_PROLOGUE(main,SLOTS)
#endif

#if defined __i386__
#  define MAIN_EPILOGUE(SLOTS) FUNCTION_EPILOGUE(main,SLOTS)
#elif defined __x86_64__
#  define MAIN_EPILOGUE(SLOTS) FUNCTION_EPILOGUE(main,SLOTS)
#elif defined __powerpc__
#  define MAIN_EPILOGUE(SLOTS) FUNCTION_EPILOGUE(main,SLOTS)
#elif defined __powerpc64__
#  define MAIN_EPILOGUE(SLOTS) FUNCTION_EPILOGUE(main,SLOTS)
#endif

//#if defined __i386__
//#  define STACK_LOAD(DEST_REG,SLOT)
//#elif defined __x86_64__
//#  define STACK_LOAD(DEST_REG,SLOT)
//#elif defined __powerpc__
//#  define STACK_LOAD(DEST_REG,SLOT)
//#elif defined __powerpc64__
//#  define STACK_LOAD(DEST_REG,SLOT)
//#else
//#  warning "No stack-load instruction defined"
//#endif

//#if defined __i386__
//#  define STACK_STORE(SOURCE_REG,SLOT)
//#elif defined __x86_64__
//#  define STACK_STORE(SOURCE_REG,SLOT)
//#elif defined __powerpc__
//#  define STACK_STORE(SOURCE_REG,SLOT)
//#elif defined __powerpc64__
//#  define STACK_STORE(SOURCE_REG,SLOT)
//#else
//#  warning "No stack-store instruction sequence defined"
//#endif



// System calls.

// The frysk ABI supports system calls.  It is assumed that REG0
// contains the system call number, while REG1-REG3 contain the system
// call parameters.

// For instance, the getpid system call can be made using:

//         LOAD_IMMED_WORD(REG0, SYSCALL_getpid)
//         SYSCALL

#if defined __i386__
#  define SYSCALL int $0x80
#elif defined __x86_64__
#  define SYSCALL syscall
//#elif defined __powerpc__
//#elif defined __powerpc64__
#else
#  warning "No stack-store instruction sequence defined"
#endif



// The FRYSK ABI

// In addition to the standard ABI, frysk assembler can use a minimal
// frameless ABI.  As part of the call sequence, the ABI saves the
// return address in REG0.  And a function return is implemented by
// jumping to the saved REG0.

// For instance:

//         LOAD_IMMED_BYTE(REG1, 1) ;; parameter 1
//         LOAD_IMMED_WORD(REG0, .1) ;; return address
//         JUMP(foo)
//     .1: ....
//     ...
//     foo:
//         MOVE(REG1, REG0) ;; Save return address in REG1
//         NO_OP; NO_OP
//         LOAD_IMMED_WORD(REG0, .1) ;; return address
//         JUMP(bar)
//     .1: NO_OP; NO_OP
//         JUMP_REG(REG1) ;; REG1 has the saved return address
//         ....
//     bar:
//         JUMP_REG(REG0) ;; just return

// Implementation note: Other than for testing frameless function call
// edge cases, the ABI is pretty much useless.

// Implementation note: This abi requires custom CFI information at
// present.



.section        .note.GNU-stack,"",@progbits
.text

#endif
