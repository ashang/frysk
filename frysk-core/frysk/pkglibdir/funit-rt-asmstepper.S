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



FUNCTION_BEGIN(main,0)
	
MAIN_PROLOGUE(0)
	
	LOAD_IMMED_BYTE (REG0, 0x14)
	LOAD_IMMED_BYTE (REG1, 1)
	LOAD_IMMED_BYTE (REG2, 2)
	LOAD_IMMED_BYTE (REG3, 3)
//	SYSCALL
loop:
	NO_OP												// _asmSingleStepStart_
	NO_OP												// _asmSingleStepFinish_
	NO_OP
	NO_OP ; NO_OP ; NO_OP								// _asmMultiStepStart_
	NO_OP												// _asmMultiStepFinish_
	NO_OP ; NO_OP ; NO_OP
	NO_OP;
	LOAD_IMMED_BYTE(REG0, 1) ; COMPARE(REG1, REG0)
	JUMP_NE (fail)
	LOAD_IMMED_BYTE(REG0, 2) ; COMPARE(REG2, REG0)
	JUMP_NE (fail)
	LOAD_IMMED_BYTE(REG0, 3) ; COMPARE(REG3, REG0)
	JUMP_NE (fail)
	JUMP (loop)                                       	// _asmStepJump_
	MAIN_EPILOGUE(0)
	FUNCTION_RETURN(main,0)
	FUNCTION_END(main,0)

fail: JUMP (0)
