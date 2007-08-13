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
#include <sys/mman.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <libdwfl.h>
#include LIBUNWIND_TARGET_H
#include <dwarf.h>

#include <gcj/cni.h>

#include <gnu/gcj/RawDataManaged.h>

#include <java/lang/String.h>
#include <java/lang/Object.h>
#include <java/util/logging/Logger.h>
#include <java/util/logging/Level.h>

#include "inua/eio/ByteBuffer.h"
#include "lib/dwfl/Dwfl.h"
#include "lib/unwind/Unwind.h"
#include "lib/unwind/AddressSpace.h"
#include "lib/unwind/Cursor.h"
#include "lib/unwind/ByteOrder.h"
#include "lib/unwind/CachingPolicy.h"
#include "lib/unwind/ProcInfo.h"
#include "lib/unwind/ProcName.h"
#include "lib/unwind/ProcInfo.h"
#include "lib/unwind/ElfImage.h"

#include "frysk/sys/cni/Errno.hxx"

#include LIB_UNWIND_UNWIND_TARGET_H

#ifndef MAX_VDSO_SIZE
# define MAX_VDSO_SIZE ((size_t) sysconf (_SC_PAGESIZE))
#endif

#ifndef MAP_32BIT
# define MAP_32BIT 0
#endif

static lib::unwind::AddressSpace*
addressSpace(void* arg)
{
  lib::unwind::AddressSpace* space = (lib::unwind::AddressSpace*)arg;
  if (space->magic != lib::unwind::AddressSpace::MAGIC)
    throwRuntimeException ("bad AddressSpace");
  return space;
}

/*
 * Get misc. proc info
 */
static int
find_proc_info (::unw_addr_space_t as, ::unw_word_t ip,
		::unw_proc_info_t *pip, int need_unwind_info,
		void *arg)
{
  lib::unwind::ProcInfo* procInfo 
    = addressSpace(arg)->findProcInfo ((jlong) ip, (jboolean) need_unwind_info);
  if (procInfo->error != 0)
    return procInfo->error;
  memcpy(pip, procInfo->procInfo, sizeof (unw_proc_info_t));
  return 0;
}

/*
 * Free space allocated during find_proc_info
 */
static void
put_unwind_info (::unw_addr_space_t as, ::unw_proc_info_t *proc_info,
		 void *arg)
{
  lib::unwind::AddressSpace* space = addressSpace(arg);
  lib::unwind::ProcInfo* procInfo
    = new lib::unwind::ProcInfo(space->unwinder,
				(gnu::gcj::RawDataManaged *) proc_info);
  space->putUnwindInfo (procInfo);
}

/*
 * Get the head of the dynamic unwind registration list.
 */
static int
get_dyn_info_list_addr (::unw_addr_space_t as, ::unw_word_t *dilap,
			void *arg)
{
  jbyteArray tmp = JvNewByteArray(sizeof (unw_word_t));
  memcpy (elements(tmp), dilap, sizeof (unw_word_t));
  int ret = addressSpace(arg)->getDynInfoListAddr (tmp);
  memcpy(dilap, elements(tmp), sizeof (unw_word_t));
  return ret;
}

/*
 * Perform memory read/write.  Implement as copy-in, copy-out.
 */
static int
access_mem (::unw_addr_space_t as, ::unw_word_t addr,
	    ::unw_word_t *valp, int write, void *arg)
{
  jbyteArray tmp = JvNewByteArray (sizeof (unw_word_t));
  memcpy (elements(tmp), valp, JvGetArrayLength(tmp));
  int ret = addressSpace(arg)->accessMem((jlong) addr, tmp, (jboolean) write);
  memcpy(valp, elements(tmp), JvGetArrayLength(tmp));
  return ret;
}

/*
 * perform register read/write
 */
static int
access_reg(::unw_addr_space_t as, ::unw_regnum_t regnum,
	   ::unw_word_t *valp, int write, void *arg)
{
  jbyteArray tmp = JvNewByteArray(sizeof (unw_word_t));
  memcpy (elements (tmp), valp, JvGetArrayLength(tmp));
  int ret = addressSpace(arg)->accessReg((jint) regnum, tmp, (jint) write);
  memcpy(valp, elements (tmp), JvGetArrayLength(tmp));
  return ret;
}

/*
 * Perform a floating point register read/write
 */
static int
access_fpreg(::unw_addr_space_t as, ::unw_regnum_t regnum,
	     ::unw_fpreg_t *fpvalp, int write, void *arg)
{
  jbyteArray tmp = JvNewByteArray(sizeof (unw_word_t));
  memcpy (elements (tmp), fpvalp, JvGetArrayLength(tmp));
  int ret = addressSpace(arg)->accessFPReg((jint) regnum, tmp, (jboolean) write);
  memcpy(fpvalp, elements (tmp), JvGetArrayLength(tmp));
  return ret;
}

/*
 * Resumes the process at the provided stack level
 */
static int
resume(::unw_addr_space_t as, ::unw_cursor_t *cp, void *arg)
{
  return (int) addressSpace(arg)->resume ((lib::unwind::Cursor *) cp);
}

static size_t
min (size_t a, size_t b)
{
  return a < b ? a : b;
}

/*
 * Returns the name of the procedure that the provided address is in as well as
 * the offset from the start of the procedure.
 */
static int
get_proc_name(::unw_addr_space_t as,
	      ::unw_word_t addr, char *bufp,
	      size_t buf_len, ::unw_word_t *offp, void *arg)
{
  lib::unwind::ProcName *procName
    = addressSpace(arg)->getProcName ((jlong) addr, (jint) buf_len);
  if (procName->error < 0 && procName->error != -UNW_ENOMEM)
    return procName->error;
  *offp = (unw_word_t) procName->offset;
  //In case get_proc_name is used only to find addr;
  if (bufp == NULL || buf_len == 0) 
    return 0;
  if (procName->name == NULL)
      return 0;
  //The maximum number of characters that can be copied are buf_len -1.
  //with bufp[buf_len - 1] = '\0'.
  //Otherwise copy name.length characters, and bufp[name.length] = '\0'
  size_t upper_limit = min(buf_len - 1 , JvGetStringUTFLength(procName->name));
  //JvGetStringUTFRegion(jstring STR, jsize START, jsize LEN, char* BUF);
  JvGetStringUTFRegion(procName->name, 0, upper_limit, bufp);
  bufp[upper_limit] = '\0';
  if (upper_limit < buf_len)
    return 0;
  else
    return -UNW_ENOMEM;
}

gnu::gcj::RawDataManaged*
lib::unwind::TARGET::initRemote(lib::unwind::AddressSpace* addressSpace)
{
  logFine(this, logger, "native initRemote");
  gnu::gcj::RawDataManaged *cursor = (gnu::gcj::RawDataManaged *) JvAllocBytes (sizeof (::unw_cursor_t));

  unw_init_remote((unw_cursor_t *) cursor,
                  (unw_addr_space_t) (addressSpace->addressSpace),
		  (void *) addressSpace);

  return cursor;
}

gnu::gcj::RawData*
lib::unwind::TARGET::createAddressSpace(lib::unwind::ByteOrder * byteOrder)
{

  logFine(this, logger, "createAddressSpace, byteOrder %d", (int) byteOrder->hashCode());
  static unw_accessors_t accessors =
    {
      find_proc_info ,
      put_unwind_info,
      get_dyn_info_list_addr,
      access_mem,
      access_reg,
      access_fpreg,
      resume,
      get_proc_name
    };

  return (gnu::gcj::RawData *) unw_create_addr_space( &accessors, (int) byteOrder->hashCode());
}

void
lib::unwind::TARGET::destroyAddressSpace(gnu::gcj::RawData* addressSpace)
{
  logFine(this, logger, "destroyAddressSpace");
  unw_destroy_addr_space((unw_addr_space_t) addressSpace);
}

void
lib::unwind::TARGET::setCachingPolicy(gnu::gcj::RawData* addressSpace,
                                      lib::unwind::CachingPolicy* cachingPolicy)
{
  jLogFine(this, logger, "setCachingPolicy, cachingPolicy: {1}",
           cachingPolicy);
  unw_set_caching_policy((unw_addr_space_t) addressSpace,
                         (unw_caching_policy_t) cachingPolicy->hashCode());
}

jint
lib::unwind::TARGET::isSignalFrame(gnu::gcj::RawDataManaged* cursor)
{
  logFine(this, logger, "isSignalFrame");
  return unw_is_signal_frame((unw_cursor_t *) cursor);
}

jint
lib::unwind::TARGET::step(gnu::gcj::RawDataManaged* cursor)
{
  logFine (this, logger, "step cursor: %p", cursor);
  return unw_step((unw_cursor_t *) cursor);
}

lib::unwind::ProcName*
lib::unwind::TARGET::getProcName(gnu::gcj::RawDataManaged* cursor, jint maxNameSize)
{
  logFine (this, logger, "getProcName cursor: %p, maxNameSize: %d", cursor, (int) maxNameSize);

  char bufp[maxNameSize];
  bufp[0] = '\0';
  unw_word_t offset;
  int err = unw_get_proc_name((unw_cursor_t *) cursor, bufp, maxNameSize, &offset);

  logFinest(this, logger, "getProcName bufp: %s, offset: %lx, error: %d", bufp,(long) offset, err);

  if (err < 0)
    return new lib::unwind::ProcName((jint) err);

  jstring jName;
  
  if (bufp[0] == '\0')
    jName = NULL;
  else
    jName = JvNewStringUTF(bufp);
  
  return new lib::unwind::ProcName((jlong) offset, jName);
}

jint
lib::unwind::TARGET::getRegister(gnu::gcj::RawDataManaged* cursor,
                                 jint regNum, jbyteArray word)
{
  return (jint) unw_get_reg((::unw_cursor_t *) cursor,
                            (::unw_regnum_t) regNum, (::unw_word_t *) elements(word));
}

jint
lib::unwind::TARGET::getFPRegister(gnu::gcj::RawDataManaged* cursor,
                                 jint regNum, jbyteArray word)
{
  return (jint) unw_get_fpreg((::unw_cursor_t *) cursor,
                            (::unw_regnum_t) regNum, (::unw_fpreg_t *) elements(word));
}

jint
lib::unwind::TARGET::setRegister(gnu::gcj::RawDataManaged* cursor,
                                 jint regNum, jlong word)
{
  return (jint) unw_set_reg((::unw_cursor_t *) cursor,
                            (::unw_regnum_t) regNum, (::unw_word_t ) word);
}

jint
lib::unwind::TARGET::getSP(gnu::gcj::RawDataManaged* cursor, jbyteArray word)
{
  return (jint) unw_get_reg((::unw_cursor_t *) cursor,
                            UNW_REG_SP, (::unw_word_t *) elements(word));
}


jint
lib::unwind::TARGET::getContext(gnu::gcj::RawDataManaged* context)
{
  return (jint) unw_getcontext((::unw_context_t *) context);
}

gnu::gcj::RawDataManaged*
lib::unwind::TARGET::copyCursor(gnu::gcj::RawDataManaged* cursor)
{
  ::unw_cursor_t *nativeCursor = (::unw_cursor_t *) JvAllocBytes (sizeof (::unw_cursor_t));

  // Create a local copy of the unwind cursor
  memcpy (nativeCursor, cursor, sizeof (::unw_cursor_t));

  return (gnu::gcj::RawDataManaged *) nativeCursor;
}

lib::unwind::ProcInfo*
lib::unwind::TARGET::getProcInfo(gnu::gcj::RawDataManaged* cursor)
{
  logFine (this, logger, "getProcInfo cursor: %p", cursor);
  unw_proc_info_t *procInfo
    = (::unw_proc_info_t *) JvAllocBytes(sizeof (::unw_proc_info_t));

  int ret = unw_get_proc_info((::unw_cursor_t *) cursor, procInfo);

  logFine( this, logger, "getProcInfo finished get_proc_info");
  lib::unwind::ProcInfo * myInfo;
  if (ret < 0)
    myInfo = new lib::unwind::ProcInfo((jint) ret);
  else
    myInfo = new lib::unwind::ProcInfo(this, (gnu::gcj::RawDataManaged*) procInfo);
  jLogFine (this, logger, "getProcInfo returned: {1}", myInfo);
  return myInfo;
}

lib::unwind::ProcInfo*
lib::unwind::TARGET::createProcInfoFromElfImage(lib::unwind::AddressSpace* addressSpace,
						jlong ip,
						jboolean needUnwindInfo,
						lib::unwind::ElfImage* elfImage)
{
  unw_proc_info_t *procInfo
    = (::unw_proc_info_t *) JvAllocBytes(sizeof (::unw_proc_info_t));

  logFine(this, logger, "Pre unw_get_unwind_table");
  int ret = unw_get_unwind_table((unw_addr_space_t) addressSpace->addressSpace,
				 (unw_word_t) ip, procInfo,
				 (int) needUnwindInfo,
				 (void *) elfImage->elfImage,
				 elfImage->size, elfImage->segbase,
				 elfImage->mapoff, (void *) addressSpace);

  logFine(this, logger, "Post unw_get_unwind_table");
  lib::unwind::ProcInfo *myInfo;
  if (ret < 0)
    myInfo = new lib::unwind::ProcInfo((jint) ret);
  else
    myInfo = new lib::unwind::ProcInfo(this,
				       (gnu::gcj::RawDataManaged*) procInfo);

  return myInfo;
}

lib::unwind::ElfImage*
lib::unwind::TARGET::createElfImageFromVDSO(lib::unwind::AddressSpace* addressSpace,
					    jlong lowAddress, jlong highAddress,
					    jlong offset)
{
  logFine(this, logger,
          "entering segbase: 0x%lx, highAddress: 0x%lx, mapoff: 0x%lx",
          (unsigned long) lowAddress, (unsigned long) highAddress,
          (unsigned long) offset);
  void *image;
  size_t size;
  unw_word_t magic;
  unw_accessors_t *a;
  unsigned long segbase = (unsigned long) lowAddress;
  unsigned long hi = (unsigned long) highAddress;
  unsigned long mapoff = (unsigned long) offset;

  size = hi - segbase;
  if (size > MAX_VDSO_SIZE)
    return new lib::unwind::ElfImage((jint) -1);

  logFine(this, logger, "checked size, 0x%lx", (unsigned long) size);
  unw_addr_space_t as = (unw_addr_space_t) addressSpace->addressSpace;
  a = unw_get_accessors (as);
  if (! a->access_mem)
    return new lib::unwind::ElfImage((jint) -1);

  logFine(this, logger, "checked access_mem");
  /* Try to decide whether it's an ELF image before bringing it all
     in.  */
  if (size <= EI_CLASS || size <= sizeof (magic))
    return new lib::unwind::ElfImage((jint) -1);

  if (sizeof (magic) >= SELFMAG)
    {
      int ret = (*a->access_mem) (as, (unw_word_t) segbase, &magic,
                                  0, (void *) addressSpace);
      if (ret < 0)
        return new lib::unwind::ElfImage((jint) ret);

      if (memcmp (&magic, ELFMAG, SELFMAG) != 0)
        return new lib::unwind::ElfImage((jint) -1);
    }

  logFine(this, logger, "checked magic size");

  image = mmap (0, size, PROT_READ | PROT_WRITE,
                MAP_PRIVATE | MAP_ANONYMOUS | MAP_32BIT, -1, 0);
  if (image == MAP_FAILED)
    return new lib::unwind::ElfImage((jint) -1);

  logFine(this, logger, "mapped elfImage");
  if (sizeof (magic) >= SELFMAG)
    {
      *(unw_word_t *)image = magic;
      hi = sizeof (magic);
    }
  else
    hi = 0;

  logFine(this, logger, "checked magic");
  for (; hi < size; hi += sizeof (unw_word_t))
    {
      logFinest(this, logger, "Reading memory segbase: 0x%lx, image: %p, hi: 0x%lx, at: 0x%lx to location: %p",
                segbase , image , hi, segbase+hi, (char *) image + hi);
      int ret = (*a->access_mem) (as, segbase + hi,(unw_word_t *) ((char *) image + hi),
                                  0, (void *) addressSpace);

      if (ret < 0)
        {
          munmap (image, size);
          return new lib::unwind::ElfImage((jint) ret);
        }
    }

  logFine(this, logger, "read memory into elf image");

  if (segbase == mapoff)
    mapoff = 0;

  lib::unwind::ElfImage* elfImage
    = new lib::unwind::ElfImage((jlong) image, (jlong) size,
				(jlong) segbase, (jlong) mapoff);

  jLogFine(this, logger, "elfImage returned: {1}", elfImage);
  return elfImage;
}

jlong
lib::unwind::TARGET::getStartIP(gnu::gcj::RawDataManaged* procInfo)
{
  return (jlong) ((unw_proc_info_t *) procInfo)->start_ip;
}

jlong
lib::unwind::TARGET::getEndIP(gnu::gcj::RawDataManaged* procInfo)
{
  return (jlong) ((unw_proc_info_t *) procInfo)->end_ip;
}

jlong
lib::unwind::TARGET::getLSDA(gnu::gcj::RawDataManaged* procInfo)
{
  return (jlong) ((unw_proc_info_t *) procInfo)->lsda;
}

jlong
lib::unwind::TARGET::getHandler(gnu::gcj::RawDataManaged* procInfo)
{
  return (jlong) ((unw_proc_info_t *) procInfo)->handler;
}

jlong
lib::unwind::TARGET::getGP(gnu::gcj::RawDataManaged* procInfo)
{
  return (jlong) ((unw_proc_info_t *) procInfo)->gp;
}

jlong
lib::unwind::TARGET::getFlags(gnu::gcj::RawDataManaged* procInfo)
{
  return (jlong) ((unw_proc_info_t *) procInfo)->flags;
}

jint
lib::unwind::TARGET::getFormat(gnu::gcj::RawDataManaged* procInfo)
{
  return (jint) ((unw_proc_info_t *) procInfo)->format;
}

jint
lib::unwind::TARGET::getUnwindInfoSize(gnu::gcj::RawDataManaged* procInfo)
{
  return (jint) ((unw_proc_info_t *) procInfo)->unwind_info_size;
}

gnu::gcj::RawData*
lib::unwind::TARGET::getUnwindInfo(gnu::gcj::RawDataManaged* procInfo)
{
  return (gnu::gcj::RawData *) ((unw_proc_info_t *) procInfo)->unwind_info;
}
