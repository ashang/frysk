// This file is part of the program FRYSK.
//
// Copyright 2005, Red Hat Inc.
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

#include <libunwind.h>
#include <stdio.h>
#include <gcj/cni.h>

#include "frysk/rt/StackFrame.h"

void
frysk::rt::StackFrame::initialize()
{
	unw_cursor_t *cursor = (unw_cursor_t *) this->unwind_data;
	unw_proc_info_t proc_info;
	int result = unw_get_proc_info(cursor, &proc_info);
	int len = 256;
	char buf[len];
	unw_word_t offset;
	
	if(!unw_get_proc_name(cursor, buf, len, &offset))
	{
		this->methodName = JvNewStringUTF(buf);
		
		if(result == 0)
			this->address = (jlong) offset + proc_info.start_ip;
	}
	
	unw_word_t tmp;
	unw_get_reg (cursor, UNW_REG_SP, &tmp);
	this -> cfa = tmp;
}

jlong
frysk::rt::StackFrame::get_reg(jlong reg)
{
	unw_word_t value;
	int code;
	unw_cursor_t *cursor = (unw_cursor_t *) this->unwind_data;
	code = unw_get_reg(cursor, (unw_regnum_t)reg, &value);	
	// ??? Handle code < 0	
	return value;
}

jlong
frysk::rt::StackFrame::set_reg(jlong reg, jlong val)
{
	int code;
	unw_cursor_t *cursor = (unw_cursor_t *) this->unwind_data;
	code = unw_set_reg(cursor, (unw_regnum_t)reg, (unw_word_t)val);
	return code;
}
