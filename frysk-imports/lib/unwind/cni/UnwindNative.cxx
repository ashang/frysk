// This file is part of the program FRYSK.
//
// Copyright 2007, Red Hat Inc.
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
#include <stdarg.h>

#include <libunwind.h>
#include <libunwind-ptrace.h>

#include <gcj/cni.h>

#include <gnu/gcj/RawDataManaged.h>

#define TARGET Native

#include "java/lang/String.h"
#include "java/lang/Object.h"

#include "java/util/logging/Logger.h"
#include "java/util/logging/Level.h"
#include "inua/eio/ByteBuffer.h"

#include "lib/unwind/Unwind.h"
#include "lib/unwind/UnwindNative.h"
#include "lib/unwind/Accessors.h"
#include "lib/unwind/Cursor.h"
#include "lib/unwind/ByteOrder.h"
#include "lib/unwind/CachingPolicy.h"
#include "lib/unwind/ProcInfo.h"
#include "lib/unwind/ProcName.h"
#include "lib/unwind/ProcInfo.h"

#include "frysk/sys/cni/Errno.hxx"

/*
 * Get misc. proc info
 */
int native_find_proc_info (::unw_addr_space_t as, ::unw_word_t ip, 
		    ::unw_proc_info_t *pip, int need_unwind_info,
		    void *arg)
{
	lib::unwind::ProcInfo* procInfo = ((lib::unwind::Accessors *)arg)->findProcInfo ( (long) ip,
	(jboolean) need_unwind_info);

	if (procInfo == NULL)
		return -1;

	pip->start_ip = (unw_word_t) procInfo->startIP;
	pip->end_ip = (unw_word_t) procInfo->endIP;
	pip->lsda = (unw_word_t) procInfo->lsda;
	pip->handler = (unw_word_t) procInfo->handler;
	pip->gp = (unw_word_t) procInfo->gp;
	pip->flags = (unw_word_t) procInfo->flags;
	pip->format = (int) procInfo->format;
	pip->unwind_info_size = (int) procInfo->unwindInfoSize;
	pip->unwind_info = (void *) procInfo->unwindInfo;

	return 0;
}

/*
 * Free space allocated during find_proc_info
 */
void native_put_unwind_info (::unw_addr_space_t as, ::unw_proc_info_t *proc_info,
		      void *arg)
{
	lib::unwind::ProcInfo * procInfo = new lib::unwind::ProcInfo();
	procInfo->startIP = (jlong) proc_info->start_ip;
  	procInfo->endIP = (jlong) proc_info->end_ip;
  	procInfo->lsda = (jlong) proc_info->lsda;
  	procInfo->handler = (jlong) proc_info->handler;
  	procInfo->gp = (jlong) proc_info->gp;
  	procInfo->flags = (jlong) proc_info->flags;
  	procInfo->format = (jint) proc_info->format;
  	procInfo->unwindInfoSize = (jint) proc_info->unwind_info_size;
  	procInfo->unwindInfo = (gnu::gcj::RawData *) proc_info->unwind_info;
	
	((lib::unwind::Accessors *)arg)->putUnwindInfo (procInfo);
}

/*
 * Get the head of the dynamic unwind registration list.
 */
int native_get_dyn_info_list_addr (::unw_addr_space_t as, ::unw_word_t *dilap,
			    void *arg)
{
	jbyteArray tmp = JvNewByteArray(sizeof (unw_word_t));
	memcpy (elements(tmp), dilap, JvGetArrayLength(tmp));
	return ((lib::unwind::Accessors *)arg)->getDynInfoListAddr (tmp);
}

/*
 * Perform memory read/write.
 */
int native_access_mem (::unw_addr_space_t as, ::unw_word_t addr,
		::unw_word_t *valp, int write, void *arg) 
{
	jbyteArray tmp = JvNewByteArray (sizeof (unw_word_t));
	memcpy (elements(tmp), valp, JvGetArrayLength(tmp));
	
	return ((lib::unwind::Accessors *) arg)->accessMem(
	(long) addr, 
	tmp, 
	(jboolean) write);
}

/*
 * perform register read/write
 */
int native_access_reg(::unw_addr_space_t as, ::unw_regnum_t regnum,
	       ::unw_word_t *valp, int write, void *arg)
{
	jbyteArray tmp = JvNewByteArray(sizeof (unw_word_t));
	memcpy (elements (tmp), valp, JvGetArrayLength(tmp));
	
	int ret = ((lib::unwind::Accessors *) arg)->accessReg(
	(jint) regnum, 
	tmp, 
	(jint) write);
		
	memcpy(valp, elements (tmp), JvGetArrayLength(tmp));
	
	return ret;
}

/*
 * Perform a floating point register read/write
 */
int native_access_fpreg(::unw_addr_space_t as, ::unw_regnum_t regnum,
		 ::unw_fpreg_t *fpvalp, int write, void *arg)
{ 	
	jbyteArray tmp = JvNewByteArray(sizeof (unw_word_t));
	memcpy (elements (tmp), fpvalp, JvGetArrayLength(tmp));
	
	return ((lib::unwind::Accessors *) arg)->accessFPReg(
	(jint) regnum, 
	tmp, 
	(jboolean) write);
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
	lib::unwind::ProcName *procName = ((lib::unwind::Accessors *)arg)->getProcName (
	(jlong) addr, (jint) buf_len);
	
	if (procName == NULL)
		return -1;

	strncpy (bufp, (const char *) JvGetStringChars(procName->name), buf_len);
	offp = (unw_word_t *) procName->address;
	
	return 0;
}


gnu::gcj::RawDataManaged*
lib::unwind::UnwindNative::initRemote(gnu::gcj::RawData* addressSpace, 
lib::unwind::Accessors* accessors)
{
	logMessage(this, logger, java::util::logging::Level::FINE, "native initRemote");
	gnu::gcj::RawDataManaged *cursor = (gnu::gcj::RawDataManaged *) JvAllocBytes (sizeof (::unw_cursor_t));
		
	unw_init_remote((unw_cursor_t *) cursor, 
	(unw_addr_space_t) addressSpace, (void *) accessors);

	return cursor;	
} 

gnu::gcj::RawData*
lib::unwind::UnwindNative::createAddressSpace(lib::unwind::ByteOrder * byteOrder)
{
	logMessage(this, logger, java::util::logging::Level::FINE, 
	"createAddressSpace, byteOrder %d", (int) byteOrder->hashCode());
	static unw_accessors_t accessors = {
		native_find_proc_info ,
		native_put_unwind_info, 
		native_get_dyn_info_list_addr,
		native_access_mem, 
		native_access_reg, 
		native_access_fpreg, 
		native_resume, 
		native_get_proc_name};
		
	return (gnu::gcj::RawData *) unw_create_addr_space( &accessors, (int) byteOrder->hashCode());
}

void
lib::unwind::UnwindNative::destroyAddressSpace(gnu::gcj::RawData* addressSpace)
{
	unw_destroy_addr_space((unw_addr_space_t) addressSpace);	
}

void
lib::unwind::UnwindNative::setCachingPolicy(gnu::gcj::RawData* addressSpace, 
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

lib::unwind::ProcName*
lib::unwind::UnwindNative::getProcName(gnu::gcj::RawDataManaged* cursor, jint maxNameSize)
{
	logMessage(this, logger, java::util::logging::Level::FINE, "getProcName");
	char bufp[maxNameSize];
	unw_word_t offset;
	unw_get_proc_name((unw_cursor_t *) cursor, bufp, maxNameSize, &offset);
	return new lib::unwind::ProcName((jlong) offset, JvNewStringUTF(bufp));
}

jint
lib::unwind::UnwindNative::getRegister(gnu::gcj::RawDataManaged* cursor,
jint regNum, gnu::gcj::RawDataManaged* word)
{
	return (jint) unw_get_reg((::unw_cursor_t *) cursor,
	(::unw_regnum_t) regNum, (::unw_word_t *) word);
}


jint
lib::unwind::UnwindNative::getContext(gnu::gcj::RawDataManaged* context)
{
	return (jint) unw_getcontext((::unw_context_t *) context);
}
