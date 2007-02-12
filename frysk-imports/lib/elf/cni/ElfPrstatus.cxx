// This file is part of the program FRYSK.
//
// Copyright 2006,2007 Red Hat, Inc.
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
#include <gcj/cni.h>
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

#include "asm/elf.h"
#include "lib/elf/ElfPrstatus.h"
#include "lib/elf/ElfData.h"
#include "lib/elf/ElfException.h"
#include <java/util/ArrayList.h>
#include <java/math/BigInteger.h>

using namespace std;
using namespace java::lang;
using namespace java::util;

#ifdef __cplusplus
extern "C"
{
#endif

struct elf_siginfo
{
	int	si_signo;			/* signal number */
	int	si_code;			/* extra code */
	int	si_errno;			/* errno */
};


//XXX We're not sure how to deal with bi-arch. For one 32-bit application
//    on 64-bit machine, which size(uint32_t and uint16_t) should we use for
//    pr_uid/pr_gid? If uint16_t should be choosen, should we define nother 
//    "struct elf_prpsinfo"?

/*
 * Definitions to generate Intel SVR4-like core files.
 * These mostly have the same names as the SVR4 types with "elf_"
 * tacked on the front to prevent clashes with linux definitions,
 * and the typedef forms have been avoided.  This is mostly like
 * the SVR4 structure, but more Linuxy, with things that Linux does
 * not support and which gdb doesn't really use excluded.
 * Fields present but not used are marked with "XXX".
 */
struct elf_prstatus
{
#if 0
	long	pr_flags;	/* XXX Process flags */
	short	pr_why;		/* XXX Reason for process halt */
	short	pr_what;	/* XXX More detailed reason */
#endif
	struct elf_siginfo pr_info;	/* Info associated with signal */
	short	pr_cursig;		/* Current signal */
	unsigned long pr_sigpend;	/* Set of pending signals */
	unsigned long pr_sighold;	/* Set of held signals */
#if 0
	struct sigaltstack pr_altstack;	/* Alternate stack info */
	struct sigaction pr_action;	/* Signal action for current sig */
#endif
	pid_t	pr_pid;
	pid_t	pr_ppid;
	pid_t	pr_pgrp;
	pid_t	pr_sid;
	struct timeval pr_utime;	/* User time */
	struct timeval pr_stime;	/* System time */
	struct timeval pr_cutime;	/* Cumulative user time */
	struct timeval pr_cstime;	/* Cumulative system time */
#if 0
	long	pr_instr;		/* Current instruction */
#endif
	elf_gregset_t pr_reg;	/* GP registers */
#ifdef CONFIG_BINFMT_ELF_FDPIC
	/* When using FDPIC, the loadmap addresses need to be communicated
	 * to GDB in order for GDB to do the necessary relocations.  The
	 * fields (below) used to communicate this information are placed
	 * immediately after ``pr_reg'', so that the loadmap addresses may
	 * be viewed as part of the register set if so desired.
	 */
	unsigned long pr_exec_fdpic_loadmap;
	unsigned long pr_interp_fdpic_loadmap;
#endif
	int pr_fpvalid;		/* True if math co-processor being used.  */
};


jlong
lib::elf::ElfPrstatus::getEntrySize()
{
	return sizeof(struct elf_prstatus);
}


jlong 
lib::elf::ElfPrstatus::fillMemRegion(jbyteArray buffer, jlong startAddress)
{

	jbyte *bs = elements(buffer);
	struct elf_prstatus *prstatus = NULL;

	prstatus = (struct elf_prstatus *)alloca(sizeof(struct elf_prstatus));

	memset(prstatus, 0, sizeof(struct elf_prstatus));

	// Current and pending signals are only known to the kernel it seems?

	prstatus->pr_info.si_signo = 0;
	prstatus->pr_info.si_code = 0;
	prstatus->pr_info.si_errno = 0;

	prstatus->pr_cursig = 0;
	prstatus->pr_sigpend = this->pr_sigpend;
	prstatus->pr_sighold = 0;

	prstatus->pr_pid = this->pr_pid;
	prstatus->pr_ppid = this->pr_ppid;
	prstatus->pr_pgrp = this->pr_pgrp;
	prstatus->pr_sid = this->pr_sid;

	this->convertToLong ();
	jlong *registers = elements(raw_registers);
	for(int i=0; i<this->reg_length; i++)
	  {
	    prstatus->pr_reg[i] = registers[i];
	  }

	prstatus->pr_fpvalid = 0;		/* True if math co-processor being used.  */

	memcpy(bs + startAddress, prstatus, sizeof(struct elf_prstatus));

	return sizeof(struct elf_prstatus);
}


extern ArrayList internalThreads;
jlong
lib::elf::ElfPrstatus::getNoteData(ElfData *data)
{
  void *elf_data = ((Elf_Data*)data->getPointer())->d_buf;
  GElf_Nhdr *nhdr = (GElf_Nhdr *)elf_data;
  elf_prstatus *prstatus;
  long note_loc =0;
  long note_data_loc = 0;


  // Can have more that on Prstatus note per core file. Collect all of them.
  while (note_loc <= data->getSize())
    {
      // Find Prstatus note data. If the first note header is not prstatus
      // loop through, adding up header + align + data till we find the
      // next header. Continue until section end to find correct header.
      
      while ((nhdr->n_type != NT_PRSTATUS) && (note_loc <= data->getSize()))
	{
	  note_loc += (sizeof (GElf_Nhdr) + ((nhdr->n_namesz + 0x03) & ~0x3)) + nhdr->n_descsz;
	  if (note_loc >= data->getSize())
	    break;
	  nhdr = (GElf_Nhdr *) (((unsigned char *)elf_data) + note_loc);
	}
      
      // If loop through entire note section, and header not found, return
      // here with abnormal return code.
      if (nhdr->n_type != NT_PRSTATUS)
	return -1;
      
      // Find data at current header + alignment
      note_data_loc = (note_loc + sizeof(GElf_Nhdr) + ((nhdr->n_namesz +  0x03) & ~0x3));
      
      // Run some sanity checks, as we will be doing void pointer -> cast math.
      if ((nhdr->n_descsz > sizeof(struct elf_prstatus)) || (nhdr->n_descsz > data->getSize()) 
	  || (nhdr->n_descsz > (data->getSize()-note_data_loc)))
	{
	  throw new lib::elf::ElfException(JvNewStringUTF("note size and elf_data size mismatch"));
	}
      
      // Point to the data, and cast.
      prstatus = (elf_prstatus *) (((unsigned char *) elf_data) + note_data_loc);
      
      // Fill Java class structures
      
      this->pr_info_si_signo = prstatus->pr_info.si_signo;
      this->pr_info_si_code =  prstatus->pr_info.si_code;
      this->pr_info_si_errno = prstatus->pr_info.si_errno;
      
      this->pr_cursig = prstatus->pr_cursig;
      this->pr_sigpend = prstatus->pr_sigpend;
      this->pr_sighold = prstatus->pr_sighold;
      
      this->pr_pid = prstatus->pr_pid;
      this->pr_ppid = prstatus->pr_ppid;
      this->pr_pgrp = prstatus->pr_pgrp;
      this->pr_sid = prstatus->pr_sid;
      this->pr_fpvalid = prstatus->pr_fpvalid;
      
      raw_core_registers = JvNewByteArray(sizeof (prstatus->pr_reg));
  
      memcpy(elements(raw_core_registers),prstatus->pr_reg,sizeof(prstatus->pr_reg));
      
      internalThreads->add(this);

      // Move pointer along, now we have processed the first thread
      note_loc += (sizeof (GElf_Nhdr) + ((nhdr->n_namesz + 0x03) & ~0x3)) + nhdr->n_descsz;
      nhdr = (GElf_Nhdr *) (((unsigned char *)elf_data) + note_loc);
    }
  
  return 0;
}
#ifdef __cplusplus
}
#endif
