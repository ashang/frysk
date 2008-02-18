// This file is part of the Utracer kernel module and it's userspace
// interfaces. 
//
// Copyright 2007, Red Hat Inc.
//
// Utracer is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// Utracer is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Utracer; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
//
// In addition, as a special exception, Red Hat, Inc. gives You the
// additional right to link the code of Utracer with code not covered
// under the GNU General Public License ("Non-GPL Code") and to
// distribute linked combinations including the two, subject to the
// limitations in this paragraph. Non-GPL Code permitted under this
// exception must only link to the code of Utracer through those well
// defined interfaces identified in the file named EXCEPTION found in
// the source code files (the "Approved Interfaces"). The files of
// Non-GPL Code may instantiate templates or use macros or inline
// functions from the Approved Interfaces without causing the
// resulting work to be covered by the GNU General Public
// License. Only Red Hat, Inc. may make changes or additions to the
// list of Approved Interfaces. You must obey the GNU General Public
// License in all respects for all of the Utracer code and other code
// used in conjunction with Utracer except the Non-GPL Code covered by
// this exception. If you modify this file, you may extend this
// exception to your version of the file, but you are not obligated to
// do so. If you do not wish to provide this exception without
// modification, you must delete this exception statement from your
// version and license this file solely under the GPL without
// exception.

#define _XOPEN_SOURCE 500
#define _GNU_SOURCE
#include <stdio.h>
#include <alloca.h>
#include <malloc.h>
#include <unistd.h>
#include <fcntl.h>
#include <search.h>
#include <string.h>
#include <ctype.h>
/*#include <asm/ptrace.h>*/
#include <utracer.h>
#include <asm/unistd.h>
#include <asm/user.h>
#include <asm/ldt.h>
#include <sys/types.h>
#include <regex.h>
#include <error.h>
#include <errno.h>

#include "udb.h"
#include "udb-i386.h"


const char re[] = "(\\[([[:digit:]]+)\\])?[[:space:]]*(([[:digit:]]+)|(st([[:digit:]]+))|([[:alpha:]]+))([[:space:]]*,[[:space:]]*(([-[:digit:]]+)|([[:alpha:]]+)|(all)))?";

  /*
   * 1  2            2  1             34            4 5  6            65 7            73
   *        pid              space              arg1                                 
   * (\[([[:digit:]]+)\])?[[:space:]]*(([[:digit:]]+)|(st([[:digit:]]+))|([[:alpha:]]+))
   *                                 1               1 1   1 1            1
   *      8                         90               0 1   1 2            298
   *                                    arg2
   *      ([[:space:]]*,[[:space:]]*(([\\-[:digit:]]+)|(all)|([[:alpha:]]+)))?
   *
   * [2] = pid
   * [4] = digit arg1
   * [5] = alpha arg1
   * [6] = st digit arg1
   * [7] != -1 ==> two args
   * [10] = digit arg2
   * [11] = all arg2
   * [12] = alpha arg2
   *
   *
   */
  
#define PMATCH_PID		 2
#define PMATCH_ARG1_DIGIT	 4
#define PMATCH_ARG1_ST_DIGIT	 6
#define PMATCH_ARG1_ALPHA	 7
#define PMATCH_ARG2_DIGIT	10
#define PMATCH_ARG2_ALL		11
#define PMATCH_ARG2_ALPHA	12

static struct hsearch_data reg_hash_table;
static int reg_hash_table_valid = 0;
static struct hsearch_data sys_hash_table;
static int sys_hash_table_valid = 0;

static regex_t preg = {.buffer = NULL };
static regmatch_t * pmatch = NULL;

void
arch_specific_terminate()
{
  if (reg_hash_table_valid) hdestroy_r (&reg_hash_table);
  if (sys_hash_table_valid) hdestroy_r (&sys_hash_table);
  if (pmatch) free (pmatch);
  if (preg.buffer) regfree (&preg);
}


void
create_sys_hash_table()
{
  int i, rc;
  rc = hcreate_r ((4 * nr_syscall_names)/3, &sys_hash_table);
  if (0 == rc) {
    cleanup_udb();
    utracer_close(udb_pid);
    fprintf (stderr, "\tCreating syscall hash table failed.\n");
    _exit (1);
  }

  for (i = 0; i < nr_syscall_names; i++) {
    ENTRY * entry;
    if (0 == hsearch_r (syscall_names[i], ENTER, &entry, &sys_hash_table)) {
      cleanup_udb();
      utracer_close(udb_pid);
      error (1, errno, "Error building syscall hash.");
    }
  }
  sys_hash_table_valid = 1;
}

long
parse_syscall (char * tok)
{
  long syscall_nr = SYSCALL_INVALID;
  
  if (0 == strcasecmp (tok, "all"))
    syscall_nr = SYSCALL_ALL;
  else if (sys_hash_table_valid) {
    ENTRY * entry;
    ENTRY target;
    
    target.key = tok;
    if (0 != hsearch_r (target, FIND, &entry, &sys_hash_table))
      syscall_nr = (long)(entry->data);
  }
  if (SYSCALL_INVALID == syscall_nr) {
    char * ep;
	
    syscall_nr = strtol (tok, &ep, 0);

    if (0 == *ep) {
      if ((0 > syscall_nr) || (syscall_nr >= nr_syscall_names)) {
	fprintf (stderr, "\tSorry, syscall number %ld is out of range.\n",
		 syscall_nr);
	syscall_nr = SYSCALL_INVALID;
      }
    }
    else {
      fprintf (stderr, "\tSorry, I don't recognise syscall %s\n", tok);
      syscall_nr = SYSCALL_INVALID;
    }
  }
  return syscall_nr;
}


int
parse_regspec (char ** saveptr, long * pid_p, long * regset_p, long *reg_p)
{
  int rc;
  long reg = INVALID_REG;
  long regset = INVALID_REG;


  if ('?' == **saveptr) {
    int i;

    fprintf (stderr, "\nformat: pr {[pid]} regset, reg\n");
    fprintf (stderr, "        pr {[pid]} symbol\n");
    fprintf (stderr, "        pr {[pid]} symbol, reg\n\n");
    fprintf (stderr,
	     "where regset and reg are numeric and symbol is from the \
following list.\nA register value of -1 or \"all\" prints all of the regs in \
the set.\n\n");
    for (i = 0; i < nr_regs; i++) 
      fprintf (stdout, "\t%s\n", reg_mapping[i].key);
    
    return 0;
  }

  if (!reg_hash_table_valid) {
    int i, rc;
    
    rc = hcreate_r ((4 * nr_regs)/3, &reg_hash_table);
    if (0 == rc) {
      fprintf (stderr, "\tCreating register hash table failed.\n");
      return;
    }

    for (i = 0; i < nr_regs; i++) {
      ENTRY * entry;
      if (0 == hsearch_r (reg_mapping[i], ENTER, &entry, &reg_hash_table)) {
	perror ("Error building register hash.");
	return;
      }
    }

    rc = regcomp (&preg, re, REG_EXTENDED | REG_ICASE);
    if (0 == rc) pmatch = malloc ((1 + preg.re_nsub) * sizeof(regmatch_t));
    
    reg_hash_table_valid = 1;
  }

  /* fixme -- add stuff to recognise alpha regset specs */
  if (pmatch) {
    int i;
    rc = regexec (&preg, *saveptr, 1 + preg.re_nsub, pmatch, 0);
    if (0 == rc) {
      if (-1 != pmatch[PMATCH_PID].rm_so) {
	if (pid_p) *pid_p =  atol (&((*saveptr)[pmatch[PMATCH_PID].rm_so]));
      }
      
      if (-1 != pmatch[PMATCH_ARG1_DIGIT].rm_so) {
	reg =  atol (&((*saveptr)[pmatch[PMATCH_ARG1_DIGIT].rm_so]));
      }
      else if (-1 != pmatch[PMATCH_ARG1_ST_DIGIT].rm_so) {
	regset = 1;		// fixme -- make symbolic
	reg =  atol (&((*saveptr)[pmatch[PMATCH_ARG1_ST_DIGIT].rm_so]));
      }
      else if (-1 != pmatch[PMATCH_ARG1_ALPHA].rm_so) {
	ENTRY * entry;
	ENTRY target;
	
	char * key = alloca (1 + (pmatch[PMATCH_ARG1_ALPHA].rm_eo -
	pmatch[PMATCH_ARG1_ALPHA].rm_so));
	memcpy (key, &((*saveptr)[pmatch[PMATCH_ARG1_ALPHA].rm_so]),
		pmatch[PMATCH_ARG1_ALPHA].rm_eo -
		pmatch[PMATCH_ARG1_ALPHA].rm_so);
	key[pmatch[PMATCH_ARG1_ALPHA].rm_eo -
	pmatch[PMATCH_ARG1_ALPHA].rm_so] = 0;

	target.key = key;
	if (0 != hsearch_r (target, FIND, &entry, &reg_hash_table)) {
	  reg    = ((reg_data_s *)(entry->data))->reg;
	  regset = ((reg_data_s *)(entry->data))->regset;
	}
      }

      if (-1 != pmatch[PMATCH_ARG2_DIGIT].rm_so) {
	if (INVALID_REG == regset) regset = reg;
	reg =  atol (&((*saveptr)[pmatch[PMATCH_ARG2_DIGIT].rm_so]));
      }
      else if (-1 != pmatch[PMATCH_ARG2_ALL].rm_so) {
	reg = -1;
      }
    }

#if 0
    for (i = 0; i < 1+preg.re_nsub; i++) {
      fprintf (stderr, "[%d] %d %d \"%.*s\"\n",
	       i,
	       pmatch[i].rm_so,
	       pmatch[i].rm_eo,
	       pmatch[i].rm_eo - pmatch[i].rm_so,
	       (-1 != pmatch[i].rm_so) ? &((*saveptr)[pmatch[i].rm_so])  : ""
	       );
    }
#endif
  }
  
  if (INVALID_REG == reg)    reg    = -1;
  if (INVALID_REG == regset) regset = 0;

  if (reg_p)    *reg_p    = reg;
  if (regset_p) *regset_p = regset;
  
  return 1;
}

#if 0
struct user_fxsr_struct {
  unsigned short  cwd;
  unsigned short  swd;
  unsigned short  twd;
  unsigned short  fop;
  long    fip;
  long    fcs;
  long    foo;
  long    fos;
  long    mxcsr;
  long    reserved;
  long    st_space[32];   /* 8*16 bytes for each FP-reg = 128 bytes */
  long    xmm_space[32];  /* 8*16 bytes for each XMM-reg = 128 bytes */
  long    padding[56];
};

// from arch/i386/kernel/ptrace.c native_regsets
// 0: gprs:		n =  17, size = sizeof(long), align = sizeof(long)
// 1: fprs:		n =  27, size = sizeof(long), align = sizeof(long)
// 2: fprx:		n = 126, size = sizeof(long), align = sizeof(long)
// 3: descriptors:	n =   3, size = sizeof(user_desc), align = size
//				GDTR, LDTR and IDTR
// 4: debug:		n =   8, size = sizeof(long), align = sizeof(long)
#endif

void
show_regs (long pid, long regset, long reg, void * regsinfo,
	   unsigned int regs_count, unsigned int reg_size)
{
  long i,mx,k;
  char * rn;
  
  //	from include/asm-i386/user.h
  struct user_i387_struct * i387_regs;
  
  //	from include/asm-i386/ldt.h
  struct user_desc * ud;
  
  
  fprintf (stdout, "\t[%ld] %s:\n", pid, regset_names[regset]);
  switch (regset) {
    case REGSET_GPRS:			// gprs
    case REGSET_DEBUG:			// debug
      mx = regs_count;
      break;
    case REGSET_DESC:
      mx = regs_count;
      ud = regsinfo;
      fprintf (stdout,
	       "\t\t\t\tentry\tbase\t  limit 32b cts rx lmt nosg ok\n");
      break;
    case REGSET_FPRS:			// fprs
      i387_regs = regsinfo;

      fprintf (stdout, "\t\tcwd      swd      twd      fip\n");
      fprintf (stdout, "\t\t%08x %08x %08x %08x\n",
	       i387_regs->cwd,
	       i387_regs->swd,
	       i387_regs->twd,
	       i387_regs->fip);
      fprintf (stdout, "\t\tfcs      foo      fos\n");
      fprintf (stdout, "\t\t%08x %08x %08x\n\n",
	       i387_regs->fcs,
	       i387_regs->foo,
	       i387_regs->fos);
      mx = 8;
      break;
  }
  
  if (-1 == reg) {
    i = 0;
    k = mx;
  }
  else {
    i = reg;
    k = reg + 1;
  }
  
  for (; i < k; i++) {
    switch (regset) {
    case REGSET_GPRS:			// gprs
      rn = (i < mx) ? reg_mapping[i].key : "";
      fprintf (stdout, "\t\t [%d (%s)]: [%#08x] %ld\n",
	       i, rn, ((long *)regsinfo)[i], ((long *)regsinfo)[i]);
      break;
    case REGSET_FPRS:			// fprs
      {
	int j;
	char * fp_vec = (char *)&i387_regs->st_space;
	fp_vec += i * 10;
	fprintf (stdout, "\t\t [%d]: ", i);;
	for (j = 0; j < 10; j++) fprintf (stdout, "%02x ", fp_vec[j]);
	fprintf (stdout, "\n");
      }
      break;
    case REGSET_FPRX:			// fprx
      break;
    case REGSET_DESC:			// descriptors
      rn = (i < mx) ? descriptor_names[i] : "";
      fprintf (stdout, "\t\t [%d (%s)]:\t", i, rn);
      fprintf (stdout,
	       "%u\t%08lx  %u     %u   %u   %u  %u   %u    %u\n",
	       ud[i].entry_number,
	       ud[i].base_addr,
	       ud[i].limit,
	       ud[i].seg_32bit,
	       ud[i].contents,
	       ud[i].read_exec_only,
	       ud[i].limit_in_pages,
	       ud[i].seg_not_present,
	       ud[i].useable);
      break;
    case REGSET_DEBUG:			// debug
      fprintf (stdout, "\t\t [%d]: [%#08x] %ld\n",
	       i, ((long *)regsinfo)[i], ((long *)regsinfo)[i]);
      break;
    }
  }
}


void
show_syscall (long type, long pid, struct pt_regs * regs)
{
  void * addr_to_show = NULL;
  fprintf (stdout, "\t[%ld] %s syscall: %s (",
	   pid,
	   ((IF_RESP_SYSCALL_EXIT_DATA == type) ?
	    "exiting" : "entering"),
    ((0 <= regs->orig_eax) && (regs->orig_eax < nr_syscall_names))
	   ? syscall_names[regs->orig_eax].key : "unknown");
	   
  switch (regs->orig_eax) {
  case __NR_read:
    fprintf (stdout, "%ld, 0x%08x, %ld",
	     regs->ebx,
	     regs->ecx,
	     regs->edx);
    if (IF_RESP_SYSCALL_EXIT_DATA == type) addr_to_show = (void *)regs->ecx;
    break;
  case __NR_write:
    fprintf (stdout, "%ld, 0x%08x, %ld",
	     regs->ebx,
	     regs->ecx,
	     regs->edx);
    addr_to_show = (void *)regs->ecx;
    break;
  case __NR_close:
    fprintf (stdout, "%ld", regs->ebx);
    break;
  default:
    fprintf (stdout, "?????????");
    break;
  }
  fprintf (stdout, ")\n");

  if (addr_to_show) {
    int rc;
#define SC_BFFR_LEN 56
    void * bffr = alloca (SC_BFFR_LEN);

    rc = utracer_get_mem (udb_pid, pid, addr_to_show,
			  SC_BFFR_LEN, &bffr, NULL);

    if (0 == rc)
      fprintf (stdout, "\t\tat addr %08x: \"%.*s\"\n",
	       addr_to_show, SC_BFFR_LEN, (char *)bffr);
    else utracer_uerror ("syscall getmem");
  }
}

void
show_syscall_regs (struct pt_regs * regs)
{
  fprintf (stdout, "\neip (%1$08x) %1$12lu\n",
	   regs->eip);
  
  fprintf (stdout, "orig_eax (%1$08x) %1$12lu\n",
	   regs->orig_eax);

  fprintf (stdout, "\
eax (%1$08x) %1$12ld  ebx (%2$08x) %2$12ld\n\
ecx (%3$08x) %3$12ld  edx (%4$08x) %4$12ld\n",
	   regs->eax, regs->ebx,
	   regs->ecx, regs->edx);

  fprintf (stdout, "\
ebp (%1$08x) %1$12ld  esp (%2$08x) %2$12ld\n\
edi (%3$08x) %3$12ld  esi (%4$08x) %4$12ld\n",
	   regs->ebp, regs->esp,
	   regs->edi, regs->esi);

#if 0
  fprintf (stdout, "\
xds (%1$08x) %1$12d  xes (%2$08x) %2$12d\n\
xcs (%3$08x) %3$12d  xss (%4$08x) %4$12d\n",
	   regs->xds, regs->xes,
	   regs->xcs, regs->xss);
#endif
}

