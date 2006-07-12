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
#include <endian.h>
#include <gcj/cni.h>

#include "lib/unwind/StackTraceCreator.h"
#include "lib/unwind/FrameCursor.h"
#include "lib/unwind/UnwindCallbacks.h"

/***************************
 * CALLBACKS
 * See the UnwindCallbacks
 * interface for more detailed
 * comments describing their
 * functionality
 ***************************/
 
/*
 * Get misc. proc info
 */
int find_proc_info (::unw_addr_space_t as, ::unw_word_t ip, 
					::unw_proc_info_t *pip, int need_unwind_info, void *arg)
{
	lib::unwind::UnwindCallbacks *cb = (lib::unwind::UnwindCallbacks*) arg;
	// delegate to the java interface
	jlong retval = cb->findProcInfo ((jlong) &as, (jlong) ip, need_unwind_info != 0);
	pip = (::unw_proc_info_t *) retval;
	
	return 0;
}

/*
 * Free space allocated during find_proc_info
 */
void put_unwind_info (::unw_addr_space_t as, ::unw_proc_info_t *pip,
						void *arg)
{
	lib::unwind::UnwindCallbacks *cb = (lib::unwind::UnwindCallbacks*) arg;
	cb->putUnwindInfo ((jlong) as, (jlong) pip);
}

/*
 * Get the head of the dynamic unwind registration list.
 */
int get_dyn_info_list_addr (::unw_addr_space_t as, ::unw_word_t *dilap,
							void *arg)
{
	lib::unwind::UnwindCallbacks *cb = (lib::unwind::UnwindCallbacks*) arg;
	jlong retval = cb->getDynInfoListAddr ((jlong) as);
	*dilap = retval;
	
	return 0;
}

/*
 * Perform memory read/write
 */
int access_mem (::unw_addr_space_t as, ::unw_word_t addr,
				::unw_word_t *valp, int write, void *arg)
{
	lib::unwind::UnwindCallbacks *cb = (lib::unwind::UnwindCallbacks*) arg;
	
	// we've separated read and write in the java interface for simplicity
	if(write == 0){
		jlong retval = cb->accessMem ((jlong) as, (jlong) addr);
		*valp = (::unw_word_t) retval;
	}
	else{
		cb->writeMem ((jlong) as, (jlong) addr, (jlong) *valp);
	}
	
	return 0;
}

/*
 * perform register read/write
 */
int access_reg(::unw_addr_space_t as,
			::unw_regnum_t regnum, ::unw_word_t *valp,
			int write, void *arg)
{
	lib::unwind::UnwindCallbacks *cb = (lib::unwind::UnwindCallbacks*) arg;
	
	// read and write are separated in the java interface
	if(write == 0){
		jlong retval = cb->accessReg ((jlong) as, (jlong) regnum);
		*valp = (::unw_word_t) retval;	
	}
	else{
		cb->writeReg ((jlong) as, (jlong) regnum, (jlong) *valp);
	}
	
	return 0;
}

/*
 * Perform a floating point register read/write
 */
int access_fpreg(::unw_addr_space_t as,
			::unw_regnum_t regnum, ::unw_fpreg_t *fpvalp,
			int write, void *arg)
{ 
	lib::unwind::UnwindCallbacks *cb = (lib::unwind::UnwindCallbacks*) arg;
	
	if(write == 0){
		jdouble retval = cb->accessFpreg ((jlong) as, (jlong) regnum);
		*fpvalp = (::unw_fpreg_t) retval;
	}
	else{
		cb->writeFpreg ((jlong) as, (jlong) regnum, (jdouble) *fpvalp);
	}
	
	return 0;
}

/*
 * Resumes the process at the provided stack level
 */
int resume(::unw_addr_space_t as,
			::unw_cursor_t *cp, void *arg)
{
	lib::unwind::UnwindCallbacks *cb = (lib::unwind::UnwindCallbacks*) arg;
	
	return (int) cb->resume ((jlong) as, (jlong) cp);
}

/*
 * Returns the name of the procedure that the provided address is in as well as
 * the offset from the start of the procedure.
 */
int get_proc_name(::unw_addr_space_t as,
			::unw_word_t addr, char *bufp,
			size_t buf_len, ::unw_word_t *offp, void *arg)
{
	lib::unwind::UnwindCallbacks *cb = (lib::unwind::UnwindCallbacks*) arg;
	
	jstring name = cb->getProcName ((jlong) as, (jlong) addr);
	jlong offset = cb->getProcOffset ((jlong) as, (jlong) addr);
	
	*offp = (::unw_word_t) offset;
	if((unsigned int) buf_len <= (unsigned int) name->length ()){
		JvGetStringUTFRegion (name, 0, buf_len, bufp);
		bufp[buf_len-1] = '\0';
		return -UNW_ENOMEM;
	}
	else{
		JvGetStringUTFRegion (name, 0, name->length (), bufp);
		bufp[name->length ()] = '\0';
		return 0;
	}
}

lib::unwind::FrameCursor*
lib::unwind::StackTraceCreator::unwind_setup (lib::unwind::UnwindCallbacks *cbs)
{
	/*
	 * We don't need to malloc this, since according to the libunwind docs this callback
	 * only needs to exist for the duration of the call to unw_create_address_space
	 */
	::unw_accessors_t accessors = { find_proc_info, put_unwind_info, get_dyn_info_list_addr,
			access_mem, access_reg, access_fpreg, resume, get_proc_name};
	
	// Initialize libunwind
	::unw_addr_space_t addr_space = ::unw_create_addr_space(&accessors, 0);
	::unw_cursor_t cursor;
	::unw_init_remote(&cursor, addr_space, (void*) cbs);
	
	// Create the frame objects and return the top (most recent one)
	lib::unwind::FrameCursor *base_frame = new lib::unwind::FrameCursor((jlong) &cursor);
	lib::unwind::FrameCursor *current = base_frame;
	while (::unw_step(&cursor) > 0)
	{
		lib::unwind::FrameCursor *prev = new lib::unwind::FrameCursor((jlong) &cursor);
		
		// This seems backwards, but remember we're starting from the *most recent* frame
		current->outer = prev;
		prev->innter = current;
		current = prev;
	}
	
	return base_frame;
}
