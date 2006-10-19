// This file is part of the program FRYSK.
//
// Copyright 2005, IBM Inc.
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

#include "lib/elf/ElfPrpsinfo.h"

#ifdef __cplusplus
extern "C"
{
#endif

//XXX We're not sure how to deal with bi-arch. For one 32-bit application
//    on 64-bit machine, which size(uint32_t and uint16_t) should we use for
//    pr_uid/pr_gid? If uint16_t should be choosen, should we define nother 
//    "struct elf_prpsinfo"?
 
typedef struct elf_prpsinfo {   /* Information about process                 */
  unsigned char  pr_state;      /* Numeric process state                     */
  char           pr_sname;      /* Char for pr_state                         */
  unsigned char  pr_zomb;       /* Zombie                                    */
  signed char    pr_nice;       /* Nice val                                  */
  unsigned long  pr_flag;       /* Flags                                     */
#ifdef __x86_64__
  uint32_t       pr_uid;        /* User ID                                   */
  uint32_t       pr_gid;        /* Group ID                                  */
#else 
# if defined __i386__
  uint16_t       pr_uid;        /* User ID                                   */
  uint16_t       pr_gid;        /* Group ID                                  */
# else 
# if defined __powerpc__ || defined __powerpc64__
  uint32_t       pr_uid;        /* User ID */
  uint32_t       pr_gid;        /* Group ID */
# endif
# endif
#endif

  pid_t          pr_pid;        /* Process ID                                */
  pid_t          pr_ppid;       /* Parent's process ID                       */
  pid_t          pr_pgrp;       /* Group ID                                  */
  pid_t          pr_sid;        /* Session ID                                */
  char           pr_fname[16];  /* Filename of executable                    */
  char           pr_psargs[80]; /* Initial part of arg list                  */
} elf_prpsinfo;

jlong
lib::elf::ElfPrpsinfo::getEntrySize()
{
	int entrySize = sizeof(elf_prpsinfo);
	
	return (jlong)entrySize;
}

jlong 
lib::elf::ElfPrpsinfo::fillMemRegion(jbyteArray buffer, jlong startAddress)
{
	int size = 0;
	
	jbyte *bs = elements(buffer);
	struct elf_prpsinfo *prpsinfo = NULL;
	
	prpsinfo = (struct elf_prpsinfo *)malloc(sizeof(struct elf_prpsinfo));
	
	memset(prpsinfo, 0, sizeof(struct elf_prpsinfo));
	
	prpsinfo->pr_state = this->pr_state;
	prpsinfo->pr_sname = this->pr_sname;
	prpsinfo->pr_zomb = this->pr_zomb;
	prpsinfo->pr_nice = this->pr_nice;
	prpsinfo->pr_flag = this->pr_flag;
	
	prpsinfo->pr_uid = this->pr_uid;
	prpsinfo->pr_gid = this->pr_gid;

	prpsinfo->pr_pid = this->pr_pid;
	prpsinfo->pr_ppid = this->pr_ppid;
	prpsinfo->pr_pgrp = this->pr_pgrp;
	
	prpsinfo->pr_sid = this->pr_sid;
	
	jchar *fname = elements(this->pr_fname);		
	jchar *args = elements(this->pr_psargs);
	
	size = JvGetArrayLength(this->pr_fname);
	memcpy(prpsinfo->pr_fname, fname, size);
	prpsinfo->pr_fname[size] = '\0';
	
	size = JvGetArrayLength(this->pr_psargs);
	memcpy(prpsinfo->pr_psargs, args, size);
	prpsinfo->pr_psargs[size] = '\0';
	
	
	memcpy(bs + startAddress, prpsinfo, sizeof(struct elf_prpsinfo));
	
	return sizeof(prpsinfo);
}

#ifdef __cplusplus
}
#endif
