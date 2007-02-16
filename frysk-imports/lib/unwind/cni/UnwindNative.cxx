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

#include <stdio.h>
#include <stdlib.h>
#include <libunwind.h>
#include <libunwind-ptrace.h>

#include <gcj/cni.h>

#include <gnu/gcj/RawDataManaged.h>

#define TARGET Native

#include "java/lang/String.h"
#include "inua/eio/ByteBuffer.h"

#include "lib/unwind/Unwind.h"
#include "lib/unwind/UnwindNative.h"
#include "lib/unwind/Accessors.h"
#include "lib/unwind/Cursor.h"
#include "lib/unwind/ByteOrder.h"
#include "lib/unwind/CachingPolicy.h"

jint
lib::unwind::UnwindNative::getContext(gnu::gcj::RawDataManaged* context)
{
	return (jint) unw_getcontext((::unw_context_t *) context);
}

jint
lib::unwind::UnwindNative::initRemote(gnu::gcj::RawDataManaged* cursor, 
gnu::gcj::RawDataManaged* addressSpace, lib::unwind::Accessors* accessors)
{
	cursor = (gnu::gcj::RawDataManaged *) JvAllocBytes (sizeof (::unw_cursor_t));	
	jint ret = unw_init_remote((::unw_cursor_t *)cursor, 
	(::unw_addr_space_t) addressSpace, (void *) accessors);		
	return ret;	
} 

jint
lib::unwind::UnwindNative::getRegister(gnu::gcj::RawDataManaged* cursor,
jint regNum, gnu::gcj::RawDataManaged* word)
{
	return (jint) unw_get_reg((::unw_cursor_t *) cursor,
	(::unw_regnum_t) regNum, (::unw_word_t *) word);
}

/*
 * Get misc. proc info
 */
int native_find_proc_info (::unw_addr_space_t as, ::unw_word_t ip, 
		    ::unw_proc_info_t *pip, int need_unwind_info,
		    void *arg)
{
	return ((lib::unwind::Accessors *)arg)->findProcInfo (
	(inua::eio::ByteBuffer *) ip, 
	(gnu::gcj::RawDataManaged *) pip, 
	(jint) need_unwind_info);
}

/*
 * Free space allocated during find_proc_info
 */
void native_put_unwind_info (::unw_addr_space_t as, ::unw_proc_info_t *pip,
		      void *arg)
{
	((lib::unwind::Accessors *)arg)->putUnwindInfo (
	(gnu::gcj::RawDataManaged *)pip);
}

/*
 * Get the head of the dynamic unwind registration list.
 */
int native_get_dyn_info_list_addr (::unw_addr_space_t as, ::unw_word_t *dilap,
			    void *arg)
{
	return ((lib::unwind::Accessors *)arg)->getDynInfoListAddr (
	(inua::eio::ByteBuffer *) dilap);
}

/*
 * Perform memory read/write.
 */
int native_access_mem (::unw_addr_space_t as, ::unw_word_t addr,
		::unw_word_t *valp, int write, void *arg) 
{
	return ((lib::unwind::Accessors *) arg)->accessMem(
	(inua::eio::ByteBuffer *) addr, 
	(inua::eio::ByteBuffer *) valp, 
	(jint) write);
}

/*
 * perform register read/write
 */
int native_access_reg(::unw_addr_space_t as, ::unw_regnum_t regnum,
	       ::unw_word_t *valp, int write, void *arg)
{
	return ((lib::unwind::Accessors *) arg)->accessReg(
	(jint) regnum, 
	(inua::eio::ByteBuffer *) valp, 
	(jint) write);
}

/*
 * Perform a floating point register read/write
 */
int native_access_fpreg(::unw_addr_space_t as, ::unw_regnum_t regnum,
		 ::unw_fpreg_t *fpvalp, int write, void *arg)
{ 	
	return ((lib::unwind::Accessors *) arg)->accessFPReg(
	(jint) regnum, 
	(inua::eio::ByteBuffer *) fpvalp, 
	(jint) write);
}

/*
 * Resumes the process at the provided stack level
 */
int native_resume(::unw_addr_space_t as, ::unw_cursor_t *cp, void *arg)
{	
	return (int) ((lib::unwind::Accessors *)arg)->resume (
	(lib::unwind::Cursor *) cp);
}

/*
 * Returns the name of the procedure that the provided address is in as well as
 * the offset from the start of the procedure.
 */
int native_get_proc_name(::unw_addr_space_t as,
		  ::unw_word_t addr, char *bufp,
		  size_t buf_len, ::unw_word_t *offp, void *arg)
{
	return ((lib::unwind::Accessors *)arg)->getProcName (
	(inua::eio::ByteBuffer *) addr, 
	(java::lang::String *) bufp, 
	(jsize) buf_len, 
	(inua::eio::ByteBuffer *) offp);
}

gnu::gcj::RawDataManaged*
lib::unwind::UnwindNative::createAddressSpace(lib::unwind::ByteOrder * byte_order)
{
	static unw_accessors_t accessors = {
		native_find_proc_info ,
		native_put_unwind_info, 
		native_get_dyn_info_list_addr,
		native_access_mem, 
		native_access_reg, 
		native_access_fpreg, 
		native_resume, 
		native_get_proc_name};
		
	gnu::gcj::RawDataManaged *addressSpace = (gnu::gcj::RawDataManaged *) JvAllocBytes (sizeof (::unw_addr_space_t));
	addressSpace = (gnu::gcj::RawDataManaged *) unw_create_addr_space( &accessors, (int) byte_order->hashCode());

	return addressSpace; 

	//return (gnu::gcj::RawDataManaged *) unw_create_addr_space( &accessors, (int) byte_order);
}

void
lib::unwind::UnwindNative::destroyAddressSpace(gnu::gcj::RawDataManaged* addressSpace)
{
	unw_destroy_addr_space((unw_addr_space_t) addressSpace);	
}

void
lib::unwind::UnwindNative::setCachingPolicy(gnu::gcj::RawDataManaged* addressSpace, 
lib::unwind::CachingPolicy* cachingPolicy)
{
	unw_set_caching_policy((unw_addr_space_t) addressSpace, 
	(unw_caching_policy_t) cachingPolicy->hashCode());
}

jint
lib::unwind::UnwindNative::isSignalFrame(gnu::gcj::RawDataManaged* cursor)
{
	return unw_is_signal_frame((unw_cursor_t *) cursor);
}

jint
lib::unwind::UnwindNative::step(gnu::gcj::RawDataManaged* cursor)
{
	return unw_step((unw_cursor_t *) cursor);
}
