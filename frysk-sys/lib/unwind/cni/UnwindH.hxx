// This file is part of the program FRYSK.
//
// Copyright 2007, 2008, Red Hat Inc.
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
#include <errno.h>
#include <string.h>

#include <libdwfl.h>
#include LIBUNWIND_TARGET_H

#include <libelf.h>
#include <gelf.h>

#include <gcj/cni.h>

#include <gnu/gcj/RawDataManaged.h>

#include <java/lang/String.h>
#include <java/lang/Object.h>
#include <java/lang/ArrayIndexOutOfBoundsException.h>

#include "inua/eio/ByteBuffer.h"
#include "frysk/UserException.h"
#include "frysk/rsl/Log.h"
#include "frysk/rsl/cni/Log.hxx"
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

using namespace java::lang;
using namespace lib::unwind;

static AddressSpace*
vec(void* arg) {
  AddressSpace* space = (AddressSpace*)arg;
  if (space->magic != AddressSpace::MAGIC)
    throwRuntimeException ("bad AddressSpace");
  return space;
}

/*
 * Callback: Get misc. proc info
 */
static int
find_proc_info(::unw_addr_space_t as, ::unw_word_t ip,
	       ::unw_proc_info_t *pip, int need_unwind_info,
	       void *addressSpace)
{
  ProcInfo* procInfo = new ProcInfo(vec(addressSpace)->unwinder,
				    (long) JvMalloc(sizeof(unw_proc_info_t)));
  int ok = vec(addressSpace)->findProcInfo((jlong)ip,
					   (jboolean)need_unwind_info,
					   procInfo);
  if (ok < 0)
    return ok;
  // Extract the info.
  memcpy(pip, (void*) procInfo->unwProcInfo, sizeof (unw_proc_info_t));
  return 0;
}

/*
 * Callback: Free space allocated during find_proc_info
 */
static void
put_unwind_info(::unw_addr_space_t as, ::unw_proc_info_t *proc_info,
		void *addressSpace) {
  AddressSpace* space = vec(addressSpace);
  // This is passing up a stack pointer, which may then be freed.
  ProcInfo* procInfo = new ProcInfo(space->unwinder, (jlong) proc_info);
  space->putUnwindInfo (procInfo);
}

/*
 * Callback: Get the head of the dynamic unwind registration list.
 * There is never any dynamic info in our case.
 */
static int
get_dyn_info_list_addr(::unw_addr_space_t as, ::unw_word_t *dilap,
		       void *addressSpace) {
  return -UNW_ENOINFO;
}

/*
 * Callback: Perform memory read/write.  Implement as copy-in,
 * copy-out.
 */
static int
access_mem(::unw_addr_space_t as, ::unw_word_t addr,
	   ::unw_word_t *valp, int write, void *addressSpace) {
  try {
    jbyteArray tmp = JvNewByteArray (sizeof (unw_word_t));
    memcpy (elements(tmp), valp, JvGetArrayLength(tmp));
    int ret = vec(addressSpace)->accessMem((jlong) addr,
					   tmp, (jboolean) write);
    memcpy(valp, elements(tmp), JvGetArrayLength(tmp));
    return ret;
  } catch (Throwable *t) {
    if (frysk::UserException::class$.isInstance(t)) {
      // We have to catch all RuntimeExceptions here since there
      // is no indicator for just "invalid memory location".
      // Core files might have "holes" in their memory.
      return -UNW_EINVAL;
    } else {
      throw t;
    }
  }
}

/*
 * Callback: perform register read/write
 */
static int
access_reg(::unw_addr_space_t as, ::unw_regnum_t regnum,
	   ::unw_word_t *valp, int write, void *addressSpace) {
  jbyteArray tmp = JvNewByteArray(sizeof (unw_word_t));
  // Map the REGNUM back to the published ENUM.
  Number* num = TARGET_REGISTERS::valueOf(regnum);
  memcpy (elements (tmp), valp, JvGetArrayLength(tmp));
  if (write)
    vec(addressSpace)->setReg(num, *valp);
  else
    *valp = vec(addressSpace)->getReg(num);
  return 0;
}

/*
 * Callback: Perform a floating point register read/write
 */
static int
access_fpreg(::unw_addr_space_t as, ::unw_regnum_t regnum,
	     ::unw_fpreg_t *fpvalp, int write, void *addressSpace) {
  jbyteArray tmp = JvNewByteArray(sizeof (unw_fpreg_t));
  // Map the REGNUM back to the published ENUM.
  Number* num = TARGET_REGISTERS::valueOf(regnum);
  // Implement read/modify/write style op.
  memcpy (elements (tmp), fpvalp, JvGetArrayLength(tmp));
  int ret = vec(addressSpace)->accessReg(num, tmp, (jboolean) write);
  memcpy(fpvalp, elements (tmp), JvGetArrayLength(tmp));
  return ret;
}

/*
 * Callback: Resumes the process at the provided stack level.  We
 * never resume a process through libunwind.
 */
static int
resume(::unw_addr_space_t as, ::unw_cursor_t *cp, void *addressSpace) {
  return -UNW_EINVAL;
}

/*
 * Callback: Returns the name of the procedure that the provided
 * address is in as well as the offset from the start of the
 * procedure.
 */
static int
get_proc_name(::unw_addr_space_t as,
	      ::unw_word_t addr, char *bufp,
	      size_t buf_len, ::unw_word_t *offp, void *addressSpace) {
  // This should never be called, always return an error.
  return -UNW_ENOMEM;
}

jlong
TARGET::createCursor(AddressSpace* addressSpace, jlong unwAddressSpace)
{
  logf(fine, this, "createCursor from address-space %lxf", (long) unwAddressSpace);
  unw_cursor_t* unwCursor = (unw_cursor_t*) JvMalloc(sizeof(::unw_cursor_t));
  // XXX: Need to zero out the cursor, as unw_init_remote doesn't seem
  // to do it.
  memset(unwCursor, 0, sizeof(*unwCursor));
  unw_init_remote(unwCursor, (unw_addr_space_t) unwAddressSpace,
		  (void *) addressSpace);
  logf(fine, this, "createCursor at %lx", (long) unwCursor);
  return (jlong) unwCursor;
}

void
TARGET::destroyCursor(jlong unwCursor) {
  logf(fine, this, "destroyCursor at %lx", (long) unwCursor);
  JvFree((unw_cursor_t*) (long) unwCursor);
}

jlong
TARGET::createAddressSpace(ByteOrder * byteOrder) {
  logf(fine, this, "createAddressSpace, byteOrder %d", (int) byteOrder->hashCode());
  static unw_accessors_t accessors = {
    find_proc_info ,
    put_unwind_info,
    get_dyn_info_list_addr,
    access_mem,
    access_reg,
    access_fpreg,
    resume,
    get_proc_name
  };
  jlong unwAddressSpace
    = (jlong) unw_create_addr_space(&accessors, (int) byteOrder->hashCode());
  logf(fine, this, "createAddressSpace at %lx", (long)unwAddressSpace);
  return unwAddressSpace;
}

void
TARGET::destroyAddressSpace(jlong unwAddressSpace) {
  logf(fine, this, "destroyAddressSpace %lx", (long)unwAddressSpace);
  unw_destroy_addr_space((unw_addr_space_t) unwAddressSpace);
}

void
TARGET::setCachingPolicy(jlong unwAddressSpace, CachingPolicy* cachingPolicy) {
  log(fine, this, "setCachingPolicy, cachingPolicy:", cachingPolicy);
  unw_set_caching_policy((unw_addr_space_t) unwAddressSpace,
                         (unw_caching_policy_t) cachingPolicy->hashCode());
}

jint
TARGET::isSignalFrame(jlong unwCursor) {
  logf(fine, this, "isSignalFrame");
  return unw_is_signal_frame((unw_cursor_t*) (long) unwCursor);
}

jint
TARGET::step(jlong unwCursor) {
  logf(fine, this, "step cursor: %lx", (long) unwCursor);
  return unw_step((unw_cursor_t*) (long) unwCursor);
}

static void
verifyBounds(jlong offset, jint length, jbyteArray bytes,
	     jint start, int size) {
  verifyBounds(bytes, start, length);
  if (offset < 0)
    throw new ArrayIndexOutOfBoundsException(offset);
  if (offset + length > size)
    throw new ArrayIndexOutOfBoundsException(offset + length);
}

void
TARGET::getRegister(jlong unwCursor,
		    Number* num,
		    jlong offset, jint length,
		    jbyteArray bytes, jint start) {
  int regNum = num->intValue();
  logf(fine, this, "getRegister %d from %lx, offset %ld length %d start %d",
       regNum, (long)unwCursor, (long) offset, (int)length, (int)start);
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
  if (unw_is_fpreg(regNum)) {
    status = unw_get_fpreg((::unw_cursor_t*) (long) unwCursor,
			   (::unw_regnum_t) regNum,
			   &word.fp);
  } else {
    status = unw_get_reg((::unw_cursor_t*) (long) unwCursor,
			 (::unw_regnum_t) regNum,
			 &word.w);
    logf(fine, this, "getRegister status %d %lx", status, (long)word.w);
  }
  if (status != 0)
    throwRuntimeException("get register failed");
  memcpy(elements(bytes) + start, (uint8_t*)&word + offset, length);
}

void
TARGET::setRegister(jlong unwCursor,
		    Number *num,
		    jlong offset, jint length,
		    jbyteArray bytes, jint start) {
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
    status = unw_get_fpreg((::unw_cursor_t*) (long) unwCursor,
			   (::unw_regnum_t) regNum,
			   &word.fp);
  else
    status = unw_get_reg((::unw_cursor_t*) (long) unwCursor,
			 (::unw_regnum_t) regNum,
			 &word.w);
  if (status != 0)
    throwRuntimeException("set register failed");
  memcpy((uint8_t*)&word + offset, elements(bytes) + start, length);
  if (unw_is_fpreg(regNum))
    status = unw_set_fpreg((::unw_cursor_t*) (long) unwCursor,
			   regNum, word.fp);
  else
    status = unw_set_reg((::unw_cursor_t*) (long) unwCursor,
			 regNum, word.w);
  if (status != 0)
    throwRuntimeException("set register failed");
}

jlong
TARGET::getSP(jlong unwCursor) {
  unw_word_t sp;
  int status = unw_get_reg((::unw_cursor_t*) (long) unwCursor, UNW_REG_SP, &sp);
  if (status < 0)
    return 0; // bottom of stack.
  else
    return sp;
}

jlong
TARGET::getIP(jlong unwCursor) {
  unw_word_t ip;
  int status = unw_get_reg((::unw_cursor_t*) (long) unwCursor, UNW_REG_IP, &ip);
  if (status < 0)
    return 0; // bottom of stack.
  else
    return ip;
}

jlong
TARGET::getCFA(jlong unwCursor) {
#ifdef UNW_TARGET_X86
#define FRYSK_UNW_REG_CFA UNW_X86_CFA
#else
#ifdef UNW_TARGET_X86_64
#define FRYSK_UNW_REG_CFA UNW_X86_64_CFA
#else
  // This is wasteful, but there is no generic UNW_REG_CFA.
  // So just unwind and return the stack pointer.
#define FRYSK_UNW_REG_CFA UNW_REG_SP
  unw_cursor_t copy;
  memcpy(&copy, (unw_cursor_t*) (long) unwCursor, sizeof (copy));
  if (unw_step(&copy) < 0)
    return 0;
  unwCursor = (jlong) &copy;
#endif
#endif

  unw_word_t cfa;
  int status = unw_get_reg((::unw_cursor_t*) (long) unwCursor,
			   FRYSK_UNW_REG_CFA, &cfa);
  if (status < 0)
    return 0; // bottom of stack.
  else
    return cfa;
}


jint
TARGET::getContext(jlong context) {
  return (jint) unw_getcontext((::unw_context_t *) context);
}

jlong
TARGET::copyCursor(jlong unwCursor) {
  ::unw_cursor_t *nativeCursor
    = (::unw_cursor_t*) JvMalloc(sizeof(::unw_cursor_t));
  // Create a local copy of the unwind cursor
  memcpy(nativeCursor, (unw_cursor_t*) (long) unwCursor,
	 sizeof (::unw_cursor_t));
  logf(fine, this, "copyCursor %lx to %lx", (long) unwCursor,
       (long) nativeCursor);
  return (jlong) nativeCursor;
}

jlong
TARGET::getProcInfo(jlong unwCursor) {
  logf(fine, this, "getProcInfo cursor: %lx", (long) unwCursor);
  unw_proc_info_t *procInfo
    = (::unw_proc_info_t *) JvMalloc(sizeof (::unw_proc_info_t));
  int ret = unw_get_proc_info((::unw_cursor_t*) (long) unwCursor, procInfo);

  logf(fine, this, "getProcInfo finished get_proc_info %lx", (long) procInfo);
  if (ret < 0) {
    JvFree(procInfo);
    return 0;
  } else {
    return (jlong)procInfo;
  }
}

void
TARGET::destroyProcInfo(jlong unwProcInfo) {
  JvFree((unw_proc_info_t*) (long) unwProcInfo);
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

  Elf_Data *debug_frame_data = NULL;
  size_t shstrndx;
  if (elf_getshstrndx (elf, &shstrndx) >= 0)
    {
      Elf_Scn *scn = NULL;
      while ((scn = elf_nextscn (elf, scn)) != NULL
	     && debug_frame_data == NULL)
	{
	  GElf_Shdr shdr;
	  if (gelf_getshdr (scn, &shdr) != NULL
	      && shdr.sh_type == SHT_PROGBITS)
	    {
	      const char *name = elf_strptr (elf, shstrndx, shdr.sh_name);
	      if (strcmp (name, ".debug_frame") == 0)
		debug_frame_data = elf_getdata (scn, NULL);
	    }
	}
    }

  if (ptxt_ndx == -1 || (peh_hdr_ndx == -1 && debug_frame_data == NULL))
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

  char *hdr;
  if (debug_frame_data != NULL && debug_frame_data->d_buf != NULL
      && debug_frame_data->d_size != 0)
    {
      pi->format = UNW_INFO_FORMAT_TABLE;
      pi->unwind_info_size = debug_frame_data->d_size / sizeof (unw_word_t);
      hdr = (char *) debug_frame_data->d_buf;
    }
  else
    {
      pi->format = UNW_INFO_FORMAT_REMOTE_TABLE;
      hdr = image + peh_hdr.p_offset;
    }
  return hdr;
}

/**
 * The following are local memory image address space memory accessors
 * used by unw_get_unwind_table and run_cfi_program to access and
 * interpret the eh_frame_hdr.  The arg pointer contains an image
 * descriptor, ADDR is the offset into the eh-frame table within the
 * image.
 */
struct image {
  int magic;
  void *bytes;
  size_t size;
  char *table;
};
const int IMAGE_MAGIC = 0xfeed;

static int 
image_access_mem(unw_addr_space_t as, unw_word_t addr,
		 unw_word_t *val, int write, void *arg) {
  struct image *image = (struct image*) arg;
  if (image->magic != IMAGE_MAGIC) {
    throw new RuntimeException(JvNewStringUTF("bad image magic number"));
  }
  // Writing is not supported
  if (write)
    return -UNW_EINVAL;
  else
    *val = *(unw_word_t *) (image->table + addr);
  return UNW_ESUCCESS;
}
  
static void
image_put_unwind_info(::unw_addr_space_t as, ::unw_proc_info_t *proc_info,
		      void *arg) {
  struct image *image = (struct image*) arg;
  if (image->magic != IMAGE_MAGIC) {
    fprintf(stderr, "corrupt image pointer\n");
    throw new RuntimeException(JvNewStringUTF("bad image magic number"));
  }
  munmap(image->bytes, image->size);
  ::free(image);
}

static unw_accessors_t image_accessors = {
  NULL, image_put_unwind_info, NULL, image_access_mem, NULL, NULL, NULL, NULL
};

static jint
fillProcInfoFromImage(frysk::rsl::Log* fine,
		      const char* name,
		      jlong unwProcInfo,
		      jlong ip,
		      jboolean needUnwindInfo,
		      void *bytes,
		      long size,
		      long segbase) {
  unw_proc_info_t *procInfo = (::unw_proc_info_t *) unwProcInfo;
  logf(fine, "fillProcInfoFromImage"
       " %s unwProcInfo %lx, ip %lx, bytes %p, size %ld, segBase %lx",
       name, (long) unwProcInfo, (long)ip, bytes, size, segbase);
  
  unw_word_t peh_vaddr = 0;
  char *eh_table_hdr = get_eh_frame_hdr_addr(procInfo,
					     (char *) bytes,
					     size, segbase,
					     &peh_vaddr);
  if (eh_table_hdr == NULL) {
    logf(fine, "get_eh_frame_hdr failed");
    munmap(bytes, size);
    return -UNW_ENOINFO;
  }

  struct image *image = new struct image();
  if (image == NULL) {
    munmap(bytes, size);
    return -UNW_ENOINFO;
  }
  image->magic = IMAGE_MAGIC;
  image->bytes = bytes;
  image->size = size;

  int ret;
  if (procInfo->format == UNW_INFO_FORMAT_REMOTE_TABLE) {
    // address adjustment
    image->table = eh_table_hdr - peh_vaddr;
    ret = unw_get_unwind_table((unw_word_t) ip,
			       procInfo,
			       (int) needUnwindInfo,
			       &image_accessors,
			       // virtual address
			       peh_vaddr,
			       image);
  } else {
    image->table = eh_table_hdr;
    ret = unw_get_unwind_table((unw_word_t) ip,
                               procInfo,
                               (int) needUnwindInfo,
                               &image_accessors,
                               // virtual address
                               0,
                               // address adjustment
                               image);
  }
  logf(fine, "Post unw_get_unwind_table %d", ret);
  return ret;
}

jint
TARGET::fillProcInfoFromVDSO(jlong unwProcInfo, jlong ip,
			     jboolean needUnwindInfo,
			     AddressSpace* addressSpace,
			     jlong lowAddress, jlong highAddress,
			     jlong offset) {
  logf(fine, this, "fillProcInfoFromVDSO"
       " segbase: 0x%lx, highAddress: 0x%lx, mapoff: 0x%lx",
       (unsigned long) lowAddress, (unsigned long) highAddress,
       (unsigned long) offset);
  void *image;
  size_t size;
  unw_word_t magic;
  unw_accessors_t *a;
  unsigned long segbase = (unsigned long) lowAddress;
  unsigned long hi = (unsigned long) highAddress;

  size = hi - segbase;
  if (size > MAX_VDSO_SIZE)
    return -UNW_ENOINFO;
  logf(fine, this, "checked size, 0x%lx", (unsigned long) size);

  logf(fine, this, "checking access_mem");
  unw_addr_space_t as = (unw_addr_space_t) addressSpace->unwAddressSpace;
  a = unw_get_accessors (as);
  if (! a->access_mem)
    return -UNW_ENOINFO;

  // Try to decide whether it's an ELF image before bringing it all
  // in.
  logf(fine, this, "checking magic");
  if (size <= EI_CLASS || size <= sizeof (magic))
    return -UNW_ENOINFO;
  if (sizeof (magic) >= SELFMAG) {
    int ret = (*a->access_mem) (as, (unw_word_t) segbase, &magic,
				0, (void *) addressSpace);
    if (ret < 0) {
      logf(fine, this, "error accessing VDSO %d", ret);
      return ret;
    }
    if (memcmp (&magic, ELFMAG, SELFMAG) != 0) {
      logf(fine, this, "VDSO has bad magic");
      return -UNW_ENOINFO;
    }
  }

  logf(fine, this, "mapping memory for image (using mmap, so can umaped)");
  image = mmap (0, size, PROT_READ | PROT_WRITE,
                MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
  if (image == MAP_FAILED)
    return -UNW_ENOINFO;

  logf(fine, this, "checked magic");
  if (sizeof (magic) >= SELFMAG) {
    *(unw_word_t *)image = magic;
    hi = sizeof (magic);
  } else {
    hi = 0;
  }

  logf(fine, this, "reading in VDSO");
  for (; hi < size; hi += sizeof (unw_word_t)) {
    logf(finest, this, "Reading memory segbase: 0x%lx, image: %p, hi: 0x%lx, at: 0x%lx to location: %p",
	 segbase , image , hi, segbase+hi, (char *) image + hi);
    int ret = (*a->access_mem) (as, segbase + hi,(unw_word_t *) ((char *) image + hi),
				0, (void *) addressSpace);
    if (ret < 0) {
      logf(fine, this, "error reading vdso");
      munmap (image, size);
      return ret;
    }
  }

  return fillProcInfoFromImage(fine, "[vdso]", unwProcInfo, ip, needUnwindInfo,
			       image, size, segbase);
}

jint
TARGET::fillProcInfoFromElfImage(jlong unwProcInfo, jlong ip,
				 jboolean needUnwindInfo,
				 AddressSpace* addressSpace,
				 jstring elfImageName,
				 jlong segbase, jlong hi,
				 jlong mapoff) {
  logf(fine, this, "fillProcInfoFromElfImage");
  struct stat stat;
  void *image;		/* pointer to mmap'd image */
  size_t size;		/* (file-) size of the image */
  int nameSize = JvGetStringUTFLength(elfImageName);
  char name[nameSize+1];
  //JvGetStringUTFRegion(jstring STR, jsize START, jsize LEN, char* BUF);
  JvGetStringUTFRegion(elfImageName, 0, nameSize, name);
  name[nameSize] = '\0';

  logf(fine, this, "opening %s", name);
  int fd = ::open(name, O_RDONLY);
  if (fd < 0) {
    int err = errno;
    logf(fine, this, "open failed: %s", strerror(err));
    return -UNW_ENOINFO;
  }

  logf(fine, this, "stat-ing %d", fd);
  int ret = ::fstat(fd, &stat);
  if (ret < 0) {
    int err = errno;
    ::close(fd);
    logf(fine, this, "fstat failed: %s", strerror(err));
    return -UNW_ENOINFO;
  }

  size = stat.st_size;
  logf(fine, this, "mmaping %d, size %ld", fd, (long) size);
  image = ::mmap(NULL, size, PROT_READ, MAP_PRIVATE, fd, 0);
  if (image == MAP_FAILED) {
    int err = errno;
    ::close(fd);
    logf(fine, this, "mmap failed: %s", strerror(err));
    return -UNW_ENOINFO;
  }
  ::close(fd);
  
  return fillProcInfoFromImage(fine, name, unwProcInfo, ip, needUnwindInfo,
			       image, size, segbase);
}

jint
TARGET::fillProcInfoNotAvailable(jlong unwProcInfo) {
  return -UNW_ENOINFO;
}

jlong
TARGET::getStartIP(jlong unwProcInfo) {
  return (jlong) ((unw_proc_info_t *) unwProcInfo)->start_ip;
}

jlong
TARGET::getEndIP(jlong unwProcInfo) {
  return (jlong) ((unw_proc_info_t *) unwProcInfo)->end_ip;
}

jlong
TARGET::getLSDA(jlong unwProcInfo) {
  return (jlong) ((unw_proc_info_t *) unwProcInfo)->lsda;
}

jlong
TARGET::getHandler(jlong unwProcInfo) {
  return (jlong) ((unw_proc_info_t *) unwProcInfo)->handler;
}

jlong
TARGET::getGP(jlong unwProcInfo) {
  return (jlong) ((unw_proc_info_t *) unwProcInfo)->gp;
}

jlong
TARGET::getFlags(jlong unwProcInfo) {
  return (jlong) ((unw_proc_info_t *) unwProcInfo)->flags;
}

jint
TARGET::getFormat(jlong unwProcInfo) {
  return (jint) ((unw_proc_info_t *) unwProcInfo)->format;
}

jint
TARGET::getUnwindInfoSize(jlong unwProcInfo) {
  return (jint) ((unw_proc_info_t *) unwProcInfo)->unwind_info_size;
}

jlong
TARGET::getUnwindInfo(jlong unwProcInfo) {
  return (jlong) ((unw_proc_info_t *) unwProcInfo)->unwind_info;
}
