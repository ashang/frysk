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

#include <libunwind.h>
#include <libunwind-ptrace.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <signal.h>

#include <gcj/cni.h>

#include "gnu/gcj/RawData.h"
#include "gnu/gcj/RawDataManaged.h"

#include "lib/unwind/Accessors.h"
#include "lib/unwind/PtraceAccessors.h"
#include "lib/unwind/ProcInfo.h"
#include "lib/unwind/ProcName.h"
#include "lib/unwind/Cursor.h"
#include "lib/unwind/AddressSpace.h"
#include "lib/unwind/ByteOrder.h"

#include "frysk/sys/cni/Errno.hxx"

static inline unw_word_t* getWord(jbyteArray value)
{
	return (unw_word_t *) elements(value);
}
jint 
lib::unwind::PtraceAccessors::accessFPReg (jint regnum, jbyteArray fpvalp, 
										   jboolean write)
{
	logFine(this, logger, "accessFPReg regnum: %d write: %d", (int) regnum, (int) write);
	if ((int) JvGetArrayLength(fpvalp) >= (int) sizeof (unw_fpreg_t))
		_UPT_access_fpreg((unw_addr_space_t ) addressSpace,
						  (unw_regnum_t ) regnum,
						  (unw_fpreg_t *) elements(fpvalp), (int) write , 
						  (void *) ptArgs);	
	  return 0;
}

jint
lib::unwind::PtraceAccessors::accessMem (jlong addr, jbyteArray valp, 
										 jboolean write)
{
	logFine (this, logger, "accessMem address: 0x%lx, write: %d", (long) addr, (int) write);
	if ((int) JvGetArrayLength(valp) >= (int) sizeof (unw_word_t))
	return (jint) _UPT_access_mem((unw_addr_space_t) addressSpace, (unw_word_t) addr, 
					getWord(valp), (int) write, (void *) ptArgs);
	
	return -1;
}

jint 
lib::unwind::PtraceAccessors::accessReg (jint regnum, jbyteArray valp, 
										 jboolean write)
{
	logFine(this, logger, "accessReg regnum: %d, write: %d", (int) regnum, (int) write);
	int ret = -1;
	
	if ((int) JvGetArrayLength(valp) >= (int) sizeof (unw_word_t))
	ret = _UPT_access_reg((unw_addr_space_t) addressSpace, (unw_regnum_t) regnum,
					getWord(valp), (int) write, (void *) ptArgs);
	
	logFine(this, logger, "accessReg returning: 0x%lx return value: %d", (long) *elements(valp), ret);
	
	return (jint) ret;
}										 	

lib::unwind::ProcInfo*
lib::unwind::PtraceAccessors::findProcInfo (jlong ip, jboolean needUnwindInfo)
{
	logFine(this, logger, "findProcInfo ip: 0x%lx, needUnwindInfo %d", (long) ip, (int) needUnwindInfo);
	unw_proc_info_t *proc_info = (unw_proc_info_t *)JvAllocBytes(sizeof (unw_proc_info_t));
	int ret = _UPT_find_proc_info((unw_addr_space_t) addressSpace, 
	(unw_word_t) ip, proc_info,
					(int) needUnwindInfo, (void *) ptArgs);

	lib::unwind::ProcInfo *procInfo = new 
	ProcInfo((jint) ret,(gnu::gcj::RawDataManaged *) proc_info);
	
  	jLogFinest(this, logger, "findProcInfo procInfo: {1}", procInfo);
  	return procInfo;
}

jint 
lib::unwind::PtraceAccessors::getDynInfoListAddr (jbyteArray dilap)
{
	logFine(this, logger, "getDynInfoListAddr");
	int ret = _UPT_get_dyn_info_list_addr((unw_addr_space_t) addressSpace, getWord(dilap), 
								(void *) ptArgs);

	logFinest(this, logger, "getDynInfoListAddr value of dilap: 0x%lx", (long) *elements(dilap)); 		
	return (jint) ret;
}

lib::unwind::ProcName* 
lib::unwind::PtraceAccessors::getProcName (jlong addr, jint maxNameSize)
{	
	logFine(this, logger, "getProcName address: 0x%lx, maxNameSize: %d", (long) addr, (int) maxNameSize);
	char buffp[maxNameSize];
	unw_word_t offset = 0;
	int ret = _UPT_get_proc_name((unw_addr_space_t) addressSpace, (unw_word_t) addr, 
					   buffp, (size_t) maxNameSize, &offset, (void *) ptArgs);

	logFinest(this, logger, "getProcName ret: %d", ret);
	
	if (ret == 0)
		logFinest(this, logger, "getProcName buffp: %s, offset 0x%lx", buffp, (long) offset);
	

	if (ret < 0 && ret != -UNW_ENOMEM) 
		return new ProcName((jint) ret, 0, NULL); 
	
	return new ProcName((jint) ret, (jlong) offset, JvNewStringUTF(buffp));	
}

void 
lib::unwind::PtraceAccessors::putUnwindInfo (lib::unwind::ProcInfo *procInfo)
{
	jLogFine(this, logger, "putUnwindInfo procInfo: {1}", procInfo);
	unw_proc_info_t* proc_info;
		
	proc_info = (unw_proc_info_t *) procInfo->procInfo;
	
	_UPT_put_unwind_info((unw_addr_space_t) addressSpace, proc_info, (void *) ptArgs);
		
	procInfo = NULL;
}

jint 
lib::unwind::PtraceAccessors::resume (lib::unwind::Cursor *cursor)
{
	jLogFine(this, logger, "resume cursor : {1}", cursor);
	return _UPT_resume((unw_addr_space_t) addressSpace, (unw_cursor_t *) cursor->cursor,
				(void *) ptArgs);
}

gnu::gcj::RawData*
lib::unwind::PtraceAccessors::createAddressSpace (lib::unwind::ByteOrder* byteOrder)
{
	logFine(this, logger, "createAddressSpace: %d",(int) byteOrder->hashCode());	
	return (gnu::gcj::RawData *) unw_create_addr_space (&_UPT_accessors, byteOrder->hashCode());
}

jint 
lib::unwind::PtraceAccessors::attachXXX(jint pid)
{
	int ret;
	ret = ::ptrace(PTRACE_ATTACH, pid, (void *) 0, (void *) 0);
	
	if (ret != 0) 
	{
		fprintf(stderr, "Ptrace attach failed\n");
		return ret;
	}	
	::waitpid(pid, NULL, 0);
		
	return 0;
}

jint
lib::unwind::PtraceAccessors::detachXXX(jint pid)
{
	int ret;
	ret = ::ptrace(PTRACE_DETACH, (pid), 0, (void *) 0);
	
	if (ret != 0)
		fprintf(stderr, "Ptrace detach failed\n");
		
	return ret;
}

gnu::gcj::RawData*
lib::unwind::PtraceAccessors::createPtArg (jint pid)
{
	logFine(this, logger, "createPtArg pid: %d", (int) pid);
	return (gnu::gcj::RawData *) _UPT_create(pid); 
}

void
lib::unwind::PtraceAccessors::finalize()
{
	logFine(this, logger, "finalize");
	unw_destroy_addr_space((unw_addr_space_t) addressSpace);
	addressSpace = NULL;
	_UPT_destroy ((void*) ptArgs);
	ptArgs = NULL;
}
