// This file is part of the program FRYSK.
//
// Copyright 2006, 2007, 2008 Red Hat, Inc.
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

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <stdint.h>

#include <asm/types.h>
#ifdef __x86_64__
typedef __u64 u64;
typedef __u32 u32;
#endif

#include "elf.h"
#include "libelf.h"
#include "gelf.h"
#include "sys/user.h"

#include "jni.hxx"

#include "jnixx/elements.hxx"

using namespace std;
using namespace java::lang;
using namespace java::util;

struct elf_siginfo
{
  int	si_signo;			/* signal number */
  int	si_code;			/* extra code */
  int	si_errno;			/* errno */
};


// To deal with 32 on 64 bit scenarios, have to define timevals as
// they would appear on thier native systems.
struct timeval64
{
  int64_t tv_sec;            /* Seconds.  */
  int64_t tv_usec;      /* Microseconds.  */
};


struct timeval32
{
  int32_t tv_sec;            /* Seconds.  */
  int32_t tv_usec;      /* Microseconds.  */
};


// To deal with 32 on 64 scenarios, have to alter the storage
// allocation for register so they appear in the structure on thier
// native systems. In the system includes elf_greg_t is unsigned long.
typedef uint32_t elf_greg_t_32;
typedef uint64_t elf_greg_t_64;

// Allocate space regarding true native sizes.
#define ELF_NGREG32 (sizeof (struct user_regs_struct) / sizeof(elf_greg_t_32))
#define ELF_NGREG64 (sizeof (struct user_regs_struct) /	\
		     sizeof(elf_greg_t_64))

// Define the register space
#if defined __powerpc__
typedef elf_greg_t_32 elf_gregset_t_32[96];
typedef elf_greg_t_64 elf_gregset_t_64[280];
#else
typedef elf_greg_t_32 elf_gregset_t_32[ELF_NGREG32];
typedef elf_greg_t_64 elf_gregset_t_64[ELF_NGREG64];
#endif

/*
 * Definitions to generate Intel SVR4-like core files.
 * These mostly have the same names as the SVR4 types with "elf_"
 * tacked on the front to prevent clashes with linux definitions,
 * and the typedef forms have been avoided.  This is mostly like
 * the SVR4 structure, but more Linuxy, with things that Linux does
 * not support and which gdb doesn't really use excluded.
 * Fields present but not used are marked with "XXX".
 */

// For 32 on 64 sceanrios, longs are not sufficient, as they will be
// wrongly sized. Replaces instanced of signed and unsigned longs with
// unint32_t and int32_t
struct elf_prstatus32
{
  struct elf_siginfo pr_info;	/* Info associated with signal */
  short	pr_cursig;		/* Current signal */
  uint32_t pr_sigpend;	/* Set of pending signals */
  uint32_t pr_sighold;	/* Set of held signals */
  pid_t	pr_pid;
  pid_t	pr_ppid;
  pid_t	pr_pgrp;
  pid_t	pr_sid;
  struct timeval32 pr_utime;	/* User time */
  struct timeval32 pr_stime;	/* System time */
  struct timeval32 pr_cutime;	/* Cumulative user time */
  struct timeval32 pr_cstime;	/* Cumulative system time */
  elf_gregset_t_32 pr_reg;	/* GP registers */
#ifdef CONFIG_BINFMT_ELF_FDPIC
  /* When using FDPIC, the loadmap addresses need to be communicated
   * to GDB in order for GDB to do the necessary relocations.  The
   * fields (below) used to communicate this information are placed
   * immediately after ``pr_reg'', so that the loadmap addresses may
   * be viewed as part of the register set if so desired.
   */
  uint32_t pr_exec_fdpic_loadmap;
  uint32_t pr_interp_fdpic_loadmap;
#endif
  int pr_fpvalid;		/* True if math co-processor being used.  */
};

// For 32 on 64 sceanrios, longs are not sufficient, as they will be
// wrongly sized. Replaces instanced of signed and unsigned longs with
// unint64_t and int64_t
struct elf_prstatus64
{
  struct elf_siginfo pr_info;	/* Info associated with signal */
  short	pr_cursig;		/* Current signal */
  uint64_t pr_sigpend;	/* Set of pending signals */
  uint64_t pr_sighold;	/* Set of held signals */
  pid_t	pr_pid;
  pid_t	pr_ppid;
  pid_t	pr_pgrp;
  pid_t	pr_sid;
  struct timeval64 pr_utime;	/* User time */
  struct timeval64 pr_stime;	/* System time */
  struct timeval64 pr_cutime;	/* Cumulative user time */
  struct timeval64 pr_cstime;	/* Cumulative system time */
  elf_gregset_t_64 pr_reg;	/* GP registers */
#ifdef CONFIG_BINFMT_ELF_FDPIC
  /* When using FDPIC, the loadmap addresses need to be communicated
   * to GDB in order for GDB to do the necessary relocations.  The
   * fields (below) used to communicate this information are placed
   * immediately after ``pr_reg'', so that the loadmap addresses may
   * be viewed as part of the register set if so desired.
   */
  uint64_t pr_exec_fdpic_loadmap;
  uint64_t pr_interp_fdpic_loadmap;
#endif
  int pr_fpvalid;		/* True if math co-processor being used.  */
};


jlong
lib::dwfl::ElfPrstatus::getEntrySize(jnixx::env env) {
  if (GetSize(env) == 32)
    return sizeof(struct elf_prstatus32);
  else
    return sizeof(struct elf_prstatus64);
}


jlong 
lib::dwfl::ElfPrstatus::fillMemRegion(jnixx::env env,
				      jnixx::jbyteArray jbuffer,
				      jlong startAddress) {
  jbyteArrayElements buffer = jbyteArrayElements(env, jbuffer);
  jbyte *bs = buffer.elements();
  if (GetSize(env) == 32) {
    struct elf_prstatus32 *prstatus = NULL;

    prstatus = (struct elf_prstatus32 *)alloca(sizeof(struct elf_prstatus32));

    memset(prstatus, 0, sizeof(struct elf_prstatus32));

    // Current and pending signals are only known to the kernel it seems?

    prstatus->pr_info.si_signo = 0;
    prstatus->pr_info.si_code = 0;
    prstatus->pr_info.si_errno = 0;
	    
    prstatus->pr_cursig = 0;
    prstatus->pr_sigpend = this->GetPr_sigpend(env);
    prstatus->pr_sighold = 0;

    prstatus->pr_pid = this->GetPr_pid(env);
    prstatus->pr_ppid = this->GetPr_ppid(env);
    prstatus->pr_pgrp = this->GetPr_pgrp(env);
    prstatus->pr_sid = this->GetPr_sid(env);
	    
    jnixx::jbyteArray jraw_regs = this->GetRaw_core_registers(env);
    jbyteArrayElements raw_regs = jbyteArrayElements(env, jraw_regs);
    memcpy(((unsigned char *)prstatus->pr_reg),
	   raw_regs.elements(),
	   raw_regs.length());
    /* True if math co-processor being used.  */
    prstatus->pr_fpvalid = 1;
	    
    memcpy(bs + startAddress, prstatus, sizeof(struct elf_prstatus32));

    return sizeof(struct elf_prstatus32);
  } else {
    struct elf_prstatus64 *prstatus = NULL;

    prstatus = (struct elf_prstatus64 *)alloca(sizeof(struct elf_prstatus64));

    memset(prstatus, 0, sizeof(struct elf_prstatus64));

    // Current and pending signals are only known to the kernel it seems?

    prstatus->pr_info.si_signo = 0;
    prstatus->pr_info.si_code = 0;
    prstatus->pr_info.si_errno = 0;
	    
    prstatus->pr_cursig = 0;
    prstatus->pr_sigpend = this->GetPr_sigpend(env);
    prstatus->pr_sighold = 0;

    prstatus->pr_pid = this->GetPr_pid(env);
    prstatus->pr_ppid = this->GetPr_ppid(env);
    prstatus->pr_pgrp = this->GetPr_pgrp(env);
    prstatus->pr_sid = this->GetPr_sid(env);
	    
    jnixx::jbyteArray jraw_regs = this->GetRaw_core_registers(env);
    jbyteArrayElements raw_regs = jbyteArrayElements(env, jraw_regs);
    memcpy(((unsigned char *)prstatus->pr_reg),
	   raw_regs.elements(),
	   raw_regs.length());

    /* True if math co-processor being used.  */
    prstatus->pr_fpvalid = 1;
	    
    memcpy(bs + startAddress, prstatus, sizeof(struct elf_prstatus64));

    return sizeof(struct elf_prstatus64);
  }
}


jlong
lib::dwfl::ElfPrstatus::getNoteData(jnixx::env env, ElfData data) {
  void *elf_data = ((Elf_Data*)data.getPointer(env))->d_buf;
  GElf_Nhdr *nhdr = (GElf_Nhdr *)elf_data;
  long note_loc =0;
  long note_data_loc = 0;


  // Can have more that on Prstatus note per core file. Collect all of them.
  while (note_loc <= data.getSize(env)) {
    // Find Prstatus note data. If the first note header is not prstatus
    // loop through, adding up header + align + data till we find the
    // next header. Continue until section end to find correct header.
      
    while ((nhdr->n_type != NT_PRSTATUS) && (note_loc <= data.getSize(env))) {
      note_loc += (sizeof (GElf_Nhdr) + ((nhdr->n_namesz + 0x03) & ~0x3)) + nhdr->n_descsz;
      if (note_loc >= data.getSize(env))
	break;
      nhdr = (GElf_Nhdr *) (((unsigned char *)elf_data) + note_loc);
    }
      
    // If loop through entire note section, and header not found, return
    // here with abnormal return code.
    if (nhdr->n_type != NT_PRSTATUS)
      return -1;
      
    // Find data at current header + alignment
    note_data_loc = (note_loc + sizeof(GElf_Nhdr) + ((nhdr->n_namesz +  0x03) & ~0x3));
      
    jnixx::jbyteArray jbuf = jnixx::jbyteArray::NewByteArray(env, nhdr->n_descsz);
    jbyteArrayElements buf = jbyteArrayElements(env, jbuf);
    memcpy(buf.elements(), ((unsigned char  *)elf_data)+note_data_loc,
	   nhdr->n_descsz);
      
    GetInternalThreads(env).add(env, jbuf);

    // Move pointer along, now we have processed the first thread
    note_loc += (sizeof (GElf_Nhdr) + ((nhdr->n_namesz + 0x03) & ~0x3)) + nhdr->n_descsz;
    nhdr = (GElf_Nhdr *) (((unsigned char *)elf_data) + note_loc);
  }
  
  return 0;
}
