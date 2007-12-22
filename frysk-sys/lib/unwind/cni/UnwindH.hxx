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

#include <libelf.h>
#include <gelf.h>

#include <gcj/cni.h>

#include <gnu/gcj/RawDataManaged.h>

#include <java/lang/String.h>
#include <java/lang/Object.h>
#include <java/util/logging/Logger.h>
#include <java/util/logging/Level.h>
#include <java/lang/ArrayIndexOutOfBoundsException.h>

#include "inua/eio/ByteBuffer.h"
#include "lib/dwfl/Dwfl.h"
#include "lib/unwind/Unwind.h"
#include "lib/unwind/AddressSpace.h"
#include "lib/unwind/Cursor.h"
#include "lib/unwind/ByteOrder.h"
#include "lib/unwind/CachingPolicy.h"
#include "lib/unwind/ProcInfo.h"
#include "lib/unwind/ElfImage.h"
#include LIB_UNWIND_REGISTERS_H

#include "frysk/sys/cni/Errno.hxx"

#include LIB_UNWIND_UNWIND_TARGET_H

#ifndef MAX_VDSO_SIZE
# define MAX_VDSO_SIZE ((size_t) sysconf (_SC_PAGESIZE))
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
 * There is never any dynamic info in our case.
 */
static int
get_dyn_info_list_addr (::unw_addr_space_t as, ::unw_word_t *dilap,
			void *arg)
{
  return -UNW_ENOINFO;
}

/*
 * Perform memory read/write.  Implement as copy-in, copy-out.
 */
static int
access_mem (::unw_addr_space_t as, ::unw_word_t addr,
	    ::unw_word_t *valp, int write, void *arg)
{
  try
    {
      jbyteArray tmp = JvNewByteArray (sizeof (unw_word_t));
      memcpy (elements(tmp), valp, JvGetArrayLength(tmp));
      int ret = addressSpace(arg)->accessMem((jlong) addr,
					     tmp, (jboolean) write);
      memcpy(valp, elements(tmp), JvGetArrayLength(tmp));
      return ret;
    }
  catch (java::lang::RuntimeException *t)
    {
      // We have to catch all RuntimeExceptions here since there
      // is no indicator for just "invalid memory location".
      // Core files might have "holes" in their memory.
      return -UNW_EINVAL;
    }
}

/*
 * perform register read/write
 */
static int
access_reg(::unw_addr_space_t as, ::unw_regnum_t regnum,
	   ::unw_word_t *valp, int write, void *arg)
{
  jbyteArray tmp = JvNewByteArray(sizeof (unw_word_t));
  // Map the REGNUM back to the published ENUM.
  java::lang::Number* num = lib::unwind::TARGET_REGISTERS::valueOf(regnum);
  memcpy (elements (tmp), valp, JvGetArrayLength(tmp));
  if (write)
    addressSpace(arg)->setReg(num, *valp);
  else
    *valp = addressSpace(arg)->getReg(num);
  return 0;
}

/*
 * Perform a floating point register read/write
 */
static int
access_fpreg(::unw_addr_space_t as, ::unw_regnum_t regnum,
	     ::unw_fpreg_t *fpvalp, int write, void *arg)
{
  jbyteArray tmp = JvNewByteArray(sizeof (unw_fpreg_t));
  // Map the REGNUM back to the published ENUM.
  java::lang::Number* num = lib::unwind::TARGET_REGISTERS::valueOf(regnum);
  // Implement read/modify/write style op.
  memcpy (elements (tmp), fpvalp, JvGetArrayLength(tmp));
  int ret = addressSpace(arg)->accessReg(num, tmp, (jboolean) write);
  memcpy(fpvalp, elements (tmp), JvGetArrayLength(tmp));
  return ret;
}

/*
 * Resumes the process at the provided stack level.
 * We never resume a process through libunwind.
 */
static int
resume(::unw_addr_space_t as, ::unw_cursor_t *cp, void *arg)
{
  return -UNW_EINVAL;
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
    // This should never be called, always return an error.
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

static void
verifyBounds(jlong offset, jint length, jbyteArray bytes, jint start, int size)
{
  verifyBounds(bytes, start, length);
  if (offset < 0)
    throw new java::lang::ArrayIndexOutOfBoundsException(offset);
  if (offset + length > size)
    throw new java::lang::ArrayIndexOutOfBoundsException(offset + length);
}

void
lib::unwind::TARGET::getRegister(gnu::gcj::RawDataManaged* cursor,
                                 java::lang::Number* num,
				 jlong offset, jint length,
				 jbyteArray bytes, jint start)
{
  int regNum = num->intValue();
  int status;
  union {
    unw_word_t w;
    unw_fpreg_t fp;
  } word;
  int size;
  if (unw_is_fpreg(regNum))
    size = sizeof(word.fp);
  else
    size = sizeof(word.w);
  verifyBounds(offset, length, bytes, start, size);
  if (unw_is_fpreg(regNum))
    status = unw_get_fpreg((::unw_cursor_t *) cursor,
			   (::unw_regnum_t) regNum,
			   &word.fp);
  else
    status = unw_get_reg((::unw_cursor_t *) cursor,
			 (::unw_regnum_t) regNum,
			 &word.w);
  if (status != 0)
    throwRuntimeException("get register failed");
  memcpy(elements(bytes) + start, (uint8_t*)&word + offset, length);
}

void
lib::unwind::TARGET::setRegister(gnu::gcj::RawDataManaged* cursor,
                                 java::lang::Number *num,
				 jlong offset, jint length,
				 jbyteArray bytes, jint start)
{
  int regNum = num->intValue();
  int status;
  union {
    unw_word_t w;
    unw_fpreg_t fp;
  } word;
  int size;
  if (unw_is_fpreg(regNum))
    size = sizeof(word.fp);
  else
    size = sizeof(word.w);
  verifyBounds(offset, length, bytes, start, size);
  if (unw_is_fpreg(regNum))
    status = unw_get_fpreg((::unw_cursor_t *) cursor,
			   (::unw_regnum_t) regNum,
			   &word.fp);
  else
    status = unw_get_reg((::unw_cursor_t *) cursor,
			 (::unw_regnum_t) regNum,
			 &word.w);
  if (status != 0)
    throwRuntimeException("set register failed");
  memcpy((uint8_t*)&word + offset, elements(bytes) + start, length);
  if (unw_is_fpreg(regNum))
    status = unw_set_fpreg((::unw_cursor_t *) cursor,
			   regNum, word.fp);
  else
    status = unw_set_reg((::unw_cursor_t *) cursor,
			 regNum, word.w);
  if (status != 0)
    throwRuntimeException("set register failed");
}

jlong
lib::unwind::TARGET::getSP(gnu::gcj::RawDataManaged* cursor)
{
  unw_word_t sp;
  int status = unw_get_reg((::unw_cursor_t *) cursor, UNW_REG_SP, &sp);
  if (status < 0)
    return 0; // bottom of stack.
  else
    return sp;
}

jlong
lib::unwind::TARGET::getIP(gnu::gcj::RawDataManaged* cursor)
{
  unw_word_t ip;
  int status = unw_get_reg((::unw_cursor_t *) cursor, UNW_REG_IP, &ip);
  if (status < 0)
    return 0; // bottom of stack.
  else
    return ip;
}

jlong
lib::unwind::TARGET::getCFA(gnu::gcj::RawDataManaged* cursor)
{
#ifdef UNW_TARGET_X86
#define FRYSK_UNW_REG_CFA UNW_X86_CFA
#else
#ifdef UNW_TARGET_X86_64
#define FRYSK_UNW_REG_CFA UNW_X86_64_CFA
#else
// This is wasteful, but there is no generic UNW_REG_CFA.
// So just unwind and return the stack pointer.
#define FRYSK_UNW_REG_CFA UNW_REG_SP
cursor = copyCursor (cursor);
if (unw_step((unw_cursor_t *) cursor) < 0)
  return 0;
#endif
#endif

  unw_word_t cfa;
  int status = unw_get_reg((::unw_cursor_t *) cursor, FRYSK_UNW_REG_CFA, &cfa);
  if (status < 0)
    return 0; // bottom of stack.
  else
    return cfa;
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

// Return NULL when eh_frame_hdr cannot be found.
// Also fills in ip->start_ip, ip->end_ip and ip->gp.
// peh_vaddr will point to the address of the eh_frame_hdr in the main
// address space of the inferior.
static char *
get_eh_frame_hdr_addr(unw_proc_info_t *pi, char *image, size_t size,
		      unsigned long segbase, unw_word_t *peh_vaddr)
{
  if (elf_version(EV_CURRENT) == EV_NONE)
    return NULL;

  Elf *elf = elf_memory(image, size);
  if (elf == NULL)
    return NULL;

  GElf_Ehdr ehdr;
  if (gelf_getehdr(elf, &ehdr) == NULL)
    return NULL;
  
  GElf_Phdr phdr;
  int ptxt_ndx = -1, peh_hdr_ndx = -1, pdyn_ndx = -1;
  for (int i = 0; i < ehdr.e_phnum; i++)
    {
      if (gelf_getphdr (elf, i, &phdr) == NULL)
	return NULL;
      
      switch (phdr.p_type)
        {
        case PT_LOAD:
          if (phdr.p_vaddr == segbase)
            ptxt_ndx = i;
          break;
	  
        case PT_GNU_EH_FRAME:
          peh_hdr_ndx = i;
          break;
	  
        case PT_DYNAMIC:
          pdyn_ndx = i;
          break;

        default:
          break;
        }
    }

  if (ptxt_ndx == -1 || peh_hdr_ndx == -1)
    return NULL;

  GElf_Phdr ptxt, peh_hdr;
  if (gelf_getphdr (elf, ptxt_ndx, &ptxt) == NULL)
    return NULL;

  if (gelf_getphdr (elf, peh_hdr_ndx, &peh_hdr) == NULL)
    return NULL;

  if (pdyn_ndx != -1)
    {
      /* For dynamicly linked executables and shared libraries,
	 DT_PLTGOT is the value that data-relative addresses are
	 relative to for that object.  We call this the "gp". */
      GElf_Phdr pdyn;
      if (gelf_getphdr (elf, pdyn_ndx, &pdyn) == NULL)
	return NULL;

      Elf_Scn *pdyn_scn = gelf_offscn(elf, pdyn.p_offset);
      if (pdyn_scn == NULL)
	return NULL;

      Elf_Data *pdyn_data;
      pdyn_data = elf_getdata (pdyn_scn, NULL);
      if (pdyn_data == NULL)
	return NULL;

      GElf_Shdr pdyn_shdr;
      if (gelf_getshdr (pdyn_scn, &pdyn_shdr) == NULL)
	return NULL;

      for (unsigned int i = 0;
	   i < pdyn_shdr.sh_size / pdyn_shdr.sh_entsize; i++)
	{
	  GElf_Dyn dyn;
	  if (gelf_getdyn (pdyn_data, i, &dyn) == NULL)
	    return NULL;

	  if (dyn.d_tag == DT_PLTGOT)
	    {
	      /* Assume that _DYNAMIC is writable and GLIBC has
		 relocated it (true for x86 at least). */
	      pi->gp = dyn.d_un.d_ptr;
	      break;
	    }
	}
    }
  else
    /* Otherwise this is a static executable with no _DYNAMIC.  Assume
       that data-relative addresses are relative to 0, i.e.,
       absolute. */
    pi->gp = 0;

  pi->start_ip = segbase;
  pi->end_ip = segbase + ptxt.p_memsz;

  *peh_vaddr = peh_hdr.p_vaddr;

  char *hdr = image + peh_hdr.p_offset;
  return hdr;
}

// The following is a local address space memory accessor used by
// unw_get_unwind_table to access the eh_frame_hdr.  The arg pointer
// is the base address, addr is the offset from the base address.
static int 
local_access_mem (unw_addr_space_t as, unw_word_t addr,
		  unw_word_t *val, int write, void *arg) 
{
  // Writing is not supported
  if (write)
    return -UNW_EINVAL;
  else
    *val = *(unw_word_t *) (addr + (char *) arg);

  return UNW_ESUCCESS;
}
  
static unw_accessors_t local_accessors
  = {NULL, NULL, NULL, local_access_mem, NULL, NULL, NULL, NULL};

lib::unwind::ProcInfo*
lib::unwind::TARGET::createProcInfoFromElfImage(lib::unwind::AddressSpace* addressSpace,
						jlong ip,
						jboolean needUnwindInfo,
						lib::unwind::ElfImage* elfImage)
{
  if (elfImage == NULL || elfImage->ret != 0)
    return new lib::unwind::ProcInfo(-UNW_ENOINFO);

  unw_proc_info_t *procInfo
    = (::unw_proc_info_t *) JvAllocBytes(sizeof (::unw_proc_info_t));

  logFine(this, logger, "Pre unw_get_unwind_table");
  
  unw_word_t peh_vaddr = 0;
  char *eh_table_hdr = get_eh_frame_hdr_addr(procInfo,
					     (char *) elfImage->elfImage,
					     elfImage->size,
					     elfImage->segbase,
					     &peh_vaddr);

  //jsize length = JvGetStringUTFLength (elfImage->name);
  //char buffer[length + 1];
  //JvGetStringUTFRegion (elfImage->name, 0, elfImage->name->length(), buffer);
  //buffer[length] = '\0';
  //fprintf(stderr, "%s: %p\n", buffer, eh_table_hdr);

  if (eh_table_hdr == NULL)
    return new lib::unwind::ProcInfo(-UNW_ENOINFO);

  int ret = unw_get_unwind_table((unw_word_t) ip,
				 procInfo,
				 (int) needUnwindInfo,
				 &local_accessors,
				 // virtual address
				 peh_vaddr,
				 // address adjustment
				 eh_table_hdr - peh_vaddr);
  
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
                MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
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
    = new lib::unwind::ElfImage(JvNewStringLatin1("[vdso]"), (jlong) image, (jlong) size,
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
