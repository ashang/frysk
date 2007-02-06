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

	.text

# Method that segfaults, has a global symbol, with a defined size.
.globl global_st_size
	.type	global_st_size, @function
global_st_size:
	ENTER
	LOAD_IMMED (REG1, 0)
	STORE (REG1, REG1)
	EXIT
	.size	global_st_size, .-global_st_size
	
# Method that segfaults, has a local symbol, with a defined size.
	.type	local_st_size, @function
local_st_size:
	ENTER
	LOAD_IMMED (REG1, 0)
	STORE (REG1, REG1)
	EXIT
	.size	local_st_size, .-local_st_size

# Method that segfaults, has a global symbol, with undefined (0) size.	
.global global_st_size_0
	.type	global_st_size_0, @function
global_st_size_0:
	ENTER
	LOAD_IMMED (REG1, 0)
	STORE (REG1, REG1)
	EXIT
	
# Method that segfaults, has a local symbol, with undefined (0) size.
	.type local_st_size_0, @function
local_st_size_0:
	ENTER
	LOAD_IMMED (REG1, 0)
	STORE (REG1, REG1)
	EXIT
	
.globl main
	.type	main, @function
main:
	ENTER_MAIN(REG3, REG4)
# If argc = 1 run with global_st_size.
	COMPARE_IMMED(REG3, 1)
	JUMP_NE(.L10)
	CALL(global_st_size)
.L10:
# If argc = 2 run with local_st_size.
	COMPARE_IMMED(REG3, 2)
	JUMP_NE(.L12)
	CALL(local_st_size)
.L12:
# If argc = 3 run with global_st_size_0.
	COMPARE_IMMED(REG3, 3)
	JUMP_NE(.L14)
	CALL(global_st_size_0)
.L14:
# If argc = 4 run with local_st_size_0.
	COMPARE_IMMED(REG3, 4)
	JUMP_NE(.L16)
	CALL(local_st_size_0)
.L16:
	EXIT
	.size	main, .-main	
	.section	.note.GNU-stack,"",@progbits
