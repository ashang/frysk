// This file is part of the program FRYSK.
//
// Copyright 2006, Red Hat, Inc.
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

#include "lib/elf/ElfPrstatus.h"
#include "asm/elf.h"

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
	int entrySize = sizeof(elf_prstatus);
	
	return (jlong)entrySize;
}


jlong 
lib::elf::ElfPrstatus::fillMemRegion(jbyteArray buffer, jlong startAddress)
{

	jbyte *bs = elements(buffer);
	struct elf_prstatus *prstatus = NULL;

	
	prstatus = (struct elf_prstatus *)malloc(sizeof(struct elf_prstatus));

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
	
	return sizeof(prstatus);
}

#ifdef __cplusplus
}
#endif
