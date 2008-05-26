// This file is part of the program FRYSK.
//
// Copyright 2005, 2007, IBM Inc.
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

#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <stdint.h>
#include <gelf.h>
#include <errno.h>
#include <string.h>

#include "jni.hxx"

#include "jnixx/elements.hxx"

using namespace java::lang;


typedef struct elf_prpsinfo32 {   /* Information about process                 */
  unsigned char  pr_state;      /* Numeric process state                     */
  char           pr_sname;      /* Char for pr_state                         */
  unsigned char  pr_zomb;       /* Zombie                                    */
  signed char    pr_nice;       /* Nice val                                  */
  uint32_t       pr_flag;       /* Flags                                     */
# if defined __powerpc__ || defined __powerpc64__
  uint32_t       pr_uid;        /* User ID */
  uint32_t       pr_gid;        /* Group ID */
# else
  uint16_t       pr_uid;        /* User ID                                   */
  uint16_t       pr_gid;        /* Group ID                                  */
# endif
  pid_t          pr_pid;        /* Process ID                                */
  pid_t          pr_ppid;       /* Parent's process ID                       */
  pid_t          pr_pgrp;       /* Group ID                                  */
  pid_t          pr_sid;        /* Session ID                                */
  char           pr_fname[16];  /* Filename of executable                    */
  char           pr_psargs[80]; /* Initial part of arg list                  */
} elf_prpsinfo32;
 
typedef struct elf_prpsinfo64 {   /* Information about process                 */
  unsigned char  pr_state;      /* Numeric process state                     */
  char           pr_sname;      /* Char for pr_state                         */
  unsigned char  pr_zomb;       /* Zombie                                    */
  signed char    pr_nice;       /* Nice val                                  */
  unsigned long  pr_flag;       /* Flags                                     */
# if defined __powerpc__ || defined __powerpc64__
  uint32_t       pr_uid;        /* User ID */
  uint32_t       pr_gid;        /* Group ID */
# else
  uint32_t       pr_uid;        /* User ID                                   */
  uint32_t       pr_gid;        /* Group ID                                  */
# endif

  pid_t          pr_pid;        /* Process ID                                */
  pid_t          pr_ppid;       /* Parent's process ID                       */
  pid_t          pr_pgrp;       /* Group ID                                  */
  pid_t          pr_sid;        /* Session ID                                */
  char           pr_fname[16];  /* Filename of executable                    */
  char           pr_psargs[80]; /* Initial part of arg list                  */
} elf_prpsinfo64;

jlong
lib::dwfl::ElfPrpsinfo::getEntrySize(jnixx::env env) {
  if (GetSize(env) == 32)
    return sizeof(struct elf_prpsinfo32);
  else
    return sizeof(struct elf_prpsinfo64);
}

jlong 
lib::dwfl::ElfPrpsinfo::fillMemRegion(jnixx::env env,
				      jnixx::jbyteArray jbuffer,
				      jlong startAddress) {
  jbyteArrayElements buffer = jbyteArrayElements(env, jbuffer);
  jbyte *bs = buffer.elements();
  if (GetSize(env) == 32) {
    struct elf_prpsinfo32 *prpsinfo = NULL;
    prpsinfo = (struct elf_prpsinfo32 *)alloca(sizeof(struct elf_prpsinfo32));
    memset(prpsinfo, 0, sizeof(struct elf_prpsinfo32));

    prpsinfo->pr_state = this->GetPr_state(env);
    prpsinfo->pr_sname = this->GetPr_sname(env);
    prpsinfo->pr_zomb = this->GetPr_zomb(env);
    prpsinfo->pr_nice = this->GetPr_nice(env);
    prpsinfo->pr_flag = this->GetPr_flag(env);
	
    prpsinfo->pr_uid = this->GetPr_uid(env);
    prpsinfo->pr_gid = this->GetPr_gid(env);
	    
    prpsinfo->pr_pid = this->GetPr_pid(env);
    prpsinfo->pr_ppid = this->GetPr_ppid(env);
    prpsinfo->pr_pgrp = this->GetPr_pgrp(env);
	    
    prpsinfo->pr_sid = this->GetPr_sid(env);
    
    String jpr_fname = this->GetPr_fname(env);
    jstringUTFChars pr_fname = jstringUTFChars(env, jpr_fname);
    int name_length = pr_fname.length();
    if (name_length > 15)
      name_length = 15;
    ::memcpy(prpsinfo->pr_fname, pr_fname.elements(), name_length);
    prpsinfo->pr_fname[name_length] = '\0';

    String jpr_psargs = this->GetPr_psargs(env);
    jstringUTFChars pr_psargs = jstringUTFChars(env, jpr_psargs);
    int args_length = pr_psargs.length();
    if (args_length > 79)
      args_length = 79;
    ::memcpy(prpsinfo->pr_psargs, pr_psargs.elements(), args_length);
    prpsinfo->pr_psargs[args_length] = '\0';

    memcpy(bs + startAddress, prpsinfo, sizeof(struct elf_prpsinfo32));
    return sizeof(struct elf_prpsinfo32);
  } else {
    struct elf_prpsinfo64 *prpsinfo = NULL;
    prpsinfo = (struct elf_prpsinfo64 *)alloca(sizeof(struct  elf_prpsinfo64));
    memset(prpsinfo, 0, sizeof(struct elf_prpsinfo64));

    prpsinfo->pr_state = this->GetPr_state(env);
    prpsinfo->pr_sname = this->GetPr_sname(env);
    prpsinfo->pr_zomb = this->GetPr_zomb(env);
    prpsinfo->pr_nice = this->GetPr_nice(env);
    prpsinfo->pr_flag = this->GetPr_flag(env);
	
    prpsinfo->pr_uid = this->GetPr_uid(env);
    prpsinfo->pr_gid = this->GetPr_gid(env);
	    
    prpsinfo->pr_pid = this->GetPr_pid(env);
    prpsinfo->pr_ppid = this->GetPr_ppid(env);
    prpsinfo->pr_pgrp = this->GetPr_pgrp(env);
	    
    prpsinfo->pr_sid = this->GetPr_sid(env);

    String jpr_fname = this->GetPr_fname(env);
    jstringUTFChars pr_fname = jstringUTFChars(env, jpr_fname);
    int name_length = pr_fname.length();
    if (name_length > 15)
      name_length = 15;
    ::memcpy(prpsinfo->pr_fname, pr_fname.elements(), name_length);
    prpsinfo->pr_fname[name_length] = '\0';

    String jpr_psargs = this->GetPr_psargs(env);
    jstringUTFChars pr_psargs = jstringUTFChars(env, jpr_psargs);
    int args_length = pr_psargs.length();
    if (args_length > 79)
      args_length = 79;
    ::memcpy(prpsinfo->pr_psargs, pr_psargs.elements(), args_length);
    prpsinfo->pr_psargs[args_length] = '\0';

    memcpy(bs + startAddress, prpsinfo, sizeof(struct elf_prpsinfo64));
    return sizeof(struct elf_prpsinfo64);

  }
}


jnixx::jbyteArray
lib::dwfl::ElfPrpsinfo::getNoteData(jnixx::env env, ElfData data) {
  void *elf_data = ((Elf_Data*)data.getPointer(env))->d_buf;
  GElf_Nhdr *nhdr = (GElf_Nhdr *)elf_data;
  long note_loc =0;
  long note_data_loc = 0;

  // Find Prpsinfo note data. If the first note header is not prpsinfo
  // loop through, adding up header + align + data till we find the
  // next header. Continue until section end to find correct header.
  while ((nhdr->n_type != NT_PRPSINFO) && (note_loc <= data.getSize(env))) {
    note_loc += (sizeof (GElf_Nhdr) + ((nhdr->n_namesz + 0x03) & ~0x3)) + nhdr->n_descsz;
    if (note_loc >= data.getSize(env))
      break;
    nhdr = (GElf_Nhdr *) (((unsigned char *)elf_data) + note_loc);
  }

  // If loop through entire note section, and header not found, return
  // here with abnormal return code.
  if (nhdr->n_type != NT_PRPSINFO)
    return jnixx::jbyteArray(env, NULL);

  // Find data at current header + alignment
  note_data_loc = (note_loc + sizeof(GElf_Nhdr) + ((nhdr->n_namesz +  0x03) & ~0x3));

  jnixx::jbyteArray jbuf = jnixx::jbyteArray::NewByteArray(env, nhdr->n_descsz);
  jbyteArrayElements buf = jbyteArrayElements(env, jbuf);
  ::memcpy(buf.elements(), ((unsigned char  *)elf_data)+note_data_loc,
	   nhdr->n_descsz);
  return jbuf;
}
