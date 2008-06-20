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

#include "jni.hxx"

#include "jnixx/exceptions.hxx"
#include "jnixx/logging.hxx"
#include "jnixx/elements.hxx"
#include "jnixx/bounds.hxx"

using namespace java::lang;
using namespace lib::unwind;

#define UNW_PROC_INFO ((unw_proc_info_t*)unwProcInfo)
#define UNW_ADDRESS_SPACE ((unw_addr_space_t)unwAddressSpace)
#define UNW_CURSOR ((unw_cursor_t*)unwCursor)
#define FINE env, (GetFine(env))

#ifndef MAX_VDSO_SIZE
# define MAX_VDSO_SIZE ((size_t) sysconf (_SC_PAGESIZE))
#endif

/**
 * The address space is going to be accessed in different JNI contexts
 * to when it is passed to libunwind.  Consequently, need to get the
 * ENV from JNI and can't rely on it being passed in.
 */
#define UNW_CONTEXT \
  jnixx::env env = Object::_env_();			\
  AddressSpace addressSpace = AddressSpace(env, (jobject)contextArg)
    

/*
 * Callback: Get misc. proc info
 */
static int
find_proc_info(::unw_addr_space_t as, ::unw_word_t ip,
	       ::unw_proc_info_t *pip, int need_unwind_info,
	       void *contextArg) {
  UNW_CONTEXT;
  ProcInfo procInfo = ProcInfo::New(env,
				    addressSpace.GetUnwinder(env),
				    (long) ::malloc(sizeof(unw_proc_info_t)));
  int ok = addressSpace.findProcInfo(env, (jlong)ip,
				     (jboolean)need_unwind_info,
				     procInfo);
  if (ok < 0)
    return ok;
  // Extract the info.
  memcpy(pip, (void*) procInfo.GetUnwProcInfo(env), sizeof (unw_proc_info_t));
  procInfo.DeleteLocalRef(env);
  return 0;
}

/*
 * Callback: Free space allocated during find_proc_info
 */
static void
put_unwind_info(::unw_addr_space_t as, ::unw_proc_info_t *proc_info,
		void *contextArg) {
  UNW_CONTEXT;
  // This is passing up a stack pointer, which may then be freed.
  ProcInfo procInfo = ProcInfo::New(env, addressSpace.GetUnwinder(env),
				    (jlong) proc_info);
  addressSpace.putUnwindInfo(env, procInfo);
}

/*
 * Callback: Get the head of the dynamic unwind registration list.
 * There is never any dynamic info in our case.
 */
static int
get_dyn_info_list_addr(::unw_addr_space_t as, ::unw_word_t *dilap,
		       void *contextArg) {
  return -UNW_ENOINFO;
}

/*
 * Callback: Perform memory read/write.  Implement as copy-in,
 * copy-out.
 */
static int
access_mem(::unw_addr_space_t as, ::unw_word_t addr,
	   ::unw_word_t *valp, int write, void *contextArg) {
  UNW_CONTEXT;
  try {
    jnixx::jbyteArray jtmp
      = jnixx::jbyteArray::NewByteArray(env, sizeof(unw_word_t));
    jbyteArrayElements tmp = jbyteArrayElements(env, jtmp);
    memcpy(tmp.elements(), valp, sizeof(unw_word_t));
    tmp.release();
    int ret = addressSpace.accessMem(env, (jlong) addr,
				     jtmp, (jboolean) write);
    memcpy(valp, tmp.elements(), sizeof(unw_word_t));
    tmp.release();
    jtmp.DeleteLocalRef(env);
    return ret;
  } catch (Throwable t) {
    if (t.IsInstanceOf(env, frysk::UserException::_class_(env))) {
      // We have to catch all RuntimeExceptions here since there is no
      // indicator for just "invalid memory location".  Core files
      // might have "holes" in their memory.
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
	   ::unw_word_t *valp, int write, void *contextArg) {
  UNW_CONTEXT;
  jnixx::jbyteArray jtmp
    = jnixx::jbyteArray::NewByteArray(env, sizeof(unw_word_t));
  jbyteArrayElements tmp = jbyteArrayElements(env, jtmp);
  // Map the REGNUM back to the published ENUM.
  Number num = TARGET_REGISTERS::valueOf(env, regnum);
  ::memcpy(tmp.elements(), valp, sizeof(unw_word_t));
  tmp.release();
  if (write)
    addressSpace.setReg(env, num, *valp);
  else
    *valp = addressSpace.getReg(env, num);
  num.DeleteLocalRef(env);
  jtmp.DeleteLocalRef(env);
  return 0;
}

/*
 * Callback: Perform a floating point register read/write
 */
static int
access_fpreg(::unw_addr_space_t as, ::unw_regnum_t regnum,
	     ::unw_fpreg_t *fpvalp, int write, void *contextArg) {
  UNW_CONTEXT;
  jnixx::jbyteArray jtmp
    = jnixx::jbyteArray::NewByteArray(env, sizeof (unw_fpreg_t));
  jbyteArrayElements tmp = jbyteArrayElements(env, jtmp);
  // Map the REGNUM back to the published ENUM.
  Number num = TARGET_REGISTERS::valueOf(env, regnum);
  // Implement read/modify/write style op.
  ::memcpy(tmp.elements(), fpvalp, sizeof(unw_fpreg_t));
  tmp.release();
  int ret = addressSpace.accessReg(env, num, jtmp, (jboolean) write);
  ::memcpy(fpvalp, tmp.elements(), sizeof(unw_fpreg_t));
  tmp.release();
  num.DeleteLocalRef(env);
  jtmp.DeleteLocalRef(env);
  return ret;
}

/*
 * Callback: Resumes the process at the provided stack level.  We
 * never resume a process through libunwind.
 */
static int
resume(::unw_addr_space_t as, ::unw_cursor_t *cp, void *contextArg) {
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
	      size_t buf_len, ::unw_word_t *offp, void *contextArg) {
  // This should never be called, always return an error.
  return -UNW_ENOMEM;
}

jlong
TARGET::createCursor(jnixx::env env,
		     AddressSpace addressSpace,
		     jlong unwAddressSpace) {
  logf(FINE, "createCursor from address-space %lx", (long) UNW_ADDRESS_SPACE);
  jlong unwCursor = (jlong)::malloc(sizeof(::unw_cursor_t));
  // XXX: Need to zero out the cursor, as unw_init_remote doesn't seem
  // to do it.
  memset(UNW_CURSOR, 0, sizeof(*UNW_CURSOR));
  unw_init_remote(UNW_CURSOR, UNW_ADDRESS_SPACE, addressSpace._object);
  logf(FINE, "createCursor at %lx", (long) UNW_CURSOR);
  return (jlong) UNW_CURSOR;
}

void
TARGET::destroyCursor(jnixx::env env, jlong unwCursor) {
  logf(FINE, "destroyCursor at %lx", (long) UNW_CURSOR);
  ::free(UNW_CURSOR);
}

jlong
TARGET::createAddressSpace(jnixx::env env, ByteOrder byteOrder) {
  logf(FINE, "createAddressSpace, byteOrder %d",
       (int) byteOrder.hashCode(env));
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
    = (jlong) unw_create_addr_space(&accessors, (int) byteOrder.hashCode(env));
  logf(FINE, "createAddressSpace at %lx", (long)unwAddressSpace);
  return unwAddressSpace;
}

void
TARGET::destroyAddressSpace(jnixx::env env, jlong unwAddressSpace) {
  logf(FINE, "destroyAddressSpace %lx", (long)UNW_ADDRESS_SPACE);
  unw_destroy_addr_space(UNW_ADDRESS_SPACE);
}

void
TARGET::setCachingPolicy(jnixx::env env, jlong unwAddressSpace,
			 CachingPolicy cachingPolicy) {
  log(FINE, "setCachingPolicy, cachingPolicy:", cachingPolicy);
  unw_set_caching_policy(UNW_ADDRESS_SPACE,
                         (unw_caching_policy_t) cachingPolicy.hashCode(env));
}

jint
TARGET::isSignalFrame(jnixx::env env, jlong unwCursor) {
  logf(FINE, "isSignalFrame");
  return unw_is_signal_frame(UNW_CURSOR);
}

jint
TARGET::step(jnixx::env env, jlong unwCursor) {
  logf(FINE, "step cursor: %lx", (long) UNW_CURSOR);
  return unw_step(UNW_CURSOR);
}

static void
verifyBounds(jnixx::env env, jlong offset, jint length,
	     jnixx::jbyteArray bytes, jint start, int size) {
  verifyBounds(env, bytes, start, length);
  if (offset < 0)
    ArrayIndexOutOfBoundsException::New(env, offset).Throw(env);
  if (offset + length > size)
    ArrayIndexOutOfBoundsException::New(env, offset + length).Throw(env);
}

void
TARGET::getRegister(jnixx::env env, jlong unwCursor,
		    Number num, jlong offset, jint length,
		    jnixx::jbyteArray jbytes, jint start) {
  int regNum = num.intValue(env);
  logf(FINE, "getRegister %d from %lx, offset %ld length %d start %d",
       regNum, (long)UNW_CURSOR, (long) offset, (int)length, (int)start);
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
  verifyBounds(env, offset, length, jbytes, start, size);
  if (unw_is_fpreg(regNum)) {
    status = unw_get_fpreg(UNW_CURSOR,
			   (::unw_regnum_t) regNum,
			   &word.fp);
  } else {
    status = unw_get_reg(UNW_CURSOR,
			 (::unw_regnum_t) regNum,
			 &word.w);
    logf(FINE, "getRegister status %d %lx", status, (long)word.w);
  }
  if (status != 0)
    RuntimeException::ThrowNew(env, "get register failed");
  jbyteArrayElements bytes = jbyteArrayElements(env, jbytes);
  memcpy(bytes.elements() + start, (uint8_t*)&word + offset, length);
}

void
TARGET::setRegister(jnixx::env env, jlong unwCursor,
		    Number num,
		    jlong offset, jint length,
		    jnixx::jbyteArray jbytes, jint start) {
  int regNum = num.intValue(env);
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
  verifyBounds(env, offset, length, jbytes, start, size);
  if (unw_is_fpreg(regNum))
    status = unw_get_fpreg(UNW_CURSOR, (::unw_regnum_t) regNum, &word.fp);
  else
    status = unw_get_reg(UNW_CURSOR, (::unw_regnum_t) regNum, &word.w);
  if (status != 0)
    RuntimeException::ThrowNew(env, "set register failed");
  jbyteArrayElements bytes = jbyteArrayElements(env, jbytes);
  memcpy((uint8_t*)&word + offset, bytes.elements() + start, length);
  bytes.release();
  if (unw_is_fpreg(regNum))
    status = unw_set_fpreg(UNW_CURSOR, regNum, word.fp);
  else
    status = unw_set_reg(UNW_CURSOR, regNum, word.w);
  if (status != 0)
    RuntimeException::ThrowNew(env, "set register failed");
}

jlong
TARGET::getSP(jnixx::env env, jlong unwCursor) {
  unw_word_t sp;
  int status = unw_get_reg(UNW_CURSOR, UNW_REG_SP, &sp);
  if (status < 0)
    return 0; // bottom of stack.
  else
    return sp;
}

jlong
TARGET::getIP(jnixx::env env, jlong unwCursor) {
  unw_word_t ip;
  int status = unw_get_reg(UNW_CURSOR, UNW_REG_IP, &ip);
  if (status < 0)
    return 0; // bottom of stack.
  else
    return ip;
}

jlong
TARGET::getCFA(jnixx::env env, jlong unwCursor) {
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
  memcpy(&copy, UNW_CURSOR, sizeof (copy));
  if (unw_step(&copy) < 0)
    return 0;
  unwCursor = (jlong) &copy;
#endif
#endif

  unw_word_t cfa;
  int status = unw_get_reg(UNW_CURSOR, FRYSK_UNW_REG_CFA, &cfa);
  if (status < 0)
    return 0; // bottom of stack.
  else
    return cfa;
}


jint
TARGET::getContext(jnixx::env env, jlong context) {
  return (jint) unw_getcontext((::unw_context_t *) context);
}

jlong
TARGET::copyCursor(jnixx::env env, jlong unwCursor) {
  ::unw_cursor_t *nativeCursor
    = (::unw_cursor_t*) ::malloc(sizeof(::unw_cursor_t));
  // Create a local copy of the unwind cursor
  ::memcpy(nativeCursor, UNW_CURSOR, sizeof(*UNW_CURSOR));
  logf(FINE, "copyCursor %lx to %lx", (long) UNW_CURSOR,
       (long) nativeCursor);
  return (jlong) nativeCursor;
}

jlong
TARGET::getProcInfo(jnixx::env env, jlong unwCursor) {
  logf(FINE, "getProcInfo cursor: %lx", (long) UNW_CURSOR);
  unw_proc_info_t *procInfo
    = (::unw_proc_info_t *) ::malloc(sizeof (::unw_proc_info_t));
  int ret = unw_get_proc_info(UNW_CURSOR, procInfo);

  logf(FINE, "getProcInfo finished get_proc_info %lx", (long) procInfo);
  if (ret < 0) {
    ::free(procInfo);
    return 0;
  } else {
    return (jlong)procInfo;
  }
}

void
TARGET::destroyProcInfo(jnixx::env env, jlong unwProcInfo) {
  ::free(UNW_PROC_INFO);
}

// Return NULL when eh_frame_hdr cannot be found.
// Also fills in ip->start_ip, ip->end_ip and ip->gp.
// peh_vaddr will point to the address of the eh_frame_hdr in the main
// address space of the inferior.
static char *
get_eh_frame_hdr_addr(jnixx::env env, unw_proc_info_t *pi,
		      char *image, size_t size,
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
  for (int i = 0; i < ehdr.e_phnum; i++) {
      if (gelf_getphdr (elf, i, &phdr) == NULL)
	return NULL;
      
      switch (phdr.p_type) {
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
  if (elf_getshstrndx (elf, &shstrndx) >= 0) {
      Elf_Scn *scn = NULL;
      while ((scn = elf_nextscn (elf, scn)) != NULL
	     && debug_frame_data == NULL) {
	  GElf_Shdr shdr;
	  if (gelf_getshdr (scn, &shdr) != NULL
	      && shdr.sh_type == SHT_PROGBITS) {
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

  if (pdyn_ndx != -1) {
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
	   i < pdyn_shdr.sh_size / pdyn_shdr.sh_entsize; i++) {
	  GElf_Dyn dyn;
	  if (gelf_getdyn (pdyn_data, i, &dyn) == NULL)
	    return NULL;

	  if (dyn.d_tag == DT_PLTGOT) {
	      /* Assume that _DYNAMIC is writable and GLIBC has
		 relocated it (true for x86 at least). */
	      pi->gp = dyn.d_un.d_ptr;
	      break;
	    }
	}
  } else {
    /* Otherwise this is a static executable with no _DYNAMIC.  Assume
       that data-relative addresses are relative to 0, i.e.,
       absolute. */
    pi->gp = 0;
  }

  pi->start_ip = segbase;
  pi->end_ip = segbase + ptxt.p_memsz;

  *peh_vaddr = peh_hdr.p_vaddr;

  char *hdr;
  if (debug_frame_data != NULL && debug_frame_data->d_buf != NULL
      && debug_frame_data->d_size != 0) {
      pi->format = UNW_INFO_FORMAT_TABLE;
      pi->unwind_info_size = debug_frame_data->d_size / sizeof (unw_word_t);
      hdr = (char *) debug_frame_data->d_buf;
    } else {
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
#define IMAGE_MAGIC 0xfeed
struct image {
  int magic;
  void *bytes;
  size_t size;
  char *table;
  image(void* bytes, size_t size) {
    this->magic = IMAGE_MAGIC;
    this->bytes = bytes;
    this->size = size;
  }
};

static int 
image_access_mem(unw_addr_space_t as, unw_word_t addr,
		 unw_word_t *val, int write, void *arg) {
  struct image *image = (struct image*) arg;
  if (image->magic != IMAGE_MAGIC) {
    fprintf(stderr, "%s: bad magic number\n", __func__);
    return -UNW_EINVAL;
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
    fprintf(stderr, "%s: bad magic number\n", __func__);
    return;
  }
  munmap(image->bytes, image->size);
  ::free(image);
}

static unw_accessors_t image_accessors = {
  NULL, image_put_unwind_info, NULL, image_access_mem, NULL, NULL, NULL, NULL
};

static jint
fillProcInfoFromImage(jnixx::env env, frysk::rsl::Log fine,
		      const char* name,
		      unw_proc_info_t* unwProcInfo,
		      jlong ip,
		      jboolean needUnwindInfo,
		      void *bytes,
		      long size,
		      long segbase) {
  logf(env, fine, "fillProcInfoFromImage"
       " %s unwProcInfo %lx, ip %lx, bytes %p, size %ld, segBase %lx",
       name, (long) UNW_PROC_INFO, (long)ip, bytes, size, segbase);
  
  unw_word_t peh_vaddr = 0;
  char *eh_table_hdr = get_eh_frame_hdr_addr(env, UNW_PROC_INFO,
					     (char *) bytes,
					     size, segbase,
					     &peh_vaddr);
  if (eh_table_hdr == NULL) {
    logf(env, fine, "get_eh_frame_hdr failed");
    munmap(bytes, size);
    return -UNW_ENOINFO;
  }

  struct image *image = new struct image(bytes, size);

  int ret;
  if (UNW_PROC_INFO->format == UNW_INFO_FORMAT_REMOTE_TABLE) {
    // address adjustment
    image->table = eh_table_hdr - peh_vaddr;
    ret = unw_get_unwind_table((unw_word_t) ip,
			       UNW_PROC_INFO,
			       (int) needUnwindInfo,
			       &image_accessors,
			       // virtual address
			       peh_vaddr,
			       image);
  } else {
    image->table = eh_table_hdr;
    ret = unw_get_unwind_table((unw_word_t) ip,
                               UNW_PROC_INFO,
                               (int) needUnwindInfo,
                               &image_accessors,
                               // virtual address
                               0,
                               // address adjustment
                               image);
  }
  logf(env, fine, "Post unw_get_unwind_table %d", ret);
  return ret;
}

jint
TARGET::fillProcInfoFromVDSO(jnixx::env env, jlong unwProcInfo, jlong ip,
			     bool needUnwindInfo,
			     AddressSpace addressSpace,
			     jlong lowAddress, jlong highAddress,
			     jlong offset) {
  logf(FINE, "fillProcInfoFromVDSO"
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
  logf(FINE, "checked size, 0x%lx", (unsigned long) size);

  logf(FINE, "checking access_mem");
  unw_addr_space_t as = (unw_addr_space_t) addressSpace.GetUnwAddressSpace(env);
  a = unw_get_accessors (as);
  if (! a->access_mem)
    return -UNW_ENOINFO;

  // Try to decide whether it's an ELF image before bringing it all in
  // from target memory.
  logf(FINE, "checking magic");
  if (size <= EI_CLASS || size <= sizeof (magic))
    return -UNW_ENOINFO;
  if (sizeof (magic) >= SELFMAG) {
    // Read the VDSO from the target.
    int ret = (*a->access_mem) (as, (unw_word_t) segbase, &magic,
				0, (void *) addressSpace._object);
    if (ret < 0) {
      logf(FINE, "error accessing VDSO %d", ret);
      return ret;
    }
    if (memcmp (&magic, ELFMAG, SELFMAG) != 0) {
      logf(FINE, "VDSO has bad magic");
      return -UNW_ENOINFO;
    }
  }

  logf(FINE, "mapping memory for image (using mmap, so can umaped)");
  image = mmap (0, size, PROT_READ | PROT_WRITE,
                MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
  if (image == MAP_FAILED)
    return -UNW_ENOINFO;

  logf(FINE, "checked magic");
  if (sizeof (magic) >= SELFMAG) {
    *(unw_word_t *)image = magic;
    hi = sizeof (magic);
  } else {
    hi = 0;
  }

  logf(FINE, "reading in VDSO");
  for (; hi < size; hi += sizeof (unw_word_t)) {
    logf(env, GetFinest(env),
	 "Reading memory segbase: 0x%lx, image: %p, hi: 0x%lx, at: 0x%lx to location: %p",
	 segbase , image , hi, segbase+hi, (char *) image + hi);
    int ret = (*a->access_mem) (as, segbase + hi,
				(unw_word_t *) ((char *) image + hi),
				0, (void *) addressSpace._object);
    if (ret < 0) {
      logf(FINE, "error reading vdso");
      munmap (image, size);
      return ret;
    }
  }

  return fillProcInfoFromImage(FINE, "[vdso]", UNW_PROC_INFO, ip,
			       needUnwindInfo, image, size, segbase);
}

jint
TARGET::fillProcInfoFromElfImage(jnixx::env env, jlong unwProcInfo, jlong ip,
				 bool needUnwindInfo,
				 AddressSpace addressSpace,
				 String elfImageName,
				 jlong segbase, jlong hi,
				 jlong mapoff) {
  logf(FINE, "fillProcInfoFromElfImage");
  struct stat stat;
  void *image;		/* pointer to mmap'd image */
  size_t size;		/* (file-) size of the image */
  jstringUTFChars name = jstringUTFChars(env, elfImageName);

  logf(FINE, "opening %s", name.elements());
  int fd = ::open(name.elements(), O_RDONLY);
  if (fd < 0) {
    int err = errno;
    logf(FINE, "open failed: %s", strerror(err));
    return -UNW_ENOINFO;
  }

  logf(FINE, "stat-ing %d", fd);
  int ret = ::fstat(fd, &stat);
  if (ret < 0) {
    int err = errno;
    ::close(fd);
    logf(FINE, "fstat failed: %s", strerror(err));
    return -UNW_ENOINFO;
  }

  size = stat.st_size;
  logf(FINE, "mmaping %d, size %ld", fd, (long) size);
  image = ::mmap(NULL, size, PROT_READ, MAP_PRIVATE, fd, 0);
  if (image == MAP_FAILED) {
    int err = errno;
    ::close(fd);
    logf(FINE, "mmap failed: %s", strerror(err));
    return -UNW_ENOINFO;
  }
  ::close(fd);
  
  return fillProcInfoFromImage(FINE, name.elements(), UNW_PROC_INFO, ip,
			       needUnwindInfo, image, size, segbase);
}

jint
TARGET::fillProcInfoNotAvailable(jnixx::env env, jlong unwProcInfo) {
  return -UNW_ENOINFO;
}

jlong
TARGET::getStartIP(jnixx::env env, jlong unwProcInfo) {
  return (jlong) (UNW_PROC_INFO)->start_ip;
}

jlong
TARGET::getEndIP(jnixx::env env, jlong unwProcInfo) {
  return (jlong) (UNW_PROC_INFO)->end_ip;
}

jlong
TARGET::getLSDA(jnixx::env env, jlong unwProcInfo) {
  return (jlong) (UNW_PROC_INFO)->lsda;
}

jlong
TARGET::getHandler(jnixx::env env, jlong unwProcInfo) {
  return (jlong) (UNW_PROC_INFO)->handler;
}

jlong
TARGET::getGP(jnixx::env env, jlong unwProcInfo) {
  return (jlong) (UNW_PROC_INFO)->gp;
}

jlong
TARGET::getFlags(jnixx::env env, jlong unwProcInfo) {
  return (jlong) (UNW_PROC_INFO)->flags;
}

jint
TARGET::getFormat(jnixx::env env, jlong unwProcInfo) {
  return (jint) (UNW_PROC_INFO)->format;
}

jint
TARGET::getUnwindInfoSize(jnixx::env env, jlong unwProcInfo) {
  return (jint) (UNW_PROC_INFO)->unwind_info_size;
}

jlong
TARGET::getUnwindInfo(jnixx::env env, jlong unwProcInfo) {
  return (jlong) (UNW_PROC_INFO)->unwind_info;
}
