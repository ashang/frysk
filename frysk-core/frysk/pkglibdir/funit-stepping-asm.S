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

	FUNCTION_BEGIN(fifth,1)
	FUNCTION_PROLOGUE(fifth,1) ; NO_OP	// _stepOverPrologue_
	FUNCTION_EPILOGUE(fifth,1)
	FUNCTION_RETURN(fifth,1)
	FUNCTION_END(fifth,1)
	
	
	FUNCTION_BEGIN(fourth,0)
	FUNCTION_PROLOGUE(fourth,0)
	NO_OP					// _stepAdvanceStart_
	NO_OP
	FUNCTION_CALL(fifth)
	FUNCTION_EPILOGUE(fourth,0)
	FUNCTION_RETURN(fourth,0)
	FUNCTION_END(fourth,0)

	FUNCTION_BEGIN(third,0)
	FUNCTION_PROLOGUE(third,0)
	NO_OP					// _stepOutStart_
	LOAD_IMMED_BYTE(REG0,0x0)
	LOAD_IMMED_BYTE(REG1,0x1)
	LOAD_IMMED_BYTE(REG2,0x2)
	FUNCTION_CALL(fourth)
	ADD(REG0,REG1)
	ADD(REG0,REG2)
	ADD(REG0,REG0)
	NO_OP	; NO_OP	 ;  NO_OP		// _instructionStep_
	FUNCTION_EPILOGUE(third,0)		// _lineStepEnd_
	FUNCTION_RETURN(third,0)
	FUNCTION_END(third,0)

	FUNCTION_BEGIN(second,0)
	FUNCTION_PROLOGUE(second,0)		// _stepASMFunctionEntry_
	NO_OP					// _stepASMFunctionStepOut_
	NO_OP
	NO_OP	;  NO_OP ; NO_OP
	FUNCTION_CALL(third)			// _stepOverStart_
	FUNCTION_EPILOGUE(second, 0)		// _stepOverEnd_
	FUNCTION_RETURN(second, 0)		// _stepASMFunctionExit_
	FUNCTION_END(second, 0)

	FUNCTION_BEGIN(first,0)
	FUNCTION_PROLOGUE(first,0)
	NO_OP
	NO_OP
	NO_OP	; NO_OP ; NO_OP
	NO_OP
	FUNCTION_CALL(second)			// _stepASMFunctionCall_
	NO_OP					// _stepASMFunctionReturned_
	NO_OP
	FUNCTION_EPILOGUE(first, 0)
	FUNCTION_RETURN(first, 0)
	FUNCTION_END(first, 0)

	FUNCTION_BEGIN(main, 0)
	MAIN_PROLOGUE(0)
	FUNCTION_CALL(first)
	MAIN_EPILOGUE(0)
	FUNCTION_RETURN(main,0)
	FUNCTION_END(main,0)
