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
#include <libunwind-ptrace.h>
#include <endian.h>
#include <stdio.h>
#include <gcj/cni.h>

#include "lib/unwind/StackTraceCreator.h"
#include "lib/unwind/StackTraceCreator$UnwindArgs.h"
#include "lib/unwind/FrameCursor.h"
#include "lib/unwind/UnwindCallbacks.h"

typedef lib::unwind::StackTraceCreator$UnwindArgs unwargs;

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
	  return _UPT_find_proc_info (as, ip, pip, need_unwind_info,
	                               (void *)((unwargs *)arg)->UPTarg);
}

/*
 * Free space allocated during find_proc_info
 */
void put_unwind_info (::unw_addr_space_t as, ::unw_proc_info_t *pip,
						void *arg)
{
       return _UPT_put_unwind_info (as, pip,
                                    (void *)((unwargs *)arg)->UPTarg);
}

/*
 * Get the head of the dynamic unwind registration list.
 */
int get_dyn_info_list_addr (::unw_addr_space_t as, ::unw_word_t *dilap,
							void *arg)
{
       return _UPT_get_dyn_info_list_addr (as, dilap,
                                           (void *)((unwargs *)arg)->UPTarg);
}

/*
 * Perform memory read/write
 */
int access_mem (::unw_addr_space_t as, ::unw_word_t addr,
				::unw_word_t *valp, int write, void *arg)
{
	lib::unwind::UnwindCallbacks *cb = ((unwargs *)arg)->CBarg;
	
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
	lib::unwind::UnwindCallbacks *cb = ((unwargs *)arg)->CBarg;
	
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
	lib::unwind::UnwindCallbacks *cb = ((unwargs *)arg)->CBarg;
	
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
	lib::unwind::UnwindCallbacks *cb = ((unwargs *)arg)->CBarg;
	
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
	lib::unwind::UnwindCallbacks *cb = ((unwargs *)arg)->CBarg;
	
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
lib::unwind::StackTraceCreator::unwind_setup (unwargs *args)
{
	/*
	 * We don't need to malloc this, since according to the libunwind docs this callback
	 * only needs to exist for the duration of the call to unw_create_address_space
	 */
	::unw_accessors_t accessors = { find_proc_info, put_unwind_info, get_dyn_info_list_addr,
			access_mem, access_reg, access_fpreg, resume, get_proc_name};
	
	// Initialize libunwind
	::unw_addr_space_t addr_space = ::unw_create_addr_space(&accessors, 0);
	args->unwas = (jlong)addr_space;
	::unw_cursor_t cursor;
	
    /* Since we're not actually using ptrace, the PID below is unused.  */
    args->UPTarg = (jlong)_UPT_create (/* PID = */ 0);
    ::unw_init_remote(&cursor, addr_space, args);
	::unw_set_caching_policy(addr_space, UNW_CACHE_PER_THREAD);
	
	// Create the frame objects and return the top (most recent one)
	lib::unwind::FrameCursor *base_frame = new lib::unwind::FrameCursor((jlong) &cursor);
	lib::unwind::FrameCursor *current = base_frame;
	while (::unw_step(&cursor) > 0)
	{
		lib::unwind::FrameCursor *prev = new lib::unwind::FrameCursor((jlong) &cursor);
		
		// This seems backwards, but remember we're starting from the *most recent* frame
		current->outer = prev;
		prev->inner = current;
		current = prev;
	}
	
	return base_frame;
}

void
lib::unwind::StackTraceCreator::unwind_finish (unwargs *args)
{
       unw_destroy_addr_space ((unw_addr_space_t)args->unwas);
       args->unwas = 0;
       _UPT_destroy ((void*)args->UPTarg);
       args->UPTarg = 0;
}

