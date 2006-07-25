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
#include <stdlib.h>
#include <gcj/cni.h>

#include "frysk/rt/StackCallbacks.h"
#include "../frysk-imports/lib/elf/ElfData.h"

void
frysk::rt::StackCallbacks::populate_procinfo(
		jlong procInfo, jlong lowPC, jlong highPC, jlong lsda, jlong gp, jlong flags,
		lib::elf::ElfData* debug_frame)
{
	unw_proc_info_t *proc_info = (unw_proc_info_t *) procInfo;
	
	proc_info->start_ip = (unw_word_t) lowPC;
	proc_info->end_ip = (unw_word_t) highPC;
	proc_info->lsda = (unw_word_t) lsda;
	proc_info->gp = (unw_word_t) gp;
	proc_info->flags = (unw_word_t) flags;
	proc_info->format = UNW_INFO_FORMAT_TABLE;

	int data_size = (int) debug_frame->getSize();
	unsigned char* frame_data = (unsigned char*) malloc(data_size);
	
	for(int i = 0; i < data_size; i++)
		frame_data[i] = (unsigned char) debug_frame->getByte((jlong) i);

	proc_info->unwind_info_size = data_size;
	proc_info->unwind_info = (void *) frame_data;
}

void
frysk::rt::StackCallbacks::populate_procinfo_nounwind(
		jlong procInfo, jlong lowPC, jlong highPC, jlong lsda, jlong gp, jlong flags)
{
	unw_proc_info_t *proc_info = (unw_proc_info_t *) procInfo;
	
	proc_info->start_ip = (unw_word_t) lowPC;
	proc_info->end_ip = (unw_word_t) highPC;
	proc_info->lsda = (unw_word_t) lsda;
	proc_info->gp = (unw_word_t) gp;
	proc_info->flags = (unw_word_t) flags;
	proc_info->unwind_info = NULL;
}

void
frysk::rt::StackCallbacks::free_proc_info(jlong proc_info)
{
	if(((unw_proc_info_t *) proc_info)->unwind_info != NULL)
		JvFree(((unw_proc_info_t *) proc_info)->unwind_info);
}
