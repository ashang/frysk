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

#include "frysk-asm.h"

	// Calls the function instruction twice so the test
	// harness (TestInstructions) can step through it once
	// recording all releavant information and then can run
	// it another time to check that it runs exactly the same
	// in the presence of Code breakpoint (and/or other) observers.	
	FUNCTION_BEGIN(main,0)
	MAIN_PROLOGUE(0)
	FUNCTION_CALL(instructions)
	FUNCTION_CALL(instructions)	
	MAIN_EPILOGUE(0)
	FUNCTION_RETURN(main,0)
	FUNCTION_END(main,0)

	// Make this function global so the test harness can find
	// the begin and end of it while stepping and setting
	// breakpoints. The test harness will use the istart and iend
	// markers and assume that those two addresses are used only
	// once per run of this function and that when this function is
	// called multiple times (see main) the exact same instruction
	// sequence is executed.
	.globl istart,iend
	FUNCTION_BEGIN(instructions,0)
	FUNCTION_PROLOGUE(instructions,0)
istart:	NO_OP
	NO_OP
	JUMP(label1)
label2:	JUMP(label3)
label1: NO_OP
	JUMP(label2)
label3:	NO_OP
	FUNCTION_CALL(subfunc)
	FUNCTION_CALL(subfunc)
iend:	NO_OP
	FUNCTION_EPILOGUE(instructions,0)
	FUNCTION_RETURN(instructions,0)
	FUNCTION_END(instructions,0)

	// Non-global helper function called from instructions to
	// check enter/exit of functions.
	FUNCTION_BEGIN(subfunc,0)
	FUNCTION_PROLOGUE(subfunc,0)
	LOAD_IMMED(REG2,1)
sublabel:
	LOAD_IMMED(REG0, 1)
	COMPARE(REG2,REG0)
	JUMP_NE(sublabel) // never taken
	LOAD_IMMED(REG2,2)
	LOAD_IMMED(REG0, 1)
	COMPARE(REG2,REG0)
	JUMP_NE(subexit) // always taken
	JUMP(sublabel)
subexit:	
	FUNCTION_EPILOGUE(subfunc,0)
	FUNCTION_RETURN(subfunc,0)
	FUNCTION_END(subfunc,0)
