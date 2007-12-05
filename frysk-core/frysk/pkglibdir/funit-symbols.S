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

#define SIZE(X) .size X, .-X
#define GLOBAL(X) \
	.global X 		;  \
	.type X, @function	; \
	X:		
#define LOCAL(X) \
	.local X 		;  \
	.type X, @function	; \
	X:		

# A global symbol that includes its size.
GLOBAL(global_st_size)
	LOAD_IMMED_BYTE (REG0, 0)
	STORE (REG0, REG0)
	NO_OP
	SIZE(global_st_size)



# A local symbol that includes its size.
LOCAL(local_st_size)	
	LOAD_IMMED_BYTE (REG0, 0)
	STORE (REG0, REG0)
	NO_OP
	SIZE(local_st_size)



# A global symbol that has zero size.
GLOBAL(global_st_size_0)
	LOAD_IMMED_BYTE (REG0, 0)
	STORE (REG0, REG0)
	NO_OP
	


# A local symbol that has zero size.
LOCAL(local_st_size_0)
	LOAD_IMMED_BYTE (REG0, 0)
	STORE (REG0, REG0)
	NO_OP
	


# A global symbol, with size, that contains a nested global and local
# symbols each also with sizes.
GLOBAL(global_outer)
	STORE(REG0, REG0)
	NO_OP
GLOBAL(global_in_global)
	STORE (REG0, REG0)
	NO_OP
	SIZE(global_in_global)
LOCAL(local_in_global)
	STORE (REG0, REG0)
	NO_OP
	SIZE(local_in_global)
.Lglobal_outer:
	STORE (REG0, REG0)
	NO_OP
	SIZE(global_outer)


	
# Code with no symbol after a global symbol with size.
.Lno_global:
	STORE (REG0, REG0)
	NO_OP
	


# A global symbol, with size, that contains a nested global and local
# symbols each also with sizes.
LOCAL(local_outer)
	STORE(REG0,REG0)
	NO_OP
GLOBAL(global_in_local)
	STORE (REG0, REG0)
	NO_OP
	SIZE(global_in_local)
LOCAL(local_in_local)
	STORE (REG0, REG0)
	NO_OP
	SIZE(local_in_local)
.Llocal_outer:
	STORE (REG0, REG0)
	NO_OP
	SIZE(local_outer)



# Code with no symbol after a local symbol with size.
.Lno_local:
	STORE (REG0, REG0)
	NO_OP
	

	
# A sized global symbol that contains local/global symbols of zero
# size.

GLOBAL(global_after_0)
	NO_OP
GLOBAL(global_0_in_global)
	STORE(REG0, REG0)
	NO_OP
LOCAL(local_0_in_global)
	STORE(REG0, REG0)
	NO_OP
.Lglobal_after_0:
	STORE(REG0, REG0)
	SIZE(global_after_0)



# Code with no symbol after a global with nexted zero-sized symbols.
.Lafter_global_containing_0:
	STORE(REG0, REG0)
	NO_OP



# A sized local symbol that contains local/global symbols of zero
# size.

LOCAL(local_after_0)
	NO_OP
GLOBAL(global_0_in_local)
	STORE(REG0, REG0)
	NO_OP
LOCAL(local_0_in_local)
	STORE(REG0, REG0)
	NO_OP
.Llocal_after_0:
	STORE(REG0, REG0)
	SIZE(local_after_0)



# Code with no symbol after a local with nexted zero-sized symbols.
.Lafter_local_containing_0:
	STORE(REG0, REG0)
	NO_OP


# A small global symbol at a large global symbol.

GLOBAL(large_global_at_small_global)
GLOBAL(small_global_at_large_global)
	STORE(REG0,REG0)
	NO_OP
	SIZE(small_global_at_large_global)
	NO_OP
	SIZE(large_global_at_small_global)



# A small local symbol at a large global symbol

GLOBAL(large_global_at_small_local)
LOCAL(small_local_at_large_global)
	STORE(REG0,REG0)
	NO_OP
	SIZE(small_local_at_large_global)
	NO_OP
	SIZE(large_global_at_small_local)



# A small global symbol at a large local symbol.

LOCAL(large_local_at_small_global)
GLOBAL(small_global_at_large_local)
	STORE(REG0,REG0)
	NO_OP
	SIZE(small_global_at_large_local)
	NO_OP
	SIZE(large_local_at_small_global)
	

	
# A small local symbol at a large local symbol.

LOCAL(large_local_at_small_local)
LOCAL(small_local_at_large_local)
	STORE(REG0,REG0)
	NO_OP
	SIZE(small_local_at_large_local)
	NO_OP
	SIZE(large_local_at_small_local)
	


// Dependant on ARGC jump to a symbol with either global or
// local scope.

	FUNCTION_BEGIN(main,0)
	MAIN_PROLOGUE(0)
	#REG1==ARGC
	LOAD_IMMED_BYTE(REG0, 0)
# global_st_size?
	LOAD_IMMED_BYTE(REG3, 2)
	COMPARE(REG3, REG1)
	JUMP_EQ(global_st_size)
# local_st_size?
	LOAD_IMMED_BYTE(REG3, 3)
	COMPARE(REG3, REG1)
	JUMP_EQ(local_st_size)
# global_st_size_0.
	LOAD_IMMED_BYTE(REG3, 4)
	COMPARE(REG3, REG1)
	JUMP_EQ(global_st_size_0)
# local_st_size_0?
	LOAD_IMMED_BYTE(REG3, 5)
	COMPARE(REG3, REG1)
	JUMP_EQ(local_st_size_0)
# global_in_global?
	LOAD_IMMED_BYTE(REG3, 6)
	COMPARE(REG3, REG1)
	JUMP_EQ(global_in_global)
# local_in_global?
	LOAD_IMMED_BYTE(REG3, 7)
	COMPARE(REG3, REG1)
	JUMP_EQ(local_in_global)
# global_in_local?
	LOAD_IMMED_BYTE(REG3, 8)
	COMPARE(REG3, REG1)
	JUMP_EQ(global_in_local)
# local_in_local?
	LOAD_IMMED_BYTE(REG3, 9)
	COMPARE(REG3, REG1)
	JUMP_EQ(local_in_local)
# global_outer?
	LOAD_IMMED_BYTE(REG3, 10)
	COMPARE(REG3, REG1)
	JUMP_EQ(.Lglobal_outer)
# local_outer?
	LOAD_IMMED_BYTE(REG3, 11)
	COMPARE(REG3, REG1)
	JUMP_EQ(.Llocal_outer)
# no_global?
	LOAD_IMMED_BYTE(REG3, 12)
	COMPARE(REG3, REG1)
	JUMP_EQ(.Lno_global)
# no_local?
	LOAD_IMMED_BYTE(REG3, 13)
	COMPARE(REG3, REG1)
	JUMP_EQ(.Lno_local)
# global_0_in_global?
	LOAD_IMMED_BYTE(REG3, 14)
	COMPARE(REG3, REG1)
	JUMP_EQ(global_0_in_global)
# local_0_in_global?
	LOAD_IMMED_BYTE(REG3, 15)
	COMPARE(REG3, REG1)
	JUMP_EQ(local_0_in_global)
# global_0_in_local?
	LOAD_IMMED_BYTE(REG3, 16)
	COMPARE(REG3, REG1)
	JUMP_EQ(global_0_in_local)
# local_0_in_local?
	LOAD_IMMED_BYTE(REG3, 17)
	COMPARE(REG3, REG1)
	JUMP_EQ(local_0_in_local)
# global_after_0?
	LOAD_IMMED_BYTE(REG3, 18)
	COMPARE(REG3, REG1)
	JUMP_EQ(.Lglobal_after_0)
# local_after_0?
	LOAD_IMMED_BYTE(REG3, 19)
	COMPARE(REG3, REG1)
	JUMP_EQ(.Llocal_after_0)
# small_global_at_large_global?
	LOAD_IMMED_BYTE(REG3, 20)
	COMPARE(REG3, REG1)
	JUMP_EQ(small_global_at_large_global)
# small_local_at_large_global?
	LOAD_IMMED_BYTE(REG3, 21)
	COMPARE(REG3, REG1)
	JUMP_EQ(small_local_at_large_global)
# small_global_at_large_local?
	LOAD_IMMED_BYTE(REG3, 22)
	COMPARE(REG3, REG1)
	JUMP_EQ(small_global_at_large_local)
# small_local_at_large_local?
	LOAD_IMMED_BYTE(REG3, 23)
	COMPARE(REG3, REG1)
	JUMP_EQ(small_local_at_large_local)
# after_global_containing_0?
	LOAD_IMMED_BYTE(REG3, 24)
	COMPARE(REG3, REG1)
	JUMP_EQ(.Lafter_global_containing_0)
# after_local_containing_0?
	LOAD_IMMED_BYTE(REG3, 25)
	COMPARE(REG3, REG1)
	JUMP_EQ(.Lafter_local_containing_0)

	LOAD_IMMED_BYTE(REG0, SYS_write)
	LOAD_IMMED_BYTE(REG1, 1)

	LOAD_IMMED_WORD(REG2, usage)
	LOAD_IMMED_WORD(REG3, .usage-usage)

	SYSCALL
		// exit with a non-zero status
	LOAD_IMMED_BYTE(REG0, 1)
	MAIN_EPILOGUE(0)
	FUNCTION_RETURN(main,0)
	FUNCTION_END(main,0)

//In PowerPC the next section can not stay in a executable segment
.data
usage:	.asciz "Usage:\r\n\
	ARG1 ...\r\n\
The number of arguments determines the symbol at which this program\r\n\
crashes:\r\n\
1: Global symbol with size (global_st_size)\r\n\
2: Local symbol with size (local_st_size)\r\n\
3: Global symbol with no size (global_st_size_0)\r\n\
4: Local symbol with no size (local_st_size_0)\r\n\
5: Global nested within global (global_in_global)\r\n\
6: Local nested within global (local_in_global)\r\n\
7: Global nested within local (global_in_local)\r\n\
8: Local nested within local (local_in_local)\r\n\
9: Global symbol after nested symbols (global_outer)\r\n\
10: Local symbol after nested symbols (local_outer)\r\n\
11: Region with no symbol after a global (no_global)\r\n\
12: Region with no symbol after a local (no_local)\r\n\
13: Global symbol with no size nested in global (global_0_in_global)\r\n\
14: Local symbol with no size nested within global (local_0_in_global)\r\n\
15: Global symbol with no size nested in local (global_0_in_local)\r\n\
16: Local symbol with no size nested within local (local_0_in_local)\r\n\
17: Global after nested size 0 symbols (global_after_0)\r\n\
18: Local after nested size 0 symbols (local_after_0)\r\n\
19: Small global at large global (small_global_at_large_global\r\n\
20: Small local at large global (small_local_at_large_global\r\n\
21: Small global at large local (small_global_at_large_local\r\n\
22: Small local at large local (small_local_at_large_local\r\n\
23: No symbol after global with nested zero-sized symbols\r\n\
24: No symbol after local with nested zero-sized symbols\r\n\
"
.usage:
