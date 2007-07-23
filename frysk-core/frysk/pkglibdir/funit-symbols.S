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

# A global symbol that includes its size.
.globl global_st_size
	.type	global_st_size, @function
global_st_size:
	LOAD_IMMED_BYTE (REG0, 0)
	STORE (REG0, REG0)
	NO_OP
	.size	global_st_size, .-global_st_size
	
# A local symbol that includes its size.
.local local_st_size
	.type	local_st_size, @function
local_st_size:
	LOAD_IMMED_BYTE (REG0, 0)
	STORE (REG0, REG0)
	NO_OP
	.size	local_st_size, .-local_st_size

# A global symbol that has zero size.
.global global_st_size_0
	.type	global_st_size_0, @function
global_st_size_0:
	LOAD_IMMED_BYTE (REG0, 0)
	STORE (REG0, REG0)
	NO_OP
	
# A local symbol that has zero size.
.local local_st_size_0
	.type local_st_size_0, @function
local_st_size_0:
	LOAD_IMMED_BYTE (REG0, 0)
	STORE (REG0, REG0)
	NO_OP
	
# A global symbol, with size, that contains a nested global and local
# symbols each also with sizes.
.global global_outer
	.type global_outer, @function
.global global_inner
	.type global_inner, @function
global_in_global:
	STORE (REG0, REG0)
	NO_OP
	.size global_in_global, .-global_in_global
.local local_in_global
	.type local_in_global, @function
local_in_global:
	STORE (REG0, REG0)
	NO_OP
	.size local_in_global, .-local_in_global
.Lglobal_outer:
	STORE (REG0, REG0)
	NO_OP
	.size global_outer, .-global_outer

# Code with no symbol after a global symbol with size.
.Lno_global:
	STORE (REG0, REG0)
	NO_OP
	
# A global symbol, with size, that contains a nested global and local
# symbols each also with sizes.
.local local_outer
	.type local_outer, @function
.global global_in_local
	.type global_in_local, @function
global_in_local:
	STORE (REG0, REG0)
	NO_OP
	.size global_in_local, .-global_in_local
.local local_in_local
	.type local_in_local, @function
local_in_local:
	STORE (REG0, REG0)
	NO_OP
	.size local_in_local, .-local_in_local
.Llocal_outer:
	STORE (REG0, REG0)
	NO_OP
	.size local_outer, .-local_outer

# Code with no symbol after a local symbol with size.
.Lno_local:
	STORE (REG0, REG0)
	NO_OP
	
# A sized global symbol that contains local/global symbols of zero
# size.

.global global_after_0
	.type global_after_0, @function
global_after_0:
	NO_OP
.global global_0_in_global
	.type global_0_in_global, @function
global_0_in_global:
	STORE(REG0, REG0)
	NO_OP
.local local_0_in_global
	.type local_0_in_global, @function
local_0_in_global:
	STORE(REG0, REG0)
	NO_OP
.Lglobal_after_0:
	STORE(REG0, REG0)
	.size global_after_0, .-global_after_0	

# A sized local symbol that contains local/global symbols of zero
# size.

.local local_after_0
	.type local_after_0, @function
local_after_0:
	NO_OP
.global global_0_in_local
	.type global_0_in_local, @function
global_0_in_local:
	STORE(REG0, REG0)
	NO_OP
.local local_0_in_local
	.type local_0_in_local, @function
local_0_in_local:
	STORE(REG0, REG0)
	NO_OP
.Llocal_after_0:
	STORE(REG0, REG0)
	.size local_after_0, .-local_after_0	


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
"
.usage:
