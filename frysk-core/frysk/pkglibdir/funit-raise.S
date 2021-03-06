// This file is part of the program FRYSK.
//
// Copyright 2007, 2008 Red Hat Inc.
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
// License in all respects for all o;f the FRYSK code and other code
// used in conjunction with FRYSK except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

#include "frysk-asm.h"
#include <sys/syscall.h>

// Signal that terminates and signal that gets ignored by default.
#define SIGHUP	1
#define SIGURG	23

// Architecture dependent divide by zero and illegal instruction operator.
#if defined(__i386__) || defined(__x86_64__)
	#define DIV_ZERO(REG)	div REG;
#elif defined __powerpc64__
	#define DIV_ZERO(REG)	li 5, 0; divw 6, REG, 5
#elif defined __powerpc__
	#define DIV_ZERO(REG)	li 5, 0; divw 6, REG, 5
#else
	#error unsuported architecture
#endif

// Mainly used to define the global label to beak on.
// The raise functions are jump targets for main (based on argc).
#define RAISE_FUNCTION_START(X)		\
	.global X 		;	\
	.global X_label		;	\
	.type X, @function	;	\
	X:		
#define RAISE_FUNCTION_END(X) .size X, .-X

RAISE_FUNCTION_START(div_zero)
	LOAD_BYTE_IMMED(REG0, 0)
	LOAD_BYTE_IMMED(REG1, 0)
div_zero_label:
	DIV_ZERO(REG1)
	NO_OP			;  // never reached
RAISE_FUNCTION_END(div_zero)

RAISE_FUNCTION_START(bad_addr_segv)
	LOAD_BYTE_IMMED (REG0, 0)
bad_addr_segv_label:
	STORE_REGISTER(REG0, REG0)
	NO_OP			;  // never reached
RAISE_FUNCTION_END(bad_addr_segv)

RAISE_FUNCTION_START(bad_inst_ill)
bad_inst_ill_label:
	ILLEGAL_INSTRUCTION
	NO_OP			;  // never reached
RAISE_FUNCTION_END(bad_inst_ill)

RAISE_FUNCTION_START(term_sig_hup)
        LOAD_BYTE_IMMED(REG0, SYS_gettid)
	SYSCALL
	LOAD_BYTE_IMMED(REG1,0)
	ADD(REG1,REG0)
	LOAD_BYTE_IMMED(REG2,SIGHUP)
        LOAD_BYTE_IMMED(REG0, SYS_tkill)
term_sig_hup_label:
	SYSCALL
        NO_OP                   ;   // never reached
RAISE_FUNCTION_END(term_sig_hup)

RAISE_FUNCTION_START(ign_sig_urg)
        LOAD_BYTE_IMMED(REG0, SYS_gettid)
	SYSCALL
	LOAD_BYTE_IMMED(REG1,0)
	ADD(REG1,REG0)
	LOAD_BYTE_IMMED(REG2,SIGURG)
        LOAD_BYTE_IMMED(REG0, SYS_tkill)
ign_sig_urg_label:	
	SYSCALL
        NO_OP                   ;   // Reached!
	// Exit nicely
	LOAD_BYTE_IMMED(REG0, 0)	
	JUMP(exit_main)
RAISE_FUNCTION_END(ign_sig_urg)
	
FUNCTION_BEGIN(main,0)
	MAIN_PROLOGUE(0)

	// Get argc (REG1) and see what the user wanted.
	LOAD_BYTE_IMMED(REG3, 2)
	COMPARE(REG3, REG1)
	JUMP_EQ(div_zero)

	LOAD_BYTE_IMMED(REG3, 3)
	COMPARE(REG3, REG1)
	JUMP_EQ(bad_addr_segv)

	LOAD_BYTE_IMMED(REG3, 4)
	COMPARE(REG3, REG1)
	JUMP_EQ(bad_inst_ill)

	LOAD_BYTE_IMMED(REG3, 5)
	COMPARE(REG3, REG1)
	JUMP_EQ(term_sig_hup)

	LOAD_BYTE_IMMED(REG3, 6)
	COMPARE(REG3, REG1)
	JUMP_EQ(ign_sig_urg)

	// Unknown, print usage.
	LOAD_BYTE_IMMED(REG0, SYS_write)
	LOAD_BYTE_IMMED(REG1, 1)

	LOAD_REGISTER_IMMED(REG2, usage)
	LOAD_REGISTER_IMMED(REG3, .usage-usage)

	SYSCALL

	// Non-zero return
	LOAD_BYTE_IMMED(REG0, 1)
	
exit_main:	
	MAIN_EPILOGUE(0)
	FUNCTION_RETURN(main,0)
FUNCTION_END(main,0)

.data
//In PowerPC you cant have unaligned upcodes in executable segment
usage:	.asciz "Usage:\r\n\
	ARG1 ...\r\n\
The number of arguments determines the function which this program\r\n\
calls and how it will raise a signal/trap because:\r\n\
1: Arithmetic exception (div_zero)\r\n\
2: Illegal address (bad_addr_segv)\r\n\
3: Illegal instruction (bad_inst_ill)\r\n\
4: SIGHUP terminate signal (term_sig_hup)\r\n\
5: SIGURG ignore signal (ign_sig_urg)\r\n\
"
.usage:
